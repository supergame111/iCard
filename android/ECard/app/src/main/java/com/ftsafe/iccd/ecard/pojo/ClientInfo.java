package com.ftsafe.iccd.ecard.pojo;

import ftsafe.reader.tech.Iso7816;

/**
 * Created by qingyuan on 2016/7/25.
 */
public class ClientInfo {

    public ClientInfo(Iso7816.BerHouse berHouse) {
        issuAppData = berHouse.findFirst(PbocTag.ISSUER_APP_DATA).v.getBytes();
        unpredictNum = berHouse.findFirst(PbocTag.UNPREDICT_NUM).v.getBytes();
        appCrypt = berHouse.findFirst(PbocTag.APP_CRYPTOGRAM).v.getBytes();
        amtAuthNum = berHouse.findFirst(PbocTag.AMT_AUTHOR_NUM).v.getBytes();
        amtOtherNum = berHouse.findFirst(PbocTag.AMT_OTHER_NUM).v.getBytes();
        countryCode = berHouse.findFirst(PbocTag.ISSUER_COUNTRY_CODE).v.getBytes();
        cryptInfo = berHouse.findFirst(PbocTag.CRYPTOGRAM_INFO).v.getBytes()[0];
    }

    public byte[] issuAppData;
    public byte[] ATC;
    public byte[] unpredictNum;
    public byte[] appCrypt;
    public byte[] amtAuthNum;
    public byte[] amtOtherNum;
    public byte[] countryCode;
    public byte[] TVR;
    public byte[] transCurcyCode;
    public byte[] transDate;
    public byte transTypeValue;
    public byte[] AIP;
    public byte cryptInfo;
}
