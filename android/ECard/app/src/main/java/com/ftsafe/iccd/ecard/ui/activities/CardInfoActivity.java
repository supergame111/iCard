package com.ftsafe.iccd.ecard.ui.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.ftsafe.iccd.ecard.App;
import com.ftsafe.iccd.ecard.Config;
import com.ftsafe.iccd.ecard.MainActivity;
import com.ftsafe.iccd.ecard.R;
import com.ftsafe.iccd.ecard.SPEC;
import com.ftsafe.iccd.ecard.bean.Card;
import com.ftsafe.iccd.ecard.reader.ReaderListener;
import com.ftsafe.iccd.ecard.reader.ReaderManager;
import com.ftsafe.iccd.ecard.reader.pboc.StandardECash;
import com.ftsafe.iccd.ecard.ui.pages.SpanFormatter;
import com.ftsafe.iccd.ecard.ui.pages.Toolbar;

public class CardInfoActivity extends Activity implements ReaderListener, SpanFormatter.ActionHandler {

    private ViewSwitcher board;
    private Toolbar toolbar;
    private ProgressDialog mProgressDialog;

//    private static final String TAG = "READ_CARDINFO_ACTION";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_info);

        initViews();

        readCard();
    }

//    @Override
//    public void setIntent(Intent intent) {
//        if (intent.getAction().equals(TAG))
//            readCard();
//        else
//            super.setIntent(intent);
//    }

    private void readCard() {
        if (MainActivity.BtReader != null)
            ReaderManager.readCard(MainActivity.BtReader, StandardECash.class, CardInfoActivity.this);
        if (MainActivity.MiniPay != null)
            ReaderManager.readCard(MainActivity.MiniPay, StandardECash.class, CardInfoActivity.this);
    }

    public void onCopyPageContent(View view) {
        Log.e(Config.APP_ID,view.toString());
        toolbar.copyPageContent(getFrontPage());
    }

    private void initViews() {

        // 初始化进度对话框
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("读卡中...");
        mProgressDialog.setCancelable(false);

        board = (ViewSwitcher) findViewById(R.id.switcher);

        Typeface tf = App.getFontResource(R.string.font_oem1);
        TextView tv = (TextView) findViewById(R.id.txtAppName);
        tv.setTypeface(tf);

        tf = App.getFontResource(R.string.font_oem2);

        tv = getFrontPage();
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        tv.setTypeface(tf);

        tv = getBackPage();
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        tv.setTypeface(tf);

        toolbar = new Toolbar((ViewGroup) findViewById(R.id.toolbar));

    }

    private TextView getFrontPage() {
        return (TextView) ((ViewGroup) board.getCurrentView()).getChildAt(0);
    }

    private TextView getBackPage() {
        return (TextView) ((ViewGroup) board.getNextView()).getChildAt(0);
    }

    private void resetTextArea(TextView textArea, SPEC.PAGE type, int gravity) {

        ((View) textArea.getParent()).scrollTo(0, 0);

        textArea.setTag(type);
        textArea.setGravity(gravity);
    }

    private void loadCardInfoPage(CharSequence info) {
        toolbar.show(null);

        TextView ta = getBackPage();


        resetTextArea(ta, SPEC.PAGE.INFO, Gravity.LEFT);

        ta.setText(info);

        board.showNext();
    }

    private void loadWarningPage() {
        toolbar.show(null);

        CharSequence info = new SpanFormatter(this).toSpanned(App.getStringResource(R.string.info_reader_nocard));

        TextView ta = getBackPage();
        resetTextArea(ta, SPEC.PAGE.INFO, Gravity.CENTER);
        ta.setText(info);

        board.showNext();
    }

    private void showProgressBar() {
        if (mProgressDialog != null)
            mProgressDialog.show();
    }

    private void hideProgressBar() {
        if (mProgressDialog != null)
            mProgressDialog.hide();
    }

    @Override
    public void onReadEvent(SPEC.EVENT event, Object... objs) {
        if (event == SPEC.EVENT.IDLE) {
            showProgressBar();
        } else if (event == SPEC.EVENT.READING) {
            showProgressBar();
        } else if (event == SPEC.EVENT.FINISHED) {
            hideProgressBar();
            if (objs != null && objs.length > 0) {
                Card card = (Card) objs[0];
                CharSequence info = new SpanFormatter(null).toSpanned(card.toHtml());
                if (info.toString().isEmpty())
                    loadWarningPage();
                else
                    loadCardInfoPage(info);
            }
        } else {
            hideProgressBar();
            loadWarningPage();
        }
    }

    @Override
    public void handleAction(CharSequence name) {
        readCard();
    }
}
