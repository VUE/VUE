/*
 * Copyright 2003-2008 Tufts University  Licensed under the
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

package edu.tufts.vue.rss;

import tufts.vue.*;
import tufts.Util;

import java.util.*;
import java.io.*;
import java.awt.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.*;

import com.sun.syndication.io.*;
import com.sun.syndication.feed.*;
import com.sun.syndication.feed.synd.*;

/**
 * @version $Revision: 1.20 $ / $Date: 2008-09-16 12:03:24 $ / $Author: sfraize $
 *
 * @author akumar03
 * @author Daniel J. Heller
 * @author Scott Fraize
 */

public class RSSDataSource extends BrowseDataSource
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(RSSDataSource.class);
    
    private List<SyndEntry> mItems;
    
    public RSSDataSource() {
        //Log.debug("created empty RSS feed");
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
    public int getCount() {
        return mItems == null ? -1 : mItems.size();
    }

    /** apparently, some RSS Feeds (e.g., craigslist) will fail with "TOO MANY REDIRECTS" if there isn't SOME cookie sent,
     * so this always at least returns "none" */
    @Override public String getAuthenticationCookie() {
        final String cookie = super.getAuthenticationCookie();

        return cookie == null ? "none" : cookie;
    }
    
    
    @Override
    protected JComponent buildResourceViewer() {
        return loadViewer();
    }
    

    private JComponent loadViewer() {
        
        Log.debug("loadContentAndBuildViewer...");
        tufts.vue.VUE.diagPush("RSSLoad");
        JComponent viewer = null;
        try {
            viewer = loadContentAndBuildViewer();
        } finally {
            tufts.vue.VUE.diagPop();
        }
        return viewer;
    }
    
    private JComponent loadContentAndBuildViewer()
    {
        Log.debug("loadContentAndBuildViewer...");
        
        //final WireFeedInput feedBuilder = new WireFeedInput();

        final URLConnection conn = openAddress();
        final Map<String,List<String>> headers = conn.getHeaderFields();
        final SyndFeedInput feedBuilder = new SyndFeedInput();
        final SyndFeed _feed;
        
        try {
            //feed = feedBuilder.build(new InputStreamReader(conn.getInputStream()));
            
            // XmlReader does magic to try and best handle the input charset-encoding:
            XmlReader charsetEncodingReader = new XmlReader(conn);
            _feed = feedBuilder.build(charsetEncodingReader);

        } catch (java.io.IOException io) {
            throw new DataSourceException(null, io);
        } catch (FeedException fe) {
            throw new DataSourceException(null, fe);
        }
        
        final SyndFeed feed = _feed;

        if (TEST_DEBUG) {
            dumpFeed(feed);
            return null;
        }

        feed.setFeedType("atom_1.0");
        final WireFeed wireFeed = _feed.createWireFeed();

        this.mItems = feed.getEntries();

        Log.debug("WIRE FEED: " + tufts.Util.tag(wireFeed));

        final List<Resource> resources = new ArrayList<Resource>();
        
        Log.debug("item count: " + mItems.size());

        if (mItems.size() == 0)
            throw new DataSourceException("[Empty RSS feed]");

        final Resource fr = Resource.instance(feed.getLink());

        fr.reset();
        fr.setClientType(Resource.DIRECTORY);
        //fr.setDataType("rss");
        fr.setTitle(feed.getTitle());
        
        if (DEBUG.Enabled) {
        
            if (DEBUG.WORK) {
                fr.setProperty("~WIREMARK", wireFeed.getForeignMarkup());
                fr.setProperty("~WIREMOD", wireFeed.getModules());
                fr.setProperty("zFEED", feed);

                fr.addPropertyIfContent("feed-supported-types", feed.getSupportedFeedTypes());
            }
            
            fr.addPropertyIfContent("feed-uri", feed.getUri());
            fr.addPropertyIfContent("feed-image", feed.getImage());
            fr.addPropertyIfContent("feed-type", feed.getEncoding());
            fr.addPropertyIfContent("feed-language", feed.getLanguage());
            fr.addPropertyIfContent("feed-encoding", feed.getEncoding());

            for (Map.Entry<String,List<String>> e : headers.entrySet()) {
                Object value = e.getValue();
                if (value instanceof Collection && ((Collection)value).size() == 1)
                    value = ((Collection)value).toArray()[0];
                if (e.getKey() == null)
                    fr.setProperty("HTTP-response", value);
                else
                    fr.setProperty("HTTP:" + e.getKey(), value);
            }
            
            
        }
                       
        fr.addPropertyIfContent("Title", feed.getTitle());
        fr.addPropertyIfContent("Copyright", feed.getCopyright());
        fr.addPropertyIfContent("Author", feed.getAuthor());

        final String desc = feed.getDescription().trim();
        if ("This file is an XML representation of some issues".equalsIgnoreCase(desc))
            // ignore JIRA standard meaningless comment
            ((URLResource)fr).setSpec(getAddress());
        else
            fr.addPropertyIfContent("Description", desc);
                                

        //r.addPropertyIfContent("Copyright", feed.getTitle());

        resources.add(fr);
            
        for (SyndEntry item : mItems) {

            Resource r = null;
            try {
                r = createRSSResource(item);
            } catch (Throwable t) {
                Log.error("creating resource from: " + item + " in feed " + feed, t);
            }

            if (r == null) {
                Log.warn("failed to create resource from rss feed item, skipping; link=" + item.getLink());
                continue;
            }
                
            resources.add(r);
                
        }
        
        VueDragTree fileTree = new VueDragTree(resources, this.getDisplayName());
        fileTree.setRootVisible(true);
        fileTree.setShowsRootHandles(true);
        fileTree.expandRow(0);
        fileTree.setRootVisible(false);
        fileTree.setName(getClass().getSimpleName() + ": " + getAddress());

        return fileTree;
    }

    private Resource createRSSResource(final SyndEntry item)
    {
        // DH: getUri did not work for Reuters (and Atom feeds in general?),
        // switched to getLink() instead (see below)
        final Resource r = Resource.instance(item.getLink());

        r.setTitle(item.getTitle());
            
        r.addPropertyIfContent("Title", item.getTitle());
        r.addPropertyIfContent("Author", item.getAuthor());
        r.addPropertyIfContent("Published", item.getPublishedDate());
        r.addPropertyIfContent("Updated", item.getUpdatedDate());
        r.addPropertyIfContent("URI", item.getUri());

        final SyndContent content = item.getDescription();
        
        r.addPropertyIfContent("Description", content.getValue());
            
        if (!DEBUG.Enabled)
            return r;
            
        r.addPropertyIfContent("~0rss-content-type", content.getType());
        r.addPropertyIfContent("~0rss-content-mode", content.getMode());
        
        r.addPropertyIfContent("~Authors", item.getAuthors());
        r.addPropertyIfContent("~Contributors", item.getContributors());

        Object fm = item.getForeignMarkup();
        if (fm instanceof Collection) {
            String fmt = tufts.Util.tag(fm);
            for (Object o : ((Collection)fm)) {
                fmt += '\n';
                fmt += tufts.Util.tags(o);
            }
            fm = fmt;
        }
        r.addPropertyIfContent("~ForeignMarkup", fm);
        
        r.addPropertyIfContent("~Categories", item.getCategories());
        r.addPropertyIfContent("~Contents", item.getContents());
        r.addPropertyIfContent("~Enclosures", item.getEnclosures());
        r.addPropertyIfContent("~Modules", item.getModules());
        r.addPropertyIfContent("~Description", content);
        
        return r;

    }

    private static void dumpFeed(SyndFeed _feed)
    {
        Log.info("supported-types: " + _feed.getSupportedFeedTypes());

        if (true) {
            WireFeed feed = _feed.createWireFeed();
            //feed.setFeedType("atom_1.0");
            Log.info("WIRE-OUT  types: " + WireFeedOutput.getSupportedFeedTypes());
            
            Log.info("FEED: " + Util.tags(feed));
            
            WireFeedOutput feedOut = new WireFeedOutput();
            try {
                feedOut.output(feed, new PrintWriter(System.out));
                //Log.info("AS-STRING:" + feedOut.outputString(feed));
            } catch (Throwable t) {
                Log.error(t, t);
            }
        } else {
            SyndFeed feed = _feed;
            
            feed.setFeedType("atom_1.0");
            //feed.setFeedType("rss_2.0");
            //feed.setFeedType("rss_1.0");

            SyndFeedOutput feedOut = new SyndFeedOutput();
            try {
                feedOut.output(feed, new PrintWriter(System.out));
            } catch (Throwable t) {
                Log.error(t, t);
            }
        }        
    }

    private static boolean TEST_DEBUG = false;

    
    public static void main(String[] args)
    {
        TEST_DEBUG = true;
        
        DEBUG.Enabled = true;
        DEBUG.DR = true;
        DEBUG.RESOURCE = true;
        DEBUG.DATA = true;

        tufts.vue.VUE.init(args);
        
        RSSDataSource ds = new RSSDataSource("test", args[0]);

        ds.loadContentAndBuildViewer();

    }
    
}


            
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
