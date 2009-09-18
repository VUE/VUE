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
package edu.tufts.vue.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;

import tufts.vue.LWSelection;
import tufts.vue.VUE;
import tufts.vue.VueResources;
import tufts.vue.gui.GUI;

public class DefaultQueryEditor extends javax.swing.JPanel
implements edu.tufts.vue.fsm.QueryEditor, java.awt.event.ActionListener, LWSelection.Listener
{
	//private edu.tufts.vue.fsm.FederatedSearchManager fsm = edu.tufts.vue.fsm.impl.VueFederatedSearchManager.getInstance();
	private edu.tufts.vue.fsm.SourcesAndTypesManager sourcesAndTypesManager = edu.tufts.vue.fsm.impl.VueSourcesAndTypesManager.getInstance();
	
	private java.awt.GridBagLayout gbLayout = new java.awt.GridBagLayout();
	private java.awt.GridBagConstraints gbConstraints = new java.awt.GridBagConstraints();
	
	private static javax.swing.JTextField field = new javax.swing.JTextField(15);
	private java.io.Serializable criteria = null;
	private org.osid.shared.Properties searchProperties = null;
	private org.osid.shared.Type searchType = null;
	
	private org.osid.shared.Type keywordSearchType = new edu.tufts.vue.util.Type("mit.edu","search","keyword");
	private org.osid.shared.Type multiFieldSearchType = new edu.tufts.vue.util.Type("mit.edu","search","multiField");
	
	protected javax.swing.event.EventListenerList listenerList = new javax.swing.event.EventListenerList();
	
	private static org.osid.repository.Repository[] repositories;

    private final static String SearchLabel = VueResources.getString("defaultqueryeditor.search");
    private final static String StopLabel = VueResources.getString("defaultqueryeditor.stopsearch");
	
	private static final javax.swing.JButton searchButton1 = new javax.swing.JButton(SearchLabel);
	private static final javax.swing.JButton searchButton2 = new javax.swing.JButton(SearchLabel);
	
	private static final String SELECT_A_LIBRARY = VueResources.getString("defaultqueryeditor.pleaseselect");
	//private static final String NO_MESSAGE = "";
    private final javax.swing.JLabel selectMessage = new javax.swing.JLabel(SELECT_A_LIBRARY, javax.swing.JLabel.CENTER);
	
	private javax.swing.JButton moreOptionsButton = new tufts.vue.gui.VueButton("advancedSearchMore");
	//private static final String MORE_OPTIONS = "";
	//private javax.swing.JLabel moreOptionsLabel = new javax.swing.JLabel(MORE_OPTIONS);
	private javax.swing.JPanel moreOptionsButtonPanel = new javax.swing.JPanel();

	private javax.swing.JButton fewerOptionsButton = new tufts.vue.gui.VueButton("advancedSearchLess");
	//private static final String FEWER_OPTIONS = "";
	//private javax.swing.JLabel fewerOptionsLabel = new javax.swing.JLabel(FEWER_OPTIONS);
	private javax.swing.JPanel fewerOptionsButtonPanel = new javax.swing.JPanel();
	//private javax.swing.JPanel moreFewerPanel = new javax.swing.JPanel();
	
	private static final int NOTHING_SELECTED = 0;
	private static final int BASIC = 1;
	private static final int ADVANCED_INTERSECTION = 2;
	private static final int ADVANCED_UNION = 3;
	private int currentStyle = BASIC;
	
	private javax.swing.JTextField[] advancedFields = null;
	private java.util.Vector typesVector = null;
	
	// advanced search universe of types
	private java.util.Vector advancedSearchUniverseOfTypeStringsVector = new java.util.Vector();
	private java.util.Vector advancedSearchPromptsVector = new java.util.Vector();
	
	// maintain a vector for current values
	private String[] advancedSearchFieldsText = null;
	
	//map based searching controls
	private final static javax.swing.JCheckBox mapBasedSearchCheckBox = new javax.swing.JCheckBox(VueResources.getString("defaultQueryEditor.mapBasedSearchLabel"));
	
	public DefaultQueryEditor() {

            // for mac leopard: would need to handle whatever event clicking the 'x' in the search field delivers
            // for stopping the search
            //field.putClientProperty("JTextField.variant", "search");
            
		try {
			gbConstraints.anchor = java.awt.GridBagConstraints.WEST;
			gbConstraints.insets = new java.awt.Insets(2,2,2,2);
			gbConstraints.weighty = 0;
			gbConstraints.ipadx = 0;
			gbConstraints.ipady = 0;
			
			setLayout(gbLayout);
			
			// make button panels
			java.awt.GridBagLayout moreOptionsButtonPanelgbLayout = new java.awt.GridBagLayout();
			java.awt.GridBagConstraints moreOptionsButtonPanelgbConstraints = new java.awt.GridBagConstraints();
			moreOptionsButtonPanel.setLayout(moreOptionsButtonPanelgbLayout);
			searchButton1.setEnabled(false);
			searchButton2.setEnabled(false);
			field.addKeyListener(new KeyAdapter(){
			      public void keyReleased(KeyEvent ke){
			          searchButton1.setEnabled(field.getText().equals("")==false);
			          searchButton2.setEnabled(field.getText().equals("")==false);
			        }
			      });
			moreOptionsButtonPanelgbConstraints.gridx = 0;
			moreOptionsButtonPanelgbConstraints.gridy = 0;
			moreOptionsButtonPanelgbConstraints.fill = java.awt.GridBagConstraints.NONE;
			moreOptionsButtonPanelgbConstraints.weightx = 0;
			moreOptionsButtonPanel.add(moreOptionsButton,moreOptionsButtonPanelgbConstraints);

			moreOptionsButtonPanelgbConstraints.gridx = 1;
			moreOptionsButtonPanelgbConstraints.fill = java.awt.GridBagConstraints.NONE;
			moreOptionsButtonPanelgbConstraints.anchor= java.awt.GridBagConstraints.EAST;
			moreOptionsButtonPanelgbConstraints.weightx = 1;
			moreOptionsButtonPanelgbConstraints.ipadx=20;
			moreOptionsButtonPanel.add(searchButton1,moreOptionsButtonPanelgbConstraints);
			
			java.awt.GridBagLayout fewerOptionsButtonPanelgbLayout = new java.awt.GridBagLayout();
			java.awt.GridBagConstraints fewerOptionsButtonPanelgbConstraints = new java.awt.GridBagConstraints();
			fewerOptionsButtonPanel.setLayout(fewerOptionsButtonPanelgbLayout);
			
			fewerOptionsButtonPanelgbConstraints.gridx = 0;
			fewerOptionsButtonPanelgbConstraints.gridy = 0;
			fewerOptionsButtonPanelgbConstraints.fill = java.awt.GridBagConstraints.NONE;
			fewerOptionsButtonPanelgbConstraints.weightx = 0;
			fewerOptionsButtonPanel.add(fewerOptionsButton,fewerOptionsButtonPanelgbConstraints);
			
			fewerOptionsButtonPanelgbConstraints.gridx = 1;
			fewerOptionsButtonPanelgbConstraints.fill = java.awt.GridBagConstraints.NONE;
			fewerOptionsButtonPanelgbConstraints.anchor= java.awt.GridBagConstraints.EAST;
			fewerOptionsButtonPanelgbConstraints.ipadx=20;
			fewerOptionsButtonPanelgbConstraints.weightx = 1;
			fewerOptionsButtonPanel.add(this.searchButton2,fewerOptionsButtonPanelgbConstraints);
			
			searchButton1.addActionListener(this);
			searchButton2.addActionListener(this);
			moreOptionsButton.addActionListener(this);
			fewerOptionsButton.addActionListener(this);
			field.addActionListener(this);
			searchProperties = new edu.tufts.vue.util.SharedProperties();		

			populateAdvancedSearchUniverseOfTypeStringsVector();
			repositories = sourcesAndTypesManager.getRepositoriesToSearch();
			if (repositories.length == 0) {
				makePanel(NOTHING_SELECTED);
			} else {
				makePanel(BASIC);
			}
			//add(new javax.swing.JScrollPane(this.panel));
			//map based searching requires me to listen to selection
			VUE.getSelection().addListener(this);
		} catch (Throwable t) {
		}
	}
	
	public static int getSelectedRepositoryCount()
	{
		if (repositories !=null)
			return repositories.length;
		else return 0;
	}
	public void refresh()
	{
		org.osid.repository.Repository[] oldRepositories = repositories.clone();
		//if the repository list has changed we need to set the panel back to basic...
		//because the state can get confused, looks like this has probably happened all along
		//but just got noticed now. -MK 8/28
		
		repositories = sourcesAndTypesManager.getRepositoriesToSearch();
		if (oldRepositories.length ==repositories.length)
		{
			if (repositories.length == 0) {
				makePanel(NOTHING_SELECTED);
			} else {
				makePanel(this.currentStyle);
			}	
		}
		else
		{
			if (repositories.length == 0) {
				makePanel(NOTHING_SELECTED);
			} else {
				makePanel(BASIC);
			}
		}
	}
	
	private void makePanel(int kind)
	{
		this.removeAll();
		switch (kind) {
			case NOTHING_SELECTED:
				//System.out.println("NOTHING SEL");
				makeNothingSelectedPanel();
				this.searchType = this.keywordSearchType;
				break;
			case BASIC:
				//System.out.println("BASIC SEL");
				makeBasicPanel();
				this.searchType = this.keywordSearchType;
				break;
			case ADVANCED_INTERSECTION:
				//System.out.println("ADVNCED SEL");
				makeAdvancedIntersectionPanel();
				this.searchType = this.multiFieldSearchType;
				break;
			//default:
				//System.out.println("DEFAULT SEL");
				//break;
//			case ADVANCED_UNION:
//				makeAdvancedUnionPanel();
//				this.searchType = this.multiFieldSearchType;
//				break;
		}
		this.revalidate();
	}
	
	private void makeNothingSelectedPanel()
	{
		gbConstraints.gridx = 0;
		gbConstraints.gridy = 0;
/*
		add(new javax.swing.JLabel("Keyword:"),gbConstraints);
		
		gbConstraints.gridx = 1;
		gbConstraints.gridy = 0;
		add(field,gbConstraints);
		//field.addActionListener(this);
		
		gbConstraints.gridx = 0;
		gbConstraints.gridy = 1;
		gbConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gbConstraints.weightx = 1;
*/
		gbConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gbConstraints.weightx = 1;
		selectMessage.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));  // it will be in a panel with a GUI.WidgetInsetBorder
		selectMessage.setForeground(java.awt.Color.darkGray);
		selectMessage.setFont(tufts.vue.gui.GUI.StatusFace);
		add(selectMessage,gbConstraints);
	}
	
	private void makeBasicPanel()
	{
		gbConstraints.gridx = 0;
		gbConstraints.gridy = 0;
		gbConstraints.fill = java.awt.GridBagConstraints.NONE;
		gbConstraints.weightx = 0;
		gbConstraints.anchor = java.awt.GridBagConstraints.EAST;
		javax.swing.JLabel searchTypeLabel = new javax.swing.JLabel(VueResources.getString("defaultQueryEditor.searchTypeLabel"));
		searchTypeLabel.setFont(tufts.vue.gui.GUI.LabelFace);
		add(searchTypeLabel,gbConstraints);
		
		gbConstraints.gridx = 1;
		gbConstraints.gridy = 0;
		gbConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gbConstraints.weightx = 1;
		
		mapBasedSearchCheckBox.setFont(GUI.LabelFace);
		mapBasedSearchCheckBox.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (mapBasedSearchCheckBox.isSelected())
				{	field.setEditable(false);
					field.setEnabled(false);}
				else
				{	
					field.setText("");
					field.setEnabled(true);
					field.setEditable(true);
					searchButton1.setEnabled(false);
					searchButton2.setEnabled(false);

				}
				LWSelection selection = VUE.getSelection();
				if (mapBasedSearchCheckBox.isSelected() && !(selection.size() == 1 && selection.get(0) instanceof tufts.vue.LWNode))
				{
					field.setText(VueResources.getString("analyzeraction.selectnode"));
				}
				else if (mapBasedSearchCheckBox.isSelected() && (selection.size() == 1 && selection.get(0) instanceof tufts.vue.LWNode))
				{
					searchButton1.setEnabled(true);
					searchButton2.setEnabled(true);
				}
				 
					
			}
		});
		add(mapBasedSearchCheckBox,gbConstraints);
		
		gbConstraints.gridx = 0;
		gbConstraints.gridy = 1;
		gbConstraints.fill = java.awt.GridBagConstraints.NONE;
		gbConstraints.weightx = 0;
		gbConstraints.anchor = java.awt.GridBagConstraints.EAST;
		javax.swing.JLabel keywordLbl = new javax.swing.JLabel(VueResources.getString("jlabel.keyword"));
		keywordLbl.setFont(tufts.vue.gui.GUI.LabelFace);
		add(keywordLbl,gbConstraints);
		
		gbConstraints.gridx = 1;
		gbConstraints.gridy = 1;
		gbConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gbConstraints.weightx = 1;
		add(field,gbConstraints);
		//field.addActionListener(this);
		
		gbConstraints.gridx = 0;
		gbConstraints.gridy = 2;
		gbConstraints.fill = java.awt.GridBagConstraints.NONE;
		gbConstraints.weightx = 0;
		gbConstraints.ipadx = 10;
		javax.swing.JLabel moreOptionLbl = new javax.swing.JLabel(VueResources.getString("jlabel.moreoption"));
		moreOptionLbl.setFont(tufts.vue.gui.GUI.LabelFace);
		add(moreOptionLbl,gbConstraints);

		gbConstraints.gridx = 1;
		gbConstraints.weightx = 1;
		gbConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gbConstraints.weightx = 1;
		add(moreOptionsButtonPanel,gbConstraints);
		this.currentStyle = BASIC;
		
		// enable the more options button only if there are Dublin Core fields other than 
		// Keywords available
		java.util.Vector intersection = getIntersectionSearchFields();
		moreOptionsButton.setEnabled(intersection.size() > 1);
		
	}
	private class AdvancedFieldKeyListener extends KeyAdapter
	{
		 public void keyReleased(KeyEvent ke)
		 {
			 int len = advancedFields.length;
			 int sum = 0;
			 for (int i=0;i < len; i++)
			 {
				 sum +=((String)(advancedFields[i].getText())).replaceAll(" ","").length();
			 }
		         searchButton2.setEnabled(sum > 0 ? true:false);
		         searchButton1.setEnabled(sum > 0 ? true:false);
		 }
			
	}
	private void makeAdvancedIntersectionPanel()
	{
		gbConstraints.gridx = 0;
		gbConstraints.gridy = 0;
		AdvancedFieldKeyListener advancedKeyListener = new AdvancedFieldKeyListener();
		
		this.typesVector = getIntersectionSearchFields();
		java.util.Collections.sort(typesVector);
		// always add a keyword field
		typesVector.insertElementAt("Keyword",0);
		int size = typesVector.size();
		
		if (size > 0) {
			this.advancedFields = new javax.swing.JTextField[size];
			
			for (int i=0; i < size; i++) {
				gbConstraints.fill = java.awt.GridBagConstraints.NONE;
				gbConstraints.weightx = 0;
				String prompt = (String)typesVector.elementAt(i);//+":";
				add(new javax.swing.JLabel(prompt +":"),gbConstraints);
				gbConstraints.gridx = 1;
				gbConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
				gbConstraints.weightx = 1;
				advancedFields[i] = new javax.swing.JTextField(8);
				advancedFields[i].addKeyListener(advancedKeyListener);
				if (prompt.equals("Keyword")) {
					advancedFields[i].setText(this.field.getText());
				} else {
					int index = advancedSearchPromptsVector.indexOf(prompt);
					advancedFields[i].setText(this.advancedSearchFieldsText[index]);
				}
				add(advancedFields[i],gbConstraints);
				gbConstraints.gridx = 0;
				gbConstraints.gridy++;
			}
			//searchButton2.setEnabled(true);
		}
		
		gbConstraints.fill = java.awt.GridBagConstraints.NONE;
		gbConstraints.weightx = 0;
		gbConstraints.gridx = 0;
		add(new javax.swing.JLabel(VueResources.getString("jlabel.feweroption")),gbConstraints);

		gbConstraints.gridx = 1;
		gbConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gbConstraints.weightx = 1;
		add(fewerOptionsButtonPanel,gbConstraints);
		this.currentStyle = ADVANCED_INTERSECTION;
	}
	
	private void makeAdvancedUnionPanel()
	{
		gbConstraints.gridx = 0;
		gbConstraints.gridy = 0;
		
		java.util.Vector typesVector = getUnionSearchFields();
		java.util.Collections.sort(typesVector);
		int size = typesVector.size();
		
		if (size == 0) {
			// no Dublin Core Types found
			add(new javax.swing.JLabel(VueResources.getString("jlabel.nodublin")),gbConstraints);
			gbConstraints.gridy++;
			//searchButton2.setEnabled(false);
		} else {
			this.advancedFields = new javax.swing.JTextField[size];
			
			for (int i=0; i < size; i++) {
				gbConstraints.fill = java.awt.GridBagConstraints.NONE;
				gbConstraints.weightx = 0;
				add(new javax.swing.JLabel((String)typesVector.elementAt(i)),gbConstraints);
				gbConstraints.gridx = 1;
				advancedFields[i] = new javax.swing.JTextField(8);
				gbConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
				gbConstraints.weightx = 1;
				add(advancedFields[i],gbConstraints);
				gbConstraints.gridx = 0;
				gbConstraints.gridy++;
			}
			//searchButton2.setEnabled(true);
		}
		
		gbConstraints.gridx = 0;
		add(fewerOptionsButton,gbConstraints);
		
		gbConstraints.gridx = 1;
		gbConstraints.gridy++;
		//add(searchButton,gbConstraints);
		this.currentStyle = ADVANCED_UNION;
	}
	
	public void actionPerformed(java.awt.event.ActionEvent ae)
	{
		if (ae.getSource() == this.moreOptionsButton) {
			if (this.currentStyle == BASIC) {
				makePanel(ADVANCED_INTERSECTION);
			} else {
				makePanel(ADVANCED_UNION);
			}
		} else if (ae.getSource() == this.fewerOptionsButton) {
			if (this.currentStyle == ADVANCED_UNION) { // never happens under current impl
				makePanel(ADVANCED_INTERSECTION);
			} else {
				// make sure keywords is sticky, and so are other fields
				int size = this.typesVector.size();
				for (int i=0; i < size; i++) {
					String prompt = (String)this.typesVector.elementAt(i);
					if (prompt.equals("Keyword")) {
						field.setText(advancedFields[i].getText());
					} else {						
						int index = advancedSearchPromptsVector.indexOf(prompt);
						advancedSearchFieldsText[index] = advancedFields[i].getText();
					}
					makePanel(BASIC);
				}
			}
		} else {
			if (this.mapBasedSearchCheckBox.isSelected())
			{

		        if (searchButton1.getText() == StopLabel) {
		            // If we already have the StopLabel, this means abort the search.
		            // null event currently means abort search

			        Object[] listeners = listenerList.getListenerList();
			        for (int i=0; i<listeners.length; i+=2) {
			            if (listeners[i] == edu.tufts.vue.fsm.event.SearchListener.class) {
			                ((edu.tufts.vue.fsm.event.SearchListener)listeners[i+1]).searchPerformed(null);
			            }
			        }

			        completeSearch();

		        } else {
					tufts.vue.AnalyzerAction.calais.act();
				}



			}
			else
				{
					this.criteria = field.getText();
                    //System.out.println("\n\nFIRESEARCH " + ae);
					fireSearch(new edu.tufts.vue.fsm.event.SearchEvent(this));
				}
		}
	}
	
	public void addSearchListener(edu.tufts.vue.fsm.event.SearchListener listener)
	{
		listenerList.add(edu.tufts.vue.fsm.event.SearchListener.class, listener);
	}
    
	public void removeSearchListener(edu.tufts.vue.fsm.event.SearchListener listener)
	{
		listenerList.remove(edu.tufts.vue.fsm.event.SearchListener.class, listener);
	}
    
    private void fireSearch(edu.tufts.vue.fsm.event.SearchEvent evt) 
    {
        if (searchButton1.getText() == StopLabel) {
            // If we already have the StopLabel, this means abort the search.
            // null event currently means abort search
            evt = null;
        } else {
            searchButton1.setText(StopLabel);
            searchButton2.setText(StopLabel);
        }
        field.setEnabled(false);
        Object[] listeners = listenerList.getListenerList();
        for (int i=0; i<listeners.length; i+=2) {
            if (listeners[i] == edu.tufts.vue.fsm.event.SearchListener.class) {
                ((edu.tufts.vue.fsm.event.SearchListener)listeners[i+1]).searchPerformed(evt);
            }
        }

        if (evt == null)
            completeSearch();
    }
    
    public static  void setStopLabels()
    {
    	searchButton1.setEnabled(true);
    	searchButton2.setEnabled(true);
    	searchButton1.setText(StopLabel);
    	searchButton2.setText(StopLabel);
    	field.setEnabled(false);
    	mapBasedSearchCheckBox.setSelected(true);
    }

    public void completeSearch() {
        searchButton1.setText(SearchLabel);
        searchButton2.setText(SearchLabel);
        field.setEnabled(true);
    }
	
	public java.io.Serializable getCriteria() {
		if (this.searchType.isEqual(this.keywordSearchType)) {
			return field.getText();
		} else {
			return multiFieldXML();
		}
	}
	
	public void setCriteria(java.io.Serializable searchCriteria) {
		if (searchCriteria instanceof String) {
			this.criteria = searchCriteria;
			field.setText((String)this.criteria);
		} else {
			this.criteria = null;
			field.setText("");		
		}
	}
	
	public org.osid.shared.Properties getProperties() {
		return this.searchProperties;
	}
	
	public void setProperties(org.osid.shared.Properties searchProperties) {
		this.searchProperties = searchProperties;
	}

	public void setSearchType(org.osid.shared.Type searchType)
	{
		this.searchType = searchType;
	}
	
	public org.osid.shared.Type getSearchType()
	{
		return this.searchType;
	}
	
	public String getSearchDisplayName() {
		// return the criteria, no longer than 20 characters worth
		String s =  (String)getCriteria();
		if (s.length() > 20) s = s.substring(0,20) + "...";
		return s;
	}


	private java.util.Vector getUnionSearchFields()
	{
		/*
		 Find each repository that will be searched.  Get all the asset types and for each the
		 mandatory record structures.  For each structure find the part structures and their
		 types.  Check these again the VUE set (based on Dublin Core).  Return the VUE names for
		 all that match.
		 */
		
		java.util.Vector union = new java.util.Vector();
		
		try {
			org.osid.repository.Repository[] repositories = sourcesAndTypesManager.getRepositoriesToSearch();
			for (int i=0; i < repositories.length; i++) {
                            	// not all these methods may be implemented -- in which case we are out of luck
				try {
					org.osid.shared.TypeIterator typeIterator = repositories[i].getAssetTypes();
					while (typeIterator.hasNextType()) {
						org.osid.shared.Type nextAssetType = typeIterator.nextType();
										   
						org.osid.repository.RecordStructureIterator recordStructureIterator = repositories[i].getMandatoryRecordStructures(nextAssetType);
						while (recordStructureIterator.hasNextRecordStructure()) {
							org.osid.repository.PartStructureIterator partStructureIterator = recordStructureIterator.nextRecordStructure().getPartStructures();
							while (partStructureIterator.hasNextPartStructure()) {
								org.osid.shared.Type nextType = partStructureIterator.nextPartStructure().getType();
								String nextTypeString = edu.tufts.vue.util.Utilities.typeToString(nextType);
								
								int index = advancedSearchUniverseOfTypeStringsVector.indexOf(nextTypeString);
								if (index != -1) {
									String prompt = (String)advancedSearchPromptsVector.elementAt(index);
									if (!union.contains(prompt)) {
										union.addElement(prompt);
									}
								}
							}
						}
					}
				} catch (Throwable t) {
					edu.tufts.vue.util.Logger.log(t);
				}
			}
		} catch (Throwable t1) {
			edu.tufts.vue.util.Logger.log(t1);
		}
		return union;
	}
		
	private java.util.Vector getIntersectionSearchFields()
	{
		/*
		 Find each repository that will be searched.  Get all the asset types and for each the
		 mandatory record structures.  For each structure find the part structures and their
		 types.  Check these again the VUE set (based on Dublin Core).  Return the VUE names for
		 all that match.
		 */
		java.util.Vector intersections = new java.util.Vector();
		
		try {
			org.osid.repository.Repository[] repositories = sourcesAndTypesManager.getRepositoriesToSearch();
			for (int i=0; i < repositories.length; i++) {
//				 System.out.println("QueryEditor: Respository:"+repositories[i].getDisplayName());
						
				java.util.Vector intersection = new java.util.Vector();		
				// not all these methods may be implemented -- in which case we are out of luck
				try {
					// must support the multi-field search type
					boolean supportsMultiField = false;
					org.osid.shared.TypeIterator typeIterator = repositories[i].getSearchTypes();
                                       
					while ( (!supportsMultiField) && typeIterator.hasNextType() ) {
						org.osid.shared.Type nextSearchType = typeIterator.nextType();
//                                                  System.out.println("QueryEditor: Respository:"+repositories[i].getDisplayName()+" search type:"+nextSearchType.getKeyword()+" check:"+nextSearchType.isEqual(this.multiFieldSearchType));
				
                                               if (nextSearchType.isEqual(this.multiFieldSearchType)) {
							supportsMultiField = true;
						}
					}
					if (!supportsMultiField) {
						// some repository did not have the multi-field search, so stop
						return new java.util.Vector();
					}
					
					typeIterator = repositories[i].getAssetTypes();
					while (typeIterator.hasNextType()) {
						org.osid.shared.Type nextAssetType = typeIterator.nextType();
//						 System.out.println("QueryEditor(AssetTypes): Respository:"+repositories[i].getDisplayName()+" asset type:"+nextAssetType.getKeyword());
				
						org.osid.repository.RecordStructureIterator recordStructureIterator = repositories[i].getMandatoryRecordStructures(nextAssetType);
						while (recordStructureIterator.hasNextRecordStructure()) {
							org.osid.repository.PartStructureIterator partStructureIterator = recordStructureIterator.nextRecordStructure().getPartStructures();
							while (partStructureIterator.hasNextPartStructure()) {
								org.osid.shared.Type nextType = partStructureIterator.nextPartStructure().getType();
								String nextTypeString = edu.tufts.vue.util.Utilities.typeToString(nextType);
                                                                
								int index = advancedSearchUniverseOfTypeStringsVector.indexOf(nextTypeString);
//								System.out.println("QueryEditor(PartType): Respository:"+repositories[i].getDisplayName()+" part type:"+nextTypeString+" index: "+index);
                                                                if (index != -1) {
									String prompt = (String)advancedSearchPromptsVector.elementAt(index);
									if (!intersection.contains(prompt)) {
										intersection.addElement(prompt);
									}
								}
							}
						}
					}
					intersections.addElement(intersection);
				} catch (Throwable t) {
					edu.tufts.vue.util.Logger.log(t);
                                        t.printStackTrace();
				}
			}
			// now find what is common accross intersections
			int numIntersections = intersections.size();
			if (numIntersections == 0) {
				return intersections;
			} else {
				java.util.Vector intersection = (java.util.Vector)intersections.firstElement();
				for (int j=1; j < numIntersections; j++) {
					java.util.Vector nextIntersection = (java.util.Vector)intersections.elementAt(j);
					java.util.Vector newIntersection = new java.util.Vector();

					int numCandidates = intersection.size();
					for (int k=0; k < numCandidates; k++) {
						String nextType = (String)intersection.elementAt(k);
						if (nextIntersection.contains(nextType)) {
							newIntersection.addElement(nextType);
						}
					}
					intersection = newIntersection;
				}
				return intersection;
			}
		} catch (Throwable t1) {
			edu.tufts.vue.util.Logger.log(t1);
		}
		return new java.util.Vector();
	}
	
	private void populateAdvancedSearchUniverseOfTypeStringsVector()
	{
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/contributor@mit.edu");
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/coverage@mit.edu");
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/creator@mit.edu");
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/date@mit.edu");
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/description@mit.edu");
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/format@mit.edu");
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/identifier@mit.edu");
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/language@mit.edu");
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/publisher@mit.edu");
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/relation@mit.edu");
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/rights@mit.edu");
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/source@mit.edu");
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/subject@mit.edu");
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/title@mit.edu");
		advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/type@mit.edu");
                advancedSearchUniverseOfTypeStringsVector.addElement("partStructure/course@mit.edu");
                 
		advancedSearchFieldsText = new String[16];
		int index = 0;
		advancedSearchPromptsVector.addElement("Contributor");
		advancedSearchFieldsText[index++] = null;
		advancedSearchPromptsVector.addElement("Coverage");
		advancedSearchFieldsText[index++] = null;
		advancedSearchPromptsVector.addElement("Creator");
		advancedSearchFieldsText[index++] = null;
		advancedSearchPromptsVector.addElement("Date");
		advancedSearchFieldsText[index++] = null;
		advancedSearchPromptsVector.addElement("Description");
		advancedSearchFieldsText[index++] = null;
		advancedSearchPromptsVector.addElement("Format");
		advancedSearchFieldsText[index++] = null;
		advancedSearchPromptsVector.addElement("Identifier");
		advancedSearchFieldsText[index++] = null;
		advancedSearchPromptsVector.addElement("Language");
		advancedSearchFieldsText[index++] = null;
		advancedSearchPromptsVector.addElement("Publisher");
		advancedSearchFieldsText[index++] = null;
		advancedSearchPromptsVector.addElement("Relation");
		advancedSearchFieldsText[index++] = null;
		advancedSearchPromptsVector.addElement("Rights");
		advancedSearchFieldsText[index++] = null;
		advancedSearchPromptsVector.addElement("Source");
		advancedSearchFieldsText[index++] = null;
		advancedSearchPromptsVector.addElement("Subject");
		advancedSearchFieldsText[index++] = null;
		advancedSearchPromptsVector.addElement("Title");
		advancedSearchFieldsText[index++] = null;
		advancedSearchPromptsVector.addElement("Type");
		advancedSearchFieldsText[index++] = null;
                advancedSearchPromptsVector.addElement("Course");
                advancedSearchFieldsText[index++] = null;
	}
	
	private String multiFieldXML()
	{
		// make XML for all non-blank entries
		StringBuffer buffer = new StringBuffer();
		buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><criteria>");
		for (int i =0; i < advancedFields.length; i++) {
			String value = advancedFields[i].getText();
			if (value.trim().length() > 0) {
				buffer.append("<field><type>");
				
				// determine type
				String prompt = (String)typesVector.elementAt(i);
				String typeString = null;
				int index = advancedSearchPromptsVector.indexOf(prompt);
				if (index == -1) {
					typeString = "partStructure/keywords@mit.edu";
				} else {
					typeString = (String)advancedSearchUniverseOfTypeStringsVector.elementAt(index);
				}
				
				// append elements
				buffer.append(typeString);
				buffer.append("</type><value>");
				buffer.append(value);
				buffer.append("</value><operator>contains</operator></field>");
				if (i > 0) buffer.append("<boolean>and</boolean>");
			}
		}
		buffer.append("</criteria>");
		return buffer.toString();
	}

	public void selectionChanged(LWSelection selection) {
		if (mapBasedSearchCheckBox.isSelected() && !(selection.size() == 1 && selection.get(0) instanceof tufts.vue.LWNode))
		{	this.searchButton1.setEnabled(false);
			field.setText(VueResources.getString("analyzeraction.selectnode"));
		}
		else if (mapBasedSearchCheckBox.isSelected())
		{
			 if (searchButton1.getText() != StopLabel) 
			 {
				 this.searchButton1.setEnabled(true);
				 field.setText("");
			 }
		}
		
	}
}