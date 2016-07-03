package com.ftsafe.iccd.ecard.ui.activities;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.ftsafe.iccd.ecard.App;
import com.ftsafe.iccd.ecard.Config;
import com.ftsafe.iccd.ecard.MainActivity;


public class DeviceActivity extends Activity {

    public final static int SELECTED = 10;
    // UI
    private ListView mListView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.ftsafe.iccd.ecard.R.layout.activity_device);

        final App app = (App) getApplicationContext();

        mListView = (ListView) findViewById(com.ftsafe.iccd.ecard.R.id.list_view_t);
        ArrayAdapter arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, MainActivity.DeviceNameList);
        mListView.setAdapter(arrayAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Handler handler = app.getHandler();
                if (handler != null)
                    handler.obtainMessage(Config.ACTIVITY_STATUS,SELECTED,position).sendToTarget();
                finish();
            }
        });
    }
}
