/*
 * AgentIterator.java
 *
 * Created on October 22, 2003, 8:05 AM
 */

package tufts.oki.shared;
import java.util.*;

/**
 *  Implements a means to iterate over a list of Agent objects.
 *
 *  @author  Mark Norton
 */
public class AgentIterator implements osid.shared.AgentIterator {
    
    private Vector agent_vector = null;
    
    private int offset = 0;
    
    /** Creates a new instance of AgentIterator */
    public AgentIterator(Vector vector) {
        agent_vector = vector;
    }
    
    /**
     *  Determine if there is at least one more Agent in this iteration list.
     *
     *  @author Mark Norton
     *
     *  @return True if there a next agent in the iterator.
     */
    public boolean hasNext() {
        return (offset < agent_vector.size());
    }
    
    /**
     *  Get the next Agent in this iteration list.
     *
     *  @author Mark Norton
     *
     *  @return The next agent in the iteration list.
     */
    public osid.shared.Agent next() {
        osid.shared.Agent agent = (osid.shared.Agent) agent_vector.elementAt(offset);
        offset++;
        return agent;
    }
    
}
