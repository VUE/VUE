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
 * RES.java
 *
 * Created on April 18, 2003, 3:29 PM
 */

package edu.tufts.osidimpl.repository.google.local;

import java.util.Vector;

/**
 *
 * @author  akumar03
 */
public class RES {
    
    private int startNumber;
    
    private int endNumber;
    
    private Vector resultList;
    
    /** Creates a new instance of RES */
    public RES() {
    }
    
    public void setStartNumber(int startNumber) {
        this.startNumber = startNumber;
    }
    
    public int getStartNumber() {
        return this.startNumber;
    }
    
    public void setEndNumber(int endNumber) {
        this.endNumber = endNumber;
    }
    
    public int getEndNumber(){
        return this.endNumber;
    }
    
    public void setResultList(Vector resultList) {
        this.resultList = resultList;
    }
    
    public Vector getResultList() {
      return  this.resultList;
    }
    public void addObject(Object obj) {
       //-- ignore
     }
}
    
