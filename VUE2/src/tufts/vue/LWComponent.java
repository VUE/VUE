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

import tufts.Util;
import static tufts.Util.*;

import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.Color;
import java.awt.Font;
import java.awt.Stroke;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.AlphaComposite;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

import java.util.*;
import java.net.*;
//import tufts.vue.beans.UserMapType; // remove: old SB stuff we never used
import tufts.vue.filter.*;

import edu.tufts.vue.metadata.MetadataList;

import edu.tufts.vue.style.Style;

import edu.tufts.vue.preferences.implementations.BooleanPreference;
import edu.tufts.vue.preferences.interfaces.VuePreference;    

/**
 * VUE base class for all components to be rendered and edited in the MapViewer.
 *
 * @version $Revision: 1.305 $ / $Date: 2007-07-17 22:53:56 $ / $Author: sfraize $
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

    public enum ChildKind {
        /** the default, conceptually significant chilren */
        PROPER,
            
        /** VISIBLE is PROPER + Currently visible */
        VISIBLE,

        /** Above, plus include ANY children, such as slides and their children -- the only
         * way to make sure you hit every active LWComponent in the runtime related
         * to a particular LWMap (not including the Undo queue)
         */
        ANY

       // VIRTUAL -- would be *just* what ANY currently adds, and exclude PROPER -- currently unsupported
    }

    /** order of result set for getAllDescendents -- not applicable if collection passed in isn't ordered */
    public enum Order {
            /** default traversal order: parents before children */
            TREE,
            /** order for layout operations; children before parents */
            DEPTH
    };

    

    /*
    // need an IntegerPreference and/or an IntegerRangePreference (that ImagePreference could also use)
    private static final VuePreference SlideIconPref =
        IntegerPreference.create(edu.tufts.vue.preferences.PreferenceConstants.MAPDISPLAY_CATEGORY,
			"slideIconSize", 
			"Slide Icon Size", 
			"Size of Slide icons displayed on the map",
			true);

    */
    

    public enum HideCause {
        /** special case bit for deleted objects (which always remain in the undo queue) */
        DELETED (),
            
            /** each subclass of LWComponent can use this for it's own purposes */
            DEFAULT (),
            /* we've been hidden by a filter */
            //FILTER (), 
            /** we've been hidden by link pruning */
            PRUNE (),
            /** we're a member of a pathway that hides when the pathway hides, and all pathways we're on are hidden */
            HIDES_WITH_PATHWAY (true),
            /** we've been hidden by a pathway that is in the process of revealing */
            PATH_UNREVEALED (true),
            /** we've been hidden because the current pathway is all we we want to see, and we're not on it */
            NOT_ON_CURRENT_PATH (true); 

            
        final int bit = 1 << ordinal();
        final boolean isPathwayCause;

        HideCause(boolean isPathCause) { isPathwayCause = isPathCause; }
        HideCause() { isPathwayCause = false; }
    }
    

    //Static { for (Hide reason : Hide.values()) { System.out.println(reason + " bit=" + reason.bit); } }

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

    // todo: next major re-architecting: instead of x/y width/height,
    // keep a Point2D.Float bounds up to date (and can skip creating
    // a rectangles constantly).  (Might also keep a mapBounds?)

    private float x;
    private float y;
    // TODO: if we want to support some kind of keep-relative alignment for an object
    // (in it's parent), we couldn't just use a special object on a generic x/y value
    // ptr -- we still need ACTUAL x/y values to render, but we could have an
    // xAnchor/yAnchor, which could even be a list of actions to perform every time the
    // object is laid out, or it's parent resizes.
    
    private boolean isFiltered = false; // replace with hidebits
    
    private MetadataList metadataList = new MetadataList();
    private NodeFilter nodeFilter = null; // don't know why we need this
    private URI uri;
    protected float width = NEEDS_DEFAULT;
    protected float height = NEEDS_DEFAULT;
    protected java.awt.Dimension textSize = null; // only for use with wrapped text


    /*
     * Runtime only information
     */
    protected transient TextBox labelBox = null;
    protected transient BasicStroke stroke = STROKE_ZERO;
    protected transient boolean selected = false;
    protected transient boolean rollover = false;
    protected transient boolean isZoomedFocus = false;
    protected transient int mHideBits = 0x0; // any bit set means we're hidden

    protected transient LWContainer parent = null;
    protected transient LWComponent mParentStyle;
    protected transient LWComponent mSyncSource; // "semantic source" for nodes on slide to refer back to the concept map
    protected transient Collection<LWComponent> mSyncClients; // set of sync sources that point back to us
    protected transient boolean isStyle;

    // list of LWLinks that contain us as an endpoint
    private transient final List<LWLink> mLinks = new ArrayList<LWLink>(4);
    protected transient List<LWPathway> pathwayRefs;
    private transient long mSupportedPropertyKeys;
    private transient boolean isMoveable = true;

    private transient double scale = 1.0;

    protected transient LWChangeSupport mChangeSupport = new LWChangeSupport(this);
    protected transient boolean mXMLRestoreUnderway = false; // are we in the middle of a restore? (todo: eliminate this as a member variable)
    protected transient BufferedImage mCachedImage;

    public static final Comparator XSorter = new Comparator<LWComponent>() {
            public int compare(LWComponent c1, LWComponent c2) {
                // we multiply up the result so as not to loose differential precision in the integer result
                return (int) (128f * (c1.x - c2.x));
            }
        };
    public static final Comparator YSorter = new Comparator<LWComponent>() {
            public int compare(LWComponent c1, LWComponent c2) {
                return (int) (128f * (c1.y - c2.y));
            }
        };

    public static final Comparator GridSorter = new Comparator<LWComponent>() {
            public int compare(LWComponent c1, LWComponent c2) {
                if (c1.y == c2.y)
                    return XSorter.compare(c1, c2);
                else
                    return YSorter.compare(c1, c2);
                
            }
        };
    
    

    /** for save/restore only & internal use only */
    public LWComponent()
    {
        if (DEBUG.PARENTING)
            System.out.println("LWComponent construct of " + getClass().getName() + "." + Integer.toHexString(hashCode()));
        // TODO: shouldn't have to create a node filter for every one of these constructed...
        nodeFilter = new NodeFilter();
        mSupportedPropertyKeys = Key.PropertyMaskForClass(getClass());
    }
    
    /** for internal proxy instances only */
    private LWComponent(String label) {
        setLabel(label);
    }

    public long getSupportedPropertyBits() {
        return mSupportedPropertyKeys;
    }

    /** Convenience: If key not a real Key (a String), always return true */
    public boolean supportsProperty(Object key) {
        if (key instanceof Key)
            return supportsProperty((Key)key);
        else
            return false;
    }
    
    /** @return true if the given property is currently supported on this component */
    public boolean supportsProperty(Key key) {
        return (mSupportedPropertyKeys & key.bit) != 0;
    }

    protected void disableProperty(Key key) {
        disablePropertyBits(key.bit);
    }
    
    protected void enableProperty(Key key) {
        enablePropertyBits(key.bit);
    }

    protected void disablePropertyBits(long bits) {
        mSupportedPropertyKeys &= ~bits;
    }
    
    protected void enablePropertyBits(long bits) {
        mSupportedPropertyKeys |= bits;
    }
    
    protected void disablePropertyTypes(KeyType type) {
        for (Key key : Key.AllKeys)
            if (key.type == type || (type == KeyType.STYLE && key.type == KeyType.SUB_STYLE))
                disableProperty(key);
    }
    
    
    /** Apply all style properties from styleSource to this component */
    public void copyStyle(LWComponent styleSource) {
        copyStyle(styleSource, ~0L);
    }
    
    public void copyStyle(LWComponent styleSource, long permittedPropertyBits) {
        if (DEBUG.STYLE) System.out.println("COPY STYLE of " + styleSource + " ==>> " + this + " permitBits=" + Long.bitCount(permittedPropertyBits));
        for (Key key : Key.AllKeys)
            if (key.isStyleProperty && styleSource.supportsProperty(key) && (permittedPropertyBits & key.bit) != 0)
                key.copyValue(styleSource, this);
    }

    public void copyProperties(LWComponent source, long propertyBits) {
        if (DEBUG.STYLE) System.out.println("COPY PROPS of " + source + " ==>> " + this + " bits=" + Long.bitCount(propertyBits));
        for (Key key : Key.AllKeys)
            if ((propertyBits & key.bit) != 0 && source.supportsProperty(key))
                key.copyValue(source, this);
    }
    

    public void applyCSS(edu.tufts.vue.style.Style cssStyle)
    {
        out("Applying CSS style " + cssStyle.getName() + ":");
        for (Map.Entry<String,String> se : cssStyle.getAttributes().entrySet()) {
            final String cssName = se.getKey().trim().toLowerCase(); // todo: shouldn't have to trim this
            final String cssValue = se.getValue().trim();
            boolean applied = false;

            System.err.format("%-35s CSS key %-17s value %-15s",
                              toString(),
                              '\'' + cssName + '\'',
                              '\"' + cssValue + '\"'
                              );
            
            for (Key key : Key.AllKeys) {
                if (key.cssName == null)
                    continue;
                //out("Checking key [" + cssName + "] against [" + key.cssName + "]");
                if (supportsProperty(key) && cssName.equals(key.cssName)) {
                    //out("Matched supported property key " + key.cssName);

                    applied = key.setValueFromCSS(this, cssName, cssValue);

                    /*
                    final Property slot = key.getSlot(this);
                    if (slot == Key.NO_SLOT_PROVIDED) {
                        out("Can't apply CSS Style property to non-slotted key: " + cssName + " -> " + key);
                    } else {
                        try {
                            slot.setFromCSS(cssName, cssValue);
                            System.err.println("applied value: " + slot);
                            applied = true;
                            break;
                        } catch (Throwable t) {
                            System.err.println();
                            tufts.Util.printStackTrace(new Throwable(t), "failed to apply CSS key/value " + cssName + "=" + cssValue);
                        }
                    }
                    */
                }
            }
            setFont(cssStyle.getFont());
            if (!applied)
                System.err.println("UNHANDLED");

        }
    }

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

        public final KeyType type;

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

                if (DEBUG.INIT) System.out.format("CACHED PROPERTY BITS for %s: %d\n", clazz, Long.bitCount(propMaskForClass));
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
            this.type = keyType;
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

            if (DEBUG.Enabled)
                System.out.printf("KEY %-20s %-11s %-22s bit#%2d; %25s now has %2d properties\n", 
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
        static final Property NO_SLOT_PROVIDED = EmptyStyle.mFillColor; // any slot will do
        //private static final Property BAD_SLOT = EmptyStyle.mStrokeColor; // any (different) slot will do
        /** If this isn't overriden to return non-null, getValue & setValue must be overriden to provide the setter/getter impl  */
        Property getSlot(TSubclass c) { return NO_SLOT_PROVIDED; }

        boolean isSlotted(TSubclass c) { return getSlot(c) != NO_SLOT_PROVIDED; }

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
                    tufts.Util.printStackTrace(new Throwable(t), this + ": property slot get() failed " + propertySlot);
                else
                    VUE.Log.warn(this + ": property slot get() failed " + propertySlot + " " + t);
                return DEBUG.Enabled ? (TValue) "<unsupported for this object>" : null;
                //return null;
            }
        }

        void setValueInternal(TSubclass c, TValue value) {
            setValue(c, value);
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
u                    getSlot(c).setFromString((String)value);
                } else {
                    getSlot(c).set(value);
                }
            } catch (ClassCastException e) {
                tufts.Util.printStackTrace(e, "Bad setValue type for " + getSlot(c) + ": " + (value == null ? "null" : value.getClass()));
            }
                */
        }
        
        private Property getSlotSafely(TSubclass c) {
            Property slot = null;
            try {
                slot = getSlot(c);
            } catch (ClassCastException e) {
                String msg = "Property not supported: " + this + " on\t" + c + " (getSlot failed; returned null)";
                //tufts.Util.printStackTrace(e, msg);
                VUE.Log.warn(msg + "; " + e);
                return null;
            } catch (Throwable t) {
                tufts.Util.printStackTrace(new Throwable(t), this + ": bad slot? unimplemented get/setValue?");
                return null;
            }
            //if (slot == NO_SLOT_PROVIDED) tufts.Util.printStackTrace(this + ": no slot provided");
            return slot;
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
        
        void setStringValue(TSubclass c, String stringValue) {
            Property slot = getSlotSafely(c);
            if (slot != NO_SLOT_PROVIDED) {
                slot.setFromString(stringValue);
            } else {
                TValue curValue = getValue(c);
                // handle a few special cases for standard java types, even if there's no slot (Property object) to parse the string
                // FYI, this won't work if getValue returns null, as we'll have no class object to check for type information.
                     if (curValue instanceof String)    setValue(c, (TValue) stringValue);
                else if (curValue instanceof Integer)   setValue(c, (TValue) new Integer(stringValue));
                else if (curValue instanceof Long)      setValue(c, (TValue) new Long(stringValue));
                else if (curValue instanceof Float)     setValue(c, (TValue) new Float(stringValue));
                else if (curValue instanceof Double)    setValue(c, (TValue) new Double(stringValue));
                else
                    tufts.Util.printStackTrace(this + ":setValue(" + stringValue + "); no slot provided for parsing string value");
            }
        }

        /** @return true if was successful */
        boolean setValueFromCSS(TSubclass c, String cssKey, String cssValue) {
            final Property slot = getSlot(c);
            if (slot == Key.NO_SLOT_PROVIDED) {
                c.out("Can't auto-apply CSS Style property to non-slotted key: " + cssName + " -> " + this);
                return false;
            }
            try {
                slot.setFromCSS(cssName, cssValue);
                System.err.println("applied value: " + slot);
                return true;
            } catch (Throwable t) {
                System.err.println();
                tufts.Util.printStackTrace(new Throwable(t), "failed to apply CSS key/value " + cssName + "=" + cssValue);
            }
            return false;
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
                if (DEBUG.STYLE && DEBUG.META) System.err.println(" COPY-VALUE: " + this + "; source doesn't support this property; " + source);
            } else if (!target.supportsProperty(this)) {
                if (DEBUG.STYLE && DEBUG.META) System.err.println(" COPY-VALUE: " + this + "; target doesn't support this property; " + target);
            } else {
                final TValue newValue = getValue(source);
                final TValue oldValue = getValue(target);

                if (newValue != oldValue && (newValue == null || !newValue.equals(oldValue))) {
                    if (DEBUG.STYLE) System.out.format("  COPY-VALUE: %s %-15s %-40s -> %s over (%s)\n",
                                                       source,
                                                       name,
                                                       "(" + newValue + ")",
                                                       target,
                                                       oldValue);
                    setValue(target, newValue);
                }


                //if (DEBUG.STYLE) System.err.print(" COPY-VALUE: " + this + "(");
                //if (DEBUG.STYLE) System.err.println(copyValue + ") -> " + target);
            }
        }

        public String toString() { return name; } // must == name for now until tool panels handle new key objects (is this true yet?)
        //public String toString() { return type + "{" + name + "}"; }
    }

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
            if (DEBUG.TOOL) System.out.printf("     TAKING: %-30s -> %s\n", vtag(key, o, this), LWComponent.this);
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

        void setFromCSS(String cssKey, String value) {
            throw new UnsupportedOperationException(this + " unimplemented setFromCSS " + cssKey + " = " + value);
            //VUE.Log.error("unimplemented setFromCSS " + cssKey + " = " + value);
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
            return key + "[" + value + "]";
        }
        
    }

    public class EnumProperty<T extends Enum> extends Property<T> {
        EnumProperty(Key key, T defaultValue) {
            super(key);
            value = defaultValue;
            //System.out.println("enum values: " + Arrays.asList(defaultValue.getClass().getEnumConstants()));
            //System.out.println("enum test: " + Enum.valueOf(defaultValue.getClass(), "DASH1"));
        }
        void setBy(String s) {
            // note: value can never be null, or we'll need to store the Enum class reference elsewhere
            // (e.g., in the Key -- better there anyway, where we could provide a generic "values"
            // to list the supported values)
            set((T) Enum.valueOf(value.getClass(), s.trim())); 
        }
    }
    
    private static final String _DefaultString = "";
    public class StringProperty extends Property<java.lang.String> {
        StringProperty(Key key) {
            super(key);
            value = _DefaultString;
        }
        void setBy(String s) { set(s); }
    }

    
    abstract public class NumberProperty<T> extends Property<T> {
        NumberProperty(Key key) { super(key); }
            
        void setFromCSS(String cssKey, String value) {
            if (value.endsWith("pt") || value.endsWith("px"))
                setBy(value.substring(0, value.length()-2));
            else
                throw new IllegalArgumentException("unhandled CSS number conversion for [" + value + "]");
                      
        }
        
    }


    static class PropertyValueVeto extends RuntimeException {
        PropertyValueVeto(String msg) {
            super(msg);
        }
    }

    
    
    private static final Integer _DefaultInteger = new Integer(0);
    public class IntProperty extends NumberProperty<java.lang.Integer> {
        IntProperty(Key key, Integer defaultValue) {
            super(key);
            value = defaultValue;
        }
        IntProperty(Key key) {
            this(key, _DefaultInteger);
        }
        
        void setBy(String s) { set(new Integer(s)); }
    }
    
    private static final Float _DefaultFloat = new Float(0f);
    public class FloatProperty extends NumberProperty<java.lang.Float> {
        FloatProperty(Key key) {
            super(key);
            value = _DefaultFloat;
        }
        void setBy(String s) { set(new Float(s)); }
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

    /**
     * Handles CSS font-style value "italic" ("normal", or anything else, has no effect as of yet)
     * Also handles CSS font-weight value of "bold" (anything else is ignored for now)
     * todo: no hook for font-weight yet, permits invalid CSS
     */
    public class CSSFontStyleProperty extends IntProperty {
        CSSFontStyleProperty(Key key) { super(key); }
        void setFromCSS(String cssKey, String value) {
            // todo: this ignoring the key, which will permit non-confomant CSS
            if ("italic".equalsIgnoreCase(value))
                set(java.awt.Font.ITALIC);
            else if ("bold".equalsIgnoreCase(value))
                set(java.awt.Font.BOLD);
            else
                set(0);
        }
    }

    /*
    public class CSSFontSizeProperty extends IntProperty {
        CSSFontSizeProperty(Key key) { super(key); }
        void setFromCSS(String cssKey, String value) {
            if (value.endsWith("pt"))
                setBy(value.substring(0, value.length()-2));
            else
                throw new IllegalArgumentException("unhandled CSS font size [" + value + "]");
                      
        }
    }
    */

    public class CSSFontFamilyProperty extends StringProperty {
        CSSFontFamilyProperty(Key key) { super(key); }
        void setFromCSS(String cssKey, String value) {
            // no translation needed for now: just use the raw name -- if it's a preference list tho, we'll need to handle it
            setBy(value);
        }
    }
    

    
    
    
    
    public class ColorProperty extends Property<java.awt.Color> {
        private boolean allowTranslucence = true;
        
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

        void setAllowTranslucence(boolean allow) {
            allowTranslucence = allow;
        }

        void take(Color c) {
            if (!allowTranslucence && (c == null || c.getAlpha() != 0xFF))
                throw new PropertyValueVeto(key + "; color with translucence: "
                                            + c
                                            + " alpha=" + c.getAlpha()
                                            + " not allowed on " + LWComponent.this);
            super.take(c);
        }

        void setBy(String s) {
            set(StringToColor(s));
        }

        void setFromCSS(String key, String value) {
            // todo: CSS Style object could include the already instanced Color object
            // we ignore key: assume that whatever it is is a color value
            setBy(value);
        }

        /** @return a value between 0.0 and 1.0 representing brightness: the saturation % of the strongest channel
         * e.g.: white returns 1, black returns 0
         */
        public float brightness() {
            return LWComponent.brightness(value);
         }


        String asString() {
            return ColorToString(get());
        }
    }
    
    public static float brightness(java.awt.Color c) {
            
        if (c == null)
            return 0;

        final int r = c.getRed();
        final int g = c.getGreen();
        final int b = c.getBlue();

        int max = (r > g) ? r : g;
        if (b > max) max = b;
            
        return ((float) max) / 255f;
    }
        
    public static Color StringToColor(final String s)
    {
        if (s.trim().length() < 1)
            return null;
        
        Color c = null;
        try {
            c = VueResources.makeColor(s);
        } catch (NumberFormatException e) {
            tufts.Util.printStackTrace(new Throwable(e), "LWComponent.StringToColor[" + s + "]");
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
        else
            return String.format("#%08X", c.getRGB());
    }

    
    public static final Key KEY_FillColor   = new Key("fill.color", "background")       { final Property getSlot(LWComponent c) { return c.mFillColor; } };
    public static final Key KEY_TextColor   = new Key("text.color", "font-color")       { final Property getSlot(LWComponent c) { return c.mTextColor; } };
    public static final Key KEY_StrokeColor = new Key("stroke.color", "border-color")   { final Property getSlot(LWComponent c) { return c.mStrokeColor; } };
    //public static final Key KEY_StrokeStyle = new Key("stroke.style", "border-style")   { final Property getSlot(LWComponent c) { return null; } };
    public static final Key KEY_StrokeWidth = new Key("stroke.width", "stroke-width")   { final Property getSlot(LWComponent c) { return c.mStrokeWidth; } };
    public static final Key KEY_StrokeStyle = new Key<LWComponent,StrokeStyle>
        ("stroke.style", KeyType.STYLE)   { final Property getSlot(LWComponent c) { return c.mStrokeStyle; } };


    /* font.size: point size for font */
    /* font.style: @See java.awt.Font 0x0=Plain, 0x1=Bold On, 0x2=Italic On */
    /* font.name: family name of the font */
    
    /** Aggregate font key, which represents the combination of it's three sub-properties */
    public static final Key KEY_Font = new Key("font", KeyType.STYLE)                   { final Property getSlot(LWComponent c) { return c.mFont; } };
    public static final Key KEY_FontSize  = new Key("font.size", KeyType.SUB_STYLE)     { final Property getSlot(LWComponent c) { return c.mFontSize; } };
    public static final Key KEY_FontStyle = new Key("font.style", KeyType.SUB_STYLE)    { final Property getSlot(LWComponent c) { return c.mFontStyle; } };
    public static final Key KEY_FontName  = new Key("font.name", KeyType.SUB_STYLE)     { final Property getSlot(LWComponent c) { return c.mFontName; } };
    
    public final ColorProperty mFillColor = new ColorProperty(KEY_FillColor);
    public final ColorProperty mTextColor = new ColorProperty(KEY_TextColor, java.awt.Color.black) {
            //{ color = java.awt.Color.black; } // default value
            void onChange() {
                if (labelBox != null)
                    labelBox.copyStyle(LWComponent.this); // todo better: handle thru style.textColor notification?
            }
        };
    public final ColorProperty mStrokeColor = new ColorProperty(KEY_StrokeColor, java.awt.Color.darkGray);
    public final FloatProperty mStrokeWidth = new FloatProperty(KEY_StrokeWidth) { void onChange() { rebuildStroke(); }};
    public final EnumProperty<StrokeStyle> mStrokeStyle = new EnumProperty(KEY_StrokeStyle, StrokeStyle.SOLID) { void onChange() { rebuildStroke(); }};

    public enum StrokeStyle {

        SOLID   (1,0),
            DOTTED (1,1),
            DASHED (2,2),
            DASH2 (3,2),
            DASH3 (5,3);
            
        private final float[] dashPattern = new float[2];

        StrokeStyle(float dashOn, float dashOff) {
            dashPattern[0] = dashOn; // pixels on (drawn)
            dashPattern[1] = dashOff; // pixels off (whitespace)
        }

        public BasicStroke makeStroke(double width) {
            return makeStroke((float) width);
        }
        
        public BasicStroke makeStroke(float width) {
            if (this == SOLID)
                return new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
                //return new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND);
            else
                return new BasicStroke(width
                                       , BasicStroke.CAP_BUTT // anything else will mess with the dash pattern
                                       , BasicStroke.JOIN_BEVEL
                                       , 10f // miter-limit
                                       //, 0f // miter-limit
                                       , dashPattern
                                       , 0f); // dash-phase (offset to start of pattern -- apparently pixels, not index)
        }
        // todo opt: better: could cache the strokes here for each dash pattern/size
        
    }

    private void rebuildStroke() {
        final float width = mStrokeWidth.get();
        if (width > 0)
            this.stroke = mStrokeStyle.get().makeStroke(width);
        else
            this.stroke = STROKE_ZERO;
        /*/ below code was broken in previous code.  Node child layout does NOT
        // appear to be taking into account total bounds with at the moment anyway...
        // (Or was that just for Groups?  No, those appear to be handling the full bounds change.)
        // Also, want to make generic with a flag in Key if layout needed when
        // the given property changes.
        if (getParent() != null) {
            // because stroke affects bounds-width, may need to re-layout parent
            getParent().layout();
        }
        layout();*/
    }


    public final IntProperty mFontStyle = new CSSFontStyleProperty(KEY_FontStyle)       { void onChange() { rebuildFont(); } };
    public final IntProperty mFontSize = new IntProperty(KEY_FontSize)                  { void onChange() { rebuildFont(); } };
    public final StringProperty mFontName = new CSSFontFamilyProperty(KEY_FontName)     { void onChange() { rebuildFont(); } };

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


    public static final Key KEY_Label = new Key<LWComponent,String>("label", KeyType.DATA) {
            public void setValue(LWComponent c, String val) { c.setLabel(val); }
            public String getValue(LWComponent c) { return c.getLabel(); }
        };
    public static final Key KEY_Notes = new Key<LWComponent,String>("notes", KeyType.DATA) {
            public void setValue(LWComponent c, String val) { c.setNotes(val); }
            public String getValue(LWComponent c) { return c.getNotes(); }
        };


    //===================================================================================================
    //
    // End of Key's and Properties
    //
    //===================================================================================================

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
    
    /**
     * Get the named property value from this component.
     * @param key property key (see LWKey)
     * @return object representing appropriate value, or null if none found (note: properties may be null also -- todo: fix)
     */

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

        // Old property keys that don't make use of the Key class yet:
        if (key == LWKey.Resource)      return getResource();
        if (key == LWKey.Location)      return getLocation();
        if (key == LWKey.Size)          return new Size(this.width, this.height);
        if (key == LWKey.Hidden)        return isHidden() ? Boolean.TRUE : Boolean.FALSE;
             
        VUE.Log.warn(this + " getPropertyValue; unsupported property [" + key + "] (returning null)");
        //throw new RuntimeException("Unknown property key[" + key + "]");
        return null;
    }

    public void setProperty(final Object key, Object val)
    {
        if (DEBUG.TOOL||DEBUG.UNDO) System.out.println("setProperty: " + vtag(key, val, null) + " on " + LWComponent.this);

        if (key instanceof Key) {
            final Key k = (Key) key;
            k.setValue(this, val);
            // Experiment it auto-copying over data elements to siblings
            // TODO: label's a special case due to TextBox use of non-key'd setLabel0
            //if (k.keyType == KeyType.DATA && mSibling != null)
            //    k.setValue(mSibling, val);
        }
        // Old property keys that don't make use of the Key class yet:
        //else if (key == LWKey.Hidden)        setHidden( ((Boolean)val).booleanValue());
        else if (key == LWKey.Scale)         setScale((Double) val);
        else if (key == LWKey.Resource)      setResource( (Resource) val);
        else if (key == LWKey.Location) {

            // This is a bit of a hack, in that we're relying on the fact that the only
            // thing to call setProperty with a Location key right now is the
            // UndoManager.  In any case, on undo, we do NOT want to additionally make
            // mapLocationChanged calls on all descendents (for absolute map location
            // objects; e.g. LWLink's).  Location changes as a result of these calls
            // were already recorded as events and will be undone on their own.
            
            final Point2D.Float loc = (Point2D.Float) val;
            setLocation(loc.x, loc.y, this, false);
            //setLocation( (Point2D) val);
        }
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
     * to reconnect links within the group after duplication, and
     * passing flags into the dupe context.
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
                
                final LWComponent headCopy = mCopies.get(linkOriginal.getHead());
                final LWComponent tailCopy = mCopies.get(linkOriginal.getTail());
                
                if (DEBUG.DND)
                    System.out.println("LinkPatcher: reconnecting " + linkCopy + " endpoints:"
                                       + "\n\t" + headCopy
                                       + "\n\t" + tailCopy
                                       );
                
                linkCopy.setHead(headCopy);
                linkCopy.setTail(tailCopy);
            }
        }
    }

    public static class CopyContext {
        final boolean dupeChildren;
        LinkPatcher patcher;
        CopyContext() { this(true); }
        CopyContext(boolean dupeChildren) {
            this.dupeChildren = dupeChildren;
        }
        CopyContext(LinkPatcher lp, boolean dupeChildren) {
            this.patcher = lp;
            this.dupeChildren = dupeChildren;
        }

        void reset() {
            if (patcher != null)
                patcher.reset();
        }

        void complete() {
            if (patcher != null)
                patcher.reconnectLinks();
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

    public LWComponent duplicate(CopyContext cc)
    {
        final LWComponent c;

        try {
            c = getClass().newInstance();
        } catch (Throwable t) {
            tufts.Util.printStackTrace(t, "duplicate " + getClass());
            return null;
        }

        c.mSupportedPropertyKeys = this.mSupportedPropertyKeys;
        //c.mParentStyle = this.mParentStyle;
        
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

        if (cc.patcher != null)
            cc.patcher.track(this, c);

        return c;
    }

    public LWComponent duplicate() {
        return duplicate(new CopyContext());
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

    public UndoManager getUndoManager() {
        final LWMap map = getMap();
        if (map == null)
            return null;
        else
            return map.getUndoManager();
    }

    protected void addCleanupTask(Runnable task) {
        addCleanupTask(task, this);
        //addCleanupTask(task, this, null);
    }
    
    //    protected void addCleanupTask(Runnable task, Object taskKey, Object srcMsg) {
    protected void addCleanupTask(Runnable task, Object taskKey) {
        final UndoManager um = getUndoManager();

        if (um != null) {
            if (um.isUndoing()) {
                if (DEBUG.WORK || DEBUG.UNDO) System.out.println("Ignoring cleanup task during undo: " + task + " for " + this);
            } else if (um.hasCleanupTask(taskKey)) {
                if (DEBUG.WORK || DEBUG.UNDO) System.out.println("Ignoring duplicate cleanup task: " + task + " for " + this);
            } else {

                boolean debug = DEBUG.WORK || DEBUG.UNDO;
                if (isDeleted()) {
                    Util.printStackTrace("warning: adding cleanup task when deleted");
                    debug = true;
                }
                
//                 if (debug) {
//                     System.out.println(TERM_RED + "ADDING CLEANUP TASK: " + task 
//                                        + (srcMsg==null?"":("on " + srcMsg))
//                                        + (task == this ? "" : (" for " + this))
//                                        + TERM_CLEAR);
//                 }
                
                um.addCleanupTask(this, task);
            }
        }
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
        //setHidden(HideCause.FILTER, filtered);
    }
    
    /**
     * @return true if should be hidden due to a currently applied filter, false if not
     **/
    public boolean isFiltered() {
    	return isFiltered;
        //return (mHideBits & HideCause.FILTER.bit) != 0;
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

    protected TextBox getLabelBox()
    {
        if (this.labelBox == null) {
            synchronized (this) {
                if (this.labelBox == null)
                    this.labelBox = new TextBox(this, this.label);
            }
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
        if (DEBUG.CASTOR) out("SETTING RESOURCE TO " + (resource==null?"":resource.getClass()) + " [" + resource + "]");
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

    public int getNumericID() {
        return idStringToInt(getID());
    }

    /** for use during restore */
    protected final int idStringToInt(String idStr)
    {
        int id = -1;
        try {
            id = Integer.parseInt(idStr);
        } catch (Exception e) {
            System.err.println(e + " invalid ID: '" + idStr + "'");
            e.printStackTrace();
        }
        return id;
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
    
    /**
     * left in for (possible future) backward file compatibility
     * do nothing with this data anymore for now.
     *
     * @deprecated
     *
     **/
    public void setNodeFilter(NodeFilter nodeFilter) {
        this.nodeFilter = nodeFilter;
    }
    
    /**
     *
     * Metadata List for use with RDF Index
     * It is sufficient for the minimal RDF functionality
     * to be able to retrieve this list from the LWComponent
     * using this method and add elements directly to the list as needed.
     * LWComponent may choose to create notifications/modifcations
     * for any data added directly through LWComponent itself
     * in future.
     *
     **/
     public MetadataList getMetadataList()
     {
         return metadataList;
     }
    
    /**
     * left in for (possible future) backward file compatibility
     * do nothing with this data anymore for now.
     *
     * @deprecated
     *
     **/
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
    // TODO: resolve this with supportsProperty(LWKey.Label) (perhaps lose this method)
    public boolean supportsUserLabel() {
        return supportsProperty(LWKey.Label);
    }
    /** does this support user resizing? */
    // TODO: change these "supports" calls to an arbitrary property list
    // that could have arbitrary properties added to it by plugged-in non-standard tools
    public boolean supportsUserResize() {
        return false;
    }
    
    /** @return false: subclasses (e.g. containers), override to return true if allows children dragged in and out
     * by a user.
     */
    public boolean supportsChildren() {
        return false;
    }

    /** @return true: subclasses (e.g. containers), override to return false if you never want this component
        reparented by users */
    public boolean supportsReparenting() {
        return parent instanceof LWGroup == false; // todo: handle via API that LWGroup can declare
    }

    /** @return true: by default, all objects can be selected with other objects at the same time */
    public boolean supportsMultiSelection() {
        return true;
    }


    /** @return true if we allow a link to the target, and the target allows a link to us.
     * Eventually we can use this to check ontology information.
     * @param target -- the target to check.  If null, tells is if this component allows
     * link to nothing / allows links at all.
     */
    public boolean canLinkTo(LWComponent target) {
        return canLinkToImpl(target) && (target == null || target.canLinkToImpl(this));
    }
    
    
    /** @return true -- subclass impl's can override */
    protected boolean canLinkToImpl(LWComponent target) {
        return true;
    }
    
    public boolean hasLabel() {
        return this.label != null && this.label.length() > 0;
    }
    public String getNotes() {
        return this.notes;
    }
    public boolean hasNotes() {
        return this.notes != null && this.notes.length() > 0;
    }
    public boolean hasResource() {
        return this.resource != null;
    }
    public boolean hasLinks() {
        return mLinks.size() > 0;
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
    // TODO: rename onPathway
    public boolean inPathway(LWPathway path)
    {
        if (pathwayRefs == null || path == null)
            return false;

        for (LWPathway p : pathwayRefs)
            if (p == path)
                return true;
        
        return false;
    }

    public List<LWPathway> getPathways() {
        return pathwayRefs == null ? java.util.Collections.EMPTY_LIST : pathwayRefs;
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
        // clear any hidden bits that may be set as a result
        // of the membership in the pathway.
        for (HideCause cause : HideCause.values())
            if (cause.isPathwayCause)
                clearHidden(cause);
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
    final void layout() {
        if (mXMLRestoreUnderway == false)
            layout("default");
    }
    
    final void layout(Object triggerKey) {
        if (mXMLRestoreUnderway == false) {
            layoutImpl(triggerKey);
            // need a reshape/reshapeImpl for this (size/location changes)
            //if (mSlideIconBounds != null)
            //    mSlideIconBounds.x = Float.NaN; // invalidate
        }
    }

    protected void layoutImpl(Object triggerKey) {}
    
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
    
    /**
     * Color to use at draw time. LWNode overrides to provide darkening of children.
     * We also use this for the background color in active on-map text edits.
     */
    public Color getRenderFillColor(DrawContext dc) {
        if (mFillColor.isTransparent()) {
            if (dc != null && dc.focal == this) {
                //System.out.println("     DC FILL: " + dc.getFill() + " " + this);
                return dc.getFill();
            } else if (parent != null) {
                //System.out.println(" PARENT FILL: " + parent.getRenderFillColor(dc) + " " + this);
                return parent.getRenderFillColor(dc);
            }
        }
        //System.out.println("DEFAULT FILL: " + mFillColor.get() + " " + this);
        return mFillColor.get();
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
    
    /** @return null for SOLID (ordinal 0, the default, as for old save files), or otherwise, the ordinal of the style enum
     * Castor will not bother to generate the attribute/element when it's value is null. */
    public Integer getXMLstrokeStyle() {
        int code = mStrokeStyle.get().ordinal();
        return code == 0 ? null : code;
    }
    public void setXMLstrokeStyle(Integer ordinal)  {
        // todo: have the Key class process enum's generically, caching the results of Class<? extends Enum>.getEnumConstants()
        for (StrokeStyle ss : StrokeStyle.values()) {
            if (ss.ordinal() == ordinal) {
                mStrokeStyle.set(ss);
                break;
            }
        }
    }
    
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


    
    /** 
     * The first time a TextBox is created for edit, it may not have been laid out
     * by it's parent, which is where it normally gets it's location.  This 
     * initializes the location of the TextBox for first usage.  The default
     * impl here centers the TextBox in the LWComponent.
     */
    public void initTextBoxLocation(TextBox textBox) {
        textBox.setBoxCenter(getWidth() / 2,
                             getHeight() / 2);
    }


//     /** default label X position impl: center the label in the bounding box */
//     public float getLabelX()
//     {
//         //float x = getCenterX();
//         if (hasLabel())
//             return getLabelBox().getMapX();
//         else if (labelBox != null)
//             return getCenterX() - labelBox.getMapWidth() / 2;
//         else
//             return getCenterX();
//         //  x -= (labelBox.getMapWidth() / 2) + 1;
//         //return x;
//     }
//     /** default label Y position impl: center the label in the bounding box */
//     public float getLabelY()
//     {
//         if (hasLabel())
//             return getLabelBox().getMapY();
//         else if (labelBox != null)
//             return getCenterY() - labelBox.getMapHeight() / 2;
//         else
//             return getCenterY();
        
//         //float y = getCenterY();
//         //if (hasLabel())
//         //  y -= labelBox.getMapHeight() / 2;
//         //return y;
//     }
    
    void setParent(LWContainer newParent) {

        if (DEBUG.UNDO) System.err.println("*** SET-PARENT: " + newParent + " for " + this);
        
        //final boolean linkNotify = (!mXMLRestoreUnderway && parent != null);
        if (parent == newParent) {
            // This is normal.
            // (e.g., one case: during undo of reparenting operations)
            //if (DEBUG.Enabled) Util.printStackTrace("redundant set-parent in " + this + "; parent=" + newParent);
            return;
        }
        parent = newParent;
//         if (linkNotify && mLinks.size() > 0)
//             for (LWLink link : mLinks)
//                 link.notifyEndpointReparented(this);
    }
    
    //protected void reparentNotify(LWContainer parent) {}

    public void setSyncSource(LWComponent source) {
        if (mSyncClients != null) {
            out("blowing away sync clients on syncSource set");
            // just in case
            mSyncClients.clear();
            mSyncClients = null;
        }
        mSyncSource = source;
        mSyncSource.addSyncClient(this);
    }

    public LWComponent getSyncSource() {
        return mSyncSource;
    }

    protected void addSyncClient(LWComponent c) {
        if (mSyncClients == null)
            mSyncClients = new HashSet();
        mSyncClients.add(c);
    }

    public void setStyle(LWComponent parentStyle)
    {
        mParentStyle = parentStyle;
        parentStyle.isStyle = true;
        if (!mXMLRestoreUnderway)       // we can skip the copy during restore
            copyStyle(parentStyle);
    }

    /** for castor persist */
    public LWComponent getStyle() {
        return mParentStyle;
    }

    public boolean isStyle() {
        return isStyle;
    }
    
    public Boolean getPersistIsStyle() {
        return isStyle ? Boolean.TRUE : null;
    }
    
    public void setPersistIsStyle(Boolean b) {
        isStyle = b.booleanValue();
    }

    /** @deprecated: tmp back compat only */ public void setParentStyle(LWComponent c) { setStyle(c); }
    /** @deprecated: tmp back compat only */ public Boolean getPersistIsStyleParent() { return null; }
    /** @deprecated: tmp back compat only */ public void setPersistIsStyleParent(Boolean b) { setPersistIsStyle(b); }
    /** @deprecated: tmp back compat only */ public LWComponent getParentStyle() { return null; }


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

    /**
     * @return 0 by default
     * the pick depth (in PickContext) must be >= what this returns for descdents of this component
     * be picked (selected, etc).  Mostly meaningful when an LWContainer subclass implements
     * and returns something > 0, tho a single component could use this to become a "background" item.
     * You can think of this as establishing a "wall" in the depth hierarchy, past which pick
     * traversals will not descend unless given a high enough pickDepth to jump the wall.
     */
    public int getPickLevel() {
        return 0;
    }


    //private static LWComponent ProxySlideComponent = new LWComponent("<global-slide-proxy>");

    /** return the component to be picked if we're picked: e.g., may return null if you only want children picked, and not the parent */
    protected LWComponent defaultPick(PickContext pc) {
        // If we're dropping something, never allow us to be picked
        // if we're a descendent of what's being dropped! (would be a parent/child loop)
        if (pc.dropping instanceof LWContainer && hasAncestor((LWContainer)pc.dropping))
            return null;
        else if (isDrawingSlideIcon() && getMapSlideIconBounds().contains(pc.x, pc.y)) {
            return getEntryToDisplay().getSlide();
        }
        else
            return defaultPickImpl(pc);
    }
    
    protected LWComponent defaultPickImpl(PickContext pc) {
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

    public boolean isManagedLocation() {
        return getParent().isManagingChildLocations() || (isSelected() && isAncestorSelected());
    }

    public boolean isManagingChildLocations() {
        return false;
    }

    /** @return true - A single component always "has content" -- subclasses override to provide varying semantics */
    public boolean hasContent() {
        return true;
    }

    /** @return false by default */
    public boolean isImageNode() {
        return false;
    }

    /**
     * Although unsupported on LWComponents (must be an LWContainer subclass to support children),
     * this method appears here for typing convenience and debug.  If a non LWContainer subclass
     * calls this, it's a no-op, and a diagnostic stack trace is dumped to the console.
     */
    public void addChild(LWComponent c) {
        Util.printStackTrace(this + ": can't take children; ignored new child: " + c);
    }

    /**
     * Although unsupported on LWComponents (must be an LWContainer subclass to support children),
     * this method appears here for typing convenience and debug.  If a non LWContainer subclass
     * calls this, it's a no-op, and a diagnostic stack trace is dumped to the console.
     */
    public void addChildren(Iterable<LWComponent> iterable) {
        Util.printStackTrace(this + ": can't take children; ignored iterable: " + iterable);
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
            return getAllDescendents(kind, new java.util.ArrayList(), Order.TREE);
    }
    
    public Collection<LWComponent> getAllDescendents(final ChildKind kind, final Collection<LWComponent> bag) {
        return getAllDescendents(kind, bag, Order.TREE);
    }
    
    public Collection<LWComponent> getAllDescendents(final ChildKind kind, final Collection<LWComponent> bag, Order order) {
        return bag;
    }
    

    /** for tracking who's linked to us */
    void addLinkRef(LWLink link)
    {
        if (DEBUG.UNDO) out(this + " adding link ref to " + link);
        if (mLinks.contains(link)) {
            //tufts.Util.printStackTrace("addLinkRef: " + this + " already contains " + link);
            if (DEBUG.Enabled) VUE.Log.warn("addLinkRef: " + this + " already contains " + link);
        } else {
            mLinks.add(link);
            notify(LWKey.LinkAdded, link); // informational only event
        }
    }
    /** for tracking who's linked to us */
    void removeLinkRef(LWLink link)
    {
        if (DEBUG.EVENTS||DEBUG.UNDO) out(this + " removing link ref to " + link);
        if (!mLinks.remove(link))
            throw new IllegalStateException("removeLinkRef: " + this + " didn't contain " + link);
        clearHidden(HideCause.PRUNE);
        notify(LWKey.LinkRemoved, link); // informational only event
    }
    
    /* tell us all the links who have us as one of their endpoints */
    public List<LWLink> getLinks(){
        return mLinks;
    }
    
    /** @return all LWComponents directly connected to this one: for most components, this
     * is just all the LWLink's that connect to us.  For LWLinks, it's mainly it's endpoints,
     * plus also any LWLink that may be directly connected to the link itself
     */
    public Collection<? extends LWComponent> getLinked() {
        // returning mLinks is an optimization, but requireds
        // subclasses to override this method also if want to change
        // the impl.
        return mLinks;
        
        //return getLinked(new ArrayList(mLinks.size()));
        //return Collections.unmodifiableList(mLinks);
    }
    
    public Collection<LWComponent> getLinked(Collection bag) {
        bag.addAll(mLinks);
        return bag;
    }

    /** @return a list of every component connected to this one via links, including the links themselves */
    public Collection<LWComponent> getLinkChain() {
        return getLinkChain(new HashSet());
    }
    
    /**
     * @return a list of every component connected to this one via links, including the links themselves
     * @param bag - the collection to store the results in.  Any component already in the bag will not
     * have it's outbound links followed -- this provides inherent loop protection.
     * Note that this collection isn't a Set of some kind, components will appear in the bag more than once.
     * (Once for every time they were visited).
     */
    public Collection<LWComponent> getLinkChain(Collection bag)
    {
        if (!bag.add(this)) {
            // already added to the set with all connections -- don't process again            
            return bag;
        }
        
        for (LWComponent c : getLinked())
            c.getLinkChain(bag);

        return bag;
    }

    public Rectangle2D.Float getFanBounds() {
        return getFanBounds(null);

    }

    /** @return the union of the bounds of the current component, all connected links, and all far endpoints
     * of those links.
     */
    public Rectangle2D.Float getFanBounds(Rectangle2D.Float rect)
    {
        if (rect == null)
            rect = getBounds();
        else
            rect.setRect(getBounds());
            
        for (LWLink link : getLinks()) {
            final LWComponent head = link.getHead();
            final LWComponent tail = link.getTail();

            rect.add(link.getPaintBounds());
            
            if (head != this) {
                if (head != null)
                    rect.add(head.getPaintBounds());
            } else if (tail != this) {
                if (tail != null)
                    rect.add(tail.getPaintBounds());
            } 
        }
        return rect;
    }

    public Rectangle2D.Float getCenteredFanBounds() {
        return expandToCenteredBounds(getFanBounds());
    }
    

    /** get bounds that are centered on this node that fully include the given bounds */
    public Rectangle2D.Float expandToCenteredBounds(Rectangle2D.Float r) {
        // expand the given rectangle in all directions such that the distance
        // from our center point of this component to each edge is the same.

        final float cx = getCenterX();
        final float cy = getCenterY();

        final float topDiff = cy - r.y;
        final float botDiff = (r.y + r.height) - cy;
        final float leftDiff = cx - r.x;
        final float rightDiff = (r.x + r.width) - cx;

        if (topDiff > botDiff) {
            // expand below us
            r.height = topDiff * 2;
        } else if (botDiff > topDiff) {
            // expand above us
            r.y = cy - botDiff;
            r.height = botDiff * 2;
        }
        if (leftDiff > rightDiff) {
            // expand to the right
            r.width = leftDiff * 2;
        } else if (rightDiff > leftDiff) {
            // expand to the left
            r.x = cx - rightDiff;
            r.width = rightDiff * 2;
        }

        return r;
    }

    
    /** @return a list of all LWComponents at the far end of any links that are connected to us */
    public Collection<LWComponent> getLinkEndPoints() {
        // default uses a set, in case there are multiple links to the same endpoint
        return getLinkEndPoints(new HashSet(getLinks().size()));
    }

    public Collection<LWComponent> getLinkEndPoints(Collection bag)
    {
        for (LWLink link : getLinks()) {
            final LWComponent head = link.getHead();
            final LWComponent tail = link.getTail();
            if (head != this) {
                if (head != null)
                    bag.add(head);
            } else if (tail != this) {
                if (tail != null)
                    bag.add(tail);
            }
        }
        return bag;
    }

    /*
     * Return an iterator over all link endpoints,
     * which will all be instances of LWComponent.
     * If this is a LWLink, it should include it's
     * own endpoints in the list.

    public java.util.Iterator<LWComponent> getLinkEndpointsIterator()
    {
        return
            new java.util.Iterator<LWComponent>() {
                java.util.Iterator i = getLinkRefs().iterator();
                public boolean hasNext() {return i.hasNext();}
		public LWComponent next()
                {
                    LWLink l = (LWLink) i.next();
                    LWComponent head = l.getHead();
                    LWComponent tail = l.getTail();
                    
                    // Every link, as it's connected to us, should have us as one of
                    // it's endpoints -- so return the opposite endpoint.  TODO: now
                    // that links can have null endpoints, this iterator can return null
                    // -- hasNext will have to get awfully fancy to handle this.
                    
                    if (head == LWComponent.this)
                        return tail;
                    else
                        return head;
                }
		public void remove() {
		    throw new UnsupportedOperationException();
                }
            };
    }
     */    
    /*
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
        java.util.List list = new java.util.ArrayList(mLinks.size());
        java.util.Iterator i = mLinks.iterator();
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
    
    /* include all links and far endpoints of links connected to this component 
    public java.util.List getAllConnectedComponents()
    {
        List list = new java.util.ArrayList(mLinks.size());
        for (LWLink l : mLinks) {
            list.add(l);
            if (l.getHead() != this)
                list.add(l.getHead());
            else if (l.getTail() != this) // todo opt: remove extra check eventually
                list.add(l.getTail());
            else
                // todo: actually, I think we want to support these
                throw new IllegalStateException("link to self on " + this);
            
        }
        return list;
    }
*/
    
    /** get all links to us + to any descendents */
    // TODO: return immutable versions
    public List getAllLinks() {
        return getLinks();
    }

    public int countLinksTo(LWComponent c)
    {
        if (c == null)
            return 0;
        
        int count = 0;
        for (LWLink link : mLinks)
            if (link.hasEndpoint(c))
                count++;
        return count;
    }

    /** @return true if there are any links between us and the given component */
    public boolean hasLinkTo(LWComponent c)
    {
        if (c == null)
            return false;
        
        for (LWLink link : mLinks)
            if (link.hasEndpoint(c))
                return true;
        return false;
    }

    /** @return true of this component has any connections (links) to the given component.
     *  LWLink overrides to include it's endpoints in the definition of "connected" to.
     */
    public boolean isConnectedTo(LWComponent c) {
        return hasLinkTo(c);
    }
    
        
    public int countCurvedLinksTo(LWComponent c)
    {
        int count = 0;
        for (LWLink link : mLinks)
            if (link.hasEndpoint(c) && link.isCurved())
                count++;
        return count;
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

    /** @return a collection of our ancestors.  default impl returns a list with nearest ancestor first */
    public List<LWComponent> getAncestors() {
        return (List) getAncestors(new ArrayList(8));
    }

    protected Collection<LWComponent> getAncestors(Collection bag) {
        if (parent != null) {
            bag.add(parent);
            return parent.getAncestors(bag);
        } else
            return bag;
    }

    public boolean hasAncestor(LWComponent c) {
        LWComponent parent = getParent();
        if (parent == null)
            return false;
        else if (c == parent)
            return true;
        else
            return parent.hasAncestor(c);
    }

    /** @return the first ancestor, EXCLUDING this component (starting with the parent), that is of the given type, or null if none found */
    public LWComponent getParentOfType(Class clazz) {
        LWComponent parent = getParent();
        if (parent == null)
            return null;
        else
            return parent.getAncestorOfType(clazz);
    }
    
    /** @return the first ancestor, INCLUDING this component, that is of the given type, or null if none found */
    public LWComponent getAncestorOfType(Class clazz) {
        if (clazz.isInstance(this))
            return this;
        else
            return getParentOfType(clazz);
    }

    public LWComponent getTopMostAncestorOfType(Class clazz) {
        LWComponent topAncestor = getAncestorOfType(clazz);
        LWComponent nextAncestor = topAncestor;

        if (nextAncestor != null) {
            for (;;) {
                nextAncestor = nextAncestor.getParentOfType(clazz);
                if (nextAncestor != null)
                    topAncestor = nextAncestor;
                else
                    break;
                //if (DEBUG.PICK) out("nextAncestor of type " + clazz + ": " + topAncestor);
            }
        }
        
        return topAncestor;
    }
    

    /** @return by default, return the class object as returned by getClass().  Subclasses can override to provide differentiation between runtime sub-types.
     * E.g., a node class could return getClass() by default, but the constant string "textNode" for runtime instances that we
     * want the tool code to treat is coming from a different class.  Also note that supported property bits for
     * all instances with a given type token should be the same.
     */
    public Object getTypeToken() {
        // todo: should really return null if we detect this is an instance of an anonymous class
        // -- we don't want to be duplicating and using a style holder an instance of an anon
        // glass that might be overriding god knows what and affecting property setting/getting
        // Not that this will probably hurt anything: it'll never be referenced by a VueTool,
        // so we'll never see it even if it winds up in the typed style cache.
        return getClass();
    }

    /** @return the viewer margin in pixels when we're the focal -- default is 30 */
    public int getFocalMargin() {
        return 30;
    }
    
    void setScale(double scale)
    {
        if (this.scale == scale)
            return;
        final double oldScale = this.scale;
        if (DEBUG.LAYOUT) out("setScale " + scale);
        //if (DEBUG.LAYOUT) tufts.Util.printClassTrace("tufts.vue", "setScale " + scale);
        this.scale = scale;
        
        // can only do this via debug inspector right now, and is causing lots of
        // suprious events during init:
        //if (LWLink.LOCAL_LINKS && !mXMLRestoreUnderway)
        if (!mXMLRestoreUnderway)
            notify(LWKey.Scale, oldScale); // todo: make scale a real property
        
        updateConnectedLinks();
        //System.out.println("Scale set to " + scale + " in " + this);
    }
    
    /**
     * This normnally returns 1.0 (no scaling).  If VUE.RELATIVE_COORDS == false (original implementation)
     * this scale value should be the absolute desired scale: that is, a scale value concatenated
     * (multiplied by) all parent scales.  So if the parent scale is 0.5, and we also want this
     * node to be at 0.5 with it's parent, the scale value should be 0.25.  If VUE.RELATIVE_COORDS == true,
     * this returns the scale value relative to it's parent.  So for a 50% scale in it's parent,
     * it just returns 0.5, no matter what it's parent scale is set to.
     */
    public double getScale()
    {
        return this.scale;
    }
    
    /** @return the on-map scale at 100% map scale -- only different from getScale() for VUE.RELATIVE_COORDS == true */
    public double getMapScale()
    {
        if (getParent() == null)
            return getScale();
        else
            return getParent().getMapScale() * getScale();
        
//         if (VUE.RELATIVE_COORDS) {
//             if (getParent() == null)
//                 return getScale();
//             else
//                 return getParent().getMapScale() * getScale();
//         } else {
//             return getScale();
//         }
    }

    /** Convenience for returning float */ public final float getScaleF() { return (float) getScale(); }
    /** Convenience for returning float */ public final float getMapScaleF() { return (float) getMapScale(); }
    
    

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
        if (mLinks.size() > 0)
            for (LWLink link : mLinks)
                link.notifyEndpointMoved(this);
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
    
    protected void userSetFrame(float x, float y, float w, float h, MapMouseEvent e) {
        userSetFrame(x, y, w, h);
    }

    // todo: handle via disabling a location property
    public void setMoveable(boolean moveable) {
        isMoveable = moveable;
    }
        
    public boolean isMoveable() {
        return isMoveable;
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
    
//     public void userTranslate(float dx, float dy) {
//         translate(dx, dy);
//     }
    
    /** Translate this component within it's parent by the given amount */
    public void translate(float dx, float dy) {
        setLocation(this.x + dx,
                    this.y + dy);
    }

    /** Translate this component within it's parent by the given amount -- quietly w/out generating events */
    public void takeTranslation(float dx, float dy) {
        takeLocation(this.x + dx,
                     this.y + dy);
    }
    

    /** translate across the map in absolute map coordinates */
    public void translateOnMap(double dx, double dy)
    {
        // If this node exists in a scaled context, which means it's parent is scaled or
        // the parent itself is in a scaled context, we need to adjust the dx/dy for
        // that scale. The scale of this object being "dragged" by the call to
        // translateOnMap is irrelevant -- here we're concerned with it's location in
        // it's parent, not it's contents.  So we need to beef up the translation amount
        // by the context scale so drags across the map will actually stay with the
        // mouse.  E.g., if this object exists in a parent scaled down 50% (scale=0.5),
        // to move this object 2 pixels to the right in absolute top-level map
        // coordinates, we need to change it's internal location within it's parent by 4
        // pixels (2 / 0.5 = 4) to have that show up on the map (when itself displayed
        // at 100% scale) as a movement of 4 pixels.

        final double scale = getParent().getMapScale();
        if (scale != 1.0) {
            dx /= scale;
            dy /= scale;
        }
        
        translate((float) dx, (float) dy);
        
    }
    
    /** set the absolute map location -- meant to be overriden for special cases (e.g., the special selection group) */
    public void setMapLocation(double x, double y) {
        throw new UnsupportedOperationException("unimplemented in " + this);
//         final double scale = getMapScale();
//         out("map scale: " + scale);
//         if (scale != 1.0) {
//             final double oldMapX = getMapX();
//             final double oldMapY = getMapY();
//             final double dx = (x - oldMapX) * scale;
//             final double dy = (y - oldMapY) * scale;
//             setLocation((float) (oldMapX + dx),
//                         (float) (oldMapY + dy));
//         } else
//             setLocation((float) x, (float) y);
    }
    
    /**
     * Set the location of this object within it's parent. E.g., if the parent is a group or a slide,
     * setLocation(0,0) would move the component to the upper left corner of it's parent.  If the
     * parent is a map, (0,0) has no special meaning as the origin of Maps, while it does exist,
     * has no special meaning when they draw.
     */
    public void setLocation(float x, float y) {
        setLocation(x, y, this, true);
    }

    
    /** Special setLocation to permit event notification during coordinate system changes for objects not yet added to the map */
    protected void setLocation(float x, float y, LWComponent hearableEventSource, boolean issueMapLocationChangeCalls)
    {
        if (this.x == x && this.y == y)
            return;
        
        final Point2D.Float oldValue = new Point2D.Float(this.x, this.y);
        takeLocation(x, y);
        
        //if (!linkNotificationDisabled)
        //    updateConnectedLinks();
        
        if (hearableEventSource != this)
            hearableEventSource.notifyProxy(new LWCEvent(hearableEventSource, this, LWKey.Location, oldValue));
        else //if (hearableEventSource != null) // if null, skip event delivery
            notify(LWKey.Location, oldValue);

        //        if (issueMapLocationChangeCalls && parent != null) {
        if (issueMapLocationChangeCalls) {

            // NEED TO DEAL WITH COORDINATE SYSTEM CHANGES
            // And need to be able to capture old map location from our OLD parent
            // during reparenting....

            // reparenting may want to force a location in the new parent, at it's
            // current map location, but relative to the new parent's location,
            // even if it's about to be moved/laid-out elsewhere, so that once
            // we get here, the below code should always work.  Or, we could
            // even have establishLocalCoordinates call us here with extra info... (oldMapX/oldMapY)
            // or, we could implement the general setMapLocation and have establishLocalCoords call that...
            
            // This code only works if we're moving within a single parent: no coordinate system changes!

            // Would be better to merge this somehow with notifyHierarchChanged?
            
            final double scale;
            if (parent != null)
                scale = parent.getMapScale(); // we move within the scale of our parent
            else
                scale = 1.0;
            if (DEBUG.WORK) out("notifyMapLocationChanged: using scale " + scale);
            notifyMapLocationChanged((x - oldValue.x) * scale,
                                     (y - oldValue.y) * scale);
        } else {
            // this always needs to happen no matter what, even during undo
            // (e.g., the shape of curves isn't stored anywhere -- always needs to be recomputed)
            if (!linkNotificationDisabled)
                updateConnectedLinks();
        }
    }

    /** a notification to the component that it's absolute map location has changed by the given absolute map dx / dy */
    // todo: may be better named ancestorMoved or ancestorTranslated or some such
    protected void notifyMapLocationChanged(double mdx, double mdy) {
        if (!linkNotificationDisabled) // todo: if still end up using this feature, need to pass this bit on down to children
            updateConnectedLinks();
    }

    protected void notifyMapScaleChanged(double oldParentMapScale, double newParentMapScale) {}

    /** A notification to the component that it or some ancestor is about to change parentage */
    public void notifyHierarchyChanging() {}
    
    /** A notification to the component that it or some ancestor changed parentage */
    public void notifyHierarchyChanged() {
        if (mLinks.size() > 0)
            for (LWLink link : mLinks)
                link.notifyEndpointHierarchyChanged(this);
        
    }
    
    public final void setLocation(double x, double y) {
        setLocation((float) x, (float) y);
    }
    public final void setLocation(Point2D p) {
        setLocation((float) p.getX(), (float) p.getY());
    }

    /** default calls setLocation -- override to provide constraints */
    public void userSetLocation(float x, float y) {
        setLocation(x, y);
    }
    
    public void setCenterAt(Point2D p) {
        setLocation((float) p.getX() - getWidth()/2,
                    (float) p.getY() - getHeight()/2);
    }

    /** special case for mapviewer rollover zooming to skip calling updateConnectedLinks
     * If the component is temporarily zoomed, we don't want/need to update all the connected links.
     */
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
//     public Point2D getCenterPoint()
//     {
//         return new Point2D.Float(getCenterX(), getCenterY());
//     }
    
    /** set component to this many pixels in size, quietly, with no event notification */
    protected void takeSize(float w, float h)
    {
        //if (this.width == w && this.height == h)
        //return;
        if (DEBUG.LAYOUT) out("*** takeSize (LWC)  " + w + "x" + h);
        this.width = w;
        this.height = h;
    }

    protected float mAspect = 0;
    public void setAspect(float aspect) {
        mAspect = aspect;
        if (DEBUG.IMAGE) out("setAspect " + aspect);
    }
    
    /** set component to this many pixels in size */
    public void setSize(float w, float h)
    {
        if (this.width == w && this.height == h)
            return;
        if (DEBUG.LAYOUT) out("*** setSize  (LWC)  " + w + "x" + h);
        final Size old = new Size(width, height);

        if (mAspect > 0) {
            Size constrained = ConstrainToAspect(mAspect, w, h);
            w = constrained.width;
            h = constrained.height;
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

    public static Size ConstrainToAspect(float aspect, float w, float h)
    {
        // Given width & height are MINIMUM size: expand to keep aspect
            
        if (w <= 0) w = 1;
        if (h <= 0) h = 1;
        double tmpAspect = w / h; // aspect we would have if we did not constrain it
        // a = w / h
        // w = a*h
        // h = w/a
//         if (DEBUG.PRESENT || DEBUG.IMAGE) {
//             out("keepAspect=" + mAspect);
//             out(" tmpAspect=" + tmpAspect);
//         }
        //             if (h == this.height) {
        //                 out("case0");
        //                 h = (float) (w / mAspect);
        //             } else if (w == this.width) {
        //                 out("case1");
        //                 w = (float) (h * mAspect); 
        //             } else
        if (tmpAspect > aspect) {
            //out("case2: expand height");
            h = w / aspect;
        } else if (tmpAspect < aspect) {
            //out("case3: expand width");
            w = h * aspect;
        }
        //else out("NO ASPECT CHANGE");

        return new Size(w, h);

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

    
    /** default calls setSize -- override to provide constraints */
    public void userSetSize(float w, float h) {
        setSize(w, h);
    }
    protected void userSetSize(float w, float h, MapMouseEvent e) {
        userSetSize(w, h);
    }
        
    /* set on screen visible component size to this many pixels in size -- used for user set size from
     * GUI interaction -- takes into account any current scale factor
     * (do we still need this? I think this should be deprecated -- SMF)
     */

    public void setAbsoluteSize(float w, float h)
    {
        if (true||DEBUG.LAYOUT) out("*** setAbsoluteSize " + w + "x" + h);
        setSize(w / getScaleF(), h / getScaleF());
        //setSize(w / getMapScaleF(), h / getMapScaleF());
    }
    
    /** for XML restore only --todo: remove*/ public void setX(float x) { this.x = x; }
    /** for XML restore only! --todo remove*/ public void setY(float y) { this.y = y; }

    /*
     * getMapXXX methods are for values in absolute map positions and scales (needed for VUE.RELATIVE_COORDS == true)
     * getScaledXXX methods are for VUE.RELATIVE_COORDS == false, tho I think we can get rid of them?  -- SMF
     *
     * "Map" values are absolute on-screen values that are true for any component in a map rendered at 100% scale (the size & location)
     * (better naming scheme might be "getRenderXXX" or "getAbsoluteXX" ?)
     */
    
    public float getX() { return this.x; }
    public float getY() { return this.y; }
    //public float getWidth() { return this.width * getScale(); }
    //public float getHeight() { return this.height * getScale(); }
    public float getScaledWidth()       { return (float) (this.width * getScale()); }
    public float getScaledHeight()      { return (float) (this.height * getScale()); }
    public float getWidth()             { return this.width;  }
    public float getHeight()            { return this.height; }
    //public float getWidth()           { return VUE.RELATIVE_COORDS ? this.width : getScaledWidth(); }
    //public float getHeight()          { return VUE.RELATIVE_COORDS ? this.height : getScaledHeight(); }
    public float getMapWidth()          { return (float) (this.width * getMapScale()); }
    public float getMapHeight()         { return (float) (this.height * getMapScale()); }
    public float getAbsoluteWidth()     { return this.width; }
    public float getAbsoluteHeight()    { return this.height; }
    public float getBoundsWidth()       { return (float) ((this.width + mStrokeWidth.get()) * getScale()); }
    public float getBoundsHeight()      { return (float) ((this.height + mStrokeWidth.get()) * getScale()); }
    public float getScaledBoundsWidth() { return (float) ((this.width + mStrokeWidth.get()) * getScale()); }
    public float getScaledBoundsHeight() { return (float) ((this.height + mStrokeWidth.get()) * getScale()); }
    //public float getBoundsWidth() { return (this.width + this.strokeWidth);  }
    //public float getBoundsHeight() { return (this.height + this.strokeWidth); }
    //public float getBoundsWidth() { return (this.width + this.strokeWidth) * getScale(); }
    //public float getBoundsHeight() { return (this.height + this.strokeWidth) * getScale(); }

    /*
    private class ParentIterator implements Iterator<LWContainer>, Iterable<LWContainer> {
        final LinkedList<LWContainer> list = new LinkedList();
        ParentIterator() {
            LWContainer parent = getParent();
            do {
                list.addFirst(parent);
                parent = parent.getParent();
            } while (parent != null);
            list.removeFirst();
        }
        public LWContainer next() { return null; }
        public boolean hasNext() { return false; }
        public void remove() { throw new UnsupportedOperationException(toString()); }
        public Iterator<LWContainer> iterator() { return list.iterator(); }
    }
    */


    protected double getMapXPrecise()
    {
        if (parent == null)
            return getX();
        else
            return parent.getMapXPrecise() + getX() * parent.getMapScale();
    }
    protected double getMapYPrecise() {
        if (parent == null)
            return getY();
        else
            return parent.getMapYPrecise() + getY() * parent.getMapScale();
    }

    public float getMapX() {
        return (float) getMapXPrecise();
    }
    
    public float getMapY() {
        return (float) getMapYPrecise();
    }

    /** @return center x of the component in absolute map coordinates */
    public float getCenterX() {
        return getMapX() + getMapWidth() / 2;
    }
    /** @return center y of the component in absolute map coordinates */
    public float getCenterY() {
        return getMapY() + getMapHeight() / 2;
    }

//     // these two don't handle scale properly yet: need to adjust for parent scales...
//     protected float getCenterX(LWContainer ancestor) {
//         return (float) getAncestorX(ancestor) + getScaledWidth() / 2;
//     }
//     protected float getCenterY(LWContainer ancestor) {
//         return (float) getAncestorY(ancestor) + getScaledHeight() / 2;
//     }

//     // these two don't handle scale properly yet
//     public float getLinkConnectionX(LWContainer ancestor) {
//         //return getCenterX(ancestor);
//         return (float) getAncestorX(ancestor) + getScaledWidth() / 2;
//     }
//     public float getLinkConnectionY(LWContainer ancestor) {
//         //return getCenterY(ancestor);
//         return (float) getAncestorY(ancestor) + getScaledHeight() / 2;
//     }

    protected void getLinkConnectionCenterRelativeTo(Point2D.Float point, LWContainer relative)
    {
        //if (relative == null) Util.printStackTrace("null relative for " + this + ": " + relative);

        final float scale = getMapScaleF();

        if (relative == this) {
            
            point.x = getLocalCenterX() * scale;
            point.y = getLocalCenterY() * scale;
            
        } else if (relative == null || relative == getParent()) {

            // if relative is null, just return available local data w/out accessing the parent.
            // This can happen normally during init.

            if (this instanceof LWLink) {
                point.x = getLocalCenterX();
                point.y = getLocalCenterY();
            } else {
                point.x = getX() + getLocalCenterX() * scale;
                point.y = getY() + getLocalCenterY() * scale;
            }

        } else {

            if (this instanceof LWLink) {
                // todo: consider getMapX/Y on LWLink override to return getParent().getMapX/Y (need to check all calls tho...)
                point.x = getParent().getMapX() + getLocalCenterX() * scale;
                point.y = getParent().getMapY() + getLocalCenterY() * scale;
            } else {
                point.x = getMapX() + getLocalCenterX() * scale;
                point.y = getMapY() + getLocalCenterY() * scale;
            }

            // point now has map coords -- now make relative to desired component
            // (the x/y needed if drawn in the component, that produces the same
            // ultimate map location).  Normally, relative should always
            // be one of our ancestors, as this is for special link code that
            // should only ever be interested in an ancestor value, tho we compute
            // it generically just in case.


            if (DEBUG.Enabled) {
                if (relative != null && !hasAncestor(relative)) {
                    // only if not the special invisible link endpoint, which has no parent (thus no ancestors)
                    if (getClass().getEnclosingClass() != LinkTool.LinkModeTool.class)
                        Util.printStackTrace("debug warning: " + this + " is computing link connetion center relative to a non-ancestor: " + relative);
                }
            }
            

            relative.transformMapToLocalPoint(point, point);
        }
    }

    protected float getLocalCenterX() {
        return getWidth() / 2;
    }
    protected float getLocalCenterY() {
        return getHeight() / 2;
    }
    


    //-----------------------------------------------------------------------------
    // experimental relatve-to-a-given-ancestor coord fetchers
    // TODO: NOT WORTH THE TROUBLE RIGHT NOW OF USING THE ANCESTOR OPTIMIZATION:
    // Just get the freakin mapx of the desired relative-to component --
    // someday those values may be cached in the object/transform anyway.
    // Oh tho -- I think in LWLink we need the mapX of US, plus the mapX of the target
    // (if KEEP the ancestor code, implement generically so can pass in any value: e.g, LWLink.mCurveCenterX)
    //-----------------------------------------------------------------------------
    

    protected double getAncestorX(LWContainer ancestor) {
        if (ancestor == parent) // quick check for the common case
            return getX();
        else if (parent == null) {
             Util.printStackTrace("didn't find ancestor " + ancestor + " for " + this);
             return getX();
        } else
            return parent.getAncestorX(ancestor) + getX() * parent.getMapScale();
    }
    
    protected double getAncestorY(LWContainer ancestor) {
        if (ancestor == parent) // quick check for the common case
            return getY();
        else if (parent == null) {
             Util.printStackTrace("didn't find ancestor " + ancestor + " for " + this);
             return getY();
        } else
            return parent.getAncestorY(ancestor) + getY() * parent.getMapScale();
    }
    

//     protected double ancestorY(double y, LWContainer ancestor) {
//         if (ancestor == parent) // quick check for the common case
//             return y;
//         else if (parent == null) {
//              Util.printStackTrace("didn't find ancestor " + ancestor + " for " + this);
//              return y;
//         } else
//             return parent.ancestorY(y, ancestor) + getY() * parent.getMapScale();
//     }
    

    
    //-----------------------------------------------------------------------------
    //-----------------------------------------------------------------------------
    //-----------------------------------------------------------------------------
    


    // these 2 for persistance ONLY -- they don't deliver detectable events!
    /** for persistance ONLY */
    public void setAbsoluteWidth(float w) { this.width = w; }
    /** for persistance ONLY */
    public void setAbsoluteHeight(float h) { this.height = h; }
    
    /* @return true if when this this component draws, it draws and picks on the map as a whole, not relative to it's coordinate space (default is false) */
    //public boolean hasAbsoluteMapLocation() { return false; }
    
    /* @return true if when this this component draws, it draws and picks relative to it's parent's coordinate space (default is false) */
    //public boolean hasParentLocation() { return false; }
    
    /** @return uri
     * returns an unique uri for a component. If component already has one it is returned else an new uri is created and returned.
     * At present uris will be created through rdf index
     */
    public URI getURI() {
        if(uri == null) {
            try {
                    uri = new URI(edu.tufts.vue.rdf.RDFIndex.getUniqueId());
                } catch (Throwable t) {
                    tufts.Util.printStackTrace(t, "Failed to create an uri for  "+label);
                }
           
        }
        return uri;
    }
    
    public void setURI(URI uri) {
       this.uri = uri; 
    }
    /*
    public void setShape(Shape shape)
    {
        throw new UnsupportedOperationException("unimplemented setShape in " + this);
    }
    */


    /** @return our shape, full transformed into map coords and ultimate scale when drawn at 100% map zoom
     * this is used for portal clipping, and will be imperfect for some scaled shapes, such as RountRect's
     * This only works for raw shapes that are RectangularShapes -- other Shape types just return the map bounds
     * (e.g., a link shape) */
    public Shape getMapShape()
    {
        // Will not work for shapes like RoundRect when scaled -- e..g, corner scaling will be off
            
        final Shape s = getLocalShape();
        //        if (getMapScale() != 1f && s instanceof RectangularShape) { // todo: do if any transform, not just scale
        if (s instanceof RectangularShape) {
            // todo: cache this: only need to updaate if location, size or scale changes
            // (Also, on the scale or location change of any parent!)
            RectangularShape rshape = (RectangularShape) s;
            rshape = (RectangularShape) rshape.clone();
            AffineTransform a = getLocalTransform();
            Point2D.Float loc = new Point2D.Float();
            a.transform(loc, loc);
            rshape.setFrame(loc.x, loc.y,
                            rshape.getWidth() * a.getScaleX(),
                            rshape.getHeight() * a.getScaleY());
            //System.out.println("TRANSFORMED SHAPE: " + rshape + " for " + this);
            return rshape;
        } else {
            return getBounds();
        }
    }

    /*
     * Return internal bounds of the border shape, not including
     * the width of any stroked border.
    // TODO: do we need getShapeBounds??
    public Rectangle2D.Float getShapeBounds()
    {
        // todo opt: cache this object?
        if (VUE.RELATIVE_COORDS)
            return new Rectangle2D.Float(0, 0, getWidth(), getHeight());
        else
            return new Rectangle2D.Float(this.x, this.y, getWidth(), getHeight());
        //return new Rectangle2D.Float(this.x, this.y, getAbsoluteWidth(), getAbsoluteHeight());
    }
     */

    // TODO: may also need getRenderBounds: what bounds for printing would use
    // -- needs borders, but NO room for selection strokes (wait, but pathway strokes???)
    // screw it: we can get by with getPaintedBounds for this...
    
    ///** @DEPRECATED -- TODO: remove*/
    //public Rectangle2D.Float getShapeBounds() { return getBounds(); }

    /** return border shape of this object.  If VUE.RELATIVE_COORDS, it's raw and zero based,
        otherwise, with it's location in map coordinates  */
    private Shape getShape()
    {
        //return VUE.RELATIVE_COORDS ? getRawShape() : getShapeBounds();
        return getLocalShape();
    }

    /** @return the raw, zero based, non-scaled shape; default impl returns getLocalBounds */
    public Shape getLocalShape() {
        return getLocalBounds();
    }
    
    /** @return the raw, zero based, non-scaled bounds */
    private Rectangle2D.Float getLocalBounds() {
        return new Rectangle2D.Float(0, 0, getAbsoluteWidth(), getAbsoluteHeight());
    }
    
//     /** @return the parent based, non-scaled bounds.  If the this component has absolute map location, we return getBounds() */
//     public Rectangle2D.Float getParentLocalBounds() {
//         if (hasAbsoluteMapLocation())
//             return getBounds();
//         else
//             return new Rectangle2D.Float(getX(), getY(), getMapWidth(), getMapHeight());
//     }

//     public Rectangle2D.Float getParentLocalPaintBounds() {
//         if (hasAbsoluteMapLocation())
//             return getBounds();
//         else
//             return addStrokeToBounds(getParentLocalBounds(), 0);
//     }

    
    
    
    /** @return map-coord (absolute) bounds of the stroke shape (not including any stroke width) */
    public Rectangle2D.Float getBounds()
    {
        return new Rectangle2D.Float(getMapX(), getMapY(), getMapWidth(), getMapHeight());
    }

    protected Rectangle2D.Float addStrokeToBounds(Rectangle2D.Float r, float extra)
    {
        float strokeWidth = getStrokeWidth();
        
        if (strokeWidth > 0) {
            strokeWidth *= getMapScale();
            final float exteriorStroke = strokeWidth / 2;
            r.x -= exteriorStroke;
            r.y -= exteriorStroke;
            r.width += strokeWidth;
            r.height += strokeWidth;
        }
        return r;
    }
    
    /**
     * Return absolute map bounds for hit detection & clipping.  This will vary
     * depenending on current stroke width, if in a visible pathway,
     * etc.
     */
    public Rectangle2D.Float getPaintBounds()
    {
        if (inDrawnPathway())
            return addStrokeToBounds(getBounds(), LWPathway.PathwayStrokeWidth);
        else
            return addStrokeToBounds(getBounds(), 0);
    }

//     public Rectangle2D.Float getPaintBounds()
//     {
//         //if (VUE.RELATIVE_COORDS) return new Rectangle2D.Float(0, 0, getWidth(), getHeight());
        
//         // todo opt: cache this object?
//         final Rectangle2D.Float b = getBounds();
//         float strokeWidth = getStrokeWidth();
        
//         if (inDrawnPathway())
//             strokeWidth += LWPathway.PathwayStrokeWidth;

// //         if (VUE.RELATIVE_COORDS) {
// //             //b = new Rectangle2D.Float(getMapX(), getMapY(), getMapWidth(), getMapHeight());
// //             b = getBounds();
// //         } else {
// //             b = new Rectangle2D.Float(this.x, this.y, getMapWidth(), getMapHeight());
// //         }


//         // we need this adjustment for repaint optimzation to
//         // work properly -- would be a bit cleaner to compensate
//         // for this in the viewer
//         //if (isIndicated() && STROKE_INDICATION.getLineWidth() > strokeWidth)
//         //    strokeWidth = STROKE_INDICATION.getLineWidth();

//         if (strokeWidth > 0) {
//             if (VUE.RELATIVE_COORDS)
//                 strokeWidth *= getMapScale();
//             final float adj = strokeWidth / 2;
//             b.x -= adj;
//             b.y -= adj;
//             b.width += strokeWidth;
//             b.height += strokeWidth;
//         }
//         return b;
//     }
    

    protected static final AffineTransform IDENTITY_TRANSFORM = new AffineTransform();
    

    /** @return an AffineTransform that when applied to a graphics context, will have us drawing properly
     * relative to this component, including any applicable scaling */
    //create and recursively set a transform to get from the Map to this object's coordinate space
    // note: structure is same in the different transform methods
    // TODO OPT: can cache this transform if track all ancestor hierarcy, location AND scale changes
    public AffineTransform getLocalTransform() {
        final AffineTransform a;
        if (parent == null) {
            a = new AffineTransform();
        } else {
            a = parent.getLocalTransform();
        }
        return transformDown(a);
    }

    /**
     * @return the transform that takes us from the given ancestor down to our local coordinate space/scale
     * @param ancestor -- the ancestor to get a transform relative to.  If null, this will return the
     * same result as getLocalTransform (relative to the map)
     */
    protected AffineTransform getRelativeTransform(LWContainer ancestor) {

        if (parent == ancestor || parent == null)
            return transformDown(new AffineTransform());
        else
            return transformDown(parent.getRelativeTransform(ancestor));
    }
    

    /** transform the given AffineTransform down from our parent to us, the child */
    protected AffineTransform transformDown(final AffineTransform a) {
        a.translate(getX(), getY());
        final double scale = getScale();
        if (scale != 1)
            a.scale(scale, scale);
        //if (parent instanceof LWMap) a.rotate(Math.PI / 16); // test
        return a;
    }

    /** transform relative to the child after already being transformed relative to the parent */
    public void transformRelative(final Graphics2D g) {
        if (!VUE.RELATIVE_COORDS) throw new Error("non-relative coordinate impl!");

        g.translate(getX(), getY());
        final double scale = getScale();
        if (scale != 1)
            g.scale(scale, scale);
        //if (parent instanceof LWMap) g.rotate(Math.PI / 16); // test
    }

    /** Will transform all the way from the the map down to the component, wherever nested/scaled */
    public void transformLocal(final Graphics2D g) {
        
        // todo: need a relative to parent transform only for cascading application during drawing
        // (and ultimate picking when impl is optimized)
            
        if (getParent() == null) {
            ;
        } else {
            getParent().transformLocal(g);
        }
        
        transformRelative(g);

    }

    /**
     * @param mapPoint, a point in map coordinates to transform to local coordinates
     * @param nodePoint the destination Point2D to place the resulting transformed coordinate -- may be
     * the same object as mapPoint (it will be written over)
     * @return the transformed point (will be nodePoint if transformed, mapPoint if no transformation was needed,
     * although mapPoint x/y values should stil be copied to nodePoint)
     */
    public Point2D transformMapToLocalPoint(Point2D.Float mapPoint, Point2D.Float nodePoint) {
        try {
            getLocalTransform().inverseTransform(mapPoint, nodePoint);
        } catch (java.awt.geom.NoninvertibleTransformException e) {
            Util.printStackTrace(e);
        }
        return nodePoint;
        
    }

    /** @param mapRect will be transformed (written over) and returned */
    protected Rectangle2D transformMapToLocalRect(Rectangle2D mapRect) {

        if (getParent() instanceof LWMap) {
            // this is an optimization we'll want to remove if we ever
            // embed maps in maps
            return mapRect;
        }

        final AffineTransform tx = getLocalTransform();
        double[] points = new double[8]; // todo opt: can do as len 4 & overwrite
        points[0] = mapRect.getX();
        points[1] = mapRect.getY();
        points[2] = points[0] + mapRect.getWidth();
        points[3] = points[1] + mapRect.getHeight();
        try {
            tx.inverseTransform(points, 0, points, 4, 2);
        } catch (java.awt.geom.NoninvertibleTransformException e) {
            Util.printStackTrace(e);
        }

        mapRect.setRect(points[4],
                        points[5],
                        points[6] - points[4],
                        points[7] - points[5]
                        );
//         mapRect.x = (float) points[4];
//         mapRect.y = (float) points[5];
//         mapRect.width = (float) (points[6] - points[4]);
//         mapRect.height = (float) (points[7] - points[5]);

        return mapRect;
        
    }


    /**
     * This will take the given rectangle in local coordinates, and transform it
     * into map coordinates.  The passed in Rectangle2D.Float will be modified
     * and returned.
     */
    public Rectangle2D.Float transformLocalToMapRect(Rectangle2D.Float rect) {
        final double scale = getMapScale();
        if (scale != 1) {
            rect.x *= scale;
            rect.y *= scale;
            rect.width *= scale;
            rect.height *= scale;
        }
        if (this instanceof LWLink) {
            // todo: eventually rewrite this routine entirely to use the transformations
            // (will need that if ever want to handle rotation, as well as to skip this
            // special case for links).
            rect.x += getParent().getMapX();
            rect.y += getParent().getMapY();
        } else {
            rect.x += getMapX();
            rect.y += getMapY();
        }
        
        return rect;
    }
                
    
    
    
    
    /**
     * Default implementation: checks bounding box
     * Subclasses should override and compute via shape.
     * INTERSECTIONS always intersect based on map bounds, as opposed to contains, which tests a local point.
     */
    public final boolean intersects(Rectangle2D rect)
    {
        final boolean hit = intersectsImpl(rect);
        //if (DEBUG.PAINT) System.out.println("INTERSECTS " + fmt(rect) + " " + (hit?"YES":"NO ") + " for " + fmt(getPaintBounds()) + " " + this);
        
        if (hit)
            return true;
        else if (isDrawingSlideIcon() && getMapSlideIconBounds().intersects(rect))
            return true;
        else
            return false;
    }

    /** @return true if this component currently requires painting and intersects the master paint region */
    public boolean requiresPaint(DrawContext dc)
    {
        if (dc.skipDraw == this)
            return false;

        // always draw the focal
        if (dc.focal == this)
            return true;
        
        // if filtered, don't draw, unless has children, in which case
        // we need to draw just in case any of the children are NOT filtered.
        if (isHidden() || (isFiltered() && !hasChildren()))
            return false;

        // Not currently used:
        //if (getLayer() > dc.getMaxLayer())
        //    return false;

        if (!dc.isClipOptimized()) {
            // If we're drawing raw, always draw everything, don't
            // check against the master "map" clip rect, as that's only
            // for drawing map elements (e.g., we may be drawing
            // a LWComponent that's a decoration or GUI element,
            // like a navigation node, or a master slide background).
            return true;
        }

        if (intersects(dc.getMasterClipRect()))
            return true;

        if (isDrawingSlideIcon())
            return getMapSlideIconBounds().intersects(dc.getMasterClipRect());
        else
            return false;
    }
    

    /** default impl intersects the render/paint bounds, including any borders (we use this for draw clipping as well as selection) */
    protected boolean intersectsImpl(Rectangle2D mapRect) {
        //if (DEBUG.CONTAINMENT) System.out.println("INTERSECTS " + Util.fmt(rect));
        final Rectangle2D bounds = getPaintBounds();
        final boolean hit = mapRect.intersects(bounds);
        if (DEBUG.PAINT || DEBUG.PICK) System.out.println("INTERSECTS " + fmt(mapRect) + " " + (hit?"YES":"NO ") + " for " + fmt(bounds) + " of " + this);
        //Util.printClassTrace("tufts.vue.LW", "INTERSECTS " + this);
        return hit;
    }
    
//     /**
//      * Does x,y fall within the selection target for this component.
//      * This default impl adds a 30 pixel swath to bounding box.
//      */
//     public boolean targetContains(float x, float y)
//     {
//         final int swath = 30; // todo: preference
//         float sx = this.x - swath;
//         float sy = this.y - swath;
//         float ex = this.x + getWidth() + swath;
//         float ey = this.y + getHeight() + swath;
        
//         return x >= sx && x <= ex && y >= sy && y <= ey;
//     }

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
            //if (!dc.isFocused && path.isDrawn()) {
            if (path.isDrawn()) {
                path.drawComponentDecorations(dc.create(), this);
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
                transformLocal(dc.g);
                dc.g.draw(getLocalShape());
            }
        }
    }
    
    public final boolean contains(Point2D p, float zoom) {
        return contains((float)p.getX(), (float)p.getY(), zoom);
    }

    /** @return true if the given x/y (already transformed to our local coordinate space), is within our shape */
    public final boolean contains(float x, float y) {
        return contains(x, y, 1f);
    }
    
    public final boolean contains(float x, float y, float zoom) {
        if (containsImpl(x, y, zoom))
            return true;
        else if (isDrawingSlideIcon()) {
            if (DEBUG.PICK) out("Checking slide icon bounds " + getSlideIconBounds());
            return getSlideIconBounds().contains(x, y);
        } else
            return false;
    }

    /** @return 0 means a hit, -1 a completely miss, > 0 means distance, to be sorted out by caller  */
    protected float pickDistance(float x, float y, float zoom) {
        return contains(x, y, zoom) ? 0 : -1;
    }

    /**
     * Default implementation: checks bounding box, including any stroke width.
     * Subclasses should override for more accurate hit detection.
     */
    protected boolean containsImpl(float x, float y, float zoom)
    {
        final float stroke = getStrokeWidth() / 2;
        
        return x >= -stroke
            && y >= -stroke
            && x <= getWidth() + stroke
            && y <= getHeight() + stroke;
    }

    /** For using a node in a non-map context (e.g., as an on-screen button) */
    // todo: this is bounding box only: odd shapes will have imperfect hit detection
    // also, if we ever add rotation of arbitrary LWComponents, this won't handle it --
    // will need need to dump this hack and do all in LWTraversal, or have the
    // local LWComponent contains/intersects code adjust for the local transformation
    // themselves.
    public boolean containsParentCoord(float x, float y) {
        return x >= this.x
            && y >= this.y
            && x <= (this.x+getScaledWidth())
            && y <= (this.y+getScaledHeight());
    }
    

    private final float SlideScale = 0.125f;
    private Rectangle2D.Float mSlideIconBounds;
    public Rectangle2D.Float getSlideIconBounds() {
        if (mSlideIconBounds == null)
            mSlideIconBounds = computeSlideIconBounds(new Rectangle2D.Float());
        else if (true || mSlideIconBounds.x == Float.NaN) // need a reshape/reshapeImpl trigger on move/resize to properly re-validate (wait: NaN != NaN !)
            computeSlideIconBounds(mSlideIconBounds);
        return mSlideIconBounds;
    }

    public Rectangle2D.Float getMapSlideIconBounds() {
        if (!VUE.RELATIVE_COORDS)
            return getSlideIconBounds();
        
        Rectangle2D.Float slideIcon = (Rectangle2D.Float) getSlideIconBounds().clone();
        final float scale = getMapScaleF();
        // Compress the local slide icon coords into the node's scale space:
        slideIcon.x *= scale;
        slideIcon.y *= scale;
        // Now make them absolute map coordintes (no longer local):
        slideIcon.x += getMapX();
        slideIcon.y += getMapY();
        // Now scale down size:
        slideIcon.width *= scale;
        slideIcon.height *= scale;

        return slideIcon;
    }

    /** @return the local lower right hand corner of the component: for rectangular shapes, this is just [width,height]
     * Non-rectangular shapes can override to do something fancier. */
    protected Point2D.Float getCorner() {
        return new Point2D.Float(getWidth(), getHeight());
    }

    protected Rectangle2D.Float computeSlideIconBounds(Rectangle2D.Float rect)
    {
        final float width = LWSlide.SlideWidth * SlideScale;
        final float height = LWSlide.SlideHeight * SlideScale;

        Point2D.Float corner = getCorner();
        
        float xoff = corner.x - 60;
        float yoff = corner.y - 60;

        // If shape is small, try and keep it from overlapping too much (esp the label)
        if (xoff < getWidth() / 2f)
            xoff = getWidth() / 2f;
        if (yoff < getHeight() * 0.75f)
            yoff = getHeight() * 0.75f;

        // This can happen for wierd shapes (e.g., shield)
        if (xoff > corner.x)
            xoff = corner.x;
        if (yoff > corner.y)
            yoff = corner.y;

        if (VUE.RELATIVE_COORDS) {
            rect.setRect(xoff,
                         yoff,
                         width,
                         height);
        } else {
            rect.setRect(getX() + xoff,
                         getY() + yoff,
                         width,
                         height);
        }
        
        return rect;
    }

    /** If there's a pathway entry we want to be showing, return it, otherwise, null */
    LWPathway.Entry getEntryToDisplay()
    {
        LWPathway path = VUE.getActivePathway();

        if (!inPathway(path)) {
            if (pathwayRefs != null && pathwayRefs.size() > 0)
                path = pathwayRefs.get(0); // show the first pathway it's in if it's not in the active pathway
            else
                path = null;
        }
            
        if (path != null && path.isShowingSlides()) {
            final LWPathway.Entry entry = path.getCurrentEntry();
            // This is just in case the node is in the pathway more than once: if it is,
            // and the current entry is for this node, use that, otherwise, just
            // use the first entry for the the node.
            if (entry != null && entry.node == this)
                return entry;
            else
                return path.getEntry(path.firstIndexOf(this));
        }
        return null;
    }

    public boolean isDrawingSlideIcon() {
        final LWPathway.Entry entry = getEntryToDisplay();
        return entry != null && !entry.isMapView;
    }
    
    /**
     * For every component, draw any needed pathway decorations and related slide icons,
     * and then invoke drawImpl for the sub-component.  Intended for use in LWContainer,
     * where the parent has already been drawn, and already transformed the DrawContext
     * to it's local region.
     */
    public void drawInParent(DrawContext dc)
    {
        // this will cascade to all children when they draw, combining with their calls to transformRelative
        transformRelative(dc.g);
        
        final AffineTransform saveTransform = dc.g.getTransform();

        if (dc.focal == this || dc.isFocused())
            drawRaw(dc);
        else
            drawDecorated(dc);

        if (DEBUG.BOXES) {
            //if (!hasAbsoluteMapLocation()) {
            if (!(this instanceof LWLink)) {
                
                dc.g.setTransform(saveTransform);
                
                // scaling testing -- draw an exactly 8x8 pixel (rendered) box
                //dc.g.setStroke(STROKE_ONE); // make sure stroke is set to 1!
                dc.setAbsoluteStroke(1); // make sure stroke is set to 1!
                dc.g.setColor(Color.green);
                dc.g.drawRect(0,0,7,7);

                // show the center-point to corner intersect line (debug slide icon placement):
                dc.g.setColor(Color.red);
                //dc.setAbsoluteStroke(1);
                dc.g.setStroke(STROKE_ONE);
                dc.g.draw(new Line2D.Float(new Point2D.Float(getWidth()/2, getHeight()/2), getCorner()));

                if (DEBUG.LINK && isSelected() && getLinks().size() > 0) {
                    final Rectangle2D.Float pureFan = getFanBounds();
                    final Rectangle2D.Float fan = getCenteredFanBounds();
                    final float cx = getCenterX();
                    final float cy = getCenterY();
                    final Line2D xaxis = new Line2D.Float(fan.x, cy, fan.x + fan.width, cy);
                    final Line2D yaxis = new Line2D.Float(cx, fan.y, cx, fan.y + fan.height);
                    dc.setMapDrawing();
                    dc.setAbsoluteStroke(4);
                    //dc.g.setColor(getRenderFillColor(dc));
                    dc.g.setColor(Color.blue);
                    dc.g.draw(pureFan);

                    dc.setAbsoluteStroke(2);
                    dc.g.setColor(Color.red);
                    dc.g.draw(fan);
                    dc.g.draw(xaxis);
                    dc.g.draw(yaxis);
                    
                }
            }
        }
        
    }

    /**
     * for directly forcing the drawing or redrawing a single component at it's proper map location
     * If you are going to use the passed in DrawContext after this call for other map drawing operations,
     * be sure to pass in dc.create() from the caller, as this call leaves it in an undefined state.
     **/
    public void draw(DrawContext dc) {
        dc.setClipOptimized(false); // ensure all children draw even if not inside clip
        transformLocal(dc.g);
        if (dc.focal == this) {
            drawRaw(dc);
        } else {
            if (true) {
                if (dc.drawPathways())
                    drawPathwayDecorations(dc);
                drawRaw(dc);
            } else {
                drawDecorated(dc); // to heavy for now: don't want slide icons
            }
        }
    }
    
    public void drawRaw(DrawContext dc) {
        dc.checkComposite(this);
        drawImpl(dc);
    }
    
    protected void drawDecorated(DrawContext dc)
    {
        final LWPathway.Entry entry = getEntryToDisplay();
        //final boolean drawSlide = (entry != null);
        final boolean drawSlide = (entry != null && !entry.isMapView);

        if (dc.drawPathways() && dc.focal != this)
            drawPathwayDecorations(dc);

        if (drawSlide) {

            drawRaw(dc);

            final LWSlide slide = entry.getSlide();
            
            //double slideX = getCenterX() - (slide.getWidth()*slideScale) / 2;
            //double slideY = getCenterY() - (slide.getHeight()*slideScale) / 2;
            //dc.g.translate(slideX, slideY);

            Rectangle2D.Float slideFrame = getSlideIconBounds();

            //slide.setLocation(slideFrame.x, slideFrame.y);

            dc.setClipOptimized(false);
            dc.g.translate(slideFrame.x, slideFrame.y);
            dc.g.scale(SlideScale, SlideScale);

            // A hack so that when LWLinks (hasAbsoluteMapLocation) pop to map drawing, they
            // don't pop up beyond this point.
            dc.mapTransform = dc.g.getTransform();
            
            //dc.g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f));
            //entry.pathway.getMasterSlide().drawImpl(dc);
            slide.drawImpl(dc);

            //Rectangle2D border = slideFrame;
            // todo: move to LWSlide.drawImpl:
            Rectangle2D border = slide.getBounds();
            final Color slideFill = slide.getRenderFillColor(dc);
            final Color iconBorder;
            // todo: create a contrastColor, which node icon border's can also use
            if (brightness(slideFill) == 0)
                iconBorder = Color.gray;
            else if (brightness(slideFill) > 0.5)
                iconBorder = slideFill.darker();
            else
                iconBorder = slideFill.brighter();
            //out("slideFillr: " + slideFill);
            //out("iconBorder: " + iconBorder);
            dc.g.setColor(iconBorder);
            dc.g.setStroke(VueConstants.STROKE_SEVEN);
            dc.g.draw(border);

        } else {

            //if (entry != null && !dc.isFocused) {
            if (entry != null) {
                // if we had an entry, but it was a map-view slide, do something to make it look slide-like
                dc.g.setColor(entry.pathway.getMasterSlide().getFillColor());
                if (entry.node instanceof LWGroup) {
                    if (!dc.isPresenting())
                        dc.g.fill(entry.node.getLocalBounds());
                } else if (dc.focal != this && entry.node.isTranslucent()) {
                    Area toFill = new Area(entry.node.getLocalBounds());
                    toFill.subtract(new Area(entry.node.getLocalShape()));
                    dc.g.fill(toFill);
                }
            }
            
            drawRaw(dc);
        }
    }

    protected void drawImpl(DrawContext dc) {}

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

    /** convenince method for remove a (possible) old listener, and attaching a (possible) new listener */
    public static void swapLWCListener(Listener listener, LWComponent oldSource, LWComponent newSource) {
        if (oldSource != null)
            oldSource.removeLWCListener(listener);
        if (newSource != null)
            newSource.addLWCListener(listener);
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
            //mCachedImage = null;
        }
        mChangeSupport.notifyListeners(this, e);

        if (getParent() != null && e.key instanceof Key) {
            // if parent is null, we're still initializing
            final Key key = (Key) e.key;

            if (isStyle && key.isStyleProperty)
                updateStyleWatchers(key, e);
            
            if (key.type == KeyType.DATA)
                syncUpdate(key);
        }

        // labels need own call to this due to TextBox use of setLabel0
    }

    
    /** Copy the value for the given key either back to our sync source, or to our sync clients */
    private boolean syncUnderway = false;
    private void syncUpdate(Key key) {

        if (syncUnderway)
            return;
        
        syncUnderway = true;
        try {
            doSyncUpdate(key);
        } finally {
            syncUnderway = false;
        }
    }
    
    protected void doSyncUpdate(Key key) {
        // currently we only allow one or the other: you can be a source, or a client
        // this is all we need for now (a node can be synced to nodes on multiple
        // slides on different pathways, but a node in a slide can only refer
        // back to one source)
        if (mSyncSource != null) {
            out("UPDATING SYNC SOURCE " + mSyncSource + " for " + key);
            if (!mSyncSource.isDeleted())
                key.copyValue(this, mSyncSource);

        } else if (mSyncClients != null && !mSyncClients.isEmpty()) {
            
            for (LWComponent c : mSyncClients) {
                out("UPDATING SYNC CLIENT " + c + " for " + key);
                if (!c.isDeleted())
                    key.copyValue(this, c);
            }
        }
    }

    /** If the event is a change for a style property, apply the change to all
        LWComponents that refer to us as their style parent */
    protected void updateStyleWatchers(Key key, LWCEvent e)
    {
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
    protected void notify(String what, List<LWComponent> componentList)
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
            //throw new IllegalStateException("Attempt to restore already restored: " + this);
            if (DEBUG.Enabled) out("FYI: already restored");
            //return;
        }
        // There is no reconnectToLinks: link endpoint connect events handle this.
        // We couldn't do it here anyway as we wouldn't know which of the two endpoint to connect us to.
        setDeleted(false);
    }

    public boolean isDeleted() {
        return isHidden(HideCause.DELETED);
    }
    
    private void setDeleted(boolean deleted) {
        if (deleted) {
            mHideBits |= HideCause.DELETED.bit; // direct set: don't trigger notify
            if (DEBUG.PARENTING||DEBUG.UNDO||DEBUG.EVENTS)
                if (parent != null) out("parent not yet null in setDeleted true (ok for undo of creates)");
            this.parent = null;
        } else
            mHideBits &= ~HideCause.DELETED.bit; // direct set: don't trigger notify
    }

    private void disconnectFromLinks()
    {
        // iterate through copy of the list, as it may be modified concurrently during removals
        for (LWLink link : mLinks.toArray(new LWLink[mLinks.size()]))
            link.disconnectFrom(this);
        clearHidden(HideCause.PRUNE);
     }
    
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    
    public boolean isSelected() {
        return this.selected;
    }

    protected boolean selectedOrParent() {
        return parent == null ? isSelected() : (parent.selectedOrParent() | isSelected());
    }
    
    public boolean isAncestorSelected() {
        return parent == null ? false : parent.selectedOrParent();
    }

    private void setHideBits(int bits) {
        final boolean wasHidden = isHidden();
        mHideBits = bits;
        if (wasHidden != isHidden())
            notify(LWKey.Hidden);
    }

    /** debug -- names of set HideBits */
    String getDescriptionOfSetBits() {
        StringBuffer buf = new StringBuffer();
        for (HideCause reason : HideCause.values()) {
            if (isHidden(reason)) {
                if (buf.length() > 0)
                    buf.append(',');
                buf.append(reason);
            }
        }
        return buf.toString();
    }
    
    public void setVisible(boolean visible) {
        setHidden(HideCause.DEFAULT, !visible);
    }
    
    public void setHidden(HideCause cause, boolean hide) {
        if (hide)
            setHidden(cause);
        else
            clearHidden(cause);
    }
    
    public void setHidden(HideCause cause) {
        setHideBits(mHideBits | cause.bit);
    }
    
    public void clearHidden(HideCause cause) {
        setHideBits(mHideBits & ~cause.bit);
    }

    /**
     * @return true if this component has been hidden.  Note that this
     * is different from isFiltered.  All children of a hidden component
     * are also hidden, but not all children of a filtered component
     * are hidden.
     */
    public final boolean isHidden() {
        return !isVisible();
    }

    public boolean isHidden(HideCause cause) {
        return (mHideBits & cause.bit) != 0;
    }
    
    public boolean isVisible() {
        return mHideBits == 0;
    }
    
    
    /** @return always null (false): subclasses can override to persist the DEFAULT
     * hidden bit if they wish.
     */
    public Boolean getXMLhidden() {
        //return hidden ? Boolean.TRUE : null;
        return null;
    }
    public void setXMLhidden(Boolean b) {
        setVisible(!b.booleanValue());
    }

    
    public boolean isDrawn() {
        return isVisible() && !isFiltered();
    }
    
    public void setRollover(boolean tv)
    {
        if (this.rollover != tv) {
            this.rollover = tv;
        }
    }

    public void setZoomedFocus(boolean tv) {
        return;
        //throw new UnsupportedOperationException();
        /*
        if (this.isZoomedFocus != tv) {
            this.isZoomedFocus = tv;
        }
        if (getParent() != null) {
            getParent().setFocusComponent(tv ? this : null);
        }
        */
    }

    public boolean isZoomedFocus() {
        return isZoomedFocus;
    }
    
    public boolean isRollover() {
        return this.rollover;
    }

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
        return String.format(" %+.0f,%+.0f %.0fx%.0f", getX(), getY(), width, height);
    }

    protected void out(String s) {
        if (DEBUG.THREAD) {
            String thread = Thread.currentThread().toString().substring(6);
            System.err.format("%-32s%s %s\n", thread, this, s);
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
        // 2007-06-12 SMF -- do NOT turn this off yet -- let the LWMap
        // turn it off when EVERYONE is done.
        //mXMLRestoreUnderway = false;

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

    protected static final double OPAQUE = 1.0;


    /**
     * @param alpha -- an alpha value for the whole image
     * @param maxSize -- if non-null, the max width/height of the produced image (may be smaller)
     * @param zoom -- a zoom for the map size in producing the image (currently ignored if maxSize is provided)
     */
    protected BufferedImage getAsImage(double alpha, Dimension maxSize, double zoom) {
        return createImage(alpha, maxSize, (Color) null, zoom);
    }
    
    public BufferedImage getAsImage(double alpha, Dimension maxSize) {
        return getAsImage(alpha, maxSize, 1.0);
    }
    public BufferedImage getAsImage(double zoom) {
        return getAsImage(OPAQUE, null, zoom);
    }
    public BufferedImage getAsImage() {
        return getAsImage(OPAQUE, null, 1.0);
    }

    public BufferedImage createImage(double alpha, Dimension maxSize) {
        return createImage(alpha, maxSize, null, 1.0);
    }

    private static Rectangle2D.Float grow(Rectangle2D.Float r, int size) {
        r.x -= size;
        r.y -= size;
        r.width += size * 2;
        r.height += size * 2;
        return r;
    }

    /** @return the map bounds to use for rendering when generating an image of this LWComponent */
    protected Rectangle2D.Float getImageBounds() {
        final Rectangle2D.Float bounds = (Rectangle2D.Float) getPaintBounds().clone();

        int growth = 1; // just in case / rounding errors
        
        if (this instanceof LWMap)
            growth += 15;
        
        if (growth > 0)
            grow(bounds, growth);

        return bounds;
    }

    private static double computeZoomAndSize(Rectangle2D.Float bounds, Dimension maxSize, double zoomRequest, Size sizeResult)
    {
        double fitZoom = 1.0;
        
        if (maxSize != null) {
            if (bounds.width > maxSize.width || bounds.height > maxSize.height) {
                fitZoom = ZoomTool.computeZoomFit(maxSize, 0, bounds, null);
                sizeResult.width = (float) Math.ceil(bounds.width * fitZoom);
                sizeResult.height = (float) Math.ceil(bounds.height * fitZoom);
            }
        } else if (zoomRequest != 1.0) {
            sizeResult.width *= zoomRequest;
            sizeResult.height *= zoomRequest;
            fitZoom = zoomRequest;
        }

        return fitZoom;
    }
    

    
    
    /**
     * Create a new buffered image, of max dimension maxSize, and render the LWComponent
     * (and all it's children), to it using the given alpha.
     * @param alpha 0.0 (invisible) to 1.0 (no alpha)
     * @param maxSize max dimensions for image. May be null.  Image may be smaller than maxSize.
     * @param fillColor -- if non-null, will be rendered as background for image.  If alpha is
     * @param zoomRequest -- desired zoom; ignored if maxSize is non-null
     * also set, background fill will have transparency of alpha^3 to enhance contrast.
     */

    // Note: as of Mac OS X 10.4.10 (Intel), when a java drag source declares it can
    // generate an image (as we do when we Apple-drag something), if you drop it on the
    // desktop, it will create a special mac "picture clipping", which is some kind of
    // raw format, probabaly TIFF, tho you CANNOT open these in Preview.  Apparently
    // there's some kind of bug in the special .pictClipping, where sometimes when
    // opening it up it shows entirely as a blank space (I think if the image starts to
    // get "very large"), tho the data is actually there -- if you drag the picture
    // clipping into an Apple Mail message, it shows up again (and if you dragged from
    // VUE to Apple Mail in the first place, it also works fine).  Note that AFTER
    // dragging into Apple Mail, you can THEN double-click the attachment, and it will
    // open it up in Preview as a .tiff file (Apple Mail appears to be converting the
    // .pictClipping to tiff).  Note that uncompressed TIFF isn't exactly a friendly
    // mail attachment format as it's huge.  But once you open the image in Preview, you
    // have the option of saving it / exporting it as a jpeg, and you can even adjust
    // the quality to your liking.

    
    public BufferedImage createImage(double alpha, Dimension maxSize, Color fillColor, double zoomRequest)
    {
        final Rectangle2D.Float bounds = getImageBounds();

        if (DEBUG.IMAGE)  {
            System.out.println();
            out(TERM_CYAN +
                "createImage:"
                + "\n\t zoomRequst: " + zoomRequest
                + "\n\t    maxSize: " + maxSize
                + "\n\t  mapBounds: " + fmt(bounds)
                + "\n\t  fillColor: " + fillColor
                + "\n\t      alpha: " + alpha
                + TERM_CLEAR
                );
        }
        
        final Size imageSize = new Size(bounds);
        final double usedZoom = computeZoomAndSize(bounds, maxSize, zoomRequest, imageSize);

        // Image type ARGB is needed if at any point in the generated image,
        // there is a not 100% opaque pixel all the way through the background.
        // So TYPE_INT_RGB will handle transparency with a map fine --
        // but we need TYPE_INT_ARGB if, say, we're generating drag
        // image that we want to be a borderless node (fully transparent
        // image border), or if the whole drag image itself is semi-transparent.

        final int imageType;
        if (alpha == OPAQUE && fillColor != null && fillColor.getAlpha() == 255)
            imageType = BufferedImage.TYPE_INT_RGB;
        else
            imageType = BufferedImage.TYPE_INT_ARGB;

       final int width = imageSize.pixelWidth();
       final int height = imageSize.pixelHeight();
        
        if (DEBUG.IMAGE) out(TERM_CYAN
                             + "createImage:"
                             //+ "\n\tfinal size: " + width + "x" + height
                             + "\n\t neededSize: " + imageSize
                             + "\n\t   usedZoom: " + usedZoom
                             + "\n\t       type: " + (imageType == BufferedImage.TYPE_INT_RGB ? "OPAQUE" : "TRANSPARENT")
                             + TERM_CLEAR);

        if (mCachedImage != null &&
            mCachedImage.getWidth() == width &&
            mCachedImage.getHeight() == height &&
            mCachedImage.getType() == imageType)
        {
            // todo: could also re-use if cached image is > our needed size as long it's
            // an ARGB and we fill it with full alpha first, tho we really shouldn't
            // have each component caching it's own image: some kind of small
            // recently used image buffers cache would make more sense.
            if (DEBUG.DND || DEBUG.IMAGE) out(TERM_BLUE + "got cached image: " + mCachedImage + TERM_CLEAR);
        } else {
            mCachedImage = new BufferedImage(width, height, imageType);
            if (DEBUG.DND || DEBUG.IMAGE) out(TERM_RED + "created image: " + mCachedImage + TERM_CLEAR);
        }

        drawImage((Graphics2D) mCachedImage.getGraphics(),
                  alpha,
                  maxSize,
                  fillColor,
                  zoomRequest
                  );

        return mCachedImage;
    }

    /**
     * Useful for drawing drag images into an existing graphics buffer, or drawing exportable images.
     *
     * @param alpha 0.0 (invisible) to 1.0 (no alpha -- completely opaque)
     * @param maxSize max dimensions for image. May be null.  Image may be smaller than maxSize.
     * @param fillColor -- if non-null, will be rendered as background for image.  If alpha is
     * @param zoomRequest -- desired zoom; ignored if maxSize is non-null
     * also set, background fill will have transparency of alpha^3 to enhance contrast.
     */

    public void drawImage(Graphics2D g, double alpha, Dimension maxSize, Color fillColor, double zoomRequest)
    {
        //if (DEBUG.IMAGE) out("drawImage; size " + maxSize);

        final boolean drawBorder = this instanceof LWMap; // hack for dragged images of LWMaps

        final Rectangle2D.Float bounds = getImageBounds();
        final Rectangle clip = g.getClipBounds();
        final Size fillSize = new Size(bounds);
        final double zoom = computeZoomAndSize(bounds, maxSize, zoomRequest, fillSize);
        
        if (DEBUG.IMAGE) out(TERM_GREEN
                             + "drawImage:"
                             + "\n\t   mapBounds: " + fmt(bounds)
                             + "\n\t        fill: " + fillColor
                             + "\n\t     maxSize: " + maxSize
                             + "\n\t zoomRequest: " + zoomRequest
                             + "\n\t     fitZoom: " + zoom
                             + "\n\t    fillSize: " + fillSize
                             + "\n\t          gc: " + g
                             + "\n\t        clip: " + fmt(clip)
                             + "\n\t       alpha: " + alpha
                             + TERM_CLEAR
                             );


        final int width = fillSize.pixelWidth();
        final int height = fillSize.pixelHeight();
        
        final DrawContext dc = new DrawContext(g, this);
        dc.setClipOptimized(false); // always draw all children -- don't bother to check bounds
        if (DEBUG.IMAGE) out(TERM_GREEN + "drawImage: " + dc + TERM_CLEAR);

        if (fillColor != null) {
            if (false && alpha != OPAQUE) {
                Color c = fillColor;
                // if we have an alpha and a fill, amplify the alpha on the background fill
                // by changing the fill to one that has alpha*alpha, for a total of
                // alpha*alpha*alpha given our GC already has an alpha set.
                fillColor = new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (alpha*alpha*255+0.5));
            }
            if (alpha != OPAQUE) 
                dc.setAlpha(alpha, AlphaComposite.SRC); // erase any underlying in cache
            if (DEBUG.IMAGE) out("drawImage: fill=" + fillColor);
            g.setColor(fillColor);
            g.fillRect(0, 0, width, height);
        } else if (alpha != OPAQUE) {
            // we didn't have a fill, but we have an alpha: make sure any cached data is cleared
            dc.g.setComposite(AlphaComposite.Clear);
            g.fillRect(0, 0, width, height);
        }
        
        if (alpha != OPAQUE)
            dc.setAlpha(alpha, AlphaComposite.SRC);

        if (DEBUG.IMAGE && DEBUG.META) {
            // Fill the entire imageable area
            g.setColor(Color.green);
            g.fillRect(0,0, Short.MAX_VALUE, Short.MAX_VALUE);
        }

        dc.setAntiAlias(true);

        final AffineTransform rawTransform = g.getTransform();
            
        if (zoom != 1.0)
            dc.g.scale(zoom, zoom);
                
        // translate so that the upper left corner of the map region
        // we're drawing is at 0,0 on the underlying image

        g.translate(-bounds.getX(),
                    -bounds.getY());

        // GC *must* have a bounds set or we get NPE's in JComponent (textBox) rendering
        dc.setMasterClip(bounds);

        if (DEBUG.IMAGE && DEBUG.META) {
            // fill the clipped area so we can check our clip bounds
            dc.g.setColor(Color.red);
            dc.g.fillRect(-Short.MAX_VALUE/2,-Short.MAX_VALUE/2, // larger values than this can blow out internal GC code and we get nothing
                           Short.MAX_VALUE, Short.MAX_VALUE);
        }
        

        // render to the image through the DrawContext/GC pointing to it
        draw(dc);

        if (drawBorder) {
            g.setTransform(rawTransform);
            //g.setColor(Color.red);
            //g.fillRect(0,0, Short.MAX_VALUE, Short.MAX_VALUE);
            if (DEBUG.IMAGE) {
                g.setColor(Color.black);
                dc.setAntiAlias(false);
            } else
                g.setColor(Color.darkGray);
            g.drawRect(0, 0, width-1, height-1);
        }

        if (DEBUG.IMAGE) out(TERM_GREEN + "drawImage: completed\n" + TERM_CLEAR);
        
        
    }
    

    
    public String toString()
    {
        String cname = getClass().getName();
        String typeName = cname.substring(cname.lastIndexOf('.')+1);
        String label = "";
        String s;
        if (getLabel() != null) {
            if (true||isAutoSized())
                label = "\"" + getDisplayLabel() + "\" ";
            else
                label = "(" + getDisplayLabel() + ") ";
        }

        if (getID() == null) {
            s = String.format("%-17s[",
                              typeName + "." + Integer.toHexString(hashCode())
                              );
            //s += tufts.Util.pad(9, Integer.toHexString(hashCode()));
        } else {
            s = String.format("%-17s", typeName + "[" + getID());
            //s += tufts.Util.pad(4, getID());
        }
        s += label;
        //if (this.scale != 1f) s += "z" + this.scale + " ";
        if (getScale() != 1f) s += String.format("z%.2f ", getScale());
        s += paramString();
        if (mHideBits != 0)
            s += " " + getDescriptionOfSetBits();
        if (getResource() != null)
            s += " " + getResource();
        //s += " <" + getResource() + ">";
        s += "]";
        return s;
    }



    public static void main(String args[]) throws Exception
    {
        VUE.init(args);

        /*
        for (java.lang.reflect.Field f : LWComponent.class.getDeclaredFields()) {
            Class type = f.getType();
            if (type == Key.class)
                System.out.println("KEY: " + f);
            else
                System.out.println("Field: " + f + " (" + type + ")");
        }
        */

        // for debug: ensure basic LW types created first
        
        new LWNode();
        new LWLink();
        new LWImage();

        //NodeTool.getTool();

        VueToolbarController.getController(); // make sure the tools are initialized
        
        edu.tufts.vue.style.StyleReader.readStyles("compare.weight.css");

        java.util.Set<String> sortedKeys = new java.util.TreeSet<String>(edu.tufts.vue.style.StyleMap.keySet());

        for (String key : sortedKeys) {
            final Object style = edu.tufts.vue.style.StyleMap.getStyle(key);
            System.out.println("Found CSS style key; " + key + ": " + style);
            //System.out.println("Style key: " + se.getKey() + ": " + se.getValue());
        }

        new LWNode().applyCSS(edu.tufts.vue.style.StyleMap.getStyle("node.w1"));
        new LWLink().applyCSS(edu.tufts.vue.style.StyleMap.getStyle("link.w1"));
        
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
        



