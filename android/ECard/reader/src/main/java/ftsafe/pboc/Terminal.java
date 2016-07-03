package ftsafe.pboc;

import ftsafe.common.Util;
import ftsafe.reader.Answer;
import ftsafe.reader.MiniPay;
import ftsafe.common.BaseFunction;

public class Terminal {
	BaseFunction bf = new BaseFunction();
	//IC卡回应
	Answer answer = new Answer();
	//设备对象
	MiniPay reader;
	public Terminal(MiniPay miniPay){
		reader = miniPay;
	}
	String cmd = null;
	/**
	 * type:0 读取终端设备信息(默认)
	 * type:1 读取终端设备状态
	 * @return 返回状态信息
	 */
	public String readTerminalInfo(int type){
		cmd = null;
		switch (type) {
			case 0:
				cmd = "7E100001";
				break;
			case 1:
				cmd = "7E10000001";
				break;
			default:
				cmd = "7E100001";
				break;
		}

		answer = reader.sendAPDU(Util.toBytes(cmd));
		if(!answer.sw.equals(Answer.SW_SUCCESS))
			return answer.sw;
		return answer.value;
	}
	/**
	 * 用于控制蜂鸣器的状态
	 * @param type
	 * 0:关闭(默认)
	 * 1:蜂鸣一声
	 * 2:持续蜂鸣
	 * 3:持续间歇蜂鸣
	 * @return 返回状态信息
	 */
	public String manageBuzzer(int type){
		cmd = "7E11000001";
		switch (type) {
			case 0://关闭
				cmd = cmd.concat("00");
				break;
			case 1://蜂鸣一声
				cmd = cmd.concat("01");
				break;
			case 2://持续蜂鸣
				cmd = cmd.concat("02");
				break;
			case 3://持续间歇蜂鸣
				cmd = cmd.concat("03");
				break;
			default:
				cmd = cmd.concat("00");
				break;
		}
		answer =  reader.sendAPDU(Util.toBytes(cmd));
		if(!answer.sw.equals(Answer.SW_SUCCESS))
			return answer.sw;
		return answer.value;
	}
	/**
	 * 控制终端LED
	 * @param type
	 * type: 0 LED关闭(默尔)
	 * type: 1 LED亮一次
	 * type: 2 LED长亮
	 * type: 3 LED闪烁
	 * @return 返回状态信息
	 */
	public String manageLED(int type){
		cmd = "7E12000001";
		switch (type) {
			case 0:
				cmd = cmd.concat("00");
				break;
			case 1:
				cmd = cmd.concat("01");
				break;
			case 2:
				cmd = cmd.concat("02");
				break;
			case 3:
				cmd = cmd.concat("03");
				break;
	
			default:
				cmd = cmd.concat("00");
				break;
		}
		answer =  reader.sendAPDU(Util.toBytes(cmd));
		if(!answer.sw.equals(Answer.SW_SUCCESS))
			return answer.sw;
		return answer.value;
	}
	/**
	 * 控制终端屏幕显示
	 * @param line 行号
	 * @param offset 信息偏移位置
	 * @param data 信息内容
	 * @return 返回状态信息
	 */
	public String configDisplayFormat(String line, String offset, String data){
		cmd = "7E13";
		StringBuffer Lc =  new StringBuffer();
		bf.ftLongToHex(data.length(), Lc, 0);
		cmd = cmd+line+offset+Lc+data;
		answer =  reader.sendAPDU(Util.toBytes(cmd));
		if(!answer.sw.equals(Answer.SW_SUCCESS))
			return answer.sw;
		return answer.value;
	}
	/**
	 * 
	 * @param type
	 * </br>
	 * type: 1 交易开始</br>
	 * type: 2 交易完成</br>
	 * type: 3 交易出错</br>
	 * type: 4 交易超时</br>
	 * type: 5 发卡行连接失败
	 * @return 返回状态信息
	 */
	public String exchangeStatus(int type){
		cmd = "7E14";
		String p1 = null;
		switch (type) {
			case 1:
				p1 = "01";
				break;
			case 2:
				p1 = "02";
				break;
			case 3:
				p1 = "03";
				break;
			case 4:
				p1 = "04";
				break;
			case 5:
				p1 = "05";
				break;
			default:
				p1 = "01";
				break;
		}
		cmd = p1 + "00";
		answer =  reader.sendAPDU(Util.toBytes(cmd));
		if(!answer.sw.equals("90".concat(p1)))
			return answer.sw;
		return answer.value;
	}
	/**
	 * 获取终端响应数据
	 * @param datalen 期望获取的响应数据长度
	 * @return 返回状态信息
	 */
	public String getTerminalResponse(String datalen){
		cmd = "7E150000";
		cmd = cmd.concat(datalen);
		answer =  reader.sendAPDU(Util.toBytes(cmd));
		if(!answer.sw.equals(Answer.SW_SUCCESS) || !answer.sw.startsWith("61"))
			return answer.sw;
		return answer.value;
	}
	/**
	 * 多指令处理命令
	 * @param multiplecmd 每条指令以‘,’(逗号 0x2C)为分隔 符
	 * @return 后一条指令的执行结果或错误响应返回
	 */
	public String multipleInstruction(String multiplecmd){
		cmd = "7E160000";
		StringBuffer Lc =  new StringBuffer();
		bf.ftLongToHex(multiplecmd.length(), Lc, 0);
		cmd = cmd + Lc + multiplecmd + "00";
		answer = reader.sendAPDU(Util.toBytes(cmd));
		if(!answer.sw.equals(Answer.SW_SUCCESS))
			return answer.sw;
		return answer.value;
	}
	/**
	 * 更新终端证书
	 * @param certificate 证书文件
	 * @return 返回状态信息
	 */
	public String updateTermCertificate(String certificate){
		cmd = "7E170000";
		StringBuffer Lc =  new StringBuffer();
		bf.ftLongToHex(certificate.length(), Lc, 0);
		cmd = cmd + Lc + certificate + "00";
		answer =  reader.sendAPDU(Util.toBytes(cmd));
		if(!answer.sw.equals(Answer.SW_SUCCESS))
			return answer.sw;
		return answer.value;
	}
	/**
	 * 
	 * @param certificate
	 * @param type 证书类型</br>
	 * type = 0 一级CA根证书</br>
	 * type = 1 二级CA根证书</br>
	 * type = 2 PIN加密证书</br>
	 * @return 返回状态信息
	 */
	public String addCertificate(String certificate, int type){
		cmd = "7E20";
		String p1 = null;
		String p2 = null;
		StringBuffer Lc =  new StringBuffer();
		switch (type) {
			case 0:
				p1 = "10";
				break;
			case 1:
				p1 = "20";
				break;
			case 2:
				p1 = "40";
				break;
			default:
				p1 = "10";
				break;
		}
		int certLen = certificate.length();
		int pkgQty = 0;
		if((certLen % 510) == 0)
			pkgQty = certLen/510;
		else
			pkgQty = certLen/510 + 1;
		String pkg[] = new String[pkgQty];
		for (int i = 0; i < pkgQty; i++) {
			if(i == 0)
				p2 = "80";
			else if(i == pkgQty -1)
				p2 = "40";
			else
				p2 = "00";
			pkg[i] = certificate.substring(i*510, (i+1)*510);
			bf.ftLongToHex(pkg[i].length(), Lc, 0);
			cmd = cmd + p1 + p2 + Lc + pkg[i];
			answer =  reader.sendAPDU(Util.toBytes(cmd));
			if(!answer.sw.equals(Answer.SW_SUCCESS))
				return answer.sw;
		}
		return answer.value;
	}
	/**
	 * 更新终端内已有证书
	 * @param data 终端号和固件版本号
	 * @param type 证书类型</br>
	 * type = 0 一级CA根证书</br>
	 * type = 1 二级CA根证书</br>
	 * type = 2 PIN加密证书</br>
	 * type = 3 终端证书</br>
	 * @return 返回状态信息
	 */
	public String updateCertificate(String certificate, int type){
		cmd = "7E21";
		String p1 = null;
		String p2 = null;
		StringBuffer Lc =  new StringBuffer();
		switch (type) {
			case 0:
				p1 = "10";
				break;
			case 1:
				p1 = "20";
				break;
			case 2:
				p1 = "40";
				break;
			case 3:
				p1 = "80";
				break;
			default:
				p1 = "10";
				break;
		}
		int certLen = certificate.length();
		int pkgQty = 0;
		if((certLen % 510) == 0)
			pkgQty = certLen/510;
		else
			pkgQty = certLen/510 + 1;
		String pkg[] = new String[pkgQty];
		for (int i = 0; i < pkgQty; i++) {
			if(i == 0)
				p2 = "80";
			else if(i == pkgQty -1)
				p2 = "40";
			else
				p2 = "00";
			pkg[i] = certificate.substring(i*510, (i+1)*510);
			bf.ftLongToHex(pkg[i].length(), Lc, 0);
			cmd = cmd + p1 + p2 + Lc + pkg[i];
			answer =  reader.sendAPDU(Util.toBytes(cmd));
			if(!answer.sw.equals(Answer.SW_SUCCESS))
				return answer.sw;
		}
		return answer.value;
	}
	/**
	 * 删除终端内已有公钥证书
	 * @param terminalInfo 终端号和固件版本号
	 * @param type 证书类型</br>
	 * type = 0 一级CA根证书</br>
	 * type = 1 二级CA根证书</br>
	 * type = 2 PIN加密证书</br>
	 * @return 返回状态信息
	 */
	public String deleteCertificate(String terminalInfo, int type){
		cmd = "7E22";
		String p1 = null;
		String p2 = "00";
		StringBuffer Lc =  new StringBuffer();
		switch (type) {
			case 0:
				p1 = "10";
				break;
			case 1:
				p1 = "20";
				break;
			case 2:
				p1 = "40";
				break;
			default:
				p1 = "10";
				break;
		}
		bf.ftLongToHex(terminalInfo.length(), Lc, 0);
		cmd = cmd + p1 + p2 + Lc +terminalInfo;
		answer =  reader.sendAPDU(Util.toBytes(cmd));
		if(!answer.sw.equals(Answer.SW_SUCCESS))
			return answer.sw;
		return answer.value;
	}
	/**
	 * 更新终端内已有证书
	 * @param type 证书类型</br>
	 * type = 0 一级CA根证书</br>
	 * type = 1 二级CA根证书</br>
	 * type = 2 PIN加密证书</br>
	 * type = 3 终端证书</br>
	 * @param hash = true读证书杂凑，hash = false读证书
	 * @return 返回状态信息
	 */
	public String readCertificate(int type, boolean hash){
		cmd = "7E23";
		String p1 = "00";
		if(hash)
			p1 = "01";
		String p2 = null;
		switch (type) {
			case 0:
				p2 = "01";
				break;
			case 1:
				p2 = "02";
				break;
			case 2:
				p2 = "03";
				break;
			case 3:
				p2 = "00";
				break;
			default:
				p2 = "01";
				break;
		}
		cmd = cmd + p1 + p2;
		answer =  reader.sendAPDU(Util.toBytes(cmd));
		if(!answer.sw.equals(Answer.SW_SUCCESS) && !answer.sw.startsWith("61"))
			return answer.sw;
		return answer.value;
	}
	/**
	 * 读取 READ CERTIFICATE 命令返回的响应数据
	 * @param dataLen
	 * @return 证书数据
	 */
	public String getCertResponse(String dataLen){
		cmd = "7E240000" + dataLen;
		answer =  reader.sendAPDU(Util.toBytes(cmd));
		if(!answer.sw.equals(Answer.SW_SUCCESS) && !answer.sw.startsWith("61"))
			return answer.sw;
		return answer.value;
	}
	/**
	 * 
	 * @param platform</br>
	 * 0 : 客户端(Android Pad)</br>
	 * 1 : 客户端(Android Phone)(默认)
	 * @return
	 */
	public String getClientHello(int platform){
		cmd = "7E25";
		String p1 = null;
		switch (platform) {
		case 0:
			p1 = "15";
			break;
		case 1:
			p1 = "16";
			break;

		default:
			p1 = "16";
			break;
		}
		String p2 = "00";
		String Le = "21";
		cmd = cmd + p1 + p2 + Le;
		answer =  reader.sendAPDU(Util.toBytes(cmd));
		if(!answer.sw.equals(Answer.SW_SUCCESS))
			return answer.sw;
		return answer.value;
	}
	
	public String hashServerCertificate(int Algorithm){
		cmd = "7E2600";
		String p2 = null;
		switch (Algorithm) {
		case 0:
			p2 = "00";
			break;

		default:
			break;
		}
		return null;
	}
}
