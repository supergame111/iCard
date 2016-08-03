package com.ftsafe.iccd.ecard.backend;

import com.ftsafe.iccd.ecard.pojo.PbocTag;

import ftsafe.common.ErrMessage;
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
    public byte[] pan;
    public byte[] panSerial;
    public byte transTypeValue;

    public static final short[] TAG_PARAMS = {
            PbocTag.TVR, PbocTag.APP_INTERCHANGE_PROFILE, PbocTag.ATC,
            PbocTag.ISSUER_APP_DATA, PbocTag.UNPREDICT_NUM, PbocTag.APP_CRYPTOGRAM,
            PbocTag.AMT_AUTHOR_NUM, PbocTag.AMT_OTHER_NUM,PbocTag.TERM_COUNTRY_CODE,
            PbocTag.TRANS_CURRENCY_CODE,PbocTag.TRANS_DATE,PbocTag.TRANS_TYPE,PbocTag.PAN,PbocTag.PAN_SERIAL};

    public ClientInfo(Iso7816.BerHouse berHouse) throws ErrMessage {

        Iso7816.BerTLV tlv = berHouse.findFirst(PbocTag.TVR);
        if (tlv == null)
            throw new ErrMessage("TVR为空");
        this.TVR = tlv.v.getBytes();

        tlv = berHouse.findFirst(PbocTag.APP_INTERCHANGE_PROFILE);
        if (tlv == null)
            throw new ErrMessage("AIP为空");
        this.AIP = tlv.v.getBytes();

        tlv = berHouse.findFirst(PbocTag.ATC);
        if (tlv == null)
            throw new ErrMessage("ATC为空");
        this.ATC = tlv.v.getBytes();

        tlv = berHouse.findFirst(PbocTag.ISSUER_APP_DATA);
        if (tlv == null)
            throw new ErrMessage("ISSUER_APP_DATA为空");
        this.issuAppData = tlv.v.getBytes();

        tlv = berHouse.findFirst(PbocTag.UNPREDICT_NUM);
        if (tlv == null)
            throw new ErrMessage("UNPREDICT_NUM为空");
        this.unpredictNum = tlv.v.getBytes();

        tlv = berHouse.findFirst(PbocTag.APP_CRYPTOGRAM);
        if (tlv == null)
            throw new ErrMessage("APP_CRYPTOGRAM为空");
        this.appCrypt = tlv.v.getBytes();

        tlv = berHouse.findFirst(PbocTag.AMT_AUTHOR_NUM);
        if (tlv == null)
            throw new ErrMessage("AMT_AUTHOR_NUM为空");
        this.amtAuthNum = tlv.v.getBytes();

        tlv = berHouse.findFirst(PbocTag.AMT_OTHER_NUM);
        if (tlv == null)
            throw new ErrMessage("AMT_OTHER_NUM为空");
        this.amtOtherNum = tlv.v.getBytes();

        tlv = berHouse.findFirst(PbocTag.TERM_COUNTRY_CODE);
        if (tlv == null)
            throw new ErrMessage("TERM_COUNTRY_CODE为空");
        this.countryCode = tlv.v.getBytes();

        tlv = berHouse.findFirst(PbocTag.TRANS_CURRENCY_CODE);
        if (tlv == null)
            throw new ErrMessage("TRANS_CURRENCY_CODE为空");
        this.transCurcyCode = tlv.v.getBytes();

        tlv = berHouse.findFirst(PbocTag.TRANS_DATE);
        if (tlv == null)
            throw new ErrMessage("TRANS_DATE为空");
        this.transDate = tlv.v.getBytes();

        tlv = berHouse.findFirst(PbocTag.TRANS_TYPE);
        if (tlv == null)
            throw new ErrMessage("TRANS_DATE为空");
        this.transTypeValue = tlv.v.getBytes()[0];

        tlv = berHouse.findFirst(PbocTag.PAN);
        if (tlv == null)
            throw new ErrMessage("PAN为空");
        this.pan = tlv.v.getBytes();

        tlv = berHouse.findFirst(PbocTag.PAN_SERIAL);
        if (tlv == null)
            throw new ErrMessage("PAN_SERIAL为空");
        this.panSerial = tlv.v.getBytes();

    }
}
