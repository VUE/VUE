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
 * VueAuthorizationManager.java
 *
 * Created on December 15, 2003, 11:02 AM
 */
package tufts.oki.authorization;
import java.util.*;
import osid.shared.*;

/**
 *  The authorization manager maintains three lists of object related to authorizating
 *  operations in the VUE system.  These objects are VueAuthorization, VueFunction, and
 *  VueQualifier.  As stated by the OKI OSID documentation on authorization, these objects
 *  can be viewed as the subject, verb, and direct object.  The VueAuthorization indictes
 *  who has this authorization, the VueFunction defines what operation is allowed, and the
 *  VueQualifier places restrictions (if any) on the operation.
 *  <p>
 *  The AuthZMgr queries take two forms: Explicit 
 *  and Implicit. The documentation indicates that explicit authorizations may be modified. 
 *  It is assumed that explicit authorization have a function Id and 
 *  qualifier id physically present in the Authorization instance (which allows them to be 
 *  modified). This is opposed to authorizations which are valid due to inheritied qualifiers 
 *  (ancestors of the instance), which is the implicit form of authorization.
 *  <p>
 *  This implementation of the AuthorizationManager does not include persistance of any kind.
 *  This means that all authorizations, functions, and qualifiers must be defined at run time
 *  by an application which uses it.  Note also, that funtion types and qualifier types are
 *  not defined either.
 *
 * @author  Mark Norton
 */
public class AuthorizationManager extends tufts.oki.OsidManager implements osid.authorization.AuthorizationManager {
    private Vector auths = null;            //  The list of authorizations.
    private Vector functions = null;        //  The list of known functions.
    private Vector qualifiers = null;       //  The list of known qualifiers.
    private Vector rootQualifiers = null;   //  The list of qualifier hierarchies.
    private osid.shared.Agent user = null;  //  The default user of this authorization manager.
    
    static public osid.shared.SharedManager sharedMgr = null;   //  SharedManager is cached.
    
    /** 
     *  Creates a new instance of VueAuthorizationManager.  The OsidOwner provides a context
     *  which can be shared between OSID managers.  The user-agent should be an authenticated
     *  agent who is (presumably) the user of this application session.  SharedManager is cached
     *  due to the need of resolving agent instances versus their Id's.
     */
    public AuthorizationManager() {
        super();
        this.auths = new Vector(100);
        this.functions = new Vector(100);
        this.qualifiers = new Vector(100);
        this.rootQualifiers = new Vector(100);
    }
    
    /** 
     *  Creates a new instance of VueAuthorizationManager.  The OsidOwner provides a context
     *  which can be shared between OSID managers.  The user-agent should be an authenticated
     *  agent who is (presumably) the user of this application session.  SharedManager is cached
     *  due to the need of resolving agent instances versus their Id's.
     */
    public AuthorizationManager(osid.OsidOwner owner, osid.shared.Agent user, osid.shared.SharedManager sharedMgr) {
        super(owner);
        this.user = user;
        this.auths = new Vector(100);
        this.functions = new Vector(100);
        this.qualifiers = new Vector(100);
        this.rootQualifiers = new Vector(100);
        this.sharedMgr = sharedMgr;
    }
    
    /**
     *  Initialize the authorization manager.  This should be called if the Authorization
     *  Manager is loaded by OsidLoader.  It causes the default user to be initialized and
     *  the Shared Manager to be cached locally.
     *
     *  @author Mark Norton
     */
    public void initManager (osid.shared.Agent user, osid.shared.SharedManager sharedMgr) {
        this.user = user;
        this.sharedMgr = sharedMgr;
    }
    
    /**
     *  The documentation indicates that this check doesn't determine if the agent is
     *  referenced by the AZMgr, or is present in the shared manager.  The MIT implementation
     *  checks for an agent of the Id in a database table.  It returns true if the agent
     *  is "known" to the authorization manager.  This implementation returns true if
     *  an agent of the Id given is present in the list of authorizations.
     *
     *  @author Mark Norton
     */
    public boolean agentExists(osid.shared.Id agentId) {
        for (int i = 0; i < auths.size(); i++) {
            VueAuthorization az = (VueAuthorization) auths.elementAt(i);
            try {
                if (agentId.isEqual(az.getAgent().getId()))
                    return true;
            }
            catch (osid.shared.SharedException ex) {
                return false;
            }
            catch (osid.authorization.AuthorizationException ex2) {
                return false;
            }
        }
        return false;
    }
    
    /**
     *  Return a new authorization given an agent, function, and qualifier.  Use this version
     *  if the authorization doesn't have date bounds.  Creates explicit authorizations.
     *
     *  @author Mark Norton
     */
    public osid.authorization.Authorization createAuthorization(osid.shared.Id agentId, osid.shared.Id functionId, osid.shared.Id qualifierId) {
        return createDatedAuthorization (agentId, functionId, qualifierId, null, null);
    }
    
    /**
     *  Return a new authorization given an agent, function, and qualifier.  Use this version if
     *  the authorization has date bounds.  Creates explicit authorizations.
     *
     *  @author Mark Norton
     */
    public osid.authorization.Authorization createDatedAuthorization(osid.shared.Id agentId, osid.shared.Id functionId, osid.shared.Id qualifierId, java.util.Calendar effectiveDate, java.util.Calendar expirationDate) {
        //System.out.println ("createDatedAuthorization - functionID:  "+functionId);
        osid.shared.Id authId = null;
        try {
            authId = sharedMgr.createId();
        }
        catch (osid.shared.SharedException ex) {}
        osid.authorization.Function ftn = getFunction (functionId);
        osid.authorization.Qualifier qual = getQualifier (qualifierId);
        VueAuthorization az = new VueAuthorization (authId, agentId, ftn, qual, effectiveDate, expirationDate, true);
        auths.add (az);
        return (osid.authorization.Authorization) az;
    }
    
    /**
     *  Create a new function given a functionId, display name, description, function type, and hierarchy id.
     *
     *  @author Mark Norton
     */
    public osid.authorization.Function createFunction(osid.shared.Id functionId, String displayName, String description, osid.shared.Type functionType, osid.shared.Id qualifierHierarchyId) {
        VueFunction ftn = new VueFunction (displayName, description, functionType, functionId, qualifierHierarchyId);
        functions.add (ftn);
        return (osid.authorization.Function) ftn;
    }
    
    /**
     *  Create a new qualifier given a Id, dislay name, description, qualifier type, and parent.
     *  The parent may be null, if there is no inheritance relationships.
     *
     *  @author Mark Norton
     */
    public osid.authorization.Qualifier createQualifier(osid.shared.Id qualifierId, String displayName, String description, osid.shared.Type qualifierType, osid.shared.Id parentId) {
        VueQualifier parent = (VueQualifier) this.getQualifier(parentId);
        VueQualifier qual = new VueQualifier (displayName, description, qualifierId, qualifierType, this, parent);
        qualifiers.add (qual);
        return qual;
    }
    
    /**
     *  Create a qualifier node and enter it as the root of a qualifier hierarchy.  Since osid.hierarchy is not being
     *  used in this implementation, the qualifier node created is added to the list of root nodes.  The qualifier
     *  hierarchy Id is not used and may be passed as null.  Qualifier roots do not have parents.
     *
     *  @author Mark Norton
     */
    public osid.authorization.Qualifier createRootQualifier(osid.shared.Id qualifierId, String displayName, String description, osid.shared.Type qualifierType, osid.shared.Id qualifierHierarchyId) {
        VueQualifier qual = new VueQualifier (displayName, description, qualifierId, qualifierType, null);
        qualifiers.add (qual);
        rootQualifiers.add (qual);
        return qual;
    }
    
    /**
     *  Delete the authorization given by the identifier passed.
     *
     *  @author Mark Norton
     */
    public void deleteAuthorization(osid.shared.Id authorizationId) {
        for (int i=0; i < auths.size(); i++) {
            VueAuthorization az = (VueAuthorization) auths.elementAt(i);
            try {
                if (authorizationId.isEqual(az.getId())) {
                    auths.remove(i);
                }
            }
            catch (osid.shared.SharedException ex) {}
        }
    }
    
    /**
     *  Delete the function given by its identifier.  All authorizations based on this
     *  function are also deleted.
     *
     *  @author Mark Norton
     */
    public void deleteFunction(osid.shared.Id functionId) {
        //  Search for all authorizations based on this function and delete them.
        for (int i=0; i < auths.size(); i++) {
            VueAuthorization az = (VueAuthorization) auths.elementAt(i);
            VueFunction ftn = (VueFunction) az.getFunction();
            try {
                if (functionId.isEqual (ftn.getId()))
                    auths.remove (i);
            }
            catch (osid.shared.SharedException ex) {}
        }
        
        //  Search for the function and remove it from the list of known functions.
        for (int j=0; j < functions.size(); j++) {
            VueFunction ftn = (VueFunction) functions.elementAt(j);
            try {
                if (functionId.isEqual(ftn.getId()))
                    functions.remove(j);
            }
            catch (osid.shared.SharedException ex2) {}
        }
    }
    
    /**
     *  Since deleting qualifiers has a serious impact on the trees that they are included
     *  in, deletion is not supported in this implementation.
     *
     *  @author Mark Norton
     */
    public void deleteQualifier(osid.shared.Id qualifierId) throws osid.authorization.AuthorizationException {
        //  Trees need to be repaired if qualifiers are deleted.
        throw new osid.authorization.AuthorizationException (osid.authorization.AuthorizationException.UNIMPLEMENTED);
    }
    
    /**
     *  Returns all authorizations with the agent id, function id and qualifier id given.
     *  If the active flag is true, the authorizations must be within the activation dates.
     *
     *  @author Mark Norton
     */
    //  agent-id, funtion-id, qualifier-id, active
    public osid.authorization.AuthorizationIterator getAllAZs(osid.shared.Id agentId, osid.shared.Id functionId, osid.shared.Id qualifierId, boolean isActiveNow) {
        Vector authz = new Vector(100);
        
        //  Iterate over all known authorizations.
        for (int i=0; i < auths.size(); i++) {
            VueAuthorization az = (VueAuthorization) auths.elementAt(i);
            
            //  If the authorization has the agent, function and qualifier, and is active, then add it to our final list.
            if (az.hasAgent(agentId) && az.hasFunction(functionId) && az.hasQualifierOrAncestor(qualifierId)) {
                if (isActiveNow && az.isActiveNow()) {
                    VueAuthorization azCopy = (VueAuthorization) az.clone();
                    authz.add (azCopy);
                }
                else if (isActiveNow == false) {
                    VueAuthorization azCopy = (VueAuthorization) az.clone();
                    authz.add (az);
                }
            }
        }
        return (osid.authorization.AuthorizationIterator) new VueAuthorizationIterator (authz);
    }
    
    /**
     *  Returns all authorizations with the agent id, function type and qualifier id given.
     *  If the active flag is true, the authorizations must be within the activation dates.
     *
     *  @author Mark Norton
     */
    //  agent-id, function-type, qualifier-id, active
    public osid.authorization.AuthorizationIterator getAllAZsByFuncType(osid.shared.Id agentId, osid.shared.Type functionType, osid.shared.Id qualifierId, boolean isActiveNow) {
        Vector authz = new Vector(100);
        
        //  Iterate over all known authorizations.
        for (int i=0; i < auths.size(); i++) {
            VueAuthorization az = (VueAuthorization) auths.elementAt(i);
            
            //  If the authorization has the agent, function and qualifier, and is active, then add it to our final list.
            if (az.hasAgent(agentId) && az.hasFunctionType(functionType) && az.hasQualifierOrAncestor(qualifierId)) {
                if (isActiveNow && az.isActiveNow()) {
                    VueAuthorization azCopy = (VueAuthorization) az.clone();
                    authz.add (azCopy);
                }
                else if (isActiveNow == false) {
                    VueAuthorization azCopy = (VueAuthorization) az.clone();
                    authz.add (az);
                }
            }
        }
        return (osid.authorization.AuthorizationIterator) new VueAuthorizationIterator (authz);
    }
    
    /**
     *  Returns all authorizations with the function id and qualifier id given.
     *  If the active flag is true, the authorizations must be within the activation dates.
     *
     *  @author Mark Norton
     */
    //  function-id, qualifier-id, active
    public osid.authorization.AuthorizationIterator getAllUserAZs(osid.shared.Id functionId, osid.shared.Id qualifierId, boolean isActiveNow) {
        Vector authz = new Vector(100);
        
        //  Iterate over all known authorizations.
        for (int i=0; i < auths.size(); i++) {
            VueAuthorization az = (VueAuthorization) auths.elementAt(i);
            
            //  If the authorization has the function and qualifier, and is active, then add it to our final list.
            if (az.hasFunction(functionId) && az.hasQualifierOrAncestor(qualifierId)) {
                if (isActiveNow && az.isActiveNow()) {
                    VueAuthorization azCopy = (VueAuthorization) az.clone();
                    authz.add (azCopy);
                }
                else if (isActiveNow == false) {
                    VueAuthorization azCopy = (VueAuthorization) az.clone();
                    authz.add (az);
                }
            }
        }
        return (osid.authorization.AuthorizationIterator) new VueAuthorizationIterator (authz);
    }
    
    /**
     *  Returns all authorizations with the function type and qualifier id given.
     *  If the active flag is true, the authorizations must be within the activation dates.
     *
     *  @author Mark Norton
     */
    //  agent-id, funtion-type, qualifier-id, active
    public osid.authorization.AuthorizationIterator getAllUserAZsByFuncType(osid.shared.Type functionType, osid.shared.Id qualifierId, boolean isActiveNow) {
        Vector authz = new Vector(100);
        
        //  Iterate over all known authorizations.
        for (int i=0; i < auths.size(); i++) {
            VueAuthorization az = (VueAuthorization) auths.elementAt(i);
            
            //  If the authorization has the function and qualifier, and is active, then add it to our final list.
            if (az.hasFunctionType(functionType) && az.hasQualifierOrAncestor(qualifierId)) {
                if (isActiveNow && az.isActiveNow()) {
                    VueAuthorization azCopy = (VueAuthorization) az.clone();
                    authz.add (azCopy);
                }
                else if (isActiveNow == false) {
                    VueAuthorization azCopy = (VueAuthorization) az.clone();
                    authz.add (az);
                }
            }
        }
        return (osid.authorization.AuthorizationIterator) new VueAuthorizationIterator (authz);
    }
    
    /**
     *  Returns all explicit authorizations with the agent id, function id and qualifier id given.
     *  If the active flag is true, the authorizations must be within the activation dates.
     *
     *  @author Mark Norton
     */
    //  agent-id, function-id, qualifier-id, active
    public osid.authorization.AuthorizationIterator getExplicitAZs(osid.shared.Id agentId, osid.shared.Id functionId, osid.shared.Id qualifierId, boolean isActiveNow) {
        Vector authz = new Vector(100);
        
        //  Iterate over all known authorizations.
        for (int i=0; i < auths.size(); i++) {
            VueAuthorization az = (VueAuthorization) auths.elementAt(i);
            
            //  If the authorization has the agent, function and qualifier, and is active, then add it to our final list.
            if (az.hasAgent(agentId) && az.hasFunction(functionId) && az.hasQualifier(qualifierId)) {
                if (isActiveNow && az.isActiveNow())
                    authz.add (az);
                else if (isActiveNow == false)
                    authz.add (az);
            }
        }
        return (osid.authorization.AuthorizationIterator) new VueAuthorizationIterator (authz);
    }
    
    /**
     *  Returns all explicit authorizations with the agent id, function type and qualifier id given.
     *  If the active flag is true, the authorizations must be within the activation dates.
     *
     *  @author Mark Norton
     */
    //  agent-id, function-type, qualifier-id, active
    public osid.authorization.AuthorizationIterator getExplicitAZsByFuncType(osid.shared.Id agentId, osid.shared.Type functionType, osid.shared.Id qualifierId, boolean isActiveNow) {
        Vector authz = new Vector(100);
        
        //  Iterate over all known authorizations.
        for (int i=0; i < auths.size(); i++) {
            VueAuthorization az = (VueAuthorization) auths.elementAt(i);
            
            //  If the authorization has the agent, function and qualifier, and is active, then add it to our final list.
            if (az.hasAgent(agentId) && az.hasFunctionType(functionType) && az.hasQualifier(qualifierId)) {
                if (isActiveNow && az.isActiveNow())
                    authz.add (az);
                else if (isActiveNow == false)
                    authz.add (az);
            }
        }
        return (osid.authorization.AuthorizationIterator) new VueAuthorizationIterator (authz);
    }
    
    /**
     *  Returns all explicit authorizations with the function id and qualifier id given.
     *  If the active flag is true, the authorizations must be within the activation dates.
     *
     *  @author Mark Norton
     */
    //  funtion-id, qualifier-id, active
    public osid.authorization.AuthorizationIterator getExplicitUserAZs(osid.shared.Id functionId, osid.shared.Id qualifierId, boolean isActiveNow) {
        Vector authz = new Vector(100);
        
        //  Iterate over all known authorizations.
        for (int i=0; i < auths.size(); i++) {
            VueAuthorization az = (VueAuthorization) auths.elementAt(i);
            
            //  If the authorization has the function and qualifier, and is active, then add it to our final list.
            if (az.hasFunction(functionId) && az.hasQualifier(qualifierId)) {
                if (isActiveNow && az.isActiveNow())
                    authz.add (az);
                else if (isActiveNow == false)
                    authz.add (az);
            }
        }
        return (osid.authorization.AuthorizationIterator) new VueAuthorizationIterator (authz);
    }
    
    /**
     *  Returns all explicit authorizations with the function type and qualifier id given.
     *  If the active flag is true, the authorizations must be within the activation dates.
     *
     *  @author Mark Norton
     */
    //  function-type, qualifier-id, active
    public osid.authorization.AuthorizationIterator getExplicitUserAZsByFuncType(osid.shared.Type functionType, osid.shared.Id qualifierId, boolean isActiveNow) {
        Vector authz = new Vector(100);
        
        //  Iterate over all known authorizations.
        for (int i=0; i < auths.size(); i++) {
            VueAuthorization az = (VueAuthorization) auths.elementAt(i);
            
            //  If the authorization has the function and qualifier, and is active, then add it to our final list.
            if (az.hasFunctionType(functionType) && az.hasQualifier(qualifierId)) {
                if (isActiveNow && az.isActiveNow())
                    authz.add (az);
                else if (isActiveNow == false)
                    authz.add (az);
            }
        }
        return (osid.authorization.AuthorizationIterator) new VueAuthorizationIterator (authz);
    }
    
    /**
     *  Get all the authorizations for a specified user agent.
     *  <p>
     *  Note that this method is an extension to osid.authorization.
     *
     *  @author Mark Norton
     */
    public osid.authorization.AuthorizationIterator getAllExplicitAZs(osid.shared.Id agentId, boolean isActiveNow) {
        Vector authz = new Vector(100);
        
        //  Iterate over all known authorizations.
        System.out.println ("getAllExplicitAZs:  Authorization count: " + auths.size());
        for (int i=0; i < auths.size(); i++) {
            VueAuthorization az = (VueAuthorization) auths.elementAt(i);
            
            //  If the authorization has the agent and is active, then add it to our final list.
            if (az.hasAgent(agentId)) {
                if (isActiveNow && az.isActiveNow())
                    authz.add (az);
                else if (isActiveNow == false)
                    authz.add (az);
            }
        }
        return (osid.authorization.AuthorizationIterator) new VueAuthorizationIterator (authz);
    }

    /**
     *  Return the Function given an function Id.
     *
     *  @author Mark Norton
     */
    public osid.authorization.Function getFunction(osid.shared.Id functionId) {
        osid.authorization.Function foundFtn = null;
        VueFunctionIterator it = new VueFunctionIterator (functions);
        while (it.hasNext()) {
            osid.authorization.Function ftn = it.next();
            osid.shared.Id id = ((VueFunction)ftn).getId();
            try {
                if (id.isEqual(functionId)) {
                    foundFtn = ftn;
                    break;
                }
           }
           catch (osid.shared.SharedException ex) {}
        }
        return foundFtn;
    }
    
    /**
     *  Return an iterator which lists the known function types.
     *
     *  @author Mark Norton
     */
    public osid.shared.TypeIterator getFunctionTypes() {
        return new tufts.oki.shared.TypeIterator (tufts.oki.authorization.VueFunctionType.functionTypes);
    }
    
    /**
     *  Return an iterator which lists the currently defined functions.
     *
     *  @author Mark Norton
     */
    public osid.authorization.FunctionIterator getFunctions(osid.shared.Type functionType) {
        return new VueFunctionIterator (functions);
    }
    
    /**
     *  Return the qualifier who's id is given.
     *
     *  @author Mark Norton
     */
    public osid.authorization.Qualifier getQualifier(osid.shared.Id qualifierId) {
        osid.authorization.Qualifier foundQual = null;
        VueQualifierIterator it = new VueQualifierIterator (qualifiers);
        while (it.hasNext()) {
            osid.authorization.Qualifier qual = it.next();
            osid.shared.Id id = ((VueQualifier)qual).getId();
            try {
                if (id.isEqual(qualifierId)) {
                    foundQual = qual;
                    break;
                }
            }
            catch (osid.shared.SharedException ex) {}
        }
        return foundQual;
    }
    
    /**
     *  Return an interator for the children of the qualfier given by it's Id.
     *
     *  @author Mark Norton
     */
    public osid.authorization.QualifierIterator getQualifierChildren(osid.shared.Id qualifierId) {
        Vector children = new Vector(10);
        for (int i = 0; i < this.qualifiers.size(); i++) {
            VueQualifier qual = (VueQualifier) qualifiers.elementAt(i);
            if (qual.isChildOf (qualifierId)) {
                children.add (qual);
            }
        }
        return (osid.authorization.QualifierIterator) new VueQualifierIterator (children);
    }
    
    /**
     *  Return an iterator for all descendants of the qualfier given by it's Id.
     *
     *  @author Mark Norton
     */
    public osid.authorization.QualifierIterator getQualifierDescendants(osid.shared.Id qualifierId) {
        Vector children = new Vector(10);
        for (int i = 0; i < this.qualifiers.size(); i++) {
            VueQualifier qual = (VueQualifier) qualifiers.elementAt(i);
            if (qual.isDescendantOf (qualifierId)) {
                children.add (qual);
            }
        }
        return (osid.authorization.QualifierIterator) new VueQualifierIterator (children);
    }
    
    /**
     *  Return an iterator for the list of root qualifier hierarchy Ids.
     *
     *  @author Mark Norton
     */
    public osid.shared.IdIterator getQualifierHierarchies() {
        Vector roots = new Vector (rootQualifiers.size());
        for (int i=0; i < rootQualifiers.size(); i++) {
            VueQualifier rt = (VueQualifier) rootQualifiers.elementAt(i);
            roots.add (rt.getId());
        }
        return new tufts.oki.shared.IdIterator (roots);
    }
    
    /**
     *  Return an iterator for the list of known qualifier types.
     *
     *  @author Mark Norton
     */
    public osid.shared.TypeIterator getQualifierTypes() {
        return new tufts.oki.shared.TypeIterator (tufts.oki.authorization.VueQualifierType.qualifierTypes);
    }
    
    /**
     *  This method is intended to return the roots associated with a particular hiearchy.  Since
     *  this implementation doesn't use osid.hierarchy, it returns an iterator for the known
     *  qualifier roots.  The qualifierHierarchyId is ignored and may be passed as null.
     *
     *  @author Mark Norton
     */
    public osid.authorization.QualifierIterator getRootQualifiers(osid.shared.Id qualifierHierarchyId) {
        return (osid.authorization.QualifierIterator) new VueQualifierIterator (this.rootQualifiers);
    }
    
    /**
     *  Return an iterator which lists all agents who are authorized to perform the function on the qualifier
     *  given.  The isActive flag is used to determine if all authorizations or only those which are active
     *  should be included.
     *
     *  @author Mark Norton
     */
    public osid.shared.AgentIterator getWhoCanDo(osid.shared.Id functionId, osid.shared.Id qualifierId, boolean isActiveNow) throws osid.authorization.AuthorizationException {
        //  Get all authorizations for this function, and qualifier.
        VueAuthorizationIterator it = (VueAuthorizationIterator) this.getAllUserAZs (functionId, qualifierId, isActiveNow);

        //  Iterate over those authorizations and extract a unique list of agents.
        Vector agents = new Vector(100);
        while (it.hasNext()) {
            VueAuthorization az = (VueAuthorization) it.next();
            osid.shared.Agent ag = az.getAgent();
            if (agents.indexOf (ag) == -1)
                agents.add(ag);
        }
        return new tufts.oki.shared.AgentIterator(agents);
    }
    
    /**
     *  Return true if the agent provided is currently authorized to perform the function on
     *  the qualifer provided.
     *
     *  @author Mark Norton
     */
    public boolean isAuthorized(osid.shared.Id agentId, osid.shared.Id functionId, osid.shared.Id qualifierId) {
        //  Get all authorizations for this agent, function, and qualifier currently active.
        VueAuthorizationIterator it = (VueAuthorizationIterator) this.getAllAZs(agentId, functionId, qualifierId, true);
        
        //  If there is one or more, then the agent is authorized.
        if (it.hasNext())
            return true;
        else
            return false;
    }
    
    /**
     *  Return true if the default user is currently authorized to perform the function on
     *  the qualifer provided.
     *
     *  @author Mark Norton
     */
    public boolean isUserAuthorized(osid.shared.Id functionId, osid.shared.Id qualifierId) throws osid.authorization.AuthorizationException {
        osid.shared.Id userId = null;
        
        //  Check to see that there is a valid default user agent.
        if (this.user == null)
            throw new osid.authorization.AuthorizationException ("Unknown default user.");
        
        //  If so, use it to see if that agent is authorized to perform the function on the qualifier.
        else {
            try {
                userId = this.user.getId();
            }
            catch (osid.shared.SharedException ex) {}
            return isAuthorized (userId, functionId, qualifierId);
        }
    }

}
