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
package tufts.vue;

import java.awt.*;
import java.applet.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Multiset;
import com.jgoodies.looks.LookUtils;
import com.jgoodies.looks.plastic.Plastic3DLookAndFeel;
import com.sun.org.apache.xerces.internal.impl.XMLScanner;

import edu.tufts.vue.mbs.AnalyzerResult;

import tufts.vue.ds.DataAction;
import tufts.vue.ds.DataTree;
import tufts.vue.ds.Schema;
import tufts.vue.ds.XMLIngest;
import tufts.vue.ds.XmlDataSource;
import tufts.vue.gui.GUI;
import tufts.vue.gui.VueMenuBar;

// To get to run from classpath: had to be sure to create a symlink to
// VueResources.properties
// from the build/classes tree to the source tree.

// Of course, now need all the damn support libraries...

/**
 * Experimental VUE applet.
 * 
 * @version $Revision: 1.19 $ / $Date: 2009-08-11 15:07:05 $ / $Author: mike $
 */
public class VueApplet extends JApplet {

	private static boolean firstInit = true;
	private static MapViewer viewer = null;
	private static JPanel toolbarPanel = null;
	private static JComponent toolbar = null;
	private static boolean fullyStopped = false;
	private static boolean isZotero = false;
	// If want to have viewer left in same state when go forwrd/back in browser,
	// will need javacript assist to associate the viewer with the instance of a
	// web browser page:
	// new applet & context objects are created even when you go forward/back
	// pages.	
	private static MapTabbedPane mMapTabbedPane = null;

	// applet parameters
	/*
	 * Valid values for zoteroPlugin : true , false default value: false
	 */
	private static final String zoteroPlugin = "zoteroPlugin";
	
	private static JApplet instance;
	
	public VueApplet()
	{
		instance = this;
	//	VUE.setAppletContext(this.getAppletContext());
	}
	public void init() 
	{
		super.init();
		this.setBackground(new Color(225,225,225));

		   try {
    		   UIManager.put("ClassLoader", LookUtils.class.getClassLoader());
    		      UIManager.setLookAndFeel(new Plastic3DLookAndFeel());
    		   } catch (Exception e) 
    		   {
    			   System.out.println("Couldn't load jlooks look and feel");
    		   }

		//I think the redone content window makes this code moot.
		/*
		GUI.invokeAfterAWT(new Runnable() { public void run() {
	            
	            if (VUE.getDRBrowser() != null)
	            	VUE.getDRBrowser().loadDataSourceViewer();
	            
	            // Kick-off tufts.vue.VueDataSource viewer build threads:
	            // must be done in AWT to be threadsafe, as involves
	            // non-synhcronized code in tufts.vue.VueDataSource while
	            // setting up the threads

	            // DataSourceViewer.cacheDataSourceViewers();
	            
	    }});		*/
		
	}
	
	public static String getActiveMapPath()
	{
		return VUE.getActiveMap().getFile().getAbsolutePath();
	}
	
	public static String getActiveMapDisplayTitle()
	{
		return VUE.getActiveMap().getDisplayLabel();
	}
	
	public static JApplet getInstance()
	{
		return instance;
	}
	
	public void start() 
	{	
		super.start();
		instance = this;
		VUE.setAppletContext(this.getAppletContext());
		if (!firstInit)
		while(!fullyStopped) 
		{
			try {
			   Thread.sleep(3000);
		       System.out.println("WAIT");
		    } catch (InterruptedException e) {
		            //keep trying
		    }
		}
		
		fullyStopped = false;
		super.init();	
	
		
		processAppletParameters();
		getContentPane().setLayout(new BorderLayout());
		
		msg("init\n\tapplet=" + Integer.toHexString(hashCode())
				+ "\n\tcontext=" + getAppletContext());
		VUE.initUI();		
		VUE.initApplication();
		
		loadViewer();
		
		if (!VUE.getLeftTabbedPane().isEnabled())
	       	VUE.getLeftTabbedPane().setEnabled(true);
		
		
		
	     //-------------------------------------------------------
        // Trigger the load of the OSID's and set up UrlAuthentication
        //-------------------------------------------------------      
        VUE.initDataSources();
		GUI.invokeAfterAWT(new Runnable() { public void run() {
			final DRBrowser DR_BROWSER = VUE.getDRBrowser();    
            if (DR_BROWSER != null)
                DR_BROWSER.loadDataSourceViewer();
            
            // Kick-off tufts.vue.VueDataSource viewer build threads:
            // must be done in AWT to be threadsafe, as involves
            // non-synhcronized code in tufts.vue.VueDataSource while
            // setting up the threads

            // DataSourceViewer.cacheDataSourceViewers();
          // addZoteroDatasource("collectionName", "/Users/mkorcy01/Desktop/6.xml");
         
        }});
	}
	
	public void ToggleAllVisible() {
		tufts.vue.gui.DockWindow.ToggleAllVisible();
	}
	
	public boolean AllWindowsHidden()
	{
		return tufts.vue.gui.DockWindow.AllWindowsHidden();
	}
	public void ShowPreviouslyHiddenWindows()
	{
		tufts.vue.gui.DockWindow.ShowPreviouslyHiddenWindows();
	}
	public void HideAllDockWindows()
	{
		tufts.vue.gui.DockWindow.HideAllWindows();
	}
	
	public void stop() 
	{
		tufts.vue.gui.DockWindow.HideAllWindows();
		VUE.finalizeDocks();
		System.gc();
		fullyStopped = true;
		msg("stop");
	}
	
	public void destroy() {
		super.destroy();
		msg("destroy");
	}		

	public static boolean isZoteroApplet()
	{
		return isZotero;
	}
	private final void processAppletParameters() {
		String zoteroPlugin = this.getParameter(this.zoteroPlugin);
		if (zoteroPlugin != null)
			zoteroPlugin = zoteroPlugin.toLowerCase();
		if (zoteroPlugin != null && zoteroPlugin.equals("true")) {
			isZotero=true;
		//	tufts.vue.action.TextOpenAction.setZoteroPrototypeEnabled(true);
		}
	}		

	public static String getActiveMapItems()
    {
        LWMap active = VUE.getActiveMap();
        java.util.Iterator it = active.getAllDescendents(LWComponent.ChildKind.PROPER).iterator();
        String items = "";
        do
        {
            if(!it.hasNext())
                break;
            LWComponent n = (LWComponent)it.next();
            if(n instanceof LWNode)
            {
                items = (new StringBuilder()).append(items).append(n.getLabel()).append(",").toString();
                String id = null;
                try
                {
                    id = n.getResource().getProperty("Zotero.id");
                }
                catch(Exception e)
                {
                    System.out.println("Exception in zotero import from vue: no zotero id");
                }
                if(id == null)
                    items = (new StringBuilder()).append(items).append("none,").toString();
                else
                    items = (new StringBuilder()).append(items).append(id).append(",").toString();
                String url = null;
                try
                {
                    url = n.getResource().getSpec();
                }
                catch(Exception e)
                {
                    System.out.println("Exception in zotero import from vue: no zotero url");
                }
                if(url == null)
                    items = (new StringBuilder()).append(items).append("none").toString();
                else
                    items = (new StringBuilder()).append(items).append(url).toString();
                items = (new StringBuilder()).append(items).append("\n").toString();
            }
        } while(true);
        return items;
    }

	public synchronized void loadViewer() {
	
		msg("got viewer");
		msg("is applet ? " + VUE.isApplet());

		msg("setting menu bar...");
		VueMenuBar vmb = new VueMenuBar();
		vmb.setBorderPainted(false);
		setJMenuBar(vmb);

		final VueToolbarController tbc = VueToolbarController.getController();
		toolbar = tbc.getToolbar().getMainToolbar();

		toolbarPanel = VUE.constructToolPanel(toolbar);

		getContentPane().setBackground(toolbarPanel.getBackground());
		getContentPane().add(toolbarPanel, BorderLayout.NORTH);

		getContentPane().add(VUE.getLeftTabbedPane(),BorderLayout.CENTER);	
		validate();
		
		VUE.displayMap(new LWMap("New Map"));
	
		msg("validating...");
		msg("loading complete");
        
	//	VUE.getSelection().clearAndNotify();

		//VUE.setActive(MapViewer.class, this, null); // no open viewers
        firstInit=false;      
       
	}

	public void setSize(int width, int height)
	{
		super.setSize(width, height);
		this.getContentPane().setSize(width, height);
		Container c = this.getParent();
		while (c !=null)
		{
			c.setSize(width, height);
			c.validate();
			c = c.getParent();
		}
		this.invalidate();
		this.getContentPane().validate();
		this.validate();
		
		this.repaint();
	}
	private void msg(String s) {
		System.out.println("VueApplet: " + s);
	}

	@SuppressWarnings("unchecked")
	public static void displayZoteroExport(final String urlString) {
		AccessController.doPrivileged(new PrivilegedAction() {

			public Object run() {
				try {
					File tempFile = null;
					tempFile = File.createTempFile(new Long(System
							.currentTimeMillis()).toString(), null);
					InputStream io = null;

					io = getInputStream(urlString);
					byte[] buf = new byte[256];
					int read = 0;
					java.io.FileOutputStream fos = null;
					fos = new java.io.FileOutputStream(tempFile);

					while ((read = io.read(buf)) > 0) {
						fos.write(buf, 0, read);
					}
					tufts.vue.action.TextOpenAction.displayMap(tempFile);

				} catch (Exception e) {
					e.printStackTrace();
				}

				return null;
			}

		});
	}
	
	public static String getActiveResourceSpec()
	{
		LWSelection selection = VUE.getActiveViewer().getSelection();
		if (!selection.isEmpty())
		{
			return selection.get(0).getResource().getSpec();
		}
		else
			return null;
	}
	@SuppressWarnings("unchecked")
	public static void addLinksToMap(final String content)
	{
		AccessController.doPrivileged(new PrivilegedAction() {

			public Object run() {
	    //        Reader reader = openReader();
	    javax.xml.parsers.DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringElementContentWhitespace(true);
        factory.setIgnoringComments(true);
        //factory.setCoalescing(true);
        factory.setValidating(false);
        // We don't use is.setEncoding(), as openReader will already have handled that
        //      is.setCharacterStream(reader);
        InputStream is;
		try {
			is = new java.io.ByteArrayInputStream(content.getBytes("UTF-8"));
			final org.w3c.dom.Document doc = factory.newDocumentBuilder().parse((InputStream) is);
			NodeList nodeLst = doc.getElementsByTagName("link");
			
			//build multimap of links
			Multimap<String,String> map = Multimaps.newArrayListMultimap();
			  for (int s = 0; s < nodeLst.getLength(); s++) {

			    Node fstNode = nodeLst.item(s);
			    
			    if (fstNode.getNodeType() == Node.ELEMENT_NODE) 
			    {
			    	NamedNodeMap atts = fstNode.getAttributes();
	            
	               Node n = atts.item(0);
	               Node val = atts.item(1);
	               
	               map.put(n.getNodeValue(), val.getNodeValue());
	            }

			  }
			  
			  
			  java.util.Collection<LWComponent> comps = VUE.getActiveMap().getAllDescendents();
			  java.util.Iterator<LWComponent> iter = comps.iterator();
			  //build map of data row nodes
			  HashMap<String,LWNode> dataRowNodes = new HashMap<String,LWNode>();
			  while (iter.hasNext())
			  {
				 LWComponent comp = iter.next();
				 if (comp.isDataRowNode())
				 {
					String fromId= comp.getDataValue("id");
					dataRowNodes.put(fromId, (LWNode)comp);										
				 }
			  }
			  
			  //draw links
			  Multiset<String> keys = map.keys();
			  java.util.Iterator<String> linkIterator = keys.iterator();
			  
			  while (linkIterator.hasNext())
			  {
				  String fromId = linkIterator.next();
				  
				  Collection<String> toIds = map.get(fromId);
				  java.util.Iterator<String> toIdIterator = toIds.iterator();
				  while (toIdIterator.hasNext())
				  {
					  String tid = toIdIterator.next();
					  LWNode fromNode = dataRowNodes.get(fromId);
					  LWNode toNode = dataRowNodes.get(tid);
					  
					  if (fromNode !=null && toNode !=null)
					  {
						LWLink link = new LWLink(fromNode,toNode);
						VUE.getActiveMap().add(link);
					  }
				  }
			  }
			  
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
			};
		});
		
	}
	
	@SuppressWarnings("unchecked")
	public static void addNotesToMap(final String content)
	{
		AccessController.doPrivileged(new PrivilegedAction() {

			public Object run() {
	    //        Reader reader = openReader();
	    javax.xml.parsers.DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringElementContentWhitespace(true);
        factory.setIgnoringComments(true);
        //factory.setCoalescing(true);
        factory.setValidating(false);
        // We don't use is.setEncoding(), as openReader will already have handled that
        //      is.setCharacterStream(reader);
        InputStream is;
		try {
			is = new java.io.ByteArrayInputStream(content.getBytes("UTF-8"));
			final org.w3c.dom.Document doc = factory.newDocumentBuilder().parse((InputStream) is);
			NodeList nodeLst = doc.getElementsByTagName("note");
			
			//build multimap of links
			HashMap<String,String> map = new HashMap<String,String>();
			  for (int s = 0; s < nodeLst.getLength(); s++) 
			  {

			    Node fstNode = nodeLst.item(s);
			    
			    if (fstNode.getNodeType() == Node.ELEMENT_NODE) 
			    {
			    	NamedNodeMap atts = fstNode.getAttributes();	            
	                Node n = atts.item(0);

	                NodeList noteList = fstNode.getChildNodes();

                           

	              
	                map.put(n.getNodeValue(), ((Node)noteList.item(0)).getNodeValue().trim());
	               // System.out.println("puts : " + n.getNodeValue() +"," +fstNode.getNodeValue());
	            }

			  }
			  
			  
			  java.util.Collection<LWComponent> comps = VUE.getActiveMap().getAllDescendents();
			  java.util.Iterator<LWComponent> iter = comps.iterator();
			  //build map of data row nodes
			  HashMap<String,LWNode> dataRowNodes = new HashMap<String,LWNode>();
			  while (iter.hasNext())
			  {
				 LWComponent comp = iter.next();
				 if (comp.isDataRowNode())
				 {
					String fromId= comp.getDataValue("id");
					dataRowNodes.put(fromId, (LWNode)comp);										
				 }
			  }
			  
			  //draw links
			  java.util.Set<String> keys = map.keySet();
			  java.util.Iterator<String> notesIterator = keys.iterator();
			  
			  while (notesIterator.hasNext())
			  {
				  String id = notesIterator.next();
					 
					  LWNode fromNode = dataRowNodes.get(id);
					  if (fromNode !=null)
					  {
						  String note = map.get(id);
						  fromNode.setNotes(note);
					  }
				  
			  }
			  
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
			};
		});
		
	}
	
	public static String getActiveResourceTitle()
	{
		LWSelection selection = VUE.getActiveViewer().getSelection();
		if (!selection.isEmpty())
		{
			return selection.get(0).getResource().getTitle();
		}
		else
			return null;
	}
	@SuppressWarnings("unchecked")
	public static void addZoteroDatasource(final String collectionName, final String fileString, final boolean addToMap) {
		AccessController.doPrivileged(new PrivilegedAction() {

			public Object run() {
				try 
				{
					DataSourceList l = DataSetViewer.getDataSetList();
					int length = l.getModel().getSize();
					for (int i = 0; i<length; i++)
					{
						Object o = l.getModel().getElementAt(i);
						if (o instanceof XmlDataSource) 
						{
							XmlDataSource xmlds = (XmlDataSource)o;
							if (xmlds.getDisplayName().equals(collectionName) && xmlds.getAddress().equals(fileString))
							{
								//we already have it in the list refresh it and move on...
								VUE.getContentDock().setVisible(true);
								VUE.getContentPanel().showDatasetsTab();
								VUE.getContentPanel().getDSBrowser().getDataSetViewer().setActiveDataSource(xmlds);
								VUE.getContentPanel().getDSBrowser().getDataSetViewer().refreshBrowser();
								
								if (addToMap)
								{
									
									boolean added = false;
									int tries = 0;
									
									while (!added && tries < 10)
									{
										try
										{
									   
											added = true;
									
											  DataAction.annotateForMap(xmlds.getSchema(), VUE.getActiveMap());
											List<LWComponent> nodes =  DataAction.makeRowNodes(xmlds.getSchema());

											//List<tufts.vue.DataRow> nodes = xmlds.getSchema().getRows();
									        LWMap map = VUE.getActiveMap();
									     
									        for(LWComponent component: nodes) {
									            map.add(component);
									        }
									       LayoutAction.random.act(new LWSelection(nodes));
										}
										catch(NullPointerException npe)
										{
											System.out.println(npe.toString());
											added = false;
											tries++;
											Thread.sleep(1000);
										}
									}
								}

								return null;
							}
						}
					}
				
					String xml;
					tufts.vue.DataSource ds = new XmlDataSource(collectionName, fileString);	
					Properties props = new Properties();
					props.put("displayName", collectionName);
					props.put("name", collectionName);
					props.put("address", fileString);
					props.put("item_path", "zoteroCollection.zoteroItem");
					props.put("key_field", "zoteroCollection.zoteroItem.id");
					ds.setConfiguration(props);
					final BrowseDataSource		bds = (BrowseDataSource)ds;
					VUE.getContentDock().setVisible(true);
					VUE.getContentPanel().showDatasetsTab();
					DataSetViewer.getDataSetList().addOrdered(ds);
					VUE.getContentPanel().getDSBrowser().getDataSetViewer().setActiveDataSource(ds);									       
					DataSourceViewer.saveDataSourceViewer();			    			    																			
					if (addToMap)
					{
						XmlDataSource xmlds = (XmlDataSource)ds;
						boolean added = false;
						int tries = 0;
						
						while (!added && tries < 10)
						{
							try
							{
						       // DataTree.this.addNewRowsToMap();
								added = true;
								
								List<LWComponent> nodes =  DataAction.makeRowNodes(xmlds.getSchema());
						        LWMap map = VUE.getActiveMap();
						     
						        for(LWComponent component: nodes) {
						            map.add(component);
						        }
						       LayoutAction.random.act(new LWSelection(nodes));
							}
							catch(NullPointerException npe)
							{
								added = false;
								tries++;
								Thread.sleep(1000);
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}											
				return null;		
			}

		});
		
		
	}

	public static void displayMap(String urlString) {
		innerDisplayMap(urlString);
	}

	public static void displayLocalMap(String fileString)
	{
		innerDisplayLocalMap(fileString);
	}
	@SuppressWarnings("unchecked")
	private static void innerDisplayMap(final String urlString) {

		AccessController.doPrivileged(new PrivilegedAction() {

			public Object run() {
				/*
				 * try { File tempFile = null; tempFile =
				 * File.createTempFile(new
				 * Long(System.currentTimeMillis()).toString(), null);
				 * InputStream io = null;
				 * 
				 * io = getInputStream(urlString); byte[] buf = new byte[256];
				 * int read = 0; java.io.FileOutputStream fos = null; fos = new
				 * java.io.FileOutputStream(tempFile);
				 * 
				 * while ((read = io.read(buf)) > 0) { fos.write(buf, 0, read); }
				 */
				URL url;
				try {
					url = new URL(urlString);
					tufts.vue.action.OpenURLAction.displayMap(url);
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				/*
				 * tufts.vue.action.OpenAction.displayMap(tempFile);
				 *  } catch(Exception e) { e.printStackTrace(); }
				 */
				return null;
			}

		});

	}
	
	@SuppressWarnings("unchecked")
	private static void innerDisplayLocalMap(final String fileString) {

		AccessController.doPrivileged(new PrivilegedAction() {

			public Object run() {
				
					tufts.vue.action.OpenAction.displayMap(new File(fileString));
					return null;
			}

		});

	}

	public static InputStream getInputStream(String fileName)
			throws IOException {
		InputStream input;

		if (fileName.startsWith("http:")) {
			URL url = new URL(fileName);
			URLConnection connection = url.openConnection();
			input = connection.getInputStream();
		} else {
			input = new FileInputStream(fileName);
		}

		return input;
	}

	protected static MapViewer getMapViewer() {
		return viewer;
	}

	protected static MapTabbedPane getMapTabbedPane() {
		return mMapTabbedPane;
	}

}