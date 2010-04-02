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
/*
 * Hit.java
 *
 * Created on August 5, 2004, 12:23 PM
 */

package edu.tufts.osidimpl.repository.artifact;

/**
 *
 * @author  akumar03
 *
 * A class to support marshalling and unmarshalling results from Artifact search.
 *
 *
 */

import java.util.*;
public class Hit {
    
    public String artifact;
    public String fullImage;
    public String thumb;
    public String title;
    public List<String>  artistList;
    public String origin;
    public String culture;
    public String period;
    public String subject;
    public List<String> materialList;
    public String currentLocation;
    public String view;
    
    
    /** Creates a new instance of Hit */
    public Hit() {
    }
    public void addObject(Object obj) {
        //  System.out.print("ignored"+obj);
        //-- ignore
    }
    
    public String getArtifact(){
        return artifact;
    }
    
    public void setMaterialList(List<String> materialList) {
    	this.materialList = materialList;
    }
    
    public List<String> getMaterialList() {
    	return this.materialList;
    }
    
    public void setArtistList(List<String> artistList) {
    	this.artistList = artistList;
    }
    
    public List<String> getArtistList() {
    	return this.artistList;
    }
    
    
    public String getFullImage(){
        return fullImage;
    }
    public String getThumb(){
        return thumb;
    }
    public String getTitle(){
        return title;
    }
     
}
