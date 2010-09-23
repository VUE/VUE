package org.okip.service.filing.impl.rfs;

import org.okip.service.filing.api.*;

/**
   Copyright (c) 2002 Massachusetts Institute of Technology

   This work, including software, documents, or other related items (the
   "Software"), is being provided by the copyright holder(s) subject to
   the terms of the MIT OKI&#153; API Implementation License. By
   obtaining, using and/or copying this Software, you agree that you have
   read, understand, and will comply with the following terms and
   conditions of the MIT OKI&#153; API Implementation License:

   Permission to use, copy, modify, and distribute this Software and its
   documentation, with or without modification, for any purpose and
   without fee or royalty is hereby granted, provided that you include
   the following on ALL copies of the Software or portions thereof,
   including modifications or derivatives, that you make:

    *  The full text of the MIT OKI&#153; API Implementation License in a
       location viewable to users of the redistributed or derivative
       work.

    *  Any pre-existing intellectual property disclaimers, notices, or
       terms and conditions. If none exist, a short notice similar to the
       following should be used within the body of any redistributed or
       derivative Software:
       "Copyright (c) 2002 Massachusetts Institute of Technology
       All Rights Reserved."

    *  Notice of any changes or modifications to the MIT OKI&#153;
       Software, including the date the changes were made. Any modified
       software must be distributed in such as manner as to avoid any
       confusion with the original MIT OKI&#153; Software.

   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
   IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
   CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
   TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
   SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

   The name and trademarks of copyright holder(s) and/or MIT may NOT be
   used in advertising or publicity pertaining to the Software without
   specific, written prior permission. Title to copyright in the Software
   and any associated documentation will at all times remain with the
   copyright holders.

   The export of software employing encryption technology may require a
   specific license from the United States Government. It is the
   responsibility of any person or organization contemplating export to
   obtain such a license before exporting this Software.
*/

/*
 * $Source: /home/svn/cvs2svn-2.1.1/at-cvs-repo/VUE2/src/oki/old/filing/RfsOutputBuffer.java,v $
 */

/**
 * RFS Output Buffer.  Creates & holds a buffer of data that can be written
 * to a local or remote file upon request.
 * <p>
 * Licensed under the {@link org.okip.service.ApiImplementationLicenseMIT MIT OKI&#153; API Implementation License}.
 *
 * @version $Revision: 1.1 $ / $Date: 2003-04-14 20:48:28 $
 */

public class RfsOutputBuffer
    implements java.io.Serializable
{
    private String idStr = null;
    private boolean appending = false;
    private transient byte[] buffer;
    
    private transient RfsFactory factory;
    private transient java.io.FileOutputStream fileOutputStream;
    private transient int size;
    private transient RfsEntryCache watchingCache;

    protected RfsOutputBuffer(RfsFactory factory, String idStr, int size, boolean append, RfsEntryCache wc)
        throws FilingException
    {
        if (factory == null || idStr == null || size <= 0)
            throw new FilingException("Create RfsOutputBuffer failed: bad argument(s)");
        
        this.factory = factory;
        this.idStr = idStr;
        this.appending = append;
        this.size = size;
        this.buffer = null;
        this.watchingCache = wc;
    }

    private void openFileOutputStream()
        throws FilingException
    {
        try {
            if (this.fileOutputStream == null)
                this.fileOutputStream = new java.io.FileOutputStream(this.idStr, this.appending);
        } catch (Exception e) {
            throw new FilingException(e);
        }
    }

    protected byte[] getBuffer()
    {
        if (this.buffer == null)
            this.buffer = new byte[size];
        return this.buffer;
    }
  
    protected void writeThrough(DataBlock dataBlock)
        throws FilingException
    {
        writeBuffer(dataBlock);
    }
    
    protected void writeBuffer(int len)
        throws FilingException
    {
        if (len <= 0) 
            return;
        DataBlock dataBlock = new DataBlock(this.buffer, len);
        writeBuffer(dataBlock);
        dataBlock.buf = null;
    }
    
    private void writeBuffer(DataBlock dataBlock)
        throws FilingException
    {
        if (dataBlock.length <= 0) 
            return;
        if (factory != null && factory.getClient() != null) {
            RfsEntryCache rfsData = (RfsEntryCache)
                factory.invoke(this, "writeBufferRemotely", DataBlock.class, dataBlock);
            this.appending = true;
            if (this.watchingCache != null)
                this.watchingCache.copyUpdate(rfsData);
        } else
            writeBufferLocally(dataBlock);
    }

    public RfsEntryCache writeBufferRemotely(DataBlock dataBlock)
        throws FilingException
    {
        writeBufferLocally(dataBlock);
        return new RfsEntryCache(new java.io.File(this.idStr));
    }
    
    private void writeBufferLocally(DataBlock dataBlock)
        throws FilingException
    {
        try {
            openFileOutputStream();
            if (this.fileOutputStream != null)
                this.fileOutputStream.write(dataBlock.buf, 0, dataBlock.length);
        } catch (Exception e) {
            throw new FilingException(e);
        }
    }
  
    public void closeBufferStream()
        throws FilingException
    {
        this.appending = false;
        this.buffer = null;
        try {
            if (this.fileOutputStream != null) {
                this.fileOutputStream.close();
                this.fileOutputStream = null;
            } 
        } catch (Exception e) {
            throw new FilingException(e);
        }
    }

    public void finalize()
    {
        try {
            closeBufferStream();
        } catch (Exception e) {}
    }
  
    public String toString()
    {
        String s = "RfsOutputBuffer["+idStr+"]";
        if (buffer != null)
            s += " size="+buffer.length;
        return s;
    }

}
