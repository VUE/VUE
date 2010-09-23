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
 * $Source: /home/svn/cvs2svn-2.1.1/at-cvs-repo/VUE2/src/oki/old/filing/RfsOutputStream.java,v $
 */

/**
 * RFS OutputStream.  Provides a buffered OkiOutputStream to a local or remote file.
 * RfsOutputBuffer handles the actual data transfer and write calls.
 * <p>
 * Licensed under the {@link org.okip.service.ApiImplementationLicenseMIT MIT OKI&#153; API Implementation License}.
 *
 * @version $Name: not supported by cvs2svn $ / $Revision: 1.1 $ / $Date: 2003-04-14 20:48:28 $
 */

class RfsOutputStream
    implements OkiOutputStream
{
    private RfsFactory factory = null;
    private String idStr = null;
    private boolean append = false;
    private int bufferSize;
    private int bufferPos = 0;
    private RfsOutputBuffer out = null;
    private RfsEntryCache watchingCache = null;

    protected RfsOutputStream(RfsFactory factory, String idStr, RfsEntryCache cache)
        throws FilingException
    {
        this(factory, idStr, -1, false, cache);
    }
    protected RfsOutputStream(RfsFactory factory, String idStr, int bufferSize, boolean append, RfsEntryCache cache)
        throws FilingException
    {
        this.factory = factory;
        this.idStr = idStr;
        this.append = append;
        this.watchingCache = cache;
        if (bufferSize <= 0)
            this.bufferSize = factory.getIOBufferSize();
        else
            this.bufferSize = bufferSize;
    }

    private RfsOutputBuffer getOutputBuffer()
        throws FilingException
    {
        if (this.out == null)
            this.out = new RfsOutputBuffer(factory, idStr, bufferSize, append, watchingCache);
        return this.out;
    }
    
    private byte[] getBuffer()
        throws FilingException
    {
        return getOutputBuffer().getBuffer();
    }

    /**
     * Writes b.length bytes to this IO Object.
     */
    public void write(byte[] b)
        throws FilingException
    {
        write(b, 0, b.length);
    }
    
    /*
     * Write the given data buffer straight through
     * without touching our internal buffer.
     */
    protected void writeThrough(byte[] data, int off, int len)
        throws FilingException
    {
        if (len <= 0) 
            return;
        DataBlock dataBlock = new DataBlock(data, off, len);
        writeThrough(dataBlock);
        dataBlock.buf = null;
    }
    protected void writeThrough(DataBlock dataBlock)
        throws FilingException
    {
        flush();
        getOutputBuffer().writeThrough(dataBlock);
    }

    /**
     * Writes len bytes from the specified byte array starting at
     * offset off to this IO Object.
     */
    public void write(byte[] data, int off, int len)
        throws FilingException
    {
        try {
            int toSend = len;
            int dataIndex = off;
            int roomLeft;
            /*
             * Keep filling & flushing our buffer until we've
             * processed all the data we've been asked to send.
             */
            while (toSend > 0) {
                if (this.bufferPos == 0 && toSend >= this.bufferSize) {
                    /* Don't bother doing the arraycopy if we're at
                     * the start of a buffer (nothing to flush) and would need to
                     * immediately flush the buffer -- just deliver right
                     * from the user buffer.
                     */
                    writeThrough(data, dataIndex, this.bufferSize);
                    dataIndex += this.bufferSize;
                    toSend -= this.bufferSize;
                } else if (toSend >= (roomLeft = this.bufferSize - this.bufferPos)) {
                    /*
                     * We're going to exceed our remaining buffer space -- fill it, flush it,
                     * and come back around for more.
                     */
                    System.arraycopy(data, dataIndex, getBuffer(), this.bufferPos, roomLeft);
                    dataIndex += roomLeft;
                    toSend -= roomLeft;
                    this.bufferPos += roomLeft;
                    flush();
                } else {
                    /*
                     * There isn't enough data left to fill our buffer --
                     * copy data into our buffer and return.
                     */
                    System.arraycopy(data, dataIndex, getBuffer(), this.bufferPos, toSend);
                    this.bufferPos += toSend;
                    toSend = 0;
                }
            }
        } catch (FilingException e) {
            throw e;
        } catch (Exception e) {
            throw new FilingException(e);
        }
    }


    /**
     * Writes the specified byte to this IO Object.
     */
    public void write(int b)
        throws FilingException
    {
        byte[] buffer = getBuffer();
        buffer[this.bufferPos++] = (byte) b;
        if (this.bufferPos == buffer.length)
            flush();
    }

    /**
     * Flushes this IO Object and forces any buffered output bytes
     * to be written out to the stream.
     */
    public void flush()
        throws FilingException
    {
        if (this.out != null && this.bufferPos > 0) {
            this.out.writeBuffer(this.bufferPos);
            this.bufferPos = 0;
        }
    }

    /**
     * Closes this IO Object and releases any system resources
     * associated with the IO Object.
     */
    public void close()
        throws FilingException
    {
        flush();
        if (this.out != null) {
            this.out.closeBufferStream();
            this.out = null;
        }
    }

    public void finalize()
        throws FilingException
    {
        close();
    }
    
    /**
     * Return OutputStream in native environment which can be used to
     * access this ByteStore.  In Java, this is a java.io.OutputStream
     * object.  From this one may obtain a Writer via OutputStreamReader.
     *
     * @return A java.io.OutputStream object which may be used to write to this
     * ByteStore.
     *
     * @throws FilingPermissionDeniedException - if Factory Owner
     * does not have permission to write to this ByteStore.
     * @throws FilingIOException - if an IO error occurs.
     */
    public java.io.OutputStream getNativeOutputStream()
        throws FilingException
    {
        return new org.okip.service.filing.api.JavaOutputStreamAdapter(this);
    }

    public String toString()
    {
        return "RfsOutputStream[" + idStr + "] len=" + bufferSize + " pos="+bufferPos;
    }
}
