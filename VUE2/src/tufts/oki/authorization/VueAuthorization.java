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
 * VueAuthorization.java
 *
 * Created on December 12, 2003, 4:38 PM
 */

package tufts.oki.authorization;
import osid.shared.*;
import osid.authorization.*;
import java.util.*;

/**
 *  An authorization is a tuple consisting of an agent, a function, and a qualifier.
 *  These objects define who can-do what.  The agent defines who.  A function is a
 *  specific instance of a function type and describes an operation that can be performed.
 *  A qualifier defines what the function can be performed on.  Qualifiers can be 
 *  hierarcally defined, thus allowing groups of thing can be be operated on.  Each
 *  authorization has an effectiveDate and an expirationDate.  The authorization is said
 *  to be active if the current time is between these times.  Finally, there are two
 *  kinds of authorizations:  explicit and implicit.  An authorization is explicit if
 *  it direction references a particular function and qualifier.  It is implicit if it
 *  references a particular function and a qualifier or a qualifier which is an ancestor
 *  of the one provided.  Only explicit authorizations may be modified.
 *
 *  @author  Mark Norton
 */
public class VueAuthorization implements osid.authorization.Authorization {
    private boolean explicit = false;
    private osid.shared.Id id = null;
    private osid.shared.Id agentId = null;
    private osid.shared.Id modifiedById = null;
    private VueFunction function = null;
    private VueQualifier qualifier = null;
    private Calendar effectiveDate = null;
    private Calendar expirationDate = null;
    private Calendar modifiedDate = null;
    
    /** 
     *  Creates a new instance of VueAuthorization
     *
     *   @author Mark Norton
     */
    public VueAuthorization(osid.shared.Id authId, osid.shared.Id agent, osid.authorization.Function function, osid.authorization.Qualifier qualifier, Calendar effectiveDate, Calendar expirationDate, boolean explicit) {
        this.id = authId;
        this.agentId = agent;
        this.function = (VueFunction) function;
        this.qualifier = (VueQualifier) qualifier;
        this.effectiveDate = effectiveDate;
        this.expirationDate = expirationDate;
        this.explicit = explicit;
    }
    
    /**
     *  Return a copy of this authorization.  Copies made by this method always have the
     *  explicit flag set to false, since this is primarily used to create implicit
     *  authorizations.
     *
     *  @author Mark Norton
     */
    public Object clone() {
        return new VueAuthorization (this.id, this.agentId, this.function, this.qualifier, this.effectiveDate, this.expirationDate, false);
    }
    
    /**
     *  Get and return the agent associated with this authorization.
     *
     *  @author Mark Norton
     */
    public osid.shared.Agent getAgent() throws osid.authorization.AuthorizationException {
        osid.shared.SharedManager mgr = tufts.oki.authorization.AuthorizationManager.sharedMgr;
        osid.shared.Agent ag = null;
        try {
            ag = mgr.getAgent (this.agentId);
        }
        catch (osid.shared.SharedException ex) {
            throw new osid.authorization.AuthorizationException ("Agent not found in Shared Manager");
        }
        return ag;
    }
    
    /**
     *  Return the effective starting date for this authorization.
     *
     *  @author Mark Norton
     */
    public java.util.Calendar getEffectiveDate() {
        return effectiveDate;
    }
    
    /**
     *  Return the expiration date for this authorization.
     *
     *  @author Mark Norton
     */
    public java.util.Calendar getExpirationDate() {
        return expirationDate;
    }
    
    /**
     *  Return the function associated with this authorization.
     *
     *  @author Mark Norton
     */
    public osid.authorization.Function getFunction() {
        return function;
    }
    
    /**
     *  Return the agent who last modified this authorization.  Although a great deal of 
     *  work is done here to find the agent associated with the Id, the modifying agent is
     *  never known under the current implementation.  It is not passed into either
     *  updateEffectiveDate() or updateExpirationDate(), which are the two methods which
     *  can modify an authorization.  If the Id null, null is returned.  Caveat Emptor.
     *
     *  @author Mark Norton
     */
    public osid.shared.Agent getModifiedBy() throws osid.authorization.AuthorizationException {
        if (this.modifiedById == null)
            return null;
        osid.shared.SharedManager mgr = tufts.oki.authorization.AuthorizationManager.sharedMgr;
        osid.shared.Agent ag = null;
        try {
            ag = mgr.getAgent (this.modifiedById);
        }
        catch (osid.shared.SharedException ex) {
            throw new osid.authorization.AuthorizationException ("Agent not found in Shared Manager");
        }
        return ag;
    }
    
    /**
     *  Return the modified date.
     *
     *  @author Mark Norton
     */
    public java.util.Calendar getModifiedDate() {
        return modifiedDate;
    }
    
    /**
     *  Return the qualifier associated with this authorization.
     *
     *  @author Mark Norton
     */
    public osid.authorization.Qualifier getQualifier() {
        return qualifier;
    }
    
    /**
     *  Return true if the current date and time is greater than the effectiveDate and
     *  less than the expiration date.
     *
     *  @author Mark Norton
     */
    public boolean isActiveNow() {
        //  If either dates are null, assume always valid.
        if ((effectiveDate == null) || (expirationDate == null))
            return true;
        
        //  Get the current date/time.
        Calendar now = Calendar.getInstance();
        now.setTimeInMillis (System.currentTimeMillis());
        
        //  If now is between effectiveDate and expirationDate, it is active.
        if (now.after(effectiveDate) && now.before(expirationDate))
            return true;
        else
            return false;
    }
    
    /**
     *  Return the value of the explicit flag.  Only explicit authorizations can be 
     *  modified.  Explicit authorizations are those created as new by the authorization
     *  manager.  Implicit authorizations are copies of explicit ones which are made
     *  valid due to an inherited qualifier.  They may not be modififed.
     */
    public boolean isExplicit() {
        return this.explicit;
    }
    
    /**
     *  Change the effective date to the new value.  This is permitted only if this
     *  authorization is explicit.  Modified date is updated.
     *
     *  @author Mark Norton
     */
    public void updateEffectiveDate(java.util.Calendar effectiveDate) throws osid.authorization.AuthorizationException {
        if (this.explicit == false)
            throw new osid.authorization.AuthorizationException (osid.authorization.AuthorizationException.PERMISSION_DENIED);
        this.effectiveDate = effectiveDate;
        this.modifiedDate = Calendar.getInstance();
        this.modifiedDate.setTimeInMillis (System.currentTimeMillis());
    }
    
    /**
     *  Change the expiration date to the new value.  This is permitted only if this
     *  authorization is explicit.  Modified date is updated.
     *
     *  @author Mark Norton
     */
    public void updateExpirationDate(java.util.Calendar expirationDate) throws osid.authorization.AuthorizationException {
        if (this.explicit == false)
            throw new osid.authorization.AuthorizationException (osid.authorization.AuthorizationException.PERMISSION_DENIED);
        this.expirationDate = expirationDate;
        this.modifiedDate = Calendar.getInstance();
        this.modifiedDate.setTimeInMillis (System.currentTimeMillis());
    }
    
    /**
     *  Return the Id of this authorization.
     *
     *  @author Mark Norton
     */
    public osid.shared.Id getId () {
        return id;
    }
    
    /**
     *  Return true if an agent of the given Id is present in this authorization.
     *
     *  @author Mark Norton
     */
    public boolean hasAgent (osid.shared.Id agentId) {
        try {
            if (agentId.isEqual(this.agentId))
                return true;
        }
        catch (osid.shared.SharedException ex) {}
        return false;
    }
    
    /**
     *  Return true if a function of the given Id is present in this authorization.
     *
     *  @author Mark Norton
     */
    public boolean hasFunction (osid.shared.Id functionId) {
        try {
            if (functionId.isEqual(function.getId()))
                return true;
        }
        catch (osid.shared.SharedException ex) {}
        return false;
    }
    
    /**
     *  Return true if the type of the function contained in this authorization
     *  matches the one provided.
     *
     *  @author Mark Norton
     */
    public boolean hasFunctionType (osid.shared.Type functionType) {
        if (functionType.isEqual(function.getFunctionType()))
            return true;
        return false;
    }
    
    /**
     *  Return true if a qualifier of the given Id is present in this authorization.
     *
     *  @author Mark Norton
     */
    public boolean hasQualifier (osid.shared.Id qualifierId) {
        try {
            if (qualifierId.isEqual(qualifier.getId()))
                return true;
        }
        catch (osid.shared.SharedException ex) {}
        return false;
    }
    
    /**
     *  Return true if a qualifier of the given Id is present in this authorization or
     *  is an ancestor of the qualfier in this authorization.
     *
     *  @author Mark Norton
     */
    public boolean hasQualifierOrAncestor (osid.shared.Id qualifierId) {
        //  Check to see if qualifier is present in this authorization.
        if (hasQualifier (qualifierId))
            return true;
        
        //  Otherwise check to see if qualifierId is an ancestor of this qualifier.
        else
            return this.qualifier.isDescendantOf(qualifierId);
    }
    
}
