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

import tufts.vue.*;
import tufts.Util;

import java.util.*;
import java.io.*;
import java.util.List;
import java.util.*;
import java.awt.*;
import java.net.*;
import javax.swing.*;

import static tufts.vue.ds.XMLIngest.*;

import au.com.bytecode.opencsv.CSVReader;


/**
 * @version $Revision: 1.14 $ / $Date: 2009-02-23 02:38:07 $ / $Author: sfraize $
 * @author Scott Fraize
 */
public class XmlDataSource extends BrowseDataSource
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(XmlDataSource.class);
    
    private final List<Resource> mItems = new ArrayList();

    private static final String ITEM_PATH_KEY = "item_path";
    private static final String KEY_FIELD_KEY = "key_field";
    private static final String IMAGE_FIELD_KEY = "image_field";

    private static final String NONE_SELECTED = "(none selected)";

    

    private String itemKey;
    private String keyField;
    private String imageField = NONE_SELECTED;

    private boolean isCSV; // hack while XmlDataSource supports both XML and flat-files
    
    public XmlDataSource() {}
    
    public XmlDataSource(String displayName, String address) throws DataSourceException {
        this.setDisplayName(displayName);
        this.setAddress(address);
    }

    @Override
    public String getTypeName() {
        return "XML Feed";
    }

    @Override
    public int getCount() {
        return mItems == null ? -1 : mItems.size();
    }

    @Override public void setConfiguration(java.util.Properties p) {

        if (DEBUG.DR) Log.debug(this + " setConfiguration " + p);

        super.setConfiguration(p);
        
        String val = null;
        try {
            if ((val = p.getProperty(ITEM_PATH_KEY)) != null)
                setItemKey(val);
        } catch (Throwable t) {
            Log.error("val=" + val, t);
        }
        try {
            if ((val = p.getProperty(KEY_FIELD_KEY)) != null)
                setKeyField(val);
        } catch (Throwable t) {
            Log.error("val=" + val, t);
        }
        try {
            if ((val = p.getProperty(IMAGE_FIELD_KEY)) != null)
                setImageField(val);
        } catch (Throwable t) {
            Log.error("val=" + val, t);
        }
    }
    
    @Override
    protected JComponent buildResourceViewer() {
        return loadViewer();
    }

    public String getItemKey() {
        return itemKey == null ? "rss.channel.item" : itemKey;
    }

    public void setItemKey(String k) {
        itemKey = k;
        unloadViewer();
    }


    public String getImageField() {
        return imageField == NONE_SELECTED ? null : imageField;
    }

    public void setImageField(String name) {
        if (NONE_SELECTED.equals(name))
            imageField = NONE_SELECTED;
        else
            imageField = name;
        unloadViewer();
    }
    
    public String getKeyField() {
        return keyField;
    }

    public void setKeyField(String k) {
        if (DEBUG.DR) Log.debug("setKeyField[" + k + "]");
        if (k != null) {
            k = k.trim();
            if (k.length() < 1)
                keyField = null;
            else
                keyField = k;
        } else {
            keyField = null;
        }
        unloadViewer();
    }
    
    @Override public java.util.List<ConfigField> getConfigurationUIFields() {
        
        ConfigField path
            = new ConfigField(ITEM_PATH_KEY,
                              "Item Path",
                              "XML node path of interest",
                              getItemKey()); // current value is inserted here

        String keyFieldName = getKeyField();
        Vector possibleKeyFieldValues = null;

        if (mSchema != null) {
            if (keyFieldName == null)
                keyFieldName = mSchema.getKeyField().getName();
            possibleKeyFieldValues = mSchema.getPossibleKeyFieldNames();
        }

        ConfigField keyField
            = new ConfigField(KEY_FIELD_KEY
                              ,"Key Field"
                              ,"Field with a unique value for each item"
                              ,keyFieldName // current value
                              ,edu.tufts.vue.ui.ConfigurationUI.COMBO_BOX_CONTROL);
        keyField.values = possibleKeyFieldValues;

        ConfigField imageField
            = new ConfigField(IMAGE_FIELD_KEY
                              ,"Image Field"
                              ,"Field with a path to an image for displaying in nodes"
                              ,this.imageField // current value
                              ,edu.tufts.vue.ui.ConfigurationUI.COMBO_BOX_CONTROL);
        if (mSchema != null) {
            imageField.values = new Vector();
            imageField.values.add(NONE_SELECTED);
            imageField.values.addAll(mSchema.getFieldNames());
        }
        

        List<ConfigField> fields = super.getConfigurationUIFields();
        if (!isCSV)
            fields.add(path);
        fields.add(keyField);
        fields.add(imageField);
        return fields;
    }

    private JComponent loadViewer() {
        
        Log.debug("loadContentAndBuildViewer...");
        tufts.vue.VUE.diagPush("XmlLoad");
        JComponent viewer = null;
        RuntimeException exception = null;
        try {
            viewer = loadContentAndBuildViewer();
        } catch (RuntimeException re) {
            //Log.error("loadViewer", re);
            exception = re;
        } catch (Throwable t) {
            Log.error("loadViewer", t);
        } finally {
            tufts.vue.VUE.diagPop();
        }
        if (exception != null)
            throw exception;
        return viewer;
    }

    public Schema ingestCSV(Schema schema, String file, boolean hasColumnTitles) throws java.io.IOException
    {
        //final Schema schema = new Schema(file);
        //final CSVReader reader = new CSVReader(new FileReader(file));
        // TODO: need an encoding Win/Mac encoding toggle
        // TODO: need handle this in BrowseDataSource openReader (encoding provided by user in data-source config)
        final CSVReader lineReader = new CSVReader(new InputStreamReader(new FileInputStream(file), "windows-1252"));

        String[] values = lineReader.readNext();

        if (schema == null) {
            schema = Schema.instance(Resource.instance(file), getGUID());
        } else {
            schema.flushData();
            schema.setResource(Resource.instance(file));
        }
        
        if (values == null)
            throw new IOException(file + ": empty file?");

        if (hasColumnTitles) {
            schema.ensureFields(values);
            values = lineReader.readNext();            
        } else {
            schema.ensureFields(values.length);
        }

        if (values == null)
            throw new IOException(file + ": has column names, but no data");
        
        do {

            schema.addRow(values);
            
        } while ((values = lineReader.readNext()) != null);

        lineReader.close();

        return schema;
    }
    
    private Schema mSchema;
    
    private JComponent loadContentAndBuildViewer() throws java.io.IOException
    {
        Log.debug("loadContentAndBuildViewer");

        final Schema schema;
        //boolean isCSV = false;

        if (getAddress().endsWith(".csv")) {
            schema = ingestCSV(mSchema, getAddress(), true);
            mSchema = schema;
            isCSV = true;
        } else {
            schema = XMLIngest.ingestXML(openInput(), getItemKey());
            mSchema = schema;
            //schema = XMLIngest.ingestXML(openAddress(), getItemKey());
        }

        if (getKeyField() != null)
            mSchema.setKeyField(getKeyField());

        schema.setName(getDisplayName());

        updateAllRuntimeSchemaReferences(schema);
        
        return DataTree.create(schema);

        // return buildOldStyleTree(schema);

    }

    /** find all schema handles in all nodes that match the new schema
     * and replace them with pointers to the live data schema */
    // TODO: handle deleted nodes in undo queue!
    private static void updateAllRuntimeSchemaReferences(final Schema newlyLoadedSchema)
    {
        if (!newlyLoadedSchema.isLoaded()) {
            Log.warn("newly loaded schema is empty: " + newlyLoadedSchema, new Throwable("FYI"));
            return;
        }
        
        final Collection<LWMap> allMaps = VUE.getAllMaps();

        int updateCount = 0;
        int mapUpdateCount = 0;

        // todo: if this ever gets slow, could improve performance by pre-computing a
        // lookup map of all schema handles that map to the new schema (which will
        // usually contain only a single schema mapping) then we only have to check
        // every schema reference found against the pre-computed lookups instead of
        // doing a Schema.lookup against all loaded Schema's.
        
        for (LWMap map : allMaps) {
            final int countAtMapStart = updateCount;
            Collection<LWComponent> nodes = map.getAllDescendents();
            for (LWComponent c : nodes) {
                final MetaMap data = c.getRawData();
                if (data == null)
                    continue;
                final Schema curSchema = data.getSchema();
                if (curSchema != null) {
                    final Schema newSchema = Schema.lookup(curSchema);
                    if (newSchema != curSchema) {
                        data.takeSchema(newSchema);
                        updateCount++;
                        if (newSchema != newlyLoadedSchema) {
                            Log.warn("out of data schema in " + c + "; oldSchema=" + curSchema + "; replaced with " + newSchema);
                        } else {
                            //if (DEBUG.SCHEMA) Log.debug("replaced schema handle in " + c);
                        }

                    }
                }
            }
            if (updateCount > countAtMapStart)
                mapUpdateCount++;
        }
        Log.info(String.format("updated %d schema handle references in %d maps", updateCount, mapUpdateCount));
    }


    private JComponent buildOldStyleTree(Schema schema)
    {
        mItems.clear();

        final Resource top = Resource.instance(getAddress());

        top.reset();
        //top.setClientType(Resource.DIRECTORY); // todo: fix later -- Resource.java can't be checked in at the moment
        top.setClientType(3);
        top.setTitle("XML Data Feed: " + getDisplayName() + " " + new Date());
        top.setProperty("URL", getAddress());
        //fr.setDataType("xml");

        mItems.add(top);

        for (DataRow row : schema.getRows()) {
            //Resource r = Resource.instance(row.getValue("rss.channel.item.link"));
            String link = row.getValue("link");
            final Resource r;
            if (link != null) {
                r = Resource.instance(link);
                final String title = row.getValue("title");
                r.setProperty("Title", title);                
                r.setTitle(title);
            } else {
                String value = row.getValue(getItemKey());
                if (value != null && value.startsWith("http:")) {
                    r = Resource.instance(value);
                } else {
                    r = Resource.instance("#" + getItemKey() + ": " + value);
                    r.setDataType("xml");
                }
                r.setTitle(value);
            }

            //Resource r = Resource.instance(row.getValue("link"));
            //r.setTitle(row.getValue("rss.channel.item.title"));

            String thumb = row.getValue("media:group.media:content.media:url");
            if (thumb != null)
                ((URLResource)r).setURL_Thumb(thumb);
            
            //r.getProperties().putAll(row.asMap());
            
//             for (Map.Entry<String,String> e : row.asMap().entrySet())
//                 r.addProperty(e.getKey(), e.getValue());
            
            //for (Map.Entry<String,?> e : tufts.vue.MetaMap.entries(row.asMap()))
            for (Map.Entry e : row.dataEntries())
                r.addProperty(e.getKey().toString(), e.getValue().toString());
            
            mItems.add(r);
        }

        //schema.dumpSchema(new PrintWriter(debug));
        //top.setProperty("Description", "<pre>" + debug.toString());

        if (mItems.size() == 0)
            throw new DataSourceException("[Empty XML feed]");

        
//         if (DEBUG.Enabled) {
//             for (Map.Entry<String,List<String>> e : headers.entrySet()) {
//                 Object value = e.getValue();
//                 if (value instanceof Collection && ((Collection)value).size() == 1)
//                     value = ((Collection)value).toArray()[0];
//                 if (e.getKey() == null)
//                     r.setProperty("HTTP-response", value);
//                 else
//                     r.setProperty("HTTP:" + e.getKey(), value);
//             }
            
            
//         }

        VueDragTree tree = new VueDragTree(mItems, this.getDisplayName());
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.expandRow(0);
        tree.setRootVisible(false);
        tree.setName(getClass().getSimpleName() + ": " + getAddress());

        return tree;
    }
        


    private static boolean TEST_DEBUG = false;

    
    public static void main(String[] args) throws Exception
    {
        TEST_DEBUG = true;
        
        DEBUG.Enabled = true;
        DEBUG.DR = true;
        DEBUG.RESOURCE = true;
        DEBUG.DATA = true;

        tufts.vue.VUE.init(args);
        
        XmlDataSource ds = new XmlDataSource("test", args[0]);

        ds.loadContentAndBuildViewer();

    }
    
}

