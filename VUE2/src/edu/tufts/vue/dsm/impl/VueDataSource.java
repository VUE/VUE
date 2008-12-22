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
package edu.tufts.vue.dsm.impl;

import tufts.vue.PropertyEntry;
import java.util.*;

public class VueDataSource implements edu.tufts.vue.dsm.DataSource
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(VueDataSource.class);
    
    private edu.tufts.vue.dsm.OsidFactory factory = null;
    private Vector<PropertyEntry> _propertyList = null;
    private org.osid.shared.Id providerId = null;
    private org.osid.shared.Id dataSourceId = null;
    private String osidLoadKey = null;
    private String providerIdString = null; // to support loading datasource from castor
    private String dataSourceIdString = null; // to support loading datasource from castor
    // from Provider OSID implementation
    private String osidName = null;
    private String osidBindingVersion = null;
    private String providerDisplayName = null;
    private String providerDescription = null;
    private String creator = null;
    private String publisher = null;
    private java.util.Date releaseDate = null;
    private String license = null;
    private boolean requestsLicenseAcknowledgement = false;
    private org.osid.shared.Type[] rightTypes = null;
    private java.awt.Image icon16x16 = null;
    private boolean isConfigured = false;
    private String configurationUIHints = null;
    private boolean done = true; // dummy variable for castor.
    
    // from Manager loaded via Provider
    private org.osid.repository.RepositoryManager repositoryManager = null;
    private org.osid.repository.Repository repository = null;
    private org.osid.shared.Id repositoryId = null;
    private org.osid.shared.Type repositoryType = null;
    private String repositoryDisplayName = null;
    private String repositoryDescription = null;
    private boolean repositorySupportsUpdate = false;
    private boolean repositorySupportsSearch = false;
    private org.osid.shared.TypeIterator repositoryAssetTypes = null;
    private org.osid.shared.TypeIterator repositorySearchTypes = null;
    
    // Data Source Manager
    //private edu.tufts.vue.dsm.DataSourceManager dataSourceManager = null;
    private boolean includedState = false;
    
    // constructer required by castor
    
    public VueDataSource() {
		try {
			this.factory = VueOsidFactory.getInstance();
			this.dataSourceId = this.factory.getIdManagerInstance().createId();
		} catch (Throwable t) {			
		}
    }
    
    public VueDataSource(org.osid.shared.Id dataSourceId,
            org.osid.shared.Id providerId,
            boolean isIncludedInSearch)
            throws org.osid.repository.RepositoryException, org.osid.provider.ProviderException {
		
		try {
			this.factory = VueOsidFactory.getInstance();	
			this.providerId = providerId;
			this.dataSourceId = dataSourceId;
			this.includedState = isIncludedInSearch;
			setProviderValues();
			setRepositoryManager();
			setRelatedValues();
		} catch (Throwable t) {
			throw new org.osid.provider.ProviderException(org.osid.OsidException.CONFIGURATION_ERROR);
		}
    }
    
	public boolean hasUpdate()
	{
		try {
			org.osid.provider.Provider provider = null;
			provider = this.factory.getInstalledProvider(this.providerId);
			return (provider.needsUpdate());
		} catch (Throwable t) {
			return false;
		}
	}
	
    private void setProviderValues()
    throws org.osid.provider.ProviderException {
        org.osid.provider.Provider provider = null;
        provider = this.factory.getInstalledProvider(this.providerId);
        this.osidName = provider.getOsidName();
        this.osidBindingVersion = provider.getOsidBindingVersion();
        this.providerDisplayName = provider.getDisplayName();
        this.providerDescription = provider.getDescription();
        this.creator = provider.getCreator();
        this.publisher = provider.getPublisher();
        this.releaseDate = provider.getReleaseDate();
        this.license = provider.getLicense();
        this.requestsLicenseAcknowledgement = provider.requestsLicenseAcknowledgement();
        org.osid.shared.PropertiesIterator propertiesIterator = provider.getProperties();
        try {
            while (propertiesIterator.hasNextProperties()) {
                org.osid.shared.Properties props = propertiesIterator.nextProperties();
                org.osid.shared.ObjectIterator objectIterator = props.getKeys();
                while (objectIterator.hasNextObject()) {
                    // we could have an early exit but probably not worth it since the properties we want are likely to be last
                    String key = (String)objectIterator.nextObject();
                    try {
                        if (key.equals("icon16x16")) {
                            String path = factory.getResourcePath((String)props.getProperty(key));
                            this.icon16x16 = new javax.swing.ImageIcon(path).getImage();
                        }
                    } catch (Throwable t) {
                        //t.printStackTrace();
                        System.out.println("Did not find resource");
                    }
                    //System.out.println("Getting properties.............." + key);
                    if (key.equals("configuration")) {
                        String config = (String)props.getProperty(key);
                        config = replaceAll(config,"&lt;","<");
                        config = replaceAll(config,"&gt;",">");
                        this.configurationUIHints = config;
                        //System.out.println("Fixed up " + config);
                        this.isConfigured = this.configurationUIHints != null;
                    }
                    if (key.equals("loadKey")) {
                        this.osidLoadKey = (String)props.getProperty(key);
                    }
                }
            }
        } catch (Throwable t) {
            
        }
    }
    
    private String replaceAll(String original, String old, String replacement) {
        String result = original;
        try {
            int length = old.length();
            
            int index = -1;
            while ((index = result.indexOf(old)) != -1) {
                String before = result.substring(0,index);
                String after = "";
                try {
                    after = result.substring(index + length);
                } catch (Exception ex) {
                }
                result = before + replacement + after;
            }
        } catch (Exception ex) {
            
        }
        return result;
    }
    
    private void setRepositoryManager()
    throws org.osid.provider.ProviderException {
        this.repositoryId = edu.tufts.vue.util.Utilities.getRepositoryIdFromLoadKey(this.osidLoadKey);
        this.repositoryManager = factory.getRepositoryManagerInstance(this.osidLoadKey);
    }
    
    private void setRelatedValues()
    throws org.osid.provider.ProviderException
    {
        if (this.repositoryManager == null) {
            Log.error("setRelatedValues: null repositoryManager; aborting init of " + this);
            return;
        }
        
        try {
            //Log.info("searching for repository with ID [" + repositoryId.getIdString() + "]");
            this.repository = this.repositoryManager.getRepository(this.repositoryId);
            //System.out.println("got repository " + repostiory);
        } catch (Throwable t) {
            Log.warn("getRepository(" + idString(repositoryId) + "); " + t);
            Log.warn(String.format("repositoryManager.getRepository(ID) failed; loadKey[%s]; manually searching", this.osidLoadKey));
            // special case for when the Manager implementation doesn't offer this method
            try {
                org.osid.repository.RepositoryIterator repositoryIterator = this.repositoryManager.getRepositories();
                while (repositoryIterator.hasNextRepository()) {
                    repository = repositoryIterator.nextRepository();
                    if (repositoryId.isEqual(repository.getId())) {
                        this.repository = repository;
                        //System.out.println("Set repository " + this.repository);
                    }
                }
                //if (repository != null) Log.info("Check of all repositories found " + repository);
            } catch (Throwable t1) {
                Log.error("Load by check of all repositories failed:", t);
                //throw new org.osid.provider.ProviderException(org.osid.shared.SharedException.UNKNOWN_ID);
            }
        }
        if (this.repository == null) {
            Log.error("setRelatedValues: null repository; aborting init of " + this);
            return;
        }
        // call Repository to answer these
        try {
            this.repositoryDisplayName = this.repository.getDisplayName();
        } catch (Throwable t) {
            this.repositoryDisplayName = "unconfigured";
        }
        
        try {
            this.repositoryDescription = this.repository.getDescription();
        } catch (Throwable t) {
            edu.tufts.vue.util.Logger.log(t,"in method edu.tufts.vue.dsm.VueDataSource calling Repository.getDescription()");
        }
        
        try {
            this.repositoryType = this.repository.getType();
        } catch (Throwable t) {
            edu.tufts.vue.util.Logger.log(t,"in method edu.tufts.vue.dsm.VueDataSource calling Repository.getType()");
        }
        
        try {
            this.repositoryId = this.repository.getId();
        } catch (Throwable t) {
            edu.tufts.vue.util.Logger.log(t,"in method edu.tufts.vue.dsm.VueDataSource calling Repository.getId()");
        }
        
        try {
            this.repositoryAssetTypes = this.repository.getAssetTypes();
        } catch (Throwable t) {
            edu.tufts.vue.util.Logger.log(t,"in method edu.tufts.vue.dsm.VueDataSource calling Repository.getAssetTypes()");
        }
        
        try {
            this.repositorySearchTypes = this.repository.getSearchTypes();
			this.repositorySupportsSearch = this.repositorySearchTypes.hasNextType();
        } catch (Throwable t) {
            edu.tufts.vue.util.Logger.log(t,"in method edu.tufts.vue.dsm.VueDataSource calling Repository.getSearchTypes()");
        }
        
        try {
            this.repositorySupportsUpdate = this.repository.supportsUpdate();
        } catch (Throwable t) {
            edu.tufts.vue.util.Logger.log(t,"in method edu.tufts.vue.dsm.VueDataSource calling Repository.supportsUpdating()");
        }
	}
    
    //===================================================================================================================
    // Accessor Methods
    //===================================================================================================================
    
    
    public org.osid.repository.Repository getRepository() {
        return this.repository;
    }
    
    public org.osid.shared.Id getProviderId() {
        return this.providerId;
    }
    
    public org.osid.shared.Id getId() {
        return this.dataSourceId;
    }
    
    public void setProviderId(org.osid.shared.Id providerId) {
        this.providerId = providerId;
    }
    
    public String getOsidName() {
        return this.osidName;
    }
    
  
    public String getOsidVersion() {
        return this.osidBindingVersion;
    }
    
    public String getOsidLoadKey() {
        return this.osidLoadKey;
    }
    
    public void setOsidLoadKey(String osidLoadKey) {
        this.osidLoadKey = osidLoadKey;
    }
    
    public String getProviderDisplayName() {
        return this.providerDisplayName;
    }
    
    public void setProviderDisplayName(String providerDisplayName) {
        this.providerDisplayName = providerDisplayName;
    }
    public String getProviderDescription() {
        return this.providerDescription;
    }
    
    public String getCreator() {
        return this.creator;
    }
    
    public String getPublisher() {
        return this.publisher;
    }
    
    public java.util.Date getReleaseDate() {
        return this.releaseDate;
    }
    
    public String getLicense() {
        return this.license;
    }
    
    public boolean requestsLicenseAcknowledgement() {
        return this.requestsLicenseAcknowledgement;
    }
    
    public org.osid.shared.Id getRepositoryId() {
        return this.repositoryId;
    }

    public static String idString(org.osid.shared.Id id) {
        String idString = "<null-id>";
        if (id != null) {
            try {
                idString = id.getIdString();
            } catch (Throwable t) {
                idString = "<idString? " + t + ">";
            }
        }
        return idString;
    }
    
    
    public org.osid.shared.Type getRepositoryType() {
        return this.repositoryType;
    }
    
    public String getRepositoryDisplayName() {
        if (this.repositoryDisplayName == null || this.repositoryDisplayName.trim().length() < 1) {
            if (this.providerDisplayName == null) {
                return "(Repository Name Unknown)";
            } else
                return "(" + providerDisplayName + ": unconfigured)";
            //return "(Name Unknown; Provider: " + providerDisplayName + ")";
        } else
            return this.repositoryDisplayName;
    }
    
    public boolean isOnline() {

        if (repositoryDisplayName == null || repositoryDisplayName.trim().length() < 1) {
            return false;
        } else {
            return true;
        }

//         try {
//             this.repository.getDisplayName();
//             return true;
//         } catch (Throwable t) {
//             // ignore since we are going to return false for any failure
//         }
//        return false;
        
    }
    
    public String getRepositoryDescription() {
        return this.repositoryDescription;
    }
    
    public boolean isIncludedInSearch() {
        return this.includedState;
    }
    
    public void setIncludedInSearch(boolean isIncluded) {
        this.includedState = isIncluded;
    }
    
    public boolean supportsUpdate() {
        return this.repositorySupportsUpdate;
    }
    
    public boolean supportsSearch() {
        return this.repositorySupportsSearch;
    }
    
    public org.osid.shared.TypeIterator getAssetTypes() {
        return this.repositoryAssetTypes;
    }
    
    public org.osid.shared.TypeIterator getSearchTypes() {
        return this.repositorySearchTypes;
    }
    
    public java.awt.Image getIcon16x16() {
        return this.icon16x16;
    }
    
    public boolean hasConfiguration() {
        return this.isConfigured;
    }
    
    public String getConfigurationUIHints() {
        return this.configurationUIHints;
    }
    public boolean getIncludedState() {
        return this.includedState;
    }
    public void setIncludedState(boolean includedState) {
        this.includedState = includedState;
    }
    
    public String getProviderIdString() {
        if(this.providerIdString== null) {
            try {
                return providerId.getIdString();
            } catch (Throwable t) {
                Log.error("getProviderIdString; loading data sources from XML", t);
                //edu.tufts.vue.util.Logger.log(t,"loading data sources from XML");
            }
        }
        return providerIdString;
    }
    
    public void setProviderIdString(String providerIdString) {
        try {
            providerId =  edu.tufts.vue.dsm.impl.VueOsidFactory.getInstance().getIdManagerInstance().getId(providerIdString);
            setProviderValues(); // must come first
            setRepositoryManager();
        } catch (Throwable t) {
            Log.error("loading data sources from XML.  Cannot locate Provider with Id [" + providerId + "; XML=" + providerIdString + "];", t);
            //edu.tufts.vue.util.Logger.log(t,"loading data sources from XML.  Cannot locate Provider with Id " + providerIdString);
            //System.out.println("Error loading data sources from XML.  Cannot locate Provider with Id " + providerIdString);
        }
    }
    
    public String getDataSourceIdString() {
        if (this.dataSourceIdString == null) {
            try {
                return dataSourceId.getIdString();
            } catch (Throwable t) {
                Log.error("loading data sources from XML; dataSourceId[" + dataSourceId + "];", t);
                //edu.tufts.vue.util.Logger.log(t,"loading data sources from XML");
            }
        }
        return dataSourceIdString;
    }
    
    public void setDataSourceIdString(String dataSourceIdString) {
        try {
            this.dataSourceId =  factory.getIdManagerInstance().getId(dataSourceIdString);
            setProviderValues(); // must come first
            setRepositoryManager();
        } catch (Throwable t) {
            Log.error("loading data sources from XML; dataSourceIdString[" + dataSourceIdString + "];", t);
            //edu.tufts.vue.util.Logger.log(t,"loading data sources from XML");
        }
    }
    
    public void setConfiguration(java.util.Properties properties)
		throws org.osid.repository.RepositoryException {
        _propertyList  = new Vector();
        if (this.repositoryManager != null) {
            try {
                Enumeration keys = properties.keys();
                while(keys.hasMoreElements()) {
                    String key = (String)keys.nextElement();
                    tufts.vue.PropertyEntry pe = new tufts.vue.PropertyEntry();
                    pe.setEntryKey(key);
                    pe.setEntryValue(properties.getProperty(key));
                    _propertyList.add(pe);
                }
                this.repositoryManager.assignConfiguration(properties);
                setRelatedValues();
            } catch (org.osid.repository.RepositoryException rex) {
                edu.tufts.vue.util.Logger.log(rex);
                throw new org.osid.repository.RepositoryException(rex.getMessage());
            } catch (Throwable t) {
                edu.tufts.vue.util.Logger.log(t);
            }
        }
    }
    
    public Properties getConfiguration() {
        return getPropertyConfiguration(_propertyList);
    }

    private static Properties getPropertyConfiguration(Vector<PropertyEntry> propertyList) {
        Properties properties = new Properties();

        if (propertyList == null)
            return properties;
        
        for (PropertyEntry pe : propertyList) {
            properties.put(pe.getEntryKey(),
                           pe.getEntryValue());
        }

        return properties;
    }
    
    
    
    public java.util.Vector<PropertyEntry> getPropertyList() {
        return this._propertyList;
    }
    
    public void setPropertyList(java.util.Vector propertyList) {
        this._propertyList = propertyList;
        
    }
    
//     public boolean getDone() {
//         return this.done;
//     }
    
//     /** called only by castor persistance: when this final value is "set", we know
//      * the XML deserialize is "done" */
//     public void setDone(boolean done) {
//         edu.tufts.vue.dsm.impl.VueDataSourceManager.getInstance().add(this);
//         if (VueDataSourceManager.BLOCKING_OSID_LOAD) {
//             try {
//                 assignRepositoryConfiguration();
//             } catch (Throwable t) {
//                 Log.error("setDone; " + this, t);
//             }
//         }
//     }
    
    public synchronized void assignRepositoryConfiguration()
        throws org.osid.OsidException
    {
        final Properties properties = getPropertyConfiguration(_propertyList);
        
//         Log.debug(" data-source: " + this);
//         Log.debug("     manager: " + this.repositoryManager);
//         if (properties.size() > 0)
//             Log.debug("  properties: " + properties);
        
        if (this.repositoryManager == null) {
            Log.error("null repositoryManager unmarshalling " + this + "; skipping assignConfiguration.");
        } else {
            // This is what may try network access and can possibly hang if there's a problem:
            this.repositoryManager.assignConfiguration(properties);
        }
        setRelatedValues();
        //Log.debug("  CONFIGURED: " + this);
    }
    
    @Override
    public String toString() {
        try {
            return String.format("%s@%07x[%-30s; %s]",
                                 getClass().getSimpleName(),
                                 System.identityHashCode(this),
                                 '"' + getRepositoryDisplayName() + '"',
                                 getRepository());
//             return String.format("%s@%07x[%38s; %-30s; %s]",
//                                  getClass().getSimpleName(),
//                                  System.identityHashCode(this),
//                                  getId().getIdString(),
//                                  '"' + getRepositoryDisplayName() + '"',
//                                  getRepository());
        } catch (Throwable t) {
            return "VueDataSource[" + t + "]";
        }
    }
}
