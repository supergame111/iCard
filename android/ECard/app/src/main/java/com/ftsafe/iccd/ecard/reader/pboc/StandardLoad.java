package com.ftsafe.iccd.ecard.reader.pboc;

import android.support.annotation.NonNull;
import android.util.Log;

import com.ftsafe.iccd.ecard.Config;
import com.ftsafe.iccd.ecard.SPEC;
import com.ftsafe.iccd.ecard.Terminal;
import com.ftsafe.iccd.ecard.bean.Card;

import java.io.IOException;
import java.util.ArrayList;

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

    @Override
    protected HINT readCard(Iso7816.StdTag tag, Card card) throws IOException {
        final ArrayList<Iso7816.ID> aids = getApplicationIds(tag);

        for (Iso7816.ID aid : aids) {
            /*--------------------------------------------------------------*/
            // 应用选择
            /*--------------------------------------------------------------*/
            // SELECT
            Iso7816.Response rsp = tag.selectByName(aid.getBytes());
            if (rsp.isOkey() == false)
                continue;
            Log.d(Config.APP_ID, "选择 " + aid + " 应用完成");

            // initial BerTLV
            final Iso7816.BerHouse berHouse = new Iso7816.BerHouse();
            // initialize Terminal
            Terminal terminal = initTerminal();

            // collect info
            Iso7816.BerTLV.extractPrimitives(berHouse, rsp);
            // GET DATA 9F38
//            subTLVs.add(BerTLV.read(tag.getData((short) 0x9F36)));
            /*--------------------------------------------------------------*/
            // 应用初始化
            /*--------------------------------------------------------------*/
            // GPO
            byte[] pdol = buildPDOL(berHouse, terminal);
            Log.e(Config.APP_ID, "PDOL=" + Util.toHexString(pdol));
            rsp = tag.getProcessingOptions(pdol);
            if (rsp.isOkey() == false)
                throw new IOException("GPO失败");
            BerTLV.extractPrimitives(berHouse, rsp);
            Log.d(Config.APP_ID, "GPO完成");
            /*--------------------------------------------------------------*/
            // 读取应用数据
            /*--------------------------------------------------------------*/
            readApplicationRecord(tag, berHouse);
            Log.d(Config.APP_ID, "读应用记录完成");
            /*--------------------------------------------------------------*/
            // 脱机数据认证
            /*--------------------------------------------------------------*/
//            offLineDataAuthenticate(false);

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
            // EXTERNAL AUTHENTICATE
            /*--------------------------------------------------------------*/

            /*--------------------------------------------------------------*/
            // GENERATE AC
            /*--------------------------------------------------------------*/

            /*--------------------------------------------------------------*/
            // PUT DATA 9F79
            /*--------------------------------------------------------------*/


        }
        return card.isUnknownCard() ? HINT.RESETANDGONEXT : HINT.STOP;
    }

    @NonNull
    private Terminal initTerminal() {
        Log.d(Config.APP_ID, "初始化终端参数");
        byte vlpIndicator = 0;
        byte[] termTransAtr = {0x48, 0, 0, 0};
        byte transType = 0;
        // 授权金额
        byte[] amtAuthNum = {0, 0, 0, 0, 0, 0};
        // 其他授权金额
        byte[] amtOtherNum = {0, 0, 0, 0, 0, 0};
        byte cappFlag = 0;
        byte smAlgSupp = 0;
        Terminal terminal = new Terminal(vlpIndicator, termTransAtr, transType, amtAuthNum, amtOtherNum, cappFlag, smAlgSupp);
        byte[] countryCode = {0x01, 0x56};
        byte[] tvr = {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
        // 国家代码
        terminal.setCountryCode(countryCode);
        // 交易货币代码
        terminal.setTransCurcyCode(countryCode);
        // FIXME：终端验证结果
        terminal.setTVR(tvr);
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
