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
 *  File created by keith @ Apr 25, 2003
 *
 */

package edu.tufts.vue.collab.im;

import net.kano.joscar.rv.RvSession;
import net.kano.joscar.rvcmd.sendfile.FileSendAcceptRvCmd;
import net.kano.joscar.rvproto.ft.FileTransferChecksum;
import net.kano.joscar.rvproto.ft.FileTransferHeader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;

import edu.tufts.vue.collab.im.security.SecureSession;
import edu.tufts.vue.collab.im.security.SecureSessionException;


public class RecvFileThread extends Thread {
    private VUEAim tester;
    private InetAddress address;
    private int port;
    private RvSession session;
    private long cookie;
    private boolean encrypted;
    private ServerSocket serverSocket;

    public RecvFileThread(RvSession session, ServerSocket socket) {
        this.session = session;
        this.serverSocket = socket;
    }

    public RecvFileThread(VUEAim tester, InetAddress address, int port,
            RvSession session, long cookie, boolean encrypted) {
        this.tester = tester;
        this.address = address;
        this.port = port;
        this.session = session;
        this.cookie = cookie;
        this.encrypted = encrypted;
    }

    private Socket getSocket() throws IOException {
        if (serverSocket != null) {
            return serverSocket.accept();
        } else {
            session.sendRv(new FileSendAcceptRvCmd(encrypted));
            Socket socket;
            if (encrypted) {
                try {
                    System.out.println("creating secure socket");
                    SecureSession ss = tester.getSecureSession();
                    socket = ss.createSecureSocket(address, port);
                } catch (SecureSessionException e) {
                    e.printStackTrace();
                    return null;
                }
            } else {
                socket = new Socket(address, port);
            }
            System.out.println("socket opened..");
            return socket;
        }
    }

    public void run() {
        try {
            Socket socket = getSocket();
            System.out.println("opening socket to " + address + " on " + port);

            InputStream in = socket.getInputStream();

            for (;;) {
                FileTransferHeader header = FileTransferHeader.readHeader(in);

                if (header == null) break;

                System.out.println("header: " + header);

                String[] parts = header.getFilename().getSegments();
                String filename;
                if (parts.length > 0) filename = "dl-" + parts[parts.length-1];
                else filename = "dl-" + session.getScreenname();
                System.out.println("writing to file " + filename);

                long sum = 0;
                if (new File(filename).exists()) {
                    FileInputStream fis = new FileInputStream(filename);
                    byte[] block = new byte[10];
                    for (int i = 0; i < block.length;) {
                        int count = fis.read(block);

                        if (count == -1) break;

                        i += count;
                    }

                    FileTransferChecksum summer = new FileTransferChecksum();
                    summer.update(block, 0, 10);
                    sum = summer.getValue();
                }

                FileChannel fileChannel
                        = new FileOutputStream(filename).getChannel();

                FileTransferHeader outHeader = new FileTransferHeader(header);
                outHeader.setHeaderType(FileTransferHeader.HEADERTYPE_ACK);
                outHeader.setIcbmMessageId(cookie);
                outHeader.setBytesReceived(0);
                outHeader.setReceivedChecksum(sum);

                OutputStream socketOut = socket.getOutputStream();
                System.out.println("sending header: " + outHeader);
                outHeader.write(socketOut);

//                FileTransferHeader resh = FileTransferHeader.readHeader(in);
//                System.out.println("resume header: " + resh);

                for (int i = 0; i < header.getFileSize();) {
                    long transferred = fileChannel.transferFrom(
                            Channels.newChannel(in),
                            0, header.getFileSize() - i);

                    System.out.println("transferred " + transferred);

                    if (transferred == -1) return;

                    i += transferred;
                }
                System.out.println("finished transfer!");
                fileChannel.close();

                FileTransferHeader doneHeader = new FileTransferHeader(header);
                doneHeader.setHeaderType(FileTransferHeader.HEADERTYPE_RECEIVED);
                doneHeader.setFlags(doneHeader.getFlags()
                        | FileTransferHeader.FLAG_DONE);
                doneHeader.setBytesReceived(doneHeader.getBytesReceived() + 1);
                doneHeader.setIcbmMessageId(cookie);
                doneHeader.setFilesLeft(doneHeader.getFilesLeft() - 1);
                doneHeader.write(socketOut);
                if (doneHeader.getFilesLeft() - 1 <= 0) {
                    socket.close();
                    break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }
}
