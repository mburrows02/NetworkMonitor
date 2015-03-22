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
    private static final String GENERAL = "General";
    private static final String REQ_HEAD = "Request Headers";
    private static final String RSP_HEAD = "Response Headers";
    private static final String RSP_CONTENT = "Response Content";
    private static final String COOKIES = "Cookies";
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

        String general = "";
        String requestHeaders = "";
        String responseHeaders = "";
        String responseContent = "";
        String cookies = "";

        for (Map.Entry<String, String> entry : message.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            String line = key.substring(3) + ": " + value + "\n";
            if (key.startsWith(MainActivity.REQ_HEADER)) {
                if (key.contains("Cookie")) {
                    String[] pairs = value.split(";");
                    for (String str : pairs) {
                        cookies += str + "\n";
                    }
                } else {
                    requestHeaders += line;
                }
            } else if (key.startsWith(MainActivity.RSP_HEADER)) {
                responseHeaders += line;
            } else if (key.equals(MainActivity.CONTENT)) {
                responseContent += value;
            } else {
                general += line;
            }
        }
        List<AccordionItem> detailList = new ArrayList<AccordionItem>();
        if (!general.isEmpty()) {
            detailList.add(new AccordionItem(GENERAL, general));
        }
        if (!requestHeaders.isEmpty()) {
            detailList.add(new AccordionItem(REQ_HEAD, requestHeaders));
        }
        if (!responseHeaders.isEmpty()) {
            detailList.add(new AccordionItem(RSP_HEAD, responseHeaders));
        }
        if (!responseContent.isEmpty()) {
            detailList.add(new AccordionItem(RSP_CONTENT, responseContent));
        }
        if (!cookies.isEmpty()) {
            detailList.add(new AccordionItem(COOKIES, cookies));
        }
        AccordionItem[] messageDetails = detailList.toArray(new AccordionItem[detailList.size()]);

        ListView list = (ListView)getActivity().findViewById(R.id.message_details_listview);
        list.setAdapter(new AccordionAdapter(getActivity(), messageDetails));
    }
}
