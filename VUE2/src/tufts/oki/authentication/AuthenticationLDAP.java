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
