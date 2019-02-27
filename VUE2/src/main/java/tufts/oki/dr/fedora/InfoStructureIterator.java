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
 * Behavior
 * Created on October 14, 2003, 4:01 PM
 */

package tufts.oki.dr.fedora;

/**
 *
 * @author  akumar03
 */
public class InfoStructureIterator implements osid.dr.InfoStructureIterator {
    
    /** Creates a new instance of BehaviorInfoStructureIterator */
    java.util.Vector vector = new java.util.Vector();
    java.util.Map configuration = null;
    int i = 0;
    
    /** Creates a new instance of BehaviorIterator */
    public InfoStructureIterator(java.util.Vector vector) {
        this.vector = vector;
    }

   
    /**     Return true if there are additional  InfoRecords ; false otherwise.
     *     @return boolean
     *     @throws DigitalRepositoryException if there is a general failure
     */
    public boolean hasNext() throws osid.dr.DigitalRepositoryException {
         return (i < vector.size());
    }
    
    /**     Return the next InfoRecords.
     *     @return InfoRecord
     *     @throws DigitalRepositoryException if there is a general failure or if all objects have already been returned.
     */
    public osid.dr.InfoStructure next() throws osid.dr.DigitalRepositoryException {
          if (i >= vector.size()) {
            throw new osid.dr.DigitalRepositoryException("No more BehaviorInfoStructures");
        }

        return (osid.dr.InfoStructure) vector.elementAt(i++);
    }
    
}