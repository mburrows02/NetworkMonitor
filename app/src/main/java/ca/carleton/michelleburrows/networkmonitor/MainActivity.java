package ca.carleton.michelleburrows.networkmonitor;

import android.app.ListFragment;
import android.os.Environment;
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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;


public class MainActivity extends ActionBarActivity {
    private Process captureProc;
    private static final String TAG = "NetworkMonitor";
    private static final int PACKETS = 20;
    private static final String FILE_DIR = "/sdcard/netlogs/";
    //TODO don't hard-code: Environment.getExternalStorageDirectory() + "/netlogs/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
            Log.v(TAG, files.toString());

            ListView list = (ListView)getActivity().findViewById(R.id.file_listview);
            list.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_activated_1, files));
            list.setOnItemClickListener(fileClickHandler);
        }

        private AdapterView.OnItemClickListener fileClickHandler = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String file = FILE_DIR + ((TextView) view).getText();
                Bundle bundle = new Bundle();
                bundle.putString("filename", file);
                Fragment newFrag = new PacketListFragment();
                newFrag.setArguments(bundle);

                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, newFrag)
                        .commit();
            }
        };
    }

    public static class PacketListFragment extends Fragment {
        private String openFile;

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

            //TODO load file and parse

            String[] packets = new String[]{"Hello", "World", openFile};

            ListView list = (ListView)getActivity().findViewById(R.id.packet_listview);
            list.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_activated_1, packets));
            list.setOnItemClickListener(packetClickHandler);
        }

        private AdapterView.OnItemClickListener packetClickHandler = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //TODO
            }
        };
    }
}
