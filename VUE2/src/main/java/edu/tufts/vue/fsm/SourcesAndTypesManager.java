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
 This interface returns the repositories that are going to be searched.  This information
 comes directly from the Data Source Manager.  The union or intersection of search types
 among the repositories is also available.
 */

public interface SourcesAndTypesManager
{
	int ALL_TYPES = 0;
	int TYPES_IN_COMMON = 1;
	
	org.osid.repository.Repository[] getRepositoriesToSearch();
	edu.tufts.vue.dsm.DataSource[] getDataSourcesToSearch();

	org.osid.shared.Type[] getSearchTypes(int rule);
}
