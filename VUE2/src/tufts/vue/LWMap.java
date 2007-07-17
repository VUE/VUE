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
import tufts.vue.filter.*;

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
 * @version $Revision: 1.144 $ / $Date: 2007-07-17 00:53:20 $ / $Author: sfraize $
 */

public class LWMap extends LWContainer
{
    /** file we were opened from of saved to, if any */
    private File file;
    
    /** the list of LWPathways, if any */
    private LWPathwayList mPathways;
    
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
    
    
    // only to be used during a restore from persisted
    public LWMap() {
        init();
        setLabel("<map-during-XML-restoration>");
        mLWCFilter = new LWCFilter(this);
        mFillColor.setAllowTranslucence(false);
        // on restore, set to 0 initially: if has a model version in the save file,
        // it will be overwritten
        setModelVersion(0); 
    }
    
    public LWMap(String label) {
        init();
        setID("0");
        setFillColor(java.awt.Color.white);
        mFillColor.setAllowTranslucence(false);        
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

    protected void init() {
        disablePropertyTypes(KeyType.STYLE);
        enableProperty(LWKey.FillColor);
        disableProperty(LWKey.Label);
    }

//     /** @return true -- absolute means absolute map location, so all children of a map by definition
//      * have absolute position.  We'll want to change this if we ever implement embedding of maps
//      * within maps.
//      */
//     public boolean hasAbsoluteChildren() {
//         return true;
//     }

    // if/when we support maps embedded in maps, we'll want to have these return something real
    @Override
    public float getX() { return 0; }
    @Override
    public float getY() { return 0; }
    @Override
    public float getMapX() { return 0; }
    @Override
    public float getMapY() { return 0; }

    // TODO: fix LWComponent.getMapX/YPrecise to factor in map / supposed hasAbsoluteChildren    
//     // Performance
//     @Override
//     protected double getMapXPrecise() { return 0; }
//     @Override
//     protected double getMapYPrecise() { return 0; }
    

    /** Override LWContainer draw to always call drawInParent (even tho we have absolute children, we
     * don't want to just call draw, as LWContainer would).
     */
    // todo: something cleaner than draw/drawInParent in LWComponent would be nice so
    // we wouldn't have to deal with this kind of issue.  This is all because of the
    // slide icon hack -- if we can get rid of that, these issues would go away.
    @Override
    protected void drawChild(LWComponent child, DrawContext dc)
    {
        child.drawInParent(dc);
    }


    /** for persistance */
    public int getModelVersion() {
        return mModelVersion;
    }
    
    /** for persistance */
    public void setModelVersion(int version) {
        if (DEBUG.Enabled) out("setModelVersion " + version + "; current=" + mModelVersion);
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
        if (DEBUG.INIT) System.out.println(this + " explicitly marking as modified");
        if (mChanges == 0)
            mChanges = 1;
        // notify with an event mark as not for repaint (and set same bit on "repaint" event)
    }
    public void markAsSaved() {
        if (DEBUG.INIT) System.out.println(this + " marking " + mChanges + " modifications as current");
        mChanges = 0;
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
    
    private int nextID = 1;
    protected String getNextUniqueID() {
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

    private void changeAbsoluteToRelativeCoords(Collection<LWComponent> children)
    {
        // We must process the components top-down: adjust parent, then adjust children
        // Start by calling this on the map itself.

        for (LWComponent c : children) {
            if (c instanceof LWContainer && c.hasChildren()) {
                // CRAP: still need to process the damn children of the freakin group...
                // need to go back to our prior method...
                if (isGroupRelative(getModelVersion()) && c instanceof LWGroup) {
                    if (DEBUG.Enabled) System.out.println(" DM#1 ALREADY RELATIVE EXCEPT LINKS: " + c);
                    //((LWGroup)c).normalize();
                    continue;
                }
                
                changeAbsoluteToRelativeCoords((LWContainer) c);
                changeAbsoluteToRelativeCoords(c.getChildList());
                //if (c instanceof LWGroup)
                //    ((LWGroup)c).normalize(); // TODO: may need to wait till everything is laid out? see test-pathway.vue
            }
        }
    }

    private void changeLinksToParentRelative(Collection<LWComponent> nodes)
    {
        for (LWComponent c : nodes) {
            if (! (c instanceof LWLink))
                continue;

            final LWLink link = (LWLink) c;
            final LWContainer parent = link.getParent();

            if (parent instanceof LWMap) {
                if (DEBUG.Enabled) System.out.println("LINK ALREADY RELATIVE: " + link);
            } else {
                //if (DEBUG.Enabled) System.out.println("MAKING LINK PARENT RELATIVE: " + link);
                if (DEBUG.Enabled) link.out("MAKING LINK PARENT RELATIVE");
                // theoretically the parent could be scaled -- e.g., link is in a group that
                // is in a node, tho this is rare case...
                link.translate(-parent.getX(), -parent.getY());
            }
            
            // Now make sure link is parented to it's common parent:
            link.run();
        }
    }
    
    

    private void changeAbsoluteToRelativeCoords(LWContainer container) {
        if (DEBUG.Enabled) System.out.println("CONVERTING TO RELATIVE in " + this + "; container: " + container);
        for (LWComponent c : container.getChildList()) {
            if (c instanceof LWLink) {
                continue;
//                 if (getCurrentModelVersion() >= 4 && getModelVersion() < 4) {
//                     System.out.println("NEED TO RELATIVIZE LINK IN " + this + ": " + c);
//                 } else {
//                     // always have had absolute coords
//                     continue;
//                 }
            }
//             if (c.hasAbsoluteMapLocation())
//                 continue;
            c.takeLocation(c.getX() - container.getX(),
                           c.getY() - container.getY());
        }
    }

    private void changeRelativeToAbsoluteCoords(LWGroup container) {
        if (DEBUG.Enabled) System.out.println("CONVERTING TO ABSOLUTE: " + container);
        for (LWComponent c : container.getChildList()) {
            //if (getModelVersion() == 1
            if (c instanceof LWLink)
                continue;
            c.takeLocation(c.getX() + container.getX(),
                           c.getY() + container.getY());
        }
    }
    

    public void completeXMLRestore() {

        if (DEBUG.INIT || DEBUG.IO || DEBUG.XML)
            System.out.println(getLabel() + ": completing restore...");

        final Collection<LWComponent> allRestored = getAllDescendents(ChildKind.ANY, new ArrayList(), Order.DEPTH);

//         // Not needed now that everyone keeps under-restore bit set till manually cleared
//         for (LWComponent c : allRestored) {
//             if (DEBUG.IO) System.out.println("RESTORED: " + c);
//             // turn this back on until we're done here:
//             c.mXMLRestoreUnderway = true;
//         }
        

        if (mPathways != null) {
            try {
                mPathways.completeXMLRestore(this);
            } catch (Throwable t) {
                tufts.Util.printStackTrace(new Throwable(t), "PATHWAYS RESTORE");
            }
        }

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
            
            if (isGroupAbsolute(getModelVersion()))
                changeAbsoluteToRelativeCoords(getChildList());

            if (getCurrentModelVersion() >= 4 && getModelVersion() < 4)
                changeLinksToParentRelative(allRestored);

            VUE.Log.info(this + " Updated from model version " + getModelVersion() + " to " + getCurrentModelVersion());
            mModelVersion = getCurrentModelVersion();
        }
        
        
//         if (getModelVersion() < getCurrentModelVersion()) {
            
//             if (getModelVersion() < 3) {
//                 //-----------------------------------------------------------------------------
//                 // Upgrade from model version 0 -- make everything relative
//                 //-----------------------------------------------------------------------------

//                 changeAbsoluteToRelativeCoords(getChildList());
                
// //                 // Upgrade old save file to current state of relative coordinates (all but LWGroup's)
// //                 for (LWComponent c : allRestored) {
// //                     if (c instanceof LWSlide) {

// //                         // LWSlides are the only thing we need to upgrade now (as opposed to slides
// //                         // and groups, which were both upgraded for a while) Anyway, slides are the
// //                         // only objects left that have children with free layout.  LWNodes can also
// //                         // have children, but they enforce a particular layout location for their
// //                         // children, so no matter what the current model, LWNode child coordinates
// //                         // automatically get updated.  Of course, there shouldn't be many of these
// //                         // encountered, as VUE with slide supports was only around as an early
// //                         // mostly internal alpha before the default coordinate system went
// //                         // relative.
                        
// //                         changeAbsoluteToRelativeCoords((LWSlide)c);
// //                     }
// //                 }

//                 // Might want to auto-check in case the problem Bryce experienced pops up again...
//                 // (e.g., somehow the model version got set to 0 (original absolute), even tho the
//                 // coordinate system was in fact relative (model version 1).  This may have
//                 // happened by opening the map in a prior version of VUE which didn't understand
//                 // the relative coordinates, assumed they were absolute, reformed the groups at the
//                 // child coordinates as if they were absolute, then saved out the map with those
//                 // moved groups, and no model version recorded at all -- actually that could only
//                 // have happened on one of our internal versions that temporarily went back to
//                 // model 0?  Anyway...
                
// // Ff ANY group has contents that are outside it's bounds (perhaps only if significantly so),
// // we may want to assume we've got a model version problem, and just "upgrade" automatically.
// //                 for (LWComponent c : allRestored) {
// //                     if (c instanceof LWGroup) {
// //                         // check for relative children...
// //                         changeRelativeToAbsoluteCoords((LWGroup)c);
// //                     }
// //                 }
                
//             } else if (getModelVersion() == 1) {
                
//                 //-----------------------------------------------------------------------------
//                 // Upgrade from model version 1:
//                 //-----------------------------------------------------------------------------

//                 // Change relative coords of LWGroup children back to absolute coords:
                
//                 for (LWComponent c : allRestored) {
//                     if (c instanceof LWGroup) {
//                         changeRelativeToAbsoluteCoords((LWGroup)c);
//                     }
//                 }
//             }


//             VUE.Log.info(this + " Updated from model version " + getModelVersion() + " to " + getCurrentModelVersion());
//             mModelVersion = getCurrentModelVersion();
//             //VUE.Log.info(this + "   Updated to model version " + getModelVersion());
//         }

    
        // TODO: this is so that children of LWNode's get the node-child scale
        // factor.  This would be better handled by actually saving the scale values,
        // as we're eventually going to need that anyway.  Or, as this is a hack,
        // get rid of the setScaleOnChild code completely, and consolodate the
        // hack here by manually setting any LWNode children of LWNode's to
        // the LWNode.ChildScale (we don't need the cascaded values anymore
        // as VUE.RELATIVE_COORDS is firmly in place).  Or even better, handle
        // it in LWNode.XML_completed.
        
//         for (LWComponent c : allRestored) {
//             // LWNode overrides setScaleOnChild, as defined on LWContainer,
//             // to apply proper scaling to all generations of children
//             if (c instanceof LWNode && c.hasChildren()) {
//                 if (DEBUG.WORK) System.out.println("SET-SCALE: " + c);
//                 c.setScale(c.getScale());
//             }
//         }

        
        //----------------------------------------------------------------------------------------
        
        /*
          
          Now lay everything out.  We must wait till now to do this: after all child
          scales have been updated.  TODO: There may be an issue with the fact that
          we're not enforcing that this be done depth-first..
          
          If we don't do this after the child scales have been set, auto-sized parents
          that contain scaled children (the default node) will be too big, because they
          didn't know how big their children were when they computed total size of all
          children...

          Can we change this hack-o-rama method of doing things into something more
          reliable, where the child stores it's scaled value?  Tho that wouldn't jibe
          with being able to change that in one fell swoop via a preference very
          easily...
          
        */
        
        for (LWComponent c : allRestored) {
            if (DEBUG.LAYOUT||DEBUG.WORK) System.out.println("LAYOUT: in " +  c.getParent() + ": " + c);
            // ideally, this should be done depth-first, but it appears to be
            // working for the moment...
            c.mXMLRestoreUnderway = false;
            try {
                c.layout("completeXMLRestore");
            } catch (Throwable t) {
                tufts.Util.printStackTrace(t, "RESTORE LAYOUT " + c);
            }
        }
        
        for (LWComponent c : allRestored) {
            try {
                if (c instanceof LWGroup)
                    ((LWGroup)c).normalize();
            } catch (Throwable t) {
                tufts.Util.printStackTrace(t, "RESTORE NORMALIZE " + c);
            }
        }
        
        
        if (DEBUG.INIT || DEBUG.IO || DEBUG.XML) out("RESTORE COMPLETED; nextID=" + nextID + "\n");
        
        //setEventsResumed();
        markAsSaved();
    }

    /*
      // no longer needed: we now use castor references to handle this for us
    private void resolvePersistedLinks(Collection<LWComponent> allRestored)
    {
        for (LWComponent c : allRestored) {
            if (!(c instanceof LWLink))
                continue;
            LWLink l = (LWLink) c;
            try {
                final String headID = l.getHead_ID();
                final String tailID = l.getTail_ID();
                if (headID != null && l.getHead() == null) l.setHead(findByID(allRestored, headID));
                if (tailID != null && l.getTail() == null) l.setTail(findByID(allRestored, tailID));
            } catch (Throwable e) {
                tufts.Util.printStackTrace(e, "bad link? + l");
            }
        }
    }
    */

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
         * Draw all the children of the map
         *
         * Note that when the map draws, it does NOT fill the background,
         * as the background color of the map is usually a special case
         * property used to completely fill the background of an underlying
         * GUI component or an image.
         */
        super.drawChildren(dc);
        
        /*
         * Draw the pathways
         */
        if (mPathways != null && dc.drawPathways()) {
            int pathIndex = 0;
            for (LWPathway path : mPathways) {
                if (path.isDrawn()) {
                    dc.setIndex(pathIndex++);
                    path.drawPathway(dc.create());
                }
            }
        }

        if (DEBUG.BOXES) {
            dc.g.setColor(java.awt.Color.red);
            dc.g.setStroke(STROKE_ONE);
            for (LWComponent c : getAllDescendents()) {
                if (c.isDrawingSlideIcon())
                    dc.g.draw(c.getMapSlideIconBounds());
            }
        }
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
    
//     @Override
//     protected boolean containsImpl(float x, float y, float zoom) {
//         return true;
//     }
    
    /** @return Float.MAX_VALUE - 1: map contains all points, but any contents take priority */
    @Override
    protected float pickDistance(float x, float y, float zoom) {
        //return 100;
        return Float.MAX_VALUE;
    }

    
    /** override of LWContainer: default hit component on the map
     * is nothing -- we just @return null.
     */
    @Override
    protected LWComponent defaultPick(PickContext pc) {
        //return this; // allow picking of the map
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

    LWComponent addLWC(LWComponent c) {
        addChild(c);
        return c;
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

        /*
        final Object key = e.key;
        if (key == LWKey.Repaint || key == LWKey.Scale || key == LWKey.RepaintAsync || key == LWKey.RepaintComponent) {
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
        */
        
        flushBounds(); // TODO: optimize: need a bounds event yet again
        super.notifyLWCListeners(e);

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
    public Rectangle2D.Float getBounds() {

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
                System.out.println("COMPUTED BOUNDS: " + mCachedBounds + " for map " + this);
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




    /** optimized for LWMap: remove if/when embed maps in maps */
    @Override
    public AffineTransform getLocalTransform() { return new AffineTransform(); }
    /** optimized LWMap noop: remove if/when embed maps in maps */
    @Override
    public AffineTransform transformDown(final AffineTransform a) { return a; }
    /** optimized LWMap noop: remove if/when embed maps in maps */
    @Override
    public void transformRelative(final Graphics2D g) {}
    /** optimized LWMap noop: remove if/when embed maps in maps */
    @Override
    public void transformLocal(final Graphics2D g) {}
    
    
    public String toString() {
        StringBuffer buf = new StringBuffer("LWMap[");
        buf.append(getLabel());
        buf.append(" n=" + children.size());
        if (DEBUG.DATA && file != null)
            buf.append(" <" + file + ">");
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
            File tempFile  = File.createTempFile(prefix,suffix,VueUtil.getDefaultUserFolder());
            tufts.vue.action.ActionUtil.marshallMap(tempFile, this);
            return tufts.vue.action.OpenAction.loadMap(tempFile.getAbsolutePath());
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
     * @return the current deata-model version
     *
     * Model version 0: absolute children: pre-model versions / unknown (assumed all absolute coordinates)
     * Model version 1: relative children, including groups (excepting link members)
     * Model version 2: relative children, groups back to absolute (excepting link members -- only a few days this version)
     * Model version 3: relative children, groups back to relative with crude node-embedding support
     * Model version 4: relative children, groups relative, link points relative to parent (no longer have absolute map location)
     */
    //private static final int CurrentModelVersion = LWLink.LOCAL_LINKS ? 4 : 3;
    public static int getCurrentModelVersion() {
        return 4;
        //return LWLink.LOCAL_LINKS ? 4 : 3;
    }
    
    
    
    
}
