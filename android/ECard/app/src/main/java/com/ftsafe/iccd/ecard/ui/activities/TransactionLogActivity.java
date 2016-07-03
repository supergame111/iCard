package com.ftsafe.iccd.ecard.ui.activities;

import android.app.ListActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.ftsafe.iccd.ecard.Config;
import com.ftsafe.iccd.ecard.MainActivity;
import com.ftsafe.iccd.ecard.business.TransactionLogLogic;
import com.ftsafe.iccd.ecard.pojo.Tag;
import com.ftsafe.iccd.ecard.pojo.TransactionLog;
import com.ftsafe.iccd.ecard.ui.adapters.TransactionLogListAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ftsafe.reader.Reader;

public class TransactionLogActivity extends ListActivity {

    private TransactionLogListAdapter adapter = null;

    private Reader reader;

    // UI
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.ftsafe.iccd.ecard.R.layout.activity_transaction_log);

//        mProgressBar = (ProgressBar) findViewById(R.id.load_progress);

        Log.e(Config.APP_ID, "onCreate");
        adapter = new TransactionLogListAdapter(this);
        setListAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadMessage();
    }

    private void loadMessage() {
        adapter.clear();

        if (MainActivity.MiniPay != null) {
            reader = MainActivity.MiniPay;
        } else
            reader = MainActivity.BtReader;

        if (reader != null) {
            new TransactionLogLogic(TransactionLogActivity.this, reader, new TransactionLogLogic.TlCallback() {
                @Override
                public void onResult(List<HashMap<String, String>> result) {
                    List<TransactionLog> logList = new ArrayList<>();
                    for (HashMap<String, String> cellData : result) {

                        logList.add(new TransactionLog(cellData.get(Tag._5F2A), cellData.get(Tag._9C), cellData.get(Tag._9F02), cellData.get(Tag._9A) + cellData.get(Tag._9F21)));
                    }

                    adapter.addAll(logList);
                }

                @Override
                public void onMessage(String msg) {
                    Toast.makeText(TransactionLogActivity.this, msg, Toast.LENGTH_LONG).show();
                }
            });
        }

    }

}
