/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
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
