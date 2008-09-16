package tufts.vue;


/**
 * Transitional interface for PropertyMap -> MetaMap migration.
 *
 * @version $Revision: 1.1 $ / $Date: 2008-09-16 12:09:09 $ / $Author: sfraize $
 * @author Scott Fraize
 */

public interface TableBag {

    public javax.swing.table.TableModel getTableModel();

    public int size();
    
    public interface Listener {
        void tableBagChanged(TableBag bag);
    }

    public void addListener(Listener l);
    public void removeListener(Listener l);

    
}
