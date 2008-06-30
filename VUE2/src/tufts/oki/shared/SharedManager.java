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
 * SharedManager.java
 *
 * Created on October 22, 2003, 8:30 AM
 */

package tufts.oki.shared;
import tufts.oki.*;
import java.util.*;
import java.io.*;
import osid.shared.SharedException;

/**
 *  The Shared Manager allows creation and manipulation of Id, Agent, and Group objects.
 *  The manager maintains a list for all Ids, Agents, and Groups created.
 *  <p>
 *  Support is provided for persistance via readState() and writeState().  Note that the
 *  persistance is done using serialization to a file, which is currently hard coded as
 *  SERIALZIED_FILE_NAME.  This either needs to be edited for the final installation, or
 *  a better method of naming the file provided.
 *
 *  @author  Mark Norton
 */
public class SharedManager extends OsidManager implements osid.shared.SharedManager {
    public static final String SERIALIZED_FILE_NAME = "c:/java/serialized/osid_shared_manager.sid";
    private Vector ids = null;
    private Vector agents = null;
    private Vector groups = null;
    
    //  This private flag is used to keep track of the persistance state.
    private boolean restored = false;
    
    /** 
     *  Creates a new instance of SharedManager with no owner.
     *
     *  @author Mark Norton
     */
    public SharedManager() {
        super();
        ids = new Vector(100);
        agents = new Vector(100);
        groups = new Vector(100);
    }
    
    /**
     *  Adds the Id to the internal Id list.
     *
     *  @author Mark Norton
     *
     *  @return An Id with a new unique identifier.
     */
    public osid.shared.Id createId() throws osid.shared.SharedException {
        Id id = new Id ();      //  Allocate a new Id object.
        ids.add (id);           //  Add it to the SharedManager ids list.
        return id;
    }

    /**
     *  Currently uses a linear search through the internal ids list for an Id which
     *  matches the string form given.  This could be made much faster by using a
     *  HashMap instead of a Vector.  This is simple enough to change later, though,
     *  without impacting any clients of these methods.
     *  <p>
     *  Note also that this method throws UNKWOWN_ID if the Id cannot be found.  There
     *  is still some debate as to throwing exceptions vs. returning null.
     *
     *  @author Mark Norton
     *
     *  @return An ID corrsponding to the string given.
     */
    public osid.shared.Id getId(String idString) throws osid.shared.SharedException {
        for (int i=0; i < ids.size(); i++) {
            Id id = (Id) ids.elementAt (i);
            if (id.getIdString() == idString)
                return id;
        }
        throw new osid.shared.SharedException (osid.shared.SharedException.UNKNOWN_ID);
    }
    
    /**
     *  Adds the new Agent to the internal agents list.
     *
     *  @author Mark Norton
     *
     *  @return An Agent with the name and type provided.
     */
    public osid.shared.Agent createAgent(String name, osid.shared.Type agentType) throws osid.shared.SharedException {
        Agent agent = new Agent (name, agentType);
        agents.add (agent);     //  Add it to the SharedManager agents list.
        return agent;          
    }
    
    /**
     *  Adds the Group to the internal groups list.
     *
     *  @author Mark Norton
     *
     *  @return A Group with the Id provided.
     */
    public osid.shared.Group createGroup(String name, osid.shared.Type groupType, String description) throws osid.shared.SharedException {
        Group group = new Group (name, groupType, description);
        groups.add (group);     //  Add it to the SharedManager agents list.
        return group;          
    }
    
    /**
     *  Delete the agent given by the Id passed.
     *
     *  @author Mark Norton
     */
    public void deleteAgent(osid.shared.Id id) throws osid.shared.SharedException {
         osid.shared.Agent ag = this.getAgent (id);
         agents.remove(ag);
    }
    
    /**
     *  Delete the group given by the Id passed.  Note that this implementation
     *  currently checks to see if a group is empty before deleting it, raising an
     *  exception if non-empty.  There has been some discussion on SourceForge to 
     *  indidate that a group should be empty before deleting it.
     *
     *  @author Mark Norton
     */
    public void deleteGroup(osid.shared.Id id) throws osid.shared.SharedException {
        tufts.oki.shared.Group gp = (tufts.oki.shared.Group)this.getGroup (id);
        if (!gp.isEmpty()) {
            throw new osid.shared.SharedException ("Group is not empty.");
        }
        
        groups.remove(gp);
    }
    
    /**
     *  Get the agent who's Id is equal to the one passed.
     *  Note that if there is no agent with this id, an UNKNOWN_ID exception is thrown.
     *
     *  @author Mark Norton
     *
     *  @return An agent who's id is equal to the one passed.
     */
    public osid.shared.Agent getAgent(osid.shared.Id id) throws osid.shared.SharedException {
        for (int i=0; i < ids.size(); i++) {
            Agent agent = (Agent) agents.elementAt (i);
            if ((agent.getId()).isEqual (id))
                return agent;
        }
        throw new osid.shared.SharedException (osid.shared.SharedException.UNKNOWN_ID);
    }
    
    /**
     *  Get the agent who's display name is equal to the one passed.
     *  Note that if there is no agent with this id, an UNKNOWN_ID exception is thrown.
     *
     *  @author Mark Norton
     *
     *  @return An agent who's display name is equal to the one passed.
     */
    public osid.shared.Agent getAgent(String name) throws osid.shared.SharedException {
        for (int i=0; i < agents.size(); i++) {
            Agent agent = (Agent) agents.elementAt (i);
            if (agent.getDisplayName().compareTo(name) == 0)
                return agent;
        }
        throw new osid.shared.SharedException ("Unknown Name");
    }
    
    /**
     *  Returns a interator which lists all known agent types.
     *  <p>
     *  This needs to be re-written as a pre-built list of supported Agent Types.
     *  Searching for types in the agents list only returns types that have been
     *  used, which is not the same as the list of supported agent types.
     *
     *  @author Mark Norton
     *
     *  @return An iterator which walks a list of unique agent types.
     */
    public osid.shared.TypeIterator getAgentTypes() throws osid.shared.SharedException {  
        /*  Iterate over all agents, extract type, and add to a HashSet.  */
        HashSet type_set = new HashSet();
        for (int i=0; i < ids.size(); i++) {
            Agent agent = (Agent) agents.elementAt (i);
            osid.shared.Type type = agent.getType();
            type_set.add(type);
        }
        
        /*  Create  type vector from the type set.  */
        Vector type_vector = new Vector (type_set.size());
        type_vector.addAll (type_set);
        
        /*  Create and return a TypeIterator for the list of unique types.  */
        TypeIterator it = new TypeIterator(type_vector);
        return it;
    }
    
    /**
     *  Get a list of all agents.
     *
     *  @author Mark Norton
     *
     *  @return An AgentIterator which will interate over all known agents.
     */
    public osid.shared.AgentIterator getAgents() {
        AgentIterator it = new AgentIterator (agents);
        return it;
    }
    
    /**
     *  Get a list of all agents of a given type.
     *  <p>
     *  This method was introduced in rc6.1.
     *
     *  @author Mark Norton
     *
     *  @return An AgentIterator which lists out all agents of the type given.
     */
    public osid.shared.AgentIterator getAgentsByType(osid.shared.Type agentType) throws osid.shared.SharedException {
        /*  Iterate over all agents, extract type, and add to a HashSet.  */
        HashSet agent_set = new HashSet();
        for (int i=0; i < ids.size(); i++) {
            Agent agent = (Agent) agents.elementAt (i);
            osid.shared.Type type = agent.getType();
            if (type.isEqual(agentType))
                agent_set.add(agent);
        }
        
        /*  Create  type vector from the agent set.  */
        Vector agent_vector = new Vector (agent_set.size());
        agent_vector.addAll (agent_set);
        
        /*  Create and return an AgentIterator for the list of agents of this type.  */
        AgentIterator it = new AgentIterator(agent_vector);
        return it;
    }
    
     /**
      *  Get the group assoicated with the Id given.
      *  Note that if there is no group with this id, an UNKNOWN_ID exception is thrown.
      *  @author Mark Norton
      *
      *  @return A group who's id is equal to the one passed.
      */
    public osid.shared.Group getGroup(osid.shared.Id id) throws osid.shared.SharedException {
        for (int i=0; i < ids.size(); i++) {
            Group group = (Group) groups.elementAt (i);
            if ((group.getId()).isEqual (id))
                return group;
        }
        throw new osid.shared.SharedException (osid.shared.SharedException.UNKNOWN_ID);
    }
    
    /**
     *  Get a list of group types.
     *  <p>
     *  Again, this should be done from a list of supported types, not currently used
     *  grouptypes.
     *
     *  @author Mark Norton
     *
     *  @return An iterator which walks a list of unique group types.
     */
   public osid.shared.TypeIterator getGroupTypes() throws osid.shared.SharedException {
        /*  Iterate over all agents, extract type, and add to a HashSet.  */
        HashSet type_set = new HashSet();
        for (int i=0; i < ids.size(); i++) {
            Group group = (Group) groups.elementAt (i);
            osid.shared.Type type = group.getType();
            type_set.add(type);
        }
        
        /*  Create  type vector from the type set.  */
        Vector type_vector = new Vector (type_set.size());
        type_vector.addAll (type_set);
        
        /*  Create and return a TypeIterator for the list of unique types.  */
        TypeIterator it = new TypeIterator(type_vector);
        return it;
    }
    
    /**
     *  Get a list of groups.
     *
     *  @author Mark Norton
     *
     *  @return An AgentIterator which will interate over all known groups.
     */
   public osid.shared.AgentIterator getGroups() throws osid.shared.SharedException {
        AgentIterator it = new AgentIterator (groups);
        return it;
    }
    
    /**
     *  Get a list of all groups of a given group type.
     *
     *  @author Mark Norton
     *
     *  @return An AgentIterator which lists out all groups of the type given.
     */
    public osid.shared.AgentIterator getGroupsByType(osid.shared.Type groupType) throws osid.shared.SharedException {
        /*  Iterate over all agents, extract type, and add to a HashSet.  */
        HashSet group_set = new HashSet();
        for (int i=0; i < ids.size(); i++) {
            Group group = (Group) groups.elementAt (i);
            osid.shared.Type type = group.getType();
            if (type.isEqual(groupType))
                group_set.add(group);
        }
        
        /*  Create  type vector from the agent set.  */
        Vector group_vector = new Vector (group_set.size());
        group_vector.addAll (group_set);
        
        /*  Create and return an AgentIterator for the list of agents of this type.  */
        AgentIterator it = new AgentIterator(group_vector);
        return it;    
    }

    /**
     *  Write out the state of the SharedManager.  Since the various OSID managers are
     *  loaded on demand by OsidLoader, persistence of data has to happen in the various
     *  managers after they have been loaded by OsidLoader.  This method marshalls the
     *  id, agent, and group lists into a file called "osid_shared_manager.ser".
     *  <p>
     *  While persistence of agents and groups seems useful, it is not clear that
     *  persisting Ids is.  Generally, I would expect Ids to be part of other objects
     *  like Agent or Type and be persisted by those objects if needed.  As such, the
     *  persistence of Ids is commented out below.  If desired, uncomment it.
     *
     *  Note that this method is not defined by the osid.SharedManager interface.
     *
     *  @author Mark Norton
     */
    public void writeState() throws osid.shared.SharedException {
        try {
            FileOutputStream fout = new FileOutputStream (tufts.oki.shared.SharedManager.SERIALIZED_FILE_NAME);
            ObjectOutputStream  oout = new ObjectOutputStream (fout);
            oout.writeObject (agents);
            oout.writeObject (groups);
            // oout.writeObject (ids);
            oout.close();
        }
        catch (java.io.IOException ex) {
            throw new osid.shared.SharedException ("I/O error on writing state data out of SharedManager.");
        }
    }
    
    /**
     *  Read in the state of the SharedManager.  Since the various OSID managers are
     *  loaded on demand by OsidLoader, persistence of data has to happen in the various
     *  managers after they have been loaded by OsidLoader.  This method un-marshalls the
     *  id, agent, and group lists from a file called "osid_shared_manager.ser".  This
     *  method should be called immediately after loaded by OsidLoader.  An attempt to
     *  readState() to a modified SharedManager will result in an exception.
     *  <p>
     *  While persistence of agents and groups seems useful, it is not clear that
     *  persisting Ids is.  Generally, I would expect Ids to be part of other objects
     *  like Agent or Type and be persisted by those objects if needed.  As such, the
     *  persistence of Ids is commented out below.  If desired, uncomment it.
     *
     *  Note that this method is not defined by the osid.SharedManager interface.
     *
     *  @author Mark Norton
     */
    public void readState() throws osid.shared.SharedException {
        if (this.isEmpty()) {
            //  Open up the serilization file, if it exists.
            FileInputStream fin = null;
            try {
                fin = new FileInputStream (tufts.oki.shared.SharedManager.SERIALIZED_FILE_NAME);
            }
            catch (java.io.IOException exio) {
                //  This error is most likely to be a file not found, which means
                //  that there is not state to load.  Just return.
                restored = true;
                return;
            }

            try {
                //  Create an object stream from file stream.
                ObjectInputStream oin = new ObjectInputStream (fin);

                //  Read the persistent data and cast it to objects.
                agents = (Vector)oin.readObject();
                groups = (Vector)oin.readObject();
                // ids = (Vector)oin.readObject();
                
                // Close in input stream.
                oin.close();
                restored = true;
            }
            catch (java.io.IOException ex2) {
                throw new osid.shared.SharedException ("SharedManager exception: " + ex2.getMessage());
            }
            catch (java.lang.ClassNotFoundException ex4) {
                throw new osid.shared.SharedException ("Class not found error on reading state data into SharedManager.");
            }
        }
        else
            throw new osid.shared.SharedException ("Attempt to read state into an modified SharedManager.");
    }
    
    /**
     *  Check to see if the SharedManager is emtpy, meaning that no agents or groups are
     *  currently defined.  This is largely diagnostic in nature, thought it is used by 
     *  the persisence mechanism implemented by writeState() and readState().
     *  <p>
     *  Note that this method is an extension to the OKI SharedManager interface.
     *
     *  @author Mark Norton
     */
    public boolean isEmpty() {
        return (agents.isEmpty() && groups.isEmpty());
    }

    /**
     *  Use this method to see if the state for the SharedManager has been restored
     *  by readState().
     *  <p>
     *  Note that this method is an extension to the OKI SharedManager interface.
     *
     *  @author Mark Norton
     *
     *  @return True if state data has been restored.
     */
    public boolean isRestored() {
        return (restored);
    }    
}
