
public class LockException
extends SyncException
{
    public java.util.Properties lock;

    /**
     * Constructor LockException
     *
     * @param message
     *
     */
    public LockException(java.util.Properties lock, String message) {
        super(message);
        this.lock = lock;
    }

}


