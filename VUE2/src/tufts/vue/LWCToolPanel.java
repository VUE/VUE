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

//import tufts.vue.beans.*;
import tufts.vue.gui.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.beans.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;


/**  @deprecated -- property handling functionality replaced by VUE.LWToolManager (and tufts.vue.ToolPanel provided
     for interim GUI support */
   
/*
A property editor panel for LWComponents.  General usage: a series of small
JComponents that are also LWEditors

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
    implements ActionListener, PropertyChangeListener, ChangeListener, LWComponent.Listener
{        

 	
    //protected VueBeanState mDefaultState = null;
    //protected VueBeanState mState = null;
    protected final LWComponent mDefaultState;
	
    protected static boolean debug = false;
     
    protected static final Insets NoInsets = new Insets(0,0,0,0);
    protected static final Insets ButtonInsets = new Insets(-3,-3,-3,-2);

    protected JPanel mBox;

    private final Collection<LWEditor> mEditors = new HashSet<LWEditor>();

   
    public LWCToolPanel()
    {
        if (DEBUG.INIT) out("Constructing...");
        if (DEBUG.INIT&&DEBUG.META) new Throwable(toString()).printStackTrace();
        
         setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
         // squeeze everything to keep the font editor panel from going right
         // up against the edge -- any more than 2 tho and we grow bigger than
         // the VueToolPanel which we don't want to do because the whole frame resizes.
         if (debug)
             setBorder(new LineBorder(Color.pink, 2));
         else
             setBorder(new EmptyBorder(2,1,2,1));//t,l,b,r

         if (debug)
             setBackground(Color.blue);
         else
             GUI.applyToolbarColor(this);
         setOpaque(false);
         
         
         mBox = new JPanel();//Box.createHorizontalBox();
         mBox.setLayout(new GridBagLayout());
         GUI.applyToolbarColor(mBox);
         
         //this.setAlignmentX( LEFT_ALIGNMENT);

         // Note: oddly, in Mac Aqua L&F, if we set the button icon
         // (MenuButton.setButtonIcon) during init, we get a rounded
         // button.  If we don't, we get a square button!
 		
       
    
         		
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
         //initDefaultState();
         mDefaultState = createDefaultStyle();
         if (DEBUG.INIT) out("CONSTRUCTED.");
    }


    protected void buildBox() {
    	
    }

    /** @param c component to add to the box-row.  If an instance of LWPropertyProducer,
     * also add to our tracking list of these for property updates */
    public boolean addComponent(Component c) {
        out("addComponent IGNORED " + c);

        /*
        if (c == null)
            return false;
        //mBox.add(c);
        if (c instanceof LWEditor) {
            if (DEBUG.TOOL) System.out.println("*** FOUND AS EDITOR " + c);
            addEditor((LWEditor) c);
        }
        if (c instanceof java.awt.Container) {
            // check for any editors in the children
            new EventRaiser<JComponent>(this, JComponent.class) {
                public void dispatch(JComponent c) {
                    ActionListener[] al = c.getListeners(ActionListener.class);
                    if (al == null || al.length == 0)
                        return;
                    //System.out.println("*** FOUND ACTIONLISTENERS " + al.length + " " + c);
                    //System.out.println("*** FOUND ACTIONLISTENERS " + al.length + " " + c);
                    for (ActionListener l : al) {
                        if (l instanceof LWEditor) {
                            if (DEBUG.TOOL) System.out.println("*** FOUND EDITOR " + l + " on " + c);
                            mEditors.add((LWEditor)l);
                            //if (!mEditors.add((LWEditor)l)) out("would have duplicated edtior: " + l);
                        }
                    }
                }
            }.raiseStartingAt((Container)c);
        }
        */
        return true;
    }

    // todo: would be even sweeter to on addNotify (first time only!)
    // search our entire component hierarchy for property handlers
    // and automatically add them
    protected void addEditor(LWEditor editor) {
        mEditors.add(editor);
    }

    protected JComponent getBox() {
        return mBox;
    }

    /** Return the state (style) that represents what values the tools will hold
     * when nothing is selected, and which is used for creating new objects */
    protected LWComponent createDefaultStyle() {
        //return VueBeans.getState(new LWNode("LWCToolPanel.initializer"));
        return new LWNode("defaultNodeStyle");
    }
    
    /** return the style to be used for creating new objects */
    public LWComponent getCreationStyle() {
        return mDefaultState;
    }

    private static boolean IgnoreEditorChangeEvents = false;
    
    private void loadAllEditors(LWSelection selection)
    {
        LWComponent propertySource = selection.first(); // TODO: this not what we want if selection size > 1
        
        if (DEBUG.TOOL) out("loadValues (LWCToolPanel) " + propertySource);

        // While the editors are loading, we want to ignore the
        // any change events that loading may produce.
        IgnoreEditorChangeEvents = true;
        try {
            for (LWEditor editor : mEditors) {
                boolean supported = selection.hasEditableProperty(editor.getPropertyKey());
                if (DEBUG.TOOL) out("SET-ENABLED " + (supported?"YES":" NO") + ": " + editor);
                editor.setEnabled(supported);
                if (supported)
                    loadEditor(propertySource, editor);
            }
        } finally {
            IgnoreEditorChangeEvents = false;
        }
    }
    
    private void loadEditor(LWComponent source, LWEditor editor) {
        if (DEBUG.TOOL&&DEBUG.META) out("loadEditor0 " + editor + " loading " + editor.getPropertyKey() + " from " + source);

        final Object value;
        final Object key = editor.getPropertyKey();
        if (source.supportsProperty(key))
            value = source.getPropertyValue(key);
        else
            value = null;
        if (value != null) {
            if (DEBUG.TOOL) out("loadEditor1 [" + value + "] -> " + editor);
            editor.displayValue(value);
        } else if (DEBUG.TOOL) out("loadEditor1 " + source + " -> " + editor + " skipped; unsupported property " + key);
    }

    private void loadEditorsMatchingKey(final Object key, final LWComponent source) {
        boolean loaded = false;
        for (LWEditor editor : mEditors) {
            try {
                //if (DEBUG.TOOL&&DEBUG.META) System.out.println(this + " checking key [" + propertyKey + "] against " + propertyProducer);
                if (editor.getPropertyKey() == key && source.supportsProperty(key)) {
                    if (DEBUG.TOOL) out("loadEditorsMatchingKey: found producer for key [" + key + "]: " + editor.getClass());
                    final Object value = source.getPropertyValue(key);
                    editor.displayValue(value);
                    //mState.setPropertyValue(propertyKey.toString(), value); // only load default state if nothing is selected...
                    loaded = true;
                }
            } catch (Throwable t) {
                tufts.Util.printStackTrace(new Throwable(t), "exception loading editor: " + editor);
            }
        }
        if (DEBUG.TOOL && DEBUG.META) {
            if (!loaded) System.out.println(this + " loadEditorsMatchingKey: FYI, no LWEditor for '" + key + "' in " + source);
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
            singleSelection = s.first();
            loadAllEditors(s);
            singleSelection.addLWCListener(this);

            // do array of keys supported by this tool panel... Otherwise, we'll
            // be doing constant load-values while, say, dragging the node!
            //singleSelection.addLWCListener(this, LWKey.Font);

        } else if (s.size() > 1) {
            // todo: if we are to populate the tool bar properties when
            // there's a multiple selection, what do we use?
            //loadAllEditors(s.first());
            loadAllEditors(s);
        }

        if (s.size() != 1 && singleSelection != null) {
            singleSelection.removeLWCListener(this);
            singleSelection = null;
        }
    }


    // need to add functionaltiyh for rapidly changing values (e.g., slider's
    // active color chooser, etc) -- in which case we'd ignore change events
    // coming from the component until the end -- can we have the component
    // issue events marked as rapid changers?  Or should subclasses of
    // LWCToolPanel just handle it?
    
    private boolean mIgnoreLWCEvents = false;
    public void LWCChanged(LWCEvent e) {
        // if we don't handle this property, loadToolValue will ignore this event
        if (mIgnoreLWCEvents) {
            if (DEBUG.TOOL) out("ignoring during propertyChange: " + e);
        } else {
            loadEditorsMatchingKey(e.key, e.getComponent());
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

    // TODO TODO TODO: each tool panel will have a style object associated
    // (LWNode, LWLink, LWNode(text version), that can be styled when
    // NOTHING ELSE IS SELECTED, and is used for creating new objects
    // only, and is loaded up when the selection goes empty.
    
    public void propertyChange(PropertyChangeEvent e)
    {

        if (e instanceof LWPropertyChangeEvent == false) {
            // We're not interested in "ancestor" events, icon change events, etc.
            if (DEBUG.TOOL && DEBUG.META) out("ignored AWT/Swing: [" + e.getPropertyName() + "] from " + e.getSource().getClass());
            return;
        } else {
            if (DEBUG.TOOL) out("propertyChange: " + e);
        }
            
        ApplyPropertyChangeToSelection(VUE.getSelection(), ((LWPropertyChangeEvent)e).key, e.getNewValue(), e.getSource());
    }

    /** Will either modifiy the active selection, or if it's empty, modify the default state (creation state) for this tool panel */
    private static void ApplyPropertyChangeToSelection(final LWSelection selection, final Object key, final Object newValue, Object source)
    {
        if (IgnoreEditorChangeEvents) {
            if (DEBUG.TOOL) System.out.println("APCTS: " + key + " " + newValue + " (skipping)");
            return;
        }
        
        if (DEBUG.TOOL) System.out.println("APCTS: " + key + " " + newValue);
        
        if (selection.isEmpty()) {
            /*
            if (mDefaultState != null)
                mDefaultState.setProperty(key, newValue);
            else
                System.out.println("mDefaultState is null");
            */
            //if (DEBUG.TOOL && DEBUG.META) out("new state " + mDefaultState); // need a style dumper
        } else {
            
            // As setting these properties in the model will trigger notify events from the selected objects
            // back up to the tools, we want to ignore those events while this is underway -- the tools
            // already have their state set to this.
            //mIgnoreLWCEvents = true;
            try {
                for (tufts.vue.LWComponent c : selection) {
                    if (c.supportsProperty(key))
                        c.setProperty(key, newValue);
                }
            } finally {
                // mIgnoreLWCEvents = false;
            }
            
            if (VUE.getUndoManager() != null)
                VUE.getUndoManager().markChangesAsUndo(key.toString());
        }
    }
    

    /** if we get a state changed, check to see if the source is one of our LWPropertyProducers,
     * and if so, simulate a propertyChange event
     * (e.g., a JSlider doesn't issue PropertyChangeEvents, only ChangeEvents, although
     * we don't want to invoke the undo-manager for the real-time update of the slider) */

    public void stateChanged(ChangeEvent e) {
        tufts.Util.printStackTrace("LWCToolPanel: stateChanged: " + e);
        final Object source = e.getSource();
        for (LWEditor editor : mEditors) {
            if (source == editor) {
                if (true||DEBUG.TOOL) out("matched ChangeEvent source to editor " + editor.getClass());
                propertyChange(new LWPropertyChangeEvent(editor, editor.getPropertyKey(), editor.produceValue()));
                // LWPropertyChangeEvent could detect that source is instanceof a editor, and pull key & value from that
            }
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



    /*

    //private static boolean sIgnoreEvents = false;
    // todo: move to a PropertyDispatch handler or something
    private static boolean sIgnoreLWCEvents = false;
    
    // todo: move this stuff to tool code somewhere
    public static void PropertyProducerChanged(tufts.vue.LWPropertyProducer producer)
    {
        //final Object key = producer.getPropertyKey().toString();
        final Object key = producer.getPropertyKey();

        if (sIgnoreLWCEvents) {
            if (DEBUG.TOOL) out("PropertyProducerChanged: skipping " + key + "  for " + producer);
            return;
        }
            
        if (DEBUG.TOOL) out("PropertyProducerChanged: [" + key + "] on " + producer);

        sIgnoreLWCEvents = true;
        try {
            //tufts.vue.beans.VueBeans.applyPropertyValueToSelection(VUE.getSelection(), key.toString(), producer.getPropertyValue());
            ApplyPropertyChangeToSelection(VUE.getSelection(), key, producer.getPropertyValue());
        } finally {
            sIgnoreLWCEvents = false;
        }
        
        if (VUE.getUndoManager() != null)
            VUE.getUndoManager().markChangesAsUndo(key.toString());

    }

*/
    

    
    
 	
    public static void main(String[] args) {
        System.out.println("LWCToolPanel:main");
        VUE.init(args);
        DEBUG.Enabled = true;
        //DEBUG.INIT = true;
        DEBUG.TOOL = DEBUG.EVENTS = true;
        //DEBUG.BOXES = true;
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



 	
    /*
    protected void initDefaultState() {
        //System.out.println("NodeToolPanel.initDefaultState");
        mDefaultState = getDefaultState();
        if (DEBUG.INIT) out("default state initialized to " + mDefaultState);
        loadValues(mDefaultState);
    }
    public boolean isPreferredType(Object o) {
        return o instanceof LWComponent;
    }
    */
    /* load values from either a LWComponent, or a VueBeanState 
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

        Iterator i = mEditors.iterator();
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
            //if (DEBUG.TOOL && DEBUG.META) out("ignored AWT/Swing: [" + e.getPropertyName() + "] from " + e.getSource().getClass());
            if (DEBUG.TOOL) out("ignored AWT/Swing: [" + e.getPropertyName() + "] from " + e.getSource().getClass());
        }

    }
    */    