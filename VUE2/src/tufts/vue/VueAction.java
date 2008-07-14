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

import tufts.Util;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EventObject;
import java.awt.event.InputEvent;
import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.AbstractButton;
import javax.swing.KeyStroke;
import javax.swing.Icon;

/**
 * Base class for VueActions that don't use the selection.
 * @see Actions.LWCAction for actions that use the selection
 *
 * @version $Revision: 1.41 $ / $Date: 2008-07-14 17:12:28 $ / $Author: sfraize $ 
 */
public class VueAction extends javax.swing.AbstractAction
{
    protected static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(VueAction.class);
    
    public static final boolean EnableSmallIconsForMenus = false;
    public static final String LARGE_ICON = "vue.largeIcon";
    private static final String CHECKBOX_LIST = "vue.checkBoxList";

    private static List<VueAction> AllActionList = new ArrayList();

    private static boolean allIgnored = false;

    private final String permanentName;

    public static List<VueAction> getAllActions() {
        return Collections.unmodifiableList(AllActionList);
    }

    static class DeniedException extends RuntimeException {
        DeniedException(String msg) { super(msg); }
    }

    /** Set's all action events to be temporarily ignored.
        E.g., used while a TextBox edit is active */
    // todo: may want to allow NewItem actions as they automatically
    // activate an edit, thus preventing a quick series of NewItem
    // actions to be done.
    static void setAllActionsIgnored(boolean disabled)
    {
        if (DEBUG.Enabled) {
            Log.debug("allIgnored=" + disabled);
            if (DEBUG.META) tufts.Util.printStackTrace("ALL ACTIONS IGNORED: " + disabled);
        }
        
        allIgnored = disabled;

        for (VueAction a : AllActionList)
            a.setEnabled(a.isUserEnabled());
    }
    
    /** for debug only */
    private static java.util.Map<KeyStroke,VueAction> AllStrokes;
    /** for debug only */
    private static java.util.Set<Action> DupeStrokeActions;
    /** for debug only */
    public static boolean isDupeStrokeAction(Action a) {
        if (DupeStrokeActions != null)
            return DupeStrokeActions.contains(a);
        else
            return false;
    }
    
    public static void checkForDupeStrokes()
    {
        if (AllStrokes != null)
            return; // already checked
        
        for (VueAction a : AllActionList)
            trackForDupeStrokes(a, (KeyStroke) a.getValue(ACCELERATOR_KEY));
    }
    
    private static void trackForDupeStrokes(VueAction a, KeyStroke keyStroke)
    {
        if (AllStrokes == null) {
            AllStrokes = new java.util.HashMap();
            DupeStrokeActions = new java.util.HashSet();
        }

        if (keyStroke != null) {

            // this is more complicated than it needs to be because KeyStroke.hashCode
            // is imperfect (different KeyStroke's can have the same hash code)
                
            final VueAction existingAction = AllStrokes.get(keyStroke);
            if (existingAction != null) {
                final KeyStroke existingStroke = (KeyStroke) existingAction.getValue(ACCELERATOR_KEY);
                if (existingStroke.equals(keyStroke)) {
                    Util.printStackTrace("WARNING; DUPLICATE KEYSTROKE: " + keyStroke + "; conflicting actions:"
                                         + "\n\t" + Util.tags(existingStroke) + "; " + Util.tags(existingAction)
                                         + "\n\t" + Util.tags(keyStroke) + "; " + Util.tags(a)
                                         );
                    DupeStrokeActions.add(existingAction);
                    DupeStrokeActions.add(a);
                }
            } else {
                AllStrokes.put(keyStroke, a);
            }
        }
    }
    

    public VueAction(String name, String shortDescription, KeyStroke keyStroke, Icon icon)
    {
        super(name);
        this.permanentName = name;
        if (shortDescription != null)
            putValue(SHORT_DESCRIPTION, shortDescription);
        else
            putValue(SHORT_DESCRIPTION, name);
        if (keyStroke != null)
            putValue(ACCELERATOR_KEY, keyStroke);

        setSmallIcon(icon);
        //if (DEBUG.Enabled) System.out.println("Constructed: " + this + " icon=" + getValue(SMALL_ICON));
        AllActionList.add(this);

        if (isSelectionWatcher())
            SelectionWatchers.add(this);

        if (DEBUG.Enabled)
            trackForDupeStrokes(this, keyStroke);
    }

    public VueAction(String name, KeyStroke keyStroke, String iconSpec) {
        this(name, null, keyStroke, null);
        setIcon(iconSpec);
    }
    public VueAction(String name, KeyStroke keyStroke) {
        this(name, null, keyStroke, null);
    }
    public VueAction(String name) {
        this(name, null, null, null);
    }
    public VueAction(String name, Icon icon) {
        this(name, null, null, icon);
    }

    private static int anonIndex = 0;
    public VueAction() {
        this(null, null, null, null);
	putValue(Action.NAME, getClass().getName() + anonIndex++);
    }    


    /** add a button that supports toggle state (e.g., a
    /** JCheckBoxMenuItem) to be updated if this action represents the
    /** toogle of a boolean the checkbox should stay synced with */
    
    public void trackToggler(AbstractButton toggler) {

        Collection list = (Collection) getValue(CHECKBOX_LIST);
        if (list == null)
            putValue(CHECKBOX_LIST, list = new java.util.ArrayList());
        
        list.add(toggler);
        toggler.setSelected(getToggleState());
    }

    protected void updateTogglers(boolean state) {
        final List<AbstractButton> toggles = (List<AbstractButton>) getValue(CHECKBOX_LIST);

        // JCheckBoxMenuItem's, which do add themselves as a property change
        // listener to the Action if you create them based on the Action, really
        // ought to override the default property change listener handler to
        // deal with this simple case: (then we wouldn't need the calls to trackToggler)
        //firePropertyChange("selected", state, !state); 

        if (toggles != null) {
            for (AbstractButton item : toggles) {
                if (DEBUG.EVENTS) out("selected->" + state + " for " + tufts.vue.gui.GUI.name(item) + " (isSelected=" + item.isSelected() + ")");
                item.setSelected(state);
            }
        }
    }

    private static final Boolean IS_NOT_A_TOGGLER = new Boolean(false);

    /**
     * if this is overridden to return a varying result, any AbstractButton watchers
     * of this action (added via trackToggler) will have their setSelected method
     * called with it's value.
     */
    public Boolean getToggleState() {
        // returning a fixed boolean instance is a clever way of knowing if
        // this has been overridden
        return IS_NOT_A_TOGGLER;
    }

    private void setIcon(String iconSpec) {
        Icon icon = null;
        if (iconSpec.startsWith(":")) {
            if (EnableSmallIconsForMenus) {
                icon = VueResources.getImageIconResource("/toolbarButtonGraphics/" + iconSpec.substring(1) + "16.gif");
                setSmallIcon(icon);
            }
            icon = VueResources.getImageIconResource("/toolbarButtonGraphics/" + iconSpec.substring(1) + "24.gif");
            putValue(LARGE_ICON, icon);
        } else {
            if (EnableSmallIconsForMenus) {
                icon = VueResources.getImageIconResource(iconSpec);
                setSmallIcon(icon);
            }
        }
    }

    private void setSmallIcon(Icon icon) {
        if (EnableSmallIconsForMenus) {
            if (icon != null) {
                if (icon != tufts.vue.gui.GUI.NO_ICON)
                    putValue(SMALL_ICON, icon);
            }
        }
    }


    /** undoable: the undo manager already won't bother to create an
     * undo action if it didn't detect any changes.  This method is
     * here as a backup just in case we know for sure we don't even
     * want to talk to the undo manager during an action, such as the
     * undo actions.
     */
    boolean undoable() { return true; }

    /** @return false (the default) if this is an editing action - overide and return false
     * to enable as a non-editing action
     */
    boolean isEditAction() {
        return false;
    }

    public String getActionName()
    {
        return (String) getValue(Action.NAME);
    }
    /**
     * @return the base name for this action that never changes.  E.g.:,
     * for UndoAction, return just "Undo" instead of "Undo <most recent action>"
     */
    public String getPermanentActionName()
    {
        return this.permanentName;
    }
    public void setActionName(String s)
    {
        putValue(Action.NAME, s);
    }

    public void revertActionName()
    {
        setActionName(getPermanentActionName());
    }
    

    public boolean overrideIgnoreAllActions() { return false; }

    private void dumpEvent(ActionEvent actionEvent) {

        final StringBuffer m = new StringBuffer(512);
            
        m.append(this);
        m.append(Util.TERM_GREEN);
        m.append("; actionPerformed: " + getClass().getName());
        m.append(";\n    ActionEvent: (" + actionEvent.paramString() + ")");
            
        String src;
        Object source = actionEvent.getSource();
            
        for (int i = 0; i < 10; i++) { // looping failsafe: should never be more than 2 levels
                
            if (source instanceof EventWrap) {
                m.append(String.format("\n%15s: %s", "from", ((EventWrap)source).target));
                source = ((EventWrap)source).event;
            }
                
            if (source instanceof EventObject) {
                EventObject e = (EventObject) source;
                
                //m.append('\n');
                if (e instanceof InputEvent) {
                    m.append(String.format("\n%14s%d: %s[%s]", "input source", i, e.getClass().getSimpleName(), ((InputEvent)e).paramString()));
                } else {
                    m.append(String.format("\n%14s%d: %s", "source", i, Util.tags(e)));
                }
                source = e.getSource();
            } else {
                m.append(String.format("\n%15s: %s", "source#", Util.tags(source)));
                break;
            }
        }
        m.append(Util.TERM_CLEAR);
        //m.append('\n');
        Log.debug(m.toString());
    }
        
    public void actionPerformed(ActionEvent ae)
    {
        if (DEBUG.EVENTS) {
            System.out.println("\n===============================================================================================================");
            try { dumpEvent(ae); } catch (Throwable t) { t.printStackTrace(); }
        }
        
        if (allIgnored && !isUserEnabled()) {
            //if (DEBUG.Enabled) Log.debug("ALL ACTIONS DISABLED; " + this + "; " + ae);
            if (DEBUG.Enabled) {
                Util.printStackTrace("ALL ACTIONS DISABLED; " + this + "; " + ae);
            } else {
                Log.debug("all actions disabled; disallowed: " + this + "; " + ae);
                java.awt.Toolkit.getDefaultToolkit().beep();
            }
            return;
        }
        boolean hadException = false;
        
        try {

            if (isUserEnabled()) {
                
                act();

                final Boolean state = getToggleState();
                if (getToggleState() != IS_NOT_A_TOGGLER) {
                    //if (DEBUG.EVENTS) out("new toggle state: " + Util.tags(state));
                    updateTogglers(state);
                }
                
                
            } else {
                java.awt.Toolkit.getDefaultToolkit().beep();
                Log.info(getActionName() + ": Not currently enabled");
            }
        } catch (DeniedException e) {
            Log.info("Denied: " + this + "; " + e.getMessage());
        } catch (Throwable t) {
            synchronized (System.err) {
                System.err.println("*** VueAction: exception during action [" + getActionName() + "]");
                System.err.println("*** VueAction: " + getClass());
                System.err.println("*** VueAction: selection is " + VUE.getSelection());
                System.err.println("*** VueAction: event was " + ae);
                tufts.Util.printStackTrace(t);
            }
            hadException = true;
        }

        if (VUE.getUndoManager() != null && undoable()) {
            VUE.getUndoManager().markChangesAsUndo(getUndoName(ae,hadException));
        }

        updateSelectionWatchers(VUE.getSelection());

        //if (DEBUG.EVENTS) Log.debug(this + " END OF actionPerformed: ActionEvent=" + ae.paramString() + "\n");
        if (DEBUG.EVENTS) Log.debug("\n===============================================================================================================\n");
        
        // normally handled by updateActionListeners, but if someone has actually
        // defined "enabled" instead of enabledFor, we'll need this.
        // setEnabled(enabled());
        // Okay, do NOT do this -- enabled sometimes use to just ring the bell when an
        // action is attempted that you can't actually do right now -- problem is if we
        // disable the action based on enabled(), it has no way of ever getting turned
        // back on!
    }
    
    public String getUndoName()
    {
    	return null;
    }
    
    public String getUndoName(ActionEvent e, boolean hadException)
    {
    	
    	String undoName = getUndoName();
    	if (undoName == null)
            undoName = e.getActionCommand();
        if (undoName == null)
            undoName = getActionName();
        if (hadException && DEBUG.Enabled)
            undoName += " (!)";
        
        return undoName;
    }
    
    public KeyStroke getKeyStroke() {
        return (KeyStroke) getValue(ACCELERATOR_KEY);
    }
    
    public String getKeyStrokeDescription() {
        final KeyStroke keyStroke = getKeyStroke();
        if (keyStroke != null)
            return tufts.vue.action.ShortcutsAction.getDescription(keyStroke);
        else
            return "Menu item: " + getActionName();
    }

    public void fire(Object source) {
        fire(source, getActionName());
    }

    private static class EventWrap {
        final Object target;
        final EventObject event;
        EventWrap(Object t, EventObject e) {
            target = t;
            event = e;
        }
    }
    
    public void fire(Object source, EventObject event) {
        fire(new EventWrap(source, event), getActionName());
    }

    private void fire(Object source, String name) {
        actionPerformed(new ActionEvent(source, 0, name));
    }

    
    /**
     * Fire this action, but only if the given key event matches our accelerator key.
     * @return true if the action was fired
     */
    public boolean fireIfMatching(Object source, java.awt.event.KeyEvent e) {
        if (KeyStroke.getKeyStrokeForEvent(e).equals(getValue(ACCELERATOR_KEY))) {
            fire(source);
            return true;
        } else
            return false;
    }
    
    /**
     * Fire this action, but only if the given key event matches our accelerator key.
     * The KeyEvent will be used as the source of the action.
     * @return true if the action was fired
     */
    public boolean fireIfMatching(java.awt.event.KeyEvent e) {
        return fireIfMatching(e, e);
    }


    // To update action's enabled state after an action is performed.

    // todo: create a single VueAction LWSelection listener that then
    // dispatches selection updates to all the actions (and then see
    // how many ms all together are taking), and for performance can
    // break down selection once into a selection info object for
    // action's to check against (size, #nodes, #links, etc)
    // also consider just putting all that info directly in
    // the selection.
    
    // This should be static / not run from here: should be
    // run at any "user-mark" (any undo-manager possible checkpoint)

//     protected void updateActionListeners()
//     {
//         Iterator i = VUE.getSelection().getListeners().iterator();
//         while (i.hasNext()) {
//             LWSelection.Listener l = (LWSelection.Listener) i.next();
//             if (l instanceof javax.swing.Action) {
//                 l.selectionChanged(VUE.getSelection());
//                 //System.out.println("Notifying action " + l);
//             }
//             //else System.out.println("Skipping listener " + l);
//         }
//     }

    private static final Collection<VueAction> SelectionWatchers = new java.util.ArrayList();
    
    private static void updateSelectionWatchers(LWSelection s) {
        final LWSelection selection = VUE.getActiveViewer() == null ? null : s;
        for (VueAction a : SelectionWatchers) {
            try {
                a.updateEnabled(selection);
            } catch (Throwable t) {
                Log.error("updateEnabled failed in: " + Util.tags(a) + "; with selection " + selection);
            }
        }
    }

    static {
        VUE.getSelection().addListener(new LWSelection.Listener() {
                public void selectionChanged(LWSelection s) {
                    updateSelectionWatchers(s);
                }
                @Override
                public String toString() {
                    return "Global " + VueAction.class.getSimpleName() + " enabled-state updater";
                }
            });
    }
    

    /** @return false in this impl: override to change */
    protected boolean isSelectionWatcher() { return false; }
    /** does nothing in this impl: override to make use of */
    protected void updateEnabled(LWSelection s) { }

    /** Note that overriding enabled() will not update the action's enabled
     * state based on what's in the selection -- you need to subclass
     * Actions.LWCAction and override enabledFor(LWSelection s) for
     * that -- it gets called whenever the selection changes and will
     * update the actions enabled state based on what it returns.  If
     * you want an action to update it's enabled state based on any
     * other VUE application state, all enabled states are updated
     * after every action is performed, but if you need more than
     * that, the action will need it's own listener for whatever event
     * it's interested in.
     */
    protected boolean enabled() { return VUE.getActiveViewer() != null; }

    /** @return true: must be overriden to be put to use */
    boolean enabledFor(LWSelection s) { return true; }
    
    /** public access enabled checker that also checks master action enabled states */
    public boolean isUserEnabled() {
        if (allIgnored && !overrideIgnoreAllActions())
            return false;
        else
            return enabled();
    }

    public void act() {
        System.err.println("Unhandled VueAction: " + getActionName());
    }

    protected void out(String s) {
        if (DEBUG.Enabled) Log.debug(this + ": " + s);
    }

    protected void info(String s) {
        Log.info(this + " " + s);
    }
    
    public String toString() {
        Class c = getClass();
        return Util.TERM_GREEN
            + (c.isAnonymousClass() ? c.getSuperclass().getSimpleName() : c.getSimpleName())
            + "[" + getActionName() +  "]"
            + Util.TERM_CLEAR;
    }
}

