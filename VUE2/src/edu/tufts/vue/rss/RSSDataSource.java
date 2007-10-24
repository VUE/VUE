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

import java.util.*;
import java.io.*;
import java.awt.*;
import java.net.*;
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
        System.out.println("Created empty RSS feed");
    }
    
    public RSSDataSource(String displayName, String address) throws DataSourceException {
        this.setDisplayName(displayName);
        this.setAddress(address);
    }
    
    public void setAddress(String address)  throws DataSourceException{
        try {
            
            System.out.println("Setting address for RSS feed: "+address);
            super.setAddress(address);
            this.setResourceViewer();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        
    }
    
    
    public void setResourceViewer() {
        URL address = null;
        
        try {
            address = new URL(getAddress());
        } catch(MalformedURLException mue) {
            System.out.println("Malformed URL Exception while opening RSS Feed: " + mue);
        }
        
        if(address == null) {
            System.out.println("Null URL for RSS Feed, aborting.. ");
            return;
        }
        SyndFeedInput feedBuilder = new SyndFeedInput();
        SyndFeed rssFeed = null;
        try {
            URLConnection conn = address.openConnection(); 
            conn.setRequestProperty("User-Agent","Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1.8) Gecko/20071008 Firefox/2.0.0.8");
            conn.connect(); 
            XmlReader reader = new XmlReader(conn);
            rssFeed = feedBuilder.build(reader);
         } catch(FeedException fe) {
            System.out.println("FeedException while building RSS feed: " + fe);
            fe.printStackTrace();
        } catch(java.io.IOException io) {
            System.out.println("IOException while building RSS feed: " + io);
            io.printStackTrace();
        } catch(Throwable t) {
            t.printStackTrace();
        }
        
        if(rssFeed == null) {
            System.out.println("Null rssFeed, aborting... ");
            return;
        }
        
        List<SyndEntry> itemList = rssFeed.getEntries();
        
        List<Resource> resourceList = new ArrayList<Resource>();
        
        // getUri did not work for Reuters (and Atom feeds in general?),
        // switched to getLink() instead (see below)
        System.out.println("itemList length: " + itemList.size());
        /*
        for(int j=0;j<itemList.size();j++)
        {
            SyndEntry entry = itemList.get(j);
            System.out.println(j + ":" + entry.getUri());
        }*/
        
        Iterator<SyndEntry> i = itemList.iterator();
        while(i.hasNext()) {
            SyndEntry entry = i.next();
            Resource res = null;
            try {
                //res = new URLResource(new URL(entry.getUri()));
                //System.out.println("trying to create rss item resource entry is: " + entry);
                String link = entry.getLink();
                //System.out.println("trying to create rss resource - link:" + link);
                URL url = new URL(link);
                //System.out.println("trying to create rss resource - url:" + url);
                /*link = *///java.net.URLDecoder.decode(link,"UTF-8");
                //res = new URLResource(url);
                res = Resource.getFactory().get(url);
            }
            catch(MalformedURLException mue) {
                System.out.println("Malformed URL Exception while creating RSS feed resource: " + mue);
            }
            if(res == null) {
                System.out.println("null resource created for rss feed, aborting... " + entry.getLink());
                continue;
            }
            res.setTitle(entry.getTitle());
            resourceList.add(res);
        }
        
        VueDragTree fileTree = new VueDragTree(resourceList, this.getDisplayName());
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
        
      
    }
    
    public JComponent getResourceViewer(){
        
        return this.resourceViewer;
        
    }
    
}
