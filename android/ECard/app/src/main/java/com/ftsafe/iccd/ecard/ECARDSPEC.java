package com.ftsafe.iccd.ecard;

/**
 * Created by ft on 2016/8/4.
 */
public class ECARDSPEC {
    public final static short LOAD = (byte) 0x0000;
    public final static short PAY = (byte) 0x0001;

    //transaction type
    public final static byte TERM_TRANS_CASH = 0;
    public final static byte TERM_TRANS_GOODS = 1;
    public final static byte TERM_TRANS_SERVICE = 2;
    public final static byte TERM_TRANS_CASHBACK = 3;

    // EMV模式：PBOC,VISA,MASTERCARD
    public enum EmvMode {
        PBOC, VISA, MASTERCARD;
    }
}
