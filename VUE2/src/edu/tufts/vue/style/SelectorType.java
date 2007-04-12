/*
 * SelectorType.java
 *
 * Created on April 6, 2007, 12:08 PM
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
 * <p>The entire file consists of original code.  Copyright &copy; 2003-2007
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

/**
 *
 * @author akumar03
 */

package edu.tufts.vue.style;

public class SelectorType extends org.osid.shared.Type{
    public static final SelectorType defaultType = new SelectorType("Default Selector","edu.tufts","default");
    public static final SelectorType link = new SelectorType("Link Selector","edu.tufts","link");
    public static final SelectorType node = new SelectorType("Node Selector","edu.tufts","node");
    public SelectorType(String authority
            , String domain
            , String keyword) {
        super(authority,domain,keyword);
    }
    
    public SelectorType(String authority
            , String domain
            , String keyword
            , String description) {
        super(authority,domain,keyword,description);
    }
    
    public static org.osid.shared.Type getLinkType() {
        return link;
    }
    
    public static org.osid.shared.Type getNodeType() {
        return node;
    }
    public static org.osid.shared.Type getDefaultType() {
        return defaultType;
    }
}
