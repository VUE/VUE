/*
* Copyright 2003-2010 Tufts University  Licensed under the
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
import tufts.vue.LWComponent.ColorProperty;
import tufts.vue.LWComponent.Key;
import tufts.vue.LWComponent.Property;
import tufts.vue.filter.*;
import tufts.vue.ds.Schema;

import java.net.URI;
import java.util.*;
import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.Printable;
import java.awt.print.PageFormat;
import java.io.File;

/**
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
 * @version $Revision: 1.262 $ / $Date: 2010-02-03 19:17:41 $ / $Author: mike $
 */

public class LWMap extends LWContainer
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(LWMap.class);

    /** file we were opened from of saved to, if any */
    private File mFile;
    private String mSaveLocation;
    private URI mSaveLocationURI;
    private String mSaveFile;
    private File mLastSaveLocation;
    
    /** the list of LWPathways, if any */
    private LWPathwayList mPathways;
    // todo: rename mPathwayList unless we get rid of mPathways in LWComponent (at least both are private)    
    
    /** the author of the map **/
    private String mAuthor;
    
    /** the date created **/
    // todo: change to arbirary meta-data, and add modification date & user-name of modifier
    private String mDateCreated;
    
    // /** the current Map Filter **/
    // LWCFilter mLWCFilter;
    
    /** Metadata for Publishing **/
    PropertyMap metadata = new PropertyMap();
    
    /* @deprecated - Map Metadata-  this is for adding specific metadata and filtering **/
    // MapFilterModel  mapFilterModel = new MapFilterModel();
    /* user map types -- is this still used? **/
    //private UserMapType[] mUserTypes;
    
    /** the number of "changes" since we last saved.  E.g., on deserialization, and after saves, this is 0. */
    private long mChanges = 0;
    /** just like mChages, but this is never reset to zero.  E.g., caches that depend on map state can use
     * this to see if the map has changed -- e.g., RDFIndex. */
    private long mChangeState = 0;
    
    private Rectangle2D.Float mCachedBounds = null;
    
    
    // these for persistance use only
    private float userOriginX;
    private float userOriginY;
    private double userZoom = 1;

    //private transient boolean isLayered;
    private transient Layer mActiveLayer;
    /** for use during restores only */
    private transient java.util.List<Layer> mLayers = new ArrayList();
    //private transient Layer mInternalLayer;

    private transient int mSaveFileModelVersion = -1;
    private transient int mModelVersion = getCurrentModelVersion();

    /** only used during restore */
    private final Collection<Schema> mRestoredSchemas = new ArrayList();

    private static final String InitLabel = "<map-during-XML-restoration>";
    
    // only to be used during a restore from persisted
    public LWMap() {
        initMap();
        setLabel(InitLabel);
        //mLWCFilter = new LWCFilter(this);
        // on restore, set to 0 initially: if has a model version in the save file,
        // it will be overwritten
        setModelVersion(0); 
    }
    
    public LWMap(String label) {
        initMap();
        setID("0");
        setFillColor(java.awt.Color.white);
        setTextColor(COLOR_TEXT);
        setStrokeColor(COLOR_STROKE);
        setFont(FONT_DEFAULT);
        setLabel(label);
        mPathways = new LWPathwayList(this);
        //mLWCFilter = new LWCFilter(this);
        // Always do markDate, then markAsSaved as the last items in the constructor:
        // (otherwise this map will look like it's user-modified when it first displays)
        markDate();

        installDefaultLayers();
        ensureID(this); // make sure the new default layers get ID's
        
        markAsSaved();
    }

    public static LWMap create(String filename) {
        final LWMap map = new LWMap("Empty Map");
        map.setFile(new File(filename));
        return map;
    }

    /** create a temporary, uneditable map that contains just the given component */
    LWMap(LWComponent c) {
        this(c.getDisplayLabel());
        if (c instanceof LWGroup && c.getFillColor() != null)
            setFillColor(c.getFillColor());
        mChildren.add(c);
        // todo: listen to child for events & pass up
    }

    protected void initMap() {
        disablePropertyTypes(KeyType.STYLE);
        enableProperty(LWKey.FillColor);
        disableProperty(LWKey.Label);
        mFillColor.setAllowAlpha(false);
// //         // TODO: need to handle persistance -- could match via a special name, for maybe persistIsStyle
// //         mInternalLayer = new Layer("*Internal*");
// //         mInternalLayer.setVisible(false);
// //         mInternalLayer.setFlag(Flag.INTERNAL);
// //         addChild(mInternalLayer);
    }

    // if/when we support maps embedded in maps, we'll want to have these return something real / make not final
    @Override public final float getX() { return 0; }
    @Override public final float getY() { return 0; }
    @Override public final float getMapX() { return 0; }
    @Override public final float getMapY() { return 0; }
    @Override protected final double getMapXPrecise() { return 0; }
    @Override protected final double getMapYPrecise() { return 0; }
    @Override public final boolean isCollapsed() { return false; }
    @Override public final boolean isAncestorCollapsed() { return false; }

    /** @return 1 -- currently for performance: remove this impl if we embed maps-in-maps and allow them to have their own scale */
    @Override public final double getMapScale() { return 1; }
    
    /** Override LWContainer draw to always call drawInParent (even tho we have absolute children, we
     * don't want to just call draw, as LWContainer would).
     */
    // todo: something cleaner than draw/drawInParent in LWComponent would be nice so
    // we wouldn't have to deal with this kind of issue.  This is all because of the
    // slide icon hack -- if we can get rid of that, these issues would go away.
    @Override
    protected void drawChild(LWComponent child, DrawContext dc)
    {
        child.drawLocal(dc);
    }


    /** for persistance */
    public int getModelVersion() {
        return mModelVersion;
    }
    
    /** for persistance */
    public void setModelVersion(int version) {
        if (DEBUG.Enabled) {
            if (this.label != InitLabel) // don't bother with this message on construction
                Log.debug("setModelVersion " + version + "; current=" + mModelVersion);
        }
        mModelVersion = version;
        mSaveFileModelVersion = version;
    }

    public int getSaveFileModelVersion() {
        return mSaveFileModelVersion;
    }
    
    
    
    @Override String getDiagnosticLabel() {
        return "Map: " + getLabel();
    }
    
    private void markDate() {
        final long time = System.currentTimeMillis();
        super.setCreated(time);
        java.util.Date date = new java.util.Date( time);
        java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd");
        String dateStr = df.format( date);
        setDate(dateStr);
    }
    
    private UndoManager mUndoManager;
    @Override
    public UndoManager getUndoManager() {
        return mUndoManager;
    }
    public void setUndoManager(UndoManager um) {
        if (DEBUG.EVENTS) out("setUndoManager " + um);
        if (mUndoManager != null)
            throw new IllegalStateException(this + " already has undo manager " + mUndoManager);
        mUndoManager = um;
        markAsSaved();
    }
    
    public File getFile() {
        return mFile;
    }

    public void setFile(File file) {
        Log.debug("setFile " + file);
        mFile = file;
        if (mFile != null) {
            setLabel(mFile.getName()); // todo: don't let this be undoable!
            final File parentDir = mFile.getParentFile();
            mSaveLocation = parentDir.toString();
            Log.debug("saveLocation " + mSaveLocation);
            mSaveLocationURI = parentDir.toURI();
            Log.debug("saveLocationURI " + mSaveLocationURI);
            takeResource(Resource.instance(mFile));
        } else {
            takeResource(null);
        }



// Don't handle relativzation here.  On save, handle vie makeReadyForSaving, and on restore,
// handle in XML_completed.
//         if (false && mFile != null && !mXMLRestoreUnderway) { // not turned on yet
//             // only do this on save: will be handled in completeXMLRestore
//             // for restores
//             relativizeResources(getAllDescendents(ChildKind.ANY),
//                                 mSaveLocationURI);
//         }
        
    }

    /** persistance only */
    public String getSaveLocation() {
        return mSaveLocation == null ? null : mSaveLocation.toString();
    }

    /** persistance only */
    public String getSaveFile() {
        return mFile == null ? null : mFile.toString();
    }
    

    /** persistance only */
    public void setSaveLocation(String path) {
        mSaveLocation = path;
        mSaveLocationURI = null; // may not be valid if save file was from another platform
    }

    /** persistance only */
    public void setSaveFile(String path) {
        mSaveFile = path;
    }
    
    
    public void markAsModified() {
        mChangeState++;
        if (DEBUG.Enabled) Log.debug("ensuring marked as modified: " + this + "; newChangeState = " + mChangeState);
        if (mChanges == 0) {
            mChanges = 1;
            if (DEBUG.Enabled) Log.debug("explicit marked as modified: " + this);
        }
        // notify with an event mark as not for repaint (and set same bit on "repaint" event)
    }
    public void markAsSaved() {
        if (DEBUG.Enabled && mChanges != 0) Log.debug("marking " + mChanges + " modifications as current: " + this);
        mChanges = 0;
    }
    public boolean isModified() {
        return mChanges > 0;
    }
    long getModCount() { return mChanges; }

    /** just like getModCount / mChanges, but this is never reset to zero.  E.g., caches that depend on map state can use
     * this to see if the map has changed -- e.g., RDFIndex. */
    public long getChangeState() {
        return mChangeState;
    }
    
    /** @deprecated this api / LWCFilter is no longer used -- always return false */
    public final boolean isCurrentlyFiltered() { return false; }
    /** @deprecated this api / LWCFilter is no longer used -- always return null */
    public final LWCFilter getLWCFilter() { return null; }
    /** @deprecated this api / LWCFilter is no longer used -- always return null */
    public final UserMapType[] getUserMapTypes() { return null; }
    /** @deprecated this api / LWCFilter is no longer used -- always dump exception */
    public final void setUserMapTypes(UserMapType[] x) { Log.warn("UserMapType deprecated: " + Util.tags(x), new Throwable("HERE")); }
    
    // SMF Summer 2012 commented out below en-masse: LWCFilter no longer used
    // /**
    //  * getLWCFilter()
    //  * This gets the current LWC filter
    //  **/
    // public LWCFilter getLWCFilter() {
    //     return mLWCFilter;
    // }
    // /** @return true if this map currently conditionally displaying
    //  * it's components based on a filter */
    // public boolean isCurrentlyFiltered() {
    //     return mLWCFilter != null && mLWCFilter.isFilterOn() && mLWCFilter.hasPersistentAction();
    // }
    // /**
    //  * This tells us there's a new LWCFilter or filter state in effect
    //  * for the filtering of node's & links.
    //  * This should be called anytime the filtering is to change, even if we
    //  * already have our filter set to the given filter.  We will
    //  * apply / clear as appropriate to the state of the filter.
    //  * @param LWCFilter the filter to install and/or update against
    //  **/
    // private boolean filterWasOn = false; // workaround for filter bug
    // public void setLWCFilter(LWCFilter filter) {
    //     out("setLWCFilter: " + filter);
    //     mLWCFilter = filter;
    //     applyFilter();
    // }
    // public  void clearFilter() {
    //     out("clearFilter: cur=" + mLWCFilter);
    //      if (mLWCFilter.getFilterAction() == LWCFilter.ACTION_SELECT)
    //          VUE.getSelection().clear();
    //     for (LWComponent c : getAllDescendents())
    //         c.setFiltered(false);
    //     mLWCFilter.setFilterOn(false);       
    //     notify(LWKey.MapFilter);
    // }
    // public  void applyFilter()
    // {
    //     if (mLWCFilter.getFilterAction() == LWCFilter.ACTION_SELECT)
    //         VUE.getSelection().clear();
    //     for (LWComponent c : getAllDescendents()) {
    //         if (!(c instanceof LWNode) && !(c instanceof LWLink)) // why are we only doing nodes & links?
    //             continue;
    //         boolean state = mLWCFilter.isMatch(c);
    //         if (mLWCFilter.isLogicalNot())
    //             state = !state;
    //         if (mLWCFilter.getFilterAction() == LWCFilter.ACTION_HIDE)
    //             c.setFiltered(state);
    //         else if (mLWCFilter.getFilterAction() == LWCFilter.ACTION_SHOW)
    //             c.setFiltered(!state);
    //         else if (mLWCFilter.getFilterAction() == LWCFilter.ACTION_SELECT) {
    //             if (state)
    //                 VUE.getSelection().add(c);
    //         }
    //     }
    //     filterWasOn = true;
    //     mLWCFilter.setFilterOn(true);
    //     notify(LWKey.MapFilter); // only MapTabbedPane wants to know this, to display a filtered icon...
    // }
    // /**
    //  * getUserMapTypes [was related to LWCfilter]
    //  * This returns an array of available map types for this
    //  * map.
    //  * @return UserMapType [] the array of map types
    //  **/
    // public UserMapType [] getUserMapTypes() {
    //     throw new UnsupportedOperationException("de-implemented");
    //     //return mUserTypes;
    // }
    // /**
    //  * setUserMapTypes -- was related to LWCFilter
    //  * \Types if(filterTable.isEditing()) {
    //  * filterTable.getCellEditor(filterTable.getEditingRow(),filterTable.getEditingColumn()).stopCellEditing();
    //  * System.out.println("Focus Lost: Row="+filterTable.getEditingRow()+ "col ="+ filterTable.getEditingColumn());
    //  * }
    //  * filterTable.removeEditor();
    //  * This sets the array of UserMapTypes for teh map
    //  *  @param pTypes - uthe array of UserMapTypes
    //  **/
    // public void setUserMapTypes( UserMapType [] pTypes) {
    //     throw new UnsupportedOperationException("de-implemented");
    //     //mUserTypes = pTypes;
    //     //validateUserMapTypes();
    // }
    //---------------------------------------------------------------------------------------------------
    // This stuff was previously commented out already:
    //---------------------------------------------------------------------------------------------------
    // /*
    //  * validateUserMapTypes
    //  * Searches the list of LW Compone
    // private void validateUserMapTypes() {
    //     java.util.List list = getAllDescendents();
    //     Iterator it = list.iterator();
    //     while (it.hasNext()) {
    //         LWComponent c = (LWComponent) it.next();
    //         if ( c.getUserMapType() != null)  {
    //             // Check that type still exists...
    //             UserMapType type = c.getUserMapType();
    //             if( !hasUserMapType( type) ) {
    //                 c.setUserMapType( null);
    //             }
    //         }
    //     }
    // } **/
    // /*
    //  * hasUserMapType
    //  * This method verifies that the UserMapType exists for this Map.
    //  * @return boolean true if exists; false if not
    // private boolean hasUserMapType( UserMapType pType) {
    //     boolean found = false;
    //     if( mUserTypes != null) {
    //         for( int i=0; i< mUserTypes.length; i++) {
    //             if( pType.getID().equals( mUserTypes[i].getID() ) ) {
    //                 return true;
    //             }
    //         }
    //     }
    //     return found;
    // } **/
    
    // todo: change this to arbitrary meta-data
    public String getAuthor() {
        return mAuthor;
    }
    
    // todo: change this to arbitrary meta-data
    public void setAuthor( String pName) {
        mAuthor = pName;
    }
    
    
    /** @deprecated - safe file back compat only - use getNotes - this always returns null */
    public String getDescription() {
        //return mDescription;
        return null;
    }
    
    /** @deprecated - use setNotes -- this does nothing if notes already are set */
    public void setDescription(String pDescription) {
        //mDescription = pDescription;
        // backward compat: we're getting rid of redunant
        // "description" field that was supposed to have
        // used notes in the first place. This should
        // only be happening from a map-restore, as
        // nobody should be calling setDescription anymore.
        if (!hasNotes()) 
            setNotes(pDescription);
    }
    
    // creation date.  todo: change this to arbitrary meta-data
    public String getDate() {
        return mDateCreated;
    }
    
    // creation date. todo: change this to arbitrary meta-data
    public void setDate(String pDate) {
        mDateCreated = pDate;
    }

    protected int	mNodeCount,
    				mLinkCount,
    				mGroupCount,
    				mConnectionCount,
    				mMinConnections,
    				mMaxConnections;

    // TODO: would be nice if this info were cached instead of computing
    // it every time we change the active map!  Or better yet, ONLY
    // if the MapInspectorPanel is actually visibile.
    protected void countChildren(LWComponent c) {
    	for (LWComponent child : c.getChildren()) {
            if (child instanceof LWGroup) {
                mGroupCount++;
            } else if (child instanceof LWNode) {
                mNodeCount++;

            	int		connections = 0;

            	for (LWLink link: child.getLinks()) {
                    LWComponent		head = link.getPersistHead(),
                        tail = link.getPersistTail();

                    if (head != null && tail != null && head instanceof LWNode && tail instanceof LWNode) {
                        connections++;
                    }
            	}

                if (mMinConnections == -1 || connections < mMinConnections) {
                    mMinConnections = connections;
                }

                if (connections > mMaxConnections) {
                    mMaxConnections = connections;
                }

                mConnectionCount += connections;
            } else if (child instanceof LWLink) {
                mLinkCount++;
            }

            countChildren(child);
    	}
    }

    /** the formatting should be in MapInspectorPanel, not LWMap */
    public String getObjectStatistics() {
    	mNodeCount = 0;
    	mLinkCount = 0;
    	mGroupCount = 0;
    	mConnectionCount = 0;
    	mMinConnections = -1;
    	mMaxConnections = 0;

    	countChildren(this);

        if (mMinConnections == -1) {
        	mMinConnections = 0;
        }

        return String.format(Locale.getDefault(), VueResources.getString("mapinspectorpanel.objectStats.format"),
        		VueResources.getString("mapinspectorpanel.objectStats.nodes"), mNodeCount,
        		VueResources.getString("mapinspectorpanel.objectStats.links"), mLinkCount,
        		VueResources.getString("mapinspectorpanel.objectStats.groups"), mGroupCount);
    }
    
    public String getConnectivityStatistics() {
    	// Expects that getObjectStatistics() will be called first to make counts -- no need to do it again.
    	double	avg = (mNodeCount == 0 ? 0.0 : ((double)mConnectionCount) / (double)mNodeCount);

		return String.format(Locale.getDefault(), VueResources.getString("mapinspectorpanel.connectivityStats.format"),
				VueResources.getString("mapinspectorpanel.connectivityStats.min"), mMinConnections,
				VueResources.getString("mapinspectorpanel.connectivityStats.max"), mMaxConnections,
				VueResources.getString("mapinspectorpanel.connectivityStats.avg"), avg);
    }
    
    public PropertyMap getMetadata(){
        return metadata;
    }
    
    public void setMetadata(PropertyMap metadata) {
        this.metadata = metadata;
    }
    
    /** @deprecated -- no longer used -- always returns null for old persistance compatability */
    public MapFilterModel getMapFilterModel() {
        return null;
        //return mapFilterModel;
    }
    // /** @deprecated -- MapFilterModel no longer in use */
    // public void setMapFilterModel(MapFilterModel mapFilterModel) {
    //     //out("setMapFilterModel " + mapFilterModel);
    //     this.mapFilterModel = mapFilterModel;
    // }
    
    public LWPathwayList getPathwayList() {
        return mPathways;
    }
    
    /** for persistance restore only */
    public void setPathwayList(LWPathwayList l){
        //System.out.println(this + " pathways set to " + l);
        mPathways = l;
        mPathways.setMap(this);
    }

    /** @return true */
    @Override
    public final boolean isTopLevel() {
        return true;
    }
    
    /** @return true if we have any non-layer children */
    @Override
    public boolean hasContent() {
        for (LWComponent c : getChildren()) {
            if (c instanceof Layer) {
                if (c.hasContent())
                    return true;
            } else
                return true;
        }
        return false;
    }

    /** @return all LWComponent's at the top level of all unlocked of the given kind */
    public Collection<LWComponent> getTopLevelItems(ChildKind kind)
    {
        return getAllLayerDescendents(kind, new ArrayList(), Order.DEPTH, true);
    }
    
    private Collection<LWComponent> getAllLayerDescendents(ChildKind kind, Collection bag, Order order, boolean onlyTopLevel)
    {
        for (LWComponent layer : getChildren()) {
            
            // exclude the layer objects themseleves, but include their children
            
            if ((kind == ChildKind.VISIBLE || kind == ChildKind.EDITABLE) && layer.isHidden()) {
                ; // exclude invisible
            }
            else if (kind == ChildKind.EDITABLE && layer.isLocked()) {
                    ; // exclude locked out
            }
            else if (layer instanceof Layer) {            
                // should always be the case (is a Layer)
                if (onlyTopLevel) {
                    // note: this will add hidden/filtered items as well -- could argue either way given
                    // the usage (currently only used for getting nodes to project)
                    bag.addAll(layer.getChildren());
                } else {
                    layer.getAllDescendents(kind, bag, order);
                }
            }
            else {
                // failsafe in case anything has leaked up to the map level
                Log.warn("child of map, not layer: " + layer);
                bag.add(layer);
            }
        }
        return bag;
    }

    // TODO PERFORMANCE: cache results for each kind in immutable lists; only flush if modification count goes up.
    // (to verify: modification count goes up when layers are locked, anything is hidden/shown, which will effect EDITABLE lists)
    @Override
    public Collection<LWComponent> getAllDescendents(final ChildKind kind, final Collection bag, Order order) {

        if (kind == ChildKind.ANY) {
            // include the layers and all descendents
            super.getAllDescendents(kind, bag, order);
        } else {
            getAllLayerDescendents(kind, bag, order, false);
        }

        if (kind == ChildKind.ANY) {
            if (mPathways != null) {
                for (LWPathway pathway : mPathways) {
                    bag.add(pathway);
                    pathway.getAllDescendents(kind, bag, order);
                }
            }
        }
        return bag;
    }

    /**
     * @return all instances of Resource objects found in the map, even if some of them point to the same destination.
     */
    public Collection<Resource> getAllResources() {

        final Map<Resource,Boolean> resources = new IdentityHashMap(); // really, want IdentityHashSet

        for (LWComponent c : getAllDescendents(ChildKind.ANY))
            if (c.hasResource())
                resources.put(c.getResource(), Boolean.TRUE);

        return resources.keySet();
    }

    /**
     * @return all destination unique (Resource.equals, compairing spec) Resources
     * found in the map.  So even if there are different Resource instances in
     * the map that both have the same spec, only the first one found will be
     * in the returned collection.
     */
    public Collection<Resource> getAllUniqueResources() {

        final Set resources = new HashSet();

        for (Resource r : getAllResources()) {
            if (resources.add(r)) {
                if (DEBUG.RESOURCE) Log.debug("getAllUniqueResources: Found resource " + r);
            } else {
                if (DEBUG.RESOURCE) Log.debug("getAllUniqueResources:      duplicate " + r);
            }
        }

        return resources;
    }

    private Collection<PropertyEntry> mArchiveManifest;

    public void setArchiveManifest(Collection<PropertyEntry> manifest) {
        mArchiveManifest = manifest;
    }

    public Collection getArchiveManifest() {
        if (mXMLRestoreUnderway) {
            if (mArchiveManifest == null)
                mArchiveManifest = new ArrayList();
        }
        return mArchiveManifest;
    }

    private boolean isArchive;
    public void setArchiveMap(boolean asArchive) {
        isArchive = asArchive;
    }

    public boolean isArchiveMap() {
        return isArchive;
        //return hasLabel() && getLabel().endsWith("$map.vue");
        //return hasLabel() && getLabel().endsWith(".vpk");
    }

    private final java.util.concurrent.atomic.AtomicInteger mNextID = new java.util.concurrent.atomic.AtomicInteger();
    protected String getNextUniqueID() {
        return Integer.toString(mNextID.getAndIncrement(), 10);
    }

//     private static void changeAbsoluteToRelativeCoords(LWContainer container, HashSet<LWComponent> processed) {
// //         LWContainer parent = container.getParent();
// //         //if ((parent instanceof LWGroup || parent instanceof LWSlide) && !processed.contains(parent)) {
// //         if (parent instanceof LWSlide && !processed.contains(parent)) {
// //             // we should never see this (slide in a slide?) -- this was in case
// //             // we had a group inside a slide, which we saw before the slide itself,
// //             // and it never would have worked for something more than one level below
// //             // the slide!
// //             changeAbsoluteToRelativeCoords(parent, processed);
// //         }
//         for (LWComponent c : container.getChildList()) {
//             c.takeLocation(c.getX() - container.getX(),
//                            c.getY() - container.getY());
//         }
//         processed.add(container);
//     }

    /** recursively upgrade all children */
    
    private void upgradeAbsoluteToRelativeCoords(Collection<LWComponent> children)
    {
        // We must process the components top-down: adjust parent, then adjust children
        // Start by calling this on the children of the map itself.  The top level
        // children of the don't need adjusting -- only their children.

        for (LWComponent c : children) {
            if (c instanceof LWContainer && c.hasChildren()) {

                if (c instanceof LWGroup) {
                    // old groups didn't support fill color, but may have had one persisted anyway:
                    if (java.awt.Color.white.equals(c.getFillColor()))
                        c.setFillColor(null);
                    if (isGroupRelative(getModelVersion())) {
                        if (DEBUG.Enabled) System.out.println(" DM#1 ALREADY RELATIVE EXCEPT LINKS: " + c);
                        //((LWGroup)c).normalize(); // we normalize all groups later
                        continue;
                    }
                }
                
                if (!c.isManagingChildLocations())
                    upgradeChildrenFromAbsoluteToRelative((LWContainer) c);
                upgradeAbsoluteToRelativeCoords(c.getChildren());
            }
        }
    }

    private void upgradeLinksToParentRelative(Collection<LWComponent> nodes)
    {
        for (LWComponent c : nodes) {
            if (c instanceof LWLink == false)
                continue;

            final LWLink link = (LWLink) c;
            final LWContainer parent = link.getParent();

            if (parent instanceof LWMap) { // okay instanceof check: for explicit backward compat
                //if (DEBUG.Enabled) System.out.println("LINK ALREADY RELATIVE: " + link);
            } else {
                //if (DEBUG.Enabled) System.out.println("MAKING LINK PARENT RELATIVE: " + link);
                if (DEBUG.Enabled) link.out("MAKING LINK PARENT RELATIVE");
                // theoretically the parent could be scaled -- e.g., link is in a group that
                // is in a node, tho this is rare case...
                // Parent container should already have been converted to relative,
                // so we now need to use getMapX/Y here for the offset:
                link.translate(-parent.getMapX(), -parent.getMapY());
            }
            
            // Now make sure link is parented to it's common parent:
            link.run();
        }
    }
    
    

    private void upgradeChildrenFromAbsoluteToRelative(LWContainer container) {
        if (DEBUG.Enabled) System.out.println("CONVERTING TO RELATIVE in " + this + "; container: " + container);
        for (LWComponent c : container.getChildren()) {
            if (c instanceof LWLink)
                continue;
            // todo??? use getX/Y or getMapX/Y ? if the component and container are UNCOVERTED ALREADY,
            // then getX/Y is fine, as these are already in the old absolute form -- but if, e.g.,
            // the container were already converted, we need to use getMapX/Y -- check the order
            // of operations here...
            c.takeLocation(c.getX() - container.getX(),
                           c.getY() - container.getY());
        }
    }

    private final class NoNullsArrayList extends ArrayList<LWComponent> {
        NoNullsArrayList() {
            super(Math.max(numChildren(),10));
        }

        @Override
        public boolean add(LWComponent c) {
            if (c == null) {
                Util.printStackTrace("null not allowed in map descendents");
            } else {
                super.add(c);
            }
            return true;
        }
    }

    
    /**
     * A map will always have at least one layer, and may have any number of layers.
     * All layers within the map share exactly the same coordinate space -- this is an
     * inherent property of the layer implementation. Beyond that, how layers appear is
     * determined by the LWMap and the view controller (e.g., a MapViewer).  That said,
     * generally speaking they're intended to be drawn visually one on top of another.
     * "Upper" layers appear on top, and will be picked first.  Hiding a layer hides all
     * objects in the layer, and locking a layer locks all objects in a layer.
     */
    public static final class Layer extends LWContainer {

        /** for persistance only */
        public Layer() {
            initLayer();
            setFillColor(null);
        }

        public Layer(String name) {
            initLayer();
            setFillColor(null);
            setLabel(name);
        }

        private void initLayer() {
            // style properties not used on layers (pretty much no properties at all actually)
            // we disable them mainly to prevent warnings on layers with invalid values
            disablePropertyTypes(KeyType.STYLE);
        }

        @Override public float getX() { return 0; }
        @Override public float getY() { return 0; }
        @Override public float getMapX() { return 0; }
        @Override public float getMapY() { return 0; }
        @Override protected double getMapXPrecise() { return 0; }
        @Override protected double getMapYPrecise() { return 0; }

        //@Override public double getMapScale() { return getParent().getMapScale(); } // fully accurate, in case of maps in maps impl
        @Override public double getMapScale() { return 1; } // currently always true: for performance

        @Override protected AffineTransform transformDownA(final AffineTransform a) { return a; }
        @Override protected void transformDownG(final Graphics2D g) {}
        @Override public void transformZero(final Graphics2D g) {}

        @Override protected Rectangle2D transformMapToZeroRect(Rectangle2D mapRect) { return mapRect; }

        @Override public String getXMLfillColor() { return null; }
        @Override public String getXMLtextColor() { return null; }
        @Override public String getXMLstrokeColor() { return null; }
        @Override public String getXMLfont() { return null; }
        
        @Override public java.awt.Color getRenderFillColor(DrawContext dc) {
            return parent.getRenderFillColor(dc);
        }

        @Override public boolean isCollapsed() { return false; }
        @Override public boolean isAncestorCollapsed() { return false; }


        /** @return true */
        @Override public boolean isTopLevel() { return true; }
        
        /** @return this */
        @Override public Layer getLayer() { return this; }
        
        /** @return null */
        @Override public Layer getPersistLayer() {return null; }

        @Override public boolean isFiltered() {
            return false;
        }
        
        /** currently a no-op: layers not allowed to be filtered */
        @Override public void setFiltered(boolean filtered) {
            //super.setFiltered(filtered);
            //Util.printStackTrace("FILTERED=" + filtered + "; " + this);
            // if we re-enable this, we'll need to fix pathway exlusive display, which was
            // marking layers as filtered, and leaving them that way even after turning
            // that mode off, so the layers would be visible, but contents unselectable.
            if (DEBUG.Enabled) Log.debug("layers ignore filtered; (request=" + filtered + ") " + this);
        }

        @Override
        protected void setParent(LWContainer p) {
            if (p instanceof LWMap) {
                super.setParent(p);
            } else {
                //throw new IllegalArgumentException("Layers can only be parented to map; attempted to add " + this + " to " + p);
                Util.printStackTrace("Layers can only be parented to map; attempted to add " + this + " to " + p);
            }
                
        }

        /** @return true -- to support our UI impl that keeps listening JComponents around */
        @Override
        protected boolean permitZombieEvent(LWCEvent e) {
            return true;
        }
        
        /** do nothing: when deleting, layers need to keep listeners active for our UI impl */
        @Override
        public void removeAllLWCListeners() {}


        /** @return false */
        @Override
        public boolean supportsMultiSelection() { return false; }

        /** @return false -- is editable, but not on the map */
        @Override
        public boolean supportsUserLabel() { return false; }

        /** @return false */
        @Override
        protected boolean selectedOrParent() {
            // even if a layer is selected, it's not part of the a "normal" hierarchical selection
            return false;
        }
        
        //@Override public edu.tufts.vue.metadata.MetadataList getMetadataList() { return null; }

        @Override
        public String getComponentTypeLabel() { return "Layer"; }

//         @Override
//         protected boolean intersectsImpl(Rectangle2D mapRect) {
//             // probably better to change LWComponent.requiresPaint
//             // such that there another API call to check along w/clipOptimized
//             // so we can skip making the intersects call entirely
//             // TODO: we can optimize this by having layers track their bounds,
//             // which they currently don't
//             return true;
//         }

        @Override
        protected boolean intersectsImpl(Rectangle2D mapRect) {
            // must always return FALSE otherwise can be picked via a region pick
            return false;
        }
        @Override
        public boolean requiresPaint(DrawContext dc)
        {
            // this is overkill for now: layers don't track their own bounds
            return isVisible();
        }
        

        @Override
        protected Rectangle2D.Float getZeroBounds() {
            Util.printStackTrace(this + "; Layer getZeroBounds; always empty");
            return EmptyBounds;
        }

        private void add(LWComponent c) {
            if (mChildren == NO_CHILDREN)
                mChildren = new ArrayList();
            mChildren.add(c);
        }

        @Override
        protected void setAsChildAndLocalize(LWComponent c) {
            if (c.getParent() instanceof Layer) {
                // skip localization -- un-needed as layers share
                // same coordinate space
                if (DEBUG.Enabled && getMap() != null && c.getMap() != getMap())
                    Util.printStackTrace("cross-map reparenting! " + c.getMap() + " -> " + getMap());
                c.setParent(this);
            } else {
                super.setAsChildAndLocalize(c);
            }
        }
        
        
        /** @return null to ensure will never be treated as styleable */
        @Override
        public Object getTypeToken() { return null; }
        
        // Commenting out drawImpl/drawChild overrides turns off the fading out of
        // locked layers [VUE-1429]
        
//         @Override
//         protected void drawImpl(DrawContext dc) {
//             super.drawImpl(dc);
            
//             if (isLocked() && dc.focal != this && getParent().isOnBottom(this)) {
                
//                 // this a better (easier to read) and faster method of fading out a
//                 // layer, but it only works on the bottom layer.  To make work on other
//                 // layers, we'd have to render the layer offscreen first then fade out
//                 // the results, which would be very slow.  Also, this has a bug in that
//                 // we're filling the clipRect, which can actually be larger an area than
//                 // we'd ideally like to fade out (e.g., see what happens in the
//                 // MapPanner).

//                 // The problem case is if there are any layers BELOW this one that
//                 // are not locked/faded out, they'd be faded out anyway by any
//                 // locked layers above them.
                
//                 dc.setAlpha(0.5);
//                 dc.g.setColor(getMap().getFillColor());
//                 dc.g.fill(dc.g.getClipRect());
//             }
//         }
        

//         @Override
//         protected void drawChild(LWComponent child, DrawContext dc)
//         {
//             if (isLocked() && !getParent().isOnBottom(this))
//                 dc.setAlpha(0.5);
//             super.drawChild(child, dc);
//         }
        
        /** @return null -- layer contents not persisted with layer for backward compat with old versions of VUE */
        @Override public List<LWComponent> getXMLChildList() { return null; }

        @Override
        public Layer duplicate(CopyContext cc)
        {
            LWComponent c = super.duplicate(cc);
            if (hasLabel())
                c.setLabel(getLabel() + " Copy");
            return (Layer) c;
        }
        
        @Override
        public String toString() {
            try {
                return String.format("Layer[%s<%d> \"%s\" %2d%s]",
                                     getParent().getDisplayLabel(),
                                     getParent().indexOf(this),
                                     getDisplayLabel(),
                                     numChildren(),
                                     describeBits()
                                     );
            } catch (Throwable t) {
                return super.toString();
            }
        }
    }

    public LWContainer getActiveContainer() {
        return mActiveLayer == null ? this : mActiveLayer;
    }
    
    public Layer getActiveLayer() {
        return mActiveLayer;
    }
    public Layer addLayer(String name) {
        Layer layer = new Layer(name);
        addChild(layer);
        return layer;
    }
    
    /** @return the internal layer of the given name.  Internal layers start at the back, intially locked */
    public Layer getInternalLayer(String name) {

        Layer layer = findLayer(name);
        if (layer == null) {
            layer = new Layer(name);
            //layer.setLocked(true); 
            layer.setFlag(Flag.INTERNAL);
            addChild(layer);
            sendToBack(layer);
        }
        return layer;
    }

    public Layer getOrCreateLayer(String name) {
        Layer layer = findLayer(name);
        if (layer == null) {
            layer = new Layer(name);
            addChild(layer);
        }
        return layer;
    }
    
    /** @return Layer if one by the given name is found */
    public Layer findLayer(String name) {
        for (Layer layer : Util.typeFilter(getChildren(), Layer.class)) {
            if (name.equals(layer.getLabel())) {
                return layer;
            }
        }
        return null;
    }

    public void setActiveLayer(LWComponent layer) {
        if (DEBUG.PARENTING) Log.debug("setActiveLayer: " + layer);
        // Log.debug("setActiveLayer: " + layer, new Throwable("FYI"));
        if (layer == null || layer instanceof Layer)
            mActiveLayer = (Layer) layer;
        else
            Util.printStackTrace("not a layer: " + Util.tags(layer));
    }

    @Override
    public java.util.List<LWComponent> getXMLChildList() {
        if (mXMLRestoreUnderway) {
            return super.getXMLChildList();
        } else {
            // Layer handling
            if (hasChildren()) {
                List childrenInAllLayers = new ArrayList();
                //childrenInAllLayers.addAll(mChildren);
                for (LWComponent c : getChildren()) {
                    if (c instanceof Layer) {
                        childrenInAllLayers.addAll(c.getChildren());
                    } else {
                        Log.warn("getXMLChildList: non-layer direct child of map; " + this + ": " + c);
                        childrenInAllLayers.add(c);
                    }
                }
                Log.debug("getXMLChildList: built integrated list of all layer members, n=" + childrenInAllLayers.size());
                return childrenInAllLayers;
            } else
                return null;
        }
    }

    public java.util.List<? extends LWComponent> getXMLLayers() {
        if (mXMLRestoreUnderway) {
            return mLayers;
        } else { // Layer handling
            if (Util.containsOnly(mChildren, Layer.class)) {
                // this should always be the case unless of model up-leakage
                return mChildren;
            } else {
                // just in case: never let anything that isn't a layer at the
                // top level be accidentally persisted as a layer
                Log.warn("getXMLLayers: "
                         + (mChildren.size() - Util.countTypes(mChildren, Layer.class))
                         + " non-layer children were ignored in producing the layers-list");
                return Util.extractType(mChildren, Layer.class);
            }
        } // else return null;
    }

    private boolean reparentAllToLayers() {

        boolean addedLayers = false;

        if (mLayers.size() > 0) {

            if (DEBUG.Enabled) Log.debug("restoring: found existing layers in " + this);

            for (Layer layer : mLayers) {
                for (LWComponent c : mChildren) {
                    if (c.getParent() == layer)
                        layer.add(c);
                }
            }

            // We should never have orphans, but just in case / for debug while testing this:
            List orphans = new ArrayList();
            for (LWComponent c : mChildren) {
                if (c.getParent() instanceof Layer) {
                    // what we want
                } else {
                    orphans.add(c);
                    Log.error("Layer orphaned node: " + c);
                }
            }
                    
            mChildren.clear();
            mChildren.addAll(orphans);

        } else {

            Log.debug("restoring: creating default layers in " + this);

            installDefaultLayers();
            addedLayers = true;

        }

        //Log.debug("CHILDREN: " + Util.tags(mChildren));
        //Log.debug("  LAYERS: " + Util.tags(mLayers));
        mChildren.addAll(mLayers);

        if (!addedLayers) {
            // findLayer won't work until the above mChildren.addAll
            final Layer defaultLayer = findLayer("Default");
            if (defaultLayer != null)
                setActiveLayer(defaultLayer);
            else if (mLayers.size() > 1)
                setActiveLayer(mLayers.get(1));
            else
                setActiveLayer(mLayers.get(0));

        }

        // mLayers is only needed during restore
        mLayers = null;
        //isLayered = true;

        return addedLayers;
    }

    private void installDefaultLayers() {

        // TODO: need to handle persistance -- could match via a special name, for maybe persistIsStyle
//         mInternalLayer = new Layer("*Internal*");
//         mInternalLayer.setVisible(false);
//         mInternalLayer.setFlag(Flag.INTERNAL);
//         mInternalLayer.setParent(this);

        final Layer activeLayer;
        
        if (true) {

            final Layer layer0;
            
            layer0 = new Layer("Layer 1");
            layer0.setParent(this);
            layer0.mChildren = this.mChildren;
        
            for (LWComponent c : this.mChildren)
                c.setParent(layer0);
        
            this.mChildren = new ArrayList();
            this.mChildren.add(layer0);

            activeLayer = layer0;
            
        } else {

            // old style three inital layers
            
            final Layer layer0, layer1, layer2;
        
            layer0 = new Layer("Background");
            layer0.setParent(this);
            //layer0.setVisible(false);
        
            layer1 = new Layer("Default");
            layer1.setParent(this);
            layer1.mChildren = LWMap.this.mChildren;
            for (LWComponent c : LWMap.this.mChildren)
                c.setParent(layer1);
        
            layer2 = new Layer("Notations");
            layer2.setParent(this);
            //layer2.setVisible(false);
        
            LWMap.this.mChildren = new ArrayList();
            //mChildren.add(mInternalLayer);
            mChildren.add(layer0);
            mChildren.add(layer1);
            mChildren.add(layer2);

            activeLayer = layer1;
        }


        //isLayered = true;
        setActiveLayer(activeLayer);
    }
    
    
    static final String NODE_INIT_LAYOUT = "INIT_NODE_LAYOUT";
    static final String LINK_INIT_LAYOUT = "INIT_LINK_LAYOUT";
    private static final String INIT_LAYOUT = "<validating-layout>";

    /** for debug when this is called on an LWMap only */
    @Override public void layoutAll(Object trigger) {
        Log.info("layoutAll(" + Util.tags(trigger) + ") " + this);
        super.layoutAll(trigger);
    }

    /** to be called on maps that are manually created (e.g., not deserialized) before they're displayed */
    public void layoutAndValidateNewMap() {
        layoutAll(INIT_LAYOUT);
        // final Collection<LWComponent> all = getAllDescendents(); // todo: probably should do as order-depth
        // layoutAll(all, INIT_LAYOUT); // will be auto-validated due to initial layout trigger
        // //validateAll(all);
    }

    // private void validateAll(Collection<LWComponent> components) {
    // for (LWComponent c : components) {
    // c.validateInitialValues();
    // }
    // }

    /** note side effect: will clear all mXMLRestoreUnderway flags that are set
     *  This processes all NODES first, then all LINKS.  Then normalizes all groups just to be safe.
     * The components should be provided in depth-first order (Order.DEPTH), so that
     * children are fully laid out before their parents, which will need the proper size of
     * the children to lay out correctly.
     */
    private void layoutAllAfterRestore(final Collection<LWComponent> components, final Object key)
    {
        Object layoutKey;

        if (key == INIT_LAYOUT)
            layoutKey = NODE_INIT_LAYOUT;
        else
            layoutKey = key;
        
        //-----------------------------------------------------------------------------
        // First, we layout all NON links, so we can layout the links afterwords, and when
        // the links recompute, they'll be able to know for certian the borders of what
        // they're connected to.  Note that this means that the layout of a container
        // should not depend on a link be current yet.  Groups depend on knowing the size
        // of their children, but that's not handled via the layout code -- that's handled
        // via group normalization.
        //-----------------------------------------------------------------------------

        for (LWComponent c : components) {
            // mark all, including links, now, as when we get to them, links-to-links may
            // cause cascading recomputes that would warn us they're still being restored otherwise.
            c.mXMLRestoreUnderway = false;
            if (c instanceof LWLink)
                continue;
            if (DEBUG.LAYOUT||DEBUG.INIT) out("LAYOUT NODE: in " +  c.getParent() + ": " + c);
            try {
                c.layout(layoutKey);
            } catch (Throwable t) {
                Log.warn("LAYOUT-NODE/" + layoutKey + ": " + c, t);
            }
        }

        if (key == INIT_LAYOUT)
            layoutKey = LINK_INIT_LAYOUT;
        else
            layoutKey = key;
        
        //-----------------------------------------------------------------------------
        // Layout links -- will trigger recomputes & layout any link-labels that need it.
        //-----------------------------------------------------------------------------
        
        //if (!tufts.vue.action.SaveAction.PACKAGE_DEBUG) // tmp hack
            
        for (LWComponent c : components) {
            if (c instanceof LWLink == false)
                continue;
            if (DEBUG.LAYOUT||DEBUG.INIT) out("LAYOUT LINK: in " +  c.getParent() + ": " + c);
            try {
                c.layout(layoutKey);
            } catch (Throwable t) {
                Log.warn("LAYOUT-LINK/" + layoutKey + ": " + c, t);
            }
        }
        
        //-----------------------------------------------------------------------------
        // Just to be sure, re-normalize all groups.  This shouldn't be required, except
        // perhaps if we're updating from an old model version.
        //-----------------------------------------------------------------------------
        
        for (LWComponent c : components) {
            try {
                if (c instanceof LWGroup)
                    ((LWGroup)c).normalize();
            } catch (Throwable t) {
                Log.warn("GROUP-NORMALIZE/"   + key + ": " + c, t);
            }
        }
    }

    public void completeXMLRestore(Object context)
    {
        if (DEBUG.INIT || DEBUG.IO || DEBUG.XML)
            Log.debug(getLabel() + ": completing restore...");

        if (mChildren == NO_CHILDREN || mChildren == Collections.EMPTY_LIST || mChildren == null) {
            // If there was NO content in the map, we need to make sure we manually set
            // the child list to a real list before we do anything else.  Note
            // that the only case we should have to check is NO_CHILDREN, but we
            // check for other problems just in case.  This fixes VUE-1463.
            mChildren = new ArrayList();
        }

        //-----------------------------------------------------------------------------
        // We do this every time, as nodes are always saved as children
        // of the map, so that old versions of VUE can at least
        // still see the flattened content.
        
        final boolean addedLayers = reparentAllToLayers();

        //-----------------------------------------------------------------------------
        
        if (mPathways != null) {
            try {
                mPathways.completeXMLRestore(this);
            } catch (Throwable t) {
                tufts.Util.printStackTrace(new Throwable(t), "PATHWAYS RESTORE");
            }
        }
        // Need to do this after complete XML restore, as getAllDescendents for special
        // componenets may otherwise not yet be ready to return everything (e.g. MasterSlide)
        final Collection<LWComponent> allRestored = getAllDescendents(ChildKind.ANY,
                                                                      new NoNullsArrayList(),
                                                                      //new ArrayList(Math.max(numChildren(),10)),
                                                                      Order.DEPTH);

        //resolvePersistedLinks(allRestored);
        
        if (mPathways == null)
            mPathways = new LWPathwayList(this);

        mNextID.set(findGreatestID(allRestored) + 1);

        if (addedLayers) {
            // if we created any layers, ensure their ID's now, after
            // we already know the greatest ID.
            for (LWComponent c : getChildren())
                c.ensureID(c, false);
        }

        
        for (LWPathway pathway : mPathways) {
            // 2006-11-30 14:33.32 SMF: LWPathways now have ID's,
            // but they didn't used to, so make sure they
            // have an ID on restore in case it was a save file prior
            // to 11/30/06.
            ensureID(pathway);
        }

        //----------------------------------------------------------------------------------------
        // Now update the model to the most recent data version
        //----------------------------------------------------------------------------------------

        if (getModelVersion() < getCurrentModelVersion()) {

            // note that these are one-way upgrades that can only be run on old data model:
            // running them on newer data models will corrupt the location information.
            
            if (isGroupAbsolute(getModelVersion()))
                upgradeAbsoluteToRelativeCoords(getChildren());

            if (getCurrentModelVersion() >= 4 && getModelVersion() < 4)
                upgradeLinksToParentRelative(allRestored);

            Log.info(this + " Updated from model version " + getModelVersion() + " to " + getCurrentModelVersion());
            mModelVersion = getCurrentModelVersion();
        }

        final Collection allResources = getAllResources();

        // Note: by this time, some duplicate resources have been removed from the map/
        // E.g., "image" nodes, where the LWNode and the the LWImage both point to the
        // same resource spec, the reference to the instance for the de-serialized
        // LWNode is pointed to the instance in LWImage.  See LWNode.setResource.  This
        // simplifies things, improves performance, and makes debugging easier.
        // Someday, we may change castor persistance to use Resource references in the
        // LWComponents, and then persist a separate list of all Resources with the map,
        // but for now multiple references to the same Resource object are re-persisted
        // each time in the map.
        
        if (isArchiveMap()) {

            // Archive maps don't currently look for relative resources: just run all final inits
            //runResourceDeserializeInits(allResources); is run manually by Archive
            runResourceFinalInits(allResources);
            
        } else {

            // DEFAULT: LOOK FOR MAP-RELATIVE RESOURCES (files in same directory as the map, or below it)

            // (1) First: load the property maps to we can find @file.relative properties:

            runResourceDeserializeInits(allResources);
            
            // (2) Now, patch up old absolute resource locations that were relative to the map
            // to the new absolute locations:

            if (mSaveLocationURI == null) {
                Log.info("unrooted map (no setFile) -- skipping search for relative resources; " + this);
            } else {
                
                // todo: we should be able to get rid of mSaveLocationURI and use mFile
                // (and canonicalize it like we do for recordRelativeLocations)
                // setFile should already have been called as per MapUnmarshalHandler,
                // unless this is an archive map, in which case we could be here.
                
                restoreRelativeLocations(allResources, mSaveLocationURI);
            }

            // (3) Then run final inits:
            
            runResourceFinalInits(allResources);

// Now, we could look for any NEW relative's that weren't recorded before, tho
// we shouldn't touch any existing relatives:  Need as an update/second pass
// tho so we don't touch any we've already determined are relative as they were.
//             if (getFile() != null)
//                 recordRelativeLocations(getAllResources(), getFile().getParentFile());
            
        }

        mResourceFactory.loadResources(allResources);

        Schema.restoreSavedMapSchemas(this, mRestoredSchemas, allRestored);

        // for now, any restored may is assumed to have already done an auto-cluster
        setState(State.HAS_AUTO_CLUSTERED);

        //----------------------------------------------------------------------------------------
        // Now lay everything out.  allRestored should be in depth-first order for maximum
        // reliability (the deepest items should lay themselves out first, so parent items
        // that need to know the size of their children will get accurate results).
        //----------------------------------------------------------------------------------------
        
        // Do NOT normalize the groups yet: will seriously break old maps.  It slighly improves some of our
        // interim formats (1-2), but makes others a complete mess.
        //         for (LWComponent c : allRestored)
        //                 if (c instanceof LWGroup)
        //                     ((LWGroup)c).normalize();

        
        // tmp hack: we were geting exceptions when testing just SaveAction on this code?
        //if (!tufts.vue.action.SaveAction.PACKAGE_DEBUG)

        layoutAllAfterRestore(allRestored, INIT_LAYOUT);

        if (DEBUG.INIT || DEBUG.IO || DEBUG.XML) Log.debug("RESTORE COMPLETED; nextID=" + mNextID.get());
        
        mXMLRestoreUnderway = false;
        markAsSaved();
    }

    class ResourceFactory extends Resource.DefaultFactory {

        private final Map<String,Resource> resourceMap = new java.util.concurrent.ConcurrentHashMap();

        void loadResources(Collection<Resource> resourceBag) {
            for (Resource r : resourceBag)
                track(r);
        }

        private void track(Resource r) {
            final Resource already = resourceMap.put(r.getSpec(), r);
            if (already != null) {
                // this okay for the moment: we're only using this for keeping
                // image data up to date
                if (DEBUG.WORK) Log.debug("duplicate tossed: " + already);
                if (DEBUG.WORK) Log.debug("         : " + r);
            }
        }

        private Resource trackNew(Resource r)
        {
            final Resource already = resourceMap.get(r.getSpec());

            // This is not currently a very efficient way to do this --
            // override the create methods to first check for a cache
            // member before going through all this.
            
            if (already != null) {
                if (DEBUG.Enabled) {
                    Log.debug("tossing: " + r);
                    Log.debug("reusing: " + already);
                }
                return already;
            } else {
                track(r);
                if (DEBUG.Enabled) Log.debug("keeping: " + r);
                return r;
            }
        }
        
        @Override
        protected Resource postProcess(Resource r, Object source) {
            Log.info("created: " + r + " from " + Util.tags(source));

            final Resource using = trackNew(r);

            // Of course, do NOT want to call dataHasChanged here, as it will use-up the
            // update.  Since newly dropped objects get auto-selected, our auto-update
            // code should automatically run for now -- when dropping single objects
            // that is: multiple drops will fail to trigger an update, as there won't be
            // a single-selection, so that's why this wants to be refactored further,
            // and have the update code currently in VUE.java (update of all LWImages)
            // triggerable from here.

            // Also, we can probably add to the code to the factory for ensuring that
            // any new resource added to any map element, is duplicated if it's in any
            // other map -- may need to have the Resource object itself point back to
            // it's map.  Tho as long as doing that, might as well just make it an
            // "owner", that could be an LWComponent, or null if a resource not attached
            // to a map (My Computer browser, etc), or keyed to a special owner, or for
            // search results (could point back to repository if that's helpful).
            
            // if (using != r && using.dataHasChanged()) Log.info("DO AN UPDATE");
            
            return using;
        }

// not turned on yet -- see if can move to Resource.java if we keep.
//             if (mSaveLocationURI != null) {
//                 //URI curRoot = URLResource.makeURI(mSaveLocation.getParentFile());
//                 //if (curRoot != null) {
//                     r.updateRootLocation(mSaveLocationURI, null);
//                     //}
//             }

        private void recordInode(Resource r, Object source) {
            
            if (Util.isMacPlatform() && (source instanceof java.io.File || source instanceof String)) {
                
                String inode = Util.getSystemCommandOutput(new String[] { "/usr/bin/stat", "-f", "%i", ""+source },
                                                           getSaveLocation());
                
                if (inode != null)
                    r.setHiddenProperty("file.MacOSX.inode", inode);
            }
        }

    }

 
    private final ResourceFactory mResourceFactory = new ResourceFactory();
    
    @Override
    public Resource.Factory getResourceFactory() {
        return mResourceFactory;
    }
    
    /**
     * Perform any actions on the map we want to happen just before it is persisted to the given file.
     * E.g., record the map-relative location of any local file resources.
     */
    public void makeReadyForSaving(File file) {

        if (file == null) {
            Log.debug("makeReadyForSaving: null file, must be archive");
            return;
        }
        
        Log.debug("makeReadyForSaving to " + file);
        
//         if (file == null) {
//             Util.printStackTrace("makeReadyForSaving: no file");
//             return;
//         }

        recordRelativeLocations(getAllResources(), file.getParentFile());

    }

    private Collection<Schema> findAllSchemas() {
        final Set<Schema> allSchemas = new HashSet();

        for (LWComponent c : getAllDescendents()) {
            Schema s = c.getDataSchema();
            if (s != null)
                allSchemas.add(s);
        }

        return allSchemas;
    }

    @Override
    public Collection<Schema> getIncludedSchemas() {
        if (mXMLRestoreUnderway)
            return mRestoredSchemas;
        else
            return findAllSchemas();
    }

//     // TODO: actually, put these right in the Schema?  Or redundantly right in the Field???
//     public Collection<tufts.vue.ds.Association> getIncludedAssociations() {
//         if (mXMLRestoreUnderway)
//             return null;
//         else
//             return tufts.vue.ds.Association.getAll();
//     }
    
    
//     public void setIncludedSchemas(Collection<Schema> schemas) {
//         Log.debug("FYI, PERSISTED SCHEMA HANDLES WERE: " + schemas, new Throwable("FYI"));
//     }

    private void recordRelativeLocations(Collection<Resource> resources, File mapSaveDirectory)
    {
        final URI root = Resource.toCanonicalFile(mapSaveDirectory).toURI();
        
        if (DEBUG.Enabled) Log.debug("relativizing any resources local to: " + root);
        
        for (Resource r : resources) {
            try {
                r.recordRelativeTo(root);
            } catch (Throwable t) {
                Log.warn(this + "; recordRelativeLocations failure " + root + ": " + t + "; " + r, t);
            }
        }
    }

    private void restoreRelativeLocations(Collection<Resource> resources, URI root)
    {
        if (DEBUG.IO || DEBUG.INIT || DEBUG.RESOURCE) {
            Resource.dumpURI(root, Util.TERM_GREEN + "resolving resources to map root;");
            System.out.print(Util.TERM_CLEAR);
        }
        
        for (Resource r : resources) {
            try {
                r.restoreRelativeTo(root);
            } catch (Throwable t) {
                Log.warn(this + "; restoreRelativeLocations failure " + root + ": " + t + "; " + r, t);
            }
        }
    }

    // public only for Archive to be able to call us: clean that up
    public void runResourceDeserializeInits(Collection<Resource> resources)
    {
        if (DEBUG.RESOURCE || DEBUG.IO)
            Log.debug(Util.TERM_CYAN + "initAfterDerserialize for all resources; " + Util.tags(resources) + Util.TERM_CLEAR);
        for (Resource r : resources) {
            try {
                r.initAfterDeserialize(this);
            } catch (Throwable t) {
                Log.warn(this + "; failure on: " + r, t);
            }
        }
        
    }

    private void runResourceFinalInits(Collection<Resource> resources)
    {
        if (DEBUG.Enabled)
            Log.debug(Util.TERM_CYAN + "initFinal's for all resources; " + Util.tags(resources) + Util.TERM_CLEAR);
        
        for (Resource r : resources) {
            try {
                r.initFinal(this);
            } catch (Throwable t) {
                Log.warn(this + "; failure on: " + r, t);
            }
        }
        
    }
    
    

//     private void relativizeResources(Collection<LWComponent> nodes, URI root) {
        
//         for (LWComponent c : nodes) {
//             if (!c.hasResource())
//                 continue;
//             try {
//                 c.getResource().makeRelativeTo(root);
//             } catch (Throwable t) {
//                 Log.warn(this + "; relativize failure: " + t + "; " + c.getResource(), t);
//                 //t.printStackTrace();
//             }
//         }
        
//     }
    
    
//     private void ensureAllResourcesFoundAndRelative(Collection<LWComponent> nodes, File oldMapLocation)
//     {
//         final File oldParentDirectory = oldMapLocation.getParentFile();
//         final File newParentDirectory = mFile.getParentFile();

//         if (oldParentDirectory == null) {
//             Util.printStackTrace("Unable to find parent of " + oldMapLocation + "; can't relativize local resources.");
//             return;
//         }

//         Log.info("    SAVED MAP FILE: " + oldMapLocation);
//         Log.info("SAVED MAP LOCATION: " + oldParentDirectory);
        
//         //final URI oldRoot = oldParentDirectory.toURI();
//         final URI oldRoot = URLResource.makeURI(oldParentDirectory);
//         final URI newRoot;

//         if (oldRoot == null) {
//             Log.error(this + "; unable to parse old parent directory: " + oldParentDirectory);
//             return;
//         }

//         Resource.dumpURI(oldRoot, "ROOT SAVED");
        
//         if (oldParentDirectory.equals(newParentDirectory)) {
//             System.err.println("ROOT NEW URI: (same)");
//             newRoot = null;
//         } else {
//             newRoot = newParentDirectory.toURI();
//             Resource.dumpURI(newRoot, "ROOT NEW OPENED");
//         }


//         // Normalize resources
//         for (LWComponent c : nodes) {
//             if (!c.hasResource())
//                 continue;
//             try {
//                 //Log.info(this + "; relativize: " + c.getResource());
//                 c.getResource().updateRootLocation(oldRoot, newRoot);
//             } catch (Throwable t) {
//                 Log.warn(this + "; relativiztion: " + t + "; " + c.getResource());
//             }
//         }
//     }
        
    

    LWComponent findByID(Collection<LWComponent> allRestored, String id) {
        for (LWComponent c : allRestored)
            if (id.equals(c.getID()))
                return c;
        tufts.Util.printStackTrace("Failed to child child with id [" + id + "]");
        return null;
    }
    

    /** for use during restore */
    private int findGreatestID(final Collection<LWComponent> allRestored)
    {
        LWComponent mostRecent = findMostRecentlyCreated(allRestored, null);

        if (mostRecent != null)
            return mostRecent.getNumericID();
        else
            return -1;
    }

    public LWComponent findMostRecentlyCreated(final Collection<LWComponent> components, Object typeToken)
    {
        int maxID = -1;
        LWComponent mostRecent = null;

        for (LWComponent c : components) {
            if (c.getID() == null) {
                if (!(c instanceof LWPathway)) {
                    Log.warn("found a child persisted without an id: " + Util.tags(c));
                }
                continue;
            }
            if (typeToken != null && c.getTypeToken() != typeToken)
                continue;
            int curID = c.getNumericID();
            if (curID > maxID) {
                maxID = curID;
                mostRecent = c;
            }
            
        }
        return mostRecent;
    }

    public LWComponent findMostRecentlyCreated() {
        return findMostRecentlyCreated(getAllDescendents(ChildKind.ANY), null);
    }
    
    public LWComponent findMostRecentlyCreatedType(Object typeToken) {
        return findMostRecentlyCreated(getAllDescendents(ChildKind.ANY), typeToken);
    }


    /**
     * @return the list of children
     */
    // overridden for performance
    @Override
    public final java.util.List<LWComponent> getPickList(PickContext pc, List<LWComponent> stored) {
        return getChildren();
    }

    // do nothing
    //void setScale(float scale) { }

    @Override
    public void draw(DrawContext dc) {
        if (DEBUG.SCROLL || DEBUG.CONTAINMENT) {
            dc.g.setColor(java.awt.Color.green);
            dc.setAbsoluteStroke(1);
            dc.g.draw(getBounds());
        }
        
        /*
        if (!dc.isInteractive()) {
            out("FILLING with " + getFillColor() + " " + dc.g.getClipBounds());
            //tufts.Util.printStackTrace();
            dc.g.setColor(getFillColor());
            dc.g.fill(dc.g.getClipBounds());
        }
        */

        /*
         * Draw all the children of the map.
         *
         * Note that when the map draws, it does NOT fill the background,
         * as the background color of the map is usually a special case
         * property used to completely fill the background of an underlying
         * GUI component or an image.
         */
        
        // We don't draw the pathways on top if we're zoomed in: otherwise
        // they may obscure a pseudo-focal (e.g., a slide)
        
        if (dc.zoom > PathwayOnTopZoomThreshold || dc.isPresenting()) { // VUE-1177
            drawPathways(dc);
            super.drawChildren(dc); // draw all layers
        } else {
            super.drawChildren(dc); // draw all layers
            drawPathways(dc);
        }

        
//         if (DEBUG.BOXES) {
//             dc.g.setColor(java.awt.Color.red);
//             dc.g.setStroke(STROKE_ONE);
//             for (LWComponent c : getAllDescendents()) {
//                 if (c.isDrawingSlideIcon())
//                     dc.g.draw(c.getMapSlideIconBounds());
//             }
//         }
        
    }

    private void drawPathways(DrawContext dc)
    {
                    
        if (mPathways != null && dc.drawPathways()) {
            
            LWPathway active = getActivePathway();
            
            for (LWPathway path : mPathways) {
                if (path.isDrawn()) {
                    if (path == active)
                        continue;
                    path.drawPathway(dc.create());
                        
                } else if (path == active)
                    active = null; // active isn't being drawn
            }

            // Draw the active one last (on top)
            
            // If these all have equal transparency, this shouldn't
            // actually make a visible difference, but if the active
            // should take on non-transparent value, it definitely
            // will.
            
            if (active != null) {
                final DrawContext pdc = dc.create();
                active.drawPathway(pdc);
                pdc.dispose();
            }
            
            
        }


        
//         if (mPathways != null && dc.drawPathways()) {
//             int pathIndex = 0;
//             for (LWPathway path : mPathways) {
//                 if (path.isDrawn()) {
//                     dc.setIndex(pathIndex++);
//                     path.drawPathway(dc.create());
//                 }
//             }
//         }
    }

    //for peristance
    public String getPresentationBackground()
    {
    	return mPresentationColor.asString();
    }
    //for persistance
    public void setPresentationBackground(String c)
    {
    	mPresentationColor.setFromString(c);
    }
    public java.awt.Color getPresentationBackgroundValue()
    {
    	return mPresentationColor.get();
    }
    
    public void setPresentationBackgroundValue(java.awt.Color c)
    {
    	mPresentationColor.set(c);
    }
    
    public java.awt.image.BufferedImage createImage(double alpha, java.awt.Dimension maxSize, java.awt.Color fillColor, double mapZoom) {
        return super.createImage(alpha, maxSize, fillColor == null ? getFillColor() : fillColor, mapZoom);
    }

//     /** Override default image getter to double the scale on the rendered map */
//     @Override
//     public java.awt.image.BufferedImage getAsImage() {
//         return getAsImage(OPAQUE, null, 2.0);
//     }
// Actually, as dragged images produce highest-res RAW image data (converted to TIFF if, e.g., dropped
// into the Apple Mail application), we don't really need to double-up the resolution here.  (And
// doing so can produce HUGE 20MB+ tiff attachments).  [Addendum: not all apps will create a tiff version tho: e.g., Skitch]
    
    

    
    
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
    
    private double tempZoom = 0;
    private Point2D.Float tempOrigin = null;
    private Rectangle2D tempBounds = null;
    
    public void setTempBounds(Rectangle2D bounds)
    {
    	tempBounds = bounds;
    }
    
    public Rectangle2D getTempBounds()
    {
    	return tempBounds;
    }
    
    public void setTempZoom(double zoom){
    	this.tempZoom = zoom;
    }
    public double getTempZoom()
    {
    	return tempZoom;
    }
    
    public void setTempUserOrigin(Point2D.Float origin)
    {
    	this.tempOrigin = origin;
    }
    public Point2D.Float getTempUserOrigin()
    {
    	return tempOrigin;
    }
    /** for persistance */
    public double getUserZoom() {
        return this.userZoom;
    }
    
    @Override
    public LWMap getMap() {
        return this;
    }

    /** @return false: maps can't be selected with anything else */
    public boolean supportsMultiSelection() { return false; }
    

    /** @return false -- maps aren't moveable objects */
    @Override
    public boolean isMoveable() {
        return false;
    }
    
    /** @return Float.MAX_VALUE: map contains all points, but any contents take priority */
    @Override
    protected final float pickDistance(float x, float y, PickContext pc) {
        return Float.MAX_VALUE;
    }

    
    /** override of LWContainer: default hit component on the map
     * is nothing -- we just @return null.
     */
    @Override
    protected final LWComponent defaultPick(PickContext pc) {
        //return this; // allow picking of the map
        // OPTIMIZATION: if embed maps in maps, lose this override (and make LWComponent version final)
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
    @Override
    public boolean isOrphan() {
        return false;
    }
    
    /** deprecated */
    public LWNode addNode(LWNode c) {
        addChild(c);
        return c;
    }
    /** deprecated */
    public LWLink addLink(LWLink c) {
        addChild(c);
        return c;
    }
    
    public LWPathway addPathway(LWPathway p) {
        ensureID(p);
        getPathwayList().add(p);
        return p;
    }

    public LWPathway getActivePathway()
    {
        if (getPathwayList() != null)
            return getPathwayList().getActivePathway();
        else
            return null;
    }
    
    @Override
    public void addChildren(Collection<? extends LWComponent> children, Object context) {

        // This code is a backward-compat hack for other code that is attempting to
        // add children to the map.  It used to be you could just do this directly,
        // but now that we have layers, only a Layer should be a direct child of the
        // map, and we need to divert this to add call to the appropriate layer.
        
        if (children.size() == 1 && Util.getFirst(children) instanceof LWMap.Layer) {
            //isLayered = true;
            super.addChildren(children, context);
        } else if (/*isLayered() &&*/ mActiveLayer != null) {
            mActiveLayer.addChildren(children, context);
        } else {
            super.addChildren(children, context);
        }
    }
    
    @Override
    protected void addChildImpl(LWComponent c, Object context) {
        
        if (c instanceof LWPathway)
            throw new IllegalArgumentException("LWPathways not added as direct children of map: use addPathway " + c);
        
        if (c instanceof Layer == false && !mXMLRestoreUnderway) {
            if (mActiveLayer != null) {
                Log.warn("addChildImpl: forcing to active layer: " + mActiveLayer + " in " + this);
                mActiveLayer.addChildImpl(c, context);
            } else {
                Util.printStackTrace("LWMap adding non-layer: " + Util.tags(c));
            }
        }
        
        super.addChildImpl(c, context);

    }

    public LWComponent add(LWComponent c) {
        addChild(c);
        return c;
    }
    /*
    private void removeLWC(LWComponent c)
    {
        removeChild(c);
    }
     */

    /** @return true: maps are always "alive" -- they always generate events */
    protected final boolean alive() {
        return true;
    }
    
    /**
     * Every single event anywhere in the map will ultimately end up
     * calling this notifyLWCListners.
     */
    @Override
    protected void notifyLWCListeners(LWCEvent e) {
        if (mChangeSupport.eventsDisabled()) {
            if (DEBUG.EVENTS) System.out.println(e + " SKIPPING (events disabled)");
            return;
        }
        
        if (e.isUndoable())
            markChange(e);

        flushBounds(); // TODO: optimize: need a bounds event yet again
        super.notifyLWCListeners(e);

    }
    
    private void flushBounds() {
        mCachedBounds = null;
        if (DEBUG.EVENTS&&DEBUG.META) out(this + " flushed cached bounds");
    }
    
    private void markChange(LWCEvent e) {

        if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
            
            // for now, anything from a non AWT Event Dispatch Thread (EDT) is assumed to not be a
            // real undoable change -- this mainly to prevent image size sets after the map loads
            // from leaving the map appearing to have been modified.  A more complete solution
            // might mark all events generated on specific threads known to be behaving this way.

            // NOTE: VUE features that modify the map OFF the AWT thread will need to compensate
            // for this by manually marking the map as changed.  Better would be to only skip this
            // if the modification comes from an image load thread, which is why this was
            // implemented in the first place: to prevent freshly loaded maps with images to
            // immediately present as having already been modified.  This was much more of an issue
            // when we had a preference for image icon size, as changing this preference between
            // the saves of a particular map file would force all the image icons in the map to
            // "modify" themselves when the map was openened.  Now it's only an issue if image data
            // actually changes on disk.  And in fact, in that case, we may well want to report it
            // as modified...
            
            if (DEBUG.WORK || DEBUG.EVENTS || DEBUG.INIT) Log.debug("staying clean for non-AWT event: " + e);
            return;
        }
        
        if (mChanges == 0) {
            if (DEBUG.EVENTS) out(this + " First Modification Happening on " + e);
            if (DEBUG.INIT||DEBUG.UNDO||(DEBUG.EVENTS&&DEBUG.META)) {
                Log.debug("FYI: FIRST MODIFICATION", new Throwable("HERE"));
            }
        }
        
        mChanges++;
        mChangeState++;

        if (DEBUG.UNDO) {
            //String msg = "MARKED TO +" + mChanges + " WITH OLD VALUE: " + Util.tags(e.oldValue) + "; " + e;
            String msg = "MARKED TO +" + mChanges + " ON " + e;
            if (true||mXMLRestoreUnderway)
                Log.debug(msg);
            else
                Log.debug(msg, new Throwable("HERE"));
        }
    }

    @Override
    protected Rectangle2D.Float getZeroBounds() {
        //Util.printStackTrace("LWMap getZeroBounds " + this);
        Log.warn("LWMap getZeroBounds " + this);
        return getPaintBounds();
    }
    
    
//     @Override
//     public java.awt.geom.Rectangle2D.Float getBounds() {
//         return getBounds(Short.MAX_VALUE);
//     }
    
//    public java.awt.geom.Rectangle2D.Float getBounds(int maxLayer) {

    /**
     * Return the bounds of the entire map.  NOTE THAT THE RETURNED OBJECT SHOULD NOT BE MODIFIED.
     * This is called so often that we keep a single cached object with the current value.
     */
    @Override
    public Rectangle2D.Float getMapBounds() {

        // TODO: OPTIMIZE!  This is getting called EIGHT (8) times per event --
        // e.g. computing the entire bounds of the map 8 times per mouse drag when
        // moving a component around.  All of them seem to be coming from
        // adjustCanvasSize in MapViewer, which is being called every time we get an
        // event.  We may finally want that isBoundsEvent flag in LWComponent.Key,
        // either that, or have a special info-only bounds event delivered any time the
        // bounds change, so parties interested in bounds changes (MapViewer's,
        // LWGroup's) could pay attention to just that event.
        
        if (mCachedBounds == null) {
            //mCachedBounds = getBounds(getChildIterator(), maxLayer);
            //mCachedBounds = getPaintBounds(getChildIterator());
            mCachedBounds = getPaintBounds();
            takeSize(mCachedBounds.width, mCachedBounds.height);
            //takeLocation(mCachedBounds.x, mCachedBounds.y);
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
            //if (DEBUG.CONTAINMENT && DEBUG.META)
            if (DEBUG.CONTAINMENT && DEBUG.META)
                Log.debug("COMPUTED BOUNDS " + Util.fmt(mCachedBounds) + "; for " + this);

        }
        //setSize((float)bounds.getWidth(), (float)bounds.getHeight());
            //new Throwable("computedBounds").printStackTrace();
        return mCachedBounds;
    }

    /** this object is returned if a get*Bounds result had no contents / produced no actual bounds */
    public static final Rectangle2D.Float EmptyBounds = new Rectangle2D.Float();
    
//     private static final class Rect // extends Rectangle2D.Float
//     {
//         private final String type; // for debug
//         private Rectangle2D.Float rect;

//         Rect(String type) {
//             this.type = type;
//         }
        
//         void add(final Rectangle2D.Float r,
//                  final LWComponent debug)
//         {
//             try {
//                 if (Util.isBadRect(r)) {
//                     Log.error(String.format("%s%-17s %s%s for %s",
//                                             Util.TERM_RED,
//                                             "bad " + type + ": ",
//                                             Util.fmt(r),
//                                             Util.TERM_CLEAR,
//                                             debug));
//                 } else if (rect == null) {
//                     rect = new Rectangle2D.Float();
//                     rect.setRect(r);
//                 } else {
//                     rect.add(r);
//                 }
//             } catch (Throwable t) {
//                 Log.error("AccumeRect " + type + " for " + debug, t);
//             }
//         }

//         Rectangle2D.Float result() {
//             return rect == null ? EmptyBounds : rect;
//         }
        
//     }
//     /**
//      * @return the bounds for all LWComponents in the iterator
//      */
//     public static Rectangle2D.Float getBounds(Iterator<LWComponent> i)
//     {
//         final Rect rect = new Rect("bounds");

//         while (i.hasNext()) {
//             final LWComponent c = i.next();
//             if (c.isDrawn())
//                 rect.add(c.getBounds(), c);
//         }
//         return rect.result();
//     }
    
    private static void accumulate
        (final Rectangle2D.Float accume,
         final Rectangle2D.Float r,
         final LWComponent debug,
         final String debugMsg)
    {
        try {
            if (Util.isBadRect(r)) {
                Log.error(String.format("%s%-17s %s%s for %s",
                                        Util.TERM_RED,
                                        "bad " + debugMsg + ": ",
                                        Util.fmt(r),
                                        Util.TERM_CLEAR,
                                        debug));
                
            } else if (accume.isEmpty()) {
                accume.setRect(r);
            } else {
                accume.add(r);
            }
        } catch (Throwable t) {
            Log.error("accumulate " + debugMsg + " for " + debug, t);
        }
    }

    @Override
    public Rectangle2D.Float getPaintBounds() {

        if (mChildren == NO_CHILDREN)
            return EmptyBounds;

        final Rectangle2D.Float bounds = new Rectangle2D.Float();
        
        for (LWComponent layer : getChildren()) {
            if (layer.isVisible()) {
                if (layer instanceof Layer) { // this should always be the case
                    
                    accruePaintBounds(layer.getChildren(), bounds);
                    
                } else {
                    
                    // but in case of error in maintaining the hierarchy, if any
                    // regular components leak up to the top of the map, still
                    // compute bounds correctly.
                    
                    accruePaintBounds(Util.iterable(layer), bounds);
                }
            }
        }
        
        return bounds.isEmpty() ? EmptyBounds : bounds;

    }

    public static void accruePaintBounds(Iterable<LWComponent> iterable, Rectangle2D.Float rect)
    {
        for (LWComponent c : iterable)
            if (c.isDrawn())
                accumulate(rect, c.getPaintBounds(), c, "paintBounds");
    }
    
    @Override
    public Rectangle2D.Float getFocalBounds() {
        return getPaintBounds();
    }

    /**
     * @return the bounds for all LWComponents in the iterator
     */
    public static Rectangle2D.Float getBounds(Iterator<LWComponent> i)
    {
        final Rectangle2D.Float rect = new Rectangle2D.Float();
        while (i.hasNext()) {
            final LWComponent c = i.next();
            if (c.isDrawn())
                accumulate(rect, c.getBounds(), c, "bounds");
        }
        return rect.isEmpty() ? EmptyBounds : rect;
    }
    
    public static Rectangle2D.Float getBorderBounds(Iterable<LWComponent> iterable)
    {
        final Rectangle2D.Float rect = new Rectangle2D.Float();
        for (LWComponent c : iterable) {
            if (c.isDrawn())
                accumulate(rect, c.getBorderBounds(), c, "borderBounds");
        }
        return rect.isEmpty() ? EmptyBounds : rect;
    }

    public static Rectangle2D.Float getLocalBorderBounds(Iterable<LWComponent> iterable)
    {
        final Rectangle2D.Float rect = new Rectangle2D.Float();
        for (LWComponent c : iterable) {
            if (c.isDrawn())
                accumulate(rect, c.getLocalBorderBounds(), c, "localBorderBounds");
        }
        return rect.isEmpty() ? EmptyBounds : rect;
    }

    public static Rectangle2D.Float getLocalBounds(Iterable<LWComponent> iterable)
    {
        final Rectangle2D.Float rect = new Rectangle2D.Float();
        for (LWComponent c : iterable) {
            if (c.isDrawn())
                accumulate(rect, c.getLocalBounds(), c, "localBounds");
        }
        return rect.isEmpty() ? EmptyBounds : rect;
    }

    public static Rectangle2D.Float getLayoutBounds(Iterable<LWComponent> iterable)
    {
        final Rectangle2D.Float rect = new Rectangle2D.Float();
        for (LWComponent c : iterable) {
            if (c.isDrawn())
                accumulate(rect, c.getLayoutBounds(), c, "layoutBounds");
        }
        return rect.isEmpty() ? EmptyBounds : rect;
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




//     /** optimized for LWMap: remove if/when embed maps in maps */
//     @Override
//     public AffineTransform getZeroTransform() {
//         return new AffineTransform();
//     }
    
    /** optimized LWMap noop: remove if/when embed maps in maps */
    @Override
    protected AffineTransform transformDownA(final AffineTransform a) {
        return a;
    }
    /** optimized LWMap noop: remove if/when embed maps in maps */
    @Override
    protected void transformDownG(final Graphics2D g) {}
    /** optimized LWMap noop: remove if/when embed maps in maps */
    @Override
    public void transformZero(final Graphics2D g) {}
    
    /** optimized LWMap noop: remove if/when embed maps in maps
     * Just copies mapPoint to zeroPoint if zeroPoint is non null.
     * @return mapPoint if zeroPoint is null, zeroPoint otherwise
     */
    @Override
    public final Point2D transformMapToZeroPoint(Point2D.Float mapPoint, Point2D.Float zeroPoint) {
        if (zeroPoint == null) {
            return mapPoint;
        } else {
            zeroPoint.x = mapPoint.x;
            zeroPoint.y = mapPoint.y;
            return zeroPoint;
        }
    }

    @Override
    protected final Rectangle2D transformMapToZeroRect(Rectangle2D mapRect)
    {
        return mapRect;
//         if (zeroRect == null)
//             zeroRect = (Rectangle2D) mapRect.clone();
//         else
//             zeroRect.setRect(mapRect);
//         return zeroRect;
    }
    
    
    
    public String toString() {
        final StringBuilder buf = new StringBuilder("LWMap[v");
        buf.append(getSaveFileModelVersion());
        buf.append(' ');
        buf.append(getLabel());
        buf.append(" n=" + numChildren());
        if (DEBUG.DATA && mFile != null)
            buf.append(" <" + mFile + ">");
//         return "LWMap[" + getLabel()
//             + " n=" + children.size()
//             + (file==null?"":" <" + this.file + ">")
//             + "]";
        buf.append(']');
        return buf.toString();
    }
    //todo: this method must be re-written. not to save and restore
    public Object clone() throws CloneNotSupportedException{
        try {
            String prefix = "concept_map";
            String suffix = ".vue";
            File file = this.getFile();
            File tempFile  = File.createTempFile(prefix,suffix,VueUtil.getDefaultUserFolder());
            tufts.vue.action.ActionUtil.marshallMap(tempFile, this);
            LWMap cloneMap = tufts.vue.action.OpenAction.loadMap(tempFile.getAbsolutePath());
            cloneMap.setLabel(this.getLabel());
            tufts.vue.action.ActionUtil.marshallMap(file, this);
             return cloneMap;
        }catch(Exception ex) {
            throw new CloneNotSupportedException(ex.getMessage());
        }
    }
    
    
    private static boolean isGroupRelative(int dm) {
        return dm == 1 || dm >= 3;
    }

    private static boolean isGroupAbsolute(int dm) {
        return !isGroupRelative(dm);
    }
    
    
    /**
     * @return the current data-model version -- how the LWComponent hierarchy is organized and what their coordinates mean
     *
     * Model version 0: absolute children: pre-model versions / unknown (assumed all absolute coordinates)
     * Model version 1: relative children, including groups (excepting link members)
     * Model version 2: relative children, groups back to absolute (excepting link members -- only a few days this version)
     * Model version 3: relative children, groups back to relative with crude node-embedding support
     * Model version 4: relative children, groups relative, link points relative to parent (no longer have absolute map location)
     * Model version 5: layers added
     * Model version 6: meta-data persistance change: old versions of VUE can no longer see the meta-data
     */
    public static int getCurrentModelVersion() {
        return 6;
    }
    
    
    // Moved KEY_PresentationColor to the bottom of the file -- seems
    // to be helping with the sporadic javac failures -- SMF 2008-04-09
    private final ColorProperty mPresentationColor = new ColorProperty(KEY_PresentationColor, new java.awt.Color(32,32,32));
    public static final Key KEY_PresentationColor = new Key("presentation.color", KeyType.STYLE)
        { final Property getSlot(LWMap c) { return c.mPresentationColor; } };
    
    private List searchArrLst = new ArrayList();
    public List getSearchArrLst() { return searchArrLst; }
    public void setSearchArrLst(List searchArrLst) {
        if (DEBUG.SEARCH || DEBUG.RDF) Log.debug("setSearchArrLst " + searchArrLst);
        this.searchArrLst = searchArrLst;
    }

    // /** LWMergeMap purge */ public List<LWMap> getMapList() { return null; }
    // /** LWMergeMap purge */ public void setMapListSelectionType(int choice) {}
    // /** LWMergeMap purge */ public int getMapListSelectionType(int choice) { return 0; }

    private int _mergeMapBugCount = 0;
    
    /**
     * Any XML tag found in a save file that does not match a mapping in from the current mapping file shows
     * up here -- they appear to always be instances of org.exolab.castor.types.AnyNode
     *
     * This overrride here checks for *Boundaries XML tags to ignore (that only ocurred at the
     * LWMap level) that came from persisted LWMergeMaps, that could grow to include over three (3)
     * million of them in at least one document case.
     */
    @Override public final void addObject(Object o)
    {
        if (o instanceof org.exolab.castor.types.AnyNode && ((org.exolab.castor.types.AnyNode)o).getLocalName().endsWith("Boundaries")) {
            // Christ -- there are more than THREE MILLION of them in our bug-revealing customer map ForcesTrialAlicePulman.vue
            // Note: we to NOT want to call o.toString() for that many -- it's dramatically slow.
            if (_mergeMapBugCount++ % 100000 == 0)
                Log.info("restoring: " + _mergeMapBugCount + " XML <*Boundaries> ignored...");
        } else
            super.addObject(o);
    }
    
}
