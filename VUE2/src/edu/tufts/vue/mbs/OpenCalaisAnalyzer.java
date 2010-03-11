package edu.tufts.vue.mbs;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
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
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import edu.tufts.vue.metadata.MetadataList;
import edu.tufts.vue.metadata.VueMetadataElement;
import tufts.vue.AnalyzerAction;
import tufts.vue.LWComponent;
import tufts.vue.LWNode;
import tufts.vue.MetaMap;
import tufts.vue.Resource;
import tufts.vue.VUE;
import tufts.vue.VueResources;

public class OpenCalaisAnalyzer implements LWComponentAnalyzer {

	private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger
			.getLogger(AnalyzerAction.class);
	private ArrayList<CalaisEntity> m_entities = new ArrayList<CalaisEntity>();

	public List analyze(LWComponent c) {
		return analyze(c, true);
	}

	private String downloadURL(String theURL) throws IOException {
		URL u;
		InputStream is = null;
		DataInputStream dis;
		String s;
		StringBuffer sb = new StringBuffer();

		try {
			u = new URL(theURL);
			is = u.openStream();
			dis = new DataInputStream(new BufferedInputStream(is));
			while ((s = dis.readLine()) != null)
				sb.append(s + "\n");
		} finally {
			try {
				if (is != null)
					is.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
		return sb.toString();
	}

	public Multimap analyzeResource(LWComponent c) throws IOException {
		m_entities = new ArrayList<CalaisEntity>();
		Multimap<String, AnalyzerResult> results = Multimaps
				.newArrayListMultimap();
		String resp_simple = null;
		String context = null;
		if (c != null) {
			Resource r = c.getResource();
			String spec = r.getSpec();
			if (spec.startsWith("http") || spec.startsWith("https")) {
				resp_simple = downloadURL("http://service.semanticproxy.com/processurl/xqffs8ggkmebrsehdsbt56j8/simple/"
						+ spec);
			}
		} else {
			context = "Eduardo Manet the 19th century French painter.";
		}

		javax.xml.parsers.DocumentBuilderFactory factory = DocumentBuilderFactory
				.newInstance();
		factory.setIgnoringElementContentWhitespace(true);
		factory.setIgnoringComments(true);
		factory.setValidating(false);

		InputStream is = null;
		org.w3c.dom.Document doc = null;

		try {
			is = new java.io.ByteArrayInputStream(resp_simple.getBytes("UTF-8"));
			doc = factory.newDocumentBuilder().parse((InputStream) is);
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
		NodeList nodeLst = doc.getElementsByTagName("CalaisSimpleOutputFormat");

		for (int s = 0; s < nodeLst.getLength(); s++) {

			Node fstNode = nodeLst.item(s);
			visit(fstNode, 0);
		}

		Iterator<CalaisEntity> it = m_entities.iterator();
		while (it.hasNext()) {
			CalaisEntity prop = it.next();
			results.put(prop.getType(), new AnalyzerResult(prop.getType(), prop.getName(), prop.getRelevance(), prop.getCount()));
		}

		return results;
	}
	public CalaisJavaIf prepCalais()
	{
		CalaisJavaIf calais = new CalaisJavaIf("xqffs8ggkmebrsehdsbt56j8");
		calais.setOutputFormat("text/simple");
		calais.setCalaisURL("http://api.opencalais.com/enlighten/rest");
		//calais.setCalaisURL("http://api.opencalais.com/enlighten/calais.asmx/Enlighten");
		calais.setVerifyCert(false);
		
		return calais;		
	}
	
	public Multimap analyzeString(String tweet)
	{
		m_entities = new ArrayList<CalaisEntity>();
		Multimap<String, AnalyzerResult> results = Multimaps
				.newArrayListMultimap();

	//	String resp_simple = downloadURL("http://service.semanticproxy.com/processurl/xqffs8ggkmebrsehdsbt56j8/simple/"
		//			+ spec);
		//System.out.println("TWEET:"+ tweet);
		//URLEncoder.encode(tweet);
		String resp_simple = null;
		CalaisJavaIf calais = prepCalais();
		resp_simple = calais.callEnlighten(tweet);

		resp_simple = StringUtils.unescapeHTML(resp_simple);
		System.out.println(resp_simple);
		javax.xml.parsers.DocumentBuilderFactory factory = DocumentBuilderFactory
				.newInstance();
		factory.setIgnoringElementContentWhitespace(true);
		factory.setIgnoringComments(true);
		factory.setValidating(false);

		InputStream is = null;
		org.w3c.dom.Document doc = null;

		try {
			is = new java.io.ByteArrayInputStream(resp_simple.getBytes("UTF-8"));
			doc = factory.newDocumentBuilder().parse((InputStream) is);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block		
			e.printStackTrace();
			return results;
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return results;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return results;
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return results;
		}
		NodeList nodeLst = doc.getElementsByTagName("CalaisSimpleOutputFormat");

		for (int s = 0; s < nodeLst.getLength(); s++) {

			Node fstNode = nodeLst.item(s);
			visit(fstNode, 0);
		}

		Iterator<CalaisEntity> it = m_entities.iterator();
		while (it.hasNext()) {
			CalaisEntity prop = it.next();
			results.put(prop.getType(), new AnalyzerResult(prop.getType(), prop
					.getName(), prop.getRelevance(), prop.getCount()));
			
			System.out.println("Analyzer Result : " + prop.getType() + "," + prop.getName() + "," + prop.getRelevance() + "," + prop.getCount());
		}

		return results;
	
	}
	public List analyze(LWComponent c, boolean fallback) {
		m_entities = new ArrayList<CalaisEntity>();
		List<AnalyzerResult> results = new ArrayList<AnalyzerResult>();
		CalaisJavaIf calais = prepCalais();

		String context = null;
		if (c != null) {
			MetadataList ml = c.getMetadataList();
			List<VueMetadataElement> elems = ml.getMetadata();
			Iterator i = elems.iterator();
			c.getNotes();
			context = c.getLabel().trim() + ".  ";

			if (c.getNotes() != null)
				context += c.getNotes().trim() + ".  ";

			while (i.hasNext()) {
				VueMetadataElement e = (VueMetadataElement) i.next();
				context += e.getValue() + ".  ";
			}

			if (c.getResource() != null) {
				MetaMap map = c.getResource().getProperties();
				// /c.getResource().get
				if (map != null) {
					Collection collection = map.entries();

					// Iterator iterator = collection.iterator();
					Object[] obj = collection.toArray();
					for (int p = 0; p < obj.length; p++) {
						com.google.common.collect.AbstractMapEntry o = (AbstractMapEntry) obj[p];
						// System.out.println(o.getKey());
						String key = o.getKey().toString().toLowerCase();
						if (key.startsWith("title") || key.startsWith("date")
								|| key.startsWith("creator")
								|| key.startsWith("description")) {
							// System.out.println(o.toString());
							context += "The " + o.getKey().toString().trim()
									+ " is " + o.getValue().toString().trim()
									+ ".  ";
						}
					}
				}
			}
		} else {
			context = "Eduardo Manet the 19th century French painter.";
		}

		URLEncoder.encode(context);
		String resp_simple = null;
		resp_simple = calais.callEnlighten(context);

		resp_simple = StringUtils.unescapeHTML(resp_simple);

		// m_entities
		javax.xml.parsers.DocumentBuilderFactory factory = DocumentBuilderFactory
				.newInstance();
		factory.setIgnoringElementContentWhitespace(true);
		factory.setIgnoringComments(true);
		factory.setValidating(false);

		InputStream is = null;
		org.w3c.dom.Document doc = null;

		try {
			is = new java.io.ByteArrayInputStream(resp_simple.getBytes("UTF-8"));
			doc = factory.newDocumentBuilder().parse((InputStream) is);
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
		NodeList nodeLst = doc.getElementsByTagName("CalaisSimpleOutputFormat");

		for (int s = 0; s < nodeLst.getLength(); s++) {

			Node fstNode = nodeLst.item(s);
			visit(fstNode, 0);
		}

		Iterator<CalaisEntity> it = m_entities.iterator();

		while (it.hasNext()) {
			Entity prop = it.next();
			results.add(new AnalyzerResult(prop.getType(), prop.getName()));
		}

		return results;
	}

	public void visit(Node node, int level) {
		NodeList nl = node.getChildNodes();

		if (nl.getLength() == 1)
			return;

		boolean skip = false;
		for (int i = 0, cnt = nl.getLength(); i < cnt; i++) {
			Node n = nl.item(i);

			if (n.getNodeName().equals("#text"))
				continue;
			if (n.getFirstChild() == null)
				skip = true;
			else if (n.getFirstChild().getNodeValue() == null
					|| (n.getFirstChild().getNodeValue() != null && n
							.getFirstChild().getNodeValue().trim().equals("")))
				skip = true;
			else
				skip = false;
			
			if (!skip) {
				CalaisEntity entity = new CalaisEntity();
				entity.setType(n.getNodeName());

				NamedNodeMap nnm = n.getAttributes();
				Node countNode = nnm.getNamedItem("count");
				String cString = null;
				try {
					cString = countNode.getNodeValue();
				} catch (NullPointerException npe) {
				} finally {
					if (cString == null)
						cString = "1";
				}
				entity.setCount((Integer.valueOf(cString)).intValue());
				Node doubleNode = nnm.getNamedItem("relevance");
				try {
					cString = doubleNode.getNodeValue();
				} catch (NullPointerException npe) {
				} finally {
					if (cString == null)
						cString = "0.0";
				}

				entity.setRelevance((Double.valueOf(cString)));
				entity.setName(n.getFirstChild().getNodeValue());
				m_entities.add(entity);
			}
			visit(nl.item(i), level + 1);
		}
	}

	public String getAnalyzerName() {
		return "Open Calais Analyzer";
	}

	public static void main(String[] args) {
		OpenCalaisAnalyzer oca = new OpenCalaisAnalyzer();
		oca.analyze(null);
	}
}
