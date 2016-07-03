package ftsafe.common.encryption;
import java.security.MessageDigest;

import ftsafe.common.ErrMessage;

/**
 * MD5加密算法
 * @author Administrator
 *
 */
public class MD5 {
	public final static String MD5(String s) {
        char hexDigits[]={'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};       
        try {
            byte[] btInput = s.getBytes();
            // 获得MD5摘要算法的 MessageDigest 对象
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            // 使用指定的字节更新摘要
            mdInst.update(btInput);
            // 获得密文
            byte[] md = mdInst.digest();
            // 把密文转换成十六进制的字符串形式
            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (int i = 0; i < j; i++) {
                byte byte0 = md[i];
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
	//密码生成
    public static void main(String[] args) throws ErrMessage {
    	DES des = new DES();
    	String password = "admin";
    	String id = "admin";
    	password = MD5.MD5(password);
    	id = MD5.MD5(id);
    	 System.out.println(password);
		//2.3DES加密
		password = des.enc3DES(id, password);
        System.out.println("密码:"+password);
        System.out.println("MD5:"+des.dec3DES(id, password));
    }
    //1AA7F64B6B8E54A834122E533ACDA6FC
    //4ADC603A2F8DF2B9C24C75B7FB3239F9
    

}
