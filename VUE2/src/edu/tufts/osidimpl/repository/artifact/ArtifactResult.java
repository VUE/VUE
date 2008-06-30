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
/*
 * ArtifactResult.java
 *
 * Created on August 5, 2004, 12:50 PM
 */

package edu.tufts.osidimpl.repository.artifact;

import java.util.Vector;
/**
 *
 * @author  akumar03
 *
 *A class to support marshalling and unmarshalling results from Artifact search.
 * 
 */
public class ArtifactResult {
    Vector hitList;
    SearchCriteria searchCriteria;
    
    /** Creates a new instance of ArtifactResult */
    public ArtifactResult() {
    }
    
    public void setHitList(Vector hitList){
        this.hitList = hitList;
    }
    public Vector getHitList(){
        return this.hitList;
    }
    public void setSearchCriteria(SearchCriteria searchCriteria){
        this.searchCriteria = searchCriteria;
    }
    public SearchCriteria getSearchCriteria(){
        return this.searchCriteria;
    }
     public void addObject(Object obj) {
       //-- ignore
     }
}
