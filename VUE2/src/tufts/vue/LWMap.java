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
import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.Graphics2D;
import java.io.File;
import tufts.vue.beans.*;

public class LWMap extends LWContainer
    implements ConceptMap
{
    // these for persistance use only
    private float userOriginX;
    private float userOriginY;
    private double userZoom = 1;
    private File file;
    
    private LWPathwayManager mPathwayManager = null;
    
    /** user map types **/
    private UserMapType [] mUserTypes = null;
    
    /** the author of the map **/
    private String mAuthor = null;
    
    /** the date **/
    private String mDate = null;
    
    /** the current Map Filter **/
    LWCFilter mLWCFilter = new LWCFilter();

    private long mChanges = 0;
    private boolean mCachedBoundsOld = true;
    
    // only to be used during a restore from persisted
    public LWMap()
    {   
        setLabel("<map-during-XML-restoration>");
        // this can't be right: what about the LWPathwayManager that castor is constructing for us?
        mPathwayManager = new LWPathwayManager(this);
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
        mPathwayManager = new LWPathwayManager(this);
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
        if (getUndoManager() == null)
            setUndoManager(new UndoManager(this));
        // notify with an event mark as not for repaint (and set same bit on "repaint" event)
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
    
    public String getDate() {
    	return mDate;
   }
   public void setDate( String pDate) {
   	mDate = pDate;
   }
    
     
     
    public LWPathwayManager getPathwayManager(){ 
        return mPathwayManager;
    }
    
    public void setPathwayManager(LWPathwayManager manager)
    {
        mPathwayManager = manager;
        mPathwayManager.setMap(this);
    }
    
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
        mPathwayManager.completeXMLRestore();
        this.nextID = findGreatestChildID() + 1;
        System.out.println(getLabel() + ": nextID=" + nextID);
        System.out.println(getLabel() + ": restore completed.");

        Iterator i = getAllDescendents().iterator();//slow
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            c.layout();
        }
        markAsSaved();
    }
    
    public void draw(DrawContext dc){
        //LWPathway path = this.getPathwayManager().getCurrentPathway();        
        Iterator i = getPathwayManager().getPathwayIterator();
        int pathIndex = 0;
        while (i.hasNext()) {
            Object o = i.next();
            if (o instanceof LWPathway) {
                LWPathway path = (LWPathway) o;
                if (path != null && path.getShowing()) {
                    dc.setIndex(pathIndex++);
                    path.drawPathway(dc.create());
                }
            } else {
                // What're the nodes doing in here?
                //System.out.println("Not a pathway? " + o);
            }
        }
        super.draw(dc);

        if (DEBUG.SCROLL || DEBUG.CONTAINMENT) {
            dc.g.setColor(java.awt.Color.red);
            dc.setAbsoluteStrokeWidth(1);
            dc.g.draw(getBounds());
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
        this.userOriginX = x;
        this.userOriginY = y;
    }
    /** for persistance */
    public Point2D.Float getUserOrigin()
    {
        return new Point2D.Float(this.userOriginX, this.userOriginY);
    }
    /** for persistance */
    public void setUserOrigin(Point2D.Float p)
    {
        setUserOrigin((float) p.getX(), (float) p.getY());
    }
    /** for persistance */
    public void setUserZoom(double zoom)
    {
        this.userZoom = zoom;
        //notify("userZoom");//todo perf: mapviewer may be doing needless repaints
    }
    /** for persistance */
    public double getUserZoom()
    {
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

    protected LWComponent defaultHitComponent()
    {
        return null;
    }
    

    LWNode addNode(LWNode c)
    {
        addChild(c);
        return c;
    }
    LWLink addLink(LWLink c)
    {
        addChild(c);
        return c;
    }
    
    LWPathway addPathway(LWPathway c)
    {
        mPathwayManager.addPathway(c);
        return c;
    }
    
    LWComponent addLWC(LWComponent c)
    {
        addChild(c);
        return c;
    }
    private void removeLWC(LWComponent c)
    {
        removeChild(c);
    }

    /**
     * Every single event anywhere in the map will ultimately end up
     * calling this notifyLWCListners.
     */
    protected void notifyLWCListeners(LWCEvent e)
    {
        mCachedBoundsOld = true; // consider flushing bounds if layout() called also (any child layout bubbles up to us)
        String what = e.getWhat();
        if (what != LWCEvent.Repaint && what != LWCEvent.Scale) {
            // repaint is for non-permanent changes.
            // scale sets not considered modifications as they can
            // happen do to rollover -- any time a scale happens
            // otherwise will be in conjunction with a reparenting
            // event, and so we'll detect the change that way.
            if (DEBUG.EVENTS && mChanges == 0)
                new Throwable("FIRST MODIFICATION " + e).printStackTrace();
            mChanges++;
        }
        super.notifyLWCListeners(e);
    }
    
    private Rectangle2D mCachedBounds = null;
    public java.awt.geom.Rectangle2D getBounds()
    {
        if (mCachedBoundsOld) {
            mCachedBounds = getBounds(getChildIterator());
            setFrame(mCachedBounds);
            //System.out.println(getLabel() + " cachedBounds: " + mCachedBounds);
            if (!DEBUG.SCROLL && !DEBUG.CONTAINMENT)
                mCachedBoundsOld = false;
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
        Rectangle2D rect = new Rectangle2D.Float();

        if (i.hasNext()) {
            rect.setRect(((LWComponent)i.next()).getBounds());
            while (i.hasNext())
                rect.add(((LWComponent)i.next()).getBounds());
        }
        return rect;
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

    public String paramString()
    {
        return super.paramString() + " <" + this.file + ">";
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
