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

/*
 * RemotePathTest.java
 *
 * Created on October 30, 2003, 4:45 PM
 */

package tufts.oki.remoteFiling;

/**
 *
 * @author  Mark Norton
 */
public class RemotePathTest {
    
    /** Creates a new instance of RemotePathTest */
    public RemotePathTest() {
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws osid.shared.SharedException, osid.OsidException, osid.filing.FilingException {
        System.out.println ("Remote Path Test");
        System.out.println ("----------------");
        System.out.println ("");
        
        //  Create a root cabinet.
        RemoteCabinet root = null;
        tufts.oki.shared.Agent agent = new tufts.oki.shared.Agent ("mark", new tufts.oki.shared.AgentPersonType());
        root = new RemoteCabinet ("root", agent, null);
        
        //  Add a node to it.
        //RemoteCabinet cab1 = (src.oki.filing.RemoteCabinet) root.createCabinet("dir1");
        RemoteCabinet cab1 = new RemoteCabinet ("dir1", agent, root);
        root.add (cab1);

        //  Add a second node to it.
        //RemoteCabinet cab2 = (RemoteCabinet) cab1.createCabinet("dir1");
        RemoteCabinet cab2 = new RemoteCabinet ("dir2", agent, cab1);
        cab1.add (cab2);
        
        //  Add a leaf file to it.
        //RemoteByteStore store = (RemoteByteStore) cab2.createByteStore ("readme.txt");
        RemoteByteStore store = new RemoteByteStore ("readme.txt", cab2);
        cab2.add (store);
        
        //  Create a RemotePath object.
        RemotePath path = new RemotePath ("/trueroot", store);
        
        //  Print the path name string.
        System.out.println ("Resultant path: " + path.getPathString());
    }
    
}
