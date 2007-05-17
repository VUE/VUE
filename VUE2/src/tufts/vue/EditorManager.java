package tufts.vue;

import java.beans.*;
import java.util.*;
import javax.swing.JLabel;

public class EditorManager
    implements LWSelection.Listener,
               LWComponent.Listener,
               PropertyChangeListener
{
    private static final Collection<LWEditor> mEditors = new HashSet<LWEditor>();
    private static final HashMap<LWEditor,JLabel> mLabels = new HashMap();
    private static EditorManager singleton;
    private static boolean EditorLoadingUnderway; // editors are loading values from the selection
    private static boolean PropertySettingUnderway; // editor values are being applied to the selection

    // TODO: need to load default style cache with saved preference values (or do per tool?)
    //private final LWComponent DefaultStyleCache;// = new LWNode("Multi-Selection Style Cache");
    private static LWComponent CurrentStyleType;

    private static final Map<Object,LWComponent> StylesByType = new HashMap();
    private static final Map<Object,LWComponent> ProvisionalStylesByType = new HashMap();
        
    private LWComponent singleSelection;

    private boolean extractedDefaultTypesFromMap = false;

    /**
     * Only LWEditors created and put in the AWT hierarchy before this
     * is called will be recognized.
     */
    public static synchronized void install() {
        if (singleton != null) {
            tufts.Util.printStackTrace("can only have one instance of " + singleton);
        } else {
            singleton = new EditorManager();
            singleton.refresh();
        }
    }

    /** find all LWEditors in the AWT hierarchy and register them */
    public static synchronized void refresh() {
        singleton.findEditors();
    }

    private EditorManager() {
        VUE.getSelection().addListener(this);
        VUE.addActiveListener(LWMap.class, this);
        VUE.addActiveListener(VueTool.class, this);
    }

    public void selectionChanged(LWSelection s) {

        if (s.size() == 1) {
            if (singleSelection != null)
                singleSelection.removeLWCListener(this);
            singleSelection = s.first();
            singleSelection.addLWCListener(this);
            CurrentStyleType = getStyleForType(singleSelection);
        } else {
            //StyleCache = DefaultStyleCache;
            // TODO: it will be easy for the selection to keep a hash of contents based
            // on typeToken, so we can at least know in multi-selection cases if
            // they're all of the same type, and update the right style holder,
            // as opposed to requiring a single selection to update it.
            //CurrentStyleType = null;
            if (singleSelection != null) {
                singleSelection.removeLWCListener(this);
                singleSelection = null;
            }
        }

        loadAllEditors(s);
    }

    public void activeChanged(ActiveEvent e, LWMap map)
    {
        if (map != null && !extractedDefaultTypesFromMap) {
            extractedDefaultTypesFromMap = true;
            extractMostRecentlyUsedStyles(map);
        }
    }

    private void extractMostRecentlyUsedStyles(LWMap map) {
        final Collection<LWComponent> allNodes = map.getAllDescendents(LWMap.ChildKind.ANY);

        Object typeToken;
        LWComponent curStyle;
        for (LWComponent c : allNodes) {
            typeToken = c.getTypeToken();
            curStyle = StylesByType.get(typeToken);
            
            if (curStyle == null) {
                // first type we've seen this type: just load it up
                StylesByType.put(typeToken, c);
            } else {
                if (c.getNumericID() > curStyle.getNumericID()) {
                    // the object of this type is more recent than any we've seen before:
                    // stash away a reference to it.
                    StylesByType.put(typeToken, c);
                }
            }
        }

        // now that we've found all the most recent objects of each type,
        // we need get the direct references to them OUT of the style type cache,
        // and replace them as duplicates, so that can serve as standalone
        // style holders.

        for (Map.Entry<Object,LWComponent> e : StylesByType.entrySet())
            e.setValue(makeIntoStyle(e.getValue(), e.getKey()));
    }
    
    public void activeChanged(ActiveEvent e, VueTool tool)
    {
        if (DEBUG.TOOL) out("activeChanged: " + e);
        if (tool == null || VUE.getSelection().size() > 0)
            return;

        final Object typeToken = tool.getSelectionType();

        if (typeToken == null)
            return;

        final LWComponent oldStyle = CurrentStyleType;
        
        CurrentStyleType = StylesByType.get(typeToken);

        if (CurrentStyleType != oldStyle && CurrentStyleType != null)
            loadAllEditorValues(CurrentStyleType);

    }

    /** If the single object in the selection has a property change that was NOT due to an editor,
     * (e.g., a menu) we detect this here, and re-load the editors as needed.
     */
    public void LWCChanged(LWCEvent e) {
        if (EditorLoadingUnderway || PropertySettingUnderway)
            ; // ignore
        //             else if (e.getKey() != null && e.getKey().type == LWComponent.KeyType.STYLE) {
        //                 // above assumes LWEditors only handle style types...  
        else if (e.getKey() != null) { // only listen for real Key's
            loadAllEditors(VUE.getSelection());
        }
        // TODO performance:
        // really, we only need to load the one editor for the key in LWCEvent
        // Doing this ways will constantly load all the editors, even tho
        // they don't need it.  (We could make a hash of all the keys
        // the editors listen for, and check that here).
    }
        

    public void propertyChange(PropertyChangeEvent e) {
        if (!EditorLoadingUnderway && e instanceof LWPropertyChangeEvent) {
            if (DEBUG.TOOL) out("propertyChange: " + e);
            ApplyPropertyChangeToSelection(VUE.getSelection(), ((LWPropertyChangeEvent)e).key, e.getNewValue(), e.getSource());
        }
    }
    private void loadAllEditorValues(LWComponent style) {
        loadAllEditors(new LWSelection(style), false);
    }

    private void loadAllEditors(LWSelection selection) {
        loadAllEditors(selection, true);
    }
        
    private void loadAllEditors(LWSelection selection, boolean setEnabledStates)
    {
        //final LWComponent propertySource = selection.only(); // will be null if selection size > 1
        final LWComponent propertySource;

        if (selection.size() == 1)
            propertySource = selection.first();
        else
            propertySource = null;
        //propertySource = DefaultStyleCache;
        
        if (DEBUG.TOOL||DEBUG.STYLE) out("loadAllEditors from: " + propertySource
                                         + "; currentTypedStyle: " + CurrentStyleType
                                         + " updateEnabled=" + setEnabledStates
                                         + " " + selection);

        // While the editors are loading, we want to ignore any change events that
        // loading may produce in the editors (otherwise, we'd then set the selected
        // component properties, end end up risking recursion, even tho properties
        // shouldn't be triggering events if their value hasn't actually changed)
            
        EditorLoadingUnderway = true;
        try {
            for (LWEditor editor : mEditors) {
                try {
                    setEditorState(editor, selection, propertySource, setEnabledStates);
                } catch (Throwable t) {
                    tufts.Util.printStackTrace(t, this + ": general failure processing LWEditor: " + editor);
                }
            }
        } finally {
            EditorLoadingUnderway = false;
        }
    }
    
    private void setEditorState(LWEditor editor, LWSelection selection, LWComponent propertySource, boolean setEnabledState) {
        final boolean supported;
            
        if (selection.isEmpty()) {
            supported = true;
        } else {
            supported = selection.hasEditableProperty(editor.getPropertyKey());
        }
        if (DEBUG.TOOL) out("SET-ENABLED " + (supported?"YES":" NO") + ": " + editor);
            
        if (setEnabledState) {
            try {
                editor.setEnabled(supported);
            } catch (Throwable t) {
                tufts.Util.printStackTrace(t, this + ": LWEditor.setEnabled failed on: " + editor);
            }
            
            if (mLabels.containsKey(editor))
                mLabels.get(editor).setEnabled(supported);
        }
            
        //if (supported && propertySource != null && propertySource.supportsProperty(editor.getPropertyKey()))
        if (supported && propertySource != null)
            loadEditorValue(propertySource, editor);

        if (CurrentStyleType != null)
            ApplyPropertyValue("<editor:typeSync>", editor.getPropertyKey(), editor.produceValue(), CurrentStyleType);
            
        //if (editor instanceof Component) ((Component)editor).repaint(); // not helping ShapeIcon's repaint when disabled...
    }
        
    private void loadEditorValue(LWComponent source, LWEditor editor) {
        if (DEBUG.TOOL&&DEBUG.META) out("loadEditor: " + editor + " loading " + editor.getPropertyKey() + " from " + source);

        final Object key = editor.getPropertyKey();
        final Object value = source.getPropertyValue(key);
        //             if (source.supportsProperty(key))
        //                 value = source.getPropertyValue(key);
        //             else
        //                 value = null;
        //if (value != null) {
        if (DEBUG.TOOL) out("     loadEditor: " + editor + " <- value[" + value + "]");
        try {
            editor.displayValue(value);
        } catch (Throwable t) {
            tufts.Util.printStackTrace(t, this + ": LWEditor.displayValue failed on: " + editor + " with value [" + value + "]");
        }
        //} else if (DEBUG.TOOL) out("\tloadEditor: " + source + " -> " + editor + " skipped; null value for " + key);
    }

    public static void firePropertyChange(LWEditor editor, Object source) {
        ApplyPropertyChangeToSelection(VUE.getSelection(), editor.getPropertyKey(), editor.produceValue(), source);
    }


    /** Will either modifiy the active selection, or if it's empty, modify the default state (creation state) for this tool panel */
    public static void ApplyPropertyChangeToSelection(final LWSelection selection, final Object key, final Object newValue, Object source)
    {
        if (EditorLoadingUnderway) {
            if (DEBUG.TOOL) System.out.println("ApplyPropertyChangeToSelection: " + key + " " + newValue + " (skipping)");
            return;
        }
        
        if (DEBUG.TOOL||DEBUG.STYLE) System.out.println("ApplyPropertyChangeToSelection: " + key + " " + newValue);
        
        if (!selection.isEmpty()) {
            // As setting these properties in the model will trigger notify events from the selected objects
            // back up to the tools, we want to ignore those events while this is underway -- the tools
            // already have their state set to this.
            PropertySettingUnderway = true;
            try {
                for (tufts.vue.LWComponent c : selection)
                    ApplyPropertyValue(source, key, newValue, c);
            } finally {
                PropertySettingUnderway = false;
            }
                
            if (CurrentStyleType != null)
                ApplyPropertyValue("<apply:typeSync>", key, newValue, CurrentStyleType);
            
            if (VUE.getUndoManager() != null)
                VUE.getUndoManager().markChangesAsUndo(key.toString());
        }
    }

    public static void applyCurrentProperties(LWComponent c) {
        LWComponent styleForType = fetchStyleForType(c);
        if (styleForType != null) {
            //if (DEBUG.STYLE) out("COPY STYLE of " + styleForType + " -> " + c);
            c.copyStyle(styleForType);
        }
    }
        
        
    private static void ApplyPropertyValue(Object source, Object key, Object newValue, LWComponent target) {
        //if (DEBUG.STYLE) System.out.println("APPLY " + source + " " + key + "[" + newValue + "] -> " + target);
        if (target.supportsProperty(key)) {
            if (DEBUG.STYLE) System.out.format(singleton + ": APPLY %s %-15s %-40s -> %s\n", source, key, "(" + newValue + ")", target);
            try {
                target.setProperty(key, newValue);
                //} catch (LWComponent.PropertyValueVeto ex) {
                //tufts.Util.printStackTrace(ex);
            } catch (Throwable t) {
                tufts.Util.printStackTrace(t, source + " failed to set property " + key + "; value=" + newValue + " on " + target);
            }
        }
    }

    private final LWComponent.CopyContext DUPE_WITHOUT_CHILDREN = new LWComponent.CopyContext(false);

    private synchronized LWComponent getStyleForType(LWComponent c) {
        final Object token = c.getTypeToken();
        LWComponent styleHolder = token == null ? null : StylesByType.get(token);
        if (styleHolder == null && token != null) {
            styleHolder = makeIntoStyle(c, token);
            StylesByType.put(token, styleHolder);
        }
        if (DEBUG.STYLE) out("got styleHolder for type token (" + token + "): " + styleHolder); 
        return styleHolder;
    }

    private LWComponent makeIntoStyle(LWComponent styleSource, Object typeToken)
    {
        if (DEBUG.STYLE || DEBUG.WORK) out("creating style holder based on " + styleSource + " for type (" + typeToken + ")");

        // As any LWComponent can be used as a style source,
        // we can just dupe whatever we've got (a handy
        // way to instance another component with the same typeToken)
        final LWComponent style = styleSource.duplicate(DUPE_WITHOUT_CHILDREN);

        //-----------------------------------------------------------------------------
        // for clear debugging info only:
        style.enableProperty(LWKey.Label); // in case it's override getLabel like LWSlide
        // note: property bits are duplicated, so if we ever change from a model of
        // just copying the style from the style to using it as a base for
        // duplication, we have to leave the original bits in place.
        style.setLabel("<style:" + typeToken + ">"); 
        style.setResource((Resource)null); // clear out any resource if it had it
        style.setNotes(null);
        style.takeLocation(0,0);
        style.takeSize(100,100);
        //-----------------------------------------------------------------------------

        //out("created new styleHolder for type token [" + token + "]: " + styleHolder); 
        //out("created " + styleHolder);

        return style;
    }
        
    /** @return the current style for type type of the given component only if we already have one
     * -- do not auto-create a new style for the type if we don't already have one */
    private static LWComponent fetchStyleForType(LWComponent c) {
        if (StylesByType == null) {
            tufts.Util.printStackTrace("circular static initializer dependency");
            return null;
        }
        return c == null ? null : StylesByType.get(c.getTypeToken());
    }
    
//     public static Object GetPropertyValue(LWComponent.Key key) {
//         for (LWEditor editor : mEditors) {
//             if (editor.getPropertyKey() == key)
//                 return editor.produceValue();
//         }
//         return null;
//     }


/*
    public static void ApplyProperties(LWComponent c) {
        ApplyProperties(c, ~0L);
    }
*/

    /*
     * Apply the current value of all selected tools that are applicable to the given component.
     * E.g., used for setting the properties of newly created objects.
     * @param keyBits -- only apply keys whose bit is represented in keyBits (@see LWComonent.Key.bit)
     *
    public static void ApplyProperties(LWComponent c, long keyBits) {
        for (LWEditor editor : mEditors) {
            final Object k = editor.getPropertyKey();
            final LWComponent.Key key;
            if (k instanceof LWComponent.Key)
                key = (LWComponent.Key) k;
            else {
                key = null;
                out("ApplyProperties: skipping non proper key: " + k.getClass().getName() + "[" + k + "]");
                continue;
            }

            if (c.supportsProperty(key) && (key.bit & keyBits) != 0)
                c.setProperty(key, editor.produceValue());
        }
    }
    */


    static void registerEditor(LWEditor editor) {
        if (mEditors.add(editor)) {
            if (DEBUG.TOOL || DEBUG.INIT) System.out.println("REGISTERED EDITOR: " + editor);
            if (editor instanceof java.awt.Component)
                ((java.awt.Component)editor).addPropertyChangeListener(singleton);
        } else
            System.out.println(" REGISTERED AGAIN: " + editor);
    }

    private void findEditors() {
        new EventRaiser<LWEditor>(this, LWEditor.class) {
            public void dispatch(LWEditor editor) {
                registerEditor(editor);
            }
        }.raise();
        new EventRaiser<JLabel>(this, JLabel.class) {
            public void dispatch(JLabel label) {
                java.awt.Component gui = label.getLabelFor();
                if (gui != null && gui instanceof LWEditor)
                    mLabels.put((LWEditor)gui, label);
            }
        }.raise();
        VUE.Log.debug(this + " now managing " + mEditors.size() + " LWEditors.");
    }

    static void out(Object o) {
        System.out.println("EditorManager: " + o);
    }
        

    public String toString() {
        return "EditorManager";
    }
        
        
}




    /*

     * So, can we meaningfully support the workflow of having nothing selected,
     * changing the tool states, then creating a new object of ANY arbitrary type?

     * Holy crap -- I think there's a way to really "do what I mean" with this, but it's
     * freakin hairy.  So every type has it's own style stored which can be used to
     * style newly created objects of that type, as well as become the default editor
     * states if a VueTool that is tied to that object type is enabled (and something
     * isn't selected -- that's an interesting case -- lets say a link is selected and
     * you load the node tool -- we probably want the selection to have priority in the
     * editor states, and just ignore toe VueTool in this case -- tho actually, hell, it
     * would be MUCH cleaner to just de-select what's selected when you load a VueTool
     * if it doesn't happen to match the selection type of the VueTool).

     * So anyway, what happens to the editor states if NOTHING is selected?  We could
     * use the current VueTool type if there is one, or more accurately, leave it with
     * the last type that was selected.  Tho, say, if the node tool was selected, and we
     * leave that in place, and they change the stroke color, then they create a link,
     * the link will have the properties of the last time we had the link style state,
     * not the current state.  So, to deal with this, if NOTHING is selected (and all
     * editors get enabled), we could apply any editor state changes to ALL the current
     * typed styles, which is the only way we could capture all the changes anyway, as
     * there's no such object in the system that supports every property (e.g., both
     * link shape and a node shape).
     
     * The advantage here is that no matter what change they make, they know it
     * will apply to the creation of any future object.  The only drawback
     * is that they may not know they're changing the state for ALL future
     * objects -- e.g., they change the fill to create a node, but hell,
     * now new text objects are also going to be created with that same
     * fill...

     * So, we could get crazy and keep TWO versions of the style for every type, and
     * when nothing is selected, update every one of these second "provisional" styles
     * for each type, and then the next object that's created, we know that's what they
     * wanted, and then the provisional style for that type is copied over the in-use
     * style for that type, and the provisional styles for all other types are thrown
     * out (well, actually, just ignored -- they'll always be updating when nothing is
     * selected, it's just that whenever a new object is created when nothing
     * is selected and every editor is active, the style type of the object created
     * gets updated with it's provisional style).

     * Now, this sounds complicated enough that it might actually completely
     * break some other workflow...

     * So now lets say we select the node tool: currently, the tools still allow you to
     * single select objects of any type (the only limit the drag selection) -- would
     * could change that for simplicity.  So lets say node tool is selected -- we want
     * to load the node style... crap -- whan happens if they load the node style, but
     * then forget the node tool is active, change the stroke style, and create a new
     * link, thinking it will affect the link?  Ahah -- taken care of: if a node
     * was actually selected, we'll have the node style, and if they change
     * a property they immediately see the change -- it's not for future use.
     * But if nothing is selected, it's for future use -- however, when the
     * node tool loads it could still UPDATE the provisional style with
     * it's typed style!

     * So, the rules this establishes are:

     * (1) Whatever is selected always determines the typed style loaded into
     * the editors.

     * (2) When nothing is selected, we're ALWAYS working with the provisional style,
     * even if a tool with a type is selected -- so changing the active tool is going to
     * have NO EFFECT on the editor states, because even if the node tool is active,
     * they could still create a link, and if they change the provisional style, we want
     * that to effect the created link, not the next created node.  If they have the
     * node tool selected, then change an editor state (e.g., line color: red), then
     * create a link, the provisional style updates the link style, the link gets
     * selected, the link style is loaded.  Then they click using the node tool to
     * create a node: the selection is cleared, the provisional style is returned to the
     * editor states (everything is enabled).  Does the node get the red border
     * color? If the prov style was "given" to the the link, then no, but then
     * we just created a new object that didn't match the editor state.
     * Maybe this would be easier if the active tool only allowed the editor
     * states for it's type, even when nothing is selected.  This at least
     * provides some clarity, but then a new object (the link) could be created
     * not matching the tool state...

     * More complexity: if we want to load the typed style with the tool, which nicely
     * communicates the avail properties for that type, yet it has properties that
     * overlap with other tools, the typed style for the current tool could itself serve
     * as yet another provisional style for the other styles: so when red line was
     * selected int he node style, then a link is created, the link style
     * could copy over the node style, but we'd ONLY want the "red" property,
     * the one that changed, not every other property!  Can we know that this
     * happened?  What if EVERY SINGLE EDITOR STATE CHANGE updated ALL the
     * provisinal styles?  This will work fine as long as the provisionals
     * are tossed: e.g., you set editor states perfect for your next node,
     * and created it: the link provisional style should be reset to
     * the link style: it only gets used if they actually create a link.

     * So, even if something is selected, the provisional styles are updated.
     * But it's CRUCIAL that they get cleared when a new object is created,
     * because if you just set everything up for a node, then create one,
     * you don't want all that applying to youer next link, right?

     * So all of this also means that all new on-map user object creation will have to
     * go through the editor manager, tho actually the generic applyCurrentProperties is
     * what does this, tho it would benefit from a more powerful name to suggest what
     * we're doing: e.g., applyAppropriateProperties / applyDesiredStyle /
     * popAndApplyEditorStates, something like that.  Oh, that may not be enough tho
     * because it's crucial that this is called so we know what to do with the
     * provisional styles, which will cause mass confusion if we ever create an object
     * without telling the editor manager that this is where the provisional style went.

     * WHEN THE ACTIVE TOOL CHANGES, AND NOTHING IS SELECTED: We still leave everything
     * enabled, as they might still want to style and create anything, however, we CAN
     * load the editor display VALUES with the type for the tool if there is one, so
     * they can at least see what they're working with.  HOWEVER, if there were
     * provisional properties available (editor states changed but not "used up"),
     * we should resolve the provisional changes to the type of that editor
     * if it has one, so that, say if going from the node tool to the link
     * tool, especially the temporary link tool, the provisional property
     * will be be resolved to the link style, for the link you're about to
     * create.

     
     
     
     
    
     
     *
     
     */
