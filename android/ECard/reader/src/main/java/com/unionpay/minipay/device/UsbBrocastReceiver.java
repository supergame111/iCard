package com.unionpay.minipay.device;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;

/**
 * Created by qingyuan on 2016/6/30.
 */
public class UsbBrocastReceiver extends BroadcastReceiver {

    private static final String TAG = "com.ftsafe";

    private static Handler mHandle = null;

    public static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    public static final int USB_STATUS        = 0xE100;
    public static final int USB_PERMISSION       = 0;
    public static final int USB_ATTACHED       = 3;
    public static final int USB_DETACHED        = 4;

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (ACTION_USB_PERMISSION.equals(action)){
            //Toast.makeText(context,"Device Granted Permission", Toast.LENGTH_LONG).show();
            if (device != null){
                if (mHandle != null) {
                    //send message to the BlueTooth-Activity that bletooth device is connected!
                    mHandle.obtainMessage(USB_STATUS,
                            USB_PERMISSION, -1).sendToTarget();
                }
            }

        }
        // USB 设备插入
        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
            //Log.d(TAG,"OTG IN");
            //Toast.makeText(context,"OTG IN", Toast.LENGTH_LONG).show();
            if (mHandle != null) {
                //send message to the BlueTooth-Activity that bletooth device is connected!
                mHandle.obtainMessage(USB_STATUS,
                        USB_ATTACHED, -1).sendToTarget();
            }
        }
        // USB 设备拔出
        if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
            //Log.d(TAG,"OTG OUT");
            //Toast.makeText(context, "OTG OUT", Toast.LENGTH_LONG).show();
            if (mHandle != null) {
                //send message to the BlueTooth-Activity that bletooth device is connected!
                mHandle.obtainMessage(USB_STATUS,
                        USB_DETACHED, -1).sendToTarget();
            }
        }
    }

    public static int registerCardStatusMonitoring(Handler Handler) {

        mHandle = (Handler) Handler;
        return 0;
    }
}
