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
 * TuftsDLAuthZ.java
 *
 * Created on December 19, 2003, 10:41 AM
 */

package tufts.vue;
import java.util.*;
import java.io.*;
import java.net.*;
import osid.shared.*;
import osid.authentication.*;
import osid.authorization.*;
import tufts.oki.shared.*;
import tufts.oki.authentication.*;
import tufts.oki.authorization.*;

/**
 *  This class encapsulates all of the process needed to authenticate a user and authorize
 *  certain operations on the Tufts Digital Library.  This class is a more specific process
 *  than validating and authorizing users against a Fedfora-based digital repository, of which
 *  the Tufts digital library is one.
 *  <p>
 *  The overall process consists of:
 *  <ol>
 *  <li>Load the various OSID Managers.
 *  <li>Initialize the AuthorizationManager with functions and qualifiers.
 *  <li>Authenticate the user against the Tufts LDAP service using username and password.
 *  <li>Determine what level of access the user has (agent type).
 *  <li>Create authorizations for the user.
 *  </ol> 
 *
 * @author  Mark Norton
 */
public class TuftsDLAuthZ {
    public static final String VISITOR_LIST = new String ("http://www.nolaria.org/vue_visitors.txt");
    public static final String ADMIN_LIST = new String ("http://www.nolaria.org/vue_admins.txt");
    public static final String AUTH_SEARCH = new String ("search");
    public static final String AUTH_VIEW = new String ("view");
    public static final String AUTH_UPDATE = new String ("update");
    public static final String AUTH_ADD = new String ("add");
    public static final String AUTH_DELETE = new String ("delete");
    public static final String ASSET = new String ("asset");
    public static final String OWNED_ASSET = new String ("owned-asset");
    private String username = null;             //  Cached user name.
    private String password = null;             //  Cached  password.
    private HashMap functionTypes = null;       //  Indexed function types.
    private HashMap qualifierTypes = null;      //  Indexed qualifier types.
    private HashMap agentTypes = null;          //  Index agent types.
    private HashMap functions = null;           //  Indexed functions.
    private HashMap qualifiers = null;          //  Indexed qualifiers.
    private VueQualifier root = null;           //  The root qualifier.
    private osid.shared.SharedManager sharedMgr = null;
    private tufts.oki.authentication.AuthenticationManager authNMgr = null;
    private tufts.oki.authorization.AuthorizationManager authZMgr = null;
    private osid.shared.Agent userAgent = null; //  The default user.
    
    /** 
     *  Creates a new instance of TuftsDLAuthZ.  This constructor does the bulk of the
     *  work in authenticating a user and determine what authorizations are to be assigned.
     */
    public TuftsDLAuthZ() throws osid.OsidException {
        this.agentTypes = new HashMap(100);
        this.qualifierTypes = new HashMap(100);
        this.functionTypes = new HashMap(100);
        this.qualifiers = new HashMap(100);
        this.functions = new HashMap(100);

        //  Load the various managers needed.
        osid.OsidOwner myOwner = new osid.OsidOwner();
        sharedMgr = (tufts.oki.shared.SharedManager) osid.OsidLoader.getManager("osid.shared.SharedManager", "tufts.oki.shared", myOwner);      
        authNMgr = (tufts.oki.authentication.AuthenticationManager) osid.OsidLoader.getManager("osid.authentication.AuthenticationManager", "tufts.oki.authentication", myOwner);      
        authNMgr.initManager (sharedMgr);
        authZMgr = (tufts.oki.authorization.AuthorizationManager) osid.OsidLoader.getManager("osid.authorization.AuthorizationManager", "tufts.oki.authorization", myOwner);      
        authZMgr.initManager ((osid.shared.Agent) null, sharedMgr);
        
        //  Initialize the functions, qualifiers, and their types.
        //  Qualifiers must be initialized first, since functions refer to them indirectly.
        initQualifierTypes();
        initFunctionTypes();
        initQualifiers();
        initFunctions();
        initAgentTypes();
        
    }
    
    /**
     *  Initialize the qualifier types.  These represent content objects in a Fedora repository.
     *
     *  @author Mark Norton
     */
    private void initQualifierTypes() {
        qualifierTypes.put (TuftsDLAuthZ.ASSET, new VueQualifierType (VueQualifierType.QUALIFIER_TYPE_KEY+"Fedora.Asset", "A Fedora Asset."));
        qualifierTypes.put (TuftsDLAuthZ.OWNED_ASSET, new VueQualifierType (VueQualifierType.QUALIFIER_TYPE_KEY+"Fedora.OwnedAsset", "An owned Fedora Asset."));
    }
    
    /**
     *  Initialize the function types.  These represent operations that can be performed against
     *  a Fedora repository.
     *
     *  @author Mark Norton
     */
    private void initFunctionTypes() {
        functionTypes.put (TuftsDLAuthZ.AUTH_SEARCH, new VueFunctionType (VueFunctionType.FUNCTION_TYPE_KEY+"Fedora.Search", "Search a Fedora repository."));
        functionTypes.put (TuftsDLAuthZ.AUTH_VIEW, new VueFunctionType (VueFunctionType.FUNCTION_TYPE_KEY+"Fedora.View", "View an asset in a Fedora repository."));
        functionTypes.put (TuftsDLAuthZ.AUTH_UPDATE, new VueFunctionType (VueFunctionType.FUNCTION_TYPE_KEY+"Fedora.Update", "Update an asset in a Fedora repository."));
        functionTypes.put (TuftsDLAuthZ.AUTH_ADD, new VueFunctionType (VueFunctionType.FUNCTION_TYPE_KEY+"Fedora.Add", "Add a new asset to a Fedora repository."));
        functionTypes.put (TuftsDLAuthZ.AUTH_DELETE, new VueFunctionType (VueFunctionType.FUNCTION_TYPE_KEY+"Fedora.Delete", "Delete an asset in a Fedora repository."));
    }
    
    /**
     *  Initialize the qualifiers.  These represent content objects in the Tufts digital
     *  library.
     *
     *  @author Mark Norton
     */
    private void initQualifiers () {
        try {
            root = (VueQualifier) authZMgr.createRootQualifier(sharedMgr.createId(), "Tufts DL Asset", "A Tufts digital library asset.", (osid.shared.Type) qualifierTypes.get("asset"), (osid.shared.Id) null);
            qualifiers.put (TuftsDLAuthZ.ASSET, root);
            qualifiers.put (TuftsDLAuthZ.OWNED_ASSET, authZMgr.createQualifier(sharedMgr.createId(), "Tufts DL Owned Asset", "An owned Tufts digital library asset.", (osid.shared.Type) qualifierTypes.get("owned-asset"), root.getId()));
        }
        catch (osid.shared.SharedException ex) {}
    }
    
    /**
     *  Initialize the functions.  These represent the operations that can be performed
     *  on the Tufts Digital Library.
     *
     *  @author Mark Norton
     */
    private void initFunctions () {
        try {
            functions.put (TuftsDLAuthZ.AUTH_SEARCH, authZMgr.createFunction(sharedMgr.createId(), "Search", "Search the Tufts DL.", (osid.shared.Type)functionTypes.get ("search"), root.getId()));
            functions.put (TuftsDLAuthZ.AUTH_VIEW, authZMgr.createFunction(sharedMgr.createId(), "View", "View an asset in the Tufts DL.", (osid.shared.Type)functionTypes.get ("view"), root.getId()));
            functions.put (TuftsDLAuthZ.AUTH_UPDATE, authZMgr.createFunction(sharedMgr.createId(), "Update", "Update an asset in the Tufts DL.", (osid.shared.Type)functionTypes.get ("update"), root.getId()));
            functions.put (TuftsDLAuthZ.AUTH_ADD, authZMgr.createFunction(sharedMgr.createId(), "Add", "Add an asset to the Tufts DL.", (osid.shared.Type)functionTypes.get ("add"), root.getId()));
            functions.put (TuftsDLAuthZ.AUTH_DELETE, authZMgr.createFunction(sharedMgr.createId(), "Delete", "Delete an asset from the Tufts DL.", (osid.shared.Type)functionTypes.get ("delete"), root.getId()));
        }
        catch (osid.shared.SharedException ex) {}
    }
    
    /**
     *  Initialize the agent types.  These represent classes of people who may use the Tufts Digital Library.
     *
     *  @author Mark Norton
     */
    private void initAgentTypes() {
        agentTypes.put ("visitor", new VueAgentType (VueAgentType.AGENT_TYPE_KEY+"Tufts.visitor", "A person of type visitor."));
        agentTypes.put ("student", new VueAgentType (VueAgentType.AGENT_TYPE_KEY+"Tufts.student", "A person of type student."));
        agentTypes.put ("administrator", new VueAgentType (VueAgentType.AGENT_TYPE_KEY+"Tufts.administrator", "A person of type administrator."));
    }

    /**
     *  Return the SharedManager.
     *
     *  @author Mark Norton.
     */
    public osid.shared.SharedManager getSharedManager() {
        return sharedMgr;
    }
    
    /**
     *  Return the AuthenticationManager.
     *
     *  @author Mark Norton.
     */
    public osid.authentication.AuthenticationManager getAuthNManager() {
        return authNMgr;
    }
    
    /**
     *  Return the AuthorizationManager.
     *
     *  @author Mark Norton.
     */
    public osid.authorization.AuthorizationManager getAuthZManager() {
        return authZMgr;
    }
    
    /**
     *  Authorize User is used to authenticate a a user given by a username and password
     *  against the Tufts LDAP server.  Four classes of users are defined subsequently.
     *  If the LDAP server doesn't validate the user, the username is checked against a list
     *  of accepted visitors.  If the username is on that list, an agent of type visitor is
     *  created.  If not, this is not a valid user of the Tufts Digital Library.  If accepted,
     *  the username is checked against a list of administrators.  If registered in that list
     *  an agent of type administrator is created, otherwise the agent is of type student.
     *  <p>
     *  Once the user is verifed and appropicately classed, authorizations are created for 
     *  that user class.  The following operations are permitted on the Tufts Digital Library:
     *  <ol>
     *  <li>search - search for material in the library, shown as titles and descriptions.
     *  <li>view - view an asset in the library.
     *  <li>update - update an existing asset in the library.
     *  <li>add - add a new asset to the library.
     *  <li>delete - remove an asset from the library.
     *  </ol>
     *  This method throws osid.authentication.AuthenticationException if the user is not
     *  authorized to access the Tufts Digital Library.
     *  <p>
     *  The authenticated and authorized Agent is returned.  Use this agent to test for
     *  authorizations to perform the operations defined above using the isAuthorized()
     *  method.
     *  
     *  @author Mark Norton
     */
    public osid.shared.Agent authorizeUser (String username, String password) throws osid.authentication.AuthenticationException {
        osid.shared.Agent user = null;
        
        //  Authentication the user via LDAP.
        authNMgr.setUsername (username);
        authNMgr.setPassword (password);
        
        //  Create an LDAP authentication type.
        osid.shared.Type ldapType = (osid.shared.Type) new tufts.oki.authentication.AuthenticationLDAP();

        //  Authentication the user against the Tufts LDAP system.
        try {
            authNMgr.authenticateUser(ldapType);
            try {
                user = sharedMgr.createAgent(username, (osid.shared.Type) agentTypes.get("student")); 
                System.out.println ("User is a student.");
            }
            catch (osid.shared.SharedException ex1) {}
        }
        catch (osid.authentication.AuthenticationException ex2) {
            //  Failed against Tufts LDAP server, check to see if user is on the visitor list
            if (isVisitor (username)) {
                //  Convert the agent to a visitor-agent.
                try {
                    user = sharedMgr.createAgent(username, (osid.shared.Type) agentTypes.get("visitor")); 
                }
                catch (osid.shared.SharedException ex3) {}
                System.out.println ("User is a visitor.");
            }
            else
                throw ex2;
        }
        System.out.println ("authorizeUser:  User is authenticated.");
        
        /*  Get the user agent.  -  Code not needed.  All cases covered.
        osid.shared.Id userId =authNMgr.getUserId(ldapType);
        try {
            user = sharedMgr.getAgent(userId);
        }
        catch (osid.shared.SharedException ex4) {
            throw new osid.authentication.AuthenticationException ("Unknown User Id");
        }
         */
        
        //  Check to see if this user is an administrator.
        if (isAdmin (username)) {
            //  Convert the user to an admin user.
            try {
                user = sharedMgr.createAgent(username, (osid.shared.Type) agentTypes.get("administrator")); 
                System.out.println ("User is an administrator.");
            }
            catch (osid.shared.SharedException ex2) {}
        }
        
        //  Create authorizations for this user.
        authorizeUser (user);
        
        this.userAgent = user;
        return user;
    }
    
    /**
     *  Return true if the username given is present in the list of visitor
     *  users.  This list is a text file kept at a location determined by the
     *  VISITOR_LIST constant.
     *
     *  @author Mark Norton
     */
    private boolean isVisitor (String username) {
        URL u = null;
        StringBuffer buf = null;
        try {
            //  Read the visitor list from the VISTOR url.
            u = new URL (TuftsDLAuthZ.VISITOR_LIST);
            InputStream in = u.openStream();
            buf = new StringBuffer (in.available()+1);
            int b = 0;
            while ((b = in.read()) != -1) {
                buf.append((char)b);
                //System.out.write(b);
            }
            in.close();
            
            //  Compare entries in the list against username provided.
            Vector admins = StringUtils.explode(buf, '\r');
            for (int i=0; i<admins.size(); i++) {
                //System.out.println ("visitor - \t["+ ((String)admins.elementAt(i)).trim() + "] vs. [" + username + "]");
                if (username.compareTo(((String) admins.elementAt(i)).trim()) == 0)
                    return true;
            }
        }
        catch (MalformedURLException ex1) {
            return false;
        }
        catch (IOException ex2) {
            return false;
        }
        return false;
    }
    
    /**
     *  Return true if the username given is present in the list of administrative
     *  users.  This list is a text file kept at a location determined by the
     *  ADMIN_LIST constant.
     *
     *  @author Mark Norton
     */
    private boolean isAdmin (String username) {
        URL u = null;
        StringBuffer buf = null;
        try {
            //  Read the admin list provided in the ADMIN url.
            u = new URL (TuftsDLAuthZ.ADMIN_LIST);
            InputStream in = u.openStream();
            buf = new StringBuffer (in.available()+1);
            int b = 0;
            while ((b = in.read()) != -1) {
                buf.append((char)b);
                //System.out.write(b);
            }
            in.close();
            
            //  Compare entries in the list against the username provided.
            Vector admins = StringUtils.explode(buf, '\r');
            //System.out.println ("Admin user count is: " + admins.size());
            //System.out.println ("Admin users: ");
            for (int i=0; i<admins.size(); i++) {
                //System.out.println ("admin -\t["+ admins.elementAt(i) + "] vs. [" + username + "]");
                if (username.compareTo(((String) admins.elementAt(i)).trim()) == 0)
                    return true;
            }
        }
        catch (MalformedURLException ex1) {
            return false;
        }
        catch (IOException ex2) {
            return false;
        }
        return false;
    }
    
    /**
     *  Assign authorizations to a user based on the agent type.  Three agents types are
     *  currently defined for the Tufts Digital Library:
     *  <p>
     *  Visitors are allowed to search and view a restricted set of assets.<br>  
     *  Students are allowed to search and view assets and update owned-assets.<br>
     *  Administrators are allowed to search, view, update, add, and delete assets.<br> 
     *
     *  @author Mark Norton
     */
    private void authorizeUser (osid.shared.Agent user) {
        //  Get the agent type for this user.
        osid.shared.Type userType = null;
        osid.shared.Id userId = null;
        osid.authorization.Function ftn = null;
        try {
            userType = user.getType();
            userId = user.getId();
        }
        catch (osid.shared.SharedException ex) {}
        
        //  Check to see if this user is a visitor.
        if (userType.isEqual((osid.shared.Type) agentTypes.get ("visitor"))) {
            try {
                this.authZMgr.createAuthorization(userId, ((osid.authorization.Function)functions.get(TuftsDLAuthZ.AUTH_SEARCH)).getId(), ((osid.authorization.Qualifier)qualifiers.get(TuftsDLAuthZ.ASSET)).getId());
                this.authZMgr.createAuthorization(userId, ((osid.authorization.Function)functions.get(TuftsDLAuthZ.AUTH_VIEW)).getId(), ((osid.authorization.Qualifier)qualifiers.get(TuftsDLAuthZ.ASSET)).getId());
            }
            catch (osid.authorization.AuthorizationException ex2) {}
        }
        
        //  Check to see if this user is a student.
        if (userType.isEqual((osid.shared.Type) agentTypes.get ("student"))) {
            try {
                ftn = ((osid.authorization.Function)functions.get(TuftsDLAuthZ.AUTH_SEARCH));
                this.authZMgr.createAuthorization(userId, ftn.getId(), ((osid.authorization.Qualifier)qualifiers.get(TuftsDLAuthZ.ASSET)).getId());
                ftn = ((osid.authorization.Function)functions.get(TuftsDLAuthZ.AUTH_VIEW));
                this.authZMgr.createAuthorization(userId, ftn.getId(), ((osid.authorization.Qualifier)qualifiers.get(TuftsDLAuthZ.ASSET)).getId());
                ftn = ((osid.authorization.Function)functions.get(TuftsDLAuthZ.AUTH_UPDATE));
                this.authZMgr.createAuthorization(userId, ftn.getId(), ((osid.authorization.Qualifier)qualifiers.get(TuftsDLAuthZ.OWNED_ASSET)).getId());
            }
            catch (osid.authorization.AuthorizationException ex2) {}
        }
        
        //  Check to see if this user is an administrator.
        if (userType.isEqual((osid.shared.Type) agentTypes.get ("administrator"))) {
            try {
                ftn = ((osid.authorization.Function)functions.get(TuftsDLAuthZ.AUTH_SEARCH));
                System.out.println ("authorizeUser - ftn id: "+ftn.getId().getIdString()+" type: "+ftn.getFunctionType().getKeyword());
                this.authZMgr.createAuthorization(userId, ftn.getId(), ((osid.authorization.Qualifier)qualifiers.get(TuftsDLAuthZ.ASSET)).getId());
                ftn = ((osid.authorization.Function)functions.get(TuftsDLAuthZ.AUTH_VIEW));
                System.out.println ("authorizeUser - ftn id: "+ftn.getId().getIdString()+" type: "+ftn.getFunctionType().getKeyword());
                this.authZMgr.createAuthorization(userId, ftn.getId(), ((osid.authorization.Qualifier)qualifiers.get(TuftsDLAuthZ.ASSET)).getId());
                ftn = ((osid.authorization.Function)functions.get(TuftsDLAuthZ.AUTH_UPDATE));
                System.out.println ("authorizeUser - ftn id: "+ftn.getId().getIdString()+" type: "+ftn.getFunctionType().getKeyword());
                this.authZMgr.createAuthorization(userId, ftn.getId(), ((osid.authorization.Qualifier)qualifiers.get(TuftsDLAuthZ.ASSET)).getId());
                ftn = ((osid.authorization.Function)functions.get(TuftsDLAuthZ.AUTH_ADD));
                System.out.println ("authorizeUser - ftn id: "+ftn.getId().getIdString()+" type: "+ftn.getFunctionType().getKeyword());
                this.authZMgr.createAuthorization(userId, ftn.getId(), ((osid.authorization.Qualifier)qualifiers.get(TuftsDLAuthZ.ASSET)).getId());
                ftn = ((osid.authorization.Function)functions.get(TuftsDLAuthZ.AUTH_DELETE));
                System.out.println ("authorizeUser - ftn id: "+ftn.getId().getIdString()+" type: "+ftn.getFunctionType().getKeyword());
                this.authZMgr.createAuthorization(userId, ftn.getId(), ((osid.authorization.Qualifier)qualifiers.get(TuftsDLAuthZ.ASSET)).getId());
            }
            catch (osid.authorization.AuthorizationException ex2) {}
            catch (osid.shared.SharedException ex3) {}
        }
    }
    
    /**
     *  Return an interator which lists all authorizations for the agent provided.
     *
     *  @author Mark Norton
     */
    public osid.authorization.AuthorizationIterator getAuthorizations (osid.shared.Agent user) {
        osid.shared.Id userId = null;
        try {
            userId = user.getId();
        }
        catch (osid.shared.SharedException ex) {}
        return this.authZMgr.getAllExplicitAZs (userId, false);
    }
    
    /**
     *  Use this method to determine if the user-agent is valid to perform the
     *  operation indicated (use the TuftsDLAuthZ keywords defined above) on the
     *  asset type provided.  (use TuftsDLAuthZ.ASSET or TuftsDLAuthZ.OWNED_ASSET)
     *
     *  @author Mark Norton
     */
    public boolean isAuthorized (osid.shared.Agent user, String operation, String assetType) {
        osid.authorization.Function ftn = (osid.authorization.Function) functions.get (operation);
        osid.authorization.Qualifier qual = (osid.authorization.Qualifier) qualifiers.get (assetType);
        osid.shared.Id userId = null;
        osid.shared.Id ftnId =  null;
        osid.shared.Id qualId = null;
        try {
            userId = user.getId();
            ftnId = ftn.getId();
            qualId = qual.getId();
        }
        catch (osid.authorization.AuthorizationException ex1) {}
        catch (osid.shared.SharedException ex2) {}
        
        return authZMgr.isAuthorized (userId, ftnId, qualId);
    }

    /**
     *  Use this method to determine if the user-agent is valid to perform the
     *  operation indicated (use the TuftsDLAuthZ keywords defined above).  This
     *  assumes an asset type of TuftsDLAuthZ.ASSET.
     *
     *  @author Mark Norton
     */
    public boolean isAuthorized (osid.shared.Agent user, String operation) {
        return isAuthorized (user, operation, TuftsDLAuthZ.ASSET);
    }

    /**
     *  Use this method to determine if the default user is valid to perform the
     *  operation indicated (use the TuftsDLAuthZ keywords defined above).  This
     *  assumes an asset type of TuftsDLAuthZ.ASSET.
     *
     *  @author Mark Norton
     */
    public boolean isAuthorized (String operation) {
        return isAuthorized (this.userAgent, operation, TuftsDLAuthZ.ASSET);
    }
}
