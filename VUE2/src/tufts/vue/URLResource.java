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
 * URLResource.java
 *
 * Created on January 23, 2004, 2:25 PM
 */

package  tufts.vue;

/**
 *
 * @author  akumar03
 */
public class URLResource extends MapResource {
    
    /** Creates a new instance of URLResource */
    public URLResource() {
    }
    public URLResource(String spec) {
        super(spec);
    }
    /*
    public void displayContent() {
        System.out.println(getClass() + " displayContent for " + this);
        try {
            this.accessAttempted = System.currentTimeMillis();
            if (VueUtil.isMacPlatform())
                VueUtil.openURL(toURLString());
            else
                VueUtil.openURL(getSpec());
            this.accessSuccessful = System.currentTimeMillis();
        } catch (Exception e) {
            System.err.println(e);
        }
    }
    */
    
}
