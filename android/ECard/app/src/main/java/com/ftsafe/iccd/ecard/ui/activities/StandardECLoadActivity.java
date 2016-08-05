package com.ftsafe.iccd.ecard.ui.activities;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.ftsafe.iccd.ecard.App;
import com.ftsafe.iccd.ecard.Config;
import com.ftsafe.iccd.ecard.ECARDSPEC;
import com.ftsafe.iccd.ecard.MainActivity;
import com.ftsafe.iccd.ecard.R;
import com.ftsafe.iccd.ecard.SPEC;
import com.ftsafe.iccd.ecard.bean.Card;
import com.ftsafe.iccd.ecard.reader.ReaderListener;
import com.ftsafe.iccd.ecard.reader.ReaderManager;
import com.ftsafe.iccd.ecard.reader.pboc.StandardECLoad;
import com.ftsafe.iccd.ecard.reader.pboc.StandardECPay;

/**
 * Created by qingyuan on 2016/7/6.
 */
public class StandardECLoadActivity extends Activity implements ReaderListener, View.OnClickListener {
    public static short TRANS_MODE;
    public static String AMT = null;
    private EditText mEditText;
    private Button mButton;
    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load);
        mEditText = (EditText) findViewById(R.id.edit_load_money);
        mButton = (Button) findViewById(R.id.btn_charge);
        mTextView = (TextView) findViewById(R.id.textView_load);
        if (TRANS_MODE == ECARDSPEC.PAY) {
            setContentView(R.layout.activity_ecpay);
            mEditText = (EditText) findViewById(R.id.edit_pay_money);
            mButton = (Button) findViewById(R.id.btn_pay);
            mTextView = (TextView) findViewById(R.id.textView_pay);
        }
        mTextView.setVisibility(View.INVISIBLE);
        mButton.setOnClickListener(this);
    }


    @Override
    public void onReadEvent(SPEC.EVENT event, Object... objs) {

        if (event == SPEC.EVENT.IDLE) {
            showProgressBar();
        } else if (event == SPEC.EVENT.READING) {
            showProgressBar();
        } else if (event == SPEC.EVENT.FINISHED) {

            final Card card;
            if (objs != null && objs.length > 0)
                card = (Card) objs[0];
            else
                card = null;

            hideProgressBar();

            if (card.hasReadingWarning())
                Toast.makeText(this, card.getReadingWarning(), Toast.LENGTH_SHORT).show();
            else {
                //Log.e(Config.APP_ID, card.toHtml());
                mTextView.setText(Html.fromHtml(card.toHtml()));
                mTextView.setVisibility(View.VISIBLE);
            }

        } else {
            hideProgressBar();
        }

    }

    private Dialog progressBar;

    private void showProgressBar() {
        Dialog d = progressBar;
        if (d == null) {
            d = new Dialog(this, R.style.progressBar);
            d.setCancelable(false);
            d.setContentView(R.layout.progress);
            progressBar = d;
        }

        if (!d.isShowing())
            d.show();
    }

    private void hideProgressBar() {
        final Dialog d = progressBar;
        if (d != null && d.isShowing())
            d.cancel();
    }

    @Override
    public void onClick(View v) {
        if (v == mButton) {
            if (mEditText.getText().length() > 0) {
                AMT = parseAmount(mEditText.getText().toString());
                if (Integer.valueOf(AMT) != 0) {
                    if (TRANS_MODE == ECARDSPEC.LOAD) {
                        ReaderManager.readCard(MainActivity.BtReader, StandardECLoad.class, this);
                    } else {
                        ReaderManager.readCard(MainActivity.BtReader, StandardECPay.class, this);
                    }
                }
            }
        }
    }

    public static String parseAmount(String in) {
        String tmp = in;
        int i = in.indexOf(".");
        if (i > 0) {
            int tmpL = tmp.length();
            if (tmpL - i == 3) {
                tmp = tmp.replace(".", "");
            } else if (tmpL - 1 == 2) {
                tmp = tmp.replace(".", "") + "0";
            } else
                tmp = tmp.substring(0, i + 3);
        } else
            tmp = tmp + "00";
        return "000000000000".substring(0, 12 - tmp.length()) + tmp;
    }
}
