package tufts.vue;

import tufts.Util;

import java.lang.ref.*;
import java.awt.Image;
import java.awt.Graphics2D;

import static tufts.vue.ImageRep.UNAVAILABLE;

public class ImageRef
{
    public static final int DEFAULT_ICON_SIZE = 128;
    public static final int[] ZERO_SIZE = ImageRep.ZERO_SIZE;

    //===================================================================================================

    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(ImageRef.class);
    private static final int PIXEL_THRESHOLD_FOR_ICON_GENERATION = (2*DEFAULT_ICON_SIZE) * (2*DEFAULT_ICON_SIZE);
    private static final int PIXEL_THRESHOLD_FOR_ICON_DRAWING = DEFAULT_ICON_SIZE*2;

    private static final boolean ICONS_ARE_DISPOSABLE = false; // todo: true case needs testing / may not work
    
    private static final String SIZE_FULL = "FULL-SIZE";
    private static final String SIZE_ICON = "ICON-SIZE";
    private static final String SIZE_UNKNOWN = "UNKNOWN-SIZE";
    
    private volatile ImageRep _full = ImageRep.UNAVAILABLE;
    private volatile ImageRep _icon = ImageRep.UNAVAILABLE;
    //private volatile Object _desired = SIZE_UNKNOWN;
    // _desired not used at moment -- would be easy to have one global instance of an ImageRef per image w/out it,
    // and could add back in this functionality by allowing a client to implement a simple recording API for desired
    // e.g., set/getDesired -- is only needed to prevent extra repaints when a new image rep arrives.
    
    private volatile ImageSource _source;

    private volatile float _aspect = 0;

    private Listener _repainter;
    
    public static interface Listener {
        public void imageRefChanged(Object cause);
    }

    public ImageRef(Listener listener) {
        _repainter = (Listener) listener;
    }
    
    public boolean isBlank() {
        return _source == null;
    }
    
    public void setImageSource(Object is) {
        //if (_source != null) throw new Error("ImageSource re-set not permitted: " + this);
        if (_source != is) {
            //-----------------------------------------------------------------------------
            // PROBLEM: if this is a local file, the URI cache key in the _source.key
            // will be null, meaning we can't later create an icon cache key from it.
            // Yet at the moment we're only seeing this as a problem if the local
            // file is missing -- so how is this being working in the regular case?
            //-----------------------------------------------------------------------------
            _source = ImageSource.create(is);
            //if (DEBUG.IMAGE) Log.debug("created image source " + _source + " from " + is);
            initReps();
        }
    }

    private void initReps() {

        if (DEBUG.IMAGE) debug("initReps");

        if (_icon != UNAVAILABLE || _full != UNAVAILABLE)
            throw new Error("re-init of reps");
        
        // first time through:
        //
        // (1) if there's an icon on disk, initiate it's loading.
        //
        //     (a) if there's an unloaded cache entry, initiate it's loading
        //
        //     (b) if there's a LOADED cache entry, init icon right away w/no callback
        //         This is what standard Images.getImage calls do -- but we need the
        //         icon cache key and a getImage call or ImageSource that understands
        //         that.
        //
        //     (c) If there's a loader (icon request is underway), hook up.
        //     ImageRep uses the standard Images.getImage API for this.
        //
        // (2) if there's no icon on disk, initial FULL load, and immediately after
        //     callback for full load, CREATE the icon (if the full image is big enough
        //     to warrant it) while the fully loaded image is availble in memory.  This
        //     is so that if memory runs low, we'll have the icon to use, and the full
        //     image can be GC'd.
        //
        // If it's not worth creating an icon for the full image, set this._icon to
        // UNAVAILABLE.  We immediately init the _full image ImageRep witht the
        // ImageSource.  If it's asked to draw, it will automatically start loading.

        // As icons only exist in the cache, we know we don't have an icon
        // created yet if there isn't at least a unloaded cache entry for the icon.

        final java.net.URI iconKey = _source.getIconKey(DEFAULT_ICON_SIZE);

        if (iconKey != null) {
            synchronized (Images.getCacheLock()) {
                // we do this in a cache-lock to ensure the cache entry can't
                // have been GC'd or changed from the time we check for it to the time
                // we kick a load for it.  Oh, wait -- we could dead-lock -- we want
                // to release the lock at the point getImage returns in ImageRep.reconstitute,
                // otherwise it's ImageRef callback, notifyRepHasArrived, will also
                // happen in the cache-lock, which is dangerous -- it will block
                // all other image processing threads till it returns.
                if (Images.hasCacheEntry(iconKey)) { // TODO: less special case? use same logic for loading this elsewhere? 
                    _icon = ImageRep.create(this,
                                            ImageSource.create(iconKey),
                                            ICONS_ARE_DISPOSABLE);
                    //Log.debug("found icon cache entry for " + iconKey);

                    // PRE-LOAD BY IMMEDIATELY REQUESTING A LOAD:
                    
                    // We could wait till it tries to draw just as _full does.  For now
                    // this is a mild form a pre-loading.
                    //kickLoad(_icon);
                }
            }
        }
        
//         else {
//             if (DEBUG.IMAGE) {
//                 if (iconKey == null) {
//                     Log.debug(Util.TERM_RED + "NO ICON KEY FOR " + _source + Util.TERM_CLEAR);
//                 } else {
//                     Log.debug("NO ICON CACHE ENTRY: " + iconKey);
//                 }
//             }
//             // leave _icon as UNAVAILABLE -- it will be generated and re-assigned once the full image loads.
//         }

        // rep won't load until it attempts to draw:
        _full = ImageRep.create(this, _source);
    }
    
//     public void drawInto(Graphics2D g, float width, float height)
//     {
//         // need to always try, as this is how we'll know if the 
//         // the full is ever desired (when an icon has been pre-loaded)
//         drawBestAvailable(g, width, height);
//     }
    
    public void drawInto(DrawContext dc, float width, float height)
    {
        if (dc.isAnimating() || dc.isDraftQuality()) {
            drawAvailable(dc.g, width, height);
        } else {
            drawBestAvailable(dc.g, width, height);
        }
    }

    private void drawAvailable(Graphics2D g, float width, float height) 
    {
        if (_icon.available())
            _icon.renderRep(g, width, height);
        else if (_full.available())
            _full.renderRep(g, width, height);
        else 
            ; //UNAVAILABLE.drawRep(g, width, height);
    }
    
    
    private static final java.awt.Color DebugRed = new java.awt.Color(255,0,0,128);
    private static final java.awt.Color DebugGreen = new java.awt.Color(0,255,0,128);
    private static final java.awt.Color DebugBlue = new java.awt.Color(0,0,255,128);
    private static final java.awt.Color DebugYellow = new java.awt.Color(255,255,0,128);
    
    private void drawBestAvailable(Graphics2D g, float width, float height)
    {
        final ImageRep backupRep;
        final ImageRep desiredRep;

        final double scale = g.getTransform().getScaleX();

        final int onScreenMaxDim;

        if (aspect() > 1f) {
            //debug("aspect="+aspect() + " picking width " + width);
            onScreenMaxDim = (int) (scale * width);
        } else {
            //debug("aspect="+aspect() + " picking height " + height);
            onScreenMaxDim = (int) (scale * height);
        }

        if (onScreenMaxDim <= PIXEL_THRESHOLD_FOR_ICON_DRAWING) {
            // todo: would be better to check actual iconRep size v.s. our constant,
            // tho this lets us not worry if it's been loaded or not
            desiredRep = _icon;
            backupRep = _full;
            //_desired = SIZE_ICON;

            
            //debug("onScreenMaxDim below thresh " + PIXEL_THRESHOLD_FOR_ICON_DRAWING + " at " + onScreenMaxDim);
        } else {
            //debug("onScreenMaxDim ABOVE thresh " + PIXEL_THRESHOLD_FOR_ICON_DRAWING + " at " + onScreenMaxDim);
            //_desired = SIZE_FULL;
            desiredRep = _full;
            backupRep = _icon;
        }

        final ImageRep drawRep;

        if (desiredRep.available()) {
            drawRep = desiredRep;
        } else {
            if (desiredRep != UNAVAILABLE) {
                //_desired = desiredRep; // most likely it has expired
                kickLoad(desiredRep);
            }

            //drawRep = backupRep;

            // TODO: COMMENT THE BELOW LOGIC MORE CLEARLY
            
            if (backupRep.available() || !desiredRep.loading()) { // if reps auto-load on draw, don't try if the desired rep is already loading
                // if backupRep not available, could handle the loading draw in ImageRef v.s. ImageRep
                drawRep = backupRep; // note: triggers auto-load: may want to control that in ImageRef instead
            }
            else if (_full.isTrackingProgress()) {
                drawRep = _full;
            } else
                drawRep = UNAVAILABLE;
        }

        drawRep.renderRep(g, width, height);

        if (DEBUG.BOXES) {
            final float hw = width / 2f;
            final float hh =  height / 2f;
            final java.awt.geom.Rectangle2D.Float r = new java.awt.geom.Rectangle2D.Float();
            if (drawRep == _icon) {
                // we're looking at the icon rep
                g.setColor(DebugYellow);
                r.setRect(0, 0, hw, hh);
                g.fill(r);
            }
            if (drawRep != desiredRep) {
                // we're waiting for a better rep
                g.setColor(DebugBlue);
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
    }

    private void kickLoad(ImageRep rep) {
        if (DEBUG.IMAGE) debug("kickLoad " + rep);
        rep.reconstitute();
    }
    private void requestImmediateLoad(ImageRep rep) {
        if (DEBUG.IMAGE) debug("IMMEDIATE-LOAD " + rep);
        rep.reconstituteNow();
    }

    public void notifyRepHasProgress(final ImageRep rep, final float pct) {
        //debug("progress " + pct);
        //_repainter.imageRefChanged("progress");
        repaint();
    }
    
//     public void notifyRepIsReplaced(ImageRep oldRep, ImageRep newRep)
//     {
//         // can load the aspect from the newRep, which should have size now
//         if (oldRep == _full) {
//             _full = newRep;
//         } else if (oldRep == _icon) {
//             _icon = newRep;
//         } else {
//             Log.error("lost rep-replacement:\n\told=" + oldRep + "\n\tnew=" + newRep, new Throwable("HERE"));
//         }
//     }
    
    /** the ImageRep is done loading -- it has all the renderable image data */
    public void notifyRepHasArrived(final ImageRep freshRep, final Image hardImageRef)
    {
        if (_aspect == 0 || freshRep == _full)
            _aspect = freshRep.aspect(); // the one place aspect is loaded

        //if (_desired == freshRep || _desired == SIZE_UNKNOWN)  // may be easiest/safest just to always repaint
            repaint();

        if (freshRep == _full && _icon == ImageRep.UNAVAILABLE) {
            // no icon was previously generated -- look to see if
            // one has been generated elsewhere in this runtime,
            // or if not, create it now.
            if (_full.area() > PIXEL_THRESHOLD_FOR_ICON_GENERATION) {
                _icon = createIconRep(freshRep, hardImageRef);
            } // else _icon left as ImageRep.UNAVAILABLE
             
        }
    }

    private ImageRep createIconRep(final ImageRep full, final Image hardFullImageRef) {
        // could pass in something like Scaler with just a produceIcon method into the image source
        // for creating the icon, or a general FutureTask.

        final ImageRep icon = ImageRep.create(this,
                                              ImageSource.createIconSource(_source, hardFullImageRef, DEFAULT_ICON_SIZE),
                                              ICONS_ARE_DISPOSABLE);
        //kickLoad(icon);
        requestImmediateLoad(icon); // TODO: should work to run us synchronously, but is NPE in Images somewhere

        //========================================================================================
        // OH, ALSO: still need to deal with contention -- if someone is ALREADY running
        // a synchronous load, we do NOT want to block, tho really what we want is the option
        // to do either.  The reason we want to do this immediately is so that we can
        // create the icon ASAP while the original image is still in memory, just in case
        // we're running low.  A fancier impl might allow icons to generate later, which
        // provides faster initial map painting under normal conditions (plenty of RAM),
        // but switches to the conservative method once any OutOfMemoryError is seen.
        //========================================================================================
        
        return icon;
        
     }
    
//         // issue a repaint for the full image, which will normally be fine till the icon
//         // comes in, except in cases of low memory where the full image may appear, then
//         // dissapear after a GC.  Technically, we only need to do this if we do NOT have
//         // an icon already displayed, and the full-rep is not currently desired.  But if
//         // we haven't generated an icon yet, that could take a while, so at least get
//         // this on screen before we start doing that.  As long as as the repaint happens
//         // before this thread is finished out, the full image will be guaranteed not to
//         // be GC'd as we're keeping a hard reference to in throughout this call-stack.
//         repaint();
//         // If this image is big enough to warrant generating a small
//         // icon, generate it now in the current thread while the image
//         // data is guaranteed to be around (in case we're running low
//         // on memory and the full image may be GC'd shortly).


    private void debug(String s) 
    {
        // todo: put this in the image source?
        String basename = _source.key.getPath();
        final int lastSlash = basename.lastIndexOf('/');
        if (lastSlash < (basename.length()-2))
            basename = basename.substring(lastSlash+1, basename.length());
        Log.debug(String.format("[%s] %s", basename, s));
    }

    private void repaint() {
        //debug("repaint");
        _repainter.imageRefChanged("repaint");
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
    
    @Override public String toString() {
        //return "ImageRef[full=" + fullRep() + "; icon=" + iconRep() + "; src=" + _source + "]";
        
        //return String.format("ImageRef[src=%s\n\ticon: %s\n\tfull: %s]", _source, _icon, _full);
        return String.format("ImageRef[full: %s\n\ticon: %s\n\tsrc: %s]", _full, _icon, _source);
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

