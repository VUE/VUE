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
