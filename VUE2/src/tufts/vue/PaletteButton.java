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

package tufts.vue;

import tufts.vue.gui.GUI;

import java.io.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;


/**
 *
 * This class provides a popup radio button selector component.
 * It is used for the main tool bar tool
 *
 * @author csb
 * @version $Revision: 1.23 $ / $Date: 2010-02-03 19:17:41 $ / $Author: mike $
 **/
public class PaletteButton extends JRadioButton
{
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

	
    /**
     * Creates a new PaletteButton with the passed array of items
     * as it's palette menu.
     * 
     *  It will preselect the first item in the array as
     *  its default selection and use its images for its own view.
     *
     * @param pItems  an array of PaletteButtonItems for the menu.
     **/
    public PaletteButton(PaletteButtonItem[] pItems)
    {
        if (pItems != null) {
            setPaletteButtonItems(pItems);
            setRolloverEnabled(true);
        }
        setBorder( null);
        setFocusable(false);
        GUI.applyToolbarColor(this);
    }
	
    /**
     * Creates a new PaletteButton with no menus
     **/
    public PaletteButton() {
        this(null);
    }
	
	
    /**
     * Sets a user context object.
     **/
    public void setContext( Object pContext) {
        mContext = pContext;
    }
	 
    /**
     * Gets teh user context object
     **/
    public Object getContext() {
        return mContext;
    }
	
    /**
     * Sets the popup indicator icon icon
     *
     * @param pIcon the icon
     **
     public void setPopupIndicatorIcon( Icon pIcon) {
     mPopupIndicatorIcon = pIcon;
     }
	 
     /**
     * Gets teh popup indicator icon
     * @return the icon
     **/
    public Icon getPopupIndicatorIcon() {
        return mPopupIndicatorIcon;
    }


    /**
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
        //return mUseIconIndicator;
        return false;
    }

    public void removePopupComponent(Component c)
    {
    	mPopup.remove(c);
    }
    public void addPopupComponent(Component c)
    {
    	mPopup.add(c);
    }
    /**
     * This method adds a new PaleeteButtonItem to the PaletteButton's
     * menu.
     *
     * @param pItem the new PaletteButtonItem to be added.
     **/
    public void addPaletteItem(PaletteButtonItem pItem) {
        if( mItems == null) {
            mItems = new PaletteButtonItem[1];
            mItems[0] = pItem;
        } else {
            int len = mItems.length;
            PaletteButtonItem newItems[] = new PaletteButtonItem[len+1];
            for(int i=0; i< len; i++) {
                newItems[i] = mItems[i];
            }
            newItems[len] = pItem;
            setPaletteButtonItems(newItems);
        }
    }
	
	
    /**
     * This removes an item from the popup menu
     * @param pItem the item to remove
     **/
    public void removePaletteItem( PaletteButtonItem pItem ) {
	 	
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
     * Sets the set of PaletteButtonItems for the popup menu
     * @param pItems the array of items.
     **/
    public void setPaletteButtonItems(PaletteButtonItem [] pItems) {
        if (DEBUG.INIT) System.out.println(this + " setPaletteButtonItems n=" + pItems.length);
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
     * This method builds the PaletteButton's palette menu and sets up
     * all appropriate event listeners to handle menu selection.  It
     * also calculates the best layout fo rthe popup based on the
     * the number of items.
     **/
    protected void buildPalette() {
		
        // clear the old
        mPopup = null;
		
        if (mItems == null || mItems.length < 2) {
            mHasPopup = false;
            return;
        }
		 
        mHasPopup = true;
        int numItems = mItems.length;
		
        int cols = 0;
        while (mColThreshold[cols] < numItems && cols < mColThreshold.length)
            cols++;
        int rows = (numItems + (numItems % cols )) / cols ;

        if (rows < 3 && GUI.isMacBrushedMetal() && VueUtil.getJavaVersion() < 1.5f /*&& tiger */) { // total hack for now

            // JAVA BUG: there appears to be an absolute minimunm width & height
            // for pop-up's: approx 125 pixels wide, no smaller, and approx 34 pixels
            // tall, no smaller.
            //
            // This bug only shows up in Java 1.4.2 on Mac OS X Tiger (10.4+) if
            // we're using the Metal version of the Mac Aqua Look & Feel.
            // The default Mac Aqua L&F doesn't see the bug, and java 1.5 works fine.
            // 
            // Note this bug affects almost all pop-ups: even combo-box's!  (See
            // VUE font-size), as well as many roll-over / tool-tip pop-ups.
            // I'm guessing the bug is somewhere down in the PopupFactory or PopupMenuUI
            // or the java small-window caching code shared by pop-ups and tool-tips.
            //
            // SMF 2005-08-11

            // This forces the smaller menus we use into 1 row which use
            // up the forced width better.  These #'s hard-coded based
            // on our current usage...
            rows = 1;
            cols = 3;
        }
		
        //JPopupMenu.setDefaultLightWeightPopupEnabled(false);

        mPopup = new JPopupMenu();//PBPopupMenu(rows, cols);
        
        new PalettePopupMenuHandler(this, mPopup); // installs mouse & popup listeners

        for (int i = 0; i < numItems; i++) {
            mPopup.add(mItems[i]);
            mItems[i].setPaletteButton(this);
            //mItems[i].addActionListener(this);
        }
        
        if (DEBUG.INIT)
            System.out.println("*** CREATED POPUP " + mPopup
                               + " margin=" + mPopup.getMargin()
                               + " layout=" + mPopup.getLayout()
                               );
	
    }
    
    /**
     * Given a trigger component (such as a label), when mouse is
     * pressed on it, pop the given menu.  Default location is below
     * the given trigger.
     */
    public static class PalettePopupMenuHandler extends tufts.vue.MouseAdapter
        implements javax.swing.event.PopupMenuListener
    {
        private long mLastHidden;
        private JPopupMenu mMenu;
        
        public PalettePopupMenuHandler(Component trigger, JPopupMenu menu)
        {
            trigger.addMouseListener(this);
            menu.addPopupMenuListener(this);
            mMenu = menu;
        }

        public void mousePressed(MouseEvent e) {
            long now = System.currentTimeMillis();
            if ((now - mLastHidden > 100) && (mMenu.getComponentCount() > 1))
                showMenu(e.getComponent());
        }

        /** show the menu relative to the given trigger that activated it */
        public void showMenu(Component trigger) {
            mMenu.show(trigger, getMenuX(trigger), getMenuY(trigger));
        }

        /** get menu X location relative to trigger: default is 0 */
        public int getMenuX(Component trigger) { return 0; }
        /** get menu Y location relative to trigger: default is trigger height (places below trigger) */
        public int getMenuY(Component trigger) { return trigger.getHeight(); }

        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            mLastHidden = System.currentTimeMillis();
            //out("HIDING");
        }
        
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) { /*out("SHOWING");*/ }
        public void popupMenuCanceled(PopupMenuEvent e) { /*out("CANCELING");*/ }
        
        // One gross thing about a pop-up menu is that there's no way to know that it
        // was just hidden by a click on the component that popped it.  That is, if you
        // click on the menu launcher once, you want to pop it, and if you click again,
        // you want to hide it.  But the AWT system autmatically cancels the pop-up as
        // soon as the mouse-press happens ANYWYERE, and even before we'd get a
        // processMouseEvent, so by the time we get this MOUSE_PRESSED, the menu is
        // already hidden, and it looks like we should show it again!  So we have to use
        // a simple timer.
        
    }
	
    /**
     * This method sets teh display properties of the button based on
     * the properties set in a PaletteButtonMenu item.  This allows the 
     * primary tool button to reflect the current selected item on the main toolbar.
     *
     * @param pItem - the PaletteButtonItem to use as the source
     **/

    public void setPropertiesFromItem(AbstractButton pItem) {
	
        this.setIcon( pItem.getIcon() );
        this.setPressedIcon( pItem.getPressedIcon() );
        this.setSelectedIcon( pItem.getSelectedIcon() );
        this.setRolloverIcon( pItem.getRolloverIcon() );
        this.setDisabledIcon( pItem.getDisabledIcon() );
        if (pItem.getToolTipText() != null)
        	this.setToolTipText(pItem.getToolTipText());
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

    public void setSelected(boolean b) {
        //System.out.println(this + " setSelected " + b);
        super.setSelected(b);
    }
    protected void fireStateChanged() {
        //System.out.println("PaletteButton: fireStateChanged, selected="+isSelected() + " " + getIcon());
        super.fireStateChanged();
    }
    /**
     * Overrides paint method and renders an additional icon ontop of
     * of the normal rendering to indicate if this button contains
     * a popup handler.
     *
     * @param Graphics g the Graphics.
     **/
    public void paint(java.awt.Graphics g) {
        super.paint(g);
		
        Dimension dim = getPreferredSize();
        Insets insets = getInsets();
		
        // now overlay the popup menu icon indicator
        // either from an icon or by brute painting
        if( !isPopupIconIndicatorEnabled()
            && mPopup != null 
            && !mPopup.isVisible() && mPopup.getComponentCount() >1
            ) {
            // draw popup arrow
            Color saveColor = g.getColor();
            g.setColor( Color.black);
			
            int w = getWidth();
            int h = getHeight();
			
            int x1 = w + mArrowHOffset;
            int y = h + mArrowVOffset;
            int x2 = x1 + (mArrowSize * 2) -1;
			
            //((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            //RenderingHints.VALUE_ANTIALIAS_ON);
            
            for(int i=0; i< mArrowSize; i++) { 
                g.drawLine(x1,y,x2,y);
                x1++;
                x2--;
                y++;
            }
            g.setColor( saveColor);
        }
        else  // Use provided popup overlay  icons
            if(   (mPopup != null) && ( !mPopup.isVisible() ) ) {
			
                Icon overlay = mPopupIndicatorUpIcon;
                if( isSelected() ) {
				
                    overlay = mPopupIndicatorDownIcon;
                }
                if( overlay != null) {
                    overlay.paintIcon( this, g, insets.top, insets.left);
                }
            }
    }

    public String toString() {
        return "PaletteButton[" + getContext() + "]";
    }
	
	
    /**
     * JPopupMenu subclass to set up appearance of pop-up menu.
     *
     * As of java 1.4.2 on Tiger (Mac OS X 10.4+) the layout
     * of this is broken (too much space).  OS X 10.3 works
     * in 1.4.2, and it works fine in java 1.5 on 10.4+.
     *
     **/
    public class PBPopupMenu extends JPopupMenu
    {
        public PBPopupMenu(int rows, int cols) {
            //setBorderPainted(false);
            setFocusable(false);
            GUI.applyToolbarColor(this);
            setBorder(new LineBorder(getBackground().darker().darker(), 1));

            GridLayout grid = new GridLayout(rows, cols);
            grid.setVgap(0);
            grid.setHgap(0);
            if (DEBUG.INIT) System.out.println("*** CREATED GRID LAYOUT " + grid.getRows() + "x" + grid.getColumns());
            setLayout(grid);
        }

        public void menuSelectionChanged(boolean isIncluded) {
            if (DEBUG.Enabled) System.out.println(this + " menuSelectionChanged included=" + isIncluded);
            super.menuSelectionChanged(isIncluded);
        }
        
        public String toString() {
            return "PBPopupMenu[" + getIcon() + "]";
        }
    }
    
	
	
}  // end of class PaletteButton






