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
    private Frame mParentFrame;
    
    private PathwayTable mPathwayTable;
    private PathwayTableModel mTableModel;
    
    private AbstractButton btnPathwayDelete, btnPathwayCreate, btnPathwayLock, btnPathwayShowOnly;
    private AbstractButton btnElementRemove, btnElementAdd, btnElementUp, btnElementDown;
    
    private JLabel pathLabel;           // updated for current PathwayTable selection
    private JLabel pathElementLabel;    // updated for current PathwayTable selection
    private JTextArea notesArea;        // updated for current PathwayTable selection
    
    private LWComponent mDisplayedComponent;
    private LWPathway mDisplayedComponentPathway;
    private boolean mNoteKeyWasPressed = false;

    private final Color bgColor = new Color(241, 243, 246);
    private final Color altbgColor = new Color(186, 196, 222);
    
    private static final Action path_rewind = new PlayerAction("pathway.control.rewind");
    private static final Action path_backward = new PlayerAction("pathway.control.backward");
    private static final Action path_forward = new PlayerAction("pathway.control.forward");
    private static final Action path_last = new PlayerAction("pathway.control.last");

    public PathwayPanel(Frame parent) 
    {   
        //Font defaultFont = new Font("Helvetica", Font.PLAIN, 12);
        //Font highlightFont = new Font("Helvetica", Font.BOLD, 12);
        final Font defaultFont = getFont();
        final Font boldFont = defaultFont.deriveFont(Font.BOLD);
        final Font smallFont = defaultFont.deriveFont((float) boldFont.getSize()-1);
        final Font smallBoldFont = smallFont.deriveFont(Font.BOLD);
    
        mParentFrame = parent;
        setBorder(new EmptyBorder(4, 4, 7, 4));

        //-------------------------------------------------------
        // Set up the PathwayTableModel, PathwayTable & Listeners
        //-------------------------------------------------------

        mTableModel = new PathwayTableModel();
        mPathwayTable = new PathwayTable(mTableModel);
        mPathwayTable.setBackground(bgColor);
        
        //-------------------------------------------------------
        // Setup selected pathway VCR style controls for current
        // element
        //-------------------------------------------------------

        JPanel playbackPanel = new VueUtil.JPanel_aa(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        JLabel playBackLabel = new JLabel("Highlight a path to playback:  ");
        playBackLabel.setFont(defaultFont);
        playbackPanel.setBorder(new EmptyBorder(7,0,0,0));
        playbackPanel.add(playBackLabel);
        playbackPanel.add(new PlaybackToolPanel());
        
        //-------------------------------------------------------
        // Setup pathway master add/remove/lock control
        //-------------------------------------------------------
         
        btnPathwayCreate   = new VueButton("pathways.add", this);
        btnPathwayDelete   = new VueButton("pathways.delete", this);
        btnPathwayLock     = new VueButton("pathways.lock", this);
        btnPathwayShowOnly = new VueButton("pathways.showOnly", this);
        
        
        JPanel pathwayMasterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 1)) {
                public void addNotify() {
                    super.addNotify();
                    setBackground(new Color(66,76,105));
                    setBorder(new EmptyBorder(2,2,2,4));

                    //JLabel label = new JLabel("Pathways:");
                    //label.setFont(defaultFont);
                    //label.setForeground(Color.white);
                    //label.setBorder(new EmptyBorder(0,0,2,2));
                    //add(label);
        
                    add(btnPathwayCreate);
                    add(btnPathwayDelete);
                    add(btnPathwayLock);
                    add(btnPathwayShowOnly);
                    
                    //JLabel help = new JLabel(VueResources.getImageIcon("smallInfo"), JLabel.LEFT);
                    //help.setBackground(altbgColor);
                    //help.setToolTipText("Check boxes below to display paths on the map. "
                    //"Click on path's layer to make a specific path active for editing or playback.");
                    //add(questionLabel, BorderLayout.WEST);
                }
            };
        
        
        //-------------------------------------------------------
        // Selected pathway add/remove element buttons
        //-------------------------------------------------------
        
        btnElementAdd = new VueButton("add", this);
        btnElementRemove = new VueButton("delete", this);
        btnElementUp = new VueButton("move-up", this);
        btnElementDown = new VueButton("move-down", this);

        JPanel elementControlPanel = new VueUtil.JPanel_aa(new FlowLayout(FlowLayout.RIGHT, 1, 1)) {
                public void addNotify() {
                    super.addNotify();
                    setBackground(new Color(98,115,161));
                    setBorder(new EmptyBorder(2,2,2,5));
                    //setBorder(BorderFactory.createMatteBorder(1,1,1,1,Color.gray));

                    JLabel label = new JLabel("OBJECT to pathway");
                    label.setFont(boldFont);
                    label.setForeground(Color.white);
                    label.setBackground(getBackground());
                    label.setBorder(new EmptyBorder(0,0,2,2)); //tlbr
                    
                    //JPanel control = new JPanel();
                    JPanel control = this;
                    //control.setLayout(new BoxLayout(control, BoxLayout.X_AXIS));
                    //control.setBackground(getBackground());
                    //control.setBorder(new EmptyBorder(0,0,0,5));
                    control.add(label);
                    control.add(btnElementAdd);
                    control.add(btnElementRemove);
                    control.add(btnElementUp);
                    control.add(btnElementDown);
                    
                    //add(control, BorderLayout.EAST);
                }
            };
                
        //-------------------------------------------------------
        // Layout for the table components 
        //-------------------------------------------------------
        
        GridBagLayout gridBag = new GridBagLayout();
        GridBagConstraints gc = new GridBagConstraints();
        
        JPanel tablePanel = new JPanel();
        //tablePanel.setBackground(bgColor);
        tablePanel.setLayout(gridBag);
        
        gc.gridwidth = GridBagConstraints.REMAINDER;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.gridheight = 1;
        
        //-------------------------------------------------------
        // add pathway create/delete/lock control panel
        gridBag.setConstraints(pathwayMasterPanel, gc);
        tablePanel.add(pathwayMasterPanel);
        
        //-------------------------------------------------------
        // add pathway element add/remove control panel
        gc.insets = new Insets(3,0,3,0);
        gridBag.setConstraints(elementControlPanel, gc);
        tablePanel.add(elementControlPanel);
        gc.insets = new Insets(0,0,0,0);
        
        //-------------------------------------------------------
        // add the PathwayTable
        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 3.0;
        gc.gridheight = 18;
        JScrollPane tablePane = new JScrollPane(mPathwayTable);
        tablePane.setPreferredSize(new Dimension(getWidth(), 180));
        //tablePane.setBorder(BorderFactory.createMatteBorder(0,1,0,1,Color.gray));
        gridBag.setConstraints(tablePane, gc);
        tablePanel.add(tablePane);
        
        //-------------------------------------------------------
        // Add the selected item text label
        //gc.weighty = 0.0;
        //gc.gridheight = 1;
        
        
        notesArea = new JTextArea("");
        notesArea.setColumns(5);
        notesArea.setWrapStyleWord(true);
        notesArea.setAutoscrolls(true);
        notesArea.setLineWrap(true);
        notesArea.setBackground(Color.white);
        //notesArea.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        //notesArea.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED, Color.white, Color.darkGray));
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


        JPanel noteLabelPanel = new VueUtil.JPanel_aa();
        JLabel notesLabel = new JLabel(" Notes: ");
        notesLabel.setFont(smallFont);
        noteLabelPanel.setLayout(new BoxLayout(noteLabelPanel, BoxLayout.X_AXIS));
        noteLabelPanel.add(notesLabel);
        noteLabelPanel.add(pathLabel = new JLabel(""));
        noteLabelPanel.add(pathElementLabel = new JLabel(""));
        pathLabel.setFont(smallBoldFont);
        pathElementLabel.setFont(smallFont);
        pathElementLabel.setForeground(Color.red.darker());

        JPanel notesPanel = new JPanel(new BorderLayout(0,0));
        notesPanel.add(noteLabelPanel, BorderLayout.NORTH);
        notesPanel.setBorder(new EmptyBorder(7,0,0,0));
        notesPanel.add(new JScrollPane(notesArea), BorderLayout.CENTER);
        
        //-------------------------------------------------------
        // Now layout the whole PathwayPanel
        //-------------------------------------------------------
        
        GridBagLayout bag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(bag);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridheight = 1;
        c.weightx = 1.0;
        
        bag.setConstraints(pathwayMasterPanel, c);
        add(pathwayMasterPanel);
        
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 2.0;
        c.gridheight = 11;
        //tablePanel.setPreferredSize(new Dimension(getWidth(), 250));
        bag.setConstraints(tablePanel, c);
        add(tablePanel);
        
        c.weighty = 1.0;
        c.gridheight = 8;
        notesPanel.setPreferredSize(new Dimension(getWidth(), 80));
        bag.setConstraints(notesPanel, c);
        add(notesPanel);

        add(playbackPanel);
        
        //-------------------------------------------------------
        
        updateEnabledStates();

        //-------------------------------------------------------
        // Set up the listeners
        //-------------------------------------------------------
        
        mTableModel.addTableModelListener(new TableModelListener() {
                public void tableChanged(TableModelEvent e) {
                    if (DEBUG.PATHWAY) System.out.println(this + " " + e);
                    updateTextAreas();
                    updateEnabledStates();
                }
            });
        
        VUE.ModelSelection.addListener(new LWSelection.Listener() {
                public void selectionChanged(LWSelection s) {
                    if (s.size() == 1 && s.first().inPathway(getSelectedPathway())) {
                        getSelectedPathway().setCurrentElement(s.first());
                    } else
                        updateEnabledStates();
                }
            }     
        );
        
        
    }

    private void ensureNotesSaved() {
        if (mNoteKeyWasPressed && mDisplayedComponent != null) {
            mNoteKeyWasPressed = false; // do this first or callback in updateTextAreas will cause 2 different setter calls
            String text = notesArea.getText();
            if (mDisplayedComponent instanceof LWPathway) {
                if (DEBUG.PATHWAY) System.out.println(this + " setPathNotes["+text+"]");
                mDisplayedComponent.setNotes(text);
            } else {
                if (DEBUG.PATHWAY) System.out.println(this + " setElementNotes["+text+"] for " + mDisplayedComponent);
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

    public static class PlaybackToolPanel extends JPanel
    {
        public PlaybackToolPanel() {
            super(new GridLayout(1, 4, 1, 0));

            add(new VueButton(path_rewind));
            add(new VueButton(path_backward));
            add(new VueButton(path_forward));
            add(new VueButton(path_last));
        }
    }

    private static class PlayerAction extends AbstractAction
    {
        PlayerAction(String name) {
            putValue(Action.ACTION_COMMAND_KEY, name);
        }
        
        public void actionPerformed(ActionEvent e)
        {
            LWPathway pathway = VUE.getActivePathway();
            if (pathway == null)
                return;
            String cmd = e.getActionCommand();
                 if (cmd.endsWith("rewind"))    pathway.setFirst();
            else if (cmd.endsWith("backward"))  pathway.setPrevious();
            else if (cmd.endsWith("forward"))   pathway.setNext();
            else if (cmd.endsWith("last"))      pathway.setLast();
            else
                throw new IllegalArgumentException(this + " " + e);
                 
            VUE.getUndoManager().mark();
        }

        private static void setAllEnabled(boolean t) {
            path_rewind.setEnabled(t);
            path_backward.setEnabled(t);
            path_forward.setEnabled(t);
            path_last.setEnabled(t);
        }
    }

    /**Reacts to actions dispatched by the buttons*/
    public void actionPerformed(ActionEvent e)
    {
        Object btn = e.getSource();
        LWPathway pathway = getSelectedPathway();

        if (pathway == null && btn != btnPathwayCreate)
            return;
        
        if (btn == btnElementRemove) {
            
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
        else if (btn == btnElementAdd)  { pathway.add(VUE.ModelSelection.iterator()); }
        else if (btn == btnElementUp)   { pathway.sendBackward(pathway.getCurrent()); }
        else if (btn == btnElementDown) { pathway.bringForward(pathway.getCurrent()); }

        else if (btn == btnPathwayDelete)   { deletePathway(pathway); }
        else if (btn == btnPathwayCreate)   { new PathwayDialog(mParentFrame, mTableModel, getLocationOnScreen()).show(); }
        else if (btn == btnPathwayLock)     { pathway.setLocked(!pathway.isLocked()); }
        else if (btn == btnPathwayShowOnly) {
            toggleHideEverythingButCurrentPathway();
        }

        VUE.getUndoManager().mark();
    }

    private LWPathway excluseDisplay;
    private synchronized void toggleHideEverythingButCurrentPathway()
    {
        LWMap map = VUE.getActiveMap();
        LWPathway pathway = VUE.getActivePathway();
        
        if (pathway == null || map == null)
            return;

        Iterator i = map.getAllDescendentsIterator();
        if (excluseDisplay != pathway) {
            while (i.hasNext()) {
                LWComponent c = (LWComponent) i.next();
                c.setIsFiltered(!c.inPathway(pathway));
            }
            excluseDisplay = pathway;
        } else {
            while (i.hasNext()) {
                LWComponent c = (LWComponent) i.next();
                c.setIsFiltered(false);
            }
            excluseDisplay = null;
        }

        i = map.getPathwayList().iterator();
        while (i.hasNext()) {
            LWPathway p = (LWPathway) i.next();
            if (excluseDisplay == null)
                p.setIsFiltered(false);
            else
                p.setIsFiltered(p != pathway);
        }
        
        //mTableModel.fireTableDataChanged(); // so will gray filtered items
        //map.notify(this, "pathway.exclusive.display");
        pathway.notify(this, "pathway.exclusive.display");
    }
   
    private void updateAddRemoveActions()
    {
        if (DEBUG.PATHWAY) System.out.println(this + " updateAddRemoveActions");
        
        LWPathway path = getSelectedPathway();
        
        if (path == null || path.isLocked()) {
            btnElementAdd.setEnabled(false);
            btnElementRemove.setEnabled(false);
            btnPathwayDelete.setEnabled(false);
            return;
        }

        btnPathwayDelete.setEnabled(true);
        
        boolean removeDone = false;
        LWSelection selection = VUE.ModelSelection;
        
        // if any viable index, AND path is open so you can see
        // it selected, enable the remove button.
        if (path.getCurrentIndex() >= 0 && path.isOpen()) {
            btnElementRemove.setEnabled(true);
            removeDone = true;
        }
            
        if (selection.size() > 0) {
            btnElementAdd.setEnabled(true);
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
                btnElementRemove.setEnabled(enabled);
            }
        } else {
            btnElementAdd.setEnabled(false);
            if (!removeDone)
                btnElementRemove.setEnabled(false);
        }
    }

    private void updateEnabledStates()
    {
        if (DEBUG.PATHWAY) System.out.println(this + " updateEnabledStates");
        
        updateAddRemoveActions();

        LWPathway pathway = VUE.getActivePathway();
        if (pathway != null && pathway.length() > 1) {
            boolean atFirst = pathway.atFirst();
            boolean atLast = pathway.atLast();
            path_rewind.setEnabled(!atFirst);
            path_backward.setEnabled(!atFirst);
            path_forward.setEnabled(!atLast);
            path_last.setEnabled(!atLast);
            //btnElementUp.setEnabled(!atFirst);
            //btnElementDown.setEnabled(!atLast);
        } else {
            PlayerAction.setAllEnabled(false);
            //btnElementUp.setEnabled(false);
            //btnElementDown.setEnabled(false);
        }
        btnPathwayLock.setEnabled(pathway != null);
    }
    
    /** Delete's a pathway and all it's contents */
    private void deletePathway(LWPathway p) {
        VUE.getActiveMap().getPathwayList().remove(p);
    }
    
    private boolean mTrailingNoteSave;
    private void updateTextAreas()
    {
        if (DEBUG.PATHWAY) System.out.println(this + " updateTextAreas, skipping="+mTrailingNoteSave);
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
            pathElementLabel.setText("");
            notesArea.setText("");
            return;
        }
        
        LWComponent c;

        // this is bit hackish: if the PathwayTable has initiated this
        // update from a selection change, we need to query it for
        // the last selected component as opposed to getting it
        // from the current pathway, because it may BE the current
        // pathway.  We might detect this by allowing getCurrent()
        // to return null, thus indicating a pathway selection, but
        // we really always want a current element in the pathway also
        // so something is always highlighted in the pathway table.
        
        if (mPathwayTable.inTableSelection())
            c = mPathwayTable.getLastSelectedComponent();
        else
            c = pathway.getCurrent();

        if (c == mDisplayedComponent && pathway == mDisplayedComponentPathway)
            return;
        
        if (c instanceof LWPathway) {
            pathLabel.setText(pathway.getLabel());
            pathElementLabel.setText("");
            notesArea.setText(c.getNotes());
        } else if (c != null) {
            pathLabel.setText(pathway.getLabel() + " : ");
            pathElementLabel.setText(c.getDisplayLabel());
            notesArea.setText(pathway.getElementNotes(c));
        }
        mNoteKeyWasPressed = false;
        mDisplayedComponent = c;
        mDisplayedComponentPathway = pathway;
    }

    public String toString() {
        return "PathwayPanel[" + VUE.getActivePathway() + "]";
    }
    
}
