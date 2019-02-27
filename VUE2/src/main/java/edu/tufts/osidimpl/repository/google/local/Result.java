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
 * Result.java
 *
 * Created on April 18, 2003, 3:19 PM
 */

package edu.tufts.osidimpl.repository.google.local;

/**
 *
 * @author  akumar03
 */
public class Result {
    
    private int count;
    
    private String mimeType;
    
    private String url;
    
    private String title;
    
    private String description;
    
    /** Creates a new instance of Result */
    public Result() {
    }
    
    public void setCount(int count) {
        this.count = count;
    }
    
    public int getCount() {
        return this.count;
    }
    
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
    
    public String getMimeType() {
        return this.mimeType;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getUrl() {
        return this.url;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getTitle(){
        return this.title;
    }
    
    public void setDescription(String description){
        this.description = description;
    }
    
    public String getDescription(){
        return this.description;
    }
    
     public void addObject(Object obj) {
   
       //-- ignore
     }

}
