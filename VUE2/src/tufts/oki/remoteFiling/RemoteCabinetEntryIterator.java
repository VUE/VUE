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

package tufts.oki.remoteFiling;
import java.util.*;

/**
 *  The CabinetEntryIterator provides a way to list all entries in a given cabinet entry.
 *
 * @author  Mark Norton
 *
 *  CabinetEntryIterator is fully implemented.
 */
public class RemoteCabinetEntryIterator implements osid.filing.CabinetEntryIterator{
    
    private Vector cabinet_vector = null;
    
    private int offset = 0;
    
    /** Creates a new instance of CabinetEntryIterator  given a vector of CabinetEntry.  */
    public RemoteCabinetEntryIterator(Vector vect) {
       // assert (vect != null) : "Cabinet entry vector passed is null.";

        cabinet_vector = vect;
    }
    
    /**  Check to see if there is at least one more entry.  */
    public boolean hasNext() throws osid.filing.FilingException {
        return (offset < cabinet_vector.size());
    }
    
    /**  Get the next entry and increment offset.  */
    public osid.filing.CabinetEntry next() throws osid.filing.FilingException {
        osid.filing.CabinetEntry ce = (osid.filing.CabinetEntry) cabinet_vector.elementAt(offset);
        offset++;
        return ce;
    }
    
}
