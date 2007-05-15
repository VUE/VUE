package tufts.vue;

/**
 * Generic interface for accepting / validating an object in any given scenario.
 *
 * @version $Revision: 1.1 $ / $Date: 2007-05-15 21:53:52 $ / $Author: sfraize $
 */
public interface Acceptor<T> {

    /** @return true if the given object is considered acceptable  */
    public boolean accept(T o);
}
