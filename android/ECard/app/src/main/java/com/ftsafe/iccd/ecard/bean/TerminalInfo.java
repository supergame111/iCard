package com.ftsafe.iccd.ecard.bean;

import java.util.HashMap;

/**
 * Created by qingyuan on 16/7/27.
 */
public class TerminalInfo {

    private TerminalInfo() {
    }

    public static TerminalInfo getInstance(int capacity) {
        TerminalInfo ti = new TerminalInfo();
        ti.map = new HashMap<>(capacity);
        return ti;
    }

    public void setValue(byte t, byte[] v) {
        if (this.map != null) {
            Short ts = (short) (((t & 0xFF) << 8) | (0x00 & 0xFF));
            this.map.put(ts, v);
        }
    }

    public byte[] getValue(byte t) {
        Short ts = (short) (((t & 0xFF) << 8) | (0x00 & 0xFF));
        return this.map.get(ts);
    }

    public void setValue(short t, byte[] v) {
        if (this.map != null) {
            this.map.put(t, v);
        }
    }


    public byte[] getValue(short t) {
        return this.map.get(t);
    }

    private HashMap<Short, byte[]> map;
    // 1F 自定义标签
    public static final Short TACDenial = (short) 0x1F01;
    public static final Short TACOnline = (short) 0x1F02;
    public static final Short TACDefault = (short) 0x1F03;
    public static final Short TransType = (short) 0x1F04;
    public static final Short VLPTACDenial = (short) 0x1F05;
    public static final Short VLPTACOnline = (short) 0x1F06;
    public static final Short VLPTACDefault = (short) 0x1F07;
    public static final Short Language = (short) 0x1F08;
    public static final Short bTermDDOL = (short) 0x1F09;
    public static final Short bForceAccept = (short) 0x1F10;
    public static final Short bForceOnline = (short) 0x1F11;
    public static final Short bBatchCapture = (short) 0x1F12;
    public static final Short bTermSupportVLP = (short) 0x1F13;
    public static final Short MaxTargetPercent = (short) 0x1F14;
    public static final Short TargetPercent = (short) 0x1F15;
    public static final Short TermDDOL = (short) 0x1F16;
    public static final Short TermTDOL = (short) 0x1F17;
    public static final Short MerchNameLocate = (short) 0x1F18;
    public static final Short TransLogMaxNum = (short) 0x1F19;
    public static final Short Threshold = (short) 0x1F20;
    public static final Short AmtTrans = (short) 0x1F21;
    public static final Short AmtNet = (short) 0x1F22;
    public static final Short BatchTransNum = (short) 0x1F23;
    public static final Short TransNum = (short) 0x1F24;
    public static final Short TransIndex = (short) 0x1F25;
    public static final Short PayPassCVMLimit = (short) 0x1F26;

    public static final Short TransCurrencyCode = (short) 0x5F2A;
    public static final Short TransCurrencyExp = (short) 0x5F36;
    public static final Short AmtAuthorBin = (short) 0x8100;
    public static final Short AuthorCode = (short) 0x8900;
    public static final Short AuthorRespCode = (short) 0x8A00;
    public static final Short IssuerAuthenData = (short) 0x9100;
    public static final Short TVR = (short) 0x9500;
    public static final Short TCHashValue = (short) 0x9800;
    public static final Short PIN = (short) 0x9900;
    public static final Short TransDate = (short) 0x9A00;
    public static final Short TSI = (short) 0x9B00;
    public static final Short TransTypeValue = (short) 0x9C00;
    public static final Short AcquireID = (short) 0x9F01;
    public static final Short AmtAuthorNum = (short) 0x9F02;
    public static final Short AmtOtherNum = (short) 0x9F03;
    public static final Short AmtOtherBin = (short) 0x9F04;
    public static final Short AID = (short) 0x9F06;
    public static final Short AppVerNum = (short) 0x9F09;
    public static final Short MerchCateCode = (short) 0x9F15;
    public static final Short MerchID = (short) 0x9F16;
    public static final Short TermCountryCode = (short) 0x9F1A;
    public static final Short FloorLimit = (short) 0x9F1B;
    public static final Short TermID = (short) 0x9F1C;
    public static final Short TRMData = (short) 0x9F1D;
    public static final Short IFDSerNum = (short) 0x9F1E;
    public static final Short TransTime = (short) 0x9F21;
    public static final Short TermCapab = (short) 0x9F33;
    public static final Short CVR = (short) 0x9F34;
    public static final Short TermType = (short) 0x9F35;
    public static final Short UnpredictNum = (short) 0x9F37;
    public static final Short POSEntryMode = (short) 0x9F39;
    public static final Short AmtReferCurrency = (short) 0x9F3A;
    public static final Short TransReferCurrencyCode = (short) 0x9F3C;
    public static final Short TransReferCurrencyExp = (short) 0x9F3D;
    public static final Short TermAddCapab = (short) 0x9F40;
    public static final Short TransSeqCount = (short) 0x9F41;
    public static final Short MerchantName = (short) 0x9F4E;
    public static final Short MCHIPTransCategoryCode = (short) 0x9F53;
    public static final Short TermTranAtr = (short) 0x9F66;
    public static final Short VLPIndicator = (short) 0x9F7A;
    public static final Short VLPTransLimit = (short) 0x9F7B;
    public static final Short CAPP_Flag = (short) 0xDF60;
    public static final Short SMAlgSupp = (short) 0xDF69;
}
