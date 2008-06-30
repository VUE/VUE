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
 The enginer calls each search on its own thread.  The engine creates the threads.
 The engine also get called by the thread when the search is done.  Results are
 available via the engine for any particular search.
 */

public class VueSearchEngine
implements edu.tufts.vue.fsm.SearchEngine
{
    private static int completed = 0;
    private static long startTime = 0;
    private static long endTime = 0;
	private static int numberOfSearches = -1;
	private static boolean isComplete = false;
	
	private static int[] status = null;
	private static String[] exceptionMessages = null;
	private static long[] durations = null;
	private static org.osid.repository.AssetIterator[] assetIterators = null;
	private static java.util.Vector foreignIdStringVector = null;
	
	private static edu.tufts.vue.fsm.SearchEngine searchEngine = new VueSearchEngine();
	
	public static edu.tufts.vue.fsm.SearchEngine getInstance()
	{
		return searchEngine;
	}
	
	public VueSearchEngine()
	{		
	}

	public void search(edu.tufts.vue.fsm.Query[] queries)
	{
		completed = 0;
		startTime = 0;
		endTime = 0;
		numberOfSearches = queries.length;
		isComplete = false;
		
		status = new int[numberOfSearches];
		exceptionMessages = new String[numberOfSearches];
		durations = new long[numberOfSearches];
		assetIterators = new org.osid.repository.AssetIterator[numberOfSearches];
		foreignIdStringVector = new java.util.Vector();
		
		for (int j=0; j < numberOfSearches; j++) {
			status[j] = SEARCH_PENDING;
		}

        try
        {
            startTime = java.util.Calendar.getInstance().getTimeInMillis();
            
            int k = 0;
            for (int i=0; i < queries.length; i++)
            {
				SearchThread searchThread = new SearchThread(queries[i].getForeignIdString(),
															 queries[i].getRepository(),
															 k++,
															 queries[i].getSearchCriteria(), 
															 queries[i].getSearchType(),
															 queries[i].getSearchProperties());
				status[i] = SEARCH_RUNNING;
				try {
					searchThread.start();
				} catch (Throwable discard) {
				}
			}
        } catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"while searching");
        }
	}

	public void searchComplete(int searchIndex,
							   int statusCode,
							   String exceptionMessage,
							   long duration,
							   org.osid.repository.AssetIterator assetIterator,
							   String foreignIdString)
	{
        try {
			completed++;
			status[searchIndex] = statusCode;
			exceptionMessages[searchIndex] = exceptionMessage;
			durations[searchIndex] = duration;
			assetIterators[searchIndex] = assetIterator;
			foreignIdStringVector.addElement(foreignIdString);

            if (completed == numberOfSearches)
            {
				isComplete = true;
				endTime = java.util.Calendar.getInstance().getTimeInMillis();
            }
        } catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"in serach complete");
        }
    }

    public long getStartTime()
    {
        return this.startTime;
    }
	
    public long getEndTime()
    {
        return this.endTime;
    }
	
    public long getDuration(int index)
    {
		try {
			return durations[index];
        } catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"in getDuration()");
        }
		return 0;
    }
	
    public int getStatus(int index)
    {
		try {
			return status[index];
        } catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"in getStatus()");
        }
		return -1;
    }
	
    public int getNumSearches()
    {
        return numberOfSearches;
    }
	
    public org.osid.repository.AssetIterator getAssetIterator(int index)
    {
		try {
			return assetIterators[index];
        } catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"in getAssetIterator()");
        }
		return null;
    }

	public org.osid.repository.AssetIterator getAssetIterator(String foreignIdString)
	{
		try {
//			System.out.println("getAssetIterator in vSearchEngine " + foreignIdString);
			int index = foreignIdStringVector.indexOf(foreignIdString);
			if (index != -1) {
//				System.out.print("found at index " + index);
				return assetIterators[index];
			} else {
//				System.out.println("not found");
			}
        } catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"in getAssetIterator()");
        }
		return null;
	}
	
    public String getExceptionMessage(int index)
    {
		try {
			return exceptionMessages[index];
        } catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"in getExceptionMessage()");
        }
		return null;
	}
		
	public boolean isComplete() {
		return isComplete;
	}
}