package edu.tufts.vue.ui;

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
 * <p>The entire file consists of original code.  Copyright &copy; 2006 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

public class DefaultQueryEditor
extends javax.swing.JPanel
implements edu.tufts.vue.fsm.QueryEditor, java.awt.event.ActionListener
{
	private edu.tufts.vue.fsm.FederatedSearchManager fsm = edu.tufts.vue.fsm.impl.VueFederatedSearchManager.getInstance();
	private edu.tufts.vue.fsm.SourcesAndTypesManager sourcesAndTypesManager = edu.tufts.vue.fsm.impl.VueSourcesAndTypesManager.getInstance();
	
	private java.awt.GridBagLayout gbLayout = new java.awt.GridBagLayout();
	private java.awt.GridBagConstraints gbConstraints = new java.awt.GridBagConstraints();
	
	private javax.swing.JTextField field = new javax.swing.JTextField(15);
	private java.io.Serializable criteria = null;
	private org.osid.shared.Properties searchProperties = null;
	
	protected javax.swing.event.EventListenerList listenerList = new javax.swing.event.EventListenerList();
	
	private org.osid.repository.Repository[] repositories;
	
	private javax.swing.JButton searchButton = new javax.swing.JButton("Search");
	private javax.swing.JButton addButton = new javax.swing.JButton("Add");
	private static final String SELECT_A_LIBRARY = "Select a library";
	private static final String NO_MESSAGE = "";
	private javax.swing.JLabel selectMessage = new javax.swing.JLabel(SELECT_A_LIBRARY);
	
	private javax.swing.JButton moreOptionsButton = new javax.swing.JButton("+");
	private static final String MORE_OPTIONS = "";
	private javax.swing.JLabel moreOptionsLabel = new javax.swing.JLabel(MORE_OPTIONS);

	private javax.swing.JButton fewerOptionsButton = new javax.swing.JButton("-");
	private static final String FEWER_OPTIONS = "";
	private javax.swing.JLabel fewerOptionsLabel = new javax.swing.JLabel(FEWER_OPTIONS);
	
	private javax.swing.JPanel moreFewerPanel = new javax.swing.JPanel();
	
	private static final int NOTHING_SELECTED = 0;
	private static final int BASIC = 1;
	private static final int ADVANCED_INTERSECTION = 2;
	private static final int ADVANCED_UNION = 3;
	private int currentStyle = BASIC;
	
	public DefaultQueryEditor() {
		try {
			gbConstraints.anchor = java.awt.GridBagConstraints.WEST;
			gbConstraints.insets = new java.awt.Insets(2,2,2,2);
			setLayout(gbLayout);
	
			setSize(new java.awt.Dimension(100,100));
			setPreferredSize(new java.awt.Dimension(100,100));
			setMinimumSize(new java.awt.Dimension(100,100));
			
			searchButton.addActionListener(this);
			moreOptionsButton.addActionListener(this);
			fewerOptionsButton.addActionListener(this);
			searchProperties = new edu.tufts.vue.util.SharedProperties();		

			repositories = sourcesAndTypesManager.getRepositoriesToSearch();
			if (repositories.length == 0) {
				makePanel(NOTHING_SELECTED);
			} else {
				makePanel(BASIC);
			}
		} catch (Throwable t) {
		}
	}
	
	public void refresh()
	{
		repositories = sourcesAndTypesManager.getRepositoriesToSearch();
		if (repositories.length == 0) {
			makePanel(NOTHING_SELECTED);
		} else {
			makePanel(this.currentStyle);
		}
	}
	
	private void makePanel(int kind)
	{
		removeAll();
		switch (kind) {
			case NOTHING_SELECTED:
				makeNothingSelectedPanel();
				break;
			case BASIC:
				makeBasicPanel();
				break;
			case ADVANCED_INTERSECTION:
				makeAdvancedIntersectionPanel();
				break;
			case ADVANCED_UNION:
				makeAdvancedUnionPanel();
				break;
		}
		repaint();
		validate();
	}
	
	private void makeNothingSelectedPanel()
	{
		gbConstraints.gridx = 0;
		gbConstraints.gridy = 0;
		add(new javax.swing.JLabel("Keyword:"),gbConstraints);
		
		gbConstraints.gridx = 1;
		gbConstraints.gridy = 0;
		add(field,gbConstraints);
		field.addActionListener(this);
		
		gbConstraints.gridx = 0;
		gbConstraints.gridy = 1;
		add(selectMessage,gbConstraints);
		
		gbConstraints.gridx = 1;
		gbConstraints.gridy = 1;
		add(searchButton,gbConstraints);
		searchButton.setEnabled(false);
	}
	
	private void makeBasicPanel()
	{
		gbConstraints.gridx = 0;
		gbConstraints.gridy = 0;
		add(new javax.swing.JLabel("Keyword:"),gbConstraints);
		
		gbConstraints.gridx = 1;
		gbConstraints.gridy = 0;
		add(field,gbConstraints);
		field.addActionListener(this);
		
		gbConstraints.gridx = 0;
		gbConstraints.gridy = 1;
		add(moreOptionsButton,gbConstraints);

		gbConstraints.gridx = 1;
		gbConstraints.gridy = 1;
		add(searchButton,gbConstraints);
		searchButton.setEnabled(true);
		this.currentStyle = BASIC;
	}
	
	private void makeAdvancedIntersectionPanel()
	{
		gbConstraints.gridx = 0;
		gbConstraints.gridy = 0;
		add(new javax.swing.JLabel("Keyword:"),gbConstraints);
		
		gbConstraints.gridx = 1;
		gbConstraints.gridy = 0;
		add(field,gbConstraints);
		field.addActionListener(this);
		
		gbConstraints.gridx = 0;
		gbConstraints.gridy = 1;
		add(fewerOptionsButton,gbConstraints);
		
		gbConstraints.gridx = 0;
		gbConstraints.gridy = 2;
		add(moreOptionsButton,gbConstraints);
		
		gbConstraints.gridx = 1;
		gbConstraints.gridy = 2;
		add(searchButton,gbConstraints);
		searchButton.setEnabled(true);
		this.currentStyle = ADVANCED_INTERSECTION;
	}
	
	private void makeAdvancedUnionPanel()
	{
		gbConstraints.gridx = 0;
		gbConstraints.gridy = 0;
		add(new javax.swing.JLabel("Keyword:"),gbConstraints);
		
		gbConstraints.gridx = 1;
		gbConstraints.gridy = 0;
		add(field,gbConstraints);
		field.addActionListener(this);
		
		gbConstraints.gridx = 0;
		gbConstraints.gridy = 1;
		add(fewerOptionsButton,gbConstraints);
		
		gbConstraints.gridx = 1;
		gbConstraints.gridy = 1;
		add(searchButton,gbConstraints);
		searchButton.setEnabled(true);
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
			if (this.currentStyle == ADVANCED_UNION) {
				makePanel(ADVANCED_INTERSECTION);
			} else {
				makePanel(BASIC);
			}
		} else {
			this.criteria = field.getText();
			fireSearch(new edu.tufts.vue.fsm.event.SearchEvent(this));
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
		Object[] listeners = listenerList.getListenerList();
		for (int i=0; i<listeners.length; i+=2) {
			if (listeners[i] == edu.tufts.vue.fsm.event.SearchListener.class) {
				((edu.tufts.vue.fsm.event.SearchListener)listeners[i+1]).searchPerformed(evt);
			}
		}
	}
	
	public java.io.Serializable getCriteria() {
		return field.getText();
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

	public String getSearchDisplayName() {
		// return the criteria, no longer than 20 characters worth
		String s =  (String)getCriteria();
		if (s.length() > 20) s = s.substring(0,20) + "...";
		return s;
	}
}