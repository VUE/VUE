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

import tufts.vue.beans.*;
import tufts.vue.gui.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.beans.*;
import javax.swing.*;
import javax.swing.border.*;


/**
   
A property editor panel for LWComponents.  General usage: a series of small
JComponents that are also LWPropertyHandlers.

 There are to major pieces to how these work:
     1 - change state of a gui component, and at least one property
             will change, and that should immediately flow thru to applicable selected objects
     2 - maintain a state object that contains an aggregate of all the current properties,
              for later querying (e.g., create a new node: what's the current state of the node
              tools? (color,shape,font, etc)

     Given this, there are two major event sources we have to deal with:
     the first are PropertyChange events coming from user interaction with
     the gui components, and the second are LWCEvents coming from currently
     selected LWComponents.  We need to keep our total state value, as
     well as gui states, sycned with any property changes from LWComponents.

     Property changes from the gui components need also to update our
     internal state, as well as flow out to the LWComponents (and in
     that case, ignore the callback coming from the LWComponent, telling
     us their property's just changed).
              
 */
 
public class LWCToolPanel extends JPanel
    implements ActionListener, PropertyChangeListener, LWComponent.Listener
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

    private Box mBox;

    private ArrayList mPropertyHandlers = new ArrayList(); // hash-map by property??

    public LWCToolPanel()
    {
        out("Constructing...");
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
         this.mBox = Box.createHorizontalBox();
         //if (false) box.setBackground(Color.green);
         //else box.setBackground(bakColor);
         mBox.setBackground(bakColor);
         //this.setAlignmentX( LEFT_ALIGNMENT);
 		
         //-------------------------------------------------------
         // Fill Color menu
         //-------------------------------------------------------
         
         Color [] fillColors = VueResources.getColorArray("fillColorValues");
         String [] fillColorNames = VueResources.getStringArray("fillColorNames");
         mFillColorButton = new ColorMenuButton(fillColors, fillColorNames, true);
         mFillColorButton.setPropertyKey(LWKey.FillColor);
         mFillColorButton.setColor(VueResources.getColor("defaultFillColor"));
         mFillColorButton.setToolTipText("Fill Color");
         mFillColorButton.addPropertyChangeListener(this); // always last or we get prop change events for setup
          
         //-------------------------------------------------------
         // Stroke Color menu
         //-------------------------------------------------------
         
         Color[] strokeColors = VueResources.getColorArray("strokeColorValues");
         String[] strokeColorNames = VueResources.getStringArray("strokeColorNames");
         mStrokeColorButton = new ColorMenuButton(strokeColors, strokeColorNames, true);
         mStrokeColorButton.setPropertyKey(LWKey.StrokeColor);
         mStrokeColorButton.setButtonIcon(new LineIcon(16,16, 4, false));
         mStrokeColorButton.setToolTipText("Stroke Color");
         mStrokeColorButton.addPropertyChangeListener(this);
         
         //-------------------------------------------------------
         // Stroke Width menu
         //-------------------------------------------------------
         
         float[] strokeValues = VueResources.getFloatArray("strokeWeightValues");
         String[] strokeMenuLabels = VueResources.getStringArray("strokeWeightNames");
         mStrokeButton = new StrokeMenuButton(strokeValues, strokeMenuLabels, true, false);
         mStrokeButton.setPropertyKey(LWKey.StrokeWidth);
         mStrokeButton.setButtonIcon(new LineIcon(16,16));
         mStrokeButton.setStroke(1f);
         //mStrokeButton.setPropertyKey(LWKey.StrokeWidth);
         mStrokeButton.setToolTipText("Stroke Width");
         mStrokeButton.addPropertyChangeListener(this);

         //-------------------------------------------------------
         // Text Color menu
         //-------------------------------------------------------
         
         Color[] textColors = VueResources.getColorArray("textColorValues");
         String[] textColorNames = VueResources.getStringArray("textColorNames");
         mTextColorButton = new ColorMenuButton(textColors, textColorNames, true);
         mTextColorButton.setPropertyKey(LWKey.TextColor);
         mTextColorButton.setToolTipText("Text Color");
         mTextColorButton.addPropertyChangeListener(this);


         /*
         mTextColorButton.setText("");
         mTextColorButton.setBackground( bakColor);
         ImageIcon textIcon = VueResources.getImageIcon("textColorIcon");
         BlobIcon textBlob = new BlobIcon();
         textBlob.setOverlay( textIcon );
         mTextColorButton.setButtonIcon(textBlob);
         mTextColorButton.setBorderPainted(false);
         mTextColorButton.setMargin(ButtonInsets);
         */

         //-------------------------------------------------------
         // Font face & size editor
         //-------------------------------------------------------
         
         mFontPanel = new FontEditorPanel();
         if (debug)
             mFontPanel.setBackground(Color.green);
         else
             mFontPanel.setBackground(bakColor);
         mFontPanel.setPropertyKey(LWKey.Font);
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
                 mBox.add(m);
             } else {
                 //fillMenu.setFocusable(true);
                 mBox.add(fillMenu);
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
         add(mBox);
         initDefaultState();
         out("CONSTRUCTED.");
    }

    /*
    protected void buildBox() {
        mBox.add( mFillColorButton);
        mBox.add( mStrokeColorButton);
        mBox.add( mStrokeButton);
        mBox.add( mFontPanel);
        mBox.add( mTextColorButton);
    }
    */

    protected void buildBox() {
        addComponent(mFillColorButton);
        addComponent(mStrokeColorButton);
        addComponent(mStrokeButton);
        addComponent(mFontPanel);
        addComponent(mTextColorButton);
    }

    /** @param c component to add to the box-row.  If an instance of LWPropertyProducer,
     * also add to out tracking list of these for property updates */
    public void addComponent(Component c) {
        mBox.add(c);
        if (c instanceof LWPropertyHandler)
            addPropertyProducer((LWPropertyHandler) c);
    }

    // todo: would be even sweeter to on addNotify (first time only!)
    // search our entire component hierarchy for property handlers
    // and automatically add them
    protected void addPropertyProducer(LWPropertyHandler p) {
        mPropertyHandlers.add(p);
    }

    protected JComponent getBox() {
        return mBox;
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
    void loadValues(Object source) {
        if (DEBUG.TOOL) out("loadValues0 (LWCToolPanel) " + source);
        VueBeanState state = null;
 		
        if (source instanceof LWComponent) {
            if (!isPreferredType(source))
                return;
            //state = VueBeans.getState(source);
            if (DEBUG.TOOL) out("loadValues1 (LWCToolPanel) " + state);
        } else if (source instanceof VueBeanState) {
            state = (VueBeanState) source;
            //if (DEBUG.TOOL) out("loadValues2 (LWCToolPanel) " + state);
        }
        //if (state == null)
             //    state = mDefaultState;
        if (source == null)
            source = mDefaultState;
 		
        mState = state;
        
        //new Throwable().printStackTrace();
        
        setIgnorePropertyChangeEvents(true);

        //------------------------------------------------------------------
        // OKAY, COOL: now, really make LWCToolPanel hold an ArrayList
        // of LWPropertyHanders: change buildBox to addToBox, and in
        // it check for any LWPropertyHanders (so NodeTool ShapeMenu
        // MenuButton is picked up) then we can iterate through that
        // SAME list here as we do below in loadToolValue (change name
        // to loadSingleValue or something)
        //
        // Can we make addToBox take an LWPropertyHander?  It could take
        // two args I guess, one for Component and one for handler, and
        // we could just to add(component, handler), and add(handler)
        // could be a shortcut that throws exception if not also a
        // Component.
        // ------------------------------------------------------------------

        Iterator i = mPropertyHandlers.iterator();
        while (i.hasNext()) {
            LWPropertyHandler dest = (LWPropertyHandler) i.next();
            copyProperty(source, dest);
        }

        /*
        copyProperty(source, mFontPanel);
        copyProperty(source, mTextColorButton);
        copyProperty(source, mFillColorButton);
        copyProperty(source, mStrokeColorButton);
        copyProperty(source, mStrokeButton);
        */

        /*
        mFontPanel.setValue(state.getPropertyValue(LWKey.Font));
          mTextColorButton.loadPropertyValue(state); // until is a MenuButton, might as will pick up property
          mFillColorButton.loadPropertyValue(state);
        mStrokeColorButton.loadPropertyValue(state);
             mStrokeButton.loadPropertyValue(state);
        */

        setIgnorePropertyChangeEvents(false);
    }

    private void copyProperty(Object source, LWPropertyHandler dest) {
        Object propertyKey = dest.getPropertyKey();

        if (DEBUG.TOOL) System.out.println(dest + " loading [" + dest.getPropertyKey() + "] from " + source);
        if (source instanceof VueBeanState) {
            dest.setPropertyValue( ((VueBeanState)source).getPropertyValue( (String) dest.getPropertyKey()) );
        } else if (source instanceof LWComponent) {
            dest.setPropertyValue( ((LWComponent)source).getPropertyValue(dest.getPropertyKey()) );
        }
    }


    private LWComponent singleSelection = null;
    void loadValues(LWSelection s) {
        if (s.size() == 1) {
            if (singleSelection != null)
                singleSelection.removeLWCListener(this);
            loadValues(singleSelection = s.first());
            singleSelection.addLWCListener(this);

            // do array of keys supported by this tool panel... Otherwise, we'll
            // be doing constant load-values while, say, dragging the node!
            //singleSelection.addLWCListener(this, LWKey.Font);

        } else if (s.size() > 1) {
            // todo: if we are to populate the tool bar properties when
            // there's a multiple selection, what do we use?
            loadValues(s.first());
            //mFillColorButton.setColor(null);
            //mStrokeColorButton.setColor(null);
            //mTextColorButton.setColor(null);
        }

        if (s.size() != 1 && singleSelection != null) {
            singleSelection.removeLWCListener(this);
            singleSelection = null;
        }
    }

    private void loadToolValue(Object propertyKey, LWComponent src) {
        boolean success = false;
        Iterator i = mPropertyHandlers.iterator();
        while (i.hasNext()) {
            LWPropertyHandler propertyHolder = (LWPropertyHandler) i.next();
            if (DEBUG.TOOL&&DEBUG.META) System.out.println(this + " checking key [" + propertyKey + "] against " + propertyHolder);
            if (propertyHolder.getPropertyKey() == propertyKey) {
                if (DEBUG.TOOL) System.out.println(this + " matched key [" + propertyKey + "] to " + propertyHolder);
                propertyHolder.setPropertyValue(src.getPropertyValue(propertyKey));
                success = true;
            }
        }
        if (!success) {
            if (DEBUG.TOOL) System.out.println(this + " loadToolValue couldn't find LWPropertyHandler for " + propertyKey + " in " + src);
        }
    }
    
    public void LWCChanged(LWCEvent e) {
        // if we don't handle this property, loadToolValue will ignore this event
        loadToolValue(e.getWhat(), e.getComponent());
    }
 	
    
    public VueBeanState getCurrentState() {
        return mState;
    }
 	
    private boolean mIgnoreEvents = false;
    protected void setIgnorePropertyChangeEvents(boolean t) {
        mIgnoreEvents = t;
    }

    // I think we may ONLY need this if the tool panel cannot actually
    // operate on something that's currently selected.  Is it really useful
    // for the tool panel to even OPERATE if nothing is selected??
    public void propertyChange(PropertyChangeEvent e)
    {
        if (mIgnoreEvents) {
            if (DEBUG.TOOL) out("propertyChange: skipping " + e);
            return;
        }
        String name = e.getPropertyName();

        if( !name.equals("ancestor") ) {
            if (DEBUG.TOOL) out("propertyChange: " + name + " " + e);
	  		
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

        } else {
            if (DEBUG.TOOL) System.out.println(this + " ignored propertyChange: " + name + " " + e);
        }

    }
 	
    public void actionPerformed(ActionEvent pEvent) {
        out("UNHANDLED: " + pEvent);
    }

    protected void out(Object o) {
        System.out.println(this + ": " + (o==null?"null":o.toString()));
    }
    public String toString() {
        return getClass().getName();
    }
    
 	
    public static void main(String[] args) {
        System.out.println("LWCToolPanel:main");
        DEBUG.Enabled = true;
        //DEBUG.INIT = true;
        DEBUG.TOOL = DEBUG.EVENTS = true;
        DEBUG.BOXES = true;
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
