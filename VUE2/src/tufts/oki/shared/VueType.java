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
 * VueType.java
 *
 * Created on November 15, 2003, 4:29 PM
 */

package tufts.oki.shared;

import tufts.oki.*;
/**
 *
 * @author  Daisuke Fujiwara
 */
public class VueType extends osid.shared.Type {
    
    public static final String VUE_TYPE_KEY = "osid.shared.VueType";
    
    /** Creates a new instance of VueType */
    public VueType() 
    {
        super ("osid.shared", OsidManager.AUTHORITY, VUE_TYPE_KEY, "This is a Vue Type");
    }
    
}
