/*
 * AgentPersonType.java
 *
 * Created on September 22, 2003, 2:57 PM
 */

package src.tufts.shared;

/**
 *  This is a person agent type.
 *
 *  @author  Mark Norton
 */
public class AgentPersonType extends osid.shared.Type {
    /* Use this keyword to search for the person type.  */
    public static final String AGENT_PERSON_TYPE_KEY = "osid.shared.Agent.Person";
    
    /** Creates a new instance of AgentPersonType */
    public AgentPersonType() {
        super ("osid.shared", osid_mjn.OsidManager.AUTHORITY, AGENT_PERSON_TYPE_KEY, "This is a person agent as implemented by osid.shared.Agent.");
    }
    
}
