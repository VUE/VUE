/*
* Copyright 2003-2010 Tufts University  Licensed under the
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

package tufts;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Entity;
import org.w3c.dom.EntityReference;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Notation;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class DocDump {
  public static void dump(Document doc) {
    dumpLoop((Node) doc, "");
  }
  public static void dump(Node node) {
    dumpLoop(node, "");
  }

  private static void dumpLoop(Node node, String indent) {
    switch (node.getNodeType()) {
    case Node.ATTRIBUTE_NODE:
      dumpAttributeNode((Attr) node, indent);
      break;
    case Node.CDATA_SECTION_NODE:
      dumpCDATASectionNode((CDATASection) node, indent);
      break;
    case Node.COMMENT_NODE:
      dumpCommentNode((Comment) node, indent);
      break;
    case Node.DOCUMENT_NODE:
      dumpDocument((Document) node, indent);
      break;
    case Node.DOCUMENT_FRAGMENT_NODE:
      dumpDocumentFragment((DocumentFragment) node, indent);
      break;
    case Node.DOCUMENT_TYPE_NODE:
      dumpDocumentType((DocumentType) node, indent);
      break;
    case Node.ELEMENT_NODE:
      dumpElement((Element) node, indent);
      break;
    case Node.ENTITY_NODE:
      dumpEntityNode((Entity) node, indent);
      break;
    case Node.ENTITY_REFERENCE_NODE:
      dumpEntityReferenceNode((EntityReference) node, indent);
      break;
    case Node.NOTATION_NODE:
      dumpNotationNode((Notation) node, indent);
      break;
    case Node.PROCESSING_INSTRUCTION_NODE:
      dumpProcessingInstructionNode((ProcessingInstruction) node, indent);
      break;
    case Node.TEXT_NODE:
      dumpTextNode((Text) node, indent);
      break;
    default:
      System.out.println(indent + "Unknown node");
      break;
    }

    NodeList list = node.getChildNodes();
    for (int i = 0; i < list.getLength(); i++)
      dumpLoop(list.item(i), indent + "   ");
  }

  /* Display the contents of a ATTRIBUTE_NODE */
  private static void dumpAttributeNode(Attr node, String indent) {
    System.out.println(indent + "ATTRIBUTE " + node.getName() + "="
                       + Util.tags(node.getValue()));
  }

  /* Display the contents of a CDATA_SECTION_NODE */
  private static void dumpCDATASectionNode(CDATASection node, String indent) {
    System.out.println(indent + "CDATA SECTION length=" + node.getLength());
    System.out.println(indent + "\"" + node.getData() + "\"");
  }

  /* Display the contents of a COMMENT_NODE */
  private static void dumpCommentNode(Comment node, String indent) {
    System.out.println(indent + "COMMENT length=" + node.getLength());
    System.out.println(indent + "  " + node.getData());
  }

  /* Display the contents of a DOCUMENT_NODE */
  private static void dumpDocument(Document node, String indent) {
    System.out.println(indent + "DOCUMENT");
  }

  /* Display the contents of a DOCUMENT_FRAGMENT_NODE */
  private static void dumpDocumentFragment(DocumentFragment node, String indent) {
    System.out.println(indent + "DOCUMENT FRAGMENT");
  }

  /* Display the contents of a DOCUMENT_TYPE_NODE */
  private static void dumpDocumentType(DocumentType node, String indent) {
    System.out.println(indent + "DOCUMENT_TYPE: " + node.getName());
    if (node.getPublicId() != null)
      System.out.println(indent + " Public ID: " + node.getPublicId());
    if (node.getSystemId() != null)
      System.out.println(indent + " System ID: " + node.getSystemId());
    NamedNodeMap entities = node.getEntities();
    if (entities.getLength() > 0) {
      for (int i = 0; i < entities.getLength(); i++) {
        dumpLoop(entities.item(i), indent + "  ");
      }
    }
    NamedNodeMap notations = node.getNotations();
    if (notations.getLength() > 0) {
      for (int i = 0; i < notations.getLength(); i++)
        dumpLoop(notations.item(i), indent + "  ");
    }
  }

  /* Display the contents of a ELEMENT_NODE */
  private static void dumpElement(Element node, String indent) {
    System.out.println(indent + "ELEMENT: " + node.getTagName());
    NamedNodeMap nm = node.getAttributes();
    for (int i = 0; i < nm.getLength(); i++)
      dumpLoop(nm.item(i), indent + "  ");
  }

  /* Display the contents of a ENTITY_NODE */
  private static void dumpEntityNode(Entity node, String indent) {
    System.out.println(indent + "ENTITY: " + node.getNodeName());
  }

  /* Display the contents of a ENTITY_REFERENCE_NODE */
  private static void dumpEntityReferenceNode(EntityReference node, String indent) {
    System.out.println(indent + "ENTITY REFERENCE: " + node.getNodeName());
  }

  /* Display the contents of a NOTATION_NODE */
  private static void dumpNotationNode(Notation node, String indent) {
    System.out.println(indent + "NOTATION");
    System.out.print(indent + "  " + node.getNodeName() + "=");
    if (node.getPublicId() != null)
      System.out.println(node.getPublicId());
    else
      System.out.println(node.getSystemId());
  }

  /* Display the contents of a PROCESSING_INSTRUCTION_NODE */
  private static void dumpProcessingInstructionNode(ProcessingInstruction node,
      String indent) {
    System.out.println(indent + "PI: target=" + node.getTarget());
    System.out.println(indent + "  " + node.getData());
  }

  /* Display the contents of a TEXT_NODE */
  private static void dumpTextNode(Text node, String indent) {
    System.out.println(indent + "TEXT length=" + node.getLength());
    System.out.println(indent + "  " + node.getData());
  }
}
