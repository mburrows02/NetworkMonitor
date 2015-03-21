package ca.carleton.michelleburrows.networkmonitor;

import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.io.DataOutputStream;
import java.io.IOException;


public class MainActivity extends ActionBarActivity {
    private Process captureProc;
    public static final String TAG = "NetworkMonitor";
    public static final int PACKETS = 20;
    public static final String FILE_DIR = "/sdcard/netlogs/";
    //TODO don't hard-code: Environment.getExternalStorageDirectory() + "/netlogs/";
    public static final int TRUNCATE_POINT = 100;
    public static final String IS_REQUEST = "nm_isRequest";
    public static final String SOURCE = "nm_src";
    public static final String DESTINATION = "nm_dest";
    public static final String PATH = "nm_path";
    public static final String METHOD = "nm_method";
    public static final String STATUS = "nm_status";
    public static final String CONTENT = "nm_content";
    public static final String HEADER_PREFIX = "h_";

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
}
