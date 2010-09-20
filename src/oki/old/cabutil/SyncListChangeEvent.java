

public class SyncListChangeEvent extends javax.swing.event.ChangeEvent
{
    public int changes; // entries added or deleted
    public int updates; // entries status changed
    public int tosync; // total entries not current
    public SyncListChangeEvent(Object src, int u, int c, int todo)
    {
        super(src);
        updates = u;
        changes = c;
        tosync = todo;
    }
    public SyncListChangeEvent(Object src, int u, int c)
    {
        this(src, u, c, 0);
    }

    public String toString()
    {
        return super.toString() + " changes="+changes + " updates="+ updates + " tosync=" + tosync;
    }
        
}
