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
public class LocalFilingTest {
    public static final String LOCAL_ROOT = "C:\\";
    static private LocalFilingManager lm = null; //  The local filing manager.
    
    /** Creates a new instance of FTPFilingTest3 */
    public LocalFilingTest() {
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
    public static void test1 () throws java.io.IOException, osid.filing.FilingException {
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
        InputStream in = lm.openForInput(fn);
        int c;
        while ((c = in.read()) != -1) {
            System.out.write(c);
        }
        lm.closeInput (in);
         
        //  Get the name of a file to read.
        System.out.print ("\nFile name to write:  ");
        fn = readline();
        
        //  Open it and copy to stdout.
        lm.trace = true;
        OutputStream out = lm.openForOutput(fn);
        String msg = "Test of local file system.";
        out.write (msg.getBytes());
        lm.closeOutput (out);        
    }
    
    /**
     *  Tests directory path names using LocalCabinet.getFullName().
     */
    public static void test2 () throws java.io.IOException, osid.filing.FilingException, osid.shared.SharedException, osid.OsidException  {
        System.out.println ("Test 2:  Directory path names.\n");

        Agent agent = new Agent ("Mark", new AgentPersonType());
        LocalCabinet root = new LocalCabinet ("root", agent, null);
        System.out.println ("Path to root: " + root.getFullName());
        
        LocalCabinet dir1 = (LocalCabinet) root.createCabinet ("dir1");
        System.out.println ("Path to first directory: " + dir1.getFullName());
        
        LocalCabinet dir2 = (LocalCabinet) dir1.createCabinet ("dir2");
        System.out.println ("Path to second directory: " + dir2.getFullName());
        
        LocalCabinet dir3 = (LocalCabinet) dir2.createCabinet ("dir3");
        System.out.println ("Path to third directory: " + dir3.getFullName());
    }

    /**
     *  Test 3:  Tests manipulation of path names using explode and implode paths.
     */
    public static void test3 () throws java.io.IOException, osid.filing.FilingException {
        System.out.println ("Test 3:  explodePath and implodePath test.\n");

        //  Create a filing manager and make an FTP client.
        lm = new LocalFilingManager();
        
        File test = new File ("c:/foo/bar/biz/baz");
        String testPath = test.getPath();
        System.out.println ("test path is: " + testPath);
        System.out.println ("separator is: " + test.separator);

        String parts[] = lm.explodePath (testPath, test.separatorChar);
        for (int i=0; i < parts.length; i++) {
            System.out.println ("\t Part " + i + ": " + parts[i]);
        }
        System.out.println ("reassembled test path: " + lm.implodePath(parts, test.separator));

        System.out.println ("\nTest of root.");
        test = new File ("c:/");
        testPath = test.getPath();
        System.out.println ("test path is: " + testPath);
        System.out.println ("separator is: " + test.separator);

        parts = lm.explodePath (testPath, test.separatorChar);
        for (int i=0; i < parts.length; i++) {
            System.out.println ("\t Part " + i + ": " + parts[i]);
        }
        System.out.println ("reassembled test path: " + lm.implodePath(parts, test.separator));
    }
    
    /**
     *  Tests opening a directory system and changing the working directory.
     */
    public static void test4 () throws java.io.IOException, osid.filing.FilingException {
        System.out.println ("Test 4:  Set working directory and open directory.\n");

        //  Create a filing manager and make an FTP client.
        lm = new LocalFilingManager();
        
        //  Set the root directory entry.
        //lm.trace = true;
        lm.addRoot (LOCAL_ROOT);
        //lm.trace = false;
        System.out.println ("Root path via getRootPath(): " + lm.getRootPath());
        LocalCabinet root = lm.getRoot();
        System.out.println ("Root path via getRoot().getFullName(): " + root.getFullName());
        printFilesInCurrent();
        System.out.flush();
        
        //  Change to osidtest directory.
        lm.trace = true;
        System.out.println ("\nChanging to osidtest.");
        lm.setWorkingDirectory ("osidtest");
        System.out.println ("Current dir:  " + lm.getWorkingDirectory().getFullName());
        printFilesInCurrent();
        System.out.flush();

        //  Change to dir1 directory.
        //lm.trace = true;
        System.out.println ("\nChanging to dir1.");
        lm.setWorkingDirectory ("dir1");
        System.out.println ("Current dir:  " + lm.getWorkingDirectory().getFullName());
        printFilesInCurrent();
        System.out.flush();

        //  Popd back to osidtest directory.
        //lm.trace = true;
        System.out.println ("\nPopd back to osidtest.");
        lm.setWorkingDirectory ("..");
        System.out.println ("Current dir:  " + lm.getWorkingDirectory().getFullName());
        printFilesInCurrent();
        System.out.flush();

        //  Change directory to /osid/di1/dir2.
        lm.trace = true;
        System.out.println ("\nChange to /osidtest/dir1/dir2.");
        lm.setWorkingDirectory ("\\osidtest\\dir1\\dir2");
        System.out.println ("Current dir:  " + lm.getWorkingDirectory().getFullName());
        printFilesInCurrent();
        System.out.flush();
    }
    
    
    /**
     *  Tests create, rename and delete.
     */
    public static void test5 () throws java.io.IOException, osid.filing.FilingException {
        System.out.println ("Test 5:  Create, rename, and delete.\n");

        //  Create a filing manager and make an FTP client.
        lm = new LocalFilingManager();
        lm.trace = true;
        lm.addRoot ("c:\\osidtest");
        System.out.println ("Root path via getRootPath(): " + lm.getRootPath());
        System.out.println ("Root path via getRoot().getFullName(): " + lm.getRoot().getFullName());
        System.out.println ("Current working directory: " + lm.getWorkingDirectory().getFullName());
        printFilesInCurrent();
        
        System.out.println ("\nDirectory test.");
        
        //  Create a new directory.
        //lm.trace = true;
        System.out.println ("Creating 'TestDir'");
        lm.createDirectory ("TestDir");
        printFilesInCurrent();
        
        //  Rename the directory.
        lm.trace = true;
        System.out.println ("Renaming 'TestDir' as 'NewDir'");
        lm.rename ("TestDir", "NewDir");
        printFilesInCurrent();
        
        //  Delete the directory.
        
        //  Create a new byte store.
        //  Rename the byte store.
        //  Delete the byte store.
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
