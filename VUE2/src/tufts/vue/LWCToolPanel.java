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
JComponents that are also LWPropertyProducers

 There are to major pieces to how these work:
     1 - change state of a gui component, and at least one property
             will change, and that should immediately flow thru to applicable selected objects
     2 - maintain a state object that contains an aggregate of all the current properties,
              for later querying (e.g., create a new node: what's the current state of the node
              tools? (color,shape,font, etc)

     Given this, there are three major event sources we have to deal with:
     the first are PropertyChange events coming from user interaction with
     the gui components, and the second are LWCEvents coming from currently
     selected LWComponents.  We need to keep our total state value, as
     well as gui states, sycned with any property changes from LWComponents.
     The third is the current selection: when that changes, we need to
     re-init everything from the current selection (note that changing
     the selection may have also installed a completely different active
     contextual tool panel).  Currently the selection change handling
     is mediated by the VueToolbarController (and maybe we want to have
     it mediate all of this?)

     Property changes from the gui components need also to update our
     internal state, as well as flow out to the LWComponents (and in
     that case, ignore the callback coming from the LWComponent, telling
     us their property's just changed).

In total: Mediates back and forth between the selection and tool states.
The VueBeanState cached holds the property values for us even if there
is no selection (so we can change the tool state w/out a selection and
remember it).
              
 */

// TODO: break out the default color & font property crap, and make this
// an abstract class that only has the property change code, VueBeanState
// caching code, and the handling of multi-selection code (uh, that TDB).

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

    private ArrayList mPropertyProducers = new ArrayList(); // hash-map by property??

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

         Color bakColor = VueTheme.getToolbarColor();
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
     * also add to our tracking list of these for property updates */
    public void addComponent(Component c) {
        mBox.add(c);
        if (c instanceof LWPropertyProducer)
            addPropertyProducer((LWPropertyProducer) c);
    }

    // todo: would be even sweeter to on addNotify (first time only!)
    // search our entire component hierarchy for property handlers
    // and automatically add them
    protected void addPropertyProducer(LWPropertyProducer p) {
        mPropertyProducers.add(p);
    }

    protected JComponent getBox() {
        return mBox;
    }

    protected VueBeanState getDefaultState() {
        return VueBeans.getState(new LWNode("LWCToolPanel.initializer"));
    }
    
    protected void initDefaultState() {
        //System.out.println("NodeToolPanel.initDefaultState");
        mDefaultState = getDefaultState();
        out("default state initialized to " + mDefaultState);
        loadValues(mDefaultState);
    }

    public boolean isPreferredType(Object o) {
        return o instanceof LWComponent;
    }
        
    /** load values from either a LWComponent, or a VueBeanState */
    private void loadValues(Object source) {
        if (DEBUG.TOOL) out("loadValues0 (LWCToolPanel) " + source);
        VueBeanState state = null;
 		
        if (source instanceof LWComponent) {
            if (!isPreferredType(source))
                return;
            state = VueBeans.getState(source);
            if (DEBUG.TOOL) out("loadValues1 (LWCToolPanel) " + state);
        } else if (source instanceof VueBeanState) {
            state = (VueBeanState) source;
            //if (DEBUG.TOOL) out("loadValues2 (LWCToolPanel) " + state);
        }

        if (state == null)
            mState = mDefaultState;
        else
            mState = state;

        if (source == null)
            source = mState;
        
        setIgnorePropertyChangeEvents(true);

        Iterator i = mPropertyProducers.iterator();
        while (i.hasNext()) {
            LWPropertyProducer dest = (LWPropertyProducer) i.next();
            loadProperty(source, dest);
        }
        setIgnorePropertyChangeEvents(false);
    }

    private void loadProperty(Object source, LWPropertyProducer dest) {
        if (DEBUG.TOOL) System.out.println(dest + " loading [" + dest.getPropertyKey() + "] from " + source);
        Object value = null;
        if (source instanceof VueBeanState)
            value = ((VueBeanState)source).getPropertyValue(dest.getPropertyKey().toString());
        else //if (source instanceof LWComponent)
            value = ((LWComponent)source).getPropertyValue(dest.getPropertyKey());

        if (value != null) {
            if (DEBUG.TOOL) out("loadProperty " + source + " -> " + dest);
            dest.setPropertyValue(value);
        }
    }

    // generic full version of property picker-uppers:
    // When selection changes, pick up properties of new selected item (or multi-prop state when handle multi-selections)
    // When a PROPERTY changes IN the selection, pick that up.
    // Can we combine these two to something generic? (either as one loop, or at least as one helper class)


    private LWComponent singleSelection = null;
    // currenly only called from VueToolbarController
    void loadSelection(LWSelection s) {
        if (DEBUG.TOOL) out("loadSelection " + s);
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
        }

        if (s.size() != 1 && singleSelection != null) {
            singleSelection.removeLWCListener(this);
            singleSelection = null;
        }
    }

    private void loadToolValue(Object propertyKey, LWComponent src) {
        boolean success = false;
        Iterator i = mPropertyProducers.iterator();
        while (i.hasNext()) {
            LWPropertyProducer propertyProducer = (LWPropertyProducer) i.next();
            if (DEBUG.TOOL&&DEBUG.META) System.out.println(this + " checking key [" + propertyKey + "] against " + propertyProducer);
            if (propertyProducer.getPropertyKey() == propertyKey) {
                if (DEBUG.TOOL) out("matched key [" + propertyKey + "] to " + propertyProducer);
                Object value = src.getPropertyValue(propertyKey);
                propertyProducer.setPropertyValue(value);
                mState.setPropertyValue(propertyKey.toString(), value);
                success = true;
            }
        }
        if (!success) {
            if (DEBUG.TOOL) System.out.println(this + " loadToolValue: FYI, no LWPropertyProducer for '" + propertyKey + "' in " + src);
        }
    }
    
    public VueBeanState getCurrentState() {
        return mState;
    }
 	
    private boolean mIgnoreEvents = false;
    protected void setIgnorePropertyChangeEvents(boolean t) {
        mIgnoreEvents = t;
    }


    private boolean mIgnoreLWCEvents = false;
    public void LWCChanged(LWCEvent e) {
        // if we don't handle this property, loadToolValue will ignore this event
        if (mIgnoreLWCEvents) {
            if (DEBUG.TOOL) out("ignoring during propertyChange: " + e);
        } else {
            loadToolValue(e.getWhat(), e.getComponent());
        }
    }
 	
    
    /** This is called when some gui sub-component of the LWCToolPanel
     * has changed state, indicating a different property value.  We
     * get this as a PropertyChangeEvent here, apply it as we can to
     * everything in the LWSelection, and apply the property value to
     * the current default state reprsented by this tool panel, used
     * for initializing new LWComponents.
     */
    // What's handy about this is simply that JComponents already have
    // a list of listeners we can use, and addListener methods.
    // So maybe try subclassing PropertyChangeEvent with LWPropertyChangeEvent
    // or something, and only pay attention to THOSE property changes.
    // Still make sure we don't have a looping issue, tho I think that's handled.
    public void propertyChange(PropertyChangeEvent e)
    {
        if (e instanceof LWPropertyChangeEvent) {

            final String propertyName = e.getPropertyName();

            if (mIgnoreEvents) {
                if (DEBUG.TOOL) out("propertyChange: skipping " + e + " name=" + propertyName);
                return;
            }
            
            if (DEBUG.TOOL) out("propertyChange: [" + propertyName + "] " + e);
	  		
            mIgnoreLWCEvents = true;
            VueBeans.applyPropertyValueToSelection(VUE.getSelection(), propertyName, e.getNewValue());
            mIgnoreLWCEvents = false;
            
            if (VUE.getUndoManager() != null)
                VUE.getUndoManager().markChangesAsUndo(propertyName);

            if (mState != null)
                mState.setPropertyValue(propertyName, e.getNewValue());
            else
                out("mState is null");

            if (mDefaultState != null)
                mDefaultState.setPropertyValue(propertyName, e.getNewValue());
            else
                out("mDefaultState is null");

            if (DEBUG.TOOL && DEBUG.META) out("new state " + mState);

        } else {
            // We're not interested in "ancestor" events, icon change events, etc.
            if (DEBUG.TOOL && DEBUG.META) out("ignored AWT/Swing: " + e + " name=" + e.getPropertyName());
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
        VUE.parseArgs(args);
        DEBUG.Enabled = true;
        //DEBUG.INIT = true;
        DEBUG.TOOL = DEBUG.EVENTS = true;
        //DEBUG.BOXES = true;
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
