package ftsafe.utils.encryption;


import java.io.IOException;
import java.security.Security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import ftsafe.utils.BaseFunction;
import ftsafe.utils.ErrMessage;

//import com.ft.ic.workSystem.excption.ErrMessage;

public class DES {
	
	private static final String Algorithm = "DES"; //定义 加密算法,可用 DES,DESede,Blowfish 
	public static final String DES_KEY = "11223344556677889900AABBCCDDEEFF";
	
	BaseFunction ftPublic = new BaseFunction();
	//keybyte为加密密钥，长度为8字节 
	//src为被加密的数据缓冲区（源） 
	/**
	 * DES加密
	 */
	public static byte[] encryptMode(byte[] keybyte, byte[] src) 
	{ 
		try { 
				//生成密钥 
				SecretKey deskey = new SecretKeySpec(keybyte, Algorithm); 
				//加密 
				Cipher c1 = Cipher.getInstance(Algorithm); 
				c1.init(Cipher.ENCRYPT_MODE, deskey); 
				return c1.doFinal(src); 
		} 
		catch (java.security.NoSuchAlgorithmException e1)
		{ 
			e1.printStackTrace(); 
		} 
		catch (javax.crypto.NoSuchPaddingException e2) 
		{ 
			e2.printStackTrace(); 
		} 
		catch (java.lang.Exception e3)
		{ 
			e3.printStackTrace(); 
		} 
		return null; 
	} 

	//keybyte为加密密钥，长度为8字节 
	//src为加密后的缓冲区 
	/**
	 * DES解密
	 */
	public static byte[] decryptMode(byte[] keybyte, byte[] src) 
	{ 
		try { 
				//生成密钥 
				SecretKey deskey = new SecretKeySpec(keybyte, Algorithm); 
				//解密 
				Cipher c1 = Cipher.getInstance(Algorithm); 
				c1.init(Cipher.DECRYPT_MODE, deskey); 
				return c1.doFinal(src); 
			} 
		catch (java.security.NoSuchAlgorithmException e1)
		{ 
			e1.printStackTrace(); 
		} 
		catch (javax.crypto.NoSuchPaddingException e2)
		{ 
			e2.printStackTrace(); 
		} 
		catch (java.lang.Exception e3) { 
			e3.printStackTrace(); 
		} 
		return null; 
	} 
	
	/**
	 * DES加密
	 * @param key 密钥  8字节
	 * @param data 需要加密的数据  8字节
	 * @return 加密后的数据
	 */
	public byte[] encDES(String key,String data)
	{
		//添加新安全算法,如果用JCE就要把它添加进去 
		Security.addProvider(new com.sun.crypto.provider.SunJCE());
		byte[] szKey = ftPublic.ftStringToByte(key);
		byte[] szData = ftPublic.ftStringToByte(data);
		byte[] encoded = encryptMode(szKey, szData);
		int len = encoded.length-8;
		byte[] out = new byte[len];
		for(int i = 0;i<len;i++)
		{
			out[i]=encoded[i];
		}
		return out;		
	}

		
	/**
	 * DES解密
	 * @param key 密钥  8字节
	 * @param data 加密数据 8字节
	 * @return 解密后的数据
	 */
	public byte[] decDES(String key,String data)
	{
		//添加新安全算法,如果用JCE就要把它添加进去 
		Security.addProvider(new com.sun.crypto.provider.SunJCE());	
		String buffer = "0808080808080808";
		byte[] szBuffer = ftPublic.ftStringToByte(buffer);
		byte[] szKey = ftPublic.ftStringToByte(key);
		szBuffer = encryptMode(szKey, szBuffer);
		buffer = ftPublic.ftByteToString(szBuffer);
		data = data+buffer;
		byte[] szData = ftPublic.ftStringToByte(data);
		byte[] srcBytes = decryptMode(szKey, szData); 
		int len = srcBytes.length-8;
		byte[] out = new byte[len];
		for(int i = 0;i<len;i++)
		{
			out[i]=srcBytes[i];
		}
		return out;	
	}
	/**
	 * 3DES加密
	 * @param key密钥 16字节
	 * @param data加密数据 8字节的倍数
	 * @return 加密后的数据
	 * @throws ErrMessage 
	 */
	public String enc3DES(String key,String data) throws ErrMessage
	{
		int keyLen = key.length();
		int dataLen = data.length();
		String key1 = new String();
		String key2 = new String();
		String out = new String();
		
		if(keyLen!=32)
		{
			throw new ErrMessage("keyLen of class enc3DES error!");			
		}
		if(dataLen%8!=0)
		{
			throw new ErrMessage("dataLen of class enc3DES error!");
		}
		key1 = key.substring(0, 16);
		key2 = key.substring(16, 32);
		byte[]enc1 = encDES(key1,data);//key1对数据加密
		String buffer = ftPublic.ftByteToString(enc1);
		byte[]dec1 = decDES(key2,buffer);//用key2对上步中的输出数据进行解密
		String buffer2 = ftPublic.ftByteToString(dec1);
		byte[]enc2 = encDES(key1,buffer2);//key1对解密后的数据加密
		out = ftPublic.ftByteToString(enc2);
		out = out.toUpperCase();
		return out;
	}
	/**
	 * 3DES解密
	 * @param key 密钥 16字节
	 * @param data 加密数据 8字节的倍数
	 * @return 解密后的数据
	 */
	public String dec3DES(String key,String data)
	{
		int keyLen = key.length();
		int dataLen = data.length();
		
		String key1 = new String();
		String key2 = new String();
		String out = new String();
		
		if(keyLen!=32)
		{
			return null;
		}
		if(dataLen%8!=0)
		{
			return null;
		}
		
		key1 = key.substring(0, 16);
		key2 = key.substring(16, 32);
		
		byte[]dec1 = decDES(key1,data);//用key1对加密数据解密
		String buffer = ftPublic.ftByteToString(dec1);
		
		byte[]enc1 = encDES(key2,buffer);//用key2对上面输出的解密数据加密
		String buffer2 = ftPublic.ftByteToString(enc1);
		
		byte[]dec2 = decDES(key1,buffer2);//用key1对加密数据解密
		out = ftPublic.ftByteToString(dec2);
		
		out = out.toUpperCase();
		
		return out;
	}
	/**
	 * 分散算法
	 * @param key 16个字节
	 * @param data 8个字节
	 * @return
	 * @throws ErrMessage 
	 */
	public String diversify(String key,String data) throws ErrMessage
	{
		int keyLen = key.length();
		int dataLen = data.length();
		String buffer = new String();
		if(keyLen!=32)
		{
			return null;
		}
		if(dataLen!=16)
		{
			return null;
		}
		byte[] buf = ftPublic.ftStringToByte(data);
		for(int i=0;i<8;i++)
		{
			byte temp = buf[i];
			buf[i]=(byte) (~temp);
		}
		buffer = ftPublic.ftByteToString(buf);		
		buffer = data+buffer;
		String out = enc3DES(key,buffer);	
		return out;
	}
	/**
	 * 3des_encode_cbc加密
	 * @param iv 初始化向量
	 * @param key 密钥数据
	 * @param data 加密数据
	 * @return 加密获得数据
	 * @throws ErrMessage 
	 */
	public String Encode_3DES_CBC(String iv,String key,String data) throws Exception
	{
		String buffer1 = new String();
		String xor = new String();
		String buffer3DES = new String();
		String _3DES = new String();
		int len ;
		
		DES des = new DES();
		
		//判断iv,key,data数据长度的正确性
		len = data.length();
		if(iv.length() != 16||key.length() != 32||len%16 != 0||len == 0)
		{
			return null;
		}		
		
		//拆分成N组（N*16）
		//第一组
		buffer1 = data.substring(0, 16);
		//buffer1与初始化向量做异或运算
		xor = ftPublic.ftCalXOR(iv, buffer1, 8);
		//对异或结果做3DES加密
		buffer3DES = des.enc3DES(key, xor);
		_3DES = buffer3DES;
		//第二组开始循环
		for(int i =1; i<len/16;i++)
		{
			String buffer = new String();
			//第i组数据
			buffer = data.substring(16*i, 16+16*i);
			//做异或运算
			xor = ftPublic.ftCalXOR(buffer3DES, buffer, 8);
			//3DES加密
			buffer3DES = des.enc3DES(key, xor);
			_3DES = _3DES+buffer3DES;	
		}
					
		return _3DES;
		
	}
	
	public static void main(String[] args) throws IOException {  
		  
	       DES des = new DES();
	       
//	        String a = "000005803031373616C5906FF0FB1E28261F03EF69F99BD80B00683470850415089CE492E09E339BDFE1EADDADE2B8356CB7371F7E6D77858276E3E2B07BD2E421CE44F5E698D5F212FE9C4AFBBE76F650B325638A315D1477361634AD5374FA17DB9BD2B7BE6506182FE8900D5DA2B4AA9DC7C004C427B8A47C027DDFD05860C656BFF1B287BC05055BBAD323CF0A94890ABE7092255B70302F82CE37892C4E1C7C80A28B24F079D05C918C32ECCBF8AE07B90DB4A61910303030384C98659E30BEC25C30313736EE4BEB9C380FA199ACE43772491E6DDB73A1473F7D210F17F583CA836757BA8308B6CC94B0F2FE9BFA179031123F2F52199E17B530924136401894BF8413B9EE6D8A29D1A2DC567261AE86D41A881BB3EEFA031C30819A6626528E071C38206C9FDDFC57880D0C35E245AD73FF02AA92DFC07CD6F0339795F8A42DBDCEA57AB6730BF7374EFB67BAED8EB3D01750B24EE028DA4082E355119CA1D15E55A0082D9B8B9A0AC5628F261E8388D83AA605DE303038380EB3D62AA51EC1E7BC2239B1BE26ACBFF074EEC443F0A82030062025AA42AFA3E407F522B8DDD12C3720F22617C052A0ECF207D855A935CDA4F87654A91B0E4D9BC6F87CB75D4C52AA76AC5EF94942BB0FCDB25C82D04AE13030383822BC1127351448F5DF93B89299433F2E786E0DA99DEEFF826B6138DFBE156231C50F3D3B1E15B5C12493FC0CA5A0ABC732AE62360A93312F812B69A374C48EEE6F784F20C5681F8F9D4673062F5E054B57FA626DD452E6F1303038388D56411115BE2A5C7E8FBD0D95BA5AE95A14FD219D88F96852C184FAE988A8DE2AE0EC77A7AFE727FBA2390B4F808AC2BF071FC3626F922CC8CBF70643F10466A1D3EFF9098D3D86D82167B9763E3DA77B656194ECFBE0FB30303838D5B82E31E6443513839E033A4583A3774BA90B6BF11B9900ECBC27A559E55A81FF24D02C47B48CFCBAE72AAF2B245E2E80203425A4C76303214A1963A925A0983202644753DD0C50C196601C9A0D6C0318339D90031059C030303838919E9A561B18016960BFD16391534A72BE7B2E61ABA1CB033516A8325E73D61704C109944ACE6BC1666F70196B8BD3209F25D07D9A929407CF43AF8797D2ACBE67CE840E3D499AEBF0EE6E66324DB7BC1DA445C77F1C6C2C";
//	       
//	        String AC_04 = "9850C901B7402CE2058E0B38A068FA7F";
//	        String AC_06 = "C773445C76FAC21DE5A8F866A52B9DBB";
//	        String AC_08 = "BF58D006A8EB2CDF623B4649949F7828";
//	 
//	        String front = a.substring(0, 760);
//	        String P = a.substring(760, 936);
//	        String Q = a.substring(944, 1120);
//	        String dP = a.substring(1128, 1304);
//	        String dQ = a.substring(1312, 1488);
//	        String qinV = a.substring(1496, 1672);
//	        P=des.dec3DES("11111111111111112222222222222222", P);
//	        Q=des.dec3DES("11111111111111112222222222222222", Q);
//	        dP=des.dec3DES("11111111111111112222222222222222", dP);
//	        dQ=des.dec3DES("11111111111111112222222222222222", dQ);
//	        qinV=des.dec3DES("11111111111111112222222222222222", qinV);
//	        
//	        
//	        
//	        
//	        System.out.println("p    = "+P);
//	        System.out.println("q    = "+Q);
//	        System.out.println("dp   = "+dP);
//	        System.out.println("dq   = "+dQ);
//	        System.out.println("qinv = "+qinV);
//	        
//	        System.out.println("stand_ac_key              = "+des.dec3DES("11111111111111112222222222222222", AC_04));
//	        System.out.println("stand_dek_key             = "+des.dec3DES("11111111111111112222222222222222", AC_06));
//	        System.out.println("stand_mac_key             = "+des.dec3DES("11111111111111112222222222222222", AC_08));
//	       // String f = DoData.BCDToASCII(c);
	        
	        //System.out.println(des.enc3DES("57E3DA8C0DEAB3CB85D5B5DA04E092C8",c));
	       // System.out.println(f);
	        //System.out.println(DoData.byteToString(DoData.stringToByte(a)));
	        
	       String a = "BD55858E4B120131AC3B6D7A53095F2F615C2F12F200DAE8AE4E1A10E8CB48987CD74113EA06D693";
	       String P=des.dec3DES("0123456789ABCDEF0123456789ABCDEF", a);
//	       System.out.println(ftPublic.ftASCIIToBCDForGBK(P));
	        
	    } 
	
}
