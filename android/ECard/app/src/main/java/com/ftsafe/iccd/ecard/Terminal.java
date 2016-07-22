package com.ftsafe.iccd.ecard;

import java.io.UnsupportedEncodingException;

import ftsafe.common.Util;

/**
 * Created by qingyuan on 2016/7/20.
 */
public class Terminal {

    // 终端参数初始化，在EMV_InitialApp之前调用
    // 参数顺序：9F7A,9F66,9C,9F02,9F03,DF60,DF69
    public Terminal(byte vlpIndicator, byte[] termTransAtr, byte transType, byte[] amtAuthNum, byte[] amtOtherNum, byte cappFlag, byte smAlgSupp) {
        try {
            // 不可预知数
            setUnpredictNum(Util.getRandom(4));
            // 交易时间
            setTransTime(Util.getSysTime(0));
            // 交易日期
            setTransDate(Util.getSysDate(3));
            // 终端名称
            setMerchantName(Config.TERMINAL_NAME_CN.getBytes(Config.CHARSET));
            setVLPIndicator(vlpIndicator);
            setTermTransAtr(termTransAtr);
            setTransType(transType);
            setAmtAuthNum(amtAuthNum);
            setAmtOtherNum(amtOtherNum);
            setTermCappFlag(cappFlag);
            setSMAlgSupp(smAlgSupp);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public byte[] getAcquireID() {
        return acquireID;
    }

    public void setAcquireID(byte[] acquireID) {
        this.acquireID = acquireID;
    }

    //0
    //tag'9F01' Acquirer ID len:6
    private byte[] acquireID;

    public byte[] getTermCapab() {
        return termCapab;
    }

    public void setTermCapab(byte[] termCapab) {
        this.termCapab = termCapab;
    }

    //tag'9F33' terminal capability len:3
    private byte[] termCapab;

    public byte[] getTermAddCapab() {
        return termAddCapab;
    }

    public void setTermAddCapab(byte[] termAddCapab) {
        this.termAddCapab = termAddCapab;
    }

    //tag'9F40' terminal additional capability len:5
    private byte[] termAddCapab;
    //tag'9F1E' IFD(POS device) serial no. asc8
    private byte[] iFD_SN;
    //tag'9F1C' Terminal ID len:8
    private byte[] termID;
    //tag'9F15' offset 30 Merchant Category Code len:2
    private byte[] merchCateCode;
    //tag'9F16' Merchant ID len:15
    private byte[] merchID;

    public byte[] getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(byte[] countryCode) {
        this.countryCode = countryCode;
    }

    //tag'9F1A' Terminal country code BCD len:2
    private byte[] countryCode;
    //tag'9F1D' Terminal Risk Management Data len:8
    private byte[] tRMData;

    public byte getTermType() {
        return termType;
    }

    public void setTermType(byte termType) {
        this.termType = termType;
    }

    //10
    //tag'9F35' offset 58 Terminal type
    private byte termType;

    public byte[] getAppVer() {
        return appVer;
    }

    public void setAppVer(byte[] appVer) {
        this.appVer = appVer;
    }

    //tag'9F09' Application Version Number in terminal//VIS1.3.1--0x0083;VIS1.3.2--0x0084;VIS140--0x008c; len:2
    private byte[] appVer;

    public byte[] getTransCurcyCode() {
        return transCurcyCode;
    }

    public void setTransCurcyCode(byte[] transCurcyCode) {
        this.transCurcyCode = transCurcyCode;
    }

    //tag'5F2A' len:2
    private byte[] transCurcyCode;
    //tag'5F36'
    private byte transCurcyExp;
    //tag'9F3C' len:2
    private byte[] transReferCurcyCode;
    //tag'9F3D' offset 66
    private byte transReferCurcyExp;

    public byte[] getTACDenial() {
        return tACDenial;
    }

    public void setTACDenial(byte[] tACDenial) {
        this.tACDenial = tACDenial;
    }

    //Terminal action code-denial len:5
    private byte[] tACDenial;

    public byte[] getTACOnline() {
        return tACOnline;
    }

    public void setTACOnline(byte[] tACOnline) {
        this.tACOnline = tACOnline;
    }

    public void orTACOnline(int offset, byte b) {
        if (tACOnline != null && tACOnline.length > offset)
            tACOnline[offset] |= b;
    }

    //Terminal action code-online len:5
    private byte[] tACOnline;

    public byte[] getTACDefault() {
        return tACDefault;
    }

    public void setTACDefault(byte[] tACDefault) {
        this.tACDefault = tACDefault;
    }

    public void orTACDefault(int offset, byte b) {
        if (tACDefault != null && tACDefault.length > offset) {
            tACDefault[offset] |= b;
        }
    }

    //Terminal action code-default len:5
    private byte[] tACDefault;

    public byte getTransType() {
        return transType;
    }

    public void setTransType(byte transType) {
        this.transType = transType;
    }

    //for distinguish different trans types such as goods and service.
    private byte transType;
    //20
    //tag'9C', offset 83  transtype value(first two digits of processing code) as stated in EMV2000. goods and service are both 0x00.
    private byte transTypeValue;
    //tag'9F7B',n12, new added in VIS1.4.0. len:6
    private byte[] vLPTransLimit;

    public byte[] getVLPTACDenial() {
        return vLPTACDenial;
    }

    public void setVLPTACDenial(byte[] vLPTACDenial) {
        this.vLPTACDenial = vLPTACDenial;
    }

    //Terminal action code-denial for VLP len:5
    private byte[] vLPTACDenial;
    //Terminal action code-online for VLP len:5
    private byte[] vLPTACOnline;

    public byte[] getVLPTACDefault() {
        return vLPTACDefault;
    }

    public void setVLPTACDefault(byte[] vLPTACDefault) {
        this.vLPTACDefault = vLPTACDefault;
    }

    //Terminal action code-default for VLP len:5
    private byte[] vLPTACDefault;

    // offset 105 CHINESE or ENGLISH for display and print language.
    private byte language;
    //0-no default DDOl in terminal;1- has default DDOL in terminal
    private byte bTermDDOL;
    //this two set according test script V2CM080.00,V2CM081.00
    private byte bForceAccept;

    public byte getbForceOnline() {
        return bForceOnline;
    }

    public void setbForceOnline(byte bForceOnline) {
        this.bForceOnline = bForceOnline;
    }

    //also see emvterm.pdf p32
    private byte bForceOnline = 0;
    //private set for send different msg to host-AuthRQ and FinaRQ.
    private byte bBatchCapture = 0;
    //30
    // offset 110  0-not support;1-support. configurable terminal parameter to indicate if VLP is supported.
    private byte bTermSupportVLP = 0;
    private byte maxTargetPercent = 0;
    //Preset by terminal. range 0-99, and MTP>=TP
    private byte targetPercent = 0;
    //term hold of default DDOL,must be initialised in init. len:128
    private byte[] termDDOL;
    //terminal stored default TDOL. len:128
    private byte[] termTDOL;
    //EMV2000 new added len:128
    private byte[] merchNameLocate;
    //max transLogs stored for check floor limit(default 20) len:20
    private byte[] transLogMaxNum;
    //40
    //offset 502 threshold for random selection.len:4
    private byte[] threshold;

    public byte[] getFloorLimit() {
        return floorLimit;
    }

    public void setFloorLimit(byte[] floorLimit) {
        this.floorLimit = floorLimit;
    }

    //tag'9F1B' terminal floor limit len:4
    private byte[] floorLimit;
    //used in online financial or batch capture msg.len:4
    private byte[] amtTrans;
    //total accumulative amount for reconciliation. len:4
    private byte[] amtNet;
    //number of trans stored in terminal.used for reconciliation len:2
    private byte[] batchTransNum;
    //offset 520 numbers of floorlimit translog for floorlimit check. len:2
    private byte[] transNum;
    //added for new floorlimit translog insert position. len:2
    private byte[] transIndex;
    //increment by 1 for each trans. BCD numeric. len:4
    private byte[] transSeqCount;

    public byte[] getAmtAuthBin() {
        return amtAuthBin;
    }

    public void setAmtAuthBin(byte[] amtAuthBin) {
        this.amtAuthBin = amtAuthBin;
    }

    //tag'81' Authorised amount of binary len:4
    private byte[] amtAuthBin;

    public byte[] getAmtAuthNum() {
        return amtAuthNum;
    }

    public void setAmtAuthNum(byte[] amtAuthNum) {
        this.amtAuthNum = amtAuthNum;
    }

    //tag'9F02' Authorised amount of BCD numeric len:6
    private byte[] amtAuthNum;

    public byte[] getAmtOtherBin() {
        return amtOtherBin;
    }

    public void setAmtOtherBin(byte[] amtOtherBin) {
        this.amtOtherBin = amtOtherBin;
    }

    //50
    //tag'9F04' offset 538 Other(cashback) amount of binary len:4
    private byte[] amtOtherBin;

    public byte[] getAmtOtherNum() {
        return amtOtherNum;
    }

    public void setAmtOtherNum(byte[] amtOtherNum) {
        this.amtOtherNum = amtOtherNum;
    }

    //tag'9F03' Other(cashback) amount of BCD numeric len:6
    private byte[] amtOtherNum;
    //tag'9F3A' Authorised amount in the reference currency len:4
    private byte[] amtReferCurcy;
    //tag'9F06' Application Identifier for selected application,5-16 len:16
    private byte[] aID;
    //tag'89'   offset 569 ret from issuer.move to TermInfo from global variable in P70. len:6
    private byte[] authorCode;

    public byte[] getAuthRespCode() {
        return authRespCode;
    }

    public void setAuthRespCode(byte[] authRespCode) {
        this.authRespCode = authRespCode;
    }

    //tag'8A'   Authorised respose code received from host. len:2
    private byte[] authRespCode;

    public byte[] getCVMResult() {
        return cVMResult;
    }

    public void setCVMResult(byte[] cVMResult) {
        this.cVMResult = cVMResult;
    }

    public void updateCVMResult(int offset, byte b) {
        cVMResult[offset] = b;
    }

    //tag'9F34' cardholder verification methods perform result len:3
    private byte[] cVMResult;
    //tag'9F39' POS entry mode,BCD
    private byte pOSEntryMode;
    //60
    //tag'99'	offset 582
    private byte[] pIN = new byte[12];

    public byte[] getTVR() {
        return tVR;
    }

    public void setTVR(byte[] tVR) {
        this.tVR = tVR;
    }

    public void orTVR(int offset, byte b) {
        if (tVR != null && tVR.length > offset)
            this.tVR[offset] |= b;
    }

    public void andTVR(int offset, byte b) {
        if (tVR != null && tVR.length > offset)
            this.tVR[offset] &= b;
    }

    //tag'95'   Terminal Verification Results
    private byte[] tVR;

    public byte[] getTSI() {
        return tSI;
    }

    public void setTSI(byte[] tSI) {
        this.tSI = tSI;
    }

    public void orTSI(int offset, byte b) {
        if (tSI != null && tSI.length > offset)
            tSI[offset] |= b;
    }

    //tag'9B' Transaction Status Information len:2
    private byte[] tSI;

    public byte getVLPIndicator() {
        return vLPIndicator;
    }

    public void setVLPIndicator(byte vLPIndicator) {
        this.vLPIndicator = vLPIndicator;
    }

    //tag'9F7A' //0-not support; 1-support; 2-VLP only. variable parameter to indicate if this trans is VLP supported.
    private byte vLPIndicator;

    public byte[] getTransDate() {
        return transDate;
    }

    public void setTransDate(byte[] transDate) {
        this.transDate = transDate;
    }

    //tag'9A'   YYMMDD len:3
    private byte[] transDate;

    public byte[] getTransTime() {
        return transTime;
    }

    public void setTransTime(byte[] transTime) {
        this.transTime = transTime;
    }

    //tag'9F21',offset 605 HHMMSS,BCD len:3
    private byte[] transTime;
    //tag'98' len:20
    private byte[] tCHashValue;

    public byte[] getUnpredictNum() {
        return unpredictNum;
    }

    public void setUnpredictNum(byte[] unpredictNum) {
        this.unpredictNum = unpredictNum;
    }

    //tag'9F37' Terminal created for each transaction. len:4
    private byte[] unpredictNum;
    //tag'91'   Issuer Authentication Data. len:16
    private byte[] issuerAuthenData;
    //70
    //tag '9F53' offset: 649 Transaction Category Code, Mastercard M/Chip private data.
    private byte mCHIPTransCateCode;
    //测试时是否执行发卡行脚本
    private byte bSendScript;
    private byte bRandomPINLen;
    //产生的随机PIN长度 len:16
    private byte[] bRandomPINData;
    //如果在开始测试时PIN验证失败,则不执行 更改PIN命令
    private byte bSendScriptOfChangePin;

    public byte[] getTermTransAtr() {
        return termTransAtr;
    }

    public void setTermTransAtr(byte[] termTransAtr) {
        this.termTransAtr = termTransAtr;
    }

    //tag '9F66' offset: 669 len:4
    private byte[] termTransAtr;

    public byte getTermCappFlag() {
        return termCappFlag;
    }

    public void setTermCappFlag(byte termCappFlag) {
        this.termCappFlag = termCappFlag;
    }

    //tag 'DF60' offset: 673
    private byte termCappFlag;

    public byte[] getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(byte[] merchantName) {
        this.merchantName = merchantName;
    }

    //tag '9F4E' offset: 674 len:20
    private byte[] merchantName;

    public byte getSMAlgSupp() {
        return sMAlgSupp;
    }

    public void setSMAlgSupp(byte sMAlgSupp) {
        this.sMAlgSupp = sMAlgSupp;
    }

    //tag 'DF69' offset: 694
    private byte sMAlgSupp;
    //tag '0000' CVM limit value, specific to PayPass offset: 695 len:4
    private byte[] pPCVMLimit;
}
