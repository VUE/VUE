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
import tufts.vue.DEBUG;
import tufts.vue.LWComponent;
import static tufts.vue.LWComponent.Flag;
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
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.tree.*;

/**
 *
 * @version $Revision: 1.11 $ / $Date: 2008-10-09 22:13:08 $ / $Author: sfraize $
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

    public static JComponent create(Schema s) {
        final DataTree tree = new DataTree(s);

        if (true) {
            return tree;
        } else {
            JPanel p = new JPanel(new BorderLayout());
            p.add(new JLabel(s.getSource().toString()), BorderLayout.NORTH);
            p.add(tree, BorderLayout.CENTER);
            return p;
        }
    }

    private DataTree(Schema schema) {

        this.schema = schema;

        setCellRenderer(new DataRenderer());

        setModel(new DefaultTreeModel(buildTree(schema), false));

        setRowHeight(0);
        setRootVisible(false);
        setShowsRootHandles(true);
        
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
                            VUE.getSelection().setSource(DataTree.this);
                            VUE.getSelection().setSelectionSourceFocal(null); // prevents from ever drawing through on map
                            VUE.getSelection().setTo(treeNode.getStyle());
                        }
                        //VUE.setActive(LWComponent.class, this, node.styleNode);
                    }
                }
            });

        
        
    }

    /** build the model and return the root node */
    private TreeNode buildTree(final Schema schema)
    {
        final DataNode template = new TemplateNode(schema, this);
        
        final DataNode root =
            new DataNode(null, null, "Data Set: " + schema.getName());
//             new DataNode(null, null,
//                          String.format("%s [%d %s]",
//                                        schema.getName(),
//                                        schema.getRowCount(),
//                                        "items"//isCSV ? "rows" : "items"));
        root.add(template);
        
        for (Field field : schema.getFields()) {
            
            DataNode fieldNode = new DataNode(field, this, null);
            root.add(fieldNode);
            
//             if (field.uniqueValueCount() == schema.getRowCount()) {
//                 //Log.debug("SKIPPING " + f);
//                 continue;
//             }

            final Set values = field.getValues();

            // could add all style nodes to the schema node to be put in an internal layer for
            // persistance: either that or store them with the datasources, which
            // probably makes more sense.
            
            if (values.size() > 1) {

                final Map<String,Integer> valueCounts = field.getValueMap();
                
                //-----------------------------------------------------------------------------
                // Add the enumerated values
                //-----------------------------------------------------------------------------
                
                for (Map.Entry<String,Integer> e : valueCounts.entrySet()) {
                    //Log.debug("ADDING " + o);
                    //fieldNode.add(new DefaultMutableTreeNode(e.getKey() + "/" + e.getValue()));

                    String bold = field.isPossibleKeyField() ? "" : "<b>";

                    fieldNode.add(new ValueNode(field, e.getKey(), String.format("<html>%s%s</b> (%s)", bold, e.getKey(), e.getValue())));
                    

//                     if (e.getValue() == 1) {
//                         fieldNode.add(new ValueNode(field, e.getKey(), String.format("<html>%s%s", bold, e.getKey())));
//                     } else {
//                         fieldNode.add(new ValueNode(field, e.getKey(), String.format("<html>%s%s</b> (%s)", bold, e.getKey(), e.getValue())));
//                     }
                }
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
                dragNode = makeValueNode(field, treeNode.getValue());
                //dragNode.setLabel(String.format(" %s: %s ", field.getName(), treeNode.value));
                //dragNode.setLabel(String.format(" %s ", field.getName());
            } else if (treeNode.isField()) {
//                 if (field.isPossibleKeyField())
//                     return;
                dragNode = new LWNode(String.format("  %d unique  \n  '%s'  \n  values  ",
                                                    field.uniqueValueCount(),
                                                    field.getName()));
                dragNode.setClientData(java.awt.datatransfer.DataFlavor.stringFlavor,
                                       " ${" + field.getName() + "}");
            } else {
                assert treeNode instanceof TemplateNode;
                final Schema schema = treeNode.getSchema();
                dragNode = new LWNode(String.format("  '%s'  \n  dataset  \n  (%d items)  ",
                                                    schema.getName(),
                                                    schema.getRowCount()
                                                    ));
            }
                         
            dragNode.copyStyle(treeNode.getStyle(), ~LWKey.Label.bit);
            //dragNode.setFillColor(null);
            //dragNode.setStrokeWidth(0);
            if (!treeNode.isValue()) {
                dragNode.mFontSize.setTo(24);
                dragNode.mFontStyle.setTo(java.awt.Font.BOLD);
//                 dragNode.setClientData(LWComponent.ListFactory.class,
//                                        new NodeProducer(treeNode));
            }
            dragNode.setClientData(LWComponent.ListFactory.class,
                                   new NodeProducer(treeNode));


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

//     // this type of node was only for intial prototype
//     private static LWComponent makeDataNodes(Schema schema, Field field)
//     {
        
//         Log.debug("PRODUCING KEY FIELD NODES " + field);
//         int i = 0;
//         for (DataRow row : schema.getRows()) {
//             n = new LWNode();
//             n.setClientData(Schema.class, schema);
//             n.getMetadataList().add(row.entries());
//             if (field != null) {
//                 final String value = row.getValue(field);
//                 n.setLabel(makeLabel(field, value));
//             } else {
//                 //n.setLabel(treeNode.getStyle().getLabel()); // applies initial style
//             }
//             nodes.add(n);
//             //Log.debug("setting meta-data for row " + (++i) + " [" + value + "]");
//             //                     for (Map.Entry<String,String> e : row.entries()) {
//             //                         // todo: this is slow: is updating UI components, setting cursors, etc, every time
//             //                         n.addMetaData(e.getKey(), e.getValue());
//             //                     }
//         }
//         Log.debug("PRODUCED META-DATA IN " + field);

//     }
    
    private static String makeLabel(Field f, Object value) {

        return value.toString();
        
//         //return String.format("%s:\n%s", f.getName(), value.toString());
//         if (f.isKeyField())
//             return value.toString();
//         else
//             return value.toString() + "  ";
//         //return "  " + value.toString() + "  ";
    }

    private static LWComponent makeValueNode(Field field, String value) {
        
        LWComponent node = new LWNode(makeLabel(field, value));
        node.addMetaData(field.getName(), value);
        node.setClientData(Field.class, field);
        if (field.getStyleNode() != null)
            node.setStyle(field.getStyleNode());
//         else
//             tufts.vue.EditorManager.targetAndApplyCurrentProperties(node);
        return node;

    }

//     private static LWComponent makeDataNode(Schema schema)
//     {
//         int i = 0;
//         LWNode node;
//         for (DataRow row : schema.getRows()) {
//             node = new LWNode();
//             node.setClientData(Schema.class, schema);
//             node.getMetadataList().add(row.entries());
//             node.setStyle(schema.getStyleNode()); // must have meta-data set first to pick up label template
            
//             nodes.add(n);
//             //Log.debug("setting meta-data for row " + (++i) + " [" + value + "]");
//             //                     for (Map.Entry<String,String> e : row.entries()) {
//             //                         // todo: this is slow: is updating UI components, setting cursors, etc, every time
//             //                         n.addMetaData(e.getKey(), e.getValue());
//             //                     }
//         }
//         Log.debug("PRODUCED META-DATA IN " + field);

//     }

    private static List<LWComponent> makeAllDataNodes(Schema schema)
    {
        final java.util.List<LWComponent> nodes = new ArrayList();

        final Field linkField = schema.getField("link");
        final Field descField = schema.getField("description");
        final Field titleField = schema.getField("title");
        
        Log.debug("PRODUCING ALL DATA NODES FOR " + schema);
        int i = 0;
        LWNode node;
        for (DataRow row : schema.getRows()) {
            
            node = LWNode.createRaw();
            node.setClientData(Schema.class, schema);
            node.getMetadataList().add(row.entries());
            node.setStyle(schema.getStyleNode()); // must have meta-data set first to pick up label template

            if (linkField != null) {
                node.setResource(row.getValue(linkField));
                if (descField != null)
                    node.getResource().setProperty("Description", row.getValue(descField));
                if (titleField != null)
                    node.getResource().setTitle(row.getValue(titleField));
            }
            
            
            nodes.add(node);
        }
        Log.debug("PRODUCED ALL DATA NODES FOR " + schema);
        
        return nodes;
    }
    
    

    
    private static class NodeProducer implements LWComponent.ListFactory {

        private final DataNode treeNode;

        NodeProducer(DataNode n) {
            treeNode = n;
        }

        public java.util.List<LWComponent> produceNodes() {
            
            Log.debug("PRODUCING NODES FOR " + treeNode.field);
            
            final Field field = treeNode.field;
            final Schema schema = treeNode.getSchema();
            final java.util.List<LWComponent> nodes;

            LWNode n = null;

            if (treeNode.isSchematic()) {

                nodes = makeAllDataNodes(schema);
                
            } else if (treeNode.isValue()) {
                
                // is a single value from a column
                nodes = Collections.singletonList(makeValueNode(field, treeNode.getValue()));
                    
            } else {

                nodes = new ArrayList();

                // handle all the enumerated values for a column
                
                for (String value : field.getValues())
                    nodes.add(makeValueNode(field, value));
            }

            ///Actions.MakeCircle.actUpon(nodes);
            
            final java.util.List<LWComponent> links = new ArrayList();
            for (LWComponent c : nodes) {
                links.addAll(makeLinks(c, field));
            }
            //nodes.addAll(links);

            if (nodes.size() > 1)
                tufts.vue.LayoutAction.table.act(nodes);
            //Actions.MakeColumn.act(nodes);
            //Actions.MakeCircle.actUpon(nodes);
            
            //for (LWComponent c : nodes)c.setToNaturalSize();
            // todo: some problem editing template values: auto-size not being handled on label length shrinkage

            if (links.size() > 0)
                VUE.getActiveMap().getInternalLayer("*Data Links*").addChildren(links);

            return nodes;
        }


        List<LWLink> makeLinks(LWComponent node, Field field) {

            final LWMap map = VUE.getActiveMap(); // hack;
            //final Schema schema = field.getSchema();
            final VueMetadataElement vme;

            //if (node.hasClientData(Field.class))
            if (field != null)
                vme = node.getMetadataList().get(field.getName());
            else
                vme = null;
            //final String key = vme.getValue();

            //Log.debug(Util.tags(vme));

            final List<LWLink> links = new ArrayList();

            final edu.tufts.vue.metadata.MetadataList metaData = node.getMetadataList();

            for (LWComponent c : map.getAllDescendents()) {
                if (c == node)
                    continue;
//                 if (f == null)
//                     continue;
//                 Schema s = f.getSchema();
                
                // TODO: don't want to check all meta-data: just check the FIELD meta-data for the new node
                // (against all meta-data in other nodes)

                if (vme == null) {

                    // check our schema-node against only field nodes

                    if (c.hasClientData(Schema.class))
                        continue;

                    // really, want to get the single, special field item from the the
                    // currently inspecting node, and see if the current schema node
                    // has the same piece of data (key/value pair)
                    if (metaData.intersects(c.getMetadataList()))
                        links.add(makeLink(node, c, false));
                    

                } else if (c.getMetadataList().contains(vme)) {
                    // check our field node against all schema and field nodes
                    
                    final Field f = c.getClientData(Field.class);
                    //links.add(makeLink(node, c, false));
                    links.add(makeLink(node, c, f == field));
                }
                    
            }
            
            return links;
        }
        
    }

    private static LWLink makeLink(LWComponent src, LWComponent dest, boolean sameField) {
        LWLink link = new LWLink(src, dest);
        link.setArrowState(0);
        if (sameField) {
            link.mStrokeStyle.setTo(LWComponent.StrokeStyle.DASH3);
            link.setStrokeWidth(3);
        }
        return link;
    }

    public void LWCChanged(tufts.vue.LWCEvent e) {
        repaint();
    }


    private static String makeFieldLabel(final Field field)
    {
    
        final Set values = field.getValues();
        //Log.debug("EXPANDING " + colNode);

        //LWComponent schemaNode = new LWNode(schema.getName() + ": " + schema.getSource());
        // add all style nodes to the schema node to be put in an internal layer for
        // persistance: either that or store them with the datasources, which
        // probably makes more sense.

        String label = field.toString();
            
        if (values.size() == 0) {

            if (field.getMaxValueLength() == 0) {
                label = String.format("<html><b><font color=gray>%s", field.getName());
            } else {
                label = String.format("<html><b>%s (max size: %d bytes)",
                                      field.getName(), field.getMaxValueLength());
            }
        } else if (values.size() == 1) {
                
            label = String.format("<html><b>%s: <font color=green>%s",
                                  field.getName(), field.getValues().toArray()[0]);

        } else if (values.size() > 1) {

            final Map<String,Integer> valueCounts = field.getValueMap();
                
//             if (field.isPossibleKeyField())
//                 label = String.format("<html><i><b>%s</b> (%d)", field.getName(), field.uniqueValueCount());
//             else
                label = String.format("<html><b>%s</b> (%d)", field.getName(), field.uniqueValueCount());

        }

        return label;
    }

    //private static final Color[] DataColors = tufts.vue.VueResources.getColorArray("dataColorValues");
    private static final Color[] DataColors = tufts.vue.VueResources.getColorArray("fillColorValues");
    private static final int FirstRotationColor = 22;
    private static final int SecondRotationColor = 18;
    private static int NextColor = FirstRotationColor;
    private static boolean FirstRotation = true;

    private static LWComponent createStyleNode(final Field field, LWComponent.Listener repainter)
    {
        final LWComponent style;

        if (field.isPossibleKeyField()) {

            style = new LWNode(); // creates a rectangular node
            //style.setLabel(" ---");
            style.setFillColor(Color.gray);
            style.setFont(DataFont);
        } else {
            //style = new LWNode(" ---"); // creates a round-rect node
            style = new LWNode(""); // creates a round-rect node
            //style.setFillColor(Color.blue);
            style.setFillColor(DataColors[NextColor]);
//             if (++NextColor >= DataColors.length)
//                 NextColor = 0;
            NextColor += 8;
            if (NextColor >= DataColors.length) {
                if (FirstRotation) {
                    NextColor = SecondRotationColor;
                    FirstRotation = false;
                } else {
                    NextColor = FirstRotationColor;
                    FirstRotation = true;
                }
            }
            style.setFont(EnumFont);
        }
        style.setFlag(Flag.INTERNAL);
        style.setFlag(Flag.DATA_STYLE); // must set before setting label, or template will atttempt to resolve
        //style.setLabel(String.format("%.9s: \n${%s} ", field.getName(),field.getName()));
        style.setLabel(String.format("${%s}", field.getName()));
        style.setNotes(String.format
                       ("Style node for field '%s' in data-set '%s'\n\nSource: %s\n\n%s\n\nvalues=%d; unique=%d; type=%s",
                        field.getName(),
                        field.getSchema().getName(),
                        field.getSchema().getSource(),
                        field.valuesDebug(),
                        field.valueCount(),
                        field.uniqueValueCount(),
                        field.getType()
                       ));
        style.setTextColor(Color.white);
        //style.disableProperty(LWKey.Label);
        style.addLWCListener(repainter);
        style.setFlag(Flag.STYLE); // set last to creation property sets don't attempt updates

        return style;
    }



        



//     public static final java.awt.datatransfer.DataFlavor DataFlavor =
//         tufts.vue.gui.GUI.makeDataFlavor(DataNode.class);

    private static final Font EnumFont = new Font("SansSerif", Font.BOLD, 24);
    private static final Font DataFont = new Font("SansSerif", Font.PLAIN, 12);
        

    private static class DataNode extends DefaultMutableTreeNode {

        final Field field;
        
        DataNode(Field field, LWComponent.Listener repainter, String description) {
            this.field = field;

            if (description == null) {
                if (field != null)
                    setDisplay(makeFieldLabel(field));
            } else
                setDisplay(description);

            //if (field != null && field.isEnumerated() && !field.isPossibleKeyField())
            if (field != null && !field.isUniqueValue() && field.isEnumerated())
                field.setStyleNode(createStyleNode(field, repainter));
        }
        
//         DataNode(String description) {
//             field = null;
//             setDisplay(description);
//         }

        protected DataNode(Field field) {
            this.field = field;
        }

        Schema getSchema() {
            return field.getSchema();
        }

        String getValue() {
            return null;
        }

        void setDisplay(String s) {
            setUserObject(s);  // sets display label
        }

        LWComponent getStyle() {
            return field == null ? null : field.getStyleNode();
        }

        boolean hasStyle() {
            //return isField();
            return field != null && field.getStyleNode() != null;
        }

        boolean isField() {
            return field != null;
            //return value == null && field != null;
        }
        
        boolean isValue() {
            //return value != null;
            return !isField();
        }

        boolean isSchematic() {
            return field == null;
        }

    }

    private static final class ValueNode extends DataNode {

        String value;

        ValueNode(Field field, String value, String label) {
            super(field);
            setDisplay(label);
            this.value = value;
        }
        
        String getValue() {
            return value;
        }
        @Override
        public boolean isField() { return false; }
        @Override
        public boolean hasStyle() { return false; }
//         @Override
//         public LWComponent getStyle() { return null; }
    }

    private static final class TemplateNode extends DataNode {

        Schema schema;

        TemplateNode(Schema schema, LWComponent.Listener repainter) {
            super(null,
                  repainter,
                  //String.format("<html><b>All Data Nodes in '%s' (%d)", schema.getName(), schema.getRowCount()));
                  String.format("<html><b><font color=red>All Data Nodes in '%s' (%d)", schema.getName(), schema.getRowCount()));
            this.schema = schema;
            LWComponent style = new LWNode();
            style.setFlag(Flag.INTERNAL);
            String fmt = "";
            Field firstField = null;
            for (Field field : schema.getFields()) {
                if (firstField == null)
                    firstField = field;
                if (field.isPossibleKeyField()) {
                    fmt = "${" + field.getName() + "}";
                    break; // only take first key field found for now
//                     if (fmt.length() > 0)
//                         fmt += "\n";
//                     fmt += String.format("%s: ${%s}", f.getName(), f.getName());
                }
            }
            style.setFlag(Flag.DATA_STYLE);
            if (fmt.length() > 0)
                style.setLabel(fmt);
            else
                style.setLabel("${" + firstField.getName() + "}");
            style.setFont(DataFont);
            style.setTextColor(Color.white);
            style.setFillColor(Color.darkGray);
            style.setStrokeWidth(0);
            //style.disableProperty(LWKey.Notes);
            style.setNotes("Style for all " + schema.getRowCount() + " data items in " + schema.getName()
                           + "\n\nSchema: " + schema.getDump());
            style.setFlag(Flag.STYLE);

            schema.setStyleNode(style);
        }
        
        @Override
        Schema getSchema() {
            return schema;
        }
        @Override
        boolean isField() { return false; }
        @Override
        boolean isValue() { return false; }
        @Override
        boolean hasStyle() { return true; }
        @Override
        LWComponent getStyle() { return schema.getStyleNode(); }
    }

    private static final int IconWidth = 32;
    private static final int IconHeight = 20;

    //private static final Border TopBorder = BorderFactory.createLineBorder(Color.gray);
    private static final Border TopBorder = new CompoundBorder(new MatteBorder(3,0,3,0, Color.white),
                                                               new CompoundBorder(new LineBorder(Color.gray),
                                                                                  GUI.makeSpace(1,0,1,2)));

    private static final Border TopTierBorder = GUI.makeSpace(0,0,2,0);
    private static final Border LeafBorder = GUI.makeSpace(0,IconWidth-16,2,0);

    private static class DataRenderer extends DefaultTreeCellRenderer {

        {
            //setIconTextGap(2);
            //setBorder(LeafBorder);
            setVerticalTextPosition(SwingConstants.CENTER);
            //setTextNonSelectionColor(Color.black);
        }

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
            
//             if (node.isField() && !leaf) {
//                 if (node.field.isPossibleKeyField())
//                     //setForeground(Color.red);
//                     setForeground(Color.black);
//                 else
//                     setForeground(Color.blue);
//             } else {
//                 setForeground(Color.black);
//             }

            setForeground(Color.black); // must do every time for some reason, or de-selected text goes invisible
            
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

            if (node.hasStyle()) {
                //setIconTextGap(4);
                setIcon(FieldIconPainter.load(node.getStyle(),
                                              selected ? backgroundSelectionColor : null));
            } else {
                
//                 if (node.isValue() || node.isField() && node.field.isSingleton()) {
//                     setIconTextGap(4);
//                 } else {
//                     // this will alingn non-styled (non-enumerated) fields, that
//                     // are part of the data-set, but not tracked because they're too long
//                     setIconTextGap(IconWidth - 16 + 4);
//                 }
                
//                 if (leaf && node.isValue())
//                     setIcon(EmptyIcon);
            }

            if (row == 0) {
                //setBorder(null);
                setBorder(TopBorder);
                //setBackgroundNonSelectionColor(Color.lightGray);
                //setFont(EnumFont);
            } else {
                //setBackgroundNonSelectionColor(null);
                //setFont(null);
                //setBorder(leaf ? LeafBorder : null);
                if (leaf) {
                    if (node.isField() && node.field.isSingleton())
                        setBorder(TopTierBorder);
                    else
                        setBorder(LeafBorder);
                } else {
                    setBorder(null);
                }
            }
            
            return this;
        }
    }

    private static final java.awt.geom.Rectangle2D IconSize
        = new java.awt.geom.Rectangle2D.Float(0,0,IconWidth,IconHeight);
    
    private static final NodeIconPainter FieldIconPainter = new NodeIconPainter();

    private static final Icon EmptyIcon = new GUI.EmptyIcon(IconWidth, IconHeight);
    
    
    private static class NodeIconPainter implements Icon {

        LWComponent node;
        Color fill;

//         NodeIcon(LWComponent c) {
//             node = c;
//         }

        public Icon load(LWComponent c, Color fill) {
            this.node = c;
            this.fill = fill;
            return this;
        }
        
        public int getIconWidth() { return IconWidth; }
        public int getIconHeight() { return IconHeight; }
        
        public void paintIcon(Component c, Graphics g, int x, int y) {
            //Log.debug("x="+x+", y="+y);
            
            ((java.awt.Graphics2D)g).setRenderingHint
                (java.awt.RenderingHints.KEY_ANTIALIASING,
                 java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

            if (fill != null) {
                if (DEBUG.BOXES) {
                    g.setColor(Color.red);
                    g.fillRect(x,y,IconWidth,IconHeight);
                } else {
                    g.setColor(fill);
                    // add to width to also fill the IconTextGap
                    g.fillRect(0,0,IconWidth+8,IconHeight+8);
                }
            }
            
            node.drawFit(new DrawContext(g.create(), node),
                         IconSize,
                         2);
            //node.drawFit(g, x, y);
        }
    
    }
    
}
