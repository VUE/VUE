package tufts.vue;

/**
 * LWMap.java
 *
 * This is the core VUE model class -- used for saving
 * and restoring maps to XML.
 *
 * @author Scott Fraize
 * @version 3/10/03
 */

import java.util.Vector;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.Graphics2D;
import java.io.File;
import tufts.vue.beans.*;
import tufts.vue.filter.*;

public class LWMap extends LWContainer
    implements ConceptMap
{
    // these for persistance use only
    private float userOriginX;
    private float userOriginY;
    private double userZoom = 1;
    private File file;
    
    //private LWPathwayManager mPathwayManager = null;
    private LWPathwayList mPathways = null;
    
    /** user map types **/
    private UserMapType [] mUserTypes = null;
    
    /** the author of the map **/
    private String mAuthor = null;
    
    /** the date **/
    private String mDate = null;
    
    /**description **/
    
    private String mDescription = null;
    /** the current Map Filter **/
    LWCFilter mLWCFilter = new LWCFilter();

    /** Metadata for Publishing **/ 
    Properties metadata = new Properties();
    
    /** Map Metadata-  this is for adding specific metadata and filtering **/
    
    MapFilterModel  mapFilterModel = new MapFilterModel();
    
    private long mChanges = 0;
    private Rectangle2D mCachedBounds = null;
    
    
    
    // only to be used during a restore from persisted
    public LWMap()
    {   
        setLabel("<map-during-XML-restoration>");
        //setEventsSuspended();
    	markDate();
    }

    public LWMap(String label)
    {
        setID("0");
        setFillColor(java.awt.Color.white);
        setTextColor(COLOR_TEXT);
        setStrokeColor(COLOR_STROKE);
        setFont(FONT_DEFAULT);
        setLabel(label);
        mPathways = new LWPathwayList(this);
        markDate();
        markAsSaved();
    }

    private void markDate()
    {
    	long time = System.currentTimeMillis();
    	java.util.Date date = new java.util.Date( time);
    	java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd");
    	String dateStr = df.format( date);
    	setDate(dateStr);
    }

    private UndoManager mUndoManager;
    public UndoManager getUndoManager()
    {
        return mUndoManager;
    }
    public void setUndoManager(UndoManager um)
    {
        if (mUndoManager != null)
            throw new IllegalStateException(this + " already has undo manager " + mUndoManager);
        mUndoManager = um;
    }

    public void setFile(File file)
    {
        this.file = file;
        if (file != null)
            setLabel(file.getName());
    }

    public File getFile()
    {
        return this.file;
    }

    public void markAsModified()
    {
        System.out.println(this + " explicitly marking as modified");
        if (mChanges == 0)
            mChanges = 1;
        // notify with an event mark as not for repaint (and set same bit on "repaint" event)
    }
    public void markAsSaved()
    {
        System.out.println(this + " marking " + mChanges + " modifications as current");
        mChanges = 0;
        /*
        if (getUndoManager() == null)
            setUndoManager(new UndoManager(this));
        else
            getUndoManager().flush();
        */
        // todo: notify with an event mark as not for repaint (and set same bit on "repaint" event)
    }
    public boolean isModified() {
        return mChanges > 0;
    }
    long getModCount() { return mChanges; } 
    
    /**
     * getLWCFilter()
     * This gets the current LWC filter
     **/
    public LWCFilter getLWCFilter() {
    	return mLWCFilter;
    }
    
    /**
     * setLWCFilter
     * This sets the LWC Filter to filter out node and link componenets.
     * @param LWCFilter - the filter
     **/
    public void setLWCFilter( LWCFilter pFilter) {
    	mLWCFilter = pFilter;
    }
    
    /**
     * getUserMapTypes
     * This returns an array of available map types for this
     * map.
     * @return UserMapType [] the array of map types
     **/
    public UserMapType [] getUserMapTypes() {
     	return mUserTypes;
     }
     
    /**
     * \Types
     * This sets the array of UserMapTypes for teh map
     *  @param pTypes - uthe array of UserMapTypes
     **/
    public void setUserMapTypes( UserMapType [] pTypes) {
    	mUserTypes = pTypes;
    	validateUserMapTypes();
    }
    
    /**
     * validateUserMapTypes
     * Searches the list of LW Compone
     **/
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
 	
 	/**
 	 * hasUserMapType
 	 * This method verifies that the UserMapType exists for this Map.
 	 * @return boolean true if exists; false if not
 	 **/
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
       this.mapFilterModel = mapFilterModel;
   }
    /* 
    public LWPathwayManager getPathwayManager(){ 
        return mPathwayManager;
    }
    */
    public LWPathwayList getPathwayList() { 
        return mPathways;
    }
    /** for persistance restore only */
    public void setPathwayList(LWPathwayList l){
        System.out.println(this + " pathways set to " + l);
        mPathways = l;
        mPathways.setMap(this);
    }

    /*
    public void setPathwayManager(LWPathwayManager manager)
    {
        mPathwayManager = manager;
        mPathwayManager.setMap(this);
    }
    */
    
    private int nextID = 1;
    protected String getNextUniqueID()
    {
        return Integer.toString(nextID++, 10);
    }

    public void completeXMLRestore()
    {
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

    protected LWComponent findChildByID(String ID)
    {
        LWComponent c = super.findChildByID(ID);
        if (c == null) {
            System.err.println(this + " failed to locate a LWComponent with id [" + ID + "]");
            return null;
        } else
            return c;
    }
    
    /** for viewer to report user origin sets via pan drags */
    void setUserOrigin(float x, float y)
    {
        if (userOriginX != x || userOriginY != y){
            this.userOriginX = x;
            this.userOriginY = y;
            markChange("userOrigin");
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

    /** for persistance.  Note that as maps can be in more than
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
    
    public LWNode addNode(LWNode c)
    {
        addChild(c);
        return c;
    }
    public LWLink addLink(LWLink c)
    {
        addChild(c);
        return c;
    }

    public LWPathway addPathway(LWPathway p)
    {
        getPathwayList().add(p);
        return p;
    }

    protected void addChildInternal(LWComponent c)
    {
        if (c instanceof LWPathway)
            throw new IllegalArgumentException("LWPathways not added as direct children of map: use addPathway " + c);
        super.addChildInternal(c);
    }
    
    LWComponent addLWC(LWComponent c)
    {
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
    protected void notifyLWCListeners(LWCEvent e)
    {
        if (mEventsDisabled) {
            if (DEBUG.EVENTS) System.out.println(e + " SKIPPING (events disabled)");
            return;
        }
        // consider flushing bounds if layout() called also (any child layout bubbles up to us)
        // todo pref: should be safe to only do this if a size, location or scale, hide or filter event.
        String what = e.getWhat();
        if (what != LWKey.Repaint && what != LWKey.Scale) {
            // repaint is for non-permanent changes.
            // scale sets not considered modifications as they can
            // happen do to rollover -- any time a scale happens
            // otherwise will be in conjunction with a reparenting
            // event, and so we'll detect the change that way.
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
    
    public java.awt.geom.Rectangle2D getBounds()
    {
        if (true||mCachedBounds == null) {
            mCachedBounds = getBounds(getChildIterator());
            try {
                setEventsSuspended();
                setFrame(mCachedBounds);
            } finally {
                setEventsResumed();
            }
            //System.out.println(getLabel() + " cachedBounds: " + mCachedBounds);
            //if (!DEBUG.SCROLL && !DEBUG.CONTAINMENT)
            //mCachedBoundsOld = false;
        }
        //setSize((float)bounds.getWidth(), (float)bounds.getHeight());
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
    public static Rectangle2D getBounds(java.util.Iterator i)
    {
        Rectangle2D rect = null;

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
    public static Rectangle2D getShapeBounds(java.util.Iterator i)
    {
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
    public static Rectangle2D.Float getULCBounds(java.util.Iterator i)
    {
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
    public static Rectangle2D.Float getLRCBounds(java.util.Iterator i)
    {
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

    public String toString()
    {
        return "LWMap[" + getLabel()
            + " n=" + children.size()
            + (file==null?"":" <" + this.file + ">")
            + "]";
    }
    public String X_paramString()
    {
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
