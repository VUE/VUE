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

package edu.tufts.osidimpl.repository.artifact;
/**
 *
 * @author  akumar03
 *A class to support marshalling and unmarshalling results from Artifact search.
 * 
 */
public class SearchCriteria {
    
    public String query;
    public String course;
    public String subject;
    public String origin;
    public String name;
    public int lectureNumber;
    public String lecturer;
    public int maxReturns;
    public int numHits;
    public int pages;
    
    /** Creates a new instance of SearchCriteria */
    public SearchCriteria() {
    }
     public void addObject(Object obj) {
      //  System.out.print("ignored"+obj);
       //-- ignore
     }
}
