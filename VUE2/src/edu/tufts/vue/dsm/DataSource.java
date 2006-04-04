package edu.tufts.vue.dsm;

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

/**
 This interface is simply an attribute holder with many accessors.  How these
 attributes are managed internally is obvious hidden.
 */

public interface DataSource
{
	public org.osid.repository.Repository getRepository();
	
	public void setProviderId(org.osid.shared.Id providerId);
	
	public org.osid.shared.Id getProviderId();
	
	public String getOsidService();
	
	public void setOsidService(String osidService);
	
	public int getMajorOsidVersion();
	
	public void setMajorOsidVersion(int majorOsidVersion);
	
	public int getMinorOsidVersion();
	
	public void setMinorOsidVersion(int minorOsidVersion);
	
	public String getOsidLoadKey();
	
	public void setOsidLoadKey(String osidLoadKey);
	
	public String getProviderDisplayName();
	
	public void setProviderDisplayName(String providerDisplayName);
	
	public String getProviderDescription();
	
	public void setProviderDescription(String providerDescription);
	
	public String getCreator();
	
	public void setCreator(String creator);

	public String getPublisher();
	
	public void setPublisher(String publisher);
	
	public String getPublisherURL();
	
	public void setPublisherURL(String publisherURL);

	public int getProviderMajorVersion();
	
	public void setProviderMajorVersion(int providerMajorVersion);
	
	public int getProviderMinorVersion();
	
	public void setProviderMinorVersion(int providerMinorVersion);
	
	public java.util.Date getReleaseDate();
	
	public void setReleaseDate(java.util.Date releaseDate);
	
	public String getLicenseAgreement();
	
	public void setLicenseAgreement(String licenseAgreement);
	
	public String[] getRights();
	
	public void setRights(String[] rights);
	
	public org.osid.shared.Type[] getRightTypes();
	
	public void setRightTypes(org.osid.shared.Type[] rightTypes);
	
	public org.osid.shared.Id getRepositoryId();
	
	public void setRepositoryId(org.osid.shared.Id repositoryId);
	
	public org.osid.shared.Type getRepositoryType();
	
	public void setRepositoryType(org.osid.shared.Type repositoryType);
	
	public String getRepositoryDisplayName();
	
	public void setRepositoryDisplayName(String repositoryDisplayName);
	
	public String getRepositoryDescription();
	
	public void setRepositoryDescription(String repositoryDescription);
	
	public String getRepositoryImage();

	public java.awt.Image getImageForRepository();
		
	public void setRepositoryImage(String repositoryImage);
	
	public java.util.Date getRegistrationDate();
	
	public void setRegistrationDate(java.util.Date registrationDate);
	
	public boolean isOnline();
	
	public boolean isNew();
	
	public void setNew(boolean isNew);
	
	public boolean isUpdated();
	
	public void setUpdated(boolean isUpdated);
	
	public boolean isHidden();
	
	public void setHidden(boolean isHidden);
	
	public boolean isIncludedInSearch();
	
	public void setIncludedInSearch(boolean isIncluded);
	
	// pass through from the underlying Repository
	public boolean supportsUpdate();
	
	// pass through from the underlying Repository
	public org.osid.shared.TypeIterator getAssetTypes();
	
	// pass through from the underlying Repository
	public org.osid.shared.TypeIterator getSearchTypes();
	
	public boolean hasConfiguration();
	
	public String[] getConfigurationKeys();
	
	public String[] getConfigurationValues();
	
	public void setConfigurationValues(String[] values);
	
	public java.util.Map[] getConfigurationMaps();
	
}
