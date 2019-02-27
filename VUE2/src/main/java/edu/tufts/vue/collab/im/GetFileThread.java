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
 *  File created by Keith @ 5:04:14 AM
 *
 */

package edu.tufts.vue.collab.im;

import net.kano.joscar.ByteBlock;
import net.kano.joscar.rv.RvSession;
import net.kano.joscar.rvcmd.SegmentedFilename;
import net.kano.joscar.rvproto.ft.FileTransferHeader;
import net.kano.joscar.rvproto.getfile.GetFileEntry;
import net.kano.joscar.rvproto.getfile.GetFileList;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class GetFileThread extends Thread {
    private RvSession rvSession;
    private ServerSocket serverSocket;

    public GetFileThread(RvSession rvSession, ServerSocket ss) {
        this.rvSession = rvSession;
        this.serverSocket = ss;
    }

    public void run() {
        try {
            Socket socket = serverSocket.accept();

            InputStream in = socket.getInputStream();

            FileTransferHeader firstHdr =
                    FileTransferHeader.readHeader(in);

            System.out.println("got header: " + firstHdr);

            FileTransferHeader ack = new FileTransferHeader(firstHdr);

            ack.setHeaderType(FileTransferHeader.HEADERTYPE_FILELIST_ACK);
            ack.setFlags(FileTransferHeader.FLAG_DEFAULT);

            System.out.println("sending ack: " + ack);

            OutputStream out = socket.getOutputStream();
            ack.write(out);

            byte[] buffer = new byte[(int) firstHdr.getFileSize()];
            for (int i = 0; i < buffer.length;) {
                int count = in.read(buffer, i, buffer.length - i);

                if (count == -1) break;

                i += count;
            }

            ByteBlock listBlock = ByteBlock.wrap(buffer);
            GetFileList list = GetFileList.readGetFileList(listBlock);

            GetFileEntry[] entries = list.getFileEntries();
            for (int i = 0; i < entries.length; i++) {
                System.out.println("* " + entries[i]);
            }

            FileTransferHeader fin = new FileTransferHeader(ack);
            fin.setHeaderType(FileTransferHeader.HEADERTYPE_FILELIST_RECEIVED);
            fin.setFlags(FileTransferHeader.FLAG_DEFAULT
                    | FileTransferHeader.FLAG_DONE);

            fin.write(out);

            FileTransferHeader dirreq = new FileTransferHeader(fin);

            dirreq.setHeaderType(FileTransferHeader.HEADERTYPE_FILELIST_REQDIR);
            dirreq.setFilename(new SegmentedFilename(
                    new String[] { "in", "ebaything" }));
            dirreq.setTotalFileSize(0);
            dirreq.setFileSize(0);
            dirreq.setFileCount(0);
            dirreq.setFilesLeft(0);
            dirreq.write(out);

            FileTransferHeader resp = FileTransferHeader.readHeader(in);

            System.out.println("got response: " + resp);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}