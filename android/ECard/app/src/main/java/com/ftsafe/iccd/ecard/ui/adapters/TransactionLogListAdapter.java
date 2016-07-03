package com.ftsafe.iccd.ecard.ui.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.ftsafe.iccd.ecard.Config;
import com.ftsafe.iccd.ecard.R;
import com.ftsafe.iccd.ecard.pojo.TransactionLog;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by takakiyo on 15/8/10.
 */
public class TransactionLogListAdapter extends BaseAdapter {

    public TransactionLogListAdapter(Context context) {
        this.context = context;
    }

    @Override
    public int getCount() {
        return log.size();
    }

    @Override
    public Object getItem(int position) {
        return log.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.layout_transaction_log_listcell, null);
            convertView.setTag(new ListCell(
                    (TextView) convertView.findViewById(R.id.text_view_item1),
                    (TextView) convertView.findViewById(R.id.text_view_item2),
                    (TextView) convertView.findViewById(R.id.text_view_item3),
                    (TextView) convertView.findViewById(R.id.text_view_item4)));
        }

        ListCell lc = (ListCell) convertView.getTag();
        TransactionLog cellData = (TransactionLog) getItem(position);

        lc.getItem1().setText(cellData.getTerminalName());
        lc.getItem2().setText(cellData.getDate());
        lc.getItem3().setText(cellData.getType());
        lc.getItem4().setText(cellData.getMoney());

        if (cellData.getType().equals(Config.KEY_DEPOSIT)) {
            lc.getItem3().setTextColor(Color.rgb(102,153,0));
        }

        return convertView;
    }

    public Context getContext() {
        return context;
    }

    public void addAll(List<TransactionLog> log) {
        this.log.addAll(log);
        notifyDataSetChanged();
    }

    public void clear() {
        log.clear();
        notifyDataSetChanged();
    }

    private Context context;
    private List<TransactionLog> log = new ArrayList<>();

    public static class ListCell {

        private TextView item1, item2, item3, item4;

        public ListCell(TextView viewById, TextView viewById1, TextView viewById2, TextView viewById3) {
            item1 = viewById;
            item2 = viewById1;
            item3 = viewById2;
            item4 = viewById3;
        }
        public TextView getItem1() {
            return item1;
        }
        public TextView getItem2() {
            return item2;
        }
        public TextView getItem3() {
            return item3;
        }
        public TextView getItem4() {
            return item4;
        }
    }
}
