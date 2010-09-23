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
 * $Source: /home/svn/cvs2svn-2.1.1/at-cvs-repo/VUE2/src/oki/old/filing/RfsByteStore.java,v $
 */

/**
 * remote file system ByteStore implementation.
 *
 * <p>
 * Licensed under the {@link org.okip.service.ApiImplementationLicenseMIT MIT OKI&#153; API Implementation License}.
 *
 * @version $Name: not supported by cvs2svn $ / $Revision: 1.1 $ / $Date: 2003-04-14 20:48:28 $
 */

public class RfsByteStore extends RfsEntry
implements org.okip.service.filing.api.ByteStore
{
    protected RfsByteStore(RfsFactory factory, String idStr)
        throws FilingException
    {
        initEntry(factory, idStr, false, null, null);
    }
    protected RfsByteStore(RfsFactory factory, String idStr, boolean create)
        throws FilingException
    {
        initEntry(factory, idStr, create, null, null);
    }
    protected RfsByteStore(RfsCabinet parent, String idStr, RfsEntryCache rc)
        throws FilingException
    {
        if (parent == null)
            initEntry(null, idStr, false, rc, null);
        else
            initEntry(parent.factory, idStr, false, rc, parent);
    }
    
    
    /**
     * Returns the length of this ByteStore
     *
     * @return The length, in bytes, of this ByteStore
     *
     * @throws FilingIOException - if an IO error occurs reading Object
     */
    public long length()
        throws FilingException
    {
        if (hasClient) {
            try {
                if (cache != null)
                    return cache.length();
                else
                    return ((Long) factory.invoke(this, "length")).longValue();
            } catch (Exception ex) {
                return 0;
            }
        } else 
            return getLocalFile().length();
    }

    /**
     * Tests whether the Factory Owner may append to this ByteStore.
     *
     * @return <code>true</code> if and only if the Factory Owner is
     *          allowed to append to this ByteStore
     *          <code>false</code> otherwise.
     *
     */
    public boolean canAppend()
        throws FilingException
    {
        return canWrite();
    }

  /**
   * Marks this ByteStore so that only append operations are allowed.
   *
   * @return <code>true</code> if and only if the operation succeeded;
   *          <code>false</code> otherwise
   *
   * @throws FilingException - if Factory Owner
   * does not have permission to set the ByteStore writable or
   * if an IO error occurs or
   * if it is not possible to restrict to append-only in this implementation
   */
  public boolean setAppendOnly()
  throws FilingException {
    throw new UnsupportedFilingOperationException(
      "Cannot set a ByteStore AppendOnly in this implementation");
  }


  /**
   * Get the mime-type of this ByteStore.
   *
   * @return the mime-type (Content-Type in a jar file manifest)
   */
  public String getMimeType()
  throws FilingException {

    // quick implementation.  Better implementation would have more
    // mime-types, configurable, and read the first few bytes of the
    // file as in the UNIX "file" command

    String name      = this.getName();
    String extension = name.substring(name.lastIndexOf(".") + 1);
    String mimeType  = (String) mimeTypesSuffixTable.get(extension);

    if (mimeType != null) {
      return mimeType;
    }
    else {
      return "application/octet-stream";
    }
  }

  private static java.util.Hashtable mimeTypesSuffixTable =
    new java.util.Hashtable();

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

    //      mimeTypesSuffixTable.put("application/x-compress");
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
    mimeTypesSuffixTable.put("mpeg", "video/mpeg");
    mimeTypesSuffixTable.put("mpg", "video/mpeg");
    mimeTypesSuffixTable.put("mpe", "video/mpeg");
    mimeTypesSuffixTable.put("mov", "video/quicktime");
    mimeTypesSuffixTable.put("qt", "video/quicktime");
    mimeTypesSuffixTable.put("avi", "video/x-msvideo");
  }

  /**
   * Set the mime-type of this ByteStore.
   * <p>Returns the actual mime-type set for the ByteStore.  This may
   * differ from the supplied mime-type for several reasons.  The
   * implementation may not support the setting of the mime-type, in
   * which case the default mime-type or one derived from the content
   * bytes or file extension may be used.  Or a cannonical, IANA
   * mime-type (see
   * {@link "http://www.iana.org/assignments/media-types/index.html"} may be
   * substuted for a vendor or experimental type.
   *
   * @param mimeType
   *
   *
   * @return String
   * @throws FilingException - if Factory Owner
   * does not have permission to set the mime-type or
   * if an IO error occurs setting the mime-type.
   */
  public String setMimeType(String mimeType)
  throws FilingException {
    // this implementation cannot set the mime-type, it derives it
    // from a guess based on the name
    return this.getMimeType();
  }

  /**
   * Returns the Digest using the specified algorithm used,
   * (algorithms such as md5 or crc).
   *
   * @param digestAlgorithm
   *
   * @return digest or null if unsupported
   *
   */
  public String getDigest(org.okip.service.shared.api.Type digestAlgorithm)
  throws FilingException {
    return null;
  }


  /**
   * Return OkiInputStream object which can be used to access this ByteStore.
   *
   * @return OkiInputStream object which may be used to access this
   * ByteStore.
   *
   * @throws FilingException - if Factory Owner
   * does not have permission to read this ByteStore or
   * if an IO error occurs.
   */
    public OkiInputStream getOkiInputStream()
        throws FilingException
    {
        return new RfsInputStream(this);
    }

  /**
   * Return OkiOutputStream object which can be used to access this ByteStore.
   *
   * @return OkiOutputStream object which may be used to access this
   * ByteStore.
   *
   * @throws FilingException - if Factory Owner
   * does not have permission to read this ByteStore or
   * if an IO error occurs.
   */
    public OkiOutputStream getOkiOutputStream()
        throws FilingException
    {
        return new RfsOutputStream(factory, this.idStr, this.cache);
    }


  /**
   * Return true if this ByteStore exists in multiple equivalent versions.
   *
   *
   * @return boolean
   *
   */
  public boolean hasVersions()
  throws FilingException {
    return false;
  }

  /**
   * Return latest version of this ByteStore.
   *
   *
   * @return ByteStore
   *
   */
  public ByteStore getLatestVersion() {
    return this;
  }

  /**
   * Return first version of this ByteStore.
   *
   *
   * @return ByteStore
   *
   */
  public ByteStore getBaseVersion()
  throws FilingException {
    return this;
  }

  /**
   * Return specified version of this ByteStore.
   *
   * @param versionKey
   *
   * @return ByteStore
   *
   * @throws UnsupportedFilingOperationException - if versions
   * are not implemented.
   */
  public ByteStore getVersion(java.lang.String versionKey)
  throws FilingException {
    throw new UnsupportedFilingOperationException(
      "ByteStore versioning not implemented");
  }

  /**
   * Return all versions of this ByteStore.
   *
   * @return ByteStore[]
   *
   */
  public ByteStore[] getVersions()
  throws FilingException {
    ByteStore[] b = new ByteStore[1];

    b[0] = this;

    return b;
  }

  /**
   * Create new version for this ByteStore.
   *
   * @param agent
   * @param versionKey
   *
   * @return ByteStore
   *
   * @throws FilingException - if Factory Owner
   * does not have permission to create a new version of this ByteStore or
   * if an IO error occurs or
   * if versions are not implemented.
   */
  public ByteStore newVersion(org.okip.service.shared.api.Agent agent,
                              String versionKey)
  throws FilingException {
    throw new UnsupportedFilingOperationException(
      "ByteStore versioning not implemented");
  }

    /**
     * Tests this entry for equality with the given object.
     * Returns <code>true</code> if and only if the argument is not
     * <code>null</code> and is equal to this RfsByteStore.
     *
     * This implentation tests to see if the creating
     * factories are the same object, and that getID().toString()
     * for both entries is equivalent.
     *
     * @param object
     *
     * @return  <code>true</code> if and only if the objects are operationally equivalent
     *          <code>false</code> otherwise, or if an error occurs
     */
    public boolean equals(Object object)
    {
        if (this == object)
            return true;
        try {
            if (object instanceof RfsByteStore) {
                RfsByteStore rb = (RfsByteStore) object;
                return this.factory == rb.factory
                    && this.getID().toString().equals(rb.getID().toString());
                
            } else
                return false;
        } catch (Throwable t) {
            return false;
        }
    }
        
    /**
     * Compares this this ByteStore to another ByteStore.
     *
     * @param byteStore The <code>ByteStore</code> to be compared to
     * this ByteStore
     *
     * @return 0 if they are equal, less than 0 or greater than 0 if
     * they differ, depending on the implementation-dependent quality
     * used for the comparison.
     *
     * @throws FilingException - if Factory Owner
     * does not have read permission on the ByteStore being compared to or
     * if an IO error occurs reading the ByteStore being compared to
     *
     * @see java.lang.Comparable
     */

    public int compareTo(ByteStore byteStore)
        throws FilingException
    {
        try {
            return getID().toString().compareTo(byteStore.getID().toString());
        } catch (Throwable e) {}
        return -1000;
    }

    /**
     * Returns a diagnostic string representation of this ByteStore.
     *
     * @return  A string form of this ByteStore
     */
    public String toString()
    {
        return "RfsByteStore[" + super.toString() + "]";
    }

    /**
     * Tests whether this is a Cabinet.
     * @return boolean - always returns false
     */
    public final boolean isCabinet() {
        return false;
    }
    
    /**
     * Tests whether this is a ByteStore.
     * @return boolean - always returns true
     */
    public final boolean isByteStore() {
        return true;
    }
}
