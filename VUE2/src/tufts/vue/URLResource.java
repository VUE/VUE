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
    public void displayContent() {
        System.out.println("displayContent for " + this);
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
    
}
