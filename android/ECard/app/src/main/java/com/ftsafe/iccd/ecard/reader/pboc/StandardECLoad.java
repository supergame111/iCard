package com.ftsafe.iccd.ecard.reader.pboc;

import android.support.annotation.NonNull;
import android.util.Log;

import com.ftsafe.iccd.ecard.Config;
import com.ftsafe.iccd.ecard.SPEC;
import com.ftsafe.iccd.ecard.Terminal;
import com.ftsafe.iccd.ecard.bean.Application;
import com.ftsafe.iccd.ecard.bean.Card;
import com.ftsafe.iccd.ecard.backend.Server;
import com.ftsafe.iccd.ecard.bean.ClientInfo;
import com.ftsafe.iccd.ecard.pojo.PbocTag;
import com.ftsafe.iccd.ecard.ui.activities.StandardECLoadActivity;

import java.io.IOException;
import java.nio.ByteBuffer;
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
public class StandardECLoad extends StandardECash {

    @Override
    protected boolean reset(Iso7816.StdTag tag) throws IOException {
        Iso7816.Response rsp = tag.selectByName(StandardECash.DFN_PPSE);
        if (!rsp.isOkey())
            return false;

        Iso7816.BerTLV.extractPrimitives(topTLVs, rsp);
        return true;
    }

    final static String AC_KEY = "7510E39445A8C42ADA89CDA10E700D29";
    final static String MAC_KEY = "3E89866D8604DF85F1C41AB3AD7AF1DA";
    final static String ENC_KEY = "6E2C947A9B01B3614025F467C4F77C01";

    private byte[] script = null;

    @Override
    protected HINT readCard(Iso7816.StdTag tag, Card card) throws IOException {
        final ArrayList<Iso7816.ID> aids = getApplicationIds(tag);

        for (Iso7816.ID aid : aids) {
            // initial app
            final Application app = createApplication();
            // initial BerTLV
            final Iso7816.BerHouse berHouse = new Iso7816.BerHouse();
            Iso7816.Response rsp;
            try {
            /*--------------------------------------------------------------*/
                // 应用选择
            /*--------------------------------------------------------------*/
                Log.d(Config.APP_ID, "选择应用");
                // SELECT
                rsp = tag.selectByName(aid.getBytes());
                if (rsp.isOkey() == false)
                    continue;
                // collect info
                Iso7816.BerTLV.extractPrimitives(berHouse, rsp);

                Log.d(Config.APP_ID, "选择 " + aid + " 应用完成");

                // 初始化终端交易参数
                // 参数顺序：9F7A,9F66,9C,9F02,9F03,DF60,DF69
                Terminal terminal = initTerminal();

                Log.d(Config.APP_ID, "初始化终端参数完成");
            /*--------------------------------------------------------------*/
                // 应用初始化
            /*--------------------------------------------------------------*/
                byte[] _80 = initialApp(tag, berHouse, terminal);

                Log.d(Config.APP_ID, "应用初始化完成");
            /*--------------------------------------------------------------*/
                // 读取应用数据
            /*--------------------------------------------------------------*/
                readApplicationRecord(tag, berHouse, _80);
                Log.d(Config.APP_ID, "读取应用记录完成");

            /*--------------------------------------------------------------*/
                // 脱机数据认证
            /*--------------------------------------------------------------*/
                offlineDataAuthenticate(tag, berHouse, terminal);
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
                // 终端决定交易方式
                TransMethod transMethod = termActionAnalyze(berHouse, terminal);

                Log.d(Config.APP_ID, "终端决定交易类型:" + transMethod);
                rsp = gacProcess(berHouse, terminal, tag, GAC_1, transMethod);
                if (!rsp.isOkey())
                    throw new ErrMessage("GAC异常响应:" + rsp.getSw12String());
                Log.d(Config.APP_ID, "第" + GAC_1 + "GAC完成");
                BerTLV.extractPrimitives(berHouse, rsp);

                Log.d(Config.APP_ID, "终端行为分析完成");

            /*--------------------------------------------------------------*/
                // 联机处理
            /*--------------------------------------------------------------*/
                byte[] result = null;
                if (transMethod == TransMethod.TRANS_ONLINE || transMethod == TransMethod.TRANS_OFFLINE) {
                    // 卡片决定交易方式
                    transMethod = onlineProcess(berHouse, terminal, rsp);
                    if (transMethod == TransMethod.TRANS_ONLINE)
                        result = onlineTransaction(berHouse, terminal);
                    else {
                        // 拒绝交易
                        transMethod = TransMethod.TRANS_DENIAL;
                    }

                } else if (transMethod == TransMethod.TRANS_OFFLINE) {
                    // 脱机
                } else if (transMethod == TransMethod.TRANS_DENIAL) {
                    // 脱机拒绝
                    Log.d(Config.APP_ID, "脱机拒绝" + transMethod);
                } else {
                    throw new ErrMessage("联机处理异常,交易方式码=" + transMethod);
                }
                Log.d(Config.APP_ID, "交易完成");
                Log.d(Config.APP_ID, "联机处理完成");
            /*--------------------------------------------------------------*/
                // 发卡行认证
            /*--------------------------------------------------------------*/
                if (result != null && result.length == 10) {
                    byte[] arc = Arrays.copyOfRange(result, 0, 2);
                    Log.d(Config.APP_ID, "ARC=" + Util.toHexString(arc));
                    byte[] arpc = Arrays.copyOfRange(result, 2, 10);
                    Log.d(Config.APP_ID, "ARPC=" + Util.toHexString(arpc));
                    // 发卡行认证
                    rsp = issuerVerify(tag, berHouse, terminal, arc, arpc);
                    if (!rsp.isOkey())
                        throw new ErrMessage("发卡行认证异常码:" + rsp.getSw12String());
                    Log.d(Config.APP_ID, "发卡行认证完成");
                }
            /*--------------------------------------------------------------*/
                // 交易结束
            /*--------------------------------------------------------------*/
                rsp = gacProcess(berHouse, terminal, tag, GAC_2, TransMethod.TRANS_OFFLINE);
                if (!rsp.isOkey())
                    throw new ErrMessage("GAC异常响应:" + rsp.getSw12String());
                Log.d(Config.APP_ID, "第" + GAC_2 + "次GAC完成");
            /*--------------------------------------------------------------*/
                // 发卡行脚本处理
            /*--------------------------------------------------------------*/
                if (script != null) {
                    ByteBuffer[] buf = new ByteBuffer[]{ByteBuffer.wrap(script)};
                    int ret = issuerScriptProcess(tag, terminal, (byte) 0x72, buf);
                    if (ret != 0) {
                        throw new ErrMessage("脚本执行错误");
                    }
                } else {
                    throw new ErrMessage("没有发卡行脚本");
                }
                Log.d(Config.APP_ID, "发卡行脚本处理完成");


            } catch (ErrMessage message) {
                card.setProperty(SPEC.PROP.WARN, message.getMessage());
            } finally {
            /*--------------------------------------------------------------*/
                // build result
            /*--------------------------------------------------------------*/
                // 读EC余额
                rsp = tag.getData(PbocTag.ECASH);
                // collect info
                Iso7816.BerTLV.extractPrimitives(berHouse, rsp);
                Log.d(Config.APP_ID, "读取电子现金完成");

                parseInfo(app, berHouse);

                card.addApplication(app);
            }
        }


        return card.isUnknownCard() ? HINT.RESETANDGONEXT : HINT.STOP;
    }

    private byte[] onlineTransaction(Iso7816.BerHouse berHouse, Terminal terminal) throws ErrMessage {
        // ON LINE
        Log.d(Config.APP_ID, "执行联机交易");
        byte[] result = null;
        byte[] ac = Util.toBytes(AC_KEY);
        byte[] mac = Util.toBytes(MAC_KEY);
        byte[] enc = Util.toBytes(ENC_KEY);
        try {
            Iso7816.BerHouse params = new Iso7816.BerHouse();
            // build params
            buildParmas(params, berHouse, terminal);
            Server server = new Server();
            // 校验ARQC
            result = server.verifyARQC(params, ac, mac, enc, 1, false);
            // 生成脚本
            script = server.putData();
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
        return result;
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
                Log.d(Config.APP_ID, "构建参数:TAG=" + Util.toHexString(berT.getBytes()) + ",Value=" + Util.toHexString(value));
                tlv = new BerTLV(berT, new Iso7816.BerL(value.length), new Iso7816.BerV(value));
                out.add(tlv);
            }
        }
    }

    @NonNull
    private Terminal initTerminal() throws ErrMessage {

        Terminal terminal = Terminal.getTermianl();
        Log.d(Config.APP_ID, "初始化终端参数");
        final String amt = StandardECLoadActivity.AMT;
        if (amt == null || amt.length() != 12)
            throw new ErrMessage("授权金额格式错误:" + amt);
        // 授权金额
        byte[] amtAuthNum = Util.toBytes(amt);
        Log(1, "授权金额" + Util.toHexString(amtAuthNum));

        // 交易类型 9C
        terminal.setTransType((byte) 0x00);
        // 授权金额 9F02
        terminal.setAmtAuthNum(amtAuthNum);
        // 其他授权金额 9F03
        terminal.setAmtOtherNum(new byte[]{0, 0, 0, 0, 0, 0});
        // 交易货币代码 5F2A
        terminal.setTransCurcyCode(new byte[]{(byte) 0x01, (byte) 0x56});
        // 终端交易属性 9F66
        terminal.setTermTransAtr(new byte[]{(byte) 0x66, (byte) 0x80, (byte) 0, (byte) 0x80});
        // 终端性能 9F33
        terminal.setTermCapab(new byte[]{(byte) 0xE0, (byte) 0xE8, (byte) 0xE8});

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
