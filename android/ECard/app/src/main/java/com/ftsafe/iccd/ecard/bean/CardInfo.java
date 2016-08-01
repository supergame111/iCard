package com.ftsafe.iccd.ecard.bean;

import java.util.Arrays;
import java.util.HashMap;

import ftsafe.common.encryption.SHA1;

/**
 * Created by qingyuan on 16/7/26.
 */
public class CardInfo {

    private CardInfo() {
    }

    public static CardInfo getInstance(int capacity) {
        CardInfo ci = new CardInfo();
        ci.map = new HashMap<>(capacity);
        return ci;
    }

    public byte[] getValue(short t) {
        return this.map.get(t);
    }

    public byte[] getValue(byte[] t) {
        if (t.length == 2) {
            Short ts = (short) (((t[0] & 0xFF) << 8) | (t[1] & 0xFF));
            return this.map.get(ts);
        }
        return null;
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

    public void setValue(byte[] t, byte[] v) {
        if (this.map != null && t.length == 2) {
            Short ts = (short) (((t[0] & 0xFF) << 8) | (t[1] & 0xFF));
            this.map.put(ts, v);
        }
    }

    public void setValue(short t, Byte v) {
        if (this.map != null) {
            this.map.put(t, new byte[]{v});
        }
    }


    public void setValue(byte t, byte[] v) {
        if (this.map != null) {
            Short ts = (short) (((t & 0xFF) << 8) | (0x00 & 0xFF));
            this.map.put(ts, v);
        }
    }

    public void setValue(byte t, Byte v) {
        if (this.map != null) {
            Short ts = (short) (((t & 0xFF) << 8) | (0x00 & 0xFF));
            this.map.put(ts, new byte[]{v});
        }
    }

    private HashMap<Short, byte[]> map;

    public static final Short AppCrypt = (short) 0x9F26;
    public static final Short AppCurcyCode = (short) 0x9F42;
    public static final Short AppCurcyExp = (short) 0x9F44;
    public static final Short AppDiscretData = (short) 0x9F05;
    public static final Short AppEffectDate = (short) 0x5F25;
    public static final Short AppExpireDate = (short) 0x5F24;
    public static final Short AFL = (short) 0x9400;
    public static final Short AID = (short) 0x4F00;
    public static final Short AIP = (short) 0x8200;
    public static final Short AppLabel = (short) 0x5000;
    public static final Short AppPreferName = (short) 0x9F12;
    public static final Short PAN = (short) 0x5A00;
    public static final Short PANSeqNum = (short) 0x5F34;
    public static final Short AppPriority = (short) 0x8700;
    public static final Short AppReferCurcy = (short) 0x9F3B;
    public static final Short AppReferCurcyExp = (short) 0x9F43;
    public static final Short ATC = (short) 0x9F36;
    public static final Short AUC = (short) 0x9F07;
    public static final Short AppVerNum = (short) 0x9F08;
    public static final Short CDOL1 = (short) 0x8C00;
    public static final Short CDOL2 = (short) 0x8D00;
    public static final Short CardholderName = (short) 0x5F20;
    public static final Short CardholderNameExt = (short) 0x9F0B;
    public static final Short CVMList = (short) 0x8E00;
    public static final Short CAPKI = (short) 0x8F00;
    public static final Short CryptInfoData = (short) 0x9F27;
    public static final Short DataAuthenCode = (short) 0x9F45;
    public static final Short DFName = (short) 0x8400;
    public static final Short DDFName = (short) 0x9D00;
    public static final Short DDOL = (short) 0x9F49;
    public static final Short ICCDynNum = (short) 0x9F4C;
    public static final Short ICCPIN_EPKCert = (short) 0x9F2D;
    public static final Short ICCPIN_EPKExp = (short) 0x9F2E;
    public static final Short ICCPIN_EPKRem = (short) 0x9F2F;
    public static final Short ICCPKCert = (short) 0x9F46;
    public static final Short ICCPKExp = (short) 0x9F47;
    public static final Short ICCPKRem = (short) 0x9F48;
    public static final Short IACDenial = (short) 0x9F0E;
    public static final Short IACOnline = (short) 0x9F0F;
    public static final Short IACDefault = (short) 0x9F0D;
    public static final Short IssuAppData = (short) 0x9F10;
    public static final Short IssuCodeTableIndex = (short) 0x9F11;
    public static final Short IssuCountryCode = (short) 0x5F28;
    public static final Short IPKCert = (short) 0x9000;
    public static final Short IPKExp = (short) 0x9F32;
    public static final Short IPKRem = (short) 0x9200;
    public static final Short LangPrefer = (short) 0x5F2D;
    public static final Short LOATCReg = (short) 0x9F13;
    public static final Short LCOL = (short) 0x9F14;
    public static final Short PINTryCount = (short) 0x9F17;
    public static final Short PDOL = (short) 0x9F38;
    public static final Short ServiceCode = (short) 0x5F30;
    public static final Short SignDynAppData = (short) 0x9F4B;
    public static final Short SignStatAppData = (short) 0x9300;
    public static final Short SDATagList = (short) 0x9F4A;
    public static final Short Track1Discret = (short) 0x9F1F;
    public static final Short Track2Discret = (short) 0x9F20;
    public static final Short Track2Equivalent = (short) 0x5700;
    public static final Short TDOL = (short) 0x9700;
    public static final Short UCOL = (short) 0x9F23;
    public static final Short IssuerURL = (short) 0x5F50;
    public static final Short VLPAvailableFund = (short) 0x9F79;
    public static final Short VLPIssuAuthorCode = (short) 0x9F74;
    public static final Short IssuerURL2 = (short) 0x9F5A;
    public static final Short LogEntry = (short) 0x9F4D;
    public static final Short CardholderID = (short) 0x9F61;
    public static final Short CardholderIDType = (short) 0x9F62;
    public static final Short RegionalCode = (short) 0x9F55;
    public static final Short IssuCountryCodePBOC = (short) 0x9F57;
    public static final Short VLPFundLimit = (short) 0x9F77;
    public static final Short VLPFundTransLimit = (short) 0x9F78;
    public static final Short AppCurrencyCode = (short) 0x9F51;
    public static final Short CardAddProcess = (short) 0x9F68;
    public static final Short CardCVMLimit = (short) 0x9F6B;
    public static final Short ECResetThreshod = (short) 0x9F6D;
    public static final Short CAPPFlag = (short) 0xDF61;
    public static final Short CAPPLimit = (short) 0xDF62;
    public static final Short CAPPDeductedFund = (short) 0xDF63;
    public static final Short CAPPDeriveIV = (short) 0xDF0C;
    public static final Short CardAuthRelData = (short) 0x9F69;
    public static final Short MC_PCVC3_Track2 = (short) 0x9F65;
    public static final Short MC_PUNATC_Track2 = (short) 0x9F66;
    public static final Short MC_Track2_Data = (short) 0x9F6B;
    public static final Short MC_MagStripe_AVN = (short) 0x9F6C;
    public static final Short MC_CVC3_Track1 = (short) 0x9F60;
    public static final Short MC_CVC3_Track2 = (short) 0x9F61;
    public static final Short MC_PCVC3_Track1 = (short) 0x9F62;
    public static final Short MC_PUNATC_Track1 = (short) 0x9F63;
    public static final Short MC_NATC_Track1 = (short) 0x9F64;
    public static final Short MC_NATC_Track2 = (short) 0x9F67;
    public static final Short MC_Track1_Data = (short) 0x5600;
    public static final Short LoadLogEntry = (short) 0xDF4D;
    public static final Short LogFormat = (short) 0x9F4F;
    public static final Short LoadLogFormat = (short) 0xDF4F;
    public static final Short ProductIndentification = (short) 0x9F63;


}
