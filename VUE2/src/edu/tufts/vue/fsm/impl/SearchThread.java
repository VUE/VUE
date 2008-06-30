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
package edu.tufts.vue.fsm.impl;

/**
 This class calls Repository.getAssetsBySearch() method on a thread.  The thread is timed.
 When the method returns, we call SearchEngine.searchComplete().
 */

public class SearchThread
implements Runnable
{
    private java.io.Serializable searchCriteria = null;
    private org.osid.shared.Type searchType = null;
    private org.osid.repository.Repository repository = null;
	private String foreignIdString = null;
    private org.osid.shared.Properties searchProperties = null;
    private int searchIndex = -1;
    private long startTime = 0;
	private edu.tufts.vue.fsm.SearchEngine searchEngine = VueSearchEngine.getInstance();

    public SearchThread(String foreignIdString,
						org.osid.repository.Repository repository,
						int searchIndex,
						java.io.Serializable searchCriteria,
						org.osid.shared.Type searchType,
						org.osid.shared.Properties searchProperties)
    {
        this.searchCriteria = searchCriteria;
        this.searchType = searchType;
        this.searchProperties = searchProperties;
        this.repository = repository;
		this.foreignIdString = foreignIdString;
        this.searchIndex = searchIndex;        
        this.startTime = java.util.Calendar.getInstance().getTimeInMillis();
        try
        {
            this.searchProperties = new edu.tufts.vue.util.SharedProperties();
        }
        catch (Throwable t) {}
    }
	
	public void start()
	{
		run();
	}

    public void run()
    {
        try {
			org.osid.repository.AssetIterator assetIterator = this.repository.getAssetsBySearch(this.searchCriteria,
																								this.searchType,
																								this.searchProperties);
			long endTime = java.util.Calendar.getInstance().getTimeInMillis();

			this.searchEngine.searchComplete(this.searchIndex,
											 edu.tufts.vue.fsm.SearchEngine.SEARCH_COMPLETED,
											 null,
											 endTime - this.startTime,
											 assetIterator,
											 this.foreignIdString);
        } catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t);
			long endTime = java.util.Calendar.getInstance().getTimeInMillis();
			
			try {
				this.searchEngine.searchComplete(this.searchIndex,
												 edu.tufts.vue.fsm.SearchEngine.SEARCH_EXCEPTION,
												 t.getMessage(),
												 endTime - this.startTime,
												 new AssetIterator(new java.util.Vector()),
												 this.foreignIdString);
			} catch (Throwable t1) {
				edu.tufts.vue.util.Logger.log(t1);
			}
        }
    }
}
