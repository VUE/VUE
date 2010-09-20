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
package edu.tufts.vue.fsm;

/**
 A query is simply an attribute holder.
 */

public interface Query
{
	public String getForeignIdString();
	public void setForeignIdString(String foreignIdString);
	
	public org.osid.repository.Repository getRepository();
	public void setRepository(org.osid.repository.Repository repository);

	public java.io.Serializable getSearchCriteria();
	public void setSearchCriteria(java.io.Serializable searchCriteria);
	
	public org.osid.shared.Type getSearchType();
	public void setSearchType(org.osid.shared.Type searchType);
	
	public org.osid.shared.Properties getSearchProperties();
	public void setSearchProperties(org.osid.shared.Properties searchProperties);
}