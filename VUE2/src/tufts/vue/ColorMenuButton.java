package tufts.vue;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

/**
* ColorMenuButton
* This class provides a popup radio button selector component.
* It is used for the main tool bar tool
*
* @author csb
* @version 1.0
**/
public class ColorMenuButton extends JButton implements ActionListener
{
    //static ImageIcon sIcon = TestResources.getImageIcon( "icon");
	
    /** FIX:  move this to Resources **/
    private static String sPickerTitle = "Select Custom Color";
	
    //private static BlobIcon  sIcon = new BlobIcon( 16,16, new Color(1,1,244) );
    private static BlobIcon  sIcon = new BlobIcon(16,16, true);

    //////////////////
    //  Fields
    //////////////////
	
    /** property name **/
    String mPropertyName = null;
	
    /** PropertyChangeListeners **/
    private Vector mListeners = new Vector();
	
    // default offsets for drawing popup arrow via code
    public int mArrowSize = 3;
    public int mArrowHOffset  = -9;
    public int mArrowVOffset = -7;
	
    /** the popup overlay icons for up and down states **/
    public Icon mPopupIndicatorIconUp = null;
    public Icon mPopupIndicatorDownIcon = null;

    /** The currently selected Color item--if any **/
    protected Color mCurColor = new Color(0,0,0);
			
    /** the BlobIcon for the swatch **/
    private BlobIcon mBlobIcon = null;
		
    /** the popup menu **/
    protected JPopupMenu mPopup = null;

    private JMenuItem mNoneSelected = new ColorMenuItem(Color.black);
	
    /**
     * Constructor
     *
     *  Creates a new ColorMenuButton with the passed array of items
     * as it's palette menu.
     * 
     *  It will preselect the first item in the array as
     *  its default selection and use its images for its own view.
     *
     * @param pItems  an array of ColorMenuButtonItems for the menu.
     **/
    public ColorMenuButton(Color [] pColors, String [] pMenuNames, boolean pHasCustom) {
        //super("v ", sIcon);
        super();
        buildMenu(pColors, pMenuNames, pHasCustom);
        setIcon(sIcon);
        addMouseListener(new ColorMenuPopupAdapter(mPopup));
        setBorder(BorderFactory.createRaisedBevelBorder());
        //setBorder(new CompoundBorder(BorderFactory.createRaisedBevelBorder(), new EmptyBorder(3,3,3,3)));
        //setBorder(new CompoundBorder(BorderFactory.createEtchedBorder(), new LineBorder(Color.blue, 6)));
        setText("v ");
        setFont(VueConstants.FONT_SMALL);
        setFocusable(false);
    }
	
    public ColorMenuButton(Color [] pColors, String  [] pMenuNames) {
        this( pColors, pMenuNames, false);
    }
	
    public ColorMenuButton( Color [] pColors) {
        this( pColors, null, false);
    }
	
    public ColorMenuButton() {
        this( sTestColors,  sTestNames, true);
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
     * setContext
     * Sets a the current color.
     **/
    public void setColor(Color pColor) {
        mCurColor = pColor;
        if (mBlobIcon != null)
            mBlobIcon.setColor(pColor);
        if (pColor == null)
            mPopup.setSelected(mNoneSelected);
    }
	 
    /**
     * getColor
     * Gets teh current color
     **/
    public Color  getColor() {
        return mCurColor;
    }
	 
    public void setIcon( Icon pIcon) {
        super.setIcon( pIcon);
        if( pIcon instanceof BlobIcon) {
            mBlobIcon = (BlobIcon) pIcon;
        }
        else {
            mBlobIcon = null;
        }
        //setPreferredSize(new Dimension(20,18));
        //setSize(new Dimension(20,18));
    }
	
	 
    /**
     * buildMenu
     * This method builds the list of colors for the popup
     *
     **/
    private void buildMenu( Color [] pColors, String [] pNames,  boolean pHasCustom) {
        mPopup = new JPopupMenu();
			
        if (pColors != null)
            addColors( pColors, pNames);

        mNoneSelected.setVisible(false);
        mPopup.add(mNoneSelected);

        if( pHasCustom ) {
            // add the last custom color editor
			 	
            ColorMenuItem item = new ColorMenuItem( true);
            mPopup.add( item);
        }
    }
		
    private void addColors( Color [] pColors, String [] pNames) {
        int num = pColors.length;
			
        for( int i=0; i< num; i++ ) {
            ColorMenuItem item = null;
            item = new ColorMenuItem( pColors[i] ) ;
            if( pNames != null)
                item.setText( pNames[i]);
            mPopup.add( item);
        }
    }


    public void handleColorSelection( Color pColor) {
        //if( ! pColor.equals( getColor() ) ) {
        // always fire color changed even if same: could be multiple items in selection
        Color old = getColor();
        setColor( pColor);
        fireColorChanged( old, pColor);
        repaint();
    }
	
	
    protected void fireColorChanged(Color pOldColor, Color pNewColor)
    {
        PropertyChangeListener [] listeners = getPropertyChangeListeners();
        if( listeners != null) {
            PropertyChangeEvent event = new PropertyChangeEvent( this, mPropertyName, pOldColor, pNewColor );
            for( int i=0; i< listeners.length; i++) {
                listeners[i].propertyChange( event);
            }
        }
    }
	
    public void paint(java.awt.Graphics g) {
        ((java.awt.Graphics2D)g).setRenderingHint
            (java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
             java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        super.paint(g);
    }
	
    /*
     * paint( Graphics g)
     * Overrides paint method and renders an additional icon ontop of
     * of the normal rendering to indicate if this button contains
     * a popup handler.
     *
     * param Graphics g the Graphics.
     **/
    /*
    public void paint( java.awt.Graphics pGraphics) {
        super.paint( pGraphics);
        if (true) return;
        Dimension dim = getPreferredSize();
        Insets insets = getInsets();
		
        // now overlay the popup menu icon indicator
        // either from an icon or by brute painting
        if(  (! false ) 
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
			
                Icon overlay = null;
                if( isSelected() ) {
				
                    overlay = mPopupIndicatorDownIcon;
                }
                if( overlay != null) {
                    overlay.paintIcon( this, pGraphics, insets.top, insets.left);
                }
            }
    }
    */
	
    /**
     * actionPerformed( ActionEvent pEvent)
     * This method handles remote or direct  selection of a ColorMenuButtonItem
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
			
            if (pEvent.getID() == MouseEvent.MOUSE_PRESSED )
                retValue =true;
            return retValue;
        }
    }
	
	
    private static boolean sDebug = false;
    private void debug( String pStr) {
        if( sDebug) {
            System.out.println("ColorMenuButton: "+pStr);
        }
    }
	
	
	
    private class ColorMenuItem extends JMenuItem implements ActionListener {
	
        /** the color this item represents **/
        private Color mMenuColor = null;
		
        /** is this a custom color item? **/
        private boolean mIsCustomItem = false;
		
        /**
         * Constructor
         * @param the color for this item.
         **/
        public ColorMenuItem( Color pColor) {
            super();
            mMenuColor = pColor;
            setIcon(new BlobIcon( 16,16, pColor));
            addActionListener( this);
        }
        public ColorMenuItem( boolean pIsCustom) {
            super("Custom...");
            mIsCustomItem = pIsCustom;
            addActionListener( this);
        }
		
        public void actionPerformed( ActionEvent pEvent)
        {
            Color oldColor = getColor();
            
            if( mIsCustomItem ) {
                Color newColor = JColorChooser.showDialog( this, sPickerTitle, oldColor );
                if( newColor != null  ) {
                    mMenuColor =  newColor;
                    handleColorSelection(  newColor);
                }
            }
            else {
                handleColorSelection( mMenuColor);
            }
        }
    }



    /**
     * ColorMenuPopupAdapter
     *
     **/
    public class ColorMenuPopupAdapter extends MouseAdapter {
	    
        /** The popup menu to be displayed **/
        private JPopupMenu mPopup;
	    
	    
        /**
         * Constructor
         *
         * Creates an adapter with the passed popup menu
         *
         * @param pPopup the popup menu to display
         **/
        // public ColorMenuPopupAdapter( JPopupMenu pPopup) {
        public ColorMenuPopupAdapter( JPopupMenu pPopup) {
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


    private static String [] sTestNames = { "Black",
                                    "White",
                                    "Red",
                                    "Green",
                                    "Blue" }; 
    private static Color [] sTestColors = { new Color(0,0,0),
                                    new Color(255,255,255),
                                    new Color(255,0,0),
                                    new Color(0,255,0),
                                    new Color(0,0,255)  };
	
    
}  // end of class

