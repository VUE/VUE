/*
 * Class.java
 *
 * Created on February 5, 2004, 3:28 PM
 */

package tufts.vue;

/**
 * An interface that holds all the dublin core field names;
 * @author  akumar03
 *
 */
public interface DublinCoreConstants {
    
    /** Creates a new instance of Class */
    public static int DC_TITLE = 0;
    public static int DC_CREATOR = 1;
    public static int DC_SUBJECT = 2;
    public static int DC_DATE = 3;
    public static int DC_TYPE= 4;
    public static int DC_FORMAT= 5;
    public static int DC_IDENTIFIER = 6;
    public static int DC_COLLECTION = 7;
    public static int DC_COVERAGE = 8;
    
    public static int SUPPORTED_NUMBER = 9;
   
    public static final String[] DC_FIELDS = {"title","creator","subject","date","type","format","identifier","collection","coverage"};
    
    
}
