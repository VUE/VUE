/*
 * SearchType.java
 *
 * Created on November 1, 2003, 9:58 AM
 */

package tufts.oki.dr.fedora;

/**
 *
 * @author  akumar03
 */
import osid.shared.Type;

public class SearchType extends Type {
    
    /** Creates a new instance of SearchType */

    public SearchType(String type) {
         super("Fedora_Search","tufts.edu",type);
    }
    
}
