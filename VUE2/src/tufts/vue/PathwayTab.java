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
    //private PathwayControl pathwayControl;
    private JDialog parent;
    private JPanel buttons = null, pnPanel = null, pcPanel = null, buttonPanel = null;
    private JLabel pathName = null;
    private final Font selectedFont = new Font("SansSerif", Font.BOLD, 12);
    private final Font normalFont = new Font("SansSerif", Font.PLAIN, 12);
    
    /* Pathway control properties */
    
    private JButton firstButton, lastButton, forwardButton, backButton;
    private JButton removeButton, createButton;
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
        
        add(getRadioPanel());
        
        setPathwayControl();
        add(pcPanel);
        //pathwayControl = new PathwayControl(parent, this);
        //add(pathwayControl);
        
        setPathwayNameLabel();
        add(pnPanel);
        
        pathwayTable = new PathwayTable(this);
        add(new JScrollPane(pathwayTable));
        
        setButtons();
        add(buttons);
    }
    
    public void setButton(JButton button){
        button.addActionListener(this);
        buttonPanel.add(button);
    }
    
    
    public void setPathwayControl(){
        pcPanel = new JPanel(new BorderLayout());
        
        /* buttons that are not used */
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        buttonPanel.setBackground(Color.white);
        
        firstButton = new JButton("<<");
        lastButton = new JButton(">>");
        forwardButton = new JButton(">");
        backButton = new JButton("<");
        setButton(firstButton);
        setButton(backButton);
        setButton(forwardButton);
        setButton(lastButton);
        /* end of buttons that are not used */
        
        createButton = new JButton("Create New Path");
        createButton.addActionListener(this);
        removeButton = new JButton("Delete Selected Path");
        removeButton.addActionListener(this);
        
        JPanel pathwayPanel = new JPanel();
        pathwayPanel.add(createButton);
        pathwayPanel.add(removeButton);
        pcPanel.add(pathwayPanel, BorderLayout.SOUTH);
        
        pathwayList = new JComboBox();
        pathwayList.setRenderer(new PathwayRenderer());
        pathwayList.addItemListener(this);
        
        JPanel mainPanel = new JPanel();
        mainPanel.add(new JLabel("Select:"));
        mainPanel.add(pathwayList);
        mainPanel.add(new JLabel("???"));
        pcPanel.add(mainPanel, BorderLayout.CENTER);
    }
    
    public void setPathwayNameLabel(){
        pnPanel = new JPanel();
        text = new JTextField();
        text.setEditable(true);
        text.addKeyListener(this);
        //Document document = text.getDocument();
        //document.addDocumentListener(this);
        
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
        pathName = new JLabel("Path Name:");
        gridbag.setConstraints(pathName, c);
        pnPanel.add(pathName);
         
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
    /*
    public PathwayControl getPathwayControl(){
        if(this.pathwayControl == null){
            this.pathwayControl = new PathwayControl(parent, this);
        }
        return this.pathwayControl;
    }
    
    public void setPathwayControl(PathwayControl pathwayControl){
        this.pathwayControl = pathwayControl;
    }
    */
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
        updateControlPanel();
    }
    
    /**Notifies the table of data change*/
    public void updateTable()
    {
        ((PathwayTableModel)pathwayTable.getModel()).fireTableDataChanged();
    }
    
    //key events for the dialog box
    public void keyPressed(KeyEvent e) {}
    public void keyReleased(KeyEvent e) {}

    public void keyTyped(KeyEvent e){
        
        if(e.getKeyChar()== KeyEvent.VK_ENTER){
            System.out.println("focus owner: " + e.getSource());
            if (text.isFocusOwner())
                if(pathName.getText() == "Edit Name:"){
                    this.getCurrentPathway().setLabel(text.getText());
                    parent.repaint();
                }else{
                    addPathway(new LWPathway(text.getText()));
                    pathName.setText("Edit Name:");
                    text.setText(this.getCurrentPathway().getLabel());
                }
        }
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
        /*
         else
        {            
            LWComponent element = ((PathwayTableModel)pathwayTable.getModel()).getPathway().getElement(selected);
            element.setNotes(text.getText());
            //submit.setEnabled(false);
        }
        */
        else if (e.getSource() == firstButton){
          this.getCurrentPathway().getFirst();
          
        }
            
        //moves to the last node of the pathway
        else if (e.getSource() == lastButton){
          this.getCurrentPathway().getLast();
          
        }
        //moves to the next node of the pathway
        else if (e.getSource() == forwardButton){
          this.getCurrentPathway().getNext();
          
        }
        //moves to the previous node of the pathway
        else if (e.getSource() == backButton){
          this.getCurrentPathway().getPrevious();
          
        }
        
        //temporarily here
        else if (e.getSource() == removeButton){
          removePathway(this.getCurrentPathway());
          pathName.setText("Path Name:");
        }
        
        else if (e.getSource() == createButton){    
                //parent.setSize(350, 350);
                pathName.setText("Path Name:");
                text.setText("");
        }
          
        //notifies the change to the panel
        updateControlPanel();
        VUE.getActiveViewer().repaint();
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
        //LWPathway pathway = this.getPathway();
        //pathway.setLabel(text.getText());
    }
    
    public void insertUpdate(javax.swing.event.DocumentEvent documentEvent) 
    {
       //if(pathwayTable.getSelectedRow() != -1)
       //  submit.setEnabled(true);
       //LWPathway pathway = this.getPathway();
       //pathway.setLabel(text.getText());
    } 
    
    public void itemStateChanged(ItemEvent ie) {
        if (ie.getStateChange() == ItemEvent.SELECTED) 
        {
            //if the pathway was selected, then sets the current pathway to the selected pathway and updates accordingly
            
            if (pathwayList.getSelectedItem() instanceof LWPathway)
            {
                LWPathway pathway = (LWPathway)pathwayList.getSelectedItem();
                this.setCurrentPathway(pathway);
                pathName.setText("Edit Name:");
                text.setText(this.getCurrentPathway().getLabel());
                //parent.setSize(350, 650);
            }
            
            //if "no" pathway was selected, then set the current pathway to nothing and updates accordingly
            else if (pathwayList.getSelectedItem().equals(noPathway)){
                this.setCurrentPathway(null);
                //parent.setSize(350, 350);
                //pathName.setText("Path Name:");
                text.setText("");
            }
            //if "add" pathway was selected, then adds a pathway, sets it to the current pathway, and updates accordingly
            /*
            else if (pathwayList.getSelectedItem().equals(addPathway))
            {    
                PathwayDialog dialog = new PathwayDialog(parent, getLocationOnScreen());
                dialog.show();
            }
             */
        } 
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
            updateControlPanel();
        }
        
        //deletes the given row from the table
        public synchronized void deleteRow(int row)
        {
            pathway.removeElement(row);
            fireTableRowsDeleted(row, row); 
            
            //update the pathway control panel
            updateControlPanel();
        }
        
        //switches a row to a new location
        public void switchRow(int oldRow, int newRow)
        {
            pathway.moveElement(oldRow, newRow);
            fireTableDataChanged();
            
            //update the pathway control panel
            updateControlPanel();
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
    
    /*** Pathway Control methods ***/
    /**Sets the pathway manager to the given pathway manager*/
    public void setPathwayManager(LWPathwayManager pathwayManager)
    {
        this.pathwayManager = pathwayManager;
        
        //clears the combo box list 
        //come up with a better way
        pathwayList.removeAllItems();
        pathwayList.addItem(noPathway);
        //pathwayList.addItem(addPathway);
        
        //iterting through to add existing pathways to the combo box list
        for (Iterator i = pathwayManager.getPathwayIterator(); i.hasNext();)
           pathwayList.addItem((LWPathway)i.next());           
        
        //sets the current pathway to the current pathway of the manager
        if ((pathwayManager.getCurrentPathway() != null) 
            && (pathwayManager.getCurrentPathway().getCurrent() == null) )
          pathwayManager.getCurrentPathway().getFirst();
        pathwayList.setSelectedItem(pathwayManager.getCurrentPathway());
        
        updateControlPanel();
    }
    
    /**Returns the currently associated pathway manager*/
    public LWPathwayManager getPathwayManager()
    {
        return pathwayManager;
    }
    
    /**Adds the given pathway to the combo box list and the pathway manager*/
    public void addPathway(LWPathway newPathway)
    {
        pathwayList.addItem(newPathway);
        pathwayManager.addPathway(newPathway);
        
        //switches to the newly added pathway
        pathwayList.setSelectedIndex(pathwayList.getModel().getSize() - 1);        
    }
    
    /**Sets the current pathway to the given pathway and updates the control panel accordingly*/
    public void setCurrentPathway(LWPathway pathway)
    {
        if(this.getPathwayManager() != null)
            this.getPathwayManager().setCurrentPathway(pathway);
        
            
        //sets to the first node if there is no current node set
        if(pathway != null)
        {
            if ((pathway.getCurrent() == null) && (pathway.getFirst() != null) )
              pathway.getFirst();
        }       
        
        updateControlPanel(); 
    }
    
    /**Returns the currently selected pathway*/
    public LWPathway getCurrentPathway()
    {
        return this.getPathwayManager().getCurrentPathway();
    }
    
    /**A method which updates the widgets accordingly*/
    public void updateControlPanel()
    {
        //if there is a pathway currently selected
        if (this.getCurrentPathway() != null)
        {
            LWComponent currentElement = this.getCurrentPathway().getCurrent();
            
            //temporarily here 
            removeButton.setEnabled(true);
            
            //if there is a node in the pathway
            if(currentElement != null)
            {
                //sets the label to the current node's label
                //nodeLabel.setText(currentElement.getLabel());
          
                //if it is the first node in the pathway, then disables first and back buttons
                if (this.getCurrentPathway().isFirst())
                {
                    backButton.setEnabled(false);
                    firstButton.setEnabled(false);
                }
          
                else
                {
                    backButton.setEnabled(true);
                    firstButton.setEnabled(true);
                }
          
                //if it is the last node in the pathway, then disables last and forward buttons
                if (this.getCurrentPathway().isLast())
                {
                    forwardButton.setEnabled(false);
                    lastButton.setEnabled(false);
                }
            
                else
                {
                    forwardButton.setEnabled(true);
                    lastButton.setEnabled(true);
                }
            }
            
            //if there is no node in the pathway, disables every button
            else
            {
                firstButton.setEnabled(false);
                lastButton.setEnabled(false);
                forwardButton.setEnabled(false);
                backButton.setEnabled(false);
                //nodeLabel.setText(emptyLabel);
            }
        }
        
        //if currently no pathway is selected, disables all buttons and resets the label
        else
        {
            firstButton.setEnabled(false);
            lastButton.setEnabled(false);
            forwardButton.setEnabled(false);
            backButton.setEnabled(false);
            //nodeLabel.setText(emptyLabel);
            
            //temporarily here
            removeButton.setEnabled(false);
        }
    }
    
    /**Removes the given pathway from the combo box list and the pathway manager*/
    public void removePathway(LWPathway oldPathway)
    {
        //switches to the no pathway selected
        pathwayList.setSelectedIndex(0);
        
        pathwayList.removeItem(oldPathway);
        pathwayManager.removePathway(oldPathway);
    }
}








    /**A dialog displayed when the user chooses to add a new pathway to the current map */
    /*private class PathwayDialog extends JDialog implements ActionListener, KeyListener
    {
        JButton okButton, cancelButton;
        JTextField textField;
        
        public PathwayDialog(JDialog dialog, Point location)
        {
            super(dialog, "New Pathway Name", true);
            setSize(250, 100);
            setLocation(location);
            setUpUI();
        }
        
        public void setUpUI()
        {
            okButton = new JButton("Ok");
            cancelButton = new JButton("Cancel");
            
            okButton.addActionListener(this);
            okButton.addKeyListener(this);
            cancelButton.addActionListener(this);
            cancelButton.addKeyListener(this);
            
            textField = new JTextField("default", 18);
            textField.addKeyListener(this);
            textField.setPreferredSize(new Dimension(40, 20));
            
            JPanel buttons = new JPanel();
            buttons.setLayout(new FlowLayout());
            buttons.add(okButton);
            buttons.add(cancelButton);
            
            JPanel textPanel = new JPanel();
            textPanel.setLayout(new FlowLayout());
            textPanel.add(textField);
            
            Container dialogContentPane = getContentPane();
            dialogContentPane.setLayout(new BorderLayout());
            
            dialogContentPane.add(textPanel, BorderLayout.CENTER);
            dialogContentPane.add(buttons, BorderLayout.SOUTH);
        }
        
        public void actionPerformed(java.awt.event.ActionEvent e) 
        {
            if (e.getSource() == okButton)
            {
                //calls addPathway method defined in PathwayControl
                addPathway(new LWPathway(textField.getText()));
                dispose();
            }
            
            else if (e.getSource() == cancelButton)
            {
                //selects empty pathway if the cancel button was pressed
                pathwayList.setSelectedItem(noPathway);
                dispose();
            }
        }
        
        //key events for the dialog box
        public void keyPressed(KeyEvent e) {}
        public void keyReleased(KeyEvent e) {}
        
        public void keyTyped(KeyEvent e) 
        {
            //when enter is pressed
            if(e.getKeyChar()== KeyEvent.VK_ENTER)
            {
                //if the ok button or the text field has the focus, add a designated new pathway
                if (okButton.isFocusOwner() || textField.isFocusOwner())
                {    
                    addPathway(new LWPathway(textField.getText()));
                    dispose();                  
                }
                
                //else if the cancel button has the focus, just aborts it
                else if (cancelButton.isFocusOwner())
                {
                    //selects empty pathway if the cancel button was pressed
                    pathwayList.setSelectedItem(noPathway);
                    dispose(); //does catch event?
                }
            }
        }
        
    }*/

/* removed from PathwayTab */
        
        /*JPanel descriptionPanel = new JPanel();
        descriptionPanel.setLayout(new FlowLayout());
        descriptionPanel.setBackground(Color.white);
        descriptionPanel.add(new JLabel("Label:"));
        descriptionPanel.add(new JLabel(emptyLabel));
        */
        

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
        
