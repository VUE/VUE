/*
 * LWContainerEvent.java
 *
 * Created on September 27, 2003, 1:46 PM
 */

package tufts.vue;

/**
 *
 * @author  Daisuke Fujiwara
 */
public class LWContainerEvent  {
    
    private LWContainer container;
    /** Creates a new instance of MapEvent */
    public LWContainerEvent(LWContainer container) 
    {
        this.container = container;
    }
    
    public LWContainer getLWContainer()
    {
        return container;
    }
}
