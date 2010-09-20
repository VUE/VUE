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
package edu.tufts.vue.fsm.impl;

/**
 This class get the repositories the Data Source Manager says are part of a federated
 search.  Each repository supports search types.  The manager returns the union or
 intersection of these types -- no duplicates.
 */

public class VueSourcesAndTypesManager
implements edu.tufts.vue.fsm.SourcesAndTypesManager
{
	private edu.tufts.vue.dsm.DataSourceManager dataSourceManager = edu.tufts.vue.dsm.impl.VueDataSourceManager.getInstance();
	private static edu.tufts.vue.fsm.SourcesAndTypesManager vueSourcesAndTypesManager= new edu.tufts.vue.fsm.impl.VueSourcesAndTypesManager();
	
	public static edu.tufts.vue.fsm.SourcesAndTypesManager getInstance() {
		return vueSourcesAndTypesManager;
	}
	
	private VueSourcesAndTypesManager() {
	}
	
	public org.osid.repository.Repository[] getRepositoriesToSearch() {
		return dataSourceManager.getIncludedRepositories();
	}

	public edu.tufts.vue.dsm.DataSource[] getDataSourcesToSearch() {
		return dataSourceManager.getIncludedDataSources();
	}
	
	public org.osid.shared.Type[] getSearchTypes(int rule) {
		try {
			org.osid.repository.Repository[] repositories = dataSourceManager.getIncludedRepositories();
			int numRepositories = repositories.length;
			if (numRepositories == 0) {
				return new org.osid.shared.Type[0];
			}
			
			if (rule == edu.tufts.vue.fsm.SourcesAndTypesManager.ALL_TYPES) {

				// accumulate types across repositories, excluding duplicates
				java.util.Vector vector = new java.util.Vector();
				for (int i=0; i < numRepositories; i++) {
					org.osid.shared.TypeIterator typeIterator = repositories[i].getSearchTypes();
					while (typeIterator.hasNextType()) {
						org.osid.shared.Type type = typeIterator.nextType();
						String typeString = edu.tufts.vue.util.Utilities.typeToString(type);
						
						// no duplicates
						if (!(vector.contains(typeString))) {
							vector.addElement(typeString);
						}
					}
				}
				
				// convert vector to array
				int size = vector.size();
				org.osid.shared.Type[] types = new org.osid.shared.Type[size];
				for (int i=0; i < size; i++) {
					types[i] = edu.tufts.vue.util.Utilities.stringToType((String)vector.elementAt(i));
				}
				
				return types;				
			} else if (rule == edu.tufts.vue.fsm.SourcesAndTypesManager.TYPES_IN_COMMON) {
				
				// start with the first repository
				java.util.Vector vector = new java.util.Vector();
				org.osid.shared.TypeIterator typeIterator = repositories[0].getSearchTypes();
				while (typeIterator.hasNextType()) {
					vector.addElement(edu.tufts.vue.util.Utilities.typeToString(typeIterator.nextType()));
				}

				// if there are any other repositories, start by putting their types in a vector
				for (int i=1; i < numRepositories; i++) {
					java.util.Vector nextTypeVector = new java.util.Vector();
					typeIterator = repositories[i].getSearchTypes();
					while (typeIterator.hasNextType()) {
						nextTypeVector.addElement(edu.tufts.vue.util.Utilities.typeToString(typeIterator.nextType()));
					}
					
					// only keep types in common
					for (int j = vector.size()-1; j >= 0; j--) {
						String typeString = (String)vector.elementAt(j);
						if ( !(nextTypeVector.contains(typeString)) ) {
							vector.removeElement(typeString);
						}
					}
				}
				
				// convert vector to array
				int size = vector.size();
				org.osid.shared.Type[] types = new org.osid.shared.Type[size];
				for (int i=0; i < size; i++) {
					types[i] = edu.tufts.vue.util.Utilities.stringToType((String)vector.elementAt(i));
				}
				
				return types;								
			}
			edu.tufts.vue.util.Logger.log("unknown rule for getting search types");
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"while getting search types");
		}		
		return null;
	}
}
