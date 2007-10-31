/*
 * Encryption.java
 *
 * Created on October 25, 2007, 5:01 PM
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003-2007
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

/**
 *
 * @author akumar03
 */

package edu.tufts.vue.util;

import javax.crypto.Cipher;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.InvalidKeyException;
import org.apache.commons.codec.binary.Hex;

import tufts.vue.*;

import java.io.*;


public class Encryption {
    
    private static String algorithm = "DESede";
    private static final String KEY_FILE = "vue.key";
    private static Key key = null;
    private static Cipher cipher = null;
    private static Hex hex = new Hex();
    
    public static String encrypt(String input)    {
        try {
            if(cipher == null) cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.ENCRYPT_MODE, getKey());
            byte[] inputBytes = input.getBytes();
            byte[] outputBytes = cipher.doFinal(inputBytes);
            String output  = new String(hex.encode(outputBytes));
            return output;
        }catch(Exception ex) {
            ex.printStackTrace();
            return input ;
        }
    }
    
    public static String decrypt(String encrypted)  {
        try {
            
            if(cipher == null) cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.DECRYPT_MODE, getKey());
            byte[] encryptedBytes = hex.decode(encrypted.getBytes());
            byte[] recoveredBytes =  cipher.doFinal(encryptedBytes);
            String recovered = new String(recoveredBytes);
            return recovered;
        } catch (Exception ex) {
            ex.printStackTrace();
            return encrypted;
        }
    }
    
    private static synchronized Key getKey() {
        try {
            if(key == null) {
                File keyFile = new File(VueUtil.getDefaultUserFolder().getAbsolutePath()+File.separator+KEY_FILE);
                if(keyFile.exists()){
                    ObjectInputStream is = new ObjectInputStream(new FileInputStream(keyFile));
                    key = (Key)is.readObject();
                    is.close();
                } else { 
                    key = KeyGenerator.getInstance(algorithm).generateKey();
                    ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(keyFile));
                    os.writeObject(key);
                    os.close();
                }
                return key;
            } else {
                return key;
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            return null;
        }
        
    }
}
