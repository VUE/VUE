/*
 * CalendarIterator.java
 *
 * Created on September 19, 2003, 2:42 PM
 */

package src.tufts.shared;
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
        assert (vector != null) : "Vector passed is null.";

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
