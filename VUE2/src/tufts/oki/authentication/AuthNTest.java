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
 * AuthNTest.java
 *
 * Created on January 4, 2004, 3:35 PM
 */

package tufts.oki.authentication;
import java.util.*;
import javax.naming.*;
import javax.naming.directory.*;
/**
 *
 * @author  Mark Norton
 */
public class AuthNTest {
    
    /** Creates a new instance of AuthNTest */
    public AuthNTest() {
    }
    
    /**
     * @param args the command line arguments
     *
     *  Takes a username as args[0] and a password as args[1]
     */
    public static void main(String[] args) throws javax.naming.NamingException {
        System.out.println ("LDAP Authentication Explorer");
        System.out.println ("----------------------------\n");

        java.util.Properties props = new java.util.Properties();
        
        //  Set up the properties needed to establish an LDAP context.
        props.put (Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        props.put (Context.PROVIDER_URL, "ldaps://ldap.tufts.edu/");
        
        /* Create the initial directory context. */
        DirContext ctx = new InitialDirContext (props);
        //System.out.println ("Initial context gained.");

        /* Specify search constraints to search subtree */
        SearchControls constraints = new SearchControls();
        constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);

        /* Search for an entry with a user name given by am_username.  List of SearchResult. */
        NamingEnumeration results = ctx.search("dc=tufts,dc=edu", "uid="+args[0], constraints);

        //  If the results are null, this is not a valid user.
        if (!results.hasMore()) {
            System.out.println (args[0] + " is not a valid user.");
        }
        else {
            System.out.println (args[0] + " is found via LDAP search.");
            SearchResult rslt = (SearchResult) results.next();
            
            //  Check to see if there are multiple results.
            if (results.hasMore()) {
                System.out.println ("More than one search result, authentication invalid.");
                System.exit(0);
            }
            
            //  Get the search result attributes.
            Attributes attrs = rslt.getAttributes();

            System.out.println ("Attributes returned by the search:");
            NamingEnumeration ne = attrs.getIDs();
            while (ne.hasMore()) {
                String id = (String) ne.next();
                System.out.println ("\t"+id);
            }
            
            Attribute attr = attrs.get ("dn");
            if (attr == null)
                System.out.println ("Unable to retrieve dn attribute.");
        }
    }
    
}
