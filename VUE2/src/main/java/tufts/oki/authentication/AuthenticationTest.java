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
 * AuthenticationTest.java
 *
 * Created on October 29, 2003, 2:32 PM
 */

package tufts.oki.authentication;
import java.io.*;
import tufts.oki.shared.*;

/**
 *
 * @author  Mark Norton
 */
public class AuthenticationTest {
    
    /** Creates a new instance of AuthenticationTest */
    public AuthenticationTest() {
    }
    
    /**
     *  Read a line from stdin.
     */
    public static String readline() throws IOException
    {
	BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

	return in.readLine();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws java.io.IOException {
        System.out.println ("Test of Tufts LDAP User Authentication");
        System.out.println ("--------------------------------------");
        
        AuthenticationManager sm = null;
        try {
            String key = "myKey";
            osid.OsidOwner myOwner = new osid.OsidOwner();
            myOwner.addContext(key, "osid_mjn");
            //sm = (AuthenticationManager) osid.OsidLoader.getManager("osid.authentication.AuthenticationManager", "src.oki.authentication", myOwner);      
            sm = new AuthenticationManager (myOwner);
        }
        catch (osid.OsidException e) {
            System.out.println ("Error when loading the authentication manager.");
            e.printStackTrace();
            System.exit(0);
        }
        
        String username = null;
        while (true) {
            System.out.println ("");
            System.out.print ("User name: ");
            username = readline();
            
            if (username.length() == 0)
                System.exit(0);
            else {
                sm.setUsername(username);
                try {
                    sm.authenticateUser(new AuthenticationLDAP());
                    //System.out.println ("tenative authentication.");
                }
                catch (osid.authentication.AuthenticationException ex) {
                    System.out.println (username + " is not authenticated.");
                    continue;
                }
                osid.shared.Agent agent = sm.getAgent(new AgentPersonType());
                if (agent == null) {
                    System.out.println ("Agent is null.");
                }
                else {
                    try {
                        String dispName = agent.getDisplayName();
                        System.out.println (dispName + " is LDAP authenticated.");
                    } catch (osid.shared.SharedException ex2) {}
                }
            }
        }
    }
    
}
