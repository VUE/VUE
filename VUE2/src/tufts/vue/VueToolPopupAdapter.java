
package tufts.vue;


import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


/**
 * VueToolPopupAdapter
 *
 * This calss handles support for activating a popuup menu for the radio
 * button tool menu of the VUE Toolbar.
 *
 * T@author scottb
 **/
public class VueToolPopupAdapter extends MouseAdapter {
    
	    /** The popup menu to be displayed **/
    private JPopupMenu mPopup;
    
    
    /**
     * Constructor
     *
     * Creates an adapter with the passed popup menu
     *
     * @param pPopup the popup menu to display
     **/
   // public VueToolPopupAdapter( JPopupMenu pPopup) {
    public VueToolPopupAdapter( JPopupMenu pPopup) {
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
    	debug("   Release Event");
    	if( mPopup.isVisible() ) {
    		//mPopup.setVisible( false);
    		  Component c = e.getComponent(); 	
    		  ((PaletteButton) c).doClick();
    		}
    }

    /**
     * mouseClicked
     * This handles the mouse clicked events
     *
     * @param MouseEvent e the event
     **/
    public void mouseClicked(MouseEvent e) {
		debug("   Click event");
    }


	private static boolean sDebug = false;
	private void debug( String pStr) {
		if( sDebug ) {
			
			System.out.println("VueToolPopupAdapter: "+pStr);
			}
	}
}
