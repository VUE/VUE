package tufts.vue;


import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.beans.*;
import javax.swing.*;
import javax.swing.border.*;

import tufts.vue.beans.*;

/**
 * LWCToolPanel
 * This creates an editor panel for LWComponents
 **/
 
public class LWCToolPanel extends JPanel implements ActionListener, PropertyChangeListener
{
    /** fill button **/
    protected ColorMenuButton mFillColorButton;
    /** stroke color editor button **/
    protected ColorMenuButton mStrokeColorButton;
    /** Text color menu editor **/
    protected ColorMenuButton mTextColorButton;
    /** stroke size selector menu **/
    protected StrokeMenuButton mStrokeButton;
    /** the Font selection combo box **/
    protected FontEditorPanel mFontPanel;
 	
    protected VueBeanState mDefaultState = null;
    protected VueBeanState mState = null;
	
    protected static boolean debug = false;
     
    protected static final Insets NoInsets = new Insets(0,0,0,0);
    protected static final Insets ButtonInsets = new Insets(-3,-3,-3,-2);

    private Box box;
    
    public LWCToolPanel()
    {
        out("CONSTRUCTED.");
        if (DEBUG.INIT&&DEBUG.META) new Throwable(toString()).printStackTrace();
        
         setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
         // squeeze everything to keep the font editor panel from going right
         // up against the edge -- any more than 2 tho and we grow bigger than
         // the VueToolPanel which we don't want to do because the whole frame resizes.
         if (debug)
             setBorder(new LineBorder(Color.pink, 2));
         else
             setBorder(new EmptyBorder(2,1,2,1));//t,l,b,r

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
         mFillColorButton.addPropertyChangeListener(this); // always last or we get prop change events for setup
          
         //-------------------------------------------------------
         // Stroke Color menu
         //-------------------------------------------------------
         
         Color[] strokeColors = VueResources.getColorArray("strokeColorValues");
         String[] strokeColorNames = VueResources.getStringArray("strokeColorNames");
         mStrokeColorButton = new ColorMenuButton(strokeColors, strokeColorNames, true);
         mStrokeColorButton.setButtonIcon(new LineIcon(16,16, 4, false));
         mStrokeColorButton.setPropertyName(LWKey.StrokeColor);
         mStrokeColorButton.setToolTipText("Stroke Color");
         mStrokeColorButton.addPropertyChangeListener(this);
         
         //-------------------------------------------------------
         // Stroke Width menu
         //-------------------------------------------------------
         
         float[] strokeValues = VueResources.getFloatArray("strokeWeightValues");
         String[] strokeMenuLabels = VueResources.getStringArray("strokeWeightNames");
         mStrokeButton = new StrokeMenuButton(strokeValues, strokeMenuLabels, true, false);
         mStrokeButton.setButtonIcon(new LineIcon(16,16));
         mStrokeButton.setStroke(1f);
         mStrokeButton.setPropertyName(LWKey.StrokeWidth);
         mStrokeButton.setToolTipText("Stroke Width");
         mStrokeButton.addPropertyChangeListener(this);

         //-------------------------------------------------------
         // Text Color menu
         //-------------------------------------------------------
         
         Color[] textColors = VueResources.getColorArray("textColorValues");
         String[] textColorNames = VueResources.getStringArray("textColorNames");
         mTextColorButton = new ColorMenuButton( textColors, textColorNames, true);
         mTextColorButton.setText("");
         mTextColorButton.setBackground( bakColor);
         ImageIcon textIcon = VueResources.getImageIcon("textColorIcon");
         BlobIcon textBlob = new BlobIcon();
         textBlob.setOverlay( textIcon );
         mTextColorButton.setButtonIcon(textBlob);
         mTextColorButton.setPropertyName(LWKey.TextColor);
         mTextColorButton.setBorderPainted(false);
         mTextColorButton.setMargin(ButtonInsets);
         mTextColorButton.setToolTipText("Text Color");
         mTextColorButton.addPropertyChangeListener(this);

         //-------------------------------------------------------
         // Font face & size editor
         //-------------------------------------------------------
         
         mFontPanel = new FontEditorPanel();
         if (debug)
             mFontPanel.setBackground(Color.green);
         else
             mFontPanel.setBackground(bakColor);
         mFontPanel.setPropertyName( LWKey.Font );
         mFontPanel.addPropertyChangeListener(this);
 		
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

         buildBox();

         add(box);
 		
         initDefaultState();
    }


    protected void buildBox() {
        box.add( mFillColorButton);
        box.add( mStrokeColorButton);
        box.add( mStrokeButton);
        box.add( mFontPanel);
        box.add( mTextColorButton);
    }

    protected JComponent getBox() {
        return box;
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
        
    /** load values from either a LWComponent, or a VueBeanState */
    void loadValues(Object pValue) {
        //System.out.println(this + " loadValues (LWCToolPanel) " + pValue);
        VueBeanState state = null;
 		
        if (pValue instanceof LWComponent) {
            if (!isPreferredType(pValue))
                return;
            state = VueBeans.getState(pValue);
            if (DEBUG.SELECTION) System.out.println(this + " loadValues (LWCToolPanel) " + state + " from " + pValue);
        } else if (pValue instanceof VueBeanState) {
            state = (VueBeanState) pValue;
            if (DEBUG.SELECTION) System.out.println(this + " loadValues (LWCToolPanel) " + state);
        }
        if (state == null)
            state = mDefaultState;
 		
        mState = state;
        
        //new Throwable().printStackTrace();
        
        setIgnorePropertyChangeEvents(true);
        
        mFontPanel.setValue(state.getPropertyValue(LWKey.Font));
        mTextColorButton.loadPropertyValue(state); // until is a MenuButton, might as will pick up property

        if (false) {
          mFillColorButton.loadPropertyValue(state);
        mStrokeColorButton.loadPropertyValue(state);
             mStrokeButton.loadPropertyValue(state);
        }

        setIgnorePropertyChangeEvents(false);
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
 	
    public VueBeanState getValue() {
        return mState;
    }
 	
    private boolean mIgnoreEvents = false;
    protected void setIgnorePropertyChangeEvents(boolean t) {
        mIgnoreEvents = t;
    }

    public void propertyChange( PropertyChangeEvent e)
    {
        if (mIgnoreEvents)
            return;
        String name = e.getPropertyName();
        //if (DEBUG.SELECTION) System.out.println(this + " PROPERTYCHANGE: " + name + " " + e);

        if( !name.equals("ancestor") ) {
            if (DEBUG.SELECTION) System.out.println(this + " propertyChange: " + name + " " + e);
	  		
            VueBeans.setPropertyValueForLWSelection(VUE.getSelection(), name, e.getNewValue());
            if (VUE.getUndoManager() != null)
                VUE.getUndoManager().markChangesAsUndo(e.getPropertyName());

            if (mState != null)
                mState.setPropertyValue( name, e.getNewValue() );
            else
                System.out.println("!!! Node ToolPanel mState is null!");

            if (mDefaultState != null)
                mDefaultState.setPropertyValue( name, e.getNewValue() );
            else
                System.out.println("!!! Node ToolPanel mDefaultState is null!");

        }
    }
 	
    public void actionPerformed( ActionEvent pEvent) {
        System.out.println(this + " " + pEvent);
 	
    }

    private void out(Object o) {
        System.out.println(this + " " + (o==null?"null":o.toString()));
    }
    public String toString() {
        return getClass().getName();
    }
    
 	
    public static void main(String[] args) {
        System.out.println("LWCToolPanel:main");
        DEBUG.Enabled = DEBUG.INIT = true;
        VUE.initUI(true);
        FontEditorPanel.sFontNames = new String[] { "Lucida Sans Typewriter", "Courier", "Arial" }; // so doesn't bother to load system fonts
        VueUtil.displayComponent(new LWCToolPanel());
        /*
        JComboBox b = new JComboBox();
        b.addItem(new JMenuItem("one")); // nope: combo box dumb
        b.addItem("two");
        VueUtil.displayComponent(b);
        */
        
    }
 	
}
