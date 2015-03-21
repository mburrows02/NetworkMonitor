package ca.carleton.michelleburrows.networkmonitor;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fragment for displaying the details of an HTTP message
 * Created by Michelle on 3/21/2015.
 */
public class MessageDetailFragment extends Fragment {
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
        List<AccordionItem> detailList = new ArrayList<AccordionItem>();
        for (Map.Entry<String, String> entry : message.entrySet()) {
            if (entry.getKey().startsWith(MainActivity.HEADER_PREFIX)) {
                detailList.add(new AccordionItem(entry.getKey().substring(2), entry.getValue()));
            }
        }
        AccordionItem[] messageDetails = detailList.toArray(new AccordionItem[detailList.size()]);

        ListView list = (ListView)getActivity().findViewById(R.id.message_details_listview);
        list.setAdapter(new AccordionAdapter(getActivity(), messageDetails));
    }
}
