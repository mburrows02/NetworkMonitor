package ca.carleton.michelleburrows.networkmonitor;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * List adapter that places items in an accordion-style widget
 * Created by Michelle on 3/21/2015.
 */
public class AccordionAdapter extends BaseAdapter {
    Context context;
    AccordionItem[] data;
    private static LayoutInflater inflater = null;

    public AccordionAdapter(Context context, AccordionItem[] data) {
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
            view = inflater.inflate(R.layout.accordion_row, null);
        }
        TextView headerView = (TextView) view.findViewById(R.id.accordion_header);
        headerView.setText(data[position].getHeader());
        TextView contentView = (TextView) view.findViewById(R.id.accordion_content);
        contentView.setText(data[position].getContents());
        headerView.setOnClickListener(new AccordionClickListener());
        ImageView icon = (ImageView) view.findViewById(R.id.accordion_button);
        icon.setOnClickListener(new AccordionClickListener());
        return view;
    }

    class AccordionClickListener implements View.OnClickListener {
        boolean open = false;

        @Override
        public void onClick(View v) {
            RelativeLayout parent = (RelativeLayout) v.getParent();
            ImageView icon = (ImageView) (v.getId() == R.id.accordion_button?
                    v:parent.findViewById(R.id.accordion_button));
            TextView contents = (TextView) parent.findViewById(R.id.accordion_content);
            contents.setVisibility(open?View.GONE:View.VISIBLE);
            icon.setImageResource(open?R.drawable.expander_open:R.drawable.expander_close);
            parent.requestLayout();
            open = !open;
        }
    }
}
