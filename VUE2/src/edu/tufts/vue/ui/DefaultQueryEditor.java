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
implements edu.tufts.vue.fsm.QueryEditor,
		   java.awt.event.ActionListener
{
	private javax.swing.JTextField field = new javax.swing.JTextField(20);
	private java.io.Serializable criteria = null;
	private org.osid.shared.Properties searchProperties = null;
	
	public DefaultQueryEditor() {
		add(new javax.swing.JLabel("Query: "));
		add(field);
		try {
			searchProperties = new edu.tufts.vue.util.SharedProperties();		
		} catch (Throwable t) {
		}
	}
	
	public void actionPerformed(java.awt.event.ActionEvent ae) {
		this.criteria = field.getText();
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
}