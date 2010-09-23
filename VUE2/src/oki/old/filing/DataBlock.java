package org.okip.service.filing.impl.rfs;

/*
 * $Source: /home/svn/cvs2svn-2.1.1/at-cvs-repo/VUE2/src/oki/old/filing/DataBlock.java,v $
 */

/**
 * A byte buffer that when serialized will only send portion
 * that has been used.
 *
 * Note that in order to be friendlier to the garbage collector
 * in dealing with the allocation of a series of large buffers, instances
 * of this class aren't intended to stay around long -- they're really
 * meant more as a bulk argument pass, and the RFS implementation is
 * frequently nulling out the buffer contained here.  So if for some
 * reason you want to keep one around for a while after getting
 * it back from one of the internal read calls, make a copy of it.
 * Passing it between the interal read/write's is fine.
 * <p>
 * Licensed under the {@link org.okip.service.ApiImplementationLicenseMIT MIT OKI&#153; API Implementation License}.
 *
 * @version $Revision: 1.1 $ / $Date: 2003-04-14 20:48:28 $
 */

class DataBlock
    implements java.io.Serializable, Cloneable
{
    int length;
    boolean containsEOF = false;
    transient int off = 0; // will always be 0 after serialization
    transient byte[] buf;
    
    protected DataBlock(byte[] buf, int off, int len)
    {
        this.buf = buf;
        this.off = off;
        this.length = len;
    }
    protected DataBlock(byte[] buf, int len)
    {
        this(buf, 0, len);
    }
    
    /*
     * We manually serialize the buffer here so that if,
     * of instance, we have a 512k buffer, yet only 3
     * bytes of it are filled up, we only have to send the 3
     * bytes instead of the whole buffer.
     */
    private void writeObject(java.io.ObjectOutputStream s)
        throws java.io.IOException
    {
        s.defaultWriteObject();
        s.write(buf, off, length);
    }
    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        s.defaultReadObject();
        buf = new byte[length];
        s.readFully(buf);
    }
    public String toString()
    {
        String s = "DataBlock" +  Integer.toHexString(hashCode()) + "[";
        if (buf != null) {
            s += buf.length + "@" + Integer.toHexString(buf.hashCode());
            if (buf.length != length)
                s += " used="+length;
            if (off != 0)
                s += " off=" + off;
            if (length <= 8)
                s += " \"" + new String(buf, 0, length) + "\"";
        } else
            s += "length="+length + " off="+off;
        if (containsEOF)
            s += " EOF";
        return s + "]";
    }

    protected DataBlock copy()
    {
        try {
            return (DataBlock) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
    }
}

