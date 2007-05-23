package tufts.vue;

/**
 * @see tufts.vue.ActiveChangeSupport
 * @see tufts.vue.ActiveEvent
 * @author Scott Fraize
 * @version $Revision: 1.2 $ / $Date: 2007-05-23 06:51:30 $ / $Author: sfraize $
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

    public boolean hasSource(Object o) {
        if (source == o)
            return true;
        else if (source instanceof ActiveEvent)
            return ((ActiveEvent)source).hasSource(o);
        else
            return false;
    }

    public boolean hasSourceOfType(Class clazz) {
        if (clazz.isInstance(source))
            return true;
        else if (source instanceof ActiveEvent)
            return ((ActiveEvent)source).hasSourceOfType(clazz);
        else
            return false;
    }
    

    public String toString() {
        return "ActiveEvent<" + type.getName() + ">[src=" + source + "; active=" + active + "]";
    }
    
}
