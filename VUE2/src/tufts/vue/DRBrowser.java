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
            JLabel label = new JLabel("Loading data sources...");
            label.setBorder(new EmptyBorder(22,22,0,0));
            add(label, BorderLayout.NORTH);
        } else {
            loadDataSourceViewer();
        }
    }
    
    public void loadDataSourceViewer()
    {
        DataSourceViewer dsv = new DataSourceViewer(this);
        dsv.setName("Data Source Viewer"); 
        if (dsViewer != null) {
            // set the statics to the first initialized DRBrowser only
            this.dsViewer = dsv;
            //tufts.vue.VUE.dataSourceViewer = dsv;
        }
        if (getComponentCount() > 0)
            remove(getComponent(0)); // remove loading message
        setMinimumSize(null);
        add(dsv, BorderLayout.NORTH);
        validate();
    }


       
    
}
