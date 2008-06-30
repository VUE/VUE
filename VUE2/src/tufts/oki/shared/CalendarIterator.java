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
 * CalendarIterator.java
 *
 * Created on September 19, 2003, 2:42 PM
 */

package tufts.oki.shared;
import java.util.*;

/**
 *
 * @author  Mark Norton
 *
 *  @description
 *  CalendarIterator is fully implemented.
 */
public class CalendarIterator implements osid.shared.CalendarIterator {
    
    private Vector calendar_vector = null;
    
    private int offset = 0;
    
    /** Creates a new instance of CalendarIterator */
    public CalendarIterator(Vector vector) {
        //assert (vector != null) : "Vector passed is null.";

        calendar_vector = vector;
    }
    
    /**
     *  @author Mark Norton
     *
     *  @return True if there is another Calender in the iterator list.
     */
    public boolean hasNext() {
        return (offset < calendar_vector.size());
    }
    
    /**
     *  @author Mark Norton
     *
     *  @return The next calendar object in the iteration list.
     */
    public java.util.Calendar next() {
        java.util.Calendar calendar = (java.util.Calendar) calendar_vector.elementAt(offset);
        offset++;
        return calendar;
    }
    
}
