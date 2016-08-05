package com.ftsafe.iccd.ecard.backend;

import android.util.Log;

import com.ftsafe.iccd.ecard.Config;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import ftsafe.common.ErrMessage;
import ftsafe.common.Util;
import ftsafe.common.encryption.DES2;
import ftsafe.reader.tech.Iso7816;

/**
 * Created by ft on 2016/8/3.
 */
public class Server {

    public final static byte[] UPDATE_EC_CMD = {(byte) 0x04, (byte) 0xDA, (byte) 0x9F, (byte) 0x79, (byte) 0x0A};
    private short mTransMode;
    private ClientInfo mClientInfo = null;
    byte[] ARQC = null;
    private byte[] ARPC = null;
    private byte[] ARC = {0x30, 0x30};
    private byte[] ACSessionKey = null;
    private byte[] MACSessionKey = null;
    private byte[] ENCSessionKey = null;

    public short getmTransMode() {
        return mTransMode;
    }

    public void setTransMode(short transMode) {
        mTransMode = transMode;
    }

    public byte[] putData() throws ErrMessage {

        if (MACSessionKey != null && mClientInfo != null && ARQC != null) {
            byte[] data = calculateMoney(mClientInfo.amtAuthNum);
            // MAC计算
            byte[] iv = {(byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0,};
            int bufLen = 0;
            ByteBuffer buf = ByteBuffer.allocate(252);
            buf.put(UPDATE_EC_CMD);
            bufLen += UPDATE_EC_CMD.length;
            buf.put(mClientInfo.ATC);
            bufLen += mClientInfo.ATC.length;
            buf.put(ARQC);
            bufLen += ARQC.length;
            buf.put(data);
            bufLen += data.length;
            byte[] msg = Arrays.copyOfRange(buf.array(), 0, bufLen);
            Log.d(Config.APP_ID, "构建脚本，msg=" + Util.toHexString(msg));
            byte[] mac = DES2.TripleDES.mac(MACSessionKey, 16, msg, msg.length, iv, 4, DES2.TripleDES.PADDING_80);
            Log.d(Config.APP_ID, "构建脚本，mac=" + Util.toHexString(mac));

            // 封装脚本
            buf.clear();
            bufLen = 0;
            buf.put(UPDATE_EC_CMD);
            bufLen += UPDATE_EC_CMD.length;
            buf.put(data);
            bufLen += data.length;
            buf.put(mac);
            bufLen += mac.length;

            return Arrays.copyOfRange(buf.array(), 0, bufLen);
        } else
            throw new ErrMessage("请先执行ARQC认证");
    }

    byte[] calculateMoney(byte[] amtAuthNum) throws ErrMessage {
        if (amtAuthNum != null) {
            byte[] issAppData = Arrays.copyOfRange(mClientInfo.issuAppData, 8, mClientInfo.issuAppData.length);
            Log.d(Config.APP_ID, "发卡行自定义数据=" + Util.toHexString(issAppData));
            if (issAppData[0] == (byte) 0x0A && issAppData[1] == (byte) 0x01) {
                final int aau = Integer.valueOf(Util.toHexString(amtAuthNum));
                Log.d(Config.APP_ID, "授权金额=" + aau);
                final int remain = Integer.valueOf(Util.toHexString(Arrays.copyOfRange(issAppData, 2, 7)));
                Log.d(Config.APP_ID, "卡内余额=" + remain);
                int total = 0;
                if (mTransMode == SPEC.LOAD)
                    total = aau + remain;
                else if (mTransMode == SPEC.PAY)
                    total = remain - aau;
                else
                    total = remain;
                Log.d(Config.APP_ID, "总金额=" + total);

                return Util.toBytes(String.format("%012d", total));
            } else
                throw new ErrMessage("发卡自定义数据(IDD)=" + Util.toHexString(issAppData));
        }

        return null;
    }

    /**
     * 验证ARQC
     *
     * @param berHouse
     * @param bACKey
     * @param bMACKey
     * @param bENCKey
     * @param emvMode
     * @param isIssuerMasterKey
     * @return ARC ARPC
     * @throws ErrMessage
     * @throws IllegalBlockSizeException
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     */

    public byte[] verifyARQC(Iso7816.BerHouse berHouse, byte[] bACKey, byte[] bMACKey, byte[] bENCKey, int emvMode, boolean isIssuerMasterKey)
            throws ErrMessage, IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {

        if ((bACKey != null && bACKey.length == 16) || (bMACKey != null && bMACKey.length == 16) || (bENCKey != null && bENCKey.length == 16)) {
            Log.d(Config.APP_ID, "主密钥");
            Log.d(Config.APP_ID, "isIssuerMasterKey=" + isIssuerMasterKey + ",AC=" + Util.toHexString(bACKey));
            Log.d(Config.APP_ID, "isIssuerMasterKey=" + isIssuerMasterKey + ",MAC=" + Util.toHexString(bMACKey));
            Log.d(Config.APP_ID, "isIssuerMasterKey=" + isIssuerMasterKey + ",ENC=" + Util.toHexString(bENCKey));

            // generate cardInfo
            mClientInfo = new ClientInfo(berHouse);

            final int EMVMode = emvMode;
            Log.d(Config.APP_ID, "EMVMode=" + EMVMode);

            String pan = Util.toHexString(mClientInfo.pan);
            String panSeq = Util.toHexString(mClientInfo.panSerial);
            String str5A = pan.toUpperCase().replace("F", "");
            String str5F34 = panSeq.trim();
            String strDIV;
            byte[] DIV = new byte[16];
            byte[] ACKey = new byte[16], MACKey = new byte[16], ENCKey = new byte[16];
            byte[] TACKey = new byte[16], TMACKey = new byte[16], TENCKey = new byte[16];

            if (isIssuerMasterKey) { // 发卡行主密钥
                strDIV = str5A + str5F34;
                int len = strDIV.length();
                if (len > 16)
                    strDIV = strDIV.substring(strDIV.length() - 16);
                Arrays.fill(DIV, (byte) 0xFF);
                System.arraycopy(Util.toBytes(strDIV), 0, DIV, 0, 8);
                byte[] tmp = Util.calXOR(Arrays.copyOfRange(DIV, 8, 16), Arrays.copyOf(DIV, 8), 8);
                System.arraycopy(tmp, 0, DIV, 8, 8);
                //算法标识 04 使用国密算法
                if ((EMVMode == SPEC.TRANS_MODE_PBOC) && mClientInfo.issuAppData[7] == (byte) 0x04) {
//                    SM4Encrypt(ucDIV, 16, CD_SM4_ECB, NULL, ucACKey, ucTACKey, & nKeyLen);
//                    SM4Encrypt(ucDIV, 16, CD_SM4_ECB, NULL, ucMACKey, ucTMACKey, & nKeyLen);
//                    SM4Encrypt(ucDIV, 16, CD_SM4_ECB, NULL, ucENCKey, ucTENCKey, & nKeyLen);
                }
                // 国际算法
                else {
                    TACKey = DES2.TripleDES.encrypt(bACKey, DIV, null, DES2.TripleDES.DESEDE_ECB_NOPADDING);
                    TMACKey = DES2.TripleDES.encrypt(bMACKey, DIV, null, DES2.TripleDES.DESEDE_ECB_NOPADDING);
                    TENCKey = DES2.TripleDES.encrypt(bENCKey, DIV, null, DES2.TripleDES.DESEDE_ECB_NOPADDING);

                }
            } // end 发卡行主密钥
            else { // 不是发卡行主密钥
                TACKey = bACKey;
                TMACKey = bMACKey;
                TENCKey = bENCKey;
            }
            Log.d(Config.APP_ID, "子密钥");
            Log.d(Config.APP_ID, "AC=" + Util.toHexString(bACKey) + ",分散因子=" + Util.toHexString(DIV));
            Log.d(Config.APP_ID, "MAC=" + Util.toHexString(bMACKey) + ",分散因子=" + Util.toHexString(DIV));
            Log.d(Config.APP_ID, "ENC=" + Util.toHexString(bENCKey) + ",分散因子=" + Util.toHexString(DIV));

            // 计算过程密钥
            Arrays.fill(DIV, (byte) 0);
            Arrays.fill(ACKey, (byte) 0);
            Arrays.fill(MACKey, (byte) 0);
            Arrays.fill(ENCKey, (byte) 0);

            // 发卡行应用数据
            byte[] issuerAppData = mClientInfo.issuAppData;
            if (issuerAppData == null)
                throw new ErrMessage("发卡行应用数据为空");
            Log.d(Config.APP_ID, "发卡行应用数据=" + Util.toHexString(issuerAppData));
            // 密文版号
            byte crypto = issuerAppData[2];
            Log.d(Config.APP_ID, "密文版本号=" + Util.toHexString(crypto));

            if (EMVMode == SPEC.TRANS_MODE_PBOC) { // PBOC
                // 计算过程密钥
                if (crypto == (byte) 0x01 || crypto == (byte) 0x17) {
                    // ATC 取反放在第14位
                    byte[] tmp = Util.calXOR(new byte[]{(byte) 0xFF, (byte) 0xFF}, mClientInfo.ATC, 2);
                    System.arraycopy(tmp, 0, DIV, 14, 2);
                    // ATC 放在第6位
                    System.arraycopy(mClientInfo.ATC, 0, DIV, 6, 2);

                    ACKey = DES2.TripleDES.encrypt(TACKey, DIV, null, DES2.TripleDES.DESEDE_ECB_NOPADDING);
                    Log.d(Config.APP_ID, "过程密钥AC=" + Util.toHexString(ACKey) + ",分散因子=" + Util.toHexString(DIV));
                    MACKey = DES2.TripleDES.encrypt(TMACKey, DIV, null, DES2.TripleDES.DESEDE_ECB_NOPADDING);
                    Log.d(Config.APP_ID, "过程密钥MAC=" + Util.toHexString(MACKey) + ",分散因子=" + Util.toHexString(DIV));
                    ENCKey = DES2.TripleDES.encrypt(TENCKey, DIV, null, DES2.TripleDES.DESEDE_ECB_NOPADDING);
                    Log.d(Config.APP_ID, "过程密钥ENC=" + Util.toHexString(ENCKey) + ",分散因子=" + Util.toHexString(DIV));
                } else {
                    throw new ErrMessage("PBOC不支持该密文版本" + Util.toHexString(crypto));
                }

            }

//            if ((EMVMode == TRANS_MODE_VISA) && (issuerAppData[2] == (byte) 0x0A || issuerAppData[2] == (byte) 0x11)) { //密文版本号10或17
//                byte[] tmp = Util.calXOR(new byte[]{(byte) 0xFF, (byte) 0xFF}, mClientInfo.ATC, 2);
//                System.arraycopy(tmp, 0, DIV, 14, 2);
//                ACKey = TACKey;
//                Log.d(Config.APP_ID, "过程密钥AC=" + Util.toHexString(ACKey) + ",分散因子=" + Util.toHexString(DIV));
//                MACKey = Util.calXOR(TMACKey, DIV, 16);
//                Log.d(Config.APP_ID, "过程密钥MAC=" + Util.toHexString(MACKey) + ",分散因子=" + Util.toHexString(DIV));
//                ENCKey = Util.calXOR(TENCKey, DIV, 16);
//                Log.d(Config.APP_ID, "过程密钥ENC=" + Util.toHexString(ENCKey) + ",分散因子=" + Util.toHexString(DIV));
//            } else if ((EMVMode == TRANS_MODE_PBOC) && issuerAppData[7] == 0x04)//算法标识 04 使用国密算法
//            {
//                byte[] tmp = Util.calXOR(new byte[]{(byte) 0xFF, (byte) 0xFF}, mClientInfo.ATC, 2);
//                System.arraycopy(tmp, 0, DIV, 14, 2);
////                SM4Encrypt(ucDIV, 16, CD_SM4_ECB, NULL, ucTACKey, ucACKey, & nKeyLen);
////                SM4Encrypt(ucDIV, 16, CD_SM4_ECB, NULL, ucTMACKey, ucMACKey, & nKeyLen);
////                SM4Encrypt(ucDIV, 16, CD_SM4_ECB, NULL, ucTENCKey, ucENCKey, & nKeyLen);
//            } else if ((EMVMode == TRANS_MODE_MASTERCARD) && (issuerAppData[1] == (byte) 0x0A || issuerAppData[1] == (byte) 0x11)) {
//                //MasterCard Proprietary SKD 等文档
//                System.arraycopy(mClientInfo.ATC, 0, DIV, 0, 2);
//                System.arraycopy(mClientInfo.unpredictNum, 0, DIV, 4, 4);
//                System.arraycopy(DIV, 0, DIV, 8, 8);
//                DIV[2] = (byte) 0xF0;
//                DIV[10] = (byte) 0x0F;
//                ACKey = DES2.TripleDES.encrypt(TACKey, DIV, null, DES2.TripleDES.DESEDE_ECB_NOPADDING);
//                Log.d(Config.APP_ID, "过程密钥AC=" + Util.toHexString(ACKey) + ",分散因子=" + Util.toHexString(DIV));
//
//                Arrays.fill(DIV, (byte) 0);
//                System.arraycopy(mClientInfo.appCrypt, 0, DIV, 0, 8);
//                System.arraycopy(DIV, 0, DIV, 8, 8);
//                DIV[2] = (byte) 0xF0;
//                DIV[10] = (byte) 0x0F;
//                MACKey = DES2.TripleDES.encrypt(TMACKey, DIV, null, DES2.TripleDES.DESEDE_ECB_NOPADDING);
//                Log.d(Config.APP_ID, "过程密钥MAC=" + Util.toHexString(MACKey) + ",分散因子=" + Util.toHexString(DIV));
//                ENCKey = DES2.TripleDES.encrypt(TENCKey, DIV, null, DES2.TripleDES.DESEDE_ECB_NOPADDING);
//                Log.d(Config.APP_ID, "过程密钥ENC=" + Util.toHexString(ENCKey) + ",分散因子=" + Util.toHexString(DIV));
//            } else {
//                //EMV CSK
//                System.arraycopy(mClientInfo.ATC, 0, DIV, 0, 2);
//                System.arraycopy(DIV, 0, DIV, 8, 2);
//                DIV[2] = (byte) 0xF0;
//                DIV[10] = (byte) 0x0F;
//                ACKey = DES2.TripleDES.encrypt(TACKey, DIV, null, DES2.TripleDES.DESEDE_ECB_NOPADDING);
//                Log.d(Config.APP_ID, "过程密钥AC=" + Util.toHexString(ACKey) + ",分散因子=" + Util.toHexString(DIV));
//
//                Arrays.fill(DIV, (byte) 0);
//                System.arraycopy(mClientInfo.appCrypt, 0, DIV, 0, 8);
//                System.arraycopy(DIV, 0, DIV, 8, 8);
//                DIV[2] = (byte) 0xF0;
//                DIV[10] = (byte) 0x0F;
//                MACKey = DES2.TripleDES.encrypt(TMACKey, DIV, null, DES2.TripleDES.DESEDE_ECB_NOPADDING);
//                Log.d(Config.APP_ID, "过程密钥MAC=" + Util.toHexString(MACKey) + ",分散因子=" + Util.toHexString(DIV));
//                ENCKey = DES2.TripleDES.encrypt(TENCKey, DIV, null, DES2.TripleDES.DESEDE_ECB_NOPADDING);
//                Log.d(Config.APP_ID, "过程密钥ENC=" + Util.toHexString(ENCKey) + ",分散因子=" + Util.toHexString(DIV));
//            }

            // 计算ARQC
            ARQC = new byte[8];
            int nDataLen = 0;
            byte[] ucData = new byte[512];
            byte[] iv = {(byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0,};
            if (crypto == (byte) 0x01) {
                nDataLen = 0;
                // 授权金额
                System.arraycopy(mClientInfo.amtAuthNum, 0, ucData, nDataLen, mClientInfo.amtAuthNum.length);
                nDataLen += mClientInfo.amtAuthNum.length;
                // 其他金额
                System.arraycopy(mClientInfo.amtOtherNum, 0, ucData, nDataLen, mClientInfo.amtOtherNum.length);
                nDataLen += mClientInfo.amtOtherNum.length;
                // 终端国家代码
                System.arraycopy(mClientInfo.countryCode, 0, ucData, nDataLen, mClientInfo.countryCode.length);
                nDataLen += mClientInfo.countryCode.length;
                // 终端验证结果
                System.arraycopy(mClientInfo.TVR, 0, ucData, nDataLen, mClientInfo.TVR.length);
                nDataLen += mClientInfo.TVR.length;
                // 交易货币代码
                System.arraycopy(mClientInfo.transCurcyCode, 0, ucData, nDataLen, mClientInfo.transCurcyCode.length);
                nDataLen += mClientInfo.transCurcyCode.length;
                // 交易日期
                System.arraycopy(mClientInfo.transDate, 0, ucData, nDataLen, mClientInfo.transDate.length); //交易日期
                nDataLen += mClientInfo.transDate.length;
                // 交易类型
                ucData[nDataLen] = mClientInfo.transTypeValue;
                nDataLen += 1;
                // 不可预知数
                System.arraycopy(mClientInfo.unpredictNum, 0, ucData, nDataLen, mClientInfo.unpredictNum.length);
                nDataLen += mClientInfo.unpredictNum.length;
                // 应用交互特征
                System.arraycopy(mClientInfo.AIP, 0, ucData, nDataLen, mClientInfo.AIP.length);
                nDataLen += mClientInfo.AIP.length;
                // 应用交易计数器
                System.arraycopy(mClientInfo.ATC, 0, ucData, nDataLen, mClientInfo.ATC.length);
                nDataLen += mClientInfo.ATC.length;
                // 卡片验证结果
                System.arraycopy(mClientInfo.issuAppData, 3, ucData, nDataLen, 4);
                nDataLen += 4;

                byte[] msg = Arrays.copyOf(ucData, nDataLen);
                Log.d(Config.APP_ID, "计算应用密文,ACKey=" + Util.toHexString(ACKey) + ",InputData=" + Util.toHexString(msg));
                ARQC = DES2.TripleDES.mac(ACKey, ACKey.length, msg, msg.length, iv, 8, DES2.TripleDES.PADDING_80);
                if (issuerAppData[7] == (byte) 0x04) {
                    // 国密
                    throw new ErrMessage("暂不支持国密算法");
                }

            } else if (crypto == (byte) 0x17) {
                nDataLen = 0;
                // 授权金额
                System.arraycopy(mClientInfo.amtAuthNum, 0, ucData, nDataLen, mClientInfo.amtAuthNum.length);
                nDataLen += mClientInfo.amtAuthNum.length;
                // 不可预知数
                System.arraycopy(mClientInfo.unpredictNum, 0, ucData, nDataLen, mClientInfo.unpredictNum.length);
                nDataLen += mClientInfo.unpredictNum.length;
                // 应用交易计数器
                System.arraycopy(mClientInfo.ATC, 0, ucData, nDataLen, mClientInfo.ATC.length);
                nDataLen += mClientInfo.ATC.length;
                // 发卡行应用数据（部分）
                System.arraycopy(mClientInfo.issuAppData, 4, ucData, nDataLen, 1);
                nDataLen += 1;

                byte[] msg = Arrays.copyOf(ucData, nDataLen);
                Log.d(Config.APP_ID, "计算应用密文,AC Key=" + Util.toHexString(ACKey) + ",Input Data=" + Util.toHexString(msg));
                ARQC = DES2.TripleDES.mac(ACKey, ACKey.length, msg, msg.length, iv, 8, DES2.TripleDES.PADDING_80);
                if (issuerAppData[7] == (byte) 0x04) {
                    // 国密
                    throw new ErrMessage("暂不支持国密算法");
                }
            } else {
                throw new ErrMessage("不支持的密文版本" + Util.toHexString(crypto));
            }
//
//            // 密文版号:17
//            if (crypto == (byte) 0x11) {
//                nDataLen = 0;
//                System.arraycopy(mClientInfo.amtAuthNum, 0, ucData, nDataLen, mClientInfo.amtAuthNum.length);
//                nDataLen += mClientInfo.amtAuthNum.length;
//
//                System.arraycopy(mClientInfo.unpredictNum, 0, ucData, nDataLen, mClientInfo.unpredictNum.length);
//                nDataLen += mClientInfo.unpredictNum.length;
//
//                System.arraycopy(mClientInfo.ATC, 0, ucData, nDataLen, mClientInfo.ATC.length);
//                nDataLen += mClientInfo.ATC.length;
//
//                System.arraycopy(mClientInfo.issuAppData, 4, ucData, nDataLen, 1);
//                nDataLen += 1;
//
//                //算法标识 04 使用国密算法
//                if (issuerAppData[7] == 0x04) {
//                    //SM4MAC(ucData, nDataLen, ISO_PADDING_2, CD_SM4_MAC_16, NULL, ucACKey, ucARQC);
//                } else { // 国际算法
//                    if (EMVMode == TRANS_MODE_MASTERCARD) { // MASTERCARD
//                        nDataLen = 0;
//                        System.arraycopy(mClientInfo.amtAuthNum, 0, ucData, nDataLen, mClientInfo.amtAuthNum.length);
//                        nDataLen += mClientInfo.amtAuthNum.length;
//
//                        System.arraycopy(mClientInfo.amtOtherNum, 0, ucData, nDataLen, mClientInfo.amtOtherNum.length);
//                        nDataLen += mClientInfo.amtOtherNum.length;
//
//                        System.arraycopy(mClientInfo.countryCode, 0, ucData, nDataLen, mClientInfo.countryCode.length);
//                        nDataLen += mClientInfo.countryCode.length;
//
//                        System.arraycopy(mClientInfo.TVR, 0, ucData, nDataLen, mClientInfo.TVR.length);
//                        nDataLen += mClientInfo.TVR.length;
//
//                        System.arraycopy(mClientInfo.transCurcyCode, 0, ucData, nDataLen, mClientInfo.transCurcyCode.length);
//                        nDataLen += mClientInfo.transCurcyCode.length;
//
//                        System.arraycopy(mClientInfo.transDate, 0, ucData, nDataLen, mClientInfo.transDate.length); //交易日期
//                        nDataLen += mClientInfo.transDate.length;
//
//                        ucData[nDataLen] = mClientInfo.transTypeValue;
//                        nDataLen += 1;
//
//                        System.arraycopy(mClientInfo.unpredictNum, 0, ucData, nDataLen, mClientInfo.unpredictNum.length);
//                        nDataLen += mClientInfo.unpredictNum.length;
//
//                        System.arraycopy(mClientInfo.AIP, 0, ucData, nDataLen, mClientInfo.AIP.length);
//                        nDataLen += mClientInfo.AIP.length;
//
//                        System.arraycopy(mClientInfo.ATC, 0, ucData, nDataLen, mClientInfo.ATC.length);
//                        nDataLen += mClientInfo.ATC.length;
//
//                        System.arraycopy(mClientInfo.issuAppData, 2, ucData, nDataLen, 6);
//                        nDataLen += 6;
//
//                        ARQC = DES2.TripleDES.mac(ACKey, ACKey.length, ucData, nDataLen, iv, 8, TripleDES.PADDING_80);
//                    } else if (EMVMode == TRANS_MODE_PBOC || EMVMode == TRANS_MODE_VISA) {
//                        ARQC = DES2.TripleDES.mac(ACKey, ACKey.length, ucData, nDataLen, iv, 8, TripleDES.PADDING_80);
//                    } else {
//                        // Nothing done
//                    }
//                }
//            }
//            // 密文版本号:10
//            else if (crypto == (byte) 0x0A) {
//                nDataLen = 0;
//                System.arraycopy(mClientInfo.amtAuthNum, 0, ucData, nDataLen, mClientInfo.amtAuthNum.length);
//                nDataLen += mClientInfo.amtAuthNum.length;
//
//                System.arraycopy(mClientInfo.amtOtherNum, 0, ucData, nDataLen, mClientInfo.amtOtherNum.length);
//                nDataLen += mClientInfo.amtOtherNum.length;
//
//                System.arraycopy(mClientInfo.countryCode, 0, ucData, nDataLen, mClientInfo.countryCode.length);
//                nDataLen += mClientInfo.countryCode.length;
//
//                System.arraycopy(mClientInfo.TVR, 0, ucData, nDataLen, mClientInfo.TVR.length);
//                nDataLen += mClientInfo.TVR.length;
//
//                System.arraycopy(mClientInfo.transCurcyCode, 0, ucData, nDataLen, mClientInfo.transCurcyCode.length);
//                nDataLen += mClientInfo.transCurcyCode.length;
//
//                System.arraycopy(mClientInfo.transDate, 0, ucData, nDataLen, mClientInfo.transDate.length); //交易日期
//                nDataLen += mClientInfo.transDate.length;
//
//                ucData[nDataLen] = mClientInfo.transTypeValue;
//                nDataLen += 1;
//
//                System.arraycopy(mClientInfo.unpredictNum, 0, ucData, nDataLen, mClientInfo.unpredictNum.length);
//                nDataLen += mClientInfo.unpredictNum.length;
//
//                System.arraycopy(mClientInfo.AIP, 0, ucData, nDataLen, mClientInfo.AIP.length);
//                nDataLen += mClientInfo.AIP.length;
//
//                System.arraycopy(mClientInfo.ATC, 0, ucData, nDataLen, mClientInfo.ATC.length);
//                nDataLen += mClientInfo.ATC.length;
//
//
//                if (issuerAppData[7] == 0x04) { //算法标识 04 使用国密算法
//
//                    //SM4MAC(ucData, nDataLen, ISO_PADDING_2, CD_SM4_MAC_16, NULL, ucACKey, ucARQC);
//                } else // 国际算法
//                {
//                    if (EMVMode == TRANS_MODE_PBOC || EMVMode == TRANS_MODE_VISA) {
//                        System.arraycopy(mClientInfo.issuAppData, 3, ucData, nDataLen, 4);
//                        nDataLen += 4;
//                        ARQC = DES2.TripleDES.mac(ACKey, ACKey.length, ucData, nDataLen, iv, 8, TripleDES.PADDING_80);
//                    } else if (EMVMode == TRANS_MODE_MASTERCARD) {
//                        System.arraycopy(mClientInfo.issuAppData, 2, ucData, nDataLen, 6);
//                        nDataLen += 6;
//                        ARQC = DES2.TripleDES.mac(ACKey, ACKey.length, ucData, nDataLen, iv, 8, TripleDES.PADDING_80);
//                    } else {
//                        // Nothing done
//                    }
//                }
//            } else {
//                nDataLen = 0;
//                // 授权金额
//                System.arraycopy(mClientInfo.amtAuthNum, 0, ucData, nDataLen, mClientInfo.amtAuthNum.length);
//                nDataLen += mClientInfo.amtAuthNum.length;
//                // 其他金额
//                System.arraycopy(mClientInfo.amtOtherNum, 0, ucData, nDataLen, mClientInfo.amtOtherNum.length);
//                nDataLen += mClientInfo.amtOtherNum.length;
//                // 终端国家代码
//                System.arraycopy(mClientInfo.countryCode, 0, ucData, nDataLen, mClientInfo.countryCode.length);
//                nDataLen += mClientInfo.countryCode.length;
//                // 终端验证结果
//                System.arraycopy(mClientInfo.TVR, 0, ucData, nDataLen, mClientInfo.TVR.length);
//                nDataLen += mClientInfo.TVR.length;
//                // 交易货币代码
//                System.arraycopy(mClientInfo.transCurcyCode, 0, ucData, nDataLen, mClientInfo.transCurcyCode.length);
//                nDataLen += mClientInfo.transCurcyCode.length;
//                // 交易日期
//                System.arraycopy(mClientInfo.transDate, 0, ucData, nDataLen, mClientInfo.transDate.length); //交易日期
//                nDataLen += mClientInfo.transDate.length;
//                // 交易类型
//                ucData[nDataLen] = mClientInfo.transTypeValue;
//                nDataLen += 1;
//                // 不可预知数
//                System.arraycopy(mClientInfo.unpredictNum, 0, ucData, nDataLen, mClientInfo.unpredictNum.length);
//                nDataLen += mClientInfo.unpredictNum.length;
//                // 应用交互特征
//                System.arraycopy(mClientInfo.AIP, 0, ucData, nDataLen, mClientInfo.AIP.length);
//                nDataLen += mClientInfo.AIP.length;
//                // 应用交易计数器
//                System.arraycopy(mClientInfo.ATC, 0, ucData, nDataLen, mClientInfo.ATC.length);
//                nDataLen += mClientInfo.ATC.length;
//                // 卡片验证结果（可选）
////                System.arraycopy(mClientInfo.issuAppData, 3, ucData, nDataLen, 4);
////                nDataLen += 4;
//
//                if ((EMVMode == TRANS_MODE_PBOC) && issuerAppData[7] == (byte) 0x04)//算法标识 04 使用国密算法
//                {
//                    //SM4MAC(ucData, nDataLen, ISO_PADDING_2, CD_SM4_MAC_16, NULL, ucACKey, ucARQC);
//                } else {
//                    byte[] msg = Arrays.copyOf(ucData, nDataLen);
//                    Log.d(Config.APP_ID, "计算应用密文,AC Key=" + Util.toHexString(ACKey) + ",Input Data" + Util.toHexString(msg));
//                    ARQC = DES2.TripleDES.mac(ACKey, ACKey.length, msg, msg.length, iv, 8, TripleDES.PADDING_80);
//                }
//            }

            if (!Arrays.equals(ARQC, mClientInfo.appCrypt)) {
                throw new ErrMessage("ARQC不匹配" + ",Server=" + Util.toHexString(ARQC) + ",Card=" + Util.toHexString(mClientInfo.appCrypt));
            }
//            if (mClientInfo.cryptInfo == 0x80) {
            //验证ARQC通过，保存密钥
//                memcpy(m_ucACKey, ucTACKey, 16);
//                memcpy(m_ucMACKey, ucTMACKey, 16);
//                memcpy(m_ucENCKey, ucTENCKey, 16);
//                memcpy(m_ucSACKey, ucACKey, 16);
//                memcpy(m_ucSMACKey, ucMACKey, 16);
//                memcpy(m_ucSENCKey, ucENCKey, 16);
//                m_bVerifyAC = TRUE;
//            }
            //验证ARQC通过，保存密钥
            ACSessionKey = ACKey;
            MACSessionKey = MACKey;
            ENCSessionKey = ENCKey;
            // 计算ARPC
            ARPC = new byte[8];
            byte[] arcBuf = new byte[8];
            Arrays.fill(arcBuf, (byte) 0);
            System.arraycopy(ARC, 0, arcBuf, 0, 2);
            byte[] tmp = new byte[8];
            for (int i = 0; i < 8; i++) {
                tmp[i] = (byte) (mClientInfo.appCrypt[i] ^ arcBuf[i]);
            }

            if (issuerAppData[7] == 0x04)//算法标识 04 使用国密算法
            {
                //SM4Encrypt(ucARPCSrc, 8, CD_SM4_ECB, NULL, ucACKey, ucARPC, & nDataLen);
            } else { // 国际算法
                if (EMVMode == SPEC.TRANS_MODE_MASTERCARD) {
                    ARPC = DES2.TripleDES.encrypt(TACKey, tmp, null, DES2.TripleDES.DESEDE_ECB_NOPADDING);
                } else if (EMVMode == SPEC.TRANS_MODE_PBOC || EMVMode == SPEC.TRANS_MODE_VISA) {
                    ARPC = DES2.TripleDES.encrypt(ACKey, tmp, null, DES2.TripleDES.DESEDE_ECB_NOPADDING);
                } else {
                    throw new ErrMessage("EMVMode非法导致生成ARPC失败,EMVMode=" + EMVMode);
                }
            }
            byte[] result = new byte[10];
            System.arraycopy(ARC, 0, result, 0, 2);
            System.arraycopy(ARPC, 0, result, 2, 8);

            return result;
        }

        return null;

    }

    // trace=0,debug=1,info=2,warn=3,error=4,fatal=5
    void Log(int level, String msg) {
        switch (level) {
            case 0: // trace

                break;
            case 1: // debug
                Log.d(Config.APP_ID, msg);
                break;
            case 2: // info
                Log.i(Config.APP_ID, msg);
                break;
            case 3: // warn
                Log.w(Config.APP_ID, msg);
                break;
            case 4: // error
                Log.e(Config.APP_ID, msg);
                break;
            case 5: // fatal
                break;
            default:
                break;
        }
    }
}
