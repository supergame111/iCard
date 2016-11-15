package ftsafe.common.encryption;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.SecretKeySpec;

import ftsafe.common.ErrMessage;
import ftsafe.common.Util;

/**
 * Created by qingyuan on 2016/7/25.
 */

public class DES2 {
    protected static final String DES = "DES";

    public static byte[] encrypt(byte[] key, byte[] msg) throws NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException {
        //generate tripleDes key
        SecretKey secretKey = new SecretKeySpec(key, DES);
        // Create and initialize the encryption engine
        Cipher cipher = Cipher.getInstance(DES);

        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        return cipher.doFinal(msg);
    }

    public static byte[] decrypt(byte[] key, byte[] data) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        //generate tripleDes key
        SecretKey secretKey = new SecretKeySpec(key, DES);
        // Create and initialize the decryption engine
        Cipher cipher = Cipher.getInstance(DES);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        return cipher.doFinal(data);
    }

    public final static class TripleDES {

        public static final String DESEDE = "DESede";
        public static final String DESEDE_ECB_NOPADDING = "DESede/ECB/NoPadding";
        private static final String DESEDE_ECB_PKCS5PADDING = "DESede/ECB/PKCS5Padding";
        private static final String DESEDE_CBC_PKCS5PADDING = "DESede/CBC/PKCS5Padding";
        private static final String DESEDE_CBC_NOPADDING = "DESede/CBC/NoPadding";


        /**
         * Generate a secret TripleDES encryption/decryption key
         */
        public static SecretKey generateKey() throws NoSuchAlgorithmException {
            // Get a key generator for Triple DES (a.k.a DESede)
            KeyGenerator keygen = KeyGenerator.getInstance(DESEDE);
            // Use it to generate a key
            return keygen.generateKey();
        }

        /**
         * Save the specified TripleDES SecretKey to the specified file
         */
        public static void writeKey(SecretKey key, File f) throws IOException,
                NoSuchAlgorithmException, InvalidKeySpecException {
            // Convert the secret key to an array of bytes like this
            SecretKeyFactory keyfactory = SecretKeyFactory.getInstance(DESEDE);
            DESedeKeySpec keyspec = (DESedeKeySpec) keyfactory.getKeySpec(key,
                    DESedeKeySpec.class);
            byte[] rawkey = keyspec.getKey();

            // Write the raw key to the file
            FileOutputStream out = new FileOutputStream(f);
            out.write(rawkey);
            out.close();
        }

        /**
         * Read a TripleDES secret key from the specified file
         */
        public static SecretKey readKey(File f) throws IOException,
                NoSuchAlgorithmException, InvalidKeyException,
                InvalidKeySpecException {
            // Read the raw bytes from the keyfile
            DataInputStream in = new DataInputStream(new FileInputStream(f));
            byte[] rawkey = new byte[(int) f.length()];
            in.readFully(rawkey);
            in.close();

            // Convert the raw bytes to a secret key like this
            DESedeKeySpec keyspec = new DESedeKeySpec(rawkey);
            SecretKeyFactory keyfactory = SecretKeyFactory.getInstance(DESEDE);
            SecretKey key = keyfactory.generateSecret(keyspec);
            return key;
        }

        /**
         * Use the specified TripleDES key to encrypt bytes from the input stream
         * and write them to the output stream. This method uses CipherOutputStream
         * to perform the encryption and write bytes at the same time.
         */
        public static void encrypt(SecretKey key, InputStream in, OutputStream out)
                throws NoSuchAlgorithmException, InvalidKeyException,
                NoSuchPaddingException, IOException {
            // Create and initialize the encryption engine
            Cipher cipher = Cipher.getInstance(DESEDE);
            cipher.init(Cipher.ENCRYPT_MODE, key);

            // Create a special output stream to do the work for us
            CipherOutputStream cos = new CipherOutputStream(out, cipher);

            // Read from the input and write to the encrypting output stream
            byte[] buffer = new byte[2048];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                cos.write(buffer, 0, bytesRead);
            }
            cos.close();

            // For extra security, don't leave any plaintext hanging around memory.
            java.util.Arrays.fill(buffer, (byte) 0);
        }

        public static byte[] encrypt(byte[] key, byte[] msg, byte[] iv, String cipherAlgorithm)
                throws NoSuchPaddingException,
                NoSuchAlgorithmException,
                BadPaddingException,
                IllegalBlockSizeException,
                InvalidKeyException {

            if (key.length == 16) {
                // E key
                byte[] eKey = Arrays.copyOf(key, 8);
                // D key
                byte[] dKey = Arrays.copyOfRange(key, 8, 16);

                byte[] keyTmp = new byte[24];
                System.arraycopy(eKey, 0, keyTmp, 0, eKey.length);
                System.arraycopy(dKey, 0, keyTmp, 8, dKey.length);
                System.arraycopy(eKey, 0, keyTmp, 16, eKey.length);

                //generate tripleDes key
                SecretKey secretKey = new SecretKeySpec(keyTmp, DESEDE);
                // Create and initialize the encryption engine
                Cipher cipher = Cipher.getInstance(DESEDE_ECB_NOPADDING);
                if (cipherAlgorithm != null)
                    cipher = Cipher.getInstance(cipherAlgorithm);

                cipher.init(Cipher.ENCRYPT_MODE, secretKey);

                return cipher.doFinal(msg);
            }
            return null;
        }

        /**
         * Use the specified TripleDES key to decrypt bytes ready from the input
         * stream and write them to the output stream. This method uses uses Cipher
         * directly to show how it can be done without CipherInputStream and
         * CipherOutputStream.
         */
        public static void decrypt(SecretKey key, InputStream in, OutputStream out)
                throws NoSuchAlgorithmException, InvalidKeyException, IOException,
                IllegalBlockSizeException, NoSuchPaddingException,
                BadPaddingException {
            // Create and initialize the decryption engine
            Cipher cipher = Cipher.getInstance(DESEDE);
            cipher.init(Cipher.DECRYPT_MODE, key);

            // Read bytes, decrypt, and write them out.
            byte[] buffer = new byte[2048];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(cipher.update(buffer, 0, bytesRead));
            }

            // Write out the final bunch of decrypted bytes
            out.write(cipher.doFinal());
            out.flush();
        }

        public static byte[] decrypt(byte[] key, byte[] data, String cipherAlgorithm) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
            if (key.length == 16) {
                // E key
                byte[] eKey = Arrays.copyOf(key, 8);
                // D key
                byte[] dKey = Arrays.copyOfRange(key, 8, 16);

                byte[] keyTmp = new byte[24];
                System.arraycopy(eKey, 0, keyTmp, 0, eKey.length);
                System.arraycopy(dKey, 0, keyTmp, 8, dKey.length);
                System.arraycopy(eKey, 0, keyTmp, 16, eKey.length);
                //generate tripleDes key
                SecretKey secretKey = new SecretKeySpec(keyTmp, DESEDE);
                // Create and initialize the decryption engine
                Cipher cipher = Cipher.getInstance(DESEDE_ECB_NOPADDING);
                if (cipherAlgorithm != null)
                    cipher = Cipher.getInstance(cipherAlgorithm);
                cipher.init(Cipher.DECRYPT_MODE, secretKey);

                return cipher.doFinal(data);
            }
            return null;
        }

        public final static int NOPADDING = 1;
        public final static int PADDING_80 = 2;

        public static byte[] mac(byte[] key, int keyLen, byte[] msg, int msgLen, byte[] iv, int outLen, int paddingMode) {
            try {
                if (keyLen != 16)
                    throw new InvalidKeyException("密钥长度不正确" + keyLen + "bytes");

                byte[] data = null;
                if (paddingMode == NOPADDING) {
                    //对数据进行补位，字节数不是8的倍数补“00 00 ・・・・”
                    data = Util.padding(msg, msgLen, Util.PADDING_00);
                } else if (paddingMode == PADDING_80) {
                    //对数据进行补位，字节数不是8的倍数补“80 00 ・・・・”
                    data = Util.padding(msg, msgLen, Util.PADDING_80);
                } else {
                    data = msg;
                }

                //80补齐后数据长度（字符）
                int newLen = data.length;
                //每8字节分割一组（字符串长度为16个字符）
                int n = newLen / 8;

                byte[] xor;
                byte[] tmp = iv;

                //取密钥前8个字节，用于DES加密
                byte[] newKey = Arrays.copyOf(key, 8);
                //对非最后一个模块做异或后的DES加密
                for (int i = 0; i < n - 1; i++) {

                    byte[] D = Arrays.copyOfRange(data, i * 8, (i + 1) * 8);
                    xor = Util.calXOR(tmp, D, 8);
                    tmp = DES2.encrypt(newKey, xor);
                }
                //最后一组数据
                byte[] lastD = Arrays.copyOfRange(data, newLen - 8, newLen);
                //对最后一组数据做异或
                xor = Util.calXOR(tmp, lastD, 8);
                //对最后一组数据做3DES加密
                tmp = TripleDES.encrypt(key, xor, null, null);
                //取选定的长度的字节数据
                if (outLen > tmp.length)
                    return tmp;

                return Arrays.copyOf(tmp, outLen);

            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (BadPaddingException e) {
                e.printStackTrace();
            } catch (IllegalBlockSizeException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
