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
 * AgentPersonType.java
 *
 * Created on September 22, 2003, 2:57 PM
 */

package tufts.oki.agent;

import tufts.oki.*;
/**
 *  This is a person agent type.
 *
 *  @author  Mark Norton
 */
public class AgentPersonType extends org.osid.shared.Type {
    /* Use this keyword to search for the person type.  */
    public static final String AGENT_PERSON_TYPE_KEY = "org.osid.agent.Agent.Person";
    
    /** Creates a new instance of AgentPersonType */
    public AgentPersonType() {
        super ("org.osid.agent", OsidManager.AUTHORITY, AGENT_PERSON_TYPE_KEY, "This is a person agent as implemented by org.osid.agent.Agent.");
    }
    
}
