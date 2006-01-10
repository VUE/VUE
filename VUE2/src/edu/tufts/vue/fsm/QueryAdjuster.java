package edu.tufts.vue.fsm;

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
