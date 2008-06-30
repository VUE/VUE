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
 * Group.java
 *
 * Created on Oct. 22, 2003, 8:15 AM
 */

package tufts.oki.shared;
import java.util.*;
import osid.shared.SharedException;

/**
 *  The Groups class defines a collection of agents.  Groups may also have sub-groups,
 *  thus creating a heirarchy of groups.  Groups are an extention of Agent, which means
 *  that a group has an Id, type and properties associated with it.
 *
 * @author  Mark Norton
 */
public class Group extends Agent implements osid.shared.Group {
    
    //private String display_name = null;   // Inherited from Agent.
    //private osid.shared.Id id = null;     // Inherited from Agent.    
    //private osid.shared.Type type = null; // Inherited from Agent.
    
    private String group_description = null;    //  Description of this group.
    private Vector agents = null;               //  Agents included in this group.
    private Vector subgroups = null;            //  Subgroups of this group.
    
    
    /** 
     *  Creates a new instance of Group 
     *
     *  @author Mark Norton
     */
    public Group(String display_name, osid.shared.Type type, String description) throws SharedException {
        super(display_name, type);
        group_description = description;
        agents = new Vector(100);
        subgroups = new Vector(100);
    }

    /**
     *  Add an agent or group to this group.
     *  <p>
     *  This method is an extention to osid.shared.Group.
     *
     *  @author Mark Norton
     */
    public void add(osid.shared.Agent memberOrGroup) {
        if (memberOrGroup instanceof osid.shared.Group) {
            subgroups.add (memberOrGroup);
        }
        else {
            agents.add (memberOrGroup);
        }
    }
    
    /**
     *  Determine if an agent is part of this group.  If searchSubGroups is true, a
     *  recursive search of all subgroups is performed.
     *
     *  @author  Mark Norton
     *
     *  @return true if the agent is included in this group or subgroups.
     */
    public boolean contains(osid.shared.Agent memberOrGroup, boolean searchSubgroups) throws SharedException {
        if (agents.contains(memberOrGroup))
            return true;
        else if (searchSubgroups) {
            for (int i = 0; i < subgroups.size(); i++) {
                osid.shared.Group sg = (osid.shared.Group) subgroups.elementAt(i);
                return sg.contains (memberOrGroup, searchSubgroups);
            }
        }
        else
            return subgroups.contains (memberOrGroup);
        return false;
    }
    
    /**
     *  Get the description string for this group.
     *
     *  @author Mark Norton
     *
     *  @return String description of this group.
     */
    public String getDescription() {
        return group_description;
    }
    
    /**
     *  Get the display name for this group.
     *
     *  @author Mark Norton
     *
     *  @return the display name for this group.
     */
    public String getDisplayName() {
        return super.getDisplayName();
    }
    
    /*  makeGroupsVedctor
     * 
     *  This private method adds all of the groups in this group to a Vector
     *  passed in.  This method is only called if subgroups are to be included, 
     *  so the method recurses (calls itself if there are subgroups).
     */
    private void makeGroupsVector (Vector master, boolean includeSubgroups) {
        for (int i=0; i < subgroups.size(); i++) {
            Group sub = (Group) subgroups.elementAt(i);
            master.add (sub);
            sub.makeGroupsVector (master, includeSubgroups);
        }
    }
    
    /**
     *  Returns an AgentIterator which will give back all groups in this group.
     *  If includeSubgroups is true, groups in sub-groups are included.  This
     *  method uses the private makeGroupVector() method to create a vector 
     *  holding all subgroups.  Note that it is possible that groups may be
     *  duplicated in sub-groups.  No effort is made to ensure uniqueness.
     *
     *  @author Mark Norton
     *
     *  @return an AgentIterator for all groups in this group.
     */
    public osid.shared.AgentIterator getGroups(boolean includeSubgroups) {
        Vector temp = new Vector(100);
        for (int i=0; i < subgroups.size(); i++) {
            Group sub = (Group) subgroups.elementAt(i);
            temp.add (sub);
            if (includeSubgroups)
                sub.makeGroupsVector (temp, includeSubgroups);
        }
        AgentIterator it = new AgentIterator(temp);
        return it;
    }
    
    /*  makeGroupsWithAgentVedctor
    * 
    *  This private method adds all groups containing the indicated agent to a Vector
    *  passed in. 
    */
    private void makeGroupsWithAgentVector (osid.shared.Agent member, Vector master) throws SharedException {
        for (int i=0; i < subgroups.size(); i++) {
            Group sub = (Group) subgroups.elementAt(i);
            if (sub.contains (member, true))
                master.add (sub);
            sub.makeGroupsWithAgentVector (member, master);
        }
    }

    /**
     *  Returns an AgentIterator which returns all grouips containing the agent
     *  passed.  This makes use of the private method makeGroupsWithAgentVector()
     *  to recursively search all subgroups containing this agent.
     *
     *  @author Mark Norton
     */
     public osid.shared.AgentIterator getGroupsContainingMember(osid.shared.Agent member) throws SharedException {
        Vector temp = new Vector(100);
        for (int i=0; i < subgroups.size(); i++) {
            Group sub = (Group) subgroups.elementAt(i);
            if (sub.contains (member, true))
                temp.add (sub);
            sub.makeGroupsWithAgentVector (member, temp);
        }
       AgentIterator it = new AgentIterator(temp);
       return it;
    }
    
    /**
     *  Get the Id of this group.
     *
     *  @author Mark Norton
     *
     *  @return The unique Id of this group.
     */
    public osid.shared.Id getId() {
        return super.getId();
    }
    
    /*  makeMembersVedctor
     * 
     *  This private method adds all of the agents in this group to a Vector
     *  passed in.  If includesSubgroups is true, then the method recurses
     *  (calls itself) to add all agents in all subgroups.
     */
    private void makeMembersVector (Vector master, boolean includeSubgroups) {
        master.addAll (agents);
        if (includeSubgroups) {
            for (int i=0; i < subgroups.size(); i++) {
                Group sub = (Group) subgroups.elementAt(i);
                sub.makeMembersVector (master, includeSubgroups);
            }
        }
    }
    
    /**
     *  Returns an AgentIterator which will give back all agents in this group.
     *  If includeSubgroups is true, agents in sub-groups are included.  This
     *  method uses the private makeMembersVector() method to create a vector 
     *  holding all agents.  Note that it is possible that agents may be
     *  duplicated in sub-groups.  No effort is made to ensure uniqueness.
     *
     *  @author Mark Norton
     */
    public osid.shared.AgentIterator getMembers(boolean includeSubgroups) {
        Vector temp = (Vector) agents.clone();
        AgentIterator it = new AgentIterator(temp);
        return it;
    }
    
    /**
     *  Get the group type.
     *
     *  @author Mark Norton
     *
     *  @return the group type.
     */
    public osid.shared.Type getType() {
        return super.getType();
    }
    
    /**
     *  Remove the member or group passed from the list og agents or
     *  groups in this group as appropriate.
     *
     *  @author Mark Norton
     */
    public void remove(osid.shared.Agent memberOrGroup) {
        if (memberOrGroup instanceof Group)
            subgroups.remove (memberOrGroup);
        else
            agents.remove (memberOrGroup);
    }
    
    /**
     *  Update the description of this group with the new description provided.
     *
     *  @author Mark Norton
     */
    public void updateDescription(String description) {
        group_description = description;
    }
    
    /**
     *  Check to see if this group is emtpy, meaning that it contains no agents or sub-groups.
     *  Note that this method is not defined by the osid.shared.Group interface defintiion.
     *
     *  @author Mark Norton
     *
     *  @return True if this group doesn't contain any agents or sub-groups.
     */
    public boolean isEmpty () {
        return (agents.isEmpty() && subgroups.isEmpty());
    }  
}
