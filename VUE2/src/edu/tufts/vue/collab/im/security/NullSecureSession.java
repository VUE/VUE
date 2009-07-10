/*
 *  Copyright (c) 2003, The Joust Project
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
 *  File created by keith @ Sep 29, 2003
 *
 */

package edu.tufts.vue.collab.im.security;

import net.kano.joscar.ByteBlock;
import net.kano.joscar.snaccmd.FullRoomInfo;

import javax.crypto.SecretKey;
import java.security.cert.X509Certificate;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;

public class NullSecureSession extends SecureSession {
    NullSecureSession() { }

    public X509Certificate getMyCertificate() {
        return null;
    }

    public void setCert(String sn, X509Certificate cert) {
    }

    public X509Certificate getCert(String sn) {
        return null;
    }

    public boolean hasCert(String sn) {
        return false;
    }

    public void setChatKey(String roomName, SecretKey chatKey) {
    }

    public SecretKey getChatKey(String chat) {
        return null;
    }

    public ByteBlock genChatSecurityInfo(FullRoomInfo chatInfo, String sn)
            throws SecureSessionException {
        return null;
    }

    public ByteBlock encryptIM(String sn, String msg)
            throws SecureSessionException {
        return null;
    }

    public String parseChatMessage(String chat, String sn, ByteBlock data)
            throws SecureSessionException {
        return null;
    }

    public SecretKey extractChatKey(String sn, ByteBlock data)
            throws SecureSessionException {
        return null;
    }

    public String decodeEncryptedIM(String sn, ByteBlock encData)
            throws SecureSessionException {
        return null;
    }

    public byte[] encryptChatMsg(String chat, String msg)
            throws SecureSessionException {
        return new byte[0];
    }

    public ServerSocket createSSLServerSocket(String sn)
            throws SecureSessionException {
        return null;
    }

    public Socket createSecureSocket(InetAddress address, int port)
            throws SecureSessionException {
        return null;
    }

    public void generateKey(String chat) throws SecureSessionException {
    }
}
