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

/*
 * CabinetResource.java
 *
 * Created on January 23, 2004, 2:32 PM
 * Updated on February 03, 2004 - Added support for marshalling.
 */

package tufts.vue;

import osid.filing.*;
import java.util.*;
import java.io.*;
import java.net.*;
import tufts.oki.remoteFiling.*;
import tufts.oki.localFiling.*;
import tufts.vue.gui.GUI;

import javax.swing.*;
import java.awt.*;

/**
 *  A wrapper for CabinetEntry objects which can be used as the user object in a 
 *  DefaultMutableTreeNode.  It implements the Resource interface specification.
 *
 * @version $Revision: 1.40 $ / $Date: 2008-06-30 20:52:55 $ / $Author: mike $
 * @author  Mark Norton
 */
public class CabinetResource extends URLResource
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(CabinetResource.class);
    
    //  Filing metadata property keywords.
    public static final String MD_NAME = DublinCoreConstants.DC_FIELDS[DublinCoreConstants.DC_TITLE];
    public static final String MD_OWNER = DublinCoreConstants.DC_FIELDS[DublinCoreConstants.DC_CREATOR];
    public static final String MD_MIME = DublinCoreConstants.DC_FIELDS[DublinCoreConstants.DC_TYPE];
    //public static final String MD_TIME = "md.filing.time";
    //public static final String MD_READ = "md.filing.read";
    //public static final String MD_WRITE = "md.filing.write";
    //public static final String MD_APPEND = "md.filing.append";
    //public static final String MD_LENGTH = "md.filing.length";
    
    //private final transient osid.filing.CabinetEntry entry;  //  This is not marshalled.
    private transient osid.filing.CabinetEntry entry;  //  This is not marshalled.
    
    
    /**
     *  Create a new instance of CabinetResource.  This creator should be used when
     *  the resource is being unmarshalled.
     */
    public CabinetResource() {
        //if (DEBUG.RESOURCE) Log.debug("empty instance: " + this);
        //this.entry = null; // entries are currently never marshalled, so we don't need this for the moment
    }
    
    /** 
     *  Creates a new instance of CabinetResource .  This creator is used when the
     *  resource is part of a CabinetNode in a JTree.
     */
    private CabinetResource(final osid.filing.CabinetEntry e) {
        this.entry = e;

             if (e instanceof tufts.oki.remoteFiling.RemoteCabinetEntry) initFrom((RemoteCabinetEntry) e);
        else if (e instanceof tufts.oki.localFiling.LocalByteStore)      setSpecByKnownFile(((LocalByteStore)e).getFile(), false);
        else if (e instanceof tufts.oki.localFiling.LocalCabinet)        setSpecByKnownFile(((LocalCabinet)e).getFile(), true);
    }

    private void initFrom(RemoteCabinetEntry e) {
        setTitle(e.getDisplayName());
        if (e instanceof tufts.oki.remoteFiling.RemoteByteStore) {
            setClientType(Resource.FILE);
            setSpec(((RemoteByteStore)e).getUrl());            
        } else {
            setClientType(Resource.DIRECTORY);
            setSpec(((RemoteCabinet)e).getUrl());            
        }
    }

    // todo: shouldn't be public -- eventually everything should go thru factory
    static CabinetResource create(final osid.filing.CabinetEntry entry) {
        final CabinetResource r = new CabinetResource(entry);
        if (DEBUG.RESOURCE)
            Log.debug("created: " + r + "; from: " + entry);
//         else
//             if (DEBUG.Enabled && tufts.Util.isWindowsPlatform()) System.err.print(".");
        return r;
    }

//     static CabinetResource create(java.io.File file) {
//         final CabinetResource r = new CabinetResource();
//         if (DEBUG.RESOURCE) Log.debug("created: " + r + "; from: " + tufts.Util.tags(file));
//         return r;
//     }
    
    
    /**
     *  Display the content associated with this resource.  For a cabinet resource,
     *  a call is made to show a URL.
     *
     *  @author Mark Norton
     */
    public void displayContent() {
        try {
            if (this.entry instanceof tufts.oki.remoteFiling.RemoteByteStore) {
                openRemoteByteStore();
            } else {   
                super.displayContent();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    /**
     *  Return the file extension for this resource.  In general, cabinets don't have
     *  an extension, since they are directories.  In those cases, the empty string is
     *  returned.
     *
     *  @author Mark Norton
     */
//     public String getExtension() {
        
//         //  Check for a restored resource.
//         /**
//         if(extension == null || extension.length() == 0)
//             extension = "none";
//          */
//         //if (this.extension != null && this.entry == null )
//         if (this.extension != null) {
//             return this.extension;
//         } else if (getSpec() == SPEC_UNSET) {
//             return "<?>";
//         } else {
//             URL url = null;
//             try {
//                 url = new URL (getSpec());      //  Get the URL of this cabinet.
//             } catch (java.net.MalformedURLException ex) {
//                 //if (DEBUG.Enabled) ex.printStackTrace();
//                 Log.warn("Illegal URL on this platform: " + getSpec());
//             }

//             if (url == null) {
//                 // this can happen if a Windows file URL with a C: specifier
//                 // in it is used on Mac, where "file://C:\foo\bar" is NOT a legal URL.
//                 return this.extension = "???";
//             }
                

//             File file = new File(url.getFile());  //  Extract the file portion.
            
// //             String name = file.getName();       //  Get the filename with out path.
// //             String ext = null;
// //             if (name.lastIndexOf('.') > -1) 
// //                 ext = name.substring (name.lastIndexOf ('.')+1);  //  Extract extention.

// //             if (tufts.Util.isMacPlatform() && "app".equals(ext))
// //                 this.extension = "app";
// //             else if (file.isDirectory())
// //                 this.extension = "dir";
// //             else if (ext != null)
// //                 this.extension = ext;
// //             else if (name.length() > 0)
// //                 this.extension = "none";
// //             else
// //                 return null;

                        
//             if (file.isDirectory())
//                 this.extension = new String ("dir");              //  Directories don't have extensions.
//             else {
//                 String name = file.getName();       //  Get the filename with out path.
//                 if(name.lastIndexOf('.')> -1) 
//                     this.extension = name.substring (name.lastIndexOf ('.')+1);  //  Extract extention.
//                 else if(name.length() > 0)
//                     this.extension = "none";
//                 else 
//                     return null;  // this is case where there is no file.  useful for castor save/restore
//             }

//             //if (file.getName().charAt(0) == '#')
//             //System.out.format("FILE ext %-10s for %s\n", extension, file);

//             return this.extension;
//         }
//     }
    
    
    
//     public void setExtension(String extenstion) {
//         this.extension = extension;
//     }
    /**
     *  Return the metadata properties associated with this object.  Metadata is extracted
     *  from the various flavors of CabinetEntry and collected into a Properties object,
     *  which is returned.  Each metadata element has a property keyword defined above.
     *
     *  @author Mark Norton
     */
    public PropertyMap getFilingProperties() {
        PropertyMap props = new PropertyMap();
        
        
        //  Check for a restored resource.
        if (this.entry == null)
            return super.getProperties();//(Properties)this.mProperties;
        else {
            try {if (this.entry instanceof tufts.oki.remoteFiling.RemoteByteStore)
                if (this.entry instanceof RemoteByteStore) {
                    RemoteByteStore bs = (RemoteByteStore) this.entry;
                    props.setProperty(CabinetResource.MD_NAME, bs.getDisplayName());
                   // props.setProperty(CabinetResource.MD_TIME,Long.toString(bs.getLastAccessedTime().getTimeInMillis()));
                    props.setProperty(CabinetResource.MD_OWNER, bs.getOwner().getDisplayName());
                    /**
                    if (bs.isReadable())
                        props.setProperty(CabinetResource.MD_READ, "true");
                    else
                        props.setProperty(CabinetResource.MD_READ, "false");
                    if (bs.isWritable())
                        props.setProperty(CabinetResource.MD_WRITE, "true");
                    else
                        props.setProperty(CabinetResource.MD_WRITE, "false");
                    props.setProperty(CabinetResource.MD_LENGTH, String.valueOf(bs.length()));
                     */
                    //props.setProperty(CabinetResource.MD_MIME, bs.getMimeType());

                }
                else if (this.entry instanceof RemoteCabinet) {
                    RemoteCabinet cab = (RemoteCabinet) this.entry;
                    props.setProperty(CabinetResource.MD_NAME, cab.getDisplayName());
                   // props.setProperty(CabinetResource.MD_TIME, cab.getLastAccessedTime().toString());
                }
                else if (this.entry instanceof LocalByteStore) {
      
                    LocalByteStore bs = (LocalByteStore) this.entry;
                    props.setProperty(CabinetResource.MD_NAME, bs.getDisplayName());
                    //props.setProperty(CabinetResource.MD_TIME, Long.toString(bs.getLastAccessedTime().getTimeInMillis()));
                    props.setProperty(CabinetResource.MD_OWNER, bs.getOwner().getDisplayName());
                    /**
                    if (bs.isReadable())
                        props.setProperty(CabinetResource.MD_READ, "true");
                    else
                        props.setProperty(CabinetResource.MD_READ, "false");
                    if (bs.isWritable())
                        props.setProperty(CabinetResource.MD_WRITE, "true");
                    else
                        props.setProperty(CabinetResource.MD_WRITE, "false");
                     
                    props.setProperty(CabinetResource.MD_LENGTH, String.valueOf(bs.length()));
                     */
                   // props.setProperty(CabinetResource.MD_MIME, bs.getMimeType());
                     
                }
                else if (this.entry instanceof LocalCabinet) {
                    LocalCabinet cab = (LocalCabinet) this.entry;
                    props.setProperty(CabinetResource.MD_NAME, cab.getDisplayName());
                    //props.setProperty(CabinetResource.MD_TIME, cab.getLastAccessedTime().toString());
                }
            }
            catch (osid.filing.FilingException ex1) {
                //  If we get an exception, just return what we got.
            }
            catch (osid.shared.SharedException ex2) {
                //  If we get an exception, just return what we got.
            }
            //this.mProperties = props;            //  Cache the metadata.
            System.err.println("Unimplemented: filing properties on resource");
            return props;
        }
    }
    
//     /*
//      *  Return the resource specification.  For cabinet resources, this is URL of either
//      *  a local or remote file.
//      *
//      *  @author Mark Norton
//      */
//     @Override
//     public String getSpec() {
//         //  Check for a restored resource.
//         final String existingSpec = super.getSpec();
//         final osid.filing.CabinetEntry e = getEntry();

//         if (e == null || existingSpec != SPEC_UNSET) {
//             return existingSpec;
//         } else {
//                  if (e instanceof tufts.oki.remoteFiling.RemoteByteStore)    setSpec(((RemoteByteStore)e).getUrl());
//             else if (e instanceof tufts.oki.remoteFiling.RemoteCabinet)      setSpec(((RemoteCabinet)e).getUrl());
//             else if (e instanceof tufts.oki.localFiling.LocalByteStore)      setSpecByKnownFile(((LocalByteStore)e).getFile(), false);
//             else if (e instanceof tufts.oki.localFiling.LocalCabinet)        setSpecByKnownFile(((LocalCabinet)e).getFile(), true);

//             return super.getSpec();
//         }
//     }

//     /**
//      *  Return the selected flag.
//      *
//      *  @author Mark Norton
//      */
//     public boolean isSelected() {
//         return selected;
//     }
    
//     /**
//      *  Set the selected flag to the value provided.
//      *
//      *  @author Mark Norton
//      */
//     public void setSelected(boolean selected) {
//         this.selected = selected;
//     }
    
    /**
     *  Return the cabinet entry associated with this resource.
     *
     *  @author Mark Norton
     */
    public osid.filing.CabinetEntry getEntry() {
        return this.entry;
    }
    
    public void setEntry(osid.filing.CabinetEntry entry){
        if (DEBUG.RESOURCE) out("setEntry: " + entry);
        this.entry = entry;
    }
    
    private void openRemoteByteStore() throws IOException,osid.filing.FilingException{
        RemoteByteStore rbs = (RemoteByteStore)this.entry;
        String fileName = rbs.getFullName();
        File tempFile = new File( VUE.getSystemProperty("java.io.tmpdir"),rbs.getDisplayName());
        FileOutputStream fos = new FileOutputStream(tempFile);
        fos.write(rbs.getBytes());
        fos.close();
        VueUtil.openURL(tempFile.getAbsolutePath());
        
    }

    @Override
    protected String paramString() {
        return entry == null ? "NO-CABINET-ENTRY; " : "";
    }
    
    
    
//     @Override
//     public Object getPreview() {

//         // We could not override this at all, and just let us see full
//         // image previews of local filesystem images in "Browse: My
//         // Computer".  At this point this may simply be an historical
//         // artifact of how we did things, but skipping all those image
//         // fetches for just the preview until the content is on the
//         // map puts alot less stress on our memory and threads.
        
//         if (isCached())
//             return super.getPreview();
//         else
//             return getFileIconImage();
//     }
    
    
}
