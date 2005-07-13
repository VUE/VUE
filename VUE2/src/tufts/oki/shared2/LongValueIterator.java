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
 * CalendarIterator.java
 *
 * Created on September 19, 2003, 2:42 PM
 */

package tufts.oki.shared2;
import java.util.*;

/**
 *
 * @author  Mark Norton
 *
 *  @description
 *  CalendarIterator is fully implemented.
 */
public class LongValueIterator implements org.osid.shared.LongValueIterator {
    
    private Vector calendar_vector = null;
    
    private int offset = 0;
    
    /** Creates a new instance of CalendarIterator */
    public LongValueIterator(Vector vector) {
        //assert (vector != null) : "Vector passed is null.";

        calendar_vector = vector;
    }
    
    /**
     *  @author Mark Norton
     *
     *  @return True if there is another Calender in the iterator list.
     */
    public boolean hasNextLongValue() {
        return (offset < calendar_vector.size());
    }
    
    /**
     *  @author Mark Norton
     *
     *  @return The next calendar object in the iteration list.
     */
    public long nextLongValue() {
        java.util.Calendar calendar = (java.util.Calendar) calendar_vector.elementAt(offset);
        offset++;
        return calendar.getTimeInMillis();
    }
    
}
