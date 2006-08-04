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
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
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