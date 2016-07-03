package ftsafe.reader;

import android.content.Context;
import android.hardware.usb.UsbManager;

/**
 * Created by qingyuan on 2016/6/30.
 */
public abstract class Reader {

    static final String TAG = "com.ftsafe.sdk";

    final static int MAX_PACKET_LENGTH = 2048;
    static byte[] CMD61 = {0x00, (byte) 0xC0, 0x00, 0x00, 0x00};
    static byte[] CMD6C = new byte[5];

    // BlueTooth
    Reader(Context context, String address ){
    }
    // USB
    Reader(Context context,UsbManager usbManager){
    }

    public abstract int powerOn() throws Exception;

    public abstract int powerOff() throws Exception;

    public abstract Answer sendAPDU(String apdu) throws Exception;

    public abstract Answer sendAPDU(byte[] apdu) throws Exception;
}
