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
 * OsidAssetResource.java
 *
 * Created on March 24, 2004, 11:21 AM
 */

package tufts.vue;

import java.net.URL;
import tufts.Util;


/**
 * A wrapper for an implementation of the Repository OSID.  A osid.dr.Asset which can be used as the user 
 * object in a DefaultMutableTreeNode.  It implements the Resource interface specification.
 */
public class Osid2AssetResource extends URLResource
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(Osid2AssetResource.class);
    
    private static final org.osid.shared.Type BrowsePartType = new edu.tufts.vue.util.Type("mit.edu","partStructure","URL");
    private static final org.osid.shared.Type ThumbnailPartType = new edu.tufts.vue.util.Type("mit.edu","partStructure","thumbnail");
    private static final org.osid.shared.Type ThumbnailPartType2 = new edu.tufts.vue.util.Type("mit.edu","partStructure","thumbnailURL");
    private static final org.osid.shared.Type LargeImagePartType = new edu.tufts.vue.util.Type("mit.edu","partStructure","largeImage");
    private static final org.osid.shared.Type LargeImagePartType2 = new edu.tufts.vue.util.Type("mit.edu","partStructure","largeImageURL");


    // SEE Fedora Impl: edu/tufts/osidimpl/repository/fedora_2_0/ImageRecordStructure.java
    private static final org.osid.shared.Type FedoraImagePartType = // TODO: temporary hack for Fedora (also: note differing authority conventions!
        new edu.tufts.vue.util.Type("mit.edu","partStructure","mediumImage");
    // TODO: Google Global is currently using mit.edu authority also for URL part type...
    
    private osid.OsidOwner owner = null;
    private org.osid.OsidContext context = null;
    private org.osid.repository.Asset asset = null;
	
    // default constructor needed for Castor
    public Osid2AssetResource() {}
	
    /** @deprecated: don't know what this is for, but storing title a third time is more than redundant -- get rid of this */
    public String getLoadString() {
        return getTitle();
    }
	
    public void setLoadString() {}
	
    public Osid2AssetResource(org.osid.repository.Asset asset, org.osid.OsidContext context)
        throws org.osid.repository.RepositoryException 
    {
        super();
        try {
            this.context = context;
            this.asset = asset;
            getProperties().holdChanges();
            setAsset(asset);
        } catch (Throwable t) {
            System.out.println(t.getMessage());
        } finally {
            getProperties().releaseChanges();
        }
    }
    
    /**
       The Resoource Title maps to the Asset DisplayName.
       The Resource Spec maps to the value in an info field with a published Id.  This should be changed
       to a field with a published name and a published InfoStructure Type after the OSID changes.
    */
	
    public void setAsset(org.osid.repository.Asset asset) //throws org.osid.repository.RepositoryException 
    {
        this.asset = asset;
        try {
            if (DEBUG.RESOURCE) dumpField("setAsset", Util.tags(asset) + "; " + Util.tags(asset.getDisplayName()));
            setAssetImpl(asset);
        } catch (Throwable t) {
            Log.error("setAsset " + Util.tags(asset) + "; in " + this, t);
        }
    }

    private static String quoteF(Object o) {
        if (o == null)
            return "null";
        else
            return quoteF(o.toString());
    }
    
    private static String quoteF(String s) {
        if (s == null || s.length() == 0)
            return "";
        else
            return "\"" + s + "\" ";
    }
    private static String quoteL(String s) {
        if (s == null || s.length() == 0)
            return "";
        else
            return " \"" + s + "\"";
    }

    private static String name(String s) {
        if (s == null || s.length() == 0)
            return "()";
        else
            return "(" + s + ")";
    }
    

    private static String fmt(org.osid.shared.Type t) {
        //return t.getAuthority() + "/" + t.getDomain() + "/" + t.getKeyword() + quoteL(t.getDescription())
        return t.getAuthority() + "/" + t.getDomain() + "/" + t.getKeyword()
            ;
    }

    private void setAssetImpl(org.osid.repository.Asset asset)
        throws org.osid.repository.RepositoryException, org.osid.shared.SharedException
    {
        //java.util.Properties osid_registry_properties = new java.util.Properties();
			
        setClientType(Resource.ASSET_OKIREPOSITORY);
        String displayName = asset.getDisplayName();
        setTitle(displayName);
        setProperty("title", displayName);
        org.osid.repository.RecordIterator recordIterator = asset.getRecords();
        while (recordIterator.hasNextRecord()) {
            final org.osid.repository.Record record = recordIterator.nextRecord();
            final org.osid.repository.PartIterator partIterator = record.getParts();
            String recordDesc = null;
            if (DEBUG.DR) {
                if (record == null)
                    recordDesc = "<NULL-RECORD>";
                else
                    recordDesc =
                        record
                        //+ "\nRepository=" + asset.getRepository().getDisplayName()
                        + "\nID=" + record.getId()
                        + "\nRecordStruct=" + quoteF(record.getRecordStructure())
                        + "\n";
//                     recordDesc = quoteF(record.getDisplayName()) + record
//                         + "\nID= " + quoteF(record.getId().getIdString()) + record.getId()
//                         + "\nRecordStruct=" + quoteF(record.getRecordStructure().getDisplayName())
//                         + " RSid=" + quoteF(record.getRecordStructure().getId().getIdString())
//                         + " " + record.getRecordStructure();
            }
            
            int partIndex = 0; // for debug
            while (partIterator.hasNextPart()) {
                final org.osid.repository.Part part = partIterator.nextPart();
                final org.osid.repository.PartStructure partStructure = part.getPartStructure();
                if ( (part != null) && (partStructure != null) ) {
                    org.osid.shared.Type partStructureType = partStructure.getType();
                    java.io.Serializable value = part.getValue();
					
                    final String description = partStructureType.getDescription();
					
                    if (DEBUG.DR) {
                                            
                        recordDesc +=
                            String.format("\nPART%3d %-35s %-23s %-15s (%s)%s",
                                          partIndex++,
                                          fmt(partStructureType),
                                          '"' + partStructureType.getDescription() + '"',
                                          name(part.getDisplayName()),
                                          part,
                                          (DEBUG.META ?
                                           (" partStructure name/desc="
                                            + quoteF(partStructure.getDisplayName())
                                            + quoteF(partStructure.getDescription()))
                                           : "")
                                          );
                    }
					
                    // metadata discovery, allow for Type descriptions
					
                    String key;
					
                    if (description != null && description.trim().length() > 0) {
                        key = description;
                        //if (DEBUG.DR) key += "|d";
                    } else {
                        key = partStructureType.getKeyword();
                        //if (DEBUG.DR) key += "|k";
                        /*
                          if (DEBUG.DR) {
                          String idName = record.getId().getIdString();
                          if (idName == null || idName.trim().length() == 0 || idName.indexOf(':') >= 0)
                          key += "|k";
                          else
                          key += "." + idName;
                          }
                        */
                    }                
					
                    if (key == null) {
                        Log.warn(this + " Asset Part [" + part + "] has null key.");
                        continue;
                    }
					
                    if (value == null) {
                        Log.warn(this + " Asset Part [" + key + "] has null value.");
                        continue;
                    }
					
                    if (value instanceof String) {
                        String s = ((String)value).trim();
						
                        // Don't add field if it's empty
                        if (s.length() <= 0)
                            continue;
						
                        if (s.startsWith("<p>") && s.endsWith("</p>")) {
                            // Ignore empty HTML paragraphs
                            String body = s.substring(3, s.length()-4);
                            if (body.trim().length() == 0) {
                                if (DEBUG.DR)
                                    value = "[empty <p></p> ignored]";
                                else
                                    continue;
                            }
                        }
                    }
					
                    addProperty(key, value);
					
                    // TODO: Fedora OSID impl is a bit of a mess: most every part is a URL part type,
                    // and it's only by virtue of the fact that the last one
                    // we process HAPPENS to be the fullView, that this even works at all!

                    // TODO: MFA appears to be duplicating a sub-set of it's part-structure's
                    // E.g., thumbnail/mediumImage/largeImage/URL all appear twice,
                    // resulting in repeated meta-data.  We can't just remove all key repeats,
                    // as some repeated keys are valid: e.g. there can be hundreds of "Subject"
                    // keys.  SMF 2008-05-26
					
                    if (BrowsePartType.isEqual(partStructureType)) {
                        setURL_Browse(value.toString());
                        //setSpec(s);
                    } else if (ThumbnailPartType.isEqual(partStructureType) || ThumbnailPartType2.isEqual(partStructureType)) {
                        setURL_Thumb(value.toString());
						
                        /*
                          if (value instanceof String) {
                          //setPreview(new javax.swing.JLabel(new javax.swing.ImageIcon(new java.net.URL((String)ser))));
                          this.icon = (String) value;
                          } else {
                          //setPreview(new javax.swing.JLabel(new javax.swing.ImageIcon((java.awt.Image)ser)));
                          //this.icon = new javax.swing.ImageIcon((java.awt.Image)ser);
                          }
                        */
                    } else if (LargeImagePartType.isEqual(partStructureType) || FedoraImagePartType.isEqual(partStructureType) || LargeImagePartType2.isEqual(partStructureType)) {
                        setURL_Image(value.toString());
                        // handle large image
                    }
                }
            }
            
            if (DEBUG.DR) addProperty("~Record", recordDesc);
        }

        // This is a default catch-all: if we've failed to find the right part-types,
        // and/or they were improperly specified, if there's any part with the name 'URL',
        // use that for the spec.
        //if (getSpec() == SPEC_UNSET && mURL_Browse == null) {
        if (getSpec() == SPEC_UNSET) {
            String defaultURL = getProperty("URL");
            if (defaultURL != null) {
                if (DEBUG.RESOURCE && DEBUG.META) Log.warn("Osid2AssetResource failsafe: using URL property " + defaultURL);
                setSpec(defaultURL);
            }
        }
    }

    
    public org.osid.repository.Asset getAsset() 
    {
        return this.asset;
    }    
	
}