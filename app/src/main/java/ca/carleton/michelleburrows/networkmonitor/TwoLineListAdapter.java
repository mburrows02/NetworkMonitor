package ca.carleton.michelleburrows.networkmonitor;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

/**
 * A list adapter that prints MessageSummary information on two lines.
 * Created by Michelle on 3/21/2015.
 */
public class TwoLineListAdapter extends BaseAdapter {
    Context context;
    MessageSummary[] data;
    private static LayoutInflater inflater = null;

    public TwoLineListAdapter(Context context, MessageSummary[] data) {
        this.context = context;
        this.data = data;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return data.length;
    }

    @Override
    public Object getItem(int position) {
        return data[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.two_line_list_item, null);
        }
        TextView headerView = (TextView) view.findViewById(R.id.listitem_header);
        headerView.setText(data[position].getMethod() + ": " + data[position].getStatus());
        TextView contentView = (TextView) view.findViewById(R.id.listitem_detail);
        contentView.setText(data[position].getPath());
        return view;
    }
}
