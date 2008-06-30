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
 * RemotePath.java
 *
 * Created on October 30, 2003, 2:49 PM
 */

package tufts.oki.remoteFiling;
import java.util.Vector;

/**
 *  The RemotePath class defines a way to represent file name paths for a remote file
 *  system.  Node[0] is always the root cabinet for this file system.
 *
 *  @author  Mark Norton
 */
public class RemotePath {
    public static final char PATH_DELIM = '/';  //  The path delimiter for this file system.
    private Vector nodes = null;  
    private RemoteCabinetEntry leaf = null;
    private String rootBase = null;
    
    /** 
     *  Creates a new instance of RemotePath given a rootBase and a RemoteCabinetEntry.
     *  Path nodes, and leaf are all initialized.  If the entry given is a ByteStore,
     *  it is considered a path leaf.  If it is a Cabinet, the leaf is left null.  The
     *  root base may be passed as null.
     *
     *  @author Mark Norton
     */
    public RemotePath(String base, RemoteCabinetEntry entry) throws osid.filing.FilingException {
       // assert (entry != null) : "CabinetEntry passed is null.";
        
        this.nodes = new Vector();
        this.rootBase = base;   //  Set the root base.
                
        //  This is a cabinet, add it as the terminal node.
        if (entry instanceof RemoteCabinet) {
            nodes.add (entry);
        }
        //  Otherwise, it is a leaf file.
        else {
            this.leaf = entry;
        }
        
        //  Interate up the parent links to root.
        RemoteCabinetEntry ptr = entry;
        int i = 0;
        while (ptr.getParent() != null) {
            nodes.add (i, ptr.getParent());
            ptr = (RemoteCabinetEntry) ptr.getParent();
        }
    }
    
    /**
     *  Construct a string equivalent for this path and return it.  Uses the rootBase
     *  To create a prefix to the path string.  Uses PATH_DELIM to denote file nodes.
     *  If path contains a leaf, it is appended.  If rootBase is null, it is not used
     *  to prefix the path name string.
     *
     *  @author Mark Norton
     */
    String getPathString() throws osid.filing.FilingException {
        StringBuffer str = new StringBuffer();
        
        //  Initialize with the rootBase;
        if (rootBase != null) {
            str.append(rootBase);
            str.append (RemotePath.PATH_DELIM);
        }
        
        //  Skip root, Interate over nodes and append them.
        for (int i = 1; i < nodes.size(); i++) {
            RemoteCabinetEntry node = (RemoteCabinetEntry) nodes.get(i);
            str.append (node.getDisplayName());
            str.append (RemotePath.PATH_DELIM);
        }
        
        //  Append the leaf, if present.
        if (leaf != null)
            str.append (leaf.getDisplayName());
        
        //  Convert to a String and return it.
        return str.toString();
    }
    
}
