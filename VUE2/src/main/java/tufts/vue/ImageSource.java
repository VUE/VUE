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
import java.net.URI;
import java.net.URL;
import java.io.File;

/** this class in support of image loading in Images.java */
class ImageSource {

    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(ImageSource.class);

    private static final File DO_NOT_CACHE_TO_DISK = new File("");
        
    // Note: we use a URI, *not* a URL as the cache key.  URL's can be very slow to compare: they use host name resolution
    // to produce the IP address and compare by that.
    
    final Object original;  // any plausably covertable image source (e.g. a Resource, URL, File, stream)
    final URI key;          // Unique key for caching
    final Resource resource;// if original was a resource, it goes here.
        
    Object readable;        // the readable image source (not a Resource, and URL's converted to stream before ImageIO)

    private File _cacheFile;         // If later stored in a file cache, is marked here.

    final int iconSize;

//     boolean nextLoadIsImmediate;

// //     interface Scaler {
// //         int size();
// //         java.awt.Image getScaled();
// //     }

//     void setImmediateRequest(boolean b) {
//         nextLoadIsImmediate = b;
//     }

    public static ImageSource create(Object o) {
        if (o instanceof ImageSource) {
            return (ImageSource) o;
        }
        else if (o instanceof URI) {
            return new ImageSource((URI)o, 0);
        }
        else {
            return new ImageSource(o);
        }
    }

    public static ImageSource createIconSource(ImageSource is, ImageRep fullRep, java.awt.Image hardFullImage, int size) {
        return new ImageSource(is, fullRep, hardFullImage, size);
    }
        
    /** @return a key that could be used for an icon version of this image */
    URI getIconKey(int size) {
        if (key == null) {
            // todo: this will happen for a local files -- only if they're missing or always?
            if (DEBUG.IMAGE||DEBUG.WORK) 
                Log.warn("can't create icon key w/null key: " + this, new Throwable("HERE"));
            else
                Log.warn("can't create icon key w/null key: " + this);
            return null;
        }
        if (isImageSourceForIcon())
            throw new Error("can't make icon key from another icon image source");
        return makeIconKey(this.key, size);
    }
    
    /** create an icon entry */
    private ImageSource(ImageSource is, ImageRep softImageSource, java.awt.Image hardImageSource, int iconSize) {
        if (iconSize <= 0)
            throw new IllegalArgumentException("bad icon size " + iconSize);
        this.original = is.original;
        this.iconSize = iconSize;

        if (Images.DELAYED_ICONS) {
            // Even if we end up getting DELAYED_ICONS to work under low-mem conditions,
            // we may still want to keep the hard image source, tho that somewhat defeats
            // the purpose of the fancy low-memory recovery code we'd need anyway to handle
            // DELAYED_ICONS.  E.g., hard references in a bunch of IconTasks at the back
            // of the queue when we run out of memory makes recovery even harder as we'd
            // really want to flush those IconTasks to allow GC and free up space before
            // proceeding single-threaded through outstanding image loads & icon tasks.
            
            this.readable = softImageSource;

            // The problem attempting the DELAYED_ICONS impl: ImageRep will go bad when
            // we clear this, tho that's easy to fix.  The big problem is an icon
            // ImageRep would need to be able to trigger the reconstitute of the
            // lost full rep, and then trigger the icon generation again when it
            // comes back in.

        } else {
            
            this.readable = hardImageSource;
            
            // drawback to this impl: lots of memory contention when we run low (longer
            // to recover), tho mainly if we were attempting the DELAYED_ICONS impl.
            // advantage: we know sooner if we're running low on memory, and Images EOM
            // recovery doesn't need to be as sophisticated.

            // Note: this.readable must be cleared later to ensure disposability. It is
            // provided and held as a reference here exactly so that it is NOT
            // disposable until we're done with it (e.g., created an icon from it).
        }
        
        this.key = makeIconKey(is.key, iconSize);
        this.resource = null;
        this._cacheFile = new File(Images.keyToCacheFileName(this.key));
    }
    
    // todo: would be better to use the actual CacheEntry.file to create
    // this, but we may not have that if there's a Loader in the Cache.
    private ImageSource(URI cacheKey, int iconSize) {
        if (cacheKey == null)
            throw new NullPointerException("cacheKey");
        this.original = cacheKey;
        this.key = cacheKey;
        this.resource = null;
        this._cacheFile = new File(Images.keyToCacheFileName(this.key));
        this.iconSize = iconSize; // okay if <=0: means not an icon
        // readable unset
        // todo: consistency check: key from file == incoming key
    }
        
    private ImageSource(Object original) {
        this.iconSize = -1;
        this.original = original;

        //Log.debug("NEW IMAGE SOURCE FROM " + Util.tags(original));

        if (original instanceof Resource) {
            resource = (Resource) original;
            readable = resource.getImageSource();
            if (DEBUG.RESOURCE) Log.debug("NEW IMAGE SOURCE FROM " + Util.tags(original) + "; readable=" + Util.tags(readable));
        } else {
            resource = null;
            if (original instanceof File) {
                readable = (File) original;
                //setCacheFile((File) original); // note side effect
            }
            else if (original instanceof java.net.URL) {
                readable = (java.net.URL) original;
                if (readable.toString().startsWith(URLResource.THUMBSHOT_FETCH)) {
                    _cacheFile = DO_NOT_CACHE_TO_DISK;
                    //isThumbshot = true;
                }
            }
            //                 else if (original instanceof java.net.URI) { // cache key
            //                     this.key = (URI) original;
            //                     this.readable = keyToCacheFileName(this.key);
            //                 }
            //                 else if (original instanceof Image) {
            //                     Util.printStackTrace("SEEING IMAGE: HANDLE PRIOR " + original);
            //                 }
            else {
                throw new Error("can't use as an ImageSource: " + Util.tags(original));
            }
        }

            
        if (readable instanceof java.net.URL) {
            final URL url = (URL) readable;
            this.key = makeKey(url);

            final File file = Resource.getLocalFileIfPresent(url);

            if (file != null) {
                //Log.debug("CONVERTED URL TO LOCAL FILE: " + file);
                this.readable = file;
            } else {
                //Log.debug("FAILED TO CONVERT URL TO LOCAL FILE: " + url);
            }
                
        } else if (readable instanceof java.io.File) {
                
            this.key = Images.makeKey((File) readable);
                
        } else {

            this.key = null; // will not be cacheable
            if (DEBUG.IMAGE) Log.debug("CREATED NULL-KEY NON-CACHEABLE IMAGE SOURCE:"
                                       + "\n\treadable="+Util.tags(readable)
                                       + "\n\toriginal="+Util.tags(original)
                                       , new Throwable(toString()));
                
        }

        //             if (DEBUG.DR) {
        //                 if (resource != null)
        //                     resource.setDebugProperty("readable", Util.tags(readable));
        //             }
            
    }

    private static URI keyFromReadable(Object readable)
    {
        if (readable instanceof java.net.URL) {
            return makeKey((URL) readable);
        } else if (readable instanceof java.io.File) {
            return Images.makeKey((File) readable);
        } else {
            if (DEBUG.IMAGE) Log.debug("readable " + Util.tags(readable) + " has no key; will not enter cache");
            return null;
        }
    }
        
    boolean mayBlockIndefinitely() {
        if (readable instanceof java.net.URL) {
            //Log.debug("MAY BLOCK INDEFINITELY: READABLE IS " + Util.tags(readable));
            return true;
        } else
            return false;
    }

    /** @return true if this source is a raw image that can be used to create an icon at runtime.
     * This is contrasted with a regular ImageSource that may point to a icon already cached on disk.
     */
    boolean isImageSourceForIcon() {
        return iconSize > 0; // tho resonable sizes would normally start at a min of 16 or 32
    }

    void setCacheFile(File file) {
        if (_cacheFile == DO_NOT_CACHE_TO_DISK)
            throw new RuntimeException("attempt to cache non-cacheable: " + this);
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
        return _cacheFile != null && _cacheFile != DO_NOT_CACHE_TO_DISK;
    }

    boolean isDiskCacheEntry() { // todo: is this still used?
        return original == key; // TODO: check to see if semantics have changed
    }

    boolean useCacheFile() {
        if (isDiskCacheEntry() || _cacheFile == DO_NOT_CACHE_TO_DISK)
            return false;
        else
            return true;
    }

    String debugName()
    {
        if (key == null)
            return "[null ImageSource.key]";
        
        try {
            // todo: put this in the image source?
            String basename = key.getPath();
            final int lastSlash = basename.lastIndexOf('/');
            if (lastSlash < (basename.length()-2))
                basename = basename.substring(lastSlash+1, basename.length());
            return basename;
        } catch (Throwable t) {
            return "[" + t.toString() + "]";
        }
    }
    

    @Override public String toString() {
        final StringBuilder s = new StringBuilder("IS[");
        
        //s.append("<<<");
        if (original instanceof Resource)
            s.append(original);
        else if (original instanceof URI) {
            s.append("URI@");
            s.append(Util.tags(original.toString()));
        } else
            s.append(Util.tags(original));
        //s.append(">>>");
        
        if (readable != original) {
            s.append("; R=");
            if (readable instanceof File) {
                if (original instanceof Resource && ((Resource)original).getImageSource() == readable) {
                    s.append("FF");
                } else if (_cacheFile == readable) {
                    s.append("FC");
                } else {
                    s.append("Fx");
                    s.append(readable.toString());
                }
            } else
                s.append(Util.tags(readable));
        }
        if (iconSize > 0) {
            s.append("; ICON");
            s.append(iconSize);
        }
        if (_cacheFile != null) {
            s.append("; CF=");
            s.append(_cacheFile);
        }
        s.append(']');
        return s.toString();
    }

    static URI makeIconKey(URI cacheKey, int size) {
        if (cacheKey == null)
            throw new IllegalArgumentException("makeIconKey: null source key");
        if (size <= 0)
            throw new IllegalArgumentException("makeIconKey: bad size " + size);
        try {
            return new URI(String.format("%s.i%d.png", cacheKey, size));
        } catch (Throwable t) {
            Util.printStackTrace(t, "can't make URI icon cache key from key " + cacheKey);
        }
        return null;
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
        

}


    //         /** @return a source that could be used for an icon version of this image */
    //         ImageSource getIconSource() {
    //             final URI iconKey = getIconKey();
    // //         final Object cr = Cache.get(iconKey);
    // //         if (cr instanceof CacheEntry) {
    // //             final CacheEntry ce = (CacheEntry) cr;
    // //             iconImage = ce.getCachedImage();
    // //             if (iconImage == null) {
    // //                 // TODO: below is re-loading cache, as opposed to just filling cache from the existing entry
    // //                 // -- move load code to CacheEntry?  Subseqent requests for this image may not be able to re-constitute
    // //                 iconImage = loadImage(ImageSource.create(ce.getFile()), listener); // todo: handle eom?
    // //                 // todo: stuff back into cache, or did loadImage do that? [yes, see above, handled badly]
    // //             }
    // //         }
    //         }

    //         private ImageSource(Object o, Resource r, Object readable) {
    //             this.original = o;
    //             this.resource = r;
    //             this.readable = readable;
    //             this.key = keyFromReadable(readable);
    //         }
    //         ImageSource(Resource r) {
    //             this(r, r, r.getImageSource());
    //         }
    //         ImageSource(File f) {
    //             this(f, null, f);
    //         }
    //         ImageSource(URL u) {
    //             this(u, null, u);
    //             isThumbshot = u.toString().startsWith(URLResource.THUMBSHOT_FETCH);
    //         }

    //         /** @return a special source that refers to something in the local disk cache */
    //         public static ImageSource create(CacheEntry ce) {
    //             return new ImageSource(ce);
    //         }
