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

package tufts.vue;

/**
 * MapItem.java
 *
 * @author Scott Fraize
 * @version 6/7/03
 */
public interface MapItem
{
    public float getX();
    public void setX(float x);
    public float getY();
    public void setY(float y);
    
    public void setID(String ID);
    public void setLabel(String label);
    public void setNotes(String notes);
    //public void setMetaData(String metaData);
    //public void setCategory(String category);
    public void setResource(Resource resource);
    
    public Resource getResource();
    //public String getCategory();
    public String getID();
    public String getLabel();
    //public String getMetaData();
    
    //added by Daisuke
    public String getNotes();
}
