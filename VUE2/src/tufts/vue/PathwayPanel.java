package tufts.vue;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.border.*;

/**
 * PathwayPanel.java
 *
 * Provides a panel that displays the PathwayTable with note panel
 * editable view of currently selected pathway or item on pathway, as
 * well as controls for navigating through the pathway, and
 * adding/removing new pathways, adding/removing items to the
 * pathways.
 *
 * @see PathwayTable
 * @see PathwayTableModel
 * @see LWPathwayList
 * @see LWPathway
 *
 * @author  Daisuke Fujiwara - 8-fold cyclic repetition of a beta-strand-loop-alpha- helix-loop module with helix alpha 7 missing. 
 * @author  Scott Fraize
 * @version February 2004
 */

public class PathwayPanel extends JPanel implements ActionListener
{    
    private PathwayTable pathwayTable = null;
    private PathwayTableModel tableModel = null;
    private JButton removeElement, addElement, moveUp, moveDown;

    private JLabel pathLabel = null;//, nodeLabel = null, slashLabel = new JLabel(" / ");
    
    private JFrame parent;
    private JPanel buttons = null;
    private JPanel buttonPanel = null;
    private JLabel pathName = null;
    
    //private Font defaultFont = new Font("Helvetica", Font.PLAIN, 12);
    //private Font highlightFont = new Font("Helvetica", Font.BOLD, 12);
    private Font defaultFont = null;
    private Font highlightFont = null;
    
    private JButton btnPathwayDelete, btnPathwayCreate, btnPathwayLock;
    private JButton firstButton, backButton, forwardButton, lastButton;
    private JPanel southNotes = null;
    private JTextArea notesArea = null;
    
    private final String noPathway = "                          ";
    private final String emptyLabel = "empty";
    
    private Color bgColor = new Color(241, 243, 246);
    private Color altbgColor = new Color(186, 196, 222);
    
    private LWComponent mDisplayedComponent;
    private LWPathway mDisplayedComponentPathway;
    private boolean mNoteKeyWasPressed = false;

    /* end Pathway Control Properties */
    
    private String[] colNames = {"A", "B", "C", "D", "E", "F"};
    private int[] colWidths = {20,20,13,100,20,20};
 
    /** Creates a new instance of PathwayPanel */

    public PathwayPanel(JFrame parent) 
    {   
        this.parent = parent;
        this.setBorder(BorderFactory.createMatteBorder(4, 4, 7, 4, bgColor));
        defaultFont = this.getFont();
        highlightFont = this.getFont();
        highlightFont.deriveFont(Font.BOLD);
        
        setupPathwayControl();
         
        btnPathwayCreate = new VueButton("pathways.add", this);
        btnPathwayDelete = new VueButton("pathways.delete", this);
        btnPathwayLock = new VueButton("pathways.lock", this);
        
        JPanel editPathwaysPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        editPathwaysPanel.setBackground(bgColor);
        
        JLabel lab = new JLabel("Pathways");
        lab.setBackground(bgColor);
        lab.setFont(defaultFont);
        lab.setBorder(BorderFactory.createMatteBorder(8, 0, 0, 5, this.bgColor));
        
        JPanel groupPanel = new JPanel(new GridLayout(1, 3, 0, 0));
        //groupPanel.setPreferredSize(new Dimension(57, 24));
        groupPanel.setBorder(BorderFactory.createMatteBorder(8, 0, 0, 5, this.bgColor));
        groupPanel.setBackground(bgColor);
        groupPanel.add(btnPathwayCreate);
        groupPanel.add(btnPathwayDelete);
        groupPanel.add(btnPathwayLock);
        
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
                    updateTextAreas();
                    updateEnabledStates();
                }
            });
        
        
        pathwayTable = new PathwayTable(tableModel);
        pathwayTable.setBackground(bgColor);
        
        for (int i = 0; i < colWidths.length; i++){
            TableColumn col = pathwayTable.getColumn(colNames[i]);
            if (i == 2) col.setMinWidth(colWidths[i]);
            if (i != 3) col.setMaxWidth(colWidths[i]);
        } 
        
        JScrollPane tablePane = new JScrollPane(pathwayTable);
        
        setupButtons();
        //toggles the add button's availability depending on the selection
        VUE.ModelSelection.addListener(new LWSelection.Listener() {
                public void selectionChanged(LWSelection s) {
                    if (s.size() == 1 && s.first().inPathway(getSelectedPathway())) {
                        getSelectedPathway().setCurrentElement(s.first());
                        tableModel.fireTableDataChanged();
                        //updateTextAreas();
                        // todo: changes to map selection are never updating
                        // the text areas
                        // (btw: should not need updateTextAreas() call above,
                        // and it doesn't appear to be helping anyway)
                    } else
                        updateEnabledStates();
                }
            }     
        );
        
        JLabel addLabel = new JLabel("Add/remove selected pathway object(s)");
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
                public void keyPressed(KeyEvent e) { mNoteKeyWasPressed = true; }
                public void keyReleased(KeyEvent e) { if (e.getKeyCode() == KeyEvent.VK_ENTER) ensureNotesSaved(); }
            });
        notesArea.addFocusListener(new FocusAdapter() {
                public void focusLost(FocusEvent e) {
                    if (DEBUG.PATHWAY) System.out.println("PathwayPanel.notesArea     focusLost to " + e.getOppositeComponent());
                    ensureNotesSaved();
                }
                public void focusGained(FocusEvent e) {
                    if (DEBUG.PATHWAY) System.out.println("PathwayPanel.notesArea focusGained from " + e.getOppositeComponent());
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
        
        updateEnabledStates();
        /* End of Layout for pathways tab */
    }

    private void ensureNotesSaved() {
        if (mNoteKeyWasPressed && mDisplayedComponent != null) {
            mNoteKeyWasPressed = false; // do this first or callback in updateTextAreas will cause 2 different setter calls
            String text = notesArea.getText();
            if (mDisplayedComponent instanceof LWPathway) {
                if (DEBUG.PATHWAY) System.out.println(this + " setPathNotes["+text+"]");
                mDisplayedComponent.setNotes(text);
            } else {
                if (DEBUG.PATHWAY) System.out.println(this + " setElementNotes["+text+"]");
                mDisplayedComponentPathway.setElementNotes(mDisplayedComponent, text);                        
            }
            VUE.getUndoManager().mark();
        }
        //else if (DEBUG.PATHWAY) System.out.println(this + " ensureNotesSaved: nothing to save: c=" + mDisplayedComponent
        //                                         + " keyPressed=" + mNoteKeyWasPressed);
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
        
        firstButton = new VueButton("pathway.control.rewind");
        //firstButton.setBackground(this.altbgColor);
        firstButton.addActionListener(this);
        
        backButton = new VueButton("pathway.control.backward");
        //backButton.setBackground(this.altbgColor);
        backButton.addActionListener(this);
        
        forwardButton = new VueButton("pathway.control.forward");
        //forwardButton.setBackground(this.altbgColor);
        forwardButton.addActionListener(this);
        
        lastButton = new VueButton("pathway.control.last");
        //lastButton.setBackground(this.altbgColor);
        lastButton.addActionListener(this);
       
        buttonPanel.add(firstButton);
        buttonPanel.add(backButton);
        buttonPanel.add(forwardButton);
        buttonPanel.add(lastButton);
    }

    private void setupButtons() {
        addElement = new VueButton("add");
        addElement.setBackground(this.altbgColor);
        addElement.addActionListener(this);
        addElement.setEnabled(false);
        
        removeElement = new VueButton("delete");
        removeElement.setBackground(this.altbgColor);
        removeElement.addActionListener(this);
        removeElement.setEnabled(false);
        
        buttons = new JPanel(new GridLayout(1, 2, 0, 0));
        buttons.setBackground(altbgColor);
        buttons.add(addElement);
        buttons.add(removeElement);
        //buttons.setPreferredSize(new Dimension(40,24));
        
    }
    
    /**Reacts to actions dispatched by the buttons*/
    public void actionPerformed(ActionEvent e)
    {
        Object btn = e.getSource();
        LWPathway pathway = getSelectedPathway();

        if (pathway == null && btn != btnPathwayCreate)
            return;
        
        if (btn == removeElement) {
            
            // This is a heuristic to try and best guess what the user
            // might want to actually remove.  If nothing in
            // selection, and we have a current pathway
            // index/element, remove that current pathway element.  If
            // one item in selection, also remove whatever the current
            // element is (which ideally is usually also the
            // selection, but if it's different, we want to prioritize
            // the current element hilighted in the PathwayTable).  If
            // there's MORE than one item in selection, do a removeAll
            // of everything in the selection.  This removes ALL instances
            // of everything in selection, so that, for instance,
            // a SelectAll followed by pathway delete is guaranteed to
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
        
        else if (btn == btnPathwayDelete)   { deletePathway(pathway); }
        else if (btn == btnPathwayCreate)   { new PathwayDialog(this.parent, this.tableModel, getLocationOnScreen()).show(); }
        else if (btn == btnPathwayLock)     { pathway.setLocked(!pathway.isLocked()); }

        VUE.getUndoManager().mark();
    }
   
    private void updateAddRemoveActions()
    {
        if (DEBUG.PATHWAY) System.out.println(this + " updateAddRemoveActions");
        
        LWPathway path = getSelectedPathway();
        
        if (path == null || path.isLocked()) {
            addElement.setEnabled(false);
            removeElement.setEnabled(false);
            btnPathwayDelete.setEnabled(false);
            return;
        }

        btnPathwayDelete.setEnabled(true);
        
        boolean removeDone = false;
        LWSelection selection = VUE.ModelSelection;
        
        // if any viable index, AND path is open so you can see
        // it selected, enable the remove button.
        if (path.getCurrentIndex() >= 0 && path.isOpen()) {
            removeElement.setEnabled(true);
            removeDone = true;
        }
            
        if (selection.size() > 0) {
            addElement.setEnabled(true);
            if (!removeDone) {
                // if at least one element in selection is on current path,
                // enable remove.  Theoretically should only get here if
                // pathway is closed.
                boolean enabled = false;
                Iterator i = selection.iterator();
                while (i.hasNext()) {
                    LWComponent c = (LWComponent) i.next();
                    if (c.inPathway(path)) {
                        if (DEBUG.PATHWAY) System.out.println(this + " in selection enables remove: " + c + " on " + path);
                        enabled = true;
                        break;
                    }
                }
                removeElement.setEnabled(enabled);
            }
        } else {
            addElement.setEnabled(false);
            if (!removeDone)
                removeElement.setEnabled(false);
        }
    }

    /**A method which updates the widgets accordingly*/
    private void updateEnabledStates()
    {
        if (DEBUG.PATHWAY) System.out.println(this + " updateEnabledStates");
        
        updateAddRemoveActions();

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
            btnPathwayLock.setEnabled(true);
        }
        else
        {
            //if currently no pathway is selected, disables all buttons and resets the label
            firstButton.setEnabled(false);
            lastButton.setEnabled(false);
            forwardButton.setEnabled(false);
            backButton.setEnabled(false);
            btnPathwayLock.setEnabled(false);
            //nodeLabel.setText(emptyLabel);
        }
    }
    
    /** Delete's a pathway and all it's contents */
    private void deletePathway(LWPathway p) {
        VUE.getActiveMap().getPathwayList().remove(p);
    }
    
    private boolean mTrailingNoteSave;
    private void updateTextAreas()
    {
        if (mTrailingNoteSave)
            return;
        try {

            // Save any unsaved changes before re-setting the labels.
            // This is backup lazy-save as workaround for java focusLost
            // limitation.
            //
            // We also wrap this in a loop spoiler because if notes do
            // get saved at this point, we'll come back here with an
            // update event, and we want to ignore it as we're
            // switching to a new note anyway.  Ideally, focusLost on
            // the notesArea would have already handled this, but
            // unfortunately java delivers that event LAST, after the
            // new focus component has gotten and handled all it's
            // events, and if it was the PathwayTable selecting
            // another curent node to display, this code is needed
            // to be sure the note gets saved.
            
            mTrailingNoteSave = true;
            ensureNotesSaved(); 
        } finally {
            mTrailingNoteSave = false;
        }
    
        
        LWPathway pathway = getSelectedPathway();
        if (pathway == null) {
            mDisplayedComponent = null;
            mDisplayedComponentPathway = null;
            pathLabel.setText("");
            notesArea.setText("");
            return;
        }
        LWComponent c = tableModel.getElement(pathwayTable.getLastSelectedRow());
        
        String labelText = "Notes: " + pathway.getLabel();
        String notesText = "";

        if (c instanceof LWPathway) {
            notesText = c.getNotes();
        } else if (c != null) {
            notesText = pathway.getElementNotes(c);
            labelText += " / " + c.getDisplayLabel();
        }
        pathLabel.setText(labelText);
        notesArea.setText(notesText);
        mNoteKeyWasPressed = false;
        
        mDisplayedComponent = c;
        mDisplayedComponentPathway = pathway;
    }

    public String toString() {
        return "PathwayPanel[" + VUE.getActivePathway() + "]";
    }
    
}
