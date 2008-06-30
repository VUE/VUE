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
 * @author  Scott Fraize
 *
 *  CabinetEntryIterator is fully implemented.
 */
public class LocalCabinetEntryIterator implements osid.filing.CabinetEntryIterator{
    
    private final osid.filing.CabinetEntry[] sorted;
    private int offset = 0;
    
    /** Creates a new instance of CabinetEntryIterator  given a vector of CabinetEntry.  */
    public LocalCabinetEntryIterator(SortedSet vect) {
        // Slow, but thread-safe:
        sorted = (osid.filing.CabinetEntry[]) vect.toArray(new osid.filing.CabinetEntry[vect.size()]);
    }
    
    /**  Check to see if there is at least one more entry.  */
    public boolean hasNext() {
        return offset < sorted.length;
    }
    
    /**  Get the next entry and increment offset.  */
    public osid.filing.CabinetEntry next() {
        return sorted[offset++];
    }
    
}
