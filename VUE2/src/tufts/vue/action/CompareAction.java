/*
 * CompareAction.java
 *
 * Created on October 30, 2006, 3:27 PM
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
 * <p>The entire file consists of original code.  Copyright &copy; 2003-2006
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

/**
 *
 * @author akumar03
 */
package tufts.vue.action;

import tufts.vue.*;
import edu.tufts.vue.compare.*;

import java.io.*;
import java.util.*;


import javax.swing.*;
import java.awt.event.*;


public class CompareAction  extends VueAction {
    
    /** Creates a new instance of CompareAction */
    public CompareAction(String label) {
        super(label);
    }
    
    public void actionPerformed(ActionEvent e) {
        try {
            ConnectivityMatrix matrix = new ConnectivityMatrix(tufts.vue.VUE.getActiveMap());
            String c = matrix.toString();
            File file = ActionUtil.selectFile("Save as Connectivity Matrix","txt");
            if(file == null) {
                return;
            }
            String fileName = file.getAbsolutePath();
            if(!fileName.esndsWith(".txt")) fileName += ".txt";
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
            writer.write(c);
            writer.close();
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        
    }
    
}
