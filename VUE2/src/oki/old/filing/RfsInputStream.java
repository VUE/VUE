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
 * $Source: /home/svn/cvs2svn-2.1.1/at-cvs-repo/VUE2/src/oki/old/filing/RfsInputStream.java,v $
 */

/**
 * RFS InputStream.  Provides a buffered OkiInputStream to a local or remote file.
 * RfsInputBuffer handles the actual data transfer and read calls.
 * <p>
 * Is not seekable.
 * </p>
 * <p>
 * Licensed under the {@link org.okip.service.ApiImplementationLicenseMIT MIT OKI&#153; API Implementation License}.
 *
 * @version $Name: not supported by cvs2svn $ / $Revision: 1.1 $ / $Date: 2003-04-14 20:48:28 $
 */

public class RfsInputStream
    implements OkiInputStream
{
    private String idStr = null;
    private long filePos = 0; // the position in the file
    private long fileStart = 0; // the position in the file of the START of the current buffer
    private int bufPos = 0; // the position within the current RfsInputBuffer
    private int bufferSize;
    private DataBlock curBuffer;
    private boolean noMoreBuffers = false;
    private RfsInputBuffer inBuf;
    private RfsByteStore rfsEntry;
    private java.io.FileInputStream inStream;
    private boolean localPassthru;

    protected RfsInputStream(RfsByteStore rfsEntry)
        throws FilingException
    {
        this.rfsEntry = rfsEntry;
        this.idStr = rfsEntry.idStr;
        this.bufferSize = rfsEntry.factory.getIOBufferSize();
        this.localPassthru = !rfsEntry.hasClient;

        if (this.idStr == null)
            throw new FilingException("null filename");
    }

    private java.io.FileInputStream getInputStream()
        throws FilingException
    {
        if (!localPassthru)
            throw new FilingException("RfsInputStream internal error: attempt to get local stream remotely");
        
        try {
            if (this.inStream == null)
                this.inStream = new java.io.FileInputStream(rfsEntry.idStr);
        } catch (java.io.FileNotFoundException e) {
            throw new NotFoundException(e, rfsEntry.idStr);
        }
        return this.inStream;
    }
    
    private void nullbuffer()
    {
        if (curBuffer != null) {
            curBuffer.buf = null;
            curBuffer = null;
        }
    }

    private DataBlock getBuffer()
        throws FilingException
    {
        if (curBuffer != null && bufPos < curBuffer.length)
            return curBuffer;
        
        if (noMoreBuffers) {
            nullbuffer();
            return null;
        }
        
        // we're going to be fetching a new buffer...
        if (curBuffer != null) {
            fileStart += curBuffer.length;
            curBuffer.buf = null;
        }
        /*
         * If we're being called remotely, this.in will be
         * null every time.  If locally, we keep re-using
         * the same RfsInputBuffer.
         */
        if (inBuf == null)
            inBuf = new RfsInputBuffer(rfsEntry.factory, idStr, fileStart, bufferSize);
        else
            inBuf.setStartPosition(fileStart);
        
        if (filePos < fileStart)
            throw new FilingException("filePos<fileStart");

        bufPos = (int) (filePos - fileStart);
        curBuffer = inBuf.readBuffer();
        
        if (curBuffer == null || curBuffer.containsEOF)
            noMoreBuffers = true;

        if (bufPos < 0 || (curBuffer != null && bufPos >= curBuffer.length))
            throw new FilingException("bad bufPos="+bufPos+">="+curBuffer.length);

        
        return curBuffer;
    }
  
    /*
     * Reads up to a full IO buffer full of data and returns
     * the internal array of bytes -- internal use only for
     * fast copies.
     */
    protected DataBlock readMax()
        throws FilingException
    {
        if (localPassthru) {
            try {
                if (curBuffer == null) {
                    int bsize = Math.min(bufferSize, getInputStream().available());
                    curBuffer = new DataBlock(new byte[bsize], 0);
                }
                int got = getInputStream().read(curBuffer.buf);
                if (got <= 0)
                    nullbuffer();
                else
                    curBuffer.length = got;
            } catch (java.io.IOException e) {
                throw new FilingException(e);
            }
        } else {
            if (getBuffer() == null)
                return null;
            if (bufPos != 0)
                throw new FilingException("readMax must not follow other read calls (bp="+bufPos+")");
            bufPos = curBuffer.length;
            filePos += curBuffer.length;
        }
        return curBuffer;
    }

    /**
     * Reads up to len bytes of data from the IO Object into an
     * array of bytes.
     * @param      b     the buffer into which the data is read.
     * @param      off   the start offset in array <code>b</code>
     *                   at which the data is written.
     * @param      max   the maximum number of bytes to read.
     * @return     the total number of bytes read into the buffer, or
     *             <code>-1</code> if there is no more data because the end of
     *             the stream has been reached.  The amount copied on this
     *             call may be < max even if there's more in the file due
     *             to buffering implementation details.
     */
    public int read(byte[] b, int off, int max)
        throws FilingException
    {
        if (localPassthru) {
            try {
                return getInputStream().read(b, off, max);
            } catch (java.io.IOException e) {
                throw new FilingException(e);
            }
        }
        
        /* For this read, we may copy out less than max even if there's
         * more data available because we only send what remains in
         * our current buffer.  This is fine.  The next call to read
         * will initiate the next buffer.  (So the semantics of the
         * return value need to paid particular attention to: e.g.,
         * we've only reached EOF if we return -1, not just because we
         * may have returned less than max.
         */

        if (getBuffer() == null)
            return -1;
        
        try {
            int available = curBuffer.length - bufPos;
            int toSend = available >= max ? max : available;
                
            System.arraycopy(curBuffer.buf, bufPos, b, off, toSend);
            bufPos += toSend;
            filePos += toSend;
            return toSend;
        }
        catch (Exception e) {
            throw new FilingException(e);
        }
    }

    /**
     * Reads some number of bytes from the IO Object and stores
     * them into the buffer array b.
     */
    public int read(byte[] b)
        throws FilingException
    {
        return read(b, 0, b.length);
    }

    /**
     * Reads the next byte of data from the IO Object.
     */
    public int read()
        throws FilingException
    {
        if (localPassthru) {
            try {
                return getInputStream().read();
            } catch (java.io.IOException e) {
                throw new FilingException(e);
            }
        }
        
        if (getBuffer() == null)
            return -1;
        // We must use & return a char here to 
        // avoid file data appearing as the
        // value -1, which using either byte or int
        // will cause.
        char c = (char) curBuffer.buf[bufPos];
        skip(1);
        return c;
    }

    /**
     * Skips over and discards n bytes of data from this IO Object.
     * This implementation allows skipping backwards up to
     * whatever is in our current buffer if we've got a
     * remote client (which is autmoatically buffered).
     * It will NOT allow skipping beyond EOF, unlike the
     * java implementation (at least on OSX Java 1.3.1).
     */

    public long skip(long n)
        throws FilingException
    {
        if (localPassthru) {
            try {
                return getInputStream().skip(n);
            } catch (java.io.IOException e) {
                throw new FilingException(e);
            }
        }
        
        if (n == 0)
            return 0;
            
        long oldPos = filePos;
        long newPos;

        if (oldPos + n < fileStart) {
            // n is less than 0 and we're trying tt
            // back up past current buffer full
            newPos = fileStart;
            n += (fileStart - (oldPos+n));
        } else
            newPos = oldPos + n;

        int windowSize;
        
        if (curBuffer == null) {
            // no buffer -- we're just repositioning
            fileStart = newPos;
            windowSize = this.bufferSize;
        } else
            windowSize = curBuffer.length;

        if (curBuffer == null || newPos < fileStart || newPos >= fileStart + windowSize) {
            // If we've moved outside the contents of the
            // current buffer, invalidate current buffer.
            filePos = newPos;
            long eofPos = rfsEntry.length();
            if (filePos >= eofPos)
                filePos = eofPos-1;
            
            if (filePos > eofPos - windowSize) {
                // We're close to end of file -- back buffer up
                // against EOF
                fileStart = eofPos - windowSize;
                if (fileStart < 0)
                    fileStart = 0;
            } else
                fileStart = filePos;
            bufPos = (int) (filePos - fileStart);
            nullbuffer();
        } else {
            // skip within existing buffer
            bufPos += n;
            filePos += n;
        }
    
        getBuffer();
        return (fileStart+bufPos) - oldPos; 
    }

    /**
     * Returns the number of bytes that can be read (or skipped over)
     * from this IO Object without blocking by the next caller of a
     * method for this IO Object.
     */
    public long available()
        throws FilingException
    {
        if (localPassthru) {
            try {
                return getInputStream().available();
            } catch (java.io.IOException e) {
                throw new FilingException(e);
            }
        }
        
        if (curBuffer == null)
            return 0;
        else
            return curBuffer.length - bufPos;
    }

    /**
     * Closes this IO Object and releases any system resources
     * associated with the IO Object.
     */
    public void close()
        throws FilingException
    {
        noMoreBuffers = false;
        nullbuffer();
        if (inBuf != null) {
            inBuf.close();
            inBuf = null;
        }
        if (inStream != null) {
            try {
                inStream.close();
                inStream = null;
            } catch (java.io.IOException e) {
                throw new FilingException(e);
            }
        }
    }

    public java.io.InputStream getNativeInputStream()
        throws FilingException {
        throw new UnsupportedFilingOperationException("getNativeInputStream not supported");
    }
    public void mark(int readlimit)
        throws FilingException {
        throw new UnsupportedFilingOperationException("mark not supported");
    }
    public void reset()
        throws FilingException {
        throw new UnsupportedFilingOperationException("reset not supported");
    }
    public boolean markSupported() {
	return false;
        // this would be easy to implement
        // by simply retaining a list of our
        // input buffers as we get them and
        // re-activating them as needed.
    }

    public String toString()
    {
        String s = "RfsInputStream[" + idStr + "] fp="+filePos + " bp="+bufPos;
        if (localPassthru)
            s += " LOCAL";
        if (curBuffer == null)
            s += " <nullbuf>";
        else if (curBuffer.length <= 16)
            s += " [" + new String(curBuffer.buf) + "]";
        
        return s;
    }
}



