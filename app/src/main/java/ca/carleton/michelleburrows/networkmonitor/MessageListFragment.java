package ca.carleton.michelleburrows.networkmonitor;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.gatech.sjpcap.IPPacket;
import edu.gatech.sjpcap.Packet;
import edu.gatech.sjpcap.PcapParser;
import edu.gatech.sjpcap.TCPPacket;

/**
 * Created by Michelle on 3/21/2015.
 */
public class MessageListFragment extends Fragment {
    private String openFile;
    private List<HashMap<String, String>> messageList;

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
        List<String> packetList = new ArrayList<String>();
        messageList = new ArrayList<HashMap<String, String>>();

        PcapParser parser = new PcapParser();
        parser.openFile(openFile);
        Packet packet = parser.getPacket();
        while (packet != Packet.EOF) {
            Log.v(MainActivity.TAG, "Parsing packet " + packetList.size());
            if (!(packet instanceof IPPacket)) {
                packet = parser.getPacket();
                continue;
            }
            IPPacket ipPack = (IPPacket) packet;
            boolean outgoing = isLocalAddress(ipPack.src_ip);
            String host = outgoing?ipPack.dst_ip.getHostAddress():ipPack.src_ip.getHostAddress();
            String type = outgoing?"Request to: ":"Response from: ";
            Log.v(MainActivity.TAG, "\t" + type + host);

            if (packet instanceof TCPPacket) {
                HashMap<String, String> messageMap = new HashMap<String, String>();
                messageMap.put(MainActivity.IS_REQUEST, String.valueOf(outgoing));
                messageMap.put(MainActivity.SOURCE, ipPack.src_ip.getHostAddress());
                messageMap.put(MainActivity.DESTINATION, ipPack.dst_ip.getHostAddress());

                TCPPacket tcpPack = (TCPPacket) ipPack;
                byte[] data = tcpPack.data;
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
                        packetList.add(host + ": " + reqLine.getMethod() + " " + path);
                        Log.v(MainActivity.TAG, "Request line: " + reqLine.toString());

                        messageMap.put(MainActivity.PATH, path);
                        messageMap.put(MainActivity.METHOD, reqLine.getMethod());
                        for (Header h : req.getAllHeaders()) {
                            messageMap.put(MainActivity.HEADER_PREFIX + h.getName(), h.getValue());
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
                        packetList.add(host + ": " + status);
                        Log.v(MainActivity.TAG, "Status line: " + resp.getStatusLine().toString());

                        messageMap.put(MainActivity.STATUS, status);
                        for (Header h : resp.getAllHeaders()) {
                            messageMap.put(MainActivity.HEADER_PREFIX + h.getName(), h.getValue());
                        }
                        StringWriter writer = new StringWriter();
                        HttpEntity entity = resp.getEntity();
                        if (entity != null) {
                            String encoding = entity.getContentEncoding().getValue();
                            IOUtils.copy(entity.getContent(), writer, encoding);
                            String contentString = writer.toString();
                            messageMap.put(MainActivity.CONTENT, contentString);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (HttpException e) {
                        e.printStackTrace();
                    }
                }

                messageList.add(messageMap);
            }

            packet = parser.getPacket();
        }

        String[] packets = packetList.toArray(new String[packetList.size()]);
        ListView list = (ListView)getActivity().findViewById(R.id.packet_listview);
        list.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_activated_1, packets));
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
