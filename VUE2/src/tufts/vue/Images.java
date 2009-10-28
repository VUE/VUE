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

import static tufts.vue.Resource.*;

import java.util.*;
import java.util.concurrent.*;
import java.lang.ref.*;
import java.net.URL;
import java.net.URI;
import java.net.URLConnection;
import java.io.*;
import java.awt.Image;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import javax.swing.ImageIcon;
import javax.imageio.*;
import javax.imageio.event.*;
import javax.imageio.stream.*;

/**
 *
 * Handle the loading of images in background threads, making callbacks to deliver
 * results to multiple listeners that can be added at any time during the image fetch,
 * and caching (memory and disk) with a URI key, using a HashMap with SoftReference's
 * for the BufferedImage's so if we run low on memory they just drop out of the cache.
 *
 * @version $Revision: 1.64 $ / $Date: 2009-10-28 04:58:07 $ / $Author: sfraize $
 * @author Scott Fraize
 */
public class Images
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(Images.class);

    private static final boolean ALLOW_HIGH_QUALITY_ICONS = true;
    
    public static VueAction ClearCacheAction = new VueAction("Empty Image Cache") {
            public void act() { Cache.clear(); }
        };
    
    private static final CacheMap Cache = new CacheMap();

    
    /**
     * Calls to Images.getImage must pass in a Listener to get results.
     * The first argument to all the callbacks is the original object
     * passed in to getImage as the imageSRC.
     */
    public interface Listener {
        /** If image is already cached, this will NOT be called -- is only called from an image loading thread. */
        void gotImageSize(Object imageSrc, int w, int h, long byteSize);
        /** If byte-tracking is enabled on the input source, this will be called periodically during loading */
        void gotImageProgress(Object imageSrc, long bytesSoFar, float percentSoFar);
        /** Will be called immediately in same thread if image cached, later in different thread if not. */
        //void gotImage(Object imageSrc, Image image, int w, int h);
        void gotImage(Object imageSrc, Image image, ImageRef ref); // todo: ImageRef arg not currently used
        /** If there is an exception or problem loading the image, this will be called */
        void gotImageError(Object imageSrc, String msg);
    }


    /**
     * Fetch the given image.  If it's cached, listener.gotImage is called back immediately
     * in the current thread.  If not, the image is fetched asynchronously, and the
     * callbacks are made later from a special image loading thread.
     *
     * @param imageSRC - anything that might be converted to an image: a Resource, File, URL, InputStream, etc.
     *
     * @param listener - the Images.Listener to callback.  If null, the result
     * of the call would be only to ensure the given image is cached.
     *
     * @return the Image if immediately available, null if the listener will be called back.
     **/
    
    public static Image getImage(Object imageSRC, Images.Listener listener)
    {
        return getImage(imageSRC, listener, false);
    }

    /**
     * If the requested content is not already loading,
     * it will be immediately loaded in the current thread.
     */
    public static Image getImageASAP(Object imageSRC, Images.Listener listener)
    {
        return getImage(imageSRC, listener, true);
    }
    
    /**
     * Fetch the given image.  If it's cached, listener.gotImage is called back immediately
     * in the current thread.  If not, the image is fetched asynchronously, and the
     * callbacks are made later from a special image loading thread.
     *
     * @param imageSRC - anything that might be converted to an image: a Resource, File, URL, InputStream, etc.
     *
     * @param listener - the Images.Listener to callback.  If null, the result
     * of the call would be only to ensure the given image is cached.
     *
     * @param immediate - if the requested content is not already loading,
     * it will be immediately loaded in the current thread.
     *
     * @return the Image if immediately available, null if the listener will be called back.
     **/
    private static Image getImage(Object imageSRC, Images.Listener listener, boolean immediate)
    {
        try {
            return getCachedOrCallbackOnLoad(imageSRC, listener, immediate);
        } catch (Throwable t) {
            if (DEBUG.IMAGE) tufts.Util.printStackTrace(t);
            if (listener != null)
                listener.gotImageError(imageSRC, t.toString());
        }
        return null;
    }
    
    
//     /** SYNCHRONOUSLY retrive an image from the given data source: e.g., a Resource, File, URL, InputStream, etc */
//     public static Image getImage(Object imageData)
//     {
//         try {
//             return getCachedOrCallbackOnLoad(imageData, null);
//         } catch (Throwable t) {
//             Log.error("getImage " + imageData, t);
//             return null;
//         }
//     }

    public static void loadDiskCache()
    {
        File dir = getCacheDirectory();
        if (dir == null)
            return;

        Log.debug("listing disk cache...");
        File[] files = dir.listFiles();
        Log.debug("listing disk cache: done; entries=" + files.length);
        
        synchronized (Cache) {
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                String name = file.getName();
                if (name.charAt(0) == '.')
                    continue;
                //out("found cache file " + file);
                URI key = null;
                try {
                    key = cacheFileNameToKey(name);
                    if (DEBUG.IMAGE && DEBUG.META) out("made cache key: " + key);
                    if (key != null)
                        Cache.put(key, new CacheEntry(null, file));
                } catch (Throwable t) {
                    Log.error("failed to load cache with [" + name + "]; key=" + key);
                }
            }
        }
    }
    
    /** @return the cache file for the given resource, or null if none exists */
    public static File findCacheFile(Resource r) {

        final ImageSource imageSRC = ImageSource.create(r);

        if (imageSRC.key != null) {
            final Object entry = Cache.get(imageSRC.key);
            if (entry instanceof CacheEntry) {
                return ((CacheEntry)entry).file;
            } else {
                Log.warn("Cache is loading, no cache file yet for " + r);
            }
        }
        Log.warn("Failed to find cache file for " + r);
        return null;
    }
    

    // todo: really, ImageCacheEntry v.s. Loader cache entries, tho they don't
    // currently have a common super-class.
    private static class CacheEntry {
        private final Reference<Image> ref;
        private final File file;
        // Loader loader;
        // todo: add loader here so we can always have CacheEntry's in the cache, and
        // so file is information always available, tho that could be extracted from the
        // Loader ImageSource? No -- that could be any file, not just a disk-cache entry.
        // This new complexity arising out of having icons in the cache -- we may just
        // want a separate icon cache.

        /** image should only be null for startup init with existing cache files */
        CacheEntry(Image image, File cacheFile)
        {
            if (image == null && cacheFile == null)
                throw new IllegalArgumentException("CacheEntry: at least one of image or file must be non null");
            if (image != null)
                this.ref = new SoftReference(image);
            else
                this.ref = null;
            this.file = cacheFile;
            if (DEBUG.IMAGE) out("new " + this);
        }

        boolean isPreloadedDiskEntry() {
            return ref == null;
        }

        Image getCachedImage() {

            // if don't even have a ref, this was for an init-time persistent cache file
            if (ref == null)
                return null;

            Image image = ref.get();
            // will be null if was cleared
            if (image == null) {
                if (DEBUG.Enabled && file != null) out("GC'd: " + file);
                return null;
            } else
                return image;
        }

        File getFile() {
            return file;
        }

        void clear() {
            if (ref != null)
                ref.clear();
        }

        public String toString() {
            return "CacheEntry[" + tag(getCachedImage()) + "; file=" + file + "]";
        }
    }


    /*
     * Not all HashMap methods covered: only safe to use
     * the ones explicity implemented here.
     */
    private static class CacheMap extends HashMap {

        public synchronized Object get(Object key) {
            return super.get(key);
        }
        
        public synchronized boolean containsKey(Object key) {
            return super.containsKey(key);
        }
        
        public synchronized Object put(Object key, Object value) {
            return super.put(key, value);
        }

        public synchronized Object remove(Object key) {
            return super.remove(key);
        }
        

        // for now, only clears memory cache
        public synchronized void clear() {
            final Iterator i = values().iterator();
            while (i.hasNext()) {
                Object entry = i.next();

                // may be a Loader: todo: may want to kill thread if it is Especially:
                // if we go off line, Loaders created immediately after that (or during)
                // tend to hang forever.  Loaders created once the OS knows we're
                // offline will usually fail immediately with "no route to host", but
                // even after going back online, and other images load, the originally
                // hung Loader's won't die...  So at least an image-cache should kill
                // them.
                
                if (entry instanceof CacheEntry) {
                    CacheEntry ce = (CacheEntry) entry;
                    ce.clear();
                    if (ce.getFile() == null)
                        i.remove();
                } else {
                    // Interrupt may not be good enough: if blocked on non-async IO
                    // (non-channel IO, e.g., "regular"), this can have no
                    // effect.  Turns out using stop doesn't help even in this
                    // case.
                    if (entry instanceof LoadThread) {
                        Log.info("STOPPING THREAD ENTRY " + entry);
                        ((LoadThread)entry).stop();
                    } else if (entry instanceof LoadTask) {
                        Log.warn("LEAVING TO RUN OUT TASK ENTRY " + entry);
                        // if it was a Future, we could attempt to de-queue it
                    }
                    //((LoadThread)entry).stop();
                    //((Loader)entry).interrupt();
                }
            }

            //super.clear();
        }
        
    }

    /**
     * flush any cache BufferedImages we have for the give file: future requests
     * will force the image data to be reloaded from the file (useful if we know
     * the file has changed on disk).
     */
    public static void flushCache(File file) {
        final Object key = makeKey(file);
        final Object entry = Cache.remove(key);
        if (entry != null) {
            Log.info(Util.TERM_RED + "flushed cache entry: " + key + "; " + entry + Util.TERM_CLEAR);
        } else {
            Log.info("failed to find cache entry for key: " + Util.tags(key));
        }
    }

//     private static URI makeKey(URL u) {
//         try {

//             if ("file".equals(u.getProtocol())) {

//                 return Resource.makeURI(u);
                
//             } else {
                
//                 return new URI(u.getProtocol(),
//                                u.getUserInfo(),
//                                u.getHost(),
//                                u.getPort(),
//                                //u.getAuthority(),
//                                u.getPath(),
//                                u.getQuery(),
//                                u.getRef()).normalize();
                
//             }
            
//         } catch (Throwable t) {
//             Util.printStackTrace(t, "can't make URI cache key from URL " + u);
//         }
//         return null;
//     }

    static URI makeKey(File file) {
        try {
            return file.toURI().normalize();
        } catch (Throwable t) {
            Util.printStackTrace(t, "can't make URI cache key from file " + file);
        }
        return null;
    }

    static String keyToCacheFileName(URI key)
    //throws java.io.UnsupportedEncodingException
    {
        try {
            //return key.toASCIIString();
            return java.net.URLEncoder.encode(key.toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.error("transforming key to cache-file name: " + key, e);
        }
        return null;
    }
    
    private static URI cacheFileNameToKey(String name) 
    {
        try {
            return new URI(java.net.URLDecoder.decode(name, "UTF-8"));
            //return new URL(java.net.URLDecoder.decode(name, "UTF-8"));
        } catch (Throwable t) {
            if (DEBUG.Enabled) tufts.Util.printStackTrace(t);
            return null;
        }
    }

    /**
     * Using a relay system, as opposed to say a list of listeners maintained by the
     * Loader, allows the image loading code to not care if there is a single listener
     * or multiple listeners, which is handy in the case where the result is cached and
     * we don't even create a loader: we just callback the listener immediately in the
     * same thread.  But when a Loader is created, it can create ListenerRelay's to
     * relay results down the chain, starting with it's special LoaderRelayer to relay
     * partial results to listeners added in the middle of an image fetch, and again,
     * the image loading code doesn't need to know about this: it just has a single
     * listener.
     *
     * Performance-wise, there is rarely ever more than a single relayer object
     * created (covering two listeners for the same image load).
     *
     * This is also a handy place for diagnostics.
     */
    private static class ListenerRelay implements Listener {
        protected final Listener head;
        protected Listener tail;
        
        ListenerRelay(Listener l0, Listener l1) {
            // if head is null, nobody is listening at the start, but listeners may be added later to tail
            this.head = l0;
            this.tail = l1;
        }
        ListenerRelay(Listener l0) {
            this(l0, null);
        }

        @Override public void gotImageSize(Object src, int w, int h, long bytes) {
            relaySize(head, src, w, h, bytes);
            relaySize(tail, src, w, h, bytes);
        }
        @Override public void gotImageProgress(Object src, long bytes, float pct) {
            relayProgress(head, src, bytes, pct);
            relayProgress(tail, src, bytes, pct);
        }
        @Override public void gotImage(Object src, Image image, ImageRef ref) {
            relayImage(head, src, image, ref);
            relayImage(tail, src, image, ref);
        }
        @Override public void gotImageError(Object src, String msg) {
            relayError(head, src, msg);
            relayError(tail, src, msg);
        }

        // todo: all these callbacks should be wrapped in try/catch
        private void relaySize(Listener l, Object src, int w, int h, long bytes) {
            if (l != null) {
                if (DEBUG.IMAGE && l instanceof ListenerRelay == false)
                    out("relay SIZE to " + tag(l));
                l.gotImageSize(src, w, h, bytes);
            }
        }
        private void relayProgress(Listener l, Object src, long bytes, float pct) {
            if (l != null) {
                if (DEBUG.IMAGE && DEBUG.META && l instanceof ListenerRelay == false)
                    out(String.format("relay PROGRESS %.2f %5d to %s", pct, bytes, tag(l)));
                l.gotImageProgress(src, bytes, pct);
            }
        }
        private void relayImage(Listener l, Object src, Image image, ImageRef ref) {
            if (l != null) {
                if (DEBUG.IMAGE && l instanceof ListenerRelay == false)
                    out(Util.TERM_CYAN + "relay IMAGE to " + tag(l) + Util.TERM_CLEAR);
                l.gotImage(src, image, ref);
            }
        }
        private void relayError(Listener l, Object src, String msg) {
            if (l != null) {
                if (DEBUG.IMAGE && l instanceof ListenerRelay == false)
                    out("relay ERROR to " + tag(head) + "; is=" + src);
                l.gotImageError(src, msg);
            }
        }

        boolean hasListener(Listener l) {
            if (head == l || tail == l)
                return true;
            else if (tail instanceof ListenerRelay)
                return ((ListenerRelay)tail).hasListener(l);
            else
                return false;
        }

    }

    /**
     * Track what's been delivered, to send to listeners that are added when
     * partial results have already been delivered.
     */
    private static class CachingRelayer extends ListenerRelay
    {
        private final ImageSource imageSRC;
        
        private Image image;
        private ImageRef ref;
        private int width = -1;
        private int height = -1;
        private long byteSize;
        private long bytesSoFar;
        private String errorMsg;
        
        CachingRelayer(ImageSource is, Listener firstListener) {
            super(firstListener, null);
            imageSRC = is;
        }

        @Override public void gotImageSize(Object imageSrc, int w, int h, long byteSize) {
            this.width = w;
            this.height = h;
            this.byteSize = byteSize;
            super.gotImageSize(imageSrc, w, h, byteSize);
        }

        @Override public void gotImageProgress(Object imageSrc, long bytesSoFar, float _p) {
            this.bytesSoFar = bytesSoFar;
            // incoming percent is empty -- we fill it here
            // Note that the underlying raw stream may send us byte progress reports before
            // the image size is known.  This is a bug -- the byteSize is known
            // before this and should be recored here.  The # of bytes read before
            // the image size is known is usually negligable tho.  For now we just don't report
            // the progress until we have a byteSize.
            if (byteSize > 0)
                super.gotImageProgress(imageSrc, bytesSoFar, percentProgress());
        }
        
        @Override public void gotImage(Object imageSrc, Image image, ImageRef ref) {
            this.image = image;
            this.ref = ref;
            super.gotImage(imageSrc, image, ref);

        }
        
        @Override public void gotImageError(Object imageSrc, String msg) {
            this.errorMsg = msg;
            super.gotImageError(imageSrc, msg);

        }

        private float percentProgress() {
//             // note: bytes can arrive from the raw stream before the im
//             final float x = (float) bytesSoFar / (float) byteSize;
//             Log.debug("bytesSoFar " + bytesSoFar + " / bytes " + byteSize + " = " + x);
            if (byteSize > 0)
                return (float) bytesSoFar / (float) byteSize;
            else
                return 0;
        }

        void addListener(Listener newListener)
        {
            synchronized (this) {
                if (hasListener(newListener)) {
                    if (DEBUG.Enabled) out("CachingRelayer: ALREADY A LISTENER FOR THIS IMAGE: " + tag(newListener));
                    return; 
                }
                if (tail == null)  {
                    tail = newListener;
                } else {
                    tail = new ListenerRelay(tail, newListener);
                }
            }

            // Note: WE CAN DEADLOCK if deliverPartialResults is in the sync.  E.g. --
            // trying moving around raw images as they're loading / icon generating.
            
            // TODO: this really should be in the sync, as well as all the above
            // Listener API methods -- normally we'll be in the AWT thread, and if a
            // loader modifies the partial results while this call is being made, they
            // may be incoherent, and not all partial data may delivered.  The lock is
            // inherently dangerous tho, in that anything may generally happen during
            // client code callbacks, including calls back into this API.
            
            // Deliver any results we've already got.  It's possible for this to happen
            // even after we have all our results, if the image completed between the
            // time we found the Loader in the cache, and the time the requestor was
            // added as a listener.
            
            deliverPartialResults(newListener);

        }
        
        private void deliverPartialResults(Listener l)
        {
            if (DEBUG.IMAGE) out("DELIVERING PARTIAL RESULTS TO: " + tag(l));
            
            if (width > 0)
                l.gotImageSize(imageSRC.original, width, height, byteSize);

            if (bytesSoFar > 0)
                l.gotImageProgress(imageSRC.original, bytesSoFar, percentProgress());

            if (image != null)
                l.gotImage(imageSRC.original, image, ref);

            if (errorMsg != null) 
                l.gotImageError(imageSRC.original, errorMsg);

            if (DEBUG.IMAGE) out(" DELIVERED PARTIAL RESULTS TO: " + tag(l));
            
        }

    }
    
//     private static Image getCachedOrCallbackOnLoad(Object imageSource, Images.Listener listener)
//         throws java.io.IOException, java.lang.InterruptedException
//     {
//         return getCachedOrCallbackOnLoad(imageSource, listener, false);
//     }

    /**
     * @return Image if cached or listener is null, otherwise makes callbacks to the listener from
     * a new thread.
     */
    private static Image getCachedOrCallbackOnLoad
        (final Object imageSource,
         final Images.Listener listener,
         final boolean immediate)
        throws java.io.IOException, java.lang.InterruptedException
    {
        if (imageSource instanceof Image) {
            if (DEBUG.Enabled) Log.info("image source was an instance of Image", new Throwable(Util.tags(imageSource)));
            final Image image = (Image) imageSource;
            final int w, h;
            if (image instanceof BufferedImage) {
                BufferedImage bi = (BufferedImage) imageSource;
                w = bi.getWidth();
                h = bi.getHeight();
            } else {
                w = image.getWidth(null);
                h = image.getHeight(null);
            }
            if (listener != null)
                listener.gotImage(imageSource, // same as image
                                  image,
                                  null);
            return image;
        }
        
        final ImageSource imageSRC = ImageSource.create(imageSource);

        //if (DEBUG.IMAGE) System.out.println("-------------------------------------------------------");

        //Log.debug("fetching image source " + imageSRC + " for " + tag(listener));
        //if (DEBUG.Enabled) Log.debug("fetching " + imageSRC + " for listener " + Util.tag(listener));
        if (DEBUG.IMAGE) Log.debug("fetching for listener " + Util.tag(listener) + " " + imageSRC);

        final Object cacheEntry = getCachedOrKickLoad(imageSRC, listener, immediate);

        // TODO: if nothing was found in the cache and an ICON for the
        // given source exists in the cache, return that.  Either that
        // or change all Images callers to request an ImageRef, which
        // will handle that for us.
        
        if (cacheEntry == IMAGE_LOADER_STARTED) {
            // if this is the START of a new load, the caller has already been attached
            // as a listener, and the results will be coming via callback.
            return null;
        }
            

        Image cachedImage = null;
        if (cacheEntry instanceof Loader) {

            // Another request has already put a Loader into the cache -- have
            // the listener observe the existing loader.
            
            final Loader loader = (Loader) cacheEntry;

            if (listener != null) {
                //if (DEBUG.IMAGE) out("Adding us as listener to existing Loader");
                loader.addListener(listener);
                return null;
                
            } else {
                
                // We had no listener, so run synchronous & wait on existing loader thread to die:
                // We can't have a cache-lock when we do this.
                
                // NOTE: this is dangerous -- multiple non-listener requests for the
                // same loading content will quickly have all image processing threads
                // hung waiting on the same load -- do we still need this?  Generally
                // speaking, image requests w/out listeners are not safe.  For now,
                // we'll only allow this if this was an immediate requrest.

                if (!immediate) {
                    // No listener, no cache entry, no result.  Caller simply
                    // gets null and doesn't know why, even tho the image
                    // may now be loading.
                    Log.warn("no listener for non-cached content: caller in the dark as to the presence of image content: " + imageSRC);
                    return null;
                }

                Log.info("Joining " + tag(loader) + "...");
                loader.join();
                Log.info("Join of " + tag(loader) + " completed, cache has filled.");
                
                // Note we get here only in one rare case: there was an entry in the
                // cache that was already loading on another thread, and somebody new
                // requested the image that did NOT have a listener, so we joined the
                // existing thread and waited for it to finish (with no listener, we
                // have to run synchronous).
                
                // So now that we've waited, we should be guaranteed to have a full
                // Image result in the cache at this point.
                
                // Note: theoretically, the GC could have cleared our SoftReference
                // betwen loading the cache and now.  We can't lock the cache while
                // we're waiting on the join tho.
                
                cachedImage = ((CacheEntry)Cache.get(imageSRC.key)).getCachedImage();
                if (cachedImage == null)
                    Log.warn("Zealous GC: image tossed immediately " + imageSRC);
            }
        }
        else if (cacheEntry instanceof Image) {
            cachedImage = (Image) cacheEntry;
        } else {
            //if (listener != null)
                Log.warn("Unhandled cache entry: " + Util.tags(cacheEntry), new Throwable("HERE"));
            // if we have no listener, it's reasonable for the getCachedOrKickLoad to return null,
            // tho wouldn't it be better just to return one anyway, and allow quiet async caching?
            // later requests could still hook up to the loader
        }

        if (cachedImage != null) {
            if (listener != null) {
                // immediately callback the listener with the result
                listener.gotImage(imageSRC.original,
                                  cachedImage,
                                  null);

            }
            //return cachedImage;
        }

        return cachedImage;
        
        
// [no longer needed: request can be immediate]        
//         // We had no image, and no Loader was started: this should
//         // only happen if there was no listener for the Loader, tho we
//         // allow the sync load to go ahead just in case.  (Could get
//         // here due to an over-zealous GC).

//         if (listener != null)
//             Log.warn("had a listener, but no Loader created: backup synchronous loading for " + imageSRC);

//         if (DEBUG.IMAGE) out("synchronous load of " + imageSRC);
        
//         // load the image and don't return until we have it
//         return loadImageAndCache(imageSRC, listener);
    }
    

    private static final Object IMAGE_LOADER_STARTED = "<image-loader-created>";
    
    private static final ExecutorService PoolForMinimallyBlockingTasks;
    
    private static final int ImageThreadPriority;

    private static final ThreadFactory ImageThreadFactory = new ThreadFactory() {
            private int count = 0;
            public Thread newThread(Runnable r) {
                final Thread it = new Thread(r);
                try {
                    it.setPriority(ImageThreadPriority);
                } catch (Throwable t) {
                    Log.warn("failed to set image thread priority to " + ImageThreadPriority, t);
                }
                it.setName("imageProcessor-" + (++count));
                if (DEBUG.Enabled) Log.debug("created thread: " + it);
                return it;
            }
        };

    static {
        final int cores = Runtime.getRuntime().availableProcessors();
        final int useCores = cores;
        // rough test: on a 2-core laptop, our use-case came in at 1min v.s. 1:30min w/all cores in use
        // (all icons being generated)

        if (useCores > 1)
            PoolForMinimallyBlockingTasks = Executors.newFixedThreadPool(useCores, ImageThreadFactory);
        else
            PoolForMinimallyBlockingTasks = Executors.newSingleThreadExecutor(ImageThreadFactory);

        Log.debug("CREATED THREAD POOL: " + Util.tags(PoolForMinimallyBlockingTasks) + "; size=" + useCores);

        int priority = 1; // should be lowest

        try {
            priority = Thread.MIN_PRIORITY + Thread.NORM_PRIORITY / 2; 
            // in case NORM_PRIORITY access fails -- there was some case of this happening in Applets?
        } catch (Throwable t) {}

        ImageThreadPriority = priority;
        
    }
    
    static abstract class Loader implements Runnable
    {
        final CachingRelayer relay;
        final ImageSource imageSRC; 

        Loader(ImageSource _imageSRC, Listener firstRelay) 
        {
            imageSRC = _imageSRC;
            relay = new CachingRelayer(imageSRC, firstRelay);
            if (firstRelay == null)
                Log.info(this + "; nobody currently listening: image may be quietly cached: " + imageSRC);
        }

        public void join() throws InterruptedException {
            if (DEBUG.Enabled) Log.debug(this + " has been joined...");
            Thread.currentThread().join();
        }

        public final void run() {
            runToResult();
        }

        /** @return an Image that's already been put into the cache */
        private final Image runToResult() {
            if (DEBUG.IMAGE) debugMark(">>>>>");
            final Image i = produceResult();
            if (DEBUG.IMAGE) debugMark("<<<<<");
            return i;
        }

        private void debugMark(String s) {
            Log.debug(Util.TERM_YELLOW
                      + s + "---"
                      + getClass().getSimpleName()
                      + "-----------------------------------------------------------------------------"
                      + Util.TERM_CLEAR
                      );
        }

        /** @return an Image that's already been put into the cache */
        Image produceResult() {
            if (DEBUG.IMAGE || DEBUG.THREAD) out("loadAndCache kick: " + imageSRC);
            Image i = loadImageAndCache(imageSRC, relay);
            if (DEBUG.IMAGE || DEBUG.THREAD) out("loadAndCache return: " + tag(i));
            return i;
        }
        
        public void addListener(Listener newListener) {
            if (DEBUG.IMAGE) out(this + " addListener " + tag(newListener));
            relay.addListener(newListener);
        }

        @Override public String toString() {
            return String.format("%s[%s head=%s]", getClass().getSimpleName(), imageSRC, relay.head);
        }

    }

    /** a marker class to differentiate from LoadThread */
    static final class LoadTask extends Loader {
        LoadTask(ImageSource is, Listener relay) {
            super(is, relay);
        }
    }

    /** a task to generate an icon */
    static final class IconTask extends Loader {
        IconTask(ImageSource is, Listener relay) {
            super(is, relay);
        }
        @Override Image produceResult() {
            //if (DEBUG.IMAGE || DEBUG.THREAD) out("ICON-TASK for " + imageSRC + " kicked off");

            // At this point, there's a cache entry containing this IconTask, so no
            // other icon requestor in another thread will attempt to create an icon --
            // it will simply be chained up to the results of this Loader waiting for
            // callback.
            return createAndCacheIcon(relay, imageSRC);
        }
    }

    
    /**
     * A thread for loading a single image.  Images.Listener results are delievered
     * from this thread (unless the image was already cached).
     */
    static final class LoadThread extends Loader {
        private static int LoaderCount = 0;
        private final Thread thread;

        /**
         * @param src must be any valid src *except* a Resource
         * @param resource - if this is tied to a resource to update with meta-data after loading
         */
        LoadThread(ImageSource imageSRC, Listener firstRelay) {
            super(imageSRC, firstRelay);
            thread = new Thread(this, String.format("ImgLoader-%02d", LoaderCount++));
            thread.setDaemon(true);
            thread.setPriority(Thread.MIN_PRIORITY);
        }

        public void join() throws InterruptedException {
            thread.join();
        }
        public void start() {
            //if (DEBUG.IMAGE) Log.debug("=======================================================");
            thread.start();
        }        
        public void stop() {
            thread.stop();
        }        
    }
    
//     private static interface Loader extends Runnable {
//         void addListener(Listener l);
//         void join() throws InterruptedException;
//     }

//     static final class LoadTask implements Loader 
//     {
//         final CachingRelayer relay;
//         final ImageSource imageSRC; 

//         LoadTask(ImageSource _imageSRC, Listener firstRelay) 
//         {
//             imageSRC = _imageSRC;
//             relay = new CachingRelayer(imageSRC, firstRelay);
//         }

//         public void join() throws InterruptedException {
//             if (DEBUG.Enabled) Log.debug(this + " has been joined...");
//             Thread.currentThread().join();
//         }

//         public void run() {
//             //Log.debug("loadImage in " + Thread.currentThread());
//             if (DEBUG.IMAGE || DEBUG.THREAD) out("Loader: load " + imageSRC + " kicked off");
//             Object o = loadImage(imageSRC, relay);
//             if (DEBUG.IMAGE || DEBUG.THREAD) out("Loader: load returned, result=" + tag(o));
//         }
//         //public abstract void run();
        
//         public void addListener(Listener newListener) {
//             relay.addListener(newListener);
//         }

//     }
    

//     /**
//      * A thread for loading a single image.  Images.Listener results are delievered
//      * from this thread (unless the image was already cached).
//      */
//     static class LoadThread extends Thread implements Loader {
//         private static int LoaderCount = 0;
//         private final ImageSource imageSRC; 
//         private final CachingRelayer relay;

//         /**
//          * @param src must be any valid src *except* a Resource
//          * @param resource - if this is tied to a resource to update with meta-data after loading
//          */
//         LoadThread(ImageSource imageSRC, Listener l) {
//             //super(String.format("IL-%02d %s", LoaderCount++, imageSRC));
//             super(String.format("ImgLoader-%02d", LoaderCount++));
//             if (l == null)
//                 Log.warn(this + "; nobody listening: image will be quietly cached: " + imageSRC);
//             this.imageSRC = imageSRC;
//             this.relay = new CachingRelayer(imageSRC, l);
//             setDaemon(true);
//             setPriority(Thread.MIN_PRIORITY);
//             //setPriority(Thread.currentThread().getPriority() - 2);
//         }

//         public void run() {
//             if (DEBUG.IMAGE || DEBUG.THREAD) out("Loader: load " + imageSRC + " kicked off");
//             Object o = loadImage(imageSRC, relay);
//             if (DEBUG.IMAGE || DEBUG.THREAD) out("Loader: load returned, result=" + tag(o));
//         }

//         public void addListener(Listener newListener) {
//             relay.addListener(newListener);
//         }
//     }


    /**
     * This method has side effects to the cache.  At the end of this call, we know
     * there is *something* in the cache for the given imageSRC.key -- either we found
     * the image already there, or we found a Loader thread already started there, or we
     * put a new Loader into the cache and kicked it off, unless it was an immediate request,
     * (an anusual but supported case) in which case first the Loader will have been put in the
     * cache, and then the final result before we return.  
     *
     * This method also deals with cache cleanup: if an entry is found to be
     * empty (it has no disk file, and it's image has been GC'd), the entry
     * is removed.
     *
     * @return If an Image, we had the image in the cache immediately availble.  If a
     * Loader thread object, the image is loading, and we should become and additional
     * listener (if there is a listener given), or wait for the Loader to die to get
     * it's results.  If the special value IMAGE_LOADER_STARTED is returned, there is
     * nothing to do be done for now -- just wait for the callbacks to the listener if
     * one was provided.
     */
    private static Object getCachedOrKickLoad(ImageSource imageSRC, Images.Listener listener, boolean immediate)
    {
        Loader loader = null;

        synchronized (Cache) {

            final Object entry = getCacheContentsWithAutoFlush(imageSRC);

            // if anything in the Cache, immediately return it.  Could
            // be the desired image, or an existing loader.

            if (entry != null)
                return entry;
        
            // Nothing was in the cache.  If we have a listener, handle
            // the image loading in another thread.  If we don't, return
            // null -- caller must handle that condition.
        
            if (imageSRC.key == null) Util.printStackTrace("attempting to load cache w/null key: " + imageSRC);
        
            // It's okay if listener is null: a CachingRelayer will still be created, in
            // the Loader, and listeners can be added later.

            // note: if imageSRC is a file, and it doesn't exist, we could fail-fast
            // (perhaps if immediate is requested?), rather than wait for the error to
            // be reported via Listener.gotImageError, tho that's the standard API for
            // now.
        
            if (imageSRC.mayBlockIndefinitely()) {
                loader = new LoadThread(imageSRC, listener);
            } else if (imageSRC.isImageSourceForIcon()) {
                loader = new IconTask(imageSRC, listener);
            } else {
                loader = new LoadTask(imageSRC, listener);
            }
            
            // Note that even if we've been requested to run synchronously, we still want
            // a Loader created that can have listeners added later, and have it marked in the
            // cache, in case subsequent asynchronous requests come in for this image, or
            // even future immediate requests, which then won't be honored: they'll get a
            // callback when the previous immediate load finishes.
            
            markCacheAsLoading(imageSRC.key, loader);

        }
        //-------------------------------------------------------
        // CACHE LOCK IS RELEASED
        //-------------------------------------------------------

        if (listener == null || immediate) {
            // It's crucial that this NOT be run in a Cache-lock, or
            // every other image thread will soon hang until this is
            // done, including the AWT thread if anything requests
            // image data.

            // runToResult should always return an image that's been loaded into the cache
            return loader.runToResult();
            
        } else {
            kickTask(loader);
            return IMAGE_LOADER_STARTED;
        }
    }

    private static void markCacheAsLoading(URI key, Loader loader) {
        final Object old = Cache.put(key, loader); // todo: under what conditiions is this normal?
        if (old != null && old instanceof CacheEntry && !((CacheEntry)old).isPreloadedDiskEntry()) {
            Log.warn("blew away existing cache content:\n\told: " + Util.tags(old) + "\n\tfor: " + Util.tags(loader));
        }
    }

    private static void kickTask(Loader loader)
    {
        if (loader instanceof LoadThread) {
            // could submit to a special pool or some future fancy non-blocking NIO multi-stream handler
            ((LoadThread)loader).start();
        } else {
            PoolForMinimallyBlockingTasks.submit(loader);
        }
    }

    /**
     * Get the cache contents for the given source (either an Image or
     * a Loader), and update the cache entry if needed. This should
     * only be called from within a Cache lock.  Will return null if
     * no cache entry was found, or one was found but it's contents
     * had been garbage collected.  In the latter case, the empty
     * entry will be flushed from the cache.
     */
    private static Object getCacheContentsWithAutoFlush(ImageSource imageSRC)
    {
        if (imageSRC.key == null)
            return null;
        
        final Object entry = Cache.get(imageSRC.key);

        if (entry == null)
            return null;
        
        if (DEBUG.IMAGE) out("found cache entry for key " + tag(imageSRC.key) + ": " + entry);
                
        if (entry instanceof Loader) {
            if (DEBUG.IMAGE) out("Image is loading into the cache via already existing Loader...");
            return entry;
        }

        // Entry is not a Loader, so it must be a regular CacheEntry
        // We still may not have an image tho: it may have be been
        // garbage collected, or the entry may actually be for a file
        // on disk.

        final CacheEntry ce = (CacheEntry) entry;
        final Image cachedImage = ce.getCachedImage();
                     	
        // if we have the image, we're done (it was loaded this runtime, and not GC'd)
        // if not, either it was GC'd, or it's a cache file entry from the persistent
        // cache -- in either case, there is a file on disk -- mark it in the imageSRC,
        // and the loader will notice it and use it.
                    
        boolean emptyEntry = true;

        if (cachedImage != null) {
            emptyEntry = false;
        } else if (ce.getFile() != null) {
            if (ce.file.canRead()) {
                imageSRC.setCacheFile(ce.file); // Note: imageSRC side effect
                emptyEntry = false;
            } else
                Log.warn("cache file no longer available: " + ce.file);
        }

        if (emptyEntry) {
            // there is a cache entry with no image OR file: this could only
            // happen if the disk cache is not operating, and the memory
            // image was garbage collected: we need to remove this entry
            // from the cache completely and start from scratch:
            if (DEBUG.Enabled) out("REMOVING FROM CACHE: " + imageSRC);
            Cache.remove(imageSRC.key);
        }

        return cachedImage;

        // If cachedImage is null at this point, there was an entry in the cache, but it was of no use:
            
        // That happens in the following cases:
        // (1) We had the image, but is was GC'd -- we're going back to the cache file
        // (2) We had the image, but is was GC'd -- original was on disk: go back to that
        // (3) We had the image, but is was GC'd, and disk cache not working: reload original from network
        // (4) We never had the image, but it is in disk cache: go get it
        // (5) unlikely case case of zealous GC: reload original

        // Note that original image sources that were on disk are NOT moved
        // to the disk cache, and CacheEntry.file should always be null for those.
    }

    private static final String NO_READABLE_FOUND = "No Readable";


    /** @return true if there's a cache entry for the given key.  The entry may be in any state:
     * an unloaded pre-registered disk cache entry, a loaded cache entry, or a loader in progress.
     */
    public static boolean hasCacheEntry(URI cacheKey)
    {
        return cacheKey != null && Cache.get(cacheKey) != null;
    }
    
    private static Image createAndCacheIcon(Listener listener, ImageSource iconSource)
    {
        final Image iconImage =
            createIcon((Image) iconSource.readable,
                       iconSource.iconSize);

        iconSource.readable = null; // lose hard reference to the original image so it can be GC'd

        if (listener != null)
            listener.gotImage(iconSource, iconImage, null);

        File cacheFile = null;
        try {
            cacheFile = makePermanentCacheFile(iconSource.key);
        } catch (Throwable t) {
            Log.error("creating cache file for " + iconSource, t);
        }
        // if for any reason the disk cache has failed, we can still create the CacheEntry with a null file
        Cache.put(iconSource.key, new CacheEntry(iconImage, cacheFile));
        if (cacheFile != null)
            cacheIconToDisk(iconSource.key, (RenderedImage) iconImage, cacheFile);
        return iconImage;
    }
    
    private static boolean cacheIconToDisk(URI iconKey, RenderedImage image, File cacheFile)
    {
        try {
            //File cacheFile = makePermanentCacheFile(iconKey);
            if (DEBUG.IMAGE||DEBUG.IO) Log.debug("writing " + cacheFile);
            ImageIO.write(image, "png", cacheFile);
            if (DEBUG.IMAGE||DEBUG.IO) Log.debug("  wrote " + cacheFile);
            return true;
        } catch (Throwable t) {
            Log.error("writing icon cache file " + iconKey, t);
            return false;
        }
    }

    public static Object getCacheLock() {
        return Cache;
    }
    

    static class ImageException extends Exception { ImageException(String s) { super(s); }}
    static class DataException extends ImageException { DataException(String s) { super(s); }}

    public static final String OUT_OF_MEMORY = "Out of memory";

    
    /** An wrapper for readAndCreateImage that deals with exceptions, and puts successful results in the cache */
    private static Image loadImageAndCache(ImageSource imageSRC, Images.Listener listener)
    {
        Image image = null;

        if (imageSRC.resource != null)
            imageSRC.resource.getProperties().holdChanges();

        try {
            image = readImageInAvailableMemory(imageSRC, listener);
        } catch (Throwable t) {
            
            if (DEBUG.IMAGE) Util.printStackTrace(t);

            Cache.remove(imageSRC.key);
            
            if (listener != null) {
                String msg;
                boolean dumpTrace = false;
                if (t instanceof java.io.FileNotFoundException) {
                    msg = "Not Found: " + t.getMessage();
                }
                else if (t instanceof java.net.UnknownHostException) {
                    msg = "Unknown Host: " + t.getMessage();
                }
                else if (t instanceof java.lang.IllegalArgumentException && t.getMessage().startsWith("LUT has improper length")) {
                    // known java bug: many small PNG images fail to read (effects thumbshots)
                    msg = null; // don't bother to report an error
                }
                else if (t instanceof OutOfMemoryError) {
                    msg = OUT_OF_MEMORY;
                }
                else if (t instanceof ImageException) {
                    msg = (t.getMessage() == NO_READABLE_FOUND ? null : t.getMessage());
                }
                else if (t instanceof ThreadDeath) {
                    msg = "interrupted";
                }
                else if (t.getMessage() != null && t.getMessage().length() > 0) {
                    msg = t.getMessage();
                    dumpTrace = true;
                }
                else {
                    msg = t.toString();
                    dumpTrace = true;
                }

                // this is the one place we deliver caught exceptions
                // during image loading:
                listener.gotImageError(imageSRC.original, msg);

                if (dumpTrace)
                    Log.warn(imageSRC + ":", t);
                else
                    Log.warn(imageSRC + ": " + t);
            }

            if (imageSRC.resource != null)
                imageSRC.resource.getProperties().releaseChanges();
        }

        if (listener != null && image != null) {
            listener.gotImage(imageSRC.original,
                              image,
                              null);
        }
        
        // TODO opt: if this items was loaded from the disk cache, we're needlessly
        // replacing the existing CacheEntry with a new one, instead of
        // updating the old with the new in-memory image buffer.

        if (imageSRC.useCacheFile()) {
            if (image != null) {
                File permanentCacheFile = null;
                if (imageSRC.hasCacheFile())
                    permanentCacheFile = ensurePermanentCacheFile(imageSRC.getCacheFile());
                
                if (DEBUG.IMAGE) out("getting cache lock for storing result; " + imageSRC);
                Cache.put(imageSRC.key, new CacheEntry(image, permanentCacheFile));
                
                // If the cache file has moved from tmp to permanent, we'd need to do this to keep imageSRC
                // current, tho at the moment this is a bit overkill as we should no longer need imageSRC
                // at this point, but just in case it wants to update something such as a Resource with
                // a reference to the right cache file, we do this here.
                if (permanentCacheFile != null)
                    imageSRC.setCacheFile(permanentCacheFile);
                
//                 if (imageSRC.resource != null && permanentCacheFile != null)
//                     imageSRC.resource.setCacheFile(permanentCacheFile);
            }
        } else {
            if (image != null)
                Cache.put(imageSRC.key, new CacheEntry(image, null));
        }

        return image;
        
    }

    private static Image readImageInAvailableMemory(ImageSource imageSRC, Listener listener)
        throws ImageException
    {
        Image image = null;
        
        do {
            try {
                image = readAndCreateImage(imageSRC, listener);
            } catch (OutOfMemoryError eom) {
                Log.warn(Util.TERM_YELLOW + "out of memory reading " + imageSRC.readable + Util.TERM_CLEAR);
//                 if (Thread.currentThread() instanceof LoadThread) {
//                     Log.info("reader sleeping & retrying...");
//                     try {
//                         // Todo: sleep until all other image load threads have finished, or,
//                         // ideally, until there aren't any running (those still running are
//                         // blocked).  May actually just be easiest to to abort and re-submit this
//                         // to entirely new execution queue, or could just stick it on the end of
//                         // the existing queue, tho if this is a network IO thread, we still want to
//                         // run it later in it's own thread, so it could just be a task the
//                         // re-creates a new LoadThread
//                         Thread.currentThread().sleep(1000 * 45);
//                     } catch (InterruptedException e) {
//                         Log.warn("reader interrupted");
//                     }
//                     Log.info(Util.TERM_GREEN + "reader re-awakened, retrying... " + imageSRC.readable + Util.TERM_CLEAR);
//                 } else {
                    // we must be running in the thread-pool accessing local disk
                    Log.info("EOM; consider re-kicking a LoadTask for " + imageSRC);
                    //kickLoadTask(imageSRC, listener); // todo: test
                    throw eom;
//                }
            } catch (Throwable t) {
                throw new ImageException("reader failed: " + t.toString());
            }
        } while (image == null);

        if (DEBUG.IMAGE) out("ImageReader.read(0) got " + Util.tags(image));

        return image;
    }
    

    // todo: this probably wants to move to a resource impl class
    private static void setDateValue(Resource r, String name, long value) {
        if (value > 0)
            r.setProperty(name, new java.util.Date(value).toString());
        // todo: set raw value for compares, but allow prop displayer to convert it?
        // or put a raw Date object in there?
    }

    // todo: this probably wants to move to a resource impl class
    private static void setResourceMetaData(Resource r, java.net.URLConnection uc) {

//         r.getProperties().holdChanges();
//         try {
        
            long len = uc.getContentLength();
            //r.setProperty("url.contentLength", len);
            r.setProperty(CONTENT_SIZE, len); 
            r.setByteSize(len); // todo: update later from cache file size for correctness
            String ct = uc.getContentType();
            //r.setProperty("url.contentType", ct);
            r.setProperty(CONTENT_TYPE, ct);
            if (DEBUG.Enabled && ct != null && !ct.toLowerCase().startsWith("image")) {
                Log.warn("NON-IMAGE CONTENT TYPE [" + ct + "]; " + r);
            }
            setDateValue(r, "URL.expires", uc.getExpiration());
            //setDateValue(r, "url.date", uc.getDate());
            setDateValue(r, CONTENT_ASOF, uc.getDate()); // should probably ignore this an generate ourselves
            //setDateValue(r, "url.lastModified", uc.getLastModified());
            setDateValue(r, CONTENT_MODIFIED, uc.getLastModified());
            
//         } catch (Throwable t) {
//             Util.printStackTrace(t);
//         } finally {
//             r.getProperties().releaseChanges();
//         }
                      
    }
    
    // todo: this probably wants to move to a resource impl class
    private static void setResourceMetaData(Resource r, File f) {
//         r.getProperties().holdChanges();
//         try {
        
            //r.setProperty("file.size", f.length());
            r.setProperty(CONTENT_SIZE, f.length());
            //setDateValue(r, "file.lastModified", f.lastModified());
            setDateValue(r, CONTENT_MODIFIED, f.lastModified());

            //r.setProperty(CONTENT_TYPE, java.net.URLConnection.guessContentTypeFromName(f.getName()));
            // todo: also URLConnection.guessContentTypeFromStream (could use in FileBackedImageInputStream)
                
//         } finally {
//             r.getProperties().releaseChanges();
//         }
            
    }

    private static File makePermanentCacheFile(URI key)
        throws java.io.UnsupportedEncodingException
    {
        return makeCacheFile(key, false);
    }
        
    private static File makeTmpCacheFile(URI key)
        throws java.io.UnsupportedEncodingException
    {
        return makeCacheFile(key, true);
    }
    
    /** @param temporary -- for temporary cache files that have yet to complete (e.g., not all data has arrived)  */
    private static File makeCacheFile(URI key, boolean temporary)
        throws java.io.UnsupportedEncodingException
    {
        final String cacheName;
        if (temporary)
            cacheName = "." + keyToCacheFileName(key);
        else
            cacheName = keyToCacheFileName(key);
        
        File cacheDir = getCacheDirectory();
        File file = null;
        if (cacheDir != null) {
            file = new File(getCacheDirectory(), cacheName);
            try {
                if (!file.createNewFile()) {
                    if (DEBUG.IO) Log.debug("cache file already exists: " + file);
                }
            } catch (java.io.IOException e) {
                String msg = "can't create cache file: " + file;
                if (DEBUG.Enabled)
                    Util.printStackTrace(e, msg);
                else
                    Log.warn(msg, e);
                return null;
            }
            if (!file.canWrite()) {
                Log.warn("can't write cache file: " + file);
                return null;
            }
            if (DEBUG.IMAGE) out("got cache file " + file);
        }
        return file;
    }

    private static File ensurePermanentCacheFile(File file)
    {
        try {
            // chop off the initial "." to make permanent
            // If doesn't start with a dot, this is one of our existing cache
            // files: nothing to do.
            String tmpName = file.getName();
            String permanentName;
            if (tmpName.charAt(0) == '.') {
                permanentName = tmpName.substring(1);
            } else {
                // it's already permanent: we must have loaded this from our cache originally
                return file;
            }
            File permanentFile = new File(file.getParentFile(), permanentName);
            if (file.renameTo(permanentFile)) {
                Log.debug("new perm cache file: " + permanentFile);
                return permanentFile;
            }
        } catch (Throwable t) {
            tufts.Util.printStackTrace(t, "Unable to create permanent cache file from tmp " + file);
        }
        return null;
    }
    

    private static File CacheDir;
    private static File getCacheDirectory()
    {
        if (CacheDir == null) {
            File dir = VueUtil.getDefaultUserFolder();
            CacheDir = new File(dir, "cache");
            if (!CacheDir.exists()) {
                Log.debug("creating cache directory: " + CacheDir);
                if (!CacheDir.mkdir())
                    Log.warn("couldn't create cache directory " + CacheDir);
            } else if (!CacheDir.isDirectory()) {
                Log.warn("couldn't create cache directory (is a file) " + CacheDir);
                return CacheDir = null;
            }
            Log.debug("Got cache directory: " + CacheDir);
        }
        return CacheDir;
    }


    /**
     * @param imageSRC - see ImageSource ("anything" that we can get an image data stream from)
     * @param listener - an Images.Listener: if non-null, will be issued callbacks for size & completion
     * @return the loaded image, or null if none found
     */
    private static boolean FirstFailure = true;
    private static Image readAndCreateImage(ImageSource imageSRC, Images.Listener listener)
        throws java.io.IOException, ImageException
    {
        if (DEBUG.IMAGE) out("trying: " + imageSRC);

        InputStream urlStream = null; // if we create one, we need to keep this ref to close it later
        File tmpCacheFile = null; // if we create a tmp cache file, it will be put here

        int dataSize = -1;
        
        if (DEBUG.IMAGE && imageSRC.resource != null) {
            imageSRC.resource.setDebugProperty("image.read", imageSRC.readable);
        }

        if (imageSRC.hasCacheFile()) {
            // just point us at the cache file: ImageIO will create the input stream
            imageSRC.readable = imageSRC.getCacheFile();
            if (DEBUG.IMAGE && imageSRC.resource != null) {
                imageSRC.resource.setDebugProperty("image.cache", imageSRC.getCacheFile());
                out("reading cache file: " + imageSRC.getCacheFile());
            }

            // note: can get away with this because imageSRC.resource will
            // be null if this is for a preview icon, so don't need to worry
            // about getting wrong size todo: a hack anyway -- include
            // in clean-up of meta-data setting

            if (imageSRC.resource != null) {
                imageSRC.resource.setProperty(CONTENT_SIZE, imageSRC.getCacheFile().length());
                // java has no creation date for Files!  Well, last modified good enough...
                setDateValue(imageSRC.resource, CONTENT_ASOF, imageSRC.getCacheFile().lastModified());
            }
        
        } else if (imageSRC.readable instanceof java.net.URL) {

            final URL url = (URL) imageSRC.readable;

            int tries = 0;
            boolean success = false;

            final boolean debug = DEBUG.IMAGE || DEBUG.IO;

            do {

                final URLConnection conn = UrlAuthentication.getAuthenticatedConnection(url);
                urlStream = conn.getInputStream();

                if (imageSRC.resource != null) {
                    dataSize = conn.getContentLength();
                    try {
                        setResourceMetaData(imageSRC.resource, conn);
                    } catch (Throwable t) {
                        // Don't fail if a problem with meta data: still give
                        // a chance for the content to work...
                        Util.printStackTrace(t, "URLConnection Meta Data Failure");
                        //imageSRC.resource.setProperty("MetaDataFailure", t.toString());
                    }
                }
                
                
                if (!imageSRC.useCacheFile()) {
                    
                    imageSRC.readable = urlStream;
                    success = true;
                    
                } else {

                    tmpCacheFile = makeTmpCacheFile(imageSRC.key);
                    imageSRC.setCacheFile(tmpCacheFile);  // will be made permanent if no errors
                    
                    if (imageSRC.hasCacheFile()) {
                        try {
                            imageSRC.readable = new FileBackedImageInputStream(urlStream, tmpCacheFile, listener);
                            success = true;
                        } catch (Images.DataException e) {
                            Log.warn(imageSRC + ": " + e);
                            if (++tries > 1) {
                                final String msg = "Try #" + tries + ": " + e;
//                                 if (DEBUG.Enabled)
//                                     Util.printStackTrace(msg);
//                                 else
                                Log.warn(msg);
                                throw e;
                            } else {
                                Log.info("second try for " + imageSRC);
                                urlStream.close();
                            }
                            // try the reconnect one more time
                        }
                    } else {
                        // unable to create cache file: read directly from the stream
                        Log.warn("Failed to create cache file " + tmpCacheFile);
                        imageSRC.readable = urlStream;
                        success = true;
                    }
                }
                
            } while (!success && tries < 2);

        } else if (imageSRC.readable instanceof java.io.File) {
            if (DEBUG.IMAGE) Log.debug("Loading local file " + imageSRC.readable);
            if (imageSRC.resource != null)
                setResourceMetaData(imageSRC.resource, (File) imageSRC.readable);
        }

        if (imageSRC.resource != null) { // in case any held changes
            //if (DEBUG.DR) imageSRC.resource.setDebugProperty("readsrc", Util.tags(imageSRC.readable));
            imageSRC.resource.getProperties().releaseChanges();
        }

        final ImageInputStream inputStream;

        if (imageSRC.readable instanceof ImageInputStream) {
            inputStream = (ImageInputStream) imageSRC.readable;
        } else if (imageSRC.readable != null) {
            //if (DEBUG.IMAGE) out("ImageIO converting " + tag(imageSRC.readable) + " to InputStream...");
            inputStream = ImageIO.createImageInputStream(imageSRC.readable);
        } else {
            throw new ImageException(NO_READABLE_FOUND);
            //Log.warn("not readable: " + imageSRC);
        }

        if (DEBUG.IMAGE) out("Got ImageInputStream " + inputStream);

        if (inputStream == null)
            throw new ImageException("Can't Access [" + imageSRC.readable + "]"); // e,g., local file permission denied

        ImageReader reader = getDecoder(inputStream, imageSRC);

        if (reader == null) {
            badStream: {
                if (FirstFailure) {
                    // This FirstFailure code was an attempt to deal with what is now handled
                    // via DataException, but it's not a bad idea to keep it around.
                    FirstFailure = false;
                    Log.warn("No reader found: first failure, rescanning for codecs: " + imageSRC);
                    // TODO: okay, problem appears to be with the URLConnection / stream? Is only
                    // getting us tiny amount of bytes the first time...
                    if (DEBUG.Enabled) tufts.Util.printStackTrace("first failure: " + imageSRC);
                    ImageIO.scanForPlugins();
                    reader = getDecoder(inputStream, imageSRC);
                    if (reader != null)
                        break badStream;
                }
                if (DEBUG.IMAGE) out("NO IMAGE READER FOUND FOR " + imageSRC);
                throw new ImageException("Unreadable Image Stream");
            }
        }

        if (DEBUG.IMAGE) out("Chosen ImageReader for stream: " + reader + "; format=" + reader.getFormatName());
        
        //reader.addIIOReadProgressListener(new ReadListener());
        //out("added progress listener");

        reader.setInput(inputStream, false, true); // allow seek back, can ignore meta-data (can generate exceptions)
        if (DEBUG.IMAGE) out("Input for reader set to " + inputStream);
        if (DEBUG.IMAGE) out("Getting size...");
        int w = reader.getWidth(0);
        int h = reader.getHeight(0);
        if (DEBUG.IMAGE) out("ImageReader got size " + w + "x" + h);

        if (w == 0 || h == 0)
            throw new ImageException("invalid size: width=" + w + "; height=" + h);

        if (imageSRC.resource != null) {
            if (DEBUG.IMAGE || DEBUG.THREAD || DEBUG.RESOURCE)
                out("setting resource image.* meta-data for " + imageSRC.resource);
            
            imageSRC.resource.getProperties().holdChanges();
            imageSRC.resource.setProperty(Resource.IMAGE_WIDTH,  Integer.toString(w));
            imageSRC.resource.setProperty(Resource.IMAGE_HEIGHT, Integer.toString(h));
            imageSRC.resource.setProperty(Resource.IMAGE_FORMAT, reader.getFormatName());
            imageSRC.resource.setCached(true);

            // Note: If MetaDataPane is not carefully coded, this call
            // can lead to a DEADLOCK against the AWT thread (e.g.,
            // entering PropertyMap.removeListener)
            imageSRC.resource.getProperties().releaseChanges();
        }

        if (listener != null) {
            if (DEBUG.IMAGE) out("Sending size to " + tag(listener));
            listener.gotImageSize(imageSRC.original, w, h, dataSize);
        }

        // FYI, if fetch meta-data, will need to trap exceptions here, as if there are
        // any problems or inconsistencies with it, we'll get an exception, even if the
        // image is totally readable.
        //out("meta-data: " + reader.getImageMetadata(0));

        //-----------------------------------------------------------------------------
        // Now read the image, creating the BufferedImage (or otherwise)
        // 
        // Todo performance: using Toolkit.getImage on MacOSX gets us OSXImages, instead
        // of the BufferedImages which we get from ImageIO, which are presumably
        // non-writeable, and may perform better / be cached at the OS level.  This of
        // course would only work for the original java image types: GIF, JPG, and PNG.
        //-----------------------------------------------------------------------------

        if (DEBUG.Enabled) out("reading " + imageSRC + "; " + reader + "...");
        
        Image image = null;
        Throwable exception = null;

        try {
            image = reader.read(0);
            if (DEBUG.Enabled) out("    got " + imageSRC + ".");
            //testImageInspect(reader, image, imageSRC);
        } catch (Throwable t) {
            exception = t;
        } finally {
            reader.reset();
            inputStream.close();
            if (urlStream != null)
                urlStream.close();
        }

        if (exception instanceof OutOfMemoryError) {
            throw (Error) exception;
        } else if (exception != null) {
            throw new ImageException("reader.read(0) failure: " + exception);
        }

        return image;
    }

    private void testImageInspect(ImageReader reader, Image image, ImageSource imageSRC) {
        try {
            int thumbs = reader.getNumThumbnails(0);
            if (thumbs > 0)
                Log.info("thumbs: " + thumbs + "; " + imageSRC);
        } catch (Throwable t) {
            Log.debug("getNumThumbnails", t);
        }
        if (DEBUG.WORK && image != null) {
            String[] tryProps = new String[] { "name", "title", "description", "comment" };
            for (int i = 0; i < tryProps.length; i++) {
                Object p = image.getProperty(tryProps[i], null);
                if (p != null && p != java.awt.Image.UndefinedProperty)
                    System.err.println("FOUND PROPERTY " + tryProps[i] + "=" + p);
            }
        }
        
    }

    static Image createIcon(Image source, int maxSide) {

        final Image icon;

//         if (USE_SCALED_INSTANCE) {
//             return producePlatformIcon(source, maxSide);
//         } else {
//             return produceDrawnIcon(source, maxSide);
//         }
        if (DEBUG.Enabled) Log.debug("image pre-scaled: " + Util.tags(source) + " -> toMaxSide " + maxSide);
        icon = produceDrawnIcon(source, maxSide);
        if (DEBUG.Enabled) Log.debug("icon post-scaled: " + Util.tags(icon));

        return icon;
    }
    
    public static java.awt.Dimension fitInto(int maxSide, int srcW, int srcH) {
        int width, height;

        if (srcW > srcH) {
            width = maxSide;
            height = srcH * maxSide / srcW;
            // todo: ensure precision in above, and round to nearest EVEN value (for memory alignment)
        } else {
            height = maxSide;
            width = srcW * maxSide / srcH;
            // todo: ensure precision in above, and round to nearest EVEN value (for memory alignment)
        }

        //Log.debug(String.format("%dx%d -> (%d) %dx%d", srcW, srcH, maxSide, width, height));

        return new java.awt.Dimension(width, height);
    }

    private static Image produceDrawnIcon(Image source, int maxSide)
    {
        final java.awt.Dimension size = fitInto(maxSide, source.getWidth(null), source.getHeight(null));

        final Image iconSource;
        int transparency;

        if (source instanceof BufferedImage)
            transparency = ((BufferedImage)source).getTransparency();
        else
            transparency = Transparency.OPAQUE;

        if (ALLOW_HIGH_QUALITY_ICONS /*&& DrawContext.isImageQualityRequested()*/) {
            // this is clever: we can still make use of the high-quality image
            // smoothing (tho it's still quite slow) by copying out the quality
            // image data then immediately flusing the created image.  And with our
            // old 1GB busting use-case we're still down at 82MB after GC cool-down.
            // Amazing.
            iconSource = source.getScaledInstance(size.width, size.height, Image.SCALE_SMOOTH);

            if (iconSource instanceof sun.awt.image.ToolkitImage) { // will probably always be a ToolkitImage
                // generally neeeded in case the the image has alpha, so transparency will be Transparency.TRANSLUCENT
                // This will normally have already been pulled from the BufferedImage source, but just in
                // case the source wasn't a BufferedImage:
                transparency = ((sun.awt.image.ToolkitImage)iconSource).getColorModel().getTransparency();
            }
                
            // note: there are supposed to be faster methods available for generating
            // similar quality that involve multi-pass down-scaling that have been
            // documented, tho the benchmarks don't likely include a comparison to
            // the most recent Mac OS X Java 1.6 implementation, that may use
            // CoreImage underneath. See:
            // http://today.java.net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html
                
        } else {
            iconSource = source;
        }

            
        final Image icon = tufts.vue.gui.GUI.getDeviceConfigForWindow(null)
            .createCompatibleImage(size.width, size.height, transparency);

        final Graphics2D g = (Graphics2D) icon.getGraphics();
            
        if (iconSource == source) {
            // The source is the original raw image -- we'll be down-scaling during the
            // drawImage, so quality is going to be low.  Note: setImageQuality is not much
            // help (if any?) as to quality in Java 1.6 (used to make a big difference in java
            // 1.5), but we try anyway.
            DrawContext.setImageQuality(g, true);
        }

        g.drawImage(iconSource, 0, 0, size.width, size.height, null);

        if (iconSource != source)
            iconSource.flush(); // crucial to release the memory consumed
        
        return icon;
    }

    private static Image producePlatformIcon(Image source, int maxSide)
    {
        final Dimension size = fitInto(maxSide, source.getWidth(null), source.getHeight(null));

        final Image icon;

        // Note that on Mac OS X this returns an apple.awt.OSXImage's, and does so
        // IMMEDIATELY, which means they will later hang the paint thread the first time
        // they're painted (and your entire application) as the image production
        // (scaling/smoothing) takes place in lazy fashion at the last possible moment.

        // Note that whatever's returned will also probably always be an instance of
        // sun.awt.image.ToolkitImage (as OSXImage is), but how the various scaling
        // hints are handled, when/if they're lazy evaluated, etc is going to vary by
        // platform.

        // SCALE_SMOOTH produces dramatically higher quality than SCALE_FAST on Mac,
        // but is significantly slower
        icon = source.getScaledInstance(size.width, size.height, Image.SCALE_SMOOTH);
            
        // On Mac OSX, immediately drawing the image to a scratch buffer will force
        // the loading (and the actual scaling) of the apple.awt.OSXImage

//         if (false) {
//             // (would we have wanted to render icon, not _image?)                
//             ScratchGraphics.drawImage(source, 0, 0, size.width, size.height, null); 
//         }
            
        // This doesn't appear to be having a big impact on memory -- OSXImage is
        // apparently holding a reference to the underlying content.  Even if all
        // images are read & scaled sequentially, we run out of memory (bumping
        // against 1GB using our test case).

        return icon;
    }
//     private static final Image ScratchImage;
//     private static final Graphics2D ScratchGraphics;

//     static {
//         if (USE_SCALED_INSTANCE) {
//             ScratchImage = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
//             ScratchGraphics = (Graphics2D) ScratchImage.getGraphics();
//         } else {
//             ScratchImage = null;
//             ScratchGraphics = null;
//         }
//     }

    
    public static void dumpImage(Image image, String debug) {

        Object g;
        try {
            g = image.getGraphics();
        } catch (Throwable t) {
            g = t;
        }
        Object s;
        try {
            s = image.getSource();
        } catch (Throwable t) {
            s = t;
        }
        
        Log.debug("Image " + debug + ": " + Util.tags(image)
                  + "\n\t    size: " + image.getWidth(null) + "x" + image.getHeight(null)
                  + "\n\t  source: " + Util.tags(s)
//                  + "\n\tgraphics: " + Util.tags(g)
//                   + "\n\taccelPri: " + image.getAccelerationPriority()
//                   + "\n\t    caps: " + image.getCapabilities(null)
                  );
        
        //Util.dump(image.getClass().getMethods());
    }

    private static ImageReader getDecoder(ImageInputStream istream, ImageSource imageSRC)
    {
        java.util.Iterator iri = ImageIO.getImageReaders(istream);

        ImageReader reader = null;
        int idx = 0;
        while (iri.hasNext()) {
            final ImageReader ir = (ImageReader) iri.next();

            String formatName;
            try {
                formatName = ir.getFormatName();
            } catch (Throwable t) {
                formatName = "[" + t + "]";
            }
            
            if (reader == null) {
                reader = ir;
            }
//             else if ("ico".equalsIgnoreCase(formatName)) {
//                 try {
//                     if (imageSRC.key.toString().toLowerCase().endsWith(".ico")) {
//                         if (DEBUG.IMAGE) out("CHOOSING ICO READER FOR .ICO");
//                         reader = ir;
//                     }
//                 } catch (Throwable t) {
//                     Log.error("getDecoder: " + imageSRC + " " + t);
//                 }
//             }
            

            if (DEBUG.IMAGE) {
                out("\t found ImageReader #" + idx + ": " + Util.tags(ir)
                    + "; format=" + formatName
                    + "; provider=" + Util.tags(ir.getOriginatingProvider()));
            }
            idx++;
        }

        return reader;
    }
    
    // nl.ikarus.nxt.priv.imageio.icoreader.lib.ICOReaderSpi.registerIcoReader(); // not needed: is self registering or SPI providing
    // nl.ikarus.nxt.priv.imageio.icoreader.lib.ICOReader [this reader, from ICOReader-1.04.jar, appears to mostly work]

    /* Apparently, not all decoders actually report to the listeners, (e.g., TIFF), so we're not using this for now */
    private static class ReadListener implements IIOReadProgressListener {
        public void sequenceStarted(ImageReader source, int minIndex) {
            out("sequenceStarted; minIndex="+minIndex);
        }
        public void sequenceComplete(ImageReader source) {
            out("sequenceComplete");
        }
        public void imageStarted(ImageReader source, int imageIndex) {
            out("imageStarted; imageIndex="+imageIndex);
        }
        public void imageProgress(ImageReader source, float pct) {
            out("imageProgress; "+(int)(pct + 0.5f) + "%");
        }
        public void imageComplete(ImageReader source) {
            out("imageComplete");
        }
        public void thumbnailStarted(ImageReader source, int imageIndex, int thumbnailIndex){}
        public void thumbnailProgress(ImageReader source, float percentageDone) {}
        public void thumbnailComplete(ImageReader source) {}
        public void readAborted(ImageReader source) {
            out("readAborted");
        }
    }

    private static String tag(Object o) {
        if (o instanceof java.awt.Component)
            return tufts.vue.gui.GUI.name(o);
        else if (o instanceof LWComponent)
            return o.toString();
        //return ((LWComponent)o).getDiagnosticLabel();
        else
            return Util.tags(o);
        
//         String s = Util.tag(o);
//         s += "[";
//         if (o instanceof Thread) {
//             s += ((Thread)o).getName();
//         } else if (o instanceof BufferedImage) {
//             BufferedImage bi = (BufferedImage) o;
//             s += bi.getWidth() + "x" + bi.getHeight();
//         } else if (o != null)
//             s += o.toString();
//         return s + "]";
    }
    
    private static void out(Object o) {
        Log.debug(o);
        //Log.debug((o==null?"null":o.toString()));

        /*
        String s = "Images " + (""+System.currentTimeMillis()).substring(8);
        s += " [" + Thread.currentThread().getName() + "]";
        System.err.println(s + " " + (o==null?"null":o.toString()));
        */
    }

    
    /*
    private static void copyStreamToFile(InputStream in, File file)
        throws java.io.IOException
    {
        ByteBuffer buf = ByteBuffer.allocate(2048);
        FileOutputStream fout = new FileOutputStream(file);
        FileChannel fcout = fout.getChannel();
        ReadableByteChannel chin = Channels.newChannel(in);

        while (true) { 
            buf.clear(); 
            int r = chin.read(buf);
            //out("read " + r + " bytes");
            if (DEBUG.IMAGE) System.err.print(r + "; ");
            if (r == -1)
                break; 
            buf.flip(); 
            fcout.write(buf);
        }

        fcout.close();
        chin.close();
        if (DEBUG.IMAGE) out("\nFILLED " + file);
    }

    
    private static File cacheURLContent(URL url, InputStream in)
        throws java.io.IOException
    {
        File file = getCacheFile(url);
        copyStreamToFile(in, file);
        return file;
    }
    */

    /*
    static {
         // ImageIO file caching is a runtime-only scheme for allowing
         // file streams to seek backwards: nothing to with a presistant store.
         // It's also on by default.
         javax.imageio.ImageIO.setUseCache(true);
         javax.imageio.ImageIO.setCacheDirectory(new java.io.File("/tmp"));
     }
    */

    public static void main(String args[]) throws Exception {

        // GUI init required for fully loading all image codecs (tiff gets left behind otherwise)
        // Ah: the TIFF reader in Java 1.5 apparently comes from the UI library:
        // [Loaded com.sun.imageio.plugins.tiff.TIFFImageReader
        // from /System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Classes/ui.jar]

        VUE.init(args);
        
        System.out.println(java.util.Arrays.asList(javax.imageio.ImageIO.getReaderFormatNames()));
        System.out.println(java.util.Arrays.asList(javax.imageio.ImageIO.getReaderMIMETypes()));

        String filename = args[0];
        java.io.File file = null;
        java.net.URL url = null;
        Object imageSRC;
        if (args[0].startsWith("http:") || args[0].startsWith("file:"))
            imageSRC = url = new java.net.URL(filename);
        else
            imageSRC = file = new java.io.File(filename);

        DEBUG.IMAGE=true;

////getImage(imageSRC, new LWImage()); new Impl no longer an Images.Listener
        //loadImage(imageSRC, null);


        /*
          
        ImageInputStream iis = ImageIO.createImageInputStream(imageSRC);

        out("Got ImageInputStream " + iis);
        
        java.util.Iterator i = ImageIO.getImageReaders(iis);

        ImageReader IR = null;
        int idx = 0;
        while (i.hasNext()) {
            ImageReader ir = (ImageReader) i.next();
            if (IR == null)
                IR = ir;
            out("\tfound ImageReader #" + idx + " " + ir);
            idx++;
        }

        if (IR == null) {
            out("NO IMAGE READER FOUND FOR " + imageSRC);
            if (file == null)
                System.err.println("ImageIO.read got: " + ImageIO.read(url));
            else
                System.err.println("ImageIO.read got: " + ImageIO.read(file));
            //System.out.println("Reading " + file);
            System.exit(0);
        }

        out("Chosen ImageReader for stream " + IR + " formatName=" + IR.getFormatName());
        

        IR.addIIOReadProgressListener(new ReadListener());
        out("added progress listener");

        out("Reading " + IR);
        IR.setInput(iis);
        out("Input for reader set to " + iis);
        //out("meta-data: " + IR.getImageMetadata(0));
        out("Getting size...");
        int w = IR.getWidth(0);
        int h = IR.getHeight(0);
        out("ImageReader got size " + w + "x" + h);
        BufferedImage bi = IR.read(0);
        out("ImageReader.read(0) got " + bi);

        */




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




/**
 * An implementation of <code>ImageInputStream</code> that gets its
 * input from a regular <code>InputStream</code>.  As the data
 * is read, is it backed by a File for seeking backward.
 *
 */
class FileBackedImageInputStream extends ImageInputStreamImpl
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(FileBackedImageInputStream.class);

    private static final int BUFFER_LENGTH = 2048;

    private final RandomAccessFile cache;
    private final byte[] streamBuf = new byte[BUFFER_LENGTH];
    private final InputStream stream;
    
    private long length = 0L;
    private boolean foundEOF = false;

    private final Images.Listener listener;

    private final File file;

    // todo opt: pass in content size up front to init RAF to full size at start
    /**
     * @param stream - image data stream -- will be closed when reading is done
     * @param file - file to write incoming data to to use as cache
     */
    public FileBackedImageInputStream(InputStream stream, File file, Images.Listener listener)
        throws IOException, Images.ImageException
    {
        if (stream == null || file == null)
            throw new IllegalArgumentException("FileBackedImageInputStream: stream or file is null");

        this.stream = stream;
        this.cache = new RandomAccessFile(file, "rw");
        this.file = file;
        this.listener = listener;

        this.cache.setLength(0); // in case already there

        if (true) {

            final byte[] testBuf = new byte[64];
            final int got = read(testBuf);
            super.seek(0); // put as back at the start
            
            final String contentHead = new String(testBuf, 0, got, "US-ASCII");
            
            if (DEBUG.IMAGE) {
                Log.debug(String.format("ContentHead; got=%d; streamPos=%d; length=%d [%s]",
                                        got,
                                        streamPos,
                                        length,
                                        contentHead.trim()));

                // For inspecting largeer content-head's in log debug stream:
                // compress strings of newlines/whitespaces into single newlines:
                //contentHead = contentHead.replaceAll("(\\n\\s*)(\\n\\s*)+", "\n");
                // or just compress all whitespace down to single spaces:
                //contentHead = contentHead.replaceAll("\\s+", " ");
                //Log.debug("CONTENT-HEAD:\n" + contentHead + "\n-------");
            }

        
            final String trimmed = contentHead.trim();
            final String matcher = trimmed.substring(0, Math.min(16, trimmed.length())).toUpperCase();

            if (matcher.startsWith("<HTML>") || matcher.startsWith("<!DOCTYPE")) {
                Log.warn("Stream " + stream + " contains HTML, not image data: [" + contentHead.trim() + "]");

                Log.info("see cache file for HTML sample:\n\t" + file);
                
                // DEBUG: we force this readUntil to get more info on the streams that are starting
                // with <HTML> every once in a while: we can be sure to have a cache file with a bit 
                // of data in it we can inspect afterwords.
                readUntil(BUFFER_LENGTH);

                if (DEBUG.IMAGE) {
                    byte[] buf = new byte[BUFFER_LENGTH];
                    int n = read(buf);
                    super.seek(0);
                    Log.debug("Cache contents:\n");
                    System.out.println(new String(buf, 0, n, "US-ASCII"));
                }
                
                close();
                throw new Images.DataException("Content is HTML, not image data");
                
            } else {
                readUntil(BUFFER_LENGTH);  // debug: same reason as above
            }

            // setting this at the end will prevent any gotImageProgress callbacks to any listener
            // until we at know it's not an HTML data stream
            //this.listener = listener;
        }
    }

    /*
    public void seek(long pos) throws IOException {
        System.err.println("SEEK " + pos);
        super.seek(pos);
    }
    */

    /**
     * Ensures that at least <code>pos</code> bytes are cached,
     * or the end of the source is reached.  The return value
     * is equal to the smaller of <code>pos</code> and the
     * length of the source file.
     */
    private long readUntil(long pos) throws IOException {

        //System.err.println("<=" + pos + "; ");
        
        // We've already got enough data cached
        if (pos < length)
            return pos;

        // pos >= length but length isn't getting any bigger, so return it
        if (foundEOF)
            return length;

        long len = pos - length;
        cache.seek(length);
        while (len > 0) {
            // Copy a buffer's worth of data from the source to the cache
            // BUFFER_LENGTH will always fit into an int so this is safe
            final int nbytes = stream.read(streamBuf, 0, (int)Math.min(len, (long)BUFFER_LENGTH));
            if (nbytes == -1) {
                if (DEBUG.IMAGE && DEBUG.IO) System.err.println("<EOF @ " + length + ">");
                foundEOF = true;
                return length;
            }

            //if (DEBUG.IMAGE && DEBUG.IO) System.err.format("+%4d; ", nbytes);
            if (DEBUG.IO) Log.debug(String.format("+%4d bytes; %7d total", nbytes, length+nbytes));
            cache.write(streamBuf, 0, nbytes);
            len -= nbytes;
            length += nbytes;
            //System.out.println("READ TO " + length);
            if (listener != null)
                listener.gotImageProgress(stream, length, -1);
        }

        return pos;
    }

    public int read() throws IOException {
        bitOffset = 0;
        long next = streamPos + 1;
        long pos = readUntil(next);
        if (pos >= next) {
            if (DEBUG.IMAGE && DEBUG.IO) Log.debug("SEEK " + (streamPos+1));
            cache.seek(streamPos++);
            return cache.read();
        } else {
            return -1;
        }
    }

    public int read(byte[] b, int off, int len) throws IOException {
        if (b == null)
            throw new NullPointerException();

        if (off < 0 || len < 0 || off + len > b.length || off + len < 0)
            throw new IndexOutOfBoundsException();

        if (len == 0)
            return 0;


        checkClosed();

        bitOffset = 0;

        long pos = readUntil(streamPos + len);

        // len will always fit into an int so this is safe
        len = (int)Math.min((long)len, pos - streamPos);
        if (len > 0) {
            if (DEBUG.IMAGE && DEBUG.IO) Log.debug("SEEK " + streamPos);
            cache.seek(streamPos);
            cache.readFully(b, off, len);
            streamPos += len;
            return len;
        } else {
            return -1;
        }
    }

    public void close() throws IOException {
        super.close();
        cache.close();
        stream.close();
    }

    public String toString() {
        return getClass().getName() + "[" + file.toString() + "]";
    }

 }