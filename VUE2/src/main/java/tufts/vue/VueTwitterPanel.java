package tufts.vue;

import static tufts.Util.reverse;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.google.common.collect.Multimap;

import edu.tufts.vue.layout.ClusterLayout;
import edu.tufts.vue.layout.ListRandomLayout;
import edu.tufts.vue.layout.ScrollingTabularLayout;
import edu.tufts.vue.layout.TabularLayout;
import edu.tufts.vue.mbs.AnalyzerResult;
import edu.tufts.vue.mbs.OpenCalaisAnalyzer;
import edu.tufts.vue.metadata.MetadataList;
import tufts.vue.ds.Field;
import tufts.vue.gui.GUI;
import twitter4j.Tweet;
import twitter4j.Twitter;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.TwitterException;

public class VueTwitterPanel extends JPanel implements ItemListener, ActionListener {

	private final static String FROM_LAYER="From";
	//private final static String TO_LAYER="To";
	private final static String GEO_LAYER="Geo";
	private final static String ANALYSIS_LAYER="Analysis";
	private final static String TWEETS_LAYER="Tweets";   
	private static final long serialVersionUID = 1L;
	
	private static final Color TweetNodeColor = VueResources.getColor("twitter.tweet.color", Color.gray);
	private static final float TweetNodeStrokeWidth = VueResources.getInt("twitter.tweet.stroke.width", 0);
	private static final Color TweetNodeStrokeColor = VueResources.getColor("twitter.tweet.stroke.color", Color.black);
	private static final Font TweetNodeFont = VueResources.getFont("twitter.tweet.font");

	private static final Color ClusterNodeTextColor = VueResources.getColor("node.dataValue.text.color", Color.black);
	private static final Font ClusterNodeFont = VueResources.getFont("node.dataValue.font");
	private static final Color[] ClusterNodeDataColors = VueResources.getColorArray("node.dataValue.color.cycle");
	private static int NextColor = 0;
	
	private JTextField mSearchFieldEditor = null;
    private PropertyPanel mPropPanel = null;
    private JButton followButton = null;
    private final JRadioButton scatterCheckbox = new JRadioButton(VueResources.getString("twitter.scatter")); 
    private final JRadioButton fromCheckbox =  new JRadioButton(VueResources.getString("twitter.from")); 
   // private final JRadioButton toCheckbox =new JRadioButton(VueResources.getString("twitter.to"));
   // private JRadioButton geographicLocationCheckbox = null;
    private final JRadioButton analysisCheckbox = new JRadioButton(VueResources.getString("twitter.analysis")); 
    private TwitterThread twitterThread = null;
    private final ButtonGroup group = new ButtonGroup();

    final private HashMap<String,LWMap.Layer> layerList = new HashMap<String,LWMap.Layer>();
    
	private boolean layersInit = false;
	
    public VueTwitterPanel() {                    
    	twitterThread = new TwitterThread();
        JPanel innerPanel = new JPanel();
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        //BoxLayout boxLayout = new BoxLayout(innerPanel,BoxLayout.Y_AXIS);
        innerPanel.setLayout(gridbag);
        //mTitleEditor = new JTextField();
        mSearchFieldEditor = new JTextField();
       // geographicLocationCheckbox = new JRadioButton(VueResources.getString("twitter.location"));       
        group.add(scatterCheckbox);
       // group.add(geographicLocationCheckbox);
        group.add(fromCheckbox);
       // group.add(toCheckbox);
        group.add(analysisCheckbox);
        scatterCheckbox.setSelected(true);
        
        scatterCheckbox.addItemListener(this);
        fromCheckbox.addItemListener(this);
     //   toCheckbox.addItemListener(this);
        analysisCheckbox.addItemListener(this);
                
        mPropPanel  = new PropertyPanel();
        mPropPanel.addProperty(VueResources.getString("twitter.searchField"), mSearchFieldEditor); //added through metadata
        JLabel titleLabel = new JLabel(VueResources.getString("twitter.title"));
        followButton = new JButton(VueResources.getString("twitter.button.activate"));
        followButton.addActionListener(this);

        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        gridbag.setConstraints(titleLabel,c);
        innerPanel.add(titleLabel);
        
        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        gridbag.setConstraints(mPropPanel,c);
        innerPanel.add(mPropPanel);
        
        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        gridbag.setConstraints(scatterCheckbox,c);
        innerPanel.add(scatterCheckbox);
        
        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        gridbag.setConstraints(fromCheckbox,c);
        innerPanel.add(fromCheckbox);
        
        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
      //  gridbag.setConstraints(toCheckbox,c);
      //  innerPanel.add(toCheckbox);

        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
      //  gridbag.setConstraints(geographicLocationCheckbox,c);
       // innerPanel.add(geographicLocationCheckbox);
        
        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        gridbag.setConstraints(analysisCheckbox,c);
        innerPanel.add(analysisCheckbox);
        
        c.gridy=12;
        c.weightx = 1.0;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.NONE;
        gridbag.setConstraints(followButton,c);
        innerPanel.add(followButton);       
        
        new PropertiesEditor(true);
        JPanel metadataPanel = new JPanel(new BorderLayout());
        mSearchFieldEditor.setFont(GUI.LabelFace);
        c.weighty = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.BOTH;
        gridbag.setConstraints(metadataPanel,c);
        innerPanel.add(metadataPanel);     
        setLayout(new BorderLayout());
        add(innerPanel,BorderLayout.CENTER);
        setBorder(BorderFactory.createEmptyBorder(10,10,0,6));
       
    }


	public void itemStateChanged(ItemEvent e) {
		// TODO Auto-generated method stub
		if (e.getStateChange() == ItemEvent.SELECTED)
			twitterThread.layout(null);
		
	}

	private Thread t = null;
	
	@SuppressWarnings("deprecation")
	public void actionPerformed(ActionEvent e) {	
		if (e.getSource().equals(followButton))
		{
			initLayers();
			
			if (followButton.getText().equals(VueResources.getString("twitter.button.activate")))
			{
				t = new Thread(twitterThread);
				t.start();	
				followButton.setText(VueResources.getString("twitter.button.deactivate"));
			}
			else
			{
				t.stop();
				followButton.setText(VueResources.getString("twitter.button.activate"));
			}		
		}
	}	
	
	private void initLayers()
	{
		LWMap map = VUE.getActiveMap();
		if (!layersInit) {
			map.addLayer(FROM_LAYER);
		//	map.addLayer(TO_LAYER);
			//map.addLayer(GEO_LAYER);
			map.addLayer(ANALYSIS_LAYER);
			map.addLayer(TWEETS_LAYER);
			 for (LWComponent layer : reverse(map.getChildren())) {
				 if (layer instanceof LWMap.Layer) {
					layerList.put(layer.getLabel(), (LWMap.Layer)layer); 
					if (!layer.getLabel().equals(TWEETS_LAYER))
						((LWMap.Layer)layer).setVisible(false);
				 }
			 }
		 layersInit=true;
		}
	}
	
	class TwitterThread implements Runnable
	{

		private static final long UPDATE_INTERVAL = 5000;
		private HashMap<String,LWNode> fromUserClusterPoints = new HashMap<String,LWNode>();
		//private HashMap<String,LWNode> geoClusterPoints = new HashMap<String,LWNode>();
		private HashMap<String,LWNode> toUserClusterPoints = new HashMap<String,LWNode>();		
		private HashMap<String,LWNode> analysisClusterPoints = new HashMap<String,LWNode>();		

		private Twitter twitter;
		private ListRandomLayout randomLayout = new ListRandomLayout();
		private TabularLayout tableLayout = new TabularLayout();
		private ClusterLayout clusterLayout = new ClusterLayout();
		private ScrollingTabularLayout scrollTableLayout = new ScrollingTabularLayout();

		
		private LWNode handleFromNode(Tweet tweet)
		{
			LWNode fromNode = fromUserClusterPoints.get(tweet.getFromUser());
			
			if (fromNode == null)
			{
				fromNode = new LWNode(tweet.getFromUser());	
				fromNode.setStyle(getClusterStyleNode());
				
				fromUserClusterPoints.put(tweet.getFromUser(), fromNode);
				final LWNode finalFromNode = fromNode;
				javax.swing.SwingUtilities.invokeLater(new Runnable() {
					public void run() 
					{
						LWMap.Layer fromLayer = layerList.get(FROM_LAYER);
						fromLayer.addChild(finalFromNode);							
					}
				});
			}
			return fromNode;
		}
		/*
		private LWNode handleToNode(Tweet tweet)
		{
			if (tweet.getToUser() != null)
			{
				LWNode toNode = toUserClusterPoints.get(tweet.getToUser());				
				
				if (toNode == null)
				{
					toNode = new LWNode(tweet.getToUser());			
					toNode.setStyle(getClusterStyleNode());
					
					toUserClusterPoints.put(tweet.getToUser(), toNode);
					final LWNode finalToNode = toNode;
					javax.swing.SwingUtilities.invokeLater(new Runnable() {
						public void run() 
						{
							LWMap.Layer toLayer = layerList.get(TO_LAYER);
							toLayer.addChild(finalToNode);							
						}
					});
				}
				return toNode;
			}
			else
				return null;
			
		}
		*/
		private OpenCalaisAnalyzer oca = new OpenCalaisAnalyzer();
		
		private LWNode handleAnalysisNode(Tweet tweet)
		{
			String toAnalyze = tweet.getText();
			Multimap<String,AnalyzerResult> results = oca.analyzeString(toAnalyze);
			
			//Set<String> keyset = results.keySet();
			//for (String key: keyset)
			//{
			//	System.out.println("KEY : " + key);
			//}
			
			String topic = "Uncategorized";
			
			Collection<AnalyzerResult> res = results.get("Topic");
			for (AnalyzerResult r: res)
			{
				topic = r.getValue();
				break;
			}
			
			if (topic.equals("Uncategorized"))
			{
				Collection<AnalyzerResult> vals = results.values();
				double highRel = 0;
				
				for (AnalyzerResult value: vals)
				{
					if (value.getRelevance() > highRel);
					{
					 topic = value.getValue();
					 highRel = value.getRelevance();
					}					 					 
				}
			}
			
		
			LWNode topicNode = analysisClusterPoints.get(topic);
			
			
			if (topicNode == null)
			{
				topicNode = new LWNode(topic);	
				topicNode.setStyle(getClusterStyleNode());
				
				analysisClusterPoints.put(topic, topicNode);
				final LWNode finalTopicNode = topicNode;
				javax.swing.SwingUtilities.invokeLater(new Runnable() {
					public void run() 
					{
						LWMap.Layer analysisLayer = layerList.get(ANALYSIS_LAYER);
						analysisLayer.addChild(finalTopicNode);							
					}
				});
			}
			return topicNode;
			
		}
		
		public void run() {
		
			twitter = new Twitter();
			long lastTweet = 0;
			while (true)
			{
				try {
					Query q = new Query(mSearchFieldEditor.getText());
					if (lastTweet > 0)
						q.setSinceId(lastTweet);
					
					QueryResult results = twitter.search(q);
					lastTweet = results.getMaxId();
					List<Tweet> tweets = results.getTweets();
					Collections.reverse(tweets);
					for (Tweet tweet: tweets)
					{
						String msg = tweet.getFromUser() + " : " + tweet.getText();
						final LWNode node = new LWNode(tufts.Util.formatLines(msg,50));
						node.setStyle(getTweetStyleNode());
						MetadataList mlist = new MetadataList();
		            	mlist.add("sender", tweet.getFromUser());
		            	mlist.add("timestamp", tweet.getCreatedAt().toString());
		            	mlist.add("profileImage", tweet.getProfileImageUrl());

		            	if (tweet.getGeoLocation() !=null)
		            		mlist.add("geolocation", tweet.getGeoLocation().toString());

		            	if (msg.indexOf("?") > -1)
		            		mlist.add("Type of Sentence", "question");
		            	else if (msg.indexOf("!") > -1)
		            		mlist.add("Type of Sentence", "exclamation");
		            	else
		            		mlist.add("Type of Sentence", "statement");
		            	node.setMetadataList(mlist);
						
						LWNode fromNode = handleFromNode(tweet);
						//LWNode toNode = handleToNode(tweet);
						final LWNode analysisNode = handleAnalysisNode(tweet);
						
					
						
						final LWLink fromLink = new LWLink(fromNode,node);
						/*
						if (toNode !=null)
						{
							final LWLink toLink = new LWLink(node,toNode);
							final LWMap.Layer toLayer = layerList.get(TO_LAYER);
							javax.swing.SwingUtilities.invokeLater(new Runnable() {
								public void run() 
								{
									toLayer.addChild(toLink);
								}
							});
						}
						*/
					
							
							
						
						javax.swing.SwingUtilities.invokeLater(new Runnable() {
							public void run() 
							{
								final LWLink analysisLink = new LWLink(analysisNode,node);
								final LWMap.Layer analysisLayer = layerList.get(ANALYSIS_LAYER);
								LWMap.Layer tweetsLayer = layerList.get(TWEETS_LAYER);
								tweetsLayer.addChild(node);	
								
								LWMap.Layer fromLayer = layerList.get(FROM_LAYER);
								fromLayer.addChild(fromLink);
								layout(node);
								
								if (analysisNode !=null)
								{
									analysisLayer.addChild(analysisLink);
								}
							}
						});						
					}
					
					
				} catch (TwitterException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				try {   
		           Thread.sleep(UPDATE_INTERVAL);  
		        }  
		        catch (InterruptedException e ) { } 
			}
		} //end of run()
		

		 public LWComponent getClusterStyleNode()
		 {
		        final LWComponent style;

	            style = new LWNode(); // creates a rectangular node
	            style.setFillColor(ClusterNodeDataColors[NextColor]);
	            if (++NextColor >= ClusterNodeDataColors.length)
		                NextColor = 0;
	            style.setFont(ClusterNodeFont);
		        style.setTextColor(ClusterNodeTextColor);
		        
		        return style;
		 }


		private LWNode getTweetStyleNode()
		{
			final LWNode style = new LWNode();
			style.setFont(TweetNodeFont);
	        style.setTextColor(Color.black);
	        style.setFillColor(TweetNodeColor);
	        style.setStrokeWidth(TweetNodeStrokeWidth);
	        style.setStrokeColor(TweetNodeStrokeColor);
			return style;
		}
		
	   	public void layout(final LWComponent node)
	   	{
	   		JRadioButton button = getSelection(group);
	   		/*if (button.equals(toCheckbox))
	   		{
	   			javax.swing.SwingUtilities.invokeLater(new Runnable() {
					public void run() 
					{
						//LWMap.Layer geoLayer = layerList.get(GEO_LAYER);
					//	LWMap.Layer toLayer = layerList.get(TO_LAYER);
						LWMap.Layer fromLayer = layerList.get(FROM_LAYER);
						LWMap.Layer analysisLayer = layerList.get(ANALYSIS_LAYER);
						//geoLayer.setVisible(false);
						toLayer.setVisible(false);
						fromLayer.setVisible(false);
						analysisLayer.setVisible(true);
						
						
			   			LWMap.Layer tweetsLayer = layerList.get(TWEETS_LAYER);
			   			final LWSelection select = VUE.getSelection();
			   			select.clear();
			   			
			   			
			   			final List<LWComponent> l = new ArrayList<LWComponent>();
			   			Iterable<LWNode> nodeIterator = toLayer.getChildrenOfType(LWNode.class);
			   			for (LWNode c : nodeIterator)
			   			{
			   				
			   				l.add(c);
			   				//System.out.println(c.getLabel());
			   			}
			   			
			   			select.addAll(l);
			   			
			   			try {
							//tableLayout.layout(select);
							nodeIterator = toLayer.getChildrenOfType(LWNode.class);
				   			for (final LWNode c : nodeIterator)
				   			{
				   				SwingUtilities.invokeLater(new Runnable() { 
				   					public void run()				   				
				   					{
				   						//Actions.ClusterAction.clusterNodesAbout(c, c.getLinked());	
				   						
				   						clusterLayout.layout(select);
				   					}
				   				});
				   				
				   				
				   		
				   			}
							
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						SwingUtilities.invokeLater(new Runnable() { 
		   					public void run()				   				
		   					{
		   					//	ZoomTool.setZoomFit();
		   					}
						});
					}
	   			});
	   			
	   		}
	   		else */if (button.equals(fromCheckbox))
	   		{	   
	   			javax.swing.SwingUtilities.invokeLater(new Runnable() {
					public void run() 
					{
						//LWMap.Layer geoLayer = layerList.get(GEO_LAYER);
						//LWMap.Layer toLayer = layerList.get(TO_LAYER);
						LWMap.Layer fromLayer = layerList.get(FROM_LAYER);
						LWMap.Layer analysisLayer = layerList.get(ANALYSIS_LAYER);
						//geoLayer.setVisible(false);
						//toLayer.setVisible(false);
						fromLayer.setVisible(true);
						analysisLayer.setVisible(false);
						
						
			   			LWMap.Layer tweetsLayer = layerList.get(TWEETS_LAYER);
			   			final LWSelection select = VUE.getSelection();
			   			select.clear();
			   			
			   			
			   			final List<LWComponent> l = new ArrayList<LWComponent>();
			   			Iterable<LWNode> nodeIterator = fromLayer.getChildrenOfType(LWNode.class);
			   			for (LWNode c : nodeIterator)
			   			{
			   				
			   				l.add(c);
			   				//System.out.println(c.getLabel());
			   			}
			   			
			   			select.addAll(l);
			   			
			   			try {
							//tableLayout.layout(select);
							nodeIterator = fromLayer.getChildrenOfType(LWNode.class);
				   			for (final LWNode c : nodeIterator)
				   			{
				   				SwingUtilities.invokeLater(new Runnable() { 
				   					public void run()				   				
				   					{
				   						//Actions.ClusterAction.clusterNodesAbout(c, c.getLinked());	
				   						
				   						clusterLayout.layout(select);
				   					}
				   				});
				   				
				   				
				   		
				   			}
							
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						SwingUtilities.invokeLater(new Runnable() { 
		   					public void run()				   				
		   					{
		   					//	ZoomTool.setZoomFit();
		   					}
						});
					}
	   			});
	   			
	   		} else if (button.equals(analysisCheckbox))
	   		{
	   			javax.swing.SwingUtilities.invokeLater(new Runnable() {
					public void run() 
					{
						//LWMap.Layer geoLayer = layerList.get(GEO_LAYER);
						//LWMap.Layer toLayer = layerList.get(TO_LAYER);
						LWMap.Layer fromLayer = layerList.get(FROM_LAYER);
						LWMap.Layer analysisLayer = layerList.get(ANALYSIS_LAYER);
						//geoLayer.setVisible(false);
						//toLayer.setVisible(false);
						fromLayer.setVisible(false);
						analysisLayer.setVisible(true);
						
						
			   			LWMap.Layer tweetsLayer = layerList.get(TWEETS_LAYER);
			   			final LWSelection select = VUE.getSelection();
			   			select.clear();
			   			
			   			
			   			final List<LWComponent> l = new ArrayList<LWComponent>();
			   			Iterable<LWNode> nodeIterator = analysisLayer.getChildrenOfType(LWNode.class);
			   			for (LWNode c : nodeIterator)
			   			{
			   				
			   				l.add(c);
			   				//System.out.println(c.getLabel());
			   			}
			   			
			   			
			   				select.addAll(l);
			   		
			   			
			   			try {
							//tableLayout.layout(select);
							nodeIterator = analysisLayer.getChildrenOfType(LWNode.class);
				   			for (final LWNode c : nodeIterator)
				   			{
				   				SwingUtilities.invokeLater(new Runnable() { 
				   					public void run()				   				
				   					{
				   						//Actions.ClusterAction.clusterNodesAbout(c, c.getLinked());	
				   						
				   						clusterLayout.layout(select);
				   					}
				   				});
				   				
				   				
				   		
				   			}
							
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						SwingUtilities.invokeLater(new Runnable() { 
		   					public void run()				   				
		   					{
		   						ZoomTool.setZoomFit();
		   					}
						});
					}
	   			});
	   			
	   		}
	   		else
	   		{
	   			//scatter
	   			javax.swing.SwingUtilities.invokeLater(new Runnable() {
					public void run() 
					{
						//LWMap.Layer geoLayer = layerList.get(GEO_LAYER);
						//LWMap.Layer toLayer = layerList.get(TO_LAYER);
						LWMap.Layer fromLayer = layerList.get(FROM_LAYER);
						LWMap.Layer analysisLayer = layerList.get(ANALYSIS_LAYER);
						//geoLayer.setVisible(false);
						//toLayer.setVisible(false);
						fromLayer.setVisible(false);
						analysisLayer.setVisible(false);
						
			   			LWMap.Layer tweetsLayer = layerList.get(TWEETS_LAYER);
			   			LWSelection select = VUE.getSelection();
			   			select.clear();
//			   			select.addAll();
			   			
			   		//	if (node == null)
			   				select.addAll(tweetsLayer.getAllDescendents());
			   		//	else
			   		//		select.add(node);
			   			
			   			try {
							scrollTableLayout.layout(select);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
			   			SwingUtilities.invokeLater(new Runnable() { 
		   					public void run()				   				
		   					{
		   						VUE.getActiveViewer().repaint();
		   					//	ZoomTool.setZoomFit();
		   					}
						});
					}
	   			});
	   		}
	   			
	   		
	   	}
	   	
	   	public JRadioButton getSelection(ButtonGroup group) {
	   	    for (Enumeration e=group.getElements(); e.hasMoreElements(); ) {
	   	        JRadioButton b = (JRadioButton)e.nextElement();
	   	        if (b.getModel() == group.getSelection()) {
	   	            return b;
	   	        }
	   	    }
	   	    return null;
	   	}
	}
}
