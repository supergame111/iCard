package com.ftsafe.iccd.ecard.reader.pboc;

import android.util.Log;

import com.ftsafe.iccd.ecard.Config;
import com.ftsafe.iccd.ecard.SPEC;
import com.ftsafe.iccd.ecard.Terminal;
import com.ftsafe.iccd.ecard.bean.Application;
import com.ftsafe.iccd.ecard.bean.Card;
import com.ftsafe.iccd.ecard.pojo.PbocTag;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import ftsafe.common.Util;
import ftsafe.common.encryption.DES;
import ftsafe.reader.Reader;
import ftsafe.reader.tech.Iso7816;

/**
 * Created by qingyuan on 2016/7/6.
 */
public abstract class StandardPboc {

    private static Class<?>[][] applets = {};

    public static void readCard(Reader reader, Card card) throws Exception {

        Iso7816.StdTag tag = new Iso7816.StdTag(reader);

        reader.powerOn();

        for (final Class<?> g[] : applets) {
            HINT hint = HINT.RESETANDGONEXT;

            for (final Class<?> r : g) {

                final StandardPboc app = (StandardPboc) r.newInstance();

                switch (hint) {

                    case RESETANDGONEXT:
                        if (app.reset(tag) == false)
                            continue;

                    case GONEXT:
                        hint = app.readCard(tag, card);
                        break;

                    default:
                        break;
                }

                if (hint == HINT.STOP)
                    break;
            }
        }
        reader.powerOff();
    }

    public static void readCard(Reader reader, Class<?> r, Card card) throws Exception {
        if (reader == null || r == null)
            return;

        Iso7816.StdTag stdApplet = new Iso7816.StdTag(reader);
        // 卡片上电
        reader.powerOn();

        final StandardPboc app = (StandardPboc) r.newInstance();

        HINT hint = HINT.RESETANDGONEXT;
        switch (hint) {

            case RESETANDGONEXT:
                if (app.reset(stdApplet) == false)
                    break;
            case GONEXT:
                hint = app.readCard(stdApplet, card);
                break;

            default:
                break;
        }

        // 卡片下电
        reader.powerOff();
    }

    protected enum HINT {
        STOP, GONEXT, RESETANDGONEXT,
    }

    protected final static byte[] DFI_MF = {(byte) 0x3F, (byte) 0x00};
    protected final static byte[] DFI_EP = {(byte) 0x10, (byte) 0x01};
    protected final static byte[] DFN_PSE = {(byte) '1', (byte) 'P', (byte) 'A', (byte) 'Y',
            (byte) '.', (byte) 'S', (byte) 'Y', (byte) 'S', (byte) '.', (byte) 'D', (byte) 'D',
            (byte) 'F', (byte) '0', (byte) '1',};
    protected final static int SFI_EXTRA = 21;
    protected static int EMVMode;
    protected static int MAX_LOG = 10;
    protected static int SFI_LOG = 24;
    protected static byte TRANS_MODE_PBOC = 0x01;
    protected static byte TRANS_MODE_VISA = 0x02;
    protected static byte TRANS_MODE_MASTERCARD = 0x10;
    protected static byte TRANS_MODE_EMV = (byte) 0x80;
    //transaction type
    protected static byte TERM_TRANS_CASH = 0;
    protected static byte TERM_TRANS_GOODS = 1;
    protected static byte TERM_TRANS_SERVICE = 2;
    protected static byte TERM_TRANS_CASHBACK = 3;
    protected static byte CVR_UNKNOWN = 0;
    protected static byte CVR_FAIL = 1;
    protected static byte CVR_SUCCESS = 2;

    protected abstract Object getApplicationId();

    protected byte[] getMainApplicationId() {
        return DFI_EP;
    }

    protected SPEC.CUR getCurrency() {
        return SPEC.CUR.CNY;
    }

    protected boolean reset(Iso7816.StdTag stdLay) throws IOException {
        return stdLay.selectByID(DFI_MF).isOkey() || stdLay.selectByName(DFN_PSE).isOkey();
    }

    protected HINT readCard(Iso7816.StdTag tag, Card card) throws IOException {

		/*--------------------------------------------------------------*/
        // select Main Application
        /*--------------------------------------------------------------*/
        if (selectMainApplication(tag) == false)
            return HINT.GONEXT;

        Iso7816.Response INFO, BALANCE;

		/*--------------------------------------------------------------*/
        // read card info file, binary (21)
        /*--------------------------------------------------------------*/
        INFO = tag.readBinary(SFI_EXTRA);

		/*--------------------------------------------------------------*/
        // read balance
        /*--------------------------------------------------------------*/
        BALANCE = tag.getBalance(0, true);

		/*--------------------------------------------------------------*/
        // read log file, record (24)
        /*--------------------------------------------------------------*/
        ArrayList<byte[]> LOG = readLog24(tag, SFI_LOG);

		/*--------------------------------------------------------------*/
        // build result
        /*--------------------------------------------------------------*/
        final Application app = createApplication();

        parseBalance(app, BALANCE);

        parseInfo21(app, INFO, 4, true);

        parseLog24(app, LOG);

        configApplication(app);

        card.addApplication(app);

        return HINT.STOP;
    }

    protected boolean selectMainApplication(Iso7816.StdTag stdLay) throws IOException {
        final byte[] aid = getMainApplicationId();
        return ((aid.length == 2) ? stdLay.selectByID(aid) : stdLay.selectByName(aid)).isOkey();
    }

    protected ArrayList<byte[]> readLog24(Iso7816.StdTag stdLay, int sfi) throws IOException {
        final ArrayList<byte[]> ret = new ArrayList<byte[]>(MAX_LOG);
        final Iso7816.Response rsp = stdLay.readRecord(sfi);
        if (rsp.isOkey()) {
            addLog24(rsp, ret);
        } else {
            for (int i = 1; i <= MAX_LOG; ++i) {
                if (!addLog24(stdLay.readRecord(sfi, i), ret))
                    break;
            }
        }

        return ret;
    }

    protected void parseInfo21(Application app, Iso7816.Response data, int dec, boolean bigEndian) {
        if (!data.isOkey() || data.size() < 30) {
            return;
        }

        final byte[] d = data.getBytes();
        if (dec < 1 || dec > 10) {
            app.setProperty(SPEC.PROP.SERIAL, Util.toHexString(d, 10, 10));
        } else {
            final int sn = bigEndian ? Util.toIntR(d, 19, dec) : Util.toInt(d, 20 - dec, dec);

            app.setProperty(SPEC.PROP.SERIAL, String.format("%d", 0xFFFFFFFFL & sn));
        }

        if (d[9] != 0)
            app.setProperty(SPEC.PROP.VERSION, String.valueOf(d[9]));

        app.setProperty(SPEC.PROP.DATE, String.format("%02X%02X.%02X.%02X - %02X%02X.%02X.%02X",
                d[20], d[21], d[22], d[23], d[24], d[25], d[26], d[27]));
    }

    protected boolean addLog24(final ftsafe.reader.tech.Iso7816.Response r, ArrayList<byte[]> l) {
        if (!r.isOkey())
            return false;

        final byte[] raw = r.getBytes();
        final int N = raw.length - 23;
        if (N < 0)
            return false;

        for (int s = 0, e = 0; s <= N; s = e) {
            l.add(Arrays.copyOfRange(raw, s, (e = s + 23)));
        }

        return true;
    }

    protected float parseBalance(Iso7816.Response data) {
        float ret = 0f;
        if (data.isOkey() && data.size() >= 4) {
            int n = Util.toInt(data.getBytes(), 0, 4);
            if (n > 1000000 || n < -1000000)
                n -= 0x80000000;

            ret = n / 100.0f;
        }
        return ret;
    }

    protected void parseBalance(Application app, Iso7816.Response... data) {

        float amount = 0f;
        for (Iso7816.Response rsp : data)
            amount += parseBalance(rsp);

        app.setProperty(SPEC.PROP.BALANCE, amount);
    }

    protected final static byte TRANS_CSU = 6;
    protected final static byte TRANS_CSU_CPX = 9;

    protected void parseLog24(Application app, ArrayList<byte[]>... logs) {
        final ArrayList<String> ret = new ArrayList<String>(MAX_LOG);

        for (final ArrayList<byte[]> log : logs) {
            if (log == null)
                continue;

            for (final byte[] v : log) {
                final int money = Util.toInt(v, 5, 4);
                if (money > 0) {
                    final char s = (v[9] == TRANS_CSU || v[9] == TRANS_CSU_CPX) ? '-' : '+';

                    final int over = Util.toInt(v, 2, 3);
                    final String slog;
                    if (over > 0) {
                        slog = String
                                .format("%02X%02X.%02X.%02X %02X:%02X %c%.2f [o:%.2f] [%02X%02X%02X%02X%02X%02X]",
                                        v[16], v[17], v[18], v[19], v[20], v[21], s,
                                        (money / 100.0f), (over / 100.0f), v[10], v[11], v[12],
                                        v[13], v[14], v[15]);
                    } else {
                        slog = String.format(
                                "%02X%02X.%02X.%02X %02X:%02X %C%.2f [%02X%02X%02X%02X%02X%02X]",
                                v[16], v[17], v[18], v[19], v[20], v[21], s, (money / 100.0f),
                                v[10], v[11], v[12], v[13], v[14], v[15]);

                    }

                    ret.add(slog);
                }
            }
        }

        if (!ret.isEmpty())
            app.setProperty(SPEC.PROP.TRANSLOG, ret.toArray(new String[ret.size()]));
    }

    private static void buildPDO(ByteBuffer out, int len, byte... val) {
        final int n = Math.min((val != null) ? val.length : 0, len);
        int i = 0;
        while (i < n) out.put(val[i++]);
        while (i++ < len) out.put((byte) 0);
    }

    protected static byte[] buildPDOL(Iso7816.BerHouse tlvs, Terminal terminal) {
        final ByteBuffer buff = ByteBuffer.allocate(64);
        buff.put((byte) 0x83).put((byte) 0x00);
        try {
            final byte[] pdol = tlvs.findFirst((short) 0x9F38).v.getBytes();
            //Log.e(Config.APP_ID, "9F38="+Util.toHexString(pdol));
            ArrayList<Iso7816.BerTLV> list = Iso7816.BerTLV.extractOptionList(pdol);
            for (Iso7816.BerTLV tlv : list) {
                final int tag = tlv.t.toInt();
                final int len = tlv.l.toInt();
                switch (tag) {
                    case 0x9F66:
                        // 终端交易属性
                        buildPDO(buff, len, terminal.getTermTransAtr());
                        break;
                    case 0x9F02:
                        // 授权金额
                        buildPDO(buff, len, terminal.getAmtAuthNum());
                        break;
                    case 0x9F03:
                        // 其它金额
                        buildPDO(buff, len, terminal.getAmtOtherNum());
                        break;
                    case 0x9F1A:
                        // 终端国家代码
                        buildPDO(buff, len, terminal.getCountryCode());
                        break;
                    case 0x9F37:
                        // 不可预知数
                        buildPDO(buff, len, terminal.getUnpredictNum());
                        break;
                    case 0x5F2A:
                        // 交易货币代码
                        //buildPDO(buff, len, (byte) 0x01, (byte) 0x56);
                        buildPDO(buff, len, terminal.getTransCurcyCode());
                        break;
                    case 0x95:
                        // 终端验证结果
                        //buildPDO(buff, len, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00);
                        buildPDO(buff, len, terminal.getTVR());
                        break;
                    case 0x9A:
                        // 交易日期
                        //buildPDO(buff, len, Util.getSysDate(3));
                        buildPDO(buff, len, terminal.getTransDate());
                        break;
                    case 0x9C:
                        // 交易类型
                        //buildPDO(buff, len, (byte) 0x00);
                        buildPDO(buff, len, terminal.getTransType());
                        break;
                    case 0xDF60:
                        // CAPP交易指示位
                        //buildPDO(buff, len, (byte) 0x00);
                        buildPDO(buff, len, terminal.getTermCappFlag());
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

    protected Application createApplication() {
        return new Application();
    }

    protected void configApplication(Application app) {
        app.setProperty(SPEC.PROP.ID, getApplicationId());
        app.setProperty(SPEC.PROP.CURRENCY, getCurrency());
    }

    /**
     * 读应用数据
     * READ RECORD命令/响应，循环读取应用数据存入 {@code berHouse}
     *
     * @param tag      标准PBOC
     * @param berHouse TLV数据包
     */
    protected void readApplicationRecord(Iso7816.StdTag tag, Iso7816.BerHouse berHouse) {
        Log.d(Config.APP_ID, "读取应用数据");
        final byte[] aip, afl;
        try {
            final Iso7816.BerTLV topTlv80 = berHouse.findFirst(PbocTag.RESPONSE_TEMPLATE_80);
            if (topTlv80 != null) {
                /*------------------------*/
                // 计算SFI
                /*------------------------*/
                int length = topTlv80.length();
                Log.e(Config.APP_ID, "80模板长度=" + length);
                byte[] tmp = topTlv80.v.getBytes();
                aip = Arrays.copyOfRange(tmp, 0, 2);
                afl = Arrays.copyOfRange(tmp, 2, length);
                Log.e(Config.APP_ID, "aip=" + Util.toHexString(aip) + ",afl=" + Util.toHexString(afl));
                /*------------------------*/
                // RREAD RECORD
                /*------------------------*/
                int group = afl.length / 4;
                int sfi, nums, nume, flag;
                for (int i = 0; i < group; i++) {
                    tmp = Arrays.copyOfRange(afl, i * 4, (i + 1) * 4);
                    sfi = Util.BCDtoInt((byte) (tmp[0] >> 3));
                    nums = Util.BCDtoInt(tmp[1]);
                    nume = Util.BCDtoInt(tmp[2]);
                    flag = Util.BCDtoInt(tmp[3]);
                    Log.e(Config.APP_ID, "sfi=" + sfi + ",nums=" + nums + ",nume=" + nume + ",flag=" + flag);
                    for (int j = nums; j <= nume; j++) {
                        Log.e(Config.APP_ID, "读记录:SFI=" + sfi + ",NUM=" + j);

                        Iso7816.Response r = tag.readRecord(sfi, j);
                        if (r.isOkey())
                            Iso7816.BerTLV.extractPrimitives(berHouse, r);
                        else
                            throw new Exception("读SFI=" + sfi + ",NUM=" + j + "记录异常响应码:" + r.getSw12String());

                    }
                }

            } else
                throw new Exception("解析GPO响应:没有80模板" + topTlv80);

        } catch (Exception e) {
            Log.e(Config.APP_ID, e.getMessage());
        }
    }

    /**
     * 脱机数据认证
     * INTERNAL AUTHENTICATE命令/响应
     */
    protected void offLineDataAuthenticate(boolean isDDA) {
        Log.d(Config.APP_ID, "脱机数据认证");
    }

    /**
     * 处理限制
     */
    protected void processRestrict(Iso7816.BerHouse berHouse, Terminal terminal) {
        Log.d(Config.APP_ID, "处理限制");
        // check app version
        Iso7816.BerTLV berTLV = berHouse.findFirst(PbocTag.CARD_APP_VER);
        if (berTLV != null && terminal.getAppVer() != null) {
            Log.d(Config.APP_ID, "处理限制-应用版本检查");
            if (!(terminal.getAppVer()[0] == 0x00 && terminal.getAppVer()[1] == 0x8C)//VIS140-0x008C(140)
                    && !(terminal.getAppVer()[0] == 0x00 && terminal.getAppVer()[1] == 0x84)//VIS132-0x0084(132)
                    && !(terminal.getAppVer()[0] == 0x00 && terminal.getAppVer()[1] == 0x83)//VIS131-0x0083(131)
                    && !(terminal.getAppVer()[0] == 0x01 && terminal.getAppVer()[1] == 0x20)//JCB1.2-0120
                    && !(terminal.getAppVer()[0] == 0x02 && terminal.getAppVer()[1] == 0x00)//JCBv2.0-0200
                    && !(terminal.getAppVer()[0] == 0x00 && terminal.getAppVer()[1] == 0x02)//MChip-0002
                    && !(terminal.getAppVer()[0] == 0x00 && terminal.getAppVer()[1] == 0x20)// PBOC 0020
                    && !(terminal.getAppVer()[0] == 0x00 && terminal.getAppVer()[1] == 0x30)// PBOC 0030
                    ) {
                terminal.orTVR(1, (byte) 0x80);
            }
        }
        // check auc
        boolean bTestFail = false;
        berTLV = berHouse.findFirst(PbocTag.AUC);
        if (berTLV != null) { // auc
            byte[] AUC = berTLV.v.getBytes();
            Log.d(Config.APP_ID, "处理限制-应用用途控制检查");
            if ((terminal.getTermType() == 0x14 || terminal.getTermType() == 0x15 || terminal.getTermType() == 0x16)
                    && ((terminal.getTermAddCapab()[0] & 0x80) != 0)) {
                //The termianl is ATM
                if ((AUC[0] & (byte) 0x02) == 0)// if‘Valid at ATMs’bit not on.
                {
                    bTestFail = true;
                }
            } else {
                //The terminal is not ATM
                if ((AUC[0] & 0x01) == 0)// if‘Valid at terminals other than ATMs’bit not on.
                {
                    bTestFail = true;
                }
            }
            // check issuer country code
            berTLV = berHouse.findFirst(PbocTag.ISSUER_COUNTRY_CODE);
            if (berTLV != null) {//Issuer country code exist
                Log.d(Config.APP_ID, "处理限制-发卡行国家代码检查");
                byte[] countryCode = berTLV.v.getBytes();
                if (Arrays.equals(countryCode, terminal.getCountryCode())) {//domestic
                    if (terminal.getTransType() == TERM_TRANS_CASH) {
                        if ((AUC[0] & 0x80) == 0)// if‘Valid for domestic cash transactions’bit not on.
                        {
                            bTestFail = true;
                        }
                    }
                    if (terminal.getTransType() == TERM_TRANS_GOODS) {
                        if ((AUC[0] & 0x20) == 0)// if‘Valid for domestic goods’bit not on.
                        {
                            bTestFail = true;
                        }
                    }
                    if (terminal.getTransType() == TERM_TRANS_SERVICE) {
                        if ((AUC[0] & 0x08) == 0)// if‘Valid for domestic services’bit not on.
                        {
                            bTestFail = true;
                        }
                    }
                    //if(AmtOtherBin!=0 || m_TermInfo.TransType==CASHBACK)
                    if (!Arrays.equals(terminal.getAmtOtherNum(), new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00})
                            || terminal.getTransType() == TERM_TRANS_CASHBACK) {
                        if ((AUC[1] & 0x80) != 0x80)// if‘domestic cashback allowed’bit not on.
                        {
                            bTestFail = true;
                        }
                    }
                } else {//international,terminal country code differ from issuer country code
                    if (terminal.getTransType() == TERM_TRANS_CASH) {
                        if ((AUC[0] & 0x40) == 0)// if‘Valid for international cash transactions’bit not on.
                        {
                            bTestFail = true;
                        }
                    }
                    if (terminal.getTransType() == TERM_TRANS_GOODS) {
                        if ((AUC[0] & 0x10) == 0)// if‘Valid for international goods’bit not on.
                        {
                            bTestFail = true;
                        }
                    }
                    if (terminal.getTransType() == TERM_TRANS_SERVICE) {
                        if ((AUC[0] & 0x04) == 0)// if‘Valid for international goods’bit not on.
                        {
                            bTestFail = true;
                        }
                    }
                    //if(AmtOtherBin!=0 || m_TermInfo.TransType==CASHBACK)
                    if (!Arrays.equals(terminal.getAmtAuthNum(), new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00})
                            && (terminal.getTransType() == TERM_TRANS_CASHBACK)) {
                        if ((AUC[1] & 0x40) == 0)// if‘international cashback allowed’bit not on.
                        {
                            bTestFail = true;
                        }
                    }
                }
            }// end issuer country code
        }
        if (bTestFail) {
            //set‘Requested service not allowed for card product’bit 1
            terminal.orTVR(1, (byte) 0x10);
        }
        // check app expiration
        berTLV = berHouse.findFirst(PbocTag.APP_START_DATE);
        byte[] currentDate = terminal.getTransDate();
        if (berTLV != null) {
            Log.d(Config.APP_ID, "处理限制-卡片生效日期检查");
            byte[] EffectDate = berTLV.v.getBytes();
            if (Util.memcmp(currentDate, EffectDate, 4) < 0) { // 小于生效日期
                //set‘Application not yet effective’bit 1
                terminal.orTVR(1, (byte) 0x20);
            }
        }
        berTLV = berHouse.findFirst(PbocTag.APP_EXPIRATION_DATE);
        if (berTLV != null) {
            Log.d(Config.APP_ID, "处理限制-卡片失效日期检查");
            byte[] EffectDate = berTLV.v.getBytes();
            if (Util.memcmp(currentDate, EffectDate, 4) > 0) { // 超过失效日期
                //set‘Expired application’bit 1
                terminal.orTVR(1, (byte) 0x40);
            }
        }
    }

    protected long emvPerformCVM(CVM CVMValue, Terminal terminal) {
        long retCode;
        byte temp = (byte) (CVMValue.method & 0x3F);
        PinProcess pinProcess = new PinProcess();
        switch (temp) {
            case 0x00://FAIL CVM PROCESSING
                //m_TermInfo.TVR[2]|=0x80;
                terminal.updateCVMResult(2, CVR_FAIL);
                retCode = ErrCode.ERR_EMV_CVRFail;
                break;
            case 0x01://PLAINTEXT PIN VERIFICATION PERFORMED BY ICC
                if (CVMValue.condition == 0x03)//terminal support the CVM
                {
                    if ((terminal.getTermCapab()[1] & 0x80) == 0) {
                        return ErrCode.ERR_EMV_CVRNoSupport;
                    }
                }
                retCode = pinProcess.emvPlaintextPinProcess();
                if (retCode == 0) {
                    terminal.updateCVMResult(2, CVR_SUCCESS);
                } else {
                    terminal.updateCVMResult(2, CVR_FAIL);
                }
                break;
            case 0x02://enciphered PIN verification online
                if (CVMValue.condition == 0x03)//terminal support the CVM
                {
                    if ((terminal.getTermCapab()[1] & 0x40) == 0)
                        return ErrCode.ERR_EMV_CVRNoSupport;
                }
                retCode = pinProcess.emvOnlinePinProcess();
                if (retCode == 0) {
                    terminal.updateCVMResult(2, CVR_SUCCESS);
                } else {
                    terminal.updateCVMResult(2, CVR_FAIL);
                }
                break;
            case 0x03://Plaintext PIN verification performed by ICC and signature(paper)
                if (CVMValue.condition == 0x03)//terminal support the CVM
                {
                    if ((terminal.getTermCapab()[1] & 0xA0) != 0xA0)
                        return ErrCode.ERR_EMV_CVRNoSupport;
                }

                retCode = pinProcess.emvPlaintextPinProcess();
                if (retCode == 0) {
                    terminal.updateCVMResult(2, CVR_UNKNOWN);
                    //Emv_Signature(temp, Num);
                } else {
                    terminal.updateCVMResult(2, CVR_FAIL);
                }

                //Emv_Signature(temp, Num);
                break;
            case 0x04://enciphered PIN verification performed by ICC

                if (CVMValue.condition == 0x03)//terminal support the CVM
                {
                    if ((terminal.getTermCapab()[1] & 0x10) == 0)
                        return ErrCode.ERR_EMV_CVRNoSupport;
                }

                retCode = pinProcess.emvEncipheredPinProcess();
                if (retCode == 0) {
                    terminal.updateCVMResult(2, CVR_UNKNOWN);
                } else {
                    terminal.updateCVMResult(2, CVR_FAIL);
                }
                //终端暂不支持密文PIN
//                terminal.orTVR(2, (byte) 0x10);//要求输入PIN但键盘不存在
//                terminal.updateCVMResult(2, CVR_FAIL);
//                retCode = ErrCode.ERR_EMV_CVRFail;

                break;
            case 0x05://enciphered PIN verification performed by ICC and signature(paper)
//                if (CVMValue.condition == 0x03)//terminal support the CVM
//                {
//                    if ((terminal.getTermCapab()[1] & 0x30) != 0x30)
//                        return ErrCode.ERR_EMV_CVRNoSupport;
//                }
//
//                retCode = Emv_OfflineEncPIN();
//                if (retCode == ERR_EMV_IccCommand || retCode == ERR_EMV_IccReturn || retCode == ERR_EMV_CancelTrans)
//                    break;//return retCode;
//                else if (retCode == 0) {
//                    terminal.updateCVMResult(2, CVR_UNKNOWN);
//                    //Emv_Signature(temp,Num);
//                } else if (retCode == ERR_EMV_GetChallenge) {
//                    terminal.updateCVMResult(2, CVR_FAIL);
//                    terminal.orTVR(2, (byte) 0x80);
//                    retCode = 0;
//                } else {
//                    terminal.updateCVMResult(2, CVR_FAIL);
//                }
//
//                Emv_Signature(temp, Num);
                terminal.updateCVMResult(2, CVR_FAIL);
                retCode = ErrCode.ERR_EMV_CVRFail;
                break;
            case 0x1E://signature (paper)
                //if(CVMValue.condition==0x03)//terminal support the CVM,
                //{
                if ((terminal.getTermCapab()[1] & 0x20) == 0) {
                    return ErrCode.ERR_EMV_CVRNoSupport;
                }
                //}

                //Emv_Signature(temp,Num);
                terminal.updateCVMResult(2, CVR_UNKNOWN);
                retCode = 0;
                break;
            case 0x1F://no CVM required
                //if(CVMValue.condition==0x03)//terminal support the CVM,deleted for M00905,J9401
                //{
                if ((terminal.getTermCapab()[1] & 0x08) == 0) {
                    return ErrCode.ERR_EMV_CVRNoSupport;
                }
                //}

                terminal.andTVR(2, (byte) 0x7F);
                terminal.updateCVMResult(2, CVR_SUCCESS);
                retCode = 0;
                break;
            case 0x20://Cardholder ID verification
                if ((terminal.getTermCapab()[1] & 0x01) == 0) {
                    terminal.updateCVMResult(2, CVR_FAIL);
                    retCode = ErrCode.ERR_EMV_CVRNoSupport;
                } else {
                    //retCode=Emv_VerifyCardholderID();
                    retCode = 0;
                    if (retCode == 0) {
                        terminal.updateCVMResult(2, CVR_SUCCESS);
                    } else {
                        terminal.updateCVMResult(2, CVR_FAIL);
                    }
                }
                break;
            default:
                terminal.orTVR(2, (byte) 0x40);     //set "Unrecognized CVM" bit 1 in TVR
                //m_TermInfo.CVMResult[2]=CVR_UNKNOWN;
                terminal.updateCVMResult(2, CVR_FAIL);
                retCode = ErrCode.ERR_EMV_CVRFail;
                break;
        }

        terminal.updateCVMResult(0, CVMValue.method);
        terminal.updateCVMResult(1, CVMValue.condition);

        return retCode;
    }

    /**
     * 持卡人验证
     * GET DATA命令/响应，VERIFY命令/响应
     */
    protected long cardHolderVerify(Iso7816.BerHouse berHouse, Terminal terminal) {
        Log.d(Config.APP_ID, "持卡人验证");
        long lr;
        int i;
        Iso7816.BerTLV berTLV;
        //see if ICC supports cardholder verification
        byte[] AIP = berHouse.findFirst(PbocTag.APP_INTERCHANGE_PROFILE).v.getBytes();
        if ((AIP[0] & 0x10) == 0)
            return 0;

        //No CVM Required
        if ((terminal.getTermCapab()[1] & 0x08) != 0) {
            return 0;
        }

        //get cvm list in the card, if not available, return
        berTLV = berHouse.findFirst(PbocTag.CVM_LIST);
        byte[] CCVMList = berTLV.v.getBytes();
        int CCVMListLen = berTLV.length(); // 从卡片获取的CVMList长度
        if (berTLV == null)//CVM List is absent.
        {
            terminal.orTVR(0, (byte) 0x20);//ICC Data Missing.
            return 0;
        }

        if (CCVMListLen < 10) {
            terminal.orTSI(0, (byte) 0x40);
            terminal.orTVR(2, (byte) 0x80);
            //return ERR_EMV_EmvDataFormat;
            return 0;
        }

        //to compatibal with PC and 51MCU and MCVZ328 MCU,make the long integer independent of byte order.
        int CVM_X = 0;
        int CVM_Y = 0;
        for (i = 0; i != 4; i++) {
            CVM_X = (CVM_X << 8) + CCVMList[i];
            CVM_Y = (CVM_Y << 8) + CCVMList[i + 4];
        }

        long lAmtAuthBin = 0, lAmtOtherBin = 0;

        //memcpy(&lAmtAuthBin, m_TermInfo.AmtAuthBin, 4);
        //memcpy(&lAmtOtherBin, m_TermInfo.AmtOtherBin, 4);
        if (terminal.getAmtAuthBin() != null)
            lAmtAuthBin = Util.BCDtoInt(terminal.getAmtAuthBin(), 0, 4);
        if (terminal.getAmtOtherBin() != null)
            lAmtOtherBin = Util.BCDtoInt(terminal.getAmtOtherBin(), 0, 4);

        int CVMListLen = (CCVMListLen - 8) / 2;
        //ArrayList<CVM> CVMList = new ArrayList<>();
        CVM[] CVMList = new CVM[64];
        for (i = 0; i != CVMListLen; i++) {
            CVMList[i].method = CCVMList[i * 2 + 8];
            CVMList[i].condition = CCVMList[i * 2 + 9];
        }

        byte[] transCurcyCode = terminal.getTransCurcyCode();
        byte[] cTransCurcyCode = berHouse.findFirst(PbocTag.APP_CURRENCY_CODE).v.getBytes();
        for (i = 0; i != CVMListLen; i++) {
            switch (CVMList[i].condition) {
                case 0x00://always
                    lr = emvPerformCVM(CVMList[i], terminal);
                    if (lr == 0) {
                        terminal.orTSI(0, (byte) 0x40);
                        return 0;
                    } else {
                        if ((CVMList[i].method & 0x40) == 0x00) {
                            terminal.orTSI(0, (byte) 0x40);
                            terminal.orTVR(2, (byte) 0x80);
                            return 0;
                        }

                        if (lr != ErrCode.ERR_EMV_CVRFail && lr != ErrCode.ERR_EMV_CVRNoSupport) {
                            return lr;
                        }
                    }
                    break;
                case 0x01://if cash or cashback(EMV2000)－> if unattended cash(modified in EMV4.1,SU16)
                    if (terminal.getTransType() == TERM_TRANS_CASH && (terminal.getTermType() & 0x0F) > 3) {
                        lr = emvPerformCVM(CVMList[i], terminal);
                        if (lr == 0) {
                            terminal.orTSI(0, (byte) 0x40);
                            return 0;
                        } else {
                            if ((CVMList[i].method & 0x40) == 0x00) {
                                terminal.orTSI(0, (byte) 0x40);
                                terminal.orTVR(2, (byte) 0x80);
                                return 0;
                            }

                            if (lr != ErrCode.ERR_EMV_CVRFail && lr != ErrCode.ERR_EMV_CVRNoSupport) {
                                return lr;
                            }
                        }
                    }
                    break;
                case 0x02://if not cash or cashback
                    if (terminal.getTransType() != TERM_TRANS_CASH
                            && terminal.getTransType() != TERM_TRANS_CASHBACK
                            && lAmtOtherBin == 0) {
                        lr = emvPerformCVM(CVMList[i], terminal);
                        if (lr == 0) {
                            terminal.orTSI(0, (byte) 0x40);
                            return 0;
                        } else {
                            if ((CVMList[i].method & 0x40) == 0x00) {
                                terminal.orTSI(0, (byte) 0x40);
                                terminal.orTVR(2, (byte) 0x80);
                                return 0;
                            }

                            if (lr != ErrCode.ERR_EMV_CVRFail && lr != ErrCode.ERR_EMV_CVRNoSupport) {
                                return lr;
                            }
                        }
                    }
                    break;
                case 0x03://if terminal support CVM
                    //if((m_TermInfo.TermCapab[1]&0xF0)!=0x00)//In fact this should be judged for each CVM.
                    //{
                    lr = emvPerformCVM(CVMList[i], terminal);
                    if (lr == 0) {
                        terminal.orTSI(0, (byte) 0x40);
                        return 0;
                    } else {
                        if ((CVMList[i].method & 0x40) == 0x00) {
                            terminal.orTSI(0, (byte) 0x40);
                            terminal.orTVR(2, (byte) 0x80);
                            return 0;
                        }

                        if (lr != ErrCode.ERR_EMV_CVRFail && lr != ErrCode.ERR_EMV_CVRNoSupport) {
                            return lr;
                        }
                    }
                    //}
                    break;
                case 0x04://if manual cash (added in EMV4.1,SU16)
                    if (terminal.getTransType() == TERM_TRANS_CASH && (terminal.getTermType() & 0x0F) <= 3) {
                        lr = emvPerformCVM(CVMList[i], terminal);
                        if (lr == 0) {
                            terminal.orTSI(0, (byte) 0x40);
                            return 0;
                        } else {
                            if ((CVMList[i].method & 0x40) == 0x00) {
                                terminal.orTSI(0, (byte) 0x40);
                                terminal.orTVR(2, (byte) 0x80);
                                return 0;
                            }

                            if (lr != ErrCode.ERR_EMV_CVRFail && lr != ErrCode.ERR_EMV_CVRNoSupport) {
                                return lr;
                            }
                        }
                    }
                    break;
                case 0x05://if purchase with cashback (added in EMV4.1,SU16)
                    if (terminal.getTransType() == TERM_TRANS_CASHBACK || lAmtOtherBin == 0) {
                        lr = emvPerformCVM(CVMList[i], terminal);
                        if (lr == 0) {
                            terminal.orTSI(0, (byte) 0x40);
                            return 0;
                        } else {
                            if ((CVMList[i].method & 0x40) == 0x00) {
                                terminal.orTSI(0, (byte) 0x40);
                                terminal.orTVR(2, (byte) 0x80);
                                return 0;
                            }

                            if (lr != ErrCode.ERR_EMV_CVRFail && lr != ErrCode.ERR_EMV_CVRNoSupport) {
                                return lr;
                            }
                        }
                    }
                    break;

                case 0x06://if trans is in App currency and under X value
                    if (cTransCurcyCode == null //Application Currency Code is not present
                            || transCurcyCode == null) //Transaction Currency Code is not present
                    {
                        break;
                    }
                    if (Arrays.equals(transCurcyCode, cTransCurcyCode) &&
                            lAmtAuthBin < CVM_X)//under x shouldn't include case of equal x.(EMV2000 2CJ.077.02)
                    {
                        lr = emvPerformCVM(CVMList[i], terminal);
                        if (lr == 0) {
                            terminal.orTSI(0, (byte) 0x40);
                            return 0;
                        } else {
                            if ((CVMList[i].method & 0x40) == 0x00) {
                                terminal.orTSI(0, (byte) 0x40);
                                terminal.orTVR(2, (byte) 0x80);
                                return 0;
                            }

                            if (lr != ErrCode.ERR_EMV_CVRFail && lr != ErrCode.ERR_EMV_CVRNoSupport) {
                                return lr;
                            }
                        }
                    }
                    break;
                case 0x07://if trans is in App currency and over X value

                    if (cTransCurcyCode == null //Application Currency Code is not present
                            || transCurcyCode == null) //Transaction Currency Code is not present
                    {
                        break;
                    }
                    if (Arrays.equals(transCurcyCode, cTransCurcyCode) &&
                            lAmtAuthBin > CVM_X) {
                        lr = emvPerformCVM(CVMList[i], terminal);
                        if (lr == 0) {
                            terminal.orTSI(0, (byte) 0x40);
                            return 0;
                        } else {
                            if ((CVMList[i].method & 0x40) == 0x00) {
                                terminal.orTSI(0, (byte) 0x40);
                                terminal.orTVR(2, (byte) 0x80);
                                return 0;
                            }

                            if (lr != ErrCode.ERR_EMV_CVRFail && lr != ErrCode.ERR_EMV_CVRNoSupport) {
                                return lr;
                            }
                        }
                    }
                    break;
                case 0x08://if trans is in App currency and under Y value
                    if (cTransCurcyCode == null //Application Currency Code is not present
                            || transCurcyCode == null) //Transaction Currency Code is not present
                    {
                        break;
                    }
                    if (Arrays.equals(transCurcyCode, cTransCurcyCode) &&
                            lAmtAuthBin < CVM_Y) {
                        lr = emvPerformCVM(CVMList[i], terminal);
                        if (lr == 0) {
                            terminal.orTSI(0, (byte) 0x40);
                            return 0;
                        } else {
                            if ((CVMList[i].method & 0x40) == 0x00) {
                                terminal.orTSI(0, (byte) 0x40);
                                terminal.orTVR(2, (byte) 0x80);
                                return 0;
                            }

                            if (lr != ErrCode.ERR_EMV_CVRFail && lr != ErrCode.ERR_EMV_CVRNoSupport) {
                                return lr;
                            }
                        }
                    }
                    break;
                case 0x09://if trans is in App currency and over Y value
                    if (cTransCurcyCode == null //Application Currency Code is not present
                            || transCurcyCode == null) //Transaction Currency Code is not present
                    {
                        break;
                    }
                    if (Arrays.equals(transCurcyCode, cTransCurcyCode) &&
                            lAmtAuthBin > CVM_Y) {
                        lr = emvPerformCVM(CVMList[i], terminal);
                        if (lr == 0) {
                            terminal.orTSI(0, (byte) 0x40);
                            return 0;
                        } else {
                            if ((CVMList[i].method & 0x40) == 0x00) {
                                terminal.orTSI(0, (byte) 0x40);
                                terminal.orTVR(2, (byte) 0x80);
                                return 0;
                            }

                            if (lr != ErrCode.ERR_EMV_CVRFail && lr != ErrCode.ERR_EMV_CVRNoSupport) {
                                return lr;
                            }
                        }
                    }
                    break;
                default:
                    break;
            }
        }

        //card holder verification has not been successful
        terminal.orTSI(0, (byte) 0x40);                //set "Cardholder verification was performed
        terminal.orTVR(2, (byte) 0x80);            //set "Cardholder verification was not successful"
        return 0;
    }

    /**
     * 终端风险管理
     * GET DATA命令/响应
     */

    protected Iso7816.Response termRiskManage(Iso7816.BerHouse berHouse, Terminal terminal, Iso7816.StdTag tag) {
        Log.d(Config.APP_ID, "终端风险管理");
        //CheckExceptionFile
        int nDataLen;
        //int i;
        byte[] ucData = new byte[300];
//        long lr;

// 	for(i=0; i < TermExceptionFileNum; i++)
// 	{
// 		if(memcmp(m_CardInfo.PAN, TermExceptionFile[i].PAN, m_CardInfo.PANLen) == 0)
// 		{
// 			m_TermInfo.TVR[0]|=0x10;//set "Card appears on exception file" bit 1
// 			break;
// 		}
// 	}

        //force Online

        //Check FloorLimit
        long amt = 0, lFloorLimit = 0;

        //memcpy(&amt, m_TermInfo.AmtAuthBin, 4);
        if (terminal.getAmtAuthBin() != null)
            amt = Util.BCDtoInt(terminal.getAmtAuthBin(), 0, 4);

        //累加交易日志记录同一卡号金额，暂不实现
// 	if(TransNum>0)
// 	{
// 		for(i=0;i<TransNum;i++)
// 		{
// 			if(memcmp(m_CardInfo.PAN,TermTransLog[i].PAN,EMV_CardInfo.PANLen)) continue;//not match PAN
// 			amt+=TermTransLog[i].TransAmt;
// 		}
// 	}

        //lFloorLimit = m_TermInfo.FloorLimit[0]*256*256*256 + m_TermInfo.FloorLimit[1]*256*256 + m_TermInfo.FloorLimit[2]*256 + m_TermInfo.FloorLimit[3];
        if (terminal.getFloorLimit() != null)
            lFloorLimit = Util.BCDtoInt(terminal.getFloorLimit(), 0, 4);
        if (lFloorLimit != 0)//added according to test script V2CJ131.00(only in EMV96,deleted in EMV2000)
        {
            if (amt >= lFloorLimit) {
                terminal.orTVR(3, (byte) 0x80);//set 'transaction exceeds floor limit' bit 1.
            }
        }
        Iso7816.Response r = null;
        //Rand Trans Select
        try {
            //Check Velocity
            if (berHouse.findFirst(PbocTag.LCOL) != null && berHouse.findFirst(PbocTag.UCOL) != null && berHouse.findFirst(PbocTag.VLP_ISSU_AUTHOR_CODE) == null)//LCOL or UCOL not exist
            {
                if (berHouse.findFirst(PbocTag.ATC) == null) {

                    r = tag.getData(PbocTag.ATC);

                    if (!r.isOkey()) {
                        terminal.orTVR(0, (byte) 0x20);//set 'ICC data missing' bit 1.
                        return r;
                    }
                    berHouse.add(PbocTag.ATC, r.getBytes());
                }
                if (berHouse.findFirst(PbocTag.LOATC) == null) {
                    r = tag.getData(PbocTag.LOATC);
                    if (!r.isOkey()) {
                        terminal.orTVR(0, (byte) 0x20);//set 'ICC data missing' bit 1.
                        return r;
                    }
                    berHouse.add(PbocTag.LOATC, r.getBytes());
                }
                // ATC and LOATC are high byte ahead,low byte behind.
//                int nATC = m_CardInfo.ATC[0] * 256 + m_CardInfo.ATC[1];
                byte[] ATC = berHouse.findFirst(PbocTag.ATC).v.getBytes();
                int nATC = ATC[0] * 256 + ATC[1];
//                int nLOATC = m_CardInfo.LOATC[0] * 256 + m_CardInfo.LOATC[1];
                byte[] LOATC = berHouse.findFirst(PbocTag.LOATC).v.getBytes();
                int nLOATC = LOATC[0] * 256 + LOATC[1];
                if (nATC <= nLOATC) {
//                    m_TermInfo.TVR[3] |= 0x40;//set 'Lower consecutive online limit exceeded' bit 1.
                    terminal.orTVR(3, (byte) 0x40);
//                    m_TermInfo.TVR[3] |= 0x20;//set 'Upper consecutive online limit exceeded' bit 1.
                    terminal.orTVR(3, (byte) 0x20);
                } else {
                    int counts = nATC - nLOATC;
                    //FIXME:什么意思
//                    if (counts > m_CardInfo.LCOL) {
                    if (counts > berHouse.findFirst(PbocTag.LCOL).v.toInt()) {
//                        m_TermInfo.TVR[3] |= 0x40;//set 'Lower consecutive online limit exceeded' bit 1.
                        terminal.orTVR(3, (byte) 0x40);
                    }

//                    if (counts > m_CardInfo.UCOL) {
                    if (counts > berHouse.findFirst(PbocTag.UCOL).v.toInt()) {
//                        m_TermInfo.TVR[3] |= 0x20;//set 'Upper consecutive online limit exceeded' bit 1.
                        terminal.orTVR(3, (byte) 0x20);
                    }
                }
            }

            //Check New Card
            if (EMVMode != TRANS_MODE_MASTERCARD) {
//                if (m_CardDataTable[ICC_Index_LOATCReg].bExist == 0) {
                if (berHouse.findFirst(PbocTag.LOATC) == null) {
                    r = tag.getData(PbocTag.LOATC);
                    if (r.isOkey()) {
                        berHouse.add(PbocTag.LOATC, r.getBytes());
                    }

                } else {
                    berHouse.add(PbocTag.LOATC, new byte[]{0x00, 0x00});
                    terminal.orTVR(1, (byte) 0x80);//set 'New Card' bit 1
                }

//                if (m_CardDataTable[ICC_Index_LOATCReg].bExist == 1) {
//                    if (memcmp(m_CardInfo.LOATC, "\x00\x00", 2) == 0) {
//                        m_TermInfo.TVR[1] |= 0x08;//set 'New Card' bit 1
//                    }
//                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return r;
    }

    public static int TRANS_OFFLINE = 101;
    public static int TRANS_APPROVE = 101;
    public static int TRANS_ONLINE = 102;
    public static int TRANS_DENIAL = 103;

    /**
     * 终端行为分析
     * GENERATE AC命令
     */
    protected int termActionAnalyze(Iso7816.BerHouse berHouse, Terminal terminal) {
        Log.d(Config.APP_ID, "终端行为分析");
        int i, k;//TermAnaResult,CardAnaResult;//0-Denial,1-Online,2-Offline
        boolean bFitIAC = false, bFitTAC = false;

        if (terminal.getbForceOnline() != 0) {
            //终端强制联机
            return TRANS_ONLINE;
        }

//        if (m_CardDataTable[ICC_Index_IACDenial].bExist == 0) {
        if (berHouse.findFirst(PbocTag.IAC_DENIAL) == null) {
            //IAC-denial not exist
//            memset(m_CardInfo.IACDenial, 0, 5);
            berHouse.add(PbocTag.IAC_DENIAL, new byte[]{0, 0, 0, 0, 0});
        }
//        if (m_CardDataTable[ICC_Index_IACOnline].bExist == 0) {
        if (berHouse.findFirst(PbocTag.IAC_ONLINE) == null) {
            //IAC-online not exist
//            memset(m_CardInfo.IACOnline, 0xFF, 5);
            berHouse.add(PbocTag.IAC_ONLINE, new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF});
        }
//        if (m_CardDataTable[ICC_Index_IACDefault].bExist == 0) {
        if (berHouse.findFirst(PbocTag.IAC_DEFAULT) == null) {
            //IAC-default not exist
//            memset(m_CardInfo.IACDefault, 0xFF, 5);
            berHouse.add(PbocTag.IAC_DEFAULT, new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF});
        }

//        if (m_TermDataTable[TERM_Index_TACDenial].bExist == 0) {
        if (terminal.getTACDenial() == null) {
//            memset(m_TermInfo.TACDenial, 0, 5);
            terminal.setTACDenial(new byte[]{0, 0, 0, 0, 0});
        }
//        if (m_TermDataTable[TERM_Index_TACOnline].bExist == 0) {
        if (terminal.getTACOnline() == null) {
//            memset(m_TermInfo.TACOnline, 0, 5);
            terminal.setTACOnline(new byte[]{0, 0, 0, 0, 0});
//            m_TermInfo.TACOnline[0] |= 0xC8;//EMVapp,p35
            terminal.orTACOnline(0, (byte) 0xC8);
        }
//        if (m_TermDataTable[TERM_Index_TACDefault].bExist == 0) {
        if (terminal.getTACDefault() == null) {
//            memset(m_TermInfo.TACDefault, 0, 5);
            terminal.setTACDefault(new byte[]{0, 0, 0, 0, 0});
//            m_TermInfo.TACDefault[0] |= 0xC8;
            terminal.orTACDefault(0, (byte) 0xC8);
        }

//        if (!memcmp(m_TermInfo.TVR, "\x00\x00\x00\x00\x00", 5)) {
        if (Arrays.equals(terminal.getTVR(), new byte[]{0, 0, 0, 0, 0})) {
            return TRANS_OFFLINE;
        }

        byte[] IACDenial = berHouse.findFirst(PbocTag.IAC_DENIAL).v.getBytes();
        for (i = 0; i < 5; i++) {
            k = terminal.getTVR()[i];
//            if ((k & m_CardInfo.IACDenial[i]) != 0)
            if ((k & IACDenial[i]) != 0)
                bFitIAC = true;
            //if(m_TermInfo.VLPIndicator==1 && bCardConfirmVLP_EMV==1)
//            if (m_TermInfo.VLPIndicator == 1) {
            if (terminal.getVLPIndicator() == 1) {
//                if ((k & m_TermInfo.VLPTACDenial[i]) != 0) {
                if ((k & terminal.getVLPTACDenial()[i]) != 0) {
                    bFitTAC = true;
                }
            } else {
//                if ((k & m_TermInfo.TACDenial[i]) != 0) {
                if ((k & terminal.getTACDenial()[i]) != 0) {
                    bFitTAC = true;
                }
            }
        }

        if (bFitIAC || bFitTAC) {
            return TRANS_DENIAL;
        }

//        k = m_TermInfo.TermType & 0x0F;
        k = terminal.getTermType() & 0x0F;
        //if((k==1||k==2||k==4||k==5)&&(bAbleOnline_EMV))
        if ((k == 1 || k == 2 || k == 4 || k == 5))//wumiaojun 20130724 本程序默认支持物理联机
        {
            //Terminal has Online capability
            bFitIAC = false;
            bFitTAC = false;
            byte[] IACOnline = berHouse.findFirst(PbocTag.IAC_ONLINE).v.getBytes();
            for (i = 0; i < 5; i++) {
//                k = m_TermInfo.TVR[i];
                k = terminal.getTVR()[i];
//                if ((k & m_CardInfo.IACOnline[i]) != 0) {
                if ((k & IACOnline[i]) != 0) {
                    bFitIAC = true;
                }

//                if ((k & m_TermInfo.TACOnline[i]) != 0) {
                if ((k & terminal.getTACOnline()[i]) != 0) {
                    bFitTAC = true;
                }
            }
            if (bFitIAC || bFitTAC) {
                return TRANS_ONLINE;
            } else {
                return TRANS_OFFLINE;
            }
        }

        bFitIAC = false;
        bFitTAC = false;
        byte[] IACDefault = berHouse.findFirst(PbocTag.IAC_DEFAULT).v.getBytes();
        for (i = 0; i < 5; i++) {
            k = terminal.getTVR()[i];
            if ((k & IACDefault[i]) != 0) {
                bFitIAC = true;
            }

            //if(m_TermInfo.VLPIndicator==1 && bCardConfirmVLP_EMV==1)
            if (terminal.getVLPIndicator() == 1) {
                if ((k & terminal.getVLPTACDefault()[i]) != 0) {
                    bFitTAC = true;
                }
            } else {
                if ((k & terminal.getTACDefault()[i]) != 0) {
                    bFitTAC = true;
                }
            }
        }
        if (bFitIAC || bFitTAC) {
            return TRANS_DENIAL;
        } else {
            return TRANS_OFFLINE;
        }
    }

    /**
     * 联机处理
     */
    protected void onLineProcess() {
        Log.d(Config.APP_ID, "联机处理");
    }

    /**
     * 发卡行认证
     * EXTERNAL AUTHENTICATE命令/响应
     */
    protected long issuerVerify(Iso7816.BerHouse berHouse, Terminal terminal, Iso7816.StdTag tag, byte[] apc, byte[] arpc) {
        Log.d(Config.APP_ID, "发卡行认证");
        long lr;
        int nLen = 0;

//        memcpy(m_TermInfo.AuthRespCode, pucAuthRespCode, 2);
//        m_TermDataTable[TERM_Index_AuthorRespCode].bExist = 1;
        if (apc != null && apc.length == 2)
            terminal.setAuthRespCode(apc);

        if (!(.AIP[0] & 0x04))//ICC not support Issuer Authenticate.
        return 0;

        m_TermInfo.TSI[0] |= 0x10;//Issuer authentication was performed

        memcpy(apdu_s.Command, "\x00\x82\x00\x00", 4);
        memcpy(apdu_s.DataIn, pucARPC, 8);
        memcpy(apdu_s.DataIn + 8, m_TermInfo.AuthRespCode, 2);
        apdu_s.Lc = 10;
        apdu_s.Le = 0;
        lr = m_Reader.ICCCommand(m_hReader, m_dwProtocol, & apdu_s,&apdu_r);
        if (lr != 0) {
            m_TermInfo.TVR[4] |= 0x40;//Issuer authentication failed
            return lr;
        }

        return 0;
    }

    /**
     * 交易结束
     * GENERATE AC命令/响应
     */
    protected void transactionDone() {
        Log.d(Config.APP_ID, "交易结束");
    }

    /**
     * 发卡行脚本处理
     * 发卡行脚本命令/响应
     */
    protected void issuerScriptDeal() {
        Log.d(Config.APP_ID, "发卡行脚本处理");
    }

    class CVM {
        byte method;
        byte condition;
    }

    byte[] verifyARQC(Iso7816.BerHouse berHouse, byte[] bACKey, byte[] bMACKey, byte[] bENCKey, boolean bMasterKey) throws Exception {


//        unsigned char ucACKey[16] = {0};
//        unsigned char ucMACKey[16] = {0};
//        unsigned char ucENCKey[16] = {0};
//        unsigned char ucTACKey[16] = {0};
//        unsigned char ucTMACKey[16] = {0};
//        unsigned char ucTENCKey[16] = {0};
//        int nKeyLen = 16;
//        unsigned char ucData[300] = {0};
//        int nDataLen;
//        unsigned char ucARQC[16] = {0};
//        unsigned char ucARPC[16] = {0};
//        unsigned char ucARPCSrc[8] = {0};
//        unsigned char ucDIV[16] = {0};
//
//        memcpy(ucACKey, pACKey, 16);
//        memcpy(ucMACKey, pMACKey, 16);
//        memcpy(ucENCKey, pENCKey, 16);
//
//        char caPAN[21] = {0};
//        char caPANSeq[3] = {0};
//        BCDtoASCII(m_CardInfo.PAN, caPAN, m_CardInfo.PANLen*2);
//        BCDtoASCII(&m_CardInfo.PANSeq, caPANSeq, 2);
//        CString sz5A = CString(caPAN);
//        CString sz5F34 = CString(caPANSeq);
//        sz5A.Replace("F","");
//        sz5A.Replace("f","");
//        CString szDIV = "";

        if ((bACKey != null && bACKey.length == 16) || (bMACKey != null && bMACKey.length == 16) || (bENCKey != null && bENCKey.length == 16)) {
            String pan = berHouse.findFirst(PbocTag.PAN).v.toString();
            String panSeq = berHouse.findFirst(PbocTag.PAN_SERIAL).v.toString();
            String str5A = pan.toUpperCase().replace("F", "");
            String str5F34 = panSeq.trim();
            String strDIV;
            if (bMasterKey) {
                //发卡行主密钥
//                szDIV = sz5A + sz5F34;
//                szDIV = szDIV.Right(16);
//                memset(ucDIV, 0xFF, sizeof(ucDIV));
//                AsciiToBcd(szDIV.GetBuffer(0), ucDIV, szDIV.GetLength());
//FIXME:                Do_XOR(ucDIV + 8, ucDIV, 8);
                strDIV = str5A + str5F34;
                int len = strDIV.length();
                if (len > 16)
                    strDIV = strDIV.substring(strDIV.length() - 16);
                byte[] bDIV = new byte[16];
                Arrays.fill(bDIV, (byte) 0xFF);
                System.arraycopy(Util.toBytes(strDIV), 0, bDIV, 0, 8);
                bDIV = Util.calXOR(Arrays.copyOf(bDIV, 8), Arrays.copyOfRange(bDIV, 8, 16), 8);
                //算法标识 04 使用国密算法
                if ((EMVMode == TRANS_MODE_PBOC) && (berHouse.findFirst(PbocTag.ISSUER_APP_DATA).v.getBytes()[7] == (byte) 0x04)) {
//                    SM4Encrypt(ucDIV, 16, CD_SM4_ECB, NULL, ucACKey, ucTACKey, & nKeyLen);
//                    SM4Encrypt(ucDIV, 16, CD_SM4_ECB, NULL, ucMACKey, ucTMACKey, & nKeyLen);
//                    SM4Encrypt(ucDIV, 16, CD_SM4_ECB, NULL, ucENCKey, ucTENCKey, & nKeyLen);
                }
                // 国际算法
                else {
                    DES.encryptMode()
                    TriDESEncrypt(ucDIV, 16, CD_DES_ECB, NULL, ucACKey, 16, ucTACKey, & nKeyLen);
                    TriDESEncrypt(ucDIV, 16, CD_DES_ECB, NULL, ucMACKey, 16, ucTMACKey, & nKeyLen);
                    TriDESEncrypt(ucDIV, 16, CD_DES_ECB, NULL, ucENCKey, 16, ucTENCKey, & nKeyLen);

                }
            } // end bMasterKey
        }

        BCDtoASCII(m_CardInfo.PAN, caPAN, m_CardInfo.PANLen * 2);
        BCDtoASCII( & m_CardInfo.PANSeq, caPANSeq, 2);
        CString sz5A = CString(caPAN);
        CString sz5F34 = CString(caPANSeq);
        sz5A.Replace("F", "");
        sz5A.Replace("f", "");
        CString szDIV = "";

        if (bMasterKey) {
            if ((m_nEMVMode == TRANS_MODE_PBOC) && m_CardInfo.IssuAppData[7] == 0x04)//算法标识 04 使用国密算法
            {
                //发卡行主密钥
                szDIV = sz5A + sz5F34;
                szDIV = szDIV.Right(16);
                memset(ucDIV, 0xFF, sizeof(ucDIV));
                AsciiToBcd(szDIV.GetBuffer(0), ucDIV, szDIV.GetLength());
                Do_XOR(ucDIV + 8, ucDIV, 8);
                SM4Encrypt(ucDIV, 16, CD_SM4_ECB, NULL, ucACKey, ucTACKey, & nKeyLen);
                SM4Encrypt(ucDIV, 16, CD_SM4_ECB, NULL, ucMACKey, ucTMACKey, & nKeyLen);
                SM4Encrypt(ucDIV, 16, CD_SM4_ECB, NULL, ucENCKey, ucTENCKey, & nKeyLen);
            } else {
                //发卡行主密钥
                szDIV = sz5A + sz5F34;
                szDIV = szDIV.Right(16);
                memset(ucDIV, 0xFF, sizeof(ucDIV));
                AsciiToBcd(szDIV.GetBuffer(0), ucDIV, szDIV.GetLength());
                Do_XOR(ucDIV + 8, ucDIV, 8);
                TriDESEncrypt(ucDIV, 16, CD_DES_ECB, NULL, ucACKey, 16, ucTACKey, & nKeyLen);
                TriDESEncrypt(ucDIV, 16, CD_DES_ECB, NULL, ucMACKey, 16, ucTMACKey, & nKeyLen);
                TriDESEncrypt(ucDIV, 16, CD_DES_ECB, NULL, ucENCKey, 16, ucTENCKey, & nKeyLen);
            }

        } else {
            memcpy(ucTACKey, ucACKey, 16);
            memcpy(ucTMACKey, ucMACKey, 16);
            memcpy(ucTENCKey, ucENCKey, 16);
        }

        memset(ucDIV, 0, 16);
        memset(ucACKey, 0, 16);
        memset(ucMACKey, 0, 16);
        memset(ucENCKey, 0, 16);

        if ((m_EMVMain.m_nEMVMode == TRANS_MODE_VISA) && (m_EMVMain.m_CardInfo.IssuAppData[2] == 0x0A || m_EMVMain.m_CardInfo.IssuAppData[2] == 0x11))//密文版本号10或17
        {
            memcpy(ucDIV + 6, m_EMVMain.m_CardInfo.ATC, 2);
            memset(ucDIV + 14, 0xFF, 2);
            Do_XOR(ucDIV + 14, ucDIV + 6, 2);
            memcpy(ucACKey, ucTACKey, 16);
            memcpy(ucMACKey, ucTMACKey, 16);
            memcpy(ucENCKey, ucTENCKey, 16);
            Do_XOR(ucMACKey, ucDIV, 16);
            Do_XOR(ucENCKey, ucDIV, 16);
        } else if ((m_EMVMain.m_nEMVMode == TRANS_MODE_PBOC) && m_EMVMain.m_CardInfo.IssuAppData[7] == 0x04)//算法标识 04 使用国密算法
        {
            memcpy(ucDIV + 6, m_EMVMain.m_CardInfo.ATC, 2);
            memset(ucDIV + 14, 0xFF, 2);
            Do_XOR(ucDIV + 14, ucDIV + 6, 2);
            SM4Encrypt(ucDIV, 16, CD_SM4_ECB, NULL, ucTACKey, ucACKey, & nKeyLen);
            SM4Encrypt(ucDIV, 16, CD_SM4_ECB, NULL, ucTMACKey, ucMACKey, & nKeyLen);
            SM4Encrypt(ucDIV, 16, CD_SM4_ECB, NULL, ucTENCKey, ucENCKey, & nKeyLen);
        } else if ((m_EMVMain.m_nEMVMode == TRANS_MODE_MASTERCARD) && (m_EMVMain.m_CardInfo.IssuAppData[1] == 0x10 || m_EMVMain.m_CardInfo.IssuAppData[1] == 0x11)) {
            //MasterCard Proprietary SKD 等文档
            memcpy(ucDIV, m_CardInfo.ATC, 2);
            memcpy(ucDIV + 4, m_TermInfo.UnpredictNum, 4);
            memcpy(ucDIV + 8, ucDIV, 8);
            ucDIV[2] = 0xF0;
            ucDIV[10] = 0x0F;
            TriDESEncrypt(ucDIV, 16, CD_DES_ECB, NULL, ucTACKey, 16, ucACKey, & nKeyLen);

            memset(ucDIV, 0, sizeof(ucDIV));
            memcpy(ucDIV, m_CardInfo.AppCrypt, 8);
            memcpy(ucDIV + 8, ucDIV, 8);
            ucDIV[2] = 0xF0;
            ucDIV[10] = 0x0F;
            TriDESEncrypt(ucDIV, 16, CD_DES_ECB, NULL, ucTMACKey, 16, ucMACKey, & nKeyLen);
            TriDESEncrypt(ucDIV, 16, CD_DES_ECB, NULL, ucTENCKey, 16, ucENCKey, & nKeyLen);
        } else if ((m_EMVMain.m_nEMVMode == TRANS_MODE_PBOC) && (m_EMVMain.m_CardInfo.IssuAppData[2] == 0x01 || m_EMVMain.m_CardInfo.IssuAppData[2] == 0x17)) {
            memcpy(ucDIV + 6, m_CardInfo.ATC, 2);
            memset(ucDIV + 14, 0xFF, 2);
            Do_XOR(ucDIV + 14, ucDIV + 6, 2);
            TriDESEncrypt(ucDIV, 16, CD_DES_ECB, NULL, ucTACKey, 16, ucACKey, & nKeyLen);
            TriDESEncrypt(ucDIV, 16, CD_DES_ECB, NULL, ucTMACKey, 16, ucMACKey, & nKeyLen);
            TriDESEncrypt(ucDIV, 16, CD_DES_ECB, NULL, ucTENCKey, 16, ucENCKey, & nKeyLen);
        } else {
            //EMV CSK
            memcpy(ucDIV, m_CardInfo.ATC, 2);
            memcpy(ucDIV + 8, ucDIV, 2);
            ucDIV[2] = 0xF0;
            ucDIV[10] = 0x0F;
            TriDESEncrypt(ucDIV, 16, CD_DES_ECB, NULL, ucTACKey, 16, ucACKey, & nKeyLen);

            memset(ucDIV, 0, sizeof(ucDIV));
            memcpy(ucDIV, m_CardInfo.AppCrypt, 8);
            memcpy(ucDIV + 8, ucDIV, 8);
            ucDIV[2] = 0xF0;
            ucDIV[10] = 0x0F;
            TriDESEncrypt(ucDIV, 16, CD_DES_ECB, NULL, ucTMACKey, 16, ucMACKey, & nKeyLen);
            TriDESEncrypt(ucDIV, 16, CD_DES_ECB, NULL, ucTENCKey, 16, ucENCKey, & nKeyLen);
        }

        if (m_CardInfo.IssuAppData[2] == 0x17)//密文版本号17 for PBOC
        {
            nDataLen = 0;
            memcpy(ucData, m_TermInfo.AmtAuthNum, sizeof(m_TermInfo.AmtAuthNum));
            nDataLen += sizeof(m_TermInfo.AmtAuthNum);

            memcpy(ucData + nDataLen, m_TermInfo.UnpredictNum, sizeof(m_TermInfo.UnpredictNum));
            nDataLen += sizeof(m_TermInfo.UnpredictNum);

            memcpy(ucData + nDataLen, m_CardInfo.ATC, sizeof(m_CardInfo.ATC));
            nDataLen += sizeof(m_CardInfo.ATC);

            memcpy(ucData + nDataLen, m_CardInfo.IssuAppData + 4, 1);
            nDataLen += 1;

            if ((m_nEMVMode == TRANS_MODE_PBOC) && m_CardInfo.IssuAppData[7] == 0x04)//算法标识 04 使用国密算法
            {
                SM4MAC(ucData, nDataLen, ISO_PADDING_2, CD_SM4_MAC_16, NULL, ucACKey, ucARQC);
            } else {
                TriDESMAC(ucData, nDataLen, ISO_PADDING_2, NULL, ucACKey, 16, ucARQC);
            }

        } else if ((m_nEMVMode == TRANS_MODE_VISA) && m_CardInfo.IssuAppData[2] == 0x11)//密文版本号17 for VISA
        {
            nDataLen = 0;
            memcpy(ucData, m_TermInfo.AmtAuthNum, sizeof(m_TermInfo.AmtAuthNum));
            nDataLen += sizeof(m_TermInfo.AmtAuthNum);

            memcpy(ucData + nDataLen, m_TermInfo.UnpredictNum, sizeof(m_TermInfo.UnpredictNum));
            nDataLen += sizeof(m_TermInfo.UnpredictNum);

            memcpy(ucData + nDataLen, m_CardInfo.ATC, sizeof(m_CardInfo.ATC));
            nDataLen += sizeof(m_CardInfo.ATC);

            memcpy(ucData + nDataLen, m_CardInfo.IssuAppData + 4, 1);
            nDataLen += 1;

            TriDESMAC(ucData, nDataLen, ISO_PADDING_1, NULL, ucACKey, 16, ucARQC);
        } else if ((m_nEMVMode == TRANS_MODE_VISA) && m_CardInfo.IssuAppData[2] == 0x0A)//密文版本号10 for VISA
        {
            nDataLen = 0;
            memcpy(ucData, m_TermInfo.AmtAuthNum, sizeof(m_TermInfo.AmtAuthNum));
            nDataLen += sizeof(m_TermInfo.AmtAuthNum);

            memcpy(ucData + nDataLen, m_TermInfo.AmtOtherNum, sizeof(m_TermInfo.AmtOtherNum));
            nDataLen += sizeof(m_TermInfo.AmtOtherNum);

            memcpy(ucData + nDataLen, m_TermInfo.CountryCode, sizeof(m_TermInfo.CountryCode));
            nDataLen += sizeof(m_TermInfo.CountryCode);

            memcpy(ucData + nDataLen, m_TermInfo.TVR, sizeof(m_TermInfo.TVR));
            nDataLen += sizeof(m_TermInfo.TVR);

            memcpy(ucData + nDataLen, m_TermInfo.TransCurcyCode, sizeof(m_TermInfo.TransCurcyCode));
            nDataLen += sizeof(m_TermInfo.TransCurcyCode);

            memcpy(ucData + nDataLen, m_TermInfo.TransDate, sizeof(m_TermInfo.TransDate));    //交易日期
            nDataLen += sizeof(m_TermInfo.TransDate);

            ucData[nDataLen] = m_TermInfo.TransTypeValue;
            nDataLen += sizeof(m_TermInfo.TransTypeValue);

            memcpy(ucData + nDataLen, m_TermInfo.UnpredictNum, sizeof(m_TermInfo.UnpredictNum));
            nDataLen += sizeof(m_TermInfo.UnpredictNum);

            memcpy(ucData + nDataLen, m_CardInfo.AIP, sizeof(m_CardInfo.AIP));
            nDataLen += sizeof(m_CardInfo.AIP);

            memcpy(ucData + nDataLen, m_CardInfo.ATC, sizeof(m_CardInfo.ATC));
            nDataLen += sizeof(m_CardInfo.ATC);

            memcpy(ucData + nDataLen, m_CardInfo.IssuAppData + 3, 4);
            nDataLen += 4;

            TriDESMAC(ucData, nDataLen, ISO_PADDING_1, NULL, ucACKey, 16, ucARQC);
        } else if ((m_nEMVMode == TRANS_MODE_MASTERCARD) && (m_CardInfo.IssuAppData[1] == 0x10 || m_CardInfo.IssuAppData[1] == 0x11))//密文版本号10 for MASTERCARD
        {
            nDataLen = 0;
            memcpy(ucData, m_TermInfo.AmtAuthNum, sizeof(m_TermInfo.AmtAuthNum));
            nDataLen += sizeof(m_TermInfo.AmtAuthNum);

            memcpy(ucData + nDataLen, m_TermInfo.AmtOtherNum, sizeof(m_TermInfo.AmtOtherNum));
            nDataLen += sizeof(m_TermInfo.AmtOtherNum);

            memcpy(ucData + nDataLen, m_TermInfo.CountryCode, sizeof(m_TermInfo.CountryCode));
            nDataLen += sizeof(m_TermInfo.CountryCode);

            memcpy(ucData + nDataLen, m_TermInfo.TVR, sizeof(m_TermInfo.TVR));
            nDataLen += sizeof(m_TermInfo.TVR);

            memcpy(ucData + nDataLen, m_TermInfo.TransCurcyCode, sizeof(m_TermInfo.TransCurcyCode));
            nDataLen += sizeof(m_TermInfo.TransCurcyCode);

            memcpy(ucData + nDataLen, m_TermInfo.TransDate, sizeof(m_TermInfo.TransDate));    //交易日期
            nDataLen += sizeof(m_TermInfo.TransDate);

            ucData[nDataLen] = m_TermInfo.TransTypeValue;
            nDataLen += sizeof(m_TermInfo.TransTypeValue);

            memcpy(ucData + nDataLen, m_TermInfo.UnpredictNum, sizeof(m_TermInfo.UnpredictNum));
            nDataLen += sizeof(m_TermInfo.UnpredictNum);

            memcpy(ucData + nDataLen, m_CardInfo.AIP, sizeof(m_CardInfo.AIP));
            nDataLen += sizeof(m_CardInfo.AIP);

            memcpy(ucData + nDataLen, m_CardInfo.ATC, sizeof(m_CardInfo.ATC));
            nDataLen += sizeof(m_CardInfo.ATC);

            memcpy(ucData + nDataLen, m_CardInfo.IssuAppData + 2, 6);
            nDataLen += 6;

            TriDESMAC(ucData, nDataLen, ISO_PADDING_2, NULL, ucACKey, 16, ucARQC);
        } else if (m_nEMVMode == TRANS_MODE_MASTERCARD)//for MASTERCARD
        {
            nDataLen = 0;
            memcpy(ucData, m_TermInfo.AmtAuthNum, sizeof(m_TermInfo.AmtAuthNum));
            nDataLen += sizeof(m_TermInfo.AmtAuthNum);

            memcpy(ucData + nDataLen, m_TermInfo.AmtOtherNum, sizeof(m_TermInfo.AmtOtherNum));
            nDataLen += sizeof(m_TermInfo.AmtOtherNum);

            memcpy(ucData + nDataLen, m_TermInfo.CountryCode, sizeof(m_TermInfo.CountryCode));
            nDataLen += sizeof(m_TermInfo.CountryCode);

            memcpy(ucData + nDataLen, m_TermInfo.TVR, sizeof(m_TermInfo.TVR));
            nDataLen += sizeof(m_TermInfo.TVR);

            memcpy(ucData + nDataLen, m_TermInfo.TransCurcyCode, sizeof(m_TermInfo.TransCurcyCode));
            nDataLen += sizeof(m_TermInfo.TransCurcyCode);

            memcpy(ucData + nDataLen, m_TermInfo.TransDate, sizeof(m_TermInfo.TransDate));    //交易日期
            nDataLen += sizeof(m_TermInfo.TransDate);

            ucData[nDataLen] = m_TermInfo.TransTypeValue;
            nDataLen += sizeof(m_TermInfo.TransTypeValue);

            memcpy(ucData + nDataLen, m_TermInfo.UnpredictNum, sizeof(m_TermInfo.UnpredictNum));
            nDataLen += sizeof(m_TermInfo.UnpredictNum);

            memcpy(ucData + nDataLen, m_CardInfo.AIP, sizeof(m_CardInfo.AIP));
            nDataLen += sizeof(m_CardInfo.AIP);

            memcpy(ucData + nDataLen, m_CardInfo.ATC, sizeof(m_CardInfo.ATC));
            nDataLen += sizeof(m_CardInfo.ATC);

            memcpy(ucData + nDataLen, m_CardInfo.IssuAppData + 2, 6);
            nDataLen += 6;

            TriDESMAC(ucData, nDataLen, ISO_PADDING_2, NULL, ucACKey, 16, ucARQC);
        } else {
            nDataLen = 0;
            memcpy(ucData, m_TermInfo.AmtAuthNum, sizeof(m_TermInfo.AmtAuthNum));
            nDataLen += sizeof(m_TermInfo.AmtAuthNum);

            memcpy(ucData + nDataLen, m_TermInfo.AmtOtherNum, sizeof(m_TermInfo.AmtOtherNum));
            nDataLen += sizeof(m_TermInfo.AmtOtherNum);

            memcpy(ucData + nDataLen, m_TermInfo.CountryCode, sizeof(m_TermInfo.CountryCode));
            nDataLen += sizeof(m_TermInfo.CountryCode);

            memcpy(ucData + nDataLen, m_TermInfo.TVR, sizeof(m_TermInfo.TVR));
            nDataLen += sizeof(m_TermInfo.TVR);

            memcpy(ucData + nDataLen, m_TermInfo.TransCurcyCode, sizeof(m_TermInfo.TransCurcyCode));
            nDataLen += sizeof(m_TermInfo.TransCurcyCode);

            memcpy(ucData + nDataLen, m_TermInfo.TransDate, sizeof(m_TermInfo.TransDate));    //交易日期
            nDataLen += sizeof(m_TermInfo.TransDate);

            ucData[nDataLen] = m_TermInfo.TransTypeValue;
            nDataLen += sizeof(m_TermInfo.TransTypeValue);

            memcpy(ucData + nDataLen, m_TermInfo.UnpredictNum, sizeof(m_TermInfo.UnpredictNum));
            nDataLen += sizeof(m_TermInfo.UnpredictNum);

            memcpy(ucData + nDataLen, m_CardInfo.AIP, sizeof(m_CardInfo.AIP));
            nDataLen += sizeof(m_CardInfo.AIP);

            memcpy(ucData + nDataLen, m_CardInfo.ATC, sizeof(m_CardInfo.ATC));
            nDataLen += sizeof(m_CardInfo.ATC);

            memcpy(ucData + nDataLen, m_CardInfo.IssuAppData + 3, 4);
            nDataLen += 4;

            if ((m_nEMVMode == TRANS_MODE_PBOC) && m_CardInfo.IssuAppData[7] == 0x04)//算法标识 04 使用国密算法
            {
                SM4MAC(ucData, nDataLen, ISO_PADDING_2, CD_SM4_MAC_16, NULL, ucACKey, ucARQC);
            } else {
                TriDESMAC(ucData, nDataLen, ISO_PADDING_2, NULL, ucACKey, 16, ucARQC);
            }
        }

        if (memcmp(ucARQC, m_CardInfo.AppCrypt, 8) != 0) {
            return ERR_EMV_AC;
        }

        if (m_CardInfo.CryptInfo == 0x80) {
            //验证ARQC通过，保存密钥
            memcpy(m_ucACKey, ucTACKey, 16);
            memcpy(m_ucMACKey, ucTMACKey, 16);
            memcpy(m_ucENCKey, ucTENCKey, 16);
            memcpy(m_ucSACKey, ucACKey, 16);
            memcpy(m_ucSMACKey, ucMACKey, 16);
            memcpy(m_ucSENCKey, ucENCKey, 16);
            m_bVerifyAC = TRUE;
        }

        byte ucARC[ 2]={
            0
        } ;
        memcpy(ucARC, "\x30\x30", 2);
        byte ucArcbuf[ 8]={
            0
        } ;
        memset(ucARPCSrc, 0, 8);
        memcpy(ucArcbuf, ucARC, 2);

        for (int i = 0; i < 8; i++) {
            ucARPCSrc[i] = m_CardInfo.AppCrypt[i] ^ ucArcbuf[i];
        }

        if ((m_nEMVMode == TRANS_MODE_PBOC) && m_CardInfo.IssuAppData[7] == 0x04)//算法标识 04 使用国密算法
        {
            SM4Encrypt(ucARPCSrc, 8, CD_SM4_ECB, NULL, ucACKey, ucARPC, & nDataLen);
        } else if ((m_nEMVMode == TRANS_MODE_MASTERCARD) && (m_CardInfo.IssuAppData[1] == 0x10 || m_CardInfo.IssuAppData[1] == 0x11)) {
            memcpy(ucACKey, ucTACKey, 16);
            TriDESEncrypt(ucARPCSrc, 8, CD_DES_ECB, NULL, ucACKey, 16, ucARPC, & nDataLen);
        } else {
            TriDESEncrypt(ucARPCSrc, 8, CD_DES_ECB, NULL, ucACKey, 16, ucARPC, & nDataLen);
        }

        memcpy(pucIAData, ucARC, 2);
        memcpy(pucIAData + 2, ucARPC, 8);

        return 0;
    }
}
