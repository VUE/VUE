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

    public final static int HALF_GUTTER = 4;
    public final static int GUTTER = HALF_GUTTER * 2;

    public final static String AND = VueResources.getString("searchgui.and");
    public final static String OR = VueResources.getString("searchgui.or");

    public final static String SELECTED_MAP_STRING = VueResources.getString("searchgui.currentmap");
    public final static String ALL_MAPS_STRING = VueResources.getString("searchgui.allopenmaps");

    public final static String SEARCH_EVERYTHING = VueResources.getString("searchgui.searcheverything");
    public final static String SEARCH_LABELS_ONLY = VueResources.getString("searchgui.labels");
    public final static String SEARCH_ALL_KEYWORDS = VueResources.getString("searchgui.keywords");
    public final static String SEARCH_CATEGORIES_AND_KEYWORDS = VueResources.getString("searchgui.categories_keywords");
    
    private static final String[] searchTypes = { SEARCH_EVERYTHING,
                                                  SEARCH_LABELS_ONLY,
                                                  SEARCH_ALL_KEYWORDS,
                                                  SEARCH_CATEGORIES_AND_KEYWORDS };

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
    private int searchType = EVERYTHING;
    
    private static final String[] andOrTypes = {
        VueResources.getString("searchgui.or"),
        VueResources.getString("searchgui.and") };
    
    private static final boolean DEBUG_LOCAL = false;
    
    private static final Object[] AllOpenMapsResultsTypes = { SearchAction.RA_COPY };
  //private static final String[] AllOpenMapsResultsTypes = { "Create New Map" };

    // combo box numbers within optionsPanel
    public final static int TYPES = 0;
    public final static int LOCATIONS = 1;
    public final static int RESULTS = 2;

    // search types
    public final static int EVERYTHING = 0;
    public final static int LABEL = 1;
    public final static int KEYWORD = 2;
    public final static int CATEGORY = 3;
    
    private static final String[] locationTypes = { SELECTED_MAP_STRING, ALL_MAPS_STRING };
    private boolean singleLine = false;

    private final static Object[] CurrentMapResultsTypes = SearchAction.ResultActionTypes;

    static public final int ANY_MODE = 0;
    static public final int ALL_MODE = 1;
    static public final int NOT_ANY_MODE = 2;
    static public final int NONE_MODE = 3;

    private MetaSearchPanel mInfoPanel = null;

    private MetadataPanel metadataPanel = null;
    private SearchResultTableModel searchResultModel = new SearchResultTableModel();
    VueTextPane mDescriptionEditor = null;
    JTextField mAuthorEditor = null;

    private WidgetStack mapInfoStack = null;
    static AbstractAction saveSearchAction;
    static AbstractAction runSearchAction;
    static AbstractAction renameAction;
    static AbstractAction deleteAction;
    private List<SearchData> searchDataList = new ArrayList<SearchData>();
    private List<JComboBox> comboBoxes = new ArrayList<JComboBox>();
    private JPopupMenu popupMenu = new JPopupMenu();
    private final String RENAME_STR = VueResources.getString("searchgui.rename");
    private final String DELETE_STR = VueResources.getString("searchgui.delete");
    private final String RESET_STR = VueResources.getString("searchgui.resetmap");
    private final String SEARCH_STR = VueResources.getString("searchgui.search");
    private final String SAVED_SEARCH_STR = VueResources.getString("searchgui.savedsearches");
    private final String SAVE_SEARCH_STR = VueResources.getString("searchgui.savesearch");
    private final String RUN_SEARCH_STR = VueResources.getString("searchgui.runsearch");
    private String strAndOrType  = VueResources.getString("searchgui.or");

    private JPanel topPanel;
    private OptionsPanel optionsPanel;
    private JPanel linePanel;
    private JPanel tablePanel;
    private JPanel searchPanel;
    private JLabel searchTypeLbl;
    private JLabel mapsLbl;            
    private JLabel resultsLbl;            
    public JComboBox searchTypeCmbBox;
    public JComboBox mapCmbBox;
    public JComboBox resultCmbBox;
    private JTable searchTermsTbl;
    private JTable searchHeaderTbl;
    private JTable searchResultTbl;
    private final JComboBox andOrCmbBox;
    //private final JComponent andOrCmbBox;
    private final JComponent macLeopardAndOr;
    private JButton saveButton;
    private JButton resetButton;
    private JButton searchButton;

    public MetadataSearchMainGUI(DockWindow w) {
        super();

        if (Util.isMacLeopard() && DEBUG.Enabled && true) {
            //final String bStyle = "segmented";
            //final String bStyle = "segmentedCapsule";
            //final String bStyle = "segmentedTextured";
            final String bStyle = "segmentedRoundRect";
            final Box box = Box.createHorizontalBox();
            // See https://developer.apple.com/library/mac/#technotes/tn2007/tn2196.html
            final ButtonGroup bg = new ButtonGroup();
            AbstractButton b;
            box.add(b=GUI.createSegmentButton(bStyle, "first", bg, "And"));
            b.setFocusPainted(false);
            b.setFont(tufts.vue.gui.GUI.LabelFace);
            // box.add(b=GUI.createSegmentButton(bStyle, "middle", bg, "Wha"));
            //b.setFocusPainted(false);
            // b.setFont(tufts.vue.gui.GUI.LabelFace);
            box.add(b=GUI.createSegmentButton(bStyle, "last", bg, "Or"));
            b.setFocusPainted(false);
            b.setFont(tufts.vue.gui.GUI.LabelFace);
            b.setSelected(true);
            macLeopardAndOr = box;
            andOrCmbBox = new JComboBox(andOrTypes);
            //andOrCmbBox = null;
        } else {
            andOrCmbBox = new JComboBox(andOrTypes);
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
                
            	if(searchTermsTbl.isEditing()){
            		searchTermsTbl.getCellEditor().stopCellEditing();
                }
                final SearchData data = new SearchData();
                searchDataList = new ArrayList<SearchData>();   
                final int rowCount = searchResultModel.getRowCount();

                String searchName = (String)
                    VueUtil.input(null,
                                  VueResources.getString("searchgui.entersearchname"), null,
                                  JOptionPane.PLAIN_MESSAGE, null,
                                  (VueResources.getString("searchgui.search") + " "+ (rowCount+1)));
                
            	if(searchName!=null && searchName.trim().length()==0){
                    searchName = VueResources.getString("searchgui.search") + " "+ (searchResultModel.getRowCount()+1);
            	}                
                if(searchName!= null){
                    data.setSearchSaveName(searchName);
                    data.setSearchType(searchTypeCmbBox.getSelectedItem().toString().trim());
                    data.setMapType(mapCmbBox.getSelectedItem().toString().trim());
                    data.setResultType(resultCmbBox.getSelectedItem().toString().trim());                    
                    data.setAndOrType(strAndOrType);
                                          
                    data.setDataList(searchTerms);
                    searchDataList.add(data);                    
                    searchResultModel.addRow(data);
                    searchTerms = new ArrayList<VueMetadataElement>();
                    if(searchResultModel.getData()!=null && VUE.getActiveMap()!=null){
                    	VUE.getActiveMap().setSearchArrLst(searchResultModel.getData());
                    }
                }
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

    // ///////////////
    // Inner Classes
    // //////////////////

    /**
     * InfoPanel This is the tab panel for displaying Map Info
     * 
     **/
    public class MetaSearchPanel extends JPanel implements
            PropertyChangeListener, FocusListener {
        public MetaSearchPanel() {            
            ItemListener searchTypesListener = new ItemListener() {
                public void itemStateChanged(ItemEvent ie) {
                    if (ie.getStateChange() == ItemEvent.SELECTED) {

                        // This would all better be handled via events instead
                        // of this direct calling into SearchTextField hack stuff.
                        
                        if (ie.getItem().equals(SEARCH_EVERYTHING)) {
                            setEverythingSearch();
                            SearchTextField.searcheveryWhereMenuItem.setSelected(true);	
                            // SearchTextField.labelMenuItem.setSelected(false);
                            // SearchTextField.keywordMenuItem.setSelected(false);
                            // SearchTextField.categoryKeywordMenuItem.setSelected(false); 
                            SearchTextField.updateSearchType();
                        }

                        if (ie.getItem().equals(SEARCH_LABELS_ONLY)) {
                            setLabelSearch();
                            SearchTextField.labelMenuItem.setSelected(true);
                            // SearchTextField.searcheveryWhereMenuItem.setSelected(false);	
                            // SearchTextField.labelMenuItem.setSelected(true);
                            // SearchTextField.keywordMenuItem.setSelected(false);
                            // SearchTextField.categoryKeywordMenuItem.setSelected(false); 
                            SearchTextField.updateSearchType();
                        }

                        if (ie.getItem().equals(SEARCH_ALL_KEYWORDS)) {
                            setAllMetadataSearch();
                            SearchTextField.keywordMenuItem.setSelected(true);
                            // SearchTextField.searcheveryWhereMenuItem.setSelected(false);	
                            // SearchTextField.labelMenuItem.setSelected(false);
                            // SearchTextField.keywordMenuItem.setSelected(true);
                            // SearchTextField.categoryKeywordMenuItem.setSelected(false); 
                            SearchTextField.updateSearchType();
                        }

                        if (ie.getItem().equals(SEARCH_CATEGORIES_AND_KEYWORDS)) {
                            setCategorySearch();
                            SearchTextField.categoryKeywordMenuItem.setSelected(true);                          
                            // SearchTextField.searcheveryWhereMenuItem.setSelected(false);	
                            // SearchTextField.labelMenuItem.setSelected(false);
                            // SearchTextField.keywordMenuItem.setSelected(false);
                            // SearchTextField.categoryKeywordMenuItem.setSelected(true);                          
                            SearchTextField.updateSearchType();
                        }
                    }
                }
            };

            ItemListener locationChoiceListener = new ItemListener() {
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

            ItemListener resultsTypeListener = new ItemListener() {
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

            optionsPanel = new OptionsPanel(new GridBagLayout());
            GridBagConstraints optionsPanelGBC = new GridBagConstraints();
            optionsPanelGBC.fill = GridBagConstraints.HORIZONTAL;
            Insets	labelInsets = new Insets(HALF_GUTTER, HALF_GUTTER, HALF_GUTTER, 0),
            		textFieldInsets = new Insets(HALF_GUTTER, HALF_GUTTER, HALF_GUTTER, HALF_GUTTER);

            // First Row
            optionsPanelGBC.gridy = 0;

            //searchTypeLbl = new JLabel(VueResources.getString("searchgui.searchtype"), SwingConstants.RIGHT);            
            searchTypeLbl = new JLabel(VueResources.getString("searchgui.search")+":", SwingConstants.RIGHT); // short & sweet
            searchTypeLbl.setFont(tufts.vue.gui.GUI.LabelFace);
            optionsPanelGBC.gridx = 0;
            optionsPanelGBC.insets = labelInsets;
            optionsPanelGBC.weightx = 0.0;
            optionsPanel.add(searchTypeLbl, optionsPanelGBC);

            searchTypeCmbBox = getCombo(searchTypes, searchTypesListener);
            searchTypeCmbBox.setPreferredSize(searchTypeCmbBox.getMinimumSize());
            optionsPanelGBC.gridx = 1;
            optionsPanelGBC.insets = textFieldInsets;
            optionsPanelGBC.weightx = 1.0;
            optionsPanel.add(searchTypeCmbBox, optionsPanelGBC);

            // Second Row
            optionsPanelGBC.gridy = 1;

            mapsLbl = new JLabel(VueResources.getString("searchgui.maps"), SwingConstants.RIGHT);            
            mapsLbl.setFont(tufts.vue.gui.GUI.LabelFace);
            optionsPanelGBC.gridx = 0;
            optionsPanelGBC.insets = labelInsets;
            optionsPanelGBC.weightx = 0.0;
            optionsPanel.add(mapsLbl, optionsPanelGBC);

            mapCmbBox = getCombo(locationTypes, locationChoiceListener);
            mapCmbBox.setPreferredSize(mapCmbBox.getMinimumSize());
            optionsPanelGBC.gridx = 1;
            optionsPanelGBC.insets = textFieldInsets;
            optionsPanelGBC.weightx = 1.0;
            optionsPanel.add(mapCmbBox, optionsPanelGBC);

            // Third Row
            optionsPanelGBC.gridy = 2;

            resultsLbl = new JLabel(GUI.ensureColon(VueResources.getString("searchgui.results")), SwingConstants.RIGHT);
            resultsLbl.setFont(tufts.vue.gui.GUI.LabelFace);
            optionsPanelGBC.gridx = 0;
            optionsPanelGBC.insets = labelInsets;
            optionsPanelGBC.weightx = 0.0;
            optionsPanel.add(resultsLbl, optionsPanelGBC);

            resultCmbBox = getCombo(CurrentMapResultsTypes, resultsTypeListener);
            resultCmbBox.setPreferredSize(resultCmbBox.getMinimumSize());
            optionsPanelGBC.gridx = 1;
            optionsPanelGBC.insets = textFieldInsets;
            optionsPanelGBC.weightx = 1.0;
            optionsPanel.add(resultCmbBox, optionsPanelGBC);

            if (DEBUG.BOXES || DEBUG_LOCAL) {
            	searchTypeLbl.setBackground(Color.YELLOW);
            	searchTypeLbl.setOpaque(true);
            	searchTypeCmbBox.setBackground(Color.YELLOW);
            	searchTypeCmbBox.setOpaque(true);
            	mapsLbl.setBackground(Color.YELLOW);
            	mapsLbl.setOpaque(true);
            	mapCmbBox.setBackground(Color.YELLOW);
            	mapCmbBox.setOpaque(true);
            	resultsLbl.setBackground(Color.YELLOW);
            	resultsLbl.setOpaque(true);
            	resultCmbBox.setBackground(Color.YELLOW);
            	resultCmbBox.setOpaque(true);
            	optionsPanel.setBackground(Color.BLUE);
            }

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
            searchTermsTbl.setDefaultRenderer(java.lang.Object.class,
                    new SearchTermsTableRenderer());
            searchTermsTbl.setDefaultEditor(java.lang.Object.class,
                    new SearchTermsTableEditor());
            ((DefaultCellEditor) searchTermsTbl
                    .getDefaultEditor(java.lang.Object.class))
                    .setClickCountToStart(1);
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
            saveButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {   
                	if(searchTermsTbl.isEditing()){
                		searchTermsTbl.getCellEditor().stopCellEditing();
                    }

                	SearchData data = new SearchData();
                    searchDataList = new ArrayList<SearchData>();                    
                    String searchName = getSearchName(searchResultModel.getRowCount());

                    if (searchName != null) {
	                    data.setSearchSaveName(searchName);
	                    data.setSearchType(searchTypeCmbBox.getSelectedItem().toString().trim());
	                    data.setMapType(mapCmbBox.getSelectedItem().toString().trim());
	                    data.setResultType(resultCmbBox.getSelectedItem().toString().trim());                    
	                    data.setAndOrType(strAndOrType);     
	                                          
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
                    if (searchTermsTbl.isEditing()) {
						searchTermsTbl.getCellEditor().stopCellEditing();
					}

					termsAction = new SearchAction(searchTerms);
                                        termsAction.setResultAction(resultCmbBox.getSelectedItem());

					termsAction.setLocationType(
							(mapCmbBox.getSelectedItem().toString().trim().equals(ALL_MAPS_STRING) ?
							SearchAction.SEARCH_ALL_OPEN_MAPS :
							SearchAction.SEARCH_SELECTED_MAP));

					if (searchTypeCmbBox.getSelectedItem().toString().trim().equals(SEARCH_EVERYTHING)) {
						searchType = EVERYTHING;
						termsAction.setBasic(false);
						termsAction.setTextOnly(true);
						termsAction.setMetadataOnly(false);
						termsAction.setOperator(getSelectedOperator());
						termsAction.setEverything(true);
						// termsAction.setOperator(andOrGroup.getSelection().getModel().getActionCommand());
					} else if (searchTypeCmbBox.getSelectedItem().toString().trim().equals(SEARCH_LABELS_ONLY)) {
						searchType = LABEL;
						termsAction.setBasic(true);
						termsAction.setTextOnly(false);
						termsAction.setMetadataOnly(false);
						termsAction.setOperator(getSelectedOperator());
						termsAction.setEverything(false);
						// termsAction.setOperator(andOrGroup.getSelection().getModel().getActionCommand());
					} else if (searchTypeCmbBox.getSelectedItem().toString().trim().equals(SEARCH_ALL_KEYWORDS)) {
						searchType = KEYWORD;
						termsAction.setBasic(false);
						termsAction.setTextOnly(true);
						termsAction.setMetadataOnly(true);
						termsAction.setOperator(getSelectedOperator());
						termsAction.setEverything(false);
						// termsAction.setOperator(andOrGroup.getSelection().getModel().getActionCommand());
					} else if (searchTypeCmbBox.getSelectedItem().toString().trim().equals(SEARCH_CATEGORIES_AND_KEYWORDS)) {
						searchType = CATEGORY;
						termsAction.setBasic(false);
						termsAction.setTextOnly(false);
						termsAction.setMetadataOnly(false);
						termsAction.setOperator(getSelectedOperator());
						termsAction.setEverything(false);
						// termsAction.setOperator(andOrGroup.getSelection().getModel().getActionCommand());
					} else {
						searchType = EVERYTHING;
						termsAction.setBasic(false);
						termsAction.setTextOnly(true);
						termsAction.setMetadataOnly(false);
						termsAction.setOperator(getSelectedOperator());
						termsAction.setEverything(true);
						// termsAction.setOperator(andOrGroup.getSelection().getModel().getActionCommand());
					}

					if (DEBUG_LOCAL) {
						System.out.println("MetadataSearchMainGUI.searchButton's actionPerformed()");
					}

                                        termsAction.actionPerformed(new ActionEvent(this, 0, "searchFromPanelButton"));

					// // Perform the action.
					// JButton btn = new JButton(termsAction);
					// btn.doClick();
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


        public String getSearchName(int rowCount){        	
        	String inputValue = (String)VueUtil.input(null,
        			VueResources.getString("searchgui.entersearchname"),
        			null, JOptionPane.PLAIN_MESSAGE, null,
        			(VueResources.getString("searchgui.search") + " "+ (rowCount+1)));

        	if (inputValue != null && inputValue.trim().length() == 0) {
        		inputValue = VueResources.getString("searchgui.search") + " "+ (rowCount+1);
        	}

            return inputValue;
        }


        // public void setResultsTypeInActions(String resultsTypeChoice) {
        //     // String resultsTypeChoice = e.getItem().toString();
        //     if (resultsTypeChoice != null /*&& allSearch != null*/ && termsAction != null) {
        //         //allSearch.setResultsType(resultsTypeChoice);
        //         termsAction.setResultsType(resultsTypeChoice);
        //     }
        // }

        private JComboBox getCombo(Object[] choices, ItemListener listener) {
            JComboBox newCombo = new JComboBox(choices);
            newCombo.setFont(tufts.vue.gui.GUI.LabelFace);
            newCombo.addItemListener(listener);
            comboBoxes.add(newCombo);
            return newCombo;
        }

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
        JComboBox combo; 
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

            //if(isSelected){                
                if(this.combo.getSelectedItem().toString().equals(VueResources.getString("searchgui.and"))){                    
                    strAndOrType = VueResources.getString("searchgui.or");
                }else{                    
                    strAndOrType = VueResources.getString("searchgui.and");
                }                                    
            //} 
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
        searchType = CATEGORY;
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

        searchType = EVERYTHING;
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
    
    public String getSelectedOperator() {        
        if(strAndOrType.equals(VueResources.getString("searchgui.and"))){
            return SearchAction.AND;
        }else{
            return SearchAction.OR;
        }            
    }

    public void setLabelSearch() {
    	if (DEBUG_LOCAL || DEBUG.SEARCH) Log.debug("setLabelSearch()");

        searchType = LABEL;

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

        searchType = KEYWORD;

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
                if (searchType == EVERYTHING) {
                    comp.setText(VueResources.getString("advancedSearch.searcheverything"));
                }

                if (searchType == LABEL) {
                    comp.setText(VueResources.getString("advancedSearch.label"));
                }

                if (searchType == KEYWORD) {
                    comp.setText(VueResources.getString("advancedSearch.keywords"));
                }
                if(searchType == CATEGORY){                    
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
        if(data.getDataList() != null)
            termsAction = new SearchAction(data.getDataList());

        //int iAndOr = 0;
        final String iAndOr;
        String andOrStr = data.getAndOrType();
        if(andOrStr.equals(VueResources.getString("searchgui.and"))){
            iAndOr = SearchAction.AND;
        }else{
            iAndOr =  SearchAction.OR;
        }

        // Christ: this means that all existing saved searches up till now (Summer 2012) have used the localized values
        // for search action result type, and wont restore properly under different localizations.
        
        termsAction.setResultActionFromSaved(data.getResultType());                 
        termsAction.setOperator(iAndOr);

         String searchType = data.getSearchType();
         if (searchType.equals(SEARCH_LABELS_ONLY)) {
             termsAction.setBasic(true);
             termsAction.setTextOnly(false);
             termsAction.setMetadataOnly(false);
             termsAction.setEverything(false);
         } else if (searchType.equals(SEARCH_ALL_KEYWORDS)) {
             termsAction.setBasic(false);
             termsAction.setTextOnly(true);
             termsAction.setMetadataOnly(true);
             termsAction.setEverything(false);
         } else if (searchType.equals(SEARCH_CATEGORIES_AND_KEYWORDS)) {
             termsAction.setBasic(false);
             termsAction.setTextOnly(false);
             termsAction.setMetadataOnly(false);
             termsAction.setEverything(false);
         } else /* if (searchType.equals(SEARCH_EVERYTHING)) */ {
             termsAction.setBasic(false);
             termsAction.setTextOnly(true);
             termsAction.setMetadataOnly(false);
             termsAction.setEverything(true);
         }

         // termsAction.setOperator(andOrGroup.getSelection().getModel().getActionCommand());

         if (DEBUG_LOCAL) {
        	 System.out.println("MetadataSearchMainGUI.searchButtonAction()");
         }

         termsAction.actionPerformed(new ActionEvent(this, 0, "searchFromMethod"));
         
         // JButton btn = new JButton();
         // btn.setAction(termsAction);
         // btn.doClick(); 
    }


    public void activeChanged(ActiveEvent<LWMap> e) {
    	fillSavedSearch();
    }
}
