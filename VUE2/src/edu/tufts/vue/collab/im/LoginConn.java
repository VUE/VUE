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
 *  File created by keith @ Mar 25, 2003
 *
 */

package edu.tufts.vue.collab.im;

import net.kano.joscar.ByteBlock;
import net.kano.joscar.tlv.Tlv;
import net.kano.joscar.tlv.TlvTools;
import net.kano.joscar.flap.ClientFlapConn;
import net.kano.joscar.flap.FlapPacketEvent;
import net.kano.joscar.flapcmd.LoginFlapCmd;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.flapcmd.SnacPacket;
import net.kano.joscar.net.ClientConnEvent;
import net.kano.joscar.snac.SnacPacketEvent;
import net.kano.joscar.snac.SnacResponseEvent;
import net.kano.joscar.snaccmd.auth.AuthRequest;
import net.kano.joscar.snaccmd.auth.AuthResponse;
import net.kano.joscar.snaccmd.auth.ClientVersionInfo;
import net.kano.joscar.snaccmd.auth.KeyRequest;
import net.kano.joscar.snaccmd.auth.KeyResponse;
import net.kano.joscar.snaccmd.chat.ChatMsg;
import net.kano.joscar.snaccmd.chat.SendChatMsgIcbm;

import java.net.InetAddress;


public class LoginConn extends AbstractFlapConn {
    protected boolean loggedin = false;

    public LoginConn(VUEAim tester) {
        super(tester);
    }

    public LoginConn(String host, int port, VUEAim tester) {
        super(host, port, tester);
    }

    public LoginConn(InetAddress ip, int port, VUEAim tester) {
        super(ip, port, tester);
    }

    protected void handleStateChange(ClientConnEvent e) {
        System.out.println("login connection state is now " + e.getNewState()
                + ": " + e.getReason());

        if (e.getNewState() == ClientFlapConn.STATE_CONNECTED) {
            System.out.println("sending flap version and key request");
            getFlapProcessor().sendFlap(new LoginFlapCmd());
            request(new KeyRequest(tester.getScreenname()));
        } else if (e.getNewState() == ClientFlapConn.STATE_FAILED) {
            tester.loginFailed("connection failed: " + e.getReason());
        } else if (e.getNewState() == ClientFlapConn.STATE_NOT_CONNECTED) {
            if (!loggedin) {
                tester.loginFailed("connection lost: " + e.getReason());
            }
        }
    }

    protected void handleFlapPacket(FlapPacketEvent e) { }

    protected void handleSnacPacket(SnacPacketEvent e) { }

    protected void handleSnacResponse(SnacResponseEvent e) {
        SnacCommand cmd = e.getSnacCommand();
        System.out.println("login conn got command "
                + Integer.toHexString(cmd.getFamily()) + "/"
                + Integer.toHexString(cmd.getCommand()) + ": " + cmd);

        if (cmd instanceof KeyResponse) {
            KeyResponse kr = (KeyResponse) cmd;

            ByteBlock authkey = kr.getKey();

//            ClientVersionInfo version = new ClientVersionInfo("Apple iChat",
//                    1, 0, 0, 60, 0xc6);
            ClientVersionInfo version = new ClientVersionInfo(
                    "AOL Instant Messenger, version 5.2.3292/WIN32",
                    5, 1, 0, 3292, 238);

            request(new AuthRequest(
                    tester.getScreenname(), tester.getPassword(),
                    version, authkey));

        } else if (cmd instanceof AuthResponse) {
            AuthResponse ar = (AuthResponse) cmd;

            int error = ar.getErrorCode();
            if (error != -1) {
                System.out.println("connection error! code: " + error);
                if (ar.getErrorUrl() != null) {
                    System.out.println("Error URL: " + ar.getErrorUrl());
                }
            } else {
                loggedin = true;
                tester.setScreennameFormat(ar.getScreenname());
                tester.startBosConn(ar.getServer(), ar.getPort(),
                        ar.getCookie());
                System.out.println("connecting to " + ar.getServer() + ":"
                        + ar.getPort());
            }

            disconnect();
        }
    }
}
