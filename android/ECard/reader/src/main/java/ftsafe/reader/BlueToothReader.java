package ftsafe.reader;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.ftreader.bluetooth.BlueToothReaderDevice;
import com.ftsafe.exception.FtBlueReadException;

import ftsafe.common.Util;
import ftsafe.common.Utils;

/**
 * Created by qingyuan on 2016/6/30.
 */
public class BlueToothReader extends Reader {

    private BlueToothReaderDevice mBtReader;

    public BlueToothReader(Context ctx, String add) {
        super(ctx, add);
        if (mBtReader == null) {
            try {
                mBtReader = new BlueToothReaderDevice(add, ctx);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


    }

    @Override
    public int powerOn() throws Exception {
        return mBtReader.PowerOn();
    }

    @Override
    public int powerOff() throws Exception {
        return mBtReader.PowerOff();
    }

    @Override
    public Answer sendAPDU(String apdu) throws Exception {
        if (apdu.isEmpty())
            return null;
        byte[] tmp = Util.toBytes(apdu);
        return sendAPDU(tmp);
    }

    @Override
    public Answer sendAPDU(byte[] apdu) throws Exception {
        try {
            byte[] resp = new byte[MAX_PACKET_LENGTH];
            int[] length = new int[2];
            int ret = 0;

            ret = mBtReader.transApdu(apdu.length, apdu, length, resp);
            if (ret != 0)
                return null;

            int len = length[0];
            byte sw1 = resp[len - 2];
            byte sw2 = resp[len - 1];

            //检查SW是否返回：61XX
            if (sw1 == (byte) 0x61) {
                CMD61[4] = sw2;
                System.arraycopy(apdu, 0, CMD61, 0, 5);
                sendAPDU(apdu);
            }
            //检查SW是否返回：6CXX
            if (sw1 == (byte) 0x6C) {
                System.arraycopy(CMD6C, 0, apdu, 0, 4);
                CMD6C[4] = sw2;
                sendAPDU(CMD6C);
            }
//            String result = Utils.bytes2HexStr(resp, len);
            String result = Util.toHexString(resp, 0, len);
            Answer answer = new Answer();
            len = result.length();
            if (len == 4) {
                answer.value = "";
                answer.sw = result;
            }
            else if (len > 4) {
                answer.value = result.substring(0, len - 4);
                answer.sw = result.substring(len - 4);
            }
            else {
                return null;
            }
            return answer;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }

    public void registerCardStatusMonitoring(Handler mHandler) {
        try {
            mBtReader.registerCardStatusMonitoring(mHandler);
        } catch (FtBlueReadException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    public void readerClose() {
        mBtReader.readerClose();
    }
}
