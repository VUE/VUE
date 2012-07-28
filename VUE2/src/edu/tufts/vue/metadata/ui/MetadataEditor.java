
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

import java.util.EventObject;
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
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;

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
    
  //public final static String CC_ADD_LOCATION_MESSAGE = "<html> Click [+] to add this <br> custom category to your own list. </html>";
  //public final static String CC_ADD_LOCATION_MESSAGE = "<html>Click &oplus; to add this custom<br>category to your own list."; // nice idea, but &oplus; looks like crap when small
    public final static String CC_ADD_LOCATION_MESSAGE = "<html>Click + to permanently add this<br>custom category to your own list.";
    
    // public final static int ROW_HEIGHT = 31;
    public final static int ROW_HEIGHT = 27;
    public final static int CELL_VERT_INSET = 4;
    public final static int CELL_HORZ_INSET = 6; // Left aligns with JTextField editor text in Mac Aqua -- todo: check Windows
    public final static int BUTTON_COL_WIDTH = 27;
    
    private static final java.awt.Color GRID_COLOR = (DEBUG.BOXES) ? Color.black : new Color(196,196,196);
    
    public final static int CC_ADD_LEFT = 0;
    public final static int CC_ADD_RIGHT = 1;
    
    //public final static String TAG_ONT = "http://vue.tufts.edu/vue.rdfs#Tag";
            
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
        
    private final JTable mdTable;
    private final MDTableModel model;

    private CellEditor activeTextEdit;
    
    private LWComponent current;
    private LWComponent previousCurrent;
    private LWGroup currentMultiples;
    private LWGroup previousMultiples;
    
    /** the column the +/- buttons are at: changes table structure changes to display categories combo box
     * JTextField column is tested as: buttonColumn-1, JComboBox column as: buttonColumn-2 */
    private int buttonColumn = 1;
    /** table-column: these are convenience comparitors: they will change when the structure changes (BC_BUTTON should always == buttonColumn) */
    private int TC_BUTTON, TC_TEXT, TC_COMBO;

    private boolean focusToggle = false;
    
    private final JButton autoTagButton =  new JButton(VueResources.local("keywordPanel.autotag"));

    private static final CategoryModel OntologiesList = VUE.getCategoryModel();
    private static final CategoryComboBoxModel GlobalCategoryComboBoxModel = new CategoryComboBoxModel();
        
    /** @see ensureModelBulge() */
    private int PlusOneForAdd = 0;

    private static final String EmptyValue = "";
    private static final String[] DefaultVMEObject = new String[] { VueMetadataElement.ONTOLOGY_NONE, EmptyValue };
    
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
        
        @Override public String getToolTipText(MouseEvent location) {
            if (getEventIsOverAddLocation(location))
                return CC_ADD_LOCATION_MESSAGE;
            else
                return null;
        }
        // @Override public boolean getRowSelectionAllowed() { return false; }
        // @Override public boolean getColumnSelectionAllowed() { return false; }
        // @Override public boolean getCellSelectionEnabled() { return false; }
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
        if(getSize().width < 100) 
            setSize(new java.awt.Dimension(300,200));
        
        autoTagButton.setAction(tufts.vue.AnalyzerAction.calaisAutoTagger);
//        autoTagButton.setLabel(VueResources.getString("keywordPanel.autotag"));
        autoTagButton.setFont(tufts.vue.gui.GUI.SmallFont);
        
        this.current = oneOff; // rare usage
        // clear gui below after create table.
        
        // hopefully don't need to do this if successful load from inspector pane'
        /*if(VUE.getSelection().contents().size() > 1) {
            currentMultiples = LWGroup.createTemporary(VUE.getSelection());
            //!! also need to do rest of stuff in selection changed.. }*/
        
        
        mdTable = new MDTable(this.model = new MDTableModel());
        model.clearGUIInfo();
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
                       model.setSaved(row, true);
                       if (mdTable.getCellEditor() !=null)
                           mdTable.getCellEditor().stopCellEditing();

                       headerAddButtonPanel.dispatchEvent(me);
                       mdTable.repaint();
                       
                       if (DEBUG.PAIN) { Log.debug("header mousePressed complete."); VUE.diagPop(); }
                   }
       });
       
        addMouseListener(new tufts.vue.MouseAdapter("JPanel=MetadataEditor") { public void mousePressed(MouseEvent e) {
            int row = mdTable.rowAtPoint(e.getPoint());
            int lsr = model.lastSavedRow;
            if(lsr != row) {    
                model.setSaved(lsr,true);  
                model.lastSavedRow = -1;
            }
            model.refresh();
            
            TableCellEditor editor = mdTable.getCellEditor();
            if(editor != null)
                editor.stopCellEditing();
            mdTable.repaint();
        }});
       
        mdTable.addMouseListener(new tufts.vue.MouseAdapter("MetadataEditor:JTable") {
                public void mousePressed(MouseEvent e) {
                    if(model.lastSavedRow != mdTable.rowAtPoint(e.getPoint()))
                        model.setSaved(model.lastSavedRow,true);
                    mdTable.repaint();
                }  
                public void mouseReleased(MouseEvent e) {
                    /*if(evt.getX()>mdTable.getWidth()-BUTTON_COL_WIDTH) {
                      java.util.List<VueMetadataElement> metadataList = MetadataEditor.this.current.getMetadataList().getMetadata();
                      int selectedRow = mdTable.getSelectedRow();
                      // VUE-912, stop short-circuiting on row 0, delete button has also been returned
                      if( mdTable.getSelectedColumn()==buttonColumn && metadataList.size() > selectedRow) {
                          metadataList.remove(selectedRow);
                          mdTable.repaint();
                          requestFocusInWindow(); }} */
                    
                    if (getEventIsOverAddLocation(e)) {

                        final int row = mdTable.rowAtPoint(e.getPoint());//evt.getY()/mdTable.getRowHeight();
                        final VueMetadataElement vme = (VueMetadataElement) model.getValueAt(row, 0);

                        if (vme.getKey() == null) throw new Error("bad vme: " + vme);
                        
                        final OntType ontType = OntologiesList.addCustomCategory(shortKey(vme.getKey()));

                        if (DEBUG.PAIN) Log.debug("added custom cat: " + Util.tags(ontType));

                        // Note we edit the existing VME directly.  I think this is actually fine,
                        // tho someday they should have all final fields. (This is a problem
                        // if the MetadataList ever keeps tract of when it becoms modified).
                        vme.setObject(new String[] { ontType.getAsKey(), vme.getValue() }); // ick, fix this!

                        if (DEBUG.PAIN) Log.debug("updated VME with new key: " + vme);
                        
                        if (currentMultiples == null) {
                            // If this is the multiples case, we're editing the pre-published set, and this change
                            // should be published to all the multiples, I believe on focus loss?
                            current.notify(MetadataEditor.this, tufts.vue.LWKey.MetaData); // todo: undoable event
                            //VUE.getActiveMap().markAsModified();
                            VUE.getActiveMap().getUndoManager().mark("Metadata Category Key");
                        }
                        
                        if (mdTable.getCellEditor() != null)
                            mdTable.getCellEditor().stopCellEditing();
                        
                        GUI.invokeAfterAWT(new Runnable() { public void run() {
                            refreshAll();
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
                        // button.dispatchEvent(me); // no need to do this
                    }
                    return inside;
                }
                public void mousePressed(MouseEvent e) {
                    if (dispatchedInButton(e)) {
                        addNewRow();
                        mdTable.editCellAt(model.getRowCount() - 1, TC_TEXT, EDIT_REQUEST); 
                        mdTable.repaint();
                    }
                }
            });
            
            /*headerAddButton.addActionListener(new java.awt.event.ActionListener(){
              public void actionPerformed(java.awt.event.ActionEvent e) { if(DEBUG_LOCAL) { System.out.println("MetadataEditor -- table header add button pressed " + e); } } });*/
            
            headerAddButton.addMouseListener(new java.awt.event.MouseAdapter(){
              public void mousePressed(java.awt.event.MouseEvent e) {
                super.mousePressed(e);
                //if(DEBUG_LOCAL)
                //{
                //  System.out.println("MetadataEditor -- table header mouse pressed on button " +
                //        e);
                //}
                headerAddButtonPanel.repaint();
                headerAddButton.repaint();
              }
            });   
        
        setBorder(BorderFactory.createEmptyBorder(10,8,15,6));
        
        validate();
    }
    
//     @Override
//     public Dimension getMinimumSize() { 	   
//         int height = 120;
//         //int lines = 1;
//         int rowCount = mdTable.getRowCount(); 	   	  
//         int rowHeight = mdTable.getRowHeight();
//         //System.out.println("rowHeight :"  + rowHeight);
//         return new Dimension(300,(height+((rowCount-1) * rowHeight)));
//     }
    
//     @Override
//     public Dimension getPreferredSize() { 	   
//  	   return getMinimumSize();
//     }
    
    public void refreshAll() {
    	model.refresh();
        ignoreCategorySelectEvents = true;
        try {
            GlobalCategoryComboBoxModel.refresh();
        } catch (Throwable t) {
            Log.error("refresh: " + GlobalCategoryComboBoxModel, t);
        } finally {
            ignoreCategorySelectEvents = false;
        }
    	validate();
        repaint();
    }
    
    public JTable getTable() { return mdTable; }
    public MDTableModel getModel() { return model; }
    public LWComponent getCurrent() { return current; }
    public LWGroup getCurrentMultiples() { return currentMultiples; }
    public CellEditor getActiveTextEdit() { return activeTextEdit; }

    private static VueMetadataElement emptyVME() { return freshVME(EmptyValue); }
    
    private static VueMetadataElement freshVME(String textValue) {
        final VueMetadataElement vme = new VueMetadataElement();
        vme.setObject(new String[] { VueMetadataElement.ONTOLOGY_NONE, textValue });
        vme.setType(VueMetadataElement.CATEGORY);
        return vme;
    }
    // private static boolean isEmptyVME(VueMetadataElement vme) {
    //     return vme == EmptyVME || vme.getValue() == EmptyValue;
    // }
    private static boolean isInputVME(VueMetadataElement vme) {
        return vme == InputVME;
    }
    
    /**
     * Set the model into a state of reporting having exactly one more piece of meta-data than the actual
     * component/selection has in it, so that we can render/activate a cell-editor at the bottom without
     * having to modify the actual component meta-data.  Calls are idempotent, so we don't have to 
     * worry about overlapping / too many calls.  Todo: move methods to MDTableModel.ensureBulge/clearBulge
     */
    private void ensureModelBulge(String reason) {
        if (DEBUG.PAIN) {
            final String noneed = (PlusOneForAdd == 1) ? " (unneeded)" : "";
            Log.debug(Util.color("temporary model bulge to +1: " + reason + noneed, Util.TERM_CYAN));
        }
        if (PlusOneForAdd == 0) {
            PlusOneForAdd = 1;
            model.clearGUIInfo();
            model.refresh();
        }
    }
    private boolean clearModelBulge(String reason) {
        if (DEBUG.PAIN) {
            final String noneed = (PlusOneForAdd == 0) ? " (unneeded)" : "";
            Log.debug(Util.color("clearing the +1 model bulge: " + reason + noneed, Util.TERM_CYAN));
        }
        if (PlusOneForAdd != 0) {
            PlusOneForAdd = 0;
            model.clearGUIInfo();
            model.refresh();
            return true;
        } else
            return false;
    }
    
    public void addNewRow()
    {
        // //mdTable.getModel().setValueAt(vme,mdTable.getRowCount()+1,0);
        // // wrong order?? not getting intial field in multiples and current could be
        // // non empty when currentMultiples is also..
        // /*if(current !=null) {    
        //   MetadataEditor.this.current.getMetadataList().getMetadata().add(vme);
        // } else if(currentMultiples != null) {
        //   currentMultiples.getMetadataList().getMetadata().add(vme);   
        //   if(DEBUG_LOCAL) {
        //       System.out.println("ME: current multiples is not null -- addNewRow " +
        //               currentMultiples.getMetadataList().getCategoryListSize() );
        //   } }*/
        
        if (currentMultiples != null) {
            //currentMultiples.getMetadataList().getMetadata().add(vme);
            // not so much a problem to do this as we don't "publish" this data
            // to the children until we detect an actual change.  Come to think of
            // it, if we just did this ALL as multiples, w/out current, that'd be simpler...
            if (DEBUG.PAIN) Log.debug("addNewRow: adding to multiples group");
            currentMultiples.getMetadataList().getMetadata().add(emptyVME()); 
            if(DEBUG_LOCAL) { System.out.println("ME: current multiples is not null -- addNewRow " + currentMultiples.getMetadataList().getCategoryListSize() ); }
            model.refresh();
        }
        else if (/*AUTO_ADD_EMPTY &&*/ current != null) {
            ensureModelBulge("addNewRow");
            // // If we skip this, we get infinite loop when selecting a node!
            // MetadataEditor.this.current.getMetadataList().getMetadata().add(EmptyVME);
            // // MetadataEditor.this.current.getMetadataList().getMetadata().add(vme);
        }
    }

    public void listChanged()
    {
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

    public boolean getEventIsOverAddLocation(MouseEvent evt)
    {
       boolean locationIsOver =
           evt.getX() > getCustomCategoryAddLocation(CC_ADD_LEFT) && 
           evt.getX() < getCustomCategoryAddLocation(CC_ADD_RIGHT);
       
       int row = mdTable.rowAtPoint(evt.getPoint());//evt.getY()/mdTable.getRowHeight();
                           
       boolean categoryIsNotInList = !model.wasCategoryFound(row);
                               
       return locationIsOver && categoryIsNotInList;
    }
    
    /**
     *
     *  returns -1 if add widget is not available (i.e. if in basic
     *  mode and/or not in "assign categories" mode)
     *
     **/
    public int getCustomCategoryAddLocation(int ccLocationConstant)
    {
        if(mdTable.getModel().getColumnCount() == 2)
        {
            return -1;
        }
        
        if(ccLocationConstant == CC_ADD_LEFT)
        {
            return mdTable.getColumnModel().getColumn(0).getWidth() - 20;
        }
        else if(ccLocationConstant == CC_ADD_RIGHT)
        {
            return mdTable.getColumnModel().getColumn(0).getWidth();
        }
        else
        {
            return -1;
        }
    }
    
    public void adjustColumnModel()
    {
        int editorWidth = MetadataEditor.this.getWidth();
        //if(MetadataEditor.this.getTopLevelAncestor() != null)
        //  editorWidth = MetadataEditor.this.getTopLevelAncestor().getWidth();
        
        if (mdTable == null)
            return;
        
        if (mdTable.getModel().getColumnCount() == 2) {
            mdTable.getColumnModel().getColumn(0).setHeaderRenderer(new MetadataTableHeaderRenderer());
            mdTable.getColumnModel().getColumn(1).setHeaderRenderer(new MetadataTableHeaderRenderer());
            mdTable.getColumnModel().getColumn(0).setWidth(editorWidth-BUTTON_COL_WIDTH);
            mdTable.getColumnModel().getColumn(1).setMaxWidth(BUTTON_COL_WIDTH);   
            mdTable.getColumnModel().getColumn(1).setMinWidth(BUTTON_COL_WIDTH);   
        } else {
            mdTable.getColumnModel().getColumn(0).setHeaderRenderer(new MetadataTableHeaderRenderer());
            mdTable.getColumnModel().getColumn(1).setHeaderRenderer(new MetadataTableHeaderRenderer());
            mdTable.getColumnModel().getColumn(2).setHeaderRenderer(new MetadataTableHeaderRenderer());
            mdTable.getColumnModel().getColumn(0).setWidth(editorWidth/2-BUTTON_COL_WIDTH/2);
            mdTable.getColumnModel().getColumn(1).setWidth(editorWidth/2-BUTTON_COL_WIDTH/2);
            mdTable.getColumnModel().getColumn(2).setMaxWidth(BUTTON_COL_WIDTH); 
            mdTable.getColumnModel().getColumn(2).setMinWidth(BUTTON_COL_WIDTH); 
        }
        //place holder for boolean to only create header renderers once
        //renderersCreated = true;
    }
    
    public void repaint() {
        super.repaint();
        // adjustColumnModel();
    }

    private String[] getObject(VueMetadataElement vme) {
        if (vme == null) {
            if (true||DEBUG.Enabled) Log.info("getObject GIVEN NULL VME, RETURNING DEFAULT_VME_OBJECT " + Util.tags(DefaultVMEObject));
            return DefaultVMEObject;
        } else
            return (String[]) vme.getObject();
        //return vme == null ? DefaultVMEObject : (String[]) vme.getObject();
    }
    
    private static final String[] NullPair = { null, null };
    private static final String[] EmptyPair = { DefaultVMEObject[0], null };

    private String[] getObjectCurrent(int row) {
        final int size = current.getMetadataList().getCategoryListSize();
        if (row >= size) {
            if (PlusOneForAdd == 1 && row == size) {
                return DefaultVMEObject;
            } else {
                Log.warn("getObjectCurrent for row " + row + " returns NullPair");
                return NullPair; // if we return EmptyPair, we'll get auto-add empty none keys again
            }
        } else
            return getObject(current.getMetadataList().getCategoryList().get(row));
    }
    
    private String[] getObjectMulti(int row) {
        return getObject(currentMultiples.getMetadataList().getCategoryList().get(row));
    }

    private String[] getObjectActive(int row) {
        if (current != null)
            return getObjectCurrent(row);
        else if (currentMultiples != null)
            return getObjectMulti(row);
        else {
            Log.warn("no active object at row " + row);
            return NullPair;
        }
    }
    
    /** @return the category key at the given row */
    String getKeyForRow(int row) {
        return getObjectActive(row)[0];
    }
    String getValueForRow(int row) {
        return getObjectActive(row)[1];
    }
    
    public void selectionChanged(final LWSelection s)
    {
        if (DEBUG.PAIN) Log.debug("selectionChanged: " + s);
        
        if (s.size() > 1) {
            // current = null;
            // changing current is handled in activeChanged: is messy: this non-nulll v.s. multiples test order probably matters in places!
            loadMultiSelection(s);
        } else {
            currentMultiples = null;
            autoTagButton.setVisible(s.size() == 1);
        }
    }
    
    public void activeChanged(ActiveEvent e)
    {
        if (DEBUG.PAIN) Log.debug("activeChanged: " + e);
        if (e != null) {
            focusToggle = false;  
            LWComponent active = (LWComponent) e.active;
            if (active instanceof tufts.vue.LWSlide || active instanceof tufts.vue.LWMap)
                active = null;
         
            this.mdTable.removeEditor();
            this.previousCurrent = current;
            this.current = active;
         
            // // This is one thing that will try and stick a damn empty element in every node...
            // if (AUTO_ADD_EMPTY && current != null && MetadataEditor.this.current.getMetadataList().getCategoryListSize() == 0)
            //     MetadataEditor.this.current.getMetadataList().getMetadata().add(VueMetadataElement.getNewCategoryElement());
            if (current != null && current.getMetadataList().getCategoryListSize() == 0)
                ensureModelBulge("selected-has-no-meta");
            else
                clearModelBulge("selected: reset for new md");
            model.refresh(); // note: bulge change may have just done this
        }
    }

    private void loadMultiSelection(final LWSelection selection) {
                
        //------------------
        focusToggle = false;  
        mdTable.removeEditor();
        //((MetadataTableModel)mdTable.getModel()).clearGUIInfo();
        previousMultiples = currentMultiples;
        //--------
                
        currentMultiples = LWGroup.createTemporary(selection);
              
        final java.util.List<VueMetadataElement> shared = new java.util.ArrayList<VueMetadataElement>();
        final java.util.Iterator<LWComponent> children = currentMultiples.getAllDescendents().iterator();
        final LWComponent firstComp = children.next();
              
        shared.addAll(firstComp.getMetadataList().getCategoryList().getList());
              
        if (DEBUG_LOCAL) Log.debug("shared initialized -- " + firstComp.getMetadataList().getMetadataAsHTML(VueMetadataElement.CATEGORY));
              
        while (children.hasNext()) {
                final LWComponent comp = children.next();
                  
                shared.retainAll(comp.getMetadataList().getCategoryList().getList());
                  
                // create set from shared
                //  use set iterator -- use element,count(frequency) in shared,count in comp
                // then remove lastIndexOf from shared as manhy times
                // as needed for each element
                java.util.Set uniqueShares = new java.util.HashSet(shared);
                java.util.Iterator<VueMetadataElement> us = uniqueShares.iterator();
                while(us.hasNext()) {
                        VueMetadataElement next = us.next();
                        int sharedCount = java.util.Collections.frequency(shared,next);
                        int compCount = java.util.Collections.frequency(comp.getMetadataList().getCategoryList().getList(),next);
                        int numberToRemove = sharedCount - compCount;
                      
                        if(DEBUG_LOCAL) {
                            System.out.println("ME -- for comp - " + comp.getID());
                            System.out.println("ME -- comp count vs shared count " + compCount + "-" + sharedCount);
                            System.out.println("ME - number of " + getObject(next) + " to remove from shared: " + numberToRemove);
                        }
                      
                        if(sharedCount > compCount) {    
                            for(int i=0;i<numberToRemove;i++) {
                                if(shared.indexOf(next) != -1)
                                    shared.remove(shared.indexOf(next));
                            }
                        }
                    }
                
            }
              
        final java.util.List<VueMetadataElement> cmList = currentMultiples.getMetadataList().getMetadata();
              
        if(DEBUG_LOCAL) Log.debug("after shared is constructed: " + currentMultiples.getMetadataList().getMetadataAsHTML(VueMetadataElement.CATEGORY));

        for(int i=0;i<shared.size();i++)
            cmList.add(shared.get(i));
              
        //((MetadataTableModel)mdTable.getModel()).refresh();
              
        if(DEBUG_LOCAL) Log.debug("shared calculated -- what is its size? : " + shared.size());
              
        //maybe add an empty item to the group instead.
        // it would never have to get added unless changed
        // actually only do this if the intersection of lists calculated above
        // is empty..
        if (shared.size() == 0) // tried cmList...
            addNewRow();
              
        //why is this needed? --**--
        //for (int i=0;i<model.getRowCount();i++) { model.setSaved(i,true); }    
              
        //this should do the same thing.. (its what activeChanged does)
        model.clearGUIInfo();
        model.refresh();
              
        // maybe also need to null out current in active changed (depends on order)
        // better to allow current to be non null..
        //current = null;
    }
    
    
    private final tufts.vue.gui.VueButton headerAddButton = new tufts.vue.gui.VueButton("keywords.button.add"); 
    private final JPanel headerAddButtonPanel = new JPanel();
    private final JLabel LabelKeyword = new JLabel(VueResources.local("jlabel.keyword")); 
    private final JLabel LabelCategory = new JLabel(VueResources.local("jlabel.category"));
    private final JLabel LabelEmpty = new JLabel("");
    private final Border HeaderCellBorder = GUI.makeSpace(0, CELL_HORZ_INSET+2, 0, 0);
        
    private class MetadataTableHeaderRenderer extends DefaultTableCellRenderer {  
        // see below - getter could be supplied in stand alone class
        //static tufts.vue.gui.VueButton button = new tufts.vue.gui.VueButton("keywords.button.add");

        public MetadataTableHeaderRenderer() {
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
            LabelCategory.setFont(GUI.LabelFace);
        }
       // can't do this statically in inner class but could be done from wholly separate class -
       // for now move the button out into the metadata editor
       /*static tufts.vue.gui.VueButton getButton() { return button; }*/
        @Override public java.awt.Component getTableCellRendererComponent(JTable table, Object value,boolean isSelected,boolean hasFocus,int row,int col) {
            JComponent comp = new JPanel();
            if(col == 0) {
                if(model.getColumnCount() == 2) {    
                    // back to "Keywords:" -- VUE-953  
                    comp = LabelKeyword;
                } else {
                    // back to "Categories" -- VUE-953
                    //comp = new JLabel("Fields:");
                    comp = LabelCategory;
                }
            }
            else if(col == 1 && col != buttonColumn) {
                if (model.getColumnCount() == 3)
                    comp = LabelKeyword;
            }
            else if(col == buttonColumn) {
                //comp = new JLabel();    
                //((JLabel)comp).setIcon(tufts.vue.VueResources.getImageIcon("metadata.editor.add.up"));
                //comp = new JPanel();
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

    private static boolean hasKnownCategory(final String keyName) {
        if (keyName == null || keyName.indexOf('#') < 0)
            return false;
        // todo: crazy that CategoryModel doesn't have a hash lookup for this.
        // We could do it here but there are no events to listen for updates.
        for (edu.tufts.vue.ontology.Ontology ont : OntologiesList) {
            for (OntType ontType : ont.getOntTypes())
                if (ontType.matchesKey(keyName))
                    return true;
        }
        return false;
    }
        
    /** @return true if we can find a proper RDF style category for the given keyName in the JComboBox model -- will also select that item in the JComboBox */
    public static boolean selectKnownCategory(final String keyName, final JComboBox catCombo) {
        ignoreCategorySelectEvents = true;
        try {
            return doSelectKnownCategory(keyName, catCombo);
        } catch (Throwable t) {
            Log.error("selectKnownCategory " + keyName + " " + catCombo, t);
            return false;
        } finally {
            ignoreCategorySelectEvents = false;;
        }
    }
        
    private static boolean doSelectKnownCategory(final String keyName, final JComboBox catCombo)
    {
        final int msize = catCombo.getModel().getSize();
        final javax.swing.ListModel catComboModel = catCombo.getModel();
        
        if (DEBUG.PAIN) Log.debug("selectKnownCategory: lookup with: " + Util.tags(keyName));

        if (keyName == null || keyName.indexOf('#') < 0) {
            catCombo.setSelectedIndex(1);
            return false;
        }
        
        //if(DEBUG_LOCAL)  System.out.println("MetadataEditor findCategory - " + currValue);
        //Object currValue = table.getModel().getValueAt(row,col); //.toString();
        //System.out.println("Editor -- currValue: " + currValue);
        
        for (int i = 0; i < msize; i++) {
            final Object item = catComboModel.getElementAt(i);
            if (item instanceof OntType) {
                final OntType ontType = (OntType) item;
                if (DEBUG.PAIN) Log.debug("scan " + Util.tags(ontType));
                if (ontType.matchesKey(keyName)) {
                    if (DEBUG.PAIN) Log.debug("findCategory: at index " + i + " hit " + Util.tags(item));
                    catCombo.setSelectedIndex(i);
                    return true;
                    // SMF: changed to break-loop-on-match, which changes this from select last to select first,
                    // tho this is a JComboBox, and no items should be the same.
                }
            }
        }
        catCombo.setSelectedIndex(1);
        return false;
    }
    
        
    private class MDCellRenderer extends DefaultTableCellRenderer
    {   
        //private final JComboBox catCombo = new JComboBox();
        private final JButton deleteButton = new edu.tufts.vue.metadata.gui.MetaButton(MetadataEditor.this, "delete");
        private final JPanel holder = new JPanel();
        private final JPanel box = new JPanel();
        //private final JLabel addLabel = new JLabel("[+]");
        //private final JLabel addLabel = new JLabel("<html>&oplus;");
        private final JLabel addLabel = new JLabel("+");
        private final JLabel faintKeyLabel = new JLabel();

        private final JTextField renderEditable = new JTextField();

        MDCellRenderer() {
            setFont(GUI.LabelFace);
            renderEditable.setFont(GUI.LabelFace);
            renderEditable.setBorder(null);
            // catCombo.setModel(new CategoryComboBoxModel());
            // catCombo.setRenderer(new CategoryComboBoxRenderer());
            // catCombo.setFont(GUI.LabelFace);
            box.setLayout(new BorderLayout());
            box.setName("MD:cellRenderer");
            faintKeyLabel.setForeground(Color.lightGray);
            faintKeyLabel.setFont(GUI.LabelFaceItalic);
            if (DEBUG.BOXES) {
                box.setOpaque(true);
                box.setBackground(Color.yellow);
            } else
                box.setOpaque(false);
            
            addLabel.setForeground(Color.gray);
            //addLabel.setFont(GUI.StatusFace);
            //addLabel.setFont(tufts.vue.VueConstants.LargeFont);
            addLabel.setFont(tufts.vue.VueConstants.LargeFixedFontBold);
        }

        public java.awt.Component getTableCellRendererComponent(final JTable t, final Object _vme, boolean selected, boolean focus, final int row, final int col)
        {
            final boolean savedRow = model.getIsSaved(row); // complex: default impl will report false for any null or empty 0 length string value
            final boolean isPlusOneEditRow = (PlusOneForAdd == 1 && row == model.getRowCount() - 1);
           
            if (DEBUG.PAIN) Log.debug("gTCRC: bc" + buttonColumn + (row<10?"  ":" ") + row + "," + col + (savedRow?"      ":" edit ") + _vme);

            box.removeAll(); // this box is used as a safe generic objext we know we can stick a border on

            if (TC_COMBO == col) {
                final String key = getKeyForRow(row);
                final boolean foundCat = hasKnownCategory(key);
                if (foundCat || !savedRow) {
                    model.reportCategoryFound(row, true); // ick
                    setText(shortKey(key));
                    box.add(this);
                    // if (!savedRow) {
                    //     box.add(catCombo);
                    //     if (DEBUG.Enabled) Log.debug("RENDERER IS DRAWING AN EDITOR (CAT-COMBO) row " + row);
                    // } else {
                    //     setText(shortKey(key));
                    //     box.add(this);
                    // }
                } else {
                    model.reportCategoryFound(row, false); // ick
                    setText(shortKey(key));
                    box.add(this);
                    box.add(addLabel, BorderLayout.EAST);
                }
            }
            else if (TC_TEXT == col && (isPlusOneEditRow || _vme != null)) { // if value is null, we'll render empty box, even if PlusOneEditRow
                final VueMetadataElement vme = (VueMetadataElement) _vme;
                if (vme != null && vme.hasValue()) {
                    setText(vme.getValue());
                    box.add(this);
                } else if (isPlusOneEditRow) {
                    // we're rendering the last row with an edit bulge: make it look inviting to activate an edit:
                    //return installRenderBorder(renderEditable, row, col);
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
                
                // if (savedRow) {
                //     setText(vme.getValue());
                //     box.add(this);
                // } else {
                //     // TODO: happens if value looks bad (empty) as well: if no keys showing, and value is empty, render
                //     // as a faded out key name Note: this should only ever happen to render an empty, inviting edit box
                //     // for selections that have no meta-data.  The real input field comes from MDCellEditor.
                //     renderEditable.setText(vme.getValue());
                //     box.add(renderEditable);
                // }
            }
            else if (TC_BUTTON == col && (isPlusOneEditRow || _vme != null))
                box.add(deleteButton);
            // else Log.error("gTCRC: rendering unknown column " + col + " " + value);

            return installRenderBorder(box, row, col);
        } 
    }
    
    /**/ public final class ComboCE  extends MDCellEditor { ComboCE()  { super(0); } }
    /**/ public final class TextCE   extends MDCellEditor { TextCE()   { super(1); } }
    /**/ public final class ButtonCE extends MDCellEditor { ButtonCE() { super(2); } }

    private static boolean ignoreCategorySelectEvents = false;
    
    // main entry point for editor interaction
    /** this was made as a global editor for all columns, which besides bad design, means that it makes it very
        hard (impossible?) for one cell to stop editing in a row, and another to begin. todo: move functionality
    into the 3 different cell editor classes */
    private class MDCellEditor extends DefaultCellEditor
    {
        private final JTextField field; 
        private final JPanel box = new JPanel();
        private final JComboBox catCombo = new JComboBox();
        
        private int activeEditRow;
        private int activeEditCol;
        private JTable table; // should only ever be one of these, yes?
        
        private final edu.tufts.vue.metadata.gui.MetaButtonPanel metaButtonBox;

        private final int INSTANCE_COLUMN; // hard coded column we're meant to be rendering for

        final String name;

        public String toString() { return ""+INSTANCE_COLUMN; }

        protected MDCellEditor(final int i) {
            super(new JTextField());
            INSTANCE_COLUMN = i;

            name = getClass().getSimpleName() + "/" + i;
            
            field = (JTextField) getComponent();
            field.setName("MD:cellEditor-" + name);
            field.setFont(GUI.LabelFace);
            field.addFocusListener(new FocusAdapter() {
                    public void focusLost(FocusEvent fe) {
                        if (DEBUG.PAIN) VUE.diagPush("FFL" + i);
                        handleFieldFocusLost(fe);
                        if (DEBUG.PAIN) VUE.diagPop();
                    }
                    public void focusGained(FocusEvent fe) { focusToggle = true; }
                });
            
            box.setName("MD:cellEditor:box-" + i);
            box.setOpaque(false);
            box.setLayout(new BorderLayout());
            box.addMouseListener(new tufts.vue.MouseAdapter("MDCE:JPanel-container-"+i) { public void mousePressed(MouseEvent e) {
                model.setSaved(activeEditRow, true);
                stopCellEditing();
            }});
           
            metaButtonBox = new edu.tufts.vue.metadata.gui.MetaButtonPanel(MetadataEditor.this, "delete");

            if (INSTANCE_COLUMN == 0)
                initCatCombo();

            // //tempPanel = new JPanel();//new ActionPanel(MetadataEditor.this,row);
            // //tempPanel.setOpaque(true);
            // //tempPanel.setBackground(java.awt.Color.BLUE);
            // deleteButton = new edu.tufts.vue.metadata.gui.MetaButton(MetadataEditor.this,"delete");
            // //tempPanel.add(deleteButton);
            // // ***needs mouseListener that saves data unless point is over Button..
            // //.addActionListener(deleteButton)

        }

        private void initCatCombo() {
            catCombo.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
          //catCombo.putClientProperty("JComboBox.isSquare", Boolean.TRUE); // won't be shown or no effect here
            catCombo.setModel(GlobalCategoryComboBoxModel);
            catCombo.setRenderer(new CategoryComboBoxRenderer());
            catCombo.setFont(GUI.LabelFace);
            //catCombo.setMaximumSize(new java.awt.Dimension(300, 20));

            //if (INSTANCE_COLUMN == 0) box.add(catCombo);

            catCombo.addFocusListener(new FocusAdapter() { public void focusLost(final FocusEvent fe) {
                if (model.lastSavedRow != activeEditRow) // worried somehow row could get out of sync...
                    model.setSaved(activeEditRow, true);
            }});

            // note: if this had been an action listener insted of an item listener, we may not have had to deal with the event disabling code
            catCombo.addItemListener(new ItemListener() { public void itemStateChanged(final ItemEvent ie) {
                // Note: this is being fired at from the INTERNAL select down in selectKnownCategory, and always has been!
                // No wonder the combo-box has been so wiggy.  We have to handle event ignoring for this to work.
                if (ignoreCategorySelectEvents || ie.getStateChange() != ItemEvent.SELECTED)
                    return;
                if (ignoreCategorySelectEvents)
                    return;
                
                if (DEBUG.PAIN) Log.debug("MDCE: catCombo itemStateChanged " + Util.tags(ie.getItem()));
                
                if (ie.getItem() instanceof edu.tufts.vue.metadata.gui.EditCategoryItem) {
                    if (mdTable.getCellEditor() == null) {
                        // We shouldn't see this now that events are disabled when editCategory refreshes the model.
                        if (DEBUG.Enabled) Log.debug("MDCE: ignoring itemStateChanged: no active editor");
                    } else
                        popEditCategoryDialog();
                } else {
                    catComboHasMadeSelection(ie);
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
            ecd.add(new CategoryEditor(ecd,catCombo,MetadataEditor.this,current,activeEditRow,activeEditCol));
            ecd.setBounds(475,300,300,250);
            ecd.setVisible(true); // I presume we block here?
            Log.info("back from CategoryEditor dialog");
            mdTable.getCellEditor().stopCellEditing();
            refreshAll();
        }

        private void catComboHasMadeSelection(final ItemEvent ie)
        {
            final Object selected = catCombo.getSelectedItem();

            if (selected instanceof OntType == false) {
                // assume coming from selectKnownCategory, having picked a divider or something
                return;
            }
                
            final OntType ontType = (OntType) catCombo.getSelectedItem();
            final int row = MDCellEditor.this.activeEditRow;
            final VueMetadataElement tableVME = model.getValueAt(row, -1); // column ignored
                       
            if (DEBUG.PAIN || tableVME == null) Log.debug("MDCE: VME for last request row " + row + ": " + tableVME);

            if (tableVME == null) {
                if (DEBUG.Enabled) Log.debug("aborting catCombo state change on null VME: probably model refresh");
                return;
            }
                
            final String[] keyValuePair = {
                ontType.getAsKey(),
                tableVME.getValue()
            };

            if (DEBUG.PAIN) Log.debug("MDCE: constructed key: [" + keyValuePair[0] + "]");

            final VueMetadataElement vme = new VueMetadataElement();
            vme.setObject(keyValuePair);
            vme.setType(VueMetadataElement.CATEGORY);
               
            if (currentMultiples != null) {
                // also need to add/set for individuals in group.. todo: subclass LWGroup to do this?
                // in the meantime just set these by hand
                if (currentMultiples.getMetadataList().getCategoryListSize() > row) {
                    VueMetadataElement oldVME = currentMultiples.getMetadataList().getCategoryList().get(row);  
                          
                    currentMultiples.getMetadataList().getCategoryList().set(row,vme);
                        
                    java.util.Iterator<LWComponent> multiples = currentMultiples.getAllDescendents().iterator();
                    while(multiples.hasNext()) {
                        LWComponent multiple = multiples.next();
                        MetadataList.SubsetList md = multiple.getMetadataList().getCategoryList();
                        if (md.contains(oldVME))
                            md.set(md.indexOf(oldVME),vme);
                        else
                            multiple.getMetadataList().getMetadata().add(vme);
                    }
                }
                else {
                    currentMultiples.getMetadataList().getMetadata().add(vme); 
                        
                    java.util.Iterator<LWComponent> multiples = currentMultiples.getAllDescendents().iterator();
                    while(multiples.hasNext()) {
                        LWComponent multiple = multiples.next();
                        MetadataList.SubsetList md = multiple.getMetadataList().getCategoryList();
                        if (md.contains(vme))
                            md.set(md.indexOf(vme),vme);
                        else
                            multiple.getMetadataList().getMetadata().add(vme);
                    }
                }  
            }
            else if (current != null) {    
                if (current.getMetadataList().getCategoryListSize() > row)
                    current.getMetadataList().getCategoryList().set(row,vme);
                else
                    current.getMetadataList().getMetadata().add(vme); 
            }

            if (mdTable.getCellEditor() != null)
                mdTable.getCellEditor().stopCellEditing();
        }

        /* @return the row from the last requested cell editor */
        //public int getRow() { return rowRequest; }
        
        public void focusField() { field.requestFocus(); }

        private boolean editWasCanceled = false;
       
        @Override public boolean stopCellEditing() { return stopEditing(false); }
        @Override public void cancelCellEditing() { stopEditing(true); }
        
       private boolean stopEditing(boolean cancel) {
            if (DEBUG.PAIN) { Log.debug(name + " stop-cell-editing: cancel=" + cancel); if (true) Util.printClassTrace("!java"); }
            previousCurrent = current;
            previousMultiples = currentMultiples;
            // If this was a NEW input, better to process any input and add it, consuming the
            // bulge that way.  Maybe we don't need this at all??
            // clearModelBulge("stopCellEditing");
            editWasCanceled = cancel;
            if (cancel) {
                super.cancelCellEditing();
                return true; // ignored
            } else
                return super.stopCellEditing();
       }
        
       @Override public java.awt.Component getTableCellEditorComponent(final JTable table, final Object value, boolean isSelected, final int row, final int col)
       {
           if (DEBUG.PAIN) Log.debug(name + " getTableCellEditorComponent ic" + INSTANCE_COLUMN + ": " + row + "," + col + " " + value);
           this.activeEditRow = row;
           this.activeEditCol = col;
           this.table = table;
           
           if (col == TC_COMBO) {
               selectKnownCategory(getKeyForRow(row), catCombo);
               return installRenderBorder(catCombo, row, col);
               // model.refresh(); // Why was this being done here?
               // mdTable.repaint();
           }
           else if (col == TC_TEXT) {
               //---------------------------------------------------------------------------------------------------
               // ***** THIS IS WHERE THE METADATA VALUE IS EXTRACTED FROM THE MODEL AND PUSHED TO THE UI *****
               //---------------------------------------------------------------------------------------------------
               if (value instanceof String) {
                   // I don't think this ever happens
                   field.setText(value.toString());
                   if (DEBUG.Enabled) Log.debug(name + " pushing to UI from String " + Util.tags(value));
               } else if (value instanceof VueMetadataElement) {
                   final VueMetadataElement vme = (VueMetadataElement) value;
                   if (DEBUG.PAIN) Log.debug(name + " pushing to UI from " + vme + (vme.getValue() == EmptyValue ? " <TheEmptyValue>" : ""));
                   if (vme == InputVME) {
                       if (DEBUG.PAIN) Log.debug(name + " re-using any old input text on InputVME: " + Util.tags(field.getText()));
                       ; // leave old text input as a handy copy: it will automatically all be selected for easy replacement
                   } else {
                       field.setText(vme.getValue());
                   }
               } // else re-use old field text value
               // Needless enclosure in JPanel hack ignored.  This fixes the long-standing bug of two-clicks
               // required to get focus, and the border painting is no longer messed up as well.  Note that
               // those crazy above/below borders might need adjusting now -- the above component seems to
               // shift up a pixel or two when this goes active.
               field.selectAll(); // Only works for newly added when put here, which is actually good (don't ask why)
               GUI.invokeAfterAWT(new Runnable() { public void run() { // Is there a better place to do this?
                   field.requestFocus();
               }}); 
               MetadataEditor.this.activeTextEdit = this;
               // field.setBorder(null); // remove system focus border and cause field to fill the entire cell
               return field;
           }
           else if (col == TC_BUTTON) {
               // The way we currently detect a click on one of these buttons is to actually return it when an editor is
               // requested, which is what the table will do if there's a click over the region.  This is a pain in that
               // we must return a button that looks exactly like the render button, including border, or the UI will
               // refelect the irrelevant editor-active state for this cell.  We also need, of course, to report to the
               // button what row it's in when fetched so it knows what's being clicked on.  The enclosing
               // MetaButtonPanel seems to exist entirely to detect the mouse-press in addition to the button, and
               // cancel the irrelevant button editing state, which creates further confusion, as it will actually
               // cancel editing for the entire row (yes?)  FURTHER BUG: a click on the button has meant only the button
               // gets the event, but a click NEAR the button has meant only the stop-cell-editing effect!  Further
               // complication: MetaButton was ignoring the row report anyway, and relying on the selected row, which is
               // the only reason we need row selection enabled.  Moved back to ignoring selection and using rendered
               // row report.

               metaButtonBox.setPanelRowForButtonClick(row);
               if (DEBUG.Enabled) metaButtonBox.setBackground(Color.yellow);
               return installRenderBorder(metaButtonBox, row, col);
           }
           
           // if (LIMITED_FOCUS) field.addMouseListener(new MouseAdapter() { public void mouseExited(MouseEvent me) { stopCellEditing(); }});
           
           // so this method had always returned a panel containing what was really needed -- that
           // was causing the horrible two-double-clicks to focus problem for the value JTextField.
           return box;
       }
       
    private void handleFieldFocusLost(final FocusEvent fe)
    {
        if (DEBUG.PAIN) Log.debug(name + " handleFieldFocusLost, wasCancel=" + editWasCanceled);
        MetadataEditor.this.activeTextEdit = null;
        
        if (current == null && currentMultiples == null) {
            //|| VUE.getActiveMap() == null || VUE.getActiveViewer() == null)
            return;
        }

        final int row = MDCellEditor.this.activeEditRow;
        if (DEBUG.PAIN) Log.debug(name + " rowRequest = " + row + ", lastSavedRow = " + model.lastSavedRow);
        
        if (true || model.lastSavedRow != row) { // always doing may prevent initial editor from placing properly / getting focus?  No, not helping.... was that original intention?
            // On VK_ENTER, was always calling this no matter what, so assuming should be safe
            // to do so as well here: don't know what case this was trying to cover, but
            // it's preventing the GUI from resetting itself.
            model.setSaved(row, true);
        }
        
        if (editWasCanceled) {
            clearModelBulge("canceled");
            return;
        }
                
        final TableCellEditor editor = mdTable.getCellEditor();
        if (DEBUG.PAIN) {
            Log.debug(name + " getCellEd: " + Util.tags(editor) + (editor == null ? " (no longer editing)" : ""));
            if (editor != null && editor != MDCellEditor.this) {
                Log.debug(name + " is not us: " + Util.tags(MDCellEditor.this));
                if (editor instanceof MDCellEditor)
                    Log.debug(name + " remote row: " + ((MDCellEditor)editor).activeEditRow);
            }
        }
                  
        if (DEBUG_LOCAL && fe != null) Log.debug(name + "opposite: " + fe.getOppositeComponent());
                  
        /*if(currentMultiples != null) { return; }
          if(fe!= null && fe.getOppositeComponent() == catCombo) { return; }*/
                  
        // // note: fix for currentMultiples != null and focus lost off bottom of info window (not
        // // as likely in the multiple selection case?)
        // if (tufts.vue.gui.DockWindow.isDockWindow(fe.getOppositeComponent()) && currentMultiples == null) {
        //     model.setSaved(row,true);
        //     if (editor != null)
        //         editor.stopCellEditing();
        // }

        if (editor != null)
            editor.stopCellEditing();
                  
        if (previousCurrent == null && current == null && previousMultiples == null && currentMultiples ==  null)
            return;

        // java.util.List<VueMetadataElement> metadata
        final MetadataList.SubsetList metadata;
        
        if (previousCurrent != null && !focusToggle) {
            // what is this for? Looks like it has to do with attempting to be sure to re-use some old
            // state after a focus loss then focus regain.  Shouldn't be needing this hack...
            metadata = previousCurrent.getMetadataList().getCategoryList();
            if (DEBUG.Enabled) Log.warn("USING PREVIOUS CURRENT " + previousCurrent, new Throwable("HERE"));
        } else {
            if (currentMultiples != null)
                metadata = currentMultiples.getMetadataList().getCategoryList();
            else if (current != null)
                metadata = current.getMetadataList().getCategoryList();
            else {
                if (DEBUG.Enabled) Log.warn("metadata is null");
                metadata = null;
            }
        }
                  
        final VueMetadataElement real_VME;
        if (metadata != null && row < metadata.size())
            real_VME = metadata.get(row);
        else
            real_VME = null;
        // if (current != null && current.getMetadataList().getCategoryListSize() > 0)
        //     real_VME = metadata.get(row);
        // else if (currentMultiples != null && currentMultiples.getMetadataList().getCategoryListSize() > 0)
        //     real_VME = metadata.get(row);
        // else
        //     real_VME = null;

        final String fieldText = field.getText();
        final String trimText = fieldText.trim();

        if (DEBUG.PAIN) {
            Log.debug("real_VME: " + real_VME);
            Log.debug("fieldText: " + Util.tags(fieldText));
        }

        //----------------------------------------------------------------------------------------
        // Check to see if a meaninful change is ready to be applied (if not, early return)
        //----------------------------------------------------------------------------------------
        
        if (real_VME == null) {
            if (trimText.length() == 0) {
                // if (DEBUG.PAIN) Log.debug("do nothing, no new text");
                clearModelBulge("nothing to do, no new text");
                field.setText(""); // don't allow re-use
                // ? model.refresh();
                // mdTable.repaint();
                return;
            }
            // new to the model
            if (DEBUG.PAIN) Log.debug("new md item for model");
        } else {
            if (DEBUG.PAIN) Log.debug("existing item being edited: " + real_VME);
            // we have something to replace, tho it may be a fresh empty vme
            if (DEBUG.Enabled && isInputVME(real_VME)) Log.info("SEEING INPUT VME IN MODEL", new Throwable("HERE"));
          //if (trimText.length() == 0 && isEmptyVME(real_VME)) {
            if (trimText.equals(real_VME.getValue())) { // getValue can return null
                // The trim() will allow us to eliminate whitespace on a field value by editing, but not add it.
                //if (DEBUG.PAIN) Log.debug("do nothing, no significant change");
                clearModelBulge("nothing to do, no significant change");
                field.setText(""); // don't allow re-use
                // ? model.refresh();
                // mdTable.repaint();
                return;
            }
        }

        //----------------------------------------------------------------------------------------
        // Actually apply a change:
        //----------------------------------------------------------------------------------------

        final VueMetadataElement new_VME;
                  
        if (real_VME == null) {
            if (DEBUG.Enabled) Log.warn("no real_VME, and ignoring category setting for now: creating fresh");
            // TODO: this should pull the value of the category selected if there is one...
            new_VME = freshVME(trimText);
        } else {
            new_VME = new VueMetadataElement();
            final String categoryKey = getObject(real_VME)[0]; // copy over the old key
            new_VME.setObject(new String[] { categoryKey, trimText }); // old crazy API
            if (DEBUG.PAIN) Log.debug("new VME with copied key: " + new_VME);
        }
        
        if ( (current != null && row < (current.getMetadataList().getCategoryListSize())) ||
             (currentMultiples != null && currentMultiples.getMetadataList().getCategoryListSize() > row) ) {
            
            if (DEBUG_LOCAL) Log.debug("setting value from field, row=" + row + ", obj=" + Util.tags(new_VME.getObject()) + " " + metadata);
                    
            final VueMetadataElement old = metadata.get(row);
                    
            metadata.set(row, new_VME); // WE ALWAYS WRITE OVER THE OLD VME

            // If we're not handling multiples, we're already done
                    
            if (currentMultiples != null) {
                // SMF: assuming that even if only one item in group, is functionally the same as non-group case.
                // SMF: iterate getChildren: this will tag just the top-level actually selected items
                // SMF: iterate getAllDescendents: the above PLUS their children, grandchildren, etc.
                // SMF: Using getAllDescendents is the historical default, tho that means we can NEVER just group-tag a set of parents,
                // SMF: Note that the same almost certainly applies to everywhere in this file we see getAllDescendents called.
                // whereas if we only do the direct children, what's in the selection could still be expanded to include children,
                // tho we don't have an action for that -- the user would have to add the children manually.
                
              //for (LWComponent child : currentMultiples.getChildren()) { 
                for (LWComponent child : currentMultiples.getAllDescendents()) { 
                    final MetadataList.SubsetList cMeta = child.getMetadataList().getCategoryList();
                    final int size = cMeta.size();
                    if(size > 0 && cMeta.get(size-1).getValue().equals(""))
                        cMeta.set(size-1, new_VME);
                    // [DAN] also need to set in condition where already in all the sub components?
                    // somehow need to detect edit here.. but condition in line above this
                    // one is not neccesarily equivalent..
                    else {
                        if (cMeta.contains(old))
                            // [DAN] should it always be the first index?
                            cMeta.set(cMeta.indexOf(old), new_VME);
                        else
                            child.getMetadataList().getMetadata().add(new_VME);
                    }
                    child.layout();
                }
            }
        }
        else {
            if (DEBUG.PAIN) Log.debug("appending");
            metadata.add(new_VME);
            if (!clearModelBulge("success: filled bulge with new data"))
                model.refresh(); // should never happen, but just in case
        }
                  
        VUE.getActiveMap().markAsModified();
        
        if (current != null)
            current.layout();
        // todo: would be better to issue a model repaint event
        VUE.getActiveViewer().repaint();
        mdTable.repaint();
    }

        /** @inteface CellEditor */ @Override public boolean shouldSelectCell(EventObject o) { return false; }

        /** @inteface CellEditor: "If editing can be started this method returns true." */
        @Override public boolean isCellEditable(java.util.EventObject object) {
            if (DEBUG.PAIN) { VUE.diagPush("isCellEditable"); Log.debug(object /*,new Throwable("HERE")*/  ); }
            boolean canEdit = canEditWithSideEffects(object);
            if (DEBUG.PAIN) { Log.debug("canEdit = " + canEdit); VUE.diagPop(); }
            return canEdit;
       }
        
        /** holy christ -- this has major side effects -- very hairy to figure out -- apparenly being used to detect mouse click! */
        private boolean canEditWithSideEffects(java.util.EventObject object)
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
                // CLICK ON THE MINUS BUTTON:
                if (mdTable.getCellEditor() != null) {
                    mdTable.getCellEditor().cancelCellEditing();
                  //mdTable.getCellEditor().stopCellEditing();
                }
                // returning true will allow us to "start editing".  This way, the table will request edit "editor" for
                // this cell (the button), and once it has the editor, it will pass this mouse press on to the button.
                // If we return false, no button could get the event, and we'd have to catch this mouse event elsewhere.
                // Since the button is actually a single constant renderer style button, the design is to call
                // setRow on our meta button when it is requested, so it will know what row to fire for.
                return true; 
            }
            else if (GUI.isDoubleClick(me)) { // || col == buttonColumn )
                if (DEBUG.PAIN) Log.debug("double-click detect in isCellEditable, row " + row + ", col " + col);
                // What's this do???
                model.setSaved(row, false);
                int lsr = model.lastSavedRow;
                if(lsr != row)
                    model.setSaved(lsr,true);
                // model.refresh();
                // mdTable.repaint();
                return true;
            }
            else if (row != -1 && !model.getIsSaved(row)) {
                if (DEBUG.PAIN) Log.debug("any non-saved detect in isCellEditable, row " + row + ", col " + col);
                model.setSaved(row, false);
                return true;
            }
            return false;
        }
    }
    
    private JComponent installRenderBorder(JComponent c, int row, int col) {
        if (true) {
            c.setBorder(col == TC_BUTTON ? buttonBorder : insetBorder);
        } else {
            final Border b = getRenderBorder(row, col);
            c.setBorder(b);
        }
        return c;
    }
    
    
    /**
     * this was a hack to get borders without using a full JTable grid, but it's a giant messy pain to try and get
     * working just right, requires the further getIsSaved hack and all the places that state needs updating, and
     * there's no reason not to just use a the JTable grid.  If we want the grid off the buttons at the right, we can
     * easily merge the buttons into the the text cell (dropping column three entirely) with a BorderLayout.EAST position
     * and a bit of extra mouse code to check for clicks in the right of that cell. */
    private Border getRenderBorder(int row, int col)
    {
        //if (DEBUG.BOXES) return DebugBorder;
        
        if (col == TC_BUTTON)
            return insetBorder;
      
        final boolean saved = model.getIsSaved(row);

        // The idea was, I think, if not saved, it's in an editor, and we don't want a border.
        // Smarter now: if we're fetching an editor, we'll ask for border or not expliticly
        // depending on editor impl, so always put the border on, which will probably
        // fix a bunch of border missing bugs.
        //if (saved) {

        if (true) {
            // if (true) return BorderFactory.createCompoundBorder(topLeftBox,insetBorder);
            // if (true) return BorderFactory.createCompoundBorder(fullBox,insetBorder);
            
            boolean aboveSaved = false;
            boolean belowSaved = false;
          
            if (row > 0)
                aboveSaved = model.getIsSaved(row - 1);
            else //if (row == -1)
                aboveSaved = false;

            final int rowCount = model.getRowCount();
          
            if (row < rowCount) {
                belowSaved = model.getIsSaved(row + 1);
                //if (DEBUG.PAIN) Log.debug("last row, belowSaved = " + belowSaved);
            } else if (row >= rowCount - 1) {
                belowSaved = false;
                //if (DEBUG.PAIN) Log.debug("last row, belowSaved = " + belowSaved);
            }
            //if (DEBUG.PAIN) Log.debug("belowSaved = " + belowSaved);
          
            if (col == TC_TEXT) {    
                     if(!aboveSaved && !belowSaved) { return BorderFactory.createCompoundBorder(fullBox,insetBorderNoBottom); } // only seen at top
                else if( aboveSaved && !belowSaved) { return BorderFactory.createCompoundBorder(fullBox,insetBorderNoBottom); }
                else if(!aboveSaved &&  belowSaved) { return BorderFactory.createCompoundBorder(noBottomBox,insetBorder); }
                else if( aboveSaved &&  belowSaved) { return BorderFactory.createCompoundBorder(noBottomBox,insetBorder); }
            }
            else {
                     if(!aboveSaved && !belowSaved) { return BorderFactory.createCompoundBorder(topLeftBotBox,insetBorderNoBottom); }  // only seen at top
                else if( aboveSaved && !belowSaved) { return BorderFactory.createCompoundBorder(topLeftBotBox,insetBorderNoBottom); }
                else if(!aboveSaved &&  belowSaved) { return BorderFactory.createCompoundBorder(topLeftBox,insetBorder); }
                else if( aboveSaved &&  belowSaved) { return BorderFactory.createCompoundBorder(topLeftBox,insetBorder); }
            }    
        }
        
        return insetBorder;
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
        private int cols = initCols(2);
         
        private final java.util.List<Boolean> saved = new java.util.ArrayList<Boolean>();
        private final java.util.List<Boolean> categoryFound = new java.util.ArrayList<Boolean>(32);

        private int lastSavedRow = -1;

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
         
        @Override public void setValueAt(Object value,int row, int column) { /*fireTableDataChanged();*/ }

        public void clearGUIInfo() {
            if (DEBUG.PAIN) Log.debug("model: clearGUIInfo (reset all saved to true)");
            this.saved.clear();
            final int rows = getRowCount();
            for (int i = 0; i < rows; i++)
                saved.add(Boolean.TRUE);
            lastSavedRow = -1;
        }
         
        public boolean getIsSaved(final int row) {
            // if (true) return true; // doesn't make that much difference -- should be able to determine via other means
            try {  
                if (row < saved.size()) {
                    return saved.get(row); // ((VueMetadataElement)getValueAt(row,buttonColumn - 1)).hasValue();
                    // Why did we care if value len > 0?  Perhaps so no brand new and completely empty edit would
                    // look like it had been saved? (yes, i think so...)  The wierd problem here is that it causes null/empty values
                    // to trigger the editor renderer for these items!  Ah, this was basically designed to report empty
                    // values as well.
                } else {
                    if (DEBUG.Enabled) Log.debug("model: getIsSaved: only 0-" + (saved.size()-1) + " rows; row " + row + " defaulting false");
                }
                return false;
            } catch (Throwable t) {
                if (DEBUG.PAIN) Log.debug("model: getIsSaved row " + row + " of " + saved.size() + " is false: " + t);
                return false;
            }
        }
         
        public void setSaved(final int row, final boolean isSaved) {
            if (DEBUG.PAIN) {
                Log.debug("model: curSaved = " + saved);
                Log.debug("model: setSaved @ row " + row + " to " + isSaved + ", lastSavedRow = " + lastSavedRow);
            }
            if (row < 0) return;
            
            if (saved.size() <= row) {
                // ensure-capacity with natural default of true:
                for (int sz = saved.size(); sz <= row; sz++) {
                    if (DEBUG.PAIN) Log.debug("model: expanding capacity to " + (sz+1));
                    saved.add(Boolean.TRUE);
                }
            }
            saved.set(row, isSaved ? Boolean.TRUE : Boolean.FALSE);
            lastSavedRow = row;
        }
         
        private boolean wasCategoryFound(int row) { return (row >= 0 && row < categoryFound.size()) ? categoryFound.get(row) : false; }
        // CALLED DURING RENDERING OF THE COMBO-BOX COLUMN!  Apparently is only used in one place for knowing if to render a tooltip or not
        // Is overkill complexity: could be re-calling some version of find/selectCategory based on the key found at row.
        private void reportCategoryFound(int row, boolean found) {
            if (categoryFound.size() <= row) // expand valid porition of capacity
                for (int i = categoryFound.size(); i < row + 1; i++)
                    categoryFound.add(Boolean.TRUE);
            categoryFound.set(row, found ? Boolean.TRUE : Boolean.FALSE);
        }
         
        /** @interface javax.swing.table.TableModel */
        public int getRowCount() {
            if (current != null) {   
                //MetadataList.CategoryFirstList list = (MetadataList.CategoryFirstList)current.getMetadataList().getMetadata();
                //int size = current.getMetadataList().getMetadata().size(); // [DAN comment]
                int size = current.getMetadataList().getCategoryListSize() + PlusOneForAdd;
                return size > 0 ? size : 1;
            } else if (currentMultiples != null) {
                //MetadataList.CategoryFirstList list = (MetadataList.CategoryFirstList)currentMultiples.getMetadataList().getMetadata();
                //int size = current.getMetadataList().getMetadata().size(); // [DAN comment]
                int size = currentMultiples.getMetadataList().getCategoryListSize();
                return size > 0 ? size : 1;
            } else { 
                //return 1;
                return 0;
            }   
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
            if (current == null && currentMultiples == null) {
                return null;
                //return "<null-VME-table-value>";
            } else if (currentMultiples != null) {
                final int size = currentMultiples.getMetadataList().getCategoryListSize();
                if (size == 0)
                    addNewRow();
                else if (row >= size) 
                    return null;
                return currentMultiples.getMetadataList().getCategoryListElement(row);
            } else {
                final int size = current.getMetadataList().getCategoryListSize();
                if (size == 0 || row == (size+PlusOneForAdd))
                    return InputVME;
                else if (row >= size)
                    return null;
                return current.getMetadataList().getCategoryListElement(row);
                // return current.getMetadataList().getMetadata().get(row);
            }
        }
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
