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
 A search engine manages the search of each repository in a federated search.  The
 queries passed in contain the repository and search data.  Information from each
 search is available separately and for the whole cummulatively.
 */

public interface SearchEngine
{
    public static final int SEARCH_PENDING = 0;
    public static final int SEARCH_RUNNING = 1;
    public static final int SEARCH_COMPLETED = 2;
    public static final int SEARCH_EXCEPTION = 3;
	
	public void search(Query[] queries);
	
	public void searchComplete(int searchIndex,
							   int statusCode,
							   String exceptionMessage,
							   long duration,
							   org.osid.repository.AssetIterator assetIterator,
							   String foreignIdString);
	
	public long getStartTime();
	
	public long getEndTime();
	
	public long getDuration(int index);
	
	public int getStatus(int index);
	
	public int getNumSearches();
	
	public org.osid.repository.AssetIterator getAssetIterator(int index);
	
	public org.osid.repository.AssetIterator getAssetIterator(String foreignIdString);
	
	public String getExceptionMessage(int index);
	
	public boolean isComplete();
}