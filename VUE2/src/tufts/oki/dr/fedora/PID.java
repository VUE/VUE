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

package tufts.oki.dr.fedora;

/*
 * PID.java
 *
 * Created on May 2, 2003, 11:18 AM
 */
 
 

import osid.shared.Id;
import osid.shared.SharedException;
import java.util.StringTokenizer;

/**
 *
 * @author  akumar03
 */
public class PID extends tufts.oki.shared.Id {
    

    
    /** Creates a new instance of PID */
    public PID()  throws osid.shared.SharedException {
        super();
    }
    
    public PID(String pid)  throws osid.shared.SharedException { 
        super(pid);
    }     
         


    /** Checks if the id is valid
     *
     *@return true if id is valid
     *
     **/
    public boolean isValid()  throws osid.shared.SharedException{
        // needs to be implemented.
        StringTokenizer st  = new StringTokenizer(getIdString(),":");
        int count = 0;
        while(st.hasMoreTokens()){
            st.nextToken();
            count++;
        }
        if(count == 2) 
            return true;
        else
            return false;
    }
    
    public String processId() throws osid.shared.SharedException{
        StringTokenizer st = new StringTokenizer(getIdString(),":");
        String processString = "";
        while(st.hasMoreTokens()) {
            processString += st.nextToken();
        }
        return processString;
    }
    
}