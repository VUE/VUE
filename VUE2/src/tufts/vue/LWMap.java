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

import java.util.Vector;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.Printable;
import java.awt.print.PageFormat;
import java.io.File;
import tufts.vue.beans.*;
import tufts.vue.filter.*;

/**
 * LWMap
 *
 * This is the top-level VUE model class.
 *
 * LWMap is a specialized LWContainer that acts as the top-level node
 * for a VUE application map.  It has special handling code for XML
 * saves/restores, keeping a reference to an UndoManager & tracking if
 * map has been user modified, maintaing user values for global zoom &
 * pan offset, keeping a list of LWPathways and telling them to draw
 * when needed, generating uniqe ID's for LWComponents, maintaining map-wide
 * meta-data definitions, etc.
 *
 * As LWCEvent's issued by changes to LWComponents are bubbled up
 * through their parents, listenting to the map for LWCEvents will
 * tell you about every change that happens anywhere in the map.
 * For instance: the UndoManager is just another listener on the map.
 * (Note however, that the application depends on LWKey.UserActionCompleted
 * events delivered from the UndoManager to indicate the right time
 * to redisplay; e.g., in the panner tool and in other map viewers
 * of the same map).
 *
 * As it extends LWComponent/LWContainer, in the future it is
 * ready to be embedded / rendered as a child of another LWContainer/LWMap.
 *
 * @author Scott Fraize
 * @author Anoop Kumar (meta-data)
 * @version March 2004
 */

public class LWMap extends LWContainer
implements ConceptMap//, Printable
{
    /** file we were opened from of saved to, if any */
    private File file;
    
    /** the list of LWPathways, if any */
    private LWPathwayList mPathways;
    
    /** the author of the map **/
    private String mAuthor;
    
    /** the date created (modified?) **/
    private String mDate;
    
    /** user description **/
    private String mDescription;
    
    /** the current Map Filter **/
    LWCFilter mLWCFilter;
    
    /** Metadata for Publishing **/
    Properties metadata = new Properties();
    
    /* Map Metadata-  this is for adding specific metadata and filtering **/
    MapFilterModel  mapFilterModel = new MapFilterModel();
    
    /* user map types -- is this still used? **/
    //private UserMapType[] mUserTypes;
    
    private long mChanges = 0;    // guaranteed >= actual change count
    private Rectangle2D.Float mCachedBounds = null;
    
    
    // these for persistance use only
    private float userOriginX;
    private float userOriginY;
    private double userZoom = 1;
    
    
    // only to be used during a restore from persisted
    public LWMap() {
        setLabel("<map-during-XML-restoration>");
        mLWCFilter = new LWCFilter(this);
    }
    
    public LWMap(String label) {
        setID("0");
        setFillColor(java.awt.Color.white);
        setTextColor(COLOR_TEXT);
        setStrokeColor(COLOR_STROKE);
        setFont(FONT_DEFAULT);
        setLabel(label);
        mPathways = new LWPathwayList(this);
        mLWCFilter = new LWCFilter(this);
        // Always do markDate, then markAsSaved as the last items in the constructor:
        // (otherwise this map will look like it's user-modified when it first displays)
        markDate();
        markAsSaved();
    }

    /** create a temporary, uneditable map that contains just the given component */
    LWMap(LWComponent c) {

        this(c.getDisplayLabel());
        if (c instanceof LWGroup && c.getFillColor() != null)
            setFillColor(c.getFillColor());
        children.add(c);
        // todo: listen to child for events & pass up
    }
    
    private void markDate() {
        long time = System.currentTimeMillis();
        java.util.Date date = new java.util.Date( time);
        java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd");
        String dateStr = df.format( date);
        setDate(dateStr);
    }
    
    private UndoManager mUndoManager;
    public UndoManager getUndoManager() {
        return mUndoManager;
    }
    public void setUndoManager(UndoManager um) {
        if (mUndoManager != null)
            throw new IllegalStateException(this + " already has undo manager " + mUndoManager);
        mUndoManager = um;
    }
    
    public void setFile(File file) {
        this.file = file;
        if (file != null)
            setLabel(file.getName()); // todo: don't let this be undoable!
    }
    
    public File getFile() {
        return this.file;
    }
    
    public void markAsModified() {
        System.out.println(this + " explicitly marking as modified");
        if (mChanges == 0)
            mChanges = 1;
        // notify with an event mark as not for repaint (and set same bit on "repaint" event)
    }
    public void markAsSaved() {
        System.out.println(this + " marking " + mChanges + " modifications as current");
        mChanges = 0;
        // todo: notify with an event mark as not for repaint (and set same bit on "repaint" event)
    }
    public boolean isModified() {
        return mChanges > 0;
    }
    long getModCount() { return mChanges; }

    /** set the fill color w/out sending any events -- for temporary use */
    void takeFillColor(java.awt.Color c) {
        super.fillColor = c;
    }
    
    /**
     * getLWCFilter()
     * This gets the current LWC filter
     **/
    public LWCFilter getLWCFilter() {
        return mLWCFilter;
    }
    
    /** @return true if this map currently conditionally displaying
     * it's components based on a filter */
    public boolean isCurrentlyFiltered() {
        return mLWCFilter != null && mLWCFilter.isFilterOn() && mLWCFilter.hasPersistentAction();
    }
    
    /**
     * This tells us there's a new LWCFilter or filter state in effect
     * for the filtering of node's & links.
     * This should be called anytime the filtering is to change, even if we
     * already have our filter set to the given filter.  We will
     * apply / clear as appropriate to the state of the filter.
     * @param LWCFilter the filter to install and/or update against
     **/
    private boolean filterWasOn = false; // workaround for filter bug
    public void setLWCFilter(LWCFilter filter) {
        out("setLWCFilter: " + filter);
        mLWCFilter = filter;
        applyFilter();
    }
    
    public  void clearFilter() {
        out("clearFilter: cur=" + mLWCFilter);
         if (mLWCFilter.getFilterAction() == LWCFilter.ACTION_SELECT)
                VUE.getSelection().clear();
        Iterator i = getAllDescendentsIterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            c.setIsFiltered(false);
        }
        mLWCFilter.setFilterOn(false);       
        notify(LWKey.MapFilter);
    }
    
    public  void applyFilter() {
      
            if (mLWCFilter.getFilterAction() == LWCFilter.ACTION_SELECT)
                VUE.getSelection().clear();
            
            Iterator it = getAllDescendentsIterator();
            while (it.hasNext()) {
                LWComponent c = (LWComponent) it.next();
                if( (c instanceof LWNode) || (c instanceof LWLink) ) {
                    boolean state = mLWCFilter.isMatch( c);
                    if( mLWCFilter.isLogicalNot() ) {
                        state = !state;
                    }
                    if (mLWCFilter.getFilterAction() == LWCFilter.ACTION_HIDE)
                        c.setIsFiltered(state);
                    else if (mLWCFilter.getFilterAction() == LWCFilter.ACTION_SHOW)
                        c.setIsFiltered(!state);
                    else if (mLWCFilter.getFilterAction() == LWCFilter.ACTION_SELECT) {
                        if (state)
                            VUE.getSelection().add(c);
                    }
                }
            }
            filterWasOn = true;
            mLWCFilter.setFilterOn(true);
     
        notify(LWKey.MapFilter);
    }
    
    /**
     * getUserMapTypes
     * This returns an array of available map types for this
     * map.
     * @return UserMapType [] the array of map types
     **/
    public UserMapType [] getUserMapTypes() {
        throw new UnsupportedOperationException("de-implemented");
        //return mUserTypes;
    }
    
    /**
     * \Types if(filterTable.isEditing()) {
     * filterTable.getCellEditor(filterTable.getEditingRow(),filterTable.getEditingColumn()).stopCellEditing();
     * System.out.println("Focus Lost: Row="+filterTable.getEditingRow()+ "col ="+ filterTable.getEditingColumn());
     * }
     * filterTable.removeEditor();
     * This sets the array of UserMapTypes for teh map
     *  @param pTypes - uthe array of UserMapTypes
     **/
    public void setUserMapTypes( UserMapType [] pTypes) {
        throw new UnsupportedOperationException("de-implemented");
        //mUserTypes = pTypes;
        //validateUserMapTypes();
    }
    
    /*
     * validateUserMapTypes
     * Searches the list of LW Compone
    private void validateUserMapTypes() {
     
        java.util.List list = getAllDescendents();
     
        Iterator it = list.iterator();
        while (it.hasNext()) {
            LWComponent c = (LWComponent) it.next();
            if ( c.getUserMapType() != null)  {
                // Check that type still exists...
                UserMapType type = c.getUserMapType();
                if( !hasUserMapType( type) ) {
                    c.setUserMapType( null);
                }
            }
        }
    }
     **/
    
    /*
     * hasUserMapType
     * This method verifies that the UserMapType exists for this Map.
     * @return boolean true if exists; false if not
     
    private boolean hasUserMapType( UserMapType pType) {
        boolean found = false;
        if( mUserTypes != null) {
            for( int i=0; i< mUserTypes.length; i++) {
                if( pType.getID().equals( mUserTypes[i].getID() ) ) {
                    return true;
                }
            }
        }
        return found;
    }
     **/
    
    /**
     * getAuthor
     *
     **/
    public String getAuthor() {
        return mAuthor;
    }
    
    public void setAuthor( String pName) {
        mAuthor = pName;
    }
    
    
    public String getDescription() {
        return mDescription;
    }
    
    public void setDescription(String pDescription) {
        mDescription = pDescription;
    }
    
    public String getDate() {
        return mDate;
    }
    public void setDate( String pDate) {
        mDate = pDate;
    }
    
    public Properties getMetadata(){
        return metadata;
    }
    
    public void setMetadata(Properties metadata) {
        this.metadata = metadata;
    }
    
    public MapFilterModel getMapFilterModel() {
        return mapFilterModel;
    }
    
    public void setMapFilterModel(MapFilterModel mapFilterModel) {
        out("setMapFilterModel " + mapFilterModel);
        this.mapFilterModel = mapFilterModel;
    }
    
    public LWPathwayList getPathwayList() {
        return mPathways;
    }
    
    /** for persistance restore only */
    public void setPathwayList(LWPathwayList l){
        System.out.println(this + " pathways set to " + l);
        mPathways = l;
        mPathways.setMap(this);
    }
    
    private int nextID = 1;
    protected String getNextUniqueID() {
        return Integer.toString(nextID++, 10);
    }
    
    public void completeXMLRestore() {
        System.out.println(getLabel() + ": completing restore...");
        resolvePersistedLinks(this);
        setChildScaleValues();
        //setScale(getScale());
        setChildParentReferences();
        if (mPathways == null)
            mPathways = new LWPathwayList(this);
        mPathways.completeXMLRestore(this);
        this.nextID = findGreatestChildID() + 1;
        System.out.println(getLabel() + ": nextID=" + nextID);
        System.out.println(getLabel() + ": restore completed.");
        
        Iterator i = getAllDescendentsIterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            c.layout();
        }
        //setEventsResumed();
        markAsSaved();
    }
    
    // do nothing
    //void setScale(float scale) { }
    
    public void draw(DrawContext dc){
        if (DEBUG.SCROLL || DEBUG.CONTAINMENT) {
            dc.g.setColor(java.awt.Color.green);
            dc.setAbsoluteStroke(1);
            dc.g.draw(getBounds());
        }
        super.draw(dc);
        Iterator i = getPathwayList().iterator();
        int pathIndex = 0;
        while (i.hasNext()) {
            LWPathway path = (LWPathway) i.next();
            if (path.isDrawn() && path.hasChildren()) {
                dc.setIndex(pathIndex++);
                path.drawPathway(dc.create());
            }
        }
    }
    
    protected LWComponent findChildByID(String ID) {
        LWComponent c = super.findChildByID(ID);
        if (c == null) {
            System.err.println(this + " failed to locate a LWComponent with id [" + ID + "]");
            return null;
        } else
            return c;
    }
    
    /** for viewer to report user origin sets via pan drags */
    void setUserOrigin(float x, float y) {
        if (userOriginX != x || userOriginY != y){
            this.userOriginX = x;
            this.userOriginY = y;
            //markChange("userOrigin");
        }
    }
    /** for persistance */
    public Point2D.Float getUserOrigin() {
        return new Point2D.Float(this.userOriginX, this.userOriginY);
    }
    /** for persistance */
    public void setUserOrigin(Point2D.Float p) {
        setUserOrigin((float) p.getX(), (float) p.getY());
    }
    
    /** for persi if(filterTable.isEditing()) {
     * filterTable.getCellEditor(filterTable.getEditingRow(),filterTable.getEditingColumn()).stopCellEditing();
     * System.out.println("Focus Lost: Row="+filterTable.getEditingRow()+ "col ="+ filterTable.getEditingColumn());
     * }
     * filterTable.removeEditor();stance.  Note that as maps can be in more than
     * one viewer, each with it's own zoom, we take on only
     * the zoom value set in the more recent viewer to change
     * it's zoom.
     */
    public void setUserZoom(double zoom) {
        this.userZoom = zoom;
    }
    /** for persistance */
    public double getUserZoom() {
        return this.userZoom;
    }
    
    /*
    public LWComponent findLWComponentAt(float mapX, float mapY)
    {
        LWComponent c = super.findLWComponentAt(mapX, mapY);
        return c == this ? null : c;
    }
     
    public LWComponent findDeepestComponentAt(float mapX, float mapY, LWComponent excluded)
    {
        LWComponent c = super.findDeepestComponentAt(mapX, mapY, excluded);
        return c == this ? null : c;
    }
     */
    
    /** override of LWContainer: default hit component on the map
     * is nothing -- we just @return null.
     */
    protected LWComponent defaultHitComponent() {
        return null;
    }
    
    
    /* override of LWComponent: parent == null indicates deleted,
     * but map parent is always null.  For now always returns
     * false.  If need to support tracking deleted map, create
     * a different internal indicator for LWMap's [OLD]
    public boolean isDeleted() {
        return false;
    }
     */
    
    /** override of LWComponent: normally, parent == null indicates orphan,
     * which is considered a problem condition if attempting to deliver
     * events, but this is normal for the LWMap which as no parent,
     * so this always returns false.
     */
    public boolean isOrphan() {
        return false;
    }
    
    public LWNode addNode(LWNode c) {
        addChild(c);
        return c;
    }
    public LWLink addLink(LWLink c) {
        addChild(c);
        return c;
    }
    
    public LWPathway addPathway(LWPathway p) {
        getPathwayList().add(p);
        return p;
    }
    
    protected void addChildInternal(LWComponent c) {
        if (c instanceof LWPathway)
            throw new IllegalArgumentException("LWPathways not added as direct children of map: use addPathway " + c);
        super.addChildInternal(c);
    }
    
    LWComponent addLWC(LWComponent c) {
        addChild(c);
        return c;
    }
    /*
    private void removeLWC(LWComponent c)
    {
        removeChild(c);
    }
     */
    
    /**
     * Every single event anywhere in the map will ultimately end up
     * calling this notifyLWCListners.
     */
    protected void notifyLWCListeners(LWCEvent e) {
        if (mChangeSupport.eventsDisabled()) {
            if (DEBUG.EVENTS) System.out.println(e + " SKIPPING (events disabled)");
            return;
        }
        // consider flushing bounds if layout() called also (any child layout bubbles up to us)
        // todo pref: should be safe to only do this if a size, location or scale, hide or filter event.
        String what = e.getWhat();
        if (what == LWKey.Repaint || what == LWKey.Scale) {
            // nop
            ;
            // repaint is for non-permanent changes.
            // scale sets not considered modifications as they can
            // happen do to rollover -- any time a scale happens
            // otherwise will be in conjunction with a reparenting
            // event, and so we'll detect the change that way.
        } else {
            markChange(e);
        }
        super.notifyLWCListeners(e);
        flushBounds();
    }
    
    private void flushBounds() {
        mCachedBounds = null;
        if (DEBUG.EVENTS&&DEBUG.META) out(this + " flushed cached bounds");
    }
    
    private void markChange(Object e) {
        if (mChanges == 0) {
            if (DEBUG.EVENTS)
                out(this + " First Modification Happening on " + e);
            if (DEBUG.INIT||(DEBUG.EVENTS&&DEBUG.META))
                new Throwable("FYI: FIRST MODIFICATION").printStackTrace();
        }
        mChanges++;
    }
    
    public java.awt.geom.Rectangle2D getBounds() {
        if (true||mCachedBounds == null) {
            mCachedBounds = getBounds(getChildIterator());
            takeSize(mCachedBounds.width, mCachedBounds.height);
            takeLocation(mCachedBounds.x, mCachedBounds.y);
            /*
            try {
                setEventsSuspended();
                setFrame(mCachedBounds);
            } finally {
                setEventsResumed();
            }
             */
            //System.out.println(getLabel() + " cachedBounds: " + mCachedBounds);
            //if (!DEBUG.SCROLL && !DEBUG.CONTAINMENT)
            //mCachedBoundsOld = false;
        }
        //setSize((float)bounds.getWidth(), (float)bounds.getHeight());
        if (DEBUG.CONTAINMENT && DEBUG.META)
            out("computed bounds: " + mCachedBounds);
        return mCachedBounds;
    }
    
    /*
    public java.awt.geom.Rectangle2D getCachedBounds()
    {
        return super.getBounds();
    }
     */
    
    /**
     * return the bounds for all LWComponents in the iterator
     * (includes shape stroke widhts)
     */
    public static Rectangle2D.Float getBounds(java.util.Iterator i) {
        Rectangle2D.Float rect = null;
        
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (c.isDrawn()) {
                if (rect == null) {
                    rect = new Rectangle2D.Float();
                    rect.setRect(c.getBounds());
                } else
                    rect.add(c.getBounds());
            }
        }
        return rect == null ? new Rectangle2D.Float() : rect;
    }
    
    /**
     * return the shape bounds for all LWComponents in the iterator
     * (does NOT include stroke widths) -- btw -- would make
     * more sense to put these in the LWContainer class.
     */
    public static Rectangle2D getShapeBounds(java.util.Iterator i) {
        Rectangle2D rect = new Rectangle2D.Float();
        
        if (i.hasNext()) {
            rect.setRect(((LWComponent)i.next()).getShapeBounds());
            while (i.hasNext())
                rect.add(((LWComponent)i.next()).getShapeBounds());
        }
        return rect;
    }
    
    /** returing a bounding rectangle that includes all the upper left
     * hand corners of the given components */
    public static Rectangle2D.Float getULCBounds(java.util.Iterator i) {
        Rectangle2D.Float rect = new Rectangle2D.Float();
        
        if (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            rect.x = c.getX();
            rect.y = c.getY();
            while (i.hasNext())
                rect.add(((LWComponent)i.next()).getLocation());
        }
        return rect;
    }
    /** returing a bounding rectangle that includes all the lower right
     * hand corners of the given components */
    public static Rectangle2D.Float getLRCBounds(java.util.Iterator i) {
        Rectangle2D.Float rect = new Rectangle2D.Float();
        
        if (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            rect.x = c.getX() + c.getWidth();
            rect.y = c.getY() + c.getHeight();
            while (i.hasNext()) {
                c = (LWComponent) i.next();
                rect.add(c.getX() + c.getWidth(),
                c.getY() + c.getHeight());
            }
        }
        return rect;
    }
    
    public String toString() {
        return "LWMap[" + getLabel()
        + " n=" + children.size()
        + (file==null?"":" <" + this.file + ">")
        + "]";
    }
    //todo: this method must be re-written. not to save and restore
    public Object clone() throws CloneNotSupportedException{
        try {
            String prefix = "concept_map";
            String suffix = ".vue";
            File tempFile  = File.createTempFile(prefix,suffix,VueUtil.getDefaultUserFolder());
            tufts.vue.action.ActionUtil.marshallMap(tempFile, this);
            return tufts.vue.action.OpenAction.loadMap(tempFile.getAbsolutePath());
        }catch(Exception ex) {
            throw new CloneNotSupportedException(ex.getMessage());
        }
    }
    
    
    public String X_paramString() {
        if (this.file == null)
            return " n=" + children.size();
        else
            return " n=" + children.size() + " <" + this.file + ">";
        
        /*
        if (this.file == null)
            return super.paramString();
        else
            return super.paramString() + " <" + this.file + ">";
         */
    }
    
    
    /*
    public int print(Graphics gc, PageFormat format, int pageIndex)
        throws java.awt.print.PrinterException
    {
        if (pageIndex > 0) {
            out("page " + pageIndex + " requested, ending print job.");
            return Printable.NO_SUCH_PAGE;
        }
     
        out("asked to render page " + pageIndex + " in " + outpf(format));
     
        Dimension page = new Dimension((int) format.getImageableWidth() - 1,
                                       (int) format.getImageableHeight() - 1);
     
        Graphics2D g = (Graphics2D) gc;
     
        if (DEBUG.Enabled) {
            g.setColor(Color.lightGray);
            g.fillRect(0,0, 9999,9999);
        }
     
        g.translate(format.getImageableX(), format.getImageableY());
     
        // Don't need to clip if printing whole map, as computed zoom
        // should have made sure everything is within page size
        //if (!isPrintingView())
        //g.clipRect(0, 0, page.width, page.height);
     
        if (DEBUG.Enabled) {
            //g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            // draw border outline of page
            g.setColor(Color.gray);
            g.setStroke(VueConstants.STROKE_TWO);
            g.drawRect(0, 0, page.width, page.height);
            //g.setComposite(AlphaComposite.Src);
        }
     
        // compute zoom & offset for visible map components
        Point2D offset = new Point2D.Double();
        // center vertically only if landscape mode
        //if (format.getOrientation() == PageFormat.LANDSCAPE)
        double scale = ZoomTool.computeZoomFit(page, 0, bounds, offset, false);
        out("rendering at scale " + scale);
        g.translate(-offset.getX(), -offset.getY());
        g.scale(scale,scale);
     
        if (isPrintingView())
            g.clipRect((int) Math.floor(bounds.getX()),
                       (int) Math.floor(bounds.getY()),
                       (int) Math.ceil(bounds.getWidth()),
                       (int) Math.ceil(bounds.getHeight()));
     
        if (DEBUG.Enabled) {
            g.setColor(Color.red);
            g.setStroke(VueConstants.STROKE_TWO);
            g.draw(bounds);
        }
     
        // set up the DrawContext
        DrawContext dc = new DrawContext(g, scale);
        dc.setPrinting(true);
        dc.setAntiAlias(true);
        // render the map
        this.draw(dc);
     
        out("page " + pageIndex + " rendered.");
        return Printable.PAGE_EXISTS;
    }
     */
    
    
    /*public Dimension getSize()
    {
        return new Dimension(getWidth(), getHeight());
        }*/
    
    /*
    public static Rectangle2D getBounds(java.util.Iterator i)
    {
        float xMin = Float.POSITIVE_INFINITY;
        float yMin = Float.POSITIVE_INFINITY;
        float xMax = Float.NEGATIVE_INFINITY;
        float yMax = Float.NEGATIVE_INFINITY;
     
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            float x = c.getX();
            float y = c.getY();
            float mx = x + c.getWidth();
            float my = y + c.getHeight();
            if (x < xMin) xMin = x;
            if (y < yMin) yMin = y;
            if (mx > xMax) xMax = mx;
            if (my > yMax) yMax = my;
        }
     
        // In case there's nothing in there
        if (xMin == Float.POSITIVE_INFINITY) xMin = 0;
        if (yMin == Float.POSITIVE_INFINITY) yMin = 0;
        if (xMax == Float.NEGATIVE_INFINITY) xMax = 0;
        if (yMax == Float.NEGATIVE_INFINITY) yMax = 0;
     
        return new Rectangle2D.Float(xMin, yMin, xMax - xMin, yMax - yMin);
    }
     
     */
    
    
    
}
