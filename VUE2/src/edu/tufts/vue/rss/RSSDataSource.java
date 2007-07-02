/*
 * RSSDatasource.java
 *
 * Created on June 25, 2007, 1:10 PM
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
 * <p>The entire file consists of original code.  Copyright &copy; 2003-2007
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

/**
 *
 * @author akumar03
 * @author Daniel J. Heller
 */

package edu.tufts.vue.rss;

import tufts.vue.*;

import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.*;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

public class RSSDataSource  extends VueDataSource{
    
    private JComponent resourceViewer;
    
    public RSSDataSource() {        
    }
    
    public RSSDataSource(String displayName, String address) throws DataSourceException
    {
        this.setDisplayName(displayName);
        this.setAddress(address);    
    }
    
    public void setAddress(String address)  throws DataSourceException{
        super.setAddress(address);
        this.setResourceViewer();
    }
    
    
    public void setResourceViewer()
    {
        URL address = null;
                
        try
        {
          address = new URL(getAddress());
        }
        catch(MalformedURLException mue)
        {
            System.out.println("Malformed URL Exception while opening RSS Feed: " + mue);
        }
        
        if(address == null)
        {
            System.out.println("Null URL for RSS Feed, aborting.. ");
            return;
        }
        
        SyndFeedInput feedBuilder = new SyndFeedInput();
        SyndFeed rssFeed = null;
        try          
        {        
          rssFeed = feedBuilder.build(new XmlReader(address));
        }
        catch(FeedException fe)
        {
            System.out.println("FeedException while building RSS feed: " + fe);
        }
        catch(java.io.IOException io)
        {
            System.out.println("IOException while building RSS feed: " + io);
        }
        
        if(rssFeed == null)
        {
            System.out.println("Null rssFeed, aborting... ");
            return;
        }
        
        List<SyndEntry> itemList = rssFeed.getEntries();
        
        List<URLResource> resourceList = new ArrayList<URLResource>();
        
        // getUri did not work for Reuters (and Atom feeds in general?), 
        // switched to getLink() instead (see below)
        /*System.out.println("itemList length: " + itemList.size());
        
        for(int j=0;j<itemList.size();j++)
        {
            SyndEntry entry = itemList.get(j);
            System.out.println(j + ":" + entry.getUri());
        }*/
        
        Iterator<SyndEntry> i = itemList.iterator();
        while(i.hasNext())
        {
            SyndEntry entry = i.next();
            URLResource res = null;
            try
            {
              //res = new URLResource(new URL(entry.getUri()));
              res = new URLResource(new URL(entry.getLink()));
            }
            catch(MalformedURLException mue)
            {
                System.out.println("Malformed URL Exception while creating RSS feed resource: " + mue);
            }
            if(res == null)
            {
                System.out.println("null resource created for rss feed, aborting... " + entry.getLink());
                return;
            }
            res.setTitle(entry.getTitle());
            resourceList.add(res);
        }
        
        VueDragTree fileTree = new VueDragTree(resourceList.iterator(), this.getDisplayName());
        fileTree.setRootVisible(true);
        fileTree.setShowsRootHandles(true);
        fileTree.expandRow(0);
        fileTree.setRootVisible(false);

        if (false) {
            JPanel localPanel = new JPanel();
            JScrollPane rSP = new JScrollPane(fileTree);
            localPanel.setMinimumSize(new Dimension(290,100));
            localPanel.setLayout(new BorderLayout());
            localPanel.add(rSP,BorderLayout.CENTER);
            this.resourceViewer = localPanel;
        } else {
            this.resourceViewer = fileTree;
        }
            
        //DataSourceViewer.refreshDataSourcePanel(this);
        
    }
    
    public JComponent getResourceViewer(){
        
        return this.resourceViewer;
        
    }
    
}
