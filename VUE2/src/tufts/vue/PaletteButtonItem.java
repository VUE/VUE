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
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

package tufts.vue;


import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;



/**
 * PaletteButtonItem class is used for displaying and controlling tool bar
 * buttons that have a palette of options available.  For example, the node tool 
 * can contain a set of shapes that are used when creating a node.  The zoom tool has two
 * items, zoom in, and zoom out.
 * 
 * PaletteButtonAItems are intended to be added to PaletteButtons with the addPaletteItem
 * method. 
 *
 *
 **/
public class PaletteButtonItem extends JMenuItem {

    private static final Color ButtonBackgroundColor = VueResources.getColor("toolbar.background");


	///////////
	// Fields
	///////////

	/*  what's teh current icon to render for this menu item */
	Icon mCurrentIcon = null;
	
	/** the PaletteButton that owns this item **/
	private PaletteButton mPaletteButton = null;
	
	/* the menu icon **/
	private Icon mMenuIcon = null;
	
	/** the menu icon for mouse over/selected **/
	private Icon mMenuSelectedIcon = null;
	
	/** contextual object for this item **/
	Object mContext = null;
	
	
	
	
	///////////////////
	// Constructors
	////////////////////
	
	
	/**
	 *  Constructor - creates a default PaletteButtonItem
	 **/
	public PaletteButtonItem() {
		super();
		setRolloverEnabled( true);
		this.addMouseListener( new PaletteButtonItem.PBMouseListener() );
		this.addActionListener( new PaletteButtonItem.PaletteItemActionListener() );
                setBackground(ButtonBackgroundColor);
	}
	
	



	//////////////////
	// Methods
	///////////////////

	
	/**
	 * setPaletteButton
	 * This method sets the PaletteButton that this item is owned by.
	 *
	 * @param pPaletteButton - the PaletteButton object that owns this item
	 **/
	public void setPaletteButton( PaletteButton pButton ) {
		mPaletteButton = pButton;
	}
	
	/**
	 * getPaletteButton
	 * This method returns the PaletteButton that owns this item.
	 * 
	 * @returns PaletteButton the owner of the item.
	 **/
	public PaletteButton getPaletteButton() {
		return mPaletteButton;
	}


	
	/**
	 * setContext
	 * This method sets the Contest object associated with the item.
	 *
	 * @param pObject - the Context object that owns this item
	 **/
	public void setContext( Object pObject ) {
		mContext = pObject;
	}
	
	/**
	 * getContext
	 * This method returns the Context for the item.
	 * 
	 * @returns Object the context associated with the item.
	 **/
	public Object getContext() {
		return mContext;
	}


	/**
	 * setMenuItemIcon()
	 * This sets teh menu item icon that's used when painting the item
	 * @param pIcon - the Icon to draw
	 **/
	public void setMenuItemIcon( Icon pIcon) {
		mMenuIcon = pIcon;
		// sdet the default menu icon, if not already set
		if( mCurrentIcon == null)  {
			mCurrentIcon = pIcon;
			}
	}
	
	/**
	 * getMenuItemIcon
	 * THis returns the menu item to draw
	 * @return Icon the icon
	 **/
	public Icon getMenuItemIcon() {
		return mMenuIcon;
	}
	
	public void setMenuItemSelectedIcon( Icon pIcon) {
		mMenuSelectedIcon = pIcon;
	}
	
	
	/**
	 * getMenuItemSelected
	 * This gets the icon to draw when the item is selected/mouse over
	 * @return Icon the icon
	 **/
	public Icon getMenuItemSelectedIcon() {
		return mMenuSelectedIcon;
	}
	
	/**
	 * handlePaletteButtonItemSelection()
	 * This method handles the selection of this item, and alerts the owner
	 * button of the current selection.
	 **/
	public void handlePaletteButtonItemSelection() {
		if( mPaletteButton != null) {
			// copy all the icons from the item to the button
			mPaletteButton.setPropertiesFromItem( this);
			// reset the item's display for next time.
			mCurrentIcon = getMenuItemIcon();
			}
		else {
			// ERROR:  should never happen.
			debug(" !!! Error:  Null Pointer - PaletteButtonItem.handlePalettteButtonItemSelection().");
			}
	}
	
	/**
	 * processMenuDragMouseEvent
	 * Override of JmenuTem's method to set up our own custom painting
	 * based on mouse drag events for the popup.  We want to set our current
	 * icon based on mouse position.
	 *
	 * @param pEvent - the MouseDragEvent to process
	 **/
	public void processMenuDragMouseEvent( MenuDragMouseEvent pEvent) {
		
		int id = pEvent.getID() ;
		int x = pEvent.getX() ;
		int y = pEvent.getY() ;
		if( id == MenuDragMouseEvent.MOUSE_ENTERED  ) {
			debug("processMenuDragMouseEvent - MOUSE_ENTERED");
			mCurrentIcon = getMenuItemSelectedIcon();
			}
		else
		if( id == MenuDragMouseEvent.MOUSE_EXITED) {
			debug("processMenuDragMouseEvent - MOUSE_EXITED");
			mCurrentIcon = getMenuItemIcon();
			}
		else
		if ( id == MenuDragMouseEvent.MOUSE_MOVED ) {
			debug(" processMenuDragEvent MOUSE_MOVED "+x+", "+y);
			if( this.contains(x,y) ) {
				mCurrentIcon = getMenuItemSelectedIcon();
				}
			else {
				mCurrentIcon = getMenuItemIcon();
				}
			}
		super.processMenuDragMouseEvent( pEvent);
	}
	
	
	/**
	* getPreferredSize()
	 * Override because we do our own drawing since JMenuItem
	 * does too much stuff that's out of our direct control
	 * We return the image size of our current image.
	 **/
	public Dimension getPreferredSize() {
		Icon icon = getIcon();
		Dimension d = new Dimension( icon.getIconWidth(), icon.getIconHeight() );
		
		//d = super.getPreferredSize();
		return d;
	}
	
	/**
	 * paint()
	 * Overrides paint( Graphics g) to only draw the icon image
	 * for the menu item.  This normal JMenuItem leaves annoying border
	 * and background margins to allow for things like a check mark for radio
	 * menu items and other shortcuts.  We want just the icon.
	 *
	 * @param pGraphics - the Graphics to draw in.
	 **/
	 public void paint( Graphics pGraphics) {
	 
	 Icon icon = mCurrentIcon;
	 
	 	
	 if( icon != null) {
	 	icon.paintIcon( this, pGraphics, 0,0);
		}
	else {
		debug(" !!! Paint() is using super().paint() method !!!");
		super.paint( pGraphics);
		}
		
	 
	 }
	
	
	private String mDisplayName;
	public void setDisplayName( String pName) {
	  mDisplayName = pName;
	}
	
	
	
	/**
	 * PaletteItemActionListener
	 * This class is the action llistener and handles our special
	 * selection techniques.
	 **/
	public class PaletteItemActionListener implements ActionListener {
	
	
		public void actionPerformed( ActionEvent pEvent) {
		 	debug("actionPerformed() !!! Item Action! ");
		 	handlePaletteButtonItemSelection();
		}
	}
	
	
	
	
	
	
	
	/**
	 * PBMouseListener class
	 * This class is used to set the icon for our custom drawing
	 * for menu items.
	 **/
	public class PBMouseListener implements MouseListener{
	
		public PBMouseListener() {
		
		}
		
		/////////////////////////////
		// MouseListener  Interface Implementations
		/////////////////////////////
		
		public void mouseClicked( MouseEvent pEvent) {
			debug( "MouseListener MouseClicked()");
		}
		
		public void mousePressed( MouseEvent pEvent) {
			debug("MouseListener mousePressed() ");
		}
		
		public void mouseReleased( MouseEvent pEvent) {
						debug("MouseLiistener mouseReleased()");
		}
		
		public void mouseEntered( MouseEvent pEvent) {
			debug("MouseListener mouseEntered() ");
			mCurrentIcon = getMenuItemSelectedIcon();
		}
		
		public void mouseExited( MouseEvent pEvent) {
			mCurrentIcon = getMenuItemIcon();
			repaint();
			debug(" MouseListener mouseExited() ");
		}
	
	}

	static private boolean sDebug = false;
	private void debug( String pStr) {
		if( sDebug) {
			System.out.println("PaletteButtonITem - "+ pStr);
			}
	}
}
