package tufts.vue;


import java.lang.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import tufts.vue.beans.*;

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
	
		
	/** was the okay button hit? **/
	boolean mIsOkay = false;	
	
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
		
		JLabel listTitle = new JLabel("User TYpes:");
		Box listControls = Box.createHorizontalBox();
		mAddTypeButton = createButton( "New TYpe");
		mRemoveTypeButton = createButton( "Remove Type");
		listControls.add( mAddTypeButton );
		listControls.add( mRemoveTypeButton);
		
		mListModel = new DefaultListModel();
		
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
		
		mAddPropertyButton = createButton("Add");
		mRemovePropertyButton = createButton( "Remove");
		mUpButton = createButton( "Nudge Up");
		mDownButton = createButton( "Nudge Down");
		JLabel propTitle = new JLabel( "Properties:");
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
		
	}
	
	
	private void initPropertyTable() {
		
		String [] columns = {"Property", "Type", "Value"};
		mTableModel = new DefaultTableModel( columns, 0);
		
		
		mTable = new JTable( mTableModel);
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
		
		mAddPropertyButton.enable( ts);
		mRemovePropertyButton.enable( ts);
		mUpButton.enable( ts);
		mDownButton.enable( ts);
		if( ts) {
			boolean ps = (mTable.getSelectedRow() != -1);
			mRemovePropertyButton.enable( ps);
			mUpButton.enable(  ps);
			mDownButton.enable( ps);
			}
			
		boolean hm = (mMap != null);
		mAddTypeButton.enable( hm);
		mRemoveTypeButton.enable( hm);
		if( hm) {
			mRemoveTypeButton.enable( mTypeList.getSelectedIndex() >= 0);
			}
	}
	
	private void setUserType( UserMapType pType) {
		if( mCurType != pType) {
			mCurType = pType;
			
			}
	}
	private UserMapType getUserType() {
		return mCurType;
	}
	
	public void doAddType() {
		String name = null;
		
		name = JOptionPane.showInputDialog("Name the new user type:");
		if( name != null) {
			UserMapType type = new UserMapType( mMap, name);
			mListModel.addElement( type);
			//System.out.println(" -- added item: "+name);
			//System.out.println("  curent size is:  "+mListModel.size() );
			}
	}
	
	public void doRemoveType() {
	
	}
	
	public void doAddProperty() {
		
		String name = null;
		name = JOptionPane.showInputDialog("Property name:");
		if( name != null) {
			UserProperty prop = mCurType.createUserProperty( name);
			Object [] objs = new Object[3];
			objs[0] = prop;
			objs[1] = "String";   // FIX: do lookup
			objs [2] = new String("");
			
			mTableModel.addRow( objs );
			if( mCurType != null)
				mCurType.addUserProperty( prop);
			}
	}
	
	public void doRemoveProperty() {
	
	}
	
	public void doOkay() {
		mDialog.dispose();
	}
	
	public boolean displayDialog() {
		mDialog  = new JDialog( VUE.frame, true);
		mDialog.getContentPane().add( this);
		mDialog.pack();
		mDialog.show();
		
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
		updateEnabledStates();
	
	}

	


}