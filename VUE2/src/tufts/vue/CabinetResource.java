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
 * @version $Revision: 1.33 $ / $Date: 2007-11-28 16:08:01 $ / $Author: peter $
 * @author  Mark Norton
 */
public class CabinetResource extends URLResource
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(CabinetResource.class);
    
    //  Filing metadata property keywords.
    public static final String MD_NAME = DublinCoreConstants.DC_FIELDS[DublinCoreConstants.DC_TITLE];
    //public static final String MD_TIME = "md.filing.time";
    public static final String MD_OWNER = DublinCoreConstants.DC_FIELDS[DublinCoreConstants.DC_CREATOR];
    //public static final String MD_READ = "md.filing.read";
    //public static final String MD_WRITE = "md.filing.write";
    //public static final String MD_APPEND = "md.filing.append";
    //public static final String MD_LENGTH = "md.filing.length";
    public static final String MD_MIME = DublinCoreConstants.DC_FIELDS[DublinCoreConstants.DC_TYPE];
    
    private osid.filing.CabinetEntry entry = null;  //  This is not marshalled.
    
    //private int type = Resource.FILE;           //  Resource type.
    private boolean selected = false;           //  Selection flag.
             //  Object specification, usually URL.
    private String extension = null;                  //  Extension.
    
    
    /**
     *  Create a new instance of CabinetResource.  This creator should be used when
     *  the resource is being unmarshalled.
     */
    public CabinetResource () {
        this.entry = entry;
        //this.type = Resource.URL;
        if (DEBUG.RESOURCE) out("RESTORED");
    }
    
    /** 
     *  Creates a new instance of CabinetResource .  This creator is used when the
     *  resource is part of a CabinetNode in a JTree.
     */
    private CabinetResource(osid.filing.CabinetEntry entry) {
        this.entry = entry;

        getSpec();

        //this.getEntry();
        //this.getProperties();
        //this.getSpec();
        //this.getExtension();
        //this.getTitle();
        
    }

    // todo: shouldn't be public -- eventually everything should go thru factory
    public static CabinetResource create(osid.filing.CabinetEntry entry) {
        return new CabinetResource(entry);
    }
    
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
    
    
    
    public void setExtension(String extenstion) {
        this.extension = extension;
    }
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
    
    
    
    /*
     *  Return the resource specification.  For cabinet resources, this is URL of either
     *  a local or remote file.
     *
     *  @author Mark Norton
     */
    @Override
    public String getSpec() {
        //  Check for a restored resource.
        final String hasSpec = super.getSpec();
        final osid.filing.CabinetEntry e = getEntry();

        // It is best to call setSpec lazily right now, as MapResource
        // set's the reference created time and all that in setSpec,
        // and no point in doing that until somebody actually attempts
        // to grab the resource and do something with it -- otherwise
        // the creation time will be set for every object displayed in
        // the filing browser when the browser is created, as opposed
        // to when a user drags one out.
        
        if (e == null || hasSpec != SPEC_UNSET)
            return hasSpec;
        else {
            //  Check for each of the four possible cases.
            if (e instanceof tufts.oki.remoteFiling.RemoteByteStore)
                setSpec(((RemoteByteStore)e).getUrl());
            if (e instanceof tufts.oki.remoteFiling.RemoteCabinet)
                setSpec(((RemoteCabinet)e).getUrl());
            if (e instanceof tufts.oki.localFiling.LocalByteStore)
                setSpec(((LocalByteStore)e).getUrl());
            if (e instanceof tufts.oki.localFiling.LocalCabinet)
                setSpec(((LocalCabinet)e).getUrl());

            final String spec = super.getSpec();
            //setProperty("URL", spec);
            
            //final String title = getTitle();
            //if (title == null || title.length() == 0) {
                final String fname;
                if (spec.startsWith("file://"))
                    fname = spec.substring(7);
                else
                    fname = spec;
                try {
                    setTitle(new File(fname).getName());
                } catch (Throwable t) { t.printStackTrace(); }
                //}

            return spec;
        }
    }

    /*
    public void setSpec(final String spec) {
        super.setSpec(spec);
        if (DEBUG.RESOURCE) tufts.Util.printStackTrace("SET SPEC");
    }
    */
    
    /*
     *  Return the title of this cabinet resource.  The cabinet entry display name is
     *  used as the title.
     *
     *  @author Mark Norton
    public String getTitle() {
        if (this.entry == null || mTitle != null)
            return this.mTitle;
        else {
            try {
                this.mTitle = entry.getDisplayName();
            }
            catch (osid.filing.FilingException ex) {
                //  This can't fail.
            }
            return this.mTitle;
        }
    }
     */
    
//     /**
//      *  Return the tool tip information for this resource.  This is currently stubbed
//      *  to return an empty string.
//      *
//      *  @author Mark Norton
//      */
//     public String getToolTipInformation() {
//         return new String ("");
//     }
    
//     /**
//      *  Return the resource type.
//      *
//      *  @author Mark Norton
//      */
//     public int getType() {
//         return this.type;
//     }
    
    /**
     *  Return the selected flag.
     *
     *  @author Mark Norton
     */
    public boolean isSelected() {
        return selected;
    }
    
    /**
     *  Set the selected flag to the value provided.
     *
     *  @author Mark Norton
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    
    /**
     *  Return the cabinet entry associated with this resource.
     *
     *  @author Mark Norton
     */
    public osid.filing.CabinetEntry getEntry() {
        return this.entry;
    }
    
    public void setEntry(osid.filing.CabinetEntry entry){
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
    public Object getPreview() {    	
        //return GUI.getSystemIconForExtension(getExtension(), 128);
        return super.getFileIconImage();
    }
    
    //*/
    
}
