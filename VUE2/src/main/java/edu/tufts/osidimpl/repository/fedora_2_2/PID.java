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
 * PID.java
 *
 * Created on April 7, 2006 11:18 AM
 */
 
 package  edu.tufts.osidimpl.repository.fedora_2_2;


import org.osid.shared.Id;
import org.osid.shared.SharedException;
import java.util.StringTokenizer;

/**
 *
 * @author  akumar03
 */
public class PID extends tufts.oki.id.Id {
    

    
    /** Creates a new instance of PID */
    public PID()  throws org.osid.shared.SharedException {
        super();
    }
    
    public PID(String pid)  throws org.osid.shared.SharedException { 
        super(pid);
    }     
         


    /** Checks if the id is valid
     *
     *@return true if id is valid
     *
     **/
    public boolean isValid()  throws org.osid.shared.SharedException{
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
    
    public String processId() throws org.osid.shared.SharedException{
        StringTokenizer st = new StringTokenizer(getIdString(),":");
        String processString = "";
        while(st.hasMoreTokens()) {
            processString += st.nextToken();
        }
        return processString;
    }
    
}