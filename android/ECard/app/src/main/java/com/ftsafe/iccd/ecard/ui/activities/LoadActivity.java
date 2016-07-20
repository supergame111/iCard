package com.ftsafe.iccd.ecard.ui.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.ftsafe.iccd.ecard.MainActivity;
import com.ftsafe.iccd.ecard.R;
import com.ftsafe.iccd.ecard.SPEC;
import com.ftsafe.iccd.ecard.reader.ReaderListener;
import com.ftsafe.iccd.ecard.reader.ReaderManager;
import com.ftsafe.iccd.ecard.reader.pboc.StandardLoad;

/**
 * Created by qingyuan on 2016/7/6.
 */
public class LoadActivity extends Activity implements ReaderListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load);
    }

    public void onLoad(View view) {
        Toast.makeText(this,"Load",Toast.LENGTH_SHORT).show();
        ReaderManager.readCard(MainActivity.BtReader, StandardLoad.class,this);
    }

    @Override
    public void onReadEvent(SPEC.EVENT event, Object... objs) {

    }
}
