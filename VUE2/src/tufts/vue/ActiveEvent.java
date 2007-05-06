package tufts.vue;

/**
 * @see tufts.vue.ActiveChangeSupport
 * @see tufts.vue.ActiveEvent
 * @author Scott Fraize
 * @version $Revision: 1.1 $ / $Date: 2007-05-06 20:14:17 $ / $Author: sfraize $
 */
public class ActiveEvent<T> {
    public final Class<T> type;
    public final Object source;
    public final T active;
    public final T oldActive;

    ActiveEvent(Class<T> type, Object source, T oldActive, T newActive) {
        this.type = type;
        this.source = source;
        this.oldActive = oldActive;
        this.active = newActive;
    }

    public String toString() {
        return "ActiveEvent<" + type.getName() + ">[src=" + source + "; active=" + active + "]";
    }
    
}
