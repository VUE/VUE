package tufts.vue;


import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.beans.*;
import javax.swing.*;

import tufts.vue.beans.*;

/**
 * LWCToolPanel
 * This creates an editor panel for LWComponents
 **/
 
public class LWCToolPanel extends JPanel implements ActionListener, PropertyChangeListener
{
    /** fill button **/
    ColorMenuButton mFillColorButton;
    /** stroke color editor button **/
    ColorMenuButton mStrokeColorButton;
    /** Text color menu editor **/
    ColorMenuButton mTextColorButton;
    /** stroke size selector menu **/
    StrokeMenuButton mStrokeButton;
    /** the Font selection combo box **/
    FontEditorPanel mFontPanel;
 	
    VueBeanState mDefaultState = null;
    VueBeanState mState = null;
	
    protected static boolean debug = false;
     
    protected static final Insets NoInsets = new Insets(0,0,0,0);
    protected static final Insets ButtonInsets = new Insets(-3,-3,-3,-2);
    //private static final Insets ButtonInsets = new Insets(-2,-2,-2,-1);
    //private static final Insets ButtonInsets = NoInsets;

    private static final float[] sStrokeValues = { 0,1,2,3,4,5,6};
    private static final String[] sStrokeMenuLabels
        = { "none",
            "1 pixel",
            "2 pixels",
            "3 pixels",
            "4 pixels",
            "5 pixels",
            "6 pixels"  };

    private Box box;
    
    public LWCToolPanel() {
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
         this.box = Box.createHorizontalBox();
         //if (false) box.setBackground(Color.green);
         //else box.setBackground(bakColor);
         box.setBackground(bakColor);
         //this.setAlignmentX( LEFT_ALIGNMENT);
 		
         //-------------------------------------------------------
         // Fill Color menu
         //-------------------------------------------------------
         
         Color [] fillColors = VueResources.getColorArray( "fillColorValues");
         String [] fillColorNames = VueResources.getStringArray( "fillColorNames");
         mFillColorButton = new ColorMenuButton(fillColors, fillColorNames, true);
         mFillColorButton.setColor(VueResources.getColor("defaultFillColor") );
         mFillColorButton.setPropertyName(LWKey.FillColor);
         mFillColorButton.setToolTipText("Fill Color");
          
         //-------------------------------------------------------
         // Stroke Color menu
         //-------------------------------------------------------
         
         Color[] strokeColors = VueResources.getColorArray("strokeColorValues");
         String[] strokeColorNames = VueResources.getStringArray("strokeColorNames");
         mStrokeColorButton = new ColorMenuButton(strokeColors, strokeColorNames, true);
         mStrokeColorButton.setIcon(new LineIcon(20,16, 3));
         mStrokeColorButton.setPropertyName(LWKey.StrokeColor);
         mStrokeColorButton.setToolTipText("Stroke Color");
         
         //-------------------------------------------------------
         // Stroke Width menu
         //-------------------------------------------------------
         
         mStrokeButton = new StrokeMenuButton(sStrokeValues, sStrokeMenuLabels, true, false);
         mStrokeButton.setIcon(new LineIcon(20,16));
         mStrokeButton.setStroke( (float) 1);
         mStrokeButton.setPropertyName( LWKey.StrokeWidth);
         mStrokeButton.setToolTipText("Stroke Width");

         //-------------------------------------------------------
         // Text Color menu
         //-------------------------------------------------------
         
         Color[] textColors = VueResources.getColorArray("textColorValues");
         String[] textColorNames = VueResources.getStringArray("textColorNames");
         mTextColorButton = new ColorMenuButton( textColors, textColorNames, true);
         mTextColorButton.setBackground( bakColor);
         ImageIcon textIcon = VueResources.getImageIcon("textColorIcon");
         BlobIcon textBlob = new BlobIcon();
         textBlob.setOverlay( textIcon );
         mTextColorButton.setIcon(textBlob);
         mTextColorButton.setPropertyName(LWKey.TextColor);
         mTextColorButton.setBorderPainted(false);
         mTextColorButton.setMargin(ButtonInsets);
         mTextColorButton.setToolTipText("Text Color");

         //-------------------------------------------------------
         // Font face & size editor
         //-------------------------------------------------------
         
         mFontPanel = new FontEditorPanel();
         if (debug)
             mFontPanel.setBackground(Color.green);
         else
             mFontPanel.setBackground(bakColor);
         mFontPanel.setPropertyName( LWKey.Font );
 		
         //-------------------------------------------------------
         // Optional label for the contextual toolbar
         //-------------------------------------------------------

         JComponent c = getLabelComponent();
         if (c != null)
             box.add(c);

         //-------------------------------------------------------
         if (debug) {
             JComponent m = new JMenuBar();
             //m.setLayout(new BoxLayout(m, BoxLayout.Y_AXIS));
             //m.setLayout(new FlowLayout());
             m.setLayout(new BorderLayout());
             //JComponent m = new JToolBar();
             final JMenu fillMenu = new JMenu("fill");
             fillMenu.setIcon(new BlobIcon(20,20, Color.green));
             fillMenu.add("red");
             fillMenu.add("green");
             if (true) {
                 m.add(BorderLayout.CENTER, fillMenu);
                 //m.setBorderPainted(false);
                 box.add(m);
             } else {
                 //fillMenu.setFocusable(true);
                 box.add(fillMenu);
                 fillMenu.addMouseListener(new MouseAdapter() {
                         public void mousePressed(MouseEvent e) {
                             //Component c = e.getComponent(); 	
                             //mPopup.show(c,  0, (int) c.getBounds().getHeight());
                             fillMenu.setPopupMenuVisible(!fillMenu.isPopupMenuVisible());
                         }
                         public void X_mouseReleased(MouseEvent e) {
                             fillMenu.setPopupMenuVisible(false);
                             /*
                               if( mPopup.isVisible() ) {
                               //mPopup.setVisible( false);
                               Component c = e.getComponent(); 	
                               ((JButton) c).doClick();
                               }
                             */
                         }
                     });
             }
         }
         //-------------------------------------------------------
         
         
         if (!(this instanceof LinkToolPanel))
             box.add( mFillColorButton);
         box.add( mStrokeColorButton);
         box.add( mStrokeButton);
         box.add( mFontPanel);
         box.add( mTextColorButton);
 		
         this.add(box);
 		
         initDefaultState();
    }

    protected JComponent getBox() {
        return box;
    }
    

    protected JComponent getLabelComponent() {
        return null;
    }
 	
    protected void initDefaultState() {
        //System.out.println("NodeToolPanel.initDefaultState");
        LWNode node = new LWNode("LWCToolPanel.initializer");
        mDefaultState = VueBeans.getState(node);
        loadValues(mDefaultState);
    }

    public static boolean isPreferredType(Object o) {
        return o instanceof LWComponent;
    }
        
    void loadValues(Object pValue) {
        System.out.println(this + " loadValues (LWCToolPanel) " + pValue);
        VueBeanState state = null;
 		
        if (pValue instanceof LWComponent) {
            if (!isPreferredType(pValue))
                return;
            state = VueBeans.getState(pValue);
        } else if (pValue instanceof VueBeanState) {
            state = (VueBeanState) pValue;
        }
        if (state == null)
            state = mDefaultState;
 		
        mState = state;
 		
        enablePropertyChangeListeners(false);
        
        Font font = (Font) state.getPropertyValue( LWKey.Font);
        mFontPanel.setValue( font);
 		
        Float weight = (Float) state.getPropertyValue( LWKey.StrokeWidth);
        float weightVal = 1;
        if( weight != null)
            weightVal = weight.floatValue();
        mStrokeButton.setStroke(weightVal);
 		
        Color fill = (Color) state.getPropertyValue( LWKey.FillColor);
        mFillColorButton.setColor( fill);
 		
        Color stroke = (Color) state.getPropertyValue( LWKey.StrokeColor);
        mStrokeColorButton.setColor( stroke);
 		
        Color text = (Color) state.getPropertyValue( LWKey.TextColor);
        mTextColorButton.setColor( text);
 		
        enablePropertyChangeListeners( true);
    }

    void loadValues(LWSelection s) {
        // todo: what's the sanest thing to do here?
        if (s.size() == 1) {
            loadValues(s.first());
        } else if (s.size() > 1) {
            //loadValues(s.first());
            //mFillColorButton.setColor(null);
            //mStrokeColorButton.setColor(null);
            //mTextColorButton.setColor(null);
        }
    }
 	
    /**
     * getValue
     *
     **/
    public VueBeanState getValue() {
        return mState;
    }
 	
    protected void enablePropertyChangeListeners( boolean pState) {
        if (pState) {
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
 	 
    public void propertyChange( PropertyChangeEvent pEvent)
    {
        //System.out.println("Node property chaged: "+pEvent.getPropertyName());
        String name = pEvent.getPropertyName();
        if( !name.equals("ancestor") ) {
            System.out.println("LWC property changed: "+ pEvent.getPropertyName() + " " + pEvent);
	  		
            VueBeans.setPropertyValueForLWSelection(VUE.getSelection(), name, pEvent.getNewValue());
            if (VUE.getUndoManager() != null)
                VUE.getUndoManager().markChangesAsUndo(pEvent.getPropertyName());

            if (mState != null)
                mState.setPropertyValue( name, pEvent.getNewValue() );
            else
                System.out.println("!!! Node ToolPanel mState is null!");

            if (mDefaultState != null)
                mDefaultState.setPropertyValue( name, pEvent.getNewValue() );
            else
                System.out.println("!!! Node ToolPanel mDefaultState is null!");

        }
    }
 	
    public void actionPerformed( ActionEvent pEvent) {
        System.out.println(this + " " + pEvent);
 	
    }

    public String toString() {
        return getClass().getName();
    }
    
 	
    public static void main(String[] args) {
        System.out.println("LWCToolPanel:main");
        VUE.initUI(true);
        FontEditorPanel.sFontNames = new String[] { "Lucida Sans Typewriter", "Courier", "Arial" }; // so doesn't bother to load system fonts
        VueUtil.displayComponent(new LWCToolPanel());
        JComboBox b = new JComboBox();
        b.addItem("one");
        b.addItem("two");
        VueUtil.displayComponent(b);
        
    }
 	
}
