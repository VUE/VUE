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

import tufts.Util;
import tufts.vue.LWComponent.ColorProperty;
import tufts.vue.LWComponent.Key;
import tufts.vue.LWComponent.Property;
import tufts.vue.filter.*;

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
 * @version $Revision: 1.198 $ / $Date: 2008-05-20 22:01:39 $ / $Author: sfraize $
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
    
    /** the current Map Filter **/
    LWCFilter mLWCFilter;
    
    /** Metadata for Publishing **/
    PropertyMap metadata = new PropertyMap();
    
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

    private transient int mSaveFileModelVersion = -1;
    private transient int mModelVersion = getCurrentModelVersion();

    private static final String InitLabel = "<map-during-XML-restoration>";
    
    // only to be used during a restore from persisted
    public LWMap() {
        initMap();
        setLabel(InitLabel);
        mLWCFilter = new LWCFilter(this);
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
        mChildren.add(c);
        // todo: listen to child for events & pass up
    }

    protected void initMap() {
        //mFillColor.setAllowAlpha(false);        
        disablePropertyTypes(KeyType.STYLE);
        enableProperty(LWKey.FillColor);
        disableProperty(LWKey.Label);
        mFillColor.setAllowAlpha(false);
    }

    // if/when we support maps embedded in maps, we'll want to have these return something real
    @Override
    public float getX() { return 0; }
    @Override
    public float getY() { return 0; }
    @Override
    public float getMapX() { return 0; }
    @Override
    public float getMapY() { return 0; }


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
    
    
    
    @Override
    String getDiagnosticLabel() {
        return "Map: " + getLabel();
    }
    
    private void markDate() {
        long time = System.currentTimeMillis();
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
        if (DEBUG.Enabled) Log.debug("explicitly marking as modified: " + this);
        if (mChanges == 0)
            mChanges = 1;
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
            c.setFiltered(false);
        }
        mLWCFilter.setFilterOn(false);       
        notify(LWKey.MapFilter);
    }
    
    public  void applyFilter()
    {
        if (mLWCFilter.getFilterAction() == LWCFilter.ACTION_SELECT)
            VUE.getSelection().clear();

        for (LWComponent c : getAllDescendents()) {
            if (!(c instanceof LWNode) && !(c instanceof LWLink)) // why are we only doing nodes & links?
                continue;
            
            boolean state = mLWCFilter.isMatch(c);
            if (mLWCFilter.isLogicalNot())
                state = !state;

            if (mLWCFilter.getFilterAction() == LWCFilter.ACTION_HIDE)
                c.setFiltered(state);
            else if (mLWCFilter.getFilterAction() == LWCFilter.ACTION_SHOW)
                c.setFiltered(!state);
            else if (mLWCFilter.getFilterAction() == LWCFilter.ACTION_SELECT) {
                if (state)
                    VUE.getSelection().add(c);
            }
        }
        filterWasOn = true;
        mLWCFilter.setFilterOn(true);
        notify(LWKey.MapFilter); // only MapTabbedPane wants to know this, to display a filtered icon...
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
    
    public PropertyMap getMetadata(){
        return metadata;
    }
    
    public void setMetadata(PropertyMap metadata) {
        this.metadata = metadata;
    }
    
    public MapFilterModel getMapFilterModel() {
        return mapFilterModel;
    }
    
    public void setMapFilterModel(MapFilterModel mapFilterModel) {
        //out("setMapFilterModel " + mapFilterModel);
        this.mapFilterModel = mapFilterModel;
    }
    
    public LWPathwayList getPathwayList() {
        return mPathways;
    }
    
    /** for persistance restore only */
    public void setPathwayList(LWPathwayList l){
        //System.out.println(this + " pathways set to " + l);
        mPathways = l;
        mPathways.setMap(this);
    }

    @Override
    public Collection<LWComponent> getAllDescendents(final ChildKind kind, final Collection bag, Order order) {
        super.getAllDescendents(kind, bag, order);
        if (kind == ChildKind.ANY && mPathways != null) {
            for (LWPathway pathway : mPathways) {
                bag.add(pathway);
                pathway.getAllDescendents(kind, bag, order);
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

    
    private int nextID = 1;
    protected synchronized String getNextUniqueID() {
        return Integer.toString(nextID++, 10);
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

            if (parent instanceof LWMap) {
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

    static final String NODE_INIT_LAYOUT = "completeXMLRestore:NODE";
    static final String LINK_INIT_LAYOUT = "completeXMLRestore:LINK";

    public void completeXMLRestore(Object context)
    {
        if (DEBUG.INIT || DEBUG.IO || DEBUG.XML)
            Log.debug(getLabel() + ": completing restore...");

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

        this.nextID = findGreatestID(allRestored) + 1;
        
        for (LWPathway pathway : mPathways) {
            // 2006-11-30 14:33.32 SMF: LWPathways now have ID's,
            // but they didn't used to, so make sure they
            // have an ID on restore in case it was a save file prior
            // to 11/30/06.
            ensureID(pathway);
        }

        //----------------------------------------------------------------------------------------
        // Now update the model the the most recent data version
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

        
        //----------------------------------------------------------------------------------------
        
        // Now lay everything out.  allRestored should be in depth-first order for maximum
        // reliability (the deepest items should lay themselves out first, so parent items
        // that need to know the size of their children will get accurate results).
        //
        
        // First, we layout all NON links, so we can layout the links afterwords, and when
        // the links recompute, they'll be able to know for certian the borders of what
        // they're connected to.  Note that this means that the layout of a container
        // should not depend on a link be current yet.  Groups depend on knowing the size
        // of link children, but that's not handled via the layout code -- that's handled
        // via group normalization.

        //----------------------------------------------------------------------------------------
        
        // Do NOT normalize the groups yet: will seriously break old maps.  It slighly improves some of our
        // interim formats (1-2), but makes others a complete mess.
        //         for (LWComponent c : allRestored)
        //                 if (c instanceof LWGroup)
        //                     ((LWGroup)c).normalize();

        //-----------------------------------------------------------------------------
        // Layout non-links:
        //-----------------------------------------------------------------------------
        
        //if (!tufts.vue.action.SaveAction.PACKAGE_DEBUG) // tmp hack: we get exceptions when testing just SaveAction on this code
        
        for (LWComponent c : allRestored) {
            // mark all, including links, now, as when we get to them, links-to-links may
            // cause cascading recomputes that would warn us they're still being restored otherwise.
            c.mXMLRestoreUnderway = false;
            if (c instanceof LWLink)
                continue;
            if (DEBUG.LAYOUT||DEBUG.INIT) out("LAYOUT NODE: in " +  c.getParent() + ": " + c);
            try {
                c.layout(NODE_INIT_LAYOUT);
            } catch (Throwable t) {
                tufts.Util.printStackTrace(t, "RESTORE LAYOUT NODE " + c);
            }
        }

        //-----------------------------------------------------------------------------
        // Layout links -- will trigger recomputes & layout any link-labels that need it.
        //-----------------------------------------------------------------------------
        
        //if (!tufts.vue.action.SaveAction.PACKAGE_DEBUG) // tmp hack
            
        for (LWComponent c : allRestored) {
            if (c instanceof LWLink == false)
                continue;
            if (DEBUG.LAYOUT||DEBUG.INIT) out("LAYOUT LINK: in " +  c.getParent() + ": " + c);
            try {
                c.layout(LINK_INIT_LAYOUT);
            } catch (Throwable t) {
                tufts.Util.printStackTrace(t, "RESTORE LAYOUT LINK " + c);
            }
        }
        
        //-----------------------------------------------------------------------------
        // Just to be sure, re-normalize all groups.  This shouldn't be required, except
        // perhaps if we're updating from an old model version.
        //-----------------------------------------------------------------------------
        
        for (LWComponent c : allRestored) {
            try {
                if (c instanceof LWGroup)
                    ((LWGroup)c).normalize();
            } catch (Throwable t) {
                tufts.Util.printStackTrace(t, "RESTORE NORMALIZE " + c);
            }
        }
        
        
        if (DEBUG.INIT || DEBUG.IO || DEBUG.XML) Log.debug("RESTORE COMPLETED; nextID=" + nextID);
        
        mXMLRestoreUnderway = false;
        //setEventsResumed();
        markAsSaved();
    }

    class ResourceFactory extends Resource.DefaultFactory {

        private final Map<String,Resource> resourceMap = new HashMap();

        void loadResources(Collection<Resource> resourceBag) {
            for (Resource r : resourceBag)
                track(r);
        }

        private void track(Resource r) {
            final Resource already = resourceMap.put(r.getSpec(), r);
            if (already != null) {
                // this okay for the moment: we're only using this for keeping
                // image data up to date
                Log.debug("duplicate: " + already + " (tossed)");
                if (DEBUG.Enabled)
                Log.debug("         : " + r);
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
        if (DEBUG.Enabled) {
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
        if (DEBUG.Enabled)
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
                    out("Found an LWCopmonent persisted without an id: " + c);
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
            super.drawChildren(dc);
        } else {
            super.drawChildren(dc);
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
// doing so can produce HUGE 20MB+ tiff attachments)
    
    

    
    
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

    public int getLayer() {
        return 0;
    }

    /** @return false: maps can't be selected with anything else */
    public boolean supportsMultiSelection() {
        return false;
    }
    

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
    protected void addChildImpl(LWComponent c) {
        if (c instanceof LWPathway)
            throw new IllegalArgumentException("LWPathways not added as direct children of map: use addPathway " + c);
        super.addChildImpl(c);
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
    
    private void markChange(Object e) {

        if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
            
            // for now, anything from a non AWT Event Dispatch Thread (EDT) is assumed
            // to not be a real undoable chage -- this mainly to prevent image size sets
            // after the map loads from leaving the map appearing to have been modified.
            // A more complete solution might mark all events generated on specific
            // threads known to be behaving this way.
            
            if (DEBUG.WORK || DEBUG.EVENTS || DEBUG.INIT) Log.debug("staying clean for non-AWT event: " + e);
            return;
        }
            
        if (mChanges == 0) {
            if (DEBUG.EVENTS) out(this + " First Modification Happening on " + e);
            if (DEBUG.INIT||(DEBUG.EVENTS&&DEBUG.META)) new Throwable("FYI: FIRST MODIFICATION").printStackTrace();
        }
        mChanges++;
    }

    @Override
    protected Rectangle2D.Float getZeroBounds() {
        Util.printStackTrace("LWMap getZeroBounds " + this);
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
            mCachedBounds = getPaintBounds(getChildIterator());
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
    
    /*
    public java.awt.geom.Rectangle2D getCachedBounds()
    {
        return super.getBounds();
    }
     */
    
//     public static Rectangle2D.Float getBounds(Iterator<LWComponent> i) {
//         return getBounds(i, Short.MAX_VALUE);
//     }

    /** this object returned if getBounds result had no contents / produced no actual bounds */
    public static final Rectangle2D.Float EmptyBounds = new Rectangle2D.Float();
    
    /**
     * return the bounds for all LWComponents in the iterator
     */
    //public static Rectangle2D.Float getBounds(Iterator<LWComponent> i, int maxLayer)
    public static Rectangle2D.Float getBounds(Iterator<LWComponent> i)
    {
        Rectangle2D.Float rect = null;
        
        while (i.hasNext()) {
            final LWComponent c = i.next();
//             if (c.getLayer() > maxLayer)
//                 continue;
            if (c.isDrawn()) {
                if (rect == null) {
                    rect = new Rectangle2D.Float();
                    rect.setRect(c.getBounds());
                } else
                    rect.add(c.getBounds());
            }
        }
        return rect == null ? EmptyBounds : rect;
        //return rect == null ? new Rectangle2D.Float() : rect;
    }

    @Override
    public Rectangle2D.Float getPaintBounds() {
        return mChildren == null ? EmptyBounds : getPaintBounds(mChildren.iterator());
    }

    @Override
    public Rectangle2D.Float getFocalBounds() {
        return getPaintBounds();
    }
    

    public static Rectangle2D.Float getPaintBounds(Iterator<LWComponent> i)
    {
        Rectangle2D.Float rect = null;
        
        while (i.hasNext()) {
            final LWComponent c = i.next();
            if (c.isDrawn()) {
                if (rect == null) {
                    rect = new Rectangle2D.Float();
                    rect.setRect(c.getPaintBounds());
                } else
                    rect.add(c.getPaintBounds());
            }
        }
        return rect == null ? EmptyBounds : rect;
        //return rect == null ? new Rectangle2D.Float() : rect;
    }

    public static Rectangle2D.Float getBorderBounds(Iterable<LWComponent> iterable)
    {
        final Iterator<LWComponent> i = iterable.iterator();
        if (i.hasNext()) {
            final Rectangle2D.Float rect = new Rectangle2D.Float();
            rect.setRect(i.next().getBorderBounds());
            while (i.hasNext()) {
                final LWComponent c = i.next();
                if (c.isDrawn())
                    rect.add(c.getBorderBounds());
            }
            return rect;
        } else
            return EmptyBounds;
    }

    public static Rectangle2D.Float getLocalBorderBounds(Iterable<LWComponent> iterable)
    {
        final Iterator<LWComponent> i = iterable.iterator();
        if (i.hasNext()) {
            final Rectangle2D.Float rect = new Rectangle2D.Float();
            rect.setRect(i.next().getLocalBorderBounds());
            while (i.hasNext()) {
                final LWComponent c = i.next();
                if (c.isDrawn())
                    rect.add(c.getLocalBorderBounds());
            }
            return rect;
        } else
            return EmptyBounds;
    }

    public static Rectangle2D.Float getLocalBounds(Iterable<LWComponent> iterable)
    {
        final Iterator<LWComponent> i = iterable.iterator();
        if (i.hasNext()) {
            final Rectangle2D.Float rect = new Rectangle2D.Float();
            rect.setRect(i.next().getLocalBounds());
            while (i.hasNext()) {
                final LWComponent c = i.next();
                if (c.isDrawn())
                    rect.add(c.getLocalBounds());
            }
            return rect;
        } else
            return EmptyBounds;
    }

    public static Rectangle2D.Float getLayoutBounds(Iterable<LWComponent> iterable)
    {
        final Iterator<LWComponent> i = iterable.iterator();
        if (i.hasNext()) {
            final Rectangle2D.Float rect = new Rectangle2D.Float();
            rect.setRect(i.next().getLayoutBounds());
            while (i.hasNext()) {
                final LWComponent c = i.next();
                if (c.isDrawn())
                    rect.add(c.getLayoutBounds());
            }
            return rect;
        } else
            return EmptyBounds;
    }
    
    
    
    
    
    
//     /**
//      * return the shape bounds for all LWComponents in the iterator
//      * (does NOT include stroke widths) -- btw -- would make
//      * more sense to put these in the LWContainer class.
//      */
//     public static Rectangle2D getShapeBounds(Iterator<LWComponent> i) {
//         Rectangle2D rect = new Rectangle2D.Float();
        
//         if (i.hasNext()) {
//             rect.setRect(((LWComponent)i.next()).getShapeBounds());
//             while (i.hasNext())
//                 rect.add(((LWComponent)i.next()).getShapeBounds());
//         }
//         return rect;
//     }
    
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
    public Point2D transformMapToZeroPoint(Point2D.Float mapPoint, Point2D.Float zeroPoint) {
        if (zeroPoint == null) {
            return mapPoint;
        } else {
            zeroPoint.x = mapPoint.x;
            zeroPoint.y = mapPoint.y;
            return zeroPoint;
        }
    }
    
    
    public String toString() {
        StringBuffer buf = new StringBuffer("LWMap[");
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
     * @return the current data-model version
     *
     * Model version 0: absolute children: pre-model versions / unknown (assumed all absolute coordinates)
     * Model version 1: relative children, including groups (excepting link members)
     * Model version 2: relative children, groups back to absolute (excepting link members -- only a few days this version)
     * Model version 3: relative children, groups back to relative with crude node-embedding support
     * Model version 4: relative children, groups relative, link points relative to parent (no longer have absolute map location)
     */
    public static int getCurrentModelVersion() {
        return 4;
    }
    
    
    // Moved KEY_PresentationColor to the bottom of the file -- seems
    // to be helping with the sporadic javac failures -- SMF 2008-04-09
    
    private final ColorProperty mPresentationColor = new ColorProperty(KEY_PresentationColor, new java.awt.Color(32,32,32));
    public static final Key KEY_PresentationColor = new Key("presentation.color", KeyType.STYLE)
        { final Property getSlot(LWMap c) { return c.mPresentationColor; } };
    
    
}
