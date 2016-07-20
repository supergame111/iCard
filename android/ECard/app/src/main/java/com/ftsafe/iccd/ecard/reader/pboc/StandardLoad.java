package com.ftsafe.iccd.ecard.reader.pboc;

import android.util.Log;

import com.ftsafe.iccd.ecard.Config;
import com.ftsafe.iccd.ecard.SPEC;
import com.ftsafe.iccd.ecard.bean.Card;
import com.ftsafe.iccd.ecard.reader.pboc.*;
import com.ftsafe.iccd.ecard.reader.pboc.StandardPboc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import ftsafe.common.BaseFunction;
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

            final Iso7816.BerHouse berHouse = new Iso7816.BerHouse();

            // collect info
            Iso7816.BerTLV.extractPrimitives(berHouse, rsp);
            // GET DATA 9F38
//            subTLVs.add(BerTLV.read(tag.getData((short) 0x9F36)));
            /*--------------------------------------------------------------*/
            // 应用初始化
            /*--------------------------------------------------------------*/
            // GPO
            byte[] pdol = buildPDOL(berHouse);
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
            dealLimit(berHouse);
            /*--------------------------------------------------------------*/
            // GENERATE AC
            /*--------------------------------------------------------------*/

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

    private static void buildPDO(ByteBuffer out, int len, byte... val) {
        final int n = Math.min((val != null) ? val.length : 0, len);
        int i = 0;
        while (i < n) out.put(val[i++]);
        while (i++ < len) out.put((byte) 0);
    }

    private static byte[] buildPDOL(Iso7816.BerHouse tlvs) {
        final ByteBuffer buff = ByteBuffer.allocate(64);
        buff.put((byte) 0x83).put((byte) 0x00);
        try {
            final byte[] pdol = tlvs.findFirst((short) 0x9F38).v.getBytes();
            //Log.e(Config.APP_ID, "9F38="+Util.toHexString(pdol));
            ArrayList<BerTLV> list = BerTLV.extractOptionList(pdol);
            for (Iso7816.BerTLV tlv : list) {
                final int tag = tlv.t.toInt();
                final int len = tlv.l.toInt();
                switch (tag) {
                    case 0x9F66:
                        // 终端交易属性
                        buildPDO(buff, len, (byte) 0x48, (byte) 0x00, (byte) 0x00, (byte) 0x00);
                        break;
                    case 0x9F02:
                        // 授权金额
                        buildPDO(buff, len, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00);
                        break;
                    case 0x9F03:
                        // 其它金额
                        buildPDO(buff, len, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00);
                        break;
                    case 0x9F1A:
                        // 终端国家代码
                        buildPDO(buff, len, (byte) 0x01, (byte) 0x56);
                        break;
                    case 0x9F37:
                        // 不可预知数
                        byte[] rand = Util.getRandom(4);
                        buildPDO(buff, len, rand);
                        break;
                    case 0x5F2A:
                        // 交易货币代码
                        buildPDO(buff, len, (byte) 0x01, (byte) 0x56);
                        break;
                    case 0x95:
                        // 终端验证结果
                        buildPDO(buff, len, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00);
                        break;
                    case 0x9A:
                        // 交易日期
                        buildPDO(buff, len, Util.getSysDate(3));
                        break;
                    case 0x9C:
                        // 交易类型
                        buildPDO(buff, len, (byte) 0x00);
                        break;
                    case 0xDF60:
                        // CAPP交易指示位
                        buildPDO(buff, len, (byte) 0x00);
                        break;
                    default:
                        throw null;
                }
            } // 更新数据长度
            buff.put(1, (byte) (buff.position() - 2));
        } catch (Exception e) {
            buff.position(2);
        }
        return Arrays.copyOfRange(buff.array(), 0, buff.position());
    }

}
