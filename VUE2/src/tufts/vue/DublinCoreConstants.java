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
    public static final String DC_NAMESPACE = "dc:";
    
}
