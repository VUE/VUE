/*******
**  ObjectInspectorPanel
**
**
*********/

package tufts.vue;


import java.io.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;


/**
* ObjectInspectorPanel
*
* The Object Inspector Panel!
*
\**/
public class ObjectInspectorPanel  extends JPanel 
			implements LWSelection.Listener
{


	/////////////
	// Statics
	//////////////
	
	private static final String kNullType = "null";
	private static final String kNodeType = "node";
	private static final String kLinkType = "link";
	private static final String kAssetType = "asset";
	
	/////////////
	// Fields
	//////////////
	
	/** the card panel **/
	JPanel mCardPanel = null;
	
	/** the card layout **/
	CardLayout mCards = null;
	
	/** **/
	NodeInspectorPanel mNodeCard = null;
	
	/** **/
	LinkInspectorPanel  mLinkCard = null;
	
	/** asset card **/
	AssetInspectorPanel  mAssetCard = null;
	
	
	/** no selection card **/
	JPanel mNullCard = null;
	
	
	/** the selected Object to inspect **/
	LWComponent mObject = null;
	
	/** the selected resource to sinpect **/
	Resource mAsset = null;
	
	
	
	///////////////////
	// Constructors
	////////////////////
	
	public ObjectInspectorPanel() {
		super();
		
		setLayout( new BorderLayout()  );
		setBorder( new EmptyBorder( 5,5,5,5) );
		
		VueResources.initComponent( this, "tabPane");
		
		mCardPanel = new JPanel();
		mCards = new CardLayout();
		mCardPanel.setLayout( mCards );
		
		mNullCard = new JPanel();
		mNullCard.setLayout( new BorderLayout() );
		mNullCard.add( BorderLayout.CENTER, new JLabel("No Selection"));
		
		mNodeCard = new NodeInspectorPanel();
		
		mLinkCard = new LinkInspectorPanel();
		
		mAssetCard = new AssetInspectorPanel();
		
		mCards.addLayoutComponent( kNullType,  mNullCard);
		mCards.addLayoutComponent( kNodeType,  mNodeCard);
		mCards.addLayoutComponent( kLinkType,  mLinkCard);
		mCards.addLayoutComponent( kAssetType,  mAssetCard);
		
		add( mCardPanel);
		setCard( mNullCard);
		//add( BorderLayout.CENTER, mCardPanel);
	}
	
	
	
	////////////////////
	// Methods
	///////////////////
	
	
	JPanel mCurCard = null;
	
	private void setCard( JPanel pCard) {
		if( mCurCard != null) {
			mCardPanel.remove( mCurCard);
			}
		mCurCard = pCard;
		if( pCard != null) {
			debug( "  setting card to: "+pCard.getClass().getName() );
			mCardPanel.add( BorderLayout.CENTER, pCard);
			pCard.show();
			validate();
			repaint();
			}
	}
	/**
	 * setMap
	 * Sets the LWMap component and updates teh display
	 *
	 * @param pMap - the LWMap to inspect
	 **/
	public void setLWComponent( LWComponent pObject) {
		
		// if we have a change in maps... 
		if( pObject != mObject) {
			mObject = pObject;
			
			if( mObject == null) {
				debug("  null type");
				//mCards.show( mCardPanel, kNullType);
				setCard( mNullCard);
				}
			else
			if( mObject instanceof LWNode ) {
				debug("  node type selection");
				mNodeCard.setNode( (LWNode) mObject);
				//mCards.show( mCardPanel, kNodeType);
				setCard( mNodeCard);
				}
			else
			if( mObject instanceof  LWLink ) {
				debug("  link selection");
				mLinkCard.setLink( (LWLink) mObject);
				//mCards.show( mCardPanel, kLinkType);
				setCard( mLinkCard);
				}
			else {
				debug("  unknown selection: "+ pObject.getClass().getName() );
				}
			
			}
	}
	
	
	/**
	 * updatePanels
	 * This method updates the panel's content pased on the selected
	 * Map
	 *
	 **/
	public void updatePanels() {
		
	}
	
	//////////////////////
	// OVerrides
	//////////////////////


	public Dimension getPreferredSize()  {
		Dimension size =  super.getPreferredSize();
		if( size.getWidth() < 200 ) {
			size.setSize( 200, size.getHeight() );
			}
		if( size.getHeight() < 250 ) {
			size.setSize( size.getWidth(), 250);
			}
		return size;
	}

	




	/////////////
		// LWSelection.Listener Interface Implementation
		/////////
		public void selectionChanged( LWSelection pSelection) {
			
			LWComponent lwc = null;
			if( pSelection.size() == 1 )  {
				debug( "Object Inspector single selection");
				lwc = pSelection.first();
				}
			else {
				debug("ObjectInspector item selection size is: "+ pSelection.size() );
				}
			setLWComponent( lwc);
			
		}
	
	
	
	
	/////////////////
	// Inner Classes
	////////////////////
	
	
	
	


	private static boolean sDebug = true;
	private void debug( String str) {
		if( sDebug) {
			System.out.println( str);
			}
	}
}