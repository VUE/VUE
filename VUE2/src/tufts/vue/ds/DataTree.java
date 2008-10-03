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

import tufts.vue.VUE;
import tufts.vue.LWComponent;
import tufts.vue.LWNode;
import tufts.vue.LWLink;
import tufts.vue.LWMap;
import tufts.vue.Actions;
import tufts.vue.LWKey;
import tufts.vue.DrawContext;
import tufts.vue.gui.GUI;
import edu.tufts.vue.metadata.VueMetadataElement;
import tufts.Util;

import java.util.List;
import java.util.*;
import java.awt.*;
import java.awt.dnd.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

/**
 *
 * @version $Revision: 1.1 $ / $Date: 2008-10-03 16:20:19 $ / $Author: sfraize $
 * @author  Scott Fraize
 */

public class DataTree extends javax.swing.JTree
    implements DragGestureListener
               , LWComponent.Listener
               //,DragSourceListener
               //,TreeSelectionListener
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(DataTree.class);
    
    final Schema schema;

    public DataTree(Schema schema) {

        this.schema = schema;

        setCellRenderer(new DataRenderer());

        setModel(new DefaultTreeModel(buildTree(), false));

        java.awt.dnd.DragSource.getDefaultDragSource()
            .createDefaultDragGestureRecognizer
            (this,
             java.awt.dnd.DnDConstants.ACTION_COPY |
             java.awt.dnd.DnDConstants.ACTION_MOVE |
             java.awt.dnd.DnDConstants.ACTION_LINK,
             this);
        

        addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
                public void valueChanged(javax.swing.event.TreeSelectionEvent e) {
                    if (e.isAddedPath() && e.getPath().getLastPathComponent() != null ) {
                        final DataNode treeNode = (DataNode) e.getPath().getLastPathComponent();
                        if (treeNode.hasStyle()) {
                            VUE.getSelection().setSelectionSourceFocal(null); // prevents from ever drawing through on map
                            VUE.getSelection().setTo(treeNode.getStyle());
                        }
                        //VUE.setActive(LWComponent.class, this, node.styleNode);
                    }
                }
            });

        
        
    }

    /** build the model and return the root node */
    private TreeNode buildTree()
    {
        DataNode root =
            new DataNode(null, schema.getName(),
                         String.format("%s [%d %s]",
                                       schema.getName(),
                                       schema.getRowCount(),
                                       "items"//isCSV ? "rows" : "items"
                                       )
                         );
        

        for (Field field : schema.getFields()) {
            
            DataNode fieldNode = new DataNode(field, this);
            root.add(fieldNode);
            
//             if (field.uniqueValueCount() == schema.getRowCount()) {
//                 //Log.debug("SKIPPING " + f);
//                 continue;
//             }

            final Set values = field.getValues();
            //Log.debug("EXPANDING " + colNode);

            //LWComponent schemaNode = new LWNode(schema.getName() + ": " + schema.getSource());
            // add all style nodes to the schema node to be put in an internal layer for
            // persistance: either that or store them with the datasources, which
            // probably makes more sense.
            
            if (values.size() == 0) {

                if (field.getMaxValueLength() == 0) {
                    fieldNode.setUserObject(String.format("<html><b><font color=gray>%s", field.getName()));
                } else {
                    fieldNode.setUserObject(String.format("<html><b>%s (max size: %d bytes)",
                                                          field.getName(), field.getMaxValueLength()));
                }
                
            } else if (values.size() == 1) {
                
                fieldNode.setUserObject(String.format("<html><b>%s: <font color=green>%s",
                                                      field.getName(), field.getValues().toArray()[0]));

            } else if (values.size() > 1) {

                final Map<String,Integer> valueCounts = field.getValueMap();
                
                fieldNode.setUserObject(String.format("<html><b>%s</b> (%d)",
                                                      field.getName(), field.uniqueValueCount()));

                //-----------------------------------------------------------------------------
                // Add the enumerated values
                //-----------------------------------------------------------------------------
                
                
                for (Map.Entry<String,Integer> e : valueCounts.entrySet()) {
                    //Log.debug("ADDING " + o);
                    //fieldNode.add(new DefaultMutableTreeNode(e.getKey() + "/" + e.getValue()));

                    if (e.getValue() == 1) {
                        fieldNode.add(new DataNode(field, e.getKey(), String.format("<html><b>%s", e.getKey())));
                    } else {
                        fieldNode.add(new DataNode(field, e.getKey(), String.format("<html><b>%s</b> (%s)", e.getKey(), e.getValue())));
                    }
                }
//                 for (Object o : values) {
//                     //Log.debug("ADDING " + o);
//                     fieldNode.add(new DefaultMutableTreeNode(o));
//                 }
            }
            
        }
    
        return root;
    }

    public void dragGestureRecognized(DragGestureEvent e) {
        if (getSelectionPath() != null) {
            Log.debug("SELECTED: " + Util.tags(getSelectionPath().getLastPathComponent()));
            final DataNode treeNode = (DataNode) getSelectionPath().getLastPathComponent();
            //                          if (resource != null) 
            //                              GUI.startRecognizedDrag(e, resource, this);
                         
            //tufts.vue.gui.GUI.startRecognizedDrag(e, Resource.instance(node.value), null);

            // TODO: how are we going to persist these styles?  Most
            // natural way would be inside a hidden layer, but that's on
            // the MAP, which would mean each map would need it's schema
            // recorded with styled nodes, that can be hooked up
            // DIFFERENTLY to the data source panel.

            final LWComponent dragNode;
            final Field field = treeNode.field;

            if (treeNode.isValue()) {
                //dragNode = new LWNode(String.format(" %s: %s", field.getName(), treeNode.value));
                dragNode = makeValueNode(field, treeNode.value);
                //dragNode.setLabel(String.format(" %s: %s ", field.getName(), treeNode.value));
                //dragNode.setLabel(String.format(" %s ", field.getName());
            } else {
                dragNode = new LWNode(String.format("  %d unique  \n  '%s'  \n  values  ",
                                                    field.uniqueValueCount(),
                                                    field.getName()));
            }
                         
            dragNode.copyStyle(treeNode.getStyle());
            //dragNode.setFillColor(null);
            //dragNode.setStrokeWidth(0);
            if (treeNode.isField()) {
                dragNode.mFontSize.setTo(24);
                dragNode.mFontStyle.setTo(java.awt.Font.BOLD);
                dragNode.setClientData(LWComponent.ListFactory.class,
                                       new NodeProducer(treeNode));
            }


            //                          if (treeNode.field != null) {
            //                              dragNode.setClientData(Field.class, treeNode.field);
            //                              int i = 0;
            //                              final int max = treeNode.field.uniqueValueCount();
            //                              for (String value : treeNode.field.getValues()) {
            //                                  if (++i > 12) {
            //                                      dragNode.addChild(new LWNode("" + (max - i + 1) + " more..."));
            //                                      break;
            //                                  }
            //                                  LWNode n = new LWNode();
            //                                  n.setLabel(value);
            //                                  dragNode.addChild(n);
            //                              }
            //                          }
                         
            tufts.vue.gui.GUI.startRecognizedDrag(e, dragNode);

            //                         e.startDrag(DragSource.DefaultCopyDrop, null);
            //                          e.startDrag(DragSource.DefaultCopyDrop, // cursor
            //                                      null, //dragImage, // drag image
            //                                      new Point(offX,offY), // drag image offset
            //                                      new GUI.ResourceTransfer(resource),
            //                                      dsl);  // drag source listener
                         
                         
        }
    }

    private static String makeLabel(Field f, Object value) {
        //return String.format("%s:\n%s", f.getName(), value.toString());
        if (f.isKeyField())
            return value.toString();
        else
            return "  " + value.toString() + "  ";
    }

    private static LWComponent makeValueNode(Field field, String value) {
        
        LWComponent node = new LWNode(makeLabel(field, value));
        node.addMetaData(field.getName(), value);
        return node;

    }

    
    private static class NodeProducer implements LWComponent.ListFactory {

        private final DataNode treeNode;

        NodeProducer(DataNode n) {
            treeNode = n;
        }

        public java.util.List<LWComponent> produceNodes() {
            Log.debug("PRODUCING NODES FOR " + treeNode.field);
            final java.util.List<LWComponent> nodes = new ArrayList();
            final Field field = treeNode.field;
            final Schema schema = field.getSchema();

            LWNode n = null;

            if (field.isPossibleKeyField()) {
                Log.debug("PRODUCING KEY FIELD NODES " + field);
                int i = 0;
                for (DataRow row : schema.getRows()) {
                    final String value = row.getValue(field);
                    n = new LWNode(makeLabel(field, value));
                    nodes.add(n);
                    //Log.debug("setting meta-data for row " + (++i) + " [" + value + "]");
                    n.getMetadataList().add(row.entries());
//                     for (Map.Entry<String,String> e : row.entries()) {
//                         // todo: this is slow: is updating UI components, setting cursors, etc, every time
//                         n.addMetaData(e.getKey(), e.getValue());
//                     }
                }
                Log.debug("PRODUCED META-DATA IN " + field);
            } else {
                for (String value : field.getValues()) {
                    
                    nodes.add(makeValueNode(field, value));
                    
//                     n = new LWNode(makeLabel(field, value));
//                     //n.setLabel(value);
//                     n.addMetaData(field.getName(), value);
//                     nodes.add(n);
                }
            }

            
            for (LWComponent c : nodes) {
                c.setClientData(Field.class, field);
                c.setStyle(treeNode.getStyle());
            }

            Actions.MakeCircle.act(nodes);
            
            final java.util.List<LWComponent> links = new ArrayList();
            for (LWComponent c : nodes) {
                links.addAll(makeLinks(c, field));
            }
            //nodes.addAll(links);

            // TODO: pass in map
            VUE.getActiveMap().getInternalLayer("*Data Links*").addChildren(links);

            return nodes;
        }


        List<LWLink> makeLinks(LWComponent node, Field field) {

            final LWMap map = VUE.getActiveMap(); // hack;
            final Schema schema = field.getSchema();
            final VueMetadataElement vme = node.getMetadataList().get(field.getName());
            //final String key = vme.getValue();

            Log.debug(Util.tags(vme));

            final List<LWLink> links = new ArrayList();

            for (LWComponent c : map.getAllDescendents()) {
                if (c == node)
                    continue;
                Field f = c.getClientData(Field.class);
                if (f == null || f == field)
                    continue;
                Schema s = f.getSchema();
                // TODO: don't want to check all meta-data: just check the FIELD meta-data for the new node
                // (against all meta-data in other nodes)
                //if (s == schema && node.metaDataIntersects(c))
                //if (s == schema && c.getMetadataList().contains(key)) // no contains?
                //if (s == schema && c.getMetadataList().contains(vme)) // AUTO-JOIN: don't check schema
                if (c.getMetadataList().contains(vme)) {
                    links.add(makeLink(node, c));
                }
                    
            }
            
            return links;
        }
        
    }

    private static LWLink makeLink(LWComponent src, LWComponent dest) {
        LWLink link = new LWLink(src, dest);
        link.setArrowState(0);
        return link;
    }

    public void LWCChanged(tufts.vue.LWCEvent e) {
        repaint();
    }
        
        


//     public static final java.awt.datatransfer.DataFlavor DataFlavor =
//         tufts.vue.gui.GUI.makeDataFlavor(DataNode.class);

    private static final Font EnumFont = new Font("SansSerif", Font.BOLD, 14);
    private static final Font DataFont = new Font("SansSerif", Font.PLAIN, 12);
        

    private static class DataNode extends DefaultMutableTreeNode {

        final Field field;
        final String value;
        
        DataNode(Field field, LWComponent.Listener repainter) {
            this.field = field;
            this.value = null;

            final LWComponent styleNode;

            if (field.isPossibleKeyField()) {
                styleNode = new LWNode(); // creates a rectangular node
                styleNode.setLabel("---");
                styleNode.setFillColor(Color.red);
                styleNode.setFont(DataFont);
            } else {
                styleNode = new LWNode("==="); // creates a round-rect node
                styleNode.setFillColor(Color.blue);
                styleNode.setFont(EnumFont);
            }
            styleNode.setNotes(String.format
                               ("Style node for field '%s' in data-set '%s'\n\nSource: %s\n\n%s",
                                field.getName(),
                                field.getSchema().getName(),
                                field.getSchema().getSource(),
                                field.valuesDebug()));
            styleNode.setFlag(LWComponent.Flag.INTERNAL);
            styleNode.setTextColor(Color.white);
            styleNode.disableProperty(LWKey.Label);
            styleNode.addLWCListener(repainter);

            field.setStyleNode(styleNode);
            
        }
        
        DataNode(Field field, String value, String description) {
            this.field = field;
            this.value = value;
            setUserObject(description); // sets display label
        }

        LWComponent getStyle() {
            return field.getStyleNode();
        }

        boolean hasStyle() {
            return isField();
        }

        boolean isField() {
            return value == null;
        }
        boolean isValue() {
            return value != null;
        }
    }

    private static class DataRenderer extends DefaultTreeCellRenderer {

        public Component getTreeCellRendererComponent(
                final JTree tree,
                final Object value,
                final boolean selected,
                final boolean expanded,
                final boolean leaf,
                final int row,
                final boolean hasFocus)
        {
            //Log.debug(Util.tags(value));
            final DataNode node = (DataNode) value;
            
            if (node.isField() && !leaf) {
                if (node.field.isPossibleKeyField())
                    setForeground(Color.red);
                else
                    setForeground(Color.blue);
            } else {
                setForeground(Color.black);
            }

            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

            if (node.isField()) {
                if (!leaf)
                    setIcon(FieldIconPainter.load(node.getStyle()));
                else
                    setIcon(EmptyIcon);
            } else {
                // enumerated value
                //setIcon(null);
            }
            
            
            return this;
        }
    }

    private static final NodeIconPainter FieldIconPainter = new NodeIconPainter();

    
    private static final int IconWidth = 20;
    private static final int IconHeight = 20;
    private static final java.awt.geom.Rectangle2D IconSize
        = new java.awt.geom.Rectangle2D.Float(0,0,IconWidth,IconHeight);

    private static final Icon EmptyIcon = new GUI.EmptyIcon(IconWidth, IconHeight);
    
    private static class NodeIconPainter implements Icon {

        LWComponent node;

//         NodeIcon(LWComponent c) {
//             node = c;
//         }

        public Icon load(LWComponent c) {
            node = c;
            return this;
        }
        
        public int getIconWidth() { return IconWidth; }
        public int getIconHeight() { return IconHeight; }
        
        public void paintIcon(Component c, Graphics g, int x, int y) {
            //Log.debug("x="+x+", y="+y);
            
            ((java.awt.Graphics2D)g).setRenderingHint
                (java.awt.RenderingHints.KEY_ANTIALIASING,
                 java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            
            node.drawFit(new DrawContext(g.create(), node),
                         IconSize,
                         0);
            //node.drawFit(g, x, y);
        }
    
    }
    
}
