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
	String getForeignIdString();
	void setForeignIdString(String foreignIdString);
	
	org.osid.repository.Repository getRepository();
	void setRepository(org.osid.repository.Repository repository);

	java.io.Serializable getSearchCriteria();
	void setSearchCriteria(java.io.Serializable searchCriteria);
	
	org.osid.shared.Type getSearchType();
	void setSearchType(org.osid.shared.Type searchType);
	
	org.osid.shared.Properties getSearchProperties();
	void setSearchProperties(org.osid.shared.Properties searchProperties);
}