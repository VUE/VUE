package tufts.vue.ds;

import tufts.Util;
import tufts.vue.DEBUG;
import tufts.vue.LWMap;
import tufts.vue.LWComponent;
import tufts.vue.LWLink;
import tufts.vue.LWNode;
import tufts.vue.VueResources;
import tufts.vue.Resource;

import static tufts.vue.LWComponent.Flag;

import edu.tufts.vue.metadata.VueMetadataElement;

import java.awt.Color;
import java.awt.Font;

import java.util.*;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * @version $Revision: 1.13 $ / $Date: 2009-05-13 17:16:17 $ / $Author: sfraize $
 * @author  Scott Fraize
 */

public final class DataAction
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(DataAction.class);

    /** annotate the given schema (note what is present and what isn't) based on the data nodes found in the given map */
    public static void annotateForMap(final Schema schema, final LWMap map)
    {
        //if (DEBUG.Enabled) Log.debug("ANNOTATING for " + map + "; " + schema);

        final Collection<LWComponent> allDataNodes;

        if (map == null) {
            // if map is null, we annotate with an empty list, which will clear all annotation data
            // (everything in the schema will be marked as newly present)
            allDataNodes = Collections.EMPTY_LIST;
        } else {
            final Collection<LWComponent> allNodes = map.getAllDescendents();
            allDataNodes = new ArrayList(allNodes.size());
            for (LWComponent c : allNodes)
                if (c.isDataNode())
                    allDataNodes.add(c);
        }

        schema.annotateFor(allDataNodes);
    }
    

    public static String valueName(Object value) {
        return StringEscapeUtils.escapeHtml(Field.valueName(value));
    }
    
    private static List<LWLink> makeDataLinksForNodes(LWMap map, List<? extends LWComponent> nodes, Field field)
    {
        final Collection linkTargets = getLinkTargets(map);
        
        java.util.List<LWLink> links = Collections.EMPTY_LIST;
        
        if (linkTargets.size() > 0) {
            links = new ArrayList();
            for (LWComponent c : nodes) {
                links.addAll(makeLinks(linkTargets, c, field));
            }
        }

        return links;
    }
    
    private static List<LWLink> makeDataLinksForNode(final LWComponent node, final Collection<? extends LWComponent> linkTargets)
    {
        if (linkTargets.size() > 0) {
            return makeLinks(linkTargets, node, node.getDataValueField()); // seems wrong to be providing this last argumen
        } else {
            return Collections.EMPTY_LIST;
        }

    }

    public static List<? extends LWComponent> getLinkTargets(LWMap map) {
        return Util.extractType(map.getAllDescendents(), LWNode.class);
    }

    // TODO: REASON TO PASS ALWAYS PASS IN MAP ARGUMENT: We may want to create data links before the nodes
    // have actually been added to the map, in which case node.getMap() will be returning null.

//     public static boolean addDataLinksForNode(LWComponent node, Collection<? extends LWComponent> linkTargets) {
//         return addDataLinksForNode(node.getMap(), node, linkTargets);
//     }
    
//     public static boolean addDataLinksForNode(final LWMap map,
//                                               final LWComponent node,
//                                               final Collection<? extends LWComponent> linkTargets)
//     {
//         List<LWLink> links = makeDataLinksForNode(node, linkTargets);

//         if (links.size() > 0) {
//             map.getInternalLayer("*Data Links*").addChildren(links);
//             return true;
//         } else
//             return false;
//     }

    /** @field -- if null, will make exaustive row-node links */
    public static boolean addDataLinksForNodes(LWMap map, List<? extends LWComponent> nodes, Field field) {
        
        List<LWLink> links = makeDataLinksForNodes(map, nodes, field);

        if (links.size() > 0) {
            map.getInternalLayer("*Data Links*").addChildren(links);
            return true;
        } else
            return false;
    }

    private static List<LWLink> makeLinks(LWComponent node)
    {
        if (node.isDataRowNode())
            return makeRowNodeLinks(getLinkTargets(node.getMap()), node);
        else if (node.isDataValueNode())
            return makeValueNodeLinks(getLinkTargets(node.getMap()), node, node.getDataValueField());
        else
            return Collections.EMPTY_LIST;
    }
    
    private static List<LWLink> makeLinks(LWComponent node, Collection<LWComponent> linkTargets) {

        if (node.isDataRowNode())
            return makeRowNodeLinks(linkTargets, node);
        else if (node.isDataValueNode())
            return makeValueNodeLinks(linkTargets, node, node.getDataValueField());
        else
            return Collections.EMPTY_LIST;
    }
        
    /** @param field -- if null, will defer to makeRowNodeLinks, and assume the given node is a row node */
    public static List<LWLink> makeLinks(final Collection<? extends LWComponent> linkTargets, LWComponent node, Field field)
    {
        //Log.debug("makeLinks: " + field + "; " + node);

        if (field == null)
            return makeRowNodeLinks(linkTargets, node);
        else
            return makeValueNodeLinks(linkTargets, node, field);
    }


    public static List<LWLink> makeValueNodeLinks(final Collection<? extends LWComponent> linkTargets, LWComponent node, Field field)
    {
        final List<LWLink> links = Util.skipNullsArrayList();

        //final edu.tufts.vue.metadata.MetadataList metaData = node.getMetadataList();

        final String fieldName = field.getName();
        final String fieldValue = node.getDataValue(fieldName);
        //final String label = String.format("%s=%s", fieldName, fieldValue);
        //final String label = String.format("DataLink: %s='%s'", fieldName, fieldValue);

        for (LWComponent c : linkTargets) {
            if (c == node)
                continue;

            if (c.hasDataValue(fieldName, fieldValue)) {
                // if the target node c is schematic at all, it should only have
                // one piece of meta-data, and it should be an exact match already
                //boolean sameField = fieldName.equals(c.getSchematicFieldName());
                final boolean sameField = c.isDataValueNode();
                links.add(makeLink(node, c, fieldName, fieldValue, sameField ? Color.red : null));
            }
                
        }
            
        return links;
    }

    /** make links from row nodes (full data nodes) to any schematic field nodes found in the link targets,
     or between row nodes from different schema's that are considered "auto-joined" (e.g., a matching key field appears) */
    public static List<LWLink> makeRowNodeLinks(final Collection<? extends LWComponent> linkTargets, final LWComponent rowNode)
    {
        if (!rowNode.isDataRowNode())
            Log.warn("making row links to non-row node: " + rowNode, new Throwable("FYI"));
        
        final List<LWLink> links = Util.skipNullsArrayList();

        final Schema sourceSchema = rowNode.getDataSchema();
        final String sourceKeyField = sourceSchema.getKeyFieldName();
        final String sourceKeyValue = rowNode.getDataValue(sourceKeyField);
        
        for (LWComponent c : linkTargets) {

            if (c == rowNode) // never link to ourself
                continue;

            try {
            
                final Schema schema = c.getDataSchema();
            
                if (schema != null && sourceSchema != schema) {

                    //-----------------------------------------------------------------------------
                    // from different schemas: can do a join-based linking -- just try key field for now
                    //-----------------------------------------------------------------------------

                    if (c.hasDataValue(sourceKeyField, sourceKeyValue)) {
                        links.add(makeLink(c, rowNode, sourceKeyField, sourceKeyValue, Color.blue));
                    
                    } else {

                        // this is the semantic reverse of the above case
                    
                        final String targetKeyField = schema.getKeyFieldName();
                        final String targetKeyValue = c.getDataValue(targetKeyField);
                        if (rowNode.hasDataValue(targetKeyField, targetKeyValue)) {
                            links.add(makeLink(rowNode, c, targetKeyField, targetKeyValue, Color.blue));
                        }
                    }

                } else {

                    final String fieldName = c.getDataValueFieldName();
                
                    if (fieldName == null) // fieldName will be null if c isn't a data value node / has no schema
                        continue;
                
                    final String fieldValue = c.getDataValue(fieldName);
                
                    if (rowNode.hasDataValue(fieldName, fieldValue)) {
                        //final String label = String.format("RowLink: %s='%s'", fieldName, fieldValue);
                        //final String label = String.format("%s=%s", fieldName, fieldValue);
                        links.add(makeLink(c, rowNode, fieldName, fieldValue, null));
                    }
                }
            } catch (Throwable t) {
                Log.warn(t + "; processing target: " + c.getUniqueComponentTypeLabel());
            }
        }

        
        return links;
    }
        
//     /** make links from row nodes (full data nodes) to any schematic field nodes found in the link targets */
//     private static List<LWLink> makeRowNodeLinks(final Collection<LWComponent> linkTargets, LWComponent rowNode)
//     {
//         final List<LWLink> links = Util.skipNullsArrayList();
        
//         for (LWComponent c : linkTargets) {
//             if (c == rowNode)
//                 continue;

//             final String fieldName = c.getDataValueFieldName();

//             if (fieldName == null) {
//                 // fieldName will be null if c isn't a schematic field
//                 continue;
//             }

//             final String fieldValue = c.getDataValue(fieldName);
            
//             if (rowNode.hasDataValue(fieldName, fieldValue)) {
//                 //final String label = String.format("RowLink: %s='%s'", fieldName, fieldValue);
//                 //final String label = String.format("%s=%s", fieldName, fieldValue);
//                 links.add(makeLink(c, rowNode, fieldName, fieldValue, false));
//             }
                
//         }
        
//         return links;
//     }
    
    private static LWLink makeLink(LWComponent src,
                                   LWComponent dest,
                                   String fieldName,
                                   String fieldValue,
                                   Color specialColor)
    {
        if (src.hasLinkTo(dest)) {
            // don't create a link if there already is one of any kind
            return null;
        }
        
        LWLink link = new LWLink(src, dest);
        link.setArrowState(0);
        link.setStrokeColor(java.awt.Color.lightGray);
        if (specialColor != null) {
            link.mStrokeStyle.setTo(LWComponent.StrokeStyle.DASH3);
            link.setStrokeWidth(3);
            link.setStrokeColor(specialColor);
        }

        final String relationship = String.format("%s=%s", fieldName, fieldValue);
        link.setAsDataLink(relationship);

        return link;

    }


    private static String makeLabel(Field f, Object value) {
        //Log.debug("*** makeLabel " + f + " [" + value + "] emptyValue=" + (value == Field.EMPTY_VALUE));

// This will be overriden by the label-style: this could would need to go there to work
//         if (value == Field.EMPTY_VALUE)
//             return String.format("(no [%s] value)", f.getName());
//         else
            return Field.valueName(value);
    }

    public static LWComponent makeValueNode(Field field, String value) {
        
        LWComponent node = new LWNode(makeLabel(field, value));
        //node.setDataInstanceValue(field.getName(), value);
        node.setDataInstanceValue(field, value);
        //node.setClientData(Field.class, field);
        if (field.getStyleNode() != null)
            node.setStyle(field.getStyleNode());
//         else
//             tufts.vue.EditorManager.targetAndApplyCurrentProperties(node);
        String target = node.getLabel();
        
        target = Util.formatLines(target, VueResources.getInt("dataNode.labelLength"));
        
        node.setLabel(target);
        return node;

    }

    public static List<LWComponent> makeRowNodes(Schema schema, DataRow singleRow) {

        Log.debug("PRODUCING SINGLE ROW NODE FOR " + schema + "; row=" + singleRow);

        List<DataRow> rows = Collections.singletonList(singleRow);

        return makeRowNodes(schema, rows);
        
    }
    
    public static List<LWComponent> makeRowNodes(Schema schema) {

        Log.debug("PRODUCING ALL DATA NODES FOR " + schema + "; rowCount=" + schema.getRows().size());

        return makeRowNodes(schema, schema.getRows());
        
    }

    public static List<LWComponent> makeRowNodes(Schema schema, final Collection<DataRow> rows)
    {
        final java.util.List<LWComponent> nodes = new ArrayList();

        // TODO: findField should find case-independed values -- wasn't our key hack supposed to handle that?

        final Field linkField = schema.findField("Link");
        final Field descField = schema.findField("Description");
        final Field titleField = schema.findField("Title");
        //final Field mediaField = schema.findField("media:group.media:content.media:url");
        final Field mediaField = schema.findField("media:content@url");
        final Field imageField = schema.getImageField();
        
        final int maxLabelLineLength = VueResources.getInt("dataNode.labelLength", 50);
        
//         final Collection<DataRow> rows;

//         if (singleRow != null) {
//             Log.debug("PRODUCING SINGLE ROW NODE FOR " + schema + "; row=" + singleRow);
//             rows = Collections.singletonList(singleRow);
//         } else {
//             Log.debug("PRODUCING ALL DATA NODES FOR " + schema + "; rowCount=" + schema.getRows().size());
//             rows = schema.getRows();
//         }

        Log.debug("PRODUCING ROW NODE(S) FOR " + schema + "; " + Util.tags(rows));
        Log.debug("IMAGE FIELD: " + imageField);

        final boolean singleRow = (rows.size() == 1);
        
        int i = 0;
        LWNode node;
        for (DataRow row : rows) {

            try {
            
                node = LWNode.createRaw();
                // node.setFlag(Flag.EVENT_SILENT); // todo performance: have nodes do this by default during init
                //node.setClientData(Schema.class, schema);
                //node.getMetadataList().add(row.entries());
                //node.addDataValues(row.dataEntries());
                node.takeAllDataValues(row.getData());
                node.setStyle(schema.getRowNodeStyle()); // must have meta-data set first to pick up label template

//                 if (singleRow) {
//                     // if handling a single node (e.g., probably a single drag),
//                     // also apply & override with the current on-map creation style
//                     tufts.vue.EditorManager.targetAndApplyCurrentProperties(node);
//                 }

                boolean addedResource = false;
            
                if (imageField != null) {
                    final String image = row.getValue(imageField);
                    if (image != null && image.length() > 0 && Resource.looksLikeURLorFile(image) && !image.equals("n/a")) {
                        // todo: note "n/a" hack above
                        Resource ir = Resource.instance(image);
                        
                        if (titleField != null) {
                            String title = row.getValue(titleField);
                            ir.setTitle(title);
                            ir.setProperty("Title", title);
                        }
                        
                        Log.debug("image resource: " + ir);
                        node.addChild(new tufts.vue.LWImage(ir));
                        addedResource = true;
                    }
                }
                String link = null;

                if (!addedResource && linkField != null) {
                    link = row.getValue(linkField);
                    if ("n/a".equals(link)) link = null; // TEMP HACK
                }
                
                if (link != null) {
                    node.setResource(link);
                    final tufts.vue.Resource r = node.getResource();
                    //                 if (descField != null) // now redundant with data fields, may want to leave out for brevity
                    //                     r.setProperty("Description", row.getValue(descField));
                    if (titleField != null) {
                        String title = row.getValue(titleField);
                        r.setTitle(title);
                        r.setProperty("Title", title);
                    }
                    if (mediaField != null) {
                        // todo: if no per-item media field, use any per-schema media field found
                        // (e.g., RSS content provider icon image)
                        // todo: refactor so cast not required
                        ((tufts.vue.URLResource)r).setURL_Thumb(row.getValue(mediaField));
                    }
                }

                //Log.debug("produced node " + node);
                String label = node.getLabel();

                label = Util.formatLines(label, maxLabelLineLength);
                node.setLabel(label);
                nodes.add(node);
            } catch (Throwable t) {
                Log.error("failed to create node for row " + row, t);
            }
            
            
        }
        
        Log.debug("PRODUCED NODE(S) FOR " + schema + "; count=" + nodes.size());
        
        return nodes;
    }

    private static final Color DataNodeColor = VueResources.getColor("node.data.color", Color.gray);
    private static final float DataNodeStrokeWidth = VueResources.getInt("node.data.stroke.width", 0);
    private static final Color DataNodeStrokeColor = VueResources.getColor("node.data.stroke.color", Color.black);
    private static final Font DataNodeFont = VueResources.getFont("node.data.font");

    private static final Color ValueNodeTextColor = VueResources.getColor("node.dataValue.text.color", Color.black);
    private static final Font ValueNodeFont = VueResources.getFont("node.dataValue.font");
    private static final Color[] ValueNodeDataColors = VueResources.getColorArray("node.dataValue.color.cycle");
    private static int NextColor = 0;

//     private static final Color[] DataColors = VueResources.getColorArray("fillColorValues");
//     private static final int FirstRotationColor = 22;
//     private static final int SecondRotationColor = 18;
//     private static int NextColor = FirstRotationColor;
//     private static boolean FirstRotation = true;
    
//     private static final Font ValueNodeFont = new Font("SansSerif", Font.BOLD, 24);
//     private static final Font DataNodeFont = new Font("SansSerif", Font.PLAIN, 12);


    private static LWComponent initStyleNode(LWComponent style) {
        style.setFlag(Flag.INTERNAL);
        style.setFlag(Flag.DATA_STYLE); // must set before setting label, or template will atttempt to resolve
        style.setID(style.getURI().toString());
        return style;
    }
    
    /** @return an empty styling node (appearance values to be set elsewhere) */ 
    public static LWComponent makeStyleNode() {
        return initStyleNode(new LWNode());
    }

    /** @return a row-styling node for the given schema */
    public static LWComponent makeStyleNode(Schema schema)
    {
        final LWComponent style = makeStyleNode();

        String titleField;
            
        // Make a guess at what might be the best field to use for the node label text
        if (schema.getRowCount() <= 42 && schema.hasField("title")) {
            // if we have hundreds of nodes, title may be too long to use -- the key
            // field may well be shorter.
            titleField = "title";
        } else {
            titleField = schema.getKeyFieldGuess().getName();
        }
            
        style.setLabel(String.format("${%s}", titleField));
            
        style.setFont(DataNodeFont);
        style.setTextColor(Color.black);
        style.setFillColor(DataNodeColor);
        style.setStrokeWidth(DataNodeStrokeWidth);
        style.setStrokeColor(DataNodeStrokeColor);
        //style.disableProperty(LWKey.Notes);
        //String notes = String.format("Style for all %d data items in %s",
        String notes = String.format("Style for row nodes in Schema '%s'",
                                     schema.getName());

        //if (DEBUG.Enabled) notes += ("\n\nSchema: " + schema.getDump());
        style.setNotes(notes);
        style.setFlag(Flag.STYLE); // do last

        return style;
    }

    public static LWComponent makeStyleNode(final Field field) {
        return makeStyleNode(field, null);
    }
    public static LWComponent makeStyleNode(final Field field, LWComponent.Listener repainter)
    {
        final LWComponent style;

        if (field.isPossibleKeyField()) {

            style = new LWNode(); // creates a rectangular node
            //style.setLabel(" ---");
            style.setFillColor(Color.lightGray);
            style.setFont(DataNodeFont);
        } else {
            //style = new LWNode(" ---"); // creates a round-rect node
            style = new LWNode(""); // creates a round-rect node
            //style.setFillColor(Color.blue);
            style.setFillColor(ValueNodeDataColors[NextColor]);
            if (++NextColor >= ValueNodeDataColors.length)
                NextColor = 0;
//             NextColor += 8;
//             if (NextColor >= DataColors.length) {
//                 if (FirstRotation) {
//                     NextColor = SecondRotationColor;
//                     FirstRotation = false;
//                 } else {
//                     NextColor = FirstRotationColor;
//                     FirstRotation = true;
//                 }
//             }
            style.setFont(ValueNodeFont);
        }
        initStyleNode(style); // must set before setting label, or template will atttempt to resolve
        //style.setLabel(String.format("%.9s: \n${%s} ", field.getName(),field.getName()));
        style.setLabel(String.format("${%s}", field.getName()));
        style.setNotes(String.format
                       ("Style node for field '%s' in data-set '%s'\n\nSource: %s\n\n%s\n\nvalues=%d; unique=%d; type=%s",
                        field.getName(),
                        field.getSchema().getName(),
                        field.getSchema().getResource(),
                        field.valuesDebug(),
                        field.valueCount(),
                        field.uniqueValueCount(),
                        field.getType()
                       ));
        style.setTextColor(ValueNodeTextColor);
        //style.disableProperty(LWKey.Label);
        if (repainter != null)
            style.addLWCListener(repainter);
        style.setFlag(Flag.STYLE); // set last so creation property sets don't attempt updates
        
        return style;
    }

    


    

//     static List<LWLink> VMEmakeLinks(final Collection<LWComponent> linkTargets, LWComponent node, Field field)
//     {
//         //final Schema schema = field.getSchema();
//         final VueMetadataElement vme;

//         //if (node.hasClientData(Field.class))
//         if (field != null)
//             vme = node.getMetadataList().get(field.getName());
//         else
//             vme = null;
//         //final String key = vme.getValue();

//         //Log.debug(Util.tags(vme));

//         final List<LWLink> links = new ArrayList();

//         final edu.tufts.vue.metadata.MetadataList metaData = node.getMetadataList();

//         for (LWComponent c : linkTargets) {
//             if (c == node)
//                 continue;
//             //                 if (f == null)
//             //                     continue;
//             //                 Schema s = f.getSchema();
                
//             // TODO: don't want to check all meta-data: just check the FIELD meta-data for the new node
//             // (against all meta-data in other nodes)

//             if (vme == null) {

//                 // check our schema-node against only field nodes

//                 //                     // TODO: below needs to be persistent info, and if we try to add new data
//                 //                     // to a save file of one of our currently created maps, we'll link to everything.
//                 //                     if (c.hasClientData(Schema.class))
//                 //                         continue;

//                 if (c.isSchematicFieldNode())
//                     continue;

//                 // really, want to get the single, special field item from the the
//                 // currently inspecting node, and see if the current schema node
//                 // has the same piece of data (key/value pair)
//                 if (metaData.intersects(c.getMetadataList()))
//                     links.add(makeLink(node, c, null, false));
                    

//             } else if (c.getMetadataList().contains(vme)) {
//                 // check our field node against all schema and field nodes
                    
//                 final Field f = c.getClientData(Field.class);
//                 //links.add(makeLink(node, c, false));
//                 links.add(makeLink(node, c, null, f == field));
//             }
                    
//         }
            
//         return links;
//     }
    
}




