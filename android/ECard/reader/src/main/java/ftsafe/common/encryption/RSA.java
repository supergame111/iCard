package ftsafe.common.encryption;
/**
 * RSA加密计算
 * @author gwg
 *
 */

import java.math.BigInteger; 
import java.security.KeyFactory;  
import java.security.KeyPair;  
import java.security.KeyPairGenerator;  
import java.security.PrivateKey;  
import java.security.PublicKey;  
import java.security.Signature;
  
import java.security.interfaces.RSAPrivateKey;  
import java.security.interfaces.RSAPublicKey;  
 
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;

  
import java.util.HashMap;  
import java.util.Map;  
  

import javax.crypto.Cipher;  

import ftsafe.common.BaseFunction;

//import com.ft.ic.keyManage.javaCard.ISD;



public class RSA {
	  public static final String KEY_ALGORITHM = "RSA";  
	  public static final String SIGNATURE_ALGORITHM = "SHA1withRSA";  
	  
	  private static final String PUBLIC_KEY = "RSAPublicKey";  
	  private static final String PRIVATE_KEY = "RSAPrivateKey"; 

	  
	  
	  /** ****************************************************************************
	     * 用私钥对信息生成数字签名 
	     *  
	     * @param data 
	     *            加密数据 
	     * @param modulus 
	     * 			     私钥的N
	     * @param privateExponet 
	     * 			      私钥的D 
	     *  
	     * @return 
	     * @throws Exception 
	     ******************************************************************************/  
	    public static byte[] sign(byte[] data, String modulus,String privateExponet) throws Exception {  	       	      

            //获得私钥
	        PrivateKey privateKey = RSA.getPrivateKey(modulus, privateExponet); 
	  
	        // 用私钥对信息生成数字签名  
	        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);  
	        signature.initSign(privateKey);  
	        signature.update(data); 
	        byte[] sign =signature.sign();
	        return sign;  
	    }  
	    
	 
	    /***************************************************************************** 
	     * 解密<br> 
	     * 用私钥解密 
	     *  
	     * @param data 
	     *      				加密数据
	     * @param modulus 
	     * 						私钥的N
	     * @param privateExponet 
	     * 						私钥的D 
	     * @return 
	     * @throws Exception 
	     ********************************************************************************/  
	    public static byte[] decryptByPrivateKey(byte[] data, String modulus,String privateExponet)  
	            throws Exception {  
	
		        PrivateKey privateKey = RSA.getPrivateKey(modulus, privateExponet);  
		        
		          //加解密类  	         
		        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding"); 
		         
		         //加密  
		         
		        cipher.init(Cipher.DECRYPT_MODE, privateKey);  
		    
		        byte[] deBytes = cipher.doFinal(data); 
		  
		        return deBytes;  
	    }  
	  
	    /************************************************************************************ 
	     * 解密<br> 
	     * 用公钥解密 
	     * @param data
	     * 						加密数据
	     * @param modulus 
	     * 						公钥的N
	     * @param publicExponet 
	     * 						公钥的E 
	     * @return 
	     * @throws Exception 
	     ***********************************************************************************/  
	    public static byte[] decryptByPublicKey(byte[] data, String modulus,String publicExponet)  
	            throws Exception {  

	            PublicKey publicKey = RSA.getPublicKey(modulus, publicExponet);  
		        
		        //加解密类  	         
		        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding"); 
		         
		        //加密  		         
		        cipher.init(Cipher.DECRYPT_MODE, publicKey);  
		    
		        byte[] deBytes = cipher.doFinal(data); 
		  
		        return deBytes;  
	    }  
	  
	    /** *******************************************************************************
	     * 加密<br> 
	     * 用公钥加密 
	     *  
	     * @param data				
	     * 						明文加密数据
	     * @param modulus 
	     * 						公钥的N
	     * @param publicExponet 
	     * 						公钥的E 
	     * @return 
	     * @throws Exception 
	     *************************************************************************************/  
	    public static byte[] encryptByPublicKey(byte[] data, String modulus,String publicExponet)  
	            throws Exception {  

	            PublicKey publicKey = RSA.getPublicKey(modulus, publicExponet);  
		        
		        //加解密类  	         
		        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding"); 
		         
		        //加密  		         
		        cipher.init(Cipher.ENCRYPT_MODE, publicKey);  
		    
		        byte[] enBytes = cipher.doFinal(data); 
		  
		        return enBytes;    
	    }  
	  
	    /** ***************************************************************************************
	     * 加密<br> 
	     * 用私钥加密 
	     *  
	     * @param data 
	     * 						明文加密数据
	     * @param modulus 
	     * 						私钥的N
	     * @param privateExponet 
	     * 						私钥的D
	     * @return 
	     * @throws Exception 
	     *****************************************************************************************/  
	    public static byte[] encryptByPrivateKey(byte[] data, String modulus,String privateExponet)  
	            throws Exception {  
	    
	        PrivateKey privateKey = RSA.getPrivateKey(modulus, privateExponet);  
	        
	          //加解密类  	         
	         Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding"); 
	         
	         //加密  
	         
	        cipher.init(Cipher.ENCRYPT_MODE, privateKey);  
	    
	        byte[] enBytes = cipher.doFinal(data); 
	  
	        return enBytes;  
	    }  
  
	    /** ***********************************************************************************
	     * 初始化密钥 
	     *  
	     * @return 
	     * @throws Exception 
	     *****************************************************************************************/  
	    public static Map<String, Object> initKey() throws Exception {  
	        KeyPairGenerator keyPairGen = KeyPairGenerator  
	                .getInstance(KEY_ALGORITHM);  
	        keyPairGen.initialize(1024);  
	  
	        KeyPair keyPair = keyPairGen.generateKeyPair();  
	  
	        // 公钥  
	        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();  
	  
	        // 私钥  
	        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();  
	  
	        Map<String, Object> keyMap = new HashMap<String, Object>(2);  
	  
	        keyMap.put(PUBLIC_KEY, publicKey);  
	        keyMap.put(PRIVATE_KEY, privateKey);  
	        
	        return keyMap;  
	    }  
	    
	    
	    
	    /************************************************************************************************ 
	     * 取得公钥 
	     *  
	     * @param modulus 
	     * 						公钥N(16进制ASCII型数据串)
	     * @param publicExponent
	     * 						公钥E(16进制ASCII型数据串)
	     * @return 
	     * @throws Exception 
	     ****************************************************************************************************/ 
	    public static PublicKey getPublicKey(String modulus,String publicExponent) throws Exception {  
	    	  
            BigInteger n = new BigInteger(modulus,16);  
           
            BigInteger e = new BigInteger(publicExponent,16);  
  
            RSAPublicKeySpec keySpec = new RSAPublicKeySpec(n,e);  
  
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);  
  
            PublicKey publicKey = keyFactory.generatePublic(keySpec);  
  
            return publicKey;  
  
      }  
   
	    /** ********************************************************************************
	     * 取得私钥 
	     *  
	     * @param modulus 
	     * 						私钥N(16进制ASCII型数据串)
	     * @param privateExponent
	     * 						私钥D(16进制ASCII型数据串) 
	     * @return 
	     * @throws Exception 
	     **************************************************************************************/  
      public static PrivateKey getPrivateKey(String modulus,String privateExponent) throws Exception {  
  
            BigInteger n = new BigInteger(modulus,16);  
  
            BigInteger d = new BigInteger(privateExponent,16);  
  
            RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(n,d);  
  
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");  
  
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);  
  
            return privateKey;  
  
      }
      /**
       * 用E，N做RSA计算
       * @param M 消息 十六进制
       * @param exponent 指数E 十六进制
       * @param module 模长N 十六进制
       * @return 结果 十六进制
       */
      public static String rsaCalculateByEN(String M, String exponent, String module){
    	  BigInteger m = new BigInteger(M,16);
    	  
    	  BigInteger n = new BigInteger(module,16);  
          
          BigInteger e = new BigInteger(exponent,16);
          
          return m.modPow(e, n).toString(16);
      }
  
	    
	    public static void main(String args[]) throws Exception     
	    {   
	    	RSA.initKey();
	    	BaseFunction bf = new BaseFunction();
	    	String issuerCert = "32EB7CD7FE1D9BED5E525A95F27821C50A13C7AA1B49102D72202CB2E92C767A4B964D33AA9680F008D7F94E9086170189BB82E8F0D07784B0F8F15BE8D36248A12232B8A46A69C05FDA85A28AB4CAFD269B7C61DB4B5A814E78D2EC0994F4E239889D8612FE87F2467A20C8874119414A30F8763C1B2523798AEDEBDFC02F843FED2A9AEE4E4D341E613583D44AC20C6010C42DEBB6B08F4D386860E2783A3462F311E1C060843970AA66A489D90DB1";
	    	String Nca = "A49097144F48886BB00843DC40725DB620D95B0485F46908946F0A2385B323D59355F133DAED8287A15EFF17F1A8EC6C9E804492F2E0E00E56B91A5484670DDA205583277A9B4AA449540050065AD5BA8E6DFDE56C762E0C24505664AA75433E66EB6E63141916FA068A8E9BA033CCDCF185994DF55D071C2C19462448134B3F65C04D3DDCC95BA3191E3EDA80FE338E4D469D91502B4D5C8C6D449B38E3D3870EB09FA2A0B77A1734D2A70C977295D3";
	    	String CAExponent = "010001";
	    	String issuer = "6A028888FFFF02300000010101B003A524F713EE8139D0C611D7F888AE01E68D88B2B90A5248A933FB72882BF110B2D32CF7A1A11D6DA5CEE52959D639EC12F6D421A8213A87A7E124F84963266980434C50B81C4D708973D2E70A019DD9C1D1386BD08F2B4F8CB27B719F65EFA8DD4B2CF042C4EEA24F238ACE88B7721562CF9BA014BC9AB94B3382DF1445C1BDDDA45C75E2BF33E4B6797614A8B9ED5B58642326C747B101CADF62849CA4B8AFAEBC";
//	    	byte[] b = RSA.Recover(bf.ftStringToByte(issuerCert), Nca, CAExponent);
	    	String temp = RSA.rsaCalculateByEN(issuerCert,CAExponent, Nca);
	    	System.out.println(temp);
//	    	System.out.println(bf.ftByteToString(b));
	    }   


}
