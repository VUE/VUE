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
 * AuthenticationManager.java
 *
 * Created on October 23, 2003, 3:22 PM
 */

package tufts.oki.authentication;
import java.util.*;
import tufts.oki.OsidManager;
import tufts.oki.shared.*;
import osid.authentication.*;
import javax.naming.*;
import javax.naming.directory.*;

/**
 *  The AuthenticationManager is responsible for verifying the identity of a person or
 *  entity desiring access to OKI OSID based services.  The interface is intended to 
 *  isolate applications from the underlying details of verify a user.  Multiple
 *  authenication methods may be supported by a single interface, identified by
 *  an AuthenticationType.  In this implementation, the following types are defined:
 *  <ul>
 *  <li>AuthenticationLDAP
 *  </ul>
 *  LDAP-based authentication uses JNDI to access enterprise
 *  directory information.  Other methods, such as Kerberos are not supported at 
 *  this time.
 *  <p>
 *  The AuthenticationManager
 *  maintains a list of known authentication types, which can be accessed using
 *  getAuthenticationTypes().  This list is created and initialized here.
 *
 *  @author  Mark Norton
 */
public class AuthenticationManager extends OsidManager implements osid.authentication.AuthenticationManager {
    private Vector am_types = null;
    private osid.shared.Agent am_agent = null;
    
    //  Used for LDAP verification.
    private String am_username = null;
    private String am_password = null;

    static public osid.shared.SharedManager sharedMgr = null;   //  SharedManager is cached.

    /** 
     *  Creates a new instance of AuthenticationManager with owner passed.  
     *
     *  @author Mark Norton
     */
    public AuthenticationManager() {
        super();
        
        //  Create a vector of AuthN types and initialize it with all known types.
        am_types = new Vector(2);
        AuthenticationLDAP l_type = new AuthenticationLDAP();
        am_types.addElement(l_type);
    }
    
    /** 
     *  Creates a new instance of AuthenticationManager with owner passed.  
     *
     *  @author Mark Norton
     */
    public AuthenticationManager(osid.OsidOwner owner) {
        super(owner);
        
        //  Create a vector of AuthN types and initialize it with all known types.
        am_types = new Vector(2);
        AuthenticationLDAP l_type = new AuthenticationLDAP();
        am_types.addElement(l_type);
    }
    
    /**
     *  Initialize the authorization manager.  This should be called if the Authorization
     *  Manager is loaded by OsidLoader.  It causes the default user to be initialized and
     *  the Shared Manager to be cached locally.
     *
     *  @author Mark Norton
     */
    public void initManager (osid.shared.SharedManager sharedMgr) {
        this.sharedMgr = sharedMgr;
    }
    
    /**
     *  Get the Agent of an authenticted user.
     *
     *  @author Mark Norton
     *
     *  @return A valid Agent object intialized with any known propertites.
     *
     *  @deprecated in rc6.1.  Replaced by authenticateUser(type).
     */
    public osid.shared.Agent authenticate(osid.shared.Type authType) throws osid.authentication.AuthenticationException {
        throw new osid.authentication.AuthenticationException("Deprecated");
    }
    
    /**
     *  AuthenticateUser is the main method used to check the identity of a user.
     *  An authenticationType is passed to indicate which information will be
     *  required to validate this user and which enterprise authentication service
     *  will be used.
     *  <p>
     *  We assume that the AuthenticationManager is initially called
     *  with an empty owner and that it will be updated to the correct owner once authenticated.
     *  This makes sense, because subsequent calls to isAuthenticated() will fail unless the
     *  agent-owner is correctly set.
     *  <p>
     *  In order to speparate out implementations which used different underlying authentication
     *  schemems, authenticateUser() calls aauthenticationViaLDAP().
     *  <p>
     *  Note that changes introduced in rc6.1 of the osid.authentication.AuthenticationManager cause
     *  an agent object to be created as a side effect.  The Id of this object can be gotten by
     *  getUserId().  I have also added getUserAgent(), which is not part of the formal interface.
     *
     *  @author Mark Norton
     */
    public void authenticateUser(osid.shared.Type authType) throws osid.authentication.AuthenticationException {
        Agent temp = null;
        
        //  Check authNType against all known authorization types.
        if (authType.isEqual (new AuthenticationLDAP()))
            am_agent = this.authenticateViaLDAP();
        else
            //  Type is not known.
            throw new osid.authentication.AuthenticationException(osid.authentication.AuthenticationException.UNKNOWN_TYPE);
    }
    
    /**
     *  Destroy all authentications of the agent-owner associated with this manager.
     *
     *  @author Mark Norton
     */
    public void destroyAuthentication() throws osid.authentication.AuthenticationException {
        //  The the owner, which has context.
        osid.OsidOwner owner = super.getOwner();
        
        //  Interate over all known autentication types and remove them from context.
        osid.shared.TypeIterator it = getAuthenticationTypes();
        try {
            while (it.hasNext() == true) {
                osid.shared.Type type = it.next();
                String key = getAuthNTypeKey(type);
                try {
                    owner.removeContext(key);
                }
                catch (osid.OsidException ex1) {
                    //  We could report an error here, but if it doesn't exist, we don't need to destroy.
                }
            }
        }
        catch (osid.shared.SharedException ex2) {
            //  As far as I know, there are no real exceptions thrown.
        }
        
        //  Set the agent to null.
        am_agent = null;
    }
    
    /**
     *  Destroy the authentication of given type of the agent-owner associated with this manager.
     *
     *  @author Mark Norton
     *
     *  @deprecated in rc6.1.  Replaced by destroyAuthenticationForType(type);
     */
    public void destroyAuthentication(osid.shared.Type authenticationType) throws osid.authentication.AuthenticationException {
        throw new osid.authentication.AuthenticationException("Deprecated");
    }
    
    /**
     *  Destroy the authentication of given type of the agent-owner associated with this manager.
     *
     *  @author Mark Norton
     */
    public void destroyAuthenticationForType(osid.shared.Type authenticationType) throws osid.authentication.AuthenticationException {
        // Check to see final this is a valid type and if not, throw exception.
        if (isValidAuthNType (authenticationType) == false) {
            throw new osid.authentication.AuthenticationException(osid.authentication.AuthenticationException.UNKNOWN_TYPE);
        }
        
        //  Remove the context associated with this type.
        osid.OsidOwner owner = super.getOwner();
        String key = getAuthNTypeKey(authenticationType);
        try {
            owner.removeContext(key);
        }
        catch (osid.OsidException ex) {
            //  We could report an error here, but if it doesn't exist, we don't need to destroy.
        }

        //  Set the agent to null.
        am_agent = null;
    }
    
    /**
     *  Returns the known authentication types as an iterated list.
     *
     *  @author Mark Norton
     *
     *  @return TypeIterator which lists each known authentication type.
     */
    public osid.shared.TypeIterator getAuthenticationTypes() {
        tufts.oki.shared.TypeIterator it = new tufts.oki.shared.TypeIterator (am_types);
        return it;
    }
    
    /**
     *  Common pratice (such as it is at this stage) indicates that the agent to
     *  be tested for authentication is kept in the agent-owner of the OsidManager.
     *  This method is called to determine if this agent object is still authenticated.
     *  The main reason that this would fail is if there is a time limit associated
     *  with the authentication type given.  Once the limit expires, the agent is
     *  no longer valid and must be re-authenticated.
     *
     *  @author Mark Norton
     *
     *  @deprecated in rc6.1.  Replaced by isUserAuthenticated(type);
     */
    public boolean isAuthenticated(osid.shared.Type authenticationType) throws osid.authentication.AuthenticationException {
        throw new osid.authentication.AuthenticationException("Deprecated");
    }
 
    /**
     *  As of rc6.1, authenticateUser() causes an agent to be located as a side
     *  effect.  Once found, it can be used as the basis for further authentication.
     *  Some form of session authorizaion might be needed to determine sustained
     *  authentication against some time limit.  In this implementation, if the
     *  user has been authenticated once, he or she remains authenticated forever.
     *  <p>
     *  Common pratice (such as it is at this stage) indicates that the agent to
     *  be tested for authentication is kept in the agent-owner of the OsidManager.
     *  This method is called to determine if this agent object is still authenticated.
     *  The main reason that this would fail is if there is a time limit associated
     *  with the authentication type given.  Once the limit expires, the agent is
     *  no longer valid and must be re-authenticated.
     *
     *  @author Mark Norton
     */
    public boolean isUserAuthenticated(osid.shared.Type authenticationType) throws osid.authentication.AuthenticationException {
        return (am_agent != null);
    }
    
    /**
     *  Get the user Id associated with the authentication type passed.
     *
     *  @author Mark Norton
     */
    public osid.shared.Id getUserId(osid.shared.Type authenticationType) throws osid.authentication.AuthenticationException {
        try {
            return am_agent.getId();
        }
        catch (osid.shared.SharedException ex) {
            throw new osid.authentication.AuthenticationException("Unknown Id");
        }
    }
    
    /**
     *  Get the user Agent associated with the authentication type passed.
     *  <p>
     *  This is an extension to the osid.authentication.AuthenticationManager definition.
     *
     *  @author Mark Norton
     */
    public osid.shared.Agent getAgent(osid.shared.Type authenticationType) {
        return am_agent;
    }

    /*  Private methods for this implementation follow.  */
    /*  -----------------------------------------------  */
    
    /**
     *  Checks to see if is a valid authentication type was passed.
     *  This is a private method in AuthenticationManager and is not part of the
     *  osid.authentication interface definitions.
     *
     *  @author Mark Norton
     *
     *  @return True if the type passed is a valid authentication type.
     */
    private boolean isValidAuthNType (osid.shared.Type authNType) {
        boolean val = false;
        
        //  Check authNType against all known authorization types.
        if (authNType.isEqual (new AuthenticationLDAP()))
            val = true;
        return val;
   }
    
    /**
     *  Get the authentication keyword associated with this authentication type.
     *  This is a private method in AuthenticationManager and is not part of the
     *  osid.authentication interface definitions.
     *
     *  @author Mark Norton
     *
     *  @return The authentication keyword for this authentication type.
     */
    private String getAuthNTypeKey (osid.shared.Type authNType) throws osid.authentication.AuthenticationException {
        String key;
        //  Check authNType against all known authorization types.
        if (authNType.isEqual (new AuthenticationLDAP()))
            key = AuthenticationLDAP.AUTHN_LDAP_KEY;
        else
            throw new osid.authentication.AuthenticationException(osid.authentication.AuthenticationException.UNKNOWN_TYPE);

        return key;
   }
    
    /**
     *  Authenticate using the LDAP method.  The current plan is to accept a user name
     *  and look it up in the tufts.edu LDAP registry.  If the user has a valid tufts.edu
     *  email address, then he or she is considered a valid user.
     *  <p>
     *  While this will allow students at Tufts to use VUE, a different validation scheme will
     *  be needed for anyone else.  Also note that it will be pretty easy to guess at a
     *  user name if you have the email address of anyone at Tufts.
     *  <p>
     *  Authenticate using LDAP.  Username and password must be set before calling this
     *  method.  Checks for null and blank username and password, then verifies against
     *  ldap.tufts.edu server using JDNI.  If the user validates against the LDAP server,
     *  he or she is considered to be a valid Tufts student, faculty, or administrator.
     *  This method replaces the previous version which only validated usernames.
     *  <p>
     *  This is a private method in AuthenticationManager and is not part of the
     *  osid.authentication interface definitions.
     *
     *  @author Mark Norton
     *
     *  @return A valid Agent object intialized with any known propertites.
     */
    private osid.shared.Agent authenticateViaLDAP() throws osid.authentication.AuthenticationException {
        java.util.Properties props = new java.util.Properties();
        
        //  Check username and password.  Username and password are not allowed to be null.  Password may not be blank.
//        if ((am_username == null) || (am_password == null) || (am_password.trim().length() == 0)) {
        if ((am_username == null) ) {
            throw new osid.authentication.AuthenticationException (osid.authentication.AuthenticationException.OPERATION_FAILED);
        }

        //  Set up the properties needed to establish an LDAP context.
        props.put (Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        props.put (Context.PROVIDER_URL, "ldaps://ldap.tufts.edu/");
        
        try {
            /* Create the initial directory context. */
            DirContext ctx = new InitialDirContext (props);
            //System.out.println ("Initial context gained.");
            
            /* Specify search constraints to search subtree */
            SearchControls constraints = new SearchControls();
            constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);
            
            /* Search for an entry with a user name given by am_username.  List of SearchResult. */
            NamingEnumeration results = ctx.search("dc=tufts,dc=edu", "uid="+am_username, constraints);
            
            //  If the results are null, this is not a valid user.
            if (!results.hasMore()) {
                throw new osid.authentication.AuthenticationException (osid.authentication.AuthenticationException.OPERATION_FAILED);
            }
            else {
                //System.out.println ("LDAP search success");
            }
        } 
        catch (NamingException e) {
            throw new osid.authentication.AuthenticationException (osid.authentication.AuthenticationException.OPERATION_FAILED);
        }
        
        // Create an agent and return it.
        try {
            if (sharedMgr != null)
                am_agent = sharedMgr.createAgent(am_username, new tufts.oki.shared.AgentPersonType());
            else
                am_agent = new tufts.oki.shared.Agent(am_username, new tufts.oki.shared.AgentPersonType());
            //System.out.println ("Agent created.");
        } catch (osid.shared.SharedException e) {}
        
        return am_agent;
    }

    //  This approach is based on an example from java.sun.com.
    private osid.shared.Agent authenticateViaLDAP3() throws osid.authentication.AuthenticationException {
        java.util.Properties props = new java.util.Properties();
        
        //  Check username and password.  Username and password are not allowed to be null.  Password may not be blank.
        if ((am_username == null) || (am_password == null) || (am_password.trim().length() == 0)) {
            throw new osid.authentication.AuthenticationException (osid.authentication.AuthenticationException.OPERATION_FAILED);
        }

        //  Set up the properties needed to establish an LDAP context.
        props.put (Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        props.put (Context.PROVIDER_URL, "ldaps://ldap.tufts.edu/o=Central Administration (Affiliate)");
        
        //  Add the username and password.
        props.put (Context.SECURITY_AUTHENTICATION,"simple");
        props.put (Context.SECURITY_PRINCIPAL, "uid="+am_username+", ou=Computer Services, o=Central Administration (Affiliate)");
        props.put (Context.SECURITY_CREDENTIALS, am_password);
        
        try {
            /* Create the initial directory context. */
            DirContext ctx = new InitialDirContext (props);
            //System.out.println ("Initial context gained.");
            
            /* Specify search constraints to search subtree */
            SearchControls constraints = new SearchControls();
            constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);
            
            /* Search for an entry with a user name given by am_username.  List of SearchResult. */
            NamingEnumeration results = ctx.search("dc=tufts,dc=edu", "uid="+am_username, constraints);
            
            //  If the results are null, this is not a valid user.
            if (!results.hasMore()) {
                throw new osid.authentication.AuthenticationException (osid.authentication.AuthenticationException.OPERATION_FAILED);
            }
            else {
                //System.out.println ("LDAP search success");
                SearchResult sr = (SearchResult) results.next();
                //System.out.println ("Search result: "+sr.toString());
                
                //  More than one result is an error.
                if (results.hasMore()) {
                    throw new osid.authentication.AuthenticationException (osid.authentication.AuthenticationException.OPERATION_FAILED);
                }
            }
        } 
        catch (NamingException e) {
            throw new osid.authentication.AuthenticationException (osid.authentication.AuthenticationException.OPERATION_FAILED);
        }
        
        // Create an agent and return it.
        try {
            if (sharedMgr != null)
                am_agent = sharedMgr.createAgent(am_username, new tufts.oki.shared.AgentPersonType());
            else
                am_agent = new tufts.oki.shared.Agent(am_username, new tufts.oki.shared.AgentPersonType());
            //System.out.println ("Agent created.");
        } catch (osid.shared.SharedException e) {}
        
        return am_agent;
    }

    /**
     *  Get the username and password of this user.  This is likely a call-out to some
     *  GUI component, perhaps a dialog, perhaps a Struts form.  In the simplest implementation
     *  this can be gotten from stdin.
     *  <p>
     *  This is a private method in AuthenticationManager and is not part of the
     *  osid.authentication interface definitions.
     *
     *  @author Mark Norton
     */
    private String[] getUsernamePassword() {
        String[] nameAndPw = new String[2];
        
        nameAndPw[0] = "mnorton";   //  Stubbed entry of user name.
        nameAndPw[1] = "xyzzy";      //  Stubbed entry of password.

        return nameAndPw;
    }
    
    /**
     *  Set the user name to be used in LDAP authenticaiton.  This must be set before
     *  calling authenticateUser().
     *  <p>
     *  Note that this method is an extension to the original osid.authentication definitions.
     *
     *  @author Mark Norton
     */
    public void setUsername (String username) {
        am_username = username;
    }
    
    /**
     *  Set the password to be used in an LDAP authentication.  This must be set before
     *  calling authenticate user().
     *  <p>
     *  Note that this method is an extension to teh original osid.authentication definitions.
     *
     *  @author Mark Norton
     */
    public void setPassword (String password) {
        am_password = password;
    }
}
