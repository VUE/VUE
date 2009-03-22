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


import java.lang.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import tufts.vue.filter.*;

/**
 * @version $Revision: 1.10 $ / $Date: 2009-03-22 07:19:52 $ / $Author: vaibhav $
 */
public class MapTypeDialog extends JPanel  implements ActionListener, ListSelectionListener {

	///////////////
	// Statics
	////////////////
	
	public static final int STRING = 1;
	public static final int INTEGER = 2;
	public static final int NUMBER = 3;
	public static final int TAGGED = 4;
	public static final int BOOLEAN = 5;
	
	// FIX:  resources
	// private static String [] sTypeNames = VueResources.getStringArray( "userPropertyTYpes");
	private static String [] sTypeNames = { "String", "Integer", "Number", "Choice", "Boolean" };
	
	////////////
	// Fields
	/////////////
	
	JDialog mDialog = null;
	
	/** the map **/
	LWMap mMap = null;
	
	/** MapTypes array to edit **/
	Vector mMapTypes = null;
	
	/** add type button **/
	JButton mAddTypeButton =  null;
	
	/** remove type button **/
	JButton mRemoveTypeButton = null;
	
	/** add property button **/
	JButton mAddPropertyButton = null;
	
	/** remove property button **/
	JButton mRemovePropertyButton = null;
	
	/** nudge up button **/
	JButton mUpButton = null;
	JButton mDownButton = null;
	
	JButton mOkayButton = null;
	
	JButton mCancelButton = null;
	
	
	/** list of types **/
	JList mTypeList = null;
	DefaultListModel mListModel = null;
	
	/** property table **/
	JTable mTable = null;
	
	UserMapType mCurType = null;
	
	DefaultTableModel mTableModel = null;
	PropertyTableModel mPropertyTableModel = null;
	
	JComboBox mTypeEditor = null;
		
	/** was the okay button hit? **/
	boolean mIsOkay = false;	
	
	/** new property name count **/
	int mCount = 0;
	
	////////////////
	// Constructors
	////////////////
	
	
	public MapTypeDialog( LWMap pMap) {
		super();
		mMap = pMap;
		initializeComponents();
		updateEnabledStates();
	}
	
	
	//////////////
	// Methods
	//////////////

	private void initializeComponents() {
		
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout( new BorderLayout() );
		
		JPanel listPanel = new JPanel();
		listPanel.setLayout( new BorderLayout() );
		
		JLabel listTitle = new JLabel(VueResources.getString("list.usertype.label"));
		Box listControls = Box.createHorizontalBox();
		mAddTypeButton = createButton( VueResources.getString("button.newtype.label"));
		mRemoveTypeButton = createButton( VueResources.getString("button.removetype.label"));
		listControls.add( mAddTypeButton );
		listControls.add( mRemoveTypeButton);
		
		
		UserMapType [] types = null;
		if( mMap != null) {
			types = mMap.getUserMapTypes();
			}
		mListModel = new DefaultListModel();
		if( types != null) {
			for(int i=0; i< types.length; i++)  {
				mListModel.addElement( types[i] );
				}
			}
		// mListModel.addElement( "Default");
		mTypeList = new JList( mListModel);
		mTypeList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION);
		mTypeList.addListSelectionListener( this);
		
		//mTypeList.setModel( mListModel);
		
		JScrollPane listScroll = new JScrollPane();
		listScroll.setVerticalScrollBarPolicy(javax.swing.JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		listScroll.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		listScroll.getViewport().add(  mTypeList);
		listPanel.add( BorderLayout.CENTER, listScroll);
		listPanel.add( BorderLayout.NORTH, listTitle);
		listPanel.add( BorderLayout.SOUTH, listControls);
		
		JPanel propPanel = new JPanel();
		propPanel.setLayout( new BorderLayout() );
		
		mAddPropertyButton = createButton(VueResources.getString("button.add.label"));
		mRemovePropertyButton = createButton(VueResources.getString("button.remove.label"));
		mUpButton = createButton( VueResources.getString("button.nudgeup.label"));
		mDownButton = createButton( VueResources.getString("button.nudgedown.label"));
		JLabel propTitle = new JLabel(VueResources.getString("label.properties"));
		Box propControls = Box.createVerticalBox();
		propControls.add( mUpButton );
		propControls.add( mDownButton);
		propControls.add( mRemovePropertyButton);
		propControls.add( mAddPropertyButton);
		
		initPropertyTable();
		JScrollPane tablePanel = new JScrollPane( mTable);
		tablePanel.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		tablePanel.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		
		propPanel.add( BorderLayout.NORTH, propTitle);
		propPanel.add( BorderLayout.EAST, propControls);
		propPanel.add( BorderLayout.CENTER, tablePanel);
		
		Box mainControls = Box.createHorizontalBox();
		mOkayButton = createButton("Okay");
		mainControls.add( mOkayButton);
		
		mainPanel.add( BorderLayout.WEST, listPanel);
		mainPanel.add( BorderLayout.SOUTH, mainControls);
		mainPanel.add( BorderLayout.CENTER, propPanel);
		add( BorderLayout.CENTER, mainPanel);
		
		if( mListModel.size() > 0 ) {
			mTypeList.setSelectedIndex( 0);
			}
	}
	
	
	private void initPropertyTable() {
		
		String [] columns = {VueResources.getString("column.property.name"), VueResources.getString("column.type.name"), VueResources.getString("column.value.name")};
		mTableModel = new DefaultTableModel( columns, 0);
		mPropertyTableModel = new PropertyTableModel( null);
		
		mTable = new JTable( mPropertyTableModel);
		mTable.setSelectionMode( ListSelectionModel.SINGLE_SELECTION);
		mTable.setRowSelectionAllowed( true);
		mTable.setColumnSelectionAllowed( false);
		mTable.getSelectionModel().addListSelectionListener( this);
		
		mTypeEditor = new JComboBox( UserProperty.sPropertyTypeNames );

		TableColumn typeColumn = mTable.getColumnModel().getColumn(1);
		typeColumn.setCellEditor(new DefaultCellEditor( mTypeEditor));
	
	}
	
	public JButton createButton( String pText) {
		JButton button = null;
		if( pText == null)
			button = new JButton();
		else
			button = new JButton( pText);
		button.addActionListener( this);
		return button;
	}
	
	private void updateEnabledStates() {
		boolean ts = (mCurType != null);
		
		mAddPropertyButton.setEnabled( ts);
		mRemovePropertyButton.setEnabled( ts);
		mUpButton.setEnabled( ts);
		mDownButton.setEnabled( ts);
		if( ts) {
			boolean ps = (mTable.getSelectedRow() != -1);
			mRemovePropertyButton.setEnabled( ps);
			
			int row = mTable.getSelectedRow();
			if( row == -1) {
				mUpButton.setEnabled(  false);
				mDownButton.setEnabled( false);
				}
			else {
				mUpButton.setEnabled( row != 0);
				mDownButton.setEnabled( row < (mTable.getRowCount() -1));
				}
			
			}
			
		boolean hm = (mMap != null);
		mAddTypeButton.setEnabled( hm);
		mRemoveTypeButton.setEnabled( hm);
		if( hm) {
			mRemoveTypeButton.setEnabled( mTypeList.getSelectedIndex() >= 0);
			}
	}
	
	private void setUserType( UserMapType pType) {
		if( mCurType != pType) {
			mCurType = pType;
			mPropertyTableModel.setUserType( pType);
			
			}
	}
	private UserMapType getUserType() {
		return mCurType;
	}
	
	public void doAddType() {
		String name = null;
		
		name = JOptionPane.showInputDialog(VueResources.getString("dialog.newuser.title"));
		if( name != null) {
			UserMapType type = new UserMapType( mMap, name);
			mListModel.addElement( type);
			mTypeList.setSelectedValue( type, true);
			//System.out.println(" -- added item: "+name);
			//System.out.println("  curent size is:  "+mListModel.size() );
			}
	}
	
	public void doRemoveType() {
		Object obj = mTypeList.getSelectedValue();
		if( obj != null) {
			int index = mTypeList.getSelectedIndex();
			UserMapType type = (UserMapType) obj;
			mListModel.removeElement( type);
			if( mListModel.size() > 0 ) {
				if( index > 0)
					index--;			
				mTypeList.setSelectedIndex( index);
				}
			}
	}
	
	public void doAddProperty() {
		
		String name = null;
		//name = JOptionPane.showInputDialog("Property name:");
		
		name = "Property "+mCount++;
		if( name != null) {
			UserProperty prop = mCurType.createUserProperty( name);
			Object [] objs = new Object[3];
			objs[0] = prop;
			objs[1] = "String";   // FIX: do lookup
			objs [2] = new String("");
			
			mTableModel.addRow( objs );
			if( mCurType != null)
				mCurType.addUserProperty( prop);
				mPropertyTableModel.fireTableDataChanged();
			}
	}
	
	public void doRemoveProperty() {
		if( mCurType != null) {
			int index = mTable.getSelectedRow();
			if( index < 0 )
				return;
			UserProperty [] props = mCurType.getUserProperties();
			mCurType.removeUserProperty( props[ index] );
			
			mPropertyTableModel.fireTableDataChanged();
		}
	}
	
	public void doNudgeUp() {
		
		if( mCurType != null) {
			int selection = mTable.getSelectedRow();
			UserProperty property = null;
			UserProperty [] rows = mCurType.getUserProperties();
			if( rows != null) {
				if( (selection >= 0) && ( selection < rows.length) ) {
					mCurType.nudgeUserProperty( rows[ selection], true);
					mPropertyTableModel.fireTableDataChanged();
					}
				}
			}
	}
	
	public void doNudgeDown() {
		
		if( mCurType != null) {
			int selection = mTable.getSelectedRow();
			UserProperty property = null;
			UserProperty [] rows = mCurType.getUserProperties();
			if( rows != null) {
				if( (selection >= 0) && ( selection < rows.length) ) {
					mCurType.nudgeUserProperty( rows[ selection], false);
					mPropertyTableModel.fireTableDataChanged();
					}
				}
			}
	}
	
	
	public void doOkay() {
		
		UserMapType [] types = null;
		
		if( !mListModel.isEmpty() ) {
			 types = new UserMapType[ mListModel.size() ];
			for( int i= 0; i< mListModel.size(); i++) {
				types[ i] = (UserMapType) mListModel.elementAt( i);
				}
			}
		mMap.setUserMapTypes( types);
	
	
		mDialog.dispose();
	}
	
	public boolean displayDialog() {
            mDialog  = new JDialog(VUE.getDialogParentAsFrame(), true);
            mDialog.getContentPane().add( this);
            mDialog.pack();
            mDialog.setVisible(true);
            
            return mIsOkay;
	}	


	public void valueChanged( ListSelectionEvent pEvent) {
		Object source = pEvent.getSource();
		if( source == mTypeList ) {
			int index = mTypeList.getSelectedIndex();
			Object obj = null;
			if( index >= 0 ) {
				obj = mListModel.elementAt( index);
				}
			UserMapType type = null;
			if( obj instanceof UserMapType) {
				type = (UserMapType) obj;
				}
			setUserType( type);
			
			}
		if( source == mTable) {
			System.out.println( "table selection.");
			}
	updateEnabledStates();
	}
	
	public void actionPerformed( ActionEvent pEvent) {
		
		Object source = pEvent.getSource();
		
		if( source == mOkayButton) {
			doOkay();
			}
		if( source == mAddTypeButton) {
			doAddType();
			}
		if( source == mRemoveTypeButton ) {
			doRemoveType();
			}
		if( source == mAddPropertyButton ) {
			doAddProperty();
			}
		if( source == mRemovePropertyButton ) {
			doRemoveProperty();
			}
		if( source == mUpButton ) {
			doNudgeUp();
			}
		if( source == mDownButton ) {
			doNudgeDown();
			}
		updateEnabledStates();
	
	}

	

	public class PropertyTableModel extends AbstractTableModel {
		
		UserMapType mModelType = null;
 String [] 		sColNames = {VueResources.getString("column.property.name"), VueResources.getString("column.type.name"), VueResources.getString("column.value.name")};
		public PropertyTableModel( UserMapType pType) {
			super();
			mModelType = pType;
		}

		
		public void setUserType( UserMapType pType) {
			mModelType = pType;
			fireTableDataChanged();
		}
		
		public UserMapType getUserType() {
			return mModelType;
		}
		
		public int getRowCount() {
  			int rows = 0;
  			if( mModelType != null) {
  				UserProperty [] names = mModelType.getUserProperties();
  				if( names != null)
  					rows = names.length;
  				}
  			return rows;
 		}
  
		public int getColumnCount() {
			return 3;
		}
		
		public Object getValueAt(int row, int column) {
			Object value = null;
			if( mModelType != null) {	
				UserProperty  [] rows = mModelType.getUserProperties();
				if( column == 0) 
					value = rows[ row].getDisplayName();
				if( column == 1) {
					int type = rows[ row].getType();
					value = UserProperty.sPropertyTypeNames[ type];
					}
				if( column == 2) {
					value = rows[ row].getValue();
					}
				}
			return value;
		}
		
		public String getColumnName( int pCol) {
			return sColNames[ pCol];
		}
		public void setValueAt( Object pValue, int pRow, int pCol) {
			System.out.println("  setvalueAt: row" +pRow+ " col: "+pCol+" to: "+pValue.toString() );
			
			if( mModelType != null) {
				UserProperty [] rows = null;
				UserProperty prop = null;
				
				if( mCurType != null) { 
					rows = mCurType.getUserProperties();
					}
				if( rows != null) {
					prop = rows[ pRow];
					}
				if( prop != null) {
					if( pCol == 0) {
						prop.setDisplayName( (String) pValue);
						}
					if( pCol == 1) {
						prop.setType( UserProperty.findTypeByName( (String) pValue) );
						System.out.println("setting type");
						}
					if( pCol == 2) {
						prop.setValue( (String) pValue) ;
						}
					}
			}
	
		}
		
	public boolean isCellEditable( int pRow, int pCol) {
		return true;
	}
	}
}