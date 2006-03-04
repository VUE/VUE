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
	
	private javax.swing.JTextField field = new javax.swing.JTextField(20);
	private java.io.Serializable criteria = null;
	private org.osid.shared.Properties searchProperties = null;
	
	protected javax.swing.event.EventListenerList listenerList = new javax.swing.event.EventListenerList();
	
	private org.osid.repository.Repository[] repositories;
	
	private javax.swing.JButton searchButton = new javax.swing.JButton("Search");
	private static final String SELECT_A_LIBRARY = "Select a library to search";
	private static final String NO_MESSAGE = "";
	private javax.swing.JLabel selectMessage = new javax.swing.JLabel(SELECT_A_LIBRARY);
	
	public DefaultQueryEditor() {
		try {
			gbConstraints.anchor = java.awt.GridBagConstraints.WEST;
			gbConstraints.insets = new java.awt.Insets(2,2,2,2);
			setLayout(gbLayout);
	
			setSize(new java.awt.Dimension(100,100));
			setPreferredSize(new java.awt.Dimension(100,100));
			setMinimumSize(new java.awt.Dimension(100,100));
			
			gbConstraints.gridx = 0;
			gbConstraints.gridy = 0;
			add(new javax.swing.JLabel("Keyword:"),gbConstraints);
			
			gbConstraints.gridx = 1;
			gbConstraints.gridy = 0;
			add(field,gbConstraints);
			field.addActionListener(this);
			
			gbConstraints.gridx = 1;
			gbConstraints.gridy = 1;
			add(searchButton,gbConstraints);
			searchButton.addActionListener(this);
			
			gbConstraints.gridx = 1;
			gbConstraints.gridy = 2;
			add(selectMessage,gbConstraints);
			searchButton.addActionListener(this);
			refresh();
			
			searchProperties = new edu.tufts.vue.util.SharedProperties();		
		} catch (Throwable t) {
		}
	}
	
	public void refresh()
	{
		// behavior depends on the whether any repository is selected
		repositories = sourcesAndTypesManager.getRepositoriesToSearch();
		if (repositories.length == 0) {
			searchButton.setEnabled(false);
			selectMessage.setText(SELECT_A_LIBRARY);
		} else {
			searchButton.setEnabled(true);
			selectMessage.setText(NO_MESSAGE);
		}
		repaint();
		validate();
	}
	
	public void actionPerformed(java.awt.event.ActionEvent ae)
	{
		this.criteria = field.getText();
		fireSearch(new edu.tufts.vue.fsm.event.SearchEvent(this));
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