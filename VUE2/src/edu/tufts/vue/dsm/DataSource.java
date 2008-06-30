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
package edu.tufts.vue.dsm;

/**
 This interface is simply an attribute holder with many accessors.  How these
 attributes are managed internally is obvious hidden.
 */

public interface DataSource
{
	public org.osid.repository.Repository getRepository();
	
	public void setProviderId(org.osid.shared.Id providerId);
	
	public org.osid.shared.Id getProviderId();
	
	public org.osid.shared.Id getId();

	public boolean hasUpdate();
	
	public String getOsidName();
	
	public String getOsidVersion();
	
	public String getOsidLoadKey();
	
	public void setOsidLoadKey(String osidLoadKey);
	
	public String getProviderDisplayName();
	
	public String getProviderDescription();
	
	public String getCreator();
	
	public String getPublisher();
	
	public java.util.Date getReleaseDate();
	
	public String getLicense();
	
	public boolean requestsLicenseAcknowledgement();
	
	public org.osid.shared.Id getRepositoryId();
	
	public org.osid.shared.Type getRepositoryType();
	
	public String getRepositoryDisplayName();
	
	public String getRepositoryDescription();
	
	public boolean isOnline();

	public boolean isIncludedInSearch();
	
	public void setIncludedInSearch(boolean isIncluded);
	
	public boolean supportsUpdate();
	
	public org.osid.shared.TypeIterator getAssetTypes();
	
	public org.osid.shared.TypeIterator getSearchTypes();
	
	public boolean hasConfiguration();
	
	public java.awt.Image getIcon16x16();
	
	public String getConfigurationUIHints();
	
	public void setConfiguration(java.util.Properties properties)
		throws org.osid.repository.RepositoryException;	

	public java.util.Properties getConfiguration();
	
	public boolean supportsSearch();
}
