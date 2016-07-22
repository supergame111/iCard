package com.ftsafe.iccd.ecard.reader.pboc;

/**
 * Created by qingyuan on 2016/7/21.
 */
public class ErrCode {
    public static long ERR_EMV_CVRFail = 0x580001;
    public static long ERR_EMV_CVRNoSupport = 0x580002;
    public static long ERR_EMV_TAGExist = 0x580003;
    public static long ERR_EMV_LENNotCorrect = 0x580004;
    public static long ERR_EMV_ErrorTLVObject = 0x580005;
    public static long ERR_EMV_LenOfGPOResp = 0x580006;
    public static long ERR_EMV_GPORespData = 0x580007;
    public static long ERR_EMV_AFLorAIPNotExist = 0x580008;
    public static long ERR_EMV_AFLNotExist = 0x580009;
    public static long ERR_EMV_AFLFormatError = 0x58000A;
    public static long ERR_EMV_NoTermDDOL = 0x58000B;
    public static long ERR_EMV_InternalAuthRespData = 0x58000C;
    public static long ERR_EMV_ErrorTransFlag = 0x58000D;
    public static long ERR_EMV_GenerateACRespData = 0x58000E;
    public static long ERR_EMV_QPBOCAIPSuppCDA = 0x58000F;
    public static long ERR_EMV_ErrorCertLen = 0x580010;
    public static long ERR_EMV_ErrorCertFormat = 0x580011;
    public static long ERR_EMV_ErrorIssuerCertHash = 0x580012;
    public static long ERR_EMV_ErrorSADHash = 0x580013;
    public static long ERR_EMV_ErrorICCCertHash = 0x580014;
    public static long ERR_EMV_ErrorDynSignHash = 0x580015;
    public static long ERR_EMV_ErrorCertFormatNum = 0x580016;
    public static long ERR_EMV_CAPKNotMatch = 0x580017;
    public static long ERR_EMV_DecryptIPKCert = 0x580018;
    public static long ERR_EMV_IPKNotMatch = 0x580019;
    public static long ERR_EMV_DecryptICCCert = 0x58001A;
    public static long ERR_EMV_DecryptDynSign = 0x58001B;
    public static long ERR_EMV_DecryptSAD = 0x58001C;
    public static long ERR_EMV_ErrorDateFormat = 0x58001D;
    public static long ERR_EMV_SMVerifyIssuerCert = 0x58001E;
    public static long ERR_EMV_SMVerifySAD = 0x58001F;
    public static long ERR_EMV_SMVerifyICCCert = 0x580020;
    public static long ERR_EMV_SMVerifyDynSign = 0x580021;
    public static long ERR_EMV_CDADATAMISSING = 0x580022;
    public static long ERR_EMV_DDADATAMISSING = 0x580023;
    public static long ERR_EMV_SDADATAMISSING = 0x580028;
    public static long ERR_EMV_MC_CCC = 0x580024;
    public static long ERR_EMV_NotVerifyARQC = 0x580025;
    public static long ERR_EMV_RetCardAuthRelData = 0x580026;
    public static long ERR_EMV_AC = 0x580027;
    public static long ERR_EMV_DOLFORMAT = 0x580029;
    public static long ERR_PB_ErrorPurchaseMAC2 = 0x680001;
    public static long ERR_PB_ErrorPurchaseTAC = 0x680002;
    public static long ERR_PB_ErrorLoadMAC1 = 0x680003;
    public static long ERR_PB_ErrorLoadTAC = 0x680004;
}
