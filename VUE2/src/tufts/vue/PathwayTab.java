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
public class PathwayTab extends JPanel implements   ActionListener,
                                                    //ListSelectionListener,
                                                    DocumentListener,
                                                    KeyListener,
                                                    TableModelListener
{    
    //necessary widgets
    private PathwayTable pathwayTable = null;
    private PathwayTableModel tableModel = null;
    public JButton removeElement = null, addElement = null, moveUp = null, moveDown = null; //, submit 
    private JTextField text;
    private JLabel pathLabel = null;//, nodeLabel = null, slashLabel = new JLabel(" / ");
    
    private JDialog parent;
    private JPanel buttons = null, pnPanel = null, pcPanel = null, buttonPanel = null;
    private JLabel pathName = null;
    
private int bHeight = 23, bWidth = 42, bWidth2 = 48;
    /* Pathway control properties */
    
    private JButton firstButton, lastButton, forwardButton, backButton;
    private JButton removeButton, createButton;
    //private JComboBox pathwayList;
    private JPanel southNotes = null;
    private JTextArea notesArea = null;
    
    private LWPathwayManager pathwayManager = null;
    
    private final String noPathway = "                          ";
    private final String emptyLabel = "empty";
    
    private LWPathway dispPath = null;
    private LWComponent dispComp = null;
    /* end Pathway Control Properties */
    
    private String[] colNames = {"A", "B", "C", "D", "E"};
    private int[] colWidths = {20,20,20,100,20};
 
    /** Creates a new instance of PathwayTab */
    public PathwayTab(JDialog parent) 
    {   
        this.parent = parent;
        
        setLayout(new GridLayout(2,1,5,0));
        pathLabel = new JLabel();
        //nodeLabel = new JLabel();
        //used but not added to GUI
        setPathwayControl();
        setPathwayNameLabel();
        //end of used
        
        JPanel pathwayPanel = new JPanel(new BorderLayout());
        
        createButton = new JButton("+");
        createButton.setPreferredSize(new Dimension(bWidth, bHeight));
        createButton.addActionListener(this);
        removeButton = new JButton("-");
        removeButton.setPreferredSize(new Dimension(bWidth, bHeight));
        removeButton.addActionListener(this);
        
        JPanel editPathwaysPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        editPathwaysPanel.add(new JLabel("Add / Delete Pathways"));
        editPathwaysPanel.add(createButton);
        editPathwaysPanel.add(removeButton);
        
        pathwayPanel.add(editPathwaysPanel, BorderLayout.NORTH);
        
        JPanel tablePanel = new JPanel(new BorderLayout());
        FlowLayout flow = new FlowLayout(FlowLayout.RIGHT);
        JPanel northPanel = new JPanel(flow);
   
        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        //pathway table setup
        tableModel = new PathwayTableModel(this);   
        
        pathwayTable = new PathwayTable(this);
        pathwayTable.setModel(tableModel);
        pathwayTable.setDefaultRenderer(String.class, new javax.swing.table.DefaultTableCellRenderer());        
        
        for(int i = 0; i < 5; i++){
            TableColumn col = pathwayTable.getColumn(colNames[i]);
            //col.setPreferredWidth(colWidths[i]);
            if(i != 3) col.setMaxWidth(colWidths[i]);
        } 
        
        northPanel.add(new JLabel("Highlight a path to playback:"));
        northPanel.add(buttonPanel);
        northPanel.setBackground(new Color(200, 200, 255));
        
        setButtons();
        southPanel.add(new JLabel("Add selected object on map to path"));
        southPanel.add(buttons);
        southPanel.setBackground(new Color(200, 200, 255));
        
        tablePanel.add(northPanel, BorderLayout.NORTH);
        tablePanel.add(new JScrollPane(pathwayTable), BorderLayout.CENTER);
        tablePanel.add(southPanel, BorderLayout.SOUTH);
        
        pathwayPanel.add(tablePanel, BorderLayout.CENTER);
        
        add(pathwayPanel);
        
        JPanel notesPanel = new JPanel(new BorderLayout());
        southNotes = new JPanel(new FlowLayout(FlowLayout.LEFT));
     
        //southNotes.add(new JLabel("Notes: "));
        southNotes.add(pathLabel);
        pathLabel.setText("Notes: ");
        //southNotes.add(slashLabel);
        //southNotes.add(nodeLabel);
        notesPanel.add(southNotes, BorderLayout.NORTH);
        
        notesArea = new JTextArea("");
        notesArea.setColumns(5);
        notesArea.addKeyListener(this);
        notesPanel.add(notesArea, BorderLayout.CENTER);
        
        add(notesPanel);
        
    }
    
    public void setButton(JButton button){
        button.addActionListener(this);
        button.setPreferredSize(new Dimension(bWidth2, bHeight));
        buttonPanel.add(button);
    }
    
    public JDialog getDialogParent(){
        return parent;
    }
    /*
    public JComboBox getPathwayList(){
        return pathwayList;
    }
    */
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
        
        JPanel mainPanel = new JPanel();
        mainPanel.add(new JLabel("Select:"));
        
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
        
        buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        addElement = new JButton("+");
        addElement.setPreferredSize(new Dimension(bWidth, bHeight));
        addElement.addActionListener(this);
        addElement.setEnabled(false);
        removeElement = new JButton("-");
        removeElement.setPreferredSize(new Dimension(bWidth, bHeight));
        removeElement.addActionListener(this);
        removeElement.setEnabled(false);
        
        //toggles the add button's availability depending on the selection
        VUE.ModelSelection.addListener( new LWSelection.Listener() {
                public void selectionChanged(LWSelection selection)
                {
                    if (!selection.isEmpty() && VUE.getActiveMap().getPathwayManager().length() > 0)
                      addElement.setEnabled(true);
                    else
                      addElement.setEnabled(false);
                }
            }     
        );
        
        buttons.add(addElement);
        buttons.add(removeElement);
        
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
        //((PathwayTableModel)pathwayTable.getModel()).setPathway(pathway);
        
        
        //disables the add button if the pathway is not selected
        if (pathway == null)
          addElement.setEnabled(false);
        
        //if the pathway is selected and map items are selected, enable the add button
        else if (!VUE.ModelSelection.isEmpty())
          addElement.setEnabled(true);
    }
    
    /**Gets the current pathway associated with the pathway table*/
    /*public LWPathway getPathway()
    {
        return ((PathwayTableModel)pathwayTable.getModel()).getPathway();
    }
    */
    /**Sets the current node of the pathway to be the selected one from the table*/
    /*public void setCurrentElement(int index)
    {
       ((PathwayTableModel)pathwayTable.getModel()).getPathway().setCurrentIndex(index);
       
        //update the pathway control panel
        updateControlPanel();
    }*/
    
    public PathwayTableModel getPathwayTableModel(){
        return this.tableModel;
    }
    
    public PathwayTable getPathwayTable(){
        return this.pathwayTable;
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
        if (notesArea.isFocusOwner())
            if(dispPath != null){
                if(dispComp != null){
                    dispComp.setNotes(notesArea.getText());
                }else
                    dispPath.setNotes(notesArea.getText());
            }        
    }
    
    /**Reacts to actions dispatched by the buttons*/
    public void actionPerformed(ActionEvent e)
    {
        int selected = pathwayTable.getSelectedRow();
        
        if (e.getSource() == removeElement){    
             Object obj = this.getPathwayTableModel().getElement(selected);
             if (obj != null && obj instanceof LWComponent){   
                LWComponent comp = (LWComponent)obj;
                this.getPathwayTableModel().removeElement(comp);
                //this.getPathwayTableModel().fireTableDataChanged();               
             }  
        }            
        else if (e.getSource() == addElement){
            LWComponent array[] = VUE.ModelSelection.getArray();             
            this.getPathwayTableModel().addElements(array);            
            //this.getPathwayTableModel().fireTableDataChanged();
        }
        else if (e.getSource() == firstButton){
          this.getCurrentPathway().getFirst();
        }
        else if (e.getSource() == lastButton){
          this.getCurrentPathway().getLast();
        }
        else if (e.getSource() == forwardButton){
          this.getCurrentPathway().getNext();
        }
        else if (e.getSource() == backButton){
          this.getCurrentPathway().getPrevious();
        }
        else if (e.getSource() == removeButton){
            removePathway(this.getCurrentPathway());
        }
        else if (e.getSource() == createButton){    
            PathwayDialog dialog = new PathwayDialog(this, getLocationOnScreen());
            dialog.show();
        }
          
        this.getPathwayTableModel().fireTableDataChanged();
        updateControlPanel();
        VUE.getActiveViewer().repaint();
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
    /*
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
            
            else if (pathwayList.getSelectedItem().equals(addPathway))
            {    
                PathwayDialog dialog = new PathwayDialog(parent, getLocationOnScreen());
                dialog.show();
            }
             
        } 
    }*/
   
    /*** Pathway Control methods ***/
    /**Sets the pathway manager to the given pathway manager*/
    
    public void setPathwayManager(LWPathwayManager pathwayManager)
    {
        this.pathwayManager = pathwayManager;
        
        //clears the combo box list 
        //come up with a better way
        //pathwayList.removeAllItems();
        //pathwayList.addItem(noPathway);
        //pathwayList.addItem(addPathway);
        
        //iterting through to add existing pathways to the combo box list
        for (Iterator i = pathwayManager.getPathwayIterator(); i.hasNext();) {
            LWPathway p = (LWPathway) i.next();
            System.out.println("PathwayTab.setPathwayManger: FAILING TO HANDLE " + p);
            // OKAY -- how do we get this into the GUI? -- SMF
            //pathwayList.addItem((LWPathway)i.next());
        }
        
        //sets the current pathway to the current pathway of the manager
        if ((pathwayManager.getCurrentPathway() != null) 
            && (pathwayManager.getCurrentPathway().getCurrent() == null) )
          pathwayManager.getCurrentPathway().getFirst();
        //pathwayList.setSelectedItem(pathwayManager.getCurrentPathway());
        
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
        //pathwayList.addItem(newPathway);
        //pathwayManager.addPathway(newPathway);
        
        //switches to the newly added pathway
        //pathwayList.setSelectedIndex(pathwayList.getModel().getSize() - 1);        
    }
    
    /**Sets the current pathway to the given pathway and updates the control panel accordingly*/
    public void setCurrentPathway(LWPathway pathway)
    {
        //if(this.getPathwayManager() != null)
        //    this.getPathwayManager().setCurrentPathway(pathway);
        this.getPathwayTableModel().setCurrentPathway(pathway);
            
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
        //return this.getPathwayManager().getCurrentPathway();
        return this.getPathwayTableModel().getCurrentPathway();
    }
    
    /**A method which updates the widgets accordingly*/
    public void updateControlPanel()
    {
        if (this.getCurrentPathway() != null){
            LWComponent currentElement = this.getCurrentPathway().getCurrent();
            
            removeButton.setEnabled(true);
            
            if(currentElement != null){
                if (this.getCurrentPathway().isFirst()){
                    backButton.setEnabled(false);
                    firstButton.setEnabled(false);
                }else{
                    backButton.setEnabled(true);
                    firstButton.setEnabled(true);
                }
          
                if (this.getCurrentPathway().isLast()){
                    forwardButton.setEnabled(false);
                    lastButton.setEnabled(false);
                }else {
                    forwardButton.setEnabled(true);
                    lastButton.setEnabled(true);
                }
            }else{
                firstButton.setEnabled(false);
                lastButton.setEnabled(false);
                forwardButton.setEnabled(false);
                backButton.setEnabled(false);
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
        //pathwayList.setSelectedIndex(0);
        
        //pathwayList.removeItem(oldPathway);
        //pathwayManager.removePathway(oldPathway);
        ((PathwayTableModel)this.pathwayTable.getModel()).removePathway(oldPathway);
    }
    
    public void updateLabels(String text, String notes, LWPathway path, LWComponent comp){
        pathLabel.setText(text);   
        pathLabel.repaint();
        
        notesArea.setText(notes);
        notesArea.repaint();
        
        dispPath = path;
        dispComp = comp;
    }
    
    public void tableChanged(javax.swing.event.TableModelEvent tableModelEvent) {
        this.repaint();
    }
    
}
