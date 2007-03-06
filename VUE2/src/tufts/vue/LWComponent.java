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

import java.awt.Shape;
import java.awt.Color;
import java.awt.Font;
import java.awt.Stroke;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.Collection;
import java.util.ArrayList;

import tufts.vue.beans.UserMapType; // remove: old SB stuff we never used
import tufts.vue.filter.*;
import edu.tufts.vue.style.Style;
import edu.tufts.vue.style.StyleMap;

/**
 * VUE base class for all components to be rendered and edited in the MapViewer.
 *
 * @version $Revision: 1.208 $ / $Date: 2007-03-06 16:36:52 $ / $Author: sfraize $
 * @author Scott Fraize
 * @license Mozilla
 */

// todo: on init, we need to force the constraint of size being set before
// label (applies to XML restore & duplicate) to support backward compat before
// line-wrapped text.  Otherwise, in LWNode's, setting label before size set will cause
// the size to be set.

public class LWComponent
    implements VueConstants, XMLUnmarshalListener
{
    enum ChildKind {
        /** the default, conceptually significant chilren */
        PROPER,

        /** Above, plus include ANY children, such as slides and their children -- the only
         * way to make sure you hit every active LWComponent in the runtime related
         * to a particular LWMap (not including the Undo queue)
         */
        ANY,

       // VIRTUAL -- would be *just* what ANY currently adds, and exclude PROPER -- currently unsupported
    }

    public static final int HIDE_USER = 0x1;
    public static final int HIDE_FILTER = 0x2;
    public static final int HIDE_PRUNE = 0x4;
    public static final int HIDE_PATHWAY_UNREVEALED = 0x8;
    public static final int HIDE_PATHWAY_EXCLUDE_OTHERS = 0x16;

    public static final java.awt.datatransfer.DataFlavor DataFlavor =
        tufts.vue.gui.GUI.makeDataFlavor(LWComponent.class);
    
    public static final int MIN_SIZE = 10;
    public static final Size MinSize = new Size(MIN_SIZE, MIN_SIZE);
    public static final float NEEDS_DEFAULT = Float.MIN_VALUE;
    
    public interface Listener extends java.util.EventListener {
        public void LWCChanged(LWCEvent e);
    }

    /*
     * Meta-data persistant information
     */
    protected String label = null; // protected for debugging purposes
    private String notes = null;
    private Resource resource = null;
    
    /*
     * Persistent information
     */

    private String ID = null;

    private float x;
    private float y;
    // TODO: if we want to support some kind of keep-relative alignment for an object
    // (in it's parent), we couldn't just use a special object on a generic x/y value
    // ptr -- we still need ACTUAL x/y values to render, but we could have an
    // xAnchor/yAnchor, which could even be a list of actions to perform every time the
    // object is laid out, or it's parent resizes.
    
    private boolean isFiltered = false; // replace with hidebits
    private NodeFilter nodeFilter = null; // don't know why we need this
    
    protected float width = NEEDS_DEFAULT;
    protected float height = NEEDS_DEFAULT;
    protected java.awt.Dimension textSize = null; // only for use with wrapped text


    /*
     * Runtime only information
     */
    protected transient TextBox labelBox = null;
    protected transient BasicStroke stroke = STROKE_ZERO;
    protected transient boolean hidden = false;
    //protected transient boolean locked = false;
    protected transient boolean selected = false;
    //protected transient boolean indicated = false;
    protected transient boolean rollover = false;
    protected transient boolean isZoomedFocus = false;

    protected transient LWContainer parent = null;
    protected transient LWComponent mParentStyle;
    protected transient boolean isStyleParent;

    // list of LWLinks that contain us as an endpoint
    private transient java.util.List<LWLink> links = new java.util.ArrayList<LWLink>();
    protected transient java.util.List<LWPathway> pathwayRefs;
    private transient long mSupportedPropertyKeys;

    // Scale currently exists ONLY to support the auto-managed child-node feature of nodes
    protected transient float scale = 1.0f;

    //protected transient java.util.List listeners;
    protected transient LWChangeSupport mChangeSupport = new LWChangeSupport(this);
    protected transient boolean mXMLRestoreUnderway = false; // are we in the middle of a restore?
    protected transient BufferedImage mCachedImage;
    protected transient double mCachedImageAlpha;
    protected transient Dimension mCachedImageMaxSize;

    public static final java.util.Comparator XSorter = new java.util.Comparator<LWComponent>() {
            public int compare(LWComponent c1, LWComponent c2) {
                // we multiply up the result so as not to loose differential precision in the integer result
                return (int) (128f * (c1.x - c2.x));
            }
        };
    public static final java.util.Comparator YSorter = new java.util.Comparator<LWComponent>() {
            public int compare(LWComponent c1, LWComponent c2) {
                return (int) (128f * (c1.y - c2.y));
            }
        };

    /** for save/restore only & internal use only */
    public LWComponent()
    {
        if (DEBUG.PARENTING)
            System.out.println("LWComponent construct of " + getClass().getName() + Integer.toHexString(hashCode()));
        // TODO: shouldn't have to create a node filter for every one of these constructed...
        nodeFilter = new NodeFilter();
        mSupportedPropertyKeys = Key.PropertyMaskForClass(getClass());
    }

    /** Convenience: If key not a real Key (a String), always return true */
    public boolean supportsProperty(Object key) {
        if (key instanceof Key)
            return supportsProperty((Key)key);
        else
            return true;
    }
    
    /** @return true if the given property is currently supported on this component */
    public boolean supportsProperty(Key key) {
        return (mSupportedPropertyKeys & key.bit) != 0;
    }

    protected void disableProperty(Key key) {
        mSupportedPropertyKeys &= ~key.bit;
    }

    /** Apply all style properties from styleSource to this component */
    public void copyStyle(LWComponent styleSource) {
        for (Key key : Key.AllKeys) {
            if (key.isStyleProperty && supportsProperty(key) && styleSource.supportsProperty(key)) {
                key.copyValue(styleSource, this);
            }
        }
    }
    /*
    public void applyCSS(StyleMap styleMap) {
        for (Map.Entry<String,Style> se : styleMap.entrySet()) {
            final String cssName = se.getKey();
            for (Key key : Key.AllKeys) {
                if (key.cssName != null && supportsProperty(key))
                }
            }
        }
    }
    */

    /**
     * Describes a property on a VUE LWComponent, and provides an info string for creating Undo names,
     * and for diagnostic output.  Implies the ability to set/get the value on an LWComponent by some means.
     */
    // todo: consdier moving all the Key/Property code to some kind of superclass to LWComponent -- LWStyle? Vnode? LWKey? LWState?
    // We'd move it elsewhere, but we'd have to export all sorts of stuff to make all thats needed available,
    // as they get everything currently being inner classes.

    // The generic type TSubclass allows the inner-class impl's of getValue & setValue, in subclasses
    // of LWComponent, to use their own type in the first argument to set/getValue, omitting
    // the need for casts in the method.

    public enum KeyType { Default, STYLE, SUB_STYLE, DATA };
            
    // todo: TValue may be overkill -- may want to revert to using just Object
    public static class Key<TSubclass extends LWComponent,TValue> {
        /** A name for this key (used for undo labels & debugging) */
        public final String name;
        /** A name for a CSS property that can be used to initialize the value for this key */
        public final String cssName;
        /** The unique bit for this property key.
            (Implies a max of 64 keys that can uniquely known as active to our tools -- use a BitSet if need more) */
        public final long bit;
        /** True if this key for a style property -- a property that moves from style holders to LWCopmonents
         * pointing to it via mParentStyle */
        public final boolean isStyleProperty;

        public final KeyType keyType;

        /* True this property is a sub-part of some other property */
        //public final boolean isSubProperty;

        public static final java.util.List<Key> AllKeys = new java.util.ArrayList<Key>();

        private static int InstanceCount; // increment for each key instance, to establish the appropriate bit
        private static final java.util.Map<Class,Long> ClassProperties = new java.util.HashMap<Class,Long>();
        
        /** Get the supported property bit mask for the given class in the LWComponent inheritance tree
         * This will only return accurate results after all Key's in the codebase have been initialized. */
        static long PropertyMaskForClass(Class<? extends LWComponent> clazz) {
            final Long bitsForClass = ClassProperties.get(clazz); // property bits for this class
            if (bitsForClass == null) {
                // If we found nothing, this must be the first instance of a new object
                // for some subclass of LWComponent that doesn't declare any of it's
                // own keys.  Merge the bits for all superclasses and put it in the
                // map for future reference.
                long propMaskForClass = 0L;
                for (Class c = clazz; c != null; c = c.getSuperclass())
                    propMaskForClass |= PartialPropertyMaskForClass(c);

                if (DEBUG.Enabled) System.out.format("CACHED PROPERTY BITS for %s: %d\n", clazz, Long.bitCount(propMaskForClass));
                ClassProperties.put(clazz, propMaskForClass);

                return propMaskForClass;
            } else
                return bitsForClass;
        }

        /** @return the currently stored property mask for the given class: only used during initialization
         * Will return 0L (no bit set) if the given class is not in the map (e.g., java.lang.Object)
         * This is used to disambiguate between properties that apply only to a particular
         * LWComponent subclass while we produce the ultimate merged results for all classes in
         * the hierarchy.
         */
        private static long PartialPropertyMaskForClass(Class clazz) {
            final Long bitsForClass = ClassProperties.get(clazz); // property bits for this class
            if (bitsForClass == null)
                return 0L;
            else
                return bitsForClass;
        }
        
        public Key(String name) {
            this(name, KeyType.Default);
        }
        public Key(String name, KeyType keyType) {
            this(name, null, keyType);
        }
        public Key(String name, String cssName) {
            this(name, cssName, KeyType.STYLE);
        }

        //protected Key(String name, String cssName, boolean partOfStyle, boolean isSubProperty) {
        protected Key(String name, String cssName, KeyType keyType) {
            this.name = name;
            this.cssName = cssName;
            this.keyType = keyType;
            this.isStyleProperty = (keyType == KeyType.STYLE);
            //this.isStyleProperty = partOfStyle;
            //this.isSubProperty = isSubProperty;
            if (InstanceCount >= Long.SIZE) {
                this.bit = 0;
                tufts.Util.printStackTrace(Key.class + ": " + InstanceCount + "th key created -- need to re-implement (try BitSet)");
            } else
                this.bit = 1 << InstanceCount;
            AllKeys.add(this);

            // Note: this only works if the key is in fact declared in the enclosing class to
            // which it applies.  If we want to declare keys elsewhere, we'll need to add
            // a Class argument to the constructor.
            final Class clazz = getClass().getEnclosingClass(); // the class that own's the Key
            long propMaskForClass = (PartialPropertyMaskForClass(clazz) | bit); // add the new bit

            // Now be sure to mix in all properties found in all super-classes:
            for (Class c = clazz; c != null; c = c.getSuperclass())
                propMaskForClass |= PartialPropertyMaskForClass(c);
            
            ClassProperties.put(clazz, propMaskForClass);

            System.out.printf("KEY %-20s %-11s %-25s bit#%2d; %25s now has %2d properties\n", 
                              name,
                              //isStyleProperty ? "STYLE;" : "",
                              keyType,
                              cssName == null ? "" : cssName,
                              InstanceCount,
                              clazz.getName(),
                              Long.bitCount(propMaskForClass)
                              );
            InstanceCount++;

            // Just referencing a class object won't load it's statics: must do a new instance.
            // This will be easy enough to ensure at startup.
            //new LWImage();
            //System.out.println("BITS FOR " + LWImage.class + " " + PropertyMaskForClass(LWImage.class));
            
            // Could build list of all key (and thus slot) values here for each subclass,
            // but where would we attach it?  Would need to pass in the class variable
            // in the constructor, and hash it to a list for the class.  Then the
            // problem would be that each list would only contain the subclass items,
            // not the super -- tho could we just iterate up through the supers getting
            // their lists to build the full list for each class?  (e.g., for duplicate,
            // persistance, or runtime diagnostic property editors)

            // OH: we also need to build the bitfield for the enclosing class:
            // the runtime-constant bit-mask representing all the properties
            // handled by this class / subclass of LWComponent

        }

        private static final LWComponent EmptyStyle = new LWComponent();
        private static final Property NO_SLOT_PROVIDED = EmptyStyle.mFillColor; // any slot will do
        //private static final Property BAD_SLOT = EmptyStyle.mStrokeColor; // any (different) slot will do
        /** If this isn't overriden to return non-null, getValue & setValue must be overriden to provide the setter/getter impl  */
        Property getSlot(TSubclass c) { return NO_SLOT_PROVIDED; }

        // If we wanted to get rid of the slot decl's in the key's (for those that use
        // slots), we could, in our defult slot-using set/getValue, search all property
        // objects in the LWComponent, and if any of them match our key, we know that's
        // that slot, and if none of them do, then we have in internal error: coder
        // should have impl'd set/getValue themselves.

        /** non slot-based property keys can override this */
        TValue getValue(TSubclass c) {
            final Property propertySlot = getSlotSafely(c);
            try {
                if (propertySlot == NO_SLOT_PROVIDED) {
                    tufts.Util.printStackTrace(this + ": no slot, and getValue not overriden");
                    return null;
                } else
                    return (TValue) propertySlot.get();
            } catch (Throwable t) {
                if (DEBUG.META)
                    tufts.Util.printStackTrace(t, this + ": property slot get() failed " + propertySlot);
                else
                    VUE.Log.warn(this + ": property slot get() failed " + propertySlot + " " + t);
                return DEBUG.Enabled ? (TValue) "<unsupported for this object>" : null;
                //return null;
            }
        }

        /** non slot-based property keys can override this */
        void setValue(TSubclass c, TValue value) {
            final Property slot = getSlotSafely(c);
            if (slot == null || slot == NO_SLOT_PROVIDED)
                return;
            if (value instanceof String) {
                // If a String value comes in, this allows us to auto-parse it
                slot.setFromString((String)value);
            } else {
                slot.set(value);
            }
                /*
            try {
                if (value instanceof String) {
                    // If a String value comes in, this allows us to auto-parse it
                    getSlot(c).setFromString((String)value);
                } else {
                    getSlot(c).set(value);
                }
            } catch (ClassCastException e) {
                tufts.Util.printStackTrace(e, "Bad setValue type for " + getSlot(c) + ": " + (value == null ? "null" : value.getClass()));
            }
                */
        }
        
        private Property getSlotSafely(TSubclass c) {
            try {
                return getSlot(c);
            } catch (ClassCastException e) {
                String msg = "Property not supported: " + this + " on\t" + c + " (getSlot failed; returned null)";
                //tufts.Util.printStackTrace(e, msg);
                VUE.Log.warn(msg + "; " + e);
                return null;
            } catch (Throwable t) {
                tufts.Util.printStackTrace(t, this + ": bad slot? unimplemented get/setValue?");
                return null;
            }
        }
        

        /** non slot-based property keys can override this */
        String getStringValue(TSubclass c) {
            final Property slot = getSlotSafely(c);
            if (slot == NO_SLOT_PROVIDED || slot == null) {
                // If there is no slot provided, we must get the value from the overridden
                // getter, getValue.
                Object typedValue = null;
                try {
                    // Call the overriden getValue:
                    typedValue = getValue(c);
                } catch (ClassCastException e) {
                    final String msg = "Property not supported(getStringValue): " + this + " on\t" + c;
                    if (DEBUG.META)
                        tufts.Util.printStackTrace(e, msg);
                    else
                        VUE.Log.warn(msg + "; " + e);
                    return DEBUG.Enabled ? "<unsupported for this object>" : null;
                }
                return typedValue == null ? null : typedValue.toString(); // produce something
//              } else if (slot == null) {
//                 // If a slot was provided, but it failed, no sense in trying
//                 // the default getValue, which presumably wasn't overriden if
//                 // a slot was provided.
//                 //tufts.Util.printStackTrace(this + ": bad slot");
//                 return DEBUG.Enabled ? "<unsupported for this object>" : null;
            } else
                return slot.asString();
        }
        
        void setValue(TSubclass c, String stringValue) {
            getSlotSafely(c).setFromString(stringValue);
        }

        /** @return true if the value for this Key in LWComponent is equivalent to otherValue
         * Override to provide non-standard equivalence (Object.equals) */
        boolean valueEquals(TSubclass c, TValue otherValue) 
        {
            final TValue value = getValue(c);
            return value == otherValue || (otherValue != null && otherValue.equals(value));
        }

        void copyValue(TSubclass source, TSubclass target)
        {
            if (!source.supportsProperty(this)) {
                // TODO: Some supported bits apparently not being fully updated... need to fix that!  (Happening to LWStyle in LWPathway)
                if (DEBUG.Enabled) System.err.println(" COPY-VALUE: " + this + "; source doesn't support this property; " + source);
            } else if (!target.supportsProperty(this)) {
                if (DEBUG.Enabled) System.err.println(" COPY-VALUE: " + this + "; target doesn't support this property; " + target);
            } else {
                if (DEBUG.Enabled) System.err.print(" COPY-VALUE: " + this + "(");
                final TValue copyValue = getValue(source);
                if (DEBUG.Enabled) System.err.println(copyValue + ") -> " + target);
                setValue(target, copyValue);
            }
        }

        
        
        public String toString() { return name; } // must == name for now until tool panels handle new key objects
    }

    /* A marker class for Key's that are considered user interested "data" (e.g., a label) 
    public static class DataKey<TSubclass extends LWComponent,TValue> extends Key<TSubclass,TValue> {
        public DataKey(String name) {
            super(name, false, false);
        }
    }
*/    
    /* A marker class for Key's that are for style properties 
    public static class StyleKey<TSubclass extends LWComponent> extends Key<TSubclass,Object> {
        public StyleKey(String name) {
            super(name, true, false);
        }
    }
*/    /*
    public static class TypedStyleKey<TSubclass extends LWComponent,TValue> extends Key<TSubclass,TValue> {
        public TypedStyleKey(String name) {
            super(name, true, false);
        }
    }
    */
    
    /* A marker class for Key's that are for sub-style properties (properties that make up some other total style value) 
    public static class SubStyleKey<TSubclass extends LWComponent,TValue> extends Key<TSubclass,TValue> {
        public SubStyleKey(String name) {
            super(name, true, true);
        }
    }
    
*/
    // this is a bit obscene given that it just recaptulations introspection, but it's obviously faster.
    // Tho it would also make handling the duplicate code alot more automatic.
    //private java.util.ArrayList<Property> allProps = new java.util.ArrayList();

    /**
     * This class allows us to define an arbitrary property for a LWComponent, and
     * define a default set of setters and getters that automatically handle stuff like
     * undo and positing change notifications.  It is also essential in allowin us to
     * easily attach meta-data to the property itself: e.g., it's locked, it's
     * overriding a parent style value, it's caching some related computed value, etc.
     */
    public abstract class Property<T> {
        
        final Key key;
        protected T value;

        boolean locked; // could handle instead as above bitfield
        
        Property(Key key) {
            this.key = key;
            //mSupportedPropertyKeys |= key.bit;
            //LWComponent.this.allProps.add(this);
        }

        T get() { return value; }

        void set(T newValue) {
            //final Object old = get(); // if "get" actually does anything tho, this is a BAD idea; if needbe, create a "curValue"
            if (this.value == newValue || (newValue != null && newValue.equals(this.value)))
                return;
            final Object oldValue = this.value;
            take(newValue);
            onChange();
            LWComponent.this.notify(this.key, oldValue);
        }

        /** This JUST changes the stored value: no notifications of any kind will be triggered, no undo recorded. */
        void take(T o) {
            this.value = o;
            if (DEBUG.WORK) System.out.printf("     TAKING: %-30s -> %s\n", vtag(key, o, this), LWComponent.this);
        }

        /** impl's can override this to do something after the value has changed (after take() has been called),
         * and before listeners have been notified */
        void onChange() {}

        void setFromString(String s) {
            try {
                setBy(s);
            } catch (Throwable t) {
                VUE.Log.error("bad value for " + this + ": [" + s + "] " + t);
            }
        }

        void setBy(String fromValue) {
            // Could get rid all of the setBy's (and then mayve even all the StyleProp subclasses!!)
            // If we just had mapper class that took a type, a value, and returned a string (e.g., Font.class, Object value)
            VUE.Log.error("unimplememnted: " + this + " setBy " + fromValue.getClass() + " " + fromValue);
        }

        /** override to provide an impl other than value.toString() */
        String asString() {
            return value == null ? null : value.toString();
        }

        /*
        void setByUser(Object newValue) { // for tools.  Actually, tools using generic setProperty right now...
            out("SetByUser: " + key + " " + newValue);
            set(newValue);
        }
        */

        /** used for debugging */
        public String toString() {
            return key + "(" + value.toString() + ")";
        }
        
    }

    private static final String _DefaultString = "";
    public class StringProperty extends Property<java.lang.String> {
        StringProperty(Key key) {
            super(key);
            value = _DefaultString;
        }
        final void setBy(String s) { set(s); }
    }

    private static final Integer _DefaultInteger = new Integer(0);
    public class IntProperty extends Property<java.lang.Integer> {
        IntProperty(Key key) {
            super(key);
            value = _DefaultInteger;
        }
        final void setBy(String s) { set(new Integer(s)); }
    }
    
    private static final Float _DefaultFloat = new Float(0f);
    public class FloatProperty extends Property<java.lang.Float> {
        FloatProperty(Key key) {
            super(key);
            value = _DefaultFloat;
        }
        final void setBy(String s) { set(new Float(s)); }
    }

    public class FontProperty extends Property<java.awt.Font> {
        FontProperty(Key key) {
            super(key);
            value = VueConstants.FONT_DEFAULT;
        }
        final void setBy(String s) { set(Font.decode(s)); }
        final String asString() {
            //if (this.font == null || this.font == getParent().getFont())
            //return null;

            final Font font = get();
            final String strStyle;
            
            if (font.isBold()) {
                strStyle = font.isItalic() ? "bolditalic" : "bold";
            } else {
                strStyle = font.isItalic() ? "italic" : "plain";
            }
            return font.getName() + "-" + strStyle + "-" + font.getSize();
        }
    }
    
    
    public class ColorProperty extends Property<java.awt.Color> {
        ColorProperty(Key key) { super(key); }
        ColorProperty(Key key, Color defaultValue) {
            this(key);
            this.value = defaultValue;
        }

        public boolean isTransparent() {
            return value == null || value.getAlpha() == 0;
        }
    
        public boolean isTranslucent() {
            return value == null || value.getAlpha() != 0xFF;
        }

        void setBy(String s) {
            set(StringToColor(s));
        }

        String asString() {
            return ColorToString(get());
        }
    }
    
    public static Color StringToColor(final String s)
    {
        if (s.trim().length() < 1)
            return null;
        
        Color c = null;
        try {
            c = VueResources.makeColor(s);
        } catch (NumberFormatException e) {
            System.err.println("LWComponent.StringToColor[" + s + "] " + e);
        }
        return c;
    }
    public static String ColorToString(final Color c)
    {
        // if null, or no hue and no alpha, return null
        //if (c == null || ((c.getRGB() & 0xFFFFFF) == 0 && c.getAlpha() == 255))
        if (c == null)
            return null;
        
        if (c.getAlpha() == 255) // opaque: only bother to save hue info
            return String.format("#%06X", c.getRGB() & 0xFFFFFF);
        //return String.format("#%06X (%s)", c.getRGB() & 0xFFFFFF, Integer.toHexString(c.getRGB() & 0xFFFFFF));
        // this puts out non zero-filled strings
        //return "#" + Integer.toHexString(c.getRGB() & 0xFFFFFF);
        else if (c.getAlpha() == 0) // totally transparent, be sure alpha still indicated!
            return "#00" + Integer.toHexString(c.getRGB());
        else
            return "#" + Integer.toHexString(c.getRGB());
    }

    
    public static final Key KEY_FillColor   = new Key("fill.color", "background")       { final Property getSlot(LWComponent c) { return c.mFillColor; } };
    public static final Key KEY_TextColor   = new Key("text.color", "font-color")       { final Property getSlot(LWComponent c) { return c.mTextColor; } };
    public static final Key KEY_StrokeColor = new Key("stroke.color", "border-color")   { final Property getSlot(LWComponent c) { return c.mStrokeColor; } };
    public static final Key KEY_StrokeStyle = new Key("stroke.style", "border-style")   { final Property getSlot(LWComponent c) { return null; } };
    public static final Key KEY_StrokeWidth = new Key("stroke.width", "border-width")   { final Property getSlot(LWComponent c) { return c.mStrokeWidth; } };


    /** Aggregate font key, which represents the combination of it's three sub-properties */
    public static final Key KEY_Font = new Key("font", KeyType.STYLE)
        { final Property getSlot(LWComponent c) { return c.mFont; } };
    /** point size for font */
    public static final Key KEY_FontSize  = new Key("font.size", "font-size", KeyType.SUB_STYLE)
        { final Property getSlot(LWComponent c) { return c.mFontSize; } };
    /** @See java.awt.Font 0x0=Plain, 0x1=Bold On, 0x2=Italic On */
    public static final Key KEY_FontStyle = new Key("font.style", "font-style", KeyType.SUB_STYLE)
        { final Property getSlot(LWComponent c) { return c.mFontStyle; } };
    /** name of the font */
    public static final Key KEY_FontName  = new Key("font.name", "font-family", KeyType.SUB_STYLE)
        { final Property getSlot(LWComponent c) { return c.mFontName; } };
    
    public final ColorProperty mFillColor = new ColorProperty(KEY_FillColor);
    public final ColorProperty mTextColor = new ColorProperty(KEY_TextColor, java.awt.Color.black) {
            //{ color = java.awt.Color.black; } // default value
            void onChange() {
                if (labelBox != null)
                    labelBox.copyStyle(LWComponent.this); // todo better: handle thru style.textColor notification?
            }
        };
    public final ColorProperty mStrokeColor = new ColorProperty(KEY_StrokeColor, java.awt.Color.darkGray);
    public final FloatProperty mStrokeWidth = new FloatProperty(KEY_StrokeWidth) {
            void onChange() {
                final float width = get();
                // TODO: caching the stroke here is kind of overkill, and if we really want to do it,
                // keep a list of the common stroke widths (1-6)...
                // Also, assuming we keep the cached stroke around, this cached data would ideally go
                // right in the the property, as another piece of meta-data related to it.
                if (width > 0)
                    LWComponent.this.stroke = new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
                else
                    LWComponent.this.stroke = STROKE_ZERO;

                // below code was broken in previous code.  Node child layout does NOT
                // appear to be taking into account total bounds with at the moment anyway...
                // (Or was that just for Groups?  No, those appear to be handling the full bounds change.)
                // Also, want to make generic with a flag in Key if layout needed when
                // the given property changes.
                /*
                if (getParent() != null) {
                    // because stroke affects bounds-width, may need to re-layout parent
                    getParent().layout();
                }
                layout();
                */
            }
        };


    public final IntProperty mFontStyle = new IntProperty(KEY_FontStyle)        { void onChange() { rebuildFont(); } };
    public final IntProperty mFontSize = new IntProperty(KEY_FontSize)          { void onChange() { rebuildFont(); } };
    public final StringProperty mFontName = new StringProperty(KEY_FontName)    { void onChange() { rebuildFont(); } };

    private boolean fontIsRebuilding; // hack till we cleanup the old font code in gui tools (it's only all-at-once)
    private void rebuildFont() {
        // This so at least for now we have backward compat with the old font property (esp. for tools & persistance)
        fontIsRebuilding = true;
        try  {
            mFont.set(new Font(mFontName.get(), mFontStyle.get(), mFontSize.get()));
        } finally {
            fontIsRebuilding = false;
        }
    }
    
    public final FontProperty mFont = new FontProperty(KEY_Font) {
            void onChange() {
                // TODO: as "take" won't send notifications, tools listenting to just
                // these properties won't get updated...  Tho we don't want notifications
                // for now, as we'd loop...
                if (!fontIsRebuilding) {
                    final Font f = get();
                    mFontStyle.take(f.getStyle());
                    mFontSize.take(f.getSize());
                    mFontName.take(f.getName());
                }

                if (labelBox != null)
                    labelBox.copyStyle(LWComponent.this);
                layout(this.key); // could make this generic: add a key bit that says "layout needed on-change";
            }
        };


    public static final Key KEY_Label = new Key("label", KeyType.DATA) {
            public void setValue(LWComponent c, Object val) { c.setLabel((String)val); }
            public Object getValue(LWComponent c) { return c.getLabel(); }
        };
    

    /**
     * Get the named property value from this component.
     * @param key property key (see LWKey)
     * @return object representing appropriate value, or null if none found (note: properties may be null also -- todo: fix)
     */
    //public static final Object UnsupportedPropertyValue = "<value-of-unsupported-property>";
    public Object getPropertyValue(final Object key)
    {
        if (key instanceof Key) {
            // If getValue on the key was overriden, we may still need to trap an exception here
            try {
                return ((Key)key).getValue(this);
            } catch (ClassCastException e) {
                String msg = "Property not supported(getPropertyValue): " + key + " on " + this + " (returned null)";
                if (DEBUG.META)
                    tufts.Util.printStackTrace(e, msg);
                else
                    VUE.Log.warn(msg + "; " + e);
                return null;
            }
        }

        //if (key == LWKey.FillColor.name)     return getFillColor();
        //if (key == LWKey.TextColor.name)     return getTextColor();
        //if (key == LWKey.StrokeColor.name)   return getStrokeColor();
        //if (key == LWKey.StrokeWidth.name)   return new Float(getStrokeWidth());
        //if (key == LWKey.Font.name)          return getFont();
        //if (key == LWKey.Label)         return getLabel();
        if (key == LWKey.Notes)         return getNotes();
        if (key == LWKey.Resource)      return getResource();
        if (key == LWKey.Location)      return getLocation();
        if (key == LWKey.Size)          return new Size(this.width, this.height);
        if (key == LWKey.Hidden)        return new Boolean(isHidden());
             
        if (DEBUG.TOOL) out("note: getPropertyValue; unsupported property [" + key + "] (returning null)");
        //return UnsupportedPropertyValue;
        return null;
        //throw new RuntimeException("Unknown property key[" + key + "]");
    }

    // for debug
    private static String vtag(Object key, Object val, Property p) 
    {
        if (val == null) {
            return key + "(null)";
        } else if (val.getClass() == String.class) {
            return key + "(\"" + val + "\")";
        }
        
        String typeName = val.getClass().getName();
        String valType = typeName.substring(typeName.lastIndexOf('.') + 1);
        String valRep = (p == null ? val.toString() : p.asString());

        String extra = "";
        
        //if (p != null) extra = val.toString();
        //valType += "@" + Integer.toHexString(val.hashCode());
        
        return key + " " + valType + "(" + valRep + ")" + extra + "";
    }
    
    public void setProperty(final Object key, Object val)
    {
        if (DEBUG.Enabled||DEBUG.UNDO) System.out.println("setProperty: " + vtag(key, val, null) + " on " + LWComponent.this);

        if (key instanceof Key) {
            ((Key)key).setValue(this, val);
        }
        
        // "Still need this hack for fill color name till toolbars handle the new key objects"
        // This is coming from the damn VueBeans code, which is presumably what
        // the above comment ultimately refers to.  All this ".name" cases are that crap.
        //else if (key == LWKey.FillColor.name)   setFillColor( (Color) val);
        //if (val == UnsupportedPropertyValue) return;
                                           
        //else if (key == LWKey.TextColor.name)        setTextColor( (Color) val);
        //else if (key == LWKey.StrokeColor.name)      setStrokeColor( (Color) val);
        //else if (key == LWKey.StrokeWidth.name)      setStrokeWidth( ((Float) val).floatValue());
        //else if (key == LWKey.Font.name)             setFont( (Font) val);
        //else if (key == LWKey.Label)            setLabel( (String) val);
        else if (key == LWKey.Notes)            setNotes( (String) val);
        else if (key == LWKey.Resource)         setResource( (Resource) val);
        else if (key == LWKey.Location)         setLocation( (Point2D) val);
        else if (key == LWKey.Hidden)           setHidden( ((Boolean)val).booleanValue());
        else if (key == LWKey.Size) {
            Size s = (Size) val;
            setSize(s.width, s.height);
        } else if (key == LWKey.Frame) {
            Rectangle2D.Float r = (Rectangle2D.Float) val;
            setFrame(r.x, r.y, r.width, r.height);
        } else {
            //out("setProperty: unknown key [" + key + "] with value [" + val + "]");
            tufts.Util.printStackTrace("FYI: Unhandled Property key: " + key.getClass() + "[" + key + "] with value [" + val + "]");
        }
    }


    /**
     * This is used during duplication of group's of LWComponent's
     * (e.g., a random selection, or a set of children, or an entire map),
     * to reconnect links within the group after duplication.
     */
    public static class LinkPatcher {
        private java.util.Map<LWComponent,LWComponent> mCopies = new java.util.HashMap();
        private java.util.Map<LWComponent,LWComponent> mOriginals = new java.util.HashMap();

        public LinkPatcher() {
            if (DEBUG.DND) System.out.println("LinkPatcher: created");
        }

        public void reset() {
            mCopies.clear();
            mOriginals.clear();
        }

        public void track(LWComponent original, LWComponent copy)
        {
            if (DEBUG.DND && DEBUG.META) System.out.println("LinkPatcher: tracking " + copy);
            mCopies.put(original, copy);
            mOriginals.put(copy, original);
        }

        //public Collection getCopies() { return mCopies.values(); }
        
        public void reconnectLinks() {
            
            // Find all LWLink instances in the set of copied
            // objects, and fix their endpoint pointers to
            // point to the right object within the copied set.
            
            for (LWComponent c : mCopies.values()) {
                if (!(c instanceof LWLink))
                    continue;

                final LWLink linkCopy = (LWLink) c;
                final LWLink linkOriginal = (LWLink) mOriginals.get(linkCopy);
                
                final LWComponent endPoint1Copy = mCopies.get(linkOriginal.getComponent1());
                final LWComponent endPoint2Copy = mCopies.get(linkOriginal.getComponent2());
                
                if (DEBUG.DND)
                    System.out.println("LinkPatcher: reconnecting " + linkCopy + " endpoints:"
                                       + "\n\t" + endPoint1Copy
                                       + "\n\t" + endPoint2Copy
                                       );
                
                linkCopy.setComponent1(endPoint1Copy);
                linkCopy.setComponent2(endPoint2Copy);
            }
        }
    }
    
    
    /**
     * Create a component with duplicate content & style.  Does not
     * duplicate any links to this component, and leaves it an
     * unparented orphan.
     *
     * @param linkPatcher may be null.  If not, it's used when
     * duplicating group's of objects containing links that need to be
     * reconnected at the end of the duplicate.
     */

    public LWComponent duplicate(LinkPatcher patcher)
    {
        final LWComponent c;

        try {
            c = getClass().newInstance();
        } catch (Throwable t) {
            tufts.Util.printStackTrace(t, "duplicate " + getClass());
            return null;
        }

        c.mSupportedPropertyKeys = this.mSupportedPropertyKeys;
        c.mParentStyle = this.mParentStyle;
        
        c.x = this.x;
        c.y = this.y;
        c.width = this.width;
        c.height = this.height;
        c.scale = this.scale;
        c.stroke = this.stroke; // cached info only

        c.copyStyle(this);

        c.setAutoSized(isAutoSized());
        //c.setFillColor(getFillColor());
        //c.setTextColor(getTextColor());
        //c.setStrokeColor(getStrokeColor());
        c.setLabel(this.label); // use setLabel so new TextBox will be created [!no longer an effect]
        c.getLabelBox().setSize(getLabelBox().getSize());

        
        if (hasResource())
            c.setResource(getResource());
        if (hasNotes())
            c.setNotes(getNotes());

        if (patcher != null)
            patcher.track(this, c);

        return c;
    }

    public LWComponent duplicate() {
        return duplicate(null);
    }

    protected boolean isPresentationContext() {
        if (true) return false;// turned off for now
        if (parent == null)
            return false; // this means presentation nodes will report wrong sizes during restores...
        else
            return parent.isPresentationContext();
    }


    /**
     * Make sure this LWComponent has an ID -- will have an effect on
     * on any brand new LWComponent exactly once per VM instance.
     */
    protected void ensureID(LWComponent c)
    {
        if (c.getID() == null) {
            String id = getNextUniqueID();
            // no ID may be available if we're an orphan: it will be
            // patched up when we eventually get added to to a map
            if (id != null)
                c.setID(id);
        }

        for (LWComponent child : c.getChildList())
            ensureID(child);
    }

    protected String getNextUniqueID()
    {
        if (getParent() == null) {
            //throw new IllegalStateException("LWComponent has null parent; needs a parent instance subclassed from LWContainer that implements getNextUniqueID: " + this);
            if (DEBUG.PARENTING) tufts.Util.printStackTrace("getNextUniqueID: returning null for current orphan " + this);
            if (DEBUG.Enabled) out("getNextUniqueID: returning null for current orphan");
            return null;
        } else
            return getParent().getNextUniqueID();
    }

    public LWMap getMap() {
        if (this.parent == null)
            return null;
        else
            return this.parent.getMap();
    }
    
    public UserMapType getUserMapType() { throw new UnsupportedOperationException("deprecated"); }
    public boolean hasMetaData() { return false; }
    public String getMetaDataAsHTML() { return null; }
   
    
    /**
     * This sets the flag for the component so that it is either
     * hidden or visible based on a match to the active LWCFilter
     **/
    public void setFiltered(boolean filtered) {
    	isFiltered = filtered;
    }
    
    /**
     * @return true if should be hidden due to a currently applied filter, false if not
     **/
    public boolean isFiltered() {
    	return isFiltered;
    }

    /**
     * Called during restore from presistance, or when newly added to a container.
     * Must be called at some point before any attempt to persist, with a unique
     * identifier within the entire LWMap.  This is how components are referenced
     * in the persisted data.
     */
    public void setID(String ID)
    {
        if (this.ID != null)
            throw new IllegalStateException("Can't set ID to [" + ID + "], already set on " + this);
        //System.out.println("setID [" + ID + "] on " + this);
        this.ID = ID;

        // special case: if undo of add of any component that was brand new, this is
        // a new component creation, and to undo it is actually a delete.
        // UndoManager handles the hierarchy end of this, but we need this here
        // to differentiate hierarchy events that are just reparentings from
        // new creation events.

        notify(LWKey.Created, new Undoable() {
                void undo() {
                    // parent may already have deleted it for us, so only delete if need be
                    if (!isDeleted())
                        removeFromModel();
                }} );
    }
    
    public void setLabel(String label)
    {
        setLabel0(label, true);
    }


    /**
     * Called directly by TextBox after document edit with setDocument=false,
     * so we don't attempt to re-update the TextBox, which has just been
     * updated.
     */
    void setLabel0(String newLabel, boolean setDocument)
    {
        Object old = this.label;
        if (this.label == newLabel)
            return;
        if (this.label != null && this.label.equals(newLabel))
            return;
        if (newLabel == null || newLabel.length() == 0) {
            this.label = null;
            if (labelBox != null)
                labelBox.setText("");
        } else {
            this.label = newLabel;
            // todo opt: only need to do this if node or link (LWImage?)
            // Handle this more completely -- shouldn't need to create
            // label box at all -- why can't do entirely lazily?
            if (this.labelBox == null) {
                // figure out how to skip this:
                //getLabelBox();
            } else if (setDocument) {
                getLabelBox().setText(newLabel);
            }
        }
        layout();
        notify(LWKey.Label, old);
    }

    TextBox getLabelBox()
    {
        if (this.labelBox == null) {
            this.labelBox = new TextBox(this, this.label);
            // hack for LWLink label box hit detection:
            this.labelBox.setMapLocation(getCenterX() - labelBox.getMapWidth() / 2,
                                         getCenterY() - labelBox.getMapHeight() / 2);
            //layout();
        }
        return this.labelBox;
    }

    public void setNotes(String pNotes)
    {
        Object old = this.notes;
        if (pNotes == null) {
            this.notes = null;
        } else {
            String trimmed = pNotes.trim();
            if (trimmed.length() > 0)
                this.notes = pNotes;
            else
                this.notes = null;
        }
        layout();
        notify(LWKey.Notes, old);
    }

    /*
    public void setMetaData(String metaData)
    {
        this.metaData = metaData;
        layout();
        notify("meta-data");
    }
    // todo: setCategory still relevant?
    public void setCategory(String category)
    {
        this.category = category;
        layout();
        notify("category");
    }
    */
    /*
    public String getCategory()
    {
        return this.category;
    }
    */
    public void setResource(Resource resource)
    {
        if (DEBUG.CASTOR) out("SETTING RESOURCE TO " + resource.getClass() + " [" + resource + "]");
        Object old = this.resource;
        this.resource = resource;
        layout();
        if (DEBUG.CASTOR) out("NOTIFYING");
        notify(LWKey.Resource, old);
        
        /*
        try {
            layout();
        } catch (Exception e) {
            e.printStackTrace();
            if (DEBUG.CASTOR) System.exit(-1);
        }
        */
    }

    public void setResource(String urn)
    {
        if (urn == null || urn.length() == 0)
            setResource((Resource)null);
        else
            setResource(new MapResource(urn));
    }
 
    public Resource getResource()
    {
        return this.resource;
    }
    public String getID() {
        return this.ID;
    }
    
  /*  public String getStyledLabel()
    {
    	return this.label;
    	
    }*/
    public String getLabel() {
    	return this.label;
    	/*
    	if (this.label == null)
    		return null;
    	
    	String noHTMLString = this.label.replaceAll("\\<.*?\\>","");
    	noHTMLString = noHTMLString.replaceAll("\\&.*?\\;","");
    	noHTMLString = noHTMLString.replaceAll("\n","");
    	noHTMLString = noHTMLString.replaceAll("\\<!--.*?--\\>","");
    	noHTMLString = noHTMLString.replaceAll(" {2,}", " ").trim();
        
    	return noHTMLString;*/
    }
    /**
     * @return a label suitable for displaying in a list: if this component
     * has no label set, generate a unique name for it, and if the label has any newlines
     * in it, replace them with spaces.
     */
    public String getDisplayLabel() {
        if (getLabel() == null) {
            return getUniqueComponentTypeLabel();
        } else
            return getLabel().replace('\n', ' ');
    }
    
    String getDiagnosticLabel() {
        if (getLabel() == null) {
            return getUniqueComponentTypeLabel();
        } else
            return getUniqueComponentTypeLabel() + ": " + getLabel().replace('\n', ' ');
    }

    /** return a guaranteed unique name for this LWComponent */
    public String getUniqueComponentTypeLabel() {
        return getComponentTypeLabel() + " #" + getID();
    }
    
    /** return a type name for this LWComponent */
    public String getComponentTypeLabel() {
        String name = getClass().getName();
        if (name.startsWith("tufts.vue.LW"))
            name = name.substring(12);
        else if (name.startsWith("tufts.vue."))
            name = name.substring(10);
        return name;
    }

    String toName() {
        if (getLabel() == null)
            return getDisplayLabel();
        else
            return getComponentTypeLabel() + "[" + getLabel() + "]";
    }
    
    public void setNodeFilter(NodeFilter nodeFilter) {
        this.nodeFilter = nodeFilter;
    }
    
    public NodeFilter getNodeFilter() {
        //out(this + " getNodeFilter " + nodeFilter);
        return nodeFilter;
    }

    /** return null if the node filter is empty, so we don't bother with entry in the save file */
    public NodeFilter XMLnodeFilter() {
        if (nodeFilter != null && nodeFilter.size() < 1)
            return null;
        else
            return nodeFilter;
    }

    /** does this support a user editable label? */
    public boolean supportsUserLabel() {
        return false;
    }
    /** does this support user resizing? */
    // TODO: change these "supports" calls to an arbitrary property list
    // that could have arbitrary properties added to it by plugged-in non-standard tools
    public boolean supportsUserResize() {
        return false;
    }
    
    public boolean hasLabel()
    {
        return this.label != null;
    }
    
    public String getNotes()
    {
        return this.notes;
    }
    public boolean hasNotes()
    {
        return this.notes != null && this.notes.length() > 0;
    }
    public boolean hasResource()
    {
        return this.resource != null;
    }
    /*
    public String getMetaData()
    {
        return this.metaData;
    }
    public boolean hasMetaData()
    {
        return this.metaData != null;gajendracircle
    }
    */
    public boolean inPathway()
    {
        return pathwayRefs != null && pathwayRefs.size() > 0;
    }

    /** Is component in the given pathway? */
    public boolean inPathway(LWPathway path)
    {
        if (pathwayRefs == null || path == null)
            return false;

        Iterator i = pathwayRefs.iterator();
        while (i.hasNext()) {
            if (i.next() == path)
                return true;
        }
        return false;
    }
    
    /**
     * @return true if this component is in a pathway that is
     * drawn with decorations (e.g., not a reveal-way)
     */
    public boolean inDrawnPathway()
    {
        if (pathwayRefs == null)
            return false;

        for (LWPathway p : pathwayRefs)
            if (p.isVisible() && !p.isRevealer())
                return true;

        return false;
    }
    
    void addPathwayRef(LWPathway p)
    {
        if (pathwayRefs == null)
            pathwayRefs = new ArrayList();
        if (!pathwayRefs.contains(p)) {
            pathwayRefs.add(p);
            layout();
        }
        //notify("pathway.add");
    }
    void removePathwayRef(LWPathway p)
    {
        if (pathwayRefs == null) {
            if (DEBUG.META) tufts.Util.printStackTrace("attempt to remove non-existent pathwayRef to " + p + " in " + this);
            return;
        }
        pathwayRefs.remove(p);
        if (p.isRevealer()) // if was a revealer, make sure we're not left invisible if it had us hidden
            setVisible(true);
        layout();
        //notify("pathway.remove");
    }

    

    /** @deprecated - not really deprecated, but intended for persistance only */
    public java.awt.Dimension getXMLtextBox() {
        return null;
        // NOT CURRENTLY USED
        /*
        if (this.labelBox == null)
            return null;
        else
            return this.labelBox.getSize();
        */
    }
    
    /** @deprecated - not really deprecated, intended for persistance only */
    public void setXMLtextBox(java.awt.Dimension d) {
        this.textSize = d;
    }

    /** for persistance */
    // todo: move all this XML handling stuff to a special castor property mapper,
    // presumably in conjunction with re-architecting the whole mapping style &
    // save mechanism.
    public String getXMLlabel()
    {
        return this.label;
        //return tufts.Util.encodeUTF(this.label);
    }

    /** for persistance */
    public void setXMLlabel(String text)
    {
        setLabel(unEscapeNewlines(text));
        //this.label = unEscapeNewlines(text);
        //getLabelBox().setText(this.label);
        // we want to make sure layout() is not called, 
        // and currently there's no need to do notify's during init.
    }

    /** for persistance */
    public String getXMLnotes()
    {
        //return this.notes;
        // TODO: can escape newlines new with &#xa; and tab with &#x9;
        return escapeWhitespace(this.notes);
    }

    /** for persistance -- gets called by castor after it reads in XML */
    public void setXMLnotes(String text)
    {
        setNotes(decodeCastorMultiLineText(text));
    }

    protected static String decodeCastorMultiLineText(String text)
    {

        // If castor xml indent was on when save was done
        // (org.exolab.castor.indent=true in castor.properties
        // somewhere in the classpath, to make the XML more human
        // readable) it will break up elements like: <note>many chars
        // of text...</note> with newlines and whitespaces to indent
        // the new lines in the XML -- however, on reading them back
        // in, it puts this white space into the string you saved!  So
        // when we save we're sure to manually encode newlines and
        // runs of white space, so when we get here, if see any actual
        // newlines followed by runs of white space, we know to trash
        // them because it was castor formatting fluff.  (btw, this
        // isn't a problem for labels because they're XML attributes,
        // not elements, which are quoted).

        // Update: As of castor 0.9.7, this no longer appears true
        // (it doesn't indent new text lines with white space
        // even after wrapping them), but we still need this
        // here to deal with old save files.
        
        text = text.replaceAll("\n[ \t]*%nl;", "%nl;");
        text = text.replaceAll("\n[ \t]*", " ");
        return unEscapeWhitespace(text);
    }

    // FYI, this is no longer needed for castor XML attributes, as
    // of version 0.9.7 it automatically encodes & preserves them.
    // Note that this is still NOT true for XML elements.
    private static String escapeNewlines(String text)
    {
        if (text == null)
            return null;
        else {
            return text.replaceAll("[\n\r]", "%nl;");
        }
    }
    private static String unEscapeNewlines(String text)
    {
        if (text == null)
            return null;
        else { 
            return text.replaceAll("%nl;", "\n");
        }

    }
    private static String escapeWhitespace(String text)
    {
        if (text == null)
            return null;
        else {
            text = text.replaceAll("%", "%pct;");
            // replace all instances of two spaces with space+%sp;
            // to break them up (and thus we wont lose space runs)
            text = text.replaceAll("  ", " %sp;");
            text = text.replaceAll("\t", "%tab;");
            return escapeNewlines(text);
        }
    }
    private static String unEscapeWhitespace(String text)
    {
        if (text == null)
            return null;
        else { 
            text = unEscapeNewlines(text);
            text = text.replaceAll("%tab;", "\t");
            text = text.replaceAll("%sp;", " ");
            return text.replaceAll("%pct;", "%");
        }
    }
    
    /**
     * If this component supports special layout for it's children,
     * or resizes based on font, label, etc, do it here.
     */
    protected void layout() {
        if (mXMLRestoreUnderway == false)
            layout("default");
    }
    protected void layout(Object triggerKey) {}
    
    public String OLD_toString()
    {
        String s = getClass().getName() + "[id=" + getID();
        if (getLabel() != null)
            s += " \"" + getLabel() + "\"";
        s += "]";
        return s;
    }

    
    /** @return true: default is always autoSized */
    //public boolean isAutoSized() { return true; }
    public boolean isAutoSized() { return false; } // LAYOUT-NEW
    /** do nothing: default is always autoSized */
    public void setAutoSized(boolean t) {}
    
    private static boolean eq(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }
    
    public boolean isTransparent() {
        return mFillColor.isTransparent();
        //return fillColor == null || fillColor.getAlpha() == 0;
    }
    
    public boolean isTranslucent() {
        return mFillColor.isTranslucent();
        //return fillColor == null || fillColor.getAlpha() != 0xFF;
    }
    
    /** Color to use at draw time. LWNode overrides to provide darkening of children. */
    public Color getRenderFillColor() {
        return getFillColor();
    }
    void takeFillColor(Color color) {
        mFillColor.take(color);
        //this.fillColor = color;
    }

    // We still need these standard style setters & getters for backward compat
    // with all sorts of old code, and espcially for persistance (the castor
    // mapping, which refers to these methods)

    public float        getStrokeWidth()                { return mStrokeWidth.get(); }
    public void         setStrokeWidth(float w)         { mStrokeWidth.set(w); }
    
    public Color        getFillColor()                  { return mFillColor.get(); }
    public void         setFillColor(Color c)           { mFillColor.set(c); }
    public String       getXMLfillColor()               { return mFillColor.asString(); }
    public void         setXMLfillColor(String xml)     { mFillColor.setFromString(xml); }
    
    public Color        getTextColor()                  { return mTextColor.get(); }
    public void         setTextColor(Color c)           { mTextColor.set(c); }
    public String       getXMLtextColor()               { return mTextColor.asString(); }
    public void         setXMLtextColor(String xml)     { mTextColor.setFromString(xml); }
    
    public Color        getStrokeColor()                { return mStrokeColor.get(); }
    public void         setStrokeColor(Color c)         { mStrokeColor.set(c); }
    public String       getXMLstrokeColor()             { return mStrokeColor.asString(); }
    public void         setXMLstrokeColor(String xml)   { mStrokeColor.setFromString(xml); }
        
    public Font         getFont()               { return mFont.get(); }
    public void         setFont(Font font)      { mFont.set(font); }
    public String       getXMLfont()            { return mFont.asString(); }
    public void         setXMLfont(String xml)  { mFont.setFromString(xml); }



    /*========================================================================================
    public Color getFillColor()
    {
        return this.fillColor;
    }
    
    public void setFillColor(Color color)
    {
        if (eq(color, fillColor))
            return;
        Object old = this.fillColor;
        takeFillColor(color);
        notify(KEY_FillColor, old);
    }

    public String getXMLfillColor()
    {
        return ColorToString(getFillColor());
    }
    public void setXMLfillColor(String xml)
    {
        setFillColor(StringToColor(xml));
    }
    
    public Color getTextColor()
    {
        return this.textColor;
    }
    public void setTextColor(Color color)
    {
        if (eq(color, textColor))
            return;
        Object old = this.textColor;
        this.textColor = color;
        if (labelBox != null)
            labelBox.copyStyle(this); // todo better: handle thru style.textColor notification?
        notify(LWKey.TextColor, old);
    }
    public String getXMLtextColor()
    {
        return ColorToString(getTextColor());
    }
    public void setXMLtextColor(String xml)
    {
        setTextColor(StringToColor(xml));
    }
    public Color getStrokeColor()
    {
        return this.strokeColor;
    }
    public void setStrokeColor(Color color)
    {
        if (eq(color, strokeColor))
            return;
        Object old = this.strokeColor;
        this.strokeColor = color;
        notify(LWKey.StrokeColor, old);
    }
    public String getXMLstrokeColor()
    {
        return ColorToString(getStrokeColor());
    }
    public void setXMLstrokeColor(String xml)
    {
        setStrokeColor(StringToColor(xml));
    }
    //========================================================================================*/
    
    /*
    static String ColorToString(Color c)
    {
        if (c == null || (c.getRGB() & 0xFFFFFF) == 0)
            return null;
        
        //return "#" + Long.toHexString(c.getRGB() & 0xFFFFFFFF);
        return "#" + Integer.toHexString(c.getRGB() & 0xFFFFFF);
    }
    static Color StringToColor(String xml)
    {
        if (xml.trim().length() < 1)
            return null;
        
	Color c = null;
        try {
            c = VueResources.makeColor(xml);
            //Integer intval = Integer.decode(xml);
            //Long longval = Long.decode(xml); // transparency test -- works,just need gui
            //c = new Color(longval.intValue(), true);
            //c = new Color(intval.intValue());
        } catch (NumberFormatException e) {
            System.err.println("LWComponent.StringToColor[" + xml + "] " + e);
        }
        return c;
    }
    */
     	
    /*
    public float getStrokeWidth() {
        return this.strokeWidth;
    }
    void takeStrokeWidth(float w) {
        this.strokeWidth = w;
    }
    public void setStrokeWidth(float w)
    {
        if (this.strokeWidth != w) {
            float oldStrokeWidth = this.strokeWidth;
            takeStrokeWidth(w);
            if (w > 0)
                this.stroke = new BasicStroke(w, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
            else
                this.stroke = STROKE_ZERO;
            // todo: caching the stroke is kind of overkill
            if (getParent() != null) {
                // because stroke affects bounds-width, may need to re-layout parent
                getParent().layout();
            }
            layout();
            notify(LWKey.StrokeWidth, new Float(oldStrokeWidth));
        }
    }
    */
    /*    
    public void setFont(Font font)
    {
        if (eq(font, this.font))
            return;
        Object old = this.font;
        this.font = font;

        if (!fontIsRebuilding) {
            mFontSize.set(font.getSize());
            mFontStyle.set(font.getStyle());
            mFontName.set(font.getName());
        }
        
        if (labelBox != null)
            labelBox.copyStyle(this);
        layout(LWKey.Font);
        notify(LWKey.Font, old);
    }
    private boolean fontIsRebuilding; // hack till we cleanup the old font code
    private void rebuildFont() {
        // This so at least for now we have backward compat with the old font property (esp. for tools & persistance)
        fontIsRebuilding = true;
        try  {
            setFont(new Font(mFontName.get(), mFontStyle.get(), mFontSize.get()));
        } finally {
            fontIsRebuilding = false;
        }
    }
    public void setFontSize(int pointSize) {
        Font newFont = getFont().deriveFont((float)pointSize);
        setFont(newFont);
    }
    // to support XML persistance 
    public String getXMLfont()
    {
        //if (this.font == null || this.font == getParent().getFont())
        //return null;
        
	String strStyle;
	if (font.isBold()) {
	    strStyle = font.isItalic() ? "bolditalic" : "bold";
	} else {
	    strStyle = font.isItalic() ? "italic" : "plain";
	}
        return font.getName() + "-" + strStyle + "-" + font.getSize();
      
    }
    // to support XML persistance
    public void setXMLfont(String xml)
    {
        setFont(Font.decode(xml));
    }
    */
    
    
    
    /** default label X position impl: center the label in the bounding box */
    public float getLabelX()
    {
        //float x = getCenterX();
        if (hasLabel())
            return getLabelBox().getMapX();
        else if (labelBox != null)
            return getCenterX() - labelBox.getMapWidth() / 2;
        else
            return getCenterX();
        //  x -= (labelBox.getMapWidth() / 2) + 1;
        //return x;
    }
    /** default label Y position impl: center the label in the bounding box */
    public float getLabelY()
    {
        if (hasLabel())
            return getLabelBox().getMapY();
        else if (labelBox != null)
            return getCenterY() - labelBox.getMapHeight() / 2;
        else
            return getCenterY();
        
        //float y = getCenterY();
        //if (hasLabel())
        //  y -= labelBox.getMapHeight() / 2;
        //return y;
    }
    
    void setParent(LWContainer c)
    {
        //LWContainer old = this.parent;
        this.parent = c;
        //if (this.parent != null) notify("set-parent", new Undoable(old) { void undo() { setParent((LWContainer)old); }} );
    }

    public void setParentStyle(LWComponent parentStyle)
    {
        mParentStyle = parentStyle;
        parentStyle.isStyleParent = true;
        if (!mXMLRestoreUnderway)       // we can skip the copy during restore
            copyStyle(parentStyle);
    }

    /** for castor persist */
    public LWComponent getParentStyle() {
        return mParentStyle;
    }

    public boolean isStyleParent() {
        return isStyleParent;
    }
    public Boolean getPersistIsStyleParent() {
        return isStyleParent ? Boolean.TRUE : null;
    }
    public void setPersistIsStyleParent(Boolean b) {
        isStyleParent = b.booleanValue();
    }


    public LWContainer getParent() {
        return this.parent;
    }

    // TODO: implement layers -- this a stop-gap for hiding LWSlides
    public int getLayer() {
        if (this.parent == null) {
            //out("parent null, layer 0");
            return 0;
        } else {
            return this.parent.getLayer();
            //int l = this.parent.getLayer();
            //out("parent " + parent + " layer is " + l);
            //return l;
        }
    }

    public int getDepth() {
        if (parent == null)
            return 0;
        else
            return parent.getDepth() + 1;
    }
    

    /** return the component to be picked if we're picked: e.g., may return null if you only want children picked, and not the parent */
    protected LWComponent defaultPick(PickContext pc) {
        // If we're dropping something, never allow us to be picked
        // if we're a descendent of what's being dropped! (would be a parent/child loop)
        if (pc.dropping instanceof LWContainer && hasAncestor((LWContainer)pc.dropping))
            return null;
        else
            return this;
    }

    /** If PickContext.dropping is a LWComponent, return parent (as we can't take children),
     * otherwise return self
     */
    protected LWComponent defaultDropTarget(PickContext pc) {
        // TODO: if this is a system drag, dropping is null,
        // and we don't know if this is a localDrop of a node,
        // or a drop of a resource, so, for example, links
        // will incorrectly get targeted for local node system drops.
        // (tho when dropped, it'll still just get added to the parent).
        if (pc.dropping instanceof LWComponent)
            return getParent();
        else
            return this;
    }
    
    public boolean isOrphan() {
        return this.parent == null;
    }

    public boolean hasChildren() {
        return false;
    }

    public boolean hasChild(LWComponent c) {
        return false;
    }

    /** @return true - A single component always "has content" -- subclasses override to provide varying semantics */
    public boolean hasContent() {
        return true;
    }

    void addChild(LWComponent c) {
        throw new UnsupportedOperationException(this + ": can't take children. ignored=" + c);
    }

    void addChildren(java.util.List children) {
        throw new UnsupportedOperationException(this + ": can't take children. ignored=" + children);
    }

    /** return the currently active view */
    public LWComponent getView() {
        return this;
    }


    /** return true if this component is only a "virutal" member of the map:
     * It may report that it's parent is in the map, but that parent doesn't
     * list the component as a child (so it will never be drawn or traversed
     * when handling the entire map).
     */
    public boolean isMapVirtual() {
        return getParent() == null || !getParent().hasChild(this);
    }
    
    public java.util.List<LWComponent> getChildList()
    {
        return java.util.Collections.EMPTY_LIST;
    }
    
    public java.util.Iterator<LWComponent> getChildIterator() {
        return tufts.Util.EmptyIterator;
    }

    /** The default is to get all ChildKind.PROPER children (backward compatability)
     * This impl always returns an empty list.  Subclasses that can have proper
     * children provide the impl for that
     */
    public Collection<LWComponent> getAllDescendents() {
        // Default is only CHILD_PROPER, and by definition,
        // LWComponents have no proper children.
        // return getAllDescendents(CHILD_PROPER);
        return java.util.Collections.EMPTY_LIST;
    }    

    public Collection<LWComponent> getAllDescendents(final ChildKind kind) {
        if (kind == ChildKind.PROPER)
            return java.util.Collections.EMPTY_LIST;
        else
            return getAllDescendents(kind, new java.util.ArrayList());
    }
    
    public Collection<LWComponent> getAllDescendents(final ChildKind kind, final Collection<LWComponent> bag) {
        return bag;
    }
    

    /** for tracking who's linked to us */
    void addLinkRef(LWLink link)
    {
        if (DEBUG.EVENTS||DEBUG.UNDO) out(this + " adding link ref to " + link);
        if (this.links.contains(link))
            throw new IllegalStateException("addLinkRef: " + this + " already contains " + link);
        this.links.add(link);
        notify(LWKey.LinkAdded, link); // informational only event
    }
    /** for tracking who's linked to us */
    void removeLinkRef(LWLink link)
    {
        if (DEBUG.EVENTS||DEBUG.UNDO) out(this + " removing link ref to " + link);
        if (!this.links.remove(link))
            throw new IllegalStateException("removeLinkRef: " + this + " didn't contain " + link);
        notify(LWKey.LinkRemoved, link); // informational only event
    }
    
    /* tell us all the links who have us as one of their endpoints */
    java.util.List<LWLink> getLinkRefs() {
        return this.links;
    }
    
    /**
     * Return an iterator over all link endpoints,
     * which will all be instances of LWComponent.
     * If this is a LWLink, it should include it's
     * own endpoints in the list.
     */
    public java.util.Iterator getLinkEndpointsIterator()
    {
        return
            new java.util.Iterator() {
                java.util.Iterator i = getLinkRefs().iterator();
                public boolean hasNext() {return i.hasNext();}
		public Object next()
                {
                    LWLink l = (LWLink) i.next();
                    LWComponent c1 = l.getComponent1();
                    LWComponent c2 = l.getComponent2();
                    
                    // Every link, as it's connected to us, should have us as one of
                    // it's endpoints -- so return the opposite endpoint.  TODO: now
                    // that links can have null endpoints, this iterator can return null
                    // -- hasNext will have to get awfully fancy to handle this.
                    
                    if (c1 == LWComponent.this)
                        return c2;
                    else
                        return c1;
                }
		public void remove() {
		    throw new UnsupportedOperationException();
                }
            };
    }
    
    /**
     * Return all LWComponents connected via LWLinks to this object.
     * Included everything except LWLink objects themselves (unless
     * it's an endpoint -- a link to a link)
     *
     * todo opt: this is repaint optimization -- when links
     * eventually know their own bounds (they know real connection
     * endpoints) we can re-do this as getAllConnections(), which
     * will can return just the linkRefs and none of the endpoints)
     */
    /*
    public java.util.List getAllConnectedNodes()
    {
        java.util.List list = new java.util.ArrayList(this.links.size());
        java.util.Iterator i = this.links.iterator();
        while (i.hasNext()) {
            LWLink l = (LWLink) i.next();
            if (l.getComponent1() != this)
                list.add(l.getComponent1());
            else if (l.getComponent2() != this) // todo opt: remove extra check eventually
                list.add(l.getComponent2());
            else
                // todo: actually, I think we want to support these
                throw new IllegalStateException("link to self on " + this);
            
        }
        return list;
    }
    */
    
    /** include all links and far endpoints of links connected to this component */
    public java.util.List getAllConnectedComponents()
    {
        List list = new java.util.ArrayList(this.links.size());
        Iterator i = this.links.iterator();
        while (i.hasNext()) {
            LWLink l = (LWLink) i.next();
            list.add(l);
            if (l.getComponent1() != this)
                list.add(l.getComponent1());
            else if (l.getComponent2() != this) // todo opt: remove extra check eventually
                list.add(l.getComponent2());
            else
                // todo: actually, I think we want to support these
                throw new IllegalStateException("link to self on " + this);
            
        }
        return list;
    }
    
    /** include all non-null far endpoints of links connected to this component */
    public java.util.List getLinkedComponents()
    {
        List list = new java.util.ArrayList(getLinks().size());
        Iterator i = getLinks().iterator();
        while (i.hasNext()) {
            LWLink link = (LWLink) i.next();
            LWComponent c1 = link.getComponent1();
            LWComponent c2 = link.getComponent2();
            if (c1 != this) {
                if (c1 != null) list.add(c1);
            } else if (c2 != this) {
                if (c2 != null) list.add(c2);
            } else
                throw new IllegalStateException("link to self on " + this);
            
        }
        return list;
    }
    
    // todo: this same as getLinkRefs
    public List getLinks(){
        return this.links;
    }

    /** get all links to us + to any descendents */
    // TODO: return immutable versions
    public List getAllLinks() {
        return getLinks();
    }

    /*
      why was this here??
    public void setLinks(List links){
        this.links = links;
    }
    */

    public LWLink getLinkTo(LWComponent c)
    {
        java.util.Iterator i = this.links.iterator();
        while (i.hasNext()) {
            LWLink l = (LWLink) i.next();
            if (l.getComponent1() == c || l.getComponent2() == c)
                return l;
        }
        return null;
    }
    
    public boolean hasLinkTo(LWComponent c)
    {
        return getLinkTo(c) != null;
    }
    /** supports ensure link paint order code */
    protected  LWComponent getParentWithParent(LWContainer parent)
    {
        if (getParent() == parent)
            return this;
        if (getParent() == null)
            return null;
        return getParent().getParentWithParent(parent);
    }

    public boolean hasAncestor(LWContainer c) {
        LWComponent parent = getParent();
        if (parent == null)
            return false;
        else if (c == parent)
            return true;
        else
            return parent.hasAncestor(c);
    }

    void setScale(float scale)
    {
        if (this.scale == scale)
            return;
        if (DEBUG.LAYOUT) out("setScale " + scale);
        if (DEBUG.LAYOUT) tufts.Util.printClassTrace("tufts.vue", "setScale " + scale);
        this.scale = scale;
        //notify(LWKey.Scale); // todo: why do we need to notify if scale is changed? try removing this
        //System.out.println("Scale set to " + scale + " in " + this);
    }
    
    public float getScale()
    {
        //if (parent == null || isIndicated() || parent.isIndicated())
        //return this.rollover ? 1f : this.scale;
        return this.scale;
        //return 1f;
    }

    public Size getMinimumSize() {
        return MinSize;
    }
    
    /**
     * Tell all links that have us as an endpoint that we've
     * moved or resized so the link knows to recompute it's
     * connection points.
     */
    protected void updateConnectedLinks()
    {
        for (LWLink l : getLinkRefs()) 
            l.setEndpointMoved(true);
    }
    
    public void setFrame(Rectangle2D r)
    {
        setFrame((float)r.getX(), (float)r.getY(),
                 (float)r.getWidth(), (float)r.getHeight());
    }


    /**
     * Default impl just call's setSize, then setLocation.  You
     * may want to override if want to constrain in some way,
     * such as to underlying content (e.g., an image).
     */
    public void setFrame(float x, float y, float w, float h)
    {
        if (DEBUG.LAYOUT) out("*** setFrame " + x+","+y + " " + w+"x"+h);

        setSize(w, h);
        setLocation(x, y);

        /*
        Object old = new Rectangle2D.Float(this.x, this.y, getWidth(), getHeight());
        takeLocation(x, y);
        takeSize(w, h);
        updateConnectedLinks();
        notify(LWKey.Frame, old);
        */
    }

    /** default calls setFrame -- override to provide constraints */
    public void userSetFrame(float x, float y, float w, float h) {
        setFrame(x, y, w, h);
    }
        

    private boolean linkNotificationDisabled = false;
    protected void takeLocation(float x, float y) {
        if (DEBUG.LAYOUT) {
            out("takeLocation " + x + "," + y);
            //if (DEBUG.META) tufts.Util.printStackTrace("takeLocation");
        }
        this.x = x;
        this.y = y;
    }
    
    public void setLocation(float x, float y)
    {
        if (this.x == x && this.y == y)
            return;
        Object old = new Point2D.Float(this.x, this.y);
        takeLocation(x, y);
        if (!linkNotificationDisabled)
            updateConnectedLinks();
        notify(LWKey.Location, old);
        // todo: setX/getX should either handle undo or throw exception if used while not during restore
    }
    public void setLocation(double x, double y) {
        setLocation((float) x, (float) y);
    }
    public void setLocation(Point2D p) {
        setLocation((float) p.getX(), (float) p.getY());
    }

    /** default calls setLocation -- override to provide constraints */
    public void userSetLocation(float x, float y) {
        setLocation(x, y);
    }
    
    public void translate(float dx, float dy) {
        setLocation(this.x + dx,
                    this.y + dy);
    }

    public void setCenterAt(Point2D p) {
        setLocation((float) p.getX() - getWidth()/2,
                    (float) p.getY() - getHeight()/2);
    }

    // special case for mapviewer rollover zooming to skip calling updateConnectedLinks
    void setCenterAtQuietly(Point2D p)
    {
        linkNotificationDisabled = true;
        setCenterAt(p);
        linkNotificationDisabled = false;
    }
    
    public Point2D getLocation()
    {
        return new Point2D.Float(this.x, this.y);
    }
    public Point2D getCenterPoint()
    {
        return new Point2D.Float(getCenterX(), getCenterY());
    }
    
    /** set component to this many pixels in size, quietly, with no event notification */
    protected void takeSize(float w, float h)
    {
        //if (this.width == w && this.height == h)
        //return;
        if (DEBUG.LAYOUT) out("*** takeSize (LWC)  " + w + "x" + h);
        this.width = w;
        this.height = h;
    }

    float mAspect = 0;
    public void setAspect(float aspect) {
        mAspect = aspect;
    }
    
    /** set component to this many pixels in size */
    public void setSize(float w, float h)
    {
        if (this.width == w && this.height == h)
            return;
        if (DEBUG.LAYOUT||DEBUG.PRESENT) out("*** setSize  (LWC)  " + w + "x" + h);
        Size old = new Size(width, height);

        if (mAspect > 0) {

            // Given width & height are MINIMUM size: expand to keep aspect
            
            if (w <= 0) w = 1;
            if (h <= 0) h = 1;
            double tmpAspect = w / h; // aspect we would have if we did not constrain it
            // a = w / h
            // w = a*h
            // h = w/a
            if (DEBUG.PRESENT) {
                out("keepAspect=" + mAspect);
                out(" tmpAspect=" + tmpAspect);
            }
//             if (h == this.height) {
//                 out("case0");
//                 h = (float) (w / mAspect);
//             } else if (w == this.width) {
//                 out("case1");
//                 w = (float) (h * mAspect); 
//             } else
            if (tmpAspect > mAspect) {
                out("case2: expand height");
                h = (float) (w / mAspect);
            } else if (tmpAspect < mAspect) {
                out("case3: expand width");
                w = (float) (h * mAspect);
            }
            else
                if (DEBUG.PRESENT) out("NO ASPECT CHANGE");

            /*
            if (false) {
                if (h == this.height || tmpAspect < mAspect)
                    h = (float) (w / mAspect);
                else if (w == this.width || tmpAspect > mAspect)
                    w = (float) (h * mAspect);
            } else {
                if (tmpAspect < mAspect)
                    h = (float) (w / mAspect);
                else if (tmpAspect > mAspect)
                    w = (float) (h * mAspect);
            }
            */
                
        }
        if (w < MIN_SIZE) w = MIN_SIZE;
        if (h < MIN_SIZE) h = MIN_SIZE;
        takeSize(w, h);
        if (getParent() != null && !(getParent() instanceof LWMap))
            getParent().layout();
        updateConnectedLinks();
        if (!isAutoSized())
            notify(LWKey.Size, old); // todo perf: can we optimize this event out?
    }

    
    /** default calls setSize -- override to provide constraints */
    public void userSetSize(float w, float h) {
        setSize(w, h);
    }
        
    /** set on screen visible component size to this many pixels in size -- used for user set size from
     * GUI interaction -- takes into account any current scale factor
     */
    public void setAbsoluteSize(float w, float h)
    {
        if (DEBUG.LAYOUT) out("*** setAbsoluteSize " + w + "x" + h);
        setSize(w / getScale(), h / getScale());
    }

    public float getX() { return this.x; }
    public float getY() { return this.y; }
    /** for XML restore only --todo: remove*/
    public void setX(float x) { this.x = x; }
    /** for XML restore only! --todo remove*/
    public void setY(float y) { this.y = y; }
    public float getWidth() { return this.width * getScale(); }
    public float getHeight() { return this.height * getScale(); }
    //public float getWidth() { return this.width; }
    //public float getHeight() { return this.height; }
    //public float getBoundsWidth() { return (this.width + this.strokeWidth);  }
    //public float getBoundsHeight() { return (this.height + this.strokeWidth); }
    //public float getBoundsWidth() { return (this.width + this.strokeWidth) * getScale(); }
    //public float getBoundsHeight() { return (this.height + this.strokeWidth) * getScale(); }
    public float getBoundsWidth() { return (this.width + mStrokeWidth.get()) * getScale(); }
    public float getBoundsHeight() { return (this.height + mStrokeWidth.get()) * getScale(); }
    public float getCenterX() { return this.x + getWidth() / 2; }
    public float getCenterY() { return this.y + getHeight() / 2; }

    public float getAbsoluteWidth() { return this.width; }
    public float getAbsoluteHeight() { return this.height; }


    // these 2 for persistance ONLY -- they don't deliver detectable events!
    /** for persistance ONLY */
    public void setAbsoluteWidth(float w) { this.width = w; }
    /** for persistance ONLY */
    public void setAbsoluteHeight(float h) { this.height = h; }
    
    /** return border shape of this object, with it's location in map coordinates  */
    public Shape getShape()
    {
        return getShapeBounds();
    }
    /*
    public void setShape(Shape shape)
    {
        throw new UnsupportedOperationException("unimplemented setShape in " + this);
    }
    */

    public boolean doesRelativeDrawing() { return false; }    

    /**
     * Return bounds for hit detection & clipping.  This will vary
     * depenending on current stroke width, if in a visible pathway,
     * etc.
     */
    public Rectangle2D getBounds()
    {
        // todo opt: cache this object?
        final Rectangle2D.Float b = new Rectangle2D.Float(this.x, this.y, getWidth(), getHeight());
        float strokeWidth = getStrokeWidth();

        // we need this adjustment for repaint optimzation to
        // work properly -- would be a bit cleaner to compensate
        // for this in the viewer
        //if (isIndicated() && STROKE_INDICATION.getLineWidth() > strokeWidth)
        //    strokeWidth = STROKE_INDICATION.getLineWidth();

        if (inDrawnPathway())
            strokeWidth += LWPathway.PathwayStrokeWidth;

        if (strokeWidth > 0) {
            final float adj = strokeWidth / 2;
            b.x -= adj;
            b.y -= adj;
            b.width += strokeWidth;
            b.height += strokeWidth;
        }
        return b;
    }

    /**
     * Return internal bounds of the border shape, not including
     * the width of any stroked border.
     */
    public Rectangle2D getShapeBounds()
    {
        // todo opt: cache this object?
        //return new Rectangle2D.Float(this.x, this.y, getAbsoluteWidth(), getAbsoluteHeight());
        return new Rectangle2D.Float(this.x, this.y, getWidth(), getHeight());
    }
    
    /**
     * Default implementation: checks bounding box
     * Subclasses should override and compute via shape.
     */
    public boolean contains(float x, float y)
    {
        return x >= this.x && x <= (this.x+getWidth())
            && y >= this.y && y <= (this.y+getHeight());
    }
    
    /**
     * Default implementation: returns false;
     * For "do-what-I-mean" hit detection, when all the more strict contains calls failed.
     */
    public boolean looseContains(float x, float y)
    {
        return false;
    }
    
    /**
     * Default implementation: checks bounding box
     * Subclasses should override and compute via shape.
     */
    public boolean intersects(Rectangle2D rect)
    {
        return rect.intersects(getBounds());
    }
    
    /**
     * Does x,y fall within the selection target for this component.
     * This default impl adds a 30 pixel swath to bounding box.
     */
    public boolean targetContains(float x, float y)
    {
        final int swath = 30; // todo: preference
        float sx = this.x - swath;
        float sy = this.y - swath;
        float ex = this.x + getWidth() + swath;
        float ey = this.y + getHeight() + swath;
        
        return x >= sx && x <= ex && y >= sy && y <= ey;
    }

    /**
     * We divide area around the bounding box into 8 regions -- directly
     * above/below/left/right can compute distance to nearest edge
     * with a single subtract.  For the other regions out at the
     * corners, do a distance calculation to the nearest corner.
     * Behaviour undefined if x,y are within component bounds.
     */
    public float distanceToEdgeSq(float x, float y)
    {
        float ex = this.x + getWidth();
        float ey = this.y + getHeight();

        if (x >= this.x && x <= ex) {
            // we're directly above or below this component
            return y < this.y ? this.y - y : y - ey;
        } else if (y >= this.y && y <= ey) {
            // we're directly to the left or right of this component
            return x < this.x ? this.x - x : x - ex;
        } else {
            // This computation only makes sense following the above
            // code -- we already know we must be closest to a corner
            // if we're down here.
            float nearCornerX = x > ex ? ex : this.x;
            float nearCornerY = y > ey ? ey : this.y;
            float dx = nearCornerX - x;
            float dy = nearCornerY - y;
            return dx*dx + dy*dy;
        }
    }

    public Point2D nearestPoint(float x, float y)
    {
        float ex = this.x + getWidth();
        float ey = this.y + getHeight();
        Point2D.Float p = new Point2D.Float(x, y);

        if (x >= this.x && x <= ex) {
            // we're directly above or below this component
            if (y < this.y)
                p.y = this.y;
            else
                p.y = ey;
        } else if (y >= this.y && y <= ey) {
            // we're directly to the left or right of this component
            if (x < this.x)
                p.x = this.x;
            else
                p.x = ex;
        } else {
            // This computation only makes sense following the above
            // code -- we already know we must be closest to a corner
            // if we're down here.
            float nearCornerX = x > ex ? ex : this.x;
            float nearCornerY = y > ey ? ey : this.y;
            p.x = nearCornerX;
            p.y = nearCornerY;
        }
        return p;
    }

    public float distanceToEdge(float x, float y)
    {
        return (float) Math.sqrt(distanceToEdgeSq(x, y));
    }

    
    /**
     * Return the square of the distance from x,y to the center of
     * this components bounding box.
     */
    public float distanceToCenterSq(float x, float y)
    {
        float cx = getCenterX();
        float cy = getCenterY();
        float dx = cx - x;
        float dy = cy - y;
        return dx*dx + dy*dy;
    }
    
    public float distanceToCenter(float x, float y)
    {
        return (float) Math.sqrt(distanceToCenterSq(x, y));
    }
    
    public void drawPathwayDecorations(DrawContext dc)
    {
        if (pathwayRefs == null)
            return;
        
        for (LWPathway path : pathwayRefs) {
            if (!dc.isFocused && path.isDrawn()) {
                path.drawComponentDecorations(new DrawContext(dc), this);
            }
        }
        
    }

    /** if this component is selected and we're not printing, draw a selection indicator */
    // todo: drawing of selection should be handled by the MapViewer and/or the currently
    // active tool -- not in the component code
    protected void drawSelectionDecorations(DrawContext dc) {
        if (isSelected() && dc.isInteractive()) {
            LWPathway p = VUE.getActivePathway();
            if (p != null && p.isVisible() && p.getCurrentNode() == this) {
                // SPECIAL CASE:
                // as the current element on the current pathway draws a huge
                // semi-transparent stroke around it, skip drawing our fat 
                // transparent selection stroke on this node.  So we just
                // do nothing here.
            } else {
                dc.g.setColor(COLOR_HIGHLIGHT);
                dc.g.setStroke(new BasicStroke(getStrokeWidth() + SelectionStrokeWidth));
                dc.g.draw(getShape());
            }
        }
    }

    public void draw(DrawContext dc)
    {
        if (dc.drawPathways())
            drawPathwayDecorations(dc);
    }

    protected LWChangeSupport getChangeSupport() {
        return mChangeSupport;
    }
    public synchronized void addLWCListener(Listener listener) {
        mChangeSupport.addListener(listener, null);
    }

    public synchronized void addLWCListener(Listener listener, LWComponent.Key singleEventKey) {
        mChangeSupport.addListener(listener, singleEventKey);
    }
    /** @param eventMask is a string constant (from LWKey) or an array of such. If one
     of these non-null values, only events matching those keys will be delievered */
    public synchronized void addLWCListener(Listener listener, Object... eventsDesired) {
        mChangeSupport.addListener(listener, eventsDesired);
    }
    public synchronized void removeLWCListener(Listener listener) {
        mChangeSupport.removeListener(listener);
    }
    public synchronized void removeAllLWCListeners() {
        mChangeSupport.removeAllListeners();
    }

    protected synchronized void notifyLWCListeners(LWCEvent e)
    {
        //if (e.key.isSignal || e.key == LWKey.Location && e.source == this) {
        if (e.key == LWKey.UserActionCompleted || e.key == LWKey.Location && e.source == this) {
            // only keep if the location event is on us:
            // if this is our child that moved, obviously
            // clear the cache (we look different)
            //out("*** KEEPING IMAGE CACHE ***");
            ; // keep the cached image
        } else {
            //out("*** CLEARING IMAGE CACHE");
            mCachedImage = null;
        }
        mChangeSupport.notifyListeners(this, e);

        if (isStyleParent)
            updateStyleWatchers(e);
        
    }

    /** If the event is a change for a style property, apply the change to all
        LWComponents that refer to us as their style parent */
    protected void updateStyleWatchers(LWCEvent e)
    {
        if ((e.key instanceof Key) == false || getParent() == null) {
            // This only works with real Key's, and if parent is null,
            // we're still initializing.
            return;
        }

        final Key key = (Key) e.key;

        if (!key.isStyleProperty) {
            // nothing to do if this isn't a style property that's changing
            return;
        }

        // Now we know a styled property is changing.  Since they Key itself
        // knows how to get/set/copy values, we can now just find all the
        // components "listening" to this style (pointing to it), and copy over
        // the value that just changed on the style object.
        
        out("STYLE OBJECT UPDATING STYLED CHILDREN with " + key);
        //final LWPathway path = ((MasterSlide)getParent()).mOwner;
        
        // We can traverse all objects in the system, looking for folks who
        // point to us.  But once slides are owned by the pathway, we'll have a
        // list of all slides here from the pathway, and we can just traverse
        // those and check for updates amongst the children, as we happen
        // to know that this style object only applies to slides
        // (as opposed to ontology style objects)
        
        // todo: this not a fast way to traverse & find what we need to change...
        for (LWComponent dest : getMap().getAllDescendents(ChildKind.ANY)) {
            // we should never be point back to ourself, but we check just in case
            if (dest.mParentStyle == this && dest.supportsProperty(key) && dest != this) {
                // Only copy over the style value if was previously set to our existing style value
                try {
                    if (key.valueEquals(dest, e.getOldValue()))
                        key.copyValue(this, dest);
                } catch (Throwable t) {
                    tufts.Util.printStackTrace(t, "Failed to copy value from " + e + " old=" + e.getOldValue());
                }
            }
        }
    }
    
    
    /**
     * A third party can ask this object to raise an event
     * on behalf of the source.
     */
    void notify(Object source, String what)
    {
        notifyLWCListeners(new LWCEvent(source, this, what));
    }

    void notifyProxy(LWCEvent e) {
        notifyLWCListeners(e);
    }

    protected void notify(String what, LWComponent contents)
    {
        notifyLWCListeners(new LWCEvent(this, contents, what));
    }

    protected void notify(String what, Object oldValue)
    {
        notifyLWCListeners(new LWCEvent(this, this, what, oldValue));
    }

    protected void notify(Key key, Object oldValue)
    {
        notifyLWCListeners(new LWCEvent(this, this, key, oldValue));
    }

    protected void notify(String what)
    {
        // todo: we still need both src & component? (this,this)
        notifyLWCListeners(new LWCEvent(this, this, what, LWCEvent.NO_OLD_VALUE));
    }
    
    /**a notify with an array of components
       added by Daisuke Fujiwara
     */
    protected void notify(String what, ArrayList componentList)
    {
        notifyLWCListeners(new LWCEvent(this, componentList, what));
    }

    /**
     * Do final cleanup needed now that this LWComponent has
     * been removed from the model.  Calling this on an already
     * deleted LWComponent has no effect.
     */
    protected void removeFromModel()
    {
        if (isDeleted()) {
            if (DEBUG.PARENTING||DEBUG.EVENTS) out(this + " removeFromModel(lwc): ignoring (already removed)");
            return;
        }
        if (DEBUG.PARENTING||DEBUG.EVENTS) out(this + " removeFromModel(lwc)");
        //throw new IllegalStateException(this + ": attempt to delete already deleted");
        notify(LWKey.Deleting);
        prepareToRemoveFromModel();
        removeAllLWCListeners();
        disconnectFromLinks();
        setDeleted(true);
    }

    /**
     * For subclasses to override that need to do cleanup
     * activity before the the default LWComponent removeFromModel
     * cleanup runs.
     */
    protected void prepareToRemoveFromModel() { }

    /** undelete */
    protected void restoreToModel()
    {
        if (DEBUG.PARENTING||DEBUG.EVENTS) out(this + " restoreToModel");
        if (!isDeleted()) {
            throw new IllegalStateException("Attempt to restore already restored: " + this);
            //out("FYI: already restored: " + this);
            //return;
        }
        // There is no reconnectToLinks: link endpoint connect events handle this.
        // We couldn't do it here anyway as we wouldn't know which of the two endpoint to connect us to.
        setDeleted(false);
    }

    public boolean isDeleted() {
        return this.scale == -1;
    }
    
    private void setDeleted(boolean deleted) {
        if (deleted) {
            this.scale = -1;
            if (DEBUG.PARENTING||DEBUG.UNDO||DEBUG.EVENTS)
                if (parent != null) out(this + " parent not yet null in setDeleted true (ok for undo of creates)");
            this.parent = null;
        } else
            this.scale = 1;
    }

    private void disconnectFromLinks()
    {
        Object[] links = this.links.toArray(); // may be modified concurrently
        for (int i = 0; i < links.length; i++) {
            LWLink l = (LWLink) links[i];
            l.disconnectFrom(this);
        }
     }
    
    public void setSelected(boolean selected)
    {
        this.selected = selected;
    }
    public boolean isSelected()
    {
        return this.selected;
    }
    
    public void setHidden(boolean hidden)
    {
        if (this.hidden != hidden) {
            Object oldValue = hidden ? Boolean.TRUE : Boolean.FALSE;
            this.hidden = hidden;
            notify(LWKey.Hidden, oldValue);
        }
    }

    public Boolean getXMLhidden() {
        return hidden ? Boolean.TRUE : null;
    }
    public void setXMLhidden(Boolean b) {
        setHidden(b.booleanValue());
    }
    
    /**
     * @return true if this component has been hidden.  Note that this
     * is different from isFiltered.  All children of a hidden component
     * are also hidden, but not all children of a filtered component
     * are filtered.
     */
    // TODO: can create a bit-set for hidden reasons: e.g.,
    // FILTERED, PRUNED, NOT_ON_PATHWAY, etc, so if field
    // is non-zero, it's hidden.
    public boolean isHidden()
    {
        return this.hidden;
    }
    /*
    public boolean isLocked()
    {
        return this.locked;
    }
    public void setLocked(boolean locked)
    {
        this.locked = locked;
    }
    */
    public void setVisible(boolean visible)
    {
        setHidden(!visible);
    }
    public boolean isVisible()
    {
        return !isHidden();
    }
    public boolean isDrawn() {
        //return !hidden && !isFiltered;
        return !isHidden() && !isFiltered();
    }
    public void setRollover(boolean tv)
    {
        if (this.rollover != tv) {
            this.rollover = tv;
        }
    }

    public void setZoomedFocus(boolean tv)
    {
        throw new UnsupportedOperationException();
        /*
        if (this.isZoomedFocus != tv) {
            this.isZoomedFocus = tv;
        }
        if (getParent() != null) {
            getParent().setFocusComponent(tv ? this : null);
        }
        */
    }

    public boolean isZoomedFocus()
    {
        return isZoomedFocus;
    }
    
    /*
    public void setIndicated(boolean indicated)
    {
        if (this.indicated != indicated) {
            this.indicated = indicated;
        }
    }
    public boolean isIndicated() { return this.indicated; }
    */
    
    public boolean isRollover()
    {
        return this.rollover;
    }

    /*
    public LWComponent findDeepestChildAt(float mapX, float mapY, LWComponent excluded, boolean ignoreSelected)
    {
        if (ignoreSelected && isSelected())
            return null;
        else
            return excluded == this ? null : this;
    }


    /** This only to be called once we  already know mapX / mapY are within this component 
    protected LWComponent findChildAt(float mapX, float mapY)
    {
        return isFiltered() ? null : this;
    }
    */


    public void mouseEntered(MapMouseEvent e)
    {
        if (DEBUG.ROLLOVER) System.out.println("MouseEntered:     " + this);
        //e.getViewer().setIndicated(this);
        mouseOver(e);
    }
    public void mouseMoved(MapMouseEvent e)
    {
        //System.out.println("MouseMoved " + this);
        mouseOver(e);
    }
    public void mouseOver(MapMouseEvent e)
    {
        //System.out.println("MouseOver " + this);
    }
    public void mouseExited(MapMouseEvent e)
    {
        if (DEBUG.ROLLOVER) System.out.println(" MouseExited:     " + this);
        //e.getViewer().clearIndicated();
    }

    /** pre-digested single-click
     * @return true if you do anything with it, otherwise
     * the viewer can/will provide default action.
     */
    public boolean handleSingleClick(MapMouseEvent e)
    {
        return false;
    }
    
    /** pre-digested double-click
     * @return true if you do anything with it, otherwise
     * the viewer can/will provide default action.
     * Default action: if we have a resource, launch
     * it in a browser, otherwise, do nothing.
     */
    public boolean handleDoubleClick(MapMouseEvent e)
    {
        if (hasResource()) {
            getResource().displayContent();
            return true;
        } else
            return false;
    }

    /** pesistance default */
    public void addObject(Object obj)
    {
        System.err.println("Unhandled XML obj: " + obj);
    }


    /** subclasses override this to add info to toString()
     (return super.paramString() + new info) */
    public String paramString()
    {
        return String.format(" %.0f,%.0f %.0fx%.0f", getX(), getY(), width, height);
        //return
            //" " + VueUtil.oneDigitDecimal(getX())+","+VueUtil.oneDigitDecimal(getY()) + " " +
            //VueUtil.oneDigitDecimal(width) + "x" + VueUtil.oneDigitDecimal(height);
    }

    protected void out(String s) {
        if (DEBUG.THREAD) {
            String thread = Thread.currentThread().toString().substring(6);
            System.err.println(thread + " " + this + " " + s);
        } else {
            System.err.println(this + " " + s);
        }
    }

    protected void outf(String format, Object ... args) {
	System.err.format(format + "\n", args);
    }
    
    /*
    static protected void out(Object o) {
        System.out.println((o==null?"null":o.toString()));
    }
    */
/*
    static protected void out(String s) {
        System.out.println(s);
    }
*/

    /** interface {@link XMLUnmarshalListener} -- does nothing here */
    public void XML_initialized() {
        mXMLRestoreUnderway = true;
    }
    
    public void XML_fieldAdded(String name, Object child) {
        if (DEBUG.XML) out("XML_fieldAdded <" + name + "> = " + child);
    }

    /** interface {@link XMLUnmarshalListener} */
    public void XML_addNotify(String name, Object parent) {
        if (DEBUG.XML) tufts.Util.printClassTrace("tufts.vue", "XML_addNotify; name=" + name
                                                  + "\n\tparent: " + parent
                                                  + "\n\t child: " + this
                                                  + "\n");

        // TODO: moving this layout from old position at end of LWMap.completeXMLRestore
        // to here may have unpredictable results... watch of bad states after restores.
        // The advantage of doing it here is that virtual children are handled,
        // and "off map" children, such as slide children are properly handled.
        //layout("XML_addNotify"); 
    }

    /** interface {@link XMLUnmarshalListener} -- call's layout */
    public void XML_completed() {
        mXMLRestoreUnderway = false;

        /*
        // TODO: TEMPORARY DEBUG: never restore slides as format changes at moment
        //mSlides.clear();
        for (LWSlide slide : mSlides.values()) {
            // slides are virtual children of the node: we're their
            // parent, tho they're not formal children of ours.
            slide.setParent((LWContainer)this);
            // TODO: currently, this means non-container objects, such as LWImages,
            // can't have slides -- prob good to remove that restriction.
            // What would break if the parent ref were just a LWComponent?
        }
        */
        
        if (DEBUG.XML) System.out.println("XML_completed " + this);
        //layout(); need to wait till scale values are all set: so the LWMap needs to trigger this
    }


    public BufferedImage getAsImage(double alpha, java.awt.Dimension maxSize) {
        if (mCachedImage == null || mCachedImageAlpha != alpha || mCachedImageMaxSize != maxSize) {
            // todo opt: mCachedImage should be a SoftReference
            mCachedImage = createImage(alpha, maxSize);
            mCachedImageAlpha = alpha;
            mCachedImageMaxSize = maxSize;
        }
        return mCachedImage;
    }
    
    public BufferedImage getAsImage() {
        return getAsImage(1.0, null);
    }

    public BufferedImage createImage(double alpha, java.awt.Dimension maxSize) {
        return createImage(alpha, maxSize, null);
    }
    
    /**
     * Create a new buffered image, of max dimension maxSize, and render the LWComponent
     * (and all it's children), to it using the given alpha.
     * @param alpha 0.0 (invisible) to 1.0 (no alpha)
     * @param maxSize max dimensions for image. May be null.  Image may be smaller than maxSize.
     * @param fillColor -- if non-null, will be rendered as background for image.  If alpha is
     * also set, background fill will have transparency of alpha^3 to enhance contrast.
     */
    public BufferedImage createImage(double alpha, java.awt.Dimension maxSize, Color fillColor)
    {
        //tufts.Util.printStackTrace("CREATE IMAGE");
        if (DEBUG.IMAGE) out("createImage; MAX size " + maxSize);
        Rectangle2D.Float bounds = (Rectangle2D.Float) getBounds();

        final boolean drawBorder = this instanceof LWMap && alpha != 1.0;

        bounds.width += 1;
        bounds.height += 1;

        if (drawBorder) {
            bounds.x--;
            bounds.y--;
            bounds.width += 2;
            bounds.height += 2;
        }
            
        if (DEBUG.IMAGE) out("createImage; natural bounds " + bounds);
        
        int width = (int) Math.ceil(bounds.width);
        int height = (int) Math.ceil(bounds.height);
        double zoom = 1.0;

        if (maxSize != null) {
            
            if (width > maxSize.width || height > maxSize.height) {
                zoom = ZoomTool.computeZoomFit(maxSize, 0, bounds, null);
                width = (int) Math.ceil(bounds.width * zoom);
                height = (int) Math.ceil(bounds.height * zoom);
            }
        }
        

        final int imageType;

        if (alpha == 1.0 && fillColor != null)
            imageType = BufferedImage.TYPE_INT_RGB;
        else
            imageType = BufferedImage.TYPE_INT_ARGB;
        
        if (DEBUG.IMAGE) out("createImage; final size " + width + "x" + height);

        BufferedImage image = new BufferedImage(width, height, imageType);

        drawImage((java.awt.Graphics2D) image.getGraphics(),
                  alpha,
                  maxSize,
                  fillColor);


        if (DEBUG.DND || DEBUG.IMAGE) out("created image: " + image);

        return image;
    }

    /**
     * Useful for drawing drag images into an existing graphics buffer, or drawing exportable images.
     *
     * @param alpha 0.0 (invisible) to 1.0 (no alpha)
     * @param maxSize max dimensions for image. May be null.  Image may be smaller than maxSize.
     * @param fillColor -- if non-null, will be rendered as background for image.  If alpha is
     * also set, background fill will have transparency of alpha^3 to enhance contrast.
     */

    public void drawImage(java.awt.Graphics2D g, double alpha, java.awt.Dimension maxSize, Color fillColor)
    {
        if (DEBUG.IMAGE) out("drawImage; size " + maxSize);
        Rectangle2D.Float bounds = (Rectangle2D.Float) getBounds();

        final boolean drawBorder = this instanceof LWMap && alpha != 1.0;

        bounds.width += 1;
        bounds.height += 1;

        if (drawBorder) {
            bounds.x--;
            bounds.y--;
            bounds.width += 2;
            bounds.height += 2;
        }
            
        if (DEBUG.IMAGE) out("drawImage; natural bounds " + bounds);
        
        int width = (int) Math.ceil(bounds.width);
        int height = (int) Math.ceil(bounds.height);
        double zoom = 1.0;

        if (maxSize != null) {
            // Shrink to fit maxSize, but don't expand to fill it.
            if (width > maxSize.width || height > maxSize.height) {
                zoom = ZoomTool.computeZoomFit(maxSize, 0, bounds, null);
                width = (int) Math.ceil(bounds.width * zoom);
                height = (int) Math.ceil(bounds.height * zoom);
            }
        }
        

        /*if (c instanceof LWGroup && ((LWGroup)c).numChildren() > 1) {
            g.setColor(new Color(255,255,255,32)); // give a bit of background
            //g.fillRect(0, 0, width, height);
            }*/

        DrawContext dc = new DrawContext(g);
        dc.setAlpha(alpha, java.awt.AlphaComposite.SRC); // erase any underlying
        
        if (fillColor != null) {
            if (alpha != 1.0) {
                Color c = fillColor;
                // if we have an alpha and a fill, amplify the alpha on the background fill
                // by changing the fill to one that has alpha*alpha, for a total of
                // alpha*alpha*alpha given our GC already has an alpha set.
                fillColor = new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (alpha*alpha*255+0.5));
            }
            g.setColor(fillColor);
            g.fillRect(0, 0, width, height);
        }

        dc.setAntiAlias(true);
            
        if (drawBorder) {
            g.setColor(Color.darkGray);
            g.drawRect(0, 0, width-1, height-1);
        }

        if (zoom != 1.0)
            dc.g.scale(zoom, zoom);
                
        // translate so that we're un the upper-left of the GC
        g.translate(-(int)Math.floor(bounds.getX()),
                    -(int)Math.floor(bounds.getY()));

        // GC *must* have a bounds set or we get NPE's in JComponent (textBox) rendering
        g.setClip(bounds);
            

        // render to the image through the DrawContext/GC pointing to it
        draw(dc);
    }
    

    
    public String toString()
    {
        String cname = getClass().getName();
        String s = cname.substring(cname.lastIndexOf('.')+1);
        s += "[";
        if (getID() == null)
            s += tufts.Util.pad(9, Integer.toHexString(hashCode()));
        else
            s += tufts.Util.pad(3, getID());
        if (getLabel() != null) {
            if (isAutoSized())
                s += "\"" + getDisplayLabel() + "\" ";
            else
                s += "(" + getDisplayLabel() + ") ";
        }
        //if (getScale() != 1f) s += "z" + getScale() + " ";
        if (this.scale != 1f) s += "z" + this.scale + " ";
        s += paramString() + " ";
        if (getResource() != null)
            s += getResource();
        //s += " <" + getResource() + ">";
        s += "]";
        return s;
    }



    public static void main(String args[]) throws Exception {

        for (java.lang.reflect.Field f : LWComponent.class.getDeclaredFields()) {
            Class type = f.getType();
            if (type == Key.class)
                System.out.println("KEY: " + f);
            else
                System.out.println("Field: " + f + " (" + type + ")");
        }
    }
    

    
}



        /*
        private final java.lang.reflect.Field field;
        public Key(String name, String fieldName) {
            this(name);

            // this successfully auto-generates the slot reference, tho not really worth
            // it, as requiring the extra code snippet for grabbing the slot (Property)
            // object at least eliminates any typo's.  If we were to bother with this,
            // we'd want to generate a Field ref to an actual member field that had the
            // real value, and wasn't a slot.  Then the renderers, etc, could get
            // directly at the real value without using the slot -- a tad faster.  Then
            // stuff like the auto-notify code would all need to happen in the key, tho
            // then all our "traditional" setters (for hand-coding convenience, and at
            // least for save file backward compat) would need to use the Key to do the
            // setting for the appropriate triggers (except for "take" usage)
            
            java.lang.reflect.Field f = null;
            if (fieldName != null) {
                try {
                    f = LWComponent.class.getField(fieldName);
                    System.out.println("Found field: " + f);
                } catch (Throwable t) {
                    tufts.Util.printStackTrace(t);
                }
            }
            field = f;
        }
        Property getSlot(LWComponent c) {
            try {
                return (Property) field.get(c);
            } catch (Throwable t) {
                tufts.Util.printStackTrace(t);
            }
            return null;
        }
        */
        /*
        Object getValue(LWComponent c) {
            if (field == null)
                return getSlot(c).get();

            try {
                return field.get(c);
            } catch (Throwable t) {
                tufts.Util.printStackTrace(t);
            }
            return null;
        }
        void setValue(LWComponent c, Object value) {
            if (field == null)
                getSlot(c).set(value);
            
            try {
                field.set(c, value);
            } catch (Throwable t) {
                tufts.Util.printStackTrace(t);
            }
        }
        */
        



