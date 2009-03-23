package edu.tufts.vue.mbs;

import java.io.IOException;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;
import com.clearforest.calais.common.CalaisJavaIf;
import com.clearforest.calais.common.Property;
import com.clearforest.calais.common.StringUtils;
import com.clearforest.calais.simple.Entity;
import com.google.common.collect.AbstractMapEntry;

import edu.tufts.vue.metadata.MetadataList;
import edu.tufts.vue.metadata.VueMetadataElement;
import tufts.vue.AnalyzerAction;
import tufts.vue.LWComponent;
import tufts.vue.MetaMap;
import tufts.vue.VueResources;

public class OpenCalaisAnalyzer implements LWComponentAnalyzer, ErrorHandler{

	private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(AnalyzerAction.class);
	private boolean		 			m_isLastErr		= false;
	private String		 			m_lastErr		= null;
	private int						m_level			= 0;
	private String					m_l0Tag			= "";
	private String					m_l1Tag			= "";
	private String					m_l2Tag			= "";
	private String					m_l3Tag			= "";
	private ArrayList<Property>		m_infoMap		= null;
	private ArrayList<Entity>		m_entities		= null;
	private Entity					m_currentEntity	= null;
	
	public List analyze(LWComponent c)
	{
		return analyze(c,true);
	}
	
	public List analyze(LWComponent c, boolean fallback) {
		List<AnalyzerResult> results = new ArrayList<AnalyzerResult>();
		CalaisJavaIf calais = new CalaisJavaIf("xqffs8ggkmebrsehdsbt56j8");

		calais.setOutputFormat("text/simple");
		calais.setCalaisURL(VueResources.getString("calaisUrl"));
		calais.setVerifyCert(false);
		
		String context = null;
		if (c !=null)
		{
			MetadataList ml = c.getMetadataList();
        	List<VueMetadataElement> elems = ml.getMetadata();
        	Iterator i = elems.iterator();
        	c.getNotes();
        	context = c.getLabel().trim() + ".  ";
        	
        	if (c.getNotes() != null)
        		context += c.getNotes().trim() + ".  ";
        
        	while (i.hasNext())
        	{
        		VueMetadataElement e = (VueMetadataElement)i.next();
        		context += e.getValue() + ".  ";
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
        				System.out.println(o.getKey());
        			    String key = o.getKey().toString().toLowerCase();
        			    if (key.startsWith("title") || key.startsWith("date") 
        			    		|| key.startsWith("creator")|| key.startsWith("description"))       				
        			    {
        			    	System.out.println(o.toString());
        			    	context += "The " +	o.getKey().toString().trim() + " is " + o.getValue().toString().trim() +".  ";
        			    }
        			}
        		}
        	}
		}
		else
		{
			context = "Eduardo Manet the 19th century French painter.";
		}
      //  method.addParameter("context","Manet was a painter at the second half of the 19th century");
		System.out.println("Context : " + context);
        URLEncoder.encode(context);
    	String 			resp_simple = null;
        resp_simple = calais.callEnlighten(context);

    	
		/*
		 * Analyze response errors
		 */
		if (calais.isLastErr())
		{
			Log.error("Analyzer Response Error");
		}

		if (resp_simple.indexOf("Enlighten ERROR:") != -1)
		{
			Log.error("Analyzer Response Error");
		}

		if (resp_simple.indexOf("<Exception>") != -1)
		{
			Log.error("Analyzer Response Error");
		}

		if (resp_simple.indexOf("<h1>403 Developer Inactive</h1>") != -1)
		{
			Log.error("Analyzer Response Error");
		}

		/*
		 * Response is valid - parse XML
		 */
		try
		{
			m_level = 0;
			m_l0Tag = "";
			m_l1Tag = "";
			m_l2Tag = "";
			m_l3Tag = "";
			m_infoMap = new ArrayList<Property>();
			m_entities = new ArrayList<Entity>();
			
			XMLReader reader = XMLReaderFactory.createXMLReader();
			reader.setContentHandler(new ContentHandler());
			reader.setErrorHandler(this);
			resp_simple = StringUtils.unescapeHTML(resp_simple);
			reader.parse(new InputSource(new StringReader(resp_simple)));
		}
		catch(IOException e)
		{
			e.printStackTrace();
			Log.error(e.getMessage());
		}
		catch (SAXException e)
		{
			e.printStackTrace();
			Log.error(e.getMessage());
		}
		
		if (m_isLastErr)
		{
			Log.error(m_isLastErr);
		}
		//System.out.println("Results :"+ resp_simple);
		Iterator<Entity> it = m_entities.iterator();
		while (it.hasNext())
		{
			Entity prop = it.next();
		//	System.out.println("Name : " + prop.getName());
		//	System.out.println("Count : " + prop.getType());
			results.add(new AnalyzerResult(prop.getType(),prop.getName()));
		}
		
		if (results.isEmpty() && fallback)
			return new YahooAnalyzer().analyze(c);
		else
			return results;
     }
	
	
	/**
	 * Parse error handler functions
	 */
	public void warning(SAXParseException e)
	{
		m_isLastErr = true;
		m_lastErr = "Failed to parse response: " + e.getMessage();
	}
	
	public void error(SAXParseException e) 
	{
		m_isLastErr = true;
		m_lastErr = "Failed to parse response: " + e.getMessage();
	}
	
	public void fatalError(SAXParseException e)
	{
		m_isLastErr = true;
		m_lastErr = "Failed to parse response: " + e.getMessage();
	}
	
	private String err(String err)
	{
		m_isLastErr = true;
		m_lastErr = err;
		return m_lastErr;
	}
	
	 public String getAnalyzerName() {
		return "Open Calais Analyzer";
	}

	/*
	 * XML parsing of Simple Output Format
	 */
	public class ContentHandler extends DefaultHandler {

		public void startElement(
			String 		namespaceURI,
			String 		localName,
			String 		qName,
			Attributes 	attributes)
		{
			String 	countStr 	= null;
			
			if (m_isLastErr)
			{
				return;
			}

			if (m_level == 0)
			{
				/*
				 * Level 0 - the string tag
				 */
				if (!qName.equals("string"))
				{
					err("Failed to parse Simple Format - root tag is not string - " + qName);
					return;		
				}
				
				m_l0Tag = qName;
				
			}
			else if (m_level == 1)
			{
				/*
				 * Level 1 - the OpenCalaisSimple tag
				 */
				if (!qName.equals("OpenCalaisSimple"))
				{
					err("Failed to parse Simple Format - below root tag is not OpenCalaisSimple - " + qName);
					return;		
				}
				
				m_l1Tag = qName;
			}
			else if (m_level == 2)
			{
				/*
				 * Level 2 - Description or CalaisSimpleOutputFormat
				 */
				if (!qName.equals("Description") && 
					!qName.equals("CalaisSimpleOutputFormat"))
				{
					err("Failed to parse Simple Format - level 2 tag is not Description or CalaisSimpleOutputFormat - " + qName);
					return;		
				}
				
				m_l2Tag = qName;
				
			}
			else if (m_level > 2)
			{
				/*
				 * Level 3+ - Description information or semantic data
				 */
				if (m_l2Tag.equals("Description"))
				{
					/*
					 * Information under the Description element - place
					 * in info array (done in characters)
					 */
					m_l3Tag = qName;
				}
				else if (m_l2Tag.equals("CalaisSimpleOutputFormat"))
				{
					/*
					 * Information under the CalaisSimpleOutputFormat - start
					 * an entities array element
					 */
					m_currentEntity = new Entity();
					m_currentEntity.setType(qName);
	
					countStr = attributes.getValue("count");
					if (countStr != null)
					{
						m_currentEntity.setCount(
								Integer.parseInt(countStr));
					}
				}
			}
			
			m_level++;
	
		}

		public void characters(
			char[] ch,
			int start,
			int length)
		{
		
			if (m_isLastErr)
			{
				return;
			}  	
			
			String data = new String(ch,start,length);
	
			/*
			 * Data within <tag> </tag> - set the name of the new element
			 */
			if (m_l2Tag.equals("CalaisSimpleOutputFormat") && m_level == 4)
			{
				m_currentEntity.setName(data.trim());
			}
	
			/*
			 * Data within <tag> </tag> for Description sub-elements - place
			 * in info array
			 */
			if (m_l2Tag.equals("Description") && m_level == 4)
			{
				m_infoMap.add(new Property(m_l3Tag, data.trim()));
			}
		
		}

		public void endElement(
			String namespaceURI,
			String localName,
			String qName)
		{
			
			if (m_isLastErr)
			{
				return;
			}
			
			if (m_level == 0)
			{
				err("Failed to parse Simple Format - internal error");
				return;		
			}
			
			if (m_level == 1)
			{
				/*
				 * Level 0 - closing the string tag 
				 */
				if (!qName.equals("string"))
				{
					err("Failed to parse Simple Format - root closing tag is not string - " + qName);
					return;		
				}
				
				m_l0Tag = "";
			}
			else if (m_level == 2)
			{
				/*
				 * Level 1 - closing the OpenCalaisSimple tag
				 */
				if (!qName.equals("OpenCalaisSimple"))
				{
					err("Failed to parse Simple Format - below root closing tag is not OpenCalaisSimple - " + qName);
					return;		
				}
				
				m_l1Tag = "";
			}
			else if (m_level == 3)
			{
				/*
				 * Level 2 - closing Description or CalaisSimpleOutputFormat
				 */
				if (!qName.equals("Description") && 
					!qName.equals("CalaisSimpleOutputFormat"))
				{
					err("Failed to parse Simple Format - level 2 closing tag is not Description or CalaisSimpleOutputFormat - " + qName);
					return;		
				}
				
				m_l2Tag = "";
			}
			else if (m_level == 4)
			{
				/*
				 * Level 3 - closing tags (maybe under description) - reset
				 * l3 tag
				 */
				m_l3Tag = "";

				/*
				 * add the new element to the array
				 */
				if (m_l2Tag.equals("CalaisSimpleOutputFormat"))
				{
					m_entities.add(m_currentEntity);
					m_currentEntity = null;
				}
			}
			else if (m_level > 4)
			{
				/*
				 * Level 4+ - no-op
				 */
			}

			m_level--;
			
		}
		
	}	
	
	public static void main(String[] args)
	{
		OpenCalaisAnalyzer oca = new OpenCalaisAnalyzer();
		oca.analyze(null);
	}
}
