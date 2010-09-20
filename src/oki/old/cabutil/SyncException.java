
public class SyncException
    extends org.okip.service.filing.api.FilingException {

    /**
     * Constructor SyncException
     *
     * @param message
     *
     */
    public SyncException(String message) {
        super(message);
    }
    public SyncException(Throwable e) {
        super(e);
    }
    public SyncException(Throwable e, String message) {
        super(e, message);
    }
}
