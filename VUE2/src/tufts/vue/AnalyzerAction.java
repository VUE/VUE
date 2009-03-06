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

import javax.swing.JMenu;
import javax.swing.KeyStroke;
import edu.tufts.vue.layout.*;
import edu.tufts.vue.mbs.AnalyzerResult;
import edu.tufts.vue.mbs.LWComponentAnalyzer;
import edu.tufts.vue.mbs.OpenCalaisAnalyzer;
import edu.tufts.vue.mbs.YahooAnalyzer;

import java.util.*;
import java.awt.event.KeyEvent;
import javax.swing.Action;

import org.osid.repository.Asset;
import org.osid.repository.AssetIterator;
import org.osid.repository.RepositoryException;

import tufts.Util;
import tufts.vue.gui.GUI;
import tufts.vue.gui.Widget;


// contains layout actions. based on ArrangeAction. The default layout is random layout

public class AnalyzerAction extends Actions.LWCAction {
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
    public static void addResultsToMap(List<Resource> resourceList,LWComponent centerComponent)
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
    	
    }
    public void act(LWComponent c) 
    {
    	
    	//if (VUE.getDRBrowser().getDataSourceViewer().mMapBasedSearchThreads.size() > 0)
    	//{		
    		//System.out.println("SEARCH ALREADY RUNNING!!!!!!!!!!!");
    	//	return;
    		
    	//}
    	List<AnalyzerResult> list = analyzer.analyze(c);
    	Iterator<AnalyzerResult> i = list.iterator();
    	VUE.getActiveViewer().getSelection().clear();
     	//System.out.println("BLAH");
    	boolean hasResults = false;
    	String query = "";
    	while (i.hasNext())
    	{		
    		hasResults = true;
    		AnalyzerResult l = i.next();
   
    		if (l !=null && l.getValue() !=null)
    			query += l.getValue().trim() + " ";
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
    	if (!hasResults)
    	{
    		VueUtil.alert("This node does not contain enough meaningful information to be analyzed.", "Node Analysis Error");
    	
    		//just use the label from the node, it didn't go right.
    	}
    	
    	VUE.getDRBrowser().getDataSourceViewer().queryEditor.setCriteria(query);
    	VUE.getDRBrowser().getDataSourceViewer().mapBasedSearch(c);    	
    	GUI.makeVisibleOnScreen(VUE.getDRBrowser());
    	return;
    }
    
    // random layout. scatters nodes at random
    public static final AnalyzerAction yahoo = new AnalyzerAction(new YahooAnalyzer(),"Yahoo Content Analyzer",null);
    public static final AnalyzerAction calais = new AnalyzerAction(new OpenCalaisAnalyzer(),"Perform Map Based Search",null);
    public static final AutoTaggerAction calaisAutoTagger = new AutoTaggerAction(new OpenCalaisAnalyzer(),"Auto Tag Node",null);
 
    public static final Action[] ANALYZER_ACTIONS = {
        yahoo,
        calais,
    };

	public static void buildSubMenu(JMenu analyzeNodeMenu) {
		
	//	analyzeNodeMenu.add(AnalyzerAction.yahoo);
	//	JMenu calaisMenu = new JMenu("Node Analysis");
    	analyzeNodeMenu.add(calais);
    	analyzeNodeMenu.add(calaisAutoTagger);
//    	analyzeNodeMenu.add(calaisMenu);
		
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
	    		VueUtil.alert("This node does not contain enough meaningful information to be analyzed.", "Node Analysis Error");
	    	}
	    	VUE.getActiveViewer().selectionAdd(c);
	    	return;
	    }
	}
}
