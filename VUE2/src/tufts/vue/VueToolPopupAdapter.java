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
 * @deprecated -- no longer used -- smf 2004-01-29 19:42.02 
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
    private boolean mMenuWasShowing = false;
    public void mousePressed(MouseEvent e) {
    	debug(e.paramString() + " on " + e.getSource());
    	// For most things, we'd pop up the menu at x, yy below,
    	//int x = e.getX();
    	//int y = e.getY();
    	// but this time, we use the compoent's lower left as the spot so
    	// it looks like a drop down menu
    	
        if (!mPopup.isVisible()) {
            mMenuWasShowing = false;

            // this almost working, but not because MenuSelectionManager now
            // SOMETIMES doesn't clear the old menu (probabaly thinks it's
            // already hidden after we overrode it's attempts to hide it)
            //if (mPopup instanceof PaletteButton.PBPopupMenu) {
            //  ((PaletteButton.PBPopupMenu)mPopup).setVisibleLocked(true);
            //}

            showPopup(e);
        } else {
            mMenuWasShowing = true;
            //mPopup.setVisible(false);
        }
    }

    /**
     * mouse Released
     * This handles the mouse release events
     *
     * @param MouseEvent e the event
     **/
    public void mouseReleased(MouseEvent e) {
    	debug(e.paramString() + " on " + e.getSource());

        // if for any reason we never get this mouse released event,
        // the pop-up menu will stay stuck on the screen till we
        // do get one...
        //if (mPopup instanceof PaletteButton.PBPopupMenu)
        //    ((PaletteButton.PBPopupMenu)mPopup).setVisibleLocked(false);

    	if ( mPopup.isVisible() ) {
            ///////mPopup.setVisible( false);
            //Component c = e.getComponent();
            // if the palette buttons can take focus, you have
            // to do this or they don't get clicked -- we've turned
            // off taking focus which improves pop-up menu behaviour.
            //debug(e.paramString() + "\tclicking " + c);
            //((PaletteButton) c).doClick();
        } else {
            // this puts it back, but we need to not do this if was just showing
            // so we can still toggle it's display on/off
            if (!mMenuWasShowing)
                showPopup(e);
            //if (sDebug) System.out.println("\tpop-up not visible");
        }

    }

    private void showPopup(MouseEvent e)
    {
        if (sDebug) System.out.println("\tshowing " + mPopup);
        Component c = e.getComponent(); 	
        mPopup.show(c, 0, c.getBounds().height);
    }

    /**
     * mouseClicked
     * This handles the mouse clicked events
     *
     * @param MouseEvent e the event
     **/
    public void mouseClicked(MouseEvent e) {
    	debug(e.paramString() + " on " + e.getSource());
    }


    private static boolean sDebug = false;
    private void debug( String pStr) {
        if( sDebug ) {
            System.out.println("VueToolPopupAdapter " + Integer.toHexString(hashCode()) + ": " +pStr);
        }
    }
    
}
