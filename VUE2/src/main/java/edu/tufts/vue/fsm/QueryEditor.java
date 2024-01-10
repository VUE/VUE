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
 To allow for a UI to use different editors and get the critial information from
 them, we provide this simple interface.
 */

public interface QueryEditor
{
	java.io.Serializable getCriteria();
	void setCriteria(java.io.Serializable searchCriteria);

	org.osid.shared.Properties getProperties();
	void setProperties(org.osid.shared.Properties searchProperties);
	
	void setSearchType(org.osid.shared.Type searchType);
	org.osid.shared.Type getSearchType();

	void addSearchListener(edu.tufts.vue.fsm.event.SearchListener listener);
	void removeSearchListener(edu.tufts.vue.fsm.event.SearchListener listener);
	
	String getSearchDisplayName();
	
	void refresh();
}