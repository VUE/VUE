/*
 * PathwayTab.java
 *
 * Created on June 19, 2003, 12:50 PM
 */

package tufts.vue;

import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.table.*;
import javax.swing.ListSelectionModel;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import javax.swing.border.*;
import java.util.Vector;
import java.awt.Color;
import java.awt.Dimension;
import java.util.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;



import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
/**
 *
 * @author  Daisuke Fujiwara
 */

/**A class which displays nodes in a pathway */
public class PathwayTab extends JPanel implements ActionListener, ListSelectionListener, DocumentListener, ItemListener, KeyListener
{    
    //necessary widgets
    private PathwayTable pathwayTable = null;
    private JButton remove = null, plus = null; //moveUp, moveDown, submit 
    private JTextField text;
    private PathwayControl pathwayControl;
    private JDialog parent;
    private JPanel buttons = null, pnPanel = null, pcPanel = null;
    private final Font selectedFont = new Font("SansSerif", Font.BOLD, 12);
    private final Font normalFont = new Font("SansSerif", Font.PLAIN, 12);
    
    /* Pathway control properties */
    
    private JButton firstButton, lastButton, forwardButton, backButton;
    private JButton removeButton, createButton;
    private JLabel nodeLabel;
    private JComboBox pathwayList;
    
    private LWPathwayManager pathwayManager = null;
    
    private final String noPathway = "                          ";
    private final String emptyLabel = "empty";
    
    /* end Pathway Control Properties */
    
    
    /** Creates a new instance of PathwayTab */
    public PathwayTab(JDialog parent) 
    {   
        this.parent = parent;
        
        setLayout(new GridLayout(5,1,5,0));
        setSize(300, 700);
        
        add(getRadioPanel());
        
        pathwayControl = new PathwayControl(parent, this);
        //setPathwayControl();
        //add(pcPanel);
        add(pathwayControl);
        
        setPathwayNameLabel();
        add(pnPanel);
        
        pathwayTable = new PathwayTable(this);
        add(new JScrollPane(pathwayTable));
        
        setButtons();
        add(buttons);
    }
    
    public void setPathwayControl(){
        pathwayList = new JComboBox();
        
        setLayout(new BorderLayout());
        
        firstButton = new JButton("<<");
        lastButton = new JButton(">>");
        forwardButton = new JButton(">");
        backButton = new JButton("<");
        nodeLabel = new JLabel(emptyLabel);
        
        firstButton.addActionListener(this);
        lastButton.addActionListener(this);
        forwardButton.addActionListener(this);
        backButton.addActionListener(this);
        
        createButton = new JButton("Create New Path");
        createButton.addActionListener(this);
        removeButton = new JButton("Delete Selected Path");
        removeButton.addActionListener(this);
        
        pathwayList.setRenderer(new PathwayRenderer());
        pathwayList.addItemListener(this);
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        buttonPanel.setBackground(Color.white);
        
        buttonPanel.add(firstButton);
        buttonPanel.add(backButton);
        buttonPanel.add(forwardButton);
        buttonPanel.add(lastButton);
      
        JPanel descriptionPanel = new JPanel();
        descriptionPanel.setLayout(new FlowLayout());
        descriptionPanel.setBackground(Color.white);
        descriptionPanel.add(new JLabel("Label:"));
        descriptionPanel.add(nodeLabel);
        
        JPanel mainPanel = new JPanel();
        mainPanel.add(new JLabel("Select:"));
        mainPanel.add(pathwayList);
        mainPanel.add(new JLabel("???"));
        //mainPanel.add(buttonPanel);
        
        JPanel pathwayPanel = new JPanel();
        
        
        pathwayPanel.add(createButton);
        pathwayPanel.add(removeButton);
        
        add(pathwayPanel, BorderLayout.SOUTH);
        
        
        
        add(mainPanel, BorderLayout.CENTER);
    }
    
    public void setPathwayNameLabel(){
        pnPanel = new JPanel();
        text = new JTextField();
        text.setEditable(true);
        Document document = text.getDocument();
        document.addDocumentListener(this);
        
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
          
        pnPanel.setLayout(gridbag);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = new Insets(0, 10, 0, 10);
        c.gridwidth = GridBagConstraints.REMAINDER; //end row
        JLabel label = new JLabel();
        gridbag.setConstraints(label, c);
        pnPanel.add(label);
        c.weightx = 0.0;		   

        c.gridwidth = GridBagConstraints.RELATIVE;
        JLabel pName = new JLabel("Path Name:");
        gridbag.setConstraints(pName, c);
        pnPanel.add(pName);
         
        c.gridwidth = GridBagConstraints.REMAINDER; 
        gridbag.setConstraints(text, c);
        pnPanel.add(text);
    }
    
    public void setButtons(){
        
        buttons = new JPanel();
        plus = new JButton("Add Selected Node To Path");
        plus.addActionListener(this);
        plus.setEnabled(false);
        remove = new JButton("Remove Node");
        remove.addActionListener(this);
        remove.setEnabled(false);
        
        //toggles the add button's availability depending on the selection
        VUE.ModelSelection.addListener( new LWSelection.Listener() {
                public void selectionChanged(LWSelection selection)
                {
                    if (!selection.isEmpty() && getPathway() != null)
                      plus.setEnabled(true);
                    else
                      plus.setEnabled(false);
                }
            }     
        );
        
        buttons.add(plus);
        buttons.add(remove);
        
    }
    
    public PathwayControl getPathwayControl(){
        if(this.pathwayControl == null){
            this.pathwayControl = new PathwayControl(parent, this);
        }
        return this.pathwayControl;
    }
    
    public void setPathwayControl(PathwayControl pathwayControl){
        this.pathwayControl = pathwayControl;
    }
    
    public JPanel getRadioPanel(){
        //JPanel pathPanel = new JPanel(new GridLayout(4,1,5,0));
        
        ButtonGroup group = new ButtonGroup();
        JRadioButton create = new JRadioButton("Create / Edit Path", true);
        JRadioButton play = new JRadioButton("Playback / View Multiple Paths");
        group.add(create);
        group.add(play);
        
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
         
        JPanel radioPanel = new JPanel();
         
        radioPanel.setLayout(gridbag);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER; //end row
        JLabel label = new JLabel();
        gridbag.setConstraints(label, c);
        radioPanel.add(label);
        c.weightx = 0.0;		   

        c.gridwidth = GridBagConstraints.RELATIVE; 
        gridbag.setConstraints(create, c);
        radioPanel.add(create);
         
        c.gridwidth = GridBagConstraints.REMAINDER; 
        JLabel label1 = new JLabel(" ");
        label1.setAlignmentY(JLabel.CENTER_ALIGNMENT);
        gridbag.setConstraints(label1, c);
        radioPanel.add(label1);

        c.gridwidth = GridBagConstraints.RELATIVE; 
        gridbag.setConstraints(play, c);
        radioPanel.add(play);
         
        c.gridwidth = GridBagConstraints.REMAINDER; 
        JLabel label2 = new JLabel(" ");
        label2.setAlignmentY(JLabel.CENTER_ALIGNMENT);
        gridbag.setConstraints(label2, c);
        radioPanel.add(label2);
        
        return radioPanel;
    }
    
    //sets the table's pathway to the given pathway
    public void setPathway(LWPathway pathway)
    {
        ((PathwayTableModel)pathwayTable.getModel()).setPathway(pathway);
        
        //disables the add button if the pathway is not selected
        if (pathway == null)
          plus.setEnabled(false);
        
        //if the pathway is selected and map items are selected, enable the add button
        else if (!VUE.ModelSelection.isEmpty())
          plus.setEnabled(true);
    }
    
    /**Gets the current pathway associated with the pathway table*/
    public LWPathway getPathway()
    {
        return ((PathwayTableModel)pathwayTable.getModel()).getPathway();
    }
    
    /**Sets the current node of the pathway to be the selected one from the table*/
    public void setCurrentElement(int index)
    {
       ((PathwayTableModel)pathwayTable.getModel()).getPathway().setCurrentIndex(index);
       
        //update the pathway control panel
        this.pathwayControl.updateControlPanel();
    }
    
    /**Notifies the table of data change*/
    public void updateTable()
    {
        ((PathwayTableModel)pathwayTable.getModel()).fireTableDataChanged();
    }
    
    /**Reacts to actions dispatched by the buttons*/
    public void actionPerformed(ActionEvent e)
    {
        //gets the selected row number
        int selected = pathwayTable.getSelectedRow();
        
        //moves up the selected row 
        /*if (e.getSource() == moveUp)
        {   
            ((PathwayTableModel)pathwayTable.getModel()).switchRow(selected, --selected);
            pathwayTable.setRowSelectionInterval(selected, selected);
            submit.setEnabled(false);
        }
        
        //moves down the selected row
        else if (e.getSource() == moveDown)
        {   
            ((PathwayTableModel)pathwayTable.getModel()).switchRow(selected, ++selected);
            pathwayTable.setRowSelectionInterval(selected, selected);
            submit.setEnabled(false);
        }
        
        //removes the selected row
        else*/
        
        if (e.getSource() == remove)
        {    
             if (selected != -1)
             {
                ((PathwayTableModel)pathwayTable.getModel()).deleteRow(selected);     
                //submit.setEnabled(false);
             }  
        }        
        
        //add selected elements to the pathway where the table is selected
        else if (e.getSource() == plus)
        {
            LWComponent array[] = VUE.ModelSelection.getArray();
              
            //adds everything in the current selection 
            for (int i = 0; i < array.length; i++)
            {
                ((PathwayTableModel)pathwayTable.getModel()).addRow(array[i], ++selected);
            }
            
            pathwayTable.setRowSelectionInterval(selected, selected);
        }

        //submit
        else
        {            
            LWComponent element = ((PathwayTableModel)pathwayTable.getModel()).getPathway().getElement(selected);
            element.setNotes(text.getText());
            //submit.setEnabled(false);
        }
    }
    
    /**Reacts to list selections dispatched by the table*/
    public void valueChanged(ListSelectionEvent le)
    {
        ListSelectionModel lsm = (ListSelectionModel)le.getSource();

        //if there is a row selected
        if (!lsm.isSelectionEmpty())
        {
            int selectedRow = lsm.getMinSelectionIndex();
            remove.setEnabled(true);
            
            LWComponent element = ((PathwayTableModel)pathwayTable.getModel()).getPathway().getElement(selectedRow);
            text.setText(element.getNotes());
      
            //if the selected row is the last row, then disables the move down button
            /*if(selectedRow == pathwayTable.getRowCount() - 1)
                moveDown.setEnabled(false);
            else
                moveDown.setEnabled(true);
            
            //if the selected row is the first row, then disables the move up button
            if(selectedRow == 0)
                moveUp.setEnabled(false);
            else
                moveUp.setEnabled(true);*/
        }
        
        //if no row is selected, then disables all buttons
        else
        {
            //moveDown.setEnabled(false);
            //moveUp.setEnabled(false);
            remove.setEnabled(false);
            //submit.setEnabled(false);
            //text.setText("comment");
        }
    }
   
    /**document listener's methods*/
    public void removeUpdate(javax.swing.event.DocumentEvent documentEvent) 
    {
        //if(pathwayTable.getSelectedRow() != -1)
        //  submit.setEnabled(true);
        //LWPathway pathway = this.getPathway();
        //pathway.setLabel(text.getText());
    }
    
    public void changedUpdate(javax.swing.event.DocumentEvent documentEvent) 
    {
        //if(pathwayTable.getSelectedRow() != -1)
        //  submit.setEnabled(true);
        LWPathway pathway = this.getPathway();
        pathway.setLabel(text.getText());
    }
    
    public void insertUpdate(javax.swing.event.DocumentEvent documentEvent) 
    {
       //if(pathwayTable.getSelectedRow() != -1)
       //  submit.setEnabled(true);
       LWPathway pathway = this.getPathway();
       pathway.setLabel(text.getText());
    } 
    
    public void itemStateChanged(ItemEvent e) {
    }
    
    public void keyPressed(KeyEvent e) {
    }
    
    public void keyReleased(KeyEvent e) {
    }
    
    public void keyTyped(KeyEvent e) {
    }
    
    private class PathwayTable extends JTable{
        
        PathwayTab tab = null;
        
        public PathwayTable(PathwayTab tab){
            super(new PathwayTableModel());
            this.tab = tab;
            setBorder(null);
            setShowGrid(false);
            setDefaultRenderer(getModel().getColumnClass(0), new DefaultTableCellRenderer(){
                public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column)
                {
                    int num = row + 1;
                    setBackground(Color.white);
                    setText(num + ". " + (String)value);
                    setForeground(Color.black);
                    setFont(normalFont);
                    if(isSelected){
                        setForeground(Color.blue);
                        setFont(selectedFont);
                        setText(" > " + getText());
                    }else setText("    " + getText());

                    return this;
                }
            });       
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            setToolTipText("Double click to select the current elment");
            addMouseListener(
                new MouseAdapter()
                {
                    public void mouseClicked(MouseEvent me)
                    {
                        if (me.getClickCount() == 2)
                           setCurrentElement(getSelectedRow());
                    }  
                }
            );
            ListSelectionModel lsm = getSelectionModel();
            lsm.addListSelectionListener(tab);     
        }
    }
    
    /**A model used by the table which displays nodes of the pathway*/
    private class PathwayTableModel extends AbstractTableModel
    {
        //pathway whose nodes are displayed in the table
        private LWPathway pathway;
        
        //column names for the table
        private final String[] columnNames = {"Nodes/Links"};
         
        public PathwayTableModel()
        {
           pathway = new LWPathway("default");
        }
        
        //sets the pathway to the given pathway
        public synchronized void setPathway(LWPathway pathway)
        {
            this.pathway = pathway;
            fireTableDataChanged();
        }
        
        //returns the current pathway
        public synchronized LWPathway getPathway()
        {
            return pathway;
        }
        
        //returns the number of row (nodes of the pathway)
        public synchronized int getRowCount()
        {
            if(pathway != null) 
                return pathway.length();
            
            else
                return 0;
        }
        
        public int getColumnCount()
        {
            return columnNames.length;
        }
        
        public String getColumnName(int column)
        {
            return "";
        }
        
        //a method which defines how what each cell should display
        public synchronized Object getValueAt(int row, int column)
        {
            try
            {
                switch(column)
                {                    
                    case 0:
                        LWComponent rowElement = pathway.getElement(row);
                        return rowElement.getLabel();
                    //case 1:
                       // return new String("Comments about the node");
                }
            }
            
            catch (Exception e)
            {
                System.err.println("exception in the table model:" + e);
            }
            
            return "";
        }
        
        //adds a row at the designated location
        public synchronized void addRow(LWComponent element, int row)
        {
            pathway.addElement(element, row);
            fireTableRowsInserted(row, row);
            
            //update the pathway control panel
            pathwayControl.updateControlPanel();
        }
        
        //deletes the given row from the table
        public synchronized void deleteRow(int row)
        {
            pathway.removeElement(row);
            fireTableRowsDeleted(row, row); 
            
            //update the pathway control panel
            pathwayControl.updateControlPanel();
        }
        
        //switches a row to a new location
        public void switchRow(int oldRow, int newRow)
        {
            pathway.moveElement(oldRow, newRow);
            fireTableDataChanged();
            
            //update the pathway control panel
            pathwayControl.updateControlPanel();
        }
    }
    
    /**A private class which defines how the combo box should be rendered*/
    private class PathwayRenderer extends BasicComboBoxRenderer 
    {
        public PathwayRenderer ()
        {
            super();
        }
        
        //a method which defines how to render cells
        public Component getListCellRendererComponent(JList list,
               Object value, int index, boolean isSelected, boolean cellHasFocus) 
        {
            //if the cell contains a pathway, then displays its label
            if (value instanceof LWPathway)
            {    
                LWPathway pathway = (LWPathway)value;
                if(pathway.getLabel().length() > 15)
                    setText(pathway.getLabel().substring(0, 13) + "...");
                else
                    setText(pathway.getLabel());
            }
            
            //if it is a string, then displays the string itself
            else
                setText((String)value);
            
            //setting the color to the default setting
            if (isSelected)
            {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            }
            
            else
            {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            
            return this;
        }   
    }
}
/* removed from PathwayTab */
        //moveUp = new JButton("^");
        //moveDown = new JButton("v");
        //submit = new JButton("Submit");
        //moveUp.addActionListener(this);
        //moveDown.addActionListener(this);
        //submit.addActionListener(this);
        //moveUp.setEnabled(false);
        //moveDown.setEnabled(false);
        //submit.setEnabled(false);
        //buttons.add(moveUp);
        //buttons.add(moveDown);
        //add(textPanel);
        //JScrollPane textScrollPane = new JScrollPane(text);
        //textScrollPane.setPreferredSize(new Dimension(200, 70));
        //textScrollPane.setBorder(new TitledBorder("Node Comment"));
        //JPanel textPanel = new JPanel();
        //textPanel.setLayout(new FlowLayout());
        //textPanel.add(textScrollPane);
        //textPanel.add(text);
        //textPanel.add(submit);
        //text.setLineWrap(true);
        //text.setWrapStyleWord(true);
        //JPanel temp = new JPanel(new BorderLayout());
        //add(temp);
        
