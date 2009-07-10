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
 *  File created by Keith @ 4:28:30 AM
 *
 */

package edu.tufts.vue.collab.im;

import net.kano.joscar.rv.RecvRvEvent;
import net.kano.joscar.rv.RvSession;
import net.kano.joscar.rvcmd.RvConnectionInfo;
import net.kano.joscar.rvcmd.SegmentedFilename;
import net.kano.joscar.rvcmd.getfile.GetFileAcceptRvCmd;
import net.kano.joscar.rvcmd.getfile.GetFileReqRvCmd;
import net.kano.joscar.rvproto.ft.FileTransferChecksum;
import net.kano.joscar.rvproto.ft.FileTransferHeader;
import net.kano.joscar.rvproto.getfile.GetFileEntry;
import net.kano.joscar.rvproto.getfile.GetFileList;
import net.kano.joscar.snaccmd.icbm.RecvRvIcbm;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

public class HostGetFileThread extends Thread {
    private RvSession rvSession;
    private RecvRvEvent origIcbm;
    private Socket socket;
    private GetFileReqRvCmd req;

    public HostGetFileThread(RvSession session, RecvRvEvent req) {
        this.rvSession = session;
        this.origIcbm = req;
        this.req = (GetFileReqRvCmd) origIcbm.getRvCommand();
    }

    public void run() {
        try {
            rvSession.sendRv(new GetFileAcceptRvCmd());
            RvConnectionInfo connInfo = req.getConnInfo();
            socket = new Socket(connInfo.getInternalIP(),
                            connInfo.getPort());

            File base = new File(".");

            sendDirList(base, null);

            InputStream in = socket.getInputStream();

            for (int i = 0;; i++) {
                FileTransferHeader header = FileTransferHeader.readHeader(in);

                System.out.println("got header " + i + ": " + header);

                int type = header.getHeaderType();

                if (type == FileTransferHeader.HEADERTYPE_FILELIST_REQDIR) {
                    SegmentedFilename filename = header.getFilename();

                    File dir = new File(base, filename.toNativeFilename());
                    sendDirList(dir, filename);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendDirList(File dir, SegmentedFilename segDir)
            throws IOException {
        File[] files = dir.listFiles();

        List list = new LinkedList();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            SegmentedFilename segFN;
            segFN = new SegmentedFilename(null, file.getName());
            System.out.println("segFN: " + segFN);
            list.add(new GetFileEntry(segFN, file));
        }

        GetFileEntry[] entryList
                = (GetFileEntry[]) list.toArray(new GetFileEntry[0]);

        GetFileList gflist = new GetFileList(entryList);

        ByteArrayOutputStream listout = new ByteArrayOutputStream();

        gflist.write(listout);
        int listSize = listout.size();

        FileTransferChecksum cs = new FileTransferChecksum();
        cs.update(listout.toByteArray(), 0, listout.size());

        FileTransferHeader hdr = new FileTransferHeader();
        hdr.setDefaults();
        hdr.setHeaderType(FileTransferHeader.HEADERTYPE_FILELIST_SENDLIST);
        hdr.setFlags(FileTransferHeader.FLAG_DEFAULT
                | FileTransferHeader.FLAG_FILELIST);
        hdr.setFileCount(entryList.length);
        hdr.setFilesLeft(1);
        hdr.setPartCount(1);
        hdr.setPartsLeft(1);
        hdr.setFileSize(listSize);
        hdr.setTotalFileSize(listSize);
        hdr.setListNameOffset(28);
        hdr.setListSizeOffset(17);
        hdr.setChecksum(cs.getValue());
        long msgid = ((RecvRvIcbm) origIcbm.getSnacCommand()).getIcbmMessageId();
        hdr.setIcbmMessageId(msgid);
        hdr.setResForkChecksum(0xffff0000l);
        hdr.setResForkReceivedChecksum(0xffff0000l);
        hdr.setReceivedChecksum(0xffff0000l);
        hdr.setLastmod(dir.lastModified() / 1000);
        hdr.setFilename(new SegmentedFilename(segDir, ""));

        System.out.println("sending: " + hdr);

        OutputStream out = socket.getOutputStream();
        hdr.write(out);

        InputStream in = socket.getInputStream();

        FileTransferHeader ack = FileTransferHeader.readHeader(in);

        System.out.println("got ack: " + ack);

        listout.writeTo(out);

        FileTransferHeader fin = FileTransferHeader.readHeader(in);

        System.out.println("got fin: " + fin);
    }
}