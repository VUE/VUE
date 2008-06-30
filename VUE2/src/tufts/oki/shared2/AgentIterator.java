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
public class AgentIterator implements org.osid.agent.AgentIterator {
    
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
    public org.osid.agent.Agent nextAgent() {
        org.osid.agent.Agent agent = (org.osid.agent.Agent) agent_vector.elementAt(offset);
        offset++;
        return agent;
    }
    
}
