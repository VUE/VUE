package tufts.vue;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * Digital Repository Browser
 */
class DRBrowser extends JPanel {
    public static DataSourceViewer dsViewer = null;

    private static final int DRBrowserWidth = 329;

    private JLabel loadingLabel;
   
    public DRBrowser()
    {
        setLayout(new BorderLayout());
        loadDataSourceViewer();
    }
    public DRBrowser(boolean delayed)
    {
        setLayout(new BorderLayout());
        if (delayed) {
            setMinimumSize(new Dimension(DRBrowserWidth,100));
            loadingLabel = new JLabel("Loading data sources...");
            loadingLabel.setBorder(new EmptyBorder(22,22,22,22));
            add(loadingLabel, BorderLayout.NORTH);
        } else {
            loadDataSourceViewer();
        }
    }
    
    public void loadDataSourceViewer()
    {
        DataSourceViewer dsv = new DataSourceViewer(this);
        dsv.setName("Data Source Viewer"); 
        if (dsViewer == null) {
            // set the statics to the first initialized DRBrowser only
            dsViewer = dsv;
            //tufts.vue.VUE.dataSourceViewer = dsv;
        }
        if (loadingLabel != null)
            remove(loadingLabel);
        setMinimumSize(null);
        add(dsv, BorderLayout.NORTH);
        validate();
    }


       
    
}
