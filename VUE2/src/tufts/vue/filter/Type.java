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
 * Class.java
 *
 * Created on February 13, 2004, 2:45 PM
 */

package tufts.vue.filter;

/**
 *
 * @author  akumar03
 *
 * A type defines all 
 */
import java.util.List;

public interface Type{
    
    public static final String STRING_TYPE = "String";
    public static final String INTEGER_TYPE = "Integer";
    public static final String BOOLEAN_TYPE = "Boolean";
    
    /** Creates a new instance of Class */
    public void setDisplayName(String displayName);
    public String getDisplayName();
    public List getOperators();
    public Operator getDefaultOperator();
    public List getSettableOperators();
    /** true if s2 is true given s1 **/
    public boolean compare(Statement s1,Statement s2);
    public boolean isValidKey();
    public boolean isValidValue();
}
