package ftsafe.common;

/**
 * Created by axis on 2015/11/2.
 */
class Utils {
    /*
    public static String bytes2HexStr(byte[] b, int length) {
        String stmp = "";
        StringBuilder sb = new StringBuilder();
//        int length = b.length;
        for (int n = 0; n < length; n++) {
            stmp = Integer.toHexString(b[n] & 0xFF);
            sb.append((stmp.length() == 1) ? "0" + stmp : stmp);
        }
        return sb.toString().toUpperCase().trim();
    }
    public static String bytes2HexStrWithSpaces(byte[] b, int length) {
        String stmp = "";
        StringBuilder sb = new StringBuilder();
//        int length = b.length;
        for (int n = 0; n < length; n++) {
            stmp = Integer.toHexString(b[n] & 0xFF);
            sb.append((stmp.length() == 1) ? "0" + stmp : stmp);
            sb.append(" ");
        }
        return sb.toString().toUpperCase().trim();
    }

    /**
     * 十六进制字符串转换成字节数组，两个字符为一个字节
     * @param str
     * @return
     */
    /*
    public static byte[] hexStringToBytes(String str){
        int i = str.length();
        int k = 0;
        if(str == null||i==0||i%2 != 0)
        {
            return null;
        }
        byte[] result = new byte[i/2];
        for( k=0;k<i/2;k++)
        {
            String buffer = str.substring(2*k, 2*k+2);
            int temp = (int) Long.parseLong(buffer, 16);
            result[k] = (byte)temp;
        }
        return result;
    }

    public  static byte[] hexStringToBytes(String hexString){
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        if (hexString.length() % 2 != 0) {
            hexString = hexString.substring(0, hexString.length() - 1)
                    + "0"
                    + hexString.substring(hexString.length() - 1,
                    hexString.length());
        }

        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    /*
    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    public static boolean isLegal(String dataSendStr) {
        for (int i = 0; i < dataSendStr.length(); i++) {
            if (!(((dataSendStr.charAt(i) >= '0') && (dataSendStr.charAt(i) <= '9'))
                    || ((dataSendStr.charAt(i) >= 'a') && (dataSendStr
                    .charAt(i) <= 'f')) || ((dataSendStr.charAt(i) >= 'A') && (dataSendStr
                    .charAt(i) <= 'F')))) {
                return false;
            }
        }
        return true;
    }
    */
}
