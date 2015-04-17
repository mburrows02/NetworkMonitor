package ca.carleton.michelleburrows.networkmonitor;

import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.stericson.RootShell.RootShell;
import com.stericson.RootShell.exceptions.RootDeniedException;
import com.stericson.RootShell.execution.Command;


public class MainActivity extends ActionBarActivity {
    private Command captureProc;
    public static final String TAG = "NetMon";
    public static final String FILE_DIR = "/sdcard/netlogs/";
    public static final int TRUNCATE_POINT = 100;

    public static final String HOST = "nm_host";
    public static final String PATH = "nm_path";
    public static final String METHOD = "nm_method";
    public static final String STATUS = "nm_status";
    public static final String CONTENT = "nm_content";
    public static final String REQ_HEADER = "qh_";
    public static final String RSP_HEADER = "ph_";

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
        if (captureProc == null) {
            String filename = FILE_DIR + "tcpdump_" + System.currentTimeMillis() + ".pcap";
            String commandStr = "tcpdump --packet-buffered -w " + filename + " \'tcp port 80 and " +
                    "(((ip[2:2] - ((ip[0]&0xf)<<2)) - ((tcp[12]&0xf0)>>2)) != 0)\'";
            try {
                captureProc = new Command(0, commandStr);
                RootShell.getShell(true).add(captureProc);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (RootDeniedException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            }

        }
    }

    private void stopCapture() {
        if (captureProc != null) {

            try {
                Command kill = new Command(1, "pkill tcpdump");
                RootShell.getShell(true).add(kill);
                while (!kill.isFinished());
                captureProc.terminate();
                RootShell.closeAllShells();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (RootDeniedException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            }

        }
    }
}
