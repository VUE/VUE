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
 * Hit.java
 *
 * Created on August 5, 2004, 12:23 PM
 */

package  tufts.artifact;

/**
 *
 * @author  akumar03
 *
 * A class to support marshalling and unmarshalling results from Artifact search.
 *
 *
 */
public class Hit {
    
    public String artifact;
    public String fullImage;
    public String thumb;
    public String title;
    public String artist;
    public String origin;
    public String culture;
    public String period;
    public String subject;
    public String material;
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
