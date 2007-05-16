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
    private static LWComponent CurrentTypedStyle;

    private static final Map<Object,LWComponent> TypedStyleCache = new HashMap();
        
    private LWComponent singleSelection;

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
        //             DefaultStyleCache = new LWComponent() {{
        //                         setLabel("<disabled styled>");
        //                         disablePropertyBits(~0L);// disable all properties

        //                     }};
        //= new LWNode("Multi-Selection Style Cache");
            
        //VUE.addActiveListener(VueTool.class, this);
        //StyleCache = DefaultStyleCache;
    }

    //         public void activeChanged(ActiveEvent e, VueTool tool) {
    //             StyleCache = tool.getStyleCache();
    //             if (StyleCache == null)
    //                 StyleCache = DefaultStyleCache;
            
    //             // either need to know this is first time, so can load cache
    //             // with current tool values, or we should expect it should
    //             // already come with the desired default values
    //             loadAllEditors(new LWSelection(StyleCache));
    //         }


    public void propertyChange(PropertyChangeEvent e) {
        if (!EditorLoadingUnderway && e instanceof LWPropertyChangeEvent) {
            if (DEBUG.TOOL) out("propertyChange: " + e);
            ApplyPropertyChangeToSelection(VUE.getSelection(), ((LWPropertyChangeEvent)e).key, e.getNewValue(), e.getSource());
        }
    }

    public void selectionChanged(LWSelection s) {

        if (s.size() == 1) {
            if (singleSelection != null)
                singleSelection.removeLWCListener(this);
            singleSelection = s.first();
            singleSelection.addLWCListener(this);
            CurrentTypedStyle = getStyleCache(singleSelection);
        } else {
            //StyleCache = DefaultStyleCache;
            // TODO: it will be easy for the selection to keep a hash of contents based
            // on typeToken, so we can at least know in multi-selection cases if
            // they're all of the same type, and update the right style holder,
            // as opposed to requiring a single selection to update it.
            CurrentTypedStyle = null;
            if (singleSelection != null) {
                singleSelection.removeLWCListener(this);
                singleSelection = null;
            }
        }

        loadAllEditors(s);
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
        
        

    private final LWComponent.CopyContext DUPE_WITHOUT_CHILDREN = new LWComponent.CopyContext(false);

    private synchronized LWComponent getStyleCache(LWComponent c) {
        final Object token = c.getTypeToken();
        LWComponent styleHolder = token == null ? null : TypedStyleCache.get(token);
        if (styleHolder == null && token != null) {
            if (DEBUG.STYLE) out("creating style holder based on " + c);

            // As any LWComponent can be used as a style source,
            // we can just dupe whatever we've got (a handy
            // way to instance another component with the same typeToken)
            styleHolder = c.duplicate(DUPE_WITHOUT_CHILDREN);
                
            //-----------------------------------------------------------------------------
            // for clear debugging info only:
            styleHolder.enableProperty(LWKey.Label); // in case it's override getLabel like LWSlide
            // note: property bits are duplicated, so if we ever change from a model of
            // just copying the style from the styleHolder to using it as a base for
            // duplication, we have to leave the original bits in place.
            styleHolder.setLabel("<style:" + token + ">"); 
            styleHolder.setResource((Resource)null); // clear out any resource if it had it
            styleHolder.setNotes(null);
            styleHolder.takeLocation(0,0);
            styleHolder.takeSize(100,100);
            //-----------------------------------------------------------------------------
                
                
            //out("created new styleHolder for type token [" + token + "]: " + styleHolder); 
            //out("created " + styleHolder);
            TypedStyleCache.put(token, styleHolder);
        }
        if (DEBUG.STYLE) out("got styleHolder for type token (" + token + "): " + styleHolder); 
        return styleHolder;
    }
        
    private static LWComponent fetchStyleCache(LWComponent c) {
        if (TypedStyleCache == null) {
            tufts.Util.printStackTrace("circular static initializer dependency");
            return null;
        }
        return c == null ? null : TypedStyleCache.get(c.getTypeToken());
    }
        

    private void loadAllEditors(LWSelection selection)
    {
        //final LWComponent propertySource = selection.only(); // will be null if selection size > 1
        final LWComponent propertySource;

        if (selection.size() == 1)
            propertySource = selection.first();
        else
            propertySource = null;
        //propertySource = DefaultStyleCache;
        
        if (DEBUG.TOOL||DEBUG.STYLE) out("\nloadAllEditors " + propertySource);

        // While the editors are loading, we want to ignore any change events that
        // loading may produce in the editors (otherwise, we'd then set the selected
        // component properties, end end up risking recursion, even tho properties
        // shouldn't be triggering events if their value hasn't actually changed)
            
        EditorLoadingUnderway = true;
        try {
            for (LWEditor editor : mEditors) {
                try {
                    setEditorState(editor, selection, propertySource);
                } catch (Throwable t) {
                    tufts.Util.printStackTrace(t, this + ": general failure processing LWEditor: " + editor);
                }
            }
        } finally {
            EditorLoadingUnderway = false;
        }
    }

    private void setEditorState(LWEditor editor, LWSelection selection, LWComponent propertySource) {
        final boolean supported;
            
        if (selection.isEmpty()) {
            supported = false;
        } else {
            supported = selection.hasEditableProperty(editor.getPropertyKey());
        }
        if (DEBUG.TOOL) out("SET-ENABLED " + (supported?"YES":" NO") + ": " + editor);
            
        try {
            editor.setEnabled(supported);
        } catch (Throwable t) {
            tufts.Util.printStackTrace(t, this + ": LWEditor.setEnabled failed on: " + editor);
        }
            
        if (mLabels.containsKey(editor))
            mLabels.get(editor).setEnabled(supported);
            
        //if (supported && propertySource != null && propertySource.supportsProperty(editor.getPropertyKey()))
        if (supported && propertySource != null)
            loadEditorValue(propertySource, editor);

        if (CurrentTypedStyle != null)
            ApplyPropertyValue("<editor:typeSync>", editor.getPropertyKey(), editor.produceValue(), CurrentTypedStyle);
            
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
                
            if (CurrentTypedStyle != null)
                ApplyPropertyValue("<apply:typeSync>", key, newValue, CurrentTypedStyle);
            
            if (VUE.getUndoManager() != null)
                VUE.getUndoManager().markChangesAsUndo(key.toString());
        }
    }

    public static void applyCurrentProperties(LWComponent c) {
        LWComponent styleForType = fetchStyleCache(c);
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
