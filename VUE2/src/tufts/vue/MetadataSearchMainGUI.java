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

package tufts.vue;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.JComponent;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import tufts.Util;
import tufts.vue.gui.DockWindow;
import tufts.vue.gui.GUI;
import tufts.vue.gui.VueTextPane;
import tufts.vue.gui.Widget;
import tufts.vue.gui.WidgetStack;
import tufts.vue.gui.WindowDisplayAction;
import tufts.vue.gui.renderer.SavedSearchTableRenderer;
import tufts.vue.gui.renderer.SearchResultTableModel;
import edu.tufts.vue.metadata.VueMetadataElement;
import edu.tufts.vue.metadata.action.SearchAction;
import static edu.tufts.vue.metadata.action.SearchAction.*;
import edu.tufts.vue.metadata.ui.CategoryComboBoxModel;
import edu.tufts.vue.metadata.ui.CategoryComboBoxRenderer;
import edu.tufts.vue.ontology.OntType;

/**
 * A tabbed-pane collection of property sheets that apply globally to a given
 * map.
 * 
 * @version $Revision: 1.61 $ / $Date: 2010-02-03 19:17:41 $ / $Author: Sheejo
 *          Rapheal $
 * 
 */
public class MetadataSearchMainGUI extends JPanel
    implements ActiveListener<LWMap>
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(MetadataSearchMainGUI.class);

    public final static String SELECTED_MAP_STRING = VueResources.getString("searchgui.currentmap");
    public final static String ALL_MAPS_STRING = VueResources.getString("searchgui.allopenmaps");

    private static final Object[] SearchScopeLimited = { SEARCH_EVERYTHING,
                                                         SEARCH_ONLY_LABELS,
                                                         SEARCH_ONLY_KEYWORDS };
    // Note: it turns out it can be crucial to access at least one of the value instances
    // before calling All() or they may not have been initialized...
    private static final Object[] SearchScope = DEBUG.TEST ? SearchScopeLimited : SearchAction.SearchType.All();
    //private static final String[] SearchScope = { SEARCH_EVERYTHING, SEARCH_ONLY_LABELS, SEARCH_ONLY_KEYWORDS, SEARCH_WITH_CATEGORIES };
    

    //private static final Object[] LogicOps = SearchAction.AllLogicOps;
    private static final Object[] LogicOps = { SearchAction.Operator.AND, SearchAction.Operator.OR };
    private static final Object[] AllOpenMapsResultsTypes = { SearchAction.RA_COPY };
    private static final boolean DEBUG_LOCAL = false;
    

    public final static int BUTTON_COL_WIDTH = 24;
    public final static int ROW_HEIGHT = 30;
    public List<VueMetadataElement> searchTerms = new ArrayList<VueMetadataElement>();
    private /*final*/ SearchAction termsAction = new SearchAction(searchTerms);
    private final JTextField allSearchField = new JTextField();
    //private final SearchAction allSearch = new SearchAction(allSearchField); // not deployed
    public final static int SHOW_OPTIONS = 1;
    public final static int HIDE_OPTIONS = 0;
    private int buttonColumn = 2;
    private int comboColumn = 1;
    private int categoryColumn = -1;
    private int valueColumn = 0;
    private int headerButtonColumn = 2;
    private int headerComboColumn = 1;
    private int headerCategoryColumn = -1;
    private int headerValueColumn = 0;
    private int conditionColumn = -1;

    //private int searchType = EVERYTHING; // LOCAL search-type -- dupes SearchType semantics
    private SearchType searchType = SEARCH_EVERYTHING;

    // combo box numbers within optionsPanel
    public final static int TYPES = 0;
    public final static int LOCATIONS = 1;
    public final static int RESULTS = 2;

    private static final String[] SearchDomains = { SELECTED_MAP_STRING, ALL_MAPS_STRING };
    private boolean singleLine = false;

    private final static Object[] CurrentMapResultsTypes = SearchAction.ResultOp.All();

    static public final int ANY_MODE = 0;
    static public final int ALL_MODE = 1;
    static public final int NOT_ANY_MODE = 2;
    static public final int NONE_MODE = 3;

    private MetaSearchPanel mInfoPanel = null;

    private MetadataPanel metadataPanel = null;
    private final SearchResultTableModel searchResultModel = new SearchResultTableModel();
    VueTextPane mDescriptionEditor = null;
    JTextField mAuthorEditor = null;

    private WidgetStack mapInfoStack = null;
    static AbstractAction saveSearchAction;
    static AbstractAction runSearchAction;
    static AbstractAction renameAction;
    static AbstractAction deleteAction;

    private final List<JComboBox> comboBoxes = new ArrayList<JComboBox>();
    private JPopupMenu popupMenu = new JPopupMenu();
    private final String RENAME_STR = VueResources.getString("searchgui.rename");
    private final String DELETE_STR = VueResources.getString("searchgui.delete");
    private final String RESET_STR = VueResources.getString("searchgui.resetmap");
    private final String SEARCH_STR = VueResources.getString("searchgui.search");
    private final String SAVED_SEARCH_STR = VueResources.getString("searchgui.savedsearches");
    private final String SAVE_SEARCH_STR = VueResources.getString("searchgui.savesearch");
    private final String RUN_SEARCH_STR = VueResources.getString("searchgui.runsearch");

    //private String strAndOrType  = VueResources.getString("searchgui.or");
    private SearchAction.Operator selectedOp = SearchAction.Operator.OR;

    private JPanel topPanel;
    private OptionsPanel optionsPanel;
    private JPanel linePanel;
    private JPanel tablePanel;
    private JPanel searchPanel;

    private JComponent choiceDomain;
    private JComponent choiceFields;
    private JComponent choiceResult;
    
    private JTable searchTermsTbl;
    private JTable searchHeaderTbl;
    private JTable searchResultTbl;
    
    private final JComboBox andOrCmbBox;
    private final JComponent macLeopardAndOr;
    
    private JButton saveButton;
    private JButton resetButton;
    private JButton searchButton;

    private void runSaveAction(ActionEvent e) {
        if(searchTermsTbl.isEditing())
            searchTermsTbl.getCellEditor().stopCellEditing();
        final SearchData data = new SearchData();
        final List<SearchData> searchDataList = new ArrayList<SearchData>();
        //searchDataList = new ArrayList<SearchData>();                    
        final String searchName = getSearchName(searchResultModel.getRowCount());

        if (searchName != null) {
            // data.setSearchType(searchTypeCmbBox.getSelectedItem().toString().trim());
            // data.setMapType(mapCmbBox.getSelectedItem().toString().trim());
            // data.setResultType(resultCmbBox.getSelectedItem().toString().trim());                    
            //data.setAndOrType(selectedOp.key);
            //data.setAndOrType(strAndOrType);
            
            data.setSearchSaveName(searchName);
            data.setSearchType(getChosenString(choiceFields));
            data.setMapType(getChosenString(choiceDomain));
            data.putResultOp((SearchAction.ResultOp) getChosen(choiceResult));
            data.putLogicalOp(selectedOp);
            data.setDataList(searchTerms);
            
            searchDataList.add(data);                    
            searchResultModel.addRow(data);
            
            searchTerms = new ArrayList<VueMetadataElement>();
            ((SearchTermsTableModel) searchTermsTbl.getModel()).refresh();
            VUE.getActiveMap().markAsModified();

            if (searchResultModel.getData() != null && VUE.getActiveMap() != null) {
                VUE.getActiveMap().setSearchArrLst(searchResultModel.getData());
            }
        }
    }
    private String getSearchName(int rowCount){        	
        String inputValue = (String) VueUtil.input
            (null, VueResources.getString("searchgui.entersearchname"),
             null, JOptionPane.PLAIN_MESSAGE, null,
             (VueResources.getString("searchgui.search") + " "+ (rowCount+1)));
        
        if (inputValue != null && inputValue.trim().length() == 0) {
            inputValue = VueResources.getString("searchgui.search") + " "+ (rowCount+1);
        }
        return inputValue;
    }




    public MetadataSearchMainGUI(DockWindow w) {
        super();

        if (Util.isMacLeopard() && DEBUG.TEST) {
            macLeopardAndOr = getButtonBox(LogicOps);
            andOrCmbBox = new JComboBox(LogicOps);
            //andOrCmbBox = null;
        } else {
            andOrCmbBox = new JComboBox(LogicOps);
            macLeopardAndOr = null;
        }
        
        JPopupMenu popup = new JPopupMenu();
        mapInfoStack = new WidgetStack(SEARCH_STR);        
        VUE.addActiveListener(LWMap.class, this);
        setMinimumSize(new Dimension(300, 250));
        setLayout(new BorderLayout());
        mInfoPanel = new MetaSearchPanel();
        mInfoPanel.setName(SEARCH_STR);
        metadataPanel = new MetadataPanel();
        metadataPanel.setName(SAVED_SEARCH_STR);
        //Widget.setWantsScroller(mapInfoStack, true);        
        adjustHeaderTableColumnModel();
        // mTabbedPane.addTab(metadataPanel.getName(),metadataPanel);
        // mapInfoStack.addPane(mInfoPanel, 1f);
        // mapInfoStack.addPane(metadataPanel, 2f);  
        mapInfoStack.addPane(SEARCH_STR,mInfoPanel);
        mapInfoStack.addPane(metadataPanel, 2f);
        // Widget.setWantsScroller(mapInfoStack, true);

        // SearchTextField.editSettingsMenuItem should have a WindowDisplayAction linked to the DockWindow
        // so that it will be properly and automatically checked, but it was created before this DockWindow
        // existed;  now that the DockWindow exists, change the menu item's action to be a WindowDisplayAction.
        if (SearchTextField.editSettingsMenuItem != null) {
        	WindowDisplayAction wda = new WindowDisplayAction(w);
            
        	wda.setTitle(VueResources.getString("search.popup.edit.search.settings"));
        	wda.setLinkedButton(SearchTextField.editSettingsMenuItem);
        
        	SearchTextField.editSettingsMenuItem.setAction(wda);
        }

        saveSearchAction = new AbstractAction(SAVE_SEARCH_STR) {
            public void actionPerformed(ActionEvent e) {
                if (DEBUG.Enabled) Log.debug("saveSearchAction: " + e);
                runSaveAction(e);
            }
        };

        runSearchAction = new AbstractAction(RUN_SEARCH_STR)
                { public void actionPerformed(ActionEvent e) { runSavedSearch(); }};

        renameAction = new AbstractAction(RENAME_STR) {
            public void actionPerformed(ActionEvent e) {
                searchResultModel.setEditableFlag(true);
                int selectedIndex = searchResultTbl.getSelectedRow();
                if (selectedIndex != -1){
                    searchResultTbl.editCellAt(selectedIndex, 0);
                    searchResultModel.setEditableFlag(false);
                }
            }
        };

        deleteAction = new AbstractAction(DELETE_STR) {
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = searchResultTbl.getSelectedRow();
                if (selectedIndex != -1){
                    searchResultModel.removeRow(selectedIndex);
                }
            }
        };

        Widget.setMenuActions(metadataPanel, new Action[] { saveSearchAction,
                runSearchAction, renameAction, deleteAction });        
        Widget.setWantsScroller(mapInfoStack, true);
        Widget.setWantsScrollerAlways(mapInfoStack, true);
        w.setContent(mapInfoStack);       
        w.setHeight(350);
        w.setWidth(300);
        validate();
        setVisible(true);
    }

    private static Box getButtonBox(Object[] stringables) {
        final Box box;
        //box = GUI.createButtonBox("segmented", stringables); // aqua buttons glossy blue
        box = GUI.createButtonBox("segmentedCapsule", stringables); // big & dark
        //box = GUI.createButtonBox("segmentedTextured", stringables); // big & dark (looks same)
        //box = GUI.createButtonBox("segmentedRoundRect", stringables); // smallest (vertically)
        return box;
    }

    private static final String CHOICE_NONE = "<choice:none>";
    
    private Object getChosen(final JComponent jc) {

        Object choice = CHOICE_NONE;
        
        if (jc instanceof javax.swing.JComboBox) {
            choice = ((JComboBox)jc).getSelectedItem();
        } else if (jc instanceof javax.swing.Box) {
            final ButtonGroup group = (ButtonGroup) jc.getClientProperty(ButtonGroup.class);
            if (group != null) {
                // was presumably created by GUI.createButtonBox
                final ButtonModel bm = group.getSelection();
                Log.debug("getChosen: group.getSelection: " + Util.tags(bm));
                final Object objs[] = bm.getSelectedObjects();
                Log.debug("getChosen: buttonModel.getSelectedObjects: " + Util.tags(objs));
                // if (objs != null && objs.length > 0) 
                //     choice = objs[0];
                //for (AbstractButton ab : bm.getElements()) { }
                for (Component c : jc.getComponents()) {
                    Log.debug("getChosen: examine: " + GUI.name(c));
                    if (c instanceof AbstractButton) {
                        if (((AbstractButton)c).isSelected()) {
                            Log.debug("getChosen: isSelec: " + GUI.name(c));
                            choice = ((JComponent)c).getClientProperty("segment.value"); // handle Strings as well as ComboKey's
                          //choice = ((JComponent)c).getClientProperty(GUI.ComboKey.class);
                            break;
                        }
                    }
                }
            }
        }
        Log.debug("getChosen: " + GUI.name(jc) + "=" + GUI.name(choice));
        //Log.debug("getChosen: " + GUI.name(jc) + "=" + Util.tags(choice));
        return choice;
    }

    private String getChosenString(JComponent jc) {
        return getChosen(jc).toString().trim();
    }
    
    private JComponent getChooser(Object[] choices, ItemListener listener, String name) {
        final JComponent jc;
        if (DEBUG.TEST) {
            jc = getButtonBox(choices); // TODO: no listeners
        } else {
            jc = getComboBox(choices, listener);
        }
        if (DEBUG.Enabled) {
            Log.debug("getChooser " + Util.tags(name) + "; choices:");
            Util.dump(choices);
        }
        jc.setName(name);
        return jc;
    }

    private JComboBox getComboBox(Object[] choices, ItemListener listener) {
        final JComboBox combo = new JComboBox(choices);
        combo.setFont(tufts.vue.gui.GUI.LabelFace);
        combo.addItemListener(listener);
        if (true)
            combo.setPreferredSize(combo.getMinimumSize());
        comboBoxes.add(combo);
        return combo;
    }

    // ///////////////
    // Inner Classes
    // //////////////////

    /**
     * InfoPanel This is the tab panel for displaying Map Info
     **/
    public class MetaSearchPanel extends JPanel implements PropertyChangeListener, FocusListener {
        
            final ItemListener searchTypesListener = new ItemListener() {
                public void itemStateChanged(ItemEvent ie) {
                    if (ie.getStateChange() != ItemEvent.SELECTED)
                        return;
                    // This would all better be handled via events instead
                    // of this direct calling into SearchTextField hack stuff.
                    final Object selected = ie.getItem();
                    
                    if (SEARCH_EVERYTHING.equals(selected)) {
                        setEverythingSearch();
                        SearchTextField.searcheveryWhereMenuItem.setSelected(true);
                    } else if (SEARCH_ONLY_LABELS.equals(selected)) {
                        setLabelSearch();
                        SearchTextField.labelMenuItem.setSelected(true);
                    } else if (SEARCH_ONLY_KEYWORDS.equals(selected)) {
                        setAllMetadataSearch();
                        SearchTextField.keywordMenuItem.setSelected(true);
                    } else if (SEARCH_WITH_CATEGORIES.equals(selected)) {
                        setCategorySearch();
                        SearchTextField.categoryKeywordMenuItem.setSelected(true);                          
                    } else
                        Log.warn("Unknown selected search-type: " + Util.tags(selected));
                    
                    SearchTextField.updateSearchType();
                }
            };
        
            final ItemListener locationChoiceListener = new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        String type = e.getItem().toString();
                        if (type.equals(ALL_MAPS_STRING)) {
                            //allSearch.setLocationType(SearchAction.SEARCH_ALL_OPEN_MAPS);                            
                            termsAction.setLocationType(SearchAction.SEARCH_ALL_OPEN_MAPS);
                            optionsPanel.switchChoices(RESULTS, AllOpenMapsResultsTypes);
                        } else // SELECTED_MAP_STRING as current default
                        {
                            //allSearch.setLocationType(SearchAction.SEARCH_SELECTED_MAP);
                            termsAction.setLocationType(SearchAction.SEARCH_SELECTED_MAP);
                            optionsPanel.switchChoices(RESULTS, CurrentMapResultsTypes);
                        }
                    }
                }
            };
        
            final ItemListener resultsTypeListener = new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        if (termsAction != null && e.getItem() instanceof GUI.ComboKey) {
                            // This means the SearchAction won't be told about the "Create New Map"
                            // result action, but it doesn't need to -- currently, any search
                            // across all maps has only one fixed result action: create a new map.
                            termsAction.setResultAction(e.getItem());
                        }
                    }
                }
            };


        private final static int HALF_GUTTER = 4;
        private final static int GUTTER = HALF_GUTTER * 2;

        final Insets
            labelInsets = new Insets(HALF_GUTTER, HALF_GUTTER, HALF_GUTTER, 0),
            textFieldInsets = new Insets(HALF_GUTTER, HALF_GUTTER, HALF_GUTTER, HALF_GUTTER);

        private void addPair(GridBagConstraints g, JComponent label, JComponent chooser)
        {
            label.setFont(tufts.vue.gui.GUI.LabelFace);
            
            g.gridx = 0;
            g.insets = labelInsets;
            //g.insets = GUI.EmptyInsets;
            g.weightx = 0.0;
            optionsPanel.add(label, g); // only member reference (enclosing class)

            g.gridx = 1;
            g.insets = textFieldInsets;
            //g.insets = GUI.EmptyInsets;
            g.weightx = 1.0;
            optionsPanel.add(chooser, g);

            g.gridy++;

            if (DEBUG.BOXES) {
                label.setBackground(Color.YELLOW);
            	label.setOpaque(true);
                chooser.setBackground(Color.YELLOW);
            	chooser.setOpaque(true);
            }
        }
        
        public MetaSearchPanel() {

            final JLabel labelDomain, labelScope, labelResult;
            final GridBagConstraints gb = new GridBagConstraints();
            
            optionsPanel = new OptionsPanel(new GridBagLayout()); // on MDSMGU
            gb.fill = GridBagConstraints.HORIZONTAL;
            gb.gridy = 0; // first row starts at 0

            labelDomain = new JLabel(VueResources.getString("searchgui.maps"), SwingConstants.RIGHT);            
          //labelScope = new JLabel(VueResources.getString("searchgui.searchtype"), SwingConstants.RIGHT); // too long
            labelScope = new JLabel(VueResources.getString("searchgui.search")+":", SwingConstants.RIGHT); // short & sweet
            labelResult = new JLabel(GUI.ensureColon(VueResources.getString("searchgui.results")), SwingConstants.RIGHT);
            
            choiceDomain = getChooser(SearchDomains, locationChoiceListener, "locationTypes");
            choiceFields = getChooser(SearchScope, searchTypesListener, "searchScope");
            choiceResult = getChooser(CurrentMapResultsTypes, resultsTypeListener, "resultTypes");
            
            if (DEBUG.TEST) {
                addPair(gb, labelDomain, choiceDomain);
                addPair(gb, labelScope, choiceFields);
            } else {
                addPair(gb, labelScope, choiceFields);
                addPair(gb, labelDomain, choiceDomain);
            }
            addPair(gb, labelResult, choiceResult);


            if (DEBUG.BOXES || DEBUG_LOCAL) optionsPanel.setBackground(Color.BLUE);

            linePanel = new JPanel() {
                protected void paintComponent(java.awt.Graphics g) {
                    if (isOpaque()) {
                    	g.setColor(getBackground());
                        g.fillRect(0, 0, getWidth(), getHeight());
                    }

                    g.setColor(java.awt.Color.DARK_GRAY);
                    g.drawLine(HALF_GUTTER, getHeight() / 2, getWidth() - HALF_GUTTER - 1, getHeight() / 2);
                }
            };

            searchHeaderTbl = new JTable(new SearchHeaderTableModel());
            searchHeaderTbl.setOpaque(false);
            searchHeaderTbl.setRowHeight(ROW_HEIGHT);
            if (DEBUG.BOXES) {
                searchHeaderTbl.setShowGrid(true);
                searchHeaderTbl.setGridColor(Color.red);
            } else
                searchHeaderTbl.setShowGrid(false);
                
            searchHeaderTbl.setIntercellSpacing(new Dimension(GUTTER, GUTTER));

            searchTermsTbl = new JTable(new SearchTermsTableModel());                
            adjustColumnModel();
            searchTermsTbl.setDefaultRenderer(java.lang.Object.class, new SearchTermsTableRenderer());
            searchTermsTbl.setDefaultEditor(java.lang.Object.class, new SearchTermsTableEditor());
            ((DefaultCellEditor) searchTermsTbl .getDefaultEditor(java.lang.Object.class)).setClickCountToStart(1);
            searchTermsTbl.setRowHeight(ROW_HEIGHT);
            searchTermsTbl.getTableHeader().setReorderingAllowed(false);
            searchTermsTbl.setGridColor(new java.awt.Color(getBackground()
                    .getRed(), getBackground().getBlue(), getBackground()
                    .getGreen(), 0));
            searchTermsTbl.setBackground(getBackground());        
            searchTermsTbl.setIntercellSpacing(new Dimension(GUTTER, GUTTER));

            searchTermsTbl.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseReleased(java.awt.event.MouseEvent evt) {
                    if (evt.getX() > searchTermsTbl.getWidth() - BUTTON_COL_WIDTH) {
                        // java.util.List<VueMetadataElement>
                        // searchTermsList =
                        // MetadataSearchMainGUI.this.searchTerms;
                        int selectedRow = searchTermsTbl.getSelectedRow();

                        if (searchTermsTbl.getSelectedColumn() == buttonColumn
                                && searchTerms.size() > selectedRow) {                                    
                            searchTerms.remove(selectedRow);
                            searchTermsTbl.repaint();
                            requestFocusInWindow();
                        }
                    }
                }
            });

            tablePanel = new JPanel(new GridBagLayout());
            final GridBagConstraints tablePanelGBC = new GridBagConstraints();
            tablePanelGBC.fill = GridBagConstraints.HORIZONTAL;
            tablePanelGBC.insets = new Insets(0, 0, 0, 0);
            tablePanelGBC.weightx = 1.0;
            tablePanelGBC.gridx = 0;
            tablePanelGBC.gridy = 0;

            if (macLeopardAndOr != null) {
                tablePanel.add(new JLabel("HERE MY BUDDY"), tablePanelGBC);
                tablePanelGBC.fill = GridBagConstraints.REMAINDER;
                tablePanelGBC.anchor = GridBagConstraints.EAST;
                tablePanel.add(macLeopardAndOr, tablePanelGBC);
                tablePanelGBC.gridy++;
                tablePanelGBC.fill = GridBagConstraints.HORIZONTAL;
            }
            
            tablePanel.add(searchHeaderTbl, tablePanelGBC);
            tablePanelGBC.gridy++;

            tablePanel.add(searchTermsTbl, tablePanelGBC);
            tablePanelGBC.gridy++;

            if (DEBUG_LOCAL || DEBUG.BOXES) {
            	searchHeaderTbl.setOpaque(true);
            	searchHeaderTbl.setBackground(Color.BLUE);
            	searchTermsTbl.setOpaque(true);
            	searchTermsTbl.setBackground(Color.BLUE);
            	tablePanel.setOpaque(true);
            	tablePanel.setBackground(Color.BLUE);
            }

            saveButton = new JButton(VueResources.getString("searchgui.save"));
            saveButton.setFont(tufts.vue.gui.GUI.LabelFace);
            saveButton.addActionListener(new ActionListener() { // TODO: CHRIST: MORE REPEATED CODE???
                    public void actionPerformed(ActionEvent e) {
                        if (DEBUG.Enabled) Log.debug("saveButton action: " + e);
                        runSaveAction(e);
                    }
                });

            resetButton = new JButton(VueResources.getString("searchgui.resetmap"));
            resetButton.setFont(tufts.vue.gui.GUI.LabelFace);
            resetButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    SearchAction.revertGlobalSearchSelectionFromMSGUI();
                    VUE.getActiveViewer().repaint();
                }
            });

            searchButton = new JButton(VueResources.getString("searchgui.search"));
            searchButton.setFont(tufts.vue.gui.GUI.LabelFace);
            searchButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (searchTermsTbl.isEditing())
                            searchTermsTbl.getCellEditor().stopCellEditing();
                        
                        //final SearchType whichFieldsToSearch = (SearchType) getChosen(choiceFields);
                        MetadataSearchMainGUI.this.searchType = (SearchType) getChosen(choiceFields);
                        MetadataSearchMainGUI.this.termsAction = new SearchAction(searchTerms);
                        
                        termsAction.setParamsByType(searchType);
                        termsAction.setResultAction(getChosen(choiceResult));
                        
                        if (ALL_MAPS_STRING.equals(getChosenString(choiceDomain)))
                            termsAction.setLocationType(SearchAction.SEARCH_ALL_OPEN_MAPS);
                        else
                            termsAction.setLocationType(SearchAction.SEARCH_SELECTED_MAP);

                        termsAction.setOperator(getSelectedOperator());

                        //        if (SEARCH_LABELS_ONLY.equals(fields)) { searchType = LABEL;
                        // } else if (SEARCH_ALL_KEYWORDS.equals(fields)) { searchType = KEYWORD;
                        // } else if (SEARCH_CATEGORIES_AND_KEYWORDS.equals(fields)) { searchType = CATEGORY;
                        // } else { // if (SEARCH_EVERYTHING.equals(fields)) searchType = EVERYTHING;
                        // }

                        termsAction.fire(this, "searchFromPanelButton");
                    }
                });

            termsAction.setResultAction(SearchAction.RA_SELECT);
            //setResultsTypeInActions("Select");

            searchPanel = new JPanel(new GridBagLayout());
            GridBagConstraints searchPanelGBC = new GridBagConstraints();
            searchPanelGBC.insets = new Insets(HALF_GUTTER, HALF_GUTTER, HALF_GUTTER, HALF_GUTTER);
            searchPanelGBC.fill = GridBagConstraints.HORIZONTAL;
            searchPanelGBC.weightx = 0.0;
            searchPanelGBC.gridy = 0;

            searchPanelGBC.gridx = 0;
            searchPanel.add(saveButton, searchPanelGBC);

            // An empty panel to take up the space between saveButton to the left and the other
            // two buttons to the right.
            searchPanelGBC.gridx = 1;
            searchPanelGBC.weightx = 1.0;
            searchPanel.add(new JPanel(), searchPanelGBC);

            searchPanelGBC.gridx = 2;
            searchPanelGBC.weightx = 0.0;
            searchPanel.add(resetButton, searchPanelGBC);

            searchPanelGBC.gridx = 3;
            searchPanel.add(searchButton, searchPanelGBC);

            searchPanel.setOpaque(true);
            searchPanel.setBackground(getBackground());

            if (DEBUG_LOCAL || DEBUG.BOXES) {
                saveButton.setOpaque(true);
                saveButton.setBackground(Color.YELLOW);
                resetButton.setOpaque(true);
                resetButton.setBackground(Color.YELLOW);
                searchButton.setOpaque(true);
                searchButton.setBackground(Color.YELLOW);
                searchPanel.setBackground(Color.CYAN);
            }

            topPanel = new JPanel(new GridBagLayout());
            GridBagConstraints topPanelGBC = new GridBagConstraints();
            topPanelGBC.fill = GridBagConstraints.HORIZONTAL;
            topPanelGBC.weightx = 1.0;
            topPanelGBC.insets = new java.awt.Insets(HALF_GUTTER, HALF_GUTTER, 0, HALF_GUTTER);
            topPanelGBC.gridx = 0;

            topPanelGBC.gridy = 0;
            topPanel.add(optionsPanel, topPanelGBC);

            topPanelGBC.insets = new java.awt.Insets(0, HALF_GUTTER, 0, HALF_GUTTER);
            topPanelGBC.gridy = 1;
            topPanel.add(linePanel, topPanelGBC);

            topPanelGBC.gridy = 2;
            topPanel.add(tablePanel, topPanelGBC);

            topPanelGBC.gridy = 3;
            topPanelGBC.insets = new java.awt.Insets(0, HALF_GUTTER, HALF_GUTTER, HALF_GUTTER);
            topPanel.add(searchPanel, topPanelGBC);

            if (DEBUG_LOCAL || DEBUG.BOXES) {
            	linePanel.setBackground(Color.CYAN);
            	linePanel.setOpaque(true);
            	topPanel.setBackground(Color.ORANGE);
            }

            setLayout(new BorderLayout());
            add(topPanel);
        }


        // public void setResultsTypeInActions(String resultsTypeChoice) {
        //     // String resultsTypeChoice = e.getItem().toString();
        //     if (resultsTypeChoice != null /*&& allSearch != null*/ && termsAction != null) {
        //         //allSearch.setResultsType(resultsTypeChoice);
        //         termsAction.setResultsType(resultsTypeChoice);
        //     }
        // }

        public String getName() {
            return VueResources.getString("searchgui.search");
        }

        private void saveInfo() {
            // System.out.println("MIP saveInfo " +
            // mDescriptionEditor.getText());
            // if( mMap != null) {
            // // for now, only description/notes needs saving, and it handles
            // that it itself
            // // add back in Author until/unless synched with metadata list:
            // VUE-951
            // //mMap.setLabel( mTitleEditor.getText() );
            // //mMap.setAuthor( mAuthorEditor.getText() );
            // //mMap.setNotes(mDescriptionEditor.getText());
            // //mMap.setDescription(mDescriptionEditor.getText());
            // }
        }

        /**
         * public void actionPerformed( ActionEvent pEvent) { Object source =
         * pEvent.getSource(); System.out.println("Action Performed :"+source);
         * if( (source == saveButton) || (source == mTitleEditor) || (source ==
         * mAuthorEditor) || (source == mDescriptionEditor) ) { saveInfo(); } }
         **/
        public void propertyChange(PropertyChangeEvent pEvent) {

        }

        public void focusGained(FocusEvent e) {
        }

        public void focusLost(FocusEvent e) {
            saveInfo();
        }

    }

    /**
     * MetadataPanel This is the Result Set Display Panel
     * 
     **/

    public class MetadataPanel extends JPanel implements ActionListener,
            PropertyChangeListener {
        PropertiesEditor propertiesEditor = null;

        public MetadataPanel() {
            searchResultTbl = new JTable(searchResultModel);    
            searchResultModel.setEditableFlag(false);
            //searchResultTbl.setOpaque(true);            
            searchResultTbl
                    .setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            searchResultTbl.setDefaultRenderer(java.lang.Object.class,
                    new SavedSearchTableRenderer(searchResultModel));
            //searchResultTbl.setCellEditor( new SearchResultTableEditor(new JCheckBox()));
            
            ((DefaultCellEditor) searchResultTbl
                    .getDefaultEditor(java.lang.Object.class))
                    .setClickCountToStart(1);
            JMenuItem resetMenuItem = new JMenuItem(RESET_STR);
            popupMenu.add(resetMenuItem);
            popupMenu.addSeparator();
            resetMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                	SearchAction.revertGlobalSearchSelectionFromMSGUI();
                    VUE.getActiveViewer().repaint();
                }
            });
            JMenuItem renameMenuItem = new JMenuItem(RENAME_STR);
            popupMenu.add(renameMenuItem);
            popupMenu.addSeparator();
            JMenuItem deleteMenuItem = new JMenuItem(DELETE_STR);
            popupMenu.add(deleteMenuItem);            
            deleteMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    int selectedIndex = searchResultTbl.getSelectedRow();
                    if (selectedIndex != -1){
                        searchResultModel.removeRow(selectedIndex);
                    }
                }
            });
            renameMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    searchResultModel.setEditableFlag(true);
                    int selectedIndex = searchResultTbl.getSelectedRow();
                    if (selectedIndex != -1){
                        searchResultTbl.editCellAt(selectedIndex, 0);
                        searchResultModel.setEditableFlag(false);
                    }
                }
            });
            // add the listener to the jtable
            MouseListener popupListener = new PopupListener();
            // add the listener specifically to the header
            searchResultTbl.addMouseListener(popupListener);
            searchResultTbl.setIntercellSpacing(new Dimension(0,1));
            searchResultTbl.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            searchResultTbl.setShowGrid(true); 
            searchResultTbl.setGridColor(Color.LIGHT_GRAY);
            searchResultTbl.setRowHeight(23);           
            //searchResultTbl.setBackground(this.getBackground());

            //searchResultTbl.getColumnModel().getColumn(1).setMaxWidth(30);
            //searchResultTbl.getColumnModel().getColumn(1).setMinWidth(30);
            searchResultTbl.getColumnModel().getColumn(1).setPreferredWidth(100);
            
            //searchResultTbl.setOpaque(true);
            
            // searchResultTbl.setMinimumSize(new Dimension(300,80));

            GridBagLayout gridBag = new GridBagLayout();
            GridBagConstraints gBC = new GridBagConstraints();
            gBC.fill = GridBagConstraints.HORIZONTAL;
            JPanel panel = new JPanel(); 
            panel.setLayout(new BorderLayout());
           
            gBC.gridx = 0;
    	    gBC.gridy = 0;
    	    gBC.weightx = 1.0;	    
            gridBag.setConstraints(searchResultTbl, gBC);
            panel.add(searchResultTbl,BorderLayout.NORTH);
            panel.add(new JLabel(DEBUG.BOXES ? "x" : " "), BorderLayout.SOUTH);
            setLayout(new BorderLayout());
            add(panel);

            // add(searchResultTbl);
            setName(VueResources.getString("searchgui.keywords"));
        }

        public void actionPerformed(ActionEvent e) {
        }

        public void propertyChange(PropertyChangeEvent evt) {
        }

    }    
    class SearchTermsTableModel extends AbstractTableModel {
        
        private int columns = 3;
        

        public int getRowCount() {            
            if (searchTerms.size() > 0)
                return searchTerms.size();
            else
                return 1;
        }

        public int getColumnCount() {
            return columns;
        }

        public void setColumns(int columns) {
            this.columns = columns;
            fireTableStructureChanged();
        }

        public boolean isCellEditable(int row, int col) {
            if ((col == valueColumn) || (col == categoryColumn)
                    || (col == conditionColumn) || (col == comboColumn))
                return true;
            else
                return false;
        }

        public Object getValueAt(int row, int col) {            
            if (row == 0 && searchTerms.size() == 0) {
                VueMetadataElement vme = new VueMetadataElement();                
                String statementObject[] = {
                    edu.tufts.vue.rdf.RDFIndex.VueTermOntologyNone,
                    //VueResources.getString("metadata.vue.url") + "#none",
                    "",
                    edu.tufts.vue.rdf.Query.Qualifier.CONTAINS.toString() };
                vme.setObject(statementObject);
                vme.setType(VueMetadataElement.SEARCH_STATEMENT);                
                searchTerms.add(vme);
            }            
            if (col == buttonColumn)
                return "delete button";
            else if (col == valueColumn)
                return searchTerms.get(row).getValue();
            else if (col == conditionColumn) {
                String[] statement = (String[]) searchTerms.get(row)
                        .getObject();                
                if (statement.length > 2){                    
                    return ((String[]) (searchTerms.get(row)).getObject())[2];
                }
                else{
                    return "";
                }
            } else
                return searchTerms.get(row).getKey();
        }
         public void setValueAt(Object aValue, int rowIndex, int columnIndex) {             
             VueMetadataElement searchTerm = searchTerms.get(rowIndex);                                        
                   if (searchTerm.getObject() instanceof String[]) {        
                          // ((String[]) searchTerms.get(rowIndex).getObject())[1] = aValue.toString();
                   String[] newStatement = {
             ((String[]) searchTerms.get(rowIndex).getObject())[0],
             aValue.toString(),
             ((String[]) searchTerms.get(rowIndex).getObject())[2] };
             searchTerm.setObject(newStatement);
            }
        }
        public void refresh() {
            fireTableDataChanged();
        }
    }

    class SearchHeaderTableModel extends AbstractTableModel {

        private int columns = 3;

        public int getRowCount() {
            return 1;
        }

        public int getColumnCount() {
            return columns;
        }

        public void setColumns(int columns) {
            this.columns = columns;
            fireTableStructureChanged();
        }

        public boolean isCellEditable(int row, int col) {
            if(this.getColumnCount() == 3){
                if (col == 0) {
                    return false;
                } else {
                    return true;
                }
            }else if(this.getColumnCount() == 4){
                if (col == 0 || col == 1) {
                    return false;
                } else {
                    return true;
                }
            }
            return false;
        }

        public Object getValueAt(int row, int col) {
            return null;
        }

        public void refresh() {
            fireTableDataChanged();
        }
    }
    public void adjustHeaderTableColumnModel() {
        if (searchHeaderTbl == null)
            return;
        int editorWidth = searchHeaderTbl.getWidth();
        
        if (searchHeaderTbl.getModel().getColumnCount() == 3) {    
            searchHeaderTbl.getColumnModel().getColumn(0).setCellRenderer(new SearchTermsTableHeaderRenderer());
            searchHeaderTbl.getColumnModel().getColumn(1).setCellRenderer(new ComboBoxAndOrRenderer(andOrCmbBox));            
            searchHeaderTbl.getColumnModel().getColumn(2).setCellRenderer(new SearchTermsTableHeaderRenderer());
            searchHeaderTbl.getColumnModel().getColumn(1).setCellEditor(new ComboBoxAndOrEditor(andOrCmbBox));
            searchHeaderTbl.getColumnModel().getColumn(2).setCellEditor(new AddButtonTableCellEditor());
            
            if (Util.isMacLeopard()) {
                searchHeaderTbl.getColumnModel().getColumn(1).setMaxWidth(78);
                searchHeaderTbl.getColumnModel().getColumn(1).setMinWidth(78);
            } else {
                searchHeaderTbl.getColumnModel().getColumn(1).setMaxWidth(60);
                searchHeaderTbl.getColumnModel().getColumn(1).setMinWidth(60);
            }

            searchHeaderTbl.getColumnModel().getColumn(2).setMaxWidth(BUTTON_COL_WIDTH);
            searchHeaderTbl.getColumnModel().getColumn(2).setMinWidth(BUTTON_COL_WIDTH);

        } else if (searchHeaderTbl.getModel().getColumnCount() == 4) {
        
            searchHeaderTbl.getColumnModel().getColumn(0).setCellRenderer(new SearchTermsTableHeaderRenderer());
            searchHeaderTbl.getColumnModel().getColumn(1).setCellRenderer(new SearchTermsTableHeaderRenderer());
            searchHeaderTbl.getColumnModel().getColumn(2).setCellRenderer(new ComboBoxAndOrRenderer(andOrCmbBox));            
            searchHeaderTbl.getColumnModel().getColumn(3).setCellRenderer(new SearchTermsTableHeaderRenderer());            
            searchHeaderTbl.getColumnModel().getColumn(2).setCellEditor(new ComboBoxAndOrEditor(andOrCmbBox));
            searchHeaderTbl.getColumnModel().getColumn(3).setCellEditor(new AddButtonTableCellEditor());
            
            searchHeaderTbl.getColumnModel().getColumn(0).setPreferredWidth(145);

            if (Util.isMacLeopard()) {
                searchHeaderTbl.getColumnModel().getColumn(2).setMaxWidth(78);
                searchHeaderTbl.getColumnModel().getColumn(2).setMinWidth(78);
            } else {
                searchHeaderTbl.getColumnModel().getColumn(2).setMaxWidth(60);
                searchHeaderTbl.getColumnModel().getColumn(2).setMinWidth(60);
            }

            searchHeaderTbl.getColumnModel().getColumn(3).setMaxWidth(BUTTON_COL_WIDTH);
            searchHeaderTbl.getColumnModel().getColumn(3).setMinWidth(BUTTON_COL_WIDTH);
        }
    }

    final static JLabel EmptyLabel = new JLabel(DEBUG.BOXES ? "(hid)" : "");
    
    public class ComboBoxAndOrEditor extends DefaultCellEditor {
        final JComboBox combo; 
        public ComboBoxAndOrEditor(JComboBox combo) {                
            super(combo);
            this.combo = combo;
        }
        public Component getTableCellEditorComponent(JTable table,
                Object value, boolean isSelected, int rowIndex,
                int vColIndex) {
            if(table.isEditing()){
                table.getCellEditor().stopCellEditing();
            }

            selectedOp = (SearchAction.Operator) this.combo.getSelectedItem();

            // //if(isSelected){                
            //     if(this.combo.getSelectedItem().toString().equals(VueResources.getString("searchgui.and"))){                    
            //         strAndOrType = VueResources.getString("searchgui.or");
            //     }else{                    
            //         strAndOrType = VueResources.getString("searchgui.and");
            //     }                                    
            // //}
                
            if(vColIndex == 1 || vColIndex == 2){                    
                SearchTermsTableModel model = (SearchTermsTableModel)searchTermsTbl.getModel();
                if(model.getRowCount()<2){                        
                    return EmptyLabel;
                }else{
                    return this.combo;
                }
            }                
            return EmptyLabel;
        }
        public boolean stopCellEditing() {            
            return super.stopCellEditing();
        }

    }

    /** for debug tracking */
    private static class MDAddLabel extends JLabel {}
    private static class MDDeleteLabel extends JLabel {}

    public class AddButtonTableCellEditor extends AbstractCellEditor
            implements TableCellEditor {
        // This is the component that will handle the editing of the cell
        // value
        final JLabel component = new MDAddLabel();

        // This method is called when a cell value is edited by the user.
        public Component getTableCellEditorComponent(JTable table,
                Object value, boolean isSelected, int rowIndex,
                int vColIndex) {    
            if(searchTermsTbl.isEditing()){
                searchTermsTbl.getCellEditor().stopCellEditing();
            }
            if (vColIndex == 2 || vColIndex == 3) {    
                component.setIcon(tufts.vue.VueResources.getImageIcon("metadata.editor.add.up"));
                final VueMetadataElement newElement = new VueMetadataElement();
                final String statementObject[] = {
                    edu.tufts.vue.rdf.RDFIndex.VueTermOntologyNone,
                    "",
                    edu.tufts.vue.rdf.Query.Qualifier.CONTAINS.toString() };
                // Don't know why this had STARTS_WITH as a default -- tho
                // later on down the line this value is currently ignored 
                // as we force to always doing CONTAINS searches in SearchAction.
                //edu.tufts.vue.rdf.Query.Qualifier.STARTS_WITH.toString() };
                newElement.setObject(statementObject);
                newElement.setType(VueMetadataElement.SEARCH_STATEMENT);                
                searchTerms.add(newElement);                
                ((SearchTermsTableModel) searchTermsTbl.getModel()).refresh();
                ((SearchHeaderTableModel) searchHeaderTbl.getModel()).refresh();                    
            }

            if (DEBUG_LOCAL || DEBUG.BOXES) {
            	component.setBackground(Color.YELLOW);
            	component.setOpaque(true);
            }

            // Return the configured component
            return component;
        }

        // This method is called when editing is completed.
        // It must return the new value to be stored in the cell.
        public Object getCellEditorValue() {
            return (JLabel) component;
        }
    }
    public class ComboBoxAndOrRenderer implements TableCellRenderer {

        JComboBox combo = null;

        public ComboBoxAndOrRenderer(JComboBox combo) {        	
            this.combo = combo;
        }

        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            if (searchTermsTbl.getModel().getRowCount() > 1) {
             
               //Don't ask me how or why but this makes the combo box paint correctly on leopard.
            	
               if (Util.isMacLeopard())
            	   combo.putClientProperty("JComboBox.isTableCellEditor",Boolean.FALSE);
                
                // combo.setPreferredSize(new Dimension(60,30));
                combo.setFont(tufts.vue.gui.GUI.LabelFace);        
                combo.setVisible(true);
                //combo.repaint();
                // combo.invalidate();

                if (DEBUG_LOCAL || DEBUG.BOXES) {
                	combo.setOpaque(true);
                	combo.setBackground(Color.YELLOW);
                }

                table.revalidate();
                table.repaint();
            } else {
                JLabel label = EmptyLabel;
                label.setOpaque(true);
                label.setEnabled(false);

                if (DEBUG_LOCAL || DEBUG.BOXES) { label.setBackground(Color.YELLOW); }

                return EmptyLabel;
            }
            
            return combo;
        }
    }
   
    public void adjustColumnModel() {

        if (searchTermsTbl == null)
            return;
        int editorWidth = this.getWidth();
        if (searchTermsTbl.getModel().getColumnCount() == 3) {
            
            searchTermsTbl.getColumnModel().getColumn(1).setMaxWidth(0);
            searchTermsTbl.getColumnModel().getColumn(1).setMinWidth(0);
            searchTermsTbl.getColumnModel().getColumn(1).setPreferredWidth(0);
            searchTermsTbl.getColumnModel().getColumn(2).setMaxWidth(24);
            searchTermsTbl.getColumnModel().getColumn(2).setMinWidth(24);

        } else if (searchTermsTbl.getModel().getColumnCount() == 4) {
        
            searchTermsTbl.getColumnModel().getColumn(0).setPreferredWidth(80);
            //searchTermsTbl.getColumnModel().getColumn(1).setPreferredWidth(150);
            searchTermsTbl.getColumnModel().getColumn(2).setMaxWidth(0);
            searchTermsTbl.getColumnModel().getColumn(2).setMinWidth(0);    
            searchTermsTbl.getColumnModel().getColumn(2).setPreferredWidth(0);
            searchTermsTbl.getColumnModel().getColumn(3).setMaxWidth(BUTTON_COL_WIDTH);
            searchTermsTbl.getColumnModel().getColumn(3).setMinWidth(BUTTON_COL_WIDTH);
        
        } 
    }

    // public void setCategorySearchWithNoneCase() {        
    // 	if (DEBUG_LOCAL || DEBUG.SEARCH) Log.debug("setCategorySearchWithNoneCase()");
    //     singleLine = false;
    //     SearchTermsTableModel model = (SearchTermsTableModel) searchTermsTbl.getModel();
    //     buttonColumn = 2;
    //     valueColumn = 1;
    //     categoryColumn = 0;
    //     conditionColumn = -1;
    //     model.setColumns(3);
    //     adjustColumnModel();
    //     headerButtonColumn = 2;
    //     headerComboColumn = 1;
    //     headerCategoryColumn = -1;
    //     headerValueColumn = 0;
    //     //termsAction = new SearchAction(searchTerms);
    //     adjustHeaderTableColumnModel();
    //     // treatNoneSpecially = true;
    //     termsAction.setNoneIsSpecial(true);
    //     termsAction.setTextOnly(false);
    //     termsAction.setBasic(false);
    //     termsAction.setMetadataOnly(false);
    //     termsAction.setOperator(getSelectedOperator());
    //     termsAction.setEverything(false);
    //     // termsAction.setOperator(andOrGroup.getSelection().getModel().getActionCommand());
    // }

    /**
     * the root kind of search for this is functionally the same as search-everything, EXCEPT
     * that it turns on the keyword field boxes (labeled "categories").  That also means
     * it doesn't really make sense to have it as an option in the main search text field
     * pull-down.
     */
    public void setCategorySearch() {
    	if (DEBUG_LOCAL || DEBUG.SEARCH) Log.debug("setCategorySearch()");
        
        singleLine = false;
        searchType = SEARCH_WITH_CATEGORIES;
        SearchTermsTableModel model = (SearchTermsTableModel) searchTermsTbl.getModel();    
        SearchHeaderTableModel headerModel = (SearchHeaderTableModel) searchHeaderTbl
        .getModel();
        headerModel.setColumns(4);
        buttonColumn = 3;        
        valueColumn = 1;
        categoryColumn = 0;
        conditionColumn = -1;
        model.setColumns(4);
        adjustColumnModel();
        adjustHeaderTableColumnModel();
        headerButtonColumn = 3;
        headerComboColumn = 2;
        headerCategoryColumn = 0;
        headerValueColumn = 1;
        adjustHeaderTableColumnModel();
        //termsAction = new SearchAction(searchTerms);

        termsAction.setBasic(false);
        termsAction.setTextOnly(false);
        termsAction.setMetadataOnly(false);        
        termsAction.setOperator(getSelectedOperator());
        termsAction.setEverything(false);
        // termsAction.setOperator(andOrGroup.getSelection().getModel().getActionCommand());
    }

    public void setEverythingSearch() {
    	if (DEBUG_LOCAL || DEBUG.SEARCH) Log.debug("setEverythingSearch()");

        searchType = SEARCH_EVERYTHING;
        singleLine = false;
        SearchTermsTableModel model = (SearchTermsTableModel) searchTermsTbl.getModel();
        SearchHeaderTableModel headerModel = (SearchHeaderTableModel) searchHeaderTbl
        .getModel();
        headerModel.setColumns(3);
        buttonColumn = 2;        
        valueColumn = 0;
        categoryColumn = -1;
        conditionColumn = -1;
        model.setColumns(3);
        adjustColumnModel();
        adjustHeaderTableColumnModel();
        headerButtonColumn = 2;
        headerComboColumn = 1;
        headerCategoryColumn = -1;
        headerValueColumn = 0;
        
        //termsAction = new SearchAction(searchTerms);

        termsAction.setBasic(false);
        termsAction.setTextOnly(true);
        termsAction.setMetadataOnly(false);        
        termsAction.setOperator(getSelectedOperator());
        termsAction.setEverything(true);
        // termsAction.setOperator(andOrGroup.getSelection().getModel().getActionCommand());
    }
    
    // public String getSelectedOperator() {        
    //     if(strAndOrType.equals(VueResources.getString("searchgui.and"))){
    //         return SearchAction.AND;
    //     }else{
    //         return SearchAction.OR;
    //     }            
    // }
    public SearchAction.Operator getSelectedOperator() {        
        return selectedOp;
    }

    public void setLabelSearch() {
    	if (DEBUG_LOCAL || DEBUG.SEARCH) Log.debug("setLabelSearch()");

        searchType = SEARCH_ONLY_LABELS;

        singleLine = false;
        SearchTermsTableModel model = (SearchTermsTableModel) searchTermsTbl.getModel();
        SearchHeaderTableModel headerModel = (SearchHeaderTableModel) searchHeaderTbl
        .getModel();
        headerModel.setColumns(3);
        buttonColumn = 2;
        comboColumn = 1;
        valueColumn = 0;
        categoryColumn = -1;
        conditionColumn = -1;
        model.setColumns(3);
        headerButtonColumn = 2;
        headerComboColumn = 1;
        headerCategoryColumn = -1;
        headerValueColumn = 0;
        adjustColumnModel();
        adjustHeaderTableColumnModel();
        //termsAction = new SearchAction(searchTerms);
        termsAction.setBasic(true);
        termsAction.setTextOnly(false);
        termsAction.setMetadataOnly(false);
        termsAction.setOperator(getSelectedOperator());
        termsAction.setEverything(false);
        // termsAction.setOperator(andOrGroup.getSelection().getModel().getActionCommand());
    }

    /** "keywords" search, which means leave OUT node fields like labels and notes */
    public void setAllMetadataSearch() {
    	if (DEBUG_LOCAL || DEBUG.SEARCH) Log.debug("setAllMetadataSearch()");

        searchType = SEARCH_ONLY_KEYWORDS;

        singleLine = false;
        SearchTermsTableModel model = (SearchTermsTableModel) searchTermsTbl.getModel();
        SearchHeaderTableModel headerModel = (SearchHeaderTableModel) searchHeaderTbl
        .getModel();
        headerModel.setColumns(3);
        buttonColumn = 2;
        comboColumn = 1;
        valueColumn = 0;
        categoryColumn = -1;
        conditionColumn = -1;
        headerButtonColumn = 2;
        headerComboColumn = 1;
        headerCategoryColumn = -1;
        headerValueColumn = 0;
        model.setColumns(3);
        adjustColumnModel();
        adjustHeaderTableColumnModel();
        // termsAction = new SearchAction(searchTerms);

        termsAction.setBasic(false);
        termsAction.setTextOnly(true);
        termsAction.setMetadataOnly(true);
        termsAction.setOperator(getSelectedOperator());
        termsAction.setEverything(false);
        // termsAction.setOperator(andOrGroup.getSelection().getModel().getActionCommand());
    }

    public void setConditionSearch() {
    	if (DEBUG_LOCAL || DEBUG.SEARCH) Log.debug("setConditionSearch()");

        singleLine = false;
        SearchTermsTableModel model = (SearchTermsTableModel) searchTermsTbl.getModel();
        SearchHeaderTableModel headerModel = (SearchHeaderTableModel) searchHeaderTbl
        .getModel();
        headerModel.setColumns(3);
        buttonColumn = 4;
        comboColumn = 3;
        valueColumn = 2;
        categoryColumn = 0;
        conditionColumn = 1;
        model.setColumns(5);
        adjustColumnModel();
        adjustHeaderTableColumnModel();
        //termsAction = new SearchAction(searchTerms);
        termsAction.setBasic(false);
        termsAction.setTextOnly(false);
        termsAction.setMetadataOnly(false);
        termsAction.setOperator(getSelectedOperator());
        termsAction.setEverything(false);
        // termsAction.setOperator(andOrGroup.getSelection().getModel().getActionCommand());
    }

    public void setAllSearch() {
    	if (DEBUG_LOCAL || DEBUG.SEARCH) Log.debug("setAllSearch()");

        SearchTermsTableModel model = (SearchTermsTableModel) searchTermsTbl.getModel();
        buttonColumn = 2;
        comboColumn = 1;
        valueColumn = 0;
        categoryColumn = -1;
        conditionColumn = -1;
        model.setColumns(3);
        headerButtonColumn = 2;
        headerComboColumn = 1;
        headerCategoryColumn = -1;
        headerValueColumn = 0;
        adjustColumnModel();
        SearchHeaderTableModel headerModel = (SearchHeaderTableModel) searchHeaderTbl
        .getModel();
        headerModel.setColumns(3);
        adjustHeaderTableColumnModel();
        
        // allSearchField.setText("FF");

        // allSearch.setResultsType(resultsTypeChoice.getSelectedItem().toString());
        // allSearch = new SearchAction(allSearchField);

        singleLine = true;
    }

    class SearchTermsTableHeaderRenderer extends DefaultTableCellRenderer {
        public java.awt.Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int col) {
            JLabel comp = new JLabel();
            comp.setFont(tufts.vue.gui.GUI.LabelFace);            
            if (col == headerButtonColumn) {
                comp.setIcon(tufts.vue.VueResources.getIcon("metadata.editor.add.up"));
            } else if (table.getModel().getColumnCount() == 3
                    && col == headerValueColumn) {
                if (searchType == SEARCH_EVERYTHING) {
                    comp.setText(VueResources.getString("advancedSearch.searcheverything"));
                }

                if (searchType == SEARCH_ONLY_LABELS) {
                    comp.setText(VueResources.getString("advancedSearch.label"));
                }

                if (searchType == SEARCH_ONLY_KEYWORDS) {
                    comp.setText(VueResources.getString("advancedSearch.keywords"));
                }
                if(searchType == SEARCH_WITH_CATEGORIES){                    
                    comp.setText(VueResources.getString("advancedSearch.category"));    
                }                

            } else if ((table.getModel().getColumnCount() == 4)
                    && col == headerCategoryColumn) {                
                comp.setText(VueResources.getString("advancedSearch.category"));
            } else if ((table.getModel().getColumnCount() == 4 )
                    && col == headerValueColumn){
                comp.setText(VueResources.getString("advancedSearch.keywords"));
            }

            comp.setOpaque(true);
            comp.setBackground(MetadataSearchMainGUI.this.getBackground());

            if (DEBUG_LOCAL || DEBUG.BOXES) {
            	comp.setBackground(Color.YELLOW);
            }

            return comp;
        }
    }

     class SearchTermsTableRenderer extends DefaultTableCellRenderer {
        JPanel comp = new JPanel();
        JTextField field = new JTextField();

        public SearchTermsTableRenderer() {
            comp.setLayout(new java.awt.BorderLayout());
            field.setFont(tufts.vue.gui.GUI.LabelFace);

            if (DEBUG_LOCAL || DEBUG.BOXES) {
            	comp.setBackground(Color.YELLOW);
            	comp.setOpaque(true);
            }

            comp.add(field);
        }

        public java.awt.Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int col) {
            if (col == (valueColumn)) {
                String val = ((String[]) searchTerms.get(row).getObject())[1];
                field.setText(val);
                return comp;
            } else {
                return createRendererComponent(table, value, row, col);
            }
        }
    }

    class SearchTermsTableEditor extends DefaultCellEditor {
        JPanel comp;

        public SearchTermsTableEditor() {
            super(new JTextField());
            super.editorComponent.setName(getClass().getSimpleName());
            comp = new JPanel();
            comp.setLayout(new java.awt.BorderLayout());

            if (DEBUG_LOCAL || DEBUG.BOXES) {
            	comp.setBackground(Color.YELLOW);
            	comp.setOpaque(true);
            }
        }

        public java.awt.Component getTableCellEditorComponent(JTable table,
                Object value, boolean isSelected, int row, int col) {
            if (searchHeaderTbl != null && searchHeaderTbl.getCellEditor() != null){
                searchHeaderTbl.getCellEditor().stopCellEditing();
            }
            if(table.isEditing()){
             table.getCellEditor().stopCellEditing();
            }
            if (col == (valueColumn)) {
                JTextField field = (JTextField) super
                        .getTableCellEditorComponent(table, value, isSelected,
                                row, col);
                field.setFont(tufts.vue.gui.GUI.LabelFace);
                String val = ((String[]) searchTerms.get(row).getObject())[1];
                field.setText(val);

                field.addActionListener(new ActionListener(){
                    public void actionPerformed(ActionEvent actionEvent){
                        searchButton.doClick();
                    }
                });

                comp.add(field);
                return comp;
            } else {
                return createRendererComponent(table, value, row, col);
            }
        }

        public Object getCellEditorValue() {
            Object obj = super.getCellEditorValue();            
            return obj;
        }

        public boolean stopCellEditing() {            
            return super.stopCellEditing();
        }
    }
//    class SearchTermsTableRenderer extends DefaultTableCellRenderer {
//        public java.awt.Component getTableCellRendererComponent(JTable table,
//                Object value, boolean isSelected, boolean hasFocus, int row,
//                int col) {            
//            return createRendererComponent(table, value, row, col);
//        }
//    }
//
//    class SearchTermsTableEditor extends DefaultCellEditor {
//        public SearchTermsTableEditor() {
//            super(new JTextField());
//        }
//
//        public java.awt.Component getTableCellEditorComponent(JTable table,
//                Object value, boolean isSelected, int row, int col) {
//            return createRendererComponent(table, value, row, col);
//        }
//    }

    public java.awt.Component createRendererComponent(JTable table,
            Object value, final int row, int col) {
        JPanel comp = new JPanel();    
        if(table.isEditing()){
            //table.getCellEditor().stopCellEditing();
        }
        comp.setLayout(new java.awt.BorderLayout());
        if (col == (valueColumn)) {
            final JTextField field = new JTextField();
            field.setFont(tufts.vue.gui.GUI.LabelFace);
            field.addFocusListener(new FocusAdapter() {
                public void focusLost(FocusEvent fe) {                    
                    VueMetadataElement searchTerm = searchTerms.get(row);                    
                    // Object searchTerm = searchTerms.get(row).getObject();
                    if (searchTerm.getObject() instanceof String[]) {                        
                        String[] newStatement = {
                                ((String[]) searchTerms.get(row).getObject())[0],
                                field.getText(),
                                ((String[]) searchTerms.get(row).getObject())[2] };                        
                        searchTerm.setObject(newStatement);    
                        field.setText(field.getText());
                    }
                }                    
            });            
            String val = ((String[]) searchTerms.get(row).getObject())[1];            
            field.setText(val);
            // #VUE-887 -- whoops -- metadataeditor not here
            comp.add(field);
        } else if (col == (categoryColumn)) {
            final JComboBox categories = new JComboBox();
            categories.setFont(tufts.vue.gui.GUI.LabelFace);
            categories.setModel(new CategoryComboBoxModel());
            categories.setRenderer(new CategoryComboBoxRenderer());

            categories.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent ie) {
                    if (ie.getStateChange() == ItemEvent.SELECTED) {

                        if (!(categories.getSelectedItem() instanceof OntType))
                            return;

                        OntType type = (OntType) categories.getSelectedItem();
                        String[] statement = {
                                type.getBase() + "#" + type.getLabel(),
                                searchTerms.get(row).getValue(),
                                ((String[]) (searchTerms.get(row).getObject()))[2] };

                        VueMetadataElement ele = new VueMetadataElement();
                        ele.setObject(statement);
                        ele.setType(VueMetadataElement.SEARCH_STATEMENT);                        
                        searchTerms.set(row, ele);

                    }
                }
            });

            Object currValueObject = searchTerms.get(row).getObject();
            if (currValueObject instanceof String[]) {
                findCategory(searchTerms.get(row).getKey(), row, col,
                        categories);
            }
            comp.add(categories);
        } else if (col == buttonColumn) {
            JLabel buttonLabel = new MDDeleteLabel();
            buttonLabel.setIcon(tufts.vue.VueResources.getIcon("metadata.editor.delete.up"));
            comp.add(buttonLabel);
        } else if (col == conditionColumn) {

            // conditionCombo does not appear to be UI deployed at moment...
            
            // String [] conditions = {"starts with","contains"};
            String[] conditions = { "contains", "starts with" };
            final JComboBox conditionCombo = new JComboBox(conditions);
            conditionCombo.setFont(tufts.vue.gui.GUI.LabelFace);
            conditionCombo.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent ie) {
                    if (ie.getStateChange() == ItemEvent.SELECTED) {
                        String condition = conditionCombo.getSelectedItem()
                                .toString();
                        if (condition.equals("starts with")) {
                            condition = "STARTS_WITH";
                        } else {
                            condition = "CONTAINS";
                        }

                        String[] statement = {
                                ((String[]) (searchTerms.get(row).getObject()))[0],
                                searchTerms.get(row).getValue(), condition };
                        VueMetadataElement ele = new VueMetadataElement();
                        ele.setObject(statement);
                        ele.setType(VueMetadataElement.SEARCH_STATEMENT);
                        searchTerms.set(row, ele);
                    }
                }
            });
            String currentCondition = ((String[]) (searchTerms.get(row)
                    .getObject()))[2];
            if (currentCondition.equals("CONTAINS"))
                conditionCombo.setSelectedIndex(1);
            comp.add(conditionCombo);
        } 

        if (DEBUG_LOCAL || DEBUG.BOXES) {
        	comp.setBackground(Color.YELLOW);
        	comp.setOpaque(true);
        }

        return comp;
    }

    public void findCategory(Object currValue, int row, int col,
            JComboBox categories) {

        // System.out.println("MetadataSearchGUI: find category");

        if (!(currValue instanceof String)) {
            if (DEBUG_LOCAL) {
                System.out
                        .println("MetadataSearchGUI - findCategory - currValue not instance of String -- returning ");
            }
            return;
        }

        int n = categories.getModel().getSize();
        for (int i = 0; i < n; i++) {

            Object item = categories.getModel().getElementAt(i);

            if (DEBUG_LOCAL) {
                System.out.println("i: " + i);
                if (item instanceof OntType) {
                    System.out
                            .println("MetadataSearchGUI - find category - item.getBase() and currValue  - "
                                    + "i :"
                                    + i
                                    + ":"
                                    + ((OntType) item).getBase()
                                    + ","
                                    + currValue);
                    System.out
                            .println("MetadataSearchGUI - ((OntType)item).getBase() + # + ((OntType)item).getLabel() "
                                    + ((OntType) item).getBase()
                                    + "#"
                                    + ((OntType) item).getLabel());
                }
            }

            if (item instanceof OntType
                    && (((OntType) item).getBase() + "#" + ((OntType) item)
                            .getLabel()).equals(currValue)) {
                // System.out.println("MetadataSearchGUI: find category - found - "
                // + i);
                categories.setSelectedIndex(i);
            }

        }

    }

    class OptionsPanel extends JPanel {
        GridBagLayout optionsGrid;
        GridBagConstraints optionsConstraints;

        // in case easy access to the labels is ever needed:
        // they can also be found in the layout
        // List<JLabel> labels = new ArrayList<JLabel>();

        // in case easy access to the combo boxes is ever needed:
        // they can also be found in the layout
        // ** now needed for switching choices in the results combo
        // on location switch

        OptionsPanel(LayoutManager layout) {
        	super(layout);
        }


        public void switchChoices(int i, Object[] choices) {
            final JComboBox box = comboBoxes.get(i);
            box.removeAllItems();

            for (int j = 0; j < choices.length; j++)
                box.addItem(choices[j]);
            
            box.setEnabled(choices.length > 1);

            if (choices.length > 2 && i == RESULTS) {
                box.setSelectedIndex(2);
            } else if (choices.length > 1) {
                box.setSelectedItem(0);
            }
        }
    }


    class PopupListener extends MouseAdapter {        
        public void mousePressed(MouseEvent e) {
           //Bug #: 1302
            if(/*e.getClickCount()==2 &&*/ e.getX()>(searchResultTbl.getWidth()-40)){                 
                runSavedSearch();
            }            
            showPopup(e);
        }
        public void mouseReleased(MouseEvent e) {
            showPopup(e);
        }
        private void showPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                popupMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }


    public void fillSavedSearch() {     
    	
    	List savedList =null;
    	if (VUE.getActiveMap() !=null) {
            savedList = VUE.getActiveMap().getSearchArrLst();
            if (DEBUG.SEARCH && DEBUG.RDF) {
                Log.debug("saved searches:", new Throwable("HERE"));
                Util.dump(savedList);
            }
        }
        if(savedList!=null){
            searchResultModel.setData((ArrayList)savedList);
        }else{			
            searchResultModel.setData(null);
        }
    }
    
    /** Handle any click at the far right of the saved searches table (the _run_ "button") */
    public void runSavedSearch()
    {
        final int selectedRow = searchResultTbl.getSelectedRow();
        final SearchData data = searchResultModel.getSearchData(selectedRow);

        if (DEBUG.SEARCH) Log.debug("runSavedSearch; selectedRow=" + selectedRow + "; data:\n" + Util.tags(data));

        //searchTerms = data.getDataList();             
        if (data.getDataList() != null) {
            // What would be a saved search w/out a data-list, and what would
            // the rest of this code go on to do if it didn't have one??
            // It's re-using the old SearchAction...
            termsAction = new SearchAction(data.getDataList());
            // Does this mean if the saved search was somehow of just a type,
            // but had no terms, we'd what -- just search with the saved OP???
        }

        termsAction.setOperator(data.logicalOp());
        termsAction.setResultAction(data.resultOp());

        // Christ: this means that all existing saved searches up till now (Summer 2012) have used the localized values
        // for search action result type, and wont restore properly under different localizations.
        
        if (termsAction.getResultAction() == SearchAction.RA_SELECT) {
            // Special case: if saved type was select, allow overriding of the saved action with
            // the user selected action.  This is a cheap squirrely way of allowing a nice feature:
            // using a different action with a saved search, which isn't something we have UI for,
            // or are likely to.
            termsAction.setResultAction(getChosen(choiceResult));
        }

        termsAction.setParamsByType(searchType);

        termsAction.fire(this, "runSavedSearch");
    }


    public void activeChanged(ActiveEvent<LWMap> e) {
    	fillSavedSearch();
    }
}
