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
 * ResultNode.java
 *
 * Created on May 7, 2003, 11:57 AM
 */
package tufts.vue;
import tufts.google.*;
import javax.swing.*;
import java.io.*;
import javax.swing.tree.*;


/**
 *
 * @author  rsaigal
 */
public class ResultNode extends DefaultMutableTreeNode{
    
    /** Creates a new instance of ResultNode */
    public ResultNode(Result result) {
        setUserObject(result);
    }
    public Result getResult(){
        return (Result)getUserObject();
    }
    public String toString(){
        Result result = (Result)getUserObject();
        return(result.getUrl());
    }
    
}
