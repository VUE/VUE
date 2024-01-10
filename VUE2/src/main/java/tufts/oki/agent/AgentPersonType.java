/*
* Copyright 2003-2010 Tufts University  Licensed under the
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
