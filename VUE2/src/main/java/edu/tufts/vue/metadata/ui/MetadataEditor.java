
/*
* Copyright 2003-2010 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package edu.tufts.vue.metadata.ui;

import java.util.*;
import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.Color;
import java.awt.Insets;
import java.awt.event.*;

import javax.swing.BorderFactory;
import javax.swing.CellEditor;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ComboBoxModel;
import javax.swing.border.Border;
import javax.swing.table.*;

import tufts.Util;
import tufts.vue.DEBUG;
import tufts.vue.ActiveEvent;
import tufts.vue.ActiveListener;
import tufts.vue.LWComponent;
import tufts.vue.LWGroup;
import tufts.vue.LWSelection;
import tufts.vue.VUE;
import tufts.vue.VueResources;
import tufts.vue.gui.GUI;
import edu.tufts.vue.metadata.CategoryModel;
import edu.tufts.vue.metadata.MetadataList;
import edu.tufts.vue.metadata.VueMetadataElement;
import edu.tufts.vue.ontology.OntType;
import edu.tufts.vue.rdf.RDFIndex;

/*
 * MetadataEditor.java
 *
 * The Metadata Editor is primarily a keyword(/category) editor
 * in VUE 2.0.
 *
 * It has the potential to display VueMetadataElements
 * of non category type and was developed in initial demos
 * to be more flexible in this regard. If neccesary, it shouldn't 
 * be too difficult to recover such flexibility.
 *
 * See OntologicalMembershipPane for an example of 
 * an editor for other types of VueMetadataElement data
 *
 * A slightly thornier issue has arisen in terms of applying
 * keywords to multiple selections as this component now
 * listens to both the current Active and current Selection so
 * that it can display/edit either the current Component's 
 * MetadataList or that of an LWGroup - additionally
 * the behavior that propagates the editing/merging of data into the sub
 * components of the group currently resides here - at some
 * point it might make sense to factor this behavior into
 * a subclass, particularly if needed elsewhere. 
 *
 *
 * Created on June 29, 2007, 2:37 PM
 *
 * @author dhelle01
 */
public class MetadataEditor extends JPanel
    implements ActiveListener, MetadataList.MetadataListListener, LWSelection.Listener
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(MetadataEditor.class);

    private static final boolean AUTO_ADD_EMPTY = false; // leaving this on for now, tho we should refactor not to need it
    
    private static final boolean DEBUG_LOCAL = DEBUG.PAIN;
    
  //public final static String CC_ADD_LOCATION_MESSAGE = "<html>Click &oplus;" // nice idea, but &oplus; looks like crap unless it's huge
    public final static String CC_ADD_LOCATION_MESSAGE = "<html>Click + to permanently add this<br>custom category to your own list.";
    
  //public final static int ROW_HEIGHT = 31;
    public final static int ROW_HEIGHT = 27;
    public final static int CELL_VERT_INSET = 4;
    public final static int CELL_HORZ_INSET = 6; // Left aligns with JTextField editor text in Mac Aqua -- todo: check Windows
    public final static int BUTTON_COL_WIDTH = 27;
    
    private static final java.awt.Color GRID_COLOR = (DEBUG.BOXES) ? Color.black : new Color(196,196,196);
    
    public final static Object CC_ADD_LEFT = "left-edge";
    public final static Object CC_ADD_RIGHT = "right-edge";
    
    public final static Border insetBorder  = GUI.makeSpace(CELL_VERT_INSET, CELL_HORZ_INSET,   CELL_VERT_INSET, CELL_HORZ_INSET/2);
    public final static Border buttonBorder = GUI.makeSpace(CELL_VERT_INSET, CELL_HORZ_INSET-1, CELL_VERT_INSET, CELL_HORZ_INSET-1);
    public final static Border insetBorderNoBottom = GUI.makeSpace(CELL_VERT_INSET, CELL_HORZ_INSET, CELL_VERT_INSET-1, CELL_HORZ_INSET);
    public final static Border fullBox = BorderFactory.createMatteBorder(1,1,1,1,GRID_COLOR);
    // public final static Border verticalStartingBox = BorderFactory.createMatteBorder(0,1,1,1,GRID_COLOR);
    public final static Border noBottomBox = BorderFactory.createMatteBorder(1,1,0,1,GRID_COLOR);
    public final static Border topLeftBotBox = BorderFactory.createMatteBorder(1,1,1,0,GRID_COLOR);
    public final static Border topLeftBox = BorderFactory.createMatteBorder(1,1,0,0,GRID_COLOR);
    public final static Border oneLeftBox = GUI.makeSpace(0,1,0,0);
    
    private final static Border DebugBorder = BorderFactory.createMatteBorder(1,1,1,1,Color.red);
        
    private final MDTable mdTable;
    private final MDTableModel model;

    private CellEditor activeTextEdit;
    private int activeEditRow = -1;
    private int activeEditCol = -1;
        
    /** table-column: these are convenience comparitors: they will change when the structure changes (BC_BUTTON should always == buttonColumn) */
    private int TC_BUTTON, TC_TEXT, TC_COMBO;
    
    private static boolean DisplayChangesDuringEdit = true;
        
    
    /**
     * If the selection was of multiple objects, it's put in this group, otherwise this is null.  This serves
     * TWO purposes.  The main one is that we now have a temporary internal LWComponent with a MetadataList we
     * can work with.  The second is simply so we can take advantage of calling getAllDescendents(), if that's
     * what we wish.  All we REALLY need tho is a list of VME's, as they're cached as being category only
     * anyway.  In fact, maybe we should make THAT the common model object: a VME list cache.
     */
    private LWGroup group;
    private final Set<LWComponent> groupContents = new HashSet<LWComponent>();
    /** if selection was size 1, this is set to selection.first(), otherwise, null */
    private LWComponent single;
    // We could add the analysis code for these as a feature of LWSelection:
    private final Set<String> commonKeys = new HashSet<String>();
    private final List<VueMetadataElement> commonPairs = new ArrayList<VueMetadataElement>();
    
    /** the column the +/- buttons are at: changes table structure changes to display categories combo box
     * JTextField column is tested as: buttonColumn-1, JComboBox column as: buttonColumn-2 */
    private int buttonColumn = 1;
    
    private final JButton autoTagButton =  new JButton(VueResources.local("keywordPanel.autotag"));

    private static final CategoryModel OntologiesList = VUE.getCategoryModel();
    private static final CategoryComboBoxModel CategoryMenuModel = new CategoryComboBoxModel();
        
    /** @see ensureModelBulge() */
    private int PlusOneWhenAdding = 0;

    private static final String EmptyValue = "";
    private static final String[] DefaultVMEObject = new String[] { VueMetadataElement.ONTOLOGY_NONE, EmptyValue };
    
    /** for newly added items, not existing items */
    private static final VueMetadataElement InputVME = new VueMetadataElement(-1, DefaultVMEObject[0], DefaultVMEObject[1], DefaultVMEObject.clone()) {
            @Override public void setType(int t) { throw new Error("setType " + t); }
            @Override public void setKey(String k) { throw new Error("setKey " + k); }
            @Override public void setValue(String v) { throw new Error("setValue " + v); }
            @Override public void setObject(Object o) { throw new Error("setObject " + Util.tags(o)); }
        };
    
    private static final java.util.EventObject EDIT_REQUEST = new java.util.EventObject("<md-edit-request>");
    
    private final class MDTable extends JTable {
        MDTable(AbstractTableModel m) {
            super(m);
            setName("ME:JTable");
            // It's crucial that the table itself not be able to take the focus, otherwise it
            // sometimes will steal it from the cell editor, which then won't get a focus border,
            // and will not issue the crucial focusLost event.
            setFocusable(false);

            setShowGrid(true);
            setGridColor(GRID_COLOR);
            //setIntercellSpacing(new java.awt.Dimension(0,0));
            
            // None of these below seem to be making any difference in terms of the BasicTableUI
            // calling down through:
            // at edu.tufts.vue.metadata.ui.MetadataEditor$MDCellEditor.isCellEditable(MetadataEditor.java:1503)
            // at javax.swing.JTable.editCellAt(JTable.java:3482)
            // at javax.swing.plaf.basic.BasicTableUI$Handler.adjustSelection(BasicTableUI.java:1078)
            setRowSelectionAllowed(false); // MetaButton uses/used this to detect row
            setColumnSelectionAllowed(false); 
            setCellSelectionEnabled(false); // note: defaults true if both row & col selection enabled

            setDefaultRenderer(Object.class, new MDCellRenderer());
            setDefaultEditor(JComboBox.class, new ComboCE());
            setDefaultEditor(JTextField.class, new TextCE());
            setDefaultEditor(Object.class, new ButtonCE());
        
            ((DefaultCellEditor)getDefaultEditor(java.lang.Object.class)).setClickCountToStart(2);
            setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
            setRowHeight(ROW_HEIGHT);
            getTableHeader().setReorderingAllowed(false);
        }
        
        public void repaintLater() {
            GUI.invokeAfterAWT(new Runnable() { public void run() { repaint(); }});
        }
                
        @Override public String getToolTipText(MouseEvent me) {
            if (getEventIsOverAddLocation(me))
                return CC_ADD_LOCATION_MESSAGE;
            else
                return null;
            // final int row = rowAtPoint(me.getPoint());
            // final int col = columnAtPoint(me.getPoint());
            // try {
            //     return model.getValueAt(row, col).getKey();
            // } catch (Exception e) {
            //     if (DEBUG.Enabled) Log.debug("tooltip: " + e);
            // }
            // return null;
            //return "Row: " + row + " Col: " + col;
        }
        // @Override public boolean getRowSelectionAllowed() { return false; }
        // @Override public boolean getColumnSelectionAllowed() { return false; }
        // @Override public boolean getCellSelectionEnabled() { return false; }

        public void stopAnyEditing() {
            if (isEditing())
                getCellEditor().stopCellEditing();
        }
        public void cancelAnyEditing() {
            if (isEditing())
                getCellEditor().cancelCellEditing();
        }
    }

    private static String shortKey(String k) {
        if (k == null) return DEBUG.Enabled ? "null" : "";
        final int localPart = k.indexOf('#'); // RDFIndex.ONT_SEPARATOR_CHAR
        if (localPart > 1 && localPart < (k.length() - 1))
            return k.substring(localPart + 1);
        else
            return k;
    }
        
    
    public MetadataEditor(tufts.vue.LWComponent oneOff, boolean showOntologicalMembership, boolean followAllActive)
    {
        //VUE-846 -- special case related to VUE-845 (see comment on opening from keyword item in menu) 
        if (getSize().width < 100) 
            setSize(new java.awt.Dimension(300,200));
        
        autoTagButton.setAction(tufts.vue.AnalyzerAction.calaisAutoTagger);
     // autoTagButton.setLabel(VueResources.getString("keywordPanel.autotag"));
        autoTagButton.setFont(tufts.vue.gui.GUI.SmallFont);
        
        this.single = oneOff; // rare usage
        
        mdTable = new MDTable(this.model = new MDTableModel());
        mdTable.setBackground(getBackground());
        mdTable.getTableHeader().addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseReleased(java.awt.event.MouseEvent evt) {
                    java.awt.event.MouseEvent me = javax.swing.SwingUtilities.convertMouseEvent(mdTable.getTableHeader(), evt, headerAddButtonPanel);
                    //javax.swing.SwingUtilities.convertMouseEvent(mdTable.getTableHeader(), evt, headerAddButton);
                    me.translatePoint(evt.getX()-me.getX() - mdTable.getWidth() + BUTTON_COL_WIDTH, evt.getY()-me.getY());
                    headerAddButtonPanel.dispatchEvent(me);
                }
                public void mousePressed(java.awt.event.MouseEvent evt)
                {
                    if (DEBUG.PAIN) { VUE.diagPush("MP"); Log.debug("header mousePressed..."); }
                    //tufts.vue.gui.VueButton addButton = MetadataTableHeaderRenderer.getButton();
                    java.awt.event.MouseEvent me = javax.swing.SwingUtilities.convertMouseEvent(mdTable.getTableHeader(), evt, headerAddButtonPanel);
                    //javax.swing.SwingUtilities.convertMouseEvent(mdTable.getTableHeader(), evt, headerAddButton);
                       
                    me.translatePoint(evt.getX()-me.getX() - mdTable.getWidth() + BUTTON_COL_WIDTH, evt.getY()-me.getY());
                    //headerAddButton.dispatchEvent(me);
                       
                    //if(DEBUG_LOCAL) {
                    //    System.out.println("MetadataEditor -- header -- unconverted mouse event: " + evt);
                    //    System.out.println("MetadataEditor: converted mouse event: " + me); }
                       
                    /*if(evt.getX()>mdTable.getWidth()-BUTTON_COL_WIDTH) { addNewRow(); }*/
                    // no need for this else, should make more sense to save whenever
                    // adding a new row...
                    //else {                    	
                    final int row = mdTable.rowAtPoint(evt.getPoint());
                    mdTable.stopAnyEditing();
                    headerAddButtonPanel.dispatchEvent(me);
                    mdTable.repaint();
                       
                    if (DEBUG.PAIN) { Log.debug("header mousePressed complete."); VUE.diagPop(); }
                }
            });
       
        // addMouseListener(new tufts.vue.MouseAdapter("JPanel=MetadataEditor") { public void mousePressed(MouseEvent e) {
        //     int row = mdTable.rowAtPoint(e.getPoint());
        //     int lsr = model.lastSavedRow;
        //     if(lsr != row) {    
        //         model.setSaved(lsr,true);  
        //         model.lastSavedRow = -1;
        //     }
        //     model.refresh();
        //     mdTable.stopAnyEditing();
        //     mdTable.repaint();
        // }});
       
        mdTable.addMouseListener(new tufts.vue.MouseAdapter("MetadataEditor:JTable") {
                // public void mousePressed(MouseEvent e) {
                //     if(model.lastSavedRow != mdTable.rowAtPoint(e.getPoint()))
                //         model.setSaved(model.lastSavedRow,true);
                //     mdTable.repaint();
                // }  
                //     /*if(evt.getX()>mdTable.getWidth()-BUTTON_COL_WIDTH) {
                //       java.util.List<VueMetadataElement> metadataList = MetadataEditor.this.current.getMetadataList().getMetadata();
                //       int selectedRow = mdTable.getSelectedRow();
                //       // VUE-912, stop short-circuiting on row 0, delete button has also been returned
                //       if( mdTable.getSelectedColumn()==buttonColumn && metadataList.size() > selectedRow) {
                //           metadataList.remove(selectedRow);
                //           mdTable.repaint();
                //           requestFocusInWindow(); }} */
                public void mouseReleased(MouseEvent e)
                {
                    if (getEventIsOverAddLocation(e)) {
                        final int row = mdTable.rowAtPoint(e.getPoint());//evt.getY()/mdTable.getRowHeight();
                        final VueMetadataElement vme = model.getValueAt(row, -1);

                        if (vme.getKey() == null) throw new Error("bad vme: " + vme);
                        
                        final OntType existing = OntologiesList.getCustomCategoryByLabel(shortKey(vme.getKey()));
                        final OntType ontType;
                        if (existing == null) {
                            ontType = OntologiesList.addCustomCategory(shortKey(vme.getKey()));
                            if (DEBUG.Enabled) Log.debug("added custom cat: " + Util.tags(ontType));
                        } else {
                            ontType = existing;
                            if (DEBUG.Enabled) Log.debug("found custom cat: " + Util.tags(ontType));
                        }


                        final VueMetadataElement customCategory_VME = freshVME(ontType.getAsKey(), vme.getValue());

                        if (DEBUG.PAIN) Log.debug("updated VME with new key: " + customCategory_VME);

                        publishEdit(row, customCategory_VME, "Custom Category");
                        
                        mdTable.stopAnyEditing();
                        
                        GUI.invokeAfterAWT(new Runnable() { public void run() {
                            refreshAll();
                            if (existing == null)
                                OntologiesList.saveCustomOntology();
                        }}); 
                    }
                }
        }); 

        adjustColumnModel();

        //setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setLayout(new BorderLayout());
        
        final JPanel metaPanel = new JPanel(new BorderLayout());
        final JPanel tablePanel = new JPanel(new BorderLayout());
        
        //re-enable for VUE-953 (revert back to categories/keywords) 
        //tablePanel.add(mdTable.getTableHeader(), BorderLayout.NORTH);
        tablePanel.add(mdTable);
        tablePanel.setBorder(topLeftBox);
        
        if (DEBUG.BOXES) {
            metaPanel.setOpaque(true);
            metaPanel.setBackground(Color.green);
            mdTable.getTableHeader().setOpaque(true);
            mdTable.getTableHeader().setBackground(Color.blue);
            //mdTable.getTableHeader().setBorder(GUI.makeSpace(0,0,0,1));
        } else
            mdTable.getTableHeader().setOpaque(false);
        
        metaPanel.add(mdTable.getTableHeader(), BorderLayout.NORTH);
        metaPanel.add(tablePanel);
        
        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        
        //back to "assign categories" as per VUE-953
        //final JLabel optionsLabel = new JLabel("Use full metadata schema");
        final JLabel optionsLabel = new JLabel(VueResources.getString("jlabel.assigncategory"));
        optionsLabel.setFont(GUI.SmallFont);
        //final JButton advancedSearch = new JButton(new ImageIcon(VueResources.getURL("advancedSearchMore.raw")));//tufts.vue.gui.VueButton("advancedSearchMore");
        final JCheckBox advancedSearch = new JCheckBox();
        //advancedSearch.setBorder(BorderFactory.createEmptyBorder(0,0,15,0));

        advancedSearch.addActionListener(new java.awt.event.ActionListener() { public void actionPerformed(java.awt.event.ActionEvent e) {
            //advancedSearch.setIcon(new ImageIcon(VueResources.getURL("advancedSearchLess.raw"))); // getURL("advancedSearchMore.raw")));
            model.toggleCategoryKeyColumn();
            advancedSearch.setSelected(model.getColumnCount() == 3);
            adjustColumnModel();
            revalidate();
        }});
        
        optionsPanel.add(advancedSearch);
        advancedSearch.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        optionsPanel.add(optionsLabel);
        optionsPanel.add(autoTagButton);
        
        final JPanel controlPanel = new JPanel(new BorderLayout());
        
        controlPanel.add(optionsPanel);
        metaPanel.add(controlPanel, BorderLayout.SOUTH);
        add(metaPanel, BorderLayout.NORTH);
        
        // followAllActive needed for MapInspector, it only follows the map, 
        // not any particular component
        if(followAllActive)
        {
          tufts.vue.VUE.addActiveListener(tufts.vue.LWComponent.class,this);
          VUE.getSelection().addListener(this);
        }
        else
        {
          tufts.vue.VUE.addActiveListener(tufts.vue.LWMap.class,this); 
        }
        
        MetadataList.addListener(this);
        
        headerAddButtonPanel.addMouseListener(new java.awt.event.MouseAdapter() {
                private boolean dispatchedInButton(MouseEvent e) {
                    //if(DEBUG_LOCAL) {
                    //  System.out.println("MetadataEditor -- table header mouse pressed " + //        e);
                    //  System.out.println(" components count " + headerAddButtonPanel.getComponentCount());
                    //  if(headerAddButtonPanel.getComponentCount() > 0)  {    
                    //    System.out.println(" component 0 " + headerAddButtonPanel.getComponents()[0].getClass());
                    //    System.out.println(" component 0 - location: " + headerAddButtonPanel.getComponents()[0].getLocation());
                    //    System.out.println(" component 0 - location: " + headerAddButtonPanel.getComponents()[0].getSize());
                    //    System.out.println(" component at location " + headerAddButtonPanel.getComponentAt(e.getPoint())); }}
                    final tufts.vue.gui.VueButton button = (tufts.vue.gui.VueButton) headerAddButtonPanel.getComponents()[0]; 
                    final java.awt.event.MouseEvent me = javax.swing.SwingUtilities.convertMouseEvent(headerAddButtonPanel, e, button);
                    // javax.swing.SwingUtilities.convertMouseEvent(mdTable.getTableHeader(), evt, headerAddButton);
                    me.translatePoint(-me.getX(), -me.getY());
                    final java.awt.Point p = e.getPoint();
                    final boolean inside =
                        p.x > button.getLocation().getX() &&
                        p.x < button.getLocation().getX() + button.getWidth() &&
                        p.y > button.getLocation().getY() &&
                        p.y < button.getLocation().getY() + button.getHeight();
                    if (inside) {
                        if (DEBUG.PAIN) Log.debug("header caught button hit on " + button);
                        // we only need to actually dispatch this if the headerAddButton below
                        // makes itself a MouseListener
                        button.dispatchEvent(me); 
                    }
                    return inside;
                }
                public void mousePressed(MouseEvent e) {
                    if (dispatchedInButton(e)) {
                        model.ensureBulge("gui-add");
                        mdTable.editCellAt(model.getRowCount() - 1, TC_TEXT, EDIT_REQUEST); 
                        mdTable.repaint();
                    }
                }
            });
            
        /*headerAddButton.addActionListener(new java.awt.event.ActionListener(){
          public void actionPerformed(java.awt.event.ActionEvent e) { if(DEBUG_LOCAL) { System.out.println("MetadataEditor -- table header add button pressed " + e); } } });*/
            
        // Not sure what this was for: if we bother to dispatch the event to the button above, this WOULD
        // let the the button display a pressed state, but it isn't configured with one, nor configured to
        // try and generate them.  */
        headerAddButton.addMouseListener(new tufts.vue.MouseAdapter("<headerAddButton>") { public void mousePressed(MouseEvent e) {
            super.mousePressed(e);
            //if(DEBUG_LOCAL)  System.out.println("MetadataEditor -- table header mouse pressed on button " + e);
            headerAddButtonPanel.repaint();
            headerAddButton.repaint();
        }});   
        
        setBorder(BorderFactory.createEmptyBorder(10,8,15,6));
        validate();

        // As this can be lazily constructed, included the request for a selection listener, if this is the
        // first time we've been created, simulate an update call from the selection.
        GUI.invokeAfterAWT(new Runnable() { public void run() {
            selectionChanged(VUE.getSelection());
        }}); 
                
    }
    
//     @Override public Dimension getMinimumSize() { 	   
//         int height = 120;
//         //int lines = 1;
//         int rowCount = mdTable.getRowCount(); 	   	  
//         int rowHeight = mdTable.getRowHeight();
//         //System.out.println("rowHeight :"  + rowHeight);
//         return new Dimension(300,(height+((rowCount-1) * rowHeight)));
//     }
//     @Override public Dimension getPreferredSize() { return getMinimumSize(); }
    
    public void refreshAll() {
    	model.refresh();
        ignoreCategorySelectEvents = true;
        try {
            CategoryMenuModel.refreshCategoryMenu();
        } catch (Throwable t) {
            Log.error("refresh: " + CategoryMenuModel, t);
        } finally {
            ignoreCategorySelectEvents = false;
        }
    	validate();
        repaint();
    }
    
    public JTable getTable() { return mdTable; }
    public MDTableModel getModel() { return model; }
    public CellEditor getActiveTextEdit() { return activeTextEdit; }

    private static VueMetadataElement emptyVME() { return freshVME(EmptyValue); }
    
    private static VueMetadataElement freshVME(String textValue) {
        return freshVME(VueMetadataElement.ONTOLOGY_NONE, textValue);
    }
    
    private static VueMetadataElement freshVME(String key, String text) {
        return new VueMetadataElement(key, text); // defaults category type, inits object
        // final VueMetadataElement vme = new VueMetadataElement();
        // vme.setObject(new String[] { key, textValue }); // note: will force category type anyway
        // vme.setType(VueMetadataElement.CATEGORY);
        // return vme;
    }
    
    public boolean getEventIsOverAddLocation(MouseEvent evt)
    {
       final boolean locationIsOver =
           evt.getX() > getCustomCategoryAddLocation(CC_ADD_LEFT) && 
           evt.getX() < getCustomCategoryAddLocation(CC_ADD_RIGHT);
       
       final int row = mdTable.rowAtPoint(evt.getPoint());//evt.getY()/mdTable.getRowHeight();
                           
       return locationIsOver && !model.wasCategoryFound(row);
    }
    /** returns -1 if add widget is not available (i.e. if in basic
     *  mode and/or not in "assign categories" mode) **/
    public int getCustomCategoryAddLocation(Object ccLocationConstant)
    {
        if(mdTable.getModel().getColumnCount() == 2)
            return -1;
            
        if (ccLocationConstant == CC_ADD_LEFT)
            return mdTable.getColumnModel().getColumn(0).getWidth() - 20;
        else if(ccLocationConstant == CC_ADD_RIGHT)
            return mdTable.getColumnModel().getColumn(0).getWidth();
        else
            return -1;
    }

    public void adjustColumnModel()
    {
        final int editorWidth = MetadataEditor.this.getWidth();
        //if(MetadataEditor.this.getTopLevelAncestor() != null)
        //  editorWidth = MetadataEditor.this.getTopLevelAncestor().getWidth();
        
        if (mdTable == null)
            return;
        
        if (mdTable.getModel().getColumnCount() == 2) {
            mdTable.getColumnModel().getColumn(0).setHeaderRenderer(HeaderRenderer);
            mdTable.getColumnModel().getColumn(1).setHeaderRenderer(HeaderRenderer);
            mdTable.getColumnModel().getColumn(0).setWidth(editorWidth-BUTTON_COL_WIDTH);
            mdTable.getColumnModel().getColumn(1).setMaxWidth(BUTTON_COL_WIDTH);   
            mdTable.getColumnModel().getColumn(1).setMinWidth(BUTTON_COL_WIDTH);   
        } else {
            mdTable.getColumnModel().getColumn(0).setHeaderRenderer(HeaderRenderer);
            mdTable.getColumnModel().getColumn(1).setHeaderRenderer(HeaderRenderer);
            mdTable.getColumnModel().getColumn(2).setHeaderRenderer(HeaderRenderer);
            mdTable.getColumnModel().getColumn(0).setWidth(editorWidth/2-BUTTON_COL_WIDTH/2);
            mdTable.getColumnModel().getColumn(1).setWidth(editorWidth/2-BUTTON_COL_WIDTH/2);
            mdTable.getColumnModel().getColumn(2).setMaxWidth(BUTTON_COL_WIDTH); 
            mdTable.getColumnModel().getColumn(2).setMinWidth(BUTTON_COL_WIDTH); 
        }
    }
    
    // Below code was to support construct such as:
    // final String categoryKey = getObject(source_VME)[0];
    // Using vme.getObject() v.s. just vme.getKey() might have been attempt at one point to support OntType vme.obj,
    // but in any case its a horrible hack.
    
  //   private String[] getObject(VueMetadataElement vme) {
  //       if (vme == null) {
  //           if (true||DEBUG.Enabled) Log.info("getObject GIVEN NULL VME, RETURNING DEFAULT_VME_OBJECT " + Util.tags(DefaultVMEObject));
  //           return DefaultVMEObject;
  //       } else
  //           return (String[]) vme.getObject();
  //       //return vme == null ? DefaultVMEObject : (String[]) vme.getObject();
  //   }
    
  //   private static final String[] NullPair = { null, null };
  //   private static final String[] EmptyPair = { DefaultVMEObject[0], null };

  //   private String[] getObjectSingle(int row) {
  //       final int size = single.getMetadataList().getCategoryListSize();
  //       if (row >= size) {
  //           if (PlusOneWhenAdding == 1 && row == size) {
  //               return DefaultVMEObject;
  //           } else {
  //               Log.warn("getObjectCurrent for row " + row + " returns NullPair");
  //               return NullPair; // if we return EmptyPair, we'll get auto-add empty none keys again
  //           }
  //       } else
  //           return getObject(single.getMetadataList().getCategoryList().get(row)); // don't we have this cached?
  //   }
    
  //   private String[] getObjectMulti(int row) {
  //       return getObject(group.getMetadataList().getCategoryList().get(row));
  //   }

  //   private String[] getObjectActive(int row) {
  //       if (single != null)
  //           return getObjectSingle(row);
  //       else if (group != null)
  //           return getObjectMulti(row);
  //       else {
  //           Log.warn("no active object at row " + row);
  //           return NullPair;
  //       }
  //   }
    
  //   /** @return the category key at the given row */
  //   String getKeyForRow(int row)        { return getObjectActive(row)[0]; }
  // //String getValueForRow(int row)          { return getObjectActive(row)[1]; }
    
    public void selectionChanged(final LWSelection s)
    {
        if (DEBUG.PAIN) Log.debug("selectionChanged: " + s);
        
        this.single = null;
        this.group = null;
        this.groupContents.clear();

      //mdTable.stopAnyEditing(); // will auto-save: didn't used to work in selection-change case, but is now an option
        mdTable.cancelAnyEditing();
        autoTagButton.setVisible(s.size() == 1);

        if (s.size() > 1)
            loadMultiSelection(s);
        else
            loadSingleSelection(s.first()); // 0 or 1 elements in selection
        // todo: model.loadSelection(s);
    }
    
    private void loadSingleSelection(LWComponent selected)
    {
        if (DEBUG.PAIN) Log.debug("loadSingleSelection: " + selected);
        
        if (selected == null)
            return;

        this.single = selected;

        final boolean changed;
        if (single.getMetadataList().getCategoryListSize() == 0) 
            changed = model.ensureBulge("selected-has-no-meta");
        else
            changed = model.clearBulge("selected: reset for new md");
        if (!changed)
            model.refresh();
    }

    private void loadMultiSelection(final LWSelection selection)
    {
        if (selection.size() < 2) throw new Error("size<2="+selection.size());

        this.group = LWGroup.createTemporary(selection);

        // SMF: iterate getChildren: this will tag just the top-level actually selected items
        // SMF: iterate getAllDescendents: the above PLUS their children, grandchildren, etc.
        // SMF: Using getAllDescendents is the historical default, tho that means we can NEVER just group-tag a set of parents,
        // If we only did the direct children, what's in the selection could still be expanded to include children,
        // tho we don't have an action for that -- the user would have to add the children manually.

        for (LWComponent c : group.getAllDescendents())
            if (!c.hasFlag(LWComponent.Flag.ICON))
                groupContents.add(c);
        
        // note: a HashSet does not help us much here, except perhaps for the remove(o) calls in AbstractCollection.retainAll
        final List<VueMetadataElement> shared = new ArrayList<VueMetadataElement>();

        // This finds the intersection for exact key+value pairs, so it's very easy for this to display
        // nothing in common.  We could change this to instead show the union of all the keys, and in cases
        // where there are multiple values, display something like "Values: 27" in the value field.

        for (LWComponent c : groupContents) {
            // getAll() will include merge-source data, tho it won't display in UI.  But it WILL exclude
            // OTHER meta-data if any is found, so we don't want that.
         // final Collection<VueMetadataElement> data = c.getMetadataList().getAll();
            final Collection<VueMetadataElement> data = c.getMetadataList().getCategoryList().getAsList();

            if (data.isEmpty())
                continue;
            if (shared.isEmpty()) { // first time
                shared.addAll(data);
            } else {
                shared.retainAll(data);
                // If we're ever shrunk back to 0 size, we know we'll never find something in 100% of the
                // items, and thus we're already done.
                if (shared.isEmpty())
                    break;
            }
        }
        if (DEBUG.Enabled) { Log.debug("shared: " + Util.tags(shared)); Util.dumpCollection(shared); }
        
        if (shared.isEmpty()) {
            model.ensureBulge("multi-is-empty");
        } else {
            // Normally, this should only go in the category list...  we only need this for the
            // data-model tho, so howbout we keep this as a separate list globally, and we never
            // have to touch the sick meta-data API again after this, yes?  The only real
            // convenience we get from the LWGroup is being able to call getAllDescendents()...
            final List<VueMetadataElement> shareTarget = group.getMetadataList().getAll();
            for (VueMetadataElement vme : shared)
                shareTarget.add(vme);
            //if (DEBUG.Enabled) Log.debug("cats after update" + currentMultiples.getMetadataList().getMetadataAsHTML(VueMetadataElement.CATEGORY));
        }
        model.refresh();
    }
    
    public void activeChanged(ActiveEvent e) {
        if (DEBUG.PAIN) Log.debug("activeChanged: " + e + " (now ignored)");
        // if (e != null) ... if (active instanceof tufts.vue.LWSlide || active instanceof tufts.vue.LWMap) active = null;
    }
    
    
    private final tufts.vue.gui.VueButton headerAddButton = new tufts.vue.gui.VueButton("keywords.button.add"); 
    private final JPanel headerAddButtonPanel = new JPanel();
    private final JLabel LabelKeyword = new JLabel(VueResources.local("jlabel.keyword")); 
    private final JLabel LabelKeywordShared = new JLabel("Shared " + VueResources.local("jlabel.keyword"));  // todo: localize
    private final JLabel LabelCategory = new JLabel(VueResources.local("jlabel.category"));
    private final JLabel LabelEmpty = new JLabel("");
    private final Border HeaderCellBorder = GUI.makeSpace(0, CELL_HORZ_INSET+2, 0, 0);
    
    private final TableCellRenderer HeaderRenderer = new MDTableHeaderRenderer();
        
    private class MDTableHeaderRenderer extends DefaultTableCellRenderer {  
        // see below - getter could be supplied in stand alone class
        //static tufts.vue.gui.VueButton button = new tufts.vue.gui.VueButton("keywords.button.add");

        public MDTableHeaderRenderer() {
            headerAddButton.setBorderPainted(false);
            headerAddButton.setContentAreaFilled(false);
            // headerAddButton.setBorder(javax.swing.BorderFactory.createEmptyBorder()); why?
            headerAddButton.setSize(new java.awt.Dimension(5,5));
            /*headerAddButton.addActionListener(new java.awt.event.ActionListener(){
              public void actionPerformed(java.awt.event.ActionEvent e) {
                if(DEBUG_LOCAL) System.out.println("MetadataEditor -- table header add button pressed " + e); } });*/
            /*headerAddButton.addMouseListener(new java.awt.event.MouseAdapter(){ public void mousePressed(java.awt.event.MouseEvent e) {
                if(DEBUG_LOCAL) System.out.println("MetadataEditor -- table header mouse pressed " + e); } } });*/
            LabelKeyword.setFont(GUI.LabelFace);
            LabelKeywordShared.setFont(GUI.LabelFace);
            LabelCategory.setFont(GUI.LabelFace);
        }
       // can't do this statically in inner class but could be done from wholly separate class -
       // for now move the button out into the metadata editor
       /*static tufts.vue.gui.VueButton getButton() { return button; }*/
        @Override public java.awt.Component getTableCellRendererComponent(JTable table, Object value,boolean isSelected,boolean hasFocus,int row,int col) {
            JComponent comp = new JPanel();
            if (col == TC_TEXT) {
                // back to "Keywords:" -- VUE-953
                comp = (group != null) ? LabelKeywordShared : LabelKeyword;
            } else if (col == TC_COMBO) {
                // back to "Categories" -- VUE-953
                comp = LabelCategory;
            } else if (col == TC_BUTTON) {
                //((JLabel)comp).setIcon(tufts.vue.VueResources.getImageIcon("metadata.editor.add.up"));
                comp = headerAddButtonPanel;
                comp.add(headerAddButton);
            } else
                comp = LabelEmpty;
            if (col == TC_BUTTON)
                comp.setBorder(buttonBorder);
            else
                comp.setBorder(HeaderCellBorder);
            comp.setOpaque(true);
            comp.setBackground(MetadataEditor.this.getBackground());
            return comp;
        }
    }

    private static boolean hasKnownCategory(final String key) {
        // A guess is good enough -- users can always add items manually via "Edit Categories"
        return key != null && key.indexOf('#') > 0;
        // if (keyName == null || keyName.indexOf('#') < 0)
        //     return false;
        // // todo: crazy that CategoryModel doesn't have a hash lookup for this.
        // for (edu.tufts.vue.ontology.Ontology ont : OntologiesList)
        //     for (OntType ontType : ont.getOntTypes())
        //         if (ontType.matchesKey(keyName))
        //             return true;
        // // for (OntType ot : OntologiesList<>getOntTypes()) // any languages that do something like this?
        // return false;
    }

    private class MDCellRenderer extends DefaultTableCellRenderer
    {   
        private final JComponent deleteButton;
        private final JPanel box = new JPanel();
        private final JLabel addLabel = new JLabel("+");
        private final JLabel faintKeyLabel = new JLabel();

        private final JTextField renderEditable = new JTextField();

        MDCellRenderer() {
            setFont(GUI.LabelFace);
            deleteButton = new edu.tufts.vue.metadata.gui.MetaButton(MetadataEditor.this, "renderDelete");
            
            renderEditable.setFont(GUI.LabelFace);
            renderEditable.setBorder(null);
            
            box.setLayout(new BorderLayout());
            box.setName("MD:cellRenderer");
            if (DEBUG.BOXES) {
                box.setOpaque(true);
                box.setBackground(Color.yellow);
            } else
                box.setOpaque(false);
            
            faintKeyLabel.setForeground(Color.lightGray);
            faintKeyLabel.setFont(GUI.LabelFaceItalic);
            
            addLabel.setForeground(Color.gray);
            addLabel.setFont(tufts.vue.VueConstants.LargeFixedFontBold);
        }

        public java.awt.Component getTableCellRendererComponent(final JTable t, final Object _vme, boolean selected, boolean focus, final int row, final int col)
        {
            //final boolean isPlusOneEditRow = (PlusOneWhenAdding == 1 && row == model.getRowCount() - 1);
            final boolean isPlusOneEditRow = false;
           
            if (DEBUG.PAIN) Log.debug("gTCRC: bc=" + buttonColumn + (row<10?"  ":" ") + row + "," + col + " " + _vme);

            box.removeAll(); // this box is used as a safe generic objext we know we can stick a border on

            if (TC_COMBO == col) {
                final VueMetadataElement vme = (VueMetadataElement) _vme;
                final String key = vme.getKey();
                final boolean knownCategory = hasKnownCategory(key);
                model.reportCategoryFound(row, knownCategory); // ick
                setText(shortKey(key));
                box.add(this);
                if (DisplayChangesDuringEdit && row == activeEditRow) {
                    ; // don't draw '+'
                    // if (mdTable.isEditing()) ; // turn them all off
                } else if (!knownCategory)
                    box.add(addLabel, BorderLayout.EAST);
            }
            else if (TC_TEXT == col && (isPlusOneEditRow || _vme != null)) { // if value is null, we'll render empty box, even if PlusOneEditRow
                final VueMetadataElement vme = (VueMetadataElement) _vme;
                if (vme != null && vme.hasValue()) {
                    setText(vme.getValue());
                    box.add(this);
                } else if (isPlusOneEditRow) {
                    // we're rendering the last row with an edit bulge: make it look inviting to activate an edit:
                    box.add(renderEditable);
                } else {
                    if (!model.keyColumnVisible()) {
                        // the value is an empty string: display the key itself, faintly
                        if (vme.isKeyNone())
                            faintKeyLabel.setText("None");
                        else
                            faintKeyLabel.setText(shortKey(vme.getKey()));
                        box.add(faintKeyLabel);
                    } // else just render the empty box
                }
            }
            else if (TC_BUTTON == col && (isPlusOneEditRow || _vme != null))
                return deleteButton;

            return installRenderBorder(box, row, col);
        } 
    }
    
    /**/ public final class ComboCE  extends MDCellEditor { ComboCE()  { super(0); } }
    /**/ public final class TextCE   extends MDCellEditor { TextCE()   { super(1); } }
    /**/ public final class ButtonCE extends MDCellEditor { ButtonCE() { super(2); } }

    private static boolean ignoreCategorySelectEvents = false;
    
    /** This was made as a global editor for all columns, which besides bad design, means that it makes it
        impossiblefor one cell to stop editing in a single row, and another to begin. Would be nice to move
        most functionality into the 3 different cell editor sub-classes */
    private class MDCellEditor extends DefaultCellEditor
    {
        private final JTextField field; 
        private final JPanel box = new JPanel();
        private final JComboBox catCombo = new JComboBox();
        
        private final edu.tufts.vue.metadata.gui.MetaButton deleteButton;

        private final int INSTANCE_COLUMN; // hard coded column we're meant to be rendering for

        final String name;

        private VueMetadataElement source_VME;
        private MetadataList.SubsetList source_list;

        public String toString() { return ""+INSTANCE_COLUMN; }

        protected MDCellEditor(final int i) {
            super(new JTextField());
            INSTANCE_COLUMN = i;

            name = getClass().getSimpleName() + "/" + i;

            box.setName("MD:cellEditor:box-" + i);
            box.setOpaque(false);
            box.setLayout(new BorderLayout());
            // box.addMouseListener(new tufts.vue.MouseAdapter("MDCE:JPanel-container-"+i) { public void mousePressed(MouseEvent e) {
            //     model.setSaved(activeEditRow, true);
            //     stopCellEditing();
            // }});
            
            if (INSTANCE_COLUMN == 0) {
                initCatCombo();
                field = null;
                deleteButton = null;
            } else if (INSTANCE_COLUMN == 1) {
                field = (JTextField) getComponent();
                field.setName("MD:cellEditor-" + name);
                field.setFont(GUI.LabelFace);
                field.addFocusListener(new FocusAdapter() {
                        public void focusLost(FocusEvent fe) {
                            if (DEBUG.Enabled) {
                                VUE.diagPush("FL" + i);
                                debug("focusLost (" + (editWasCanceled?"CANCELLED":"natural") + ") source_VME=" + source_VME);
                                if (DEBUG.PAIN && fe != null) debug("opposite: " + GUI.name(fe.getOppositeComponent()));
                            }
                            activeTextEdit = null;
                            if (editWasCanceled) {
                                model.clearBulge("canceled");
                                flush_UI_text();
                            } else {
                                saveChangesOnFocusLoss(fe);
                            }
                            activeEditRow = -1;
                            if (DisplayChangesDuringEdit && model.keyColumnVisible())
                                mdTable.repaintLater();
                            if (DEBUG.Enabled) VUE.diagPop();
                        }
                        public void focusGained(FocusEvent fe) {
                            if (DisplayChangesDuringEdit && model.keyColumnVisible())
                                mdTable.repaintLater();
                        }
                });
                field.addKeyListener(new KeyAdapter() { public void keyPressed(KeyEvent ke) {
                    // BTW, *sometimes* this works automatically in the JTextField, but for some reason, not always.
                    if (ke.getKeyCode() == KeyEvent.VK_ESCAPE)
                        mdTable.cancelAnyEditing();
                }});
                deleteButton = null;
            } else { // if (INSTANCE_COLUMN == 2) {
                field = null;
                deleteButton = new edu.tufts.vue.metadata.gui.MetaButton(MetadataEditor.this, "realDelete");
            }
        }

        private void debug(String s) { Log.debug(this.name + " " + s); }

        private void initCatCombo() {
            catCombo.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
          //catCombo.putClientProperty("JComboBox.isSquare", Boolean.TRUE); // won't be shown or no effect here
            catCombo.setModel(CategoryMenuModel);
            catCombo.setRenderer(new CategoryComboBoxRenderer());
            catCombo.setFont(GUI.LabelFace);
            //catCombo.setMaximumSize(new java.awt.Dimension(300, 20));

            //if (INSTANCE_COLUMN == 0) box.add(catCombo);

            // note: if this had been an action listener insted of an item listener, we may not have had to deal with the event disabling code
            catCombo.addItemListener(new ItemListener() { public void itemStateChanged(final ItemEvent ie) {
                // Note: this is being fired at from the INTERNAL select down in selectKnownCategory, and always has been!
                // No wonder the combo-box has been so wiggy.  We have to handle event ignoring for this to work.
                if (ignoreCategorySelectEvents || ie.getStateChange() != ItemEvent.SELECTED)
                    return;
                
                if (DEBUG.PAIN) debug("catCombo itemStateChanged " + Util.tags(ie.getItem()));
                
                if (ie.getItem() instanceof edu.tufts.vue.metadata.gui.EditCategoryItem) {
                    if (mdTable.getCellEditor() == null) {
                        // We shouldn't see this now that events are disabled when editCategory refreshes the model.
                        if (DEBUG.Enabled) debug("ignoring itemStateChanged: no active editor");
                    } else {
                        popEditCategoryDialog(); // and block...
                        mdTable.stopAnyEditing();
                        refreshAll();
                    }
                } else {
                    catComboHasMadeSelection(ie);
                    mdTable.stopAnyEditing();
                }
            }});

        }            
       
        private void popEditCategoryDialog() {
            // todo: re-recreating this each time is usually not a great idea -- we often end up with multiple objects
            // hanging out there in the heap, still listening to events, sometimes doing nasty things when they get them
            // that conflict with newer versions of the same components.
            
            if (mdTable.getCellEditor() != MDCellEditor.this) throw new Error("what the hell in " + GUI.name(MDCellEditor.this));
            
            final JDialog ecd = new JDialog(VUE.getApplicationFrame(), VueResources.local("dialog.editcat.title"));
            ecd.setModal(true);
            ecd.add(new CategoryEditor(ecd,catCombo,MetadataEditor.this,single,activeEditRow,activeEditCol));
            ecd.setBounds(475,300,300,250);
            ecd.setVisible(true); // We will now block until modal dialog returns...
            Log.info("back from CategoryEditor dialog");
        }

        private void catComboHasMadeSelection(final ItemEvent ie)
        {
            final Object selected = catCombo.getSelectedItem();

            if (selected instanceof OntType == false) {
                // assume coming from selectKnownCategory, having picked a divider or something
                return;
            }
                
            final OntType ontType = (OntType) catCombo.getSelectedItem();
            final int row = activeEditRow;
            final VueMetadataElement tableVME = model.getValueAt(row, -1); // column ignored

            if (DEBUG.Enabled) Log.debug("OntType selected: " + ontType);
            // if (DEBUG.PAIN || tableVME == null) debug("VME for last request row " + row + ": " + tableVME);

            if (tableVME == null) {
                if (DEBUG.Enabled) debug("aborting catCombo state change on null VME: probably model refresh");
                return;
            }
            publishEdit(activeEditRow,
                        freshVME(ontType.getAsKey(), tableVME.getValue()),
                        "Metadata Category Key");

            // final VueMetadataElement vme = freshVME(ontType.getAsKey(), tableVME.getValue());
            // if (DEBUG.PAIN) debug("constructed new key in: " + vme);
            // if (group != null) {
            //     publishGroupChange(row, vme);
            // }
            // else if (single != null) {
            //     if (single.getMetadataList().getCategoryListSize() > row) {
            //         single.getMetadataList().getCategoryList().set(row, vme);
            //     } else {
            //         Log.error("adding a new VME via category menu; should not happen: " + vme);
            //         single.getMetadataList().getMetadata().add(vme);
            //     }
            // }
        }

        private boolean editWasCanceled = false;
       
        @Override public boolean stopCellEditing() { return stopEditing(false); }
        @Override public void cancelCellEditing() { stopEditing(true); }

        private boolean stopEditing(boolean cancel) {
            if (DEBUG.PAIN) { debug("stopCellEditing, " + (cancel?"CANCELLING":"saving")); if (DEBUG.META) Util.printClassTrace("!java"); }
            editWasCanceled = cancel;
            if (cancel) {
                super.cancelCellEditing();
                return true; // ignored
            } else
                return super.stopCellEditing();
        }
        
        private void load_UI_text(VueMetadataElement vme) {
            source_list = null;
            if (group != null)
                source_list = group.getMetadataList().getCategoryList();
            else if (single != null)
                source_list = single.getMetadataList().getCategoryList();
            else
                throw new Error("can't load VME w/out source list: " + vme);

            if (DEBUG.PAIN) debug("pushing to UI from " + vme);
            
            if (vme == null) vme = InputVME;

            //if (DEBUG.PAIN) Log.debug(name + " pushing to UI from " + vme + (vme.getValue() == EmptyValue ? " <TheEmptyValue>" : ""));
            
            source_VME = vme;
            if (vme != InputVME)
                field.setText(vme.getValue());
            else
                if (DEBUG.PAIN) debug("re-using any old input text on InputVME: " + Util.tags(field.getText()));
                // leave old text input as a handy copy: it will automatically all be selected for easy replacement
        }
        
        private void flush_UI_text() {
            source_VME = null;
            source_list = null;
            field.setText("");
        }
        private void apply_UI_text(VueMetadataElement target) {
            // source_VME = vme;
            // field.setText(vme.getValue());
        }
        
       @Override public java.awt.Component getTableCellEditorComponent(final JTable table, final Object _vme, boolean isSelected, final int row, final int col)
       {
           if (DEBUG.PAIN) debug("getTableCellEditorComponent ic" + INSTANCE_COLUMN + ": " + row + "," + col + " " + _vme);
           activeEditRow = row;
           activeEditCol = col;
           
           final VueMetadataElement vme = (VueMetadataElement) _vme;
           
           if (col == TC_COMBO) {
               //CategoryMenuModel.selectBestMatchQuietly(getKeyForRow(row));
               CategoryMenuModel.selectBestMatchQuietly(vme.getKey());
               return installRenderBorder(catCombo, row, col);
           }
           else if (col == TC_TEXT) {
               //---------------------------------------------------------------------------------------------------------------
               //       THIS IS WHERE THE VueMetadataElement VALUE IS EXTRACTED FROM THE MODEL AND PUSHED TO THE UI
               //---------------------------------------------------------------------------------------------------------------
               load_UI_text( vme ); // copy into field
               GUI.invokeAfterAWT(new Runnable() { public void run() {
                   // This object is not yet in the AWT hierarchy (we haven't even returned it yet).  It
                   // normally will be alive be by the time this is run.
                   field.requestFocus();
                   if (vme == InputVME)
                       field.selectAll();
               }}); 
               MetadataEditor.this.activeTextEdit = this;
               // field.setBorder(null); // this will remove the focus border and cause the field to fill the entire cell
               return field;
           }
           else if (col == TC_BUTTON) {
               // The way we currently detect a click on one of these buttons is to actually return it when an
               // editor is requested, which is what the table will do if there's a click over the region.
               // This is a pain in that we must return a button that looks exactly like the render button,
               // including border, or the UI will refelect the irrelevant editor-active state for this cell.
               // We also need, of course, to report to the button what row it's in when fetched so it knows
               // what's being clicked on.
               deleteButton.setRowForButtonClick(row);
               if (getActiveTextEdit() != null)
                   deleteButton.setBackground(Color.white); // we'll cancel the edit instead of delete
               else
                   deleteButton.setBackground(Color.yellow);
               return deleteButton;
           }
           
           // if (LIMITED_FOCUS) field.addMouseListener(new MouseAdapter() { public void mouseExited(MouseEvent me) { stopCellEditing(); }});
           
           // so this method had always returned a panel containing what was really needed -- that
           // was causing the horrible two-double-clicks to focus problem for the value JTextField.
           return box;
       }
       
        private void saveChangesOnFocusLoss(final FocusEvent fe)
        {
            //if (DEBUG.Enabled) debug("handleFieldFocusLost (" + (editWasCanceled?"CANCELLED":"natural") + ") source_VME=" + source_VME);
                
            final int row = activeEditRow;
            if (DEBUG.PAIN) debug("row of last editor requested: " + row);
        
            final TableCellEditor editor = mdTable.getCellEditor();
            if (DEBUG.PAIN) {
                debug("getCellEd: " + Util.tags(editor) + (editor == null ? " (no longer editing)" : ""));
                if (editor != null && editor != MDCellEditor.this) {
                    debug("is not us: " + Util.tags(MDCellEditor.this));
                    if (editor instanceof MDCellEditor)
                        debug("remote row: " + activeEditRow);
                }
            }
            if (editor != null)
                editor.stopCellEditing();
                  
            final String fieldText = field.getText();
            final String trimText = fieldText.trim();

            if (DEBUG.PAIN) {
                debug("source_VME: " + source_VME);
                debug("field-text: " + Util.tags(fieldText));
            }

            //----------------------------------------------------------------------------------------
            // Check to see if a meaninful change is ready to be applied (if not, early return)
            //----------------------------------------------------------------------------------------
        
            if (source_VME == InputVME) {
                if (trimText.length() == 0) {
                    // if (DEBUG.PAIN) Log.debug("do nothing, no new text");
                    model.clearBulge("nothing to do, no new text");
                    flush_UI_text(); // don't allow re-use
                    return;
                }
                // new to the model
                if (DEBUG.PAIN) debug("new md item for model");
            } else {
                if (DEBUG.PAIN) debug("existing item being edited: " + source_VME);
                // we have something to replace, tho it may be a fresh empty vme
                //if (DEBUG.Enabled && isInputVME(real_VME)) Log.info("SEEING INPUT VME IN MODEL", new Throwable("HERE"));
                //if (trimText.length() == 0 && isEmptyVME(real_VME)) {
                if (trimText.equals(source_VME.getValue())) { // getValue can return null
                    // The trim() will allow us to eliminate whitespace on a field value by editing, but not add it.
                    model.clearBulge("nothing to do, no significant change");
                    flush_UI_text(); // don't allow re-use
                    return;
                }
            }

            // Even if nothing is now selected, we still want to process up to this point to allow
            // for the flush_UI_text() calls above.
            
            if (single == null && group == null)
                return;

            //----------------------------------------------------------------------------------------
            // Actually apply a change:
            //----------------------------------------------------------------------------------------

            final VueMetadataElement new_VME;
                  
            if (source_VME == InputVME) {
                if (DEBUG.Enabled) debug("at bottom with InputVME -- creating fresh -- ignoring any category menu selection");
                // Leaving out the category should be fine (we don't have one in this case), as any attempt to
                // click on the category menu (displaying "none" at this point), will focus-loss the text
                // input, aborting the new line entirely if empty, or creating a new VME to then category edit
                // if text is present.
                new_VME = freshVME(trimText);
            } else {
                new_VME = freshVME(source_VME.getKey(), trimText); // copy over the old key
                if (DEBUG.PAIN) debug("replacement VME with copied key: " + new_VME);
            }
        
            if (source_VME == InputVME) {
                if (DEBUG.PAIN) debug("appending");
                source_list.add(new_VME);
                if (!model.clearBulge("success: filled bulge with new data"))
                    model.refresh(); // should never happen, but just in case
            } else {
                if (DEBUG.PAIN) debug("inserting new_VME at row " + row + " " + new_VME + " -> " + source_list);
                // We're using ROW, which is from the current model, which is technically bad, because
                // it could have changed if this was a focus-loss on selection change, but in that
                // case, editing should have been canceled.
                source_list.set(row, new_VME); // WE ALWAYS WRITE OVER THE OLD VME

                // BUG: DOES NOT WORK IN THE GROUP CASE
            }

            // todo: handle in publish
            
            // If we're not handling multiples, we're already done.  If we are, we've only just changed
            // the temporary group data, and we now need to publish the change to the selection.
            
            if (groupContents != null) {
                // final VueMetadataElement old = source_list.get(row);
                for (LWComponent c : groupContents) { 
                    final MetadataList md = c.getMetadataList();
                    if (source_VME == InputVME)
                        md.addElement(new_VME);
                    else
                        md.replaceValueForKey(new_VME);
                    c.layout();
                }
            }
                  
            if (single != null)
                single.layout();

            VUE.getActiveMap().markAsModified();
            // todo: would be better to issue a model repaint event
            VUE.getActiveViewer().repaint();
            mdTable.repaint();
        }

        /** @inteface CellEditor */ @Override public boolean shouldSelectCell(EventObject o) { return false; }

        /** @inteface CellEditor: "If editing can be started this method returns true." */
        @Override public boolean isCellEditable(java.util.EventObject object) {
            if (DEBUG.PAIN) { VUE.diagPush("isCellEditable"); Log.debug(object /*,new Throwable("HERE")*/  ); }
            //boolean canEdit = canEditWithSideEffects(object);
            boolean canEdit = canEdit(object);
            if (DEBUG.PAIN) { Log.debug("canEdit = " + canEdit); VUE.diagPop(); }
            return canEdit;
       }
        
        private boolean canEdit(java.util.EventObject object)
        {
            if (object == EDIT_REQUEST)
                return true;
            else if (object instanceof MouseEvent == false) {
                if (DEBUG.Enabled) Log.debug("unexpected event input for isCellEditable: " + Util.tags(object));
                return false;
            }
            final MouseEvent me = (MouseEvent) object;
            final Point point = me.getPoint();
            final int row = mdTable.rowAtPoint(point); 
            final int col = mdTable.columnAtPoint(point);
            if (col == TC_BUTTON) {
                if (DEBUG.PAIN) Log.debug("button-detect in isCellEditable, row " + row + ", col " + col);
                // CLICK ON THE MINUS BUTTON -- if we actually miss the button itself in the cell, this will ensure an edit cancel.
                // if (mdTable.getCellEditor() != null)
                //     mdTable.getCellEditor().cancelCellEditing();
                return true; 
            }
            else if (GUI.isDoubleClick(me)) {
                if (DEBUG.PAIN) Log.debug("double-click detect in isCellEditable, row " + row + ", col " + col);
                return true;
            }
            return false;
        }
    }

    private JComponent installRenderBorder(JComponent c, int row, int col) {
        c.setBorder(col == TC_BUTTON ? buttonBorder : insetBorder);
        // final Border b = getRenderBorder(row, col);
        return c;
    }
    
    private void publishEdit(int row, VueMetadataElement vme, String description)
    {
        final VueMetadataElement oldVME = model.getValueAt(row, -1);

        if (DEBUG.Enabled) {
            Log.debug(Util.tags(description) + "; publishing change to row " + row + ": " + oldVME);
            Log.debug(Util.tags(description) + ";      replacement for row " + row + ": " + vme);
        }
        
        if (group != null) {
            for (LWComponent c : groupContents) {
                final MetadataList.SubsetList md = c.getMetadataList().getCategoryList();
                md.set(md.indexOf(oldVME), vme); // note: all will have same instance
                if (DEBUG.Enabled) Log.debug("published to: " + c);
            }
            group.getMetadataList().getCategoryList().set(row, vme);
            VUE.getActiveMap().notify(MetadataEditor.this, tufts.vue.LWKey.MetaData); // todo: undoable event
        }
        else if (single != null) {
            single.getMetadataList().getCategoryList().set(row, vme); // better: model.setVmeAt
            // if (single.getMetadataList().getCategoryListSize() > row) {
            //     single.getMetadataList().getCategoryList().set(row, vme);
            // } else {
            //     Log.error("adding a new VME via category menu; should not happen: " + vme);
            //     single.getMetadataList().getMetadata().add(vme);
            // }
            single.notify(MetadataEditor.this, tufts.vue.LWKey.MetaData); // todo: undoable event
        }
        VUE.getActiveMap().markAsModified();
        VUE.getActiveMap().getUndoManager().mark(description);
        VUE.getActiveMap().notify(MetadataEditor.this, tufts.vue.LWKey.Repaint);
    }
    
    /**
     * [DAN] watch out for current == null
     * [SMF] So what getRowCount returns is presumably important for what the UI decides to draw, if anything.
     * This only models the category (type 1) items from MetadataList.
     *
     * This is public for MetaButtonPanel
     **/
    public class MDTableModel extends AbstractTableModel
    {
        private final java.util.List<Boolean> categoryFound = new java.util.ArrayList<Boolean>(32);

        private int cols = initCols(2);
        private LWComponent source;

        private int initCols(int cols) {
            TC_BUTTON = buttonColumn = (cols - 1); // 1 or 2
            TC_TEXT = TC_BUTTON - 1; // 0 or 1
            TC_COMBO = TC_BUTTON - 2; // -1 or 0 (-1 if not visible)
            return cols;
        }

        @Override public Class getColumnClass(int col) {
            if (col == TC_TEXT)
                return JTextField.class;
            else if (col == TC_COMBO)
                return JComboBox.class;
            else
                return Object.class;
        }
         
        @Override public void setValueAt(Object value, int row, int col) {
            if (DEBUG.PAIN) Log.debug("  setValueAt " + row + "," + col + " " + Util.tags(value) + " (always ignored)");
            /*fireTableDataChanged();*/
        }

        private boolean wasCategoryFound(int row) { return (row >= 0 && row < categoryFound.size()) ? categoryFound.get(row) : false; }
        // CALLED DURING RENDERING OF THE COMBO-BOX COLUMN.  Apparently is only used in one place for knowing if to render a tooltip or not.
        // Is overkill complexity: could be re-calling some version of find/selectCategory based on the key found at row.
        private void reportCategoryFound(int row, boolean found) {
            if (categoryFound.size() <= row) // expand valid porition of capacity
                for (int i = categoryFound.size(); i < row + 1; i++)
                    categoryFound.add(Boolean.TRUE);
            categoryFound.set(row, found ? Boolean.TRUE : Boolean.FALSE);
        }
         
        /** @interface javax.swing.table.TableModel */
        public int getRowCount() {
            final int displayRows = getDataRowCount() + PlusOneWhenAdding;
            // even if we're not adding, always render at least one empty row:
            return displayRows > 0 ? displayRows : 1;
        }
        
        private int getDataRowCount() {
            return source == null ? 0 : source.getMetadataList().getCategoryListSize();
        }
         
        /** @interface javax.swing.table.TableModel */
        public int getColumnCount() { return cols; }

        boolean keyColumnVisible() { return cols == 3; }
         
        private void toggleCategoryKeyColumn() {
            if (DEBUG.PAIN) Log.debug("model: toggleCategoryKeyColumn: cols <- " + cols);

            if (this.cols == 2)
                this.cols = initCols(3);
            else
                this.cols = initCols(2);
            
            fireTableStructureChanged();
            if (DEBUG.PAIN) Log.debug("model: toggleCategoryKeyColumn: cols -> " + cols);
        }

        public void refresh() {
            this.source = (single != null ? single : group);
            if (DEBUG.Enabled) Log.debug(getClass().getName() + ": refresh; src==" + source);
            fireTableDataChanged();
        }
        
         
        // public void fireTableStructureChanged() { super.fireTableStructureChanged(); }
         
        /* note: CellEditor also has isCellEditable (main entry point for mouse actions on
         * metadatatable) actually the table has this method through its cell editor */
        @Override public boolean isCellEditable(int row,int column) {
            // old -- for combined view with ont types non editable -- now in seperate panel
            //if(getValueAt(row,column) instanceof OntType) return false;
            // deletebutton is now editable...
            //return ( (column == buttonColumn -1) || (column == buttonColumn - 2) )
            return true;
        }
         
        /** @interface javax.swing.table.TableModel */
        public VueMetadataElement getValueAt(int row, int _column_ignored_)
        {
            final int size = getDataRowCount();
            if (DEBUG.PAIN) Log.debug(" getValueAt" + (row<10?"  ":" ") + row + "," + _column_ignored_ + " size=" + size + (PlusOneWhenAdding != 0 ? "+1":""));
            if (source != null) {
                if (size == 0 || row == size)
                    return InputVME;
                else if (row >= size) 
                    return null;
                return source.getMetadataList().getCategoryListElement(row);
            }
            else Log.error(" getValueAt " + row + "," + _column_ignored_ + ": no data in model");
            return null;
        }

        /**
         * Set the model into a state of reporting having exactly one more piece of meta-data than the actual
         * component/selection has in it, so that we can render/activate a cell-editor at the bottom without
         * having to modify the actual component meta-data.  Calls are idempotent, so we don't have to 
         * worry about overlapping / too many calls.  Todo: move methods to MDTableModel.ensureBulge/clearBulge
         */
        private boolean ensureBulge(String reason) {
            if (DEBUG.PAIN) {
                final String noneed = (PlusOneWhenAdding == 1) ? " (unneeded)" : "";
                Log.debug(Util.color("temporary model bulge to +1: " + reason + noneed, Util.TERM_CYAN));
            }
            if (PlusOneWhenAdding == 0) {
                PlusOneWhenAdding = 1;
                refresh();
                return true;
            }
            return false;
        }
        private boolean clearBulge(String reason) {
            if (DEBUG.PAIN) {
                final String noneed = (PlusOneWhenAdding == 0) ? " (unneeded)" : "";
                Log.debug(Util.color("clearing the +1 model bulge: " + reason + noneed, Util.TERM_CYAN));
            }
            if (PlusOneWhenAdding != 0) {
                PlusOneWhenAdding = 0;
                refresh();
                return true;
            } else
                return false;
        }
    
        public void deleteAtRow(final int row)
        {
            final VueMetadataElement vme = getValueAt(row, -1);
            
            if (DEBUG.Enabled) Log.debug("deleteAtRow " + row + " " + vme);

            if (row < 0 || row >= getDataRowCount()) {
                // Checking getDataRowCount() is crucial, or we could end up secretly deleting OTHER data-list
                // items, that are further down the list than the CATEOGRY items, such as merge-source data.
                Log.warn("invalid row for delete: " + row);
                return;
            }
            if (vme == InputVME || vme == null) {
                Log.warn("invalid for delete: " + vme);
                return;
            }
            
            if (groupContents != null) {
                for (LWComponent c : groupContents) {
                    final List<VueMetadataElement> md = c.getMetadataList().getAll(); // could use subesetlist?
                    if (md.remove(vme)) {
                        if (DEBUG.Enabled) Log.debug("multiple deleted " + vme + "; " + c.getDiagnosticLabel());
                        if (md.size() == 0) 
                            c.layout(); // no more meta-data: icon display may change
                    } else {
                        Log.warn("multiple failed to remove " + vme + "; " + c.getDiagnosticLabel());
                    }
                }
            }
            
            // We want the below for both source == single, where we'll delete data off the actual node, and
            // source == group, where we'll just delete out of our table-model LWGroup holder.
            
            if (DEBUG.Enabled) Log.debug("delete--row " + row + " " + vme);
            
            // How did removing by row ever work?  We're indexing into the full, unfiltered-by-type VME list.
            // Well, if CATEGORY type really is maintained first in the list ("CategoryFirstList"), then this
            // could work, but if it ISN'T, such as we suspect with RESOURCE type, then this will delete the
            // wrong thing -- but the only thing with RESOURCE_CATEGORY VME types is currently the LWMap
            // itself, so we're just getting lucky here.
            
            // Note that we could just use the fetched getValueAt VME from the row as a delete-key source, as
            // opposed to our row index, but there can be multiple VME's with the same key + value (which
            // really isn't something we should allow, but we do).  To make it feel right, if there are dupes,
            // we need to delete the one at the actual row, instead of just the 1st one with that same
            // key/value in the list.

            final VueMetadataElement removed = source.getMetadataList().getAll().remove(row);
            
            if (!vme.equals(removed))
                Log.error("BAD DELETE: " + removed + " != " + vme);
            
            refresh();
            
            // todo: someday, this layout/repaint would be better triggered by some kind of model update
            // event from MetadataLlist up through its LWComponent, which if we had could then
            // even become undoable.  Also, make these undoable events!
            
            if (single != null) {
                single.layout();
                single.notify(MDTableModel.this, tufts.vue.LWKey.MetaData); 
            } else {
                VUE.getActiveMap().notify(MDTableModel.this, tufts.vue.LWKey.MetaData);
            }
            
            VUE.getActiveMap().markAsModified();
            VUE.getActiveMap().getUndoManager().mark("Remove Data");
        }
    }

    public void listChanged() {
        // This was implemented horribly, and we don't actually need it.
        
        // THIS WAS FIRING, VERY FREQUENTLY, DURING DERSERIALIZATION AND SOMETIMES CAUSING
        // AWT TO HANG (deep AWT cursor code hang?)

        // So this was happening for EVERY SINGLE VueMetadataElement add/load ON EVERY SINGLE NODE.
        // And since the impl has put an EMPTY VME on every node, that's at minimum once per
        // LWComponent.  And god forbid there are, say, 10 meta-data fields on a node from a
        // data-set -- that means 10 times per node. So with a map that has, say 500 nodes, it
        // means 5,000 (FIVE-THOUSAND) calls to the AWT to run freaking VALIDATE() deserializing
        // such maps...

        // Furthermore, after init, we don't actually edit VME meta-data at runtime outside of the UI,
        // meaning we don't need this at all.
        // tufts.Util.printStackTrace("listChanged");
        // ((MetadataTableModel)mdTable.getModel()).refresh();
        // validate();
    }

    // the following was for the MapInfoPanel usage of this component -- currently using a more direct approach
    // with JTable filtering built into JDK 1.6 the following approach might make more sense (and if additional
    // filtering is needed and/or additional flexibility/ modifiability of filters)
    /*public class CreatorFilterModel extends MetadataTableModel {
        private int firstCreatorRow = -1;
        public int findFirstCreatorRow() {
            if(current == null)
                return -1;
            // todo: put proper dublic core category here
            //return current.getMetadataList().findCategory("");
            return -1;
        }
        public int getRowCount() {
            return super.getRowCount() - 1;
        }
        public Object getValueAt(int row,int column) {
            findFirstCreatorRow();
            if(row< firstCreatorRow)
                return super.getValueAt(row,column);
            else
                return super.getValueAt(row-1,column); 
        }
    }*/

}

    
    // /** @return true if we can find a proper RDF style category for the given keyName in the JComboBox model -- will also select that item in the JComboBox */
    // public static void selectKnownCategory(final String keyName, final JComboBox catCombo) {
    //     CategoryMenuModel.selectBestMatch(keyName);        
    //     // ignoreCategorySelectEvents = true;
    //     // int index = -1;
    //     // try {
    //     //     index = CategoryMenuModel.indexOfCategoryKey(keyName);
    //     //     // If not found, default to the 1st separator:
    //     //     catCombo.setSelectedIndex(index >= 0 ? index : 1);
    //     // } catch (Throwable t) {
    //     //     Log.error("selectKnownCategory " + keyName + " " + catCombo, t);
    //     //     return false;
    //     // } finally {
    //     //     ignoreCategorySelectEvents = false;;
    //     // }
    //     // return index != -1;
    // }
        
    //     /** holy christ -- this has major side effects -- very hairy to figure out -- apparenly being used to detect mouse click! */
    //     private boolean canEditWithSideEffects(java.util.EventObject object)
    //     {
    //         if (object == EDIT_REQUEST)
    //             return true;
    //         else if (object instanceof MouseEvent == false) {
    //             if (DEBUG.Enabled) Log.debug("unexpected event input for isCellEditable: " + Util.tags(object));
    //             return false;
    //         }
    //         final MouseEvent me = (MouseEvent) object;
    //         final Point point = me.getPoint();
    //         final int row = mdTable.rowAtPoint(point); 
    //         final int col = mdTable.columnAtPoint(point);
    //         if (col == TC_BUTTON) {
    //             if (DEBUG.PAIN) Log.debug("button-detect in isCellEditable, row " + row + ", col " + col);
    //             // CLICK ON THE MINUS BUTTON -- if we actually miss the button itself in the cell, this will ensure an edit cancel.
    //             // if (mdTable.getCellEditor() != null)
    //             //     mdTable.getCellEditor().cancelCellEditing();
    //             // returning true will allow us to "start editing".  This way, the table will request edit "editor" for
    //             // this cell (the button), and once it has the editor, it will pass this mouse press on to the button.
    //             // If we return false, no button could get the event, and we'd have to catch this mouse event elsewhere.
    //             // Since the button is actually a single constant renderer style button, the design is to call
    //             // setRow on our meta button when it is requested, so it will know what row to fire for.
    //             return true; 
    //         }
    //         else if (GUI.isDoubleClick(me)) { // || col == buttonColumn )
    //             if (DEBUG.PAIN) Log.debug("double-click detect in isCellEditable, row " + row + ", col " + col);
    //             // What's this do???
    //             model.setSaved(row, false);
    //             int lsr = model.lastSavedRow;
    //             if(lsr != row)
    //                 model.setSaved(lsr,true);
    //             // model.refresh();
    //             // mdTable.repaint();
    //             return true;
    //         }
    //         else if (row != -1 && !model.getIsSaved(row)) {
    //             if (DEBUG.PAIN) Log.debug("any non-saved detect in isCellEditable, row " + row + ", col " + col);
    //             model.setSaved(row, false);
    //             return true;
    //         }
    //         return false;
    //     }
        
    
    // /**
    //  * this was a hack to get borders without using a full JTable grid, but it's a giant messy pain to try and get
    //  * working just right, requires the further getIsSaved hack and all the places that state needs updating, and
    //  * there's no reason not to just use a the JTable grid.  If we want the grid off the buttons at the right, we can
    //  * easily merge the buttons into the the text cell (dropping column three entirely) with a BorderLayout.EAST position
    //  * and a bit of extra mouse code to check for clicks in the right of that cell. */
    // private Border getRenderBorder(int row, int col)
    // {
    //     //if (DEBUG.BOXES) return DebugBorder;
    //     if (col == TC_BUTTON)
    //         return insetBorder;
    //     final boolean saved = model.getIsSaved(row);
    //     // The idea was, I think, if not saved, it's in an editor, and we don't want a border.
    //     // Smarter now: if we're fetching an editor, we'll ask for border or not expliticly
    //     // depending on editor impl, so always put the border on, which will probably
    //     // fix a bunch of border missing bugs.
    //     //if (saved) {
    //     if (true) {
    //         // if (true) return BorderFactory.createCompoundBorder(topLeftBox,insetBorder);
    //         // if (true) return BorderFactory.createCompoundBorder(fullBox,insetBorder);
    //         boolean aboveSaved = false;
    //         boolean belowSaved = false;
    //         if (row > 0)
    //             aboveSaved = model.getIsSaved(row - 1);
    //         else //if (row == -1)
    //             aboveSaved = false;
    //         final int rowCount = model.getRowCount();
    //         if (row < rowCount) {
    //             belowSaved = model.getIsSaved(row + 1);
    //             //if (DEBUG.PAIN) Log.debug("last row, belowSaved = " + belowSaved);
    //         } else if (row >= rowCount - 1) {
    //             belowSaved = false;
    //             //if (DEBUG.PAIN) Log.debug("last row, belowSaved = " + belowSaved);
    //         }
    //         //if (DEBUG.PAIN) Log.debug("belowSaved = " + belowSaved);
    //         if (col == TC_TEXT) {    
    //                  if(!aboveSaved && !belowSaved) { return BorderFactory.createCompoundBorder(fullBox,insetBorderNoBottom); } // only seen at top
    //             else if( aboveSaved && !belowSaved) { return BorderFactory.createCompoundBorder(fullBox,insetBorderNoBottom); }
    //             else if(!aboveSaved &&  belowSaved) { return BorderFactory.createCompoundBorder(noBottomBox,insetBorder); }
    //             else if( aboveSaved &&  belowSaved) { return BorderFactory.createCompoundBorder(noBottomBox,insetBorder); }
    //         }
    //         else {
    //                  if(!aboveSaved && !belowSaved) { return BorderFactory.createCompoundBorder(topLeftBotBox,insetBorderNoBottom); }  // only seen at top
    //             else if( aboveSaved && !belowSaved) { return BorderFactory.createCompoundBorder(topLeftBotBox,insetBorderNoBottom); }
    //             else if(!aboveSaved &&  belowSaved) { return BorderFactory.createCompoundBorder(topLeftBox,insetBorder); }
    //             else if( aboveSaved &&  belowSaved) { return BorderFactory.createCompoundBorder(topLeftBox,insetBorder); }
    //         }    
    //     }
    //     return insetBorder;
    // }
    

