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

public class QueryAdjuster implements edu.tufts.vue.fsm.QueryAdjuster
{
	private org.osid.shared.Type oldKeywordSearchType = new Type("mit.edu","search","keyword");
	private org.osid.shared.Type newKeywordSearchType = new Type("mit.edu","search","keyword");
		
	public edu.tufts.vue.fsm.Query adjustQuery(org.osid.repository.Repository repository,
											   java.io.Serializable searchCriteria,
											   org.osid.shared.Type searchType,
											   org.osid.shared.Properties searchProperties)
{
		if (searchType.isEqual(this.oldKeywordSearchType)) {
			return new Query(repository,
							 searchCriteria,
							 this.newKeywordSearchType,
							 null);
		} else {
			return new Query(repository,
							 searchCriteria,
							 searchType,
							 searchProperties);
		}
	}
}