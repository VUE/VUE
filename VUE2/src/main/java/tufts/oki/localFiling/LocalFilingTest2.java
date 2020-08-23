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
 * FTPFilingTest3.java
 *
 * Created on October 30, 2003, 10:50 AM
 */

package tufts.oki.localFiling;
import java.lang.*;
import java.io.*;
import tufts.oki.shared.*;

/**
 *  Implements a set of tests on the LocalFilingManager and related classes.  These tests
 *  use the following directory and file structures.  Add them to your LOCAL_ROOT (see below):
 *  <blockquote>
 *  osidtest
 *  &nbsp;&nbsp;dir1
 *  &nbsp;&nbsp;&nbsp;&nbsp;dir2
 *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;file1.txt
 *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;file2.txt
 *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;dir3  (empty)
 *  &nbsp;&nbsp;&nbsp;&nbsp;file3.txt
 *  &nbsp;&nbsp;file4.txt
 *  </blockquote>
 *  <p>
 *  The following tests are implemented:<br>
 *  1.  Interactive file access test.
 *  2.  Directory test.
 * @author  Mark Norton
 */
public class LocalFilingTest2 {
    public static final String LOCAL_ROOT = "C:\\";
    static private LocalFilingManager lm = null; //  The local filing manager.
    
    /** Creates a new instance of FTPFilingTest3 */
    public LocalFilingTest2() {
    }
    
    public static String readline() throws IOException
    {
	BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

	return in.readLine();
    }

    public static void printFilesInCurrent () throws java.io.IOException, osid.filing.FilingException {
        osid.filing.CabinetEntryIterator it = lm.list();
        System.out.println ("Entries in " + (lm.getWorkingDirectory()).getFullName());
        while (it.hasNext()) {
            LocalCabinetEntry entry = (LocalCabinetEntry) it.next();
            //String pathname = path.getPathString();
            
            if (entry instanceof LocalCabinet) {
                //System.out.println ("\tDir:  " + entry.getDisplayName() + "\t==>" + pathname);
                System.out.println ("\tDir:  " + entry.getDisplayName());
            }
            else if (entry instanceof LocalByteStore) {
                //System.out.println ("\tFile:  " + entry.getDisplayName() + "\t==>" + pathname);
                System.out.println ("\tFile:  " + entry.getDisplayName());
            }
            else
                System.out.println ("\tUnknown entry type.");
        }
    }

    /**
     *  Create a LocalFilingManager and test to see that local roots are intialized.
     */
    public static void test0 () throws java.io.IOException, osid.filing.FilingException {
        System.out.println ("Test 0:  Root initialization test.\n");
        
        //  Create a filing manager and make an FTP client.
        lm = new LocalFilingManager();
        
        System.out.println ("Roots present in system: ");
        osid.filing.CabinetEntryIterator it = lm.listRoots();
        int i = 0;
        while (it.hasNext()) {
            LocalCabinet cab = (LocalCabinet) it.next();
            System.out.println ("\tRoot " + i + ": " + cab.getRootBase());
            i++;
        }
        
        //  Get the current working directory and show it.
        LocalCabinet cwd = lm.getWorkingDirectory();
        System.out.println ("Current working directory is: " + cwd.getRootBase() + cwd.getFullName());
    }
    
    /**
     *  Get a root path.  Add it as a root.  Lists all files in that directory.  Get a file name to read,
     *  get a second name to write to.  Copy files.
     */
    public static void test1 () throws java.io.IOException, osid.filing.FilingException, osid.shared.SharedException {
        String path = null;       //  Path to root.
        String username = null;     //  User name.
        String password = null;     //  Password.
        
        System.out.println ("Test 1:  interactive add root test.\n");
        
        //  Get the server name, user name, and password.
        System.out.print ("Enter a file path: ");
        path = readline();
        
        //  Create a filing manager and make an FTP client.
        lm = new LocalFilingManager();
        
        //  Set the root directory entry.
        //lm.trace = true;
        lm.addRoot (path);
        System.out.println ("Root path via mgr: " + lm.getRootPath());
        System.out.println ("Root base via getRoot(): " + lm.getRoot().getRootBase());
        
        //  Display entries in the root.
        printFilesInCurrent();
        
        //  Get the name of a file to read.
        System.out.print ("\nFile name to read:  ");
        String fn = readline();
        
        //  Open it and copy to stdout.
        lm.trace = true;
        LocalCabinet cwd = lm.getWorkingDirectory();
        LocalCabinetEntry entry = (LocalCabinetEntry) cwd.getCabinetEntryByName(fn);
        if (entry.isCabinet()) {
            System.out.println ("Entry selected is a directory.");
        }
        else {
            LocalByteStore bs = (LocalByteStore) entry;
            System.out.println ("Entry selected is a file.");
            System.out.println ("Entry name is: " + bs.getFile().getAbsolutePath());
            osid.shared.ByteValueIterator it = bs.read(null);
            while (it.hasNext())
                System.out.write(it.next());

            //  Get the name of a file to write.
            System.out.print ("\nFile name to write:  ");
            fn = readline();
            
            //  Create a new ByteStore.
            LocalCabinet parent = (LocalCabinet) bs.getParent();
            String absolute = parent.getRootBase() + parent.getFullName() + fn;
            System.out.println ("New byte store name: " + absolute);
            LocalByteStore newStore = (LocalByteStore) parent.createByteStore(absolute);
            
            //  Get the bytes as a chunk.
            byte[] buf = bs.getBytes();
            
            //  Write the bytes out to a the new file.
            newStore.write(buf);
            System.out.println (buf.length + " bytes were written to " + absolute);
        }
    }
    
    public static void test2 () throws java.io.IOException, osid.filing.FilingException, osid.shared.SharedException, osid.OsidException  {
        System.out.println ("Test 2:  TBD.\n");
    }

    public static void test3 () throws java.io.IOException, osid.filing.FilingException {
        System.out.println ("Test 3:  TBD.\n");

    }
    
    public static void test4 () throws java.io.IOException, osid.filing.FilingException {
        System.out.println ("Test 4:  TBD.\n");

    }
    
    public static void test5 () throws java.io.IOException, osid.filing.FilingException {
        System.out.println ("Test 5:  TBD.\n");

    }
        
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws java.io.IOException, osid.filing.FilingException, osid.shared.SharedException, osid.OsidException {
        int testNo = Integer.parseInt(args[0]);
        
        System.out.println ("Local Filing System Test");
        System.out.println ("------------------------\n");

        if (testNo == 0)
            test0();
        if (testNo == 1)
            test1();
        if (testNo == 2)
            test2();
        if (testNo == 3)
            test3();
        if (testNo == 4)
            test4();
        if (testNo == 5)
            test5();
        
        System.out.println ("Done!");
    }
}
