package ca.carleton.michelleburrows.networkmonitor;

import android.os.StrictMode;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.gatech.sjpcap.IPPacket;
import edu.gatech.sjpcap.Packet;
import edu.gatech.sjpcap.PcapParser;
import edu.gatech.sjpcap.TCPPacket;

import org.apache.commons.io.IOUtils;
import org.apache.httpcopy.Header;
import org.apache.httpcopy.HttpEntity;
import org.apache.httpcopy.HttpException;
import org.apache.httpcopy.HttpMessage;
import org.apache.httpcopy.HttpRequest;
import org.apache.httpcopy.HttpResponse;
import org.apache.httpcopy.impl.io.DefaultHttpRequestParser;
import org.apache.httpcopy.impl.io.DefaultHttpResponseParser;
import org.apache.httpcopy.impl.io.HttpTransportMetricsImpl;
import org.apache.httpcopy.impl.io.SessionInputBufferImpl;


public class MainActivity extends ActionBarActivity {
    private Process captureProc;
    private static final String TAG = "NetworkMonitor";
    private static final int PACKETS = 20;
    private static final String FILE_DIR = "/sdcard/netlogs/";
    //TODO don't hard-code: Environment.getExternalStorageDirectory() + "/netlogs/";
    private static final int TRUNCATE_POINT = 100;
    private static final String IS_REQUEST = "nm_isRequest";
    private static final String SOURCE = "nm_src";
    private static final String DESTINATION = "nm_dest";
    private static final String PATH = "nm_path";
    private static final String METHOD = "nm_method";
    private static final String STATUS = "nm_status";
    private static final String CONTENT = "nm_content";
    private static final String HEADER_PREFIX = "h_";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.enableDefaults();
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new FileListFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        switch(id) {
            case R.id.action_start_capture:
                startCapture();
                return true;
            case R.id.action_stop_capture:
                stopCapture();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void startCapture() {
        String filename = FILE_DIR + "tcpdump_" + System.currentTimeMillis() + ".pcap";
        String command = "tcpdump -c " + PACKETS + " -w " + filename + " \'tcp port 80 and " +
                "(((ip[2:2] - ((ip[0]&0xf)<<2)) - ((tcp[12]&0xf0)>>2)) != 0)\'";
        Log.v(TAG, "Starting capture: " + command);
        try {
            captureProc = new ProcessBuilder().command("su").start();
            DataOutputStream os = new DataOutputStream(captureProc.getOutputStream());
            //DataInputStream is = new DataInputStream(captureProc.getInputStream());
            os.writeBytes(command + "\n");
            os.flush();

            Log.v(TAG, "Capture started");
        } catch (IOException e) {
            Log.e(TAG, "IOException while starting capture", e);
        }

    }

    private void stopCapture() {
        Log.v(TAG, "Stopping capture");
        if (captureProc != null) {
            try {
                DataOutputStream os = new DataOutputStream(captureProc.getOutputStream());
                os.writeBytes("\u0003\n");
                os.writeBytes("exit\n");
                os.flush();
            } catch (IOException e) {
                Log.e(TAG, "IOException while stopping capture", e);
            }
            captureProc.destroy();
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class FileListFragment extends Fragment {

        public FileListFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_file_list, container, false);
            return rootView;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            File dir = new File(FILE_DIR);
            String[] files = dir.list();

            ListView list = (ListView)getActivity().findViewById(R.id.file_listview);
            list.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_activated_1, files));
            list.setOnItemClickListener(fileClickHandler);
        }

        private AdapterView.OnItemClickListener fileClickHandler = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String file = FILE_DIR + ((TextView) view).getText();
                Log.v(TAG, "Opening file " + file);
                Bundle bundle = new Bundle();
                bundle.putString("filename", file);
                Fragment newFrag = new PacketListFragment();
                newFrag.setArguments(bundle);

                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, newFrag)
                        .addToBackStack(null)
                        .commit();
            }
        };
    }

    public static class PacketListFragment extends Fragment {
        private String openFile;
        private List<HashMap<String, String>> messageList;

        public PacketListFragment() {

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

            Log.v(TAG, "Parsing " + openFile);
            List<String> packetList = new ArrayList<String>();
            messageList = new ArrayList<HashMap<String, String>>();

            PcapParser parser = new PcapParser();
            parser.openFile(openFile);
            Packet packet = parser.getPacket();
            while (packet != Packet.EOF) {
                Log.v(TAG, "Parsing packet " + packetList.size());
                if (!(packet instanceof IPPacket)) {
                    packet = parser.getPacket();
                    continue;
                }
                IPPacket ipPack = (IPPacket) packet;
                boolean outgoing = isLocalAddress(ipPack.src_ip);
                String host = outgoing?ipPack.dst_ip.getCanonicalHostName():ipPack.src_ip.getCanonicalHostName();
                String type = outgoing?"Request to: ":"Response from: ";
                Log.v(TAG, "\t" + type + host);

                if (packet instanceof TCPPacket) {
                    HashMap<String, String> messageMap = new HashMap<String, String>();
                    messageMap.put(IS_REQUEST, String.valueOf(outgoing));
                    messageMap.put(SOURCE, ipPack.src_ip.getCanonicalHostName());
                    messageMap.put(DESTINATION, ipPack.dst_ip.getCanonicalHostName());

                    TCPPacket tcpPack = (TCPPacket) ipPack;
                    byte[] data = tcpPack.data;
                    SessionInputBufferImpl inBuffer = new SessionInputBufferImpl(new HttpTransportMetricsImpl(), data.length);
                    InputStream inStream = new ByteArrayInputStream(data);
                    inBuffer.bind(inStream);
                    if (outgoing) {
                        DefaultHttpRequestParser reqParser = new DefaultHttpRequestParser(inBuffer);
                        try {
                            HttpRequest req = (HttpRequest) reqParser.parse();
                            packetList.add(host + ": " + req.getRequestLine().getMethod() + " " + req.getRequestLine().getUri());
                            Log.v(TAG, "Request line: " + req.getRequestLine().toString());

                            messageMap.put(PATH, req.getRequestLine().getUri());
                            messageMap.put(METHOD, req.getRequestLine().getMethod());
                            for (Header h : req.getAllHeaders()) {
                                messageMap.put(HEADER_PREFIX + h.getName(), h.getValue());
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
                            Log.v(TAG, "Status line: " + resp.getStatusLine().toString());

                            messageMap.put(STATUS, status);
                            for (Header h : resp.getAllHeaders()) {
                                messageMap.put(HEADER_PREFIX + h.getName(), h.getValue());
                            }
                            StringWriter writer = new StringWriter();
                            HttpEntity entity = resp.getEntity();
                            if (entity != null) {
                                String encoding = entity.getContentEncoding().getValue();
                                IOUtils.copy(entity.getContent(), writer, encoding);
                                String contentString = writer.toString();
                                messageMap.put(CONTENT, contentString);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (HttpException e) {
                            e.printStackTrace();
                        }
                    }

                    messageList.add(messageMap);
                }

                //packetList.add(type + host);
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

    public static class MessageDetailFragment extends Fragment {
        private HashMap<String, String> message;

        public MessageDetailFragment() {

        }

        @Override
        public void setArguments(Bundle args) {
            super.setArguments(args);
            message = (HashMap<String, String>) args.getSerializable("message");
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_message_detail, container, false);
            return rootView;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            List<String> detailList = new ArrayList<String>();
            for (Map.Entry<String, String> entry : message.entrySet()) {
                if (entry.getKey().startsWith(HEADER_PREFIX)) {
                    detailList.add(entry.getKey().substring(2) + ": " + entry.getValue());
                }
            }
            String[] messageDetails = detailList.toArray(new String[detailList.size()]);

            ListView list = (ListView)getActivity().findViewById(R.id.message_details_listview);
            list.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_activated_1, messageDetails));
        }
    }
}
