/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

package tufts.vue;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * Digital Repository Browser
 */
class DRBrowser extends JPanel {
    public static DataSourceViewer dsViewer = null;

    private static final int DRBrowserWidth = 319;

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
        try {
            DataSourceViewer dsv = new DataSourceViewer(this);
            dsv.setName("Data Source Viewer"); 
            if (dsViewer == null) {
                // set the statics to the first initialized DRBrowser only
                dsViewer = dsv;
                //tufts.vue.VUE.dataSourceViewer = dsv;
            }
            if (loadingLabel != null)
                remove(loadingLabel);
            //setMinimumSize(null); some data-sources smaller: don't allow shrinkage
            add(dsv, BorderLayout.NORTH);
            validate();
        } catch (Exception e) {
            e.printStackTrace();
            loadingLabel.setText(e.toString());
        }
    }


       
    
}
