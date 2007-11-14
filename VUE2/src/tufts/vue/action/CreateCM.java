/*
 * CreateCM.java
 *
 * Created on November 6, 2006, 11:48 AM
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

public class CreateCM  extends VueAction{
    
    private String label;
    
    /** Creates a new instance of CreateCM */
    public CreateCM(String label) {
        super(label);
        this.label = label;
    }
   
    public void actionPerformed(ActionEvent e) {
        try {
            
            java.awt.Frame dialogParent = VUE.getDialogParentAsFrame();
            int indexOfDot = label.indexOf(".");
            
            String correctedLabel = label;
            
            if(indexOfDot > -1)
            {
              correctedLabel = label.substring(0,indexOfDot);
            }
            MapChooser mapChooser = new MapChooser(dialogParent,correctedLabel);

        } catch(Exception ex) {
            ex.printStackTrace();
        }
        
    }    
    
}
