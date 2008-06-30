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

import edu.tufts.vue.metadata.*;
import edu.tufts.vue.ontology.*;

import edu.tufts.vue.metadata.gui.*;
public class CategoryComboBoxModel extends DefaultComboBoxModel {
    public static final String ERROR_LABEL = "Not Found";
    Object selectedItem;
    CategoryModel vueCategoryModel;
    EditCategoryItem editCategory = new EditCategoryItem();
    /** Creates a new instance of CategoryComboBoxModel */
    public CategoryComboBoxModel() {
        vueCategoryModel = tufts.vue.VUE.getCategoryModel();
        setSelectedItem(getElementAt(0));
    }
    public Object getSelectedItem() {
        return selectedItem;
    }
    
    public List<Ontology> getOntologuies() {
        return vueCategoryModel;
    }
    
    public void setSelectedItem(Object newValue) {
        selectedItem = newValue;
    }
    
    public int getSize() {
        int count = 0;
        for(Ontology ont: vueCategoryModel) {
            count += ont.getOntTypes().size();
            count++;
        }
        count++; // for the edit item in the end
        return count;
    }
    
    public Object getElementAt(int i) {
        if(i == (getSize()-1)) {
            return editCategory;
        }
        for(Ontology ont: vueCategoryModel) {
            if(i <ont.getOntTypes().size()) {
                return ont.getOntTypes().get(i);
            } else if(i == ont.getOntTypes().size()) {
                return new ComboBoxSeparator();
            } else {
                i -= ont.getOntTypes().size();
                i--; // for the divider int the middle;
            }
        }
        return ERROR_LABEL;
    }
    
    public void refresh()
    {
        fireContentsChanged(this,0,getSize());
    }

    
}
