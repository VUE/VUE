/*
 * GSP.java
 *
 * Created on April 18, 2003, 3:37 PM
 */

package google;

/**
 *
 * @author  akumar03
 */
public class GSP {
    
    private RES res;
    
    public String TM;
    
    /** Creates a new instance of GSP */
    public GSP() {
    }
    
    public void setRES(RES res) {
        this.res = res;
    }
    
    public RES getRES() {
        return this.res;
    }
 
    public void addObject(Object obj) {
      //  System.out.print("ignored"+obj);
       //-- ignore
     }

}
