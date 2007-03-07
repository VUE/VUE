 /*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

package tufts.vue;

import tufts.vue.gui.*;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.TreeUI;
import javax.swing.table.*;
import javax.swing.border.*;

import edu.tufts.vue.preferences.ui.tree.VueTreeUI;

/**
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
 * @author  Daisuke Fujiwara
 * @author  Scott Fraize
 * @version $Revision: 1.68 $ / $Date: 2007-03-07 18:01:10 $ / $Author: mike $
 */

public class PathwayPanel extends JPanel
    implements ActionListener, VUE.ActivePathwayEntryListener
{    
    private Frame mParentFrame;
    private VueButton btnPresentationCreate = new VueButton("presentationDialog.button.add",this);        
    private VueButton btnPresentationDelete = new VueButton("presentationDialog.button.delete",this);        
    private VueButton btnLockPresentation = new VueButton("presentationDialog.button.lock",this);
    private VueButton btnAddSlide = new VueButton("presentationDialog.button.makeSlides",this);
    private VueButton btnDeleteSlide = new VueButton("presentationDialog.button.delete",this);
    private AbstractButton btnPathwayOnly = new VueButton.Toggle("presentationDialog.button.viewAll",this);    
    private VueButton btnPreview = new VueButton("presentationDialog.button.preview", this);
    private VueButton btnMasterSlide = new VueButton("presentationDialog.button.masterSlide",this);
    
    ///NOT YET IMPLEMENTED
    private VueButton btnAnnotatePresentation = new VueButton("presentationDialog.button.annotate");        
    private VueButton btnAnnotateSlide = new VueButton("presentationDialog.button.annotate");                
    private VueButton btnPlayMaps = new VueButton("presentationDialog.button.playMap");
    private VueButton btnPlaySlides = new VueButton("presentationDialog.button.playSlides");    
    private VueButton btnMergeInto = new VueButton("presentationDialog.button.mergeInto");
    private VueButton btnDisplayAsText = new VueButton("presentationDialog.button.displayAsText");
    private VueButton btnDisplayAsMap = new VueButton("presentationDialog.button.displayAsMap");
    
    
    
    private PathwayTable mPathwayTable;
    private PathwayTableModel mTableModel;
    
    //private AbstractButton btnPathwayShowOnly;
    //private AbstractButton btnElementUp, btnElementDown;
    
    private JLabel pathLabel;           // updated for current PathwayTable selection
    private JLabel pathElementLabel;    // updated for current PathwayTable selection
    private JTextArea notesArea;        // updated for current PathwayTable selection
    
    private LWPathway.Entry mSelectedEntry;
    private boolean mNoteKeyWasPressed = false;

    private final Color BGColor = new Color(241, 243, 246);
 
    //MK - Despite these not being used on the presentation window anymore they are still
    //referenced by the pathway tool so they're sticking around for now.
    private static final Action path_rewind = new PlayerAction("pathway.control.rewind");
    private static final Action path_backward = new PlayerAction("pathway.control.backward");
    private static final Action path_forward = new PlayerAction("pathway.control.forward");
    private static final Action path_last = new PlayerAction("pathway.control.last");

    private final JTabbedPane tabbedPane = new JTabbedPane();
    
    public PathwayPanel(Frame parent) 
    {   
    	//DISABLE THE NOTES BUTTONS FOR NOW UNTIL WE FIGURE OUT WHAT THEY DO -MK
    	btnAnnotateSlide.setEnabled(false);
    	btnAnnotatePresentation.setEnabled(false);
    	btnMergeInto.setEnabled(false);
    	btnPlayMaps.setEnabled(false);
    	btnPlaySlides.setEnabled(false);
    	btnDisplayAsMap.setEnabled(false);
    	btnDisplayAsText.setEnabled(false);
    	//END
    	
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
        
        mPathwayTable.setBackground(BGColor);
        
        //-------------------------------------------------------
        // Setup selected pathway VCR style controls for current
        // element
        //-------------------------------------------------------

        //JPanel playbackPanel = new VueUtil.JPanelAA(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        //JLabel playbackLabel = new JLabel("Playback on selected path:  ");
        //playbackLabel.setFont(defaultFont);
      //  playbackPanel.setBorder(new EmptyBorder(7,0,0,0));
     //   playbackPanel.add(playbackLabel);
    //    playbackPanel.add(new PlaybackToolPanel());
        
        //-------------------------------------------------------
        // Setup pathway master add/remove/lock control
        //-------------------------------------------------------
         
        
        
        
    //    btnPathwayShowOnly = new VueButton("pathways.showOnly", this);
        //btnPathwayShowOnly = new VueButton.Toggle("pathways.showOnly", this);
        
     /*   JPanel pathwayMasterPanel = new VueUtil.JPanelAA() {
                public void addNotify() {
                    setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
                    setBackground(new Color(66,76,105));
                    setBorder(new EmptyBorder(2,2,2,4));

      //              add(btnPathwayShowOnly);
                    add(Box.createHorizontalGlue());
                        
                    JLabel label = new JLabel("Create Pathways");
                    //if (DEBUG.Enabled) System.out.println("PathPanel.SmallBoldFont: " + smallBoldFont);
                    label.setFont(smallBoldFont);
                    label.setForeground(Color.white);
                    label.setBorder(new EmptyBorder(0,0,2,3));
                    add(label);
        
                    add(Box.createHorizontalStrut(2));
        
                    add(Box.createHorizontalStrut(2));
        
                    add(Box.createHorizontalStrut(2));
                    
                    
                    super.addNotify();
                    //JLabel help = new JLabel(VueResources.getImageIcon("smallInfo"), JLabel.LEFT);
                    //help.setBackground(altbgColor);
                    //help.setToolTipText("Check boxes below to display paths on the map. "
                    //"Click on path's layer to make a specific path active for editing or playback.");
                    //add(questionLabel, BorderLayout.WEST);
                }
            };
       */ 
        
        //-------------------------------------------------------
        // Selected pathway add/remove element buttons
        //-------------------------------------------------------
        
        
        
        //btnElementAdd.setToolTipText("Add items to pathway");
        //btnElementRemove.setToolTipText("Remove items from pathway");

       // btnElementUp = new VueButton("move-up", this);
   //     btnElementDown = new VueButton("move-down", this);

        /*JPanel elementControlPanel = new VueUtil.JPanelAA(new FlowLayout(FlowLayout.RIGHT, 1, 1)) {
                public void addNotify() {
                    setBackground(new Color(98,115,161));
                    setBorder(new EmptyBorder(1,2,1,5));

                    JLabel label = new JLabel("Add object to pathway ");
                    label.setFont(smallBoldFont);
                    label.setForeground(Color.white);
                    label.setBackground(getBackground());
                    label.setBorder(new EmptyBorder(0,0,1,2)); //tlbr
                    
                    add(label);
        
        
            ///        add(btnElementUp);
              ///      add(btnElementDown);
                    
                    super.addNotify();
                }
            };
        */        
        
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


        JPanel noteLabelPanel = new VueUtil.JPanelAA();
        JLabel notesLabel = new JLabel(" Notes: ");
        //notesLabel.setFont(smallFont);
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
        // Layout for the table components 
        //-------------------------------------------------------
        
        GridBagLayout bag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();

        setLayout(bag);
        c.gridwidth = GridBagConstraints.REMAINDER; // put everything in one column
        c.weightx = 1.0; // make sure everything can fill to width
        
        //-------------------------------------------------------
        // add pathway create/delete/lock control panel
        //-------------------------------------------------------

        c.fill = GridBagConstraints.HORIZONTAL;
        //bag.setConstraints(pathwayMasterPanel, c);
        //add(pathwayMasterPanel);        
        JPanel presentationPanel = new JPanel();
        JPanel slidePanel = new JPanel();
        JPanel masterSlidePanel = new JPanel();
        
        
        
        buildPresentationPanel(presentationPanel);
        buildSlidePanel(slidePanel);
        buildMasterSlidePanel(masterSlidePanel);
        tabbedPane.add(VueResources.getString("presentationDialog.presentationTab.title"), presentationPanel);
        tabbedPane.add(VueResources.getString("presentationDialog.slideTab.title"), slidePanel);
        tabbedPane.add(VueResources.getString("presentationDialog.masterSlideTab.title"), masterSlidePanel);
        
    //    setLayout(new BorderLayout());
        add(tabbedPane,c);
        
        
        //-------------------------------------------------------
        // add the PathwayTable
        //-------------------------------------------------------

        c.fill = GridBagConstraints.BOTH;
        c.weighty = 2.5;
        JScrollPane tablePane = new JScrollPane(mPathwayTable);
        tablePane.setPreferredSize(new Dimension(getWidth(), 180));
        bag.setConstraints(tablePane, c);
        add(tablePane,c);
        
        //-------------------------------------------------------
        // add pathway element add/remove control panel
        //-------------------------------------------------------

        //c.fill = GridBagConstraints.HORIZONTAL;
       // c.weighty = 0;
       // c.insets = new Insets(3,0,1,1);
       // bag.setConstraints(elementControlPanel, c);
        //add(elementControlPanel);
        
        //-------------------------------------------------------
        // Add the notes panel
        //-------------------------------------------------------
        
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1.0;
        c.insets = new Insets(0,0,0,0);
        notesPanel.setPreferredSize(new Dimension(getWidth(), 80));
        bag.setConstraints(notesPanel, c);
        add(notesPanel);

        //-------------------------------------------------------
        // Add the playback panel
        //-------------------------------------------------------

        // (no constraints needed)
        //add(playbackPanel);
        
        //-------------------------------------------------------
        // Disable all that need to be
        //-------------------------------------------------------
        
        updateEnabledStates();

        //-------------------------------------------------------
        // Set up the listeners
        //-------------------------------------------------------
        
        /*
        mTableModel.addTableModelListener(new TableModelListener() {
                public void tableChanged(TableModelEvent e) {
                    if (DEBUG.PATHWAY) System.out.println(this + " " + e);
                    updateTextAreas();
                    updateEnabledStates();
                }
            });
        */

        VUE.addActivePathwayEntryListener(this);
        
        VUE.getSelection().addListener(new LWSelection.Listener() {
                public void selectionChanged(LWSelection s) {
                    final LWPathway curPath = getSelectedPathway();
                    if (s.size() == 1 && s.first().inPathway(curPath)) {
                        curPath.setIndex(curPath.firstIndexOf(s.first()));
                    } else
                        updateEnabledStates();
                }
            }     
        );
        
        
    }
    
    private void buildSlidePanel(JPanel slidePanel)
    {
    	slidePanel.setLayout(new BoxLayout(slidePanel,BoxLayout.X_AXIS));
    	DividerPanel p = new DividerPanel(20);
        DividerPanel p1 = new DividerPanel(20);
        DividerPanel p2 = new DividerPanel(20);
    
        slidePanel.add(Box.createHorizontalStrut(6));
        slidePanel.add(btnAddSlide);
        slidePanel.add(Box.createHorizontalStrut(3));
        slidePanel.add(btnMergeInto);
        slidePanel.add(Box.createHorizontalStrut(3));
        slidePanel.add(btnDeleteSlide);
        slidePanel.add(Box.createHorizontalStrut(6));
        slidePanel.add(p);      
        slidePanel.add(Box.createHorizontalStrut(6));        
        slidePanel.add(btnDisplayAsText);
        slidePanel.add(Box.createHorizontalStrut(6));
        slidePanel.add(btnDisplayAsMap);
        slidePanel.add(Box.createHorizontalStrut(6));
        slidePanel.add(p1);
        slidePanel.add(btnAnnotateSlide);        
        slidePanel.add(Box.createHorizontalStrut(6));
        slidePanel.add(p2);
        slidePanel.add(btnPreview);
        slidePanel.add(Box.createHorizontalStrut(6));                        
        
        return;
    }
    
    private void buildMasterSlidePanel(JPanel masterSlidePanel)
    {
    	masterSlidePanel.setLayout(new BoxLayout(masterSlidePanel,BoxLayout.X_AXIS));
        masterSlidePanel.add(Box.createHorizontalStrut(16));
        masterSlidePanel.add(btnMasterSlide);
        masterSlidePanel.add(Box.createHorizontalStrut(6));
        
        return;
    }
    
    private void buildPresentationPanel(JPanel presentationPanel)
    {
    	DividerPanel p = new DividerPanel(20);
        DividerPanel p1 = new DividerPanel(20);
        DividerPanel p2 = new DividerPanel(20);
    
    	presentationPanel.setLayout(new BoxLayout(presentationPanel,BoxLayout.X_AXIS));        
        presentationPanel.add(Box.createHorizontalStrut(16));
        presentationPanel.add(btnPresentationCreate);
        presentationPanel.add(Box.createHorizontalStrut(6));
        presentationPanel.add(btnPresentationDelete);
        presentationPanel.add(Box.createHorizontalStrut(6));
        presentationPanel.add(p);
        presentationPanel.add(Box.createHorizontalStrut(6));
        presentationPanel.add(btnAnnotatePresentation);
        presentationPanel.add(Box.createHorizontalStrut(6));
        presentationPanel.add(btnLockPresentation);
        presentationPanel.add(Box.createHorizontalStrut(6));
        presentationPanel.add(p1);
        presentationPanel.add(Box.createHorizontalStrut(6));
        presentationPanel.add(btnPathwayOnly);
        presentationPanel.add(Box.createHorizontalStrut(6));
        presentationPanel.add(p2);
        presentationPanel.add(Box.createHorizontalStrut(6));
        presentationPanel.add(btnPlayMaps);
        presentationPanel.add(Box.createHorizontalStrut(6));
        presentationPanel.add(btnPlaySlides);
        
        return;
    }
    public void activePathwayEntryChanged(LWPathway.Entry entry) {
        updateTextAreas(entry);
        updateEnabledStates();
    }
    

    private void ensureNotesSaved() {
        if (mNoteKeyWasPressed && mSelectedEntry != null) {
            mNoteKeyWasPressed = false; // do this first or callback in updateTextAreas will cause 2 different setter calls
            mSelectedEntry.setNotes(notesArea.getText());
            VUE.getUndoManager().mark();
        }
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
            // as we're to be used by a VueButton, store the key
            // as the action command, not the name, as we don't
            // want it to show up as the button label
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
                 
                 //VUE.getUndoManager().mark(); // the above stuff not worth having undoable
        }

        private static void setAllEnabled(boolean t) {
            path_rewind.setEnabled(t);
            path_backward.setEnabled(t);
            path_forward.setEnabled(t);
            path_last.setEnabled(t);
        }
    }

    public void actionPerformed(ActionEvent e)
    {
        Object btn = e.getSource();
        LWPathway pathway = getSelectedPathway();

        if (pathway == null && btn != btnPresentationCreate)
            return;
        if (btn == btnPreview)
        {
        	VUE.getSlideDock().setVisible(true);
        	VUE.getSlideViewer().showSlideMode();
        }    
        else if (btn == btnMasterSlide)
        {
        	VUE.getSlideDock().setVisible(true);
        	VUE.getSlideViewer().showMasterSlideMode();
        }
        else if (btn == btnDeleteSlide) {
            
            // This is a heuristic to try and best guess what the user might want to
            // actually remove.  If nothing in selection, and we have a current pathway
            // index/element, remove that current pathway element.  If one item in
            // selection, also remove whatever the current element is (which ideally is
            // usually also the selection, but if it's different, we want to prioritize
            // the current element hilighted in the PathwayTable).  If there's MORE than
            // one item in selection, do a removeAll of everything in the selection.
            // This removes ALL instances of everything in selection, so that, for
            // instance, a SelectAll followed by pathway delete is guaranteed to empty
            // the pathway entirely.

            if (pathway.getCurrentIndex() >= 0 && VUE.ModelSelection.size() < 2) {
                pathway.remove(pathway.getCurrentIndex());
            } else {
                pathway.remove(VUE.getSelection().iterator());
            }
        }
        else if (btn == btnAddSlide)  { pathway.add(VUE.getSelection().iterator()); }
      //  else if (btn == btnElementUp)   { pathway.moveCurrentUp(); }
      //  else if (btn == btnElementDown) { pathway.moveCurrentDown(); }

        else if (btn == btnPresentationDelete)   { deletePathway(pathway); }
        else if (btn == btnPresentationCreate)   { new PathwayDialog(mParentFrame, mTableModel, getLocationOnScreen()).setVisible(true); }
        else if (btn == btnLockPresentation)     { pathway.setLocked(!pathway.isLocked()); }
        else if (btn == btnPathwayOnly) {
            toggleHideEverythingButCurrentPathway();
        }

        VUE.getUndoManager().mark();
    }

    private LWPathway exclusiveDisplay;
    private synchronized void toggleHideEverythingButCurrentPathway()
    {
        LWMap map = VUE.getActiveMap();
        LWPathway pathway = VUE.getActivePathway();
        
        if (pathway == null || map == null)
            return;

        // This code is a bit complicated, as it both sets a pathway
        // to be exclusively shown, or toggles it if it already is.

        // To make this simple, first we de-filter (show) everything on the map.
        for (LWComponent c : map.getAllDescendents())
            c.setFiltered(false);
        
        if (exclusiveDisplay == pathway) {
            // We're toggling: just leave everything visible in the map
            exclusiveDisplay = null;
        } else {

            // We're exclusively showing the current pathway: hide (filter) everything
            // that isn't in it.  Currently, any child of an LWComponent that is on a
            // pathway, is also considered on that pathway for display purposes.

            hideAllNotOnPathway(map.getChildIterator(), pathway);
            exclusiveDisplay = pathway;
        }

        // Now we make sure the Pathway objects themselves
        // have their filter flag properly set.

        Iterator<LWPathway> i = map.getPathwayList().iterator();
        while (i.hasNext()) {
            LWPathway p = i.next();
            if (exclusiveDisplay == null)
                p.setFiltered(false);
            else
                p.setFiltered(p != pathway);
        }
        
        //mTableModel.fireTableDataChanged(); // so will gray filtered items
        //map.notify(this, "pathway.exclusive.display");
        pathway.notify(this, "pathway.exclusive.display");
    }

    private void hideAllNotOnPathway(Iterator i, LWPathway pathway) {
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (c.inPathway(pathway))
                continue;
            c.setFiltered(true);
            if (c instanceof LWContainer)
                hideAllNotOnPathway(((LWContainer)c).getChildIterator(), pathway);
        }
    }
    
   
    private void updateAddRemoveActions()
    {
        if (DEBUG.PATHWAY&&DEBUG.META) System.out.println(this + " updateAddRemoveActions");
        
        LWPathway path = getSelectedPathway();
        
        if (path == null || path.isLocked()) {
            btnAddSlide.setEnabled(false);
            btnDeleteSlide.setEnabled(false);
            btnPresentationDelete.setEnabled(false);
            notesArea.setEnabled(false);
            return;
        }

        notesArea.setEnabled(true);
        btnPresentationDelete.setEnabled(true);
        
        boolean removeDone = false;
        LWSelection selection = VUE.ModelSelection;
        
        // if any viable index, AND path is open so you can see
        // it selected, enable the remove button.
        if (path.getCurrentIndex() >= 0 && path.isOpen()) {
            btnDeleteSlide.setEnabled(true);
            removeDone = true;
        }
            
        if (selection.size() > 0) {
            btnAddSlide.setEnabled(true);
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
                btnDeleteSlide.setEnabled(enabled);
            }
        } else {
            btnAddSlide.setEnabled(false);
            if (!removeDone)
                btnDeleteSlide.setEnabled(false);
        }
    }

    private void updateEnabledStates()
    {
        if (DEBUG.PATHWAY&&DEBUG.META) System.out.println(this + " updateEnabledStates");
        
        updateAddRemoveActions();

        LWPathway pathway = VUE.getActivePathway();
        if (pathway != null && pathway.length() > 1) {
            boolean atFirst = pathway.atFirst();
            boolean atLast = pathway.atLast();
              path_rewind.setEnabled(!atFirst);
              path_backward.setEnabled(!atFirst);
             path_forward.setEnabled(!atLast);
             path_last.setEnabled(!atLast);
//            if (pathway.isLocked()) {
  //              btnElementUp.setEnabled(false);
    //            btnElementDown.setEnabled(false);
      //      } else {
    //            btnElementUp.setEnabled(!atFirst);
    //            btnElementDown.setEnabled(!atLast);
           // }
        } else {
            PlayerAction.setAllEnabled(false);
    //        btnElementUp.setEnabled(false);
    //        btnElementDown.setEnabled(false);
        }
        btnPathwayOnly.setEnabled(pathway != null && pathway.length() > 0);
        btnLockPresentation.setEnabled(pathway != null);
    }
    
    /** Delete's a pathway and all it's contents */
    private void deletePathway(LWPathway p) {
        // We only ever delete the current pathway, and if's
        // exclusively displayed, make sure to undo the filter.
        // TODO: handle for undo: is critical for undo of the pathway create!
        if (exclusiveDisplay != null)
            toggleHideEverythingButCurrentPathway();
        VUE.getActiveMap().getPathwayList().remove(p);
    }

    private void setSelectedEntry(LWPathway.Entry entry) {
        mSelectedEntry = entry;
    }

    
    private boolean mTrailingNoteSave;
    private void updateTextAreas(LWPathway.Entry entry)
    {
        if (DEBUG.PATHWAY||DEBUG.META)
            System.out.println(this + " updateTextAreas: " + entry + ", skipping="+mTrailingNoteSave);
        
        if (mTrailingNoteSave)
            return;
        
        try {

            // Save any unsaved changes before re-setting the labels.  This is backup
            // lazy-save as workaround for java focusLost limitation.

            // We also wrap this in a loop spoiler because if notes do get saved at this
            // point, we'll come back here with an update event, and we want to ignore
            // it as we're switching to a new note anyway.  Ideally, focusLost on the
            // notesArea would have already handled this, but unfortunately java
            // delivers that event LAST, after the new focus component has gotten and
            // handled all it's events, and if it was the PathwayTable selecting another
            // curent node to display, this code is needed to be sure the note gets
            // saved.
            
            mTrailingNoteSave = true;
            ensureNotesSaved(); 
        } finally {
            mTrailingNoteSave = false;
        }
    
        if (entry == null) {
            pathLabel.setText("");
            pathElementLabel.setText("");
            notesArea.setText("");
            setSelectedEntry(null);
            return;
        }

        String pathText = entry.pathway.getLabel();
        String entryText;

        if (entry.isPathway()) {
            entryText = "";
        } else {
            pathText += ": ";
            if (DEBUG.PATHWAY) pathText += "(" + (entry.index()+1) + ") ";
            entryText = entry.getLabel();
        }

        pathLabel.setText(pathText);
        pathElementLabel.setText(entryText);
        notesArea.setText(entry.getNotes());
        
        mNoteKeyWasPressed = false;
        setSelectedEntry(entry);
    }

    public static void main(String[] args) {
        System.out.println("PathwayPanel:main");
        DEBUG.Enabled = DEBUG.INIT = true;
        VUE.init(args);
        //VueUtil.displayComponent(new PlaybackToolPanel());
    }
    

    public String toString() {
        return "PathwayPanel[" + VUE.getActivePathway() + "]";
    }
    
}
