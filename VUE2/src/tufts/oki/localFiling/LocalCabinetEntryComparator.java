/*
 * LocalCabinetEntryComparator.java
 *
 * Created on May 24, 2007, 12:58 PM
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
 * <p>The entire file consists of original code.  Copyright &copy; 2003-2007
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

/**
 *
 * @author akumar03
 */

package tufts.oki.localFiling;

import java.util.*;

public class LocalCabinetEntryComparator  implements Comparator{
    
    /** Creates a new instance of LocalCabinetEntryComparator */
    public LocalCabinetEntryComparator() {
    }
    public int compare(Object o1,Object o2) {
        if(o1 instanceof LocalCabinetEntry && o2 instanceof LocalCabinetEntry) {
            try {
                return ((LocalCabinetEntry) o1).getDisplayName().compareToIgnoreCase(((LocalCabinetEntry) o2).getDisplayName());
            } catch(Exception ex) {
                ex.printStackTrace();
                return -1;
            }
        } else {
            return o1.toString().compareToIgnoreCase(o2.toString());
        }
    }
}
