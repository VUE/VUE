

package tufts.vue;



import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;
import javax.swing.*;


/**
* StrokeMenuButton
* This class provides a popup radio button selector component.
* It is used for the main tool bar tool
*
* @author csb
* @version 1.0
**/
public class StrokeMenuButton extends JButton implements ActionListener {

	
	
	//////////////////
	//  Fields
	//////////////////
	
	static private Color sDefaultFillColor = new Color( 255,255,255);
	static private Color sDefaultLineColor = new Color( 0,0,0);
	static private int sDefaultWidth = 24;
	static private int sDefaultHeight = 16;
	
	/** property name **/
	String mPropertyName = null;
	
		/** The currently selected Color item--if any **/
	protected float mStroke = 1;
			
	protected ButtonGroup mGroup = new ButtonGroup();
	
		/** the popup menu **/
	protected JPopupMenu mPopup = null;

	
	
	///////////////
	// Constructors
	////////////////
	
	
	
	
	/**
	 * Constructor
	 *
	 *  Creates a new StrokeMenuButton with the passed array of items
	 * as it's palette menu.
	 * 
	 *  It will preselect the first item in the array as
	 *  its default selection and use its images for its own view.
	 *
	 * @param pItems  an array of StrokeMenuButtonItems for the menu.
	 **/
	public StrokeMenuButton(  float [] pValues, String [] pMenuNames, Icon [] pIcons, boolean pHasCustom) {
		super();
		buildMenu( pValues, pMenuNames, pIcons, pHasCustom);
		
		StrokeMenuPopupAdapter ourPopupAdapter;
		ourPopupAdapter = new StrokeMenuPopupAdapter( mPopup);
		this.addMouseListener(  ourPopupAdapter );
	}
	
	
	public StrokeMenuButton(  float [] pValues, String [] pMenuNames, boolean pGenerateIcons, boolean pHasCustom) {
		super();
		
		LineIcon [] icons = null;
		if( pGenerateIcons) {
			int num = 0;
			if( pValues != null)  {
				num = pValues.length;
				}
			
			icons = new LineIcon[num];
			for( int i=0; i<num; i++) {
				LineIcon icon = new LineIcon( sDefaultWidth, sDefaultHeight);
				icon.setColor( sDefaultFillColor);
				icon.setLineColor( sDefaultLineColor);
				icon.setWeight( pValues[i] );
				icons[i] = icon;
				}
			}
		buildMenu( pValues, pMenuNames, icons, pHasCustom);
		
		StrokeMenuPopupAdapter ourPopupAdapter;
		ourPopupAdapter = new StrokeMenuPopupAdapter( mPopup);
		this.addMouseListener(  ourPopupAdapter );
	}
	
	

	public StrokeMenuButton() {
		super();
	}
	
	
	
	//////////////////
	// Methods
	//////////////////
	
	
	/**
	 * setPropertyName
	 *
	 **/
	public void setPropertyName( String pName) {
		mPropertyName = pName;
	}
	
	/**
	 * mPropertyName
	 *
	 **/
	public String getPropertyName() {
		return mPropertyName;
	}
	
	/**
	 * setValue
	 * Sets a the current stroke weight.
	 **/
	 public void setStroke( float pValue) {
	 	mStroke = pValue;
	 	
	 		// if we are using a LineIcon, update teh visual feedback **/
	 	if( getIcon() instanceof LineIcon ) {
	 		LineIcon icon = (LineIcon) getIcon();
	 		icon.setWeight( mStroke);
	 		}
	 }
	 
	 /**
	  * getStroke
	  * Gets teh current stroke value.
	  **/
	 public float  getStroke() {
	 	return mStroke;
	 }
	 

	
	 
	  
	  
		/**
		 * buildMenu
		 * This method builds the list of itemss for the popup
		 *
		 **/
		public void buildMenu( float [] pStrokes, String [] pNames,  Icon [] pIcons, boolean pHasCustom) {
			mPopup = new JPopupMenu();
			
			if( pStrokes != null) {
				addStrokes( pStrokes, pNames, pIcons);
				}
			if( pHasCustom ) {
			 	// add the last custom stroke item
			 	
			 	StrokeMenuItem item = new StrokeMenuItem( true);
			 	mPopup.add( item);
			 	mGroup.add( item);
			 }
		
		}
		
		private void addStrokes( float [] pStrokes, String [] pNames, Icon [] pIcons) {
			int num = pStrokes.length;
			
			for( int i=0; i< num; i++ ) {
				StrokeMenuItem item = null;
				item = new StrokeMenuItem( pStrokes[i] ) ;
				if( pIcons[i] != null ) {
					item.setIcon( pIcons[i] );
					}
				item.setText( pNames[i]);
				mPopup.add( item);
				mGroup.add( item);
				}
		}
	  


	/** 
	 * handStrokeSelection
	 * This method handles a stroke selection action from
	 *( the UI.  If the value changed, it will fire
	 * a PropertyChangeEvent .
	 **/
	public void handleStrokeSelection( float pStroke) {
		// did the stroke change?
		if( pStroke != getStroke()   ) {
			// yes!
			float old = getStroke();
			setStroke( pStroke );
			fireStrokeChanged( new Float(old), new Float( pStroke) );
			}
	}
	
	
	/**
	 * fireStrokeChanged
	 * This fires a ProeprtyChangeEvent to all listeners
	 *
	 **/
	public void fireStrokeChanged( Float pOld, Float  pNew) {
		
		PropertyChangeListener [] listeners = getPropertyChangeListeners();
		if( listeners != null) {
			PropertyChangeEvent event = new PropertyChangeEvent( this, mPropertyName, pOld, pNew );
			for( int i=0; i< listeners.length; i++) {
				listeners[i].propertyChange( event);
				}
			}
	}
	
	
	
	/**
	 * actionPerformed( ActionEvent pEvent)
	 * This method handles remote or direct  selection of a StrokeMenuButtonItem
	 *
	 * It will update its own icons based on the selected item
	 *
	 * @param pEvent the action event.
	 **/
	public void actionPerformed( ActionEvent pEvent) {
		// fake a click to handle radio selection after menu selection
		//doClick();		
	}
	
	
	
	
	
	
	private class StrokeMenuItem extends JRadioButtonMenuItem implements ActionListener {
	
		/** the color this item represents **/
		private float mMenuStroke = 0;
		
		/** is this a custom color item? **/
		private boolean mIsCustomItem = false;
		
		/**
		 * Constructor
		 * @param the float stroke value for this item.
		 **/
		public StrokeMenuItem( float pStroke) {
			super();
			mMenuStroke = pStroke;
			
			addActionListener( this);
			
			
		}
		public StrokeMenuItem( boolean pIsCustom) {
			super("Custom...");
			mIsCustomItem = pIsCustom;
			addActionListener( this);
		}
		
		public void actionPerformed( ActionEvent pEvent) {
			
			float oldStroke = getStroke();
			
			if( mIsCustomItem ) {
				float newStroke = 1; // do dialog here FIX: todo:
				if( newStroke != 0  ) {
						mMenuStroke =  newStroke;
						handleStrokeSelection(  newStroke);
						}
				}
			else {
				handleStrokeSelection( mMenuStroke);
				}
		}
	}



	/**
	 * StrokeMenuPopupAdapter
	 *
	 **/
	public class StrokeMenuPopupAdapter extends MouseAdapter {
	    
		    /** The popup menu to be displayed **/
	    private JPopupMenu mPopup;
	    
	    
	    /**
	     * Constructor
	     *
	     * Creates an adapter with the passed popup menu
	     *
	     * @param pPopup the popup menu to display
	     **/
	   // public StrokeMenuPopupAdapter( JPopupMenu pPopup) {
	    public StrokeMenuPopupAdapter( JPopupMenu pPopup) {
	    	super();
	    	setPopup( pPopup);
	    }
	    
	    /**
	     * setPopupMenu
	     *
	     * This method sets the popup menu to display
	     *
	     * @param pPopup the popup menu to display
	     **/
	    public void setPopup(JPopupMenu pPopup) {
	      mPopup = pPopup;
	    }
	    
	    /**
	    * getPopupMenu
	    * this method returns the current popup menu used by the adapter
	    * @return the popup menu
	    **/
	    public JPopupMenu getPopupMenu() {
	    	return mPopup;
	    }
	    
	    /**
	     * mousePressed
	     * Thimethod will handle the mouse press event and cause the
	     * popup to display at the proper location of th
	     **/
	    public void mousePressed(MouseEvent e) {
	    	
	    	// For most things, we'd pop up the menu at x, yy below,
	    	//int x = e.getX();
	    	//int y = e.getY();
	    	// but this time, we use the compoent's lower left as the spot so
	    	// it looks like a drop down menu
	    	
	    	Component c = e.getComponent(); 	
	    	mPopup.show(c,  0, (int) c.getBounds().getHeight());
	       
	    
	    }

	    /**
	     * mouse Released
	     * This handles the mouse release events
	     *
	     * @param MouseEvent e the event
	     **/
	    public void mouseReleased(MouseEvent e) {
	    	if( mPopup.isVisible() ) {
	    		//mPopup.setVisible( false);
	    		  Component c = e.getComponent(); 	
	    		  ((JButton) c).doClick();
	    		}
	    }

	    /**
	     * mouseClicked
	     * This handles the mouse clicked events
	     *
	     * @param MouseEvent e the event
	     **/
	    public void mouseClicked(MouseEvent e) {
	    }

		}
}  // end of class

