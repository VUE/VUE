/*
 * RES.java
 *
 * Created on April 18, 2003, 3:29 PM
 */

package google;

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
    
