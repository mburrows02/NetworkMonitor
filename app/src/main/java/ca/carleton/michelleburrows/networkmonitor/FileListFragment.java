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
import android.widget.TextView;

import java.io.File;

/**
 * Created by Michelle on 3/21/2015.
 */
public class FileListFragment extends Fragment {

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
    }

    @Override
    public void onResume() {
        super.onResume();
        File dir = new File(MainActivity.FILE_DIR);
        String[] files = dir.list();

        ListView list = (ListView)getActivity().findViewById(R.id.file_listview);
        list.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_activated_1, files));
        list.setOnItemClickListener(fileClickHandler);
    }

    private AdapterView.OnItemClickListener fileClickHandler = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String file = MainActivity.FILE_DIR + ((TextView) view).getText();
            Log.v(MainActivity.TAG, "Opening file " + file);
            Bundle bundle = new Bundle();
            bundle.putString("filename", file);
            Fragment newFrag = new MessageListFragment();
            newFrag.setArguments(bundle);

            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, newFrag)
                    .addToBackStack(null)
                    .commit();
        }
    };
}
