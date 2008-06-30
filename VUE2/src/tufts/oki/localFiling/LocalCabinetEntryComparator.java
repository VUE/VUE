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
