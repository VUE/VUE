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
 * Result.java
 *
 * Created on April 18, 2003, 3:19 PM
 */

package  tufts.google;

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
