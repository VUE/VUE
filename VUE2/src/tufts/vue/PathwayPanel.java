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
 * 8-fold cyclic repetition of a beta-strand-loop-alpha- helix-loop module with helix alpha 7 missing. 
 * @author  Daisuke Fujiwara
 */

/**A class which displays nodes in a pathway */
public class PathwayPanel extends JPanel implements   ActionListener,
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
    
    private JFrame parent;
    //private JDialog parent;
    private JPanel buttons = null, pnPanel = null, pcPanel = null, buttonPanel = null;
    private JLabel pathName = null;
    
    private boolean elemSelection = false;
    
    private int bHeight = 23, bWidth = 42, bWidth2 = 48;
    
    //private Font defaultFont = new Font("Helvetica", Font.PLAIN, 12);
    //private Font highlightFont = new Font("Helvetica", Font.BOLD, 12);
    private Font defaultFont = null;
    private Font highlightFont = null;
    /* Pathway control properties */

    private ImageIcon firstUp = VueResources.getImageIcon("controlRewindUp");
    private ImageIcon backUp = VueResources.getImageIcon("controlPlayBackwardUp");
    private ImageIcon forwardUp = VueResources.getImageIcon("controlPlayForwardUp");
    private ImageIcon lastUp = VueResources.getImageIcon("controlForwardUp");
    private ImageIcon firstDown = VueResources.getImageIcon("controlRewindDown");
    private ImageIcon backDown = VueResources.getImageIcon("controlPlayBackwardDown");
    private ImageIcon forwardDown = VueResources.getImageIcon("controlPlayForwardDown");
    private ImageIcon lastDown = VueResources.getImageIcon("controlForwardDown");
    private ImageIcon firstDisabled = VueResources.getImageIcon("controlRewindDisabled");
    private ImageIcon backDisabled = VueResources.getImageIcon("controlPlayBackwardDisabled");
    private ImageIcon forwardDisabled = VueResources.getImageIcon("controlPlayForwardDisabled");
    private ImageIcon lastDisabled = VueResources.getImageIcon("controlForwardDisabled");
    
    private ImageIcon addUpIcon = VueResources.getImageIcon("addPathwayUp");
    private ImageIcon addDownIcon = VueResources.getImageIcon("addPathwayDown");
    private ImageIcon addDisabledIcon = VueResources.getImageIcon("addDisabled");
    private ImageIcon deleteUpIcon = VueResources.getImageIcon("deletePathwayUp");
    private ImageIcon deleteDownIcon = VueResources.getImageIcon("deletePathwayDown");
    private ImageIcon deleteDisabledIcon = VueResources.getImageIcon("deleteDisabled");
    private ImageIcon lockUpIcon = VueResources.getImageIcon("lockUp");
        
    private JButton removeButton, createButton, lockButton;
    private JButton firstButton, backButton, forwardButton, lastButton;
    private JPanel southNotes = null;
    private JTextArea notesArea = null;
    
    private final String noPathway = "                          ";
    private final String emptyLabel = "empty";
    
    private LWPathway dispPath = null;
    private LWComponent dispComp = null;
    
    private Color bgColor = new Color(241, 243, 246);
    private Color altbgColor = new Color(186, 196, 222);
    
    /* end Pathway Control Properties */
    
    private String[] colNames = {"A", "B", "C", "D", "E", "F"};
    private int[] colWidths = {20,20,20,100,20,20};
 
    /** Creates a new instance of PathwayPanel */

    public PathwayPanel(JFrame parent) 
    {   
        this.parent = parent;
        this.setBorder(BorderFactory.createMatteBorder(4, 4, 7, 4, bgColor));
        defaultFont = this.getFont();
        highlightFont = this.getFont();
        highlightFont.deriveFont(Font.BOLD);
        
        pathLabel = new JLabel();
        
        setPathwayControl();
        setPathwayNameLabel();
        
         
        createButton = new JButton(addUpIcon);
        createButton.setSelectedIcon(addDownIcon);
        createButton.setDisabledIcon(this.addDisabledIcon);
        createButton.addActionListener(this);
        createButton.setBorderPainted(false);
        createButton.setBackground(Color.white);
        
        removeButton = new JButton(deleteUpIcon);
        removeButton.setSelectedIcon(deleteDownIcon);
        removeButton.setDisabledIcon(this.deleteDisabledIcon);
        removeButton.addActionListener(this);
        removeButton.setBorderPainted(false);
        removeButton.setBackground(Color.white);
        
        lockButton = new JButton(lockUpIcon);
        lockButton.setSelectedIcon(lockUpIcon);
        lockButton.setBackground(Color.white);
        lockButton.addActionListener(this);
        lockButton.setBorderPainted(false);
        
        JPanel editPathwaysPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        editPathwaysPanel.setBackground(bgColor);
        
        JLabel lab = new JLabel("Pathways");
        lab.setBackground(bgColor);
        lab.setFont(defaultFont);
        lab.setBorder(BorderFactory.createMatteBorder(8, 0, 0, 5, this.bgColor));
        
        JPanel groupPanel = new JPanel(new GridLayout(1, 3, 0, 0));
        groupPanel.setPreferredSize(new Dimension(57, 24));
        groupPanel.setBorder(BorderFactory.createMatteBorder(8, 0, 0, 5, this.bgColor));
        groupPanel.setBackground(bgColor);
        groupPanel.add(createButton);
        groupPanel.add(removeButton);
        groupPanel.add(lockButton);
        
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
        tableModel = new PathwayTableModel();
        
        pathwayTable = new PathwayTable(this, tableModel);
        pathwayTable.setBackground(bgColor);
        
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
                        LWPathway path = VUE.getActivePathway();
                        addElement.setEnabled(path != null && !path.isLocked());
                        removeElement.setEnabled(path != null && !path.isLocked() && path.length() > 0);
                        /*
                        if (VUE.getActiveMap().getPathwayManager().length() > 0
                            && !VUE.getActiveMap().getPathwayManager().getCurrentPathway().isLocked())
                            addElement.setEnabled(true);
                        else
                            addElement.setEnabled(false);
                        
                        LWPathway path = VUE.getActiveMap().getPathwayManager().getCurrentPathway();
                        if (path != null && !path.isLocked() && path.getCurrentIndex() > -1)
                            removeElement.setEnabled(true);
                        else
                            removeElement.setEnabled(false);
                        */
                    }else{
                        addElement.setEnabled(false);
                        removeElement.setEnabled(false);
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
        
        northPanel.setPreferredSize(new Dimension(this.getWidth(), 30));
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

        //southPanel.setPreferredSize(new Dimension(this.getWidth(), 30));
        //southPanel.setPreferredSize(new Dimension(this.getWidth(), 15));
        
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
        
        editPathwaysPanel.setPreferredSize(new Dimension(this.getWidth(), 35));
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

    /**Returns the currently selected pathway*/
    private LWPathway getSelectedPathway()
    {
        return VUE.getActivePathway();
    }
    /*
    public LWPathway getCurrentPathway()
    {
        //return this.getPathwayManager().getCurrentPathway();
        return getTableModel().getCurrentPathway();
    }
    */
    
    
    
    public void setAddElementEnabled(){
        System.out.println("selection: " + this.elemSelection);
        //SMF if(this.elemSelection && !getTableModel().getCurrentPathway().isLocked())
        this.addElement.setEnabled(this.elemSelection && !VUE.getActivePathway().isLocked());
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
    
    //public JDialog getDialogParent(){
    public JFrame getParentFrame(){
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
        buttonPanel = new JPanel(new GridLayout(1, 4, 0, 0));
        buttonPanel.setPreferredSize(new Dimension(80, 20));
        buttonPanel.setBackground(altbgColor);
        
        firstButton = new JButton(this.firstUp);
        firstButton.setSelectedIcon(this.firstDown);
        firstButton.setDisabledIcon(this.firstDisabled);
        firstButton.addActionListener(this);
        firstButton.setBackground(this.altbgColor);
        firstButton.setBorderPainted(false);
        
        backButton = new JButton(this.backUp);
        backButton.setSelectedIcon(this.backDown);
        backButton.setDisabledIcon(this.backDisabled);
        backButton.addActionListener(this);
        backButton.setBackground(this.altbgColor);
        backButton.setBorderPainted(false);
        
        forwardButton = new JButton(this.forwardUp);
        forwardButton.setSelectedIcon(this.forwardDown);
        forwardButton.setDisabledIcon(this.forwardDisabled);
        forwardButton.addActionListener(this);
        forwardButton.setBackground(this.altbgColor);
        forwardButton.setBorderPainted(false);
        
        lastButton = new JButton(this.lastUp);
        lastButton.setSelectedIcon(this.lastDown);
        lastButton.setDisabledIcon(this.lastDisabled);
        lastButton.addActionListener(this);
        lastButton.setBackground(this.altbgColor);
        lastButton.setBorderPainted(false);
       
        buttonPanel.add(firstButton);
        buttonPanel.add(backButton);
        buttonPanel.add(forwardButton);
        buttonPanel.add(lastButton);
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
        
        addElement = new JButton(addUpIcon);
        addElement.setSelectedIcon(addDownIcon);
        addElement.setDisabledIcon(this.addDisabledIcon);
        addElement.setBackground(this.altbgColor);
        addElement.addActionListener(this);
        addElement.setEnabled(false);
        addElement.setBorderPainted(false);
        
        
        removeElement = new JButton(deleteUpIcon);
        removeElement.setSelectedIcon(deleteDownIcon);
        removeElement.setDisabledIcon(this.deleteDisabledIcon);
        removeElement.setBackground(this.altbgColor);
        removeElement.addActionListener(this);
        removeElement.setEnabled(false);
        removeElement.setBorderPainted(false);
        
        buttons = new JPanel(new GridLayout(1, 2, 0, 0));
        buttons.setBackground(altbgColor);
        
        buttons.add(addElement);
        buttons.add(removeElement);
        buttons.setPreferredSize(new Dimension(40,24));
        
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
    
    public PathwayTableModel getTableModel(){
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
                    getSelectedPathway().setLabel(text.getText());
                    parent.repaint();
                }else{
                    addPathway(new LWPathway(text.getText()));
                    pathName.setText("Edit Name:");
                    text.setText(getSelectedPathway().getLabel());
                }                
        }
        if (notesArea.isFocusOwner()){
            if(dispPath != null){
                if(dispComp != null){
                    
                    //dispComp.setNotes(notesArea.getText());
                    dispPath.setElementNotes(dispComp, notesArea.getText());
                    
                }else
                    dispPath.setNotes(notesArea.getText());
            } 
        }
        getTableModel().fireTableDataChanged();
    }
    
    /**Reacts to actions dispatched by the buttons*/
    public void actionPerformed(ActionEvent e)
    {
        int selected = pathwayTable.getSelectedRow();
        Object btn = e.getSource();
        
        if (btn == removeElement){    
            Object obj = null;
            if (selected != -1) 
                obj = getTableModel().getElement(selected);
            else {
                if (getTableModel().getCurrentPathway() != null
                   && getTableModel().getCurrentPathway().getCurrent() != null)
                    obj = getTableModel().getCurrentPathway().getCurrent();
            }
            if (obj != null && obj instanceof LWComponent) {
                getSelectedPathway().remove((LWComponent) obj);
            } else {
                System.out.println("Error trying to remove pathway element: selected object not found.");
            }
        }            
        else if (btn == addElement) {
            getSelectedPathway().add(VUE.ModelSelection.iterator());            
        }
        else if (btn == firstButton) {
            getSelectedPathway().getFirst();
        }
        else if (btn == lastButton) {
            getSelectedPathway().getLast();
        }
        else if (btn == forwardButton) {
            getSelectedPathway().getNext();
        }
        else if (btn == backButton) {
            getSelectedPathway().getPrevious();
        }
        else if (btn == removeButton) {
            removePathway(getSelectedPathway());
        }
        else if (btn == createButton) {
            PathwayDialog dialog = new PathwayDialog(this, getLocationOnScreen());
            dialog.show();
        }
        else if (btn == lockButton) {
            if(getTableModel().getCurrentPathway() != null){
                //int currentRow = getTableModel().getManager().getPathwayIndex(getTableModel().getCurrentPathway());
                int currentRow = getTableModel().getCurrentPathwayIndex();
                this.getPathwayTable().setValueAt(this, currentRow, 5);
                this.setAddElementEnabled();
            }
        }
        
        getTableModel().fireTableDataChanged();
        updateControlPanel();
        VUE.getActiveMap().notify(this, LWCEvent.Repaint);
    }
   
    /**document listener's methods*/
    public void removeUpdate(javax.swing.event.DocumentEvent documentEvent) {}
    public void changedUpdate(javax.swing.event.DocumentEvent documentEvent) {}
    public void insertUpdate(javax.swing.event.DocumentEvent documentEvent) {}

    /*** Pathway Control methods ***/
    /**Sets the pathway manager to the given pathway manager*/
    
    private void setPathwayManager(LWPathwayManager manager){
        //PATH TODO JUNK: getTableModel().setPathwayManager(manager);
    }

    /*
    private void setPathwayManager()
    {
        //PATH TODO:getTableModel().setPathwayManager(VUE.getActiveMap().getPathwayList());
        new Throwable("PATH TODO").printStackTrace();
        updateControlPanel();
    }
    */
    
    /**Returns the currently associated pathway manager*/
/*    public LWPathwayManager getPathwayManager()
    {
        return pathwayManager;
    }
  */  
    /**Adds the given pathway to the combo box list and the pathway manager*/

    private void addPathway(LWPathway newPathway)
    {
        new Throwable("addPathway " + newPathway).printStackTrace();
    }
    
    /**Sets the current pathway to the given pathway and updates the control panel accordingly*/
    public void setCurrentPathway(LWPathway pathway)
    {
        //if(this.getPathwayManager() != null)
        //    this.getPathwayManager().setCurrentPathway(pathway);
        getTableModel().setCurrentPathway(pathway);
            
        //sets to the first node if there is no current node set
        if(pathway != null)
        {
            if ((pathway.getCurrent() == null) && (pathway.getFirst() != null) )
              pathway.getFirst();
        }       
        
        updateControlPanel(); 
    }
    
    /**A method which updates the widgets accordingly*/
    public void updateControlPanel()
    {
        if (getSelectedPathway() != null){
            LWComponent currentElement = getSelectedPathway().getCurrent();
            removeButton.setEnabled(true);
            
            if(currentElement != null){
                if (getSelectedPathway().isFirst()){
                    backButton.setEnabled(false);
                    firstButton.setEnabled(false);
                }else{
                    backButton.setEnabled(true);
                    firstButton.setEnabled(true);
                }
          
                if (getSelectedPathway().isLast()){
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
    private void removePathway(LWPathway oldPathway)
    {
        VUE.getActiveMap().getPathways().remove(oldPathway);
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
    
    public void mapViewerEventRaised(MapViewerEvent e) {
        if ((e.getID() & MapViewerEvent.DISPLAYED) != 0){
            updateControlPanel();
            parent.repaint();
            //this.setPathwayManager();
            //VUE.getPathwayInspector().repaint();
            //System.out.println("Map Viewer Event: "+e.getID());
            //System.out.println("Active Map: "+VUE.getActiveMap().getLabel());
        }   
    }
    
}
