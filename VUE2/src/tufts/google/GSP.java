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
 * GSP.java
 *
 * Created on April 18, 2003, 3:37 PM
 */

package tufts.google;

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
