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
 * Agent.java
 *
 * Created on October 21, 2003, 5:31 PM
 */

package tufts.oki.agent;
import org.osid.*;
import java.util.*;
import java.io.*;


/**
 *  The Agent object combines an identifier, display name and type to create an object
 *  which can be used to represent people, systems, etc.  Also associated with each agent
 *  is a set of properties.  In order to group properties by type, a vector of osid.Properties
 *  is maintained by the Agent.
 *
 *  @author  Mark Norton
 *
 *  @see Properties
 */
public class Agent implements org.osid.agent.Agent{
    private org.osid.shared.Id ag_id = null;                        //  The unique ID of this Agent.
    private String ag_name = "unknown";             //  This display name of this Agent.
    private org.osid.shared.Type ag_type;               //  The type of this Agent.
    private Vector ag_props = null;                 //  A list of properties for this agent.
    
    /**
     *  A new instance of Agent.  The Properties list is initially empty.
     *
     *  @author Mark Norton
     */
    public Agent(String display_name, org.osid.shared.Type type) throws org.osid.agent.AgentException {
        try
		{
			ag_id = new tufts.oki.id.Id();
			ag_name = display_name;
			ag_type = type;
			ag_props = new Vector(100);   // List will expand automatically if needed.
		}
		catch (Throwable t)
		{	
			throw new org.osid.agent.AgentException(t.getMessage());
		}
	}
    
    /**
     *  Get the display name for this agent.
     *
     *  @author Mark Norton
     *
     *  @return The display name of this agent.
     **/
    public String getDisplayName() {
        return ag_name;
    }
    
    /**
     *  Get the Id for this agent.
     *
     *  @author Mark Norton
     *
     *  @return  The unique ID object for this Agent.
     */
    public org.osid.shared.Id getId() {
        return (org.osid.shared.Id) ag_id;
    }
    
    /**
     *  Get the type of this agent.
     *
     *  @author Mark Norton
     *
     *  @return The OSID Type associated with this Agent.
     */
    public org.osid.shared.Type getType() {
         return ag_type;
    }
    
    /**
     *  Get a list of properties objects by interating over the internal list.
     *
     *  @see Properties
     *
     *  @author Mark Norton
     *
     *  @return A PropertiesIterator which iterates over the list of Properties
     *  associated with this Agent.  Each Properties will have its own, unique Type.
     */
    public org.osid.shared.PropertiesIterator getProperties() {
         return new tufts.oki.shared2.PropertiesIterator (ag_props);
    }
    
    /**
     *  Builds a list of all types in the properties list and creates an interator to 
     *  walk over them.
     *
     *  @author Mark Norton
     *
     *  @return Returns a TypeIterator that iterates over all of the Types represented in
     *      the collection of Properties maintained by this Agent.
     */
    public org.osid.shared.TypeIterator getPropertyTypes() throws org.osid.agent.AgentException {
        Vector type_list = new Vector (100);
         
        try
		{
			for (int i = 0; i < ag_props.size(); i++) {
				tufts.oki.shared2.Properties prop = (tufts.oki.shared2.Properties) ag_props.elementAt (i);
				type_list.addElement (prop.getType());
			}
		}
		catch (Throwable t)
		{
			throw new org.osid.agent.AgentException(t.getMessage());
		}

        return new tufts.oki.shared2.TypeIterator (type_list);
    }
    
    /**
     *  Get the Properties object associated with the type passed.  An UNKNOWN_TYPE 
     *  exception is thrown if there are not properities of the type passed.
     *  <p>
     *  This method was introduced in rc6.1.
     *
     *  @author Mark Norton
     *
     *  @return The Properties object associated with type.
     *
     */
    public org.osid.shared.Properties getPropertiesByType(org.osid.shared.Type propertiesType) throws org.osid.agent.AgentException {
        try
		{
			for (int i = 0; i < ag_props.size(); i++) {
				org.osid.shared.Properties prop = (org.osid.shared.Properties) ag_props.elementAt (i);
				if ((prop.getType()).isEqual(propertiesType))
					return prop;
			}
		}
		catch (Throwable t)
		{
			throw new org.osid.agent.AgentException(t.getMessage());
		}
        throw new org.osid.agent.AgentException (org.osid.shared.SharedException.UNKNOWN_TYPE);
    }
    
    /*  Extensions to org.osid.shared.Agent  */
    /*  -------------------------------  */
    
    /**
     *  Add the properties object passed to the internal list of properties associated
     *  with this agent.
     *  <p>
     *  This is an extension to org.osid.shared.Agent.
     *  
     *  @author Mark Norton
     */
    public void addProperties (org.osid.shared.Properties prop) {
        ag_props.add (prop);
    }
    
}
