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
import java.security.Key;
import java.security.InvalidKeyException;


public class Encryption {
    
    private static String algorithm = "DESede";
    private static Key key = null;
    private static Cipher cipher = null;
    public static byte[] encrypt(String input) throws Exception    {
        if(key == null) key = KeyGenerator.getInstance(algorithm).generateKey();
        if(cipher == null) cipher = Cipher.getInstance(algorithm);
        
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] inputBytes = input.getBytes();
        return cipher.doFinal(inputBytes);
    }
    
    public static String decrypt(byte[] encryptionBytes) throws  Exception {
        if(key == null) key = KeyGenerator.getInstance(algorithm).generateKey();
        if(cipher == null) cipher = Cipher.getInstance(algorithm);
        
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] recoveredBytes =
                cipher.doFinal(encryptionBytes);
        String recovered =
                new String(recoveredBytes);
        return recovered;
    }
    
    
}
