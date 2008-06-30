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

package edu.tufts.osidimpl.repository.google.global;

/**
 This class holds a set of Query-related properties and provides accessors.  
 Values are initialized from what is passed into the constructor.
 */
public class Query
implements edu.tufts.vue.fsm.Query
{
	private org.osid.repository.Repository repository = null;
	private java.io.Serializable searchCriteria = null;
	private org.osid.shared.Type searchType = null;
	private org.osid.shared.Properties searchProperties = null;
	private String foreignIdString = null;
	
	protected Query(String foreignIdString,
					org.osid.repository.Repository repository,
					java.io.Serializable searchCriteria,
					org.osid.shared.Type searchType,
					org.osid.shared.Properties searchProperties) {
		
		this.foreignIdString = foreignIdString;
		this.repository = repository;
		this.searchCriteria = searchCriteria;
		this.searchType = searchType;
		this.searchProperties = searchProperties;
	}
	
	public String getForeignIdString() {
		return this.foreignIdString;
	}
	
	public void setForeignIdString(String foreignIdString) {
		this.foreignIdString = foreignIdString;
	}
		
	protected Query(org.osid.repository.Repository repository,
					java.io.Serializable searchCriteria,
					org.osid.shared.Type searchType,
					org.osid.shared.Properties searchProperties) {
		this.repository = repository;
		this.searchCriteria = searchCriteria;
		this.searchType = searchType;
		this.searchProperties = searchProperties;
	}
	
	
	public org.osid.repository.Repository getRepository() {
		return this.repository;
	}
	
	public void setRepository(org.osid.repository.Repository repository) {
		this.repository = repository;
	}
	
	public java.io.Serializable getSearchCriteria() {
		return this.searchCriteria;
	}
	
	public void setSearchCriteria(java.io.Serializable searchCriteria) {
		this.searchCriteria = searchCriteria;
	}
	
	public org.osid.shared.Type getSearchType() {
		return this.searchType;
	}
	
	public void setSearchType(org.osid.shared.Type searchType) {
		this.searchType = searchType;		
	}
	
	public org.osid.shared.Properties getSearchProperties() {
		return this.searchProperties;
	}
	
	public void setSearchProperties(org.osid.shared.Properties searchProperties) {
		this.searchProperties = searchProperties;
	}
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