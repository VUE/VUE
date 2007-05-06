package tufts.vue;

/**
 * @see tufts.vue.ActiveChangeSupport
 * @see tufts.vue.ActiveEvent
 *
 * If a particular class impl is only interested in a single type of activeChanged,
 * it can use the type information in it's implementation.  If it is interested
 * in updates on more than one type of active object, it can implement
 * the generic version and check the type information itself in the callback.
 *
 * @author Scott Fraize
 * @version $Revision: 1.1 $ / $Date: 2007-05-06 20:14:17 $ / $Author: sfraize $
 */
public interface ActiveListener<T> extends java.util.EventListener {
    public void activeChanged(ActiveEvent<T> e);
}
    
        
