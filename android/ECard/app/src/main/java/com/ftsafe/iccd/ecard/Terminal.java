package com.ftsafe.iccd.ecard;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import ftsafe.common.Util;
import ftsafe.reader.tech.Iso7816;

/**
 * Created by qingyuan on 2016/7/20.
 */
public class Terminal {

    private Terminal() {
    }

    private static Terminal mTerminal = null;

    public static Terminal getTermianl() {
        if (mTerminal == null)
            mTerminal = getInstance();
        return mTerminal;
    }

    public static Terminal getInstance() {

        Terminal terminal = new Terminal();
        terminal.map = new HashMap<>(100);
        // 终端类型 9F35
        terminal.setTermType((byte) 0x32);
        // 终端国家代码 9F1A
        terminal.setCountryCode(new byte[]{(byte) 0x01, (byte) 0x56});
        // 电子现金终端支持指示器 9F7A 0:no 1:yes
        terminal.setVLPIndicator((byte) 0x01);
        // 电子现金终端交易限额 9F7B
        terminal.setvLPTransLimit(new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x10, (byte) 0x00, (byte) 0x00});
        // capp 支持 DF60
        terminal.setTermCappFlag((byte) 0x00);
        // 设置国密支持 DF69
        terminal.setSMAlgSupp((byte) 0x00);
        // TVR 95
        terminal.setTVR(new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00});
        // 商户名称 9F4E
        terminal.setMerchantName(new byte[]{});
        // 不可预知数 9F37
        terminal.setUnpredictNum(Util.getRandom(4));
        // 交易日期 9A
        terminal.setTransDate(Util.getSysDate(3));
        // 交易时间 9F21
        terminal.setTransTime(Util.getSysTime(0));

        return terminal;
    }

    // 终端参数初始化，在EMV_InitialApp之前调用
    // 参数顺序：9F7A,9F66,9C,9F02,9F03,DF60,DF69
//    public Terminal(byte[] amtAuthNum) {
//        // 终端类型 9F35
//        setTermType((byte) 0x32);
//        // 初始化MAP
//        this.map = new HashMap<>(100);
//        try {
//
//            if (transMode == SPEC.LOAD) {
//
//                // 不可预知数 9F37
//                setUnpredictNum(Util.getRandom(4));
//                // 交易时间 9F21
//                setTransTime(Util.getSysTime(0));
//                // 交易日期 9A
//                setTransDate(Util.getSysDate(3));
//                // 商户名称 9F4E
//                setMerchantName(Config.TERMINAL_NAME_CN.getBytes(Config.CHARSET));
//                // 电子现金终端支持指示器 0:no 1:yes 9F7A
//                setVLPIndicator((byte) 0x00);
//                // 终端交易属性 9F66
//                setTermTransAtr(new byte[]{(byte) 0x66, (byte) 0x80, (byte) 0, (byte) 0x80});
//                // 交易类型 9C
//                setTransType((byte) 0x00);
//                // 授权金额 9F02
//                setAmtAuthNum(amtAuthNum);
//                // 其他授权金额 9F03
//                setAmtOtherNum(new byte[]{0, 0, 0, 0, 0, 0});
//                // capp 支持 DF60
//                setTermCappFlag((byte) 0x00);
//                // 设置国密支持 DF69
//                setSMAlgSupp((byte) 0x00);
//                // 终端国家代码 9F1A
//                setCountryCode(new byte[]{(byte) 0x01, (byte) 0x56});
//                // 交易货币代码 5F2A
//                setTransCurcyCode(new byte[]{(byte) 0x01, (byte) 0x56});
//                // 终端性能 9F33
//                setTermCapab(new byte[]{(byte) 0xE0, (byte) 0xE8, (byte) 0xE8});
//                // TVR 95
//                setTVR(new byte[]{0, 0, 0, 0, 0});
//
//            } else if (transMode == SPEC.PAY) {
//
//                // 不可预知数 9F37
//                setUnpredictNum(Util.getRandom(4));
//                // 交易时间 9F21
//                setTransTime(Util.getSysTime(0));
//                // 交易日期 9A
//                setTransDate(Util.getSysDate(3));
//                // 商户名称 9F4E
//                setMerchantName(Config.TERMINAL_NAME_CN.getBytes(Config.CHARSET));
//                // 电子现金终端支持指示器 0:no 1:yes 9F7A
//                setVLPIndicator((byte) 0x00);
//                // 终端交易属性 9F66
//                setTermTransAtr(new byte[]{(byte) 0x28, (byte) 0x00, (byte) 0x00, (byte) 0x80});
//                // 交易类型 9C
//                setTransType((byte) 0x00);
//                // 授权金额 9F02
//                setAmtAuthNum(amtAuthNum);
//                // 其他授权金额 9F03
//                setAmtOtherNum(new byte[]{0, 0, 0, 0, 0, 0});
//                // capp 支持 DF60
//                setTermCappFlag((byte) 0x00);
//                // 设置国密支持 DF69
//                setSMAlgSupp((byte) 0x00);
//                // 终端国家代码 9F1A
//                setCountryCode(new byte[]{(byte) 0x01, (byte) 0x56});
//                // 交易货币代码 5F2A
//                setTransCurcyCode(new byte[]{(byte) 0x01, (byte) 0x56});
//                // 终端性能 9F33
//                setTermCapab(new byte[]{(byte) 0xE0, (byte) 0xE8, (byte) 0xE8});
//                // TVR 95
//                setTVR(new byte[]{0, 0, 0, 0, 0});
//                // 电子现金终端交易限额
//
//
//            } else {
//
//            }
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
//    }

//    public Iso7816.BerHouse getBerHouse() {
//        return berHouse;
//    }
//
//    public void setBerHouse(Iso7816.BerHouse berHouse) {
//        this.berHouse = berHouse;
//    }
//
//    private Iso7816.BerHouse berHouse;

    public byte[] getAcquireID() {
        return acquireID;
    }

    public void setAcquireID(byte[] acquireID) {
        this.acquireID = acquireID;
        setValue(AcquireID, acquireID);
    }

    //0
    //tag'9F01' Acquirer ID len:6
    private byte[] acquireID;

    public byte[] getTermCapab() {
        return termCapab;
    }

    public void setTermCapab(byte[] termCapab) {
        this.termCapab = termCapab;
        setValue(TermCapab, termCapab);
    }

    //tag'9F33' terminal capability len:3
    private byte[] termCapab;

    public byte[] getTermAddCapab() {
        return termAddCapab;
    }

    public void setTermAddCapab(byte[] termAddCapab) {
        this.termAddCapab = termAddCapab;
        setValue(TermAddCapab, termAddCapab);
    }

    //tag'9F40' terminal additional capability len:5
    private byte[] termAddCapab;

    public byte[] getiFD_SN() {
        return iFD_SN;
    }

    public void setiFD_SN(byte[] iFD_SN) {
        this.iFD_SN = iFD_SN;
        setValue(IFDSerNum, iFD_SN);
    }

    //tag'9F1E' IFD(POS device) serial no. asc8
    private byte[] iFD_SN;

    public byte[] getTermID() {
        return termID;
    }

    public void setTermID(byte[] termID) {
        this.termID = termID;
        setValue(TermID, termID);
    }

    //tag'9F1C' Terminal ID len:8
    private byte[] termID;

    public byte[] getMerchCateCode() {
        return merchCateCode;
    }

    public void setMerchCateCode(byte[] merchCateCode) {
        this.merchCateCode = merchCateCode;
        setValue(MerchCateCode, merchCateCode);
    }

    //tag'9F15' offset 30 Merchant Category Code len:2
    private byte[] merchCateCode;

    public byte[] getMerchID() {
        return merchID;
    }

    public void setMerchID(byte[] merchID) {
        this.merchID = merchID;
        setValue(MerchID, merchID);
    }

    //tag'9F16' Merchant ID len:15
    private byte[] merchID;

    public byte[] getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(byte[] countryCode) {
        this.countryCode = countryCode;
        setValue(TermCountryCode, countryCode);
    }

    //tag'9F1A' Terminal country code BCD len:2
    private byte[] countryCode;

    public byte[] getTRMData() {
        return tRMData;
    }

    public void setTRMData(byte[] tRMData) {
        this.tRMData = tRMData;
        setValue(TRMData, tRMData);
    }

    //tag'9F1D' Terminal Risk Management Data len:8
    private byte[] tRMData;

    public byte getTermType() {
        return termType;
    }

    public void setTermType(byte termType) {
        this.termType = termType;
        setValue(TermType, termType);
    }

    //10
    //tag'9F35' offset 58 Terminal type
    private byte termType;

    public byte[] getAppVer() {
        return appVer;
    }

    public void setAppVer(byte[] appVer) {
        this.appVer = appVer;
        setValue(AppVerNum, appVer);
    }

    //tag'9F09' Application Version Number in terminal//VIS1.3.1--0x0083;VIS1.3.2--0x0084;VIS140--0x008c; len:2
    private byte[] appVer;

    public byte[] getTransCurcyCode() {
        return transCurcyCode;
    }

    public void setTransCurcyCode(byte[] transCurcyCode) {
        this.transCurcyCode = transCurcyCode;
        setValue(TransCurrencyCode, transCurcyCode);
    }

    //tag'5F2A' len:2
    private byte[] transCurcyCode;

    public byte getTransCurcyExp() {
        return transCurcyExp;
    }

    public void setTransCurcyExp(byte transCurcyExp) {
        this.transCurcyExp = transCurcyExp;
        setValue(TransCurrencyExp, transCurcyExp);
    }

    //tag'5F36'
    private byte transCurcyExp;

    public byte[] getTransReferCurcyCode() {
        return transReferCurcyCode;
    }

    public void setTransReferCurcyCode(byte[] transReferCurcyCode) {
        this.transReferCurcyCode = transReferCurcyCode;
        setValue(TransReferCurrencyCode, transReferCurcyCode);
    }

    //tag'9F3C' len:2
    private byte[] transReferCurcyCode;

    public byte getTransReferCurcyExp() {
        return transReferCurcyExp;
    }

    public void setTransReferCurcyExp(byte transReferCurcyExp) {
        this.transReferCurcyExp = transReferCurcyExp;
        setValue(TransReferCurrencyExp, transReferCurcyExp);
    }

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
        setValue(TransType, transType);
    }

    //for distinguish different trans types such as goods and service.
    private byte transType;

    public byte getTransTypeValue() {
        return transTypeValue;
    }

    public void setTransTypeValue(byte transTypeValue) {
        this.transTypeValue = transTypeValue;
    }

    //20
    //tag'9C', offset 83  transtype value(first two digits of processing code) as stated in EMV2000. goods and service are both 0x00.
    private byte transTypeValue;

    public byte[] getvLPTransLimit() {
        return vLPTransLimit;
    }

    public void setvLPTransLimit(byte[] vLPTransLimit) {
        this.vLPTransLimit = vLPTransLimit;
        setValue(VLPTransLimit, vLPTransLimit);
    }

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

    //also see emvterm.pdf p32 default:0-offLine
    private byte bForceOnline = 0;
    //private set for send different msg to host-AuthRQ and FinaRQ.
    private byte bBatchCapture = 0;
    //30
    // offset 110  0-not support;1-support. configurable terminal parameter to indicate if VLP is supported.
    private byte bTermSupportVLP = 0;
    private byte maxTargetPercent = 0;
    //Preset by terminal. range 0-99, and MTP>=TP
    private byte targetPercent = 0;

    public byte[] getTermDDOL() {
        return termDDOL;
    }

    public void setTermDDOL(byte[] termDDOL) {
        this.termDDOL = termDDOL;
    }

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
        setValue(FloorLimit, floorLimit);
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
        setValue(AmtAuthorBin, amtAuthBin);
    }

    //tag'81' Authorised amount of binary len:4
    private byte[] amtAuthBin;

    public byte[] getAmtAuthNum() {
        return amtAuthNum;
    }

    public void setAmtAuthNum(byte[] amtAuthNum) {
        this.amtAuthNum = amtAuthNum;
        setValue(AmtAuthorNum, amtAuthNum);
    }

    //tag'9F02' Authorised amount of BCD numeric len:6
    private byte[] amtAuthNum;

    public byte[] getAmtOtherBin() {
        return amtOtherBin;
    }

    public void setAmtOtherBin(byte[] amtOtherBin) {
        this.amtOtherBin = amtOtherBin;
        setValue(AmtOtherBin, amtOtherBin);
    }

    //50
    //tag'9F04' offset 538 Other(cashback) amount of binary len:4
    private byte[] amtOtherBin;

    public byte[] getAmtOtherNum() {
        return amtOtherNum;
    }

    public void setAmtOtherNum(byte[] amtOtherNum) {
        this.amtOtherNum = amtOtherNum;
        setValue(AmtOtherNum, amtOtherNum);
    }

    //tag'9F03' Other(cashback) amount of BCD numeric len:6
    private byte[] amtOtherNum;

    public byte[] getAmtReferCurcy() {
        return amtReferCurcy;
    }

    public void setAmtReferCurcy(byte[] amtReferCurcy) {
        this.amtReferCurcy = amtReferCurcy;
        setValue(AmtReferCurrency, amtReferCurcy);
    }

    //tag'9F3A' Authorised amount in the reference currency len:4
    private byte[] amtReferCurcy;

    public byte[] getAID() {
        return aID;
    }

    public void setAID(byte[] aID) {
        this.aID = aID;
        setValue(AID, aID);
    }

    //tag'9F06' Application Identifier for selected application,5-16 len:16
    private byte[] aID;

    public byte[] getAuthorCode() {
        return authorCode;
    }

    public void setAuthorCode(byte[] authorCode) {
        this.authorCode = authorCode;
        setValue(AuthorCode, authorCode);
    }

    //tag'89'   offset 569 ret from issuer.move to TermInfo from global variable in P70. len:6
    private byte[] authorCode;

    public byte[] getAuthRespCode() {
        return authRespCode;
    }

    public void setAuthRespCode(byte[] authRespCode) {
        this.authRespCode = authRespCode;
        setValue(AuthorRespCode, authRespCode);
    }

    //tag'8A'   Authorised respose code received from host. len:2
    private byte[] authRespCode;

    public byte[] getCVMResult() {
        return cVMResult;
    }

    public void setCVMResult(byte[] cVMResult) {
        this.cVMResult = cVMResult;
        setValue(CVM, cVMResult);
    }

    public void updateCVMResult(int offset, byte b) {
        cVMResult[offset] = b;
    }

    //tag'9F34' cardholder verification methods perform result len:3
    private byte[] cVMResult;

    public byte getPOSEntryMode() {
        return pOSEntryMode;
    }

    public void setPOSEntryMode(byte pOSEntryMode) {
        this.pOSEntryMode = pOSEntryMode;
        setValue(POSEntryMode, pOSEntryMode);
    }

    //tag'9F39' POS entry mode,BCD
    private byte pOSEntryMode;

    public byte[] getPIN() {
        return pIN;
    }

    public void setPIN(byte[] pIN) {
        this.pIN = pIN;
        setValue(PIN, pIN);
    }

    //60
    //tag'99'	offset 582
    private byte[] pIN = new byte[12];

    public byte[] getTVR() {
        return tVR;
    }

    public void setTVR(byte[] tVR) {
        this.tVR = tVR;
        setValue(TVR, tVR);
    }

    public void orTVR(int offset, byte b) {
        if (tVR != null && tVR.length > offset) {
            this.tVR[offset] |= b;
            setValue(TVR, tVR);
        }
    }

    public void andTVR(int offset, byte b) {
        if (tVR != null && tVR.length > offset) {
            this.tVR[offset] &= b;
            setValue(TVR, tVR);
        }
    }

    //tag'95'   Terminal Verification Results
    private byte[] tVR;

    public byte[] getTSI() {
        return tSI;
    }

    public void setTSI(byte[] tSI) {
        this.tSI = tSI;
        setValue(TSI, tSI);
    }

    public void orTSI(int offset, byte b) {
        if (tSI != null && tSI.length > offset) {
            tSI[offset] |= b;
            setValue(TSI, tSI);
        }
    }

    //tag'9B' Transaction Status Information len:2
    private byte[] tSI;

    public byte getVLPIndicator() {
        return vLPIndicator;
    }

    public void setVLPIndicator(byte vLPIndicator) {
        this.vLPIndicator = vLPIndicator;
        setValue(VLPIndicator, vLPIndicator);
    }

    //tag'9F7A' //0-not support; 1-support; 2-VLP only. variable parameter to indicate if this trans is VLP supported.
    // EC Terminal Support Indicator
    private byte vLPIndicator;

    public byte[] getTransDate() {
        return transDate;
    }

    public void setTransDate(byte[] transDate) {
        this.transDate = transDate;
        setValue(TransDate, transDate);
    }

    //tag'9A'   YYMMDD len:3
    private byte[] transDate;

    public byte[] getTransTime() {
        return transTime;
    }

    public void setTransTime(byte[] transTime) {
        this.transTime = transTime;
        setValue(TransTime, transTime);
    }

    //tag'9F21',offset 605 HHMMSS,BCD len:3
    private byte[] transTime;

    public byte[] getTCHashValue() {
        return tCHashValue;
    }

    public void setTCHashValue(byte[] tCHashValue) {
        this.tCHashValue = tCHashValue;
        setValue(TCHashValue, tCHashValue);
    }

    //tag'98' len:20
    private byte[] tCHashValue;

    public byte[] getUnpredictNum() {
        return unpredictNum;
    }

    public void setUnpredictNum(byte[] unpredictNum) {
        this.unpredictNum = unpredictNum;
        setValue(UnpredictNum, unpredictNum);
    }

    //tag'9F37' Terminal created for each transaction. len:4
    private byte[] unpredictNum;

    public byte[] getIssuerAuthenData() {
        return issuerAuthenData;
    }

    public void setIssuerAuthenData(byte[] issuerAuthenData) {
        this.issuerAuthenData = issuerAuthenData;
        setValue(IssuerAuthenData, issuerAuthenData);
    }

    //tag'91'   Issuer Authentication Data. len:16
    private byte[] issuerAuthenData;

    public byte getmCHIPTransCateCode() {
        return mCHIPTransCateCode;
    }

    public void setmCHIPTransCateCode(byte mCHIPTransCateCode) {
        this.mCHIPTransCateCode = mCHIPTransCateCode;
        setValue(MCHIPTransCategoryCode, mCHIPTransCateCode);
    }

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
        setValue(TermTranAtr, termTransAtr);
    }

    //tag '9F66' offset: 669 len:4
    private byte[] termTransAtr;

    public byte getTermCappFlag() {
        return termCappFlag;
    }

    public void setTermCappFlag(byte termCappFlag) {
        this.termCappFlag = termCappFlag;
        setValue(TERMCAPPFlag, termCappFlag);
    }

    //tag 'DF60' offset: 673
    private byte termCappFlag;

    public byte[] getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(byte[] merchantName) {
        this.merchantName = merchantName;
        setValue(MerchantName, merchantName);
    }

    //tag '9F4E' offset: 674 len:20
    private byte[] merchantName;

    public byte getSMAlgSupp() {
        return sMAlgSupp;
    }

    public void setSMAlgSupp(byte sMAlgSupp) {
        this.sMAlgSupp = sMAlgSupp;
        setValue(SMAlgSupp, sMAlgSupp);
    }

    //tag 'DF69' offset: 694
    private byte sMAlgSupp;
    //tag '0000' CVM limit value, specific to PayPass offset: 695 len:4
    private byte[] pPCVMLimit;

    // 1F 自定义标签
    public static final Short TACDenial = (short) 0x1F01;
    public static final Short TACOnline = (short) 0x1F02;
    public static final Short TACDefault = (short) 0x1F03;
    public static final Short VLPTACDenial = (short) 0x1F05;
    public static final Short VLPTACOnline = (short) 0x1F06;
    public static final Short VLPTACDefault = (short) 0x1F07;
    public static final Short Language = (short) 0x1F08;
    public static final Short ForceAccept = (short) 0x1F10;
    public static final Short ForceOnline = (short) 0x1F11;
    public static final Short BatchCapture = (short) 0x1F12;
    public static final Short TermSupportVLP = (short) 0x1F13;
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
    public static final Short AmtAuthorBin = (short) 0xFF81;
    public static final Short AuthorCode = (short) 0xFF89;
    public static final Short AuthorRespCode = (short) 0xFF8A;
    public static final Short IssuerAuthenData = (short) 0xFF91;
    public static final Short TVR = (short) 0xFF95;
    public static final Short TCHashValue = (short) 0xFF98;
    public static final Short PIN = (short) 0xFF99;
    public static final Short TransDate = (short) 0xFF9A;
    public static final Short TSI = (short) 0xFF9B;
    public static final Short TransType = (short) 0xFF9C;
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
    public static final Short CVM = (short) 0x9F34;
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
    public static final Short TERMCAPPFlag = (short) 0xDF60;
    public static final Short SMAlgSupp = (short) 0xDF69;

//    public void setValue(byte t, byte[] v) {
//        if (this.map != null) {
//            Short ts = (short) (((t & 0xFF) << 8) | (0x00 & 0xFF));
//            this.map.put(ts, v);
//        }
//    }

//    public byte[] getValue(byte t) {
//        Short ts = (short) (((t & 0xFF) << 8) | (0x00 & 0xFF));
//        return this.map.get(ts);
//    }

    public void setValue(short t, byte[] v) {
        if (this.map != null) {
            this.map.put(t, v);
        }
    }

    public void setValue(short t, byte v) {
        if (this.map != null) {
            this.map.put(t, new byte[]{v});
        }
    }

    public byte[] getValue(short t) {
        return this.map.get(t);
    }

    public byte[] getValue(byte[] t) {
        Short ts = 0;
        if (t.length == 2)
            ts = (short) (((t[0] & 0xFF) << 8) | (t[1] & 0xFF));
        else
            ts = (short) (((0xFF & 0xFF) << 8) | (t[0] & 0xFF));
        return getValue(ts);
    }

    private HashMap<Short, byte[]> map;
}
