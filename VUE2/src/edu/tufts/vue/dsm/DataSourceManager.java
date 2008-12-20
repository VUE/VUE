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
package edu.tufts.vue.dsm;

/**
 * The Data Source Manager is a high-level construct.  For a caller, the manager
 * returns a particular data source or all and allows for adding and removing
 * data sources.
 *
 * [This may change]  The refresh and save operations are for synchronization with a
 * store.  The safest approach is to call refresh() then another method.  Similarly,
 * if the method you are calling is add() or remove(), then call save().
 *
 * There is a mapping of graphics to various types.  This interface provides access
 * to the graphics / icons, by type.
 */

public interface DataSourceManager {
    public DataSource[] getDataSources();
    
    public DataSource getDataSource(org.osid.shared.Id repositoryId);
    
    public void add(DataSource dataSource);
    
    public void remove(org.osid.shared.Id dataSourceId);
    
    public org.osid.repository.Repository[] getIncludedRepositories();
    
    public DataSource[] getIncludedDataSources();
    
    public void save();
    
    public java.awt.Image getImageForAssetType(org.osid.shared.Type assetType);
    
    public java.awt.Image getImageForSearchType(org.osid.shared.Type searchType);
    
    public java.awt.Image getImageForRepositoryType(org.osid.shared.Type repositoryType);
    
    public void addDataSourceListener(edu.tufts.vue.dsm.DataSourceListener listener) ;
    
    public void removeDataSourceListener(edu.tufts.vue.dsm.DataSourceListener listener);
    
    //public void  notifyDataSourceListeners();
}