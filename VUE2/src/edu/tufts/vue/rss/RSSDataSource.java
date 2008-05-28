/*
 * Copyright 2003-2007 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
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

public class RSSDataSource extends VueDataSource
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(RSSDataSource.class);
    private static final String JIRA_SFRAIZE_COOKIE = "seraph.os.cookie=LkPlQkOlJlHkHiEpGiOiGjJjFi";
    
    public static final String DEFAULT_AUTHENTICATION_COOKIE = JIRA_SFRAIZE_COOKIE;
    public static final String AUTHENTICATION_COOKIE_KEY = "url_authentication_cookie";
    
    private String authenticationCookie = null;
    
    public RSSDataSource() {
        Log.debug("Created empty RSS feed");
    }
    
    public RSSDataSource(String displayName, String address) throws DataSourceException {
        this.setDisplayName(displayName);
        this.setAddress(address);
    }

    @Override
    public String getTypeName() {
        return "RSS Feed";
    }
    
    @Override
    public void setConfiguration(java.util.Properties p) {

        super.setConfiguration(p);

        String val;
        
        if ((val = p.getProperty(AUTHENTICATION_COOKIE_KEY)) != null)
            setAuthenticationCookie(val);
    }

    private void setAuthenticationCookie(String s) {
        authenticationCookie = s;
    }
    
//     public CharSequence getConfigurationUI_XML_Fields() {

//         final StringBuffer b = new StringBuffer();       
//         // all elements appear to be required or ConfigurationUI bombs
//         b.append("<field>");
//         b.append("<key>url_authentication_cookie</key>");
//         b.append("<title>Authentication</title>");
//         b.append("<description>Any required authentication cookie</description>");
//         //b.append("<default></default>");
//         b.append("<default>" + DEFAULT_AUTHENTICATION_COOKIE + "</default>");
//         b.append("<mandatory>false</mandatory>");
//         b.append("<maxChars>99</maxChars>");
//         b.append("<ui>0</ui>");
//         b.append("</field>");

//         return b;
//     }
    
    @Override
    protected JComponent buildResourceViewer() {
        return loadContentAndBuildViewer();
    }
    

    private JComponent loadContentAndBuildViewer() {
        
        Log.debug("loadContentAndBuildViewer");
        tufts.vue.VUE.pushDiag("RSSLoad");
        JComponent viewer = null;
        try {
            viewer = _loadContentAndBuildViewer();
        } catch (Throwable t) {
            Log.error("loadContentAndBuildViewer", t);
        }
        tufts.vue.VUE.popDiag();
        return viewer;
    }
    
    private JComponent _loadContentAndBuildViewer()
    {
        URL address = null;
        
        try {
            address = new URL(getAddress());
        } catch(MalformedURLException mue) {
            System.out.println("Malformed URL Exception while opening RSS Feed: " + mue);
        }
        
        if(address == null) {
            System.out.println("Null URL for RSS Feed, aborting.. ");
            return null;
        }
        SyndFeedInput feedBuilder = new SyndFeedInput();
        SyndFeed rssFeed = null;
        try {
            if (DEBUG.Enabled) Log.debug("opening " + address);
            URLConnection conn = address.openConnection(); 
            conn.setRequestProperty("User-Agent","Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1.8) Gecko/20071008 Firefox/2.0.0.8");
            
            if (authenticationCookie != null)
                conn.setRequestProperty("Cookie", authenticationCookie);
            else if (tufts.vue.DEBUG.Enabled)
                conn.setRequestProperty("Cookie", DEFAULT_AUTHENTICATION_COOKIE);
            
            // TODO: "old-stye" build-in VueDataSource's don't appear to be able to persist
            // extra properties, so above we're always sending a default cookie above
            // just in case we're accessing VUE's JIRA site -- this for VUE3 test phase only
            
            if (DEBUG.Enabled) Log.debug("request-properties: " + conn.getRequestProperties());
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
        
        if (rssFeed == null) {
            System.out.println("Null rssFeed, aborting... ");
            return null;
        }
        
        List<SyndEntry> itemList = rssFeed.getEntries();
        
        List<Resource> resourceList = new ArrayList<Resource>();
        
        // getUri did not work for Reuters (and Atom feeds in general?),
        // switched to getLink() instead (see below)
        Log.debug("itemList length: " + itemList.size());
        /*
        for(int j=0;j<itemList.size();j++)
        {
            SyndEntry entry = itemList.get(j);
            System.out.println(j + ":" + entry.getUri());
        }*/
        
        Iterator<SyndEntry> i = itemList.iterator();
        while (i.hasNext()) {
            SyndEntry entry = i.next();

            final Resource res = Resource.getFactory().get(entry.getLink());
            
//             Resource res = null;
//             try {
//                 //res = new URLResource(new URL(entry.getUri()));
//                 //System.out.println("trying to create rss item resource entry is: " + entry);
//                 String link = entry.getLink();
//                 //System.out.println("trying to create rss resource - link:" + link);
//                 URL url = new URL(link);
//                 //System.out.println("trying to create rss resource - url:" + url);
//                 /*link = *///java.net.URLDecoder.decode(link,"UTF-8");
//                 //res = new URLResource(url);
//                 res = Resource.getFactory().get(url);
//             }
//             catch(MalformedURLException mue) {
//                 System.out.println("Malformed URL Exception while creating RSS feed resource: " + mue);
//             }
            
            if (res == null) {
                Log.warn("null resource created for rss feed, skipping: " + entry.getLink());
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
        
//         if (false) {
//             JPanel localPanel = new JPanel();
//             JScrollPane rSP = new JScrollPane(fileTree);
//             localPanel.setMinimumSize(new Dimension(290,100));
//             localPanel.setLayout(new BorderLayout());
//             localPanel.add(rSP,BorderLayout.CENTER);
//             this.mViewer = localPanel;
//         } else {
//             this.mViewer = fileTree;
//         }

        fileTree.setName(getClass().getSimpleName() + ": " + getAddress());

        return fileTree;
        
      
    }
    
}
