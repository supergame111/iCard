package ftsafe.common;

import java.util.HashMap;
import java.util.Map;

import ftsafe.common.encryption.DES;
import ftsafe.common.encryption.MAC;


public class BasePboc {
	int MAXTAGNUM = 40;
	public final int FTOK = 0;
	BaseFunction ftPublic = new BaseFunction();
	
	/**
	 * @函数名称 ftReadRecordSFIConvert
	 * @说明 PBOC3.0 计算SFI: 01->0C
	 * @param source (输入)两个字符的字符串
	 * @param dest (输出)
	 * @return 0 成功
	 */
	public int ftReadRecordSFIConvert(String source, StringBuffer dest){
		if(source.length() > 2)
			return -1;
		//01 -> 0000 0001
		StringBuffer binStr = new StringBuffer(9);
		ftPublic.ftDataToBitStringNoASCII(source,binStr);
		
		//0000 0001 -> 0000 1100
		StringBuffer binStr2 = new StringBuffer(9);
		ftReadRecordP2CtlF100(binStr.toString(), binStr2);
		
		//0000 1100 -> 0C
		StringBuffer binStr3 = new StringBuffer(3);
		ftPublic.ftBinToHex(binStr2.toString(), binStr3);
		
		//清空dest
		dest.setLength(0);
		dest.append(binStr3);
		return 0;
	}
	/**
	 * splitRecordTLV
	 * @param tlv 格式要求是70或61标签下的TLV模板数据。
	 * @return {tag:value}对
	 */
	public Map<String, String> ftSplitTemplateData(String tlv)
	{
		Map<String, String> map = new HashMap<String, String>();
		int nPos = 0;
		while(true)
		{
			//get tag
			String tag = tlv.substring(nPos, nPos+2);
			int tagLen = getTagLen(tag);
			if(tagLen == 2)
			{
				tag = tlv.substring(nPos, nPos+4);
				nPos += 4;
			}else
				nPos += 2;
			//get len
			String len = tlv.substring(nPos, nPos+2);
			//值长度
			int valLen = (int) (ftPublic.ftHexToLong(len))*2;
			//值开始位置
			nPos += 2;
			//get value
			String value = tlv.substring(nPos, nPos+valLen);
			//后移
			nPos += valLen;
			
			map.put(tag, value);
			
			if(nPos == tlv.length())
				break;
		}
		return map;
	}
	/**
	 * @函数名称 ftTLVGetStrVal
	 * @说明 根据tag获取tlv里对应的值
	 * @param tlv (输入)tlv字符串
	 * @param tag (输入)tag标签(逗号分割)
	 * @param dest (输出)对应的Tag值
	 * @return 0 成功
	 */
	public int ftTLVGetStrVal(String tlv, String tag, StringBuffer dest)
	{
		String szTags[] = new String[10];
		String szBuf = null;
		String szTag = null;
		String szTagBuf = null;
		String szLen1 = null;
		StringBuffer szVal = new StringBuffer(256);
		int nLayer = 0;
		int nPos;
		int nLen = tag.length();
		int i = 0;
		int nTagLen;
		int nValLen;
		boolean bFlag = false;
		while(true){
			nPos = tag.indexOf(",",i);
			if(nPos == -1){
				if(nLen > i)
				{
					szTags[nLayer] = tag.substring(i, nLen);
					nLayer++;
				}
				break;
			}else{
				szTags[nLayer] = tag.substring(i, nPos);
				i = nPos+1;
				nLayer++;
			}
			//最多处理9个Tag
			if(nLayer >= 9)
				break;
		}
		if(nLayer <= 0)
			return -1;
		//将TLV数据保存
		szBuf = tlv;
		for (i = 0; i < nLayer; i++) {
			nPos = 0;
			bFlag = true;
			//设置要获取数据的tag
			szTag = szTags[i];
			while(bFlag)
			{
				szTagBuf = szBuf.substring(nPos, nPos+2);
				nTagLen = getTagLen(szTagBuf);
				if(nTagLen == 2)
				{
					szTagBuf = szBuf.substring(nPos, nPos+4);
					nPos += 4;
				}else
					nPos += 2;
				if(szTagBuf.equals(szTag))
				{
					//取TAG数据长度高子节
					szLen1 = szBuf.substring(nPos, nPos+2);
					if(szLen1.equals("81")){
						nPos += 2;
						szLen1 = szBuf.substring(nPos, nPos+2);
					}
					//值长度
					nValLen = (int) (ftPublic.ftHexToLong(szLen1))*2;
					//值开始位置
					nPos += 2;
					//截取值数据
					if(szVal.length() == 0)
						szVal.append(szBuf.substring(nPos, nPos+nValLen));
					else
						szVal.append(",").append(szBuf.substring(nPos, nPos+nValLen));
//					szVal += szValTmp;
					
					//把值保存到szBuf中用于下次查询
					szBuf = szBuf.substring(nPos+nValLen);
					bFlag = false;
				}
				else{
					//取TAG数据长度高子节
					szLen1 = szBuf.substring(nPos, nPos+2);
					if(szLen1.equals("81")){
						nPos += 2;
						szLen1 = szBuf.substring(nPos, nPos+2);
					}
					//值长度
					nValLen = (int) (ftPublic.ftHexToLong(szLen1))*2;
					//值开始位置
					nPos += 2;
//					int k = szBuf.substring(nPos).length();
					if(nValLen != szBuf.substring(nPos).length()){
						//截取值数据
//						String szValTmp = szBuf.substring(nPos, nPos+nValLen);
						nPos += nValLen;
					}
//					else{
//						nPos += nValLen;
//					}
					
					//tag寻找完成
					if(nPos > szBuf.length()){
						bFlag = false;
					}
				}
			}
		}
		dest.setLength(0);
		dest.append(szVal);
		return FTOK;
	}
	/**
	 * @函数名称 getTagLen
	 * @param tag (输入)
	 * @return tag的长度 1:两个字符，2:四个字符
	 */
	public int getTagLen(String tag) {
		String buf1 = null;
		String buf2 = null;
		int buf;
		buf1 = tag.substring(0, 1);
		buf2 = tag.substring(1, 2);
		buf = (int) ftPublic.ftHexToLong(buf1);
		if(buf%2!=0 && buf2.equals("F"))
			return 2;
		else
			return 1;
	}
	/**
	 * @函数名称 ftGetGPO
	 * @说明 生成GPO指令 
	 * @param pRecvTag (输入)tag 9F38 的值
	 * @param pMoney (输入)交易金额 单位分
	 * @param pFlag (输入)交易类型 (0：q交易，1：电子现金交易，2：圈存，3:q扩展)
	 * @param pRandom (输入)4字节随机数 
	 * @param pSysDate (输入)系统日期
	 * @param pSysTime (输入)系统时间
	 * @param GPO (输出)完整的GPO指令
	 * @return 0 成功
	 */
	public int ftGetGPO(String pRecvTag, String pMoney, int pFlag, String pRandom, String pSysDate, String pSysTime, StringBuffer GPO){
		StringBuffer szTagValue = new StringBuffer();
		StringBuffer szHexLen = new StringBuffer();
		int len = 0;
		
		//根据tag 9F38 的值获取GPO的Data数据部分
		int nRet = ftGetValueByTag(pFlag,pMoney,pRandom,pSysDate,pSysTime,pRecvTag,szTagValue);
		if (nRet!=FTOK)
		{
			return -1;
		}
		//计算szTagvalue数据长度
		len = szTagValue.length()/2;
		ftPublic.ftLongToHex(len+2, szHexLen, 0);
		
		//清空GPO
		GPO.setLength(0);
		GPO.append("80A80000");
		GPO.append(szHexLen);
		GPO.append("83");
		ftPublic.ftLongToHex(len, szHexLen, 0);
		GPO.append(szHexLen);
		GPO.append(szTagValue);
		
		return 0;
	}
	/**
	 * @函数名称 ftGetValueByTag
	 * @说明 根据Tag值获得GPO数据
	 * @param pFlag (输入)交易类型 
	 *              0:	q交易
	 *              1:  电子现金交易
	 *              2:  圈存
	 * @param pMoney (输入) 交易金额
	 * @param pRandom (输入)4字节随机数
	 * @param pSysDate (输入)系统日期
	 * @param pSysTime (输入)系统时间
	 * @param pAllTag (输入)TAG集合数组
	 * @param outValue (输出)根据tag填充的完整数据 
	 * @return 0 成功
	 */
	public int ftGetValueByTag(int pFlag, String pMoney, String pRandom,
			String pSysDate, String pSysTime, String pAllTag, StringBuffer outValue) {
		String _9F66 = null; //终端交易属性
		String _9F02 = null; //授权金额
		String _9F03 = null; //其他金额
		String _9F1A = null; //终端国家代码
		String _95 = null;   //终端验证结果
		String _5F2A = null; //交易货币代码
		String _9A = null;       //交易日期
		String _9C = null;       //交易类型
		String _9F21 = null;     //交易时间
		String _9F37 = null;     //不可预知数
		String _9F33 = null;     //
		String _9F4E = null;    //商户名称
		String _DF60 = null;     //CAPP交易指示位
		String _9F7A = null;     //电子现金终端支持指示器
		String _DF69 = null;     //卡片认证相关数据
		String _8A = null;     //授权响应代码
		String _SysDate = pSysDate;
		String _SysTime = pSysTime;
		String szMoney = pMoney;
		String szData = new String();
		StringBuffer szTagList[] = new StringBuffer[MAXTAGNUM];
		
		int tagNum = 0;
		
		//授权响应代码
		_8A="3030";
	    //交易类型
	    _9C="00";
	    //卡片认证相关数据
	  	_DF69 = "00";
		//授权金额
		_9F02 = "000000000000".substring(0, 12-szMoney.length());
		_9F02 = _9F02.concat(szMoney);
		//其他金额
		_9F03 = "000000000000";
		//国家终端代码
		_9F1A = "0156";
		//终端验证结果
		_95 = "0000000000";
		//交易货币代码
		_5F2A = "0156";
		//交易日期
		_9A= pSysDate.substring(2, _SysDate.length());
		//交易时间
		_9F21 = _SysTime;
		//4字节随机数
		if(pRandom.length() != 8){
			return -1;
		}
		_9F37 = pRandom;
		//终端性能
		_9F33 = "E0E8E8";
		//商户名称
		_9F4E = "";
		//CAPP交易指示位
		_DF60 = "00";
		//电子现金终端支持指示器
		_9F7A = "00";
		//终端交易属性
		switch (pFlag) {
		case 0://q交易
			_9F66 = "26000000";
			break;
		case 1://电子现金交易
			_9F66 = "56000000";
			_9F7A = "01";
			_9C = "01";
			break;
		case 2://圈存
			_9F66 = "56000000";
			_9C = "02";
			break;
		case 3://q扩展交易
			_9F66 = "34000000";
			_DF60 = "01";
			_9C = "63";
			break;
		default:
			_9F66 = "26000000";
			break;
		}
		
		//解析tag数据
		tagNum = getTag(pAllTag, szTagList);
		
		if(tagNum == 0)
			return -2;
		for (int i = 0; i < tagNum; i++) {
			String temp = szTagList[i].toString();
			if(temp == null)
				continue;
			else if(temp.equals("8A"))
				szData = szData.concat(_8A);
			else if(temp.equals("9F02"))
				szData = szData.concat(_9F02);
			else if(temp.equals("9F03"))
				szData = szData.concat(_9F03);
			else if(temp.equals("95"))
				szData = szData.concat(_95);
			else if(temp.equals("9A"))
				szData = szData.concat(_9A);
			else if(temp.equals("9C"))
				szData = szData.concat(_9C);
			else if(temp.equals("9F21"))
				szData = szData.concat(_9F21);
			else if(temp.equals("9F37"))
				szData = szData.concat(_9F37);
			else if(temp.equals("9F1A"))
				szData = szData.concat(_9F1A);
			else if(temp.equals("5F2A"))
				szData = szData.concat(_5F2A);
			else if(temp.equals("9F66"))
				szData = szData.concat(_9F66);
			else if(temp.equals("9F33"))
				szData = szData.concat(_9F33);
			else if(temp.equals("9F4E"))
				szData = szData.concat(_9F4E);
			else if(temp.equals("9F7A"))
				szData = szData.concat(_9F7A);
			else if(temp.equals("DF69"))
				szData = szData.concat(_DF69);
			else if(temp.equals("DF60"))
				szData = szData.concat(_DF60);
		}
		outValue.setLength(0);
		outValue.append(szData);
		return 0;
	}
	/**
	 * 
	 * @param tagData
	 * @param tag
	 * @return
	 */
	public int getTag(String tagData, StringBuffer[] tag) {
		String[] szTagList = new String[MAXTAGNUM];
		String szBuf = null;
		
		int tagNum = 0;
		int tagLen;
		int len = tagData.length();
		int i=0;
		while(i<len){
			tagLen = 0;
			szBuf = tagData.substring(i, i+2);
			tagLen = getTagLen(szBuf.toString());
			if(tagLen == 1){
				szTagList[tagNum] = szBuf;
				i += 2;
			}
			else if(tagLen == 2){
				szTagList[tagNum] = tagData.substring(i, i+4);
				i += 4;
			}
			else
				return 0;
			i += 2;
			tagNum++;
		}
		for(int j = 0; j< tagNum; j++)
			tag[j] = new StringBuffer(szTagList[j]);
		return tagNum;
	}
	/**
	 * 获取tag标签和值长度
	 * @param tagData
	 * @param tag
	 * @return
	 */
	public int getTagAndValueLen(String tagData, StringBuffer[] tag, StringBuffer[] vlen) {
		String[] valueLenList = new String[MAXTAGNUM];
		String[] szTagList = new String[MAXTAGNUM];
		String szBuf = null;
		
		int tagNum = 0;
		int tagLen;
		int len = tagData.length();
		int i=0;
		while(i<len){
			tagLen = 0;
			szBuf = tagData.substring(i, i+2);
			tagLen = getTagLen(szBuf.toString());
			if(tagLen == 1){
				szTagList[tagNum] = szBuf;
				i += 2;
			}
			else if(tagLen == 2){
				szTagList[tagNum] = tagData.substring(i, i+4);
				i += 4;
			}
			else
				return 0;
			valueLenList[tagNum] = tagData.substring(i, i+2);
			i += 2;
			tagNum++;
		}
		for(int j = 0; j< tagNum; j++){
			tag[j] = new StringBuffer(szTagList[j]);
			vlen[j] = new StringBuffer(valueLenList[j]);
		}
		return tagNum;
	}
	/**
	 * @函数名称 ftReadRecordP2CtlF100
	 * @说明 控制字符左移右补100
	 * 例如：0001 0000->1000 0100 
	 * @param source (输入)P2控制符（文件组）
	 * @param dest (输出)二进制表示的字符串 
	 * @return 0 成功
	 */
	public int ftReadRecordP2CtlF100(String source, StringBuffer dest) {
		int sourceLen = source.length();
		if(sourceLen != 8)
			return -1;
		//左移source 3位存入临时变量temp
		StringBuffer temp = new StringBuffer(source.substring(sourceLen-5, sourceLen));
		//后面添加100
		temp.append("100");
		
		//清空dest
		dest.setLength(0);
		dest.append(temp);
		return 0;
	}
	/**
	 * @函数名称 ftHandlGPOCMDReciveData
	 * @说明 处理GPO指令 自动转换
	 * @param ReciveData (输入)
	 * @param dest (输出) 转换成SFI和记录数直接对应READ RECORD的P2和P1
	 * @return 0 成功
	 */
	public int ftHandlGPOCMDReciveData(String ReciveData, StringBuffer[] dest){
		String szData = null;
		String strBuf = null;
		String ReciveDatalen = null;
		String p1 = null;
		String p2 = null;
		String p1prefix = null;
		String p1suffix = null;
		int valuelen = 0;
		int recordeNum = 0;
		
		//计算数据长度
		ReciveDatalen = ReciveData.substring(2, 4);
		valuelen = (int) (ftPublic.ftHexToLong(ReciveDatalen)) - 2;
		//截取数据部分，去掉Tag80（响应报文模板）和AIP部分
		strBuf = ReciveData.substring(8);
		//数据区分组，每组8个字节
		int index = valuelen/4;
		for (int i = 0; i < index; i++) {
			szData = strBuf.substring(i*8, (i+1)*8);
			//截取p2
			p2 = szData.substring(0,2);
			StringBuffer binP2 = new StringBuffer(9);
			//10->0001 0000
			ftPublic.ftDataToBitStringNoASCII(p2, binP2);
			//binP2转换，低三位0->1,例子：0001 0000->0001 0100
			binP2.setCharAt(5, '1');
			//0001 0100->14
			StringBuffer outBuf = new StringBuffer(3);
			ftPublic.ftBinToHex(binP2.toString(),outBuf);
			p2 = outBuf.toString();
			//计算p1prefix
			p1prefix = szData.substring(2, 4);
			int p1prefixLen = (int) ftPublic.ftHexToLong(p1prefix);
			//计算p1suffix
			p1suffix = szData.substring(4, 6);
			int p1suffixLen = (int) ftPublic.ftHexToLong(p1suffix);
			if(p1suffixLen == p1prefixLen){
				p1 = p1prefix;
				//保存p1
				dest[recordeNum] = new StringBuffer(p1);
				//保存p2
				dest[recordeNum].append(p2);
				//下标加1
				recordeNum++;
			}
			else{
				int klen = p1suffixLen - p1prefixLen;
				for (int j = 0; j <= klen; j++) {
					//计算p1
					outBuf = new StringBuffer(3);
					ftPublic.ftLongToHex(p1prefixLen+j, outBuf, 0);
					p1 = outBuf.toString();
					//保存p1
					dest[recordeNum] = new StringBuffer(p1);
					//保存p2
					dest[recordeNum].append(p2);
					//下标加1
					recordeNum++;
				}
			}
		}
		return recordeNum;
	}
	/**
	 * @函数名称 ftAnalyzeGPOResponse
	 * @说明 分析GPO数据 查看AFL & API
	 * @param ReciveData (输入)
	 * @param dest (输出) SFI 和 RecordNo
	 * @return 返回AFL的条数，不包括在最后的用于静态数据验证的应用数据的AFL
	 */
	public int ftAnalyzeGPOResponse(String ReciveData, StringBuffer[] dest){
		String szData = null;
		String strBuf = null;
		String ReciveDatalen = null;
		String p1 = null;
		String p2 = null;
		String p1prefix = null;
		String p1suffix = null;
		String sdaDataFlag = null;
		String SDAAFL = null;
		int valuelen = 0;
		int recordeNum = 0;
		
		//计算数据长度
		ReciveDatalen = ReciveData.substring(2, 4);
		valuelen = (int) (ftPublic.ftHexToLong(ReciveDatalen)) - 2;
		//截取数据部分，去掉Tag80（响应报文模板）和AIP部分
		strBuf = ReciveData.substring(8);
		//数据区分组，每组8个字节
		int index = valuelen/4;
		for (int i = 0; i < index; i++) {
			szData = strBuf.substring(i*8, (i+1)*8);
			//截取p2
			p2 = szData.substring(0,2);
			StringBuffer binP2 = new StringBuffer(9);
			//10->0001 0000
			ftPublic.ftDataToBitStringNoASCII(p2, binP2);
			//binP2转换，低三位移到高三位
			String temp = "000"+binP2.substring(0, 5);
//			binP2.setCharAt(5, '1');binP2转换，低三位0->1,例子：0001 0000->0001 0100
			//0001 0100->14
			StringBuffer outBuf = new StringBuffer(3);
			ftPublic.ftBinToHex(temp,outBuf);
			p2 = outBuf.toString();
			//计算p1prefix
			p1prefix = szData.substring(2, 4);
			int p1prefixLen = (int) ftPublic.ftHexToLong(p1prefix);
			//计算p1suffix
			p1suffix = szData.substring(4, 6);
			int p1suffixLen = (int) ftPublic.ftHexToLong(p1suffix);
			//获取静态数据认证标志
			sdaDataFlag = szData.substring(6, 8);
			if(!sdaDataFlag.equals("00"))
				SDAAFL = p2 + sdaDataFlag;
			if(p1suffixLen == p1prefixLen){
				p1 = p1prefix;
				//保存SFI
				dest[recordeNum] = new StringBuffer(p2);
				//保存RecordNO
				dest[recordeNum].append(p1);
				//下标加1
				recordeNum++;
			}
			else{
				int klen = p1suffixLen - p1prefixLen;
				for (int j = 0; j <= klen; j++) {
					//计算p1
					outBuf = new StringBuffer(3);
					ftPublic.ftLongToHex(p1prefixLen+j, outBuf, 0);
					p1 = outBuf.toString();
					//保存SFI
					dest[recordeNum] = new StringBuffer(p2);
					//保存RecordNO
					dest[recordeNum].append(p1);
					//下标加1
					recordeNum++;
				}
			}
		}
		//添加静态数据认证AFL
		dest[recordeNum] = new StringBuffer(SDAAFL);
		return recordeNum;
	}
	/**
	 * @函数名称 ftGenAppCryptogram
	 * @说明 产生应用密文
	 * @param recvTag (输入) tag 8C或8D的值 
	 * @param Money (输入) 金额 
	 * @param random (输入)4字节随机数
	 * @param pFlag (输入)应用密文类型
	 *              0:	应用认证密文（AAC）
	 *              1:  授权请求密文（ARQC）
	 *              2:  交易证书（TC）
	 * @param SysDate (输入)系统日期
	 * @param SysTime (输入)系统时间
	 * @param AppCryptogram (输出)完整的应用密文
	 * @return 0 成功
	 */
	public int ftGenAppCryptogram(String recvTag, String Money, String random, int pFlag, String SysDate, String SysTime,
			StringBuffer AppCryptogram){

		AppCryptogram.setLength(0);
		StringBuffer szTagValue = new StringBuffer();
			
		switch (pFlag) {
		case 0://AAC(认证应用密文)
			AppCryptogram.append("80AE0000");
			break;
		case 1://APQC(联机交易)
			AppCryptogram.append("80AE4000");
			break;
		case 2://TC(交易证书)
			AppCryptogram.append("80AE8000");
			break;
		default:
			AppCryptogram.append("80AE8000");
			break;
		}
			
		//根据tag 8C/8D的值获取GAC的Data数据部分
		int nRet = ftGetValueByTag(pFlag, Money, random, SysDate, SysTime, recvTag, szTagValue);
		if (nRet != 0){
			return -1;
		}
		//计算CDOL1的长度
		long cdolLen = szTagValue.length()/2;
		StringBuffer hexStr = new StringBuffer(2);
		//10进制转成16进制字符串
		ftPublic.ftLongToHex(cdolLen, hexStr, 0);
		//设置GAC命令的CDOL1长度
		AppCryptogram.append(hexStr);
		//设置GAC命令的CDOL1
		AppCryptogram.append(szTagValue);
		return 0;
	}
	public int ftExtAuth(String pRecv, String pACKey, StringBuffer extAuth){
		//1.获取交易计数器和应用密文
		//取交易计数器
		String szTranCounter = pRecv.substring(6, 10);
		//应用密文(AC) 42FE359EF7428D51
		String szAC = pRecv.substring(10, 26);
		//卡片验证结果(CVR)03A42010
//		String szAppData = pRecv.substring(32, 40);
		//2.将ATC结果与“FFFF”做异或运算，得结果NATC
		try {
			String szHexval = ftPublic.ftCalXOR(szTranCounter, "FFFF", 2);
			//3.做3des_encode_ecb计算获得AC_KEY_SESSION临时密钥
			String szACVal = "000000000000"+szTranCounter+"000000000000";
			szACVal = szACVal+szHexval;
			//对卡片AC-KEY加密生成交易AC-KEY密钥
			DES Des = new DES();
			String szACSubKey = Des.enc3DES(pACKey, szACVal);
			//4.使用应用密文（AC）和授权响应码+“000000000000”做异或运算
			szACVal = ftPublic.ftCalXOR(szAC, "3030000000000000", 8);
			//5.计算APRC
			String szAPRC = Des.enc3DES(szACSubKey, szACVal);
			//6.外部认证APDU指令00 82 00 00 +APRC+ 授权响应码
			String szData = "008200000A"+szAPRC+"3030";
			//赋值
			extAuth.setLength(0);
			extAuth.append(szData);
		} catch (ErrMessage e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
		return 0;
	}
	public int ftUpdateRecord(String money, String pRecv, String MACKey, int flag, StringBuffer updateRecord){
		//1.获取交易计数器和应用密文
		//取交易计数器
		String szTranCounter = pRecv.substring(6, 10);
		//应用密文(AC) 42FE359EF7428D51
		String szAC = pRecv.substring(10, 26);
		//卡片验证结果(CVR)03A42010
//		String szAppData = pRecv.substring(32, 40);
		
		try {
			//2.将ATC结果与“FFFF”做异或运算，得结果NATC
			String szHexval = ftPublic.ftCalXOR(szTranCounter, "FFFF", 2);
			//3.做3des_encode_ecb计算获得AC_KEY_SESSION临时密钥
			String szACVal = "000000000000"+szTranCounter+"000000000000";
			szACVal = szACVal+szHexval;
			//对卡片MAC-KEY加密生成交易MAC-KEY密钥
			DES Des = new DES();
			String szMACSubKey = Des.enc3DES(MACKey, szACVal);
			//4.组合数据块 D= CLA、INS、P1、P2 和 Lc（Lc 的长度包括 MAC 的长度）、电子金额
			String szVal = "04DA";
			//p1,p2
			switch (flag) {
			case 0://0:电子现金余额(9F79) 
				szVal.concat("9F79");
				break;
			case 1://1:电子现金余额上限(9F77)	
				szVal.concat("9F77");
				break;
			case 2://2:电子现金单笔交易限额(9F78)
				szVal.concat("9F78");
				break;
			case 3://3:电子现金重置阈值(9F6D)
				szVal.concat("9F6D");
				break;
			default://电子现金余额(9F79)
				szVal.concat("9F79");
				break;
			}
			//LC
			szVal.concat("0A");
			String szData = szVal;
			//联机交易计算器,2字节
			szVal.concat(szTranCounter);
			//GAC-ARQC命令应答应用密文,8字节
			szVal.concat(szAC);
			//金额
			String zero = "000000000000";
			String moneyTmp = zero.substring(money.length())+money;
			szVal.concat(moneyTmp);
			
			szData.concat(moneyTmp);
			//填充值,3字节
			szVal.concat("800000");
			//5.使用MAC过程密钥和数据块D做MAC计算
			String szInitData = "0000000000000000";
			String szMAC = MAC.macForDes_3Des(szMACSubKey, szInitData, szVal);
			//MAC计算结束	
			szData.concat(szMAC);
			//updateRecord赋值
			updateRecord.setLength(0);
			updateRecord.append(szData);
			
		} catch (ErrMessage e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}
	/**
	 * 
	 * @param certification
	 * @return
	 */
	public Map<String, String> ftAnalazyIssuerCert(String certification){
		Map<String, String> map = new HashMap<String, String>();
		map.put("certFormat", certification).substring(2, 4);
		map.put("issuer", certification.substring(4, 12));
		map.put("expire", certification.substring(12, 16));
		map.put("certificationNo", certification.substring(16, 22));
		map.put("HashAlgorithm", certification.substring(22, 24));
		map.put("PKAlgorithm", certification.substring(24, 26));
		map.put("Ni", certification.substring(26, 28));
		return null;
	}
	public static void main(String args[])
	{
		BasePboc pboc = new BasePboc();
		BaseFunction bf = new BaseFunction();
		StringBuffer gpo = new StringBuffer(256);
		pboc.ftSplitTemplateData("5A0888880003000000995F3401018E0C00000000000000005E031F009F0D05D86004A8009F0E0500109800009F0F05D86804F8005F24033504195F25031504195F280201569F0702FF00");
		StringBuffer[] dest = new StringBuffer[20];
		int i = pboc.ftAnalyzeGPOResponse("80127C0008010200100104011801030020010100", dest);
		System.out.println(i);
		String aid_9f38 = "6F588408A000000333010101A54C50084A542050424F43308701019F381B9F6604DF60019F02069F03069F1A0295055F2A029A039C019F37045F2D027A689F1101019F12084A542050424F4330BF0C0A9F4D020B0ADF4D020C0A";
		pboc.ftTLVGetStrVal(aid_9f38, "84,9F38,9F4D", gpo);
		String temp = gpo.toString();
		System.out.println(temp);
		pboc.ftGetGPO(aid_9f38, "100", 3, bf.RandomString(8), bf.ftGetSysDate(0), bf.ftGetSysTime(0), gpo);
		System.out.println(gpo);
//		StringBuffer[] tagList = new StringBuffer[20];
//		int num = pboc.ftHandlGPOCMDReciveData("9F39840E325041592E5359532E4444463031A527BF0C2461224F10A0000006324D4F542E43505449433031500B50424F4320437265646974870101", tagList);
//		System.out.println("tag num: "+num);
//		System.out.println("tag list");
//		for (int i=0;i<tagList.length;i++) {
//			System.out.println(tagList[i]);
//		}
	}
}
