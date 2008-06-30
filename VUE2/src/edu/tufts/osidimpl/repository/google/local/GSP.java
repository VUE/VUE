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
 * GSP.java
 *
 * Created on April 18, 2003, 3:37 PM
 */

package edu.tufts.osidimpl.repository.google.local;

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
