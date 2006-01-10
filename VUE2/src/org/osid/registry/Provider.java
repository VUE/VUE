package org.osid.registry;

/**
 * Provider implements a "straw-man" registry interface.  Each RepositoryManager
 * is a separate Provider.  RepositoryManagers are the top-level object in the
 * O.K.I. Repository OSID.  Providers have several attributes.
 * </p>
 * 
 * @author Massachusetts Institute of Technology
 */
public interface Provider
extends java.io.Serializable
{
	/**
	*/
	public org.osid.shared.Id getProviderId()
	throws org.osid.registry.RegistryException;
	
	/**
	*/
	public void updateProviderId(org.osid.shared.Id providerId)
		throws org.osid.registry.RegistryException;
	
	/**
	 */
	public String getOsidService()
		throws org.osid.registry.RegistryException;

	/**
	 */
	public void updateOsidService(String osidService)
		throws org.osid.registry.RegistryException;

	/**
	 */
	public int getOsidMajorVersion()
		throws org.osid.registry.RegistryException;

	/**
	 */
	public void updateOsidMajorVersion(int majorVersion)
		throws org.osid.registry.RegistryException;

	/**
	*/
	public int getOsidMinorVersion()
		throws org.osid.registry.RegistryException;
	
	/**
	*/
	public void updateOsidMinorVersion(int minorVersion)
		throws org.osid.registry.RegistryException;
	
	/**
	 */
	public String getOsidLoadKey()
		throws org.osid.registry.RegistryException;

	/**
	 */
	public void updateOsidLoadKey(String osidLoadKey)
		throws org.osid.registry.RegistryException;

	/**
	 */
	public String getDisplayName()
		throws org.osid.registry.RegistryException;

	/**
	 */
	public void updateDisplayName(String displayName)
		throws org.osid.registry.RegistryException;

	/**
	 */
	public String getDescription()
		throws org.osid.registry.RegistryException;

	/**
	 */
	public void updateDescription(String description)
		throws org.osid.registry.RegistryException;

	/**
	 */
	public String[] getKeywords()
		throws org.osid.registry.RegistryException;

	/**
	 */
	public void addKeyword(String keyword)
		throws org.osid.registry.RegistryException;
	
	/**
	*/
	public void removeKeyword(String keyword)
		throws org.osid.registry.RegistryException;
	
	/**
	*/
	public String[] getCategories()
		throws org.osid.registry.RegistryException;
	
	/**
	*/
	public void addCategory(String category)
		throws org.osid.registry.RegistryException;
	
	/**
	*/
	public void removeCategory(String category)
		throws org.osid.registry.RegistryException;
	
	/**
		*/
	public org.osid.shared.Type[] getCategoryTypes()
		throws org.osid.registry.RegistryException;
	
	/**
		*/
	public void addCategoryType(org.osid.shared.Type categoryType)
		throws org.osid.registry.RegistryException;
	
	/**
		*/
	public void removeCategoryType(org.osid.shared.Type categoryType)
		throws org.osid.registry.RegistryException;
	
	/**
	*/
	public String getCreator()
		throws org.osid.registry.RegistryException;
	
	/**
	*/
	public void updateCreator(String creator)
		throws org.osid.registry.RegistryException;
	
	/**
	 */
	public String getPublisher()
		throws org.osid.registry.RegistryException;
	
	/**
	 */
	public void updatePublisher(String publisher)
		throws org.osid.registry.RegistryException;
	
	/**
	*/
	public String getPublisherURL()
		throws org.osid.registry.RegistryException;
	
	/**
	*/
	public void updatePublisherURL(String publisherURL)
		throws org.osid.registry.RegistryException;
	
	/**
	*/
	public int getProviderMajorVersion()
		throws org.osid.registry.RegistryException;
	
	/**
	*/
	public void updateProviderMajorVersion(int majorVersion)
		throws org.osid.registry.RegistryException;
	
	/**
	*/
	public int getProviderMinorVersion()
		throws org.osid.registry.RegistryException;
	
	/**
	*/
	public void updateProviderMinorVersion(int minorVersion)
		throws org.osid.registry.RegistryException;
	
	/**
	*/
	public String getReleaseDate()
		throws org.osid.registry.RegistryException;
	
	/**
	*/
	public void updateReleaseDate(String releaseDate)
		throws org.osid.registry.RegistryException;
	
	/**
	*/
	public String getContactName()
		throws org.osid.registry.RegistryException;
	
	/**
	*/
	public void updateContactName(String contactName)
		throws org.osid.registry.RegistryException;
	
	/**
	*/
	public String getContactPhone()
		throws org.osid.registry.RegistryException;
	
	/**
	*/
	public void updateContactPhone(String contactPhone)
		throws org.osid.registry.RegistryException;
	
	/**
	*/
	public String getContactEMail()
		throws org.osid.registry.RegistryException;
	
	/**
	*/
	public void updateContactEMail(String contactEMail)
		throws org.osid.registry.RegistryException;
	
	/**
	*/
	public String getLicenseAgreement()
		throws org.osid.registry.RegistryException;
	
	/**
	*/
	public void updateLicenseAgreement(String licenseAgreement)
		throws org.osid.registry.RegistryException;
	
	/**
	*/
	public String[] getRights()
		throws org.osid.registry.RegistryException;
	
	/**
		*/
	public void addRight(String right)
		throws org.osid.registry.RegistryException;
	
	/**
	*/
	public void removeRight(String right)
		throws org.osid.registry.RegistryException;
	
	/**
	*/
	public org.osid.shared.Type[] getRightTypes()
		throws org.osid.registry.RegistryException;
	
	/**
	*/
	public void addRightType(org.osid.shared.Type rightType)
		throws org.osid.registry.RegistryException;
	
	/**
	*/
	public void removeRightType(org.osid.shared.Type rightType)
		throws org.osid.registry.RegistryException;
	
	/**
	*/
	public String getReadme()
		throws org.osid.registry.RegistryException;
	
	/**
	*/
	public void updateReadme(String readme)
		throws org.osid.registry.RegistryException;
	
	/**
	*/
	public String getImplementationLanguage()
		throws org.osid.registry.RegistryException;
	
	/**
	*/
	public void updateImplementationLanguage(String implementationLanguage)
		throws org.osid.registry.RegistryException;
	
	/**
	*/
	public boolean isSourceAvailable()
		throws org.osid.registry.RegistryException;
	
	/**
	*/
	public void updateSourceAvailable(boolean sourceAvailable)
		throws org.osid.registry.RegistryException;
	
	/**
	*/
	public org.osid.shared.Id getRepositoryId()
		throws org.osid.registry.RegistryException;
		
	/**
	*/
	public void updateRepositoryId(org.osid.shared.Id repositoryId)
		throws org.osid.registry.RegistryException;
		
	/**
	*/
	public String getRepositoryImage()
		throws org.osid.registry.RegistryException;
	
	/**
	*/
	public void updateRepositoryImage(String repositoryImage)
		throws org.osid.registry.RegistryException;
	
	/**
	*/
	public String getRegistrationDate()
		throws org.osid.registry.RegistryException;
	
	/**
	*/
	public void updateRegistrationDate(String registrationDate)
		throws org.osid.registry.RegistryException;	
	
	public String[] getFilenames()
		throws org.osid.registry.RegistryException;

	public void addFilename(String filename,
							String fileDisplayName)
		throws org.osid.registry.RegistryException;
		
	public void removeFilename(String filename)
		throws org.osid.registry.RegistryException;
		
	public String[] getFileDisplayNames()
		throws org.osid.registry.RegistryException;	
	/**
		<p>MIT O.K.I&#46; SID Implementation License.
	 <p>	<b>Copyright and license statement:</b>
	 </p>  <p>	Copyright &copy; 2003 Massachusetts Institute of
	 Technology &lt;or copyright holder&gt;
	 </p>  <p>	This work is being provided by the copyright holder(s)
	 subject to the terms of the O.K.I&#46; SID Implementation
	 License. By obtaining, using and/or copying this Work,
	 you agree that you have read, understand, and will comply
	 with the O.K.I&#46; SID Implementation License. 
	 </p>  <p>	THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
	 KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
	 THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
	 PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
	 MASSACHUSETTS INSTITUTE OF TECHNOLOGY, THE AUTHORS, OR
	 COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR
	 OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
	 OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
	 THE WORK OR THE USE OR OTHER DEALINGS IN THE WORK.
	 </p>  <p>	<b>O.K.I&#46; SID Implementation License</b>
	 </p>  <p>	This work (the &ldquo;Work&rdquo;), including software,
	 documents, or other items related to O.K.I&#46; SID
	 implementations, is being provided by the copyright
	 holder(s) subject to the terms of the O.K.I&#46; SID
	 Implementation License. By obtaining, using and/or
	 copying this Work, you agree that you have read,
	 understand, and will comply with the following terms and
	 conditions of the O.K.I&#46; SID Implementation License:
	 </p>  <p>	Permission to use, copy, modify, and distribute this Work
	 and its documentation, with or without modification, for
	 any purpose and without fee or royalty is hereby granted,
	 provided that you include the following on ALL copies of
	 the Work or portions thereof, including modifications or
	 derivatives, that you make:
	 </p>  <ul>	<li>	  The full text of the O.K.I&#46; SID Implementation
	 License in a location viewable to users of the
	 redistributed or derivative work.
	 </li>  </ul>  <ul>	<li>	  Any pre-existing intellectual property disclaimers,
	 notices, or terms and conditions. If none exist, a
	 short notice similar to the following should be used
	 within the body of any redistributed or derivative
	 Work: &ldquo;Copyright &copy; 2003 Massachusetts
	 Institute of Technology. All Rights Reserved.&rdquo;
	 </li>  </ul>  <ul>	<li>	  Notice of any changes or modifications to the
	 O.K.I&#46; Work, including the date the changes were
	 made. Any modified software must be distributed in such
	 as manner as to avoid any confusion with the original
	 O.K.I&#46; Work.
	 </li>  </ul>  <p>	THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
	 KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
	 THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
	 PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
	 MASSACHUSETTS INSTITUTE OF TECHNOLOGY, THE AUTHORS, OR
	 COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR
	 OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
	 OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
	 THE WORK OR THE USE OR OTHER DEALINGS IN THE WORK.
	 </p>  <p>	The name and trademarks of copyright holder(s) and/or
	 O.K.I&#46; may NOT be used in advertising or publicity
	 pertaining to the Work without specific, written prior
	 permission. Title to copyright in the Work and any
	 associated documentation will at all times remain with
	 the copyright holders.
	 </p>  <p>	The export of software employing encryption technology
	 may require a specific license from the United States
	 Government. It is the responsibility of any person or
	 organization contemplating export to obtain such a
	 license before exporting this Work.
	 </p>*/
}