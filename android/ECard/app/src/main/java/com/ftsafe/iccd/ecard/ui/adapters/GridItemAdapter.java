package com.ftsafe.iccd.ecard.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ftsafe.iccd.ecard.R;

/**
 * Created by qingyuan on 16/7/2.
 */
public class GridItemAdapter extends BaseAdapter {

    private Context mContext;

    public GridItemAdapter(Context ctx) {
        mContext = ctx;
    }

    @Override
    public int getCount() {
        return imgs.length;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(
                    R.layout.grid_item, parent, false);
        }
//        TextView tv = BaseViewHolder.get(convertView, R.id.tv_item);
//        ImageView iv = BaseViewHolder.get(convertView, R.id.iv_item);
        TextView tv = (TextView) convertView.findViewById(R.id.tv_item);
        ImageView iv = (ImageView) convertView.findViewById(R.id.iv_item);

        iv.setBackgroundResource(imgs[position]);
        tv.setText(img_text[position]);
        return convertView;
    }

    // references to our images
    private Integer[] imgs = {
            R.drawable.ic_app_card_info,
            R.drawable.ic_app_load,
            R.drawable.ic_app_consumption,
            R.drawable.ic_app_help,
            R.drawable.ic_app_more
    };
    private String[] img_text = {
            "卡片信息","充值", "消费", "帮助","更多"
    };
}
