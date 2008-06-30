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
