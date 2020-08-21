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
package edu.tufts.vue.dsm;

/**
 This interface is simply an attribute holder with many accessors.  How these
 attributes are managed internally is obvious hidden.
 */

public interface DataSource
{
	org.osid.repository.Repository getRepository();
	
	void setProviderId(org.osid.shared.Id providerId);
	
	org.osid.shared.Id getProviderId();
	
	org.osid.shared.Id getId();

	boolean hasUpdate();
	
	String getOsidName();
	
	String getOsidVersion();
	
	String getOsidLoadKey();
	
	void setOsidLoadKey(String osidLoadKey);
	
	String getProviderDisplayName();
	
	String getProviderDescription();
	
	String getCreator();
	
	String getPublisher();
	
	java.util.Date getReleaseDate();
	
	String getLicense();
	
	boolean requestsLicenseAcknowledgement();
	
	org.osid.shared.Id getRepositoryId();
	
	org.osid.shared.Type getRepositoryType();
	
	String getRepositoryDisplayName();
	
	String getRepositoryDescription();
	
	boolean isOnline();

	boolean isIncludedInSearch();
	
	void setIncludedInSearch(boolean isIncluded);
	
	boolean supportsUpdate();
	
	org.osid.shared.TypeIterator getAssetTypes();
	
	org.osid.shared.TypeIterator getSearchTypes();
	
	boolean hasConfiguration();
	
	java.awt.Image getIcon16x16();
	
	String getConfigurationUIHints();
	
	void setConfiguration(java.util.Properties properties)
		throws org.osid.repository.RepositoryException;	

	java.util.Properties getConfiguration();
	
	boolean supportsSearch();
}
