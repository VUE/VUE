/*
 * Copyright 2003-2007 Tufts University  Licensed under the
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
 * FTPFilingTest3.java
 *
 * Created on October 30, 2003, 10:50 AM
 */

package tufts.oki.remoteFiling;
import java.io.*;
import org.apache.commons.net.ftp.*;

/**
 *
 * @author  Mark Norton
 */
public class RemoteFilingTest {
    private static RemoteFilingManager fm = null; //  The FTP filing manager.
    
    /** Creates a new instance of FTPFilingTest3 */
    public RemoteFilingTest() {
    }
    
    public static String readline() throws IOException
    {
	BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

	return in.readLine();
    }
    
    public static void printFilesInCurrent () throws java.io.IOException, osid.filing.FilingException {
        osid.filing.CabinetEntryIterator it = fm.list();
        System.out.println ("Entries in " + (fm.getWorkingDirectory()).getFullName());
        while (it.hasNext()) {
            RemoteCabinetEntry entry = (RemoteCabinetEntry) it.next();
            //String pathname = path.getPathString();
            
            if (entry instanceof RemoteCabinet) {
                //System.out.println ("\tDir:  " + entry.getDisplayName() + "\t==>" + pathname);
                System.out.println ("\tDir:  " + entry.getDisplayName());
            }
            else if (entry instanceof RemoteByteStore) {
                //System.out.println ("\tFile:  " + entry.getDisplayName() + "\t==>" + pathname);
                System.out.println ("\tFile:  " + entry.getDisplayName());
            }
            else
                System.out.println ("\tUnknown entry type.");
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws java.io.IOException, osid.filing.FilingException {
        String server = null;       //  Name of the FTP server.
        String username = null;     //  User name.
        String password = null;     //  Password.
        
        System.out.println ("Remote Filing System Test using FTP");
        System.out.println ("-----------------------------------");
        System.out.println ("");
        
        //  Get the server name, user name, and password.
        /*
        System.out.print ("Enter FTP server name: ");
        server = readline();
        System.out.print ("Enter user name: ");
        username = readline();
        System.out.print ("Enter password: ");
        password = readline();
         */
        server = "ftp.org";
        username = "ftpUser";
        password = "password";
        
        //  Create a filing manager and make an FTP client.

        fm = new RemoteFilingManager();
        fm.createClient(server, username, password);
        //FTPClient client = rc.getClient();
        
        //  Print the root base to confirm connection.
       // System.out.println ("Root Base: " + rc.getRootBase());
        
        //  Open the root directory and list it's contents.
        printFilesInCurrent ();

        //  Get the current working directory.
        RemoteCabinet cwd = fm.getWorkingDirectory();
        //System.out.println ("Current working directory: " + client.printWorkingDirectory());
        
        //  Change the current working directory.
        fm.setWorkingDirectory ("www");
        //System.out.println ("Current working directory: " + client.printWorkingDirectory());
        //printFilesInCurrent ();
        fm.setWorkingDirectory ("consult");
       // System.out.println ("Current working directory: " + client.printWorkingDirectory());
        //printFilesInCurrent ();

        cwd = fm.getWorkingDirectory();

        //  Get the byte store for file to download.
        RemoteByteStore bs = (RemoteByteStore) cwd.getCabinetEntryByName("ftptest.txt");
        System.out.println ("Byte store to retrieve: " + bs.getDisplayName());
        System.out.println ("Size of byte store (should be 56): " + bs.length());
        System.out.println ("Size of byte store (second try): " + bs.length());
        
        //  Download the bytes in remote file.
        System.out.println ("Get bytes to display.");
        byte[] buf = bs.getBytes();
        System.out.println ("Contents of file - size is: " + buf.length);
        for (int i=0; i<buf.length; i++)
            System.out.print ((char)buf[i]);
        
        System.out.println ("Done!");
    }
}
