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
 *  File created by keith @ Apr 28, 2003
 *
 */

package edu.tufts.vue.collab.im;

import net.kano.joscar.ByteBlock;
import net.kano.joscar.ImEncodedString;
import net.kano.joscar.OscarTools;
import net.kano.joscar.net.ClientConn;
import net.kano.joscar.net.ClientConnEvent;
import net.kano.joscar.net.ClientConnListener;
import net.kano.joscar.net.ClientConnStreamHandler;
import net.kano.joscar.rv.RecvRvEvent;
import net.kano.joscar.rv.RvSession;
import net.kano.joscar.rvcmd.RvConnectionInfo;
import net.kano.joscar.rvcmd.directim.DirectIMReqRvCmd;
import net.kano.joscar.rvproto.directim.DirectImHeader;
import net.kano.joscar.rvproto.rvproxy.RvProxyInitRecvCmd;
import net.kano.joscar.snaccmd.icbm.RecvRvIcbm;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;

public class DirectIMSession {
    private final String mysn;
    private final RvSession rvSession;
    private final RecvRvEvent rvEvent;
    private final DirectIMReqRvCmd rvCommand;
    private final RvConnectionInfo connInfo;

    private ClientConn normalConn;

    private ClientConn conn = null;

    public DirectIMSession(String mysn, RvSession session,
            RecvRvEvent event) {
        this.mysn = mysn;
        this.rvSession = session;
        this.rvEvent = event;
        this.rvCommand = (DirectIMReqRvCmd) event.getRvCommand();
        this.connInfo = rvCommand.getConnInfo();

        open();
    }

    private void open() {
        if (connInfo.isProxied()) openProxy();
        else openNormally();
    }

    private void openProxy() {
        System.out.println("opening proxy...");

//        proxyConn = new ClientRvProxyConn(connInfo.getProxyIP(), 5190);
//        proxyProcessor = proxyConn.getRvProxyProcessor();
//
//        proxyConn.addConnListener(new ClientConnListener() {
//            public void stateChanged(ClientConnEvent e) {
//                handleProxyStateChange(e);
//            }
//        });
//        proxyConn.getRvProxyProcessor().addCommandListener(
//                new RvProxyCmdListener() {
//            public void handleRvProxyCmd(RvProxyCmdEvent e) {
//                handleProxyCommand(e);
//            }
//        });
//
//        proxyConn.connect();
    }

    private void handleProxyStateChange(ClientConnEvent e) {
        System.out.println("proxy connection state changed to "
                + e.getNewState());

        if (e.getNewState() == ClientConn.STATE_CONNECTED) {
            initProxy();
        }
    }

    private void initProxy() {
//        System.out.println("initing proxy...");
        long cookie = ((RecvRvIcbm) rvEvent.getSnacCommand()).getIcbmMessageId();
        int port = connInfo.getPort();
        RvProxyInitRecvCmd cmd = new RvProxyInitRecvCmd(mysn, cookie, port);

//        proxyProcessor.sendRvProxyCmd(cmd);
    }

//    private void handleProxyCommand(RvProxyCmdEvent e) {
//        System.out.println("got proxy command: " + e.getRvProxyCommand());
//        System.out.println("header: " + e.getRvProxyHeader());
//
//        if (e.getRvProxyCommand() instanceof RvProxyReadyCmd) {
//            proxyProcessor.detach();
//
//            startConn(proxyConn);
//        }
//    }

    private void openNormally() {
        InetAddress ip = connInfo.getInternalIP();
        int port = connInfo.getPort();

        System.out.println("connecting to " + ip + ":" + port);

        normalConn = new ClientConn(ip, port);

        normalConn.addConnListener(new ClientConnListener() {
            public void stateChanged(ClientConnEvent e) {
                Object state = e.getNewState();
                System.out.println("normal connection state changed to "
                        + state);
            }
        });

        normalConn.setStreamHandler(new ClientConnStreamHandler() {
            public void handleStream(ClientConn conn, Socket socket)
                    throws IOException {
                startConn(normalConn);
            }
        });

        normalConn.connect();
    }

    private void startConn(ClientConn socket) {
        System.out.println("starting dim connection..");
        this.conn = socket;


        System.out.println("direct IM to " + rvSession.getScreenname()
                + " opened");
    }

    private void close() {
        conn.disconnect();
    }

    private boolean handleDirectImData()
            throws IOException {
        String sn = rvSession.getScreenname();
        DirectImHeader header = null;
        InputStream in = null;

        long flags = header.getFlags();
        if ((flags & DirectImHeader.FLAG_TYPINGPACKET) != 0) {
            if ((flags & DirectImHeader.FLAG_TYPING) != 0) {
                System.out.println("=== " + sn + " is typing");
            } else if ((flags & DirectImHeader.FLAG_TYPED) != 0) {
                System.out.println("=== " + sn + " typed text");
            } else {
                System.out.println("=== " + sn + " erased typed text");
            }
        }

        // there're data to be read!
        final ByteBlock packetBlock;
        if (header.getDataLength() > 0) {
            byte[] packet = new byte[(int) header.getDataLength()];

            for (int i = 0; i < packet.length;) {
                int count = in.read(packet, i, packet.length - i);

                if (count != packet.length) {
                    System.out.println("read " + count + " bytes...");
                }

                if (count == -1) return false;

                i += count;
            }

            packetBlock = ByteBlock.wrap(packet);
            String str = ImEncodedString.readImEncodedString(
                    header.getEncoding(), packetBlock);

            System.out.println("=" + sn + "= " + OscarTools.stripHtml(str));

        } else {
            packetBlock = null;
        }
//        dimProcessor.sendPacket(header, new DirectImDataWriter() {
//            public void writeData(DirectImDataSendEvent e) throws IOException {
//                packetBlock.write(e.getOutputStream());
//            }
//        });

        return true;
    }
}
