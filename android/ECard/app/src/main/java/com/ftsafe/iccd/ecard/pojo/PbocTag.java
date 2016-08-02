package com.ftsafe.iccd.ecard.pojo;

/**
 * Created by qingyuan on 2016/7/20.
 */
public class PbocTag {
    public static final short APP_CRYPTOGRAM = (short) 0x9F26;
    public static final short CRYPTOGRAM_INFO = (short) 0x9F27;
    public static final short APP_CURRENCY_CODE = (short) 0x9F42;
    public static final short APP_CURRENCY_CODE2 = (short) 0x9F51;
    public static final short APP_CURRENCY_EXPONENT = (short) 0x9F44;
    public static final short APP_DEFAULT_ACTION = (short) 0x9F52;
    public static final short APP_CUSTOM_DATA = (short) 0x9F05;
    public static final short APP_START_DATE = (short) 0x5F25;
    public static final short APP_EXPIRATION_DATE = (short) 0x5F24;
    public static final byte APP_FILE_LOCATOR = (byte) 0x94;
    public static final byte APP_IDENTIFIER = (byte) 0x4F;
    public static final byte APP_INTERCHANGE_PROFILE = (byte) 0x82;
    public static final byte APP_TAG = (byte) 0x50;
    public static final short APP_PRIORITY_NAME = (short) 0x9F12;
    public static final byte PAN = (byte) 0x5A;
    public static final short PAN_SERIAL = (short) 0x5F34;
    public static final short AUC = (short) 0x9F07;
    public static final byte APP_PRIORITY_INDICATOR = (byte) 0x87;
    public static final short CARD_APP_VER = (short) 0x9F08;
    public static final byte RESPONSE_TEMPLATE_80 = (byte) 0x80;
    public static final byte RESPONSE_TEMPLATE_77 = (byte) 0x77;
    public static final short ISSUER_COUNTRY_CODE = (short) 0x5F28;
    public static final byte CVM_LIST = (byte) 0x8E;
    public static final short LCOL = (short) 0x9F14;
    public static final short UCOL = (short) 0x9F23;
    public static final byte TDOL = (byte) 0x97;
    public static final short VLP_ISSU_AUTHOR_CODE = (short) 0x9F74;
    public static final short ATC = (short) 0x9F36;
    public static final short LOATC = (short) 0x9F13;
    public static final short IAC_DENIAL = (short) 0x9F0E;
    public static final short IAC_ONLINE = (short) 0x9F0F;
    public static final short IAC_DEFAULT = (short) 0x9F0D;
    public static final short ISSUER_APP_DATA = (short) 0x9F10;
    public static final short AMT_AUTHOR_NUM = (short) 0x9F02;
    public static final short AMT_OTHER_NUM = (short) 0x9F03;
    public static final short UNPREDICT_NUM = (short) 0x9F37;
    public static final byte CDOL1=(byte)0x8C;
    public static final byte CDOL2=(byte)0x8D;
    public static final short SIGN_DYN_APP_DATA = (short)0x9F4B;
    public static final byte CAPKI = (byte)0x8F;
    public static final byte IPK_CERT = (byte)0x90;
    public static final short ICC_PK_CERT = (short)0x9F46;
    public static final short DDOL = (short)0x9F49;
    public static final byte TVR = (byte)0x95;
    public static final short TERM_COUNTRY_CODE = (short)0x9F1A;
    public static final short TRANS_CURRENCY_CODE = (short)0x5F2A;
    public static final byte TRANS_DATE = (byte)0x9A;
    public static final byte TRANS_TYPE = (byte)0x9C;
}
