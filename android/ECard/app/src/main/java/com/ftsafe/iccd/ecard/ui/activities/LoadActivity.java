package com.ftsafe.iccd.ecard.ui.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
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

    public static String AMT = "000000000000";
    private EditText mEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load);
        mEditText = (EditText) findViewById(R.id.edit_load_money);
    }

    public void onLoad(View view) {
        if (mEditText.getText().length() > 0) {
            String tmp = mEditText.getText().toString();
            int i = tmp.indexOf(".");
            if (i > 0) {
                int tmpL = tmp.length();
                if (tmpL - i == 3) {
                    tmp = tmp.replace(".", "");
                } else if (tmpL - 1 == 2) {
                    tmp = tmp.replace(".", "") + "00";
                } else
                    tmp = tmp.substring(0, i + 3);
            } else
                tmp = tmp + "00";
            AMT = AMT.substring(0, 12 - tmp.length()) + tmp;
            Toast.makeText(this, "Load", Toast.LENGTH_SHORT).show();
            ReaderManager.readCard(MainActivity.BtReader, StandardLoad.class, this);
        }
    }

    @Override
    public void onReadEvent(SPEC.EVENT event, Object... objs) {

    }
}
