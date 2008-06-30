/*
 * Copyright 2003-2008 Tufts University  Licensed under the
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

package tufts.oki.localFiling;
import java.io.*;
import java.lang.*;
import java.util.*;
import tufts.oki.shared.*;

/**
 *  Implements the ByteStore class on a local filing system.  Care must be taken to use
 *  absolute local file names when creating the LocalByteStore.  Two methods are provided
 *  for getting the bytes in the byte store:  getBytes(), and read().  One method is 
 *  provided for saving bytes in a file:  write().
 *
 * @author  Mark Norton
 *
 */
public class LocalByteStore extends LocalCabinetEntry implements osid.filing.ByteStore
{
    //private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(LocalByteStore.class);
    
    //private byte[] byte_store;          //  This is the actual byte storage buffer. (UNUSED)
    private int used = 0;                   //  Bytes written to the buffer.
    private String mime_type = null;    //  The mime type of this byte store.
    private boolean writable = true;    //  Is it writable?
    private boolean readable = true;    //  Is it readable?
    private boolean appendable = true;  //  Is it appendable?
    private File file = null;
    
    /**
     *  Create a ByteStore object given a display name and parent.
     *
     *  @author Mark Norton
     *
     */
    public LocalByteStore(String displayName, osid.filing.Cabinet parent) throws osid.OsidException {
        super (displayName, parent.getCabinetEntryAgent(), parent);
        
        //System.out.println ("LocalByteStore creator - name: " + displayName);

        //byte_store = new byte[1024]; // UNUSED
        file = new File (displayName);
        
        //System.out.println ("LocalByteStore creator - absolute path: " + file.getAbsolutePath());
        
        updateDisplayName (file.getName());

        if (tufts.vue.DEBUG.IO && Log.isDebugEnabled()) Log.debug("CREATED in " + parent + ": " + this);        
    }
    

// initialCapacity version not actually supported:

//     /**
//      *  Create a new ByteStore object given a display name, parent, and capacity.
//      *
//      *  @author Mark Norton
//      *
//      */
//      public LocalByteStore(String displayName, osid.filing.Cabinet parent, int initialCapacity) throws osid.OsidException {
//         super (displayName, parent.getCabinetEntryAgent(), parent);

//         //byte_store = new byte[initialCapacity]; // UNUSED
//         //file = new File (((LocalCabinet)parent).getPath(), displayName);
//         file = new File (displayName);
//         updateDisplayName (file.getName());

//         if (tufts.vue.DEBUG.IO && Log.isDebugEnabled()) Log.debug("CREATED in " + parent + ": " + this + "; w/capacity=" + initialCapacity);
//     }
    
    @Override
    public final boolean isCabinet() {
        return false;
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
        throw new osid.filing.FilingException (osid.filing.FilingException.UNIMPLEMENTED);
    }
    
    /**
     *  Get an interator over all digest algorithm types supported.  Currently unimplemented.
     *
     *  @author Mark Norton
     *
     *  @return A TypeIterator which lists all Digest Algorithm Types.
     */
    public osid.shared.TypeIterator getDigestAlgorithmTypes() throws osid.filing.FilingException {
         throw new osid.filing.FilingException (osid.filing.FilingException.UNIMPLEMENTED);
   }
    
    /**
     *  Get the MIME type of this byte store.
     *
     *  @author Mark Norton
     *
     *  @return The mime type of this byte store.
     */
    public String getMimeType() {
        return mime_type;
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
     *  Get the current length of the file associated with this byte store..
     *
     *  @author Mark Norton
     *
     *  @return The current length of this byte store.
     */
    public long length() throws osid.filing.FilingException {
        return file.length();
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
     *  Check to see if the mime type passed is valid.  If so, set the
     *  byte store mime type to it.  Return the current mime type, which
     *  may be the old one if new one isn't valid.
     *
     *  @author Mark Norton
     *
     *  @return The current mime type of this byte store.
      */
    public String updateMimeType(String mimeType) {

        mime_type = mimeType;
        return mime_type;
    }
    
     /**
     *  Return a URL string for this LocalByteStore.
     */
    public String getUrl() {
        try {
            return getFile().toURL().toString();
        } catch (Throwable t) {
            Log.debug("failed to create URL from file: " + getFile(), t);
            return getFile().toString();
        }
        //String fn = getFile().getAbsolutePath();
        //return"file://" + fn;
    }

    /**
     *  Rename the file corresponding to this byte store and update it's display
     *  name to the new name.
     *
     *  @author Mark Norton
     */
    
    /**
     *  Get the bytes in a file associated with this byte store.
     */
    public byte[] getBytes() throws osid.filing.FilingException {
        // Open the file for input access. 
        FileInputStream stream = null;
        try {
            stream = new FileInputStream (file);
        }
        catch (java.io.IOException ex1) {
            throw new osid.filing.FilingException (osid.filing.FilingException.IO_ERROR);
        }

        //  Allocate a buffer to hold the file.  Note that this must fit in memory and be
        //  small in size than Integer.MAX_VALUE.
        if (file.length() > (long) Integer.MAX_VALUE)
            throw new osid.filing.FilingException ("File is too big to read.");
        int len = (int) file.length();
        byte[] buf = new byte[len];

        //  Copy the file stream into a buffer.
        try {
            for (int i = 0; i < len; i++) {
                buf[i] = (byte)stream.read();
            }
            stream.close();
        }
        catch (java.io.IOException ex) {
            throw new osid.filing.FilingException (osid.filing.FilingException.IO_ERROR);
        }
        
        return buf;
    }
    
    /**
     *  Iterate over bytes given a version.  Version is ignored if null.
     *
     *  @author Mark Norton
     *
     *  @return A ByteValueIterator which lists all bytes saved in this byte store.
     */
    public osid.shared.ByteValueIterator read(java.util.Calendar version) throws osid.filing.FilingException {
        byte[] buf = getBytes();

        //  Create an iterator over the buffer.
        osid.shared.ByteValueIterator it = (osid.shared.ByteValueIterator) new ByteValueIterator(buf);
        return it;
    }
    
    /**
     *
     *  Replace the contents of the file associated with this ByteStore with the contents
     *  of the buffer provided.
     *
     *  @author Mark Norton
     */
    public void write(byte[] b) throws osid.filing.FilingException {
        // Open the file for output access. 
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream (file);
        }
        catch (java.io.IOException ex1) {
            throw new osid.filing.FilingException (osid.filing.FilingException.IO_ERROR);
        }
        
        //  Copy the buffer to the file stream.
        try {
            for (int i = 0; i < b.length; i++) {
                stream.write((int)b[i]);
            }
            stream.close();
        }
        catch (java.io.IOException ex) {
            throw new osid.filing.FilingException (osid.filing.FilingException.IO_ERROR);
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
    public void writeByte (int b) throws osid.filing.FilingException {
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
    public void writeBytesAtOffset (byte[] b, int off, int len) throws osid.filing.FilingException {
        throw new osid.filing.FilingException (osid.filing.FilingException.UNIMPLEMENTED);
    }

    /**
     *  Get the File associated with this LocalByteStore.
     *
     *  @author Mark Norton
     */
    public File getFile() {
        return file;
    }

    /**
     *  Rename the file corresponding to this byte store and update it's display
     *  name to the new name.
     *
     *  @author Mark Norton
     */
    public void rename (String absolute, String newName) throws osid.filing.FilingException {
        File dst = new File (absolute);
        file.renameTo (dst);
        updateDisplayName (newName);
    }
}
