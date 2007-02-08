
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

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.CellEditorListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import tufts.vue.LWMap;
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
    
    private LWMap map = null;
    
    /** Creates a new instance of WeightVisualizationSettingsPanel */
    public WeightVisualizationSettingsPanel() 
    {
        String[] parameterChoices = {"Nodes","Links"};
        JLabel parameterChoiceMessage = new JLabel(parameterChoiceMessageString,JLabel.RIGHT);
        parameterChoice = new JComboBox(parameterChoices);
        JLabel helpLabel = new JLabel(VueResources.getIcon("helpIcon.raw"),JLabel.LEFT);
        JLabel intervalNumberChoiceMessage = new JLabel(intervalChoiceMessageString);
        Integer[] intervalNumberChoices = {3,4,5,6,7,8,9,10};
        intervalNumberChoice = new JComboBox(intervalNumberChoices);
        intervalNumberChoice.setSelectedItem(5);
        nodeModel = new IntervalListModel();
        linkModel = new IntervalListModel();
        intervalList = new JTable();
        intervalList.setDefaultRenderer(PercentageInterval.class,new PercentageIntervalRenderer());
        intervalList.setDefaultEditor(PercentageInterval.class,new PercentageIntervalEditor());
        intervalList.setDefaultRenderer(IntervalStylePreview.class,new IntervalStylePreviewRenderer());
        intervalList.setDefaultEditor(IntervalStylePreview.class,new IntervalStylePreviewEditor());

        //$
           intervalList.setRowHeight(30);
        //$

        loadSettings();
        
        setModel();
        
        GridBagLayout gridBag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridBag);
        
        //first row
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.EAST;
        gridBag.setConstraints(parameterChoiceMessage,c);
        add(parameterChoiceMessage);
        c.anchor = GridBagConstraints.WEST;
        gridBag.setConstraints(parameterChoice,c);
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
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridBag.setConstraints(intervalNumberChoice,c);
        add(intervalNumberChoice);
        
        //third row
        
        //table
        //c.fill = GridBagConstraints.NONE;
        JScrollPane scroll = new JScrollPane(intervalList);
        gridBag.setConstraints(scroll,c);
        add(scroll);
        
    }
    
    //Likely will do more than just return map later, Probably will be initialized as needed in the constructor
    //and/or through a setMap() method called from the Chooser.
    public LWMap getMap()
    {
        return map;
    }
    
    public void loadDefaultSettings()
    {
        // these should be read from the resource file in future
        //(waiting for style loading code)
        nodeModel.addRow(0,20,new java.awt.Color(230,230,255),Color.BLACK);
        nodeModel.addRow(21,40,new java.awt.Color(180,180,255),Color.BLACK);
        nodeModel.addRow(41,60,new java.awt.Color(100,100,255),Color.BLACK);
    }
    
    public void loadSettings()
    {
        // really need to check further if have LWMergeMap here?
        if(getMap() == null)
        {
            loadDefaultSettings();
        }
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
        f.setLayout(new java.awt.GridLayout(1,1));
        f.getContentPane().add(new WeightVisualizationSettingsPanel());
        f.pack();
        f.setVisible(true);
    }
    
    class PercentageInterval
    {
        private int start;
        private int end;
        
        public PercentageInterval(int start,int end)
        {
            this.start = start;
            this.end = end;
        }
        
        public int getStart()
        {
            return start;
        }
        
        public int getEnd()
        {
            return end;
        }
    }
    
    class IntervalStylePreview
    {
        private Color background;
        private Color foreground;
        
        public IntervalStylePreview(Color back,Color front)
        {
            background = back;
            foreground = front;
        }
        
        public Color getBackground()
        {
            return background;
        }
        
        public Color getForeground()
        {
            return foreground;
        }
        
    }
    
    class PercentageIntervalRenderer extends JPanel implements TableCellRenderer
    {
        private JTextField startField = new JTextField(3);
        private JTextField endField = new JTextField(3);
        
        public java.awt.Component getTableCellRendererComponent(JTable table,Object value,boolean isSelected,boolean hasFocus,int row, int col)
        {
            // these intial values are not likely to be seen (unless wrong class used for value)
            // just a fail safe as this point
            int start = 0;
            int end = 100;
            
            if(value instanceof PercentageInterval)
            {
                PercentageInterval pi = (PercentageInterval)value;
                start = pi.getStart();
                end = pi.getEnd();
            }
            
            //JPanel renderer = new JPanel();
            
            //&
              System.out.println("PercentageIntervalRenderComponent: getComponent");
              setOpaque(true);
              setBackground(Color.red);
            //&
            
            // todo: add rounded border for button image effect
            // perhaps use icon in future for node/link contrast at this spot in GUI?
            //JTextField startField = new JTextField(start+"");
            //JTextField endField = new JTextField(end+"");
            startField.setText(start+"");
            endField.setText(end+"");
            add(startField);
            add(endField);
            return this;
        }
    }
    
    class PercentageIntervalEditor extends javax.swing.AbstractCellEditor implements TableCellEditor
    {
        
        private JTextField startField = new JTextField(3);
        private JTextField endField = new JTextField(3);
        private JPanel panel = new JPanel();
        
        public PercentageIntervalEditor()
        {
            startField.setHorizontalAlignment(JTextField.LEFT);
            endField.setHorizontalAlignment(JTextField.LEFT);
            panel.add(startField);
            panel.add(endField);
        }
        
        public java.awt.Component getTableCellEditorComponent(JTable table,Object value,boolean isSelected,int row,int col)
        {
            //$
            System.out.println("getTableCellEditorComponent: ");
            //$
            //JPanel p = new JPanel();
            if(value instanceof PercentageInterval)
            {    
               PercentageInterval pi = (PercentageInterval)value;
               startField.setText(pi.getStart()+"");
               endField.setText(pi.getEnd()+"");
            }
            else
            {
                startField.setText("0");
                endField.setText("0");
            }
            //add(startField);
            //add(endField);
            
            //$
              panel.setOpaque(true);
              panel.setBackground(Color.BLUE);
            //$
            
            /*p.addMouseListener(new java.awt.event.MouseAdapter(){
               public void mouseClicked(java.awt.event.MouseEvent e)
               {
                   System.out.println("Mouse clicked on PI cell editor: " + e);
               }
            });*/
            
            return panel;
        }
        
        public Object getCellEditorValue()
        {
            return new PercentageInterval(Integer.parseInt(startField.getText()),Integer.parseInt(endField.getText()));
        }
        
        /*public void addCellEditorListener(CellEditorListener listener)
        {
            System.out.println("WVSP: add listener: " + listener);
        }
        
        public void removeCellEditorListener(CellEditorListener listener)
        {
            System.out.println("WVSP: remove listener: " + listener);
        }
        
        public void cancelCellEditing()
        {
            System.out.println("WVSP: Cancel cell editing");
        }

        public boolean isCellEditable(java.util.EventObject e)
        {
            return true;
        }
        
        public boolean shouldSelectCell(java.util.EventObject e)
        {
            return true;
        }
        
        public boolean stopCellEditing()
        {
            System.out.println("WVSP: stop cell editing - returning true");
            return true;
        }*/
        
        /*public java.awt.Component getTableCellRendererComponent(JTable table,Object value,boolean isSelected,boolean hasFocus,int row, int col)
        {
            // these intial values are not likely to be seen (unless wrong class used for value)
            // just a fail safe as this point
            int start = 0;
            int end = 100;
            
            if(value instanceof PercentageInterval)
            {
                PercentageInterval pi = (PercentageInterval)value;
                start = pi.getStart();
                end = pi.getEnd();
            }
            
            //JPanel renderer = new JPanel();
            
            //&
              System.out.println("PercentageIntervalRenderComponent: getComponent");
              setOpaque(true);
              setBackground(Color.red);
            //&
            
            // todo: add rounded border for button image effect
            // perhaps use icon in future for node/link contrast at this spot in GUI?
            //JTextField startField = new JTextField(start+"");
            //JTextField endField = new JTextField(end+"");
            startField.setText(start+"");
            endField.setText(end+"");
            startField.setHorizontalAlignment(JTextField.LEFT);
            endField.setHorizontalAlignment(JTextField.LEFT);
            //add(startField);
            //add(endField);
            return this;
        }*/

        
    }
    
    class IntervalStylePreviewRenderer implements TableCellRenderer
    {
        public java.awt.Component getTableCellRendererComponent(JTable table,Object value,boolean isSelected,boolean hasFocus,int row, int col)
        {
            // not likely to be seen (unless wrong class used for value)
            // just a fail safe as this point
            Color textColor = Color.BLACK;
            Color backColor = Color.WHITE;
            
            if(value instanceof IntervalStylePreview)
            {
                IntervalStylePreview isp = (IntervalStylePreview)value;
                textColor = isp.getForeground();
                backColor = isp.getBackground();
            }
            
            JPanel renderer = new JPanel();
            // todo: add rounded border for button image effect
            // perhaps use icon in future for node/link contrast at this spot in GUI?
            
            //$
               renderer.setOpaque(true);
               renderer.setBackground(Color.YELLOW);
            //$
            
            JLabel buttonImage = new JLabel("Label");
            buttonImage.setOpaque(true);
            buttonImage.setForeground(textColor);
            buttonImage.setBackground(backColor);
            JLabel hotSpot = new JLabel("[edit style]");
            renderer.add(buttonImage);
            renderer.add(hotSpot);
            return renderer;
        }
    }
   
    class IntervalStylePreviewEditor extends javax.swing.AbstractCellEditor implements TableCellEditor
    {
        
        // not likely to be seen (unless wrong class assigned to Table Cell value)
        // just a fail safe as this point
        Color textColor = Color.BLACK;
        Color backColor = Color.WHITE;

        private JLabel buttonImage;
        private JLabel hotSpot;
        private JPanel panel = new JPanel();
        
        public IntervalStylePreviewEditor()
        {
            buttonImage = new JLabel("Label");
            buttonImage.setOpaque(true);
            buttonImage.setForeground(textColor);
            buttonImage.setBackground(backColor);
            hotSpot = new JLabel("[edit style]");
            panel.add(buttonImage);
            panel.add(hotSpot);
        }
        
        public java.awt.Component getTableCellEditorComponent(JTable table,Object value,boolean isSelected,int row,int col)
        {
            //$
            System.out.println("getTableCellEditorComponent: ");
            //$
            //JPanel p = new JPanel();
            if(value instanceof IntervalStylePreview)
            {
                IntervalStylePreview isp = (IntervalStylePreview)value;
                textColor = isp.getForeground();
                backColor = isp.getBackground();
            }
            
            buttonImage.setForeground(textColor);
            buttonImage.setBackground(backColor);
            
            //$
              panel.setOpaque(true);
              panel.setBackground(Color.GREEN);
            //$
            
            /*p.addMouseListener(new java.awt.event.MouseAdapter(){
               public void mouseClicked(java.awt.event.MouseEvent e)
               {
                   System.out.println("Mouse clicked on PI cell editor: " + e);
               }
            });*/
            
            return panel;
        }
        
        public Object getCellEditorValue()
        {
            return new IntervalStylePreview(textColor,backColor);
        }
       
    }
    
        
    class IntervalListModel implements TableModel
    {
        
        private List<PercentageInterval> piList = new ArrayList<PercentageInterval>();
        private List<IntervalStylePreview> ispList = new ArrayList<IntervalStylePreview>();
        
        public void addRow(int startPercentage,int endPercentage,Color backColor,Color foreColor)
        {
           piList.add(new PercentageInterval(startPercentage,endPercentage));
           ispList.add(new IntervalStylePreview(backColor,foreColor));
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
           //$
           System.out.println("setValueAt: " + value);
           //$
           if(col == 0 && (value instanceof PercentageInterval))
           {
                piList.set(row,(PercentageInterval)value);
           }
        }
        
    }
    
    
}