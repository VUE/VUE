package edu.tufts.vue.mbs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;

import org.w3c.dom.*;
import org.xml.sax.SAXException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;

import com.google.common.collect.AbstractMapEntry;
import com.google.common.collect.Multimap;

import edu.tufts.vue.metadata.MetadataList;
import edu.tufts.vue.metadata.VueMetadataElement;

import tufts.vue.AnalyzerAction;
import tufts.vue.LWComponent;
import tufts.vue.MetaMap;

public class YahooAnalyzer implements LWComponentAnalyzer {
	private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(AnalyzerAction.class);
	private static final String ANALYZER_NAME = "Yahoo Term Extractor";
	
	public List analyze(LWComponent c)
	{
		return analyze(c,true);
	}
	
	public Multimap<String, AnalyzerResult> analyzeResource(LWComponent c)
	{
		return null;
	}
	public List<AnalyzerResult> analyze(LWComponent c, boolean fallback) {
		List<AnalyzerResult> results = new ArrayList<AnalyzerResult>();
		String request = "http://search.yahooapis.com/ContentAnalysisService/V1/termExtraction";
        
        HttpClient client = new HttpClient();
        PostMethod method = new PostMethod(request);
        
        method.addParameter("appid","sfeSlmvV34GVJdO3q6r7sECK4KVE0GIP3xVbWKtwu8Ta2gOCOSkAt1sC2FNl");
        MetadataList ml = c.getMetadataList();
        List<VueMetadataElement> elems = ml.getMetadata();
        Iterator<VueMetadataElement> i = elems.iterator();
        c.getNotes();
        String context = c.getLabel() + " " + c.getNotes() + " ";
        
        while (i.hasNext())
        {
        	VueMetadataElement e = i.next();
        	context += e.getValue() + " ";
        }
        
    	if (c.getResource() !=null)
    	{
    		MetaMap map = c.getResource().getProperties();
    		///c.getResource().get
    		if (map!=null)
    		{
    			Collection collection = map.entries();
    			
    		//	Iterator iterator = collection.iterator();
    			Object[] obj = collection.toArray();
    			for (int p = 0; p < obj.length; p++)
    			{
    				com.google.common.collect.AbstractMapEntry o = (AbstractMapEntry) obj[p];
    				
    			    
    			    if (o.getKey().equals("Title") || o.getKey().equals("Date") || o.getKey().equals("Creator") || o.getKey().equals("Description"))       				
    			    {
    			    	System.out.println(o.toString());
    			    	context += o.getValue() + ".  ";
    			    }
    			}
    		}
    	}
        
      //  method.addParameter("context","Manet was a painter at the second half of the 19th century");

        URLEncoder.encode(context);
        method.addParameter("context",context);
        // Send POST request
        int statusCode;
     //   BufferedReader br = null;
        InputStream rstream = null;
		try 
		{
			statusCode = client.executeMethod(method);
	
        
			if (statusCode != HttpStatus.SC_OK) {
				System.err.println("Method failed: " + method.getStatusLine());
			}
			
        
			// Get the response body
			rstream = method.getResponseBodyAsStream();
        
			 // Process response
	        Document response = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(rstream);
	        
	      
	      
	        
	        //Get all search Result nodes
	       // NodeList nodes = (NodeList)xPath.evaluate("/ResultSet/Result", response, XPathConstants.NODESET);
	        NodeList nodes = response.getElementsByTagName("Result");

	        int nodeCount = nodes.getLength();
	        System.out.println("Node Count : " + nodeCount);
	        //iterate over search Result nodes
	        for (int i1 = 0; i1 < nodeCount; i1++) {
	        //	String value = (String)xPath.evaluate("Result", nodes.item(i), XPathConstants.STRING);
	            System.out.println("Value: " + getTextValue(nodes.item(i1)));
	            System.out.println("--");
	           // results.add(new Property().getTextValue(nodes.item(i1)));
	            results.add(new AnalyzerResult("NA", getTextValue(nodes.item(i1))));
	        }

        
		} catch (HttpException e) {
			Log.error(e.getMessage());
		} catch (IOException e) {
			Log.error(e.getMessage());
		} catch (SAXException e) {
			Log.error(e.getMessage());
		} catch (ParserConfigurationException e) {
			Log.error(e.getMessage());
		} finally
		{
			if (rstream !=null)
				try {
					rstream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
		return results;
	}
	 private String getTextValue(Node node) {
		    if (node.hasChildNodes()) {
		      return node.getFirstChild().getNodeValue();
		    } else {
		      return "";
		    }
		  }


	
	public static void main(String[] args)
	{
		YahooAnalyzer ya = new YahooAnalyzer();
		ya.analyze(null);
	}
	public String getAnalyzerName() {

		return ANALYZER_NAME;
	}

}
