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

public class VueDataSource
implements edu.tufts.vue.dsm.DataSource
{
	private edu.tufts.vue.dsm.Registry registry = VueRegistry.getInstance();

	private org.osid.shared.Id providerId = null;
	private String osidService = null;
	private int majorOsidVersion = -1;
	private int minorOsidVersion = -1;
	private String osidLoadKey = null;
	private String providerDisplayName = null;
	private String providerDescription = null;
	private String creator = null;
	private String publisher = null;
	private String publisherURL = null;
	private int providerMajorVersion = -1;
	private int providerMinorVersion = -1;
	private java.util.Date releaseDate = null;
	private String licenseAgreement = null;
	private String[] rights = null;
	private org.osid.shared.Type[] rightTypes = null;
	
	private org.osid.repository.RepositoryManager repositoryManager = null;
	private org.osid.repository.Repository repository = null;
	private org.osid.shared.Id repositoryId = null;
	private org.osid.shared.Type repositoryType = null;
	private String repositoryDisplayName = null;
	private String repositoryDescription = null;
	private String repositoryImage = null;
	private java.util.Date registrationDate = null;
	
	private boolean newState = false;
	private boolean updatedState = false;
	private boolean hiddenState = false;
	private boolean includedState = false;
	
	private boolean repositorySupportsUpdate = false;
	private org.osid.shared.TypeIterator repositoryAssetTypes = null;
	private org.osid.shared.TypeIterator repositorySearchTypes = null;
	
	// configuration-related
	private boolean isConfigured = false;
	private String configurationKeys[] = new String[0];
	private String configurationValues[] = new String[0];
	private java.util.Map configurationMaps[] = new java.util.Map[0];
	
	public VueDataSource()
	{
	}
	
	// Construct a data source from A STORED SNAPSHOT OF provider data
	public VueDataSource(org.osid.shared.Id providerId,
						 String osidService,
						 int majorOsidVersion,
						 int minorOsidVersion,
						 String osidLoadKey,
						 String providerDisplayName,
						 String providerDescription,
						 String creator,
						 String publisher,
						 String publisherURL,
						 int providerMajorVersion,
						 int providerMinorVersion,
						 java.util.Date releaseDate,
						 String[] rights,
						 org.osid.shared.Type[] rightTypes,
						 org.osid.shared.Id repositoryId,
						 String repositoryImage,
						 java.util.Date registrationDate,
						 boolean isHidden,
						 boolean isIncludedInSearch,
						 String[] configurationKeys,
						 String[] configurationValues,
						 java.util.Map[] configurationMaps) {

		this.providerId = providerId;
		this.osidService = osidService;
		this.majorOsidVersion = majorOsidVersion;
		this.minorOsidVersion = minorOsidVersion;
		this.osidLoadKey = osidLoadKey;
		this.providerDisplayName = providerDisplayName;
		this.providerDescription = providerDescription;
		this.creator = creator;
		this.publisher = publisher;
		this.publisherURL = publisherURL;
		this.providerMajorVersion = providerMajorVersion;
		this.providerMinorVersion = providerMinorVersion;
		this.releaseDate = releaseDate;
		this.rights = rights;
		this.rightTypes = rightTypes;
		this.repositoryId = repositoryId;
		this.repositoryImage = repositoryImage;
		this.registrationDate = registrationDate;
		setHidden(isHidden);
		setIncludedInSearch(isIncludedInSearch);
		this.configurationKeys = configurationKeys;
		setConfigurationValues(configurationValues);
		this.configurationMaps = configurationMaps;
		if (this.configurationKeys.length > 0) {
			this.isConfigured = true;
		}
		
		setRepositoryRelatedValues();
	}
	
	private void setRepositoryRelatedValues() {
		// get Repository
		try {
			this.repositoryManager = edu.tufts.vue.dsm.impl.VueOsidFactory.getInstance().getRepositoryManagerInstance(this.osidLoadKey);
			this.repository = (edu.tufts.vue.dsm.impl.VueOsidFactory.getInstance().getRepositoryManagerInstance(this.osidLoadKey)).getRepository(this.repositoryId);	
		} catch (Throwable t) {
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
				edu.tufts.vue.util.Logger.log(t,"in method edu.tufts.vue.dsm.VueDataSource calling getting Repository via Factory");
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
							
	
	// Construct a data source from LIVE provider data
	public VueDataSource(org.osid.shared.Id providerId) {
		this.providerId = providerId;
		org.osid.registry.Provider provider = this.registry.getProvider(providerId);
		
		// call Registry to answer these
		try {
			this.osidService = provider.getOsidService();
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"in method edu.tufts.vue.dsm.VueDataSource calling Registry.getOsidService()");
		}
		
		try {
			this.majorOsidVersion = provider.getOsidMajorVersion();
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"in method edu.tufts.vue.dsm.VueDataSource calling Provider.getOsidMajorVersion()");
		}
		
		try {
			this.minorOsidVersion = provider.getOsidMinorVersion();
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"in method edu.tufts.vue.dsm.VueDataSource calling Provider.getOsidMinorVersion()");
		}
		
		try {
			this.osidLoadKey = provider.getOsidLoadKey();
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"in method edu.tufts.vue.dsm.VueDataSource calling Provider.getOsidLoadKey()");
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
			this.publisherURL = provider.getPublisherURL();
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"in method edu.tufts.vue.dsm.VueDataSource calling Provider.getPublisherURL()");
		}
		
		try {
			this.providerMajorVersion = provider.getProviderMajorVersion();
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"in method edu.tufts.vue.dsm.VueDataSource calling Provider.getProviderMajorVersion()");
		}
		
		try {
			this.providerMinorVersion = provider.getProviderMinorVersion();
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"in method edu.tufts.vue.dsm.VueDataSource calling Provider.getProviderMinorVersion()");
		}
		
		try {
			this.releaseDate = edu.tufts.vue.util.Utilities.stringToDate(provider.getReleaseDate());
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"in method edu.tufts.vue.dsm.VueDataSource calling Provider.getReleaseDate()");
		}
		try {
			this.licenseAgreement = provider.getLicenseAgreement();
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"in method edu.tufts.vue.dsm.VueDataSource calling Provider.getLicenseAgreement()");
		}
		
		try {
			this.rights = provider.getRights();
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"in method edu.tufts.vue.dsm.VueDataSource calling Provider.getRight()");
		}
		
		try {
			this.rightTypes = provider.getRightTypes();
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"in method edu.tufts.vue.dsm.VueDataSource calling Provider.getRightTypes()");
		}
		
		try {
			this.configurationKeys = provider.getConfigurationKeys();
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"in method edu.tufts.vue.dsm.VueDataSource calling Provider.getConfigurationKeys()");
		}		
		
		try {
			this.configurationValues = provider.getConfigurationValues();
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"in method edu.tufts.vue.dsm.VueDataSource calling Provider.getConfigurationValues()");
		}		
		
		try {
			this.configurationMaps = provider.getConfigurationMaps();
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"in method edu.tufts.vue.dsm.VueDataSource calling Provider.getConfigurationMaps()");
		}		
		
		try {
			this.registrationDate = edu.tufts.vue.util.Utilities.stringToDate(provider.getRegistrationDate());
			
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"in method edu.tufts.vue.dsm.VueDataSource calling Provider.getRegistrationDate()");
		}
		
		try {
			this.repositoryImage = provider.getRepositoryImage();
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"in method edu.tufts.vue.dsm.VueDataSource calling Provider.getRepositoryImage()");
		}
		
		try {
			this.repositoryId = provider.getRepositoryId();
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"in method edu.tufts.vue.dsm.VueDataSource calling Registry.getRepsoitory()");
		}
		
		try {
			this.repositoryId = provider.getRepositoryId();
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"in method edu.tufts.vue.dsm.VueDataSource calling Registry.getRepsoitory()");
		}

		// isHidden and isIncluded are not clear here
		
		setRepositoryRelatedValues();
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
	
	public String getOsidService() {
		return this.osidService;
	}
	
	public void setOsidService(String osidService) {
		this.osidService = osidService;
	}
	
	public int getMajorOsidVersion() {
		return this.majorOsidVersion;
	}
	
	public void setMajorOsidVersion(int majorOsidVersion) {
		this.majorOsidVersion = majorOsidVersion;
	}
	
	public int getMinorOsidVersion() {
		return this.minorOsidVersion;
	}
	
	public void setMinorOsidVersion(int minorOsidVersion) {
		this.minorOsidVersion = minorOsidVersion;
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
	
	public void setProviderDescription(String providerDescription) {
		this.providerDescription = providerDescription;
	}
	
	public String getCreator() {
		return this.creator;
	}
	
	public void setCreator(String creator) {
		this.creator = creator;
	}
	
	public String getPublisher() {
		return this.publisher;
	}
	
	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}
	
	public String getPublisherURL() {
		return this.publisherURL;
	}
	
	public void setPublisherURL(String publisherURL) {
		this.publisherURL = publisherURL;
	}
	
	public int getProviderMajorVersion() {
		return this.providerMajorVersion;
	}
	
	public void setProviderMajorVersion(int providerMajorVersion) {
		this.providerMajorVersion = providerMajorVersion;
	}
	
	public int getProviderMinorVersion() {
		return this.providerMinorVersion;
	}
	
	public void setProviderMinorVersion(int providerMinorVersion) {
		this.providerMinorVersion = providerMinorVersion;
	}
	
	public java.util.Date getReleaseDate() {
		return this.releaseDate;
	}
	
	public void setReleaseDate(java.util.Date releaseDate) {
		this.releaseDate = releaseDate;
	}
	
	public String getLicenseAgreement() {
		return this.licenseAgreement;
	}
	
	public void setLicenseAgreement(String licenseAgreement) {
		this.licenseAgreement = licenseAgreement;
	}
	
	public String[] getRights() {
		return this.rights;
	}
	
	public void setRights(String[] rights) {
		this.rights = rights;
	}
	
	public org.osid.shared.Type[] getRightTypes() {
		return this.rightTypes;
	}
	
	public void setRightTypes(org.osid.shared.Type[] rightTypes) {
		this.rightTypes = rightTypes;
	}
	
	public org.osid.shared.Id getRepositoryId() {
		return this.repositoryId;
	}
	
	public void setRepositoryId(org.osid.shared.Id repositoryId) {
		this.repositoryId = repositoryId;
	}
	
	public org.osid.shared.Type getRepositoryType() {
		return this.repositoryType;
	}
	
	public void setRepositoryType(org.osid.shared.Type repositoryType) {
		this.repositoryType = repositoryType;
	}
	
	public String getRepositoryDisplayName() {
		return this.repositoryDisplayName;
	}
	
	public void setRepositoryDisplayName(String repositoryDisplayName) {
		this.repositoryDisplayName = repositoryDisplayName;
	}
	
	public String getRepositoryDescription() {
		return this.repositoryDescription;
	}
	
	public void setRepositoryDescription(String repositoryDescription) {
		this.repositoryDescription = repositoryDescription;
	}
	
	public java.awt.Image getImageForRepository() {
		return edu.tufts.vue.util.Utilities.getImageFromReference(this.repositoryImage); // TO DO: caching?
	}
	
	public String getRepositoryImage() {
		return this.repositoryImage;
	}
	
	public void setRepositoryImage(String repositoryImage) {
		this.repositoryImage = repositoryImage;
	}
	
	public java.util.Date getRegistrationDate() {
		return this.registrationDate;
	}
	
	public void setRegistrationDate(java.util.Date registrationDate) {
		
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
	
	public boolean isNew() {
		return this.newState;
	}
	
	public void setNew(boolean isNew) {
		this.newState = isNew;
	}
	
	public boolean isUpdated() {
		return this.updatedState;
	}
	
	public void setUpdated(boolean isUpdated) {
		this.updatedState = isUpdated;
	}
	
	public boolean isHidden() {
		return this.hiddenState;
	}
	
	public void setHidden(boolean isHidden) {
		this.hiddenState = isHidden;
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
	
	public org.osid.shared.TypeIterator getAssetTypes() {
		return this.repositoryAssetTypes;
	}
	
	public org.osid.shared.TypeIterator getSearchTypes() {
		return this.repositorySearchTypes;
	}

	public boolean hasConfiguration()
	{
		return this.isConfigured;
	}
	
	public String[] getConfigurationKeys()
	{
		for (int i=0; i < this.configurationKeys.length; i++) System.out.println("Key " + this.configurationKeys[i]);
		return this.configurationKeys;
	}
	
	public void setConfigurationKeys(String[] keys)
	{
		this.configurationKeys = keys;
	}
	
	public String[] getConfigurationValues()
	{
		return this.configurationValues;
	}

	public void setConfigurationValues(String[] values)
	{
		if ((this.repositoryManager != null) && (this.configurationValues.length > 0) && (values != null) && (values.length > 0)) {
//		if ((this.repositoryManager != null) && (values.length == this.configurationValues.length) && (values.length == this.configurationKeys.length)) {
			try {
				java.util.Properties properties = new java.util.Properties();
				for (int i=0; i < values.length; i++) {
					this.configurationValues[i] = values[i];
					properties.setProperty(this.configurationKeys[i],this.configurationValues[i]);
				}
				System.out.println("ready to assign " + properties);
				this.repositoryManager.assignConfiguration(properties);
				System.out.println("Assigned");
			} catch (Throwable t) {
				edu.tufts.vue.util.Logger.log(t);
			}
		}
	}
	
	public java.util.Map[] getConfigurationMaps()
	{
		return this.configurationMaps;
	}

	public void setConfigurationMaps(java.util.Map[] maps)
	{
		this.configurationMaps = maps;
	}

    public String toString() {
        return super.toString() + "[" + getRepositoryDisplayName() + "]";
    }
}
