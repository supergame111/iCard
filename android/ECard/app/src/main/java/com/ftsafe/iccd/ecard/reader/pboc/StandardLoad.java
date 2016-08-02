package com.ftsafe.iccd.ecard.reader.pboc;

import android.support.annotation.NonNull;
import android.util.Log;

import com.ftsafe.iccd.ecard.Config;
import com.ftsafe.iccd.ecard.SPEC;
import com.ftsafe.iccd.ecard.Terminal;
import com.ftsafe.iccd.ecard.bean.Card;
import com.ftsafe.iccd.ecard.pojo.PbocTag;
import com.ftsafe.iccd.ecard.ui.activities.LoadActivity;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import ftsafe.common.ErrMessage;
import ftsafe.common.Util;
import ftsafe.reader.tech.Iso7816;
import ftsafe.reader.tech.Iso7816.BerTLV;

/**
 * Created by qingyuan on 16-7-14.
 */
public class StandardLoad extends com.ftsafe.iccd.ecard.reader.pboc.StandardPboc {

    @Override
    protected SPEC.APP getApplicationId() {
        return SPEC.APP.CREDIT;
    }

    @Override
    protected boolean reset(Iso7816.StdTag tag) throws IOException {
        Iso7816.Response rsp = tag.selectByName(StandardECash.DFN_PPSE);
        if (!rsp.isOkey())
            return false;

        Iso7816.BerTLV.extractPrimitives(topTLVs, rsp);
        return true;
    }

    String acStr = "7510E39445A8C42ADA89CDA10E700D29";
    String macStr = "3E89866D8604DF85F1C41AB3AD7AF1DA";
    String encStr = "6E2C947A9B01B3614025F467C4F77C01";

    @Override
    protected HINT readCard(Iso7816.StdTag tag, Card card) throws IOException, ErrMessage {
        final ArrayList<Iso7816.ID> aids = getApplicationIds(tag);

        for (Iso7816.ID aid : aids) {
            /*--------------------------------------------------------------*/
            // 应用选择
            /*--------------------------------------------------------------*/
            Log.d(Config.APP_ID, "选择应用");
            // SELECT
            Iso7816.Response rsp = tag.selectByName(aid.getBytes());
            if (rsp.isOkey() == false)
                continue;
            // initial BerTLV
            final Iso7816.BerHouse berHouse = new Iso7816.BerHouse();
            // collect info
            Iso7816.BerTLV.extractPrimitives(berHouse, rsp);

            Log.d(Config.APP_ID, "选择 " + aid + " 应用完成");

            // 初始化终端交易参数
            // 参数顺序：9F7A,9F66,9C,9F02,9F03,DF60,DF69
            Terminal terminal = null;

            terminal = initTerminal();
            Log.d(Config.APP_ID, "初始化终端参数完成");
            /*--------------------------------------------------------------*/
            // 应用初始化
            /*--------------------------------------------------------------*/
            Log.d(Config.APP_ID, "应用初始化");
            // GPO
            byte[] pdol = buildPDOL(berHouse, terminal);

            Log.d(Config.APP_ID, "PDOL=" + Util.toHexString(pdol));

            rsp = tag.getProcessingOptions(pdol);
            if (rsp.isOkey() == false)
                throw new IOException("GPO失败");

            BerTLV.extractPrimitives(berHouse, rsp);

            Log.d(Config.APP_ID, "GPO完成");
            Log.d(Config.APP_ID, "应用初始化完成");
            /*--------------------------------------------------------------*/
            // 读取应用数据
            /*--------------------------------------------------------------*/
            readApplicationRecord(tag, berHouse);
            Log.d(Config.APP_ID, "读取应用记录完成");
            /*--------------------------------------------------------------*/
            // 脱机数据认证
            /*--------------------------------------------------------------*/
            offLineDataAuthenticate(tag, berHouse, terminal);
            Log.d(Config.APP_ID, "脱机数据认证完成");
            /*--------------------------------------------------------------*/
            // 处理限制
            /*--------------------------------------------------------------*/
            processRestrict(berHouse, terminal);
            Log.d(Config.APP_ID, "处理限制完成");
            /*--------------------------------------------------------------*/
            // 持卡人验证
            /*--------------------------------------------------------------*/
            cardHolderVerify(berHouse, terminal);
            Log.d(Config.APP_ID, "持卡人验证完成");
            /*--------------------------------------------------------------*/
            // 终端风险管理
            /*--------------------------------------------------------------*/
            rsp = termRiskManage(tag, berHouse, terminal);
            if (!rsp.isOkey()) {
                throw new ErrMessage("终端风险管理异常响应:" + rsp.getSw12String());
            }
            Log.d(Config.APP_ID, "终端风险管理完成");
            /*--------------------------------------------------------------*/
            // 终端行为分析
            /*--------------------------------------------------------------*/
            int transType = termActionAnalyze(berHouse, terminal);
            Log.e(Config.APP_ID, "终端决定交易类型:" + transType);
            rsp = gacProcess(berHouse, terminal, tag, GAC_1, transType);
            if (!rsp.isOkey())
                throw new ErrMessage("GAC异常响应:" + rsp.getSw12String());
            Log.d(Config.APP_ID, "第" + GAC_1 + "GAC完成");
            BerTLV.extractPrimitives(berHouse, rsp);
            Log.d(Config.APP_ID, "终端行为分析完成");
            /*--------------------------------------------------------------*/
            // 联机交易
            /*--------------------------------------------------------------*/
            byte[] result = null;

            transType = onLineProcess(berHouse, terminal, rsp);
            if (transType == TRANS_ONLINE) {
                // ON LINE
                Log.d(Config.APP_ID, "执行联机交易" + transType);
                byte[] ac = Util.toBytes(acStr);
                byte[] mac = Util.toBytes(macStr);
                byte[] enc = Util.toBytes(encStr);
                try {
                    Iso7816.BerHouse params = new Iso7816.BerHouse();
                    // build params
                    buildParmas(params, berHouse, terminal);
                    result = verifyARQC(params, ac, mac, enc, false);
                } catch (IllegalBlockSizeException e) {
                    e.printStackTrace();
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                } catch (BadPaddingException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (NoSuchPaddingException e) {
                    e.printStackTrace();
                }
            } else if (transType == TRANS_OFFLINE) {
                // 脱机
                Log.d(Config.APP_ID, "执行脱机交易" + transType);
            } else if (transType == TRANS_DENIAL) {
                // 脱机拒绝
                Log.d(Config.APP_ID, "脱机拒绝" + transType);
            } else {
                throw new ErrMessage("联机处理异常:" + transType);
            }
            Log.d(Config.APP_ID, "联机处理完成");
            /*--------------------------------------------------------------*/
            // 发卡行认证
            /*--------------------------------------------------------------*/
            if (result != null && result.length == 10) {
                byte[] arc = Arrays.copyOfRange(result, 0, 3);
                Log.e(Config.APP_ID, "ARC=" + Util.toHexString(arc));
                byte[] arpc = Arrays.copyOfRange(result, 2, 10);
                Log.e(Config.APP_ID, "ARPC=" + Util.toHexString(arpc));
                // 发卡行认证
                rsp = issuerVerify(tag, berHouse, terminal, arc, arpc);
                if (!rsp.isOkey())
                    throw new ErrMessage("发卡行认证异常码:" + rsp.getSw12String());
                Log.d(Config.APP_ID, "发卡行认证完成");
            }
            /*--------------------------------------------------------------*/
            // 交易结束
            /*--------------------------------------------------------------*/
            rsp = gacProcess(berHouse, terminal, tag, GAC_2, transType);
            if (!rsp.isOkey())
                throw new ErrMessage("GAC异常响应:" + rsp.getSw12String());
            Log.d(Config.APP_ID, "第" + GAC_1 + "GAC完成");
            /*--------------------------------------------------------------*/
            // 发卡行脚本处理
            /*--------------------------------------------------------------*/

        }
        return card.isUnknownCard() ? HINT.RESETANDGONEXT : HINT.STOP;
    }

    private void buildParmas(Iso7816.BerHouse out, Iso7816.BerHouse berHouse, Terminal terminal) {
        for (int i = 0; i < ClientInfo.TAG_PARAMS.length; i++) {
            short t = ClientInfo.TAG_PARAMS[i];
            byte[] tmp = Util.toBytes(t);

            Iso7816.BerT berT;
            if (tmp[0] == (byte) 0xFF || tmp[0] == (byte) 0x00) {
                berT = new Iso7816.BerT(tmp[1]);
            } else {
                berT = new Iso7816.BerT(t);
            }

            byte[] value = null;
            BerTLV tlv = berHouse.findFirst(berT);
            if (tlv != null) {
                value = tlv.v.getBytes();
            } else {
                value = terminal.getValue(tmp);
            }

            if (value != null) {
                tlv = new BerTLV(berT, new Iso7816.BerL(value.length), new Iso7816.BerV(value));
                out.add(tlv);
            }
        }
    }

    public static String amt = LoadActivity.AMT;

    @NonNull
    private Terminal initTerminal() throws ErrMessage {

        Log.d(Config.APP_ID, "初始化终端参数");
        byte vlpIndicator = 0;
        byte[] termTransAtr = {0x48, 0, 0, 0};
        byte transType = 0;
        if (amt == null || amt.length() != 12)
            throw new ErrMessage("授权金额格式错误:" + amt);
        // 授权金额
        byte[] amtAuthNum = Util.toBytes(amt);
        // 其他授权金额
        byte[] amtOtherNum = {0, 0, 0, 0, 0, 0};
        // capp标识
        byte cappFlag = 0;
        // 国密支持
        byte smAlgSupp = 0;
        // 国家代码
        byte[] countryCode = {0x01, 0x56};
        // TVR 终端验证结果
        byte[] tvr = {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};

        Terminal terminal = new Terminal(vlpIndicator, termTransAtr, transType, amtAuthNum, amtOtherNum, cappFlag, smAlgSupp, countryCode, tvr);

        return terminal;

    }

    private final Iso7816.BerHouse topTLVs = new Iso7816.BerHouse();

    private ArrayList<Iso7816.ID> getApplicationIds(Iso7816.StdTag tag) throws IOException {

        final ArrayList<Iso7816.ID> ret = new ArrayList<Iso7816.ID>();

        // try to read DDF
        Iso7816.BerTLV sfi = topTLVs.findFirst(Iso7816.BerT.CLASS_SFI);
        if (sfi != null && sfi.length() == 1) {
            final int SFI = sfi.v.toInt();
            Iso7816.Response r = tag.readRecord(SFI, 1);
            for (int p = 2; r.isOkey(); ++p) {
                Iso7816.BerTLV.extractPrimitives(topTLVs, r);
                r = tag.readRecord(SFI, p);
            }
        }

        // add extracted
        ArrayList<Iso7816.BerTLV> aids = topTLVs.findAll(Iso7816.BerT.CLASS_AID);
        if (aids != null) {
            for (Iso7816.BerTLV aid : aids)
                ret.add(new Iso7816.ID(aid.v.getBytes()));
        }

        // use default list
        if (ret.isEmpty()) {
            ret.add(new Iso7816.ID(StandardECash.AID_DEBIT));
            ret.add(new Iso7816.ID(StandardECash.AID_CREDIT));
            ret.add(new Iso7816.ID(StandardECash.AID_QUASI_CREDIT));
        }

        return ret;
    }

}
