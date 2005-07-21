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
 * AgentIterator.java
 *
 * Created on October 22, 2003, 8:05 AM
 */

package tufts.oki.shared2;
import java.util.*;

/**
 *  Implements a means to iterate over a list of Agent objects.
 *
 *  @author  Mark Norton
 */
public class AgentIterator implements org.osid.shared2.AgentIterator {
    
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
    public boolean hasNextAgent() {
        return (offset < agent_vector.size());
    }
    
    /**
     *  Get the next Agent in this iteration list.
     *
     *  @author Mark Norton
     *
     *  @return The next agent in the iteration list.
     */
    public osid.shared.Agent nextAgent() {
        org.osid.shared.Agent agent = (org.osid.shared.Agent) agent_vector.elementAt(offset);
        offset++;
        return agent;
    }
    
}
