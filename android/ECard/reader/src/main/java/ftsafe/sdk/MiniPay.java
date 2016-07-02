package ftsafe.sdk;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.unionpay.minipay.device.MinipayDevice;

import ftsafe.utils.Utils;

public class MiniPay extends Reader {
    private MinipayDevice mReader;
    private final int vendorId = 2414;
    public static boolean isOpen = false;

    public MiniPay(Context context, UsbManager usbManager) {
        super(context, usbManager);
        if (mReader == null)
            mReader = new MinipayDevice(context, usbManager);
    }

    @Override
    public int powerOn() throws Exception {
        return 0;
    }

    @Override
    public int powerOff() throws Exception {
        return 0;
    }

    public void openMiniPay(UsbDevice usbDevice) {
        if (isOpen == false) {
            if (usbDevice.getVendorId() == vendorId) {
                mReader.scardConnection(usbDevice);
                isOpen = true;
            }
        } else
            return;
    }

    public void cardConnection() {
    }

    public void closeMiniPay() {
        isOpen = false;
        mReader.closeDevice();
    }

    //	public int checkRead(){
//		int nRet = mReader.Check_read();
//		return nRet;
//	}
    public String readDeviceInfo() {
        return null;
    }

    public String readDeviceStatus() {
        return null;
    }

    /**
     * @param apdu
     * @return
     * @throws Exception
     */
    @Override
    public Answer sendAPDU(String apdu) throws Exception {
        if (apdu.isEmpty())
            return null;
        byte[] tmp = Utils.hexStringToBytes(apdu);
        return sendAPDU(tmp);
    }

    /**
     * 发送APDU指令，可自动检测SW＝61XX，和6CXX</br>
     * SW和返回数据分开
     *
     * @param apdu
     * @return Answer
     */
    public Answer sendAPDU(byte[] apdu) {
        byte[] resp = new byte[0];
        try {
            resp = mReader.sendAPDU(apdu);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        int len = resp.length;
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
        String result = Utils.bytes2HexStr(resp, resp.length);
        Answer answer = new Answer();
        len = result.length();
        if (len > 4)
            answer.value = result.substring(0, len - 4);
        answer.sw = result.substring(len - 4);
        return answer;
    }
}
