
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

/*
 * WeightVisualizationSettingsPanel.java
 *
 * Created on February 2, 2007, 3:47 PM
 *
 * @author dhelle01
 */

package edu.tufts.vue.compare.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;

import tufts.vue.VueResources;

public class WeightVisualizationSettingsPanel extends JPanel {
   
    public static final String parameterChoiceMessageString = "Set parameters for:";
    public static final String intervalChoiceMessageString = "Set number of intervals:";
    public static final String paletteChoiceMessageString = "Select a color Palette";
    
    private JComboBox parameterChoice;
    private JComboBox intervalNumberChoice;
    
    private IntervalListModel nodeModel;
    private IntervalListModel linkModel;
    private JTable intervalList;
    
    /** Creates a new instance of WeightVisualizationSettingsPanel */
    public WeightVisualizationSettingsPanel() 
    {
        String[] parameterChoices = {"Nodes","Links"};
        JLabel parameterChoiceMessage = new JLabel(parameterChoiceMessageString);
        parameterChoice = new JComboBox(parameterChoices);
        JLabel helpLabel = new JLabel(VueResources.getIcon("helpIcon.raw"));
        JLabel intervalNumberChoiceMessage = new JLabel(intervalChoiceMessageString);
        String[] intervalNumberChoices = {"3","4","5","6","7","8","9","10"};
        intervalNumberChoice = new JComboBox(intervalNumberChoices);
        nodeModel = new IntervalListModel();
        linkModel = new IntervalListModel();
        intervalList = new JTable();
        
        setModel();
        
        GridBagLayout gridBag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridBag);
        
        //first row
        c.anchor = GridBagConstraints.EAST;
        gridBag.setConstraints(parameterChoiceMessage,c);
        add(parameterChoiceMessage);
        c.anchor = GridBagConstraints.WEST;
        gridBag.setConstraints(parameterChoiceMessage,c);
        add(parameterChoice);
        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridBag.setConstraints(helpLabel,c);
        add(helpLabel);
        
        //second row
        c.weightx = 0.0;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridBag.setConstraints(intervalNumberChoiceMessage,c);
        add(intervalNumberChoiceMessage);
        c.anchor = GridBagConstraints.WEST;
        gridBag.setConstraints(intervalNumberChoice,c);
        add(intervalNumberChoice);
        
    }
    
    // sets model based on parameter choice selection
    public void setModel()
    {
        if(parameterChoice.getSelectedIndex() == 0)
        {
            intervalList.setModel(nodeModel);
        }
        else
        {
            intervalList.setModel(linkModel);
        }
    }
    
    public static void main(String[] args)
    {
        javax.swing.JFrame f = new javax.swing.JFrame("test");
        f.setBounds(100,100,300,400);
        f.getContentPane().add(new WeightVisualizationSettingsPanel());
        f.setVisible(true);
    }
    
    class PercentageInterval
    {
        
    }
    
    class IntervalStylePreview
    {
        
    }
        
    class IntervalListModel implements TableModel
    {
        
        private List<PercentageInterval> piList = new ArrayList<PercentageInterval>();
        private List<IntervalStylePreview> ispList = new ArrayList<IntervalStylePreview>();
        
        public void addRow(int startPercentage,int endPercentage,java.awt.Color previewColor)
        {
           // add new objects to both lists
        }
        
        public void addTableModelListener(TableModelListener tml)
        {
            
        }
        
        public void removeTableModelListener(TableModelListener tml)
        {
            
        }
        
        public Class getColumnClass(int col)
        {
            if(col == 0)
            {
                return PercentageInterval.class;
            }
            else
            {
                return IntervalStylePreview.class;
            }
            
        }
        
        public int getColumnCount()
        {
            return 2;
        }
        
        public String getColumnName(int col)
        {
            if(col == 0)
                return "Intervals:";
            else
                return "Preview:";
        }
        
        public int getRowCount()
        {
            return piList.size();
        }
        
        public Object getValueAt(int row,int col)
        {
            if(col == 0)
            {
                return piList.get(row);
            }
            else
            {
                return ispList.get(row);
            }
        }
        
        public boolean isCellEditable(int row,int col)
        {
            return true;
        }
        
        public void setValueAt(Object value,int row,int col)
        {
            
        }
        
    }
    
    
}