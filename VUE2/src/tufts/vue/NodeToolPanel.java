package tufts.vue;


import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.beans.*;
import javax.swing.*;

import tufts.vue.beans.*;



/**
 * NodeToolPanel
 * This creates a font editor panel for editing fonts in the UI
 *
 **/
 
public class NodeToolPanel extends JPanel implements ActionListener, PropertyChangeListener {
// public class NodeToolPanel extends Box implements ActionListener, PropertyChangeListener {
 
 	////////////
 	// Statics
 	/////////////
 	
 	private static float [] sStrokeValues = { 0,1,2,3,4,5,6};
 	private static String [] sStrokeMenuLabels = { "none",
                                                       "1 pixel",
                                                       "2 pixels",
                                                       "3 pixels",
                                                       "4 pixels",
                                                       "5 pixels",
                                                       "6 pixels"  };
    
 // End of array list
 
 	
	///////////
	// Fields 
	////////////
	
	
 	
 	/** fill button **/
 	ColorMenuButton mFillColorButton = null;
 	
 	/** stroke color editor button **/
 	ColorMenuButton mStrokeColorButton = null;
 	
 	/** Text color menu editor **/
 	ColorMenuButton mTextColorButton = null;
 	
 	/** stroke size selector menu **/
 	StrokeMenuButton mStrokeButton = null;
 	
 	/** the Font selection combo box **/
 	FontEditorPanel mFontPanel = null;
 	
 	
	VueBeanState mDefaultState = null;
	
	VueBeanState mState = null;
	
	 	 	
 	
 	/////////////
 	// Constructors
 	//////////////////
 	
     
    private static boolean debug = false;
     private static final Insets NoInsets = new Insets(0,0,0,0);
    //private static final Insets ButtonInsets = new Insets(-2,-2,-2,-1);
    private static final Insets ButtonInsets = new Insets(-3,-3,-3,-2);
    //private static final Insets ButtonInsets = NoInsets;
     
    public NodeToolPanel() {
        this(false);
    }

    public NodeToolPanel(boolean textOnly) {
        //System.out.println("*** CONSTRUCTED " + this);
        //new Throwable().printStackTrace();
         //super(BoxLayout.X_AXIS);
         setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
         // squeeze everything to keep the font editor panel from going right
         // up against the edge -- any more than 2 tho and we grow bigger than
         // the VueToolPanel which we don't want to do because the whole frame resizes.
         if (debug)
             setBorder(new javax.swing.border.LineBorder(Color.pink, 2));
         else
             setBorder(new javax.swing.border.EmptyBorder(2,1,2,1));//t,l,b,r

         Color bakColor = VueResources.getColor("toolbar.background");
         if (debug) bakColor = Color.red;
         if (debug)
             setBackground(Color.blue);
         else
             setBackground( bakColor);
         Box box = Box.createHorizontalBox();
         //if (false) box.setBackground(Color.green);
         //else box.setBackground(bakColor);
         box.setBackground(bakColor);
         //this.setAlignmentX( LEFT_ALIGNMENT);
 		
         Color [] fillColors = VueResources.getColorArray( "fillColorValues");
         String [] fillColorNames = VueResources.getStringArray( "fillColorNames");
         mFillColorButton = new ColorMenuButton( fillColors, fillColorNames, true);
         mFillColorButton.setBackground( bakColor);
         ImageIcon fillIcon = VueResources.getImageIcon("nodeFillIcon");
         BlobIcon fillBlob = new BlobIcon();
         fillBlob.setOverlay( fillIcon );
         mFillColorButton.setIcon(fillBlob);
         mFillColorButton.setPropertyName( VueLWCPropertyMapper.kFillColor);
         mFillColorButton.setBorderPainted(false);
         mFillColorButton.setColor( VueResources.getColor( "defaultFillColor") );
         mFillColorButton.setMargin(ButtonInsets);

         // this skips painting background: will technically perform
         // better as we don't need to fill it with the icon filling
         // it all, but leaving off for now so we can see region for
         // debug (not a performance concern)
         // mFillColorButton.setContentAreaFilled(false);

         Color [] strokeColors = VueResources.getColorArray( "strokeColorValues");
         String [] strokeColorNames = VueResources.getStringArray( "strokeColorNames");
         mStrokeColorButton = new ColorMenuButton( strokeColors, strokeColorNames, true);
         mStrokeColorButton.setBackground( bakColor);
         ImageIcon strokeIcon = VueResources.getImageIcon("nodeStrokeIcon");
         BlobIcon strokeBlob = new BlobIcon();
         strokeBlob.setOverlay( strokeIcon );
         mStrokeColorButton.setPropertyName( VueLWCPropertyMapper.kStrokeColor);
         mStrokeColorButton.setIcon( strokeBlob);
         mStrokeColorButton.setBorderPainted(false);
         mStrokeColorButton.setMargin(ButtonInsets);

         Color [] textColors = VueResources.getColorArray( "textColorValues");
         String [] textColorNames = VueResources.getStringArray( "textColorNames");
         mTextColorButton = new ColorMenuButton( textColors, textColorNames, true);
         mTextColorButton.setBackground( bakColor);
         ImageIcon textIcon = VueResources.getImageIcon("textColorIcon");
         BlobIcon textBlob = new BlobIcon();
         textBlob.setOverlay( textIcon );
         mTextColorButton.setIcon(textBlob);
         mTextColorButton.setPropertyName(VueLWCPropertyMapper.kTextColor);
         mTextColorButton.setBorderPainted(false);
         mTextColorButton.setMargin(ButtonInsets);

         mFontPanel = new FontEditorPanel();
         if (debug)
             mFontPanel.setBackground(Color.green);
         else
             mFontPanel.setBackground(bakColor);
         mFontPanel.setPropertyName( VueLWCPropertyMapper.kFont );
 		
		
         mStrokeButton = new StrokeMenuButton( sStrokeValues, sStrokeMenuLabels, true, false);
         LineIcon lineIcon = new LineIcon( 16,12);
         mStrokeButton.setBackground( bakColor);
         mStrokeButton.setIcon( lineIcon);
         mStrokeButton.setStroke( (float) 1);
         mStrokeButton.setPropertyName( VueLWCPropertyMapper.kStrokeWeight);
         //mStrokeButton.setBorderPainted(false);
         mStrokeButton.setMargin(ButtonInsets);
 		
         if (!textOnly) {
             JLabel label = new JLabel("   Node:");
             label.setFont(VueConstants.FONT_SMALL);
             box.add(label);
             box.add( mFillColorButton);
             box.add( mStrokeColorButton);
             box.add( mStrokeButton);
         }
         box.add( mFontPanel);
         box.add( mTextColorButton);
 		
         this.add(box);
 		
         initDefaultState();
     }
 	
 	
     ////////////////
     // Methods
     /////////////////
 	
 	
 	
    protected void initDefaultState() {
        //System.out.println("NodeToolPanel.initDefaultState");
        LWNode node = new LWNode("NodeToolPanel.initializer");
        mDefaultState = VueBeans.getState(node);
        setValue(mDefaultState);
    }
 	
    /**
     * setValue
     * Generic property editor access
     **/
    public void setValue( Object pValue) {
        //System.out.println("NTP setValue " + pValue);
        VueBeanState state = null;
 		
        enablePropertyChangeListeners( false);
        if( pValue instanceof LWComponent) {
            if (!(pValue instanceof LWNode))
                return;
            state = VueBeans.getState( pValue);
        }
        else
            if( pValue instanceof VueBeanState ) {
                state = (VueBeanState) pValue;
            }
 		
        if( state == null)  {
            state = mDefaultState;
        }
 		
        mState = state;
 		
        Font font = (Font) state.getPropertyValue( VueLWCPropertyMapper.kFont);
        mFontPanel.setValue( font);
 		
        Float weight = (Float) state.getPropertyValue( VueLWCPropertyMapper.kStrokeWeight);
        float weightVal = 1;
        if( weight != null)
            weightVal = weight.floatValue();
        mStrokeButton.setStroke(weightVal);
 		
        Color fill = (Color) state.getPropertyValue( VueLWCPropertyMapper.kFillColor);
        mFillColorButton.setColor( fill);
 		
        Color stroke = (Color) state.getPropertyValue( VueLWCPropertyMapper.kStrokeColor);
        mStrokeColorButton.setColor( stroke);
 		
        Color text = (Color) state.getPropertyValue( VueLWCPropertyMapper.kTextColor);
        mTextColorButton.setColor( text);
 		
        enablePropertyChangeListeners( true);
    }
 	
    /**
     * getValue
     *
     **/
    public VueBeanState getValue() {
        return mState;
    }
 	
    /**
     *
     **/
    public void enablePropertyChangeListeners( boolean pState) {
        if( pState ) {
            mStrokeButton.addPropertyChangeListener( this );
            mFontPanel.addPropertyChangeListener( this);
            mTextColorButton.addPropertyChangeListener( this);
            mStrokeColorButton.addPropertyChangeListener( this);
            mFillColorButton.addPropertyChangeListener( this);
        } else {
            mStrokeButton.removePropertyChangeListener( this );
            mFontPanel.removePropertyChangeListener( this);
            mTextColorButton.removePropertyChangeListener( this);
            mStrokeColorButton.removePropertyChangeListener( this);
            mFillColorButton.removePropertyChangeListener( this);
        }
    }
 	 
    public void propertyChange( PropertyChangeEvent pEvent) {
        //System.out.println("Node property chaged: "+pEvent.getPropertyName());
        String name = pEvent.getPropertyName();
        if( !name.equals("ancestor") ) {
            System.out.println("Node property chaged: "+ pEvent.getPropertyName() + " " + pEvent);
	  		
            VueBeans.setPropertyValueForLWSelection( VUE.ModelSelection, name, pEvent.getNewValue() );
            if( mState != null) {
                mState.setPropertyValue( name, pEvent.getNewValue() );
            }
            else {
                // should never happen
                System.out.println("!!! Node ToolPanel mState is null!");
            }
            if( mDefaultState != null) {
                mDefaultState.setPropertyValue( name, pEvent.getNewValue() );
            }
            else {
                // should never happen
                System.out.println("!!! Node ToolPanel mDefaultState is null!");
            }
        }
    }
 	
    public void actionPerformed( ActionEvent pEvent) {
 	
    }
 	
    public static void main(String[] args) {
        System.out.println("NodeToolPanel:main");
        VUE.initUI(true);

        debug = true;

        VueUtil.displayComponent(new NodeToolPanel());
    }
 	
}
