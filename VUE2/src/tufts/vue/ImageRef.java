package tufts.vue;

import tufts.Util;

import java.lang.ref.*;
import java.awt.Image;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;

import static tufts.vue.ImageRep.UNAVAILABLE;

public class ImageRef
{
    public static final int DEFAULT_ICON_SIZE = 128;
    public static final int[] ZERO_SIZE = ImageRep.ZERO_SIZE;
    public static final Object GOT_SIZE = "ImageRef.GOT-SIZE";
    public static final Object REPAINT = "ImageRef.REPAINT";
    public static final Object KICKED = "ImageRef.*****KICKED*****";
    
    public static final ImageRef EMPTY = new ImageRef() {
//             @Override public void setImageSource(Object is) {
//                 Log.error("attempt to set image source on the empty ImageRef: " + Util.tags(is), new Throwable("HERE"));
//             }
            @Override protected void repaint() {}
            @Override void preLoadFullRep() {}
            @Override public boolean equals(Object o) { return false; }
            @Override public String toString() { return "ImageRef[___EMPTY___]"; }
        };

    //===================================================================================================

    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(ImageRef.class);
    private static final int PIXEL_THRESHOLD_FOR_ICON_GENERATION = (2*DEFAULT_ICON_SIZE) * (2*DEFAULT_ICON_SIZE);
    /**
     * This determins when icons are drawn v.s. the full rep.  The smaller this is, the more
     * often the full representation will be requested for drawing.
     */
    private static final int PIXEL_THRESHOLD_FOR_ICON_DRAWING = DEFAULT_ICON_SIZE*2;

    private static final boolean ICONS_ARE_DISPOSABLE = false; // todo: true case needs testing / may not work
    
//     private static final String SIZE_FULL = "FULL-SIZE";
//     private static final String SIZE_ICON = "ICON-SIZE";
//     private static final String SIZE_UNKNOWN = "UNKNOWN-SIZE";
    
    private final ImageSource _source;

    private final Listener _repainter;
    
    private volatile float _aspect = 0;

    private volatile ImageRep _full = ImageRep.UNAVAILABLE;
    private volatile ImageRep _icon = ImageRep.UNAVAILABLE;
    
    //private volatile Object _desired = SIZE_UNKNOWN;
    // _desired not used at moment -- would be easy to have one global instance of an ImageRef per image w/out it,
    // and could add back in this functionality by allowing a client to implement a simple recording API for desired
    // e.g., set/getDesired -- is only needed to prevent extra repaints when a new image rep arrives.
    
    public static interface Listener {
        public void imageRefUpdate(Object cause);
    }

//     static ImageRef create(java.io.File file) {
//         return null;
//     }

    public static ImageRef create(Listener listener, Object imageData) {
        // could allow for single-instance per Resource/URI caching here
        return new ImageRef(listener, imageData);
    }

    private ImageRef(Listener listener, Object imageData) {
        _repainter = listener;
        _source = ImageSource.create(imageData);
        //if (DEBUG.IMAGE) Log.debug("created image source " + _source + " from " + is);
        initReps();
    }

    private ImageRef() {
        _source = null;
        _repainter = null;
    }

    ImageSource source() {
        return _source;
    }

//     public boolean isBlank() {
//         return _source == null;
//     }
    
//     private void setImageSource(Object is) {
//         //if (_source != null) throw new Error("ImageSource re-set not permitted: " + this);
//         if (_source != is) {
//             //-----------------------------------------------------------------------------
//             // PROBLEM: if this is a local file, the URI cache key in the _source.key
//             // will be null, meaning we can't later create an icon cache key from it.
//             // Yet at the moment we're only seeing this as a problem if the local
//             // file is missing -- so how is this working in the regular case?
//             //-----------------------------------------------------------------------------
//             _source = ImageSource.create(is);
//             //if (DEBUG.IMAGE) Log.debug("created image source " + _source + " from " + is);
//             initReps();
//         }
//     }

    private static final boolean PRE_LOAD_ICONS = false;

    private void initReps() {

        if (DEBUG.IMAGE) debug("initReps");

        //boolean reload = false;

//         if (_icon != UNAVAILABLE || _full != UNAVAILABLE) {
//             //Log.info("re-loading ref " + ref);
//             throw new Error("re-init of reps");
//         }
        
        // rep won't load until it attempts to draw:
        // We init this first so it's available for the icon to init with
        // the full pixel original size pulled from the icon.
        _full = ImageRep.create(this, _source);
        
        // As icons only exist in the cache, we know we don't have an icon
        // created yet if there isn't at least a unloaded cache entry for the icon.

        final java.net.URI iconKey = _source.getIconKey(DEFAULT_ICON_SIZE);

        if (iconKey != null) {
            if (PRE_LOAD_ICONS) {
                //synchronized (Images.getCacheLock()) {
                    // we did this in a cache-lock to ensure the cache entry can't have been GC'd or
                    // changed from the time we check for it to the time we kick a load for it,
                    // but could we dead-lock? -- Ideally, we want to release the lock at the point
                    // getImage returns in ImageRep.reconstitute, otherwise it's ImageRef callback,
                    // notifyRepHasArrived, will also happen in the cache-lock, which is dangerous
                    // -- it will block all other image processing threads till it returns [todo:
                    // test/verify]
                    if (Images.hasCacheEntry(iconKey)) {
                        _icon = createPreLoadedIconRep(iconKey);
                        kickLoad(_icon);
                    }
                    //}
            } else {
                if (Images.hasCacheEntry(iconKey)) {
                    _icon = createPreLoadedIconRep(iconKey);
                }
            }
        } // _icon left as ImageRep.UNAVAILABLE

//         // rep won't load until it attempts to draw:
//         _full = ImageRep.create(this, _source);
    }
    
//     public void drawInto(Graphics2D g, float width, float height)
//     {
//         // need to always try, as this is how we'll know if the 
//         // the full is ever desired (when an icon has been pre-loaded)
//         drawBestAvailable(g, width, height);
//     }
    
    public void drawInto(DrawContext dc, float width, float height)
    {
        try {
            drawBestAvailable(dc, width, height);
        } catch (Throwable t) {
            Log.error("exception painting " + this, t);
        }
    }

//     private void drawAvailable(Graphics2D g, float width, float height) 
//     {
//         if (_icon.available())
//             _icon.renderRep(g, width, height);
//         else if (_full.available())
//             _full.renderRep(g, width, height);
//         else 
//             ; //UNAVAILABLE.drawRep(g, width, height);
//     }
    
    private static final java.awt.Color LoadingOverlay = new java.awt.Color(128,128,128,128);
    private static final java.awt.Color LoadingOverlayWhite = new java.awt.Color(255,255,255,128);
    private static final java.awt.Color LoadingOverlayBlack = new java.awt.Color(0,0,0,128);
    private static final java.awt.Color DebugRed = new java.awt.Color(255,0,0,128);
    private static final java.awt.Color DebugGreen = new java.awt.Color(0,255,0,128);
    private static final java.awt.Color DebugBlue = new java.awt.Color(0,0,255,128);
    private static final java.awt.Color DebugYellow = new java.awt.Color(255,255,0,128);

    private ImageRep pickRepToDraw(final ImageRep ideal) {
        return pickRepToDraw(ideal, ideal == _full ? _icon : _full);
    }
    
    private ImageRep pickRepToDraw(final ImageRep desired, final ImageRep backup)
    {
        // Note that a rep that is not "available" may also be "loading", and be receiving progress
        // on that load which it is able to display. (Maybe different semantics would be helpful
        // e.g. -- provide a "drawable" as well as "available" and/or rename "available" to
        // "loaded").
        
        if (desired.available()) // the most common case at runtime
            return desired; 
        else if (backup.available())
            return backup;
        else if (desired == backup) {
            // this is the common case at init
            if (desired != ImageRep.UNAVAILABLE) {
                // should never happen
                Log.error("desired == backup != UNAVAILABLE: " + desired, new Throwable("HERE"));
                reload();
            }
            return ImageRep.UNAVAILABLE;
        }
        else if (desired.loading())
            return desired;
        else if (backup.loading())
            return backup;
        else if (desired.hasError())
            return desired;
        else if (backup.hasError())
            return backup;
        else
            return ImageRep.UNAVAILABLE;
        
    }

    //private static final double ASSUMED_PRINTER_DPI = 600;
    
    private ImageRep getIdealRep(DrawContext dc, float width, float height, Image[] GC_lock)
    {
        if (dc.isPrintQuality()) {
            // Note: Even the non-blocking image fetch runes in ImageRep w/out a listener, ImageRep
            // still generates needed callbacks to us as this comes in via cacheData.  It's
            // possibly we may at some point want to skip even that though, as it could place less
            // strain on memory if we're running low.  Memory constraints while printing appear to
            // be worse than just for rendering maps to the screen.  E.g., we would never want to
            // start generating an icon during a print job, tho that is theoretically possible.  So,
            // todo: at some point ensure that ImageRep.cacheData w/immediate call does NOT make
            // the ImageRef callback, and we do anything we need to do for recording an arrived
            // full rep right here, immediately (e.g., record the size if this is the first time
            // we've seen the full rep).  So ImageRef may want it's own cacheData style method to
            // serve the same purpose it does in ImageRep.
            GC_lock[0] = _full.getImageBlocking();
            return _full;
        }
        
//         final double scale;
//         // Actually, little point in trying this optimization -- e.g., if
//         // we're "printing" to a PDF or a print-preview, it's generated with essentially infinte scale,
//         // as the result will be user-zoomable. [HOWEVER: we may wish to restore this just to reduce memory consumption]
//         if (dc.isPrintQuality()) {
//             scale = dc.g.getTransform().getScaleX() * (ASSUMED_PRINTER_DPI / 72.0);
//         } else {
//             scale = dc.g.getTransform().getScaleX();
//         }

        final double scale = dc.g.getTransform().getScaleX();
        final int onDisplayMaxDim;

        if (aspect() > 1f) {
            //debug("aspect="+aspect() + " picking width " + width);
            onDisplayMaxDim = (int) (scale * width);
        } else {
            //debug("aspect="+aspect() + " picking height " + height);
            onDisplayMaxDim = (int) (scale * height);
        }

        final ImageRep idealRep;
        
        // We don't worry about coherency sync issues between _full & _icon here -- we should
        // handle whatever's thrown at us on a best-available basis. The only thing we rely on is
        // they should never be null.

        if (onDisplayMaxDim <= PIXEL_THRESHOLD_FOR_ICON_DRAWING) {
            // It would be more complete to check the actual iconRep size v.s. our constant, tho this
            // lets us not worry if it's been loaded or not, and currently we only ever generate
            // icons of a single size.  Note that this also assumes the icon will always be smaller
            // than the full rep, which should hold true as we don't bother to generate an icon
            // otherwise.
            idealRep = _icon;
            //backupRep = _full;
            //_desired = SIZE_ICON;
            //debug("onScreenMaxDim below thresh " + PIXEL_THRESHOLD_FOR_ICON_DRAWING + " at " + onScreenMaxDim);
        } else {
            //debug("onScreenMaxDim ABOVE thresh " + PIXEL_THRESHOLD_FOR_ICON_DRAWING + " at " + onScreenMaxDim);
            //_desired = SIZE_FULL;
            idealRep = _full;
            //backupRep = _icon;
        }

        // Technically, we only need a GC lock for _full, as _icon reps are permanently GC locked internally
        // in the current implementation.
        GC_lock[0] = idealRep.image();

//         if (dc.isPrintQuality()) {
//             Log.debug(String.format("print GC scale %.2f net=%.2f px=%4d %s",
//                                     dc.g.getTransform().getScaleX(), scale, onDisplayMaxDim, idealRep));
//         }

        return idealRep;
        
    }
    
    private void drawBestAvailable(DrawContext dc, float width, float height)
    {
        // Tasks to accomplish here:
        // (1) find the ideal representation for the situation (the current rendering size v.s. available pixels)
        // (2) pick the best representation to draw given what's actually available
        // (3) start loading the ideal represation for future use if it wasn't available

        final Image[] idealImageLock = new Image[1]; // for holding a GC-lock on the image if it's there
        final ImageRep ideal = getIdealRep(dc, width, height, idealImageLock);
        final ImageRep drawable;
        
        if (idealImageLock[0] != null) {
            // This is the common case once everything has been loaded and cached in memory, and presuming
            // we're not running low on memory.
            
            renderImage(dc.g, idealImageLock[0], width, height);

            idealImageLock[0] = null; // ensure GC-lock is immediately released
            
            // setting drawable here is now just for possible debug, which could probably be factored out,
            // and allow us to return an object from getIdealRep, which is sometimes an Image, sometimes
            // an ImageRep.
            drawable = ideal;

        } else {

            // Note: the logic below is tuned to cover many possible corner cases,
            // and is not easy to refactor w/out breaking one or more of them.
            
            drawable = pickRepToDraw(ideal);

            if (DEBUG.IMAGE && DEBUG.BOXES) {
                debug("    ideal " + ideal);
                debug(" drawable " + drawable);
            }

            if (!dc.isAnimating()) {
                // We never kick image data loading during animations, as the desired representation
                // may only be a momentary need (and it could suddenly slow down the animation to boot).
                kickLoad(ideal, drawable);
            }

            // rendering before/after kickloads doesn't matter as long as reps don't auto-constitute
            drawable.renderRep(dc.g, width, height);

            if (DEBUG.Enabled) {
                if (drawable != ideal && !dc.isPrintQuality() && drawable.available() && !ideal.hasError())
                    drawBetterRepAvailableIndicator(dc.g, width, height);
            }
        }


        if (DEBUG.BOXES) drawDebugStatus(dc.g, ideal, drawable, width, height);

    }
    
    /** render a fully loaded image who's size is known to the java.awt.Image provided into the given width/height */
    static void renderImage
        (final Graphics2D g,
         Image image,
         final float toWidth,
         final float toHeight)
    {
        final float pixelsWide = image.getWidth(null);
        final float pixelsTall = image.getHeight(null);
        g.drawImage(image,
                    AffineTransform.getScaleInstance(toWidth / pixelsWide,
                                                     toHeight / pixelsTall),
                    null);

        image = null; // attempt to help GC

        // todo performance: keep a re-usable AffineTransform in the DrawContext for the above
        // kinds of usage, and just use setToScale on it?  Add image rendering to the DrawContext?
        
    }
    

    private void drawBetterRepAvailableIndicator(Graphics2D g, float width, float height) {
        // draw a "loading" indicator
        //             if (drawable != ideal)
        //                 dc.g.setColor(DebugRed);
        //             else
        //                 dc.g.setColor(DebugGreen);
        final float sw = Math.max(width,height) / 64f;
        g.setStroke(new java.awt.BasicStroke(sw));
        final float xoff, yoff;
        xoff = yoff = sw / 2f;
        //xoff = width / 8f;
        //yoff = height / 8f;
        final Rectangle2D.Float r = new Rectangle2D.Float(xoff,yoff,width-xoff*2,height-yoff*2);
        g.setColor(LoadingOverlayBlack);
        g.draw(r);
        g.setColor(LoadingOverlayWhite);
        r.x += xoff;
        r.y += yoff;
        r.width -= xoff * 2;
        r.height -= yoff * 2;
        g.draw(r);
    }
    

    private void kickLoad(ImageRep ideal, ImageRep drawable)
    {
        // Note: the logic below is tuned to cover many possible corner cases,
        // and is not easy to refactor w/out breaking one or more of them.
        
        if (ideal != UNAVAILABLE) {
            kickLoad(ideal);
        } else if (!drawable.available()) {
            if (drawable == UNAVAILABLE) { // if icon load failed, must create a new one (low memory) [NOT ENOUGH!]
                if (DEBUG.Enabled) debug("forcing full load");
                kickLoad(_full);
            } else if (drawable == _icon && drawable.hasError()) {
                //****************************************************************************************
                // if icon load failed, must create a new one (low memory) [TODO: NOT ENOUGH]
                //****************************************************************************************
                if (DEBUG.Enabled) debug("forcing full load on bad icon");
                kickLoad(_full);
            } else
                kickLoad(drawable);
        }
    }

    private void drawDebugStatus(Graphics2D g, ImageRep idealRep, ImageRep drawRep, float width, float height)
    {
        final float hw = width / 2f;
        final float hh =  height / 2f;
        final java.awt.geom.Rectangle2D.Float r = new java.awt.geom.Rectangle2D.Float();
        if (drawRep == _icon) {
            // we're looking at the icon rep
            g.setColor(DebugYellow);
            r.setRect(0, 0, hw, hh);
            g.fill(r);
        }
        if (drawRep != idealRep) {
            // we're waiting for a better rep
            g.setColor(DebugRed);
            r.setRect(0, hh, hw, hh);
            g.fill(r);
        }
        if (_full.available()) {
            //                 // we've got the full rep loaded
            //                 if (_full.isFading()) // was to check to Reference enequing
            //                     g.setColor(DebugYellow);
            //                 else
            g.setColor(DebugBlue);
            r.setRect(hw, 0, hw, height);
            g.fill(r);
        }
    }

    protected void repaint() {
        _repainter.imageRefUpdate(REPAINT);
    }

    private static final boolean ENABLE_IMMEDIATE_SIZES = true; // turn off for debugging undo of delayed size reports/layouts

    public void notifyRepHasProgress(final ImageRep rep, final float pct) {
        if (ENABLE_IMMEDIATE_SIZES && _aspect == 0 && rep == _full && rep.size() != ZERO_SIZE) {
            _aspect = rep.aspect();
            _repainter.imageRefUpdate(GOT_SIZE);
        } else {
            repaint();
        }
    }
    
    /** the ImageRep is done loading -- it has all the renderable image data, unless hardImageRef is null,
     in which case we had an error */
    public void notifyRepHasArrived(final ImageRep freshRep, final Images.Handle hardImageRef)
    {
        //if (_desired == freshRep || _desired == SIZE_UNKNOWN)  // may be easiest/safest just to always repaint
        // used to force some reasonable aspect at top and always issue the repaint first
        //repaint();
            
        // note that we may actually get this call with hardImageRef set to null, which means we got an error,
        // and just want to repaint
        if (freshRep == _full && _icon == UNAVAILABLE && hardImageRef != null) {
            // no icon was previously generated -- look to see if
            // one has been generated elsewhere in this runtime,
            // or if not, and we need one, create it now.
            if (_full.area() > PIXEL_THRESHOLD_FOR_ICON_GENERATION) {
                _icon = createRuntimeScaledIconRep(freshRep, hardImageRef.image);
            } // else _icon left as ImageRep.UNAVAILABLE
             
        }
        else if (freshRep == _icon && (_full == UNAVAILABLE || _full.size() == ZERO_SIZE)) {
            // We don't have a full rep loaded -- pull it from meta-data stored with the icon.
            // There are several reasons this is important: (1) We may need to know the full
            // pixel size before the full representation is available (e.g., set to natural size).
            // (2) We want to know our "perfect" aspect even if we don't have the full image.
            // Generated icons may have slight changes in aspect, and even slight changes in aspect
            // has given us major problems in the past (e.g., maps w/images changing the first time
            // they're opened). (3) an interaction of these various problems had completey broken
            // image folder import.
            loadFullPixelSize(hardImageRef);
        }

        if (_aspect == 0 || freshRep == _full) {
            // note: this used to be at the very top, before the repaint() issue
            _aspect = freshRep.aspect(); 
        }
        repaint();
    }

    private void loadFullPixelSize(Images.Handle icon) {
        try {
            unpackFullPixelSize(icon);
        } catch (Throwable t) {
            Log.error("extracting full pixel size from " + icon, t);
        }
    }
        
    private void unpackFullPixelSize(Images.Handle icon) {
        Object ss = icon.data.get("sourcePixels");
        if (ss != null) {

            if (_full == UNAVAILABLE) {
                Log.error("UNAVAILABLE FULL-REP", new Throwable("HERE"));

                // note: we could lazily force create the _full rep with just the size info,
                // but currently reps should have always already been created at init (containing
                // no data at all except their ImageSource)
                    
                //_full = ImageRep.create(this, _source);
                    
            } else {
                if (DEBUG.Enabled) Util.dump(icon.data, "FOUND SOURCE PIXEL SIZE");
                _full.takeSize((int[])ss);
                _aspect = _full.aspect(); // force aspect based on exact pixel dimensions
            }
        }

        //========================================================================================
        // This problem is what determined that we MUST save this size in the cache somehow (e.g.,
        // with the icon).  PROBLEM: if an image has an icon in cache, and we're creating a NEW
        // RESOURCE, such that resource properties image.width & image.height were never set, we
        // can't know the full pixel size.  Well HAVE to use the aspect (old image code didn't use
        // aspect here -- always used full pixel size).  The ONLY WAY around that one, w/out
        // forcing a load of a the whole image (which defeats the purpose of the image code
        // entirely) would be to store the full pixel size in the icon itself somehow.  That would
        // be a good idea anyway... how to best do it?  .PNG meta-data would be great, tho putting
        // it in the filename would be easier, tho if if the source image changed...  actually,
        // that could be one way we detect that the source image has changed, tho including the
        // modification date would be ideal -- now we REALLY need meta-data...  Oh, wait, we could
        // actually use the modification date of the icon file -- just make sure it's AFTER the
        // on-disk file.
        // ========================================================================================


        
    }

    private ImageRep createPreLoadedIconRep(java.net.URI cacheKey)
    {
        return ImageRep.create(this,
                               ImageSource.create(cacheKey),
                               ICONS_ARE_DISPOSABLE);
    }
    

    private ImageRep createRuntimeScaledIconRep(final ImageRep full, final Image hardFullImageRef) {
        // could pass in something like Scaler with just a produceIcon method into the image source
        // for creating the icon, or a general FutureTask.

        // NOTE: if the icon is NOT created immediately, and we're in the middle of loading lots of
        // images, that hard image reference is going to stay around, held by the ImageSource in an
        // Images IconTask, in the the thread-pool task queue, unable to be GC'd, which will lead
        // to contention that's very difficult to recover from should we start running out of
        // memory.

        // Although an incompletely impl, Images.DELAYED_ICONS uses the full ImageRep into the
        // icon-source ImageSource instead of the hard image reference, and could attempt to
        // reconstitute it if it's been GC'd once the IconTask get's around to running.  That
        // implies a bunch more complexity to the code.  We're going with simpler and more reliable
        // for now.

        final ImageRep icon = ImageRep.create(this,
                                              ImageSource.createIconSource(_source, full, hardFullImageRef, DEFAULT_ICON_SIZE),
                                              ICONS_ARE_DISPOSABLE);

        // TODO: if the source image changes on disk, any icon needs to be re-generated

        // For ideal memory usage, we'd create the icon immediately in this thread, but we don't
        // actually want to do this: if there are multiple Ref's to the same content, they'll all
        // be in the listener-relay chain, but the FIRST one to get this callback is going to hang
        // up the rest of the thread while generating the icon if we request an immediate load, and
        // the down-relay ImageRef's, which could at least draw the full-rep while waiting, will be
        // waiting until the icon generation is done.  Furthermore: this will trigger callbacks
        // with icon data to nodes, THEN the backed up relay's will fire, making it look like the
        // full rep has arrived after the icon, which explains why when we tried this some of the
        // images in our repeats test are displaying the full image AFTER it's been generated, tho
        // that gets fixed on the first repaint after the updates.

        // The issue of generating icons sooner rather than later is how handled
        // in Images by giving higher priority to icon generating tasks than image loading tasks,
        // and by keeping a hard-ref to the image in the ImageSource.

        kickLoad(icon);

        return icon;
        
     }
    
    private void kickLoad(ImageRep rep) {
        //if (DEBUG.IMAGE && rep == _full) Log.debug("FULL REP LOAD " + rep, new Throwable("HERE"));
        if (DEBUG.IMAGE) debug(" kickLoad " + rep);
        if (!rep.loading()) {
            //if (DEBUG.IMAGE) debug(" kickLoad " + rep);
            final boolean waitingForCallback = rep.reconstitute();
            //if (waitingForCallback && rep == _full)
            if (waitingForCallback)
                _repainter.imageRefUpdate(KICKED);
        }
    }
//     private void requestImmediateLoad(ImageRep rep) {
//         if (DEBUG.IMAGE) debug("IMMEDIATE " + rep);
//         rep.reconstituteNow();
//     }

    void preLoadFullRep() {
        // TODO: would be better if we could somehow tag this as a low-priority task --
        // e.g., these pre-caching tasks should only ever consume a single thread (low
        // CPU usage, especially during a presentation), and if we transition to a
        // low-memory state, all outstanding pre-caches should be flushed.  And if we
        // get really fancy, we might be able to flush outstanding pre-caches if if the
        // content is no longer needed -- e.g., you're fast-paging through a
        // presentation in low-memory conditions, and you only need previews until you
        // settle on where you want to be.
        kickLoad(_full);
    }

    private void debug(String s) {
        Log.debug(String.format("%08x[%s] %s", System.identityHashCode(this), debugSRC(_source), s));
    }
    static String debugSRC(ImageSource s)
    {
        if (s == null)
            return "[null ImageSource]";
        else
            return s.debugName();
    }

    public boolean available() {
        return _icon.available() || _full.available();
    }

    public boolean hasError() {
        return _full.hasError();
    }

    public float aspect() {
        return _aspect;
    }

    public int[] fullPixelSize() {
        return _full.size();
    }

    public void reload() {
        _full = ImageRep.UNAVAILABLE;
        _icon = ImageRep.UNAVAILABLE;
        repaint();
    }
    
    @Override public boolean equals(Object o) {
        if (o instanceof ImageRef)
            return ((ImageRef)o)._source.original == _source.original;
        else
            return false;
    }
    
    @Override public String toString() {
        //return "ImageRef[full=" + fullRep() + "; icon=" + iconRep() + "; src=" + _source + "]";
        
        return String.format("ImageRef[full=%s icon=%s]", _full, _icon.handle());
        //return String.format("ImageRef[full: %s\n\ticon: %s\n\tsrc: %s]", _full, _icon, _source);
    }

}

// ALTERNATIVE DESIGN: Images.getImageRef returns an ImageRef, so that multiple
// LWImage's, preview panes, etc, are all pointing to the same ImageRef.  Which means
// the cache would always contain instances of ImageRef?  Tho that conflicts with the
// idea of keeping the cache code very clean & flat: e.g., only knows about 1 key, 1
// file, and the the icon cache file generation would get handled in ImageRef.  Tho
// actually that really doesn't need to be -- Images handles fetching and caching image
// data -- if it creates icons behind the scenes, that's an impl detail.

// If we instance the ImageRef ourselves, that is more amenable to subclassing and just
// overriding the update methods we'd like -- that could be very handy.  That would work
// here and in ResourceIcon, both which always work with a single image.  But
// PreviewPane works w/multiple images, so that means it needs the callbacks, tho the
// only once it really needs are gotImage and gotImageError.

// The big advantage to having the ImageRef doing the low-level listening to the image
// loader is that it can be one place that could handle a default common form of display
// update: e.g., while image is loading, draw as a transparent box w/loading %, display
// common unavail info, etc.  It would also probably want to take a java.awt.Component
// handle for being able to issue optional repaint() calls.  It would be one standard
// place to have all the info about raw size, loading status, etc.  It could also handle
// going between a Resource (if it has one) and the image data/image icon.

//----------------------------------------------------------------------------------------
// Note: subclassing java.awt.Image is not actually supported; in AWT Graphics drawing:
// SurfaceManager can't get surface.  see
// http://forums.sun.com/thread.jspa?threadID=5208043 -- basically, this is a 10+ year
// old bug that's never been fixed -- either java.awt.Image needs to be abstract or the
// impls need changing, and it looks like there's just no hope on this one at all.
// Subclassing BufferedImage MAY be possible tho...  this would lock us in 100% to never
// using ToolkitImage's, tho we can probably live with that as that impl gives us memory
// heartburn, at least on the Mac.  The bigger issue is we'd like to be able to rely on
// using createCompatibleImage for creating our BufferedImages, and not have to create
// our own delegating instances.

