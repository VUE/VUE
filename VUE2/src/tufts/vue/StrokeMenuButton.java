package tufts.vue;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;
import javax.swing.*;

/**
 * StrokeMenuButton
 *
 * This class provides a popup radio button selector component.
 * It is used for the main tool bar tool
 *
 * @author csb
 * @version 1.0
 **/
public class StrokeMenuButton extends MenuButton
{
    static private Color sDefaultFillColor = new Color( 255,255,255);
    static private Color sDefaultLineColor = new Color( 0,0,0);
    static private int sDefaultWidth = 24;
    static private int sDefaultHeight = 16;
	
    /** The currently selected Color item--if any **/
    protected float mStroke = 1;
			
    protected ButtonGroup mGroup = new ButtonGroup();
	
	
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
    /*
    public StrokeMenuButton(  float [] pValues, String [] pMenuNames, Icon [] pIcons, boolean pHasCustom) {
        super();
        buildMenu( pValues, pMenuNames, pIcons, pHasCustom);
		
        StrokeMenuPopupAdapter ourPopupAdapter;
        ourPopupAdapter = new StrokeMenuPopupAdapter( mPopup);
        this.addMouseListener(  ourPopupAdapter );
    }
    */
	
    public StrokeMenuButton(  float [] pValues, String [] pMenuNames, boolean pGenerateIcons, boolean pHasCustom)
    {
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
                //icon.setLineColor( sDefaultLineColor);
                icon.setColor( sDefaultLineColor);
                icon.setWeight( pValues[i] );
                icons[i] = icon;
            }
        }
        
        buildMenu( pValues, pMenuNames, icons, pHasCustom);
    }
	
    public StrokeMenuButton() {}
	
    public void setStroke( float pValue) {
        mStroke = pValue;
	 	
        // if we are using a LineIcon, update teh visual feedback **/
        if( getIcon() instanceof LineIcon ) {
            LineIcon icon = (LineIcon) getIcon();
            icon.setWeight( mStroke);
        }
    }
	 
    public float getStroke() {
        return mStroke;
    }
	  
    public void setPropertyValue(Object o) {
        setStroke(((Float)o).floatValue());
    }
    public Object getPropertyValue() {
        return new Float(getStroke());
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
                    handleMenuSelection(new Float(newStroke));
                }
            }
            else {
                handleMenuSelection(new Float(mMenuStroke));
            }
        }
    }

}

