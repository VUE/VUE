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
import net.kano.joscar.rv.RvSession;
import net.kano.joscar.rvcmd.SegmentedFilename;
import net.kano.joscar.rvproto.ft.FileTransferHeader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

public class SendFileThread extends Thread {
    private RvSession rvSession;
    private ServerSocket serverSocket;

    public SendFileThread(RvSession rvSession, ServerSocket serverSocket) {
        this.rvSession = rvSession;
        this.serverSocket = serverSocket;
    }

    public void run() {
        Timer timer = null;
        try {
            System.out.println("waiting for connection..");
            Socket socket = serverSocket.accept();
            System.out.println("got connection from " + socket.getInetAddress());

            final OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

//            byte[] data = new byte[1024];
//            int ct = in.read(data);
//            System.out.println("ct=" + ct);
//            System.out.println(ByteBlock.wrap(data));

            FileTransferHeader fsh = new FileTransferHeader();
            fsh.setDefaults();
            fsh.setFileCount(1);
            fsh.setFilesLeft(1);
            fsh.setFilename(
                    SegmentedFilename.fromNativeFilename("wut up.gif"));
            fsh.setFileSize(2000000);
            fsh.setTotalFileSize(2000000);
            fsh.setHeaderType(FileTransferHeader.HEADERTYPE_SENDHEADER);
            fsh.setPartCount(1);
            fsh.setPartsLeft(1);
            fsh.setLastmod(System.currentTimeMillis() / 1000);
            fsh.setChecksum(100);

            System.out.println("writing: " + fsh);

            fsh.write(out);

            System.out.println("waiting for ack header..");

            FileTransferHeader inFsh = FileTransferHeader.readHeader(in);

            System.out.println("got ack:" + inFsh);

            if (inFsh.getHeaderType() == FileTransferHeader.HEADERTYPE_RESUME) {
                fsh.setHeaderType(FileTransferHeader.HEADERTYPE_RESUME_SENDHEADER);
                fsh.write(out);

                inFsh = FileTransferHeader.readHeader(in);

                System.out.println("resumesendresponse: " + inFsh);
            }

            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                public void run() {
                    try {
                        System.out.println("writing..");
                        out.write(new byte[100]);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, 10000, 2000);

            for (;;) {
                System.out.println("trying to read..");
                int b = in.read();

                if (b == -1) break;

                System.out.println("got stuff: "
                        + BinaryTools.describeData(ByteBlock.wrap(
                                new byte[] { (byte) b })));
            }

        } catch (IOException e) {
            e.printStackTrace();
            return;
        } finally {
            if (timer != null) timer.cancel();
        }
    }
}
