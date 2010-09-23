/*
* Copyright 2003-2010 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

/*
 * ByteStore.java
 *
 * Created on September 20, 2003, 7:39 PM
 *
 *  The software contained in this file is copyright 2003 by Mark J. Norton, all rights reserved.
 */

package tufts.oki.remoteFiling;
import tufts.oki.shared.*;
import org.apache.commons.net.ftp.*;

import java.io.*;
import java.lang.*;
import java.util.*;

/**
 *  Implements the ByteStore class on a remote filing system.  To read the contents of
 *  this byte store, use the getBytes() method.  To replace or initialize the contents,
 *  use write(byte[]).
 *
 *  @author  Mark Norton
 *  @author  Salem Berhanu - much of the FTP transactions.
 *
 */
public class RemoteByteStore extends RemoteCabinetEntry implements osid.filing.ByteStore {
    protected static final FTPFileListParser __fileListParser = new DefaultFTPFileListParser();
    private int used = 0;                   //  Bytes written to the buffer.
    private String mime_type = null;    //  The mime type of this byte store.
    private boolean writable = true;    //  Is it writable?
    private boolean readable = true;    //  Is it readable?
    private boolean appendable = true;  //  Is it appendable?
    
    /**
     *  Create a ByteStore object given a display name and parent.
     *
     *  @author Mark Norton
     *
     */
    public RemoteByteStore(String displayName, osid.filing.Cabinet parent, RemoteClient rc) throws osid.filing.FilingException {
        super(displayName, parent.getCabinetEntryAgent(), parent,rc);
      
    }
    
    /**
     *  Commit any pending I/O operations.
     *  In this implementation, commit() doesn't do anything.
     *
     *  @author Mark Norton
     */
    public void commit() {
    }
    
    /**
     *  Get the digest string.  Currently, this is unimplemented.
     *
     *  @author Mark Norton
     *
     *  @return A digest string.
     */
    public String getDigest(osid.shared.Type algorithmType) throws osid.filing.FilingException {
        throw new osid.filing.FilingException(osid.filing.FilingException.UNIMPLEMENTED);
    }
    
    /**
     *  Get an interator over all digest algorithm types supported.  Currently unimplemented.
     *
     *  @author Mark Norton
     *
     *  @return A TypeIterator which lists all Digest Algorithm Types.
     */
    public osid.shared.TypeIterator getDigestAlgorithmTypes() throws osid.filing.FilingException {
        throw new osid.filing.FilingException(osid.filing.FilingException.UNIMPLEMENTED);
    }
    
    /*
     *  The mime types suffix table.
     */
    private static java.util.Hashtable mimeTypesSuffixTable = new java.util.Hashtable();
    static {
        mimeTypesSuffixTable.put("doc", "application/msword");
        mimeTypesSuffixTable.put("pdf", "application/pdf");
        mimeTypesSuffixTable.put("ai", "application/postscript");
        mimeTypesSuffixTable.put("ps", "application/postscript");
        mimeTypesSuffixTable.put("eps", "application/postscript");
        mimeTypesSuffixTable.put("xls", "application/vnd.ms-excel");
        mimeTypesSuffixTable.put("ppt", "application/vnd.ms-powerpoint");
        mimeTypesSuffixTable.put("dcr", "application/x-director");
        mimeTypesSuffixTable.put("dir", "application/x-director");
        mimeTypesSuffixTable.put("dxr", "application/x-director");
        mimeTypesSuffixTable.put("swf", "application/x-shockwave-flash");
        mimeTypesSuffixTable.put("zip", "application/zip");
        //mimeTypesSuffixTable.put("application/x-compress");
        mimeTypesSuffixTable.put("tar", "application/x-tar");
        mimeTypesSuffixTable.put("mpga", "audio/mpeg");
        mimeTypesSuffixTable.put("mp2", "audio/mpeg");
        mimeTypesSuffixTable.put("mp3", "audio/mpeg");
        mimeTypesSuffixTable.put("ram", "audio/x-pn-realaudio");
        mimeTypesSuffixTable.put("rm", "audio/x-pn-realaudio");
        mimeTypesSuffixTable.put("rpm", "audio/x-pn-realaudio");
        mimeTypesSuffixTable.put("ra", "audio/x-pn-realaudio");
        mimeTypesSuffixTable.put("wav", "audio/x-wav");
        mimeTypesSuffixTable.put("gif", "image/gif");
        mimeTypesSuffixTable.put("jpg", "image/jpeg");
        mimeTypesSuffixTable.put("jpeg", "image/jpeg");
        mimeTypesSuffixTable.put("jpe", "image/jpeg");
        mimeTypesSuffixTable.put("tif", "image/tiff");
        mimeTypesSuffixTable.put("tiff", "image/tiff");
        mimeTypesSuffixTable.put("bmp", "image/bmp");
        mimeTypesSuffixTable.put("html", "text/html");
        mimeTypesSuffixTable.put("htm", "text/html");
        mimeTypesSuffixTable.put("rtx", "text/richtext");
        mimeTypesSuffixTable.put("rtf", "text/rtf");
        mimeTypesSuffixTable.put("txt", "text/plain");
        mimeTypesSuffixTable.put("mpeg", "video/mpeg");
        mimeTypesSuffixTable.put("mpg", "video/mpeg");
        mimeTypesSuffixTable.put("mpe", "video/mpeg");
        mimeTypesSuffixTable.put("mov", "video/quicktime");
        mimeTypesSuffixTable.put("qt", "video/quicktime");
        mimeTypesSuffixTable.put("avi", "video/x-msvideo");
    }
    
    /*
     *  The mime types table.
     */
    private static java.util.Hashtable mimeTypesTable = new java.util.Hashtable();
    static {
        mimeTypesTable.put("application/msword","doc");
        mimeTypesTable.put("application/pdf","pdf");
        mimeTypesTable.put("application/postscript","ai");
        mimeTypesTable.put("application/postscript","ps");
        mimeTypesTable.put("application/vnd.ms-excel","xls");
        mimeTypesTable.put("application/vnd.ms-powerpoint","ppt" );
        mimeTypesTable.put("application/zip","zip" );
        mimeTypesTable.put("application/x-tar","tar" );
        mimeTypesTable.put("audio/mpeg","mpga" );
        mimeTypesTable.put("audio/mpeg","mp2" );
        mimeTypesTable.put("audio/mpeg","mp3" );
        mimeTypesTable.put("audio/x-pn-realaudio","ram" );
        mimeTypesTable.put("audio/x-pn-realaudio","rm" );
        mimeTypesTable.put("audio/x-pn-realaudio","rpm" );
        mimeTypesTable.put("audio/x-pn-realaudio","ra" );
        mimeTypesTable.put("audio/x-wav","wav" );
        mimeTypesTable.put("image/gif","gif" );
        mimeTypesTable.put("image/jpeg","jpg" );
        mimeTypesTable.put("image/jpeg","jpeg" );
        mimeTypesTable.put("image/jpeg","jpe" );
        mimeTypesTable.put("image/tiff","tif" );
        mimeTypesTable.put("image/tiff","tiff" );
        mimeTypesTable.put("image/bmp","bmp" );
        mimeTypesTable.put("text/html","html" );
        mimeTypesTable.put("text/html","htm" );
        mimeTypesTable.put("text/richtext","rtx" );
        mimeTypesTable.put("text/rtf","rtf" );
        mimeTypesTable.put("text/plain","txt");
        mimeTypesTable.put("video/mpeg","mpeg" );
        mimeTypesTable.put("video/mpeg","mpg" );
        mimeTypesTable.put("video/mpeg","mpe" );
        mimeTypesTable.put("video/quicktime","mov" );
        mimeTypesTable.put("video/quicktime","qt" );
        mimeTypesTable.put("video/x-msvideo","avi" );
    }

    
    /**
     * Gets the mime-type of this ByteStore.
     *
     *  @author Salem Berhanu
     */
    public String getMimeType() throws osid.filing.FilingException 
    {
        String dispName = getDisplayName();
        String extension = dispName.substring(dispName.lastIndexOf(".") + 1);
        String mimeType  = (String) mimeTypesSuffixTable.get(extension);
        if (mimeType != null)
            return mimeType;
        else 
            return "text/plain";
    }

        
    /**
     * Set the mime-type of this ByteStore.
     */
    /**
     *  Check to see if the mime type passed is valid.  If so, set the
     *  byte store mime type to it.  Return the current mime type, which
     *  may be the old one if new one isn't valid.
     *
     *  @author Salem Berhanu
     *
     *  @return The current mime type of this byte store.
     */
    public String updateMimeType(String mimeType) throws osid.filing.FilingException {
        throw new osid.filing.FilingException (osid.filing.FilingException.UNIMPLEMENTED);
        
        /*  This needs work to make it compile.
        
        String extension = (String) mimeTypesTable.get(mimeType);

        System.out.println("extension :" +extension);
        if(extension != null) {
            FTPClient client = RemoteClient.getClient();
            try {
                String parentPath = ((RemoteCabinetEntry) getParent()).getIdString();
                String oldPath = parentPath + "/" + this.displayname;
                      
                String newPath = parentPath + "/";
                String newdisplayname = "";
                if(this.displayname.lastIndexOf(".") != -1)
                    newdisplayname += this.displayname.substring(0, this.displayname.lastIndexOf(".") + 1) + extension;
                else 
                   newdisplayname += this.displayname + "." + extension; 
                newPath += newdisplayname;
                System.out.println("new "+newPath+" old "+oldPath);
                if(client.rename(oldPath,newPath))
                { 
                    String oldname = this.displayname;
                    this.displayname = newdisplayname;
                    this.parent.childRenameUpdate(this, oldname);
                    return mimeType;
                }
                else     
                    throw new FilingException("FTPByteStore.updateMimeType: "+FilingException.IO_ERROR);
            } catch (IOException e) {
                throw new FilingException("FTPByteStore.updateMimeType: "+FilingException.IO_ERROR);
            }
        }
        return getMimeType();
         */
    }
    
    /**
     *  Determine if this byte store is readable.
     *
     *  @author Mark Norton
     *
     *  @return True if this byte store is readable.
     */
    public boolean isReadable() {
        return readable;
    }
    
    /**
     *  Update this byte store to being read only.
     *
     *  @author Mark Norton
     */
    public void updateReadOnly() {
        readable = true;
        writable = false;
    }
    
    /**
     *  Determine if this byte store is writable.
     *  @author Mark Norton
     *
     *  @return True if this byte store is writable.
     */
    public boolean isWritable() {
        return writable;
    }
    
    /**
     *  Force this byte store to be marked as writable.
     *
     *  @author Mark Norton
     */
    public void updateWritable() {
        writable = true;
    }
    
    /**
     *  Determine if this byte store can be appeneded.
     *
     *  @author Mark Norton
     *
     *  @return True if this byte store can be appended.
     */
    public boolean canAppend() {
        return appendable;
    }
    
    /**
     *  Force this byte store to be marked as appendable.
     *
     *  @author Mark Norton
     */
    public void updateAppendOnly() {
        appendable = true;
    }
    
    /**
     *  Get the current length of thsi byte store.
     *
     *  @author Mark Norton
     *
     *  @return The current length of this byte store.
     */
    public long length() throws osid.filing.FilingException {
        long length = 0;
        try {
            FTPClient client = rc.getClient();
            client.setFileTransferMode(FTP.STREAM_TRANSFER_MODE);
            //  The file to open consists of the root base plus, path to current directory plus name.
            //String fn = rc.getRootBase() + ((RemoteCabinet)getParent()).separator() + getDisplayName();
            //String fn = rc.getRootBase() + "/" + getDisplayName();
            String fn = getFullName();
            //System.out.println("length - file name to open: " + fn);
            FTPFile[] replies = client.listFiles(__fileListParser, fn);
            System.out.println("File Name = "+fn+" replies ="+replies+"Client:"+client.getStatus());
            if(replies == null) {
                System.out.println(client.getReplyCode());
                throw new osid.filing.FilingException("RemoteByteStore.length: "+osid.filing.FilingException.IO_ERROR);
            }
            //System.out.println(client.getReplyCode());
            length = replies[0].getSize();
        } catch (IOException e) {
            throw new osid.filing.FilingException("RemoteByteStore.length: "+osid.filing.FilingException.IO_ERROR);
        }
        return length;
    }
    
    /**
     *  Return the number of bytes used in this byte store.
     *  Note that this method is not part of osid.filing.ByteStore.
     *
     *  @author Mark Norton
     *
     *  @return The number of bytes currently written in the buffer.
     */
    public int used() {
        return used;
    }
    
    
    /**
     *  Get the bytes in a file associated with this byte store.
     */
    public byte[] getBytes() throws osid.filing.FilingException {
        // Open the file for input access.
        InputStream stream = null;
        String fn = getFullName();
        
        //  Allocate a buffer to hold the file.  Note that this must fit in memory and be
        //  small in size than Integer.MAX_VALUE.
        long trueLen = length();
        if (trueLen > (long) Integer.MAX_VALUE)
            throw new osid.filing.FilingException("File is too big to read.");
        int len = (int) trueLen;
        byte[] buf = new byte[len];

        //System.out.println ("getBytes - file name to retrieve: " + fn);
        try {
            FTPClient client = rc.getClient();
            stream = client.retrieveFileStream(fn);
        }
        catch (java.io.IOException ex1) {
            throw new osid.filing.FilingException(osid.filing.FilingException.IO_ERROR);
        }
                
        //  Copy the file stream into a buffer.
        try {
            for (int i = 0; i < len; i++) {
                buf[i] = (byte)stream.read();
            }
            stream.close();
        }
        catch (java.io.IOException ex) {
            throw new osid.filing.FilingException(osid.filing.FilingException.IO_ERROR);
        }
        
        return buf;
    }
    
    /**
     *  Iterate over bytes given a version.
     *
     *  @author Mark Norton
     *
     *  @return A ByteValueIterator which lists all bytes saved in this byte store.
     */
    public osid.shared.ByteValueIterator read(java.util.Calendar version) throws osid.filing.FilingException {
        byte[] buf = getBytes();
        
        osid.shared.ByteValueIterator it = (osid.shared.ByteValueIterator) new ByteValueIterator(buf);
        return it;
    }
    
    /**
     *  Replace the byte_store of this object with the array of bytes passed.
     *  <p>
     *  Note that there is no indication that this should be an append operation
     *  versus an overwrite in the documentation.  It is imlemented as overwrite here.
     *  <p>
     *  Note also that this assumes that the byte array passed is full of data (nothing unused).
     *  This is important to maintain the used total byte count.
     *
     *  @author Mark Norton
     *
     */
    public void write(byte[] b) throws osid.filing.FilingException {
        OutputStream stream = null;

        String fn = getFullName();
        try {
            FTPClient client = rc.getClient();
            stream = client.storeFileStream(fn);
        }
        catch (java.io.IOException ex1) {
            throw new osid.filing.FilingException(osid.filing.FilingException.IO_ERROR);
        }
                
        //  Copy the file stream into a buffer.
        try {
            for (int i = 0; i < b.length; i++) {
                stream.write(b[i]);
            }
            stream.close();
        }
        catch (java.io.IOException ex) {
            throw new osid.filing.FilingException(osid.filing.FilingException.IO_ERROR);
        }
    }
    
    /**
     *  Append the byte passed to the byte store.
     *  <br>
     *  A clarification request has been made to SourceForge on this method.  The
     *  value passed is declared as int, but documented as byte.  Until this is
     *  cleared up, this method throws UNIMPLEMENTED.
     *
     *  @author Mark Norton
     */
    public void writeByte(int b) throws osid.filing.FilingException {
        throw new osid.filing.FilingException (osid.filing.FilingException.UNIMPLEMENTED);
    }
    
    /**
     *  Write the bytes passed to the offset given.  If this set of bytes would
     *  extend beyond the current size of the byte store, it is expanded.  Note that
     *  len is redundant in this interface, as b.length should equal len.
     *  <p>
     *  Note also that any previous data in the range of off to off+len will be
     *  overwritten with the new bytes.
     *
     *  @author Mark Norton
     */
    public void writeBytesAtOffset(byte[] b, int off, int len) throws osid.filing.FilingException {
        throw new osid.filing.FilingException (osid.filing.FilingException.UNIMPLEMENTED);
    }
    
    /**
     *  Get the full file name of this byte store, all the way from the absolute root.
     */
    public String getFullName() {
        StringBuffer fn = new StringBuffer("/");
        ArrayList parts = new ArrayList(100);
        
        //  Walk path to root.
        RemoteCabinet ptr = (RemoteCabinet) getParent();
        while (ptr.getParent() != null) {
            parts.add(0, ptr.getDisplayName());
            ptr = (RemoteCabinet)ptr.getParent();
            // System.out.println("GETTING FN: parent "+ptr.getDisplayName());
        }
        
        //  Add intermediate path to file name.
        for (int i=0; i < parts.size(); i++) {
            fn.append("/" + parts.get(i));
        }
        
        //  Add the final file name.
        fn.append("/" + getDisplayName());
        
        return fn.toString();
    }
     public String getUrl() {
       String url = "ftp://"+rc.getUserName()+":"+rc.getPassword()+"@"+ rc.getServerName() + this.getFullName();
       //System.out.println("URL:"+ url);
        return url;
    }
}
