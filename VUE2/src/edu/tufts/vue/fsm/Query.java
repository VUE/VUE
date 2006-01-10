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
 A query is simply an attribute holder.
 */

public interface Query
{
	public org.osid.repository.Repository getRepository();
	public void setRepository(org.osid.repository.Repository repository);

	public java.io.Serializable getSearchCriteria();
	public void setSearchCriteria(java.io.Serializable searchCriteria);
	
	public org.osid.shared.Type getSearchType();
	public void setSearchType(org.osid.shared.Type searchType);
	
	public org.osid.shared.Properties getSearchProperties();
	public void setSearchProperties(org.osid.shared.Properties searchProperties);
}