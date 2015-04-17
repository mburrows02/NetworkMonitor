package ca.carleton.michelleburrows.networkmonitor;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import org.apache.commons.io.IOUtils;
import org.apache.httpcopy.Header;
import org.apache.httpcopy.HttpEntity;
import org.apache.httpcopy.HttpException;
import org.apache.httpcopy.HttpRequest;
import org.apache.httpcopy.HttpResponse;
import org.apache.httpcopy.RequestLine;
import org.apache.httpcopy.impl.io.ChunkedInputStream;
import org.apache.httpcopy.impl.io.DefaultHttpRequestParser;
import org.apache.httpcopy.impl.io.DefaultHttpResponseParser;
import org.apache.httpcopy.impl.io.HttpTransportMetricsImpl;
import org.apache.httpcopy.impl.io.SessionInputBufferImpl;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.GZIPInputStream;

import io.pkts.Pcap;

/**
 * Fragment that parses a packet capture file, reconstructs HTTP messages, pairs requests
 * and responses together, and lists message pairs.
 * Created by Michelle on 3/21/2015.
 */
public class MessageListFragment extends Fragment {
    private String openFile;
    private List<HashMap<String, String>> messageList;
    private List<Integer> openReqIndices;

    public MessageListFragment() {

    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        openFile = args.getString("filename");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_packet_list, container, false);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        List<MessageSummary> packetList = new ArrayList<MessageSummary>();
        messageList = new ArrayList<HashMap<String, String>>();
        openReqIndices = new ArrayList<Integer>();

        try {
            Pcap pcap = Pcap.openStream(openFile);
            ReassemblingPacketHandler handler = new ReassemblingPacketHandler();
            pcap.loop(handler);
            for (ReassembledPacket packet : handler.packets) {
                boolean outgoing = !isResponse(packet.getData());
                String host = outgoing ? packet.getDst() : packet.getSrc();
                String type = outgoing ? "Request to: " : "Response from: ";

                HashMap<String, String> messageMap = null;
                MessageSummary msgSummary = null;
                int reqIndex = -1;
                if (!outgoing) {
                    for (int index : openReqIndices) {
                        HashMap<String, String> mm = messageList.get(index);
                        if (mm.get(MainActivity.HOST).equals(host)) {
                            messageMap = mm;
                            msgSummary = packetList.get(index);
                            reqIndex = index;
                            break;
                        }
                    }
                    if (reqIndex != -1) {
                        openReqIndices.remove(Integer.valueOf(reqIndex));
                    }
                }

                if (messageMap == null) {
                    messageMap = new HashMap<String, String>();
                    messageList.add(messageMap);
                    messageMap.put(MainActivity.HOST, host);
                    msgSummary = new MessageSummary();
                    packetList.add(msgSummary);
                    if (outgoing) {
                        openReqIndices.add(packetList.size() - 1);
                    }
                }

                byte[] data = packet.getData();
                SessionInputBufferImpl inBuffer = new SessionInputBufferImpl(new HttpTransportMetricsImpl(), data.length);
                InputStream inStream = new ByteArrayInputStream(data);
                inBuffer.bind(inStream);
                if (outgoing) {
                    DefaultHttpRequestParser reqParser = new DefaultHttpRequestParser(inBuffer);
                    try {
                        HttpRequest req = (HttpRequest) reqParser.parse();
                        RequestLine reqLine = req.getRequestLine();
                        String path = reqLine.getUri();
                        if (path.length() > MainActivity.TRUNCATE_POINT) {
                            path = path.substring(0, MainActivity.TRUNCATE_POINT) + "...";
                        }
                        msgSummary.setMethod(reqLine.getMethod());
                        msgSummary.setPath(path);

                        messageMap.put(MainActivity.PATH, path);
                        messageMap.put(MainActivity.METHOD, reqLine.getMethod());
                        for (Header h : req.getAllHeaders()) {
                            messageMap.put(MainActivity.REQ_HEADER + h.getName(), h.getValue());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (HttpException e) {
                        e.printStackTrace();
                    }
                } else {
                    DefaultHttpResponseParser respParser = new DefaultHttpResponseParser(inBuffer);
                    try {
                        HttpResponse resp = (HttpResponse) respParser.parse();
                        String status = resp.getStatusLine().getStatusCode() + " " + resp.getStatusLine().getReasonPhrase();
                        msgSummary.setStatus(status);

                        messageMap.put(MainActivity.STATUS, status);
                        for (Header h : resp.getAllHeaders()) {
                            messageMap.put(MainActivity.RSP_HEADER + h.getName(), h.getValue());
                        }
                        HttpEntity entity = resp.getEntity();
                        if (entity != null) {
                            String encoding = entity.getContentEncoding().getValue();
                            byte[] buff = new byte[(int) entity.getContentLength()];
                            entity.getContent().read(buff);
                            String contentString = IOUtils.toString(buff, encoding);
                            messageMap.put(MainActivity.CONTENT, contentString);
                        } else {
                            StringBuilder contentBuilder = new StringBuilder();
                            if (resp.containsHeader("Content-Type") && resp.getFirstHeader("Content-Type").getValue().contains("text") &&
                                    resp.containsHeader("Transfer-Encoding") && resp.getFirstHeader("Transfer-Encoding").getValue().contains("chunked") &&
                                    resp.containsHeader("Content-Encoding") && resp.getFirstHeader("Content-Encoding").getValue().contains("gzip")) {
                                byte[] content = getContent(packet.getData());
                                if (content.length > 0) {
                                    InputStream byteIS = new ByteArrayInputStream(content);
                                    SessionInputBufferImpl contentBuf = new SessionInputBufferImpl(new HttpTransportMetricsImpl(), content.length);
                                    contentBuf.bind(byteIS);

                                    ChunkedInputStream chunkedIS = new ChunkedInputStream(contentBuf);

                                    GZIPInputStream gzipIS = new GZIPInputStream(chunkedIS);

                                    while (gzipIS.available() != 0) {
                                        byte[] buf = new byte[128];
                                        gzipIS.read(buf);
                                        contentBuilder.append(new String(buf, "UTF-8"));
                                    }
                                    gzipIS.close();
                                    String contentString = contentBuilder.toString();
                                    messageMap.put(MainActivity.CONTENT, contentString);

                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (HttpException e) {
                        e.printStackTrace();
                    }
                }

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        MessageSummary[] packets = packetList.toArray(new MessageSummary[packetList.size()]);
        ListView list = (ListView)getActivity().findViewById(R.id.packet_listview);
        list.setAdapter(new TwoLineListAdapter(getActivity(), packets));
        list.setOnItemClickListener(packetClickHandler);
    }

    private AdapterView.OnItemClickListener packetClickHandler = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Bundle bundle = new Bundle();
            bundle.putSerializable("message", messageList.get(position));
            Fragment newFrag = new MessageDetailFragment();
            newFrag.setArguments(bundle);

            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, newFrag)
                    .addToBackStack(null)
                    .commit();
        }
    };

    private boolean isResponse(byte[] data) {
        try {
            return new String(data, "UTF-8").startsWith("HTTP");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return false;
    }


    private byte[] getContent(byte[] message) {
        int start = -1;
        byte[] content = null;
        for (int i = 0; i < message.length; ++i) {
            if (start >= 0) {
                content[i-start] = message[i];
                continue;
            }
            System.out.print((char)message[i]);
            if (message[i] == (byte) 13 && message[i+1]==(byte)10 && message[i+2] == (byte) 13 && message[i+3]==(byte)10 ) { //CR
                start = i+4;
                content = new byte[message.length-(i+4)];
                i += 3;
            }
        }
        return content;
    }
}
