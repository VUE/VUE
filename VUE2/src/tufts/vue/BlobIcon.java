package tufts.vue;

import java.awt.*;
import javax.swing.*;


/**
 * BlobIcon Class
 *  This clas is used for creating dynamic color blobs with or without
 * an overlay image.  It can be used to create swatches of color for things
 * like menu icons and buttons.  It is useful in color menus for selecting
 * custom colors where you require an icon in a UI widget.
 *
 * 
 *
 **/
public class BlobIcon implements Icon {


	/** the icon height **/
	private int mHeight = 0;
	
	/** the icon width **/
	private int mWidth = 0;
	
	/** the blob color **/
	private Color mColor = null;
	
	/** the overlay icon, if any **/
	private Icon mOverlay = null;
	
	
	//////////////////
	// Constructors
	/////////////////
	
	/**
	 * Generic emply BlobIcon constructor
	 **/
	public BlobIcon() {
		super();
	}
	
	public BlobIcon( int pWidth,int  pHeight) {
		this( pWidth, pHeight, null, null);
	}
	
	public BlobIcon( int pWidth, int pHeight, Color pColor) {
		this( pWidth, pHeight, pColor, null);
	}
	
	
	/**
	 * This constrocutor makes a BlobIcon with the 
	 * @param pWidth - the icon width
	 * @param pHeight - the icon height
	 * @param pColor - the blob swatch color
	 * @param pOverlay - an overlay icon to draw over the blob
	 **/
	public BlobIcon(int pWidth, int pHeight, Color pColor, Icon pOverlay) {
		mWidth = pWidth;
		mHeight = pHeight;
		mColor = pColor;
		mOverlay = pOverlay;
	}
	
	
	
	//////////////
	// Methods
	///////////////
	
	
	/**
	 * getIconHeight
	 * Implementation of Icon interface
	 **/
	public int getIconHeight() {
		return mHeight;
	}
	
	/**
	 * getIconWidth
	 * Implementation of Icon interface.
	 * @see java.awt.Icon
	 **/
	public int getIconWidth() {
		return mWidth;
	}
	
	/**
	 * setIconSize
	 * A convenience method to set the height an dwidth
	 **/
	public void setIconSize( int pWidth, int pHeight) {
		mWidth = pWidth;
		mHeight = pHeight;
	}
	
	public void setColor( Color pColor) {
		mColor = pColor;
	}
	
	public Color getColor() {
		return mColor;
	}
	
	
	/**
	 * setIconHeight
	 * Sets the icon height
	 **/
	public void setIconHeight( int pHeight) {
		mHeight = pHeight;
	}
	
	/**
	 * setIconWidth
	 * Sets the icon's width.
	 **/
	public void setIconWidth( int pWidth) {
		mWidth = pWidth;
	}
	
	
	/**
	 * setOverlya
	 * Sets the overlay icon to draw an overlay over the color blob
	 * This is a convienience for transparent icons.
	 * Also, the BlobICon will resize to size of the overlay
	 * icon.  Note, if the overlay has no transparent pixels,
	 * the BlobIcon is nothing more than a normal icon with
	 * the overlay icon being painted.
	 *
	 * @param pOverlay - the overlay icon (should have transparent pixels).
	 **/
	public void setOverlay( Icon pOverlay) {
		mOverlay = pOverlay;
		if( pOverlay != null) {
			setIconSize( pOverlay.getIconWidth(), pOverlay.getIconHeight() );
			}
	}
	
	/** 
	 * getOverlay()
	 * Gets teh overlay icon.
	 **/
	public Icon getOverlay() {
		return mOverlay;
	}
	
	/**
	 * paintIcon
	 * Implementation of Icon interface
	 * This paints a color blob of the icon size at the 
	 * specified coords in the specified graphics.
	 * If an overlay icon is set, the overlay icon will be
	 * painted on top of the blob, thus providing a framing
	 * system.
	 * @see java.awt.Icon
	 **/
	public void paintIcon( Component c, Graphics g, int x, int y) {
		
		Color color = mColor;
		if ( color == null) {
			color = c.getBackground();
			}
		
		g.setColor( color);
		g.fillRect(x,y, mWidth, mHeight);
		if( mOverlay != null) {
			mOverlay.paintIcon( c, g, x, y);
			//g.drawImage( mOverlay.getImage(), x, y, null);
			}
	}
}