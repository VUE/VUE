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
 * RES.java
 *
 * Created on April 18, 2003, 3:29 PM
 */

package tufts.google;

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
    
