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
