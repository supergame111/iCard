package com.ftsafe.iccd.ecard.reader.pboc;

import android.util.Log;

import com.ftsafe.iccd.ecard.Config;
import com.ftsafe.iccd.ecard.ECARDSPEC;
import com.ftsafe.iccd.ecard.SPEC;
import com.ftsafe.iccd.ecard.Terminal;
import com.ftsafe.iccd.ecard.bean.Application;
import com.ftsafe.iccd.ecard.bean.Card;
import com.ftsafe.iccd.ecard.bean.TerminalInfo;
import com.ftsafe.iccd.ecard.pojo.PbocTag;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import ftsafe.common.ErrMessage;
import ftsafe.common.Util;
import ftsafe.reader.Reader;
import ftsafe.reader.tech.Iso7816;

/**
 * Created by qingyuan on 2016/7/6.
 */
public abstract class StandardPboc {
    // 交易方法：联机，脱机，拒绝
    public enum TransMethod {
        TRANS_ONLINE, TRANS_OFFLINE, TRANS_DENIAL
    }

    // 交易模式：圈存，消费
    public enum TransMode {
        ECLOAD, ECPAY;
    }

    ECARDSPEC.EmvMode mEmvMode = ECARDSPEC.EmvMode.PBOC;

    private static Class<?>[][] applets = {{BeijingMunicipal.class}, {StandardECash.class,}};

    public static void readCard(Reader reader, Card card) throws Exception {

        try {
            if (reader == null || card == null)
                return;

            reader.powerOn();

            Iso7816.StdTag tag = new Iso7816.StdTag(reader);


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
        } catch (ErrMessage errMessage) {
            reader.powerOff();
            throw errMessage;
        }
    }

    public static void readCard(Reader reader, Class<?> cls, Card card) throws Exception {
        if (reader == null || cls == null)
            return;

        Iso7816.StdTag tag = new Iso7816.StdTag(reader);
        // 卡片上电
        reader.powerOn();

        final StandardPboc app = (StandardPboc) cls.newInstance();

        HINT hint = HINT.RESETANDGONEXT;
        while (true) {
            switch (hint) {

                case RESETANDGONEXT:
                    if (app.reset(tag) == false)
                        break;
                case GONEXT:
                    hint = app.readCard(tag, card);
                    break;
                default:
                    break;
            }
            if (hint == HINT.STOP)
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
    protected static int MAX_LOG = 10;
    protected static int SFI_LOG = 24;

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

    private static void buildDO(ByteBuffer out, int len, byte... val) {
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
            //Log.("9F38="+Util.toHexString(pdol));
            ArrayList<Iso7816.BerTLV> list = Iso7816.BerTLV.extractOptionList(pdol);
            for (Iso7816.BerTLV tlv : list) {
                final int tag = tlv.t.toInt();
                final int len = tlv.l.toInt();
                switch (tag) {
                    case 0x9F66:
                        // 终端交易属性
                        buildDO(buff, len, terminal.getTermTransAtr());
                        break;
                    case 0x9F02:
                        // 授权金额
                        buildDO(buff, len, terminal.getAmtAuthNum());
                        break;
                    case 0x9F03:
                        // 其它金额
                        buildDO(buff, len, terminal.getAmtOtherNum());
                        break;
                    case 0x9F1A:
                        // 终端国家代码
                        buildDO(buff, len, terminal.getCountryCode());
                        break;
                    case 0x9F37:
                        // 不可预知数
                        buildDO(buff, len, terminal.getUnpredictNum());
                        break;
                    case 0x5F2A:
                        // 交易货币代码
                        //buildPDO(buff, len, (byte) 0x01, (byte) 0x56);
                        buildDO(buff, len, terminal.getTransCurcyCode());
                        break;
                    case 0x95:
                        // 终端验证结果
                        //buildPDO(buff, len, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00);
                        buildDO(buff, len, terminal.getTVR());
                        break;
                    case 0x9A:
                        // 交易日期
                        //buildPDO(buff, len, Util.getSysDate(3));
                        buildDO(buff, len, terminal.getTransDate());
                        break;
                    case 0x9C:
                        // 交易类型
                        //buildPDO(buff, len, (byte) 0x00);
                        buildDO(buff, len, terminal.getTransType());
                        break;
                    case 0xDF60:
                        // CAPP交易指示位
                        //buildPDO(buff, len, (byte) 0x00);
                        buildDO(buff, len, terminal.getTermCappFlag());
                        break;
                    default:
                        throw null;
                }
            }
            // 更新数据长度
            buff.put(1, (byte) (buff.position() - 2));
        } catch (Exception e) {
            buff.position(2);
        }
        return Arrays.copyOfRange(buff.array(), 0, buff.position());
    }

    private byte[] buildDOL(byte[] dol, Terminal terminal) throws ErrMessage {
        final ByteBuffer buff = ByteBuffer.allocate(252);
        ArrayList<Iso7816.BerTLV> list = Iso7816.BerTLV.extractOptionList(dol);
        for (Iso7816.BerTLV tlv : list) {
            final Iso7816.BerT tag = tlv.t;
            final int len = tlv.l.toInt();
            final byte[] value = terminal.getValue(tag.getBytes());
            Log(1, "Tag=" + Util.toHexString(tag.getBytes()) + ",Len=" + len + ",Value=" + Util.toHexString(value));
            if (value != null) {
                buildDO(buff, len, value);
            }
        }
        // 更新数据长度
        //buff.put(1, (byte) (buff.position() - 2));
        Log(1, "buff len=" + buff.position());
        return Arrays.copyOfRange(buff.array(), 0, buff.position());
//
//        int index = 0, nLen, outLen = 0;
//        int nDOLLen = dol.length;
//        byte[] ucTag;
//        byte[] result = null;
//        while (index < nDOLLen - 1) {
//            ucTag = Arrays.copyOfRange(dol, index, 2);
//            if ((ucTag[0] & 0x1F) != 0x1F) { //单字节Tag
//
//                nLen = ucTag[1];
//                index += 2;
//            } else { //双字节Tag
//
//                nLen = dol[index + 2];
//                index += 3;
//
//                if (Arrays.equals(ucTag, new byte[]{(byte) 0xDF, (byte) 0x69}))
//                    isCardSMSupported = true;
//            }
//            if (nLen == 0 || ucTag[0] == 0)
//                continue;
//
//            Iso7816.BerTLV berTLV = berHouse.findFirst(ucTag);
//            if (berTLV != null) {
//                buildPDO(buff, len, terminal.getTermCappFlag());
//                result = Arrays.copyOfRange(berTLV.v.getBytes(), outLen, outLen + berTLV.length());
//                outLen += berTLV.length();
//            } else
//                outLen += nLen;
//
//            if (outLen > 252)
//                throw new ErrMessage("长度超过限制" + outLen + "bytes");
//
//        }
//        if (result != null)
//            return result;
//        else
//            return Arrays.copyOf(new byte[]{0x00}, outLen);
    }

    protected Application createApplication() {
        return new Application();
    }

    protected void configApplication(Application app) {
        app.setProperty(SPEC.PROP.ID, getApplicationId());
        app.setProperty(SPEC.PROP.CURRENCY, getCurrency());
    }

    protected void initialApp(Iso7816.StdTag tag, Iso7816.BerHouse berHouse, Terminal terminal) throws ErrMessage, IOException {
        Log(1, "应用初始化");
        byte[] aip = null, afl = null;
        // GPO
        byte[] pdol = buildPDOL(berHouse, terminal);

        Log.d(Config.APP_ID, "PDOL=" + Util.toHexString(pdol));

        Iso7816.Response rsp = tag.getProcessingOptions(pdol);
        if (!rsp.isOkey())
            throw new ErrMessage("GPO异常响应码:" + rsp.getSw12String());

        final byte[] data = rsp.getBytes();
        /*------------------------*/
        // 计算SFI
        /*------------------------*/
        if (data[0] == (byte) 0x80) {
            Log(1, "80模板=" + Util.toHexString(data));

            int dlen = data.length - 1;
            aip = Arrays.copyOfRange(data, 2, 4);
            afl = Arrays.copyOfRange(data, 4, dlen);
            berHouse.add(new Iso7816.BerT(PbocTag.APP_INTERCHANGE_PROFILE), aip);
            berHouse.add(new Iso7816.BerT(PbocTag.APP_FILE_LOCATOR), afl);
        } else if (data[0] == (byte) 0x77) {
            Log(1, "77模板=" + Util.toHexString(data));
            Iso7816.BerTLV.extractPrimitives(berHouse, rsp);
        } else {
            throw new ErrMessage("非法GPO响应数据=" + Util.toHexString(data));
        }

    }

    /**
     * 读应用数据
     * READ RECORD命令/响应，循环读取应用数据存入 {@code berHouse}
     *
     * @param tag      标准PBOC
     * @param berHouse TLV数据包
     */

    protected void readApplicationRecord(Iso7816.StdTag tag, Iso7816.BerHouse berHouse) {
        Log(1, "读取应用数据");
        byte[] aip = null;
        byte[] afl = null;
        Iso7816.BerTLV berTLV = berHouse.findFirst(PbocTag.APP_INTERCHANGE_PROFILE);
        if (berTLV != null)
            aip = berTLV.v.getBytes();
        berTLV = berHouse.findFirst(PbocTag.APP_FILE_LOCATOR);
        if (berTLV != null)
            afl = berTLV.v.getBytes();
        Log(1, "aip=" + Util.toHexString(aip) + ",afl=" + Util.toHexString(afl));
        try {
            if (aip != null && afl != null) {
            /*------------------------*/
                // RREAD RECORD
            /*------------------------*/
                int group = afl.length / 4;
                int sfi, nums, nume, flag;
                byte[] tmp;
                for (int i = 0; i < group; i++) {
                    tmp = Arrays.copyOfRange(afl, i * 4, (i + 1) * 4);
                    sfi = Util.BCDtoInt((byte) (tmp[0] >> 3));
                    nums = Util.BCDtoInt(tmp[1]);
                    nume = Util.BCDtoInt(tmp[2]);
                    flag = Util.BCDtoInt(tmp[3]);
                    Log(1, "sfi=" + sfi + ",nums=" + nums + ",nume=" + nume + ",flag=" + flag);
                    for (int j = nums; j <= nume; j++) {
                        Log(1, "读记录:SFI=" + sfi + ",NUM=" + j);

                        Iso7816.Response r = tag.readRecord(sfi, j);
                        if (r.isOkey())
                            Iso7816.BerTLV.extractPrimitives(berHouse, r);
                        else
                            throw new Exception("读SFI=" + sfi + ",NUM=" + j + "记录异常响应码:" + r.getSw12String());

                    }
                }
            } else
                throw new ErrMessage("读应用失败,aip=" + Util.toHexString(aip) + ",afl=" + Util.toHexString(afl));

        } catch (Exception e) {
            Log(4, e.getMessage());
        }
    }

    int internalAuthen(Iso7816.StdTag tag, Iso7816.BerHouse berHouse, Terminal terminal) throws ErrMessage, IOException {

        byte[] ddol;
        Iso7816.BerTLV DDOLTLV = berHouse.findFirst(PbocTag.DDOL);
        if (DDOLTLV == null)//ICC has no DDOL,tag:9F49
        {
            if (terminal.getTermDDOL() != null)
                ddol = buildDOL(terminal.getTermDDOL(), terminal);

            else
                return ErrCode.ERR_EMV_NoTermDDOL;//Terminal has also no DDOL

        } else {
            ddol = buildDOL(DDOLTLV.v.getBytes(), terminal);
        }
        if (ddol == null) {
            throw new ErrMessage("内部认证:构建DDOL失败");
        }

        Iso7816.Response r = tag.internalAuthenticate(ddol);
        if (!r.isOkey()) {
            throw new ErrMessage("内部认证:异常响应码" + r.getSw12String());
        }

        Iso7816.BerTLV tlv = Iso7816.BerTLV.read(r);
        if (tlv.t.match(PbocTag.RESPONSE_TEMPLATE_80)) {
            berHouse.add(PbocTag.SIGN_DYN_APP_DATA, tlv.v.getBytes());
        } else if (tlv.t.match(PbocTag.RESPONSE_TEMPLATE_77)) {
            parseData(tlv.v.getBytes(), berHouse, PARSE_DATA_COMMON);
        } else
            return ErrCode.ERR_EMV_InternalAuthRespData;
        Log(1, "内部认证完成");
        return 0;
    }

    /**
     * 脱机数据认证
     * INTERNAL AUTHENTICATE命令/响应
     */
    protected int offlineDataAuthenticate(Iso7816.StdTag tag, Iso7816.BerHouse berHouse, Terminal terminal) throws IOException, ErrMessage {
        Log(1, "脱机数据认证");
        int lr = 0; // 暂不实现脱机数据认证
        Iso7816.BerTLV aipTlv = berHouse.findFirst(PbocTag.APP_INTERCHANGE_PROFILE);
        if (aipTlv == null)
            throw new ErrMessage("没有AIP");
        byte[] termCapab = terminal.getTermCapab();
        if (termCapab == null)
            throw new ErrMessage("终端性能参数为空");
        byte[] AIP = aipTlv.v.getBytes();
        Calendar cal = Calendar.getInstance();
        byte[] ICCAPKI = null, IPK_CERT = null, ICC_PK_CERT = null;
        if ((AIP[0] & 0x01) != 0 && (termCapab[2] & 0x08) != 0) {
            //ICC and terminal support Combined DDA/AC.(EMV2000 & Bulletin No9, March 2002)
            String tmp = String.format("%02d:%02d:%02d 开始执行CDA", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));
            Log(1, tmp);
            Iso7816.BerTLV berTLV = berHouse.findFirst(PbocTag.CAPKI);
            if (berTLV != null)
                ICCAPKI = berTLV.v.getBytes();
            berTLV = berHouse.findFirst(PbocTag.IPK_CERT);
            if (berTLV != null)
                IPK_CERT = berTLV.v.getBytes();
            berTLV = berHouse.findFirst(PbocTag.ICC_PK_CERT);
            if (berTLV != null)
                ICC_PK_CERT = berTLV.v.getBytes();
            if (ICCAPKI != null && IPK_CERT != null && ICC_PK_CERT != null) {
                if (isCardSMSupported && terminal.getSMAlgSupp() == 0x01) {
                    //lr = EMV_SM2CDDAStepICPK( & m_ICCSM2Cert);
                } else {
                    //lr = EMV_CDDAStepICPK( & m_ICCCert);
                }
                if (lr != 0) {
                    Log(1, "脱机CDA认证失败!");
                    terminal.orTVR(0, (byte) 0x04);        //Offline Combined DDA/AC Generation failed
                    terminal.orTSI(0, (byte) 0x80);        //set bit 'Offline Data Authentication was performed' bit 1
                }
            } else {
                terminal.orTVR(0, (byte) 0x20);
            }
        } else if ((AIP[0] & 0x20) != 0 && (termCapab[2] & 0x40) != 0) {
            //ICC and terminal support dynamic data auth.
            String tmp = String.format("%02d:%02d:%02d 开始执行DDA", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));
            Log(1, tmp);
            Iso7816.BerTLV SignDynAppData = berHouse.findFirst(PbocTag.SIGN_DYN_APP_DATA);
            if (SignDynAppData == null) {
                // 内部认证
                lr = internalAuthen(tag, berHouse, terminal);
                if (lr != 0) {
                    terminal.orTVR(0, (byte) 0x20);
                    terminal.orTVR(0, (byte) 0x08);            //Offline dynamic Data Authentication failed
                    terminal.orTSI(0, (byte) 0x80);            //Offline Data Authentication was performed
                    return lr;
                }
            }
            Iso7816.BerTLV berTLV = berHouse.findFirst(PbocTag.CAPKI);
            if (berTLV != null)
                ICCAPKI = berTLV.v.getBytes();
            berTLV = berHouse.findFirst(PbocTag.IPK_CERT);
            if (berTLV != null)
                IPK_CERT = berTLV.v.getBytes();
            berTLV = berHouse.findFirst(PbocTag.ICC_PK_CERT);
            if (berTLV != null)
                ICC_PK_CERT = berTLV.v.getBytes();
            if (ICCAPKI != null && IPK_CERT != null && ICC_PK_CERT != null) {
                if (isCardSMSupported && terminal.getSMAlgSupp() == 0x01) {
                    //lr = EMV_SM2DynamicAuth();
                } else {
                    //lr = EMV_DynamicAuth();
                }
                if (lr != 0) {
                    Log(1, "脱机DDA认证失败");
                    terminal.orTVR(0, (byte) 0x08);            //Offline dynamic Data Authentication failed
                    terminal.orTSI(0, (byte) 0x80);          //Offline Data Authentication was performed
                }

            } else {
                terminal.orTVR(0, (byte) 0x20);
            }
        } else if ((AIP[0] & 0x40) != 0 && (terminal.getTermCapab()[2] & 0x80) != 0) {
            //ICC and terminal support static data auth.
            String tmp = String.format("%02d:%02d:%02d 开始执行SDA", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));
            Log(1, tmp);
            Iso7816.BerTLV berTLV = berHouse.findFirst(PbocTag.CAPKI);
            if (berTLV != null)
                ICCAPKI = berTLV.v.getBytes();
            berTLV = berHouse.findFirst(PbocTag.IPK_CERT);
            if (berTLV != null)
                IPK_CERT = berTLV.v.getBytes();

            Iso7816.BerTLV SignDynAppData = berHouse.findFirst(PbocTag.SIGN_DYN_APP_DATA);
            if (ICCAPKI != null && IPK_CERT != null && SignDynAppData != null) {
                //lr = EMV_StaticAuth();
                if (lr != 0) {
                    Log(1, "脱机SDA认证失败!");
                    terminal.orTVR(0, (byte) 0x40);            //Offline dynamic Data Authentication failed
                    terminal.orTSI(0, (byte) 0x80);            //Offline Data Authentication was performed
                }
            } else {
                terminal.orTVR(0, (byte) 0x20);
            }
        } else {
            //The bit should be set to 1 according to test script 2CI.023.00
            String tmp = String.format("%02d:%02d:%02d,交易未执行脱机数据认证", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));
            Log(1, tmp);
            terminal.orTVR(0, (byte) 0x80);            //Offline Data Authentication not performed
            terminal.orTSI(0, (byte) 0x7F);            //set bit 'Offline data authentication was performed' bit 0.
        }
        return 0;
    }

    /**
     * 处理限制
     */
    protected void processRestrict(Iso7816.BerHouse berHouse, Terminal terminal) {
        Log(1, "处理限制");
        // check app version
        Iso7816.BerTLV berTLV = berHouse.findFirst(PbocTag.CARD_APP_VER);
        if (berTLV != null && terminal.getAppVer() != null) {
            Log(1, "处理限制-应用版本检查");
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
            Log(1, "处理限制-应用用途控制检查");
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
                Log(1, "处理限制-发卡行国家代码检查");
                byte[] countryCode = berTLV.v.getBytes();
                if (Arrays.equals(countryCode, terminal.getCountryCode())) {//domestic
                    if (terminal.getTransType() == ECARDSPEC.TERM_TRANS_CASH) {
                        if ((AUC[0] & 0x80) == 0)// if‘Valid for domestic cash transactions’bit not on.
                        {
                            bTestFail = true;
                        }
                    }
                    if (terminal.getTransType() == ECARDSPEC.TERM_TRANS_GOODS) {
                        if ((AUC[0] & 0x20) == 0)// if‘Valid for domestic goods’bit not on.
                        {
                            bTestFail = true;
                        }
                    }
                    if (terminal.getTransType() == ECARDSPEC.TERM_TRANS_SERVICE) {
                        if ((AUC[0] & 0x08) == 0)// if‘Valid for domestic services’bit not on.
                        {
                            bTestFail = true;
                        }
                    }
                    //if(AmtOtherBin!=0 || m_TermInfo.TransType==CASHBACK)
                    if (!Arrays.equals(terminal.getAmtOtherNum(), new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00})
                            || terminal.getTransType() == ECARDSPEC.TERM_TRANS_CASHBACK) {
                        if ((AUC[1] & 0x80) != 0x80)// if‘domestic cashback allowed’bit not on.
                        {
                            bTestFail = true;
                        }
                    }
                } else {//international,terminal country code differ from issuer country code
                    if (terminal.getTransType() == ECARDSPEC.TERM_TRANS_CASH) {
                        if ((AUC[0] & 0x40) == 0)// if‘Valid for international cash transactions’bit not on.
                        {
                            bTestFail = true;
                        }
                    }
                    if (terminal.getTransType() == ECARDSPEC.TERM_TRANS_GOODS) {
                        if ((AUC[0] & 0x10) == 0)// if‘Valid for international goods’bit not on.
                        {
                            bTestFail = true;
                        }
                    }
                    if (terminal.getTransType() == ECARDSPEC.TERM_TRANS_SERVICE) {
                        if ((AUC[0] & 0x04) == 0)// if‘Valid for international goods’bit not on.
                        {
                            bTestFail = true;
                        }
                    }
                    //if(AmtOtherBin!=0 || m_TermInfo.TransType==CASHBACK)
                    if (!Arrays.equals(terminal.getAmtAuthNum(), new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00})
                            && (terminal.getTransType() == ECARDSPEC.TERM_TRANS_CASHBACK)) {
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
            Log(1, "处理限制-卡片生效日期检查");
            byte[] EffectDate = berTLV.v.getBytes();
            if (Util.memcmp(currentDate, EffectDate, 4) < 0) { // 小于生效日期
                //set‘Application not yet effective’bit 1
                terminal.orTVR(1, (byte) 0x20);
            }
        }
        berTLV = berHouse.findFirst(PbocTag.APP_EXPIRATION_DATE);
        if (berTLV != null) {
            Log(1, "处理限制-卡片失效日期检查");
            byte[] EffectDate = berTLV.v.getBytes();
            if (Util.memcmp(currentDate, EffectDate, 4) > 0) { // 超过失效日期
                //set‘Expired application’bit 1
                terminal.orTVR(1, (byte) 0x40);
            }
        }
    }

    final static byte CVR_UNKNOWN = 0;
    final static byte CVR_FAIL = 1;
    final static byte CVR_SUCCESS = 2;

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
    protected long cardHolderVerify(Iso7816.BerHouse berHouse, Terminal terminal) throws ErrMessage {
        Log(1, "持卡人验证");
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
//        if (berTLV == null)
//            throw new ErrMessage("CVM List为空");
        if (berTLV == null)//CVM List is absent.
        {
            terminal.orTVR(0, (byte) 0x20);//ICC Data Missing.
            return 0;
        }
        byte[] CCVMList = berTLV.v.getBytes();
        int CCVMListLen = berTLV.length(); // 从卡片获取的CVMList长度

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
//        ArrayList<CVM> CVMs = new ArrayList<>(64);
        CVM[] CVMs = new CVM[64];
        for (i = 0; i != CVMListLen; i++) {
            CVM cvm = new CVM();
            cvm.method = CCVMList[i * 2 + 8];
            cvm.condition = CCVMList[i * 2 + 9];
            CVMs[i] = cvm;
        }

        byte[] transCurcyCode = terminal.getTransCurcyCode();
        byte[] cTransCurcyCode = berHouse.findFirst(PbocTag.APP_CURRENCY_CODE).v.getBytes();
        for (i = 0; i != CVMListLen; i++) {
            switch (CVMs[i].condition) {
                case 0x00://always
                    lr = emvPerformCVM(CVMs[i], terminal);
                    if (lr == 0) {
                        terminal.orTSI(0, (byte) 0x40);
                        return 0;
                    } else {
                        if ((CVMs[i].method & 0x40) == 0x00) {
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
                    if (terminal.getTransType() == ECARDSPEC.TERM_TRANS_CASH && (terminal.getTermType() & 0x0F) > 3) {
                        lr = emvPerformCVM(CVMs[i], terminal);
                        if (lr == 0) {
                            terminal.orTSI(0, (byte) 0x40);
                            return 0;
                        } else {
                            if ((CVMs[i].method & 0x40) == 0x00) {
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
                    if (terminal.getTransType() != ECARDSPEC.TERM_TRANS_CASH
                            && terminal.getTransType() != ECARDSPEC.TERM_TRANS_CASHBACK
                            && lAmtOtherBin == 0) {
                        lr = emvPerformCVM(CVMs[i], terminal);
                        if (lr == 0) {
                            terminal.orTSI(0, (byte) 0x40);
                            return 0;
                        } else {
                            if ((CVMs[i].method & 0x40) == 0x00) {
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
                    lr = emvPerformCVM(CVMs[i], terminal);
                    if (lr == 0) {
                        terminal.orTSI(0, (byte) 0x40);
                        return 0;
                    } else {
                        if ((CVMs[i].method & 0x40) == 0x00) {
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
                    if (terminal.getTransType() == ECARDSPEC.TERM_TRANS_CASH && (terminal.getTermType() & 0x0F) <= 3) {
                        lr = emvPerformCVM(CVMs[i], terminal);
                        if (lr == 0) {
                            terminal.orTSI(0, (byte) 0x40);
                            return 0;
                        } else {
                            if ((CVMs[i].method & 0x40) == 0x00) {
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
                    if (terminal.getTransType() == ECARDSPEC.TERM_TRANS_CASHBACK || lAmtOtherBin == 0) {
                        lr = emvPerformCVM(CVMs[i], terminal);
                        if (lr == 0) {
                            terminal.orTSI(0, (byte) 0x40);
                            return 0;
                        } else {
                            if ((CVMs[i].method & 0x40) == 0x00) {
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
                        lr = emvPerformCVM(CVMs[i], terminal);
                        if (lr == 0) {
                            terminal.orTSI(0, (byte) 0x40);
                            return 0;
                        } else {
                            if ((CVMs[i].method & 0x40) == 0x00) {
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
                        lr = emvPerformCVM(CVMs[i], terminal);
                        if (lr == 0) {
                            terminal.orTSI(0, (byte) 0x40);
                            return 0;
                        } else {
                            if ((CVMs[i].method & 0x40) == 0x00) {
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
                        lr = emvPerformCVM(CVMs[i], terminal);
                        if (lr == 0) {
                            terminal.orTSI(0, (byte) 0x40);
                            return 0;
                        } else {
                            if ((CVMs[i].method & 0x40) == 0x00) {
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
                        lr = emvPerformCVM(CVMs[i], terminal);
                        if (lr == 0) {
                            terminal.orTSI(0, (byte) 0x40);
                            return 0;
                        } else {
                            if ((CVMs[i].method & 0x40) == 0x00) {
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

    protected Iso7816.Response termRiskManage(Iso7816.StdTag tag, Iso7816.BerHouse berHouse, Terminal terminal) {
        Log(1, "终端风险管理");
        //CheckExceptionFile

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
                // 读取ATC
                if (berHouse.findFirst(PbocTag.ATC) == null) {

                    r = tag.getData(PbocTag.ATC);

                    if (!r.isOkey()) {
                        terminal.orTVR(0, (byte) 0x20);//set 'ICC data missing' bit 1.
                        return r;
                    }
                    berHouse.add(Iso7816.BerTLV.read(r));
                }
                // 读上次联机应用交易计数器
                if (berHouse.findFirst(PbocTag.LOATC) == null) {
                    r = tag.getData(PbocTag.LOATC);
                    if (!r.isOkey()) {
                        terminal.orTVR(0, (byte) 0x20);//set 'ICC data missing' bit 1.
                        return r;
                    }
                    berHouse.add(Iso7816.BerTLV.read(r));
                }
                // ATC and LOATC are high byte ahead,low byte behind.
                byte[] ATC = berHouse.findFirst(PbocTag.ATC).v.getBytes();
                int nATC = ATC[0] * 256 + ATC[1];
                byte[] LOATC = berHouse.findFirst(PbocTag.LOATC).v.getBytes();
                int nLOATC = LOATC[0] * 256 + LOATC[1];
                if (nATC <= nLOATC) {
                    terminal.orTVR(3, (byte) 0x40);//set 'Lower consecutive online limit exceeded' bit 1.
                    terminal.orTVR(3, (byte) 0x20);//set 'Upper consecutive online limit exceeded' bit 1.
                } else {
                    int counts = nATC - nLOATC;
                    //FIXME:什么意思
                    if (counts > berHouse.findFirst(PbocTag.LCOL).v.toInt()) {
                        terminal.orTVR(3, (byte) 0x40);//set 'Lower consecutive online limit exceeded' bit 1.
                    }

                    if (counts > berHouse.findFirst(PbocTag.UCOL).v.toInt()) {
                        terminal.orTVR(3, (byte) 0x20);//set 'Upper consecutive online limit exceeded' bit 1.
                    }
                }
            }

            //Check New Card
            if (mEmvMode != ECARDSPEC.EmvMode.MASTERCARD) {
                if (berHouse.findFirst(PbocTag.LOATC) == null) {
                    r = tag.getData(PbocTag.LOATC);
                    if (r.isOkey()) {
                        berHouse.add(Iso7816.BerTLV.read(r));
                    }
                } else {
                    berHouse.add(PbocTag.LOATC, new byte[]{0x00, 0x00});
                    terminal.orTVR(1, (byte) 0x80);//set 'New Card' bit 1
                }
            }
        } catch (IOException e) {
            Log(4, e.getMessage());
        }

        return r;
    }

    /**
     * 终端行为分析
     * GENERATE AC命令
     */
    protected TransMethod termActionAnalyze(Iso7816.BerHouse berHouse, Terminal terminal) throws ErrMessage {
        Log(1, "终端行为分析");
        int i, k;//TermAnaResult,CardAnaResult;//0-Denial,1-Online,2-Offline
        boolean bFitIAC = false, bFitTAC = false;

        if (terminal.getbForceOnline() != 0) {
            //终端强制联机
            return TransMethod.TRANS_ONLINE;
        }

//        if (m_CardDataTable[ICC_Index_IACDenial].bExist == 0) {
        if (berHouse.findFirst(PbocTag.IAC_DENIAL) == null) {
            //IAC-denial not exist
//            memset(m_CardInfo.IACDenial, 0, 5);
            berHouse.add(new Iso7816.BerT(PbocTag.IAC_DENIAL), new byte[]{0, 0, 0, 0, 0});
        }
//        if (m_CardDataTable[ICC_Index_IACOnline].bExist == 0) {
        if (berHouse.findFirst(PbocTag.IAC_ONLINE) == null) {
            //IAC-online not exist
//            memset(m_CardInfo.IACOnline, 0xFF, 5);
            berHouse.add(new Iso7816.BerT(PbocTag.IAC_ONLINE), new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF});
        }
//        if (m_CardDataTable[ICC_Index_IACDefault].bExist == 0) {
        if (berHouse.findFirst(PbocTag.IAC_DEFAULT) == null) {
            //IAC-default not exist
//            memset(m_CardInfo.IACDefault, 0xFF, 5);
            berHouse.add(new Iso7816.BerT(PbocTag.IAC_DEFAULT), new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF});
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

        byte[] tvr = terminal.getTVR();
        if (tvr == null) {
            throw new ErrMessage("终端验证结果为空");
        }
        if (Arrays.equals(tvr, new byte[]{0, 0, 0, 0, 0})) {
            return TransMethod.TRANS_OFFLINE;
        }
        Iso7816.BerTLV IACTlv = berHouse.findFirst(PbocTag.IAC_DENIAL);
        if (IACTlv == null)
            throw new ErrMessage("IAC_DENIAL为空");
        byte[] IACDenial = IACTlv.v.getBytes();

        for (i = 0; i < 5; i++) {
            k = tvr[i];
            if ((k & IACDenial[i]) != 0)
                bFitIAC = true;
            if (terminal.getVLPIndicator() == 1) {
                if (terminal.getVLPTACDenial() != null && (k & terminal.getVLPTACDenial()[i]) != 0) {
                    bFitTAC = true;
                }
            } else {
                if (terminal.getTACDenial() != null && (k & terminal.getTACDenial()[i]) != 0) {
                    bFitTAC = true;
                }
            }
        }

        if (bFitIAC || bFitTAC) {
            return TransMethod.TRANS_DENIAL;
        }

        k = terminal.getTermType() & 0x0F;
        if ((k == 1 || k == 2 || k == 4 || k == 5))//wumiaojun 20130724 本程序默认支持物理联机
        {
            //Terminal has Online capability
            bFitIAC = false;
            bFitTAC = false;
            IACTlv = berHouse.findFirst(PbocTag.IAC_ONLINE);
            if (IACTlv == null)
                throw new ErrMessage("IAC_ONLINE为空");
            byte[] IACOnline = IACTlv.v.getBytes();
            for (i = 0; i < 5; i++) {
                k = terminal.getTVR()[i];
                if ((k & IACOnline[i]) != 0) {
                    bFitIAC = true;
                }
                if ((k & terminal.getTACOnline()[i]) != 0) {
                    bFitTAC = true;
                }
            }
            if (bFitIAC || bFitTAC) {
                return TransMethod.TRANS_ONLINE;
            } else {
                return TransMethod.TRANS_OFFLINE;
            }
        }

        bFitIAC = false;
        bFitTAC = false;
        IACTlv = berHouse.findFirst(PbocTag.IAC_DEFAULT);
        if (IACTlv == null)
            throw new ErrMessage("IAC_DEFAULT为空");
        byte[] IACDefault = IACTlv.v.getBytes();
        for (i = 0; i < 5; i++) {
            k = terminal.getTVR()[i];
            if ((k & IACDefault[i]) != 0) {
                bFitIAC = true;
            }

            if (terminal.getVLPIndicator() == 1) {
                if (terminal.getVLPTACDefault() != null && (k & terminal.getVLPTACDefault()[i]) != 0) {
                    bFitTAC = true;
                }
            } else {
                if (terminal.getTACDefault() != null && (k & terminal.getTACDefault()[i]) != 0) {
                    bFitTAC = true;
                }
            }
        }
        if (bFitIAC || bFitTAC) {
            return TransMethod.TRANS_DENIAL;
        } else {
            return TransMethod.TRANS_OFFLINE;
        }
    }

    //CardInfo cardInfo;
    TerminalInfo terminalInfo;
    int CARD_INFO_CAPACITY = 500;
    int TERMINAL_INFO_CAPACITY = 500;

    protected static final int GAC_1 = 1;
    protected static final int GAC_2 = 2;
    protected static final int PARSE_DATA_COMMON = 3;

    protected Iso7816.Response gacProcess(Iso7816.BerHouse berHouse, Terminal terminal, Iso7816.StdTag tag, int gacType, TransMethod transMethod) throws ErrMessage, IOException {
        Log(1, "第" + gacType + "次GAC");
        byte p1 = 0;
        byte[] AIP = berHouse.findFirst(PbocTag.APP_INTERCHANGE_PROFILE).v.getBytes();

        if (transMethod == TransMethod.TRANS_DENIAL) { // TRANS_DENIAL
            p1 = 0x00; // get AAC
            if (terminal.getAuthRespCode() == null) {
                if (gacType == GAC_1)
                    terminal.setAuthRespCode(new byte[]{'Z', '1'});
                else
                    terminal.setAuthRespCode(new byte[]{'Z', '3'});

            }

        } else if (transMethod == TransMethod.TRANS_ONLINE) { // TRANS_ONLINE
            if ((AIP[0] & 0x01) != 0 && (terminal.getTermCapab()[2] & 0x08) != 0) {
                p1 = (byte) 0x90;// get ARQC

            } else {
                p1 = (byte) 0x80;// get ARQC
            }

        } else if (transMethod == TransMethod.TRANS_OFFLINE) { // TRANS_OFFLINE
            if ((AIP[0] & 0x01) != 0 && (terminal.getTermCapab()[2] & 0x08) != 0) {
                p1 = (byte) 0x50; // get ARQC
            } else {
                p1 = (byte) 0x40; // get TC
            }
            if (terminal.getAuthRespCode() == null) {
                if (gacType == GAC_1)
                    terminal.setAuthRespCode(new byte[]{'Y', '1'});
                else
                    terminal.setAuthRespCode(new byte[]{'Y', '3'});
            }
        } else {
            throw new ErrMessage("GENERATE AC:错误的交易类型");
        }
        byte[] CDOL1 = null, CDOL2 = null;
        Iso7816.BerTLV berTLV = berHouse.findFirst(PbocTag.CDOL1);
        if (berTLV != null)
            CDOL1 = berTLV.v.getBytes();
        berTLV = berHouse.findFirst(PbocTag.CDOL2);
        if (berTLV != null)
            CDOL2 = berTLV.v.getBytes();
        byte[] cdol = null;
        // CDOL
        if (gacType == GAC_1) {
            if (CDOL1 != null)
                cdol = buildDOL(CDOL1, terminal);
        } else if (gacType == GAC_2) {
            if (CDOL2 != null)
                cdol = buildDOL(CDOL2, terminal);
        } else
            throw new ErrMessage("GENERATE AC:错误的GAC类型");

        if (cdol == null)
            throw new ErrMessage("CDOL为空");

        Log(1, "P1=" + Util.toHexString(p1) + ",CDOL=" + Util.toHexString(cdol));
        // 发送GAC命令
        Iso7816.Response r = tag.generateAC(p1, cdol);
        if (!r.isOkey())
            throw new ErrMessage("GENETATE AC:第" + gacType +
                    "次GAC 错误响应码" + r.getSw12String() + ",P1=" + Util.toHexString(p1) + ",CDOL=" + Util.toHexString(cdol));
        return r;
    }

    private void parseData(byte[] data, Iso7816.BerHouse berHouse, int mode) throws ErrMessage {
        byte[] tag = new byte[2];
        int offset = 0;
        byte uctemp;
        int nLen, tl, t;
        int nDataLen = data.length;

        while (offset < nDataLen - 1) {
            System.arraycopy(data, offset, tag, 0, 2);
            if ((tag[0] & 0x1F) != 0x1F) {
                //单字节Tag
                nLen = tag[1];
                tag[1] = 0x0;
                if (tag[0] == 0x61 || tag[0] == 0x6F || tag[0] == 0x70 || tag[0] == 0x77 || tag[0] == 0xA5)//跳过Tag
                {
                    offset += (nLen > 0x80) ? 3 : 2;
                    continue;
                }
                offset += 2;
            } else {
                //双字节Tag
                nLen = data[offset + 2];
                if (Arrays.equals(tag, new byte[]{(byte) 0xBF, (byte) 0x0C})) //跳过Tag
                {
                    offset += (nLen > 0x80) ? 4 : 3;
                    continue;
                }
                offset += 3;
            }

            if (nLen == 0 || tag[0] == 0x0) {
                continue;
            }

            if (nLen > 0x80) {
                tl = 0;
                for (t = 0; t < nLen - 0x80; t++) {
                    uctemp = data[offset];
                    tl = 256 * tl + uctemp;
                    offset++;
                }
                nLen = tl;
            }

            if (berHouse.findFirst(tag) != null) {

                if (mode == GAC_2) {
                    // nothing done
                } else {
                    byte[] tmp = Arrays.copyOfRange(data, offset, offset + nLen);
                    berHouse.add(new Iso7816.BerT(tag), tmp);
                }
            }

            offset += nLen;
            if (offset > nDataLen) {
                //TLV结构有误
                throw new ErrMessage("TLV结构有错误");
            }
        } // end while
    }


    public static boolean isCardSMSupported = false;

    /**
     * 联机处理
     */
    protected TransMethod onlineProcess(Iso7816.BerHouse berHouse, Terminal terminal, Iso7816.Response resp) throws ErrMessage {
        Log(1, "联机处理");
        byte[] gacresp = resp.getBytes();
        int len = Util.toInt(gacresp[1]);
        if (gacresp[0] == (byte) 0x80) {
            byte[] ucPrimData = new byte[0];
            if (len == gacresp.length - 2)
                ucPrimData = Arrays.copyOfRange(resp.getBytes(), 2, len);

            //Log(1, Util.toHexString(resp.getBytes()));
            //Log(1, Util.toHexString(ucPrimData));
            // 密文信息数据 CID
            berHouse.add(new Iso7816.BerT(PbocTag.CRYPTOGRAM_INFO), new byte[]{ucPrimData[0]});
            // 应用交易计数器 ATC
            berHouse.add(new Iso7816.BerT(PbocTag.ATC), Arrays.copyOfRange(ucPrimData, 1, 3));
            // 应用密文 AC
            berHouse.add(new Iso7816.BerT(PbocTag.APP_CRYPTOGRAM), Arrays.copyOfRange(ucPrimData, 3, 11));
            // 发卡行应用数据
            berHouse.add(new Iso7816.BerT(PbocTag.ISSUER_APP_DATA), Arrays.copyOfRange(ucPrimData, 11, len));

        } else if (gacresp[0] == (byte) 0x77) {
            Iso7816.BerTLV.extractPrimitives(berHouse, resp);
        } else {
            throw new ErrMessage("联机处理:错误的GAC响应数据:" + Util.toHexString(gacresp));
        }

        byte cryptInfo = berHouse.findFirst(PbocTag.CRYPTOGRAM_INFO).v.getBytes()[0];
        //Log(1, Util.toHexString(cryptInfo));
        byte[] AIP = berHouse.findFirst(PbocTag.APP_INTERCHANGE_PROFILE).v.getBytes();
        Iso7816.BerTLV signDynAppDataTlv = berHouse.findFirst(PbocTag.SIGN_DYN_APP_DATA);
        if ((cryptInfo & 0x80) == 0x80) {
            if ((AIP[0] & 0x01) != 0 && (terminal.getTermCapab()[2] & 0x08) != 0 && signDynAppDataTlv != null) {
                byte[] signDynAppData = signDynAppDataTlv.v.getBytes();
                if (isCardSMSupported && (terminal.getSMAlgSupp() == 0x01)) {
                    // 复合动态数据认证（国密）
//                    lr = EMV_SM2CombineDDA(0);
                } else {
                    // 复合动态数据认证（国际）
//                    lr = EMV_CombineDDA(0);
                }
                int lr = 0;
                if (lr != 0) {
                    terminal.orTVR(0, (byte) 0x04);        //Offline Combined DDA/AC Generation failed
                    terminal.orTSI(0, (byte) 0x80);        //set bit 'Offline Data Authentication was performed' bit 1
                }
            }

            terminal.setAuthRespCode(new byte[]{(byte) 0, (byte) 0});
            return TransMethod.TRANS_ONLINE;

        } else if ((cryptInfo & 0x40) == 0x40) {

            if ((AIP[0] & 0x01) != 0 && (terminal.getTermCapab()[2] & 0x08) != 0 && signDynAppDataTlv != null) {
                byte[] signDynAppData = signDynAppDataTlv.v.getBytes();
                if (isCardSMSupported && (terminal.getSMAlgSupp() == 0x01)) {
//                    lr = EMV_SM2CombineDDA(0);
                } else {
//                    lr = EMV_CombineDDA(0);
                }
                int lr = 0;
                if (lr != 0) {
                    terminal.orTVR(0, (byte) 0x04);        //Offline Combined DDA/AC Generation failed
                    terminal.orTSI(0, (byte) 0x80);        //set bit 'Offline Data Authentication was performed' bit 1
                }
            }
            return TransMethod.TRANS_OFFLINE;

        } else
            return TransMethod.TRANS_DENIAL;

    }

    /**
     * 发卡行认证
     * EXTERNAL AUTHENTICATE命令/响应
     */
    protected Iso7816.Response issuerVerify(Iso7816.StdTag tag, Iso7816.BerHouse berHouse, Terminal terminal, byte[] arc, byte[] arpc) throws IOException, ErrMessage {
        Log(1, "发卡行认证");

//        memcpy(m_TermInfo.AuthRespCode, pucAuthRespCode, 2);
//        m_TermDataTable[TERM_Index_AuthorRespCode].bExist = 1;
        if (arc != null && arc.length == 2)
            terminal.setAuthRespCode(arc);

        if ((berHouse.findFirst(PbocTag.APP_INTERCHANGE_PROFILE).v.getBytes()[0] & 0x04) == 0)//ICC not support Issuer Authenticate.
            throw new ErrMessage("IC卡不支持发卡行认证");

        terminal.orTSI(0, (byte) 0x10);//Issuer authentication was performed

        byte[] b = Arrays.copyOf(arpc, 10);
        System.arraycopy(terminal.getAuthRespCode(), 0, b, 8, 2);

        // 外部认证
        Iso7816.Response r = tag.externalAuthenticate(b);
        if (!r.isOkey()) {
            terminal.orTVR(4, (byte) 0x40); //Issuer authentication failed
        }
        return r;
    }

    /**
     * 交易结束
     * GENERATE AC命令/响应
     */
    protected void transactionDone() {
        Log(1, "交易结束");
    }

    /**
     * 发卡行脚本处理
     * 发卡行脚本命令/响应
     */
    protected int issuerScriptProcess(Iso7816.StdTag tag, Terminal terminal, byte scriptType, ByteBuffer[] bufs) throws IOException, ErrMessage {
        Log(1, "发卡行脚本处理");
        Iso7816.Response r;
        int nErrNum = 0;

        for (int i = 1; i <= bufs.length; i++) {
            byte[] script = bufs[i - 1].array();
            Log(1, "发卡行第" + i + "条脚本:" + Util.toHexString(script));
            r = tag.sendAPDU(script);
            if (!r.isOkey()) {
                nErrNum++;
                if (scriptType == 0x71) {
                    terminal.orTVR(4, (byte) 0x20);
                    terminal.orTSI(0, (byte) 0x04);
                } else if (scriptType == 0x72) {
                    terminal.orTVR(4, (byte) 0x10);//Issuer authentication failed
                    terminal.orTSI(0, (byte) 0x04);
                }
                throw new ErrMessage("发卡行第" + i + "条脚本异常响应码:" + r.getSw12String());
            }

        }

        return nErrNum;

    }

    class CVM {
        byte method;
        byte condition;
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
