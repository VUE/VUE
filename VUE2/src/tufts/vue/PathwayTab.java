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
                                                    TableModelListener,
                                                    MapViewer.Listener//,
                                                    //LWSelection.Listener
{    
    //necessary widgets
    private PathwayTable pathwayTable = null;
    private PathwayTableModel tableModel = null;
    public JButton removeElement = null, addElement = null, moveUp = null, moveDown = null; //, submit 
    private JTextField text;
    private JLabel pathLabel = null;//, nodeLabel = null, slashLabel = new JLabel(" / ");
    
    //private Frame parent;
    private JDialog parent;
    private JPanel buttons = null, pnPanel = null, pcPanel = null, buttonPanel = null;
    private JLabel pathName = null;
    
    private boolean elemSelection = false;
    
    private int bHeight = 23, bWidth = 42, bWidth2 = 48;
    
    //private Font defaultFont = new Font("Helvetica", Font.PLAIN, 12);
    //private Font highlightFont = new Font("Helvetica", Font.BOLD, 12);
    private Font defaultFont = null;
    private Font highlightFont = null;
    /* Pathway control properties */

    private JButton firstButton = new JButton(VueResources.getImageIcon("controlRewindUp"));
    private JButton backButton = new JButton(VueResources.getImageIcon("controlPlayBackwardUp"));
    private JButton forwardButton = new JButton(VueResources.getImageIcon("controlPlayForwardUp"));
    private JButton lastButton = new JButton(VueResources.getImageIcon("controlForwardUp"));
    
    private ImageIcon addIcon = VueResources.getImageIcon("addLight");
    private ImageIcon deleteIcon = VueResources.getImageIcon("deleteLight");
        
    private JButton removeButton, createButton, lockButton;
    //private JComboBox pathwayList;
    private JPanel southNotes = null;
    private JTextArea notesArea = null;
    
    //private LWPathwayManager pathwayManager = null;
    
    private final String noPathway = "                          ";
    private final String emptyLabel = "empty";
    
    private LWPathway dispPath = null;
    private LWComponent dispComp = null;
    
    private Color bgColor = new Color(241, 243, 246);
    private Color altbgColor = new Color(186, 196, 222);
    
    /* end Pathway Control Properties */
    
    private String[] colNames = {"A", "B", "C", "D", "E", "F"};
    private int[] colWidths = {20,20,20,100,20,20};
 
    /** Creates a new instance of PathwayTab */
    public PathwayTab(JDialog parent) 
    //public PathwayTab(Frame parent) 
    {   
        this.parent = parent;
        this.setBorder(BorderFactory.createMatteBorder(4, 4, 7, 4, bgColor));
        defaultFont = this.getFont();
        highlightFont = this.getFont();
        highlightFont.deriveFont(Font.BOLD);
        
        pathLabel = new JLabel();
        
        setPathwayControl();
        setPathwayNameLabel();
        
         
        createButton = new JButton(addIcon);
        createButton.setPreferredSize(new Dimension(17, 17));
        createButton.addActionListener(this);
        
        removeButton = new JButton(deleteIcon);
        removeButton.setPreferredSize(new Dimension(17, 17));
        removeButton.addActionListener(this);
        
        lockButton = new JButton(VueResources.getImageIcon("lock"));
        lockButton.setPreferredSize(new Dimension(17, 17));
        lockButton.addActionListener(this);
        
        JPanel editPathwaysPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        editPathwaysPanel.setBackground(bgColor);
        
        JLabel lab = new JLabel("Pathways");
        lab.setBackground(bgColor);
        lab.setFont(defaultFont);
        
        JPanel groupPanel = new JPanel(new GridLayout(1, 3, 0, 0));
        groupPanel.setPreferredSize(new Dimension(56, 25));
        groupPanel.add(createButton);
        groupPanel.add(removeButton);
        groupPanel.add(lockButton);
        
        lab.setBorder(BorderFactory.createMatteBorder(8, 0, 0, 5, this.bgColor));
        groupPanel.setBorder(BorderFactory.createMatteBorder(8, 0, 0, 5, this.bgColor));
        
        editPathwaysPanel.add(lab);
        editPathwaysPanel.add(groupPanel);
        
        
        JLabel questionLabel = new JLabel(VueResources.getImageIcon("smallInfo"), JLabel.LEFT);
        questionLabel.setPreferredSize(new Dimension(22, 17));
        questionLabel.setBackground(altbgColor);
        questionLabel.setToolTipText("Check boxes below to display paths on the map. Click on path's layer to make a specific path active for editing or playback.");
        
        
        JLabel filler = new JLabel(" ");
        filler.setBackground(altbgColor);
        
        
        JLabel playBackLabel = new JLabel("Highlight a path to playback:   ");
        playBackLabel.setFont(defaultFont);
        playBackLabel.setBackground(altbgColor);
        
        JPanel buttonGroupPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonGroupPanel.setBackground(altbgColor);
        buttonGroupPanel.setBorder(null);
        buttonGroupPanel.add(playBackLabel);
        buttonGroupPanel.add(buttonPanel); // buttonPanel setup in setPathwayControl
        
        
        JPanel northPanel = new JPanel(new BorderLayout(0,0));
        northPanel.setBackground(altbgColor);
        
        questionLabel.setBorder(BorderFactory.createMatteBorder(0, 5, 0, 0, this.altbgColor));
        buttonGroupPanel.setBorder(BorderFactory.createMatteBorder(8, 0, 0, 5, this.altbgColor));
        northPanel.add(questionLabel, BorderLayout.WEST);
        northPanel.add(buttonGroupPanel, BorderLayout.EAST);
        
        //pathway table setup
        tableModel = new PathwayTableModel(this);   
        
        pathwayTable = new PathwayTable(this);
        pathwayTable.setBackground(bgColor);
        pathwayTable.setModel(tableModel);
        
        for(int i = 0; i < 6; i++){
            TableColumn col = pathwayTable.getColumn(colNames[i]);
            if(i != 3) col.setMaxWidth(colWidths[i]);
        } 
        
        JScrollPane tablePane = new JScrollPane(pathwayTable);
        
        setButtons();
        //toggles the add button's availability depending on the selection
        VUE.ModelSelection.addListener( new LWSelection.Listener() {
                public void selectionChanged(LWSelection sel)
                {
                    setElemSelection(!sel.isEmpty());
                    if (!sel.isEmpty()){
                        if (VUE.getActiveMap().getPathwayManager().length() > 0
                            && !VUE.getActiveMap().getPathwayManager().getCurrentPathway().getLocked())
                            addElement.setEnabled(true);
                        else
                            addElement.setEnabled(false);
                    }else{
                        addElement.setEnabled(false);
                    }
                }
            }     
        );
        
        JLabel addLabel = new JLabel("Add selected object on map to pathway");
        addLabel.setFont(defaultFont);
        addLabel.setBackground(altbgColor);
        
        buttons.setBorder(BorderFactory.createMatteBorder(8, 0, 0, 5, this.altbgColor));
        addLabel.setBorder(BorderFactory.createMatteBorder(8, 0, 0, 5, this.altbgColor));
        
        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        southPanel.setBackground(altbgColor);
        southPanel.add(addLabel);
        southPanel.add(buttons); // buttons panel setup in setButtons() method
        
        /* Layout for the table components */
        
        GridBagLayout bagLayout = new GridBagLayout();
        GridBagConstraints gc = new GridBagConstraints();
        
        JPanel tablePanel = new JPanel();
        tablePanel.setBackground(bgColor);
        tablePanel.setLayout(bagLayout);
        
        gc.gridwidth = GridBagConstraints.REMAINDER;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.gridheight = 1;
        
        northPanel.setPreferredSize(new Dimension(this.getWidth(), 15));
        northPanel.setBorder(BorderFactory.createMatteBorder(1,1,1,1,Color.gray));
        bagLayout.setConstraints(northPanel, gc);
        tablePanel.add(northPanel);
        
        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 3.0;
        gc.gridheight = 18;
        tablePane.setPreferredSize(new Dimension(this.getWidth(), 220));
        tablePane.setBorder(BorderFactory.createMatteBorder(0,1,0,1,Color.gray));
        bagLayout.setConstraints(tablePane, gc);
        tablePanel.add(tablePane);
        
        gc.weighty = 0.0;
        gc.gridheight = 1;
        southPanel.setPreferredSize(new Dimension(this.getWidth(), 15));
        southPanel.setBorder(BorderFactory.createMatteBorder(1,1,1,1,Color.gray));
        bagLayout.setConstraints(southPanel, gc);
        tablePanel.add(southPanel);
        
        /* End of Layout for table components */
        
        southNotes = new JPanel(new FlowLayout(FlowLayout.LEFT));
        southNotes.setBackground(bgColor);
        
        southNotes.add(pathLabel);
        pathLabel.setText("Notes: ");
        
        notesArea = new JTextArea("");
        notesArea.setColumns(5);
        notesArea.setWrapStyleWord(true);
        notesArea.setAutoscrolls(true);
        notesArea.setLineWrap(true);
        notesArea.addKeyListener(this);
        notesArea.setBackground(Color.white);
        notesArea.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED, Color.white, Color.darkGray));
     
        JPanel notesPanel = new JPanel(new BorderLayout(0,0));
        notesPanel.setBackground(bgColor);
        
        notesPanel.add(southNotes, BorderLayout.NORTH);
        notesPanel.add(new JScrollPane(notesArea), BorderLayout.CENTER);
        
        /* Layout for pathways tab */
        
        GridBagLayout bag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(bag);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridheight = 1;
        c.weightx = 1.0;
        
        editPathwaysPanel.setPreferredSize(new Dimension(this.getWidth(), 20));
        bag.setConstraints(editPathwaysPanel, c);
        add(editPathwaysPanel);
        
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1.0;
        c.gridheight = 11;
        tablePanel.setPreferredSize(new Dimension(this.getWidth(), 250));
        bag.setConstraints(tablePanel, c);
        add(tablePanel);
        
        c.gridheight = 8;
        notesPanel.setPreferredSize(new Dimension(this.getWidth(), 180));
        bag.setConstraints(notesPanel, c);
        add(notesPanel);
        
        /* End of Layout for pathways tab */
        
        
    }
    
    
    public void setAddElementEnabled(){
        System.out.println("selection: " + this.elemSelection);
        if(this.elemSelection && !this.getPathwayTableModel().getCurrentPathway().getLocked())
            this.addElement.setEnabled(true);
        else
            this.addElement.setEnabled(false);
    }
    
    public void setElemSelection(boolean val){
        System.out.println("set element selection: " + val);
        this.elemSelection = val;
    }
    /*
    public void selectionChanged(LWSelection sel) {
        System.out.println("setting selection in Pathway Tab...");
        if(!sel.isEmpty())
            selection = true;
        else
            selection = false;
        /*if(!compArray.equals(selection.getArray())){
            compArray = selection.getArray();
            System.out.println("compArray different from selection.getArray()");
            if(compArray != null && compArray.length != 0
                && VUE.getPathwayInspector().getCurrentPathway() != null)
            {
                System.out.println("compArray size: "+compArray.length);
                this.addElement.setEnabled(true);
            }
            else
                this.addElement.setEnabled(false);
                
         }
    }*/
    
    public void setButton(JButton button){
        button.addActionListener(this);
        buttonPanel.add(button);
    }
    
    public JDialog getDialogParent(){
    //public Frame getParentFrame(){
        return parent;
    }
    /*
    public JComboBox getPathwayList(){
        return pathwayList;
    }
    */
    public void setPathwayControl(){
        //pcPanel = new JPanel(new BorderLayout());
        
        /* buttons that are not used */
        buttonPanel = new JPanel(new GridLayout(1, 4, 1, 0));
        buttonPanel.setPreferredSize(new Dimension(79, 19));
        buttonPanel.setBackground(altbgColor);
        
        firstButton.setPreferredSize(new Dimension(19, 19));
        lastButton.setPreferredSize(new Dimension(19, 19));
        forwardButton.setPreferredSize(new Dimension(19, 19));
        backButton.setPreferredSize(new Dimension(19, 19));
        
        setButton(firstButton);
        setButton(backButton);
        setButton(forwardButton);
        setButton(lastButton);
        
        /*JPanel mainPanel = new JPanel();
        mainPanel.add(new JLabel("Select:"));
        
        mainPanel.add(new JLabel("???"));
        pcPanel.add(mainPanel, BorderLayout.CENTER);*/
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
        
        addElement = new JButton(addIcon);
        addElement.setPreferredSize(new Dimension(16, 16));
        addElement.addActionListener(this);
        addElement.setEnabled(false);
        
        removeElement = new JButton(deleteIcon);
        removeElement.setPreferredSize(new Dimension(16, 16));
        removeElement.addActionListener(this);
        removeElement.setEnabled(false);
        
        buttons = new JPanel(new GridLayout(0, 2, 1, 0));
        buttons.setBackground(altbgColor);
        
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
        if (notesArea.isFocusOwner()){
            if(dispPath != null){
                if(dispComp != null){
                    dispComp.setNotes(notesArea.getText());
                }else
                    dispPath.setNotes(notesArea.getText());
            } 
        }
        this.getPathwayTableModel().fireTableDataChanged();
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
        else if (e.getSource() == lockButton){
            int currentRow = this.getPathwayTableModel().getManager().getPathwayIndex(
                this.getPathwayTableModel().getCurrentPathway());
            this.getPathwayTable().setValueAt(this, currentRow, 5);
            this.setAddElementEnabled();
        }
        
        this.getPathwayTableModel().fireTableDataChanged();
        updateControlPanel();
        VUE.getActiveMap().notify(this, LWCEvent.Repaint);
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
    
    public void setPathwayManager(LWPathwayManager manager){
        this.getPathwayTableModel().setPathwayManager(manager);
    }
    
    public void setPathwayManager()
    {
        //this.pathwayManager = pathwayManager;
        this.getPathwayTableModel().setPathwayManager(VUE.getActiveMap().getPathwayManager());
        //iterting through to add existing pathways to the combo box list
        /*
        for (Iterator i = pathwayManager.getPathwayIterator(); i.hasNext();) {
            LWPathway p = (LWPathway) i.next();
            System.out.println("PathwayTab.setPathwayManger: FAILING TO HANDLE " + p);
            // OKAY -- how do we get this into the GUI? -- SMF
            //pathwayList.addItem((LWPathway)i.next());
        }
        */
        
        //sets the current pathway to the current pathway of the manager
        /*
        if ((pathwayManager.getCurrentPathway() != null) 
            && (pathwayManager.getCurrentPathway().getCurrent() == null) )
          pathwayManager.getCurrentPathway().getFirst();
        //pathwayList.setSelectedItem(pathwayManager.getCurrentPathway());
        */
        
        updateControlPanel();
    }
    
    /**Returns the currently associated pathway manager*/
/*    public LWPathwayManager getPathwayManager()
    {
        return pathwayManager;
    }
  */  
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
    
    final int TitleChangeMask = MapViewerEvent.DISPLAYED;
    
    public void mapViewerEventRaised(MapViewerEvent e) {
        if ((e.getID() & TitleChangeMask) != 0){
               this.setPathwayManager();
               VUE.getPathwayInspector().repaint();
               //System.out.println("Map Viewer Event: "+e.getID());
               //System.out.println("Active Map: "+VUE.getActiveMap().getLabel());
            }   
    }
    
}
