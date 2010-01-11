
/*
 * Copyright 2003-2008 Tufts University  Licensed under the
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

import java.awt.Image;
import java.awt.Point;
import java.awt.Color;
import java.awt.Font;
import java.awt.Shape;
import java.awt.BasicStroke;
import java.awt.geom.*;
import java.awt.AlphaComposite;

/**
 * Handle the presentation of an image resource, allowing resize.
 * Also provides special support for appear as a "node icon" -- a fixed
 * size image inside a node to represent it's resource.
 *
 * @version $Revision: 1.82 $ / $Date: 2007/11/19 06:20:27 $ / $Author: sfraize $
 */

public class LWImage extends LWComponent
    implements ImageRef.Listener
{
    //private static final class X__________ {}
    //private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(X__________.class); // debug marker
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(LWImage.class);

    public static final boolean SLIDE_LABELS = false;
    
    private static final int DefaultIconMaxSide = 128;
    private static final int DefaultWidth = 128;
    private static final int DefaultHeight = 128;
    
    private final static int MinWidth = 16;
    private final static int MinHeight = 16;

    private volatile ImageRef mImageRef = ImageRef.EMPTY;

    // point on AWT thread where the undo queue was before an async load operation was triggered
    private volatile Object mAWTUndoMark;

    private void initImage() {
        disableProperty(LWKey.FontSize); // prevent 0 font size warnings (font not used on images)
        takeFillColor(null);
        takeSize(DefaultWidth, DefaultHeight);
        setFlag(Flag.UNSIZED);
    }
    
    public LWImage() {
        initImage();
    }

    public LWImage(Resource r) {
        initImage();
        if (r == null)
            throw new IllegalArgumentException("resource is not image content: " + r);
        if (!r.isImage())
            Log.warn("making LWImage: may not be image content: " + r);
        setResource(r);
    }

    static LWImage create(Resource r) {
        return new LWImage(r);
        // auto set title based on MapDropTarget.makeNodeTitle?
    }

    public static LWImage createNodeIcon(Resource r) {
        if (DEBUG.IMAGE) Log.debug("createNodeIcon: " + r);
        final LWImage icon = new LWImage();
        icon.setNodeIcon(true);
        icon.setResource(r);
        return icon;
    }
    static LWImage createNodeIcon(LWImage i, Resource r) {
        if (DEBUG.IMAGE) Log.debug("createNodeIcon: " + i + "; " + r);
        final LWImage icon = (LWImage) i.duplicate(); // copy styling, title & notes
        // note: above will copy the resource over, but will skip initRef as mXMLRestoreUnderway is true during duplicates 
        icon.setNodeIcon(true);
        icon.setFlag(Flag.UNSIZED);
        icon.setResource(r);
        // as we've created a duplicate LWImage, which will have the same
        // size as the original, the newly loaded image Resource will
        // be automatically aspect-fit to the existing image size.
        return icon;
    }

    /** @return true -- an image is always it's own content */
    @Override public boolean hasContent() {
        return true;
    }

    @Override public LWImage duplicate(CopyContext cc)
    {
        // note: do not duplicate the isNodeIcon bit -- leave unset
        
        // note: when the resource is copied over, the new LWImage will
        // init the new ImageRef.
        
        // note: if the ImageRef has a bad status (e.g., ERROR), that's
        // not being transferred to the new ImageRef.  The new ImageRef
        // should just try to access the image again and generate it's
        // own status.

        final LWImage newImage = new LWImage();

        if (!hasFlag(Flag.UNSIZED))
            newImage.clearFlag(Flag.UNSIZED);

        return super.duplicateTo(newImage, cc);
    }

    /** @return false: images are never auto-sized */
    @Override public boolean isAutoSized() { return false; }
    /** @return false: images are never transparent */
    @Override public boolean isTransparent() { return false; }
    /** @return false: images are never translucent */
    @Override public boolean isTranslucent() { return false; }
    /** @return 0 */
    @Override public int getFocalMargin() { return 0; }
    
    /** @return true if this LWImage is being used as an icon for an LWNode */
    public boolean isNodeIcon() { return hasFlag(Flag.ICON); }

    void setNodeIcon(boolean t) {
        if (DEBUG.IMAGE) out("setNodeIcon " + t + "; " + this);
        setFlag(Flag.ICON, t);
    }

    @Override public boolean supportsCopyOnDrag() {
        return !hasFlag(Flag.SLIDE_STYLE) && isNodeIcon();
    }
    
    
    /** @return true unless this is a node icon image */
    @Override public boolean supportsUserResize() {
        return hasFlag(Flag.SLIDE_STYLE) || !isNodeIcon();
    }

    @Override public boolean supportsUserLabel() {
        return SLIDE_LABELS && hasFlag(Flag.SLIDE_STYLE);
    }

    /** this for backward compat with old save files to establish the image as a special "node" image */
    @Override public void XML_addNotify(Object context, String name, Object parent) {
        super.XML_addNotify(context, name, parent);
        if (parent instanceof LWNode)
            updateNodeIconStatus((LWNode)parent);
    }

    @Override protected void setParent(LWContainer parent) {
        super.setParent(parent);
        updateNodeIconStatus(parent);
    }
    
    private void updateNodeIconStatus(LWContainer parent) {

        //tufts.Util.printStackTrace("updateNodeIconStatus, mImage=" + mImage + " parent=" + parent);
        if (DEBUG.Enabled) out("updateNodeIconStatus " + mImageRef + "; parent=" + parent);

        if (parent == null)
            return;

        if (parent instanceof LWNode &&
            parent.getChild(0) == this &&
            getResource() != null &&
            getResource().equals(parent.getResource()))
        {
            // if first child of a LWNode is an LWImage, treat it as an icon
            setNodeIcon(true);
        } else {
            setNodeIcon(false);
        }
    }

    /** used by Actions to size the image */
    void setMaxDimension(final float max)
    {
        //========================================================================================
        // [FIXED]: if an image has an icon in cache, and we're creating a NEW RESOURCE,
        // such that resource properties image.width & image.height were never set, we can't know
        // the full pixel size, thus we can't be certiain of the *precise* aspect, which is
        // important to prevent minor pixel size tweaking later.  The only way around that
        // w/out forcing a load of a the whole image (which defeats the purpose of the image code
        // entirely) is to store the full pixel size in the icon itself.  We now do this
        // when we write generated icons to disk, but saving the original full pixel size
        // in the image meta-data.
        // ========================================================================================

        final int[] rawPixels = getFullPixelSize();

        // note: we used to do this via aspect, not full pixel size (which is much safer), tho that
        // means we can no longer adjust the icon size until we have the full pixel size loaded.

        if (rawPixels == ImageRep.ZERO_SIZE) {
            Log.warn("setMaxDimension: image rep has unset size; " + ref());
        } else {
            Size newSize = Images.fitInto(max, rawPixels);
            if (DEBUG.Enabled) out("setMaxDimension " + max + " -> " + newSize);
            setSize(newSize);
        }
    }

    public boolean hasImageError() {
        return ref().hasError();
    }
    
    @Override public void setSelected(boolean selected) {
        boolean wasSelected = isSelected();
        super.setSelected(selected);
        
        if (selected && !wasSelected && hasImageError() && hasResource()) {

            // TODO: this check wants to be on LWComponent or LWNode, in case this is a regular
            // node containing an LWImage, we want the image to update, as it doesn't get selected.
            // Even better is actually to handle this in the global selection listener or
            // ActiveInstance of LWComponent listener.
    
            //Util.printStackTrace("ADD SELCTED IMAGE CLEANUP " + this);
            // don't know if this really needs to be a cleanup task,
            // or just an after-AWT task, but safer to do one of them:

            // TODO: this may be conflicting with our new image update code, and this would much
            // better be handled in the ActiveComponentHandler than via a cleanup task (which
            // should generally be a solution of last resort) See if we can handle this in
            // VUE.checkForAndHandleResourceUpdate

            // Note that this code does however also deal with a missing network resource suddenly
            // appearing, and then we can load the image from that
            
            addCleanupTask(new Runnable() { public void run() {
                if (hasResource() && VUE.getSelection().only() == LWImage.this)
                    //loadSizeAndRef(getResource(), null);
                    // TODO: would need an UNDO-MARK for this, so really
                    // this wants to trigger our new LWNode image0 replacement code
                    initRef(getResource());
            }});
        }
    }
    
    @Override public void XML_completed(Object context) {
        super.XML_completed(context);

        if (super.width < MinWidth || super.height < MinHeight) {
            Log.info(String.format("bad size: adjusting to minimum %dx%d: %s", MinWidth, MinHeight, this));
            takeSize(MinWidth, MinHeight);
        }
        
        // we can't rely on this being cleared via setSizeImpl, as persistance currently accesses width/height directly
        clearFlag(Flag.UNSIZED); 
    }

    @Override public void setResource(Resource r) {
        super.setResource(r);
        // note: mXMLRestoreUnderway is set to true during duplicate operations
        if (mXMLRestoreUnderway || r == null) {
            // don't need to init anything -- size already known
            if (r == null)
                setFlag(Flag.UNSIZED);
        } else {
            initRef(r);
        }
    }

    private void recordUndoMark() {
//         if (!javax.swing.SwingUtilities.isEventDispatchThread())
//             throw new Error("can only record marks in AWT");
//         mAWTUndoMark = UndoManager.getKeyForNextMark(this);
//         out("SET MARK TO " + Util.tags(mAWTUndoMark));
//         Util.printStackTrace("SET-MARK " + mAWTUndoMark);
    }
    
    private void syncFurtherEventsToLastMark() {
//         if (javax.swing.SwingUtilities.isEventDispatchThread()) {
//             out("can only sync to events in non-AWT threads");
//         } else {
//             UndoManager.attachCurrentThreadToMark(mAWTUndoMark);
//             out("ATTACHED TO MARK " + Util.tags(mAWTUndoMark));
//             if (mAWTUndoMark != null) Util.printStackTrace("ATTACHED TO MARK " + mAWTUndoMark);
//         }
    }

//     private void forceUndoSizeRecords() {
//         UndoManager undoQueue = getUndoManager();
//         if (undoQueue == null)
//             return;
//         LWComponent sizeMayChange = getParent();
//         while (sizeMayChange != null && !sizeMayChange.isTopLevel()) {
//             // Simulate a size record to make certain there's one in there, just in case.
//             // There's no problem if the size doesn't actually end up changing -- the event
//             // will be applied but then ignored.
//             // Oh, crap -- this won't handle the REDO case for user-sized nodes tho, will it -- the
//             // size they took on after the image load completed was ignored!

//             // Oh, and our undo-mark hack won't even work now, as image-load threads are reused....

//             undoQueue.LWCChanged(new LWCEvent(this, sizeMayChange, LWKey.Size, sizeMayChange.getSize()));
//             Log.info("FORCED SIZE RECORDING FOR " + sizeMayChange + ": " + sizeMayChange.getSize());
//             sizeMayChange = sizeMayChange.getParent();
//         } 
//     }
    
    /** @see ImageRef.Listener */
    public synchronized void imageRefUpdate(Object cause) {

        // We will ONLY get this message once we already know the full image size.
        
        // note: this WILL NOT be in the AWT thread, so for full thread safety any
        // size changes should happen on AWT, tho that may conflict with our undo-tracking
        // for threaded inits?  Tho as long as we make use of the mark we obtain,
        // that shouldn't matter, right?
        //
        // Oh -- that happens via UndoManager.attachCurrentThreadToMark(mUndoMarkForThread),
        // which if we do in AWT will then attach all of AWT to that undo mark?  That
        // whole mechanism probably really needs to passed all the way through to the
        // notify so the undo manager can detect that there?

        if (DEBUG.IMAGE) out("imageRefUpdate: cause=" + Util.tags(cause));

        if (cause == ImageRef.KICKED) {
            // doesn't work: the mark's already been made
            //forceUndoSizeRecords();
            recordUndoMark();
            // In any case, now that we ignore non-AWT size updates, in practice,
            // this is a rare bug -- only when an image load is so slow that even
            // the size comes in delayed.
            return;
        }

        //---------------------------------------------------------------------------------------------------
        // Note: there are an absurd number of codepaths to test for the proper handlding of size & aspect setting:
        //  - runtime 0: fresh raw disk image load, then w/icon generation
        //  - runtime 0: now icon is generated, is now in memory cache (TODO: check: is the full sourceSize in the cache?)
        //  - new runtime, icon in disk cache
        //  - new runtime, icon in memory cache
        //  - quick import
        //  - file system drags
        //  - web browser URL field drag
        //  - web browser image drag (different for different browsers)
        //  - web browser image search light-tray drag (sizes sometimes decoded from the URLs)
        //  - persisted map restores
        // ++ PLUS POSSIBLE COMBINATIONS OF THE ABOVE (!)
        //---------------------------------------------------------------------------------------------------

        if (hasFlag(Flag.UNSIZED) && ref().fullPixelSize() != ImageRef.ZERO_SIZE) {

            syncFurtherEventsToLastMark();

            if (DEBUG.IMAGE) out("imageRefUpdate: SIZE IS UNSET; pixels=" + Util.tags(ref().fullPixelSize()));

            //========================================================================================
            // todo: call a method, that will have half of the guessAtBestSizeCode,
            // which if UNSIZED is true, will handle aspecting for both
            // node icons and slide styles.  The our guess method will become much simpler.
            // This might even be able to happen in setSizeImpl?
            //========================================================================================
            
            if (isNodeIcon())
                autoShape();
            else
                setToNaturalSize(); // PROBLEM?: shouldn't be undoable, tho it's already working that way because of general setSizeImpl?
        }

        // always repaint -- e.g., we may already have the valid size, so no
        // new size event will trigger a repaint, or this just may mean the
        // pixels have arrived.  Technically, we should be able to skip this
        // unless we've got new pixel data, but we don't get a separate message
        // for that at the moment.
        repaintPixels(); 
    }

    public void reloadImage() {
        ref().reload();
    }

    private void repaintPixels() {
        //Log.debug("ISSUING PIXEL REPAINT " + getLabel());

        // tho we're not going to be on the AWT thread, the result of a Repaint/RepaintRegion is
        // ultimately going to be to safely stick a repaint request into the AWT queue, so we
        // should be okay.  We will have to pull the bounds of this LWImage non-synchronized
        // tho.

        if (alive()) {
            if (isSelected()) { // can force on for seeing DEBUG.BOXES behind-scenes status changes in other images
                // need to also redraw selection boxes
                notify(LWKey.Repaint);
            } else
                notify(LWKey.RepaintRegion);
        }
    }
    
    @Override public void setToNaturalSize() {
        setSize(getFullPixelSize());
    }

    private int[] getFullPixelSize() {
        int[] size = ref().fullPixelSize();
        
        if (size == ImageRep.ZERO_SIZE && hasResource()) {

            // It's possible to have only the icon loaded, and not the full representation, and
            // thus not immediately know the size of the full representation.

            // todo: note that even if one ImageRep loads it's full size, others that haven't
            // reqested it yet will still end up using this method.  As soon as one rep has a real
            // size, all should get their real size.  (also another argument for making them
            // singleton)

            int[] suggested = getResourceImageSize(getResource());
            if (suggested != null)
                return suggested;
            // if no resource suggestion, be sure to return ImageRep.ZERO_SIZE
        }
        
        return size;
    }

    private int[] getResourceImageSize(Resource r) {

        if (r == null || r.getProperties() == null)
            return null;

        if (DEBUG.IMAGE) out("checking for resource size props in: " + r.getProperties().asProperties());
        
        final int w = r.getProperty(Resource.IMAGE_WIDTH, -1);
        final int h = r.getProperty(Resource.IMAGE_HEIGHT, -1);
        
        if (w > 0 && h > 0) {
            final int[] size = new int[2];
            size[0] = w;
            size[1] = h;
            // todo: would be better to NOT let this be used as a "valid" size -- if it later
            // disagrees with something found on disk (the image has changed on disk since this
            // resource was created), the new, real image size should take priority.
            return size;
        } else {
            return null;
        }
    }
    
                                      
    private void setSize(Size s) {
        setSize(s.width, s.height);
    }

    private void setSize(int[] size) {
        if (size == ImageRep.ZERO_SIZE) {
            Log.warn("skipping setSize of ZERO_SIZE; " + this);
        } else {
            setSize(size[0], size[1]);
        }
    }
    
    public void suggestSize(int w, int h) 
    {
        if (DEBUG.Enabled) out("suggestSize " + w + "x" + h);
        setTmpSize(w, h);
    }

    void setTmpSize(float w, float h) {
        setSizeImpl(w, h, true);
    }

    @Override protected synchronized void setSizeImpl(float w, float h, final boolean internal) {
        
        if (DEBUG.Enabled) out("setSizeImpl " + w + "x" + h + "; internal=" + internal);

        // Tracking UNSIZED lets us skip the undo-queue for size changes, but not for size
        // changes to auto-sizing containers (e.g., LWNode) that happen due to re-layout. E.g., you
        // drop an image, it takes a long time to load, you do other stuff to the map, the image
        // finally gets it's size, resizing it's parents -- if we purely ignore all those size
        // changes, then you UNDO THE DROP, those nodes won't be sized back.  We used to handle
        // this via our undo-mark hack.

        // This is now handled very nicely in LWComponent by skipping undo events for
        // size reports from non-AWT threads.  This works most of the time because
        // most nodes are auto-sized and auto-relayout.  But user-sized nodes are still
        // a problem.

        // A way to handle that could be to force a size report for all parents when we begin a
        // threaded image load, so no matter what there will be a size recorded into the undo-queue
        // at that point.  This is much simpler than our our undo-mark hack.  The problem is doing
        // this BEFORE the "Drop" mark happens -- it will only work if that's possible.
        
        super.setSizeImpl(w, h, internal);
        
        if (internal) {
            if (!hasFlag(Flag.UNSIZED)) {
                if (DEBUG.Enabled) Log.error("ATTEMPT TO DE-VALIDATE SIZE! " + this, new Throwable("HERE"));
                //setFlag(Flag.UNSIZED);
            }
        } else {
            if (hasFlag(Flag.UNSIZED)) {
                if (DEBUG.Enabled) out("setting first VALID size " + w + "x" + h);
                clearFlag(Flag.UNSIZED);
            }
        }
    }

    private float aspect() {
        return ref().aspect();
    }
    
//     private void autoShape() {
//         float maxSide = (float) Math.max(this.width, this.height);
             
//         final Size newSize = Images.fitInto(maxSide, this.width, this.height);

//         if (DEBUG.Enabled) out("autoShape: maxSiide=" + maxSide
//                                + "\n\t in: " + width + "," + height
//                                + "\n\tout: " + newSize);
        
//         setSize(newSize);
//     }
    
    private void autoShape() {
        shapeToAspect(aspect());
    }
    
    private void shapeToAspect(float aspect) {

        if (aspect <= 0) {
            Log.warn("bad aspect in shapeToAspect: " + aspect);
            return;
        }
             
        // TODO: reconcile w/Imags.fitInto used in setMaxDimension
        final Size newSize = ConstrainToAspect(aspect, this.width, this.height);
        //final Size newSize = Images.fitInto(

        if (DEBUG.Enabled) out("shapeToAspect " + aspect
                               + "\n\t in: " + width + "," + height
                               + "\n\tout: " + newSize);
        
        setSize(newSize);
    }
    
    /**
     * Don't let us get bigger than the size of our image, or
     * smaller than MinWidth/MinHeight.
     */
    protected void userSetSize(float width, float height, MapMouseEvent e)
    {
        final float aspect = aspect();
        
        if (DEBUG.IMAGE) out("userSetSize " + Util.fmt(new Point2D.Float(width, height)) + "; aspect=" + aspect + "; e=" + e);
        //Util.printStackTrace("HERE");

        if (e != null && e.isShiftDown()) {
            // Unconstrained aspect ratio scaling
            super.userSetSize(width, height, e);
        } else if (aspect > 0) {
            Size newSize = ConstrainToAspect(aspect, width, height);
            setSize(newSize.width, newSize.height);
        } else if (width > 0 && height > 0) {
            setSize(width, height);
        } else {
            Log.error("unhandled userSetSize w/aspect " + aspect + ": " + width + "x" + height + " on " + this);
        }

//         If (e != null && e.isShiftDown())
//             croppingSetSize(width, height);
//         else
//             scalingSetSize(width, height);
    }

    private Shape getClipShape() {
        //return super.drawnShape;
        // todo: cache & handle knowing if we need to update
        return new Rectangle2D.Float(0,0, getWidth(), getHeight());
    }

    private static final AlphaComposite HudTransparency = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f);
    //private static final Color IconBorderColor = new Color(255,255,255,64);
    //private static final Color IconBorderColor = new Color(0,0,0,64); // screwing up in composite drawing
    //private static final Color IconBorderColor = Color.gray;

    private ImageRef ref() {
        return mImageRef;
    }
    
    private ImageRef refLoaded() {
        if (mImageRef == ImageRef.EMPTY && hasResource())
            return initRef(getResource());
        else
            return ref();
    }

    private ImageRef initRef(Resource r) {
        
        if (DEBUG.IMAGE) {
            out("initRef: " + r + "; props=" + r.getProperties());
            if (!java.awt.EventQueue.isDispatchThread())
                out("initRef: NOT ON AWT: " + Thread.currentThread());
        }
        
        if (mImageRef != ImageRef.EMPTY && mImageRef.source().original == r) {
            Log.info("re-init of same image source: " + this + "; " + r, new Throwable("HERE"));
        }
        
        final ImageRef newRef = ImageRef.create(this, r);
        mImageRef = newRef;

        // we should also auto-shape if we already have the size info, which we should
        // if the image is in the cache -- both for proper node icones when alread in cache,
        // and for replacing the ImageRef on a raw images -- both to get the right aspect

        // THIS IS WHERE WE USED TO CREATE AN UNDO MARK -- I think we
        // can now handle this by just ignoring async size events?

        if (hasFlag(Flag.UNSIZED)) {
            // okay to do this after undo-mark was obtained, as this should NOT be generating
            // undoable events.
            guessAtBestSize(r);
        }

        return newRef;
        
    }

    // if the size in the ref() is already known, we'll be using that, otherwise,
    // if we find any size info in the resource, use that as a temporary size
    private void guessAtBestSize(Resource r) {

        if (DEBUG.IMAGE) out("guessAtBestSize: " + ref() + "; " + r);
        
        final int[] fullSize = ref().fullPixelSize();

        Size guess = null;

        if (fullSize != ImageRef.ZERO_SIZE) {
            if (isNodeIcon()) {
                guess = Images.fitInto(DefaultIconMaxSide, fullSize);
            } else if (hasFlag(Flag.SLIDE_STYLE)) {
                guess = Images.fitInto(LWSlide.SlideWidth / 4, fullSize);
            } else {
                guess = new Size(fullSize);
            }
            // really, we want to just do setSize, but we need to do setSizeImpl so it's
            // not undoable, but then we have to clear our own UNSIZED bit
            setSizeImpl(guess.width, guess.height, true/*=INTERNAL*/);
            clearFlag(Flag.UNSIZED);
        }
        else {

            final int[] suggestSize = getResourceImageSize(r);
            
            if (suggestSize != null) {
                guess = new Size(suggestSize);
                
                if (isNodeIcon())
                    guess = Images.fitInto(DefaultIconMaxSide, guess);
                else if (hasFlag(Flag.SLIDE_STYLE))
                    guess = Images.fitInto(LWSlide.SlideWidth / 4, guess);
                
                setTmpSize(guess.width, guess.height);
            }
        }
    }
    
    @Override protected void preCacheContent() {
        refLoaded().preLoadFullRep();
    }    
    
    /** This currently makes LWImages invisible to selection (they're locked in their parent node */
    @Override protected LWComponent defaultPickImpl(PickContext pc) {
        if (!hasFlag(Flag.SLIDE_STYLE) && isNodeIcon())
            return pc.pickDepth > 0 ? this : getParent();
        else
            return this;
    }

    private void drawImage(DrawContext dc)
    {
        final ImageRef ref = refLoaded();
        
        if (hasFlag(Flag.UNSIZED)) {
            // seems a bit overkill, but oddly needed if the image is already in the cache, as we
            // don't currently immediately pull the aspect from the icon image size data in the
            // cache at init time (we're still waiting for a draw to do that), which we should be
            // doing in ImageRef/ImageRep
            guessAtBestSize(getResource());
        }

        if (isSelected() && dc.isInteractive() && dc.focal != this) {
            dc.g.setColor(COLOR_HIGHLIGHT);
            dc.g.setStroke(new BasicStroke(getStrokeWidth() + SelectionStrokeWidth));
            dc.g.draw(getZeroShape());
        }

        ref.drawInto(dc, getWidth(), getHeight());
    }
    
    @Override protected void drawImpl(DrawContext dc)
    {
        drawImage(dc);
    }
    
//     private boolean isIndicatedIn(DrawContext dc) {

//         if (dc.hasIndicated()) {
//             final LWComponent indication = dc.getIndicated();

//             return indication == this
//                 || (isNodeIcon() && getParent() == indication);
//         } else {
//             return false;
//         }
//     }

    
//     public void drawWithoutShape(DrawContext dc)
//     {
//         // see comments on VUE-892 - this code does not seem to present the right behavior for search
//         // please re-enable this code and reopen the bug if this code is needed- Dan H
//         /*
//         if (isNodeIcon()) {
//             final LWComponent parent = getParent();
//             if (parent != null && parent.isFiltered()) {
//                 // this is a hack because images are currently special cased as tied to their parent node
//                 return;
//             }
//         }*/

//         final Shape shape = getZeroShape();

//         if (isSelected() && dc.isInteractive() && dc.focal != this) {
//             dc.g.setColor(COLOR_HIGHLIGHT);
//             dc.g.setStroke(new BasicStroke(getStrokeWidth() + SelectionStrokeWidth));
//             dc.g.draw(shape);
//         }

//         final boolean indicated = isIndicatedIn(dc);

//         if (indicated && dc.focal != this) {
//             Color c = getParent().getRenderFillColor(dc);
//             if (VueUtil.isTranslucent(c))
//                 c = Color.gray;
//             dc.g.setColor(c);
//             dc.g.fill(shape);
//         }
        
//         if (isNodeIcon && dc.focal != this) {

//             if (!indicated && DefaultMaxDimension > 0)
//                 drawImageBox(dc);
            
//             // Forced border for node-icon's:
//             if ((mImage != null ||*/ indicated) && !getParent().isTransparent() && DefaultMaxDimension > 0) {
//                 // this is somehow making itext PDF generation through a GC worse... (probably just a bad tickle)
//                 dc.g.setStroke(STROKE_TWO);
//                 //dc.g.setColor(IconBorderColor);
//                 dc.g.setColor(getParent().getRenderFillColor(dc).darker());
//                 dc.g.draw(shape);
//             }
            
//         } else if (!indicated) {
            
//             if (!super.isTransparent()) {
//                 final Color fill = getFillColor();
//                 if (fill == null || fill.getAlpha() == 0) {
//                     Util.printStackTrace("isTransparent lied about fill " + fill);
//                 } else {
//                     dc.g.setColor(fill);
//                     dc.g.fill(shape);
//                 }
//             }

        
//             	drawImageBox(dc);
            
//             if (getStrokeWidth() > 0) {
//                 dc.g.setStroke(this.stroke);
//                 dc.g.setColor(getStrokeColor());
//                 dc.g.draw(shape);
//             }

//             if (supportsUserLabel() && hasLabel()) {
//                 initTextBoxLocation(getLabelBox());
//                 if (this.labelBox.getParent() == null) {
//                     dc.g.translate(labelBox.getBoxX(), labelBox.getBoxY());
//                     this.labelBox.draw(dc);
//                 }
//             }
//         }

//         //super.drawImpl(dc); // need this for label
//     }

    /** For interactive images as separate objects, which are currently disabled */
    /*
    private void drawInteractive(DrawContext dc)
    {
        drawPathwayDecorations(dc);
        drawSelectionDecorations(dc);
        
        dc.g.translate(getX(), getY());
        float _scale = getScale();

        if (_scale != 1f) dc.g.scale(_scale, _scale);
        
//         if (getStrokeWidth() > 0) {
//             dc.g.setStroke(new BasicStroke(getStrokeWidth() * 2));
//             dc.g.setColor(getStrokeColor());
//             dc.g.draw(new Rectangle2D.Float(0,0, getWidth(), getHeight()));
//         }

        drawImage(dc);

        if (getStrokeWidth() > 0) {
            dc.g.setStroke(this.stroke);
            dc.g.setColor(getStrokeColor());
            dc.g.draw(new Rectangle2D.Float(0,0, getWidth(), getHeight()));
        }
        
        if (isSelected() && dc.isInteractive()) {
            dc.g.setComposite(HudTransparency);
            dc.g.setColor(Color.WHITE);
            dc.g.fill(mIconBlock);
            dc.g.setComposite(AlphaComposite.Src);
            // TODO: set a clip so won't draw outside
            // image bounds if is very small
            mIconBlock.draw(dc);
        }

        if (_scale != 1f) dc.g.scale(1/_scale, 1/_scale);
        dc.g.translate(-getX(), -getY());
    }
*/

    //----------------------------------------------------------------------------------------
    // some old status code
    //----------------------------------------------------------------------------------------
    
    private static final Color EmptyColorDark = new Color(0,0,0,128);
    private static final Color LoadedColorDark = new Color(0,0,0,160);
    private static final Color EmptyColorLight = new Color(128,128,128,64);
    private static final Color LoadedColorLight = new Color(128,128,128,128);

    private Color getEmptyColor(DrawContext dc) {
        Color fill = getFinalFillColor(dc);

        return Color.black.equals(fill) ? EmptyColorLight : EmptyColorDark;
        
//         final LWComponent parent = getParent();
//         Color pc;
//         if (parent != null && (pc=parent.getRenderFillColor(dc)) != null) {
//             return Color.black.equals(pc) ? EmptyColorLight : EmptyColorDark;
//         } else if (dc.getBackgroundFill() != null && dc.getBackgroundFill().equals(Color.black))
//             return EmptyColorLight;
//         else
//             return EmptyColorDark;
            
    }
    private Color getLoadedColor(DrawContext dc) {
        final LWComponent parent = getParent();
        Color pc;
        if (parent != null && (pc=parent.getRenderFillColor(dc)) != null) {
            return Color.black.equals(pc) ? LoadedColorLight : LoadedColorDark;
        } else
            return LoadedColorDark;
    }

    //----------------------------------------------------------------------------------------
    // old experimental on-map text label code
    //----------------------------------------------------------------------------------------
    
    @Override protected TextBox getLabelBox()
    {
        if (super.labelBox == null) {
            initTextBoxLocation(super.getLabelBox());
            //layoutImpl("LWImage.labelBox-init");
        }
        return this.labelBox;
    }
    
    
    @Override public void initTextBoxLocation(TextBox textBox) {
        textBox.setBoxLocation(0, -textBox.getHeight());
    }

    //----------------------------------------------------------------------------------------
    // Image rotation was old feature we removed, but this stuff may still be in some old save files.
    //----------------------------------------------------------------------------------------

    public static final Key KEY_Rotation = new Key("image.rotation", KeyType.STYLE) { // rotation in radians
            public void setValue(LWComponent c, Object val) { ((LWImage)c).setRotation(((Double)val).doubleValue()); }
            public Object getValue(LWComponent c) { return new Double(((LWImage)c).getRotation()); }
        };
    
    public void setRotation(double rad) {
//         Object old = new Double(mRotation);
//         this.mRotation = rad;
//         notify(KEY_Rotation, old);
    }
    public double getRotation() {
        //return mRotation;
        return 0;
    }

//     public static final Key Key_ImageOffset = new Key("image.pan", KeyType.STYLE) {
//             public void setValue(LWComponent c, Object val) { ((LWImage)c).setOffset((Point2D)val); }
//             public Object getValue(LWComponent c) { return ((LWImage)c).getOffset(); }
//         };

    public void setOffset(Point2D p) {
//         if (p.getX() == mOffset.x && p.getY() == mOffset.y)
//             return;
//         Object oldValue = new Point2D.Float(mOffset.x, mOffset.y);
//         if (DEBUG.IMAGE) out("LWImage setOffset " + VueUtil.out(p));
//         this.mOffset.setLocation(p.getX(), p.getY());
//         notify(Key_ImageOffset, oldValue);
    }

    public Point2D getOffset() {
        return null;
//         return new Point2D.Float(mOffset.x, mOffset.y);
    }

    
    @Override protected void out(String s) {
        Log.debug(String.format("%s %s", this, s));
    }
    
//     @Override
//     protected boolean intersectsImpl(Rectangle2D mapRect) {
//         boolean i = super.intersectsImpl(mapRect);
//         Log.info("INTERSECTS " + Util.tags(i?"YES":" NO") + " " + Util.tags(mapRect) + " " + getLabel());
//         return i;
//     }
//     @Override
//     public boolean requiresPaint(DrawContext dc) {
//         boolean i = super.requiresPaint(dc);
//         Log.info("REQRSPAINT " + Util.tags(i?"YES":" NO") + getLabel());
//         return i;
//     }

    public static void main(String args[]) throws Exception {

        // GUI init required for fully loading all image codecs (tiff gets left behind otherwise)
        // Ah: the TIFF reader in Java 1.5 apparently comes from the UI library:
        // [Loaded com.sun.imageio.plugins.tiff.TIFFImageReader
        // from /System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Classes/ui.jar]

        VUE.init(args);
        
        System.out.println(java.util.Arrays.asList(javax.imageio.ImageIO.getReaderFormatNames()));
        System.out.println(java.util.Arrays.asList(javax.imageio.ImageIO.getReaderMIMETypes()));

        String filename = args[0];
        java.io.File file = new java.io.File(filename);

        System.out.println("Reading " + file);

        System.out.println("ImageIO.read got: " + javax.imageio.ImageIO.read(file));

        /*
          The below code requires the JAI libraries:
          // JAI (Java Advandced Imaging) libraries
          /System/Library/Java/Extensions/jai_core.jar
          /System/Library/Java/Extensions/jai_codec.jar

          Using this code below will also get us decoding .fpx images,
          tho we would need to convert it from the resulting RenderedImage / PlanarImage
          
        */

        /*
        try {
            // Use the ImageCodec APIs
            com.sun.media.jai.codec.SeekableStream stream = new com.sun.media.jai.codec.FileSeekableStream(filename);
            String[] names = com.sun.media.jai.codec.ImageCodec.getDecoderNames(stream);
            System.out.println("ImageCodec API's found decoders: " + java.util.Arrays.asList(names));
            com.sun.media.jai.codec.ImageDecoder dec =
                com.sun.media.jai.codec.ImageCodec.createImageDecoder(names[0], stream, null);
            java.awt.image.RenderedImage im = dec.decodeAsRenderedImage();
            System.out.println("ImageCodec API's got RenderedImage: " + im);
            Object image = javax.media.jai.PlanarImage.wrapRenderedImage(im);
            System.out.println("ImageCodec API's got PlanarImage: " + image);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // We're not magically getting any new codec's added to ImageIO after the above code
        // finds the .fpx codec...
        
        System.out.println(java.util.Arrays.asList(javax.imageio.ImageIO.getReaderFormatNames()));
        System.out.println(java.util.Arrays.asList(javax.imageio.ImageIO.getReaderMIMETypes()));

        */
        
    }
}