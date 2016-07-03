package ftsafe.common.encryption;
/**
 * SHA1安全哈希计算
 */
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import ftsafe.common.BaseFunction;




//import com.ft.ic.keyManage.javaCard.CapFile;



public class SHA1 {
	
	//static Logger logger = Logger.getLogger(SHA1.class);
	static BaseFunction ftPublic = new BaseFunction();
	
	  /**
	   * SHA1安全哈希计算
	   * @param inStr 所要加密的字符串数据(BCD数据)
	   * @return ACSII类型数据 长度20字节
	   */
	    public static String doSHA1(String inStr) {
	        MessageDigest md;
	        String outStr = null;
	        try {
	            md = MessageDigest.getInstance("SHA-1");           //选择SHA-1，也可以选择MD5
	            byte[] digest = md.digest(inStr.getBytes());       //返回的是byet[]，要转化为String存储比较方便
	            outStr = ftPublic.ftByteToString(digest);
	        }
	        catch (NoSuchAlgorithmException nsae) {
	            nsae.printStackTrace();
	        }
	        return outStr;
	    }
	    
	    /**
		   * SHA1安全哈希计算
		   * @param inByte 所要加密的字节数据(ACSII型数据)
		   * @return ACSII类型数据 长度20字节
		   */
		    public static String doSHA1(byte[] inByte) {
		        MessageDigest md;
		        String outStr = null;
		        try {
		            md = MessageDigest.getInstance("SHA-1");     //选择SHA-1，也可以选择MD5
		            byte[] digest = md.digest(inByte);           //返回的是byet[]，要转化为String存储比较方便
		            outStr = ftPublic.ftByteToString(digest);
		        }
		        catch (NoSuchAlgorithmException nsae) {
		            nsae.printStackTrace();
		        }
		        return outStr;
		    }
		    
		    /**
		     * 对cap文件做哈希计算
		     * @param path cap文件路径
		     * @return
		     */
		  public static String SHA1ForCap(String path)
		    {
		    	String result = new String();
//		    	CapFile cf = new CapFile();
		    	
//		    	String buf = cf.getLoadData(path);
//		    	buf = buf.substring(8);
//		    	byte[] capBuf = DoData.stringToByte(buf);
		    	//哈希计算  
//		   	     result = doSHA1(capBuf);			    	
		        
		        return result;
		    }
	   
	    public static void main(String args[])     
	    {   
	    	//logger.info( "nepalon:title =2 " ); 
	    	//String a= DoData.BCDToASCII("admin");
	    	System.out.println(doSHA1(ftPublic.ftStringToByte("02621779FF08180025100101B001B9510E00B3888EAA8373B10B4D7A4E2CCD6E8E8600D512DD877CE3F58772D547CE8F97A810B842E3EC7943BD65F64EEF1B65A790E47906F07A49C6F45E61E01DCD2B0FD73674E509AE926942FF3D6B93048036880B262FC086B2521943BB5EF7AEF53ABB7823707BDDEFC3ED80BB3DBC1FE6C00AD918280223E9E62D5E81B293EDA2191893219381B28B40660316536320A706BBC4CE88A059510B72FC27AE8A8ECB6755DD0BBAD1C27109FB24865CA303")));
	      //  System.out.println(SHA1ForCap("F:\\cap文件\\project.cap"));     
	        
	      
			
	    }  

}
