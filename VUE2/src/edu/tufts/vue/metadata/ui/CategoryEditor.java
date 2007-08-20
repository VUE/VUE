
/*
 *
 * * <p><b>License and Copyright: </b>The contents of this file are subject to the
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
 *
 */

package edu.tufts.vue.metadata.ui;

import edu.tufts.vue.metadata.*;
import edu.tufts.vue.ontology.*;

import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.table.*;

//import tufts.vue.gui.DockWindow;

/*
 * CategoryEditor.java
 *
 * Created on August 17, 2007, 2:05 PM
 *
 * @author dhelle01
 */
public class CategoryEditor extends JPanel 
{
    
    private JTable metadataSetTable;
    private JTable customCategoryTable;
    
    private JPanel setPanel;
    private JPanel customPanel;
    
    public CategoryEditor() 
    {
        setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
        setPanel = new JPanel();
        metadataSetTable = new JTable(new FullMetadataSetTableModel());
        setPanel.add(new JScrollPane(metadataSetTable));
        customPanel = new JPanel();
        customCategoryTable = new JTable(new MetadataCategoryTableModel());
        customPanel.add(new JScrollPane(customCategoryTable));
        add(setPanel);
        // add separator
        add(customPanel);
    }
    
    class FullMetadataSetTableModel extends AbstractTableModel
    {
        List<Boolean> selections = new ArrayList<Boolean>();
        private CategoryModel vueCategoryModel = tufts.vue.VUE.getCategoryModel();
        
        public int getRowCount()
        {
            return vueCategoryModel.size();
        }
        
        /**
         *
         * first column contains checkbox that will remove or add ontologies
         * second column contains ontology names 
         *
         **/
        public int getColumnCount()
        {
            return 2;
        }
        
        public Object getValueAt(int row,int col)
        {
            if(col == 1)
            {
              return vueCategoryModel.get(row);
            }
            else
            //if(col == 0)
            {
              //return selections.get(row);
              return Boolean.TRUE;
            }

        }
    }
    
    class MetadataCategoryTableModel extends AbstractTableModel
    {
        
        private CategoryModel vueCategoryModel = tufts.vue.VUE.getCategoryModel();
        
        /**
         *
         * column 1: category, which is an OntType and will appear
         * as a simple string
         *
         * column 2: remove button (should change CategoryModel)
         *
         **/
        public int getColumnCount()
        {
            return 2;
        }
        
        public int getRowCount()
        {
            return vueCategoryModel.getCustomOntology().getOntTypes().size();
            // vueCategoryModel.getCustomCategories.getSize();
        }
        
        public Object getValueAt(int row,int col)
        {
            if(col == 0)
            {
              OntType category = vueCategoryModel.getCustomOntology().getOntTypes().get(row);
              return category;
            }
            else
            //if(row == 1)
            {
              return "remove";
            }
        }
    }
    
}