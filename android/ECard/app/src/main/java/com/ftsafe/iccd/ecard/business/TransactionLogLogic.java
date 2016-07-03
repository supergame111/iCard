package com.ftsafe.iccd.ecard.business;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.ftsafe.iccd.ecard.Config;
import com.ftsafe.iccd.ecard.pojo.AID;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

import ftsafe.pboc.PBOC;
import ftsafe.reader.Reader;
import ftsafe.common.BaseFunction;
import ftsafe.common.BasePboc;

/**
 * Created by admin on 15/8/13.
 */
public class TransactionLogLogic {

    public interface TlCallback {
        void onResult(List<HashMap<String, String>> result);

        void onMessage(String msg);
    }

    private ProgressDialog progDailog;

    public TransactionLogLogic(final Context ctx, final Reader reader, final TlCallback callback) {

        new AsyncTask<Reader, Void, List<HashMap<String, String>>>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                    progDailog = new ProgressDialog(ctx);
                    progDailog.setMessage("读取中...");
                    progDailog.setIndeterminate(false);
                    progDailog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    progDailog.setCancelable(true);
                    progDailog.show();
            }

            @Override
            protected List<HashMap<String, String>> doInBackground(Reader... params) {

                if (reader == null) return null;

                try {
                    //power on
                    reader.powerOn();

                    List<HashMap<String, String>> list = new ArrayList<>();
                    PBOC pboc = new PBOC(reader);
                    BasePboc bp = new BasePboc();
                    BaseFunction bf = new BaseFunction();
                    //PSE or PPSE
                    String result = pboc.pse();

                    if (result == null) {
                        return null;
                    }

                    if (result.length() == 4) {
                        result = pboc.ppse();
                        if (result.length() == 4) {
                            Log.d(Config.APP_ID, "PSE/PPSE" + result);
                        }
                    }
                    //AID
                    result = pboc.selectAID(AID.PBOC_CREDIT.getAid());
                    if (result.length() == 4)//回复异常的SW
                    {
                        for (AID aid : EnumSet.range(AID.PBOC_DEBIT, AID.ZJJT)) {
                            result = pboc.selectAID(aid.getAid());
                            if (result.length() != 4) {
                                Log.e(Config.APP_ID,"AID "+aid.getAid());
                                break;
                            }
                        }
                    }
                    //寻找交易日志入口 Tag = 9F4D
                    StringBuffer dest = new StringBuffer(4);
                    int nRet = bp.ftTLVGetStrVal(result, "9F4D", dest);
                    if (nRet != bp.FTOK)
                        return null;
                    String SFI = dest.substring(0, 2);
                    String recordQty = dest.substring(2);
                    //read Log
                    String[] log = pboc.readLogRecord(SFI, recordQty);
                    if (log.length < 1) {
                        return null;
                    }
                    //read log format
                    result = pboc.readLogFormat();
                    //整理log数据
                    StringBuffer[] tagListBuf = new StringBuffer[40];
                    StringBuffer[] valueLenBuf = new StringBuffer[40];
                    int tagQty = bp.getTagAndValueLen(result.substring(6), tagListBuf, valueLenBuf);
                    int k = 0;
                    while (k < 10) {
                        String logone = log[k];
                        if (logone == null)
                            break;
                        HashMap<String, String> map = new HashMap<>();
                        int pos = 0;
                        for (int i = 0; i < tagQty; i++) {
                            String tagone = tagListBuf[i].toString();
                            String valueLen = valueLenBuf[i].toString();
                            int end = pos + bf.ftHexToDec(valueLen) * 2;
                            String value = logone.substring(pos, end);
                            map.put(tagone, value);
                            pos = end;
                        }
                        list.add(map);
                        k++;
                    }

                    return list;
                } catch (Exception e) {
                    Log.e(Config.APP_ID, e.getMessage(), e);
                }
                return null;

            }

            @Override
            protected void onPostExecute(List<HashMap<String, String>> hashMaps) {
                try {
                    if (progDailog.isShowing())
                        progDailog.dismiss();
                    reader.powerOff();

                    if (callback != null) {
                        if (hashMaps != null) {
                            callback.onResult(hashMaps);
                        } else {
                            callback.onMessage("无法识别日志格式");
                        }
                    }
                } catch (Exception e) {
                    callback.onMessage(e.getMessage());
                }
            }
        }.execute();

    }

}
