package com.ftsafe.iccd.ecard.reader.pboc;

import android.support.annotation.NonNull;
import android.util.Log;

import com.ftsafe.iccd.ecard.Config;
import com.ftsafe.iccd.ecard.SPEC;
import com.ftsafe.iccd.ecard.Terminal;
import com.ftsafe.iccd.ecard.bean.Application;
import com.ftsafe.iccd.ecard.bean.Card;
import com.ftsafe.iccd.ecard.bean.ClientInfo;
import com.ftsafe.iccd.ecard.pojo.PbocTag;
import com.ftsafe.iccd.ecard.ui.activities.StandardECTransactionActivity;

import java.io.IOException;
import java.util.ArrayList;

import ftsafe.common.ErrMessage;
import ftsafe.common.Util;
import ftsafe.reader.tech.Iso7816;

/**
 * Created by ft on 2016/8/4.
 */
public class StandardECPay extends StandardECash {

    @Override
    protected boolean reset(Iso7816.StdTag tag) throws IOException {
        Iso7816.Response rsp = tag.selectByName(StandardECash.DFN_PPSE);
        if (!rsp.isOkey())
            return false;

        Iso7816.BerTLV.extractPrimitives(topTLVs, rsp);
        return true;
    }

    private byte[] script = null;

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

    @NonNull
    private Terminal initTerminal() throws ErrMessage {

        Terminal terminal = Terminal.getTermianl();
        final String amt = StandardECTransactionActivity.AMT;
        Log.d(Config.APP_ID, "初始化终端参数");
        if (amt == null || amt.length() != 12)
            throw new ErrMessage("授权金额格式错误:" + amt);
        // 授权金额
        byte[] amtAuthNum = Util.toBytes(amt);

        // 交易类型 9C
        terminal.setTransType((byte) 0x51);
        // 授权金额 9F02
        terminal.setAmtAuthNum(amtAuthNum);
        // 其他授权金额 9F03
        terminal.setAmtOtherNum(new byte[]{0, 0, 0, 0, 0, 0});
        // 交易货币代码 5F2A
        terminal.setTransCurcyCode(new byte[]{(byte) 0x01, (byte) 0x56});
        // 终端交易属性 9F66
        terminal.setTermTransAtr(new byte[]{(byte) 0x28, (byte) 0x00, (byte) 0x00, (byte) 0x80});
        // 终端性能 9F33
        terminal.setTermCapab(new byte[]{(byte) 0xE0, (byte) 0xE8, (byte) 0xE8});

        return terminal;

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
            Iso7816.BerTLV tlv = berHouse.findFirst(berT);
            if (tlv != null) {
                value = tlv.v.getBytes();
            } else {
                value = terminal.getValue(tmp);
            }

            if (value != null) {
                Log.d(Config.APP_ID, "构建参数:TAG=" + Util.toHexString(berT.getBytes()) + ",Value=" + Util.toHexString(value));
                tlv = new Iso7816.BerTLV(berT, new Iso7816.BerL(value.length), new Iso7816.BerV(value));
                out.add(tlv);
            }
        }
    }

    TransMethod transMethod;

    @Override
    protected HINT readCard(Iso7816.StdTag tag, Card card) throws IOException {

        final ArrayList<Iso7816.ID> aids = getApplicationIds(tag);

        for (Iso7816.ID aid : aids) {
            // initial app
            final Application app = createApplication();
            // initial BerTLV
            final Iso7816.BerHouse berHouse = new Iso7816.BerHouse();
            try {
            /*--------------------------------------------------------------*/
                // 应用选择
            /*--------------------------------------------------------------*/
                Log.d(Config.APP_ID, "选择应用");
                // SELECT
                Iso7816.Response rsp = tag.selectByName(aid.getBytes());
                if (rsp.isOkey() == false)
                    continue;

                // collect info
                Iso7816.BerTLV.extractPrimitives(berHouse, rsp);

                Log.d(Config.APP_ID, "选择 " + aid + " 应用完成");

                // 初始化终端交易参数
                // 参数顺序：9F7A,9F66,9C,9F02,9F03,DF60,DF69
                Terminal terminal = initTerminal();
                Log.d(Config.APP_ID, "初始化终端参数完成");

                Log.d(Config.APP_ID, "电子现金检查");
                // 读电子现金
                rsp = tag.getData(PbocTag.ECASH);
                if (!rsp.isOkey()) {
                    throw new ErrMessage("读电子现金(9F79)异常码:" + rsp.getSw12String());
                }
                Iso7816.BerTLV tlv_9f79 = Iso7816.BerTLV.read(rsp);
                // 电子现金交易限额
                rsp = tag.getData(PbocTag.ECASH_LIMIT);
                if (!rsp.isOkey()) {
                    throw new ErrMessage("读电子现金单笔交易限额(9F78)异常码:" + rsp.getSw12String());
                }
                berHouse.add(Iso7816.BerTLV.read(rsp));

                float amt = Util.BCDtoInt(terminal.getAmtAuthNum()) / 100.0f;
                Log(1, "授权金额=" + amt);
                float ec = Util.BCDtoInt(tlv_9f79.v.getBytes()) / 100.0f;
                Log(1, "电子现金=" + ec);
                float limit = Util.BCDtoInt(berHouse.findFirst(PbocTag.ECASH_LIMIT).v.getBytes()) / 100.0f;
                Log(1, "电子现金单笔交易限额=" + limit);

                if (amt > limit)
                    throw new ErrMessage("电子现金单笔交易限额" + limit + "元");
                if (amt > ec)
                    throw new ErrMessage("电子现金不足" + amt + "元");

                Log.d(Config.APP_ID, "电子现金检查完成");

            /*--------------------------------------------------------------*/
                // 应用初始化
            /*--------------------------------------------------------------*/
                rsp = initialApp(tag, berHouse, terminal);
                if (!rsp.isOkey())
                    throw new ErrMessage("GPO异常响应码:" + rsp.getSw12String());

                Log.d(Config.APP_ID, "应用初始化完成");

                //应用密文9F26存在，则应为qPBOC交易，完成后直接做DDA
                Iso7816.BerTLV tlv_9f26 = berHouse.findFirst(PbocTag.APP_CRYPTOGRAM);
                Log(1, "9F26=" + Util.toHexString(tlv_9f26.v.getBytes()));
                //发卡行应用数据9F10存在，则应为qPBOC交易，完成后直接做DDA
                Iso7816.BerTLV tlv_9f10 = berHouse.findFirst(PbocTag.ISSUER_APP_DATA);
                Log(1, "9F10=" + Util.toHexString(tlv_9f10.v.getBytes()));
                if (tlv_9f26 != null && tlv_9f10 != null) {
                    int ret = 0;
                    byte[] AIP = berHouse.findFirst(PbocTag.APP_INTERCHANGE_PROFILE).v.getBytes();
                    Log(1, "AIP=" + Util.toHexString(AIP));
                    // 密文类型检查
                    byte algFlg = tlv_9f10.v.getBytes()[4];
                    Log(1, "algFlg=" + Util.toHexString(algFlg));
                    Log(1, "6=" + Util.toHexString((byte) (algFlg & (byte) 0x40)));
                    Log(1, "5=" + Util.toHexString((byte) (algFlg & (byte) 0x10)));
                    // 如果返回 ARQC（发卡行应用数据（标签“9F10”）字节 5 的第 6-5 位=“10”），
                    // 那么终端应将交易联机发送；
                    if ((algFlg & (byte) 0x40) != 0 && (algFlg & (byte) 0x10) == 0) {
                        transMethod = TransMethod.TRANS_ONLINE;
                    }
                    // 如果返回 AAC（发卡行应用数据（标签“9F10”）字节 5 的第 6-5 位=“00”），
                    // 那么终端应拒绝交易；
                    if ((algFlg & (byte) 0x40) == 0 && (algFlg & (byte) 0x10) == 0) {
                        transMethod = TransMethod.TRANS_DENIAL;
                    }
                    // 如果返回 TC（发卡行应用数据（标签“9F10”）字节 5 的第 6-5 位=“01”），
                    // 那么终端应检查终端异常文件（如果存在），
                    // 如果应用 PAN 在终端异常文件中出现，那么终端应脱机拒绝交易；
                    if ((algFlg & (byte) 0x40) == 0 && (algFlg & (byte) 0x10) != 0) {
                        byte[] termCapab = terminal.getTermCapab();
                        // 执行 fDDA
                        if ((AIP[0] & 0x01) == 0 && (AIP[0] & 0x20) != 0 && (termCapab[2] & 0x40) != 0) {
                            //卡片支持DDA但不支持CDA
                            ret = offlineDataAuthenticate(tag, berHouse, terminal);
                            Log.d(Config.APP_ID, "脱机数据认证完成");
                            if (ret != 0) {
                                transMethod = TransMethod.TRANS_DENIAL;
                            }
                            transMethod = TransMethod.TRANS_OFFLINE;
                        } else if ((AIP[0] & 0x01) != 0 && (termCapab[2] & 0x08) == 0) {
                            //qPBOC交易,卡片支持CDA,数据错误
                            transMethod = TransMethod.TRANS_DENIAL;
                        }
                    }
                }

                if (transMethod == TransMethod.TRANS_DENIAL)
                    throw new ErrMessage("拒绝脱机交易");

            /*--------------------------------------------------------------*/
                // 读取应用数据
            /*--------------------------------------------------------------*/
                readApplicationRecord(tag, berHouse);
                Log.d(Config.APP_ID, "读取应用记录完成");

                // 读EC
                rsp = tag.getData(PbocTag.ECASH);
                if (!rsp.isOkey()) {
                    throw new ErrMessage("读电子现金(9F79)异常码:" + rsp.getSw12String());
                }
                berHouse.add(Iso7816.BerTLV.read(rsp));
                Log.d(Config.APP_ID, "读取电子现金完成");
            /*--------------------------------------------------------------*/
                // 脱机数据认证
            /*--------------------------------------------------------------*/
                //offlineDataAuthenticate(tag, berHouse, terminal);
                //Log.d(Config.APP_ID, "脱机数据认证完成");
            /*--------------------------------------------------------------*/
                // 处理限制
            /*--------------------------------------------------------------*/
//            processRestrict(berHouse, terminal);
//            Log.d(Config.APP_ID, "处理限制完成");
            /*--------------------------------------------------------------*/
                // 持卡人验证
            /*--------------------------------------------------------------*/
//            cardHolderVerify(berHouse, terminal);
//            Log.d(Config.APP_ID, "持卡人验证完成");
            /*--------------------------------------------------------------*/
                // 终端风险管理
            /*--------------------------------------------------------------*/
//            rsp = termRiskManage(tag, berHouse, terminal);
//            if (!rsp.isOkey()) {
//                throw new ErrMessage("终端风险管理异常响应:" + rsp.getSw12String());
//            }
//            Log.d(Config.APP_ID, "终端风险管理完成");
            /*--------------------------------------------------------------*/
                // 终端行为分析
            /*--------------------------------------------------------------*/
                // 终端决定交易方式
//            transMethod = termActionAnalyze(berHouse, terminal);
//
//            Log.d(Config.APP_ID, "终端决定交易类型:" + transMethod);
//            rsp = gacProcess(berHouse, terminal, tag, GAC_1, transMethod);
//            if (!rsp.isOkey())
//                throw new ErrMessage("GAC异常响应:" + rsp.getSw12String());
//            Log.d(Config.APP_ID, "第" + GAC_1 + "GAC完成");
//            Iso7816.BerTLV.extractPrimitives(berHouse, rsp);
//
//            Log.d(Config.APP_ID, "终端行为分析完成");

            /*--------------------------------------------------------------*/
                // 联机处理
            /*--------------------------------------------------------------*/
//            byte[] result = null;
//            try {
//                if (transMethod == TransMethod.TRANS_ONLINE) {
//                    // 卡片决定交易方式
//                    transMethod = onlineProcess(berHouse, terminal, rsp);
//                    if (transMethod == TransMethod.TRANS_ONLINE) {
//                        //result = onlineTransaction(berHouse, terminal);
//                    }
//
//                } else if (transMethod == TransMethod.TRANS_OFFLINE ) {
//
//                    offlineTransaction(berHouse, terminal);
//                } else if (transMethod == TransMethod.TRANS_DENIAL) {
//                    // 脱机拒绝
//                    Log.d(Config.APP_ID, "脱机拒绝" + transMethod);
//                } else {
//                    throw new ErrMessage("联机处理异常,交易方式码=" + transMethod);
//                }
//                Log.d(Config.APP_ID, "交易完成");
//
//            } catch (BadPaddingException e) {
//                e.printStackTrace();
//            } catch (NoSuchAlgorithmException e) {
//                e.printStackTrace();
//            } catch (IllegalBlockSizeException e) {
//                e.printStackTrace();
//            } catch (NoSuchPaddingException e) {
//                e.printStackTrace();
//            } catch (InvalidKeyException e) {
//                e.printStackTrace();
//            }
            /*--------------------------------------------------------------*/
                // 发卡行认证
            /*--------------------------------------------------------------*/
                //if (TRANS_METHOD == TRANS_ONLINE && result != null && result.length == 10) {
                //    byte[] arc = Arrays.copyOfRange(result, 0, 2);
                //    Log.d(Config.APP_ID, "ARC=" + Util.toHexString(arc));
                //    byte[] arpc = Arrays.copyOfRange(result, 2, 10);
                //    Log.d(Config.APP_ID, "ARPC=" + Util.toHexString(arpc));
                //   // 发卡行认证
                //    rsp = issuerVerify(tag, berHouse, terminal, arc, arpc);
                //    if (!rsp.isOkey())
                //        throw new ErrMessage("发卡行认证异常码:" + rsp.getSw12String());
                //    // 批准交易
                //    TRANS_METHOD = TRANS_APPROVE;
                //   Log.d(Config.APP_ID, "发卡行认证完成");
                //}
            /*--------------------------------------------------------------*/
                // 交易结束
            /*--------------------------------------------------------------*/
                //rsp = gacProcess(berHouse, terminal, tag, GAC_2, TRANS_METHOD);
                //if (!rsp.isOkey())
                //    throw new ErrMessage("GAC异常响应:" + rsp.getSw12String());
                //Log.d(Config.APP_ID, "第" + GAC_2 + "次GAC完成");
            /*--------------------------------------------------------------*/
                // 发卡行脚本处理
            /*--------------------------------------------------------------*/
                //ByteBuffer[] buf = new ByteBuffer[]{ByteBuffer.wrap(script)};
                //int ret = issuerScriptProcess(tag, terminal, (byte) 0x72, buf);
                //if (ret != 0) {
                //    throw new ErrMessage("脚本执行错误");
                //}
                //Log.d(Config.APP_ID, "发卡行脚本处理完成");

            } catch (ErrMessage message) {
                card.setProperty(SPEC.PROP.WARN, message.getMessage());
            } finally {
             /*--------------------------------------------------------------*/
                // build result
            /*--------------------------------------------------------------*/
                parseInfo(app, berHouse);

                card.addApplication(app);
            }
        }
        return card.isUnknownCard() ? HINT.RESETANDGONEXT : HINT.STOP;
    }
}
