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
        root = new RemoteCabinet ("root", agent, null,null);
        
        //  Add a node to it.
        //RemoteCabinet cab1 = (src.oki.filing.RemoteCabinet) root.createCabinet("dir1");
        RemoteCabinet cab1 = new RemoteCabinet ("dir1", agent, root,null);
        root.add (cab1);

        //  Add a second node to it.
        //RemoteCabinet cab2 = (RemoteCabinet) cab1.createCabinet("dir1");
        RemoteCabinet cab2 = new RemoteCabinet ("dir2", agent, cab1,null);
        cab1.add (cab2);
        
        //  Add a leaf file to it.
        //RemoteByteStore store = (RemoteByteStore) cab2.createByteStore ("readme.txt");
        RemoteByteStore store = new RemoteByteStore ("readme.txt", cab2, null);
        cab2.add (store);
        
        //  Create a RemotePath object.
        RemotePath path = new RemotePath ("/trueroot", store);
        
        //  Print the path name string.
        System.out.println ("Resultant path: " + path.getPathString());
    }
    
}
