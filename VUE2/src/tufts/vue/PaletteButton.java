

package tufts.vue;



import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


/**
* PaletteButton
* This class provides a popup radio button selector component.
* It is used for the main tool bar tool
*
* @author csb
* @version 1.0
**/
public class PaletteButton extends JRadioButton implements ActionListener {

	
	
	//////////////////
	//  Fields
	//////////////////
	
	/* this is thr eolumn threshold array to tell when to add another columng in the palette */
 	static int mColThreshold[] = VueResources.getIntArray("menuFlowThreshold") ;
	
	// default offsets for drawing popup arrow via code
	public int mArrowSize = 3;
	public int mArrowHOffset  = -9;
	public int mArrowVOffset = -7;
	
	/** the popup overlay icons for up and down states **/
	public Icon mPopupIndicatorIconUp = null;
	public Icon mPopupIndicatorDownIcon = null;

		/** The currently selected palette item--if any **/
	protected PaletteButtonItem mCurSelection = null;
	
		/** the set of palette items for this buttons menu **/
	protected PaletteButtonItem [] mItems = null;
	
		/** does this button have a popup? **/
	protected boolean mHasPopup = false;
	
	/** do we need to draw an arrow via code **/
	protected boolean mDrawArrowByCode = false;
	
		/** the popup menu **/
	protected JPopupMenu mPopup = null;

		/** are we drawing the popup icon indcator with an image or in code? **/
	protected boolean mUseIconIndicator = true;
	
		/* the context object */
	private Object mContext = null;
	
		/** the current overlay popup indicator icon **/
	protected Icon mPopupIndicatorIcon = null;	
	protected Icon mPopupIndicatorUpIcon = null;	
	
	

	
	
	
	///////////////
	// Constructors
	////////////////
	
	
	
	
	/**
	 * Constructor
	 *
	 * Creates a new PaletteButton with no menus
	 *
	 **/
	public PaletteButton() {
		super();
		setBorder( null);
	}
	
	
	/**
	 * Constructor
	 *
	 *  Creates a new PaletteButton with the passed array of items
	 * as it's palette menu.
	 * 
	 *  It will preselect the first item in the array as
	 *  its default selection and use its images for its own view.
	 *
	 * @param pItems  an array of PaletteButtonItems for the menu.
	 **/
	public PaletteButton(  PaletteButtonItem [] pItems) {
		super();
		setPaletteButtonItems( pItems );
		setRolloverEnabled( true);
		setBorder( null);
		
	}
	
	
	
	//////////////////
	// Methods
	//////////////////
	
	
	/**
	 * setContext
	 * Sets a user context object.
	 **/
	 public void setContext( Object pContext) {
	 	mContext = pContext;
	 }
	 
	 /**
	  * getContext
	  * Gets teh user context object
	  **/
	 public Object getContext() {
	 	return mContext;
	 }
	
	/**
	 * setPopupIndicatorIcon
	 * Sets the popup indicator icon icon
	 *
	 * @param pIcon the icon
	 **
	 public void setPopupIndicatorIcon( Icon pIcon) {
	 	mPopupIndicatorIcon = pIcon;
	 }
	 
	 /**
	  * getPopupIndicatorIcon
	  * Gets teh popup indicator icon
	  * @return the icon
	  **/
	  public Icon getPopupIndicatorIcon() {
	  	return mPopupIndicatorIcon;
	  }


	/**
	 * setPopupIconIndicatorEnabled
	 * This sets the state of the mUseArrowIcon property.  It is
	 * used to tell how to draw the popup visual cue.  If true,
	 * then it uses the image, otherwise, it draws the default
	 * arrow.
	 * 
	 **/
	public void setPopupIconIndicatorEnabled( boolean pState) {
		mUseIconIndicator = pState;
	}
	
	/**
	 * isPopupIconIndicatorEnabled
	 * This method tells how to draw the popup arrow indicator
	 * on the button in paint().  If some image should be used, then this
	 * returns true.  If the default arrow that's drawn by code
	 * should be used, this returns false.
	 *
	 * @ return boolean true if should use image to draw indictor; false if not
	 **/
	public boolean isPopupIconIndicatorEnabled() {
		return mUseIconIndicator;
	}

	/**
	 * addPaletteItem( PaletteButtonItem pItem)
	 * This method adds a new PaleeteButtonItem to the PaletteButton's
	 * menu.
	 *
	 * @param pItem the new PaletteButtonItem to be added.
	 **/
	public void addPaletteItem( PaletteButtonItem  pItem ) {
		if( mItems == null) {
		  mItems = new PaletteButtonItem[1];
		  mItems[0] = pItem;
		  }
		else {
			int len = mItems.length;
			PaletteButtonItem newItems[] = new PaletteButtonItem[ len+1];
			for(int i=0; i< len; i++) {
				newItems[i] = mItems[i];
				}
			newItems[len] = pItem;
			setPaletteButtonItems( newItems);
			}
	}
	
	
	/**
	 * removePaletteItem
	 * This removes an item from the popup menu
	 * @param pItem the item to remove
	 **/
	 public void emovePaletteItem( PaletteButtonItem pItem ) {
	 	
	 	if( mItems != null) {
	 		int len = mItems.length;
	 		PaletteButtonItem [] newItems = null;
	 		boolean found = false;
	 		int slot = 0;
	 		if( len > 1) 
	 			newItems = new PaletteButtonItem [len-1];
	 		
	 		for( int i=0; i< len; i++) {
	 		  if( mItems[i].equals( pItem) ) {
	 		  	found = true;
	 		  	}
	 		  else {
	 		  	newItems[slot] = mItems[i];
	 		  	slot++;
	 		  	}
	 		  }
	 		if( found) {
	 			if( len == 1)
	 				newItems = null;
	 			setPaletteButtonItems( newItems);
	 			}
	 		}
	 }
	
	
	/**
	 * setPaletteButtonItems
	 * Sets the set of PaletteButtonItems for the popup menu
	 * @param pItems the array of items.
	 **/
	 public void setPaletteButtonItems( PaletteButtonItem [] pItems) {
	 	mItems = pItems;
	 	buildPalette();
	 }
	 
	 /**
	  * getPaletteButtonItems
	  * Gets the array of PaletteButtonItems
	  * @return the array of items
	  **/
	  public PaletteButtonItem[] getPaletteButtonItems() {
	  	return mItems;
	  }
	 
	 
	  
	/**
	 * buildPalette()
	 *
	 * This method builds the PaletteButton's palette menu and sets up
	 * all appropriate event listeners to handle menu selection.  It
	 * also calculates the best layout fo rthe popup based on the
	 * the number of items.
	 *
	 **/
	protected void buildPalette() {
		
		// clear the old
		mPopup = null;
		
		if( mItems == null) {
		  mHasPopup = false;
		  return;
		  }
		 
		int numItems = mItems.length;
		
		if( numItems < 2) {
			mHasPopup = false;
			return;
			}
			
		mHasPopup = true;
		int cols = 0;
		while(  (mColThreshold[cols] < numItems) && (cols < mColThreshold.length) )  {
			cols++;
			}
		int rows = (numItems + (numItems % cols )) / cols ;
		
		GridLayout grid = new GridLayout( rows, cols);
		grid.setVgap( 0);
		grid.setHgap( 0);
		
		mPopup = new PBPopupMenu();
		mPopup.setLayout( grid);
		
		
		VueToolPopupAdapter ourPopupAdapter;
		ourPopupAdapter = new VueToolPopupAdapter( mPopup);
		this.addMouseListener(  ourPopupAdapter );
		
		
		for(int i=0; i<numItems; i++) {
			mItems[i].setPaletteButton( this);
			mPopup.add( mItems[i] );
		 	mItems[i].addActionListener( this);
		 	}
		 
		 //mPopup.pack();
	
	}
	
	/**
	 *  setPropertiesFromItem
	 * This method sets teh display properties of the button based on
	 * the properties set in a PaletteButtonMenu item.  This allows the 
	 * primary tool button to reflect the current selected item on the main toolbar.
	 *
	 * @param pItem - the PaletteButtonItem to use as the source
	 **/
	public void setPropertiesFromItem( PaletteButtonItem pItem) {
	
		this.setIcon( pItem.getIcon() );
		this.setPressedIcon( pItem.getPressedIcon() );
		this.setSelectedIcon( pItem.getSelectedIcon() );
		this.setRolloverIcon( pItem.getRolloverIcon() );
		this.setDisabledIcon( pItem.getDisabledIcon() );
		
		this.setRolloverEnabled( getRolloverIcon() != null);
	}
	
	
	public void setPopupOverlayUpIcon( Icon pIcon) {
		mPopupIndicatorUpIcon = pIcon;
	}
	
	public void setPopupOverlayDownIcon( Icon pIcon) {
		mPopupIndicatorDownIcon = pIcon;
	}
	
	public void setOverlayIcons( Icon pUpIcon, Icon pDownIcon) {
		setPopupOverlayUpIcon( pUpIcon);
		setPopupOverlayDownIcon( pDownIcon);
	}
	
	
	public void setIcons( Icon pUp, Icon pDown, Icon pSelect, Icon pDisabled, 
						  Icon pRollover ) {
		this.setIcon( pUp);
		this.setPressedIcon( pDown);
		this.setSelectedIcon( pSelect);
		this.setDisabledIcon( pDisabled);
		this.setRolloverIcon( pRollover);
		
		this.setRolloverEnabled(  pRollover != null );
		
	}
	
	/**
	 * paint( Graphics g)
	 * Overrides paint method and renders an additional icon ontop of
	 * of the normal rendering to indicate if this button contains
	 * a popup handler.
	 *
	 * @param Graphics g the Graphics.
	 **/
	public void paint( java.awt.Graphics pGraphics) {
		super.paint( pGraphics);
		
		Dimension dim = getPreferredSize();
		Insets insets = getInsets();
		
		// now overlay the popup menu icon indicator
		// either from an icon or by brute painting
		if(  (!isPopupIconIndicatorEnabled() ) 
			   &&  (mPopup != null) 
			   && ( !mPopup.isVisible() ) ) {
			// draw popup arrow
			Color saveColor = pGraphics.getColor();
			pGraphics.setColor( Color.black);
			
			int w = getWidth();
			int h = getHeight();
			
			int x1 = w + mArrowHOffset;
			int y = h + mArrowVOffset;
			int x2 = x1 + (mArrowSize * 2) -1;
			
			for(int i=0; i< mArrowSize; i++) { 
				pGraphics.drawLine(x1,y,x2,y);
				x1++;
				x2--;
				y++;
				}
			pGraphics.setColor( saveColor);
			}
		else  // Use provided popup overlay  icons
		if(   (mPopup != null) && ( !mPopup.isVisible() ) ) {
			
			Icon overlay = mPopupIndicatorUpIcon;
			if( isSelected() ) {
				
				overlay = mPopupIndicatorDownIcon;
				}
			if( overlay != null) {
				overlay.paintIcon( this, pGraphics, insets.top, insets.left);
				}
			/***
			pGraphics.drawImage( mPopupIndicatorIcon.getImage(),
								 0, 0 ,
								 //Color.white,
								 mPopupIndicatorIcon.getImageObserver()  );
		 ******/
			}
	}
	
	/**
	 * actionPerformed( ActionEvent pEvent)
	 * This method handles remote or direct  selection of a PaletteButtonItem
	 *
	 * It will update its own icons based on the selected item
	 *
	 * @param pEvent the action event.
	 **/
	public void actionPerformed( ActionEvent pEvent) {
		// fake a click to handle radio selection after menu selection
		doClick();		
	}
	
	
	/**
	* JPopupMenu subclass  to deal with popup triggers.
	*( and fix some swing bugs.
	**/
	public class PBPopupMenu extends JPopupMenu {
	
	
		public boolean isPopupMenuTrigger( MouseEvent pEvent ) {
			boolean retValue = false;
			
			if(pEvent.getID() == MouseEvent.MOUSE_PRESSED )
				retValue =true;
			return retValue;
		}
	}
	
	
	private static boolean sDebug = false;
	private void debug( String pStr) {
		if( sDebug) {
			System.out.println("PaletteButton: "+pStr);
			}
	}
	
}  // end of class






