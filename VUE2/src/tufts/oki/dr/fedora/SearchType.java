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
