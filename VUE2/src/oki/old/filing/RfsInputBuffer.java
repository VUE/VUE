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
 * $Source: /home/svn/cvs2svn-2.1.1/at-cvs-repo/VUE2/src/oki/old/filing/RfsInputBuffer.java,v $
 */

/**
 * RFS Input Buffer.  Obtains and holds a chunk of file data,
 * either locally or remotely.
 * <p>
 * Licensed under the {@link org.okip.service.ApiImplementationLicenseMIT MIT OKI&#153; API Implementation License}.
 *
 * @version $Revision: 1.1 $ / $Date: 2003-04-14 20:48:28 $
 */

public class RfsInputBuffer
    implements java.io.Serializable
{
    private String idStr = null;
    private long startPos = -1;
    private int maxSize = -1;
    
    private transient RfsFactory factory = null;
    private transient java.io.FileInputStream fileInputStream = null;
    private transient DataBlock dataBlock;
    private transient byte[] buffer;
    private transient long openedStart = -1;

    protected RfsInputBuffer(RfsFactory factory, String idStr, long startPos, int maxSize)
        throws FilingException
    {
        if (factory == null || idStr == null || startPos < 0 || maxSize <= 0)
            throw new FilingException("Create RfsInputBuffer failed: bad argument(s)");
        
        this.factory = factory;
        this.idStr = idStr;
        this.startPos = startPos;
        this.maxSize = maxSize;
    }

    public void setStartPosition(long startPosition)
    {
        this.startPos = startPosition;
    }

    private void getInputStream()
        throws FilingException
    {
        try {
            if (this.fileInputStream == null) {
                this.fileInputStream = new java.io.FileInputStream(this.idStr);
                openedStart = startPos;
                if (startPos > 0) {
                    long skipped;
                    try {
                        skipped = this.fileInputStream.skip(startPos);
                    } catch (java.io.IOException e) {
                        throw new FilingException(e);
                    }
                    if (skipped < startPos)
                        throw new FilingIOException("skipped " + skipped + " of " + startPos + " bytes");
                }
            }
        } catch (java.io.FileNotFoundException e) {
            throw new NotFoundException(e, this.idStr);
        }
        if (startPos != openedStart)
            throw new FilingIOException("RfsInputBuffer internal error: startPos "
                                        + startPos + " != openedStart " + openedStart);
        
    }

   public DataBlock readBuffer()
        throws FilingException
    {
        if (factory != null && factory.getClient() != null)
            return (DataBlock) factory.invoke(this, "readBufferRemotely");
        else
            return readBufferLocally();
    }

    private DataBlock getDataBlock(int size)
        throws FilingException
    {
        // the way we manage DataBlocks may look a bit
        // odd as we're trying to help out the garbage
        // collector a little as we're dealing with
        // large chunks of memory.
        
        if (buffer == null || size > buffer.length)
            buffer = new byte[size];
        if (dataBlock != null) {
            dataBlock.buf = buffer;
            dataBlock.length = size;
        } else
            dataBlock = new DataBlock(buffer, size);
        return dataBlock;
    }

    public DataBlock readBufferRemotely()
        throws FilingException
    {
        DataBlock dataBlock = readBufferLocally();
        
        /* Closing the stream is going to null out our dataBlock, so
         * make a shallow copy here just before returning. */

        if (dataBlock != null)
            dataBlock = dataBlock.copy();

        /* We close the file after each use -- we cannot maintain
         * state -- we have to re-initialize again at next RMI call.
         * This is a kindness to the server only and could be skipped
         * for a miniscule response-time improvement. */
        
        close();
        return dataBlock;
    }

    private DataBlock readBufferLocally()
        throws FilingException
    {
        getInputStream();
        try {
            int available = this.fileInputStream.available();
            int requestSize;
            if (available <= 0)
                requestSize = this.maxSize;// it's going to block anyway, so just go for it
            else
                requestSize = available;
            if (requestSize > this.maxSize)
                requestSize = this.maxSize;
            getDataBlock(requestSize);
            int got = this.fileInputStream.read(dataBlock.buf, 0, dataBlock.length);
            if (got <= 0) {
                this.dataBlock.buf = null;
                this.dataBlock = null;
            } else if (got < requestSize)
                this.dataBlock.length = got;
            if (got == available) {
                // we got all we could -- check to see if we hit EOF
                if (startPos + got == RfsEntry.getEntryFile(idStr, false).length())
                    this.dataBlock.containsEOF = true;
            }
            return this.dataBlock;
        } catch (FilingException fe) {
            throw fe;
        } catch (Exception e) {
            throw new FilingException(e);
        }
    }

    protected void close()
        throws FilingException
    {
        buffer = null;
        if (dataBlock != null) {
            dataBlock.buf = null;
            dataBlock = null;
        }
        try {
            if (fileInputStream != null) {
                fileInputStream.close();
                fileInputStream = null;
            } 
        } catch (Exception e) {
            throw new FilingException(e, "close " + idStr);
        }
    }

    public void finalize()
    {
        try {
            close();
        } catch(Exception e) {}
    }
  
    public String toString()
    {
        return "RfsInputBuffer[" + idStr + "] p="+startPos;
    }
}
