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

public interface Pathway //extends MapItem
{
    public java.util.Iterator getElementIterator();
    public void addElement(Object element);
    public void removeElement(int index);
    public void moveElement(int oldIndex, int newIndex);
    public void setWeight(int weight);
    public int getWeight();
    public void setOrdered(boolean ordered);
    public boolean getOrdered();
    public java.util.List getElementList();
    //public void setElementList(java.util.List elementList);
    public String getLabel();
    public void setLabel(String label);
    public String getNotes();
    public void setNotes(String notes);
}
