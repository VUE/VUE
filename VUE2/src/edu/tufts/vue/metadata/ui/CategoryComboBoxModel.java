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

import tufts.Util;
import tufts.vue.DEBUG;

import edu.tufts.vue.metadata.*;
import edu.tufts.vue.ontology.*;

import edu.tufts.vue.metadata.gui.*;

public class CategoryComboBoxModel extends AbstractListModel implements ComboBoxModel
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(CategoryComboBoxModel.class);

    static final Object SEPARATOR = "--------------";
    
    private final CategoryModel VUE_Ontologies;
    private final EditCategoryItem editCategory = new EditCategoryItem();
    private final List contents = new ArrayList();

    private Object selected;
    
    /** Creates a new instance of CategoryComboBoxModel */
    public CategoryComboBoxModel() {
        // Note that the CategoryModel constructor starts a thread to init itself, so if this is
        // the first ask for the CategoryModel, the contents may well be empty.
        VUE_Ontologies = tufts.vue.VUE.getCategoryModel();
        rebuild(contents);
    }
    
    /** @interface ListModel */ public int getSize() { return contents.size(); }
    /** @interface ListModel */ public Object getElementAt(int i) { return contents.get(i); }
    
    /** @interface ComboBoxModel */ public void setSelectedItem(Object o) { selected = o; }
    /** @interface ComboBoxModel */ public Object getSelectedItem() { return selected; }

    private void rebuild(final List list) {
        list.clear();
        for (Ontology ont : VUE_Ontologies) {
            if (DEBUG.PAIN) list.add("[" + ont.getLabel() + "]" + ont.getBase());
            if (ont.getOntTypes().size() > 0) {
                for (OntType ot : ont.getOntTypes())
                    list.add(ot);
                list.add(SEPARATOR);
            }
        }
        list.add(editCategory);
        if (DEBUG.Enabled) Log.debug("rebuid: " + list.size() + " items");
    }

    /** QUIETLY (no events) select the best match for the given key -- if no match, select the 1st SEPARATOR */
    public void selectBestMatchQuietly(final String key) {
        final int index = indexOfCategoryKey(key);
        selected = index >= 0 ? contents.get(index) : SEPARATOR;
        // Note that it turns out we could make the failure selection be anything.  E.g., if
        // nothing matches, and we change this.selected to the String "Nothing Yet", that string
        // would show up as the selected JComboBox value while the menu was popped, even tho
        // nothing is highlighted in the menu.
    }

    /** @return the index of the given fully-qualified RDF/URL style key, -1 if not found */
    public int indexOfCategoryKey(final String key)
    {
        if (key == null || key.indexOf('#') < 0)
            return -1;

        // Todo: could guess at full keys where just the local-part matches the key name
        
        int index = -1;
        for (Object item : contents) {
            index++;
            if (item instanceof OntType) {
                final OntType ontType = (OntType) item;
                // if (DEBUG.PAIN) Log.debug("scan " + Util.tags(ontType));
                if (ontType.matchesKey(key)) {
                    // if (DEBUG.PAIN) Log.debug("found at index " + index + " hit " + Util.tags(ontType));
                    return index;
                }
            }
        }
        return index;
    }
    
    public void refreshCategoryMenu() {
        rebuild(contents);
        fireContentsChanged(this, 0, getSize());
    }
    
    // Old crazy impl:
    // Class no longer needed:
    // private final ComboBoxSeparator Separator = new ComboBoxSeparator();
    // public static final String ERROR_LABEL = "Not Found";
    // public List<Ontology> getOntologuies() { return vueCategoryModel; }
    // private int computeSize() {
    //     int count = 0;
    //     for (Ontology ont : VUE_Ontologies) {
    //         final int size = ont.getOntTypes().size();
    //         if (size > 0 || DEBUG.PAIN) {
    //             count += ont.getOntTypes().size();
    //             count++; // for spacer
    //         }
    //     }
    //     count++; // for the edit item in the end
    //     return count;
    // }
    // private Object computeElementAt(int i) {
    //     final int totSize = getSize();
    //     if (i == totSize - 2)
    //         return Separator;
    //     else if (i == totSize - 1)
    //         return editCategory;
    //     // tofix: crazy slow 
    //     for (Ontology ont : VUE_Ontologies) {
    //         final int size = ont.getOntTypes().size();
    //         if (i < size) {
    //             return ont.getOntTypes().get(i);
    //         } else if (i == size) {
    //             if (DEBUG.PAIN) return "^^^^^^^" + ont.getLabel() + "^^^^^^^";
    //             // if size == 0, don't put in duplicate consecutive dividers
    //             if (i == 0)     // empty ontology
    //                 continue;   // as if didn't exist
    //             else
    //                 return Separator;
    //         } else {
    //             i -= size;
    //             i--; // for the Separator
    //         }
    //     }
    //     return ERROR_LABEL;
    // }

}
