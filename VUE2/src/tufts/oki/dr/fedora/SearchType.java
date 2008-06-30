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
