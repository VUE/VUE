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
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */
/*
 * ArtifactResult.java
 *
 * Created on August 5, 2004, 12:50 PM
 */

package tufts.artifact;

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
