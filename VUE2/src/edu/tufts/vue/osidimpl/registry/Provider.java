package edu.tufts.vue.osidimpl.registry;

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
 * Provider implements a "straw-man" registry interface.  Each RepositoryManager
 * is a separate Provider.  RepositoryManagers are the top-level object in the
 * O.K.I. Repository OSID.  Providers have several attributes.
 * </p>
 * 
 * @author Massachusetts Institute of Technology
 */
public class Provider implements org.osid.registry.Provider
{
	private org.osid.registry.RegistryManager registryManager = null;
	private org.osid.shared.Id providerId = null;
	private String osidService = null;
	private int osidMajorVersion = 0;
	private int osidMinorVersion = 0;
	private String osidLoadKey = null;				

	private String displayName = null;
	private String description = null;
	private java.util.Vector keywordVector = new java.util.Vector();
	private java.util.Vector categoryVector = new java.util.Vector();
	private java.util.Vector categoryTypeVector = new java.util.Vector();

	private String creator = null;
	private String publisher = null;
	private String publisherURL = null;
	private int providerMajorVersion = 0;
	private int providerMinorVersion = 0;
	private String releaseDate = null;
	private String contactName = null;
	private String contactPhone = null;
	private String contactEMail = null;
	private String licenseAgreement = null;

	private java.util.Vector rightVector = new java.util.Vector();
	private java.util.Vector rightTypeVector = new java.util.Vector();
	private String readme = null;
	private String implementationLanguage = null;
	private boolean sourceAvailable = false;

	private org.osid.shared.Id repositoryId = null;
	private String repositoryImage = null;
	private String registrationDate = null;
	
	private java.util.Vector filenameVector = new java.util.Vector();
	private java.util.Vector fileDisplayNameVector = new java.util.Vector();
				
	/**
	 * Store away the input arguments for use by accessors.
	 */
	protected Provider(org.osid.registry.RegistryManager registryManager,
					   org.osid.shared.Id providerId,
					   String osidService,
					   int osidMajorVersion,
					   int osidMinorVersion,
					   String osidLoadKey,				
					   String displayName,
					   String description,
					   java.util.Vector keywordVector,
					   java.util.Vector categoryVector,
					   java.util.Vector categoryTypeVector,
					   String creator,
					   String publisher,
					   String publisherURL,
					   int providerMajorVersion,
					   int providerMinorVersion,
					   String releaseDate,
					   String contactName,
					   String contactPhone,
					   String contactEMail,
					   String licenseAgreement,
					   java.util.Vector rightVector,					   
					   java.util.Vector rightTypeVector,					   
					   String readme,
					   String implementationLangauge,
					   boolean sourceAvailable,
					   org.osid.shared.Id repositoryId,
					   String repositoryImage,
					   String registrationDate,
					   java.util.Vector filenameVector,
					   java.util.Vector fileDisplayNameVector)
	{
		this.registryManager = registryManager;
		this.providerId = providerId;
		this.osidService = osidService;
		this.osidMajorVersion = osidMajorVersion;
		this.osidMinorVersion = osidMinorVersion;
		this.osidLoadKey = osidLoadKey;
		this.displayName = displayName;
		this.description = description;
		this.keywordVector = keywordVector;
		this.categoryVector = categoryVector;
		this.categoryTypeVector = categoryTypeVector;
		this.creator = creator;
		this.publisher = publisher;
		this.publisherURL = publisherURL;
		this.providerMajorVersion = providerMajorVersion;
		this.providerMinorVersion = providerMinorVersion;
		this.releaseDate = releaseDate;
		this.contactName = contactName;
		this.contactPhone = contactPhone;
		this.contactEMail = contactEMail;
		this.licenseAgreement = licenseAgreement;
		this.rightVector = rightVector;
		this.rightTypeVector = rightTypeVector;
		this.readme = readme;
		this.implementationLanguage = implementationLanguage;
		this.sourceAvailable = sourceAvailable;
		this.repositoryId = repositoryId;
		this.repositoryImage = repositoryImage;
		this.registrationDate = registrationDate;
		this.filenameVector = filenameVector;
		this.fileDisplayNameVector = fileDisplayNameVector;
	}
	
	/**
	 * A unique identifier for the Provider.
	 * Get the value stored at construction or during an update.
	 */
	public org.osid.shared.Id getProviderId()
		throws org.osid.registry.RegistryException
	{
		return this.providerId;
	}
	
	/**
	 * A unique identifier for the Provider.
	 * Update the value stored at construction or during an earlier update.
	 * This change is immediately written to the registry.
	 */
	public void updateProviderId(org.osid.shared.Id providerId)
		throws org.osid.registry.RegistryException
	{
		if (providerId == null) {
			throw new org.osid.registry.RegistryException(org.osid.shared.SharedException.NULL_ARGUMENT);
		}
		
		org.osid.shared.Id undo = this.providerId;
		this.providerId = providerId;
		try {
			syncWithXML();
		} catch (Throwable t) {
			log(t);
			this.providerId = undo;
		}
	}
	
	/**
	 * The OSID Service is the package name defined by O.K.I.
	 * An example is org.osid.repository
	 * Get the value stored at construction or during an update.
	 */
	public String getOsidService()
	throws org.osid.registry.RegistryException
	{
		return this.osidService;
	}

	/**
	 * The OSID Service is the package name defined by O.K.I.
	 * An example is org.osid.repositor
	 * Update the value stored at construction or during an earlier update.
	 * This change is immediately written to the registry.
	 */
	public void updateOsidService(String osidService)
	throws org.osid.registry.RegistryException
	{
		if ( (osidService == null) || (osidService.length() == 0) ) {
			throw new org.osid.registry.RegistryException(org.osid.shared.SharedException.NULL_ARGUMENT);
		}
		
		String undo = this.osidService;
		this.osidService = osidService;
		try {
			syncWithXML();
		} catch (Throwable t) {
			log(t);
			this.osidService = undo;
		}
	}

	/**
	 * The version of the OSID -- not the version of the implementation.  An example is 2.0.
	 * Get the value stored at construction or during an update.	 
	 */
	public int getOsidMajorVersion()
	throws org.osid.registry.RegistryException
	{
		return this.osidMajorVersion;
	}

	/**
	 * The version of the OSID -- not the version of the implementation.  An example is 2.0.
	 * Update the value stored at construction or during an earlier update.
	 * This change is immediately written to the registry.
	 */
	public void updateOsidMajorVersion(int osidMajorVersion)
	throws org.osid.registry.RegistryException
	{
		int undo = this.osidMajorVersion;
		this.osidMajorVersion = osidMajorVersion;
		try {
			syncWithXML();
		} catch (Throwable t) {
			log(t);
			this.osidMajorVersion = undo;
		}
	}
	
	/**
	 * The version of the OSID -- not the version of the implementation.  An example is 2.0.
	 * Get the value stored at construction or during an update.	 
	 */
	public int getOsidMinorVersion()
		throws org.osid.registry.RegistryException
	{
		return this.osidMinorVersion;
	}
	
	/**
	 * The version of the OSID -- not the version of the implementation.  An example is 2.0.
	 * Update the value stored at construction or during an earlier update.
	 * This change is immediately written to the registry.
	 */
	public void updateOsidMinorVersion(int osidMinorVersion)
		throws org.osid.registry.RegistryException
	{
		int undo = this.osidMinorVersion;
		this.osidMinorVersion = osidMinorVersion;
		try {
			syncWithXML();
		} catch (Throwable t) {
			log(t);
			this.osidMinorVersion = undo;
		}
	}
	
	/**
	 * The information a loader / factory would need to return an instance of an implementation.
	 * An example is org.osidimpl.repository.xyz
	 * Get the value stored at construction or during an update.
	 */
	public String getOsidLoadKey()
	throws org.osid.registry.RegistryException
	{
		return this.osidLoadKey;
	}

	/**
	 * The information a loader / factory would need to return an instance of an implementation.
	 * An example is org.osidimpl.repository.xyz
	 * Update the value stored at construction or during an earlier update.
	 * This change is immediately written to the registry.
	 */
	public void updateOsidLoadKey(String osidLoadKey)
	throws org.osid.registry.RegistryException
	{
		if ( (osidLoadKey == null) || (osidLoadKey.length() == 0) ) {
			throw new org.osid.registry.RegistryException(org.osid.shared.SharedException.NULL_ARGUMENT);
		}
		
		String undo = this.osidLoadKey;
		this.osidLoadKey = osidLoadKey;
		try {
			syncWithXML();
		} catch (Throwable t) {
			log(t);
			this.osidLoadKey = undo;
		}
	}

	/**
	 * A short name for this implementation suitable for display in a picklist.
	 * Get the value stored at construction or during an update.
	 */
	public String getDisplayName()
	throws org.osid.registry.RegistryException
	{
		return this.displayName;
	}

	/**
	 * A short name for this implementation suitable for display in a picklist.
	 * Update the value stored at construction or during an earlier update.
	 * This change is immediately written to the registry.
	 */
	public void updateDisplayName(String displayName)
	throws org.osid.registry.RegistryException
	{
		if ( (displayName == null) || (displayName.length() == 0) ) {
			throw new org.osid.registry.RegistryException(org.osid.shared.SharedException.NULL_ARGUMENT);
		}
		
		String undo = this.displayName;
		this.displayName = displayName;
		try {
			syncWithXML();
		} catch (Throwable t) {
			log(t);
			this.displayName = undo;
		}
	}

	/**
	 * Longer and more informative text than the display name.
	 * Get the value stored at construction or during an update.
	 */
	public String getDescription()
	throws org.osid.registry.RegistryException
	{
		return this.description;
	}

	/**
	 * Longer and more informative text than the display name.
	 * Update the value stored at construction or during an earlier update.
	 * This change is immediately written to the registry.
	 */
	public void updateDescription(String description)
	throws org.osid.registry.RegistryException
	{
		if ( (description == null) || (description.length() == 0) ) {
			throw new org.osid.registry.RegistryException(org.osid.shared.SharedException.NULL_ARGUMENT);
		}
		
		String undo = this.description;
		this.description = description;
		try {
			syncWithXML();
		} catch (Throwable t) {
			log(t);
			this.description = undo;
		}
	}

	/**
	 */
	public String[] getKeywords()
		throws org.osid.registry.RegistryException
	{
		int size = this.keywordVector.size();
		String results[] = new String[size];
		for (int i=0; i < size; i++) results[i] = (String)this.keywordVector.elementAt(i);
		return results;
	}
	
	/**
	 */
	public void addKeyword(String keyword)
		throws org.osid.registry.RegistryException
	{
		if ( (keyword == null) || (keyword.length() == 0) ) {
			throw new org.osid.registry.RegistryException(org.osid.shared.SharedException.NULL_ARGUMENT);
		}
		
		this.keywordVector.addElement(keyword);
		try {
			syncWithXML();
		} catch (Throwable t) {
			log(t);
			this.keywordVector.removeElement(keyword);
		}
	}

	/**
	*/
	public void removeKeyword(String keyword)
		throws org.osid.registry.RegistryException
	{
		if ( (keyword == null) || (keyword.length() == 0) ) {
			throw new org.osid.registry.RegistryException(org.osid.shared.SharedException.NULL_ARGUMENT);
		}
		
		this.keywordVector.removeElement(keyword);
		try {
			syncWithXML();
		} catch (Throwable t) {
			log(t);
			this.keywordVector.addElement(keyword);
		}
	}
	
	/**
		*/
	public String[] getCategories()
		throws org.osid.registry.RegistryException
	{
		int size = this.categoryVector.size();
		String results[] = new String[size];
		for (int i=0; i < size; i++) results[i] = (String)this.categoryVector.elementAt(i);
		return results;
	}
	
	/**
		*/
	public void addCategory(String category)
		throws org.osid.registry.RegistryException
	{
		if ( (category == null) || (category.length() == 0) ) {
			throw new org.osid.registry.RegistryException(org.osid.shared.SharedException.NULL_ARGUMENT);
		}
		
		this.categoryVector.addElement(category);
		try {
			syncWithXML();
		} catch (Throwable t) {
			log(t);
			this.categoryVector.removeElement(category);
		}
	}
	
	/**
		*/
	public void removeCategory(String category)
		throws org.osid.registry.RegistryException
	{
		if ( (category == null) || (category.length() == 0) ) {
			throw new org.osid.registry.RegistryException(org.osid.shared.SharedException.NULL_ARGUMENT);
		}
		
		this.categoryVector.removeElement(category);
		try {
			syncWithXML();
		} catch (Throwable t) {
			log(t);
			this.categoryVector.addElement(category);
		}
	}
	
	/**
		*/
	public org.osid.shared.Type[] getCategoryTypes()
		throws org.osid.registry.RegistryException
	{
		int size = this.categoryTypeVector.size();
		org.osid.shared.Type results[] = new org.osid.shared.Type[size];
		for (int i=0; i < size; i++) results[i] = (org.osid.shared.Type)this.categoryTypeVector.elementAt(i);
		return results;
	}
	
	/**
		*/
	public void addCategoryType(org.osid.shared.Type categoryType)
		throws org.osid.registry.RegistryException
	{
		java.util.Vector undo = this.categoryTypeVector;
		
		// check this is not a duplicate
		for (int i=0, size = this.categoryTypeVector.size(); i < size; i++) {
			org.osid.shared.Type type = (org.osid.shared.Type)this.categoryTypeVector.elementAt(i);
			if (type.isEqual(categoryType)) {
				return;
			}
		}
		
		try {
			syncWithXML();
		} catch (Throwable t) {
			log(t);
			this.categoryTypeVector = undo;
		}
	}
	
	/**
		*/
	public void removeCategoryType(org.osid.shared.Type categoryType)
		throws org.osid.registry.RegistryException
	{
		java.util.Vector undo = this.categoryTypeVector;
		
		// remove if here
		for (int i=0, size = this.categoryTypeVector.size(); i < size; i++) {
			org.osid.shared.Type type = (org.osid.shared.Type)this.categoryTypeVector.elementAt(i);
			if (type.isEqual(categoryType)) {
				this.categoryTypeVector.removeElementAt(i);
			}
		}
		
		try {
			syncWithXML();
		} catch (Throwable t) {
			log(t);
			this.categoryTypeVector = undo;
		}
	}
	
	/**
	 * The author of the implementation.
	 * Get the value stored at construction or during an update.
	 */
	public String getCreator()
	throws org.osid.registry.RegistryException
	{
		return this.creator;
	}

	/**
	 * The author of the implementation.
	 * Update the value stored at construction or during an earlier update.
	 * This change is immediately written to the registry.
	 */
	public void updateCreator(String creator)
	throws org.osid.registry.RegistryException
	{
		if ( (creator == null) || (creator.length() == 0) ) {
			throw new org.osid.registry.RegistryException(org.osid.shared.SharedException.NULL_ARGUMENT);
		}
		
		String undo = this.creator;
		this.creator = creator;
		try {
			syncWithXML();
		} catch (Throwable t) {
			log(t);
			this.creator = undo;
		}
	}
	
	/**
	 * The institution that developed or is providing the implementation.
	 * Get the value stored at construction or during an update.
	 */
	public String getPublisher()
	throws org.osid.registry.RegistryException
	{
		return this.publisher;
	}
	
	/**
	 * The institution that developed or is providing the implementation.
	 * Update the value stored at construction or during an earlier update.
	 * This change is immediately written to the registry.
	 */
	public void updatePublisher(String publisher)
	throws org.osid.registry.RegistryException
	{
		if ( (publisher == null) || (publisher.length() == 0) ) {
			throw new org.osid.registry.RegistryException(org.osid.shared.SharedException.NULL_ARGUMENT);
		}
		
		String undo = this.publisher;
		this.publisher = publisher;
		try {
			syncWithXML();
		} catch (Throwable t) {
			log(t);
			this.publisher = undo;
		}
	}
	
	/**
	 * The institution that developed or is providing the implementation.
	 * Get the value stored at construction or during an update.
	 */
	public String getPublisherURL()
		throws org.osid.registry.RegistryException
	{
		return this.publisherURL;
	}
	
	/**
	 * The institution that developed or is providing the implementation.
	 * Update the value stored at construction or during an earlier update.
	 * This change is immediately written to the registry.
	 */
	public void updatePublisherURL(String publisherURL)
		throws org.osid.registry.RegistryException
	{
		if ( (publisherURL == null) || (publisherURL.length() == 0) ) {
			throw new org.osid.registry.RegistryException(org.osid.shared.SharedException.NULL_ARGUMENT);
		}
		
		String undo = this.publisherURL;
		this.publisherURL = publisherURL;
		try {
			syncWithXML();
		} catch (Throwable t) {
			log(t);
			this.publisherURL = undo;
		}
	}
	
	/**
	 */
	public int getProviderMajorVersion()
		throws org.osid.registry.RegistryException
	{
		return this.providerMajorVersion;
	}
	
	/**
	 */
	public void updateProviderMajorVersion(int providerMajorVersion)
		throws org.osid.registry.RegistryException
	{		
		int undo = this.providerMajorVersion;
		this.providerMajorVersion = providerMajorVersion;
		try {
			syncWithXML();
		} catch (Throwable t) {
			log(t);
			this.providerMajorVersion = undo;
		}
	}
	
	/**
	*/
	public int getProviderMinorVersion()
		throws org.osid.registry.RegistryException
	{
		return this.providerMinorVersion;
	}
	
	/**
	*/
	public void updateProviderMinorVersion(int providerMinorVersion)
		throws org.osid.registry.RegistryException
	{		
		int undo = this.providerMinorVersion;
		this.providerMinorVersion = providerMinorVersion;
		try {
			syncWithXML();
		} catch (Throwable t) {
			log(t);
			this.providerMinorVersion = undo;
		}
	}
	
	/**
	 * The timestamp for this release.  This is set by the RegistryManager.createProvider()
	 * method.  Consumers of the Registry may use this date to determine which of two versions
	 * of a Provider is more recent.
	 * Get the value stored at construction or during an update.
	 */
	public String getReleaseDate()
	throws org.osid.registry.RegistryException
	{
		return this.releaseDate;
	}
	
	/**
	 * The timestamp for this release.  This is set by the RegistryManager.createProvider()
	 * method.  Consumers of the Registry may use this date to determine which of two versions
	 * of a Provider is more recent.  Call this method with care.  Letting createProvider()
	 * populate this value may be safer.
	 * Update the value stored at construction or during an earlier update.
	 * This change is immediately written to the registry.
	 */
	public void updateReleaseDate(String releaseDate)
	throws org.osid.registry.RegistryException
	{
		if ( (releaseDate == null) || (releaseDate.length() == 0) ) {
			throw new org.osid.registry.RegistryException(org.osid.shared.SharedException.NULL_ARGUMENT);
		}
		
		String undo = this.releaseDate;
		this.releaseDate = releaseDate;
		try {
			syncWithXML();
		} catch (Throwable t) {
			log(t);
			this.releaseDate = undo;
		}		
	}

	/**
	 */
	public String getContactName()
		throws org.osid.registry.RegistryException
	{
		return this.contactName;
	}
	
	/**
	 */
	public void updateContactName(String contactName)
		throws org.osid.registry.RegistryException
	{
		if ( (contactName == null) || (contactName.length() == 0) ) {
			throw new org.osid.registry.RegistryException(org.osid.shared.SharedException.NULL_ARGUMENT);
		}
		
		String undo = this.contactName;
		this.contactName = contactName;
		try {
			syncWithXML();
		} catch (Throwable t) {
			log(t);
			this.contactName = undo;
		}		
	}
	
	/**
	*/
	public String getContactPhone()
		throws org.osid.registry.RegistryException
	{
		return this.contactPhone;
	}
	
	/**
	*/
	public void updateContactPhone(String contactPhone)
		throws org.osid.registry.RegistryException
	{
		if ( (contactPhone == null) || (contactPhone.length() == 0) ) {
			throw new org.osid.registry.RegistryException(org.osid.shared.SharedException.NULL_ARGUMENT);
		}
		
		String undo = this.contactPhone;
		this.contactPhone = contactPhone;
		try {
			syncWithXML();
		} catch (Throwable t) {
			log(t);
			this.contactPhone = undo;
		}		
	}
	
	/**
	*/
	public String getContactEMail()
		throws org.osid.registry.RegistryException
	{
		return this.contactEMail;
	}
	
	/**
	*/
	public void updateContactEMail(String contactEMail)
		throws org.osid.registry.RegistryException
	{
		if ( (contactEMail == null) || (contactEMail.length() == 0) ) {
			throw new org.osid.registry.RegistryException(org.osid.shared.SharedException.NULL_ARGUMENT);
		}
		
		String undo = this.contactEMail;
		this.contactEMail = contactEMail;
		try {
			syncWithXML();
		} catch (Throwable t) {
			log(t);
			this.contactEMail = undo;
		}		
	}

	/**
		*/
	public String getLicenseAgreement()
		throws org.osid.registry.RegistryException
	{
		return this.licenseAgreement;
	}
	
	/**
		*/
	public void updateLicenseAgreement(String licenseAgreement)
		throws org.osid.registry.RegistryException
	{
		if ( (licenseAgreement == null) || (licenseAgreement.length() == 0) ) {
			throw new org.osid.registry.RegistryException(org.osid.shared.SharedException.NULL_ARGUMENT);
		}
		
		String undo = this.licenseAgreement;
		this.licenseAgreement = licenseAgreement;
		try {
			syncWithXML();
		} catch (Throwable t) {
			log(t);
			this.licenseAgreement = undo;
		}
	}
	
	/**
	 * A description of how the Repository may be used.  Note there is no digital rights enforcement
	 * in the Registry, so this is descriptive only.
	 * Get the value stored at construction or during an update.
	 */
	public String[] getRights()
		throws org.osid.registry.RegistryException
	{
		int size = this.rightVector.size();
		String results[] = new String[size];
		for (int i=0; i < size; i++) results[i] = (String)this.rightVector.elementAt(i);
		return results;
	}
	
	/**
	 * A description of how the Repository may be used.  Note there is no digital rights enforcement
	 * in the Registry, so this is descriptive only.
	 * Update the value stored at construction or during an earlier update.
	 * This change is immediately written to the registry.
	 */
	public void addRight(String right)
		throws org.osid.registry.RegistryException
	{
		if ( (right == null) || (right.length() == 0) ) {
			throw new org.osid.registry.RegistryException(org.osid.shared.SharedException.NULL_ARGUMENT);
		}
		
		this.rightVector.addElement(right);
		try {
			syncWithXML();
		} catch (Throwable t) {
			log(t);
			this.rightVector.removeElement(right);
		}
	}
	
	/**
		* A description of how the Repository may be used.  Note there is no digital rights enforcement
	 * in the Registry, so this is descriptive only.
	 * Update the value stored at construction or during an earlier update.
	 * This change is immediately written to the registry.
	 */
	public void removeRight(String right)
		throws org.osid.registry.RegistryException
	{
		if ( (right == null) || (right.length() == 0) ) {
			throw new org.osid.registry.RegistryException(org.osid.shared.SharedException.NULL_ARGUMENT);
		}
		
		this.rightVector.removeElement(right);
		try {
			syncWithXML();
		} catch (Throwable t) {
			log(t);
			this.rightVector.addElement(right);
		}
	}

	/**
		* A description of how the Repository may be used.  Note there is no digital rights enforcement
	 * in the Registry, so this is descriptive only.
	 * Get the value stored at construction or during an update.
	 */
	public org.osid.shared.Type[] getRightTypes()
		throws org.osid.registry.RegistryException
	{
		int size = this.rightTypeVector.size();
		org.osid.shared.Type results[] = new org.osid.shared.Type[size];
		for (int i=0; i < size; i++) results[i] = (org.osid.shared.Type)this.rightTypeVector.elementAt(i);
		return results;
	}
	
	/**
		* A description of how the Repository may be used.  Note there is no digital rightTypes enforcement
	 * in the Registry, so this is descriptive only.
	 * Update the value stored at construction or during an earlier update.
	 * This change is immediately written to the registry.
	 */
	public void addRightType(org.osid.shared.Type rightType)
		throws org.osid.registry.RegistryException
	{
		java.util.Vector undo = this.rightTypeVector;
		
		// check this is not a duplicate
		for (int i=0, size = this.rightTypeVector.size(); i < size; i++) {
			org.osid.shared.Type type = (org.osid.shared.Type)this.rightTypeVector.elementAt(i);
			if (type.isEqual(rightType)) {
				return;
			}
		}
		
		try {
			syncWithXML();
		} catch (Throwable t) {
			log(t);
			this.rightTypeVector = undo;
		}
	}
	
	/**
		* A description of how the Repository may be used.  Note there is no digital rightTypes enforcement
	 * in the Registry, so this is descriptive only.
	 * Update the value stored at construction or during an earlier update.
	 * This change is immediately written to the registry.
	 */
	public void removeRightType(org.osid.shared.Type rightType)
		throws org.osid.registry.RegistryException
	{
		java.util.Vector undo = this.rightTypeVector;
		
		// remove if here
		for (int i=0, size = this.rightTypeVector.size(); i < size; i++) {
			org.osid.shared.Type type = (org.osid.shared.Type)this.rightTypeVector.elementAt(i);
			if (type.isEqual(rightType)) {
				this.rightTypeVector.removeElementAt(i);
			}
		}
		
		try {
			syncWithXML();
		} catch (Throwable t) {
			log(t);
			this.rightTypeVector = undo;
		}
	}
	

	/**
		*/
	public String getReadme()
		throws org.osid.registry.RegistryException
	{
		return this.readme;
	}
	
	/**
		*/
	public void updateReadme(String readme)
		throws org.osid.registry.RegistryException
	{
		if ( (readme == null) || (readme.length() == 0) ) {
			throw new org.osid.registry.RegistryException(org.osid.shared.SharedException.NULL_ARGUMENT);
		}
		
		String undo = this.readme;
		this.readme = readme;
		try {
			syncWithXML();
		} catch (Throwable t) {
			log(t);
			this.readme = undo;
		}
	}
	
	/**
	 */
	public String getImplementationLanguage()
		throws org.osid.registry.RegistryException
	{
		return this.implementationLanguage;
	}
	
	/**
	 */
	public void updateImplementationLanguage(String implementationLanguage)
		throws org.osid.registry.RegistryException
	{
		if ( (implementationLanguage == null) || (implementationLanguage.length() == 0) ) {
			throw new org.osid.registry.RegistryException(org.osid.shared.SharedException.NULL_ARGUMENT);
		}
		
		String undo = this.implementationLanguage;
		this.implementationLanguage = implementationLanguage;
		try {
			syncWithXML();
		} catch (Throwable t) {
			log(t);
			this.implementationLanguage = undo;
		}
	}
	
	/**
	*/
	public boolean isSourceAvailable()
		throws org.osid.registry.RegistryException
	{
		return this.sourceAvailable;
	}
	
	/**
		*/
	public void updateSourceAvailable(boolean sourceAvailable)
		throws org.osid.registry.RegistryException
	{
		boolean undo = this.sourceAvailable;
		this.sourceAvailable = sourceAvailable;
		try {
			syncWithXML();
		} catch (Throwable t) {
			log(t);
			this.sourceAvailable = undo;
		}
	}
	
	/**
	*/
	public org.osid.shared.Id getRepositoryId()
		throws org.osid.registry.RegistryException
	{
		return this.repositoryId;
	}
	
	/**
	*/
	public void updateRepositoryId(org.osid.shared.Id repositoryId)
		throws org.osid.registry.RegistryException
	{
		org.osid.shared.Id undo = this.repositoryId;
		this.repositoryId = repositoryId;
		try {
			syncWithXML();
		} catch (Throwable t) {
			log(t);
			this.repositoryId = undo;
		}
	}
	
	/**
		*/
	public String getRepositoryImage()
		throws org.osid.registry.RegistryException
	{
		return this.repositoryImage;
	}
	
	/**
		*/
	public void updateRepositoryImage(String repositoryImage)
		throws org.osid.registry.RegistryException
	{
		String undo = this.repositoryImage;
		this.repositoryImage = repositoryImage;
		try {
			syncWithXML();
		} catch (Throwable t) {
			log(t);
			this.repositoryImage = undo;
		}
	}
	
	/**
		* The timestamp for this registration.  This is set by the RegistryManager.createProvider()
	 * method.  Consumers of the Registry may use this date to determine which of two versions
	 * of a Provider is more recent.
	 * Get the value stored at construction or during an update.
	 */
	public String getRegistrationDate()
		throws org.osid.registry.RegistryException
	{
		return this.registrationDate;
	}
	
	/**
		* The timestamp for this registration.  This is set by the RegistryManager.createProvider()
	 * method.  Consumers of the Registry may use this date to determine which of two versions
	 * of a Provider is more recent.  Call this method with care.  Letting createProvider()
	 * populate this value may be safer.
	 * Update the value stored at construction or during an earlier update.
	 * This change is immediately written to the registry.
	 */
	public void updateRegistrationDate(String registrationDate)
		throws org.osid.registry.RegistryException
	{
		if ( (registrationDate == null) || (registrationDate.length() == 0) ) {
			throw new org.osid.registry.RegistryException(org.osid.shared.SharedException.NULL_ARGUMENT);
		}
		
		String undo = this.registrationDate;
		this.registrationDate = registrationDate;
		try {
			syncWithXML();
		} catch (Throwable t) {
			log(t);
			this.registrationDate = undo;
		}		
	}

	/**
	 */
	public String[] getFilenames()
		throws org.osid.registry.RegistryException
	{
		int size = this.filenameVector.size();
		String results[] = new String[size];
		for (int i=0; i < size; i++) results[i] = (String)this.filenameVector.elementAt(i);
		return results;
	}
	
	/**
	 */
	public void addFilename(String filename,
							String fileDisplayName)
		throws org.osid.registry.RegistryException
	{
		if ( (filename == null) || (filename.length() == 0) ) {
			throw new org.osid.registry.RegistryException(org.osid.shared.SharedException.NULL_ARGUMENT);
		}
		
		this.filenameVector.addElement(filename);
		this.fileDisplayNameVector.addElement(fileDisplayName);
		try {
			syncWithXML();
		} catch (Throwable t) {
			log(t);
			this.filenameVector.removeElement(filename);
			this.fileDisplayNameVector.removeElement(fileDisplayName);
		}
	}
	
	/**
	 */
	public void removeFilename(String filename)
		throws org.osid.registry.RegistryException
	{
		if ( (filename == null) || (filename.length() == 0) ) {
			throw new org.osid.registry.RegistryException(org.osid.shared.SharedException.NULL_ARGUMENT);
		}
		
		int index = this.filenameVector.indexOf(filename);
		if (index != -1) {
			this.filenameVector.removeElementAt(index);
			Object o = this.fileDisplayNameVector.elementAt(index);
			this.fileDisplayNameVector.removeElementAt(index);
			try {
				syncWithXML();
			} catch (Throwable t) {
				log(t);
				this.filenameVector.addElement(filename);
				this.fileDisplayNameVector.addElement(o);
			}
		}
	}
	
	/**
		*/
	public String[] getFileDisplayNames()
		throws org.osid.registry.RegistryException
	{
		int size = this.fileDisplayNameVector.size();
		String results[] = new String[size];
		for (int i=0; i < size; i++) results[i] = (String)this.fileDisplayNameVector.elementAt(i);
		return results;
	}
	
	//=================================================================================================
	// end of public interface implementation
	//=================================================================================================
	
	private void log(Throwable t)
	{
		t.printStackTrace();
	}
	
	/*
	 * Update the XML by deleting and then creating (with updated data) this provider.  If the update
	 * fails, the data will be out-of-sync.
	*/
	private void syncWithXML()
		throws org.osid.registry.RegistryException
	{
		this.registryManager.deleteProvider(this.providerId);
		this.registryManager.createProvider(this.providerId,
											this.osidService,
											this.osidMajorVersion,
											this.osidMinorVersion,
											this.osidLoadKey,				
											this.displayName,
											this.description,
											this.keywordVector,
											this.categoryVector,
											this.categoryTypeVector,
											this.creator,
											this.publisher,
											this.publisherURL,
											this.providerMajorVersion,
											this.providerMinorVersion,
											this.releaseDate,
											this.contactName,
											this.contactPhone,
											this.contactEMail,
											this.licenseAgreement,
											this.rightVector,					   
											this.rightTypeVector,
											this.readme,
											this.implementationLanguage,
											this.sourceAvailable,
											this.repositoryId,
											this.repositoryImage,
											this.registrationDate,
											this.filenameVector,
											this.fileDisplayNameVector);
	}
}