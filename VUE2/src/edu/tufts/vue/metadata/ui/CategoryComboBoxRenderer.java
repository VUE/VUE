/*
 * CategoryComboBoxRenderer.java
 *
 * Created on August 17, 2007, 2:45 PM
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
 * <p>The entire file consists of original code.  Copyright &copy; 2003-2007
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

/**
 *
 * @author akumar03
 */
package edu.tufts.vue.metadata.ui;

import java.util.*;
import javax.swing.*;
import java.awt.*;

import edu.tufts.vue.metadata.*;
import edu.tufts.vue.ontology.*;

public class CategoryComboBoxRenderer extends DefaultListCellRenderer {
    
    /** Creates a new instance of CategoryComboBoxRenderer */
    public CategoryComboBoxRenderer() {
    }
    public Component getListCellRendererComponent(
            JList list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {
        
        if(value != null){
            if((value instanceof edu.tufts.vue.ontology.OntType)) {
                setText(((OntType)value).getLabel());
                //System.out.println("RENDERER(OntType) - label:"+((OntType)value).getLabel()+" index: "+index+" selected:"+isSelected);
                if (isSelected) {
                    setBackground(list.getSelectionBackground());
                    setForeground(list.getSelectionForeground());
                } else {
                    setBackground(list.getBackground());
                    setForeground(list.getForeground());
                }
                
                
            } else {
                //System.out.println("RENDERER  - label:"+value.toString()+"class:"+ value.getClass()+" index: "+index+" selected:"+isSelected);
                setText(value.toString());
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
        }
        
        return this;
    }
    
}