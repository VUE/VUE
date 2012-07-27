/*
* Copyright 2003-2010 Tufts University  Licensed under the
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

import tufts.vue.DEBUG;

import edu.tufts.vue.metadata.*;
import edu.tufts.vue.ontology.*;

import edu.tufts.vue.metadata.gui.*;

public class CategoryComboBoxModel extends DefaultComboBoxModel {
    public static final String ERROR_LABEL = "Not Found";
    private final CategoryModel VUE_Ontologies;
    private final EditCategoryItem editCategory = new EditCategoryItem();
    
    Object selectedItem;
    
    /** Creates a new instance of CategoryComboBoxModel */
    public CategoryComboBoxModel() {
        VUE_Ontologies = tufts.vue.VUE.getCategoryModel();
        setSelectedItem(getElementAt(0));
    }
    public Object getSelectedItem() { return selectedItem; }
    
    // public List<Ontology> getOntologuies() { return vueCategoryModel; }
    
    public void setSelectedItem(Object newValue) {
        selectedItem = newValue;
    }
    
    public int getSize() {
        int count = 0;
        for (Ontology ont : VUE_Ontologies) {
            count += ont.getOntTypes().size();
            count++; // for spacer
        }
        count++; // for the edit item in the end
        // System.out.println("CCMsize=" + count);
        return count;
    }

    private final ComboBoxSeparator Separator = new ComboBoxSeparator();
    
    public Object getElementAt(int i) {
        if (i == getSize() - 1) 
            return editCategory;

        for (Ontology ont : VUE_Ontologies) {
            if (i < ont.getOntTypes().size()) {
                return ont.getOntTypes().get(i);
            } else if (i == ont.getOntTypes().size()) {
                if (DEBUG.PAIN)
                    return "^^^^" + ont.getLabel() + "^^^^";
                else
                    return Separator;
                // todo: if getOntTypes().size() == 0, don't put in duplicate consecutive dividers
            } else {
                i -= ont.getOntTypes().size();
                i--; // for the divider int the middle;
            }
        }
        return ERROR_LABEL;
    }
    
    public void refresh() {
        // System.out.println("CCM REFRESH");
        fireContentsChanged(this, 0, getSize());
    }

    
}
