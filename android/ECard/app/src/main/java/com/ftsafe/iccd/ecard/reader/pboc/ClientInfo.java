package com.ftsafe.iccd.ecard.reader.pboc;

import ftsafe.reader.tech.Iso7816;

/**
 * Created by qingyuan on 16/7/27.
 */
public class ClientInfo {
    
    public byte[] TVR;
    public byte[] AIP;
    public byte[] ATC;
    public byte[] issuAppData;
    public byte[] unpredictNum;
    public byte[] appCrypt;
    public byte[] amtAuthNum;
    public byte[] amtOtherNum;
    public byte[] countryCode;
    public byte[] transCurcyCode;
    public byte[] transDate;
    public byte transTypeValue;

    public ClientInfo(Iso7816.BerHouse berHouse) {

    }
}
