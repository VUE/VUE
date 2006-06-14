package edu.tufts.vue.dsm.impl;

/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2006
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

import java.util.*;

public class VueDataSource
        implements edu.tufts.vue.dsm.DataSource {
    private edu.tufts.vue.dsm.OsidFactory factory = VueOsidFactory.getInstance();
    private tufts.vue.PropertyMap mProperties = new tufts.vue.PropertyMap();
    private Vector mXMLpropertyList = null;
    private org.osid.shared.Id providerId = null;
    private String osidLoadKey = null;
    private String providerIdString = null; // to support loading datasource from castor
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
    
    // from Manager loaded via Provider
    private org.osid.repository.RepositoryManager repositoryManager = null;
    private org.osid.repository.Repository repository = null;
    private org.osid.shared.Id repositoryId = null;
    private org.osid.shared.Type repositoryType = null;
    private String repositoryDisplayName = null;
    private String repositoryDescription = null;
    private boolean repositorySupportsUpdate = false;
    private org.osid.shared.TypeIterator repositoryAssetTypes = null;
    private org.osid.shared.TypeIterator repositorySearchTypes = null;
    
    // Data Source Manager
    private edu.tufts.vue.dsm.DataSourceManager dataSourceManager = null;
    private boolean includedState = false;
    
    // constructer required by castor
    
    public VueDataSource() {
    }
    
    // Construct a data source from stored data
    public VueDataSource(org.osid.shared.Id providerId,
            boolean isIncludedInSearch) {
        
        this.providerId = providerId;
        this.includedState = isIncludedInSearch;
        setProviderValues(); // must come first
        setRelatedValues();
    }
    
    private void setProviderValues() {
        org.osid.provider.Provider provider = null;
        try {
            provider = this.factory.getProvider(providerId);
            this.osidName = provider.getOsidName();
        } catch (Throwable t) {
            edu.tufts.vue.util.Logger.log(t,"in method edu.tufts.vue.dsm.VueDataSource calling Provider.getOsidName()");
        }
        
        try {
            this.osidBindingVersion = provider.getOsidBindingVersion();
        } catch (Throwable t) {
            edu.tufts.vue.util.Logger.log(t,"in method edu.tufts.vue.dsm.VueDataSource calling Provider.getOsidBindingVersion()");
        }
        
        try {
            this.providerDisplayName = provider.getDisplayName();
        } catch (Throwable t) {
            edu.tufts.vue.util.Logger.log(t,"in method edu.tufts.vue.dsm.VueDataSource calling Provider.getDisplayName()");
        }
        
        try {
            this.providerDescription = provider.getDescription();
        } catch (Throwable t) {
            edu.tufts.vue.util.Logger.log(t,"in method edu.tufts.vue.dsm.VueDataSource calling Provider.getDescription()");
        }
        
        try {
            this.creator = provider.getCreator();
        } catch (Throwable t) {
            edu.tufts.vue.util.Logger.log(t,"in method edu.tufts.vue.dsm.VueDataSource calling Provider.getCreator()");
        }
        
        try {
            this.publisher = provider.getPublisher();
        } catch (Throwable t) {
            edu.tufts.vue.util.Logger.log(t,"in method edu.tufts.vue.dsm.VueDataSource calling Provider.getPublisher()");
        }
        
        try {
            this.releaseDate = provider.getReleaseDate();
        } catch (Throwable t) {
            edu.tufts.vue.util.Logger.log(t,"in method edu.tufts.vue.dsm.VueDataSource calling Provider.getReleaseDate()");
        }
        
        try {
            this.license = provider.getLicense();
        } catch (Throwable t) {
            edu.tufts.vue.util.Logger.log(t,"in method edu.tufts.vue.dsm.VueDataSource calling Provider.getLicense()");
        }
        
        try {
            this.requestsLicenseAcknowledgement = provider.requestsLicenseAcknowledgement();
        } catch (Throwable t) {
            edu.tufts.vue.util.Logger.log(t,"in method edu.tufts.vue.dsm.VueDataSource calling Provider.requestsLicenseAcknowledgement()");
        }
        
        try {
            org.osid.shared.PropertiesIterator propertiesIterator = provider.getProperties();
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
            edu.tufts.vue.util.Logger.log(t,"in method edu.tufts.vue.dsm.VueDataSource calling Provider");
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
    
    private void setRelatedValues() {
		try {
			System.out.println("Load key is " + this.osidLoadKey);
			this.repositoryId = edu.tufts.vue.util.Utilities.getRepositoryIdFromLoadKey(this.osidLoadKey);
			System.out.println("Repository id from load key is " + this.repositoryId.getIdString());
			this.repositoryManager = edu.tufts.vue.dsm.impl.VueOsidFactory.getInstance().getRepositoryManagerInstance(this.osidLoadKey);
			System.out.println("got manager");
			this.repository = (edu.tufts.vue.dsm.impl.VueOsidFactory.getInstance().getRepositoryManagerInstance(this.osidLoadKey)).getRepository(this.repositoryId);	
			System.out.println("got repository");
        } catch (Throwable t) {
			System.out.println("Load by key failed, trying a check of all repositories");
            // special case for when the Manager implementation doesn't offer this method
            try {
                org.osid.repository.RepositoryIterator repositoryIterator = repositoryManager.getRepositories();
                while (repositoryIterator.hasNextRepository()) {
                    repository = repositoryIterator.nextRepository();
                    if (repositoryId.isEqual(repository.getId())) {
                        this.repository = repository;
                    }
                }
            } catch (Throwable t1) {
				System.out.println("Load by check of all repositories failed");
				edu.tufts.vue.util.Logger.log(t,"in method edu.tufts.vue.dsm.VueDataSource calling getting Repository via Factory");
				return;
            }
        }
        
        // call Repository to answer these
        try {
            this.repositoryDisplayName = this.repository.getDisplayName();
        } catch (Throwable t) {
            edu.tufts.vue.util.Logger.log(t,"in method edu.tufts.vue.dsm.VueDataSource calling Repository.getDisplayName()");
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
    
    public org.osid.shared.Type getRepositoryType() {
        return this.repositoryType;
    }
    
    public String getRepositoryDisplayName() {
        return this.repositoryDisplayName;
    }
    
    public String getRepositoryDescription() {
        return this.repositoryDescription;
    }
    
    public boolean isOnline() {
        try {
            this.repository.getDisplayName();
            return true;
        } catch (Throwable t) {
            // ignore since we are going to return false for any failure
        }
        return false;
    }
    
    public boolean isIncludedInSearch() {
        return this.includedState;
    }
    
    public void setIncludedInSearch(boolean isIncluded) {
        this.includedState = isIncluded;
        if (this.dataSourceManager == null) {
            dataSourceManager = edu.tufts.vue.dsm.impl.VueDataSourceManager.getInstance();
            dataSourceManager.save();
        }
    }
    
    public boolean supportsUpdate() {
        return this.repositorySupportsUpdate;
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
                edu.tufts.vue.util.Logger.log(t,"loading data sources from XML");
            }
        }
        return providerIdString;
    }
    public void setProviderIdString(String providerIdString) {
        try {
            providerId =  edu.tufts.vue.dsm.impl.VueOsidFactory.getInstance().getIdManagerInstance().getId(providerIdString);
            setProviderValues(); // must come first
            setRelatedValues();
        } catch (Throwable t) {
            edu.tufts.vue.util.Logger.log(t,"loading data sources from XML");
        }
    }
    public void setConfiguration(java.util.Properties properties) {
        if (this.repositoryManager != null) {
            try {
                Enumeration keys = properties.keys();
                while(keys.hasMoreElements()) {
                    String key = (String)keys.nextElement();
                    mProperties.setProperty(key,properties.getProperty(key));
                }
                this.repositoryManager.assignConfiguration(properties);
            } catch (Throwable t) {
                edu.tufts.vue.util.Logger.log(t);
            }
        }
    }
    
    public Properties getConfiguration() {
        Properties properties = new Properties();
        Iterator i = mProperties.keySet().iterator();
        while (i.hasNext()) {
            final String key = (String) i.next();
            final String value = mProperties.getProperty(key);
            properties.getProperty(key,value);
        }
        return properties;
    }
    
    /** this is for castor persistance only */
    public java.util.List getPropertyList() {
        if (mProperties.size() == 0) // a hack for castor to work
            return null;
        mXMLpropertyList = new Vector(mProperties.size());
        Iterator i = mProperties.keySet().iterator();
        while (i.hasNext()) {
            final String key = (String) i.next();
            final tufts.vue.PropertyEntry entry = new tufts.vue.PropertyEntry();
            entry.setEntryKey(key);
            entry.setEntryValue(mProperties.get(key));
            mXMLpropertyList.add(entry);
        }
        
        return mXMLpropertyList;
    }
    
    public void setPropertyList(Vector propertyList) {
    }
    
    public void setProperty(String key, Object value) {
        if (key != null && value != null) {
            if (!(value instanceof String && ((String)value).length() < 1))
                mProperties.put(key, value);
        }
        
        //SET configuration of repository manager.
        Properties p = new Properties();
        p.put(key,value);
        try{
            this.repositoryManager.assignConfiguration(p);
        } catch (Throwable t) {
            edu.tufts.vue.util.Logger.log(t);
        }
    }
    public String toString() {
        return super.toString() + "[" + getRepositoryDisplayName() + "]";
    }
}
