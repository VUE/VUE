package tufts.vue;

/*
 * AnalyzerAction.java
 *
 * Created on October 8, 2008, 1:01 PM
 *
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

import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import edu.tufts.seasr.MeandreItem;
import edu.tufts.seasr.MeandreResponse;
import edu.tufts.vue.layout.*;
import edu.tufts.vue.mbs.AnalyzerResult;
import edu.tufts.vue.mbs.LWComponentAnalyzer;
import edu.tufts.vue.mbs.OpenCalaisAnalyzer;
import edu.tufts.vue.mbs.YahooAnalyzer;
import edu.tufts.vue.mbs.SeasrAnalyzer;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.Action;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osid.repository.Asset;
import org.osid.repository.AssetIterator;
import org.osid.repository.RepositoryException;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import tufts.Util;
import tufts.vue.Actions.LWCAction;
import tufts.vue.gui.GUI;
import tufts.vue.gui.Widget;

import tufts.vue.VueUtil;


import edu.tufts.vue.metadata.MetadataList;
import edu.tufts.vue.preferences.implementations.ShowAgainDialog;
import edu.tufts.vue.ui.DefaultQueryEditor;
import edu.tufts.seasr.SeasrConfigLoader;
import edu.tufts.seasr.FlowGroup;
import edu.tufts.seasr.Flow;



// contains layout actions. based on ArrangeAction. The default layout is random layout

public class AnalyzerAction extends Actions.LWCAction {
	private boolean firstTime = true;
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(AnalyzerAction.class);
    private LWComponentAnalyzer analyzer = null;
    
    private AnalyzerAction(LWComponentAnalyzer analyzer, String name,KeyStroke keyStroke) {
        super(name,keyStroke);
        this.analyzer = analyzer;
    }
  //  boolean mayModifySelection() { return true; }
    boolean mayModifySelection() { return true; }
    
    boolean enabledFor(LWSelection s) {
        return (s.size() == 1
                && s.first() instanceof LWNode); // todo: a have capability check (free-layout?  !isLaidOut() ?)
    }
    private final static int maxLabelLineLength =30;
    private static final Color[] ValueNodeDataColors = VueResources.getColorArray("node.dataValue.color.cycle");
    
    public static void addResultsToMap(List<Resource> resourceList,LWComponent centerComponent, int colorSelection)
    {
    	 //*** MAP BASED SEARCHING ***
        //Equivalent of the are you feeling lucky feature where
        //we are going to grab a bunch of results and add them right to the map.
        final int RESULTS_TO_ADD=5;
        int size = resourceList.size();
        
       // VUE.getSelection().clear();
   //     VUE.getSelection().add(mCenterComponent);
        LWMap active = VUE.getActiveMap();
        java.util.List<LWComponent> comps = new ArrayList<LWComponent>();
        for (int p=0; p < RESULTS_TO_ADD && p<size; p++)
        {
        	//System.out.println(resourceList.get(p).toString());
        	Resource r = (Resource)resourceList.get(p);
        	tufts.vue.LWNode node = new tufts.vue.LWNode(r.getTitle());
        	node.setFillColor(ValueNodeDataColors[colorSelection%10]);
        	node.setResource(r);
        	String label = node.getLabel();

            label = Util.formatLines(label, maxLabelLineLength);
            node.setLabel(label);
            
        	comps.add(node);
        	node.layout();
        	node.setLocation(centerComponent.getLocation());
        	LWLink link = new LWLink(centerComponent,node);            
        	comps.add(link);
        	link.layout();
        	
        }
        active.addChildren(comps);
       // VUE.getSelection().setTo(comps,"search results");
        LayoutAction.search.act(comps);
        VUE.getSelection().setTo(centerComponent);
        LWSelection selection = VUE.getSelection();
        LWContainer.bringToFront(selection);

    	
    }
    
 
    public void act(LWComponent c) 
    {
    	
    	//if (VUE.getDRBrowser().getDataSourceViewer().mMapBasedSearchThreads.size() > 0)
    	//{		
    		//System.out.println("SEARCH ALREADY RUNNING!!!!!!!!!!!");
    	//	return;
    		
    	//}  
    	
    	//Set the wait cursor here clear it when there's no threads left in the mbs.
    	GUI.activateWaitCursor();
    	
    	//figure out if anything is checked
    	int selectionCount = DefaultQueryEditor.getSelectedRepositoryCount();
    	if (selectionCount == 0)
    	{
			
			 final ShowAgainDialog sad = new ShowAgainDialog(VUE.getApplicationFrame(),"noResourceSelected",VueResources.local("noResourceSelected.title"),"OK",null);
			            	JPanel panel = new JPanel(new GridLayout(1,1));
			            	String label = Util.formatLines(VueResources.local("noResourceSelected.message"),30);
			            	JLabel vLabel = new  JLabel(label);
			            	if(Util.isMacPlatform()){
			            		panel.setPreferredSize(new Dimension(425,25));
			            		panel.setSize(new Dimension(425,25));
			            		panel.setMinimumSize(new Dimension(425,25));
			            	}else{
			            		panel.setPreferredSize(new Dimension(425,25));
			            	}
			            	panel.add(vLabel);
			        	    sad.setContentPanel(panel);
			                VueUtil.centerOnScreen(sad);
			                if (sad.showAgain())
			                {
			                	sad.setVisible(true);
			                	
			                
			                	sad.setVisible(false);
			                    sad.dispose();
			                    GUI.clearWaitCursor();
			                    return;
			                }
			    
    	}
    	List<AnalyzerResult> list = analyzer.analyze(c);
    	Iterator<AnalyzerResult> i = list.iterator();
    	VUE.getActiveViewer().getSelection().clear();
     	//System.out.println("BLAH");
    	boolean hasResults = false;
    	String query = "";
    	final int MAX_TERMS=1;
    	int termCount = 0;
    	while (i.hasNext() && termCount < MAX_TERMS)
    	{		
    		hasResults = true;
    		AnalyzerResult l = i.next();
   
    		if (l !=null && l.getValue() !=null)
    		{ //System.out.println(l.getRelevance() + " : " + l.getValue());
    			query += l.getValue().trim() + " ";
    			termCount++;
    		}
    	/*
    	 * MK - For testing purposes I was adding Nodes of the search terms to the map.
    	 * 	if (l.getValue() !=null  && l.getValue().trim() != " " && !label.startsWith("Topic"))
    		{
    			LWNode node = new LWNode(label);
    			VUE.getActiveMap().add(node);
    			//VUE.getActiveViewer().getSelection().add(node);
    			LWLink link = new LWLink(c,node);
    			VUE.getActiveMap().add(link);
    			VUE.getActiveViewer().getSelection().add(link);            		
    			LayoutAction.random.act(VUE.getActiveViewer().getSelection());
    		}*/
    	}
    	
    	if (query.equals(""))
    		query = c.getLabel();
    	//if (!hasResults)
    	//{
    	//	VueUtil.alert(VueResources.getString("dialog.analyzeerror.message"), VueResources.getString("dialog.analyzeerror.title"));
    	
    		//just use the label from the node, it didn't go right.
    	//}
    	
    	VUE.getDRBrowser().getDataSourceViewer().queryEditor.setCriteria(query);
    	VUE.getDRBrowser().getDataSourceViewer().mapBasedSearch(c);    	
    	if (firstTime)
    	{
    		GUI.makeVisibleOnScreen(VUE.getDRBrowser());
    		firstTime = false;
    	}
    	return;
    }
    
    // random layout. scatters nodes at random
   // public static final AnalyzerAction yahoo = new AnalyzerAction(new YahooAnalyzer(),VueResources.getString("analyzeaction.yahoocontent"),null);
    public static final AnalyzerAction calais = new AnalyzerAction(new OpenCalaisAnalyzer(),VueResources.getString("analyzeaction.performmapbased"),null);
    public static final AutoTaggerAction calaisAutoTagger = new AutoTaggerAction(new OpenCalaisAnalyzer(),VueResources.getString("analyzeaction.autotag"),null);
    public static final SemanticMapAction semanticMapAction = new SemanticMapAction(new OpenCalaisAnalyzer(),VueResources.getString("analyzeaction.semanticmap"),null);
 
    public static final Action luckyImageAction =
        new LWCAction(VueResources.getString("analyzeaction.luckyimage")) {
            public void act(LWComponent c) {
            	JSONArray _ja;
            	try {
        	
            		HttpClient client = new HttpClient();
        			GetMethod method = new GetMethod("http://api.flickr.com/services/rest/?method=flickr.photos.search&api_key=093a05d30b0be54c8b18d227fec80b6b&per_page=1&tag_mode=any&sort=relevance&format=json&text=" + URLEncoder.encode(c.getLabel()));
        			method.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY); 
        			client.getParams().setParameter(HttpMethodParams.USER_AGENT,
        											"Visual Understanding Environment http://vue.tufts.edu");
        											// Send GET request
        			int statusCode = client.executeMethod(method);
        			
        			if (statusCode != HttpStatus.SC_OK) {
        				System.err.println("Method failed: " + method.getStatusLine());
        			}
        			InputStream rstream = null;
        			
        			// Get the response body
        			rstream = method.getResponseBodyAsStream();
        			
        			// Process the response from Yahoo! Web Services
        			BufferedReader br = new BufferedReader(new InputStreamReader(rstream));
        			String jsonString = "";
        			String line;
        			while ((line = br.readLine()) != null) {
        				jsonString += line;
        			}
        			br.close();
        			
        			// Construct a JSONObject from a source JSON text string.
        			// A JSONObject is an unordered collection of name/value pairs. Its external
        			// form is a string wrapped in curly braces with colons between the names
        			// and values, and commas between the values and names.
        			jsonString = jsonString.substring(14);
        			jsonString = jsonString.substring(0,jsonString.length()-1);
        			JSONObject jo = new JSONObject(jsonString);

        			// A JSONArray is an ordered sequence of values. Its external form is a
        	        // string wrapped in square brackets with commas between the values.
        			
        	        // Get the JSONObject value associated with the search result key.
        	        jo = jo.getJSONObject("photos");
        			
        	        //System.out.println(jo.toString());
        			
        	        // Get the JSONArray value associated with the Result key
        	        _ja = jo.getJSONArray("photo");
        			
        	        // Get the number of search results in this set
        	        int resultCount = _ja.length();
        			//System.out.println("results: " + resultCount);
        			
        			java.util.Vector vector = new java.util.Vector();
        			
        			for (int i=0; i < resultCount; i++) {
        				
        				JSONObject resultObject = _ja.getJSONObject(i);
        				
        				String id = null;
        				String owner = null;
        				String secret = null;
        				String server = null;
        				Integer farm = null;
        				String title = null;
        				
        				id = (String)resultObject.getString("id");
        				owner = (String)resultObject.get("owner");
        				secret = (String)resultObject.getString("secret");
        				server = (String)resultObject.get("server");				
        				farm = (Integer)resultObject.get("farm");
        				title = (String)resultObject.get("title");       
        				String big = "http://farm" + farm + ".static.flickr.com/" + server +"/" + id + "_" + secret +".jpg";
        				c.setResource(URLResource.create(new URL(big)));
        			}
        			
        		} catch (Throwable ex) {
        			ex.printStackTrace();
        		}

             return;   
            }
    };
    
    public static final Action[] ANALYZER_ACTIONS = {
        calais,
    };

	public static void buildSubMenu(JMenu analyzeNodeMenu) {
		
	//	analyzeNodeMenu.add(AnalyzerAction.yahoo);
	//	JMenu calaisMenu = new JMenu("Node Analysis");
    	analyzeNodeMenu.add(calais);
    	analyzeNodeMenu.add(calaisAutoTagger);
    	analyzeNodeMenu.add(semanticMapAction);
    	analyzeNodeMenu.add(luckyImageAction);
    	analyzeNodeMenu.add(getSeasrMenu());
//    	analyzeNodeMenu.add(seasr);
//    	analyzeNodeMenu.add(calaisMenu);
		
	}
	
	public static JMenu getSeasrMenu() {
		JMenu seasrMenu = new JMenu("SEASR Analysis");
		JMenu createNodesMenu = new JMenu(SeasrConfigLoader.CREATE_NODES);
		JMenu getMetadataMenu = new JMenu(SeasrConfigLoader.ADD_METADATA);
		JMenu getInfoMenu = new JMenu(SeasrConfigLoader.ADD_NOTES);
		SeasrConfigLoader scl = new SeasrConfigLoader();
		
		try {
		FlowGroup fg = scl.getFlowGroup(SeasrConfigLoader.CREATE_NODES);
		
		for(Flow flow: fg.getFlowList()) {
			SeasrAction seasr = new SeasrAction(new SeasrAnalyzer(flow),flow.getLabel(),null);
			createNodesMenu.add(seasr);
		}
		fg = scl.getFlowGroup(SeasrConfigLoader.ADD_METADATA);
		for(Flow flow: fg.getFlowList()){
			getMetadataMenu.add(new SeasrMetadataAction(new SeasrAnalyzer(flow),flow.getLabel(),null));
		}
		fg = scl.getFlowGroup(SeasrConfigLoader.ADD_NOTES);
		for(Flow flow: fg.getFlowList()){
			getInfoMenu.add(new SeasrInfoAction(new SeasrAnalyzer(flow),flow.getLabel(),null));
		} 
		seasrMenu.add(createNodesMenu);
		seasrMenu.add(getMetadataMenu);
		seasrMenu.add(getInfoMenu);
		 
		} catch(Exception ex) {
			System.out.println(ex);
			ex.printStackTrace();
		}
		 
		return seasrMenu;
		 
	}
	
	static class SemanticMapAction extends Actions.LWCAction {
		   // private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(AnalyzerAction.class);
		    private LWComponentAnalyzer analyzer = null;
		    
		    private SemanticMapAction(LWComponentAnalyzer analyzer, String name,KeyStroke keyStroke) {
		        super(name,keyStroke);
		        this.analyzer = analyzer;
		    }
		  //  boolean mayModifySelection() { return true; }
		    
		    
	        
		    public void act(LWComponent c) 
		    {
		    	try
		    	{
		    		GUI.activateWaitCursor();
			    	//bags of components to add to map
			    	final java.util.List<LWComponent> subCatcomps = new ArrayList<LWComponent>();
			    	java.util.List<LWComponent> categoryComps = new ArrayList<LWComponent>();
			    	
			    	//Analyze the resource and get a multimap of types and values
			    	Multimap<String,AnalyzerResult> list = null;
			    	try{
			    		list = analyzer.analyzeResource(c);	
			    	}
			    	catch(Exception e)
			    	{
			    		VueUtil.alert(VueResources.getString("dialog.semanticmaperror.message"), VueResources.getString("dialog.analyzeerror.title"));
			    		e.printStackTrace();
			    		return;
			    	}
			    	
			    	if (list.isEmpty())
			    	{
			    		VueUtil.alert(VueResources.getString("dialog.semanticmaperror.noresults"), VueResources.getString("dialog.analyzeerror.title"));		    	
			    		return;
			    	
			    	}
			    	Color joinNodeColor = new Color(8,119,192);
			    	Color leafNodeColor = new Color(157,219,83);
			    			    
			    	Iterator<String> categoryIterator = list.keySet().iterator();
			    	boolean hasResults = false;
			    	HashMap<LWNode,Collection> categoryChildren = new HashMap<LWNode,Collection>();
			    	while (categoryIterator.hasNext())
			    	{		
			    		String key = categoryIterator.next();
			    		
			    		//Make a node for the key connect it to the central node.
			    		//TODO
	
			    		Collection al =  list.get(key);
			    		tufts.vue.LWNode node = new tufts.vue.LWNode(key);
			    		node.setLocation(c.getLocation());
			    		tufts.vue.LWLink link = new tufts.vue.LWLink(c,node);
			    		
			    		if (al.size() > 1)
			    		{
			    		//	node.setFillColor(new Color())
			    			node.setFillColor(joinNodeColor);
			    			link.mStrokeStyle.setTo(LWComponent.StrokeStyle.DASHED);
			    			categoryComps.add(node);
			    			categoryComps.add(link);
			    		
			    			categoryChildren.put(node, al);
			    		}
			    		else
			    		{
			    			Iterator analyzerResults = al.iterator();
			    			AnalyzerResult res = (AnalyzerResult)analyzerResults.next();
			    			tufts.vue.LWNode singleNode = new tufts.vue.LWNode(res.getValue());
				    		tufts.vue.LWLink singleLink = new tufts.vue.LWLink(c,singleNode);
				    		singleNode.setFillColor(leafNodeColor);
				    		singleLink.setLabel(key);
				    		Font f = node.getFont();
		    				Font derive = f.deriveFont(((float)(10+res.getCount())));
		    				node.setFont(derive);
		    				link.setStrokeColor(getColorFromRelevance(res.getRelevance()));
				    		
		    				//Topic is a weird category so just skip it.
				    		if (!key.equals("Topic"))
				    		{categoryComps.add(singleNode);
				    		categoryComps.add(singleLink);}			    		
			    		}
			    	}
			    		final LWMap active = VUE.getActiveMap();
					    active.addChildren(categoryComps);			    
					    LayoutAction.circle.act(categoryComps);
					     
					    Iterator nodeIterator = categoryChildren.keySet().iterator();
					    while (nodeIterator.hasNext())
					    {
					    	subCatcomps.clear();
					    	LWNode joinNode = (LWNode) nodeIterator.next();
					    	
					    	Collection subCategories = categoryChildren.get(joinNode);
					    	Iterator subCatIt = subCategories.iterator();
					    	while (subCatIt.hasNext())
					    	{
					    		AnalyzerResult res = (AnalyzerResult) subCatIt.next();
					    		tufts.vue.LWNode node = new tufts.vue.LWNode(res.getValue());
					    		tufts.vue.LWLink link = new tufts.vue.LWLink(joinNode,node);
					    		node.setLocation(joinNode.getLocation());
					    		node.setFillColor(leafNodeColor);
					    		Font f = node.getFont();
			    				Font derive = f.deriveFont(((float)(10+res.getCount())));
			    				node.setFont(derive);
			    				link.setStrokeColor(getColorFromRelevance(res.getRelevance()));
					    		subCatcomps.add(node);
					    		subCatcomps.add(link);				    						    		
					    	}
					    
					           	active.addChildren(subCatcomps);			    
					           	LayoutAction.search.act(subCatcomps);
	        				    	
	                        
					    
					    }		  
		    	}
		    	finally
		    	{
		    		GUI.clearWaitCursor();
		    	}
		    }
		    
		    public Color getColorFromRelevance(double relevance)
		    {
		    	/*if (relevance == 1.0)
		    		relevance=relevance-.01;
		    	double component = 255 - (255 * relevance);
		    	
		    	if (component < 0)
		    		component = 0;
		    	java.awt.Color c =null;
		    	try
		    	{
		    		c= new java.awt.Color((int)component,(int)component,(int)component);
		    	}
		    	catch(IllegalArgumentException iae)
		    	{

		    		return Color.black;
		    	}*/
		    	return Color.gray;
		    }
		}
	static class AutoTaggerAction extends Actions.LWCAction {
	   // private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(AnalyzerAction.class);
	    private LWComponentAnalyzer analyzer = null;
	    
	    private AutoTaggerAction(LWComponentAnalyzer analyzer, String name,KeyStroke keyStroke) {
	        super(name,keyStroke);
	        this.analyzer = analyzer;
	    }
	  //  boolean mayModifySelection() { return true; }
	    
	  
	    public void act(LWComponent c) 
	    {
	    	
	    	List<AnalyzerResult> list = analyzer.analyze(c,true);
	    	Iterator<AnalyzerResult> i = list.iterator();
	    	VUE.getActiveViewer().getSelection().clear();
	     	//System.out.println("BLAH");
	    	boolean hasResults = false;
	    	while (i.hasNext())
	    	{		
	    		AnalyzerResult l = i.next();	    		
	    		if (c.getMetadataList().size() ==1 && 
	    				c.getMetadataList().get(0).getValue().equals(""))
	    			c.getMetadataList().remove(0);
	    		if (l.getType()==null || (l.getType() !=null && l.getType().equals("NA")))
	    		{
	    			if (l.getValue()!=null && l.getValue().trim() != "" && l.getValue().length() > 1)
	    			{	    		
	    				hasResults = true;
	    				c.getMetadataList().add("none", l.getValue());
	    			}
	    		}
	        	else
	        	{
	    			if (l.getValue()!=null && l.getValue().trim() != "" && l.getValue().length() > 1)
	    			{	
	    				hasResults = true;
	    				c.getMetadataList().add(l.getType(), l.getValue());
	    			}
	        	}
	    		
	    	}
	    	
	    	if (!hasResults)
	    	{
	    		VueUtil.alert(VueResources.getString("dialog.analyzeerror.message"), VueResources.getString("dialog.analyzeerror.title"));
	    	}
	    	VUE.getActiveViewer().selectionAdd(c);
	    	return;
	    }
	}
	static class SeasrAction extends Actions.LWCAction{
		  private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(SeasrAction.class);
		  private LWComponentAnalyzer analyzer = null;
		    
		   public SeasrAction(LWComponentAnalyzer analyzer, String name,KeyStroke keyStroke) {
		        super(name,keyStroke);
		        this.analyzer = analyzer;
		    }
		   public void act(LWComponent c) 
		    {
		    	System.out.println("Executing action on:"+c.getLabel());
		    	List<AnalyzerResult> list = analyzer.analyze(c,true);
		    	Iterator<AnalyzerResult> i = list.iterator();
		    	LWMap active = VUE.getActiveMap();
		        java.util.List<LWComponent> comps = new ArrayList<LWComponent>();
		    	while (i.hasNext())
		    	{		
		    		AnalyzerResult l = i.next();
		    		tufts.vue.LWNode node = new tufts.vue.LWNode(l.getType()); 
		    		comps.add(node);
		    		node.layout();
		    		node.setLocation(c.getLocation());
		        	LWLink link = new LWLink(c,node);            
		        	comps.add(link);
		        	link.layout();
		        	
		        }
		        active.addChildren(comps);
		        LayoutAction.circle.act(comps);
		         
		    }
	}
	static class SeasrMetadataAction extends Actions.LWCAction{
		  private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(SeasrAction.class);
		  private LWComponentAnalyzer analyzer = null;
		    
		   public  SeasrMetadataAction(LWComponentAnalyzer analyzer, String name,KeyStroke keyStroke) {
		        super(name,keyStroke);
		        this.analyzer = analyzer;
		    }
		   public void act(LWComponent c) 
		    {
		    	
		    	List<AnalyzerResult> list = analyzer.analyze(c,true);
		    	Iterator<AnalyzerResult> i = list.iterator();
		    	LWMap active = VUE.getActiveMap();
		        java.util.List<LWComponent> comps = new ArrayList<LWComponent>();
		    	while (i.hasNext())
		    	{		
		    		AnalyzerResult l = i.next();
		    		 MetadataList mList  = c.getMetadataList();
		    		mList.add("tag",l.getType());
		        	
		        }
		        
		         
		    }
	}
	
	static class SeasrInfoAction extends Actions.LWCAction{
		  private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(SeasrAction.class);
		  public static final String OUT_TITLE = "Output from SEASR Flow: ";
		  private  SeasrAnalyzer analyzer = null;
		    
		   public  SeasrInfoAction( SeasrAnalyzer analyzer, String name,KeyStroke keyStroke) {
		        super(name,keyStroke);
		        this.analyzer = analyzer;
		    }
		   public void act(LWComponent c) 
		    {
		    	
		    	List<AnalyzerResult> list = analyzer.analyze(c,true);
		    	Iterator<AnalyzerResult> i = list.iterator();
		    	LWMap active = VUE.getActiveMap();
		        java.util.List<LWComponent> comps = new ArrayList<LWComponent>();
		        String info = OUT_TITLE+analyzer.getFlow().getLabel()+"\n\n";
		    	while (i.hasNext())
		    	{		
		    		AnalyzerResult l = i.next();
		    		info += l.getType()+" - "+l.getValue()+"\n";
		    		
		        	
		        }
		    	 c.setNotes(info);
		         
		    }
	}
}
