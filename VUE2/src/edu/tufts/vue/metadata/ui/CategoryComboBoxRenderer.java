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
                
                if(value instanceof edu.tufts.vue.metadata.gui.EditCategoryItem)
                {
                    if(tufts.vue.VUE.getCategoryModel().isLoaded() == false)
                    {
                        setText("loading...");
                    }
                }
            }
        }
        
        return this;
    }
    
}
