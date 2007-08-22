
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

import edu.tufts.vue.metadata.action.*;
import java.awt.BorderLayout;
import java.awt.event.*;
import java.net.*;
import javax.swing.*;
import javax.swing.table.*;
import tufts.vue.*;

import java.util.*;

/*
 * MetadataSearchGUI.java
 *
 * Will likely replace MapInspector in all its incarnations
 * and locations within VUE (this GUI supports the new
 * metadata format)
 *
 * Created on July 19, 2007, 1:31 PM
 *
 * @author dhelle01
 */
public class MetadataSearchGUI extends JPanel {
    
    public static final int ONE_LINE = 0;
    
    //ONE_LINE
    private JTextField searchField;
    private JTable searchTable;
    private JButton searchButton;
    private List<URI> found = null;
    private List<List<URI>> finds = null;
    
    //TEXT FIELD BASED
    private JPanel optionsPanel;
    private JComboBox searchTypesChoice;
    private String[] searchTypes = {"Basic","Categories","Advanced"};
    
    private JPanel fieldsPanel;
    private JTable searchTermsTable;

    
    public MetadataSearchGUI() 
    {
        setUpOneLineSearch();      
    }
    
    public MetadataSearchGUI(int type)
    {
        if(type == ONE_LINE)
        {
           setUpOneLineSearch();
        }
        else
        {
           setUpFieldsSearch();
        }
        
    }
    
    public void setUpOneLineSearch()
    {
        setLayout(new BorderLayout());
        searchField = new JTextField();
        searchField.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(9,9,9,9),
                              BorderFactory.createLineBorder(new java.awt.Color(200,200,200),1)));
        JPanel buttonPanel = new JPanel(new BorderLayout());
        searchButton = new JButton(new SearchAction(searchField));
        searchButton.setBackground(java.awt.Color.WHITE);
        buttonPanel.setBackground(java.awt.Color.WHITE);
        buttonPanel.add(BorderLayout.EAST,searchButton);
        add(BorderLayout.NORTH,searchField);
        add(buttonPanel);
    }
    
    public void setUpFieldsSearch()
    {
        setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
        optionsPanel = new JPanel();
        fieldsPanel = new JPanel();
        
        searchTypesChoice = new JComboBox(searchTypes);
        
        
        searchTermsTable = new JTable(new SearchTermsTableModel());
        
        
        add(optionsPanel);
        add(fieldsPanel);
    }
    
    class SearchTermsTableModel extends AbstractTableModel
    {
        
        //public List<VueMetadataElement> searchTerms = new ArrayList<VueMetadataElement>();
        
        public int getRowCount()
        {
            return 0;
        }
        
        public int getColumnCount()
        {
            return 0;
        }
        
        public Object getValueAt(int row,int col)
        {
            return null;
        }
        
    }
        
}
