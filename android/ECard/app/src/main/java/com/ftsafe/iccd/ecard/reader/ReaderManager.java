package com.ftsafe.iccd.ecard.reader;

import android.os.AsyncTask;
import android.util.Log;

import com.ftsafe.iccd.ecard.Config;
import com.ftsafe.iccd.ecard.SPEC;
import com.ftsafe.iccd.ecard.bean.Card;
import com.ftsafe.iccd.ecard.reader.pboc.StandardPboc;

import ftsafe.common.Util;
import ftsafe.reader.Reader;

/**
 * Created by qingyuan on 2016/7/7.
 */
public class ReaderManager extends AsyncTask<Reader, SPEC.EVENT, Card> {

    public static void readCard(Reader reader, ReaderListener listener) {
        new ReaderManager(listener).execute(reader);
    }

    public static void readCard(Reader reader, Class<?> app, ReaderListener listener) {
        new ReaderManager(listener, app).execute(reader);
    }

    private ReaderListener mRealListener;
    private Class<?> mClass;

    private ReaderManager(ReaderListener listener) {
        mRealListener = listener;
    }

    private ReaderManager(ReaderListener listener, Class<?> cls) {
        mRealListener = listener;
        mClass = cls;
    }

    @Override
    protected Card doInBackground(Reader... params) {
        return readCard(params[0]);
    }

    @Override
    protected void onProgressUpdate(SPEC.EVENT... events) {
        if (mRealListener != null)
            mRealListener.onReadEvent(events[0]);
    }

    @Override
    protected void onPostExecute(Card card) {
        if (mRealListener != null)
            mRealListener.onReadEvent(SPEC.EVENT.FINISHED, card);
    }

    private Card readCard(Reader reader) {
        final Card card = new Card();

        try {

            publishProgress(SPEC.EVENT.IDLE);

            card.setProperty(SPEC.PROP.ID, Util.toHexString(reader.getId()));

            if (reader != null) {
                publishProgress(SPEC.EVENT.READING);
                if (mClass != null)
                    StandardPboc.readCard(reader, mClass, card);
                else
                    StandardPboc.readCard(reader, card);
            }

            publishProgress(SPEC.EVENT.FINISHED);

        } catch (Exception e) {
            Log.e(Config.APP_ID,e.getMessage(),e);
            card.setProperty(SPEC.PROP.EXCEPTION, e);
            publishProgress(SPEC.EVENT.ERROR);
        }

        return card;
    }

}
