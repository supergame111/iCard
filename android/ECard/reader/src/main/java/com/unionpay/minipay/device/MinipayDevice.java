package com.unionpay.minipay.device;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.util.Log;

import com.util.MobileReader;

/**
 * Created by axis on 2015/11/2.
 */
public class MinipayDevice {

    public UsbManager usbManager;
    //public UsbDevice usbDevice;
    //public static boolean isOpen = false;
    private MobileReader reader = null;
    //private static MinipayDevice device = null;
    Handler mHandler;


    public MinipayDevice(Context context, UsbManager usbManager) {
        this.usbManager = usbManager;
        if (reader == null)
            reader = new MobileReader();
        reader.initReader(usbManager, null, null);
        //device = this;
    }

//    public static MinipayDevice getInstance()
//    {
//        return device;
//    }

//    public UsbDevice getUsbDevice(String deviceName) {
//        for (UsbDevice device : usbManager.getDeviceList().values()) {
//            if (device.getDeviceName().toString().equals(deviceName)) {
//                usbDevice = device;
//            }
//        }
//        return usbDevice;
//    }

//    public void openDeivce() {
//        if (reader != null)
//            reader.openReader();
//    }

    public int scardConnection(UsbDevice usbDevice) {
        if (usbDevice != null) {
            try {
                return reader.ScardConnect(usbDevice);
            } catch (ReaderException e) {
                e.printStackTrace();
            }
        }
        return -1;
    }

    public byte[] powerOn() {
        return reader.powerup();
    }

//    public byte poweOff() {

//    }
    public void closeDevice() {
        reader.ScardDisconnect();
    }

    public byte[] sendAPDU(byte[] apdu) throws Exception {
        return reader.sendApdu(apdu, apdu.length);
    }
}
