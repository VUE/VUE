/*
 * Copyright 2003-2007 Tufts University  Licensed under the
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

import static tufts.vue.LWComponent.*;

import tufts.Util;
import java.beans.*;
import java.util.*;
import javax.swing.JLabel;

public class EditorManager
    implements LWSelection.Listener,
               LWComponent.Listener,
               PropertyChangeListener
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(EditorManager.class);
    
    private static final Collection<LWEditor> mEditors = new HashSet<LWEditor>();
    private static final Map<LWEditor,JLabel> mLabels = new HashMap();
    private static EditorManager singleton;
    private static boolean EditorLoadingUnderway; // editors are loading values from the selection
    private static boolean PropertySettingUnderway; // editor values are being applied to the selection
    
    /** Property bits for any editor state changes in the "free" state (nothing is
     * selected, all editors are enabled -- property changes are not being
     * applied directly to any LWComponents in a selection).
     */
    private static long FreePropertyBits;
    // now that we're tracking these, we may be able to do away with the provisionals:
    // resolution becomes asking the editors to produce the value for the recorded
    // free bits, and applying those to the typed style (ignoring/tossing free bits
    // not supported on the target style).

    private static class StyleType {
        final Object token;
        final LWComponent style;
        final LWComponent provisional;

        StyleType(Object t, LWComponent s, LWComponent p) {
            token = t;
            style = s;
            provisional = p;
        }

        void resolveToProvisional(long freeBits) {
            if (DEBUG.STYLE && (DEBUG.META /*|| DEBUG.WORK*/)) tufts.Util.printStackTrace(this + " RESOLVING TO " + provisional);
            
            if (token == LWNode.TYPE_TEXT || token == LWText.TYPE_RICHTEXT) {

                // special case for "text" nodes:
                // don't assume shape or fill was pre-ordained for the text object
                // -- these can only be set after the object is created.  (And as it
                // stands at the moment changing the fill color to will actually
                // change it's type from "textNode" back to LWNode.class).
                
                if (freeBits != 0) {
                    freeBits &= ~ (LWKey.FillColor.bit | LWKey.Shape.bit);
                    style.copyProperties(provisional, freeBits);
                } else {
                    style.copyStyle(provisional, ~ (LWKey.FillColor.bit | LWKey.Shape.bit) );
                }

            } else {
                if (freeBits != 0)
                    style.copyProperties(provisional, freeBits);
                else
                    style.copyStyle(provisional);
            }
        }

        /** @return the style ready for use to be applied to something -- does sanity checking
         * on the style before returning it.
         */
        LWComponent produceStyle()
        {
            if (token == LWNode.TYPE_TEXT || token == LWText.TYPE_RICHTEXT) {
                // special case for "text" nodes:

                Object shape;
                
                if (style.isTransparent() &&
                    style.getStrokeWidth() <= 0 &&
                    (shape = style.getPropertyValue(LWKey.Shape)) != java.awt.geom.Rectangle2D.Float.class)
                    {
                        // It also must HAVE a shape property (e.g., LWText does not)
                        if (shape != null) {
                            // If completely transparent, assume no point in having a shape that's non-rectangular
                            style.setProperty(LWKey.Shape, java.awt.geom.Rectangle2D.Float.class);
                            
                            // just in case, make sure synced with provisional:
                            provisional.setProperty(LWKey.Shape, java.awt.geom.Rectangle2D.Float.class);
                        }
                    }
            }
            
            return style;
        }

        void takeProperty(String source, Object key, Object newValue) {
            // if current token is LWNode.class, and we're going transparent,
            // that will switch us to "textNode", so we may not want to update
            // in that case -- no easy way around this if you can convert back
            // and forth between textNode and regular node by switching the fill color
            applyPropertyValue("<" + source + ":typeSync>", key, newValue, style);
            applyPropertyValue("<" + source + ":provSync>", key, newValue, provisional);
        }
        

        void discardProvisional() {
            provisional.copyStyle(style);
        }

        public String toString() {
            return style.toString();
        }
    }

    private static StyleType CurrentStyle;
    private static StyleType CurrentToolStyle;

    private static final Map<Object,StyleType> StylesByType = new HashMap();
    //private static final Map<Object,LWComponent> ProvisionalStylesByType = new HashMap();
        
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
        VUE.addActiveListener(LWMap.class, this);
        VUE.addActiveListener(VueTool.class, this);
    }

    public void selectionChanged(LWSelection s) {

        if (DEBUG.STYLE) out("selectionChanged: " + s);

        // todo: if selection goes from 1 to 0, and we have an active
        // tool, we might want to revert the editor states to the
        // provisional for the active tool, so you can again see
        // the props for what you'd be about to create.

        if (s.size() == 1) {
            
            if (singleSelection != null)
                singleSelection.removeLWCListener(this);

            singleSelection = s.first();
            
//             // This seems a bit agressive:
//             if (FreePropertyBits != 0) {
//                 // if we select something with unapplied changes (free properties),
//                 // apply them to the selection.  We should be moving from a nothing
//                 // selected to a new selection state, as we should only ever have
//                 // unapplied changes if there wasn't a selection to apply them to.
//                 resolveToProvisionalStyle(singleSelection, FreePropertyBits);
//                 applyCurrentProperties(singleSelection, FreePropertyBits);
//             }

            // add the listener after auto-applying any free style info
            singleSelection.addLWCListener(this);
            
            CurrentStyle = getStyleForType(singleSelection);
        } else {
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

    public void activeChanged(ActiveEvent e, VueTool tool)
    {
        
        //out("ACTIVE TOOL temp=" + tool.isTemporary() + " " + tool);
        if (tool == null || VUE.getSelection().size() > 0 || tool.isTemporary()) {
            if (tool == null)
                CurrentToolStyle = null;
            return;
        }

        CurrentToolStyle = null;
        
        final Object typeToken = tool.getSelectionType();

        if (typeToken == null)
            return;

        if (FreePropertyBits != 0) {
            // if we switch to a new tool, and there were editor
            // changes that had been unused, target them for the new
            // tool type if there is one.
            resolveToProvisionalStyle(typeToken, FreePropertyBits);
        }


        final StyleType oldStyle = CurrentStyle;
        
        CurrentStyle = CurrentToolStyle = StylesByType.get(typeToken);

        if (CurrentStyle != null && CurrentStyle != oldStyle)
            loadAllEditorValues(CurrentStyle.style);

    }
    
    private boolean extractedDefaultTypesFromMap = false;
    public void activeChanged(ActiveEvent e, LWMap map)
    {
        if (map != null && map.hasChildren() && !extractedDefaultTypesFromMap) {
            // performance: might be alot to do every time we switch the active map...
            extractedDefaultTypesFromMap = true;
            extractMostRecentlyUsedStyles(map);
            // Update the active style types and tool states with any newly
            // extracted default styles:
            if (CurrentStyle != null)
                CurrentStyle = StylesByType.get(CurrentStyle.token);
            if (CurrentToolStyle != null)
                CurrentToolStyle = StylesByType.get(CurrentToolStyle.token);
            if (CurrentStyle != null)
                loadAllEditorValues(CurrentStyle.style);            

        }
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
            try {
                applySinglePropertyChange(VUE.getSelection(), ((LWPropertyChangeEvent)e).key, e.getNewValue(), e.getSource());
            } catch (Throwable t) {
                tufts.Util.printStackTrace(t, this + ": failed to handle property change event: " + e);
            }
        }
    }
    
    private void loadAllEditorValues(LWComponent style) {
        loadAllEditors(new LWSelection(style), style, false);
    }

    private void loadAllEditors(LWSelection selection) {
        loadAllEditors(selection, selection.only(), true);
    }
        
    // TODO: can get rid of passing in selection: only need editable property bits
    private void loadAllEditors(LWSelection selection, LWComponent propertyValueSource, boolean setEnabledStates)
    {
        if (DEBUG.TOOL||DEBUG.STYLE) {
            String msg = "loadAllEditors from: " + propertyValueSource
                + "; currentTypedStyle: " + CurrentStyle
                + " updateEnabled=" + setEnabledStates
                + " " + selection;
            if (DEBUG.META/*||DEBUG.WORK*/)
                tufts.Util.printStackTrace(msg);
            else
                out(msg);
        }

        // While the editors are loading, we want to ignore any change events that
        // loading may produce in the editors (otherwise, we'd then set the selected
        // component properties, end end up risking recursion, even tho properties
        // shouldn't be triggering events if their value hasn't actually changed)
            
        EditorLoadingUnderway = true;
        try {
            for (LWEditor editor : mEditors) {
                try {
                    setEditorState(editor, selection, propertyValueSource, setEnabledStates);
                } catch (Throwable t) {
                    tufts.Util.printStackTrace(t, this + ": general failure processing LWEditor: " + editor);
                }
            }
        } finally {
            EditorLoadingUnderway = false;
        }
    }

    private static String dumpEditor(LWEditor editor) {
        String msg = tufts.vue.gui.GUI.name(editor);

        if (DEBUG.TOOL) {
            msg += ";\n\tObject: " + editor;
            if (editor instanceof java.awt.Component) {
                java.awt.Component parent = ((java.awt.Component)editor).getParent();
                msg += "\n\tAWT parent: " + tufts.vue.gui.GUI.name(parent) + "; " + parent;
            }
        }
        return msg;
    }
    
    private void setEditorState(LWEditor editor, LWSelection selection, LWComponent propertySource, boolean setEnabledState) {
        final boolean supported;
            
        if (selection.isEmpty()) {
            supported = true;
        } else {
            supported = selection.hasEditableProperty(editor.getPropertyKey());
            //supported = (supportedPropertyBits & editor.getPropertyKey().bit) != 0;
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

        final Object key = editor.getPropertyKey();

        if (key == null) {
            //Log.debug("editor reports null property key: " + dumpEditor(editor));
            return;
        }

        
        //final Object value = propertySource.getPropertyValue(key);
            
        //if (DEBUG.TOOL&&DEBUG.META) out("loadEditor: " + editor + " loading " + editor.getPropertyKey() + " from " + source);

        //if (supported && propertySource != null && propertySource.supportsProperty(editor.getPropertyKey()))
        if (supported && propertySource != null)
            loadEditorWithValue(editor, propertySource.getPropertyValue(key));

        // TODO: a bit overkill to do this no matter what -- do we need to if no property source?
        if (propertySource != null)
            recordPropertyChangeInStyles("loadEditor", editor.getPropertyKey(), editor.produceValue(), selection.isEmpty());
        
        //if (editor instanceof Component) ((Component)editor).repaint(); // not helping ShapeIcon's repaint when disabled...
    }
        
    private void loadEditorWithValue(LWEditor editor, Object value) {
        if (DEBUG.TOOL) out("     loadEditor: " + editor + " <- value[" + value + "]");
        try {
            editor.displayValue(value);
        } catch (Throwable t) {
            tufts.Util.printStackTrace(t, this + ": LWEditor.displayValue failed on: " + editor + " with value [" + value + "]");
        }
        //} else if (DEBUG.TOOL) out("\tloadEditor: " + source + " -> " + editor + " skipped; null value for " + key);
    }

    public static void firePropertyChange(LWEditor editor, Object source) {
        try {
            applySinglePropertyChange(VUE.getSelection(), editor.getPropertyKey(), editor.produceValue(), source);
        } catch (Throwable t) {
            tufts.Util.printStackTrace(t, "failed to fire property for LWEditor " + editor + " for source " + source);
        }
    }


    /** Will either modifiy the active selection, or if it's empty, modify the default state (creation state) for this tool panel */
    private static void applySinglePropertyChange(final Collection<LWComponent> components, final Object key, final Object newValue, Object source)
    {
        if (EditorLoadingUnderway) {
            if (DEBUG.TOOL) out("applySinglePropertyChange: " + key + " " + newValue + " (skipping)");
            return;
        }
        
        if (DEBUG.TOOL||DEBUG.STYLE) out("applySinglePropertyChange: " + key + " " + newValue);
        
        if (!components.isEmpty()) {
            consumeFreeProperties();
            // As setting these properties in the model will trigger notify events from the selected objects
            // back up to the tools, we want to ignore those events while this is underway -- the tools
            // already have their state set to this.
            PropertySettingUnderway = true;
            try {
                for (tufts.vue.LWComponent c : components)
                    applyPropertyValue(source, key, newValue, c);
            } finally {
                PropertySettingUnderway = false;
            }
                
            if (VUE.getUndoManager() != null)
                VUE.getUndoManager().markChangesAsUndo(key.toString());

            recordPropertyChangeInStyles("apply", key, newValue, false);
        } else {
            recordPropertyChangeInStyles("apply", key, newValue, true);
        }
    }

    private static void declareFreeProperty(Object k) {
        if (k instanceof Key) {
            final Key key = (Key) k;
            
            // Note that if any LWEditors are created that are handling non KeyType.STYLE properties,
            // we could get some odd effects (e.g., we wouldn't want label or notes being
            // tagged as having been free property bit, then applying it to newly created objects!)

            if (key.type != KeyType.STYLE && key.type != KeyType.SUB_STYLE) {
                tufts.Util.printStackTrace("Warning: free property of non-style type being ignored: " + key + "; type=" + key.type);
                // we can safely ignore this, but I'm dumping a stack trace for now so we know if we get into this situation
                return;
            }
            
            FreePropertyBits |= key.bit;
            
            if (DEBUG.STYLE) out("declaring free (unused) property change: " + key + "; type=" + key.type);
        }
    }

    private static void consumeFreeProperties() {
        if (DEBUG.STYLE) out("consuming free property bits: " + Long.bitCount(FreePropertyBits));
        FreePropertyBits = 0;
    }
        

    private static void recordPropertyChangeInStyles(String source, Object key, Object newValue, boolean provisionals) {
        // provisionals should be true when there is no selection, and the tools are all enabled in their "free" state
        if (provisionals) {
            declareFreeProperty(key);
            for (StyleType styleType : StylesByType.values()) {
                applyPropertyValue("<" + source + ":provSync>", key, newValue, styleType.provisional);
                if (CurrentToolStyle == styleType)
                    applyPropertyValue("<" + source + ":provSync>", key, newValue, styleType.style);
            }
        } else if (CurrentStyle == null) {
            if (DEBUG.STYLE) out("NO CURRENT STYLE FOR " + source + " " + key + " " + newValue);
        } else {
            CurrentStyle.takeProperty(source, key, newValue);
        }
    }

    /**
     * Apply the current appropriate properties to the given newly created object based on it's type.
     */
    public static void applyCurrentProperties(LWComponent c) {
        applyCurrentProperties(c, 0L);
    }
            
    private static void applyCurrentProperties(LWComponent c, long freeBits) {
        if (c == null)
            return;

        try {
            
            // TODO: we can fix the problem of all font properties coming along if you
            // change any one of them (because copyStyle currently copies the entire
            // font property at once -- it doesn't do it by sub-property) by dumping the
            // provisionals code -- we just have to extract any free properties from the
            // LWEditors whenever we do this, and apply them to type typed style,
            // creating a temporary style to use without having to resolve it to a
            // target (a transient resolve).  E.g.: the creation node / creation link:
            // we want to use the current properties for drawing the newly drag-created
            // object, but we don't want to target them until / unless they actually
            // create the node/link, which can be aborted during the drag operation.

            // Currently tho, this is actually NOT a problem, as neither the creation
            // node or link draw text and (using the font) while being dragged,
            // so unless that happens, we won't be hit by the font limitation.

            StyleType styleType = StylesByType.get(typeToken(c));
            if (styleType != null) {
                if (freeBits != 0)
                    c.copyProperties(styleType.provisional, freeBits);
                else
                    c.copyStyle(styleType.provisional);
            }
            
        } catch (Throwable t) {
            tufts.Util.printStackTrace(t, "failed to apply current properties to: " + c);
        }
    }

    /**

     * Apply the current appropriate properties to the given newly created object based
     * on it's type.  If nothing was selected, and the the editors were all enabled in
     * their "free" state (not tied to the property of something selected) resolve that
     * any changes to their free state were meant for the type of the given node.
     
     */
    public static void targetAndApplyCurrentProperties(LWComponent c) {
        try {
            
            LWComponent styleForType = resolveToProvisionalStyle(c);
            consumeFreeProperties();
            if (styleForType != null)
                c.copyStyle(styleForType);
            
        } catch (Throwable t) {
            tufts.Util.printStackTrace(t, "failed to target and apply current properties to: " + c);
        }
        
    }
    
        
        
    private static void applyPropertyValue(Object source, Object key, Object newValue, LWComponent target) {
        //if (DEBUG.STYLE) System.out.println("APPLY " + source + " " + key + "[" + newValue + "] -> " + target);
        if (target.supportsProperty(key)) {
            if (DEBUG.STYLE) out(String.format("APPLY %s %-15s %-40s -> %s", source, key, "(" + newValue + ")", target));
            try {
                target.setProperty(key, newValue);
                //} catch (LWComponent.PropertyValueVeto ex) {
                //tufts.Util.printStackTrace(ex);
            } catch (Throwable t) {
                tufts.Util.printStackTrace(t, source + " failed to set property " + key + "; value=" + newValue + " on " + target);
            }
        }
    }

    private synchronized StyleType getStyleForType(LWComponent c) {
        final Object token = typeToken(c);
        StyleType styleType = token == null ? null : StylesByType.get(token);
        if (styleType == null && token != null) {
            styleType = putStyle(token, createStyle(c, token));
            //styleHolder = createStyle(c, token, "style");
            //StylesByType.put(token, styleHolder);
        }
        if (DEBUG.STYLE) out("got styleHolder for type token (" + token + "): " + styleType); 
        return styleType;
    }

    private static StyleType putStyle(Object token, LWComponent style) {
        StyleType newType = new StyleType(token,
                                          style,
                                          createStyle(style, token, "provi"));
        StylesByType.put(token, newType);
        return newType;
    }

    private static final LWComponent.CopyContext DUPE_WITHOUT_CHILDREN = new LWComponent.CopyContext(false);

    private static LWComponent createStyle(LWComponent styleSource, Object typeToken) {
        return createStyle(styleSource, typeToken, "style");
    }
    
    private static LWComponent createStyle(LWComponent styleSource, Object typeToken, String version)
    {
        //if (DEBUG.STYLE || DEBUG.WORK) out("creating style holder based on " + styleSource + " for type (" + typeToken + ")");

        // As any LWComponent can be used as a style source,
        // we can just dupe whatever we've got (a handy
        // way to instance another component with the same typeToken)
        //final LWComponent style = styleSource.duplicate(DUPE_WITHOUT_CHILDREN);
        // Too risky: e.g., styles getting image updates?  Not fatal, but messy
        // / confusing to debug.

        LWComponent style = null;

        try {
            style = styleSource.getClass().newInstance();
        } catch (Throwable t) {
            Util.printStackTrace(t, "newInstance " + styleSource.getClass());
            return null;
        }
        
        style.copySupportedProperties(styleSource);
        style.copyStyle(styleSource);
        style.setPersistIsStyle(Boolean.TRUE); // mark as a style: e.g., so if link, can know not to recompute

        //-----------------------------------------------------------------------------
        // for clear debugging info only:
        style.enableProperty(LWKey.Label); // in case it's override getLabel like LWSlide
        // note: property bits are duplicated, so if we ever change from a model of
        // just copying the style from the style to using it as a base for
        // duplication, we have to leave the original bits in place.
        style.setLabel("<" + version + ":" + typeToken + ">"); 
//         style.setResource((Resource)null); // clear out any resource if it had it
//         style.setNotes(null);
//         style.takeLocation(0,0);
//         style.takeSize(100,100);
        //-----------------------------------------------------------------------------

        //out("created new styleHolder for type token [" + token + "]: " + styleHolder); 
        //out("created " + styleHolder);

        if (DEBUG.STYLE) out("made style " + style + " based on " + styleSource);
        return style;
    }
        
//     private static LWComponent resolveToProvisionalStyleFor(LWComponent c) {
//         if (c != null)
//             return resolveToProvisionalStyle(c.getTypeToken());
//         else
//             return null;
//     }
    
    private static LWComponent resolveToProvisionalStyle(Object typeToken) {
        return resolveToProvisionalStyle(typeToken, 0L);
    }
    
    /** @return the current style for type type of the given component only if we already have one
     * -- do not auto-create a new style for the type if we don't already have one */
    private static LWComponent resolveToProvisionalStyle(Object typeToken, long freeBits)
    {
        if (DEBUG.STYLE) out("resolveToProvisionalStyle: " + typeToken);
        
        if (typeToken == null)
            return null;
        
        if (typeToken instanceof LWComponent) {
            //Util.printStackTrace("oops; passed LWComponent as type-token: " + typeToken);
            typeToken = typeToken((LWComponent)typeToken);
            if (DEBUG.STYLE) out("resolveToProvisionalStyle: " + typeToken);
        }
        
        if (StylesByType == null) {
            tufts.Util.printStackTrace("circular static initializer dependency");
            return null;
        }

        StyleType resolver = StylesByType.get(typeToken);

        // move the provisional style (unselected tool state style) to the actual
        // style for this type:
        if (resolver != null) {
            resolver.resolveToProvisional(freeBits);
            if (DEBUG.STYLE || DEBUG.WORK) out("Resolved provisional type to final applied type: " + resolver);
            consumeFreeProperties();

        }

        // new reset all other provisional styles: the tool state change has been
        // resolved to been have meant for an object of the type just created

        for (StyleType styleType : StylesByType.values())
            if (styleType != resolver)
                styleType.discardProvisional();

        return resolver == null ? null : resolver.produceStyle();

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


    private void extractMostRecentlyUsedStyles(LWMap map) {
        final Collection<LWComponent> allNodes = map.getAllDescendents(LWMap.ChildKind.ANY);

        final Map<Object,LWComponent> foundStyles = new HashMap();

        Object typeToken;
        LWComponent curStyle;
        for (LWComponent c : allNodes) {
            typeToken = typeToken(c);

            if (typeToken == null)
                continue;
            
            curStyle = foundStyles.get(typeToken);
            
            if (curStyle == null) {
                // first type we've seen this type: just load it up
                foundStyles.put(typeToken, c);
            } else {
                if (c.getNumericID() > curStyle.getNumericID()) {
                    // the object of this type is more recent than any we've seen before:
                    // stash away a reference to it.
                    foundStyles.put(typeToken, c);
                }
            }
        }

        for (Map.Entry<Object,LWComponent> e : foundStyles.entrySet()) {
            final Object token = e.getKey();
            final LWComponent lastCreated = e.getValue();
            if (DEBUG.STYLE || DEBUG.INIT) out("extracted style for " + token + " from " + lastCreated);
            final LWComponent extractedStyle = createStyle(lastCreated, token);
            putStyle(token, extractedStyle);
        }
    }

    private static Object typeToken(LWComponent c) {

        return c.getTypeToken();

// This won't work for keeping separate slide styles until new objects created on the slide get
// their SLIDE_STYLE bit immediately set before they're auto-styled: will probably need a
// NewItem factory (may replace code that NodeTool currently has) fetchable from any LWContainer.
// Also, anywhere we use token == value would need to be updated, at least using this method
// of token differentiation, as it creates a new string object each time a SLIDE_STYLE token is
// fetched (could get around that with another HashMap...)        
//        
//         if (c.hasFlag(Flag.SLIDE_STYLE))
//             return c.getTypeToken() + "/slide";
//         else
//             return c.getTypeToken();
    }
    

    static void unregisterEditor(LWEditor editor) {

        if (editor.getPropertyKey() == null) {
            //if (DEBUG.Enabled) System.out.println("EditorManager: registration ignoring editor w/null key: " + dumpEditor(editor));
            Log.debug("unregistration ignoring editor w/null key: " + dumpEditor(editor));
            return;
        }
        
        if (mEditors.remove(editor)) {
            if (DEBUG.TOOL || DEBUG.INIT) out("UNREGISTERED EDITOR: " + editor);
            if (editor instanceof java.awt.Component)
                ((java.awt.Component)editor).removePropertyChangeListener(singleton);
        } 
    }
    
    static boolean isRegistered(LWEditor editor)
    {
    	if (editor.getPropertyKey() == null) {
            //if (DEBUG.Enabled) System.out.println("EditorManager: registration ignoring editor w/null key: " + dumpEditor(editor));
            Log.debug("ignoring editor w/null key: " + dumpEditor(editor));
            return false;
        }
    	
    	if (mEditors.contains(editor))
    		return true;
    	else
    		return false;
    	
    }
    static void registerEditor(LWEditor editor) {

        if (editor.getPropertyKey() == null) {
            //if (DEBUG.Enabled) System.out.println("EditorManager: registration ignoring editor w/null key: " + dumpEditor(editor));
            Log.debug("registration ignoring editor w/null key: " + dumpEditor(editor));
            return;
        }
        
        if (mEditors.add(editor)) {
            if (DEBUG.TOOL || DEBUG.INIT) out("REGISTERED EDITOR: " + editor);
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
        Log.debug(this + " now managing " + mEditors.size() + " LWEditors.");
    }

    static void out(Object o) {
        //Log.forcedLog("FOO", org.apache.log4j.Level.ALL, "EditorManager: " + o, null);
        Log.debug(o);
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
