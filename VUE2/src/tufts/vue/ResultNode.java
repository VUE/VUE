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
