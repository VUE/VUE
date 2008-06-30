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
 * VueAgentType.java
 *
 * Created on December 22, 2003, 2:16 PM
 */

package tufts.oki.shared;
import java.util.*;

/**
 *  This class serves as a generic type for VUE agent types.  All
 *  keynames should be unique.  It is recommended that agent type keynames be created
 *  by concatenating a specific function type to the AGENT_TYPE_KEY provided here.<br>
 *  <br>
 *  For example:  VueAgentType.AGENT_TYPE_KEY + ".Tufts.Student"
 *
 * @author  Mark Norton
 */
public class VueAgentType extends osid.shared.Type {
     public static final String AGENT_TYPE_KEY = "osid.shared.Agent";
     public static Vector agentTypes = null;
    
    /** Creates a new instance of VueAgentType given a keyname. */
    public VueAgentType(String keyname) {
        super ("osid.shared", tufts.oki.OsidManager.AUTHORITY, keyname);
        
        //  Initialize the global list of function types and add the new one.
        if (agentTypes == null)
            agentTypes = new Vector(100);
        agentTypes.add (keyname);
    }
    
    /** Creates a new instance of VueAgentType given a keyname and description.  */
    public VueAgentType(String keyname, String description) {
        super ("osid.shared", tufts.oki.OsidManager.AUTHORITY, keyname, description);
        
        //  Initialize the global list of function types and add the new one.
        if (agentTypes == null)
            agentTypes = new Vector(100);
        agentTypes.add (keyname);
    }
}
