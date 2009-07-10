/*
 *  Copyright (c) 2002-2003, The Joust Project
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without 
 *  modification, are permitted provided that the following conditions 
 *  are met:
 *
 *  - Redistributions of source code must retain the above copyright 
 *    notice, this list of conditions and the following disclaimer. 
 *  - Redistributions in binary form must reproduce the above copyright 
 *    notice, this list of conditions and the following disclaimer in 
 *    the documentation and/or other materials provided with the 
 *    distribution. 
 *  - Neither the name of the Joust Project nor the names of its 
 *    contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission. 
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS 
 *  FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE 
 *  COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, 
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, 
 *  BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER 
 *  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT 
 *  LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN 
 *  ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 *  POSSIBILITY OF SUCH DAMAGE.
 *
 *  File created by keith @ Apr 27, 2003
 *
 */

package edu.tufts.vue.collab.im;

import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.rv.RecvRvEvent;
import net.kano.joscar.rv.RvSession;
import net.kano.joscar.rv.RvSessionListener;
import net.kano.joscar.rv.RvSnacResponseEvent;
import net.kano.joscar.rvcmd.trillcrypt.TrillianCryptAcceptRvCmd;
import net.kano.joscar.rvcmd.trillcrypt.TrillianCryptBeginRvCmd;
import net.kano.joscar.rvcmd.trillcrypt.TrillianCryptCloseRvCmd;
import net.kano.joscar.rvcmd.trillcrypt.TrillianCryptMsgRvCmd;
import net.kano.joscar.rvcmd.trillcrypt.TrillianCryptReqRvCmd;
import net.kano.joscar.snaccmd.icbm.RvCommand;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

public class TrillianEncSession implements RvSessionListener {
    private static final BigInteger FIVE = new BigInteger("5");

    private Cipher encoder;
    private Cipher decoder;

    private BigInteger modulus;
    private BigInteger myPrivate;
    private BigInteger myPublic;
    private BigInteger otherPublic;
    private BigInteger sessionKey;

    private final RvSession rvSession;
    public Random random = new SecureRandom();

    public TrillianEncSession(RvSession session) {
        this.rvSession = session;
    }

    public void init() {
        modulus = new BigInteger(128, random);
        myPrivate = new BigInteger(128, random).mod(modulus);
        myPublic = FIVE.modPow(myPrivate, modulus);
        rvSession.sendRv(new TrillianCryptReqRvCmd(modulus, myPublic));
    }

    public RvSession getRvSession() { return rvSession; }

    public BigInteger getModulus() { return modulus; }

    public BigInteger getMyPrivate() { return myPrivate; }

    public BigInteger getMyPublic() { return myPublic; }

    public BigInteger getOtherPublic() { return otherPublic; }

    public BigInteger getSessionKey() { return sessionKey; }

    public void handleRv(RecvRvEvent event) {
        RvCommand rvc = event.getRvCommand();

        System.out.println("encsession handling event!");

        if (rvc instanceof TrillianCryptReqRvCmd) {
            System.out.println("got request for secureim from "
                    + rvSession.getScreenname());

            rvSession.addListener(this);

            TrillianCryptReqRvCmd cmd = (TrillianCryptReqRvCmd) rvc;

            modulus = cmd.getModulus();
            otherPublic = cmd.getPublicValue();


            myPrivate = new BigInteger(128, random).mod(modulus);
            myPublic = FIVE.modPow(myPrivate, modulus);

            initCiphers();

            rvSession.sendRv(new TrillianCryptAcceptRvCmd(myPublic));

        } else if (rvc instanceof TrillianCryptAcceptRvCmd) {
            otherPublic = ((TrillianCryptAcceptRvCmd) rvc).getPublicValue();
            initCiphers();

            rvSession.sendRv(new TrillianCryptBeginRvCmd());

        } else if (rvc instanceof TrillianCryptBeginRvCmd) {
            System.out.println("encrypted session with "
                    + rvSession.getScreenname() + " begun!");

        } else if (rvc instanceof TrillianCryptMsgRvCmd) {
            TrillianCryptMsgRvCmd cmd = (TrillianCryptMsgRvCmd) rvc;

            byte[] encrypted = cmd.getEncryptedMsg().toByteArray();

            byte[] decoded;
            try {
                decoded = decoder.doFinal(encrypted);
            } catch (IllegalBlockSizeException e) {
                e.printStackTrace();
                return;
            } catch (BadPaddingException e) {
                e.printStackTrace();
                return;
            }
            ByteBlock fullDecoded = ByteBlock.wrap(decoded);
            // the first eight bytes are garbage and the last byte is null
            ByteBlock textBlock = fullDecoded.subBlock(8,
                    fullDecoded.getLength() - 8 - 1);
            String msg = BinaryTools.getAsciiString(textBlock);
            System.out.println("message: " + msg);

        } else if (rvc instanceof TrillianCryptCloseRvCmd) {
            System.out.println("encryption session with "
                    + rvSession.getScreenname() + " closed!");
        }
    }

    private void initCiphers() {
        sessionKey = otherPublic.modPow(myPrivate, modulus);

        byte[] fbytes = sessionKey.toByteArray();
        if (fbytes.length == 17) {
            byte[] old = fbytes;
            fbytes = new byte[fbytes.length - 1];
            System.arraycopy(old, 1, fbytes, 0, fbytes.length);
        }

        SecretKeySpec spec = new SecretKeySpec(fbytes, "Blowfish");

        byte[] ivb = new byte[8];
        random.nextBytes(ivb);

        IvParameterSpec ips = new IvParameterSpec(ivb);

        try {
            encoder = Cipher.getInstance("Blowfish/CFB64/NoPadding");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }
        try {
            encoder.init(Cipher.ENCRYPT_MODE, spec, ips);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }

        try {
            decoder = Cipher.getInstance("Blowfish/CFB64/NoPadding");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }
        try {
            decoder.init(Cipher.DECRYPT_MODE, spec, ips);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
    }

    public void handleSnacResponse(RvSnacResponseEvent event) {
        System.out.println("got response: " + event.getSnacCommand());
    }

    public void sendMsg(String msg) {
        byte[] data = BinaryTools.getAsciiBytes(msg);
        byte[] encoded = new byte[encoder.getOutputSize(8 + data.length)];
        int totalLen;
        try {
            int offset = encoder.update(new byte[8], 0, 8, encoded, 0);
            int len = encoder.doFinal(data, 0, data.length, encoded, offset);
            totalLen = offset + len;
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
            return;
        } catch (BadPaddingException e) {
            e.printStackTrace();
            return;
        } catch (ShortBufferException e) {
            e.printStackTrace();
            return;
        }

        ByteBlock encodedBlock = ByteBlock.wrap(encoded, 0, totalLen);

        getRvSession().sendRv(new TrillianCryptMsgRvCmd(encodedBlock));
    }
}
