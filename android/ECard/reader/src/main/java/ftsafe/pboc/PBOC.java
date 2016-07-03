package ftsafe.pboc;

import android.util.Log;

import java.util.HashMap;
import java.util.Iterator;

import ftsafe.common.Util;
import ftsafe.reader.Answer;
import ftsafe.reader.Reader;
import ftsafe.common.BaseFunction;
import ftsafe.common.BasePboc;
import ftsafe.common.Utils;

public class PBOC {
    private static final String TAG = "com.ftsafe.pboc";

    private final String SW_SUCCESS = "9000";
    private String caExponent = "10001";
    private String caPK = "A49097144F48886BB00843DC40725DB620D95B0485F46908946F0A2385B323D59355F133DAED8287A15EFF17F1A8EC6C9E804492F2E0E00E56B91A5484670DDA205583277A9B4AA449540050065AD5BA8E6DFDE56C762E0C24505664AA75433E66EB6E63141916FA068A8E9BA033CCDCF185994DF55D071C2C19462448134B3F65C04D3DDCC95BA3191E3EDA80FE338E4D469D91502B4D5C8C6D449B38E3D3870EB09FA2A0B77A1734D2A70C977295D3";
    //IC卡回应
    Answer answer = new Answer();
    //设备对象
    Reader reader;
    BasePboc bp = new BasePboc();
    BaseFunction bf = new BaseFunction();
    String cmdStr = null;
    String P1 = null;
    String P2 = null;
    String Lc = null;
    String Le = "00";

    public PBOC(Reader reader) {
        this.reader = reader;
    }

    /**
     * PSE
     *
     * @return
     */
    public String pse() {
        cmdStr = "00A404000E315041592E5359532E4444463031";
        byte[] cmd = Util.toBytes(cmdStr);
        try {
            answer = reader.sendAPDU(cmd);
            if (!answer.sw.equals(SW_SUCCESS))
                return answer.sw;
            return answer.value;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }

    /**
     * PPSE
     *
     * @return
     */
    public String ppse() {
        byte[] cmd = Util.toBytes("00A404000E325041592E5359532E4444463031");
        try {
            answer = reader.sendAPDU(cmd);
            if (!answer.sw.equals(SW_SUCCESS))
                return answer.sw;
            return answer.value;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }

    /**
     * 选择AID
     *
     * @param aid
     * @return
     */
    public String selectAID(String aid) {
        cmdStr = "00A4040008" + aid;
        byte[] cmd = Util.toBytes(cmdStr);
        try {
            answer = reader.sendAPDU(cmd);
            if (!answer.sw.equals(SW_SUCCESS))
                return answer.sw;
            return answer.value;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }

    /**
     * Read CAPP Data
     * 读扩展应用数据
     *
     * @param SFI：短文件标识(手动转换)
     * @param ID：当卡片不支持扩展应用记录的 R-MAC 保护时,命令报文数据域包括 2 个字节的 ID 号;当卡片支
     *                         持扩展应用记录的 R-MAC 保护时,命令报文数据域包括 2 个字节的 ID 号和 8 个字节的终端随机数。
     * @return
     */
    public String readCappData(String SFI, String ID) {
        try {
            P2 = SFI;
            if (ID.length() == 4)
                Lc = "02";
            if (ID.length() == 20)
                Lc = "0A";
            cmdStr = "80B400" + P2 + Lc + ID + Le;
            byte[] cmd = Util.toBytes(cmdStr);

            answer = reader.sendAPDU(cmd);
            if (!answer.sw.equals(SW_SUCCESS))
                return answer.sw;
            return answer.value;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }

    //GPO
    public String gpo(String tlvStr, String money) {
        try {
            StringBuffer szVal = new StringBuffer(256);
            int nRet = bp.ftTLVGetStrVal(tlvStr, "9F38", szVal);
            if (nRet != bp.FTOK) {
                return "获取9F38失败";
            }
            //交易日期
            String szSysDate = bf.ftGetSysDate(0);
            //交易时间
            String szSysTime = bf.ftGetSysTime(0);
            //圈存金额
            String chargeMoney = money;//分为单位
            //随机数
            String random = bf.RandomString(8);
            //生成GPO数据，0:q交易，1:电子现金交易
            StringBuffer GPO = new StringBuffer(256);
            nRet = bp.ftGetGPO(szVal.toString(), chargeMoney, 3, random, szSysDate, szSysTime, GPO);
            if (nRet != bp.FTOK) {
                return "生成GPO数据失败";
            }
            cmdStr = GPO.toString();
            byte[] cmd = Util.toBytes(cmdStr);

            answer = reader.sendAPDU(cmd);
            if (!answer.sw.equals(SW_SUCCESS))
                return answer.sw;
            return answer.value;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }

    //update capp data
    public String updateCappData(String SFI, String cappData) {
        try {
            P2 = SFI;
            Lc = bf.ftDecToHex(cappData.length() / 2);
            cmdStr = "84DE00" + P2 + Lc + cappData + Le;
            byte[] cmd = Util.toBytes(cmdStr);

            answer = reader.sendAPDU(cmd);
            if (!answer.sw.equals(SW_SUCCESS))
                return answer.sw;
            return answer.value;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }

    /**
     * 读取单条纪录
     *
     * @param SFI      短文件表示符号(自动转换)
     * @param recordNo 记录号
     * @return
     */
    public String readRecord(String SFI, String recordNo) {
        try {
            StringBuffer dest = new StringBuffer(3);
            //转换SFI
            bp.ftReadRecordSFIConvert(SFI, dest);
            P2 = dest.toString();
            //转换记录数
//		P1 = bf.ftDecToHex(recordNo);
            P1 = recordNo;
            cmdStr = "00B2" + P1 + P2 + Le;
            byte[] cmd = Util.toBytes(cmdStr);

            answer = reader.sendAPDU(cmd);
            if (!answer.sw.equals(SW_SUCCESS))
                return answer.sw;
            return answer.value;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }

    /**
     * 获取CA公钥模
     *
     * @param RID CA公钥的RID
     * @param PKI CA公钥索引
     * @return CA公钥模和公钥RSA指数逗号","隔开
     */
    public String getCAPublicKeyModule(String RID, String PKI) {
        //CA公钥的RID
        String rid = RID;
        //CA公钥索引
        String pki = PKI;
        return caPK + "," + caExponent;
    }

    /**
     * Loop Read Record Response GPO Data
     * 根据gpo返回的内容循环读纪录，用于读交易记录或者圈存记录。
     *
     * @param gpoReponse：gpo返回内容。
     * @param tagMap：要读取数据的tag值。
     * @return
     */
    public String LoopReadGPOResponse(String gpoReponse, HashMap tagMap) {
        try {
            StringBuffer[] gpoDest = new StringBuffer[20];
            int destLen = bp.ftHandlGPOCMDReciveData(gpoReponse, gpoDest);
            //读各种记录信息
            for (int i = 0; i < destLen; i++) {
                String cmdStr = "00B2" + gpoDest[i].toString() + "00";
                byte[] cmd = Util.toBytes(cmdStr);

                answer = reader.sendAPDU(cmd);

                if (!answer.sw.equals(SW_SUCCESS))
                    return answer.sw;
                //分析tlv取得需要的tag
                Iterator tagIterator = tagMap.values().iterator();
                while (tagIterator.hasNext()) {
                    String tag = (String) tagIterator.next();
//				int nRet = bp.ftTLVGetStrVal(answer.value, tag, dest);
                }

            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }

    /**
     * Read Log Record
     * 读日志记录
     *
     * @param SFI：短文件标识符(自动转换)
     * @param recordQty：日志记录条数
     * @return
     */
    public String[] readLogRecord(String SFI, String recordQty) {
        try {
            StringBuffer dest = new StringBuffer(3);
            //转换SFI
            bp.ftReadRecordSFIConvert(SFI, dest);
            P2 = dest.toString();
            int logQty = bf.ftHexToDec(recordQty);
            String[] log = new String[logQty];
            for (int i = 1, k = 0; i <= logQty; i++, k++) {
                cmdStr = "00B20x" + P2 + Le;
                if (i == 10)
                    cmdStr = cmdStr.replace("x", "A");
                else
                    cmdStr = cmdStr.replace("x", Integer.toString(i));
                byte[] cmd = Util.toBytes(cmdStr);

                answer = reader.sendAPDU(cmd);

                if (!answer.sw.equals(SW_SUCCESS))
                    return log;
                log[k] = answer.value;
            }
            return log;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }

    //Read Log format
    public String readLogFormat() {
        try {
            byte[] cmd = Util.toBytes("80CA9F4F00");

            answer = reader.sendAPDU(cmd);

            if (!answer.sw.equals(SW_SUCCESS))
                return answer.sw;
            return answer.value;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }

    /**
     * 读电子现金余额
     *
     * @return String
     */
    public String readECBalance() {

        try {
            byte[] cmd = Util.toBytes("80CA9F7900");
            answer = reader.sendAPDU(cmd);
            if (!answer.sw.equals(SW_SUCCESS))
                return answer.sw;
            return answer.value;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }

    /**
     * 内部认证
     *
     * @param DDOL 动态数据认证数据对象列表
     * @return 标签是“80”的基本数据对象
     */
    public String internalAuthenticate(String DDOL) {

        try {
            cmdStr = "00880000";
            Lc = bf.ftDecToHex(DDOL.length() / 2);
            cmdStr = cmdStr + Lc + DDOL + "00";
            byte[] cmd = Util.toBytes(cmdStr);
            answer = reader.sendAPDU(cmd);
            if (!answer.sw.equals(SW_SUCCESS))
                return answer.sw;
            return answer.value;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }

    public static void main(String[] args) {
    }
}
