package com.ftsafe.iccd.ecard.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.ftsafe.iccd.ecard.App;
import com.ftsafe.iccd.ecard.R;
import com.ftsafe.iccd.ecard.SPEC;
import com.ftsafe.iccd.ecard.nfc.NfcManager;
import com.ftsafe.iccd.ecard.ui.pages.AboutPage;
import com.ftsafe.iccd.ecard.ui.pages.MainPage;
import com.ftsafe.iccd.ecard.ui.pages.NfcPage;
import com.ftsafe.iccd.ecard.ui.pages.Toolbar;

public class NfcActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc);

        initViews();

        nfc = new NfcManager(this);

        onNewIntent(getIntent());
    }

    @Override
    public void onBackPressed() {
        if (isCurrentPage(SPEC.PAGE.ABOUT))
            loadDefaultPage();
        else if (safeExit)
            super.onBackPressed();
    }

    @Override
    public void setIntent(Intent intent) {
        if (NfcPage.isSendByMe(intent))
            loadNfcPage(intent);
        else if (AboutPage.isSendByMe(intent))
            loadAboutPage();
        else
            super.setIntent(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        nfc.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        nfc.onResume();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            if (nfc.updateStatus())
                loadDefaultPage();

            // 有些ROM将关闭系统状态下拉面板的BACK事件发给最顶层窗口
            // 这里加入一个延迟避免意外退出
            board.postDelayed(new Runnable() {
                public void run() {
                    safeExit = true;
                }
            }, 800);
        } else {
            safeExit = false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        loadDefaultPage();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (!nfc.readCard(intent, new NfcPage(this)))
            loadDefaultPage();
    }

    public void onSwitch2DefaultPage(View view) {
        if (!isCurrentPage(SPEC.PAGE.DEFAULT))
            loadDefaultPage();
    }

    public void onSwitch2AboutPage(View view) {
        if (!isCurrentPage(SPEC.PAGE.ABOUT))
            loadAboutPage();
    }

//    public void onCopyPageContent(View view) {
//        toolbar.copyPageContent(getFrontPage());
//    }
//
//    public void onSharePageContent(View view) {
//        toolbar.sharePageContent(getFrontPage());
//    }

    private void loadDefaultPage() {
        toolbar.show(null);

        TextView ta = getBackPage();

        resetTextArea(ta, SPEC.PAGE.DEFAULT, Gravity.CENTER);
        ta.setText(MainPage.getContent(this));

        board.showNext();
    }

    private void loadAboutPage() {
        toolbar.show(R.id.btnBack);

        TextView ta = getBackPage();

        resetTextArea(ta, SPEC.PAGE.ABOUT, Gravity.LEFT);
        ta.setText(AboutPage.getContent(this));

        board.showNext();
    }

    private void loadNfcPage(Intent intent) {
        final CharSequence info = NfcPage.getContent(this, intent);

        TextView ta = getBackPage();

        if (NfcPage.isNormalInfo(intent)) {
            toolbar.show(R.id.btnCopy, R.id.btnShare, R.id.btnReset);
            resetTextArea(ta, SPEC.PAGE.INFO, Gravity.LEFT);
        } else {
            toolbar.show(R.id.btnBack);
            resetTextArea(ta, SPEC.PAGE.INFO, Gravity.CENTER);
        }

        ta.setText(info);

        board.showNext();
    }

    private boolean isCurrentPage(SPEC.PAGE which) {
        Object obj = getFrontPage().getTag();

        if (obj == null)
            return which.equals(SPEC.PAGE.DEFAULT);

        return which.equals(obj);
    }

    private void resetTextArea(TextView textArea, SPEC.PAGE type, int gravity) {

        ((View) textArea.getParent()).scrollTo(0, 0);

        textArea.setTag(type);
        textArea.setGravity(gravity);
    }

    private TextView getFrontPage() {
        return (TextView) ((ViewGroup) board.getCurrentView()).getChildAt(0);
    }

    private TextView getBackPage() {
        return (TextView) ((ViewGroup) board.getNextView()).getChildAt(0);
    }

    private void initViews() {
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

    private ViewSwitcher board;
    private Toolbar toolbar;
    private NfcManager nfc;
    private boolean safeExit;
}
