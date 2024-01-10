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
 When a Repository is included in a federated search, it may not be able to digest
 the search criteria, type, and properties the user enters for all repositories.  An
 adjuster can fix up the input and will be called just before the repository is asked
 to perform the search.
 */

public interface QueryAdjuster
{
	public Query adjustQuery(org.osid.repository.Repository repository,
							 java.io.Serializable searchCriteria,
							 org.osid.shared.Type searchType,
							 org.osid.shared.Properties searchProperties);
}
