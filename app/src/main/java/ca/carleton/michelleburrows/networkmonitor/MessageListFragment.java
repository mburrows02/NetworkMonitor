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
import org.apache.httpcopy.impl.io.DefaultHttpRequestParser;
import org.apache.httpcopy.impl.io.DefaultHttpResponseParser;
import org.apache.httpcopy.impl.io.HttpTransportMetricsImpl;
import org.apache.httpcopy.impl.io.SessionInputBufferImpl;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.pkts.Pcap;

/**
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

        Log.v(MainActivity.TAG, "Parsing " + openFile);
        List<MessageSummary> packetList = new ArrayList<MessageSummary>();
        messageList = new ArrayList<HashMap<String, String>>();
        openReqIndices = new ArrayList<Integer>();

        try {
            Pcap pcap = Pcap.openStream(openFile);
            ReassemblingPacketHandler handler = new ReassemblingPacketHandler();
            pcap.loop(handler);
            for (ReassembledPacket packet : handler.packets) {
                Log.v(MainActivity.TAG, "Parsing packet " + packetList.size());
                boolean outgoing = isLocalAddress(InetAddress.getByName(packet.getSrc()));
                String host = outgoing ? packet.getDst() : packet.getSrc();
                String type = outgoing ? "Request to: " : "Response from: ";
                Log.v(MainActivity.TAG, "\t" + type + host);

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
                /*try {
                    Log.v(MainActivity.TAG, IOUtils.toString(data, "US-ASCII"));
                } catch (IOException e) {
                    e.printStackTrace();
                }*/
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
                        Log.v(MainActivity.TAG, "Request line: " + reqLine.toString());

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
                        Log.v(MainActivity.TAG, "Status line: " + resp.getStatusLine().toString());

                        messageMap.put(MainActivity.STATUS, status);
                        for (Header h : resp.getAllHeaders()) {
                            messageMap.put(MainActivity.RSP_HEADER + h.getName(), h.getValue());
                        }
                        StringWriter writer = new StringWriter();
                        HttpEntity entity = resp.getEntity();
                        if (entity != null) {
                            String encoding = entity.getContentEncoding().getValue();
                            byte[] buff = new byte[(int) entity.getContentLength()];
                            entity.getContent().read(buff);
                            String contentString = IOUtils.toString(buff, encoding);
                            Log.v(MainActivity.TAG, "Response contents: " + contentString); //TODO ensure this is removed
                            //IOUtils.copy(entity.getContent(), writer, encoding);
                            //String contentString = writer.toString();
                            messageMap.put(MainActivity.CONTENT, contentString);
                        } else {
                            Log.v(MainActivity.TAG, "Entity is null!");
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

    private boolean isLocalAddress(InetAddress addr) {
        if (addr.isAnyLocalAddress() || addr.isLoopbackAddress()) {
            return true;
        }

        try {
            return NetworkInterface.getByInetAddress(addr) != null;
        } catch (SocketException e) {
            return false;
        }
    }
}
