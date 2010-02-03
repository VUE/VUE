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
package edu.tufts.vue.ui;

import tufts.vue.VueResources;
import fedora.server.types.gen.*;

public class AdvancedQueryEditor
extends javax.swing.JPanel
implements edu.tufts.vue.fsm.QueryEditor,
java.awt.event.ActionListener
{
	private javax.swing.JTextField partField = new javax.swing.JTextField(12);
	private javax.swing.JTextField valueField = new javax.swing.JTextField(20);
	private java.io.Serializable criteria = null;
	private org.osid.shared.Properties searchProperties = null;
	private javax.swing.JPanel AdvancedSearch = new javax.swing.JPanel(new java.awt.BorderLayout());
	private ConditionsTableModel m_model=new ConditionsTableModel();
	private javax.swing.JTable conditionsTable = new javax.swing.JTable(m_model);
	private javax.swing.JComboBox endusersComboBox;
    private javax.swing.CellEditor defaultCellEditor;
	private javax.swing.JComboBox maxReturnsAdvancedSearch;  // combobox for advanced search.
    private String[] maxReturnItems = {"10","20",};
	protected javax.swing.event.EventListenerList listenerList = new javax.swing.event.EventListenerList();
	private org.osid.shared.Type searchType = null;
	
	public AdvancedQueryEditor()
	{
 		try {
			searchProperties = new edu.tufts.vue.util.SharedProperties();		
			maxReturnsAdvancedSearch = new javax.swing.JComboBox(maxReturnItems);
			maxReturnsAdvancedSearch.setEditable(true);
		} catch (Throwable t) {
		}
		
        conditionsTable.setPreferredScrollableViewportSize(new java.awt.Dimension(100,100));
        conditionsTable.addFocusListener(new java.awt.event.FocusListener() {
            public void focusLost(java.awt.event.FocusEvent e) {
                if(conditionsTable.isEditing()) {
                    conditionsTable.getCellEditor(conditionsTable.getEditingRow(),conditionsTable.getEditingColumn()).stopCellEditing();
                }
                conditionsTable.removeEditor();
            }
            public void focusGained(java.awt.event.FocusEvent e) {
            }
        });
		
        javax.swing.JScrollPane conditionsScrollPane = new javax.swing.JScrollPane(conditionsTable);
        conditionsScrollPane.setBorder(javax.swing.BorderFactory.createLineBorder(java.awt.Color.DARK_GRAY));
        javax.swing.JPanel innerConditionsPanel = new javax.swing.JPanel();
        innerConditionsPanel.setLayout(new java.awt.BorderLayout());
        innerConditionsPanel.add(conditionsScrollPane, java.awt.BorderLayout.CENTER);
        
        // GRID: addConditionButton
        javax.swing.JButton addConditionButton = new javax.swing.JButton(VueResources.getString("button.add.label"));
        addConditionButton.setBackground(this.getBackground());
        addConditionButton.setToolTipText(VueResources.getString("advancedqueryeditor.addcondition"));
        // GRID: deleteConditionButton
        javax.swing.JButton deleteConditionButton=new javax.swing.JButton(VueResources.getString("button.delete.label"));
        deleteConditionButton.setBackground(this.getBackground());
        deleteConditionButton.setToolTipText(VueResources.getString("advancedqueryeditor.deletecondition"));
		//        javax.swing.JLabel questionLabel = new javax.swing.JLabel(VueResources.getImageIcon("smallInfo"), JLabel.LEFT);
		//        questionLabel.setPreferredSize(new Dimension(22, 17));
		//        questionLabel.setToolTipText("Add or Delete conditions using +/- buttons. Click on table cell to modify  conditions");
        
        // Now that buttons are available, register the
        // list selection listener that sets their enabled state.
        conditionsTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        
        // setting editors for columns
        // field column.
        try {
            String fields[] = { "identifier","title","language","description","end user", "age range" };
            String endusers[] = { "author","counsellor","learner","manager","parent","teacher","other" };
            String operators[] = { "eq","lt","lte","gt","gte" };
            
            javax.swing.JComboBox fieldsComboBox = new javax.swing.JComboBox(fields);
            fieldsComboBox.addActionListener(this);
            this.endusersComboBox = new javax.swing.JComboBox(endusers);
            javax.swing.JComboBox operatorsComboBox = new javax.swing.JComboBox(operators);
            
            conditionsTable.getColumnModel().getColumn(0).setCellEditor(new javax.swing.DefaultCellEditor(fieldsComboBox));
            conditionsTable.getColumnModel().getColumn(1).setCellEditor(new javax.swing.DefaultCellEditor(operatorsComboBox));
            this.defaultCellEditor = conditionsTable.getColumnModel().getColumn(2).getCellEditor();
        } catch(Exception ex) {
            System.out.println("Can't set the editors"+ex);
        }
        
        ConditionSelectionListener sListener= new ConditionSelectionListener(deleteConditionButton, -1);
        conditionsTable.getSelectionModel().addListSelectionListener(sListener);
        // ..and add listeners to the buttons
        
        
        addConditionButton.addActionListener(new AddConditionButtonListener(m_model));
        deleteConditionButton.addActionListener(new DeleteConditionButtonListener(m_model, sListener));
        
        javax.swing.JPanel topPanel=new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT,2,0));
        topPanel.add(addConditionButton);
        topPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(3,6,3,0));
        topPanel.add(deleteConditionButton);
		//        topPanel.add(questionLabel);
        
        javax.swing.JPanel returnPanel = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT,2, 0));
        returnPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(4,6,6,0));
		//      returnPanel.add(returnLabelAdvancedSearch);
        returnPanel.add(maxReturnsAdvancedSearch);
        
		//        javax.swing.JPanel bottomPanel=new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT,2,0));
		//        bottomPanel.add(advancedSearchButton);
		//        bottomPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(6,6,6,0));
        
        javax.swing.JPanel advancedSearchPanel=new javax.swing.JPanel();
        advancedSearchPanel.setLayout(new javax.swing.BoxLayout(advancedSearchPanel, javax.swing.BoxLayout.Y_AXIS));
        advancedSearchPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(2,6,6,6));
        
        advancedSearchPanel.add(topPanel);
        advancedSearchPanel.add(innerConditionsPanel);
        advancedSearchPanel.add(returnPanel);
		//        advancedSearchPanel.add(bottomPanel);
        
        add(advancedSearchPanel,java.awt.BorderLayout.NORTH);
        validate();
        //advancedSearchPanel.add(advancedSearchButton,BorderLayout.SOUTH);
	}
	
	// return the criteria, no longer than 20 characters worth
	public String getSearchDisplayName() {
		String s =  (String)getCriteria();
		if (s.length() > 20) s = s.substring(0,20) + "...";
		return s;
	}
	
	public void refresh() {
		
	}
		
	public void actionPerformed(java.awt.event.ActionEvent ae) {
		this.criteria = partField.getText() + "::" + valueField.getText();
	}
	
	public java.io.Serializable getCriteria() {
		return partField.getText() + "::" + valueField.getText();
	}
	
	public void setCriteria(java.io.Serializable searchCriteria) {
		if (searchCriteria instanceof String) {
			
			String part = "";
			String value = "";
			
			String criteria = (String)searchCriteria;
			int index = criteria.indexOf("::");
			if (index != -1) {
				part = criteria.substring(0,index);
				value = criteria.substring(index+2);
			}
			
			this.criteria = searchCriteria;
			partField.setText(part);
			valueField.setText(value);
		} else {
			this.criteria = null;
			partField.setText("");		
			valueField.setText("");		
		}
	}
	
	public void setSearchType(org.osid.shared.Type searchType)
	{
		this.searchType = searchType;
	}
	
	public org.osid.shared.Type getSearchType()
	{
		return this.searchType;
	}
	
	public org.osid.shared.Properties getProperties() {
		return this.searchProperties;
	}
	
	public void setProperties(org.osid.shared.Properties searchProperties) {
		this.searchProperties = searchProperties;
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
	
	public class ConditionsTableModel extends javax.swing.table.AbstractTableModel {
        
        java.util.List m_conditions;
        
        public ConditionsTableModel() {
            m_conditions=new java.util.ArrayList();
            Condition cond = new Condition();
            cond.setProperty("");
            cond.setOperator(ComparisonOperator.eq);
            cond.setValue("");
            m_conditions.add(cond);
        }
        
        public ConditionsTableModel(java.util.List conditions) {
            m_conditions=conditions;
        }
        
        public java.util.List getConditions() {
            return m_conditions;
        }
        
        public String getColumnName(int col) {
            if (col==0) {
                return "Field";
            } else if (col==1) {
                return "Operator";
            } else {
                return "Value";
            }
        }
        
        public int getRowCount() {
            return m_conditions.size();
        }
        
        public int getColumnCount() {
            return 3;
        }
        
        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }
        
        public Object getValueAt(int row, int col) {
            Condition cond=(Condition) m_conditions.get(row);
            if (col==0) {
                return cond.getProperty();
            } else if (col==1) {
                return getNiceName(cond.getOperator().toString());
            } else {
                return cond.getValue();
            }
        }
        public boolean isCellEditable(int row, int col) {
            //Note that the data/cell address is constant,
            //no matter where the cell appears onscreen.
            return true;
        }
        
        /*
         * Don't need to implement this method unless your table's
         * data can change.
         */
        
        public void setValueAt(Object value, int row, int col) {
            Condition cond;
            if(row == -1)
                cond  = new Condition();
            else
                cond = (Condition) m_conditions.get(row);
            if(col == 0)
                cond.setProperty((String)value);
			/*
			 if(col == 1) {
				 try {
					 cond.setOperator(ComparisonOperator.fromValue(FedoraUtils.getAdvancedSearchOperatorsActuals((tufts.oki.dr.fedora.DR)dr,(String)value)));
				 } catch (Exception ex) {
					 System.out.println("Value = "+value+": Not supported -"+ex);
					 cond.setOperator(ComparisonOperator.ge);
				 }
			 }
			 */
            if(col == 2)
                cond.setValue((String)value);
            // row = -1 adds new condions else replace the existing one.
            if (row==-1)
                m_conditions.add(cond);
            else
                m_conditions.set(row, cond);
            fireTableCellUpdated(row, col);
        }
        
        private String getNiceName(String operString) {
            if (operString.equals("has")) return "contains";
            if (operString.equals("eq")) return "eq";
            if (operString.equals("lt")) return "lt";
            if (operString.equals("lte")) return "lte";
            if (operString.equals("gt")) return "gt";
            return "gte";
        }
        
    }
    //todo: need to have single thread for all searches
    public class SearchThread extends Thread {
        public void run() {
            
        }
    }
    
    public class ConditionSelectionListener  implements javax.swing.event.ListSelectionListener {
        private int m_selectedRow;
        private javax.swing.JButton m_deleteButton;
        
        public ConditionSelectionListener(javax.swing.JButton deleteButton, int selectedRow) {
            m_selectedRow=selectedRow;
            m_deleteButton=deleteButton;
            updateButtons();
        }
        
        public void valueChanged(javax.swing.event.ListSelectionEvent e) {
            //Ignore extra messages.
            if (e.getValueIsAdjusting()) return;
            
            javax.swing.ListSelectionModel lsm = (javax.swing.ListSelectionModel)e.getSource();
            if (lsm.isSelectionEmpty()) {
                m_selectedRow=-1;
            } else {
                m_selectedRow=lsm.getMinSelectionIndex();
            }
            updateButtons();
        }
        
        public int getSelectedRow() {
            return m_selectedRow;
        }
        
        private void updateButtons() {
            if (getSelectedRow()==-1) {
                m_deleteButton.setEnabled(false);
            } else {
                m_deleteButton.setEnabled(true);
            }
        }
    }
    
    public class AddConditionButtonListener
		implements java.awt.event.ActionListener {
			
			private ConditionsTableModel m_model;
			
			public AddConditionButtonListener(ConditionsTableModel model) {
				m_model=model;
				
			}
			
			public void actionPerformed(java.awt.event.ActionEvent e) {
				Condition cond=new Condition();
				/*
				 try {
					 cond.setProperty(FedoraUtils.getAdvancedSearchFields((tufts.oki.dr.fedora.DR)dr)[0]);
				 } catch (Exception ex) {
					 */
                cond.setProperty("label");
				//            }
				cond.setOperator(ComparisonOperator.has);
				cond.setValue("");
				m_model.getConditions().add(cond);
				m_model.fireTableDataChanged();
			}
			
		}
    
    public class DeleteConditionButtonListener
		implements java.awt.event.ActionListener {
			
			private ConditionsTableModel m_model;
			private ConditionSelectionListener m_sListener;
			
			public DeleteConditionButtonListener(ConditionsTableModel model,
												 ConditionSelectionListener sListener) {
				m_model=model;
				m_sListener=sListener;
			}
			
			public void actionPerformed(java.awt.event.ActionEvent e) {
				// will only be invoked if an existing row is selected
				int r=m_sListener.getSelectedRow();
				m_model.getConditions().remove(r);
				m_model.fireTableRowsDeleted(r,r);
			}
		}
}