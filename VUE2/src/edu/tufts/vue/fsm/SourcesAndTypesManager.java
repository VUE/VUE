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
 This interface returns the repositories that are going to be searched.  This information
 comes directly from the Data Source Manager.  The union or intersection of search types
 among the repositories is also available.
 */

public interface SourcesAndTypesManager
{
	public static final int ALL_TYPES = 0;
	public static final int TYPES_IN_COMMON = 1;
	
	public org.osid.repository.Repository[] getRepositoriesToSearch();
	public edu.tufts.vue.dsm.DataSource[] getDataSourcesToSearch();

	public org.osid.shared.Type[] getSearchTypes(int rule);
}
