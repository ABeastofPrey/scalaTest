/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.servotronix.mcwebserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author omri.soudry
 */
public class MCEncryptor {
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES";
    private static final String KEY = "Mary has one cat";
 
    public static String encrypt(String txt) throws Exception {
        File tmp = new File("tmp.txt");
        FileOutputStream outputStream = new FileOutputStream(tmp);
        outputStream.write(txt.getBytes());
        outputStream.close();
        return doCrypto(Cipher.ENCRYPT_MODE, tmp);
    }
 
    public static String decrypt(File inputFile)
            throws Exception {
        return doCrypto(Cipher.DECRYPT_MODE, inputFile);
    }
 
    private static String doCrypto(int cipherMode, File inputFile) throws Exception {
        try {
            Key secretKey = new SecretKeySpec(KEY.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(cipherMode, secretKey);
             
            FileInputStream inputStream = new FileInputStream(inputFile);
            byte[] inputBytes = new byte[(int) inputFile.length()];
            inputStream.read(inputBytes);
             
            byte[] outputBytes = cipher.doFinal(inputBytes);
            
            /* create license file
            if (cipherMode == Cipher.ENCRYPT_MODE) {
                FileOutputStream outputStream = new FileOutputStream(new File("MC_License.txt"));
                outputStream.write(outputBytes);
                outputStream.close();
            }*/
            
            inputStream.close();
            
            return new String(outputBytes);
             
        } catch (NoSuchPaddingException | NoSuchAlgorithmException
                | InvalidKeyException | BadPaddingException
                | IllegalBlockSizeException | IOException ex) {
            throw new Exception("Error encrypting/decrypting file", ex);
        }
    }
    
}
