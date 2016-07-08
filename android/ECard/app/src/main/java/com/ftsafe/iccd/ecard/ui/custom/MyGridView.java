package com.ftsafe.iccd.ecard.ui.custom;

import android.content.Context;
import android.widget.GridView;

/**
 * Created by qingyuan on 2016/7/7.
 */
public class MyGridView  extends GridView{
    public MyGridView(Context context) {
        super(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2,MeasureSpec.AT_MOST);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
