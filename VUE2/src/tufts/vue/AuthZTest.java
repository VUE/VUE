/*
* Copyright 2003-2010 Tufts University  Licensed under the
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
 * AuthZTest.java
 *
 * Created on December 19, 2003, 10:52 AM
 */

package tufts.vue;
import java.io.*;

/**
 *
 * @author  Mark Norton
 */
public class AuthZTest {
    private static TuftsDLAuthZ tuftsDL = null;
    
    /** Creates a new instance of AuthZTest */
    public AuthZTest() {
    }
    
    public static String readline() throws IOException
    {
	BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

	return in.readLine();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws osid.OsidException {
        osid.shared.Agent user = null;
        
        System.out.println ("Test of Tufts Digital Lbirary Authorization");
        System.out.println ("-------------------------------------------\n");
        
        System.out.println ("User name given: " + args[0]);
        System.out.println ("Password given:  " + args[1]);
        
        //  Create a Tufts DL Authorization object.
        try {
            tuftsDL = new TuftsDLAuthZ ();
            user = tuftsDL.authorizeUser (args[0], args[1]);
        }
        catch (osid.OsidException ex) {
            ex.printStackTrace();
        }
        if (user != null) {
            System.out.println ("Valided user is: "+user.getDisplayName());
            System.out.println ("User is of type: "+user.getType().getKeyword());

            //  Show all the authorizations for this user.
            System.out.println ("Authorizations for this user:  ");
            osid.authorization.AuthorizationIterator it = tuftsDL.getAuthorizations(user);
            int ct = 0;
            while (it.hasNext()) {
                osid.authorization.Authorization auth = it.next();
                osid.authorization.Function ftn = auth.getFunction();
                osid.shared.Type ftnType = ftn.getFunctionType();
                osid.authorization.Qualifier qual = auth.getQualifier();
                osid.shared.Type qualType = qual.getQualifierType();
                System.out.println ("\t"+ct+":  "+ftnType.getKeyword()+" on "+qualType.getKeyword());
                ct++;
            }
            if (ct == 0)
                System.out.println ("\t(none)");
            else {
                System.out.println ("");
                
                if (tuftsDL.isAuthorized (user, TuftsDLAuthZ.AUTH_SEARCH))
                    System.out.println ("User is authorized for search.");
                else
                    System.out.println ("User is not authorized for search.");

                if (tuftsDL.isAuthorized (user, TuftsDLAuthZ.AUTH_VIEW))
                    System.out.println ("User is authorized for view.");
                else
                    System.out.println ("User is not authorized for view.");
                
                if (tuftsDL.isAuthorized (user, TuftsDLAuthZ.AUTH_UPDATE))
                    System.out.println ("User is authorized for update.");
                else
                    System.out.println ("User is not authorized for update.");
                
                if (tuftsDL.isAuthorized (user, TuftsDLAuthZ.AUTH_ADD))
                    System.out.println ("User is authorized for add.");
                else
                    System.out.println ("User is not authorized for add.");
                
                if (tuftsDL.isAuthorized (user, TuftsDLAuthZ.AUTH_DELETE))
                    System.out.println ("User is authorized for delete.");
                else
                    System.out.println ("User is not authorized for delete.");

                if (tuftsDL.isAuthorized (user, TuftsDLAuthZ.AUTH_UPDATE))
                    System.out.println ("User is authorized to update owned asset.");
                else
                    System.out.println ("User is not authorized to update owned asset.");
            }
        }
        else
            System.out.println (args[0]+" is not an authenticated user.");
    }
    
}
