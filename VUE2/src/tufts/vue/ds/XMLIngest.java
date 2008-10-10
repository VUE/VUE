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

package tufts.vue.ds;

import tufts.Util;
import tufts.vue.DEBUG;
import tufts.vue.MetaMap;
import tufts.vue.MetaMap.*;

import java.util.*;
import java.io.*;
import java.net.*;

import javax.xml.xpath.*;
import javax.xml.parsers.*;

import org.w3c.dom.Node;
import org.w3c.dom.*;
import org.xml.sax.*;


// TODO: just forget handling depth (e.g., jira comments) for now -- can tackle later.
// The keep in mind w/respect to how we handle data-set interation, so could
// add this under the hood if we like later.

// As for our data-model, we could literally use the XML DOM, tho that's got
// way more than we need in it and isn't very convenient.  I guess we
// just need our own API that nicely abstracts everything, so under the
// hood we could use anything from Jackrabbit to DOM to Mutlimaps to SQL
// or whatever.

// NEED TO GENERICALLY HANDLE KEY MANAGEMENT, AND DATA-CHANGE DETECTION.

// Big question: do we persist original raw XML streams, or digest
// the data first then persist it?  First case is safer for
// ultimate data integrity -- can fix parsing / data coalesecing
// bugs or make enhancements more easily.  We could persist the
// mashed data, but but then we'd just need another format / persist
// schema anyway.


/**
 * @version $Revision: 1.4 $ / $Date: 2008-10-10 04:57:46 $ / $Author: sfraize $
 * @author Scott Fraize
 */

class XMLIngest {

    private static final boolean XML_DEBUG = false;

    static class XmlSchema extends tufts.vue.ds.Schema
    {
        final String itemPath;
        final int itemPathLen;

        final boolean keyFold;
        
        DataRow curRow;

        public XmlSchema(Object source, String itemPath) 
        {
            super(source);
            this.itemPath = itemPath;
            if (itemPath == null || itemPath.length() == 0)
                itemPathLen = 0;
            else
                itemPathLen = itemPath.length() + 1; // add one for dot
            keyFold = (itemPath != null && itemPath.startsWith("plist"));
            
            //itemPathLen = itemPath.length() + (itemPath.endsWith(".") ? 0 : 1);
        }

        @Override public boolean isXMLKeyFold() {
            return keyFold;
        }
        
        @Override public void dumpSchema(PrintWriter ps) {
            if (itemPath != null) ps.println("ItemPath: " + itemPath);
            super.dumpSchema(ps);
        }
        
        void trackFieldValuePair(String name, String value) {

            //errout("TRACK " + name + "=" + value);

            if (itemPath != null && name.startsWith(itemPath) && name.length() > itemPathLen)
                name = name.substring(itemPathLen);

            Field field = mFields.get(name);
            if (field == null) {
                field = new Field(name, this);
//                 if (name.equals(getKeyNode()))
//                     keyField = field;
                mFields.put(name, field);
                if (name.length() > mLongestFieldName)
                    mLongestFieldName = name.length();
            }
            if (curRow != null)
                curRow.addValue(field, value);
            else
                field.trackValue(value);
        }

        void trackNodeOpen(String name) {
            if (name.equals(getRowStartNode())) {
                //errout("OPEN " + name);
                // curRow = new VRow(fields.size()); // fields includes non-row-extraction values
                curRow = new DataRow();
                addRow(curRow);
            }
        }

        void trackNodeClose(String name) {
            if (name.equals(getRowStartNode())) {
                //errout(String.format("CLOSE %s with %2d fields, key %s", name, curRow.size(), curRow.getValue(keyField)));
                curRow = null;
            }
        }

        String getRowStartNode() { return itemPath; }
        String getKeyNode() { return null; }
        
    }

//     static class RssSchema extends XmlSchema {
        
//         @Override
//         final String getRowStartNode() {
//             //return "item";
//             return "rss.channel.item";
//         }

//         @Override
//         final String getKeyNode() {
//             //return "rss.channel.item.key";
//             //return "item.key";
//             return "key";
//         }
        
//     }

    
    static int depth = 0;


    static void XPathExtract(XmlSchema schema, Document document)
    {
    
        try {
      
            XPath xpath = XPathFactory.newInstance().newXPath();

            String expression = "/rss/channel/item";
            //String expression = "rss/channel/item/title";

            errout("Extracting " + expression);
            
            // First, obtain the element as a node.

            Node nodeValue = (Node) xpath.evaluate(expression, document, XPathConstants.NODE);
            errout("   Node: " + nodeValue);

            // Next, obtain the element as a String.

            String stringValue = (String) xpath.evaluate(expression, document, XPathConstants.STRING);
            System.out.println(" String: " + stringValue);

            NodeList nodeSet = (NodeList) xpath.evaluate(expression, document, XPathConstants.NODESET);
            errout("NodeSet: " + Util.tag(nodeSet) + "; size=" + nodeSet.getLength());

            for (int i = 0; i < nodeSet.getLength(); i++) {
                scanNode(schema, nodeSet.item(i), null, null);
            }
            

//             // Finally, obtain the element as a Number (Double).

//             Double birthdateDouble = (Double) xpath.evaluate(expression, document, XPathConstants.NUMBER);
   
//             System.out.println("Double is: " + birthdateDouble);
   
        } catch (XPathExpressionException e) {
            System.err.println("XPathExpressionException caught...");
            e.printStackTrace();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

  //public static Schema ingestXML(URLConnection conn, String itemKey)
    public static Schema ingestXML(InputSource input, String itemKey)
    {
//         // SMF 2008-10-02: E.g. Craigslist XML streams use ISO-8859-1, which is provided in
//         // HTML headers as "Content-Type: application/rss+xml; charset=ISO-8859-1", (tho not
//         // in a special content-encoding header), and our current XML parser fails unless
//         // the stream is read with this set: e.g.: [org.xml.sax.SAXParseException: Character
//         // conversion error: "Unconvertible UTF-8 character beginning with 0x95" (line
//         // number may be too low).]  Actually, in this case it turns out that providing a
//         // default InputStreamReader (encoding not specified) as opposed to a direct
//         // InputStream from the URLConnection works, and the XML parser is presumably then
//         // finding and handling the "<?xml version="1.0" encoding="ISO-8859-1"?>" line at
//         // the top of the XML stream
//         final XmlSchema schema = new XmlSchema(conn.getURL(), itemKey);
//         InputStream is = null;
//         try {
//             is = conn.getInputStream();
//             errout("GOT INPUT STREAM: " + Util.tags(is));
//         } catch (IOException e) {
//             e.printStackTrace();
//             return null;
//         }
//         final Document doc = parseXML(is, false);

        // Could also use a ROME API XmlReader(URLConnection) for handling
        // the input, which does it's own magic to figure out the encoding.
        // For more on the complexity of this issue, see:
        // http://diveintomark.org/archives/2004/02/13/xml-media-types
        
        final XmlSchema schema = new XmlSchema(input.getSystemId(), itemKey);
        
        final Document doc = parseXML(input, false);

        //doc.normalizeDocument();
        errout("GOT DOC " + Util.tag(doc) + " " + doc);
        //errout("InputEncoding: " + doc.getInputEncoding()); // AbstractMethodError ?
        //errout("xmlEncoding: " + doc.getXmlEncoding());
        //errout("xmlVersion: " + doc.getXmlVersion());
        errout("docType: " + Util.tags(doc.getDoctype()));
        errout("impl: " + Util.tags(doc.getImplementation().getClass()));
        errout("docElement: " + Util.tags(doc.getDocumentElement().getClass())); // toString() can dump whole document!
        //out("element: " + Util.tags(doc.getDocumentElement()));

        //outln("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
        //outln("<!-- created by RSSTest " + new Date() + " from " + src + " -->");

        if (false)
            XPathExtract(schema, doc);
        else
            scanNode(schema, doc.getDocumentElement(), null, null);

        schema.dumpSchema(System.err);
        return schema;
    }


    private static boolean isText(int type) {
        return type == Node.TEXT_NODE || type == Node.CDATA_SECTION_NODE;
    }
    private static boolean isText(Node node) {
        return isText(node.getNodeType());
    }
        
    
    // parentPath is the fully-qualified parent name

    private static void scanNode(XmlSchema schema, org.w3c.dom.Node n, String parentPath, String parentName) {

        final int type = n.getNodeType();
        final String value = n.getNodeValue();
        final boolean isAttribute = (type == Node.ATTRIBUTE_NODE);        
        String name = n.getNodeName();

        scanNode(schema, n, type, parentPath, parentName, name, value);
    }

    private static void scanNode(final XmlSchema schema,
                                final org.w3c.dom.Node node,
                                final int type,
                                final String parentPath,
                                final String parentName,
                                final String nodeName,
                                final String value)
    {
        final boolean isAttribute = (type == Node.ATTRIBUTE_NODE);
        final boolean isMergedText = FOLD_TEXT && isText(type);
        final boolean hasAttributes = (!isAttribute && node != null && node.hasAttributes());
        Node firstChild = null, lastChild = null;

        if (node != null) {
            firstChild = node.getFirstChild();
            lastChild = node.getLastChild();
        }

        final String XMLName;

        if (isAttribute)
            XMLName = parentName + ATTR_SEPARATOR + nodeName;
        else
            XMLName = nodeName;
        
        final String fullName;

        if (parentPath != null) { // should only be null first time in at the top root
            if (isMergedText)
                fullName = parentPath;
            else if (isAttribute)
                fullName = parentPath + ATTR_SEPARATOR + nodeName;
            else 
                fullName = parentPath + '.' + nodeName;
        } else {
            fullName = nodeName;
        }

        if (type == Node.ELEMENT_NODE)
            schema.trackNodeOpen(fullName);

        if (depth < REPORT_THRESH) {
            if (depth < REPORT_THRESH - 1) {
                if (type == Node.TEXT_NODE)
                    eoutln(String.format("node(%d) %s(len=%d)", type, fullName, value.length()));
                else
                    eoutln(String.format("NODE(%d) %s %s", type, fullName, node, Util.tags(firstChild)));
            }
            //eoutln("NODE: " + type + " name=" + name + " " + Util.tags(n) + " firstChild=" + Util.tags(firstChild));
                //System.err.println(name);
            else if (XML_DEBUG)
                System.err.print(".");
        }


        if (hasAttributes && ATTRIBUTES_IMMEDIATE)
            scanAttributes(schema, fullName, nodeName, node.getAttributes());

        String outputValue = null;

        if (value != null) {
            outputValue = value.trim();
            if (outputValue.length() > 0) {
                schema.trackFieldValuePair(fullName, outputValue);
            } else
                outputValue = null;
        }
        
        final NodeList children = node.getChildNodes();
        final boolean DO_TAG;

        if (isMergedText) {
            DO_TAG = false;
        }
        else if (outputValue == null && node != null) {
            if (!node.hasChildNodes()) {
                DO_TAG = false;
            }
            else if (children.getLength() == 1 && isText(firstChild) && firstChild.getNodeValue().trim().length() == 0) {
                DO_TAG = false;
            }
            else
                DO_TAG = true;
            
            // if (!DO_TAG) ioutln("<!-- empty: " + nodeName + " -->");
        }
        else
            DO_TAG = true;
        
        boolean closeOnSameLine = false;

        if (DO_TAG) {
            
            iout("<");
            out(XMLName);
            //if (node.hasChildNodes()) out(" children=" + node.getChildNodes().getLength() + " first=" + node.getFirstChild());
            out(">");
            
            if (firstChild == null || (isText(firstChild) && firstChild == lastChild)) {
//                 if (firstChild != null && firstChild.getNodeType() == Node.CDATA_SECTION_NODE)
//                     ;
//                 else
                    closeOnSameLine = true;
            } else if (XML_DEBUG)
                System.out.print('\n');
            
            if (FOLD_TEXT && (type != Node.ELEMENT_NODE && type != Node.ATTRIBUTE_NODE)) {
                final String err = "UNHANDLED TYPE=" + type + "; " + nodeName;
                outln("<" + err + ">");
                errout(err);
            }
        }

        if (outputValue != null) {
            if (type == Node.CDATA_SECTION_NODE) {
                out("<![CDATA[");
                out(outputValue);
                out("]]>");
            } else {
                out(XMLEntityEncode(outputValue));
            }
        }
            
        if (!isAttribute && node != null) {

            // god knows why, but attributes have themselves as children?  (or is that
            // the #text entry?)  Anyway, if we allow this for an attribute dump, the
            // value of the attribute will literally appear twice in the output,
            // back-to-back as one string.
            
            depth++;

            if (FOLD_KEYS || schema.isXMLKeyFold()) {

                scanFoldedChildren(schema, children, fullName, nodeName);

            } else {
                
                for (int i = 0; i < children.getLength(); i++)
                    scanNode(schema, children.item(i), fullName, nodeName);
            }
            
            depth--;
            
        }

        if (DO_TAG) {

            if (closeOnSameLine)
                outln("</" + XMLName + ">");
            else
                ioutln("</" + XMLName + ">");
        }

        if (type == Node.ELEMENT_NODE)
            schema.trackNodeClose(fullName);
            
        if (hasAttributes && ! ATTRIBUTES_IMMEDIATE)
            scanAttributes(schema, fullName, nodeName, node.getAttributes());
        
        //iout("children: " + Util.tags(n.getChildNodes()));
    }

    private static void scanAttributes(XmlSchema schema, String fullName, String nodeName, NamedNodeMap attr) {

        if (attr != null && attr.getLength() > 0) {
            //depth++;
            for (int i = 0; i < attr.getLength(); i++) {
                final Node a = attr.item(i);
                scanNode(schema, a, fullName, nodeName);
            }
            //depth--;
        }
    }

    private static void scanFoldedChildren(XmlSchema schema, final NodeList children, final String fullName, final String nodeName)
    {
        // Test code for folding Apple plist style <dict> pairs (<key>UserKey</key><string>UserValue</string>)
        // using iTunes Music Library.xml as test case.
                
        for (int i = 0; i < children.getLength(); i++) {
            final Node item = children.item(i);
            final Node next = children.item(i+1);

            if (next != null) {
                final String nextName = next.getNodeName();
                //errout("checking pair: " + item.getNodeName() + "/" + nextName); 
                //if ("key".equals(item.getNodeName()) && !"dict".equals(nextName)) {
                if ("key".equals(item.getNodeName())) {
                    //final String newNodeName = item.getNodeValue();
                    //final String newNodeValue = next.getNodeValue();
                            
                    // must extract through one more layer of indirection

                    String newNodeName = item.getChildNodes().item(0).getNodeValue();
                            
                    if (newNodeName != null)
                        newNodeName = newNodeName.replace(' ', '_');

                    final String newNodeValue;

                    if ("true".equals(nextName)) {
                        //newNodeValue = next.getNodeValue()
                        newNodeValue = "true"; // is a simle "<true/>" self-terminating value with/NO CHILDREN
                    }
                    else if ("false".equals(nextName)) {
                        // almost never see this in iTunes Music Library.xml
                        //errout("GOT FALSE");
                        newNodeValue = "false";
                    }
                    else if ("dict".equals(nextName) || "array".equals(nextName)) {

                        continue;
                                
                        //                                 //newNodeValue = "(todo: pull-up under: " + nextName + ")";
                        //                                 newNodeValue = nextName;
                        //                                 i--;  // we're not extracting this yet, so don't pull it out below
                    }
                    else {
                        newNodeValue = next.getChildNodes().item(0).getNodeValue();
                    }

                    //if ("Visible".equals(newNodeName)) errout("VALUE: " + newNodeValue);
                            
                    //errout(String.format("\t%s=[%s]", newNodeName, newNodeValue));
                    //errout("value children: " + item.getChildNodes());
                    // extract the current node value as a new node name, and the next node value as the new node value
                    scanNode(schema, null, Node.ELEMENT_NODE, fullName, nodeName, newNodeName, newNodeValue);
                    i++;
                    continue;
                }
            }

            scanNode(schema, item, fullName, nodeName);
        }
    }
    
    
    /*
    public static void dumpElement(Element e) {
        out("\tElement: " + Util.tags(e));
        out("\tElement tag: " + e.getTagName());
        out("\tElement SchemaTypeInfo: " + Util.tags(e.getSchemaTypeInfo()));
    }
    */
    
    // Parses an XML file and returns a DOM document.
    // If validating is true, the contents is validated against the DTD
    // specified in the file.
    private static org.w3c.dom.Document parseXML(Object input, boolean validating) {
        try {
            // Create a builder factory
            javax.xml.parsers.DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringElementContentWhitespace(true);
            factory.setIgnoringComments(true);
            //factory.setCoalescing(true);
            factory.setValidating(validating);
    
            // Create the builder and parse the file
            Document doc;
            if (input instanceof String) {
                doc = factory.newDocumentBuilder().parse(new File((String)input));
            } else if (input instanceof InputSource) {
                doc = factory.newDocumentBuilder().parse((InputSource)input);
            } else if (input instanceof InputStream) {
//                 InputSource encoded = new InputSource();
//                 encoded.setByteStream((InputStream)input);
//                 encoded.setEncoding("ISO-8859-1"); // TODO: get from url stream
//                 doc = factory.newDocumentBuilder().parse(encoded);
//                 //doc = factory.newDocumentBuilder().parse(new InputStreamReader((InputStream) input, "ISO-8859-1"));
                doc = factory.newDocumentBuilder().parse((InputStream) input);
            }
            else
                throw new Error("Unhandled input type: " + Util.tags(input));
            return doc;
        } catch (Throwable t) {
            t.printStackTrace();
        }
        /*catch (SAXException e) {
            // A parsing error occurred; the xml input is not valid
        } catch (ParserConfigurationException e) {
        } catch (IOException e) {
        }
        */
        return null;
    }

   public static String XMLEntityEncode(final String text)
   {
       // todo: if the result of this is simply destined for a writer, would
       // be more efficient to pass the writer in, and skip constructing new
       // StringBuffers.  Apache Commons has methods for this, presumably
       // for this reason -- eventually go ahead and use that:

       // will NOT introduce &quot; uneeded for us, possibly problematic, in
       // that this text may ultimately be handled by an HTML component which
       // won't handle "&quot;" (todo: test w/JLabel <html>)
       //return org.apache.commons.lang.StringEscapeUtils.escapeHtml(s);
       
       // will introduce &quot;
       //return org.apache.commons.lang.StringEscapeUtils.escapeXml(s);
       
       StringBuilder buf = null;
       final int len = (text == null ? -1 : text.length());

       for ( int i = 0; i < len; i++ )
       {
           final char c = text.charAt(i);
           String entity = null;

           switch (c) {
               // These are the five basic XML entities:
               // See http://commons.apache.org/lang/api/org/apache/commons/lang/StringEscapeUtils.html
               
           case '&':  entity = "&amp;";     break;
           case '<':  entity = "&lt;";      break;
           case '>':  entity = "&gt;";      break;
           case '"':  entity = "&quot;";    break;
         //case '\'': entity = "&apos;";    break; // not a legal HTML entity, even tho is a legal XML entity
         //case '\r': entity = "&#13;";     break; // test
           default:
               if (buf != null)
                   buf.append(c);
               continue;
           }

           // We've encountered something to encode: entity has been set:
           
           if (buf == null) {
               buf = new StringBuilder(len + 12);
               buf.append(text, 0, i);
           }
           buf.append(entity);
           
       }
       return buf == null ? text : buf.toString();

//        for ( int i = 0; i < len; i++ ) {
//            final char c = s.charAt( i );
//            if (c >= 'a' && c <= 'z' || c >='A' && c <= 'Z' || c >= '0' && c <= '9') {
//                buf.append( c );
//            } else {
//                final String entity;
//                switch (c) {
//                case '&':  entity = "&amp;";     break;
//                case '<':  entity = "&lt;";      break;
//                case '>':  entity = "&gt;";      break;
//                case '"':  entity = "&quot;";    break;
//              //case '\'': entity = "&apos;";    break; // apparently, not actually a legal entity
//              //case '\r': entity = "&#13;";     break;
//                default:   entity = null;
//                }
//                if (entity != null)
//                    buf.append(entity);
//                else
//                    //buf.append( "&#" + (int)c + ";" );
//                    buf.append(c);
//            }
//        }
//       return buf.toString();
       
   }    


    public static void iout(String s) {
        iout(depth, s);
    }

    public static void ioutln(String s) {
        ioutln(depth, s);
    }

    final static String TAB = "    ";

    public static void iout(int _depth, String s) {
        //for (int x = 0; x < _depth; x++) System.out.print(TAB);
        //System.out.print(s);
    }
    
    public static void ioutln(int _depth, String s) {
        //for (int x = 0; x < _depth; x++) System.out.print(TAB);
        //System.out.println(s);
    }

    public static void eoutln(int _depth, String s) {
        //for (int x = 0; x < _depth; x++) System.err.print(TAB);
        //System.err.println(s);
    }

    public static void eoutln(String s) {
        eoutln(depth, s);
    }
    
    

    public static void out(String s) {
        //System.out.print(s == null ? "null" : s);
    }

    public static void outln(String s) {
        //System.out.println(s == null ? "null" : s);
    }
    public static void errout(String s) {
        System.err.println(s == null ? "null" : s);
    }




    final static boolean ATTRIBUTES_IMMEDIATE = false; // false better for clearer XML output, true better for schema output (e.g., rss.version 1st, not last)
    final static boolean FOLD_TEXT = true; // default true: fold Node.TEXT_NODE(#text) and CDATA items into parent node

    final static boolean FOLD_KEYS = false; // auto-enabled if top-level item is "plist" (current breaks on JIRA XML if true)

    //final static int REPORT_THRESH = FOLD_KEYS ? 4 : 3;
    final static int REPORT_THRESH = 1;

    final static char ATTR_SEPARATOR = '@';


    private static final String JIRA_VUE_URL = "http://bugs.atech.tufts.edu/secure/IssueNavigator.jspa?view=rss&pid=10001&tempMax=9999&reset=true&decorator=none";
    private static final String JIRA_SFRAIZE_COOKIE = "seraph.os.cookie=LkPlQkOlJlHkHiEpGiOiGjJjFi";

    private static InputStream getTestXMLStream()
        throws IOException
    {
        URL url = new URL(JIRA_VUE_URL);
        URLConnection conn = url.openConnection();
        conn.setRequestProperty("Cookie", JIRA_SFRAIZE_COOKIE);
        errout("Opening connection to " + url);
        conn.connect();
        
        errout("Getting InputStream...");
        InputStream in = conn.getInputStream();
        errout("Got " + Util.tags(in));
        
        errout("Getting headers...");
        Map<String,List<String>> headers = conn.getHeaderFields();

        errout("HEADERS:");
        for (Map.Entry<String,List<String>> e : headers.entrySet()) {
            errout(e.getKey() + ": " + e.getValue());
        }

        return in;
    }
    
    public static void main(String[] args)
        throws IOException
    {
        //final XmlSchema schema = new RssSchema();
        
        errout("Max mem: " + Util.abbrevBytes(Runtime.getRuntime().maxMemory()));
        //getXMLStream();System.exit(0);
        
        Document doc;
        String src;

        if (args.length < 1) {
            doc = parseXML(getTestXMLStream(), false);
            src = JIRA_VUE_URL;
        } else {
            doc = parseXML(args[0], false);
            src = args[0];
        }
        //doc.normalizeDocument();
        errout("GOT DOC " + Util.tag(doc) + " " + doc);
        errout("InputEncoding: " + doc.getInputEncoding());
        errout("xmlEncoding: " + doc.getXmlEncoding());
        errout("xmlVersion: " + doc.getXmlVersion());
        errout("docType: " + Util.tags(doc.getDoctype()));
        errout("impl: " + Util.tags(doc.getImplementation()));
        errout("docElement: " + Util.tags(doc.getDocumentElement()));
        //out("element: " + Util.tags(doc.getDocumentElement()));

        outln("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
        outln("<!-- created by RSSTest " + new Date() + " from " + src + " -->");

        final XmlSchema schema = new XmlSchema(Util.tag(doc), "rss.channel.item");
        
        if (true)
            XPathExtract(schema, doc);
        else
            scanNode(schema, doc.getDocumentElement(), null, null);

        schema.dumpSchema(System.err);
    }

    

    

    
    

    


}