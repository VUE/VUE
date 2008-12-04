/*
 * Copyright 2003-2008 Tufts University  Licensed under the
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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import tufts.vue.gui.DockWindow;
import tufts.vue.gui.VueTextPane;
import tufts.vue.gui.Widget;
import tufts.vue.gui.WidgetStack;
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
 * @version $Revision: 1.11 $ / $Date: 2008-12-04 18:56:20 $ / $Author: Sheejo
 *          Rapheal $
 * 
 */
public class MetadataSearchMainGUI extends JPanel

{
	private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger
			.getLogger(MetadataSearchMainGUI.class);
	public final static String AND = "and";
	public final static String OR = "or";

	public final static String SELECTED_MAP_STRING = "Current Map";
	public final static String ALL_MAPS_STRING = "All Open Maps";

	public final static String SEARCH_EVERYTHING = "Search everything";
	public final static String SEARCH_LABELS_ONLY = "Labels";
	public final static String SEARCH_ALL_KEYWORDS = "Keywords";
	public final static String SEARCH_CATEGORIES_AND_KEYWORDS = "Categories + Keywords";
	private String[] searchTypes = { SEARCH_EVERYTHING, SEARCH_LABELS_ONLY,
			SEARCH_ALL_KEYWORDS, SEARCH_CATEGORIES_AND_KEYWORDS };
	private JPanel linePanel;
	public final static int BUTTON_COL_WIDTH = 35;
	public final static int ROW_HEIGHT = 30;
	public final static int ROW_GAP = 7;
	private List<VueMetadataElement> searchTerms = new ArrayList<VueMetadataElement>();
	public final static int ROW_INSET = 5;
	private JTextField allSearchField = new JTextField();
	private SearchAction allSearch = new SearchAction(allSearchField);
	public final static int SHOW_OPTIONS = 1;
	public final static int HIDE_OPTIONS = 0;
	private JPanel buttonPanel;
	private JPanel fieldsInnerPanel;
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
	private JButton searchButton;
	private static String[] andOrTypes = { "and", "or" };
	private static final boolean DEBUG_LOCAL = false;
	private JPanel topPanel;
	private JPanel innerTopPanel;
	private OptionsPanel optionsPanel;
	private SearchAction termsAction;
	private String[] allOpenMapsResultsTypes = { "new map" };
	private JLabel optionsLabel;
	// combo box numbers within optionsPanel
	public final static int TYPES = 0;
	public final static int LOCATIONS = 1;
	public final static int RESULTS = 2;
	// search types
	public final static int EVERYTHING = 0;
	public final static int LABEL = 1;
	public final static int KEYWORD = 2;
	public final static int CATEGORY = 3;
	private JPanel fieldsPanel;
	private JButton advancedSearch;
	private String[] locationTypes = { SELECTED_MAP_STRING, ALL_MAPS_STRING };
	private JTable searchTermsTable;
	private boolean singleLine = false;

	private String[] currentMapResultsTypes = { "Show", "Hide", "Select",
			"Copy to new map" };
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
	private JComboBox searchTypeCmbBox;
	private JComboBox mapCmbBox;
	private JComboBox resultCmbBox;
	private List<SearchData> searchDataList = new ArrayList<SearchData>();
	private List<JComboBox> comboBoxes = new ArrayList<JComboBox>();
	private JPopupMenu popupMenu = new JPopupMenu();
	private final String RENAME_STR = "Rename";
	private final String DELETE_STR = "Delete";
	private final String SEARCH_STR = "Search";
	private final String SAVED_SEARCH_STR = "Saved Searches";
	private final String SAVE_SEARCH_STR = "Save Search";
	private final String RUN_SEARCH_STR = "Run Search";
	private JTable searchHeaderTbl;
	private JTable searchResultTbl;
    private JComboBox andOrCmbBox = new JComboBox(andOrTypes);
    private String strAndOrType  = "and";
	public MetadataSearchMainGUI(DockWindow w) {
		super();
		JPopupMenu popup = new JPopupMenu();
		mapInfoStack = new WidgetStack(SEARCH_STR);		
		// VUE.addActiveListener(LWMap.class, this);
		setMinimumSize(new Dimension(320, 300));
		setLayout(new BorderLayout());
		mInfoPanel = new MetaSearchPanel();
		mInfoPanel.setName(SEARCH_STR);
		metadataPanel = new MetadataPanel();
		metadataPanel.setName(SAVED_SEARCH_STR);
		Widget.setWantsScroller(mapInfoStack, true);
		adjustHeaderTableColumnModel();
		// mTabbedPane.addTab(metadataPanel.getName(),metadataPanel);
		mapInfoStack.addPane(mInfoPanel, 1f);
		mapInfoStack.addPane(metadataPanel, 2f);

		// Widget.setWantsScroller(mapInfoStack, true);
		saveSearchAction = new AbstractAction(SAVE_SEARCH_STR) {
			public void actionPerformed(ActionEvent e) {
				//System.err.println("Enter in Save>>>>");
			}
		};
		runSearchAction = new AbstractAction(RUN_SEARCH_STR) {
			public void actionPerformed(ActionEvent e) {
				//System.err.println("Enter in Run Search>>>>");
			}
		};
		renameAction = new AbstractAction(RENAME_STR) {
			public void actionPerformed(ActionEvent e) {
				searchResultModel.setEditableFlag(true);
				int selectedIndex = searchResultTbl.getSelectedRow();
				if (selectedIndex != -1){
					searchResultTbl.editCellAt(selectedIndex, 0);
					searchResultModel.setEditableFlag(false);
				}else{					
					JOptionPane.showMessageDialog((Component)mapInfoStack,
						    "Please select a Row",
						    "Message",
						    JOptionPane.WARNING_MESSAGE);

				}
			}
		};
		deleteAction = new AbstractAction(DELETE_STR) {
			public void actionPerformed(ActionEvent e) {
				int selectedIndex = searchResultTbl.getSelectedRow();
				if (selectedIndex != -1){
					searchResultModel.removeRow(selectedIndex);
				}else{
					JOptionPane.showMessageDialog((Component)mapInfoStack,
						    "Please select a Row",
						    "Message",
						    JOptionPane.WARNING_MESSAGE);
				}
			}
		};
		Widget.setMenuActions(metadataPanel, new Action[] { saveSearchAction,
				runSearchAction, renameAction, deleteAction });
		w.setContent(mapInfoStack);
		w.setHeight(350);
		w.setWidth(320);
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
			setLayout(new BorderLayout());
			topPanel = new JPanel();
			topPanel.setLayout(new GridBagLayout());
			GridBagConstraints topPanelGBC = new GridBagConstraints();
			topPanelGBC.fill = GridBagConstraints.HORIZONTAL;

			innerTopPanel = new JPanel(new BorderLayout());

			optionsPanel = new OptionsPanel();

			linePanel = new JPanel() {
				protected void paintComponent(java.awt.Graphics g) {
					// setSize(300,20);
					g.setColor(java.awt.Color.DARK_GRAY);
					g.drawLine(5, getHeight() / 2, getWidth() - 15,
							getHeight() / 2);
				}

				public java.awt.Dimension getMinimumSize() {
					return new java.awt.Dimension(MetadataSearchMainGUI.this
							.getWidth(), 30);
				}
			};
			linePanel.setBorder(BorderFactory.createLineBorder(this
					.getBackground(), 1));
			fieldsPanel = new JPanel(new java.awt.BorderLayout());
			ItemListener searchTypesListener = new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					if (ie.getStateChange() == ItemEvent.SELECTED) {

						if (ie.getItem().equals(SEARCH_EVERYTHING)) {
							setEverythingSearch();
						}
						if (ie.getItem().equals(SEARCH_LABELS_ONLY)) {
							setLabelSearch();
						}
						if (ie.getItem().equals(SEARCH_ALL_KEYWORDS)) {
							setAllMetadataSearch();
						}
						if (ie.getItem().equals(SEARCH_CATEGORIES_AND_KEYWORDS)) {
							setCategorySearch();
						}

					}
				}
			};

			ItemListener locationChoiceListener = new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					if (e.getStateChange() == ItemEvent.SELECTED) {
						String type = e.getItem().toString();
						if (type.equals(ALL_MAPS_STRING)) {
							allSearch
									.setLocationType(SearchAction.SEARCH_ALL_OPEN_MAPS);
							termsAction
									.setLocationType(SearchAction.SEARCH_ALL_OPEN_MAPS);
							optionsPanel.switchChoices(RESULTS,
									allOpenMapsResultsTypes);
						} else // SELECTED_MAP_STRING as current default
						{
							allSearch
									.setLocationType(SearchAction.SEARCH_SELECTED_MAP);
							termsAction
									.setLocationType(SearchAction.SEARCH_SELECTED_MAP);
							optionsPanel.switchChoices(RESULTS,
									currentMapResultsTypes);
						}
					}
				}
			};

			ItemListener resultsTypeListener = new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					if (e.getStateChange() == ItemEvent.SELECTED) {
						String resultsTypeChoice = e.getItem().toString();
						/*
						 * if(resultsTypeChoice != null && allSearch != null &&
						 * termsAction != null) {
						 * allSearch.setResultsType(resultsTypeChoice);
						 * termsAction.setResultsType(resultsTypeChoice); }
						 */
						setResultsTypeInActions(resultsTypeChoice);
					}
				}
			};

			JPanel advancedSearchPanel = new JPanel(new java.awt.FlowLayout(
					java.awt.FlowLayout.RIGHT));
			optionsLabel = new JLabel("show options");
			advancedSearch = new JButton(new ImageIcon(VueResources
					.getURL("advancedSearchMore.raw")));
			advancedSearch.setBorder(BorderFactory.createEmptyBorder());

			advancedSearchPanel.add(optionsLabel);
			advancedSearchPanel.add(advancedSearch);

			optionsPanel.setLayout(new GridBagLayout());
			GridBagConstraints gBC = new GridBagConstraints();
			gBC.fill = GridBagConstraints.HORIZONTAL;

			// First Row
			JLabel searchTypeLbl = new JLabel("Search Type:", SwingConstants.RIGHT);			
			gBC.gridx = 0;
		    gBC.gridy = 0;
		    gBC.weightx = 0.0;
			optionsPanel.add(searchTypeLbl, gBC);

			searchTypeCmbBox = getCombo(searchTypes, searchTypesListener);
			gBC.insets = new Insets(0, 5, 5, 0);

	        gBC.gridx = 1;
	        gBC.gridy = 0;
	        gBC.weightx = 1.0;
			optionsPanel.add(searchTypeCmbBox, gBC);

			// Second Row
			JLabel mapsLbl = new JLabel("Maps:", SwingConstants.RIGHT);			
			gBC.gridx = 0;
	        gBC.gridy = 1;
	        gBC.weightx = 0.0;
			optionsPanel.add(mapsLbl, gBC);

			mapCmbBox = getCombo(locationTypes, locationChoiceListener);
			gBC.weightx = 1.0;
		    gBC.insets = new Insets(0, 5, 5, 0);

		    gBC.gridx = 1;
		    gBC.gridy = 1;
			optionsPanel.add(mapCmbBox, gBC);

			// Third Row
			JLabel resultsLbl = new JLabel("Results:", SwingConstants.RIGHT);			
			gBC.weightx = 0.0;

	        gBC.gridx = 0;
	        gBC.gridy = 2;
			optionsPanel.add(resultsLbl, gBC);

			resultCmbBox = getCombo(currentMapResultsTypes, resultsTypeListener);
			gBC.weightx = 1.0;
	        gBC.insets = new Insets(0, 5, 5, 0);

	        gBC.gridx = 1;
	        gBC.gridy = 2;
			optionsPanel.add(resultCmbBox, gBC);

			searchTermsTable = new JTable(new SearchTermsTableModel());				

			adjustColumnModel();
			searchTermsTable.setDefaultRenderer(java.lang.Object.class,
					new SearchTermsTableRenderer());
			searchTermsTable.setDefaultEditor(java.lang.Object.class,
					new SearchTermsTableEditor());
			((DefaultCellEditor) searchTermsTable
					.getDefaultEditor(java.lang.Object.class))
					.setClickCountToStart(1);
			searchTermsTable.setRowHeight(ROW_HEIGHT);
			searchTermsTable.getTableHeader().setReorderingAllowed(false);
			searchTermsTable.setGridColor(new java.awt.Color(getBackground()
					.getRed(), getBackground().getBlue(), getBackground()
					.getGreen(), 0));
			searchTermsTable.setBackground(getBackground());		

			JPanel corner = new JPanel();
			corner.setOpaque(true);
			corner.setBackground(getBackground());

			fieldsInnerPanel = new JPanel();

			fieldsInnerPanel.setLayout(new BorderLayout());

			JPanel tablePanel = new JPanel();
			tablePanel.setLayout(new BorderLayout());
			searchHeaderTbl = new JTable(new SearchHeaderTableModel());
			searchHeaderTbl.setOpaque(false);
			
			
			searchHeaderTbl.setRowHeight(25);
			searchHeaderTbl.setShowGrid(false);
			tablePanel.add(searchHeaderTbl, BorderLayout.NORTH);

			tablePanel.add(searchTermsTable);

			topPanelGBC.weightx = 0.5;
			topPanelGBC.insets = new java.awt.Insets(0, 5, 5, 0);
			topPanelGBC.gridx = 0;
			topPanelGBC.gridy = 0;

			topPanel.add(innerTopPanel, topPanelGBC);

			// fieldsPanel.add(linePanel,BorderLayout.NORTH);
			// topPanel.add(fieldsPanel);

			buttonPanel = new JPanel(new BorderLayout());
			termsAction = new SearchAction(searchTerms);
			// SearchAction.revertGlobalSearchSelection();
			// termsAction.setResultsType(resultsTypeChoice.getSelectedItem().toString());

			searchButton = new JButton(termsAction);
			buttonPanel.setOpaque(true);
			buttonPanel.setBackground(getBackground());
			JButton resetButton = new JButton("Reset Map");
			JButton saveButton = new JButton("Save");
			saveButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {					
					SearchData data = new SearchData();
					searchDataList = new ArrayList<SearchData>();					
					data.setSearchSaveName("Search" + " "+ searchResultModel.getRowCount());
					data.setSearchType(searchTypeCmbBox.getSelectedItem()
							.toString().trim());
					data.setMapType(mapCmbBox.getSelectedItem().toString()
							.trim());
					data.setResultType(resultCmbBox.getSelectedItem()
							.toString().trim());					
					data.setAndOrType(strAndOrType);					
					// TO DO have to and/or						
					data.setDataList(searchTerms);
					searchDataList.add(data);
					searchResultModel.addRow(data);
					WriteSearchXMLData xc = new WriteSearchXMLData(
							searchDataList);
					xc.runSearchWriteToFile();
				}
			});

			resetButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					SearchAction.revertGlobalSearchSelectionFromMSGUI();

					VUE.getActiveViewer().repaint();
				}
			});
			JPanel searchPanel = new JPanel();
			searchPanel.add(saveButton);
			searchPanel.add(resetButton);
			searchPanel.add(searchButton);
			searchPanel.setOpaque(true);
			searchPanel.setBackground(getBackground());
			// buttonPanel.add(resetButton);
			searchPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
			buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 5));
			buttonPanel.add(BorderLayout.EAST, searchPanel);

			// add(BorderLayout.NORTH,searchField);

			/*
			 * add(topPanel); add(buttonPanel,BorderLayout.SOUTH);
			 */

			searchTermsTable
					.addMouseListener(new java.awt.event.MouseAdapter() {
						public void mouseReleased(java.awt.event.MouseEvent evt) {

							if (evt.getX() > searchTermsTable.getWidth()
									- BUTTON_COL_WIDTH) {
								// java.util.List<VueMetadataElement>
								// searchTermsList =
								// MetadataSearchMainGUI.this.searchTerms;
								int selectedRow = searchTermsTable
										.getSelectedRow();
								if (searchTermsTable.getSelectedColumn() == buttonColumn
										&& searchTerms.size() > selectedRow) {									
									searchTerms.remove(selectedRow);
									searchTermsTable.repaint();
									requestFocusInWindow();
								}
							}
						}
					});
			setResultsTypeInActions("Select");
			innerTopPanel.add(optionsPanel);

			topPanelGBC.weightx = 0.5;
			topPanelGBC.fill = GridBagConstraints.HORIZONTAL;
			topPanelGBC.insets = new java.awt.Insets(0, 5, 5, 0);
			topPanelGBC.gridx = 0;
			topPanelGBC.gridy = 1;

			topPanel.add(linePanel, topPanelGBC);

			topPanelGBC.weightx = 0.5;
			topPanelGBC.insets = new java.awt.Insets(0, 5, 5, 0);
			topPanelGBC.gridx = 0;
			topPanelGBC.gridy = 2;

			topPanel.add(tablePanel, topPanelGBC);

			// topPanel.add(tablePanel,BorderLayout.CENTER);
			add(topPanel);
			add(buttonPanel, BorderLayout.SOUTH);
		}
		

		

		public void setResultsTypeInActions(String resultsTypeChoice) {
			// String resultsTypeChoice = e.getItem().toString();
			if (resultsTypeChoice != null && allSearch != null
					&& termsAction != null) {
				allSearch.setResultsType(resultsTypeChoice);
				termsAction.setResultsType(resultsTypeChoice);
			}
		}

		private JComboBox getCombo(String[] choices, ItemListener listener) {
			JComboBox newCombo = new JComboBox(choices);
			newCombo.setFont(tufts.vue.gui.GUI.LabelFace);
			newCombo.addItemListener(listener);
			if (choices.length > 2 && choices[2].equals("Select")) {
				newCombo.setSelectedIndex(2);
			}
			comboBoxes.add(newCombo);
			return newCombo;
		}

		public String getName() {
			return "Search";
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
			//searchResultTbl.setCellEditor( new SearchResultTableEditor());
			
			((DefaultCellEditor) searchResultTbl
					.getDefaultEditor(java.lang.Object.class))
					.setClickCountToStart(1);
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
					}else{
						JOptionPane.showMessageDialog((Component)mapInfoStack,
							    "Please select a Row",
							    "Message",
							    JOptionPane.WARNING_MESSAGE);
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
					}else{
						JOptionPane.showMessageDialog((Component)mapInfoStack,
							    "Please select a Row",
							    "Message",
							    JOptionPane.WARNING_MESSAGE);
					}
				}
			});
			// add the listener to the jtable
			MouseListener popupListener = new PopupListener();
			// add the listener specifically to the header
			searchResultTbl.addMouseListener(popupListener);
			searchResultTbl.setIntercellSpacing(new Dimension(0,1));
			searchResultTbl.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			searchResultTbl.setShowGrid(false);			
			searchResultTbl.setRowHeight(23);
			
			//searchResultTbl.setBackground(this.getBackground());

			//searchResultTbl.getColumnModel().getColumn(1).setMaxWidth(30);
			//searchResultTbl.getColumnModel().getColumn(1).setMinWidth(30);
			searchResultTbl.getColumnModel().getColumn(1).setPreferredWidth(100);
			
			//searchResultTbl.setOpaque(true);
			
			// searchResultTbl.setMinimumSize(new Dimension(300,80));

			setLayout(new GridBagLayout());
			GridBagConstraints gBC = new GridBagConstraints();
			gBC.fill = GridBagConstraints.BOTH;			
			gBC.gridx = 0;
			gBC.gridy = 0;
			gBC.weightx = 1.0;
			gBC.insets = new Insets(0, 0, 0, 0);
			add(searchResultTbl, gBC);

			gBC.fill = GridBagConstraints.BOTH;
			gBC.gridx = 0;
			gBC.gridy = 1;
			gBC.weightx = 1.0;
			gBC.insets = new Insets(25, 0, 25, 0);
			add(new JLabel(""), gBC);

			// add(searchResultTbl);
			setName("Keywords");
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
						VueResources.getString("metadata.vue.url") + "#none",
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
				if (statement.length > 2)
					return ((String[]) (searchTerms.get(row)).getObject())[2];
				else
					return "";
			} else
				return searchTerms.get(row).getKey();
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
			searchHeaderTbl.getColumnModel().getColumn(0).setCellRenderer(
					new SearchTermsTableHeaderRenderer());
			searchHeaderTbl.getColumnModel().getColumn(1).setCellRenderer(
					new ComboBoxAndOrRenderer(andOrCmbBox));			
			
			searchHeaderTbl.getColumnModel().getColumn(2).setCellRenderer(
					new SearchTermsTableHeaderRenderer());
			
			searchHeaderTbl.getColumnModel().getColumn(1).setCellEditor(
					new ComboBoxAndOrEditor(andOrCmbBox));
			searchHeaderTbl.getColumnModel().getColumn(2).setCellEditor(
					new AddButtonTableCellEditor());
			
			
			searchHeaderTbl.getColumnModel().getColumn(1).setMaxWidth(60);
			searchHeaderTbl.getColumnModel().getColumn(1).setMinWidth(60);
			searchHeaderTbl.getColumnModel().getColumn(2).setMaxWidth(40);
			searchHeaderTbl.getColumnModel().getColumn(2).setMinWidth(40);

		} else if (searchHeaderTbl.getModel().getColumnCount() == 4) {
		
			searchHeaderTbl.getColumnModel().getColumn(0).setCellRenderer(
					new SearchTermsTableHeaderRenderer());
			searchHeaderTbl.getColumnModel().getColumn(1).setCellRenderer(
					new SearchTermsTableHeaderRenderer());
			searchHeaderTbl.getColumnModel().getColumn(2).setCellRenderer(
					new ComboBoxAndOrRenderer(andOrCmbBox));			
			searchHeaderTbl.getColumnModel().getColumn(3).setCellRenderer(
					new SearchTermsTableHeaderRenderer());			
			searchHeaderTbl.getColumnModel().getColumn(2).setCellEditor(
					new ComboBoxAndOrEditor(andOrCmbBox));
			searchHeaderTbl.getColumnModel().getColumn(3).setCellEditor(
					new AddButtonTableCellEditor());
			searchHeaderTbl.getColumnModel().getColumn(0).setPreferredWidth(145);
			searchHeaderTbl.getColumnModel().getColumn(2).setMaxWidth(60);
			searchHeaderTbl.getColumnModel().getColumn(2).setMinWidth(60);			
			searchHeaderTbl.getColumnModel().getColumn(3).setMaxWidth(40);
			searchHeaderTbl.getColumnModel().getColumn(3).setMinWidth(40);
		}
	}
	public class ComboBoxAndOrEditor extends DefaultCellEditor {
		JComboBox combo; 
		public ComboBoxAndOrEditor(JComboBox combo) {				
			super(combo);
			this.combo = combo;
		}
		public Component getTableCellEditorComponent(JTable table,
				Object value, boolean isSelected, int rowIndex,
				int vColIndex) {
			if(isSelected){
				if(this.combo.getSelectedItem().toString().equals("and")){
					strAndOrType = "or";
				}else{
					strAndOrType = "and";
				}									
			}			
			JLabel label = new JLabel("");
			if(vColIndex == 1 || vColIndex == 2){					
				SearchTermsTableModel model = (SearchTermsTableModel) searchTermsTable
				.getModel();
				if(model.getRowCount()<2){						
					return label;
				}else{
					return this.combo;
				}
			}				
		  return label;
		}
	}

	public class AddButtonTableCellEditor extends AbstractCellEditor
			implements TableCellEditor {
		// This is the component that will handle the editing of the cell
		// value
		JLabel component = new JLabel();

		// This method is called when a cell value is edited by the user.
		public Component getTableCellEditorComponent(JTable table,
				Object value, boolean isSelected, int rowIndex,
				int vColIndex) {			
			if (vColIndex == 2 || vColIndex == 3) {	
				component.setIcon(tufts.vue.VueResources
						.getImageIcon("metadata.editor.add.up"));
				component.setBorder(BorderFactory.createEmptyBorder(ROW_GAP,
						ROW_INSET, ROW_GAP, ROW_INSET - 5));					
				VueMetadataElement newElement = new VueMetadataElement();
				String statementObject[] = {
						VueResources.getString("metadata.vue.url")
								+ "#none",
						"",
						edu.tufts.vue.rdf.Query.Qualifier.STARTS_WITH
								.toString() };
				newElement.setObject(statementObject);
				newElement.setType(VueMetadataElement.SEARCH_STATEMENT);
				searchTerms.add(newElement);
				((SearchTermsTableModel) searchTermsTable.getModel())
						.refresh();
				((SearchHeaderTableModel) searchHeaderTbl.getModel()).refresh();					
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

		Component combo = null;

		public ComboBoxAndOrRenderer(JComboBox combo) {
			this.combo = combo;
		}

		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			if (searchTermsTable.getModel().getRowCount() > 1) {
				combo.setVisible(true);
				combo.setFont(tufts.vue.gui.GUI.LabelFace);				
				combo.repaint();
				combo.invalidate();
				table.revalidate();
				table.repaint();
			} else {
				JLabel label = new JLabel("");
				label.setOpaque(true);
				label.setEnabled(false);
				return label;
			}
			return combo;
		}
	}
	public void adjustColumnModel() {

		if (searchTermsTable == null)
			return;
		int editorWidth = this.getWidth();
		if (searchTermsTable.getModel().getColumnCount() == 3) {
			
			searchTermsTable.getColumnModel().getColumn(1).setMaxWidth(0);
			searchTermsTable.getColumnModel().getColumn(1).setMinWidth(0);
			searchTermsTable.getColumnModel().getColumn(1).setPreferredWidth(0);
			searchTermsTable.getColumnModel().getColumn(2).setMaxWidth(40);
			searchTermsTable.getColumnModel().getColumn(2).setMinWidth(40);

		} else if (searchTermsTable.getModel().getColumnCount() == 4) {
		
			searchTermsTable.getColumnModel().getColumn(0).setPreferredWidth(80);
			//searchTermsTable.getColumnModel().getColumn(1).setPreferredWidth(150);
			searchTermsTable.getColumnModel().getColumn(2).setMaxWidth(0);
			searchTermsTable.getColumnModel().getColumn(2).setMinWidth(0);	
			searchTermsTable.getColumnModel().getColumn(2).setPreferredWidth(0);
			searchTermsTable.getColumnModel().getColumn(3).setMaxWidth(40);
			searchTermsTable.getColumnModel().getColumn(3).setMinWidth(40);
		
		} 
	}

	public void setCategorySearchWithNoneCase() {		
		singleLine = false;
		SearchTermsTableModel model = (SearchTermsTableModel) searchTermsTable
				.getModel();
		buttonColumn = 2;
		valueColumn = 1;
		categoryColumn = 0;
		conditionColumn = -1;
		model.setColumns(3);
		adjustColumnModel();
		headerButtonColumn = 2;
		headerComboColumn = 1;
		headerCategoryColumn = -1;
		headerValueColumn = 0;
		termsAction = new SearchAction(searchTerms);
		adjustHeaderTableColumnModel();
		// treatNoneSpecially = true;
		termsAction.setNoneIsSpecial(true);

		termsAction.setTextOnly(false);
		termsAction.setBasic(false);
		termsAction.setMetadataOnly(false);
		termsAction.setOperator(getSelectedOperator());
		termsAction.setEverything(false);
		// termsAction.setOperator(andOrGroup.getSelection().getModel().getActionCommand());
		searchButton.setAction(termsAction);
	}

	public void setCategorySearch() {
		singleLine = false;
		searchType = CATEGORY;
		SearchTermsTableModel model = (SearchTermsTableModel) searchTermsTable
				.getModel();	
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
		// termsAction = new SearchAction(searchTerms);

		termsAction.setBasic(false);
		termsAction.setTextOnly(false);
		termsAction.setMetadataOnly(false);
		termsAction.setOperator(getSelectedOperator());
		termsAction.setEverything(false);
		// termsAction.setOperator(andOrGroup.getSelection().getModel().getActionCommand());
		searchButton.setAction(termsAction);
	}

	public void setEverythingSearch() {
		searchType = EVERYTHING;

		singleLine = false;
		SearchTermsTableModel model = (SearchTermsTableModel) searchTermsTable
				.getModel();
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
		termsAction = new SearchAction(searchTerms);

		termsAction.setBasic(false);
		termsAction.setTextOnly(true);
		termsAction.setMetadataOnly(false);
		termsAction.setOperator(getSelectedOperator());
		termsAction.setEverything(true);
		// termsAction.setOperator(andOrGroup.getSelection().getModel().getActionCommand());
		searchButton.setAction(termsAction);
	}

	public int getSelectedOperator() {
		if(strAndOrType.equals("and")){
			return SearchAction.AND;
		}else{
			return SearchAction.OR;
		}			
	}

	public void setLabelSearch() {
		searchType = LABEL;

		singleLine = false;
		SearchTermsTableModel model = (SearchTermsTableModel) searchTermsTable
				.getModel();
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
		termsAction = new SearchAction(searchTerms);
		termsAction.setBasic(true);
		termsAction.setTextOnly(false);
		termsAction.setMetadataOnly(false);
		termsAction.setOperator(getSelectedOperator());
		termsAction.setEverything(false);
		// termsAction.setOperator(andOrGroup.getSelection().getModel().getActionCommand());
		searchButton.setAction(termsAction);
	}

	public void setAllMetadataSearch() {
		searchType = KEYWORD;

		singleLine = false;
		SearchTermsTableModel model = (SearchTermsTableModel) searchTermsTable
				.getModel();
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
		termsAction = new SearchAction(searchTerms);

		termsAction.setBasic(false);
		termsAction.setTextOnly(true);
		termsAction.setMetadataOnly(true);
		termsAction.setOperator(getSelectedOperator());
		termsAction.setEverything(false);
		// termsAction.setOperator(andOrGroup.getSelection().getModel().getActionCommand());
		searchButton.setAction(termsAction);
	}

	public void setConditionSearch() {
		singleLine = false;
		SearchTermsTableModel model = (SearchTermsTableModel) searchTermsTable
				.getModel();
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
		termsAction = new SearchAction(searchTerms);
		termsAction.setBasic(false);
		termsAction.setTextOnly(false);
		termsAction.setMetadataOnly(false);
		termsAction.setOperator(getSelectedOperator());
		termsAction.setEverything(false);
		// termsAction.setOperator(andOrGroup.getSelection().getModel().getActionCommand());
		searchButton.setAction(termsAction);
	}

	public void setAllSearch() {
		SearchTermsTableModel model = (SearchTermsTableModel) searchTermsTable
				.getModel();
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
		searchButton.setAction(allSearch);

		singleLine = true;
	}

	class SearchTermsTableHeaderRenderer extends DefaultTableCellRenderer {
		public java.awt.Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int col) {
			JLabel comp = new JLabel();
			comp.setFont(tufts.vue.gui.GUI.LabelFace);			
			if (col == headerButtonColumn) {
				comp.setIcon(tufts.vue.VueResources
						.getImageIcon("metadata.editor.add.up"));
			} else if (table.getModel().getColumnCount() == 3
					&& col == headerValueColumn) {
				if (searchType == EVERYTHING) {
					comp.setText("Search everything:");
				}

				if (searchType == LABEL) {
					comp.setText("Labels:");
				}

				if (searchType == KEYWORD) {
					comp.setText("Keywords:");
				}
				if(searchType == CATEGORY){					
					comp.setText("Category:");	
				}				

			} else if ((table.getModel().getColumnCount() == 4)
					&& col == headerCategoryColumn) {				
				comp.setText("Category:");
				// comp.setBorder(BorderFactory.createEmptyBorder(0,5,0,0));
			} else if ((table.getModel().getColumnCount() == 4 )
					&& col == headerValueColumn){
				comp.setText("Keyword:");
			}
			
			if (comp.getText().equals("Category:")
					|| comp.getText().equals("Operator")) {
				if(col == headerButtonColumn){
					comp.setBorder(BorderFactory.createEmptyBorder(ROW_GAP,
							ROW_INSET, ROW_GAP, 5));
				}else{
					comp.setBorder(BorderFactory.createEmptyBorder(ROW_GAP,
							ROW_INSET, ROW_GAP, ROW_INSET - 5));
				}
			} else {
				comp.setBorder(BorderFactory.createEmptyBorder(ROW_GAP,
						ROW_INSET, ROW_GAP, ROW_INSET - 5));

			}
			comp.setOpaque(true);
			comp.setBackground(MetadataSearchMainGUI.this.getBackground());
			return comp;
		}
	}

	class SearchTermsTableRenderer extends DefaultTableCellRenderer {
		public java.awt.Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int col) {			
			return createRendererComponent(table, value, row, col);
		}
	}

	class SearchTermsTableEditor extends DefaultCellEditor {
		public SearchTermsTableEditor() {
			super(new JTextField());
		}

		public java.awt.Component getTableCellEditorComponent(JTable table,
				Object value, boolean isSelected, int row, int col) {
			return createRendererComponent(table, value, row, col);
		}
	}

	public java.awt.Component createRendererComponent(JTable table,
			Object value, final int row, int col) {
		JPanel comp = new JPanel();

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
			JLabel buttonLabel = new JLabel();
			buttonLabel.setIcon(tufts.vue.VueResources
					.getImageIcon("metadata.editor.delete.up"));
			comp.add(buttonLabel);
		} else if (col == conditionColumn) {
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
		comp.setBorder(BorderFactory
				.createEmptyBorder(5, ROW_INSET, 0, 0));		
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

		OptionsPanel() {
			optionsGrid = new GridBagLayout();
			optionsConstraints = new GridBagConstraints();
			setLayout(optionsGrid);
			setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 20));
		}

		/**
		 * 
		 * adds the label to the layout and sets its Font and alignment
		 * 
		 * Precondition: ready for new line in layout (beginning of layout or
		 * combo just added) Postcondition: ready to add a new combo box
		 * 
		 **/
		void addLabel(String name) {
			JLabel label = new JLabel(name, JLabel.RIGHT);
			label.setFont(tufts.vue.gui.GUI.LabelFace);

			optionsConstraints.anchor = GridBagConstraints.EAST;
			optionsConstraints.weightx = 0.0;
			optionsConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
			optionsConstraints.gridwidth = 1;
			optionsGrid.setConstraints(label, optionsConstraints);
			optionsPanel.add(label);

		}

		public void switchChoices(int i, String[] choices) {
			JComboBox box = comboBoxes.get(i);
			box.removeAllItems();
			for (int j = 0; j < choices.length; j++) {
				box.addItem(choices[j]);
			}

			if (choices.length > 2 && i == RESULTS) {
				box.setSelectedIndex(2);
			} else if (choices.length > 1) {
				box.setSelectedItem(0);
			}
		}

	}

	class PopupListener extends MouseAdapter {
		public void mousePressed(MouseEvent e) {
			if(e.getX()>(searchResultTbl.getWidth()-40)){
				System.err.println("Clicked on Run");
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

}
