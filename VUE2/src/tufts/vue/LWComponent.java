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
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

import tufts.vue.beans.UserMapType; // remove: old SB stuff we never used
import tufts.vue.filter.*;

/**
 * LWComponent.java
 * 
 * Light-weight component base class for creating components to be
 * rendered by the MapViewer class.
 *
 * @author Scott Fraize
 * @version 3/10/03
 */

public class LWComponent
    implements MapItem
               ,VueConstants
{
    public interface Listener extends java.util.EventListener {
        public void LWCChanged(LWCEvent e);
    }

    /*
     * Persistent information
     */

    private String ID = null;
    protected String label = null; // protected for debugging purposes
    private String notes = null;
    private Resource resource = null;
    private float x;
    private float y;
    private UserMapType mUserMapType = null; // I think this is totally vestigal -- remove
    //private java.util.Map mUserPropertyValues = new java.util.HashMap();
    private boolean mIsFiltered = false;
    private NodeFilter nodeFilter = null;
    
    protected float width = 9;
    protected float height = 9;

    protected Color fillColor = null;           //style
    protected Color textColor = COLOR_TEXT;     //style
    protected Color strokeColor = COLOR_STROKE; //style
    protected float strokeWidth = 0f;            //style
    protected Font font = FONT_DEFAULT;
    //protected Font font = null;                 //style -- if we leave null won't bother to persist this value
    

    /*
     * Runtime only information
     */
    protected transient TextBox labelBox = null;
    protected transient BasicStroke stroke = STROKE_ZERO;
    protected transient boolean hidden = false;
    protected transient boolean selected = false;
    protected transient boolean indicated = false;
    protected transient boolean rollover = false;
    protected transient boolean isZoomedFocus = false;

    protected transient LWContainer parent = null;

    // list of LWLinks that contain us as an endpoint
    private transient java.util.List links = new java.util.ArrayList();
    protected transient List pathwayRefs;

    // Scale currently exists ONLY to support the auto-managed child-node feature of nodes
    protected transient float scale = 1.0f;

    //protected transient java.util.List listeners;
    protected transient LWChangeSupport mChangeSupport = new LWChangeSupport(this);

    /** for save/restore only & internal use only */
    public LWComponent()
    {
        if (DEBUG.PARENTING)
            System.out.println("LWComponent construct of " + getClass().getName() + Integer.toHexString(hashCode()));
        // tdoo: shouldn't have to create a node filter for every one of these constructed...
        nodeFilter = new NodeFilter();
    }

    public static abstract class Key {
        public final String name;
        public Key(String name) {
            this.name = name;
        }
        public abstract void setValue(LWComponent c, Object v);
        public abstract Object getValue(LWComponent c);
        public String toString() { return name; } // must == name for now until tool panels handle new key objects
        //public String toString() { return ">" + name; }
    }
    public static final Key KEY_FillColor = new Key("fill.color") {
            public void setValue(LWComponent c, Object val) { c.setFillColor((Color)val); }
            public Object getValue(LWComponent c) { return c.getFillColor(); }
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
            return ((Key)key).getValue(this);
        }
        //if (key == LWKey.FillColor)     return getFillColor();
        if (key == LWKey.TextColor)     return getTextColor();
        if (key == LWKey.StrokeColor)   return getStrokeColor();
        if (key == LWKey.StrokeWidth)   return new Float(getStrokeWidth());
        if (key == LWKey.Font)          return getFont();
        if (key == LWKey.Label)         return getLabel();
        if (key == LWKey.Notes)         return getNotes();
        if (key == LWKey.Resource)      return getResource();
        if (key == LWKey.Location)      return getLocation();
        if (key == LWKey.Size)          return new Size(this.width, this.height);
        if (key == LWKey.Hidden)        return new Boolean(isHidden());
             
        if (DEBUG.Enabled) out("note: getPropertyValue; unsupported property [" + key + "] (returning null)");
        //return UnsupportedPropertyValue;
        return null;
        //throw new RuntimeException("Unknown property key[" + key + "]");
    }

    public void setProperty(final Object key, Object val)
    {
        if (DEBUG.UNDO&&DEBUG.META) out("setProperty [" + key + "] to " + val);

        if (key instanceof Key) {
            ((Key)key).setValue(this, val);
        }
        // still need this hack for fill color name till toolbars handle the new key objects
        else if (key == LWKey.FillColor.name)   setFillColor( (Color) val);
        //if (val == UnsupportedPropertyValue) return;
                                           
        else if (key == LWKey.TextColor)        setTextColor( (Color) val);
        else if (key == LWKey.StrokeColor)      setStrokeColor( (Color) val);
        else if (key == LWKey.StrokeWidth)      setStrokeWidth( ((Float) val).floatValue());
        else if (key == LWKey.Font)             setFont( (Font) val);
        else if (key == LWKey.Label)            setLabel( (String) val);
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
            out("setProperty: unknown key [" + key + "] with value [" + val + "]");
            new Throwable("FYI: Unhandled Property").printStackTrace();
        }
    }
    
    

    /** Create a component with duplicate content & style.
     * Does not duplicate any links to this component,
     * and leaves it an unparented orphan.
     */
    public LWComponent duplicate()
    {
        LWComponent c = null;

        try {
            c = (LWComponent) getClass().newInstance();
        } catch (Exception e) {
            System.err.println(e);
            return null;
        }
        c.x = this.x;
        c.y = this.y;
        c.width = this.width;
        c.height = this.height;
        c.font = this.font;
        c.scale = this.scale;
        c.strokeWidth = this.strokeWidth;
        c.stroke = this.stroke; // cached info only

        c.setAutoSized(isAutoSized());
        c.setFillColor(getFillColor());
        c.setTextColor(getTextColor());
        c.setStrokeColor(getStrokeColor());
        c.setLabel(this.label); // use setLabel so new TextBox will be created
        
        if (hasResource())
            c.setResource(getResource());
        if (hasNotes())
            c.setNotes(getNotes());
        
        return c;
    }

    protected String getNextUniqueID()
    {
        if (getParent() == null)
            throw new IllegalStateException("LWComponent needs a parent subclass of LWContainer that implements getNextUniqueID: " + this);
        else
            return getParent().getNextUniqueID();
    }
    

    // Below is last minute hacked in code by Scott Bresnahan
    // that needs to be re-implemented if we're going to use it.
    /*
     * getUserMapType
     * @return UserMapType the user map type id
    public UserMapType getUserMapType() {
    	return mUserMapType;
    }
    
    /**
     * setUserMapType
     * @param UserMapTYpe the user map type id for this LWC
    public void setUserMapType( UserMapType pMapType) {
    	mUserMapType = pMapType;
    }
    
    public void setUSerPropertyValue( String pKey, Object pValue ) {
    	mUserPropertyValues.put( pKey, pValue);
    }
    
    public Object getUserProeprtyValue( String pKey) {
    	Object retValue = mUserPropertyValues.get( pKey);
    	return retValue;
    }
    
    /**
     * hasMeteData
     * This returns true if there is user metadata for this component
     * @return true if meta data values exist; false if not
   public boolean hasMetaData() {
   	boolean hasData = false;
   	if( mUserPropertyValues != null) {
   		hasData = (getUserMapType() != null) && ( !mUserPropertyValues.isEmpty() ); 
   		}
   	return hasData;
   }
    */
   /*
    * getMetaDataAsHTML()
   public String getMetaDataAsHTML() {
   	String str = "";
   	if( hasMetaData() ) {
   		str = getUserMapType().getAsHTML( mUserPropertyValues);
   		}
   	return str;
   }
   */

    public UserMapType getUserMapType() { throw new UnsupportedOperationException("deprecated"); }
    public boolean hasMetaData() { return false; }
    public String getMetaDataAsHTML() { return null; }
   
    
    /**
     * This sets the flag for the component so that it is either
     * hidden or visible based on a match to the active LWCFilter
     **/
    public void setIsFiltered(boolean pState) {
    	mIsFiltered = pState;
    }
    
    /**
     * @return true if should be hidden due to a currently applied filter, false if not
     **/
    public boolean isFiltered() {
    	return mIsFiltered;
    }

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
    /** called directly by TextBox after document edit with setDocument=false */
    void setLabel0(String label, boolean setDocument)
    {
        Object old = this.label;
        if (this.label == label)
            return;
        if (this.label != null && this.label.equals(label))
            return;
        if (label == null || label.length() == 0) {
            this.label = null;
            if (labelBox != null)
                labelBox.setText("");
        } else {
            this.label = label;
            // todo opt: only do this if node or link
            if (labelBox == null)
                getLabelBox();
            else if (setDocument)
                getLabelBox().setText(label);
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
        Object old = this.resource;
        this.resource = resource;
        layout();
        notify(LWKey.Resource, old);
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
    public String getLabel() {
        return this.label;
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
    
    public void setNodeFilter(NodeFilter nodeFilter) {
        this.nodeFilter = nodeFilter;
    }
    
    public NodeFilter getNodeFilter() {
        //out(this + " getNodeFilter " + nodeFilter);
        return nodeFilter;
    }

    /** does this support a user editable label? */
    public boolean supportsUserLabel() {
        return false;
    }
    /** does this support user resizing? */
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
        Iterator i = pathwayRefs.iterator();
        while (i.hasNext()) {
            LWPathway p = (LWPathway) i.next();
            if (p.isVisible() && !p.isRevealer())
                return true;
        }
        return false;
    }
    
    void addPathwayRef(LWPathway p)
    {
        if (pathwayRefs == null)
            pathwayRefs = new ArrayList();
        pathwayRefs.add(p);
        layout();
        //notify("pathway.add");
    }
    void removePathwayRef(LWPathway p)
    {
        if (pathwayRefs == null) {
            new Throwable("attempt to remove non-existent pathwayRef to " + p + " in " + this).printStackTrace();
            return;
        }
        pathwayRefs.remove(p);
        if (p.isRevealer()) // if was a revealer, make sure we're not left invisible if it had us hidden
            setVisible(true);
        layout();
        //notify("pathway.remove");
    }
    

    /** for persistance */
    // todo: move all this XML handling stuff to a special castor property mapper,
    // presumably in conjunction with re-architecting the whole mapping style &
    // save mechanism.
    public String getXMLlabel()
    {
        return escapeNewlines(this.label);
    }

    /** for persistance */
    public void setXMLlabel(String text)
    {
        setLabel(unEscapeNewlines(text));
    }

    /** for persistance */
    public String getXMLnotes()
    {
        return escapeWhitespace(this.notes);
    }

    /** for persistance -- gets called by castor after it reads in XML */
    public void setXMLnotes(String text)
    {

        // If castor xml indent was on when save was done
        // (org.exolab.castor.indent=true in castor.properties
        // somewhere in the classpath, to make the XML more human
        // readable) it will break up elements like: <note>many chars
        // of text...</note> with newlines and whitespaces to indent
        // the new lines in the XML -- however, on reading them back
        // in, it puts this white space into the string you saved! So
        // we patch up note strings here in case of that.  (btw, this
        // isn't a problem for labels because they're XML attributes,
        // not elements, which are quoted).
        
        text = text.replaceAll("\n[ \t]*%nl;", "%nl;");
        text = text.replaceAll("\n[ \t]*", " ");
        String notes = unEscapeWhitespace(text);
        setNotes(notes);
    }

    private String escapeNewlines(String text)
    {
        if (text == null)
            return null;
        else {
            return text.replaceAll("[\n\r]", "%nl;");
        }
    }
    private String unEscapeNewlines(String text)
    {
        if (text == null)
            return null;
        else { 
            return text.replaceAll("%nl;", "\n");
        }

    }
    private String escapeWhitespace(String text)
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
    private String unEscapeWhitespace(String text)
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
    protected void layout() {}
    
    public String OLD_toString()
    {
        String s = getClass().getName() + "[id=" + getID();
        if (getLabel() != null)
            s += " \"" + getLabel() + "\"";
        s += "]";
        return s;
    }

    
    /** @return true: default is always autoSized */
    public boolean isAutoSized() { return true; }
    /** do nothing: default is always autoSized */
    public void setAutoSized(boolean t) {}
    
    private boolean eq(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }
    
    public Color getFillColor()
    {
        return this.fillColor;
    }
    /** Color to use at draw time.
        LWNode overrides to provide darkening of children. */
    public Color getRenderFillColor()
    {
        return getFillColor();
    }
    public void setFillColor(Color color)
    {
        if (eq(color, fillColor))
            return;
        Object old = this.fillColor;
        this.fillColor = color;
        notify(KEY_FillColor, old);
        //notify(LWKey.FillColor, old);
    }

    /** for persistance */
    public String getXMLfillColor()
    {
        return ColorToString(getFillColor());
    }
    /** for persistance */
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
    /** for persistance */
    public String getXMLtextColor()
    {
        return ColorToString(getTextColor());
    }
    /** for persistance */
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
    /** for persistance */
    public String getXMLstrokeColor()
    {
        return ColorToString(getStrokeColor());
    }
    /** for persistance */
    public void setXMLstrokeColor(String xml)
    {
        setStrokeColor(StringToColor(xml));
    }
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
            Integer intval = Integer.decode(xml);
            //Long longval = Long.decode(xml); // transparency test -- works,just need gui
            //c = new Color(longval.intValue(), true);
            c = new Color(intval.intValue());
        } catch (NumberFormatException e) {
            System.err.println("LWComponent.StringToColor[" + xml + "] " + e);
        }
        return c;
    }
    
    public float getStrokeWidth()
    {
        return this.strokeWidth;
    }
    public void setStrokeWidth(float w)
    {
        if (this.strokeWidth != w) {
            float oldStrokeWidth = this.strokeWidth;
            this.strokeWidth = w;
            if (w > 0)
                this.stroke = new BasicStroke(w, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
            else
                this.stroke = STROKE_ZERO;
            if (getParent() != null) {
                // because stroke affects bounds-width, may need to re-layout parent
                getParent().layout();
            }
            layout();
            notify(LWKey.StrokeWidth, new Float(oldStrokeWidth));
        }
    }
    public Font getFont()
    {
        return this.font;
    }
    public void setFont(Font font)
    {
        if (eq(font, this.font))
            return;
        Object old = this.font;
        this.font = font;
        if (labelBox != null)
            labelBox.copyStyle(this);
        layout();
        notify(LWKey.Font, old);
    }
    
    /** to support XML persistance */
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
    /** to support XML persistance */
    public void setXMLfont(String xml)
    {
        setFont(Font.decode(xml));
    }
    
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
    
    public LWContainer getParent() {
        return this.parent;
    }

    public boolean isOrphan() {
        return this.parent == null;
    }

    public boolean hasChildren() {
        return false;
    }

    /* for tracking who's linked to us */
    void addLinkRef(LWLink link)
    {
        if (DEBUG.EVENTS||DEBUG.UNDO) out(this + " adding link ref to " + link);
        if (this.links.contains(link))
            throw new IllegalStateException("addLinkRef: " + this + " already contains " + link);
        this.links.add(link);
        notify(LWKey.LinkAdded, link); // informational only event
    }
    /* for tracking who's linked to us */
    void removeLinkRef(LWLink link)
    {
        if (DEBUG.EVENTS||DEBUG.UNDO) out(this + " removing link ref to " + link);
        if (!this.links.remove(link))
            throw new IllegalStateException("removeLinkRef: " + this + " didn't contain " + link);
        notify(LWKey.LinkRemoved, link); // informational only event
    }
    
    /* tell us all the links who have us as one of their endpoints */
    java.util.List getLinkRefs()
    {
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
                    // Every link, as it's connected to us, should
                    // have us as one of it's endpoints -- so return
                    // the opposite endpoint.
                    // todo: now that links can have null endpoints,
                    // this iterator can return null -- hasNext
                    // will have to get awfully fancy to handle this.
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
        java.util.List list = new java.util.ArrayList(this.links.size());
        java.util.Iterator i = this.links.iterator();
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
    
    //needed for pathways to access a node's links - jay briedis
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

    void setScale(float scale)
    {
        if (this.scale == scale)
            return;
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

    /**
     * Tell all links that have us as an endpoint that we've
     * moved or resized so the link knows to recompute it's
     * connection points.
     */
    protected void updateConnectedLinks()
    {
        java.util.Iterator i = getLinkRefs().iterator();
        while (i.hasNext()) {
            LWLink l = (LWLink) i.next();
            l.setEndpointMoved(true);
        }
    }
    
    public void translate(float dx, float dy)
    {
        setLocation(this.x + dx,
                    this.y + dy);
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
        if (DEBUG.LAYOUT) System.out.println("*** setFrame " + x+","+y + " " + w+"x"+h + " " + this);

        setSize(w, h);
        setLocation(x, y);

        /*
        Object old = new Rectangle2D.Float(this.x, this.y, getWidth(), getHeight());
        setLocation0(x, y);
        setSize0(w, h);
        updateConnectedLinks();
        notify(LWKey.Frame, old);
        */
    }

    /** default calls setFrame -- override to provide constraints */
    public void userSetFrame(float x, float y, float w, float h) {
        setFrame(x, y, w, h);
    }
        

    private boolean linkNotificationDisabled = false;
    protected void setLocation0(float x, float y) {
        this.x = x;
        this.y = y;
    }
    
    public void setLocation(float x, float y)
    {
        if (this.x == x && this.y == y)
            return;
        Object old = new Point2D.Float(this.x, this.y);
        setLocation0(x, y);
        if (!linkNotificationDisabled)
            updateConnectedLinks();
        notify(LWKey.Location, old); // todo perf: does anyone need this except for undo?  lots of these during drags...
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
    
    /** set component to this many pixels in size */
    protected void setSize0(float w, float h)
    {
        if (this.width == w && this.height == h)
            return;
        if (DEBUG.LAYOUT) out("*** setSize0 (LWC)  " + w + "x" + h);
        this.width = w;
        this.height = h;
    }
    
    /** set component to this many pixels in size */
    public void setSize(float w, float h)
    {
        if (width == w && height == h)
            return;
        if (DEBUG.LAYOUT) out("*** setSize  (LWC)  " + w + "x" + h);
        Size old = new Size(width, height);
        setSize0(w, h);
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
        if (DEBUG.LAYOUT) out("*** " + this + " setAbsoluteSize " + w + "x" + h);
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
    public float getBoundsWidth() { return (this.width + this.strokeWidth) * getScale(); }
    public float getBoundsHeight() { return (this.height + this.strokeWidth) * getScale(); }
    public float getCenterX() { return this.x + getWidth() / 2; }
    public float getCenterY() { return this.y + getHeight() / 2; }

    // these 4 for persistance ONLY -- they don't deliver detectable events!
    /** for persistance ONLY */
    public float getAbsoluteWidth() { return this.width; }
    /** for persistance ONLY */
    public float getAbsoluteHeight() { return this.height; }
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
        
        Iterator i = pathwayRefs.iterator();
        while (i.hasNext()) {
            LWPathway path = (LWPathway) i.next();
            if (path.isDrawn())
                path.drawComponentDecorations(dc, this);
        }
        
    }

    /** if this component is selected and we're not printing, draw a selection indicator */
    // todo: drawing of selection should be handled by the MapViewer and/or the currently
    // active tool -- not in the component code
    protected void drawSelectionDecorations(DrawContext dc) {
        if (isSelected() && !dc.isPrinting()) {
            LWPathway p = VUE.getActivePathway();
            if (p != null && p.isVisible() && p.getCurrent() == this) {
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
        drawPathwayDecorations(dc);
    }

    protected LWChangeSupport getChangeSupport() {
        return mChangeSupport;
    }
    public synchronized void addLWCListener(Listener listener) {
        addLWCListener(listener, null);
    }
    /** @param eventMask is a string constant (from LWKey) or an array of such. If one
     of these non-null values, only events matching those keys will be delievered */
    public synchronized void addLWCListener(Listener listener, Object eventMask) {
        mChangeSupport.addListener(listener, eventMask);
    }
    public synchronized void removeLWCListener(Listener listener) {
        mChangeSupport.removeListener(listener);
    }
    public synchronized void removeAllLWCListeners() {
        mChangeSupport.removeAllListeners();
    }

    protected synchronized void notifyLWCListeners(LWCEvent e)
    {
        mChangeSupport.notifyListeners(this, e);
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
    public boolean isHidden()
    {
        return this.hidden;
    }
    public void setVisible(boolean visible)
    {
        setHidden(!visible);
    }
    public boolean isVisible()
    {
        return !isHidden();
    }
    public boolean isDrawn() {
        return !hidden && !mIsFiltered;
    }
    public void setIndicated(boolean indicated)
    {
        if (this.indicated != indicated) {
            this.indicated = indicated;
        }
    }
    public void setRollover(boolean tv)
    {
        if (this.rollover != tv) {
            this.rollover = tv;
        }
    }
    public void setZoomedFocus(boolean tv)
    {
        if (this.isZoomedFocus != tv) {
            this.isZoomedFocus = tv;
        }
        if (getParent() != null) {
            getParent().setFocusComponent(tv ? this : null);
        }
    }

    public boolean isZoomedFocus()
    {
        return isZoomedFocus;
    }
    
    public boolean isIndicated()
    {
        return this.indicated;
    }
    public boolean isRollover()
    {
        return this.rollover;
    }

    public LWComponent findDeepestChildAt(float mapX, float mapY, LWComponent excluded)
    {
        return excluded == this ? null : this;
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
        return " " + x+","+y
            +  " " + width + "x" + height;
    }

    protected void out(String s) {
        System.out.println(this + ": " + s);
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
    public String toString()
    {
        String cname = getClass().getName();
        String s = cname.substring(cname.lastIndexOf('.')+1);
        s += "[" + getID();
        if (getID() != null && getID().length() < 2)
            s += " ";
        if (getLabel() != null) {
            if (isAutoSized())
                s += " \"" + getDisplayLabel() + "\"";
            else
                s += " (" + getDisplayLabel() + ")";
        }
        if (getScale() != 1f)
            s += " z" + getScale();
        s += paramString();
        if (getResource() != null)
            s += " <" + getResource() + ">";
        s += "]";
        return s;
    }
}
