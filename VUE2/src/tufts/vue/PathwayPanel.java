/*
 * PathwayTab.java
 *
 * Created on June 19, 2003, 12:50 PM
 */

package tufts.vue;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.border.*;

/**
 * 8-fold cyclic repetition of a beta-strand-loop-alpha- helix-loop module with helix alpha 7 missing. 
 * @author  Daisuke Fujiwara
 */

/**A class which displays nodes in a pathway */
public class PathwayPanel extends JPanel implements ActionListener//, MapViewer.Listener
{    
    private PathwayTable pathwayTable = null;
    private PathwayTableModel tableModel = null;
    private JButton removeElement = null, addElement = null, moveUp = null, moveDown = null; //, submit 

    private JLabel pathLabel = null;//, nodeLabel = null, slashLabel = new JLabel(" / ");
    
    private JFrame parent;
    private JPanel buttons = null;
    private JPanel buttonPanel = null;
    private JLabel pathName = null;
    
    private boolean elemSelection = false;
    
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
    
    private LWComponent displayedComponent;
    private LWPathway displayedComponentPathway;
    
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
        
        setupPathwayControl();
         
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
        
        /*
        JLabel questionLabel = new JLabel(VueResources.getImageIcon("smallInfo"), JLabel.LEFT);
        questionLabel.setPreferredSize(new Dimension(22, 17));
        questionLabel.setBackground(altbgColor);
        questionLabel.setToolTipText("Check boxes below to display paths on the map. Click on path's layer to make a specific path active for editing or playback.");
        */
        
        JLabel filler = new JLabel(" ");
        filler.setBackground(altbgColor);
        
        JLabel playBackLabel = new JLabel("Highlight a path to playback:   ");
        playBackLabel.setFont(defaultFont);
        playBackLabel.setBackground(altbgColor);
        
        JPanel buttonGroupPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonGroupPanel.setBackground(altbgColor);
        buttonGroupPanel.setBorder(null);
        buttonGroupPanel.add(playBackLabel);
        buttonGroupPanel.add(buttonPanel); // buttonPanel setup in setupPathwayControl
        
        
        JPanel northPanel = new JPanel(new BorderLayout(0,0));
        northPanel.setBackground(altbgColor);
        
        //questionLabel.setBorder(BorderFactory.createMatteBorder(0, 5, 0, 0, this.altbgColor));
        buttonGroupPanel.setBorder(BorderFactory.createMatteBorder(8, 0, 0, 5, this.altbgColor));
        //northPanel.add(questionLabel, BorderLayout.WEST);
        northPanel.add(buttonGroupPanel, BorderLayout.EAST);
        
        //pathway table setup
        tableModel = new PathwayTableModel();
        tableModel.addTableModelListener(new TableModelListener() {
                public void tableChanged(TableModelEvent e) {
                    if (DEBUG.PATHWAY) System.out.println(this + " " + e);
                    updateLabels();
                    updateEnabledStates();
                }
            });
        
        
        pathwayTable = new PathwayTable(this, tableModel);
        pathwayTable.setBackground(bgColor);
        
        for(int i = 0; i < 6; i++){
            TableColumn col = pathwayTable.getColumn(colNames[i]);
            if(i != 3) col.setMaxWidth(colWidths[i]);
        } 
        
        JScrollPane tablePane = new JScrollPane(pathwayTable);
        
        setupButtons();
        //toggles the add button's availability depending on the selection
        VUE.ModelSelection.addListener(new LWSelection.Listener() {
                public void selectionChanged(LWSelection s) { updateEnabledFromSelection(); }
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
        southPanel.add(buttons); // buttons panel setup in setupButtons() method
        
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
        southNotes.add(pathLabel = new JLabel("Notes: " ));
        
        notesArea = new JTextArea("");
        notesArea.setColumns(5);
        notesArea.setWrapStyleWord(true);
        notesArea.setAutoscrolls(true);
        notesArea.setLineWrap(true);
        notesArea.setBackground(Color.white);
        notesArea.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED, Color.white, Color.darkGray));
        notesArea.addKeyListener(new KeyAdapter() {
                public void keyTyped(KeyEvent e) {
                    if (DEBUG.PATHWAY) System.out.println(this + " " + e);
                    if (displayedComponent == null)
                        return;
                    // we get this event before the typed key is included in the text,
                    // thus we add it here manually (already slow and a hack to do this every
                    // key press -- should do it on focus loss instead)
                    String text = notesArea.getText() + e.getKeyChar();
                    //boolean updateIcon = false;
                    if (displayedComponent instanceof LWPathway) {
                        if (DEBUG.PATHWAY) System.out.println(this + " setPathNotes["+text+"]");
                        //updateIcon = !displayedComponent.hasNotes();
                        displayedComponent.setNotes(text);
                    } else {
                        if (DEBUG.PATHWAY) System.out.println(this + " setElementNotes["+text+"]");
                        //updateIcon = (displayedComponentPathway.getElementNotes(displayedComponent) == null);
                        displayedComponentPathway.setElementNotes(displayedComponent, text);                        
                    }
                    //if (updateIcon)
                    //fireTableModelUpdate(); // only need this if first time notes set to turn on note icon
                    // don't need to do this at all as note set will trigger LWCEvent picked up by table model
                }
            });


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

    /** Returns the currently selected pathway.  As currently
     * selected must always be same as VUE globally selected,
     * we just return that. */
    private LWPathway getSelectedPathway() {
        return VUE.getActivePathway();
    }
    
    private void setupPathwayControl(){
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

    private void setupButtons() {
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
    
    /**Reacts to actions dispatched by the buttons*/
    public void actionPerformed(ActionEvent e)
    {
        int selected = pathwayTable.getSelectedRow();
        Object btn = e.getSource();

        LWPathway pathway = getSelectedPathway();
        
        if (btn == removeElement) {
            
            // This is a heuristic to try and best guess what the user
            // might want to actually remove.  If nothing in
            // selection, and way have a current pathway
            // index/element, remove that current pathway element.  If
            // one item in selection, also remove whatever the current
            // element is (which ideally is usually also the
            // selection, but if it's different, we want to prioritize
            // the current element hilighted in the PathwayTable).  If
            // there's MORE than one item in selection, do a removeAll
            // of everything in the selection.  This removes ALL instances
            // of everything in selection, so that, for instance,
            // a select all followed by pathway delete is guaranteed to
            // empty the pathway entirely.

            if (pathway.getCurrentIndex() >= 0 && VUE.ModelSelection.size() < 2) {
                pathway.remove(pathway.getCurrentIndex());
            } else {
                pathway.remove(VUE.ModelSelection.iterator());
            }
        }
        else if (btn == addElement)     { pathway.add(VUE.ModelSelection.iterator()); }
        else if (btn == firstButton)    { pathway.setFirst(); }
        else if (btn == lastButton)     { pathway.setLast(); }
        else if (btn == forwardButton)  { pathway.setNext(); }
        else if (btn == backButton)     { pathway.setPrevious(); }
        else if (btn == removeButton)   { removePathway(pathway); }
        else if (btn == createButton)   { new PathwayDialog(this.parent, this.tableModel, getLocationOnScreen()).show(); }
        else if (btn == lockButton)     { pathway.setLocked(!pathway.isLocked()); }
    }
   
    private void setElemSelection(boolean val){
        //System.out.println("set element selection: " + val);
        this.elemSelection = val;
    }
    
    /** PathwayTable still calling this */
    // GET RID OF THIS & elemSelection (it's depending on it being set also)
    private void updateAddElementEnabled(){
        //System.out.println("selection: " + this.elemSelection);
        //SMF if(this.elemSelection && !getTableModel().getCurrentPathway().isLocked())
        boolean enabled = true;
        if (VUE.getActivePathway() != null && VUE.getActivePathway().isLocked())
            enabled = false;
        else if (!this.elemSelection)
            enabled = false;
        this.addElement.setEnabled(enabled);
    }

    private void updateEnabledFromSelection()
    {
        if (DEBUG.PATHWAY) System.out.println(this + " updateEnabledFromSelection");
        
        LWSelection selection = VUE.ModelSelection;
        setElemSelection(selection.size() > 0);

        if (selection.size() > 0) {
            LWPathway path = VUE.getActivePathway();
            addElement.setEnabled(path != null && !path.isLocked());
            if (path == null || path.length() <= 0 || path.isLocked()) {
                removeElement.setEnabled(false);
            } else {
                // if at least one element in selection is on current path,
                // enable remove.
                boolean enabled = false;
                Iterator i = selection.iterator();
                while (i.hasNext()) {
                    LWComponent c = (LWComponent) i.next();
                    if (c.inPathway(path)) {
                        if (DEBUG.PATHWAY) System.out.println("Found " + c + " on " + path);
                        enabled = true;
                        break;
                    }
                }
                removeElement.setEnabled(enabled);
            }
        } else {
            addElement.setEnabled(false);
            removeElement.setEnabled(false);
        }
    }
    
    /**A method which updates the widgets accordingly*/
    private void updateEnabledStates()
    {
        if (DEBUG.PATHWAY) System.out.println(this + " updateEnabledStates");
        
        //-------------------------------------------------------
        // handle selection
        //updateEnabledFromSelection();
        
        //-------------------------------------------------------
        // handle add elem?
        updateAddElementEnabled();

        //-------------------------------------------------------
        // original handle
        if (getSelectedPathway() != null) {
            if (getSelectedPathway().length() > 1) {
                boolean atFirst = getSelectedPathway().atFirst();
                boolean atLast = getSelectedPathway().atLast();
                backButton.setEnabled(!atFirst);
                firstButton.setEnabled(!atFirst);
                forwardButton.setEnabled(!atLast);
                lastButton.setEnabled(!atLast);
            } else {
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
        VUE.getActiveMap().getPathwayList().remove(oldPathway);
    }
    
    private void updateLabels()
    {
        /*
                    if(obj instanceof LWPathway){
                        newText = newText + ((LWPathway)obj).getLabel();
                        notesText = ((LWPathway)obj).getNotes();
                        path = (LWPathway)obj;
                    }
                    else{
                        path = tableModel.getCurrentPathway();
                        comp = (LWComponent)tableModel.getElement(row);
                        newText = newText + path.getLabel() 
                            + " / " 
                            + comp.getLabel();
                        
                        //notesText = comp.getNotes();
                        notesText = path.getElementNotes(comp);
                    }
        pathLabel.setText(getSelectedPathway().getLabel());
        pathLabel.repaint();
        
        notesArea.setText(notes);
        notesArea.repaint();
        
        dispPath = path;
        dispComp = comp;
        */
    }

    void updateLabels(String text, String notes, LWPathway path, LWComponent comp){
        pathLabel.setText(text);   
        pathLabel.repaint();
        
        notesArea.setText(notes);
        notesArea.repaint();
        
        displayedComponent = comp;
        displayedComponentPathway = path;
    }

    /*
    public void X_mapViewerEventRaised(MapViewerEvent e) {
        if ((e.getID() & MapViewerEvent.DISPLAYED) != 0){
            if (DEBUG.PATHWAY) System.out.println(this + " got " + e + " updating...");
            fireTableModelUpdate();
            //updateEnabledStates();
            //getTableModel().fireTableDataChanged();

            //old:
            //parent.repaint();
            //this.setPathwayManager();
            //VUE.getPathwayInspector().repaint();
            //System.out.println("Map Viewer Event: "+e.getID());
            //System.out.println("Active Map: "+VUE.getActiveMap().getLabel());
        }   
    }
    */

    public String toString()
    {
        return "PathwayPanel[" + VUE.getActivePathway() + "]";
    }
    
}




    
    /**Sets the current pathway to the given pathway and updates the control panel accordingly*/
    /*
    public void setCurrentPathway(LWPathway pathway)
    {
        if (DEBUG.PATHWAY) System.out.println(this + " setCurrentPathway " + pathway);
        //if(this.getPathwayManager() != null)
        //    this.getPathwayManager().setCurrentPathway(pathway);
        getTableModel().setCurrentPathway(pathway);
            
        //sets to the first node if there is no current node set
        if (pathway != null) {
            // wow: how fucked up -- getFirst has a fuckin side effect
            if (pathway.getCurrent() == null && pathway.length() > 0)
                pathway.setFirst();
        }       
        
        // would be better to wait till we get a callback to do these:
        updateEnabledFromSelection();
        updateEnabledStates();
    }
    */
    
    


/*
    private void setButton(JButton button){
        button.addActionListener(this);
        buttonPanel.add(button);
    }
    */
    

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
    /* what was this for???
    public JPanel getRadioPanel(){
        //JPanel pathPanel = new JPanel(new GridLayout(4,1,5,0));
        
        ButtonGroup group = new ButtonGroup();
        JRadioButton create = new JRadioButton("Create / Edit Path", true);
        JRadioButton play = new JRadioButton("Playback / View Multiple Paths";)
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
    */
    //sets the table's pathway to the given pathway
    /*
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
    */
    
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
        updateEnabledStates();
    }*/

