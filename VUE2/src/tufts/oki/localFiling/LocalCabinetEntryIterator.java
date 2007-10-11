/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

/*
 * CabinetEntryIterator.java
 *
 * Created on September 17, 2003, 1:05 PM
 *
 *  The software contained in this file is copyright 2003 by Mark J. Norton, all rights reserved.
 */

package tufts.oki.localFiling;
import java.util.*;

/**
 *  The CabinetEntryIterator provides a way to list all entries in a given cabinet entry.
 *
 * @author  Mark Norton
 *
 *  CabinetEntryIterator is fully implemented.
 */
public class LocalCabinetEntryIterator implements osid.filing.CabinetEntryIterator{
    
    //private SortedSet cabinet_vector = null;
    private final osid.filing.CabinetEntry[] sorted;
    private int offset = 0;
    
    /** Creates a new instance of CabinetEntryIterator  given a vector of CabinetEntry.  */
    public LocalCabinetEntryIterator(SortedSet vect) {
        sorted = (osid.filing.CabinetEntry[]) vect.toArray(new osid.filing.CabinetEntry[vect.size()]);
        //cabinet_vector = vect;
    }
    
    /**  Check to see if there is at least one more entry.  */
    public boolean hasNext() {
        return offset < sorted.length;
        //return (offset < cabinet_vector.size());
    }
    
    /**  Get the next entry and increment offset.  */
    public osid.filing.CabinetEntry next() {
        return sorted[offset++];
        
        // this is a highly inefficient impl, and could allow the iterator
        // to produce incoherent results if the vector changes during iteration -- SMF 2007-10-10
        // osid.filing.CabinetEntry ce = (osid.filing.CabinetEntry) cabinet_vector.toArray()[offset];
        //offset++;
        //return ce;

    }
    
}
