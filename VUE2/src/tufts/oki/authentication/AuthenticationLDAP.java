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

package tufts.oki.authentication;

/*
 * AuthenticationLDAP.java
 *
 * Created on October 23, 2003, 3:20 PM
 */

/**
 *  The AuthenticationLDAP type is used to indicate that the user validation information
 *  can be found in an LDAP system maintained by this organization.
 *
 *  @author  Mark Norton
 */
public class AuthenticationLDAP extends osid.shared.Type {
    /* Use this keyword to search for the person type.  */
    public static final String AUTHN_LDAP_KEY = "osid.authentication.ldap";
    
    
    /** Creates a new instance of AuthenticationLDAP */
    public AuthenticationLDAP() {
        super ("osid.authentication", tufts.oki.OsidManager.AUTHORITY, AUTHN_LDAP_KEY, "This authentication type uses enterprise LDAP information to determine user validity.");
    }
    
}
