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
    }    /** Get a string representation from a unique identifier
     *
  

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