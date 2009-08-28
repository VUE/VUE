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
import java.lang.ref.*;
import java.net.URL;
import java.net.URI;
import java.net.URLConnection;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.awt.Image;
import java.awt.image.BufferedImage;
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
 * @version $Revision: 1.61 $ / $Date: 2009-08-28 16:57:49 $ / $Author: sfraize $
 * @author Scott Fraize
 */
public class Images
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(Images.class);

    public static VueAction ClearCacheAction = new VueAction("Empty Image Cache") {
            public void act() { Cache.clear(); }
        };
    
    private static CacheMap Cache = new CacheMap();

    
    /**
     * Calls to Images.getImage must pass in a Listener to get results.
     * The first argument to all the callbacks is the original object
     * passed in to getImage as the imageSRC.
     */
    public interface Listener {
        /** If image is already cached, this will NOT be called -- is only called from an image loading thread. */
        void gotImageSize(Object imageSrc, int w, int h, long byteSize);
        /** If byte-tracking is enabled on the input source, this will be called periodically during loading */
        void gotBytes(Object imageSrc, long bytesSoFar);
        /** Will be called immediately in same thread if image cached, later in different thread if not. */
        void gotImage(Object imageSrc, Image image, int w, int h);
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
     * @return true if the result is immediately available: the image was cached or there was an immediate error
     **/
    
    public static boolean getImage(Object imageSRC, Images.Listener listener)
    {
        return getImage(imageSRC, listener, false);
    }
    
    public static boolean getImage(Object imageSRC, Images.Listener listener, boolean ignoreCache)
    {
        try {
            if (getCachedOrLoad(imageSRC, listener, ignoreCache) == null)
                return false;
        } catch (Throwable t) {
            if (DEBUG.IMAGE) tufts.Util.printStackTrace(t);
            if (listener != null)
                listener.gotImageError(imageSRC, t.toString());
        }
        return true;
    }
    
    
    /** synchronously retrive an image from the given data source: e.g., a Resource, File, URL, InputStream, etc */
    public static BufferedImage getImage(Object imageSRC)
    {
        try {
            return getCachedOrLoad(imageSRC, null, false);
        } catch (Throwable t) {
            if (DEBUG.IMAGE) tufts.Util.printStackTrace(t);
            Log.error("getImage " + imageSRC + ": " + t);
            return null;
        }
    }

//     /**
//      * synchronously retrive the raw byte-stream for the original image data if we don't already have it, and
//      * return a stream.
//      */
//     public static InputStream getImageData(Object imageSRC)
//     {
//         if (imageSRC instanceof BufferedImage) {
//             Util.printStackTrace("can't get original byte-stream from BufferedImage " + imageSRC);
//             return null;
//         }
        
//         try {
//             return getCachedOrLoad(imageSRC, null);
//         } catch (Throwable t) {
//             if (DEBUG.IMAGE) tufts.Util.printStackTrace(t);
//             Log.error("getImage " + imageSRC + ": " + t);
//             return null;
//         }
//     }

    /** @return the cache file for the given resource, or null if none exists */
    public static File findCacheFile(Resource r) {

        final ImageSource imageSRC = new ImageSource(r);

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
    

    private static class CacheEntry {
        private Reference imageRef;
        private File file;

        /** image should only be null for startup init with existing cache files */
        CacheEntry(BufferedImage image, File cacheFile)
        {
            if (image == null && cacheFile == null)
                throw new IllegalArgumentException("CacheEntry: at least one of image or file must be non null");
            if (image != null)
                this.imageRef = new SoftReference(image);
            this.file = cacheFile;
            if (DEBUG.IMAGE) out("new " + this);
        }

        BufferedImage getCachedImage() {

            // if don't even have a ref, this was for an init-time persistent cache file
            if (imageRef == null)
                return null;

            BufferedImage image = (BufferedImage) imageRef.get();
            // will be null if was cleared
            if (image == null) {
                if (DEBUG.Enabled) out("GC'd: " + file);
                return null;
            } else
                return image;
        }

        File getFile() {
            return file;
        }

        void clear() {
            if (imageRef != null)
                imageRef.clear();
        }

        public String toString() {
            return "CacheEntry[" + tag(getCachedImage()) + "; file=" + file + "]";
        }
    }


    /*
     * Not all HashMap methods covered: only safe to use
     * the onces explicity implemented here.
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
            Iterator i = values().iterator();
            while (i.hasNext()) {
                Object entry = i.next();

                // may be a Loader: todo: may want to kill thread if it is
                // Especially: if we go off line, Loaders created
                // immediately after that (or during) tend to hang forever.
                // Loaders created once the OS knows we're offline
                // will usually fail immediately with "no route to host",
                // but even after going back online, and other images
                // load, the originally hung Loader's won't die...
                // So at least an image-cache should kill them.
                
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
                    ((Loader)entry).stop();
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

    private static URI makeKey(URL u) {
        
        try {

            if ("file".equals(u.getProtocol())) {

                return Resource.makeURI(u);
                
            } else {
                
                return new URI(u.getProtocol(),
                               u.getUserInfo(),
                               u.getHost(),
                               u.getPort(),
                               //u.getAuthority(),
                               u.getPath(),
                               u.getQuery(),
                               u.getRef()).normalize();
                
            }
            
        } catch (Throwable t) {
            Util.printStackTrace(t, "can't make URI cache key from URL " + u);
        }
        return null;
    }

    private static URI makeKey(File file) {
        try {
            return file.toURI().normalize();
        } catch (Throwable t) {
            Util.printStackTrace(t, "can't make URI cache key from file " + file);
        }
        return null;
    }

    public static String keyToCacheFileName(URI key)
        throws java.io.UnsupportedEncodingException
    {
        //return key.toASCIIString();
        return java.net.URLEncoder.encode(key.toString(), "UTF-8");
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


    // Note: we use a URI, not a URL as the cache key.  URL's are slow to compare: they use host name resolution
    // to produce the IP address and compare by that.

    private static class ImageSource {
        final Object original;  // anything plausably covertable image source (e.g. a Resource, URL, File, stream)
        final Resource resource;// if original was a resource, it goes here.
        final URI key;          // Unique key for caching
        Object readable;        // the readable image source (not a Resource, and URL's converted to stream before ImageIO)

        File _cacheFile;         // If later stored in a file cache, is marked here.

        boolean isThumbshot = false;

        ImageSource(Object original) {
            this.original = original;

            if (original instanceof Resource) {

                this.resource = (Resource) original;
                this.readable = resource.getImageSource();
                
            } else if (original instanceof java.net.URL) {
                
                this.readable = (java.net.URL) original;
                if (readable.toString().startsWith(URLResource.THUMBSHOT_FETCH))
                    isThumbshot = true;
                this.resource = null;
                
            } else if (original instanceof BufferedImage) {
                
                Util.printStackTrace("SEEING BUFFERED IMAGE: HANDLE PRIOR " + original);
                this.resource = null;
                
            } else {

                this.resource = null;
            }

            
            if (readable instanceof java.net.URL) {
                final URL url = (URL) readable;
                this.key = makeKey(url);

                final File file = Resource.getLocalFileIfPresent(url);

                if (file != null)
                    this.readable = file;
                
            } else if (readable instanceof java.io.File) {
                
                this.key = makeKey((File) readable);
                
            } else {

                this.key = null; // will not be cacheable
                
            }

//             if (DEBUG.DR) {
//                 if (resource != null)
//                     resource.setDebugProperty("readable", Util.tags(readable));
//             }
            
        }

        void setCacheFile(File file) {
            _cacheFile = file;

            if (DEBUG.IMAGE) {
                if (resource != null)
                    resource.setDebugProperty("image.cache", file);
            }
            
//             if (resource != null)
//                 resource.setCacheFile(file);
        }

        File getCacheFile() {
            return _cacheFile;
        }

        boolean hasCacheFile() {
            return _cacheFile != null;
            
        }
        

        boolean useCacheFile() {
            return !isThumbshot;
        }

        public String toString() {
            String s = tag(original);
            if (readable != original)
                s += "; readable=[" + tag(readable) + "]";
            if (_cacheFile != null)
                s += "; cache=" + _cacheFile;
            return "ImageSource[" + s + "]";
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
            this.head = l0;
            this.tail = l1;
        }
        ListenerRelay(Listener l0) {
            this(l0, null);
        }

        public void gotImageSize(Object imageSrc, int w, int h, long byteSize) {
            if (DEBUG.IMAGE) out("relay SIZE to head " + tag(head) + " " + imageSrc);
            head.gotImageSize(imageSrc, w, h, byteSize);
            if (tail != null) {
                if (DEBUG.IMAGE) out("relay SIZE to tail " + tag(tail) + " " + imageSrc);
                tail.gotImageSize(imageSrc, w, h, byteSize);
            }
        }
        public void gotBytes(Object imageSrc, long bytesSoFar) {
            //if (DEBUG.IMAGE) out("relaying new byte total " + bytesSoFar + " to " + imageSrc);
            if (DEBUG.IMAGE) out(String.format("relay BYTES %5d to head %s", bytesSoFar, tag(head)));
            //if (DEBUG.IMAGE) out(String.format("relay BYTES to head " + tag(head) + " " + imageSrc);
            head.gotBytes(imageSrc, bytesSoFar);
            if (tail != null) {
                //if (DEBUG.IMAGE) out("relay BYTES to tail " + tag(tail) + " " + imageSrc);
                if (DEBUG.IMAGE) out(String.format("relay BYTES %5d to tail %s", bytesSoFar, tag(tail)));
                tail.gotBytes(imageSrc, bytesSoFar);
            }
        }
        public void gotImage(Object imageSrc, Image image, int w, int h) {
            if (DEBUG.IMAGE) out(Util.TERM_CYAN + "relay IMAGE to head " + Util.TERM_CLEAR + tag(head) + " " + imageSrc);
            head.gotImage(imageSrc, image, w, h);
            if (tail != null) {
                if (DEBUG.IMAGE) out(Util.TERM_CYAN + "relay IMAGE to tail " + Util.TERM_CLEAR + tag(tail) + " " + imageSrc);
                tail.gotImage(imageSrc, image, w, h);
            }
        }
        public void gotImageError(Object imageSrc, String msg) {
            if (DEBUG.IMAGE) out("relay ERROR to head " + tag(head) + " " + imageSrc);
            head.gotImageError(imageSrc, msg);
            if (tail != null) {
                if (DEBUG.IMAGE) out("relay ERROR to tail " + tag(tail) + " " + imageSrc);
                tail.gotImageError(imageSrc, msg);
            }
        }

        boolean hasListener(Listener l) {
            if (head == l || tail == l)
                return true;
            if (tail instanceof ListenerRelay)
                return ((ListenerRelay)tail).hasListener(l);
            else
                return false;
        }

    }

    /**
     * Track what's been delivered, to send to listeners that are added when
     * partial results have already been delivered.
     */
    private static class LoaderRelayer extends ListenerRelay
    {
        private final ImageSource imageSRC;
        
        private Image image = null;
        private int width = -1;
        private int height = -1;
        private long byteSize;
        private long bytesSoFar;
        private String errorMsg = null;
        
        
        LoaderRelayer(ImageSource is, Listener firstListener) {
            super(firstListener, null);
            imageSRC = is;
        }

        @Override
        public synchronized void gotImageSize(Object imageSrc, int w, int h, long byteSize) {
            this.width = w;
            this.height = h;
            this.byteSize = byteSize;
            super.gotImageSize(imageSrc, w, h, byteSize);
        }

        @Override
        public synchronized void gotBytes(Object imageSrc, long bytesSoFar) {
            this.bytesSoFar = bytesSoFar;
            super.gotBytes(imageSrc, bytesSoFar);
        }
        
        @Override
        public synchronized void gotImage(Object imageSrc, Image image, int w, int h) {
            this.image = image;
            super.gotImage(imageSrc, image, w, h);

        }
        
        @Override
        public synchronized void gotImageError(Object imageSrc, String msg) {
            this.errorMsg = msg;
            super.gotImageError(imageSrc, msg);

        }

        synchronized void addListener(Listener newListener)
        {
            if (hasListener(newListener)) {
                if (DEBUG.IMAGE) out("Loader; ALREADY A LISTENER FOR THIS IMAGE: " + tag(newListener));
                return; 
            }
            
            // Deliver any results we've already got.  It's theoretically possible
            // for this to happen even after we have all our results, if the image
            // completed between the time we found the Loader in the cache, and the
            // time the requestor was added as a listener.
            
            deliverPartialResults(newListener);
            
            if (tail == null)  {
                tail = newListener;
            } else {
                tail = new ListenerRelay(tail, newListener);
            }

        }
        
        private void deliverPartialResults(Listener l)
        {
            if (DEBUG.IMAGE) out("DELIVERING PARTIAL RESULTS TO: " + tag(l));
            
            if (width > 0)
                l.gotImageSize(imageSRC.original, width, height, byteSize);

            if (bytesSoFar > 0)
                l.gotBytes(imageSRC.original, bytesSoFar);

            if (image != null)
                l.gotImage(imageSRC.original, image, width, height);

            if (errorMsg != null) 
                l.gotImageError(imageSRC.original, errorMsg);

            if (DEBUG.IMAGE) out(" DELIVERED PARTIAL RESULTS TO: " + tag(l));
            
        }

    }
    
    

    /**
     * A thread for loading a single image.  Images.Listener results are delievered
     * from this thread (unless the image was already cached).
     */
    static class Loader extends Thread {
        private static int LoaderCount = 0;
        private final ImageSource imageSRC; 
        private final LoaderRelayer relay;


        /**
         * @param src must be any valid src *except* a Resource
         * @param resource - if this is tied to a resource to update with meta-data after loading
         */
        Loader(ImageSource imageSRC, Listener l) {
            //super(String.format("IL-%02d %s", LoaderCount++, imageSRC));
            super(String.format("ImgLoader-%02d", LoaderCount++));
            if (l == null)
                Log.warn(this + "; nobody listening: image will be quietly cached: " + imageSRC);
            this.imageSRC = imageSRC;
            this.relay = new LoaderRelayer(imageSRC, l);
            setDaemon(true);
            setPriority(Thread.currentThread().getPriority() - 1);
        }

        public void run() {
            if (DEBUG.IMAGE || DEBUG.THREAD) out("Loader: load " + imageSRC + " kicked off");
            BufferedImage bi = loadImage(imageSRC, relay);
            if (DEBUG.IMAGE || DEBUG.THREAD) out("Loader: load returned, result=" + tag(bi));
        }

        void addListener(Listener newListener) {
            relay.addListener(newListener);
        }
    }


    
    /**
     * @return Image if cached or listener is null, otherwise makes callbacks to the listener from
     * a new thread.
     */
    private static BufferedImage getCachedOrLoad(Object _imageSRC, Images.Listener listener, boolean ignoreCache)
        throws java.io.IOException, java.lang.InterruptedException
    {
        if (_imageSRC instanceof BufferedImage) {
            final BufferedImage bi = (BufferedImage) _imageSRC;
            if (DEBUG.IMAGE) Log.info("image source was an instance of BufferedImage", new Throwable(bi.toString()));
            if (listener != null)
                listener.gotImage(bi,
                                  bi,
                                  bi.getWidth(),
                                  bi.getHeight());
            return (BufferedImage) _imageSRC;
        }
        
        final ImageSource imageSRC = new ImageSource(_imageSRC);

        if (DEBUG.IMAGE) System.out.println("-------------------------------------------------------");

        //Log.debug("fetching image source " + imageSRC + " for " + tag(listener));
        //if (DEBUG.Enabled) Log.debug("fetching " + imageSRC + " for listener " + Util.tag(listener));
        if (DEBUG.IMAGE) Log.debug("fetching for listener " + Util.tag(listener) + " " + imageSRC);


        Object fetchResult;
        BufferedImage cachedImage = null;
        
        if (ignoreCache) {

            fetchResult = null;

        } else {

            synchronized (Cache) {
                fetchResult = getCacheFetchResult(imageSRC, listener);
            }

            if (fetchResult == IMAGE_LOADER_STARTED)
                return null;
        }


        if (fetchResult instanceof Loader) {
            Loader loader = (Loader) fetchResult;

            if (listener != null) {
                if (DEBUG.IMAGE) out("Adding us as listener to existing Loader");
                // TODO CRITICAL: above is last message we're seeing before
                // an apparent deadlock when rapidly dragging search result
                // resources onto map: is this the same deadlock we already "fixed" ??
                loader.addListener(listener);
                return null;
            }
                
            // We had no listener, so run synchronous & wait on existing loader thread to die:
            // We can't have a cache-lock when we do this.
            
            out("Joining " + tag(loader) + "...");
            loader.join();
            out("Join of " + tag(loader) + " completed, cache has filled.");
            
            // Note we get here only in one rare case: there was an entry in the
            // cache that was already loading on another thread, and somebody new
            // requested the image that did NOT have a listener, so we joined the
            // existing thread and waited for it to finish (with no listener, we
            // have to run synchronous).
            
            // So now that we've waited, we should be guarnateed to have a full
            // Image result in the cache at this point.
            
            // Note: theoretically, the GC could have cleared our SoftReference
            // betwen loading the cache and now, tho this may never happen.
            
            cachedImage = ((CacheEntry)Cache.get(imageSRC.key)).getCachedImage();
            if (cachedImage == null)
                Log.warn("Zealous GC: image tossed immediately " + imageSRC);

        } else if (fetchResult instanceof BufferedImage) {
            cachedImage = (BufferedImage) fetchResult;
        }


        if (cachedImage != null) {
            if (listener != null) {
                // immediately callback the listener with the result
                listener.gotImage(imageSRC.original,
                                  cachedImage,
                                  cachedImage.getWidth(),
                                  cachedImage.getHeight());
            }
            return cachedImage;
        }

        // We had no image, and no Loader was started: this should only
        // happen if there was no listener for the Loader, tho
        // we allow the sync load to go ahead just in case.
        // (Could get here due to an over-zealous GC).

        if (listener != null)
            Util.printStackTrace("had a listener, but no Loader created: backup syncrhonous loading for " + imageSRC);

        if (DEBUG.IMAGE) out("synchronous load of " + imageSRC);
        
        // load the image and don't return until we have it
        return loadImage(imageSRC, listener);
    }
    

    private static final Object IMAGE_LOADER_STARTED = "<image-loader-created>";

    /**
     * This method should only be called in a cache lock.  It has all sorts of side
     * effects to the cache.  At the end of this call, we usually know there is
     * something in the cache for the given imageSRC.key -- either we found the image
     * already there, or we found a Loader thread already started there, or we put and
     * started a new Loader thread there.  Unless there was no listener and nothing in
     * the cache, in which case the image needs to be loaded synchronously (and we
     * return null).
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
    private static Object getCacheFetchResult(ImageSource imageSRC, Images.Listener listener)
    {
        Object entry;
        
        if (imageSRC.key != null && (entry = Cache.get(imageSRC.key)) != null) {
            if (DEBUG.IMAGE) out("found cache entry for key " + tag(imageSRC.key) + ": " + entry);
                
            if (entry instanceof Loader) {
                if (DEBUG.IMAGE) out("Image is loading into the cache via already existing Loader...");
                return entry;
            }

            // Entry is not a Loader, so it must be a regular CacheEntry
            // We still may not have an image tho: it may be been
            // garbage collected, or the entry may actually be for a file
            // on disk.

            final CacheEntry ce = (CacheEntry) entry;
            final BufferedImage cachedImage = ce.getCachedImage();
                     	
            // if we have the image, we're done (it was loaded this runtime, and not GC'd)
            // if not, either it was GC'd, or it's a cache file entry from the persistent
            // cache -- in either case, there is a file on disk -- mark it in the imageSRC,
            // and the loader will notice it and use it.
                    
            boolean emptyEntry = true;

            if (cachedImage != null) {
                emptyEntry = false;
            } else if (ce.getFile() != null) {
                if (ce.file.canRead()) {
                    imageSRC.setCacheFile(ce.file);
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

            if (cachedImage != null)
                return cachedImage;

            // The was an entry in the cache, but it was of no use:
            
            // We get here in the following cases:
            // (1) We had the image, but is was GC'd -- we're going back to the cache file
            // (2) We had the image, but is was GC'd -- original was on disk: go back to that
            // (3) We had the image, but is was GC'd, and disk cache not working: reload original from network
            // (4) We never had the image, but it is in disk cache: go get it
            // (5) unlikely case case of zealous GC: reload original

            // Note that original image sources that were on disk are NOT moved
            // to the disk cache, and CacheEntry.file should always be null for those.

        }            

        // Nothing was in the cache: we must go get it.
        // If we have a listener, create a thread to do this.
        // If we don't, do now and don't return until we have the result.
        
        if (listener != null) {
            Loader newLoader = new Loader(imageSRC, listener);
            Cache.put(imageSRC.key, newLoader);
            newLoader.start();
            return IMAGE_LOADER_STARTED;
        }

        // With no listener, and nothing found in the cache, this
        // image will need to be loaded immediately in the current thread.
        return null;
    }
    

    static class ImageException extends Exception {
        ImageException(String s) {
            super(s);
        }
    }
    static class DataException extends ImageException {
        DataException(String s) {
            super(s);
        }
    }

    private static final String NO_READABLE_FOUND = "No Readable";

    /** An wrapper for readAndCreateImage that deals with exceptions, and puts successful results in the cache */
    private static BufferedImage loadImage(ImageSource imageSRC, Images.Listener listener)
    {
        BufferedImage image = null;

        if (imageSRC.resource != null)
            imageSRC.resource.getProperties().holdChanges();
        
        try {
            image = readAndCreateImage(imageSRC, listener);
        } catch (Throwable t) {
            
            if (DEBUG.IMAGE) Util.printStackTrace(t);

            Cache.remove(imageSRC.key);
            
            if (listener != null) {
                String msg;
                boolean dumpTrace = false;
                if (t instanceof java.io.FileNotFoundException)
                    msg = "Not Found: " + t.getMessage();
                else if (t instanceof java.net.UnknownHostException)
                    msg = "Unknown Host: " + t.getMessage();
                else if (t instanceof java.lang.IllegalArgumentException && t.getMessage().startsWith("LUT has improper length"))
                    // known java bug: many small PNG images fail to read (effects thumbshots)
                    msg = null; // don't bother to report an error
                else if (t instanceof ImageException) {
                    msg = (t.getMessage() == NO_READABLE_FOUND ? null : t.getMessage());
                } else if (t instanceof ThreadDeath)
                    msg = "interrupted";
                else if (t.getMessage() != null && t.getMessage().length() > 0) {
                    msg = t.getMessage();
                    dumpTrace = true;
                } else {
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

    private static File makeTmpCacheFile(URI key)
        throws java.io.UnsupportedEncodingException
    {
        String cacheName = "." + keyToCacheFileName(key);
        File cacheDir = getCacheDirectory();
        File file = null;
        if (cacheDir != null) {
            file = new File(getCacheDirectory(), cacheName);
            try {
                if (!file.createNewFile()) {
                    if (DEBUG.IO) Log.debug("cache file already exists: " + file);
                }
            } catch (java.io.IOException e) {
                Util.printStackTrace(e, "can't create tmp cache file " + file);
                //VUE.Log.warn(e.toString());
                return null;
            }
            if (!file.canWrite()) {
                Log.warn("can't write cache file: " + file);
                return null;
            }
            if (DEBUG.IMAGE) out("got tmp cache file " + file);
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


    
    /**
     * @param imageSRC - see ImageSource ("anything" that we can get an image data stream from)
     * @param listener - an Images.Listener: if non-null, will be issued callbacks for size & completion
     * @return the loaded image, or null if none found
     */
    private static boolean FirstFailure = true;
    private static BufferedImage readAndCreateImage(ImageSource imageSRC, Images.Listener listener)
        throws java.io.IOException, ImageException
    {
        if (DEBUG.IMAGE) out("READING " + imageSRC);

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
            /*
            
            final URL url = (URL) imageSRC.readable;
            final String asText = url.toString();
            URL cleanURL = url;

            if (asText.indexOf(' ') > 0) {
                // Added 2007-09-20 SMF -- Sakai HTTP server is rejecting spaces in the URL path.
                try {
                    cleanURL = new URL(asText.replaceAll(" ", "%20"));
                } catch (Throwable t) {
                    Util.printStackTrace(t, asText);
                    return null;
                }
            }

            int tries = 0;
            boolean success = false;

            final boolean debug = DEBUG.IMAGE || DEBUG.IO;

            final Map<String,String> sessionKeys = UrlAuthentication.getRequestProperties(url);
            
            do {
                if (debug) out("opening URLConnection... (sessionKeys " + sessionKeys + ")");
                final URLConnection conn = cleanURL.openConnection();

                if (sessionKeys != null) {
                    for (Map.Entry<String,String> e : sessionKeys.entrySet()) {
                        if (debug) System.out.println("\tHTTP request[" + e.getKey() + ": " + e.getValue() + "]");
                        conn.setRequestProperty(e.getKey(), e.getValue());
                    }
                }

                if (debug) {
                    out("got URLConnection: " + conn);
                    final Map<String,List<String>> rp = conn.getRequestProperties();
                    for (Map.Entry<String,List<String>> e : rp.entrySet()) {
                        System.out.println("\toutbound HTTP header[" +e.getKey() + ": " + e.getValue() + "]");
                    }
                }
                
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
                
                if (debug) out("opening URL stream...");
                urlStream = conn.getInputStream();
                if (debug) out("got URL stream");

                if (debug) {
                    out("Connected; Headers from [" + conn + "];");
                    final Map<String,List<String>> headers = conn.getHeaderFields();
                    List<String> response = headers.get(null);
                    if (response != null)
                        System.out.format("%20s: %s\n", "HTTP-RESPONSE", response);
                    for (Map.Entry<String,List<String>> e : headers.entrySet()) {
                        if (e.getKey() != null)
                            System.out.format("%20s: %s\n", e.getKey(), e.getValue());
                    }
                }
            */

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
        // Now read the image, creating the BufferedImage
        // 
        // Todo performance: using Toolkit.getImage on MacOSX gets us OSXImages, instead
        // of the BufferedImages which we get from ImageIO, which are presumably
        // non-writeable, and may perform better / be cached at the OS level.  This of
        // course would only work for the original java image types: GIF, JPG, and PNG.
        //-----------------------------------------------------------------------------

        if (DEBUG.IMAGE) out("Reading " + reader);
        BufferedImage image = null;
        try {
            image = reader.read(0);
        } catch (Throwable t) {
            throw new ImageException("read failed: " + t.toString());
        }
        if (DEBUG.IMAGE) {
            out("ImageReader.read(0) got " + Util.tags(image));
        }

        if (DEBUG.Enabled) {
            int thumbs = reader.getNumThumbnails(0);
            if (thumbs > 0)
                Log.info("thumbs: " + thumbs + "; " + imageSRC);
        }
            
        if (listener != null)
            listener.gotImage(imageSRC.original, image, w, h);

        inputStream.close();
        
         if (urlStream != null)
             urlStream.close();

        if (DEBUG.Enabled && image != null) {

            String[] tryProps = new String[] { "name", "title", "description", "comment" };
            for (int i = 0; i < tryProps.length; i++) {
                Object p = image.getProperty(tryProps[i], null);
                if (p != null && p != java.awt.Image.UndefinedProperty)
                    System.err.println("FOUND PROPERTY " + tryProps[i] + "=" + p);
            }
        }

        // TODO: Use GraphicsConfiguration.createCompatibleImage to produce optimal images.

        return image;
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

        getImage(imageSRC, new LWImage());
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

            // setting this at the end will prevent any gotBytes callbacks to any listener
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
                listener.gotBytes(stream, length);
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