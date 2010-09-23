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

package tufts.vue.ds;

import tufts.vue.*;
import tufts.vue.ds.XMLIngest.XmlSchema;
import java.util.*;
import java.io.*;
import java.util.List;
import java.awt.*;
import java.nio.charset.Charset;
import javax.swing.*;
import edu.tufts.vue.ui.ConfigurationUI;
import au.com.bytecode.opencsv.CSVReader;


/**
 * @version $Revision: 1.27 $ / $Date: 2010-02-03 19:13:16 $ / $Author: mike $
 * @author Scott Fraize
 */
public class XmlDataSource extends BrowseDataSource
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(XmlDataSource.class);
    
    private final List<Resource> mItems = new ArrayList<Resource>();

    private static final String ITEM_PATH_KEY = "item_path";
    private static final String KEY_FIELD_KEY = "key_field";
    private static final String IMAGE_FIELD_KEY = "image_field";
    private static final String ENCODING_FIELD_KEY = "encoding_field";
    private static final String MATRIX_FIELD_KEY = "matrix";
    private static final String MATRIX_ROW_KEY ="matrixRow";
    private static final String MATRIX_COL_KEY ="matrixCol";
    private static final String MATRIX_RELATION_KEY="matrixRelation";
    private static final String MATRIX_FORMAT_KEY ="matrixFormat";
    private static final String MATRIX_PIVOT_KEY ="matrixPivot";
    private static final String MATRIX_STARTROW_KEY ="matrixStartRow";
    private static final String MATRIX_MATRIXSIZE_KEY ="matrixSize";


    
    private static final String NONE_SELECTED = "(none selected)";
    private static final String AUTO_SELECTED = "(auto detect)";

    private static final String WIDE = "wide";
    private static final String TALL = "tall";

    public static final String TYPE_NAME = "XML Feed";
    

    private String itemKey;
    private String keyField;
    private String imageField = NONE_SELECTED;
    private String encodingField = AUTO_SELECTED;
   
    private String matrixField = "false";
    private String matrixRowField = NONE_SELECTED;
    private String matrixColField = NONE_SELECTED;
    private String matrixRelField = NONE_SELECTED;
    private String matrixFormatField = NONE_SELECTED;
    private String matrixPivotField = NONE_SELECTED;
    private String matrixStartRowField = "";
    private String matrixSizeField = "";





    private boolean isCSV; // hack while XmlDataSource supports both XML and flat-files
    
    public XmlDataSource() {}
    
    public XmlDataSource(String displayName, String address) throws DataSourceException {
        this.setDisplayName(displayName);
        this.setAddress(address);
    }

    @Override
    public String getTypeName() {
        return TYPE_NAME;
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
            if ((val = p.getProperty(ENCODING_FIELD_KEY)) != null)
                setEncodingField(val);
        } catch (Throwable t) {
            Log.error("val=" + val, t);
        }
        try {
            if ((val = p.getProperty(MATRIX_FIELD_KEY)) != null)
                setMatrixField(val);
        } catch (Throwable t) {
            Log.error("val=" + val, t);
        }
        try {
            if ((val = p.getProperty(IMAGE_FIELD_KEY)) != null)
                setImageField(val);
        } catch (Throwable t) {
            Log.error("val=" + val, t);
        }
        try {
            if ((val = p.getProperty(MATRIX_ROW_KEY)) != null)
                setMatrixRowField(val);
        } catch (Throwable t) {
            Log.error("val=" + val, t);
        }
        try {
            if ((val = p.getProperty(MATRIX_COL_KEY)) != null)
                setMatrixColField(val);
        } catch (Throwable t) {
            Log.error("val=" + val, t);
        }
        try {
            if ((val = p.getProperty(MATRIX_FORMAT_KEY)) != null)
                setMatrixColField(val);
        } catch (Throwable t) {
            Log.error("val=" + val, t);
        }
        try {
            if ((val = p.getProperty(MATRIX_PIVOT_KEY)) != null)
                setMatrixPivotField(val);
        } catch (Throwable t) {
            Log.error("val=" + val, t);
        }
        try {
            if ((val = p.getProperty(MATRIX_STARTROW_KEY)) != null)
                setMatrixStartRowField(val);
        } catch (Throwable t) {
            Log.error("val=" + val, t);
        }
        try {
            if ((val = p.getProperty(MATRIX_RELATION_KEY)) != null)
                setMatrixRelField(val);
        } catch (Throwable t) {
            Log.error("val=" + val, t);
        }
        try {
            if ((val = p.getProperty(MATRIX_MATRIXSIZE_KEY)) != null)
                setMatrixSizeField(val);
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
    
    public String getMatrixField() {
        return matrixField;
    }
    
    public String getMatrixRowField() {
        return matrixRowField;
    }
    
    public String getMatrixColField() {
        return matrixColField;
    }
    
    public String getMatrixRelField() {
        return matrixRelField;
    }
    
    public String getMatrixFormatField() {
        return matrixFormatField;
    }
    
    public String getMatrixPivotField() {
        return matrixPivotField;
    }
    
    public String getMatrixSizeField() {
        return matrixSizeField;
    }
    
    public String getMatrixStartRowField() {
        return matrixStartRowField;
    }
    
    public String getEncodingField() {
    	if (encodingField.equals("(auto detect)"))
    		return "windows-1252";
    	else
    		return encodingField;
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
    public void setEncodingField(String k) {
        if (DEBUG.DR) Log.debug("setEncodingField[" + k + "]");
        if (k != null) {
            k = k.trim();
            if (k.length() < 1)
                encodingField = null;
            else
                encodingField = k;
        } else {
            encodingField = null;
        }
        unloadViewer();
    }

    public void setMatrixField(String k) {
        if (DEBUG.DR) Log.debug("setMatrixField[" + k + "]");
        if (k != null) {
            k = k.trim();
            if (k.length() < 1)
                matrixField = null;
            else
                matrixField = k;
        } else {
            matrixField = null;
        }
        unloadViewer();
    }
    
    public void setMatrixColField(String k) {
        if (DEBUG.DR) Log.debug("setMatrixColField[" + k + "]");
        if (k != null) {
            k = k.trim();
            if (k.length() < 1)
                matrixColField = null;
            else
                matrixColField = k;
        } else {
            matrixColField = null;
        }


      //  unloadViewer();
    }
    
    public void setMatrixFormatField(String k) {
        if (DEBUG.DR) Log.debug("setMatrixFormatField[" + k + "]");
        if (k != null) {
            k = k.trim();
            if (k.length() < 1)
                matrixFormatField = null;
            else
                matrixFormatField = k;
        } else {
            matrixFormatField = null;
        }


      //  unloadViewer();
    }
    
    public void setMatrixPivotField(String k) {
        if (DEBUG.DR) Log.debug("setMatrixFormatField[" + k + "]");
        if (k != null) {
            k = k.trim();
            if (k.length() < 1)
                matrixPivotField = null;
            else
                matrixPivotField = k;
        } else {
            matrixPivotField = null;
        }


      //  unloadViewer();
    }
    public void setMatrixSizeField(String k) {
        if (DEBUG.DR) Log.debug("setMatrixSizeField[" + k + "]");
        if (k != null) {
            k = k.trim();
            if (k.length() < 1)
                matrixSizeField = null;
            else
                matrixSizeField = k;
        } else {
            matrixSizeField = null;
        }


      //  unloadViewer();
    }
    public void setMatrixStartRowField(String k) {
        if (DEBUG.DR) Log.debug("setMatrixStartRowField[" + k + "]");
        if (k != null) {
            k = k.trim();
            if (k.length() < 1)
                matrixStartRowField = null;
            else
                matrixStartRowField = k;
        } else {
            matrixStartRowField = null;
        }


      //  unloadViewer();
    }
    
    public void setMatrixRelField(String k) {
        if (DEBUG.DR) Log.debug("setMatrixRelField[" + k + "]");
        if (k != null) {
            k = k.trim();
            if (k.length() < 1)
                matrixRelField = null;
            else
                matrixRelField = k;
        } else {
            matrixRelField = null;
        }            


    //    unloadViewer();
    }
    public void setMatrixRowField(String k) {
        if (DEBUG.DR) Log.debug("setMatrixRowField[" + k + "]");
        if (k != null) {
            k = k.trim();
            if (k.length() < 1)
                matrixRowField = null;
            else
                matrixRowField = k;
        } else {
            matrixRowField = null;
        }
      //  unloadViewer();
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
                              ,"Image/Content Field"
                              ,"Field with a path to an image or resource for displaying in nodes"
                              ,this.imageField // current value
                              ,edu.tufts.vue.ui.ConfigurationUI.COMBO_BOX_CONTROL);
        if (mSchema != null) {
            imageField.values = new Vector();
            imageField.values.add(NONE_SELECTED);
            final Vector fieldNames = mSchema.getFieldNames();
            Collections.sort(fieldNames);
            imageField.values.addAll(fieldNames);
        }
        

        ConfigField encodingField
        = new ConfigField(ENCODING_FIELD_KEY
                          ,"Read as"
                          ,"Encoding to read data file as."
                          ,this.encodingField // current value
                          ,edu.tufts.vue.ui.ConfigurationUI.COMBO_BOX_CONTROL);
        
       // encodingField.value = new Vector();
        SortedMap<String,Charset> sets = java.nio.charset.Charset.availableCharsets();
        encodingField.values = new Vector();
        encodingField.values.add(AUTO_SELECTED);
        encodingField.values.addAll(sets.keySet());
        
        List<ConfigField> fields = super.getConfigurationUIFields();

        if (!isCSV)
            fields.add(path);
        fields.add(keyField);
        fields.add(imageField);

        if (isCSV)
        	fields.add(encodingField);
        
        List<ConfigField> mFields = null;
        if (this.getMatrixField().equals("true"))
        {
        	if (this.getMatrixFormatField().equals(TALL))
        		mFields = this.getMatrixConfigurationUIFields(headerValues);
        	else
        		mFields = this.getWideMatrixConfigurationUIFields(headerValues);
        	
        	fields.addAll(mFields);
        }
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

    private String[] readLine(BufferedReader r) throws java.io.IOException {
        return r.readLine().split(","); // test impl
    }
    private String[] readLine(CSVReader r) throws java.io.IOException {
        return trim(r.readNext());
    }

    private static String[] trim(String[] values) {

        if (values == null)
            return null;
        
        for (int i = 0; i < values.length; i++) {
            // fixup for au.com.bytecode.opencsv.CSVReader, which leaves whitespace & quotes in the values!
            String v = values[i].trim();
            if (v.length() > 0 && v.charAt(0) == '"')
                v = v.substring(1);
            values[i] = v;
        }
        return values;
    }
    
    public Schema ingestCSV(Schema schema, String file, boolean hasColumnTitles) throws java.io.IOException
    {
        //final Schema schema = new Schema(file);
        //final CSVReader reader = new CSVReader(new FileReader(file));
        // TODO: need an encoding Win/Mac encoding toggle
        // TODO: need handle this in BrowseDataSource openReader (encoding provided by user in data-source config)
        // TODO: the Open CSV CSVReader impl is horrible - doesn't handle quoted values properly!
        
        final CSVReader dataStream = new CSVReader(new InputStreamReader(new FileInputStream(file),this.getEncodingField()));
        //final BufferedReader dataStream = new BufferedReader(new InputStreamReader(new FileInputStream(file), "windows-1252"));

        String[] values = readLine(dataStream);

        if (schema == null) {
            //if (DEBUG.SCHEMA) Log.debug("no current schema, instancing new");

            schema = Schema.getNewAuthorityInstance(Resource.instance(file), getGUID(), getDisplayName());

        } else {
            if (DEBUG.SCHEMA) Log.debug("reloading schema " + schema);
            schema.setResource(Resource.instance(file));
        }
        schema.flushData();
        
        if (values == null)
            throw new IOException(file + ": empty file?");

        if (hasColumnTitles) {
            schema.ensureFields(values);
            values = readLine(dataStream);
        } else {
            schema.ensureFields(values.length);
        }

        if (values == null)
            throw new IOException(file + ": has column names, but no data");
        
        do {

            schema.addRow(values);
            
        } while ((values = readLine(dataStream)) != null);

        dataStream.close();

        schema.notifyAllRowsAdded();

        return schema;
    }
    
    protected String[] headerValues = null;
   
    public Schema ingestMatrixCSV(Schema schema, String file, boolean hasColumnTitles) throws java.io.IOException
    {
    	final boolean isMatrixDataset = true;
    	int matrixSize = 0;
        //final Schema schema = new Schema(file);
        //final CSVReader reader = new CSVReader(new FileReader(file));
        // TODO: need an encoding Win/Mac encoding toggle
        // TODO: need handle this in BrowseDataSource openReader (encoding provided by user in data-source config)
        // TODO: the Open CSV CSVReader impl is horrible - doesn't handle quoted values properly!
    	
        final CSVReader dataStream = new CSVReader(new InputStreamReader(new FileInputStream(file),this.getEncodingField()));
        //final BufferedReader dataStream = new BufferedReader(new InputStreamReader(new FileInputStream(file), "windows-1252"));
        
        String[] values = readLine(dataStream);
        headerValues = new String[values.length];
        headerValues  = values.clone();

        if (schema == null) {
            //if (DEBUG.SCHEMA) Log.debug("no current schema, instancing new");

            schema = Schema.getNewAuthorityInstance(Resource.instance(file), getGUID(), getDisplayName());

        } else {
            if (DEBUG.SCHEMA) Log.debug("reloading schema " + schema);
            schema.setResource(Resource.instance(file));
        }
        schema.flushData();
        
        if (values == null)
            throw new IOException(file + ": empty file?");

        if (hasColumnTitles) 
        {
        	if (isMatrixDataset)
        	{
        		int n=0;
        		
        		if (this.matrixFormatField.equals(NONE_SELECTED))
        		{        			
        			JPanel p = new JPanel();
        			p.setLayout(new BorderLayout());
        			
        			JLabel label = new JLabel();
        			label.setIcon(VueResources.getImageIcon("widetall"));
        			JLabel label2 = new JLabel("Is the matrix format wide or tall?");
        			p.add(label,BorderLayout.CENTER);
        			p.add(label2,BorderLayout.SOUTH);

        			//Custom button text
        			Object[] options = {"Wide",
        			                    "Tall"};
        			n = JOptionPane.showOptionDialog(VUE.getApplicationFrame(),
        			    p,
        			    "Define Matrix Format",
        			    JOptionPane.YES_NO_OPTION,
        			    JOptionPane.PLAIN_MESSAGE,
        			    null,
        			    options,
        			    options[1]);
        			
        			if (n==0)
        				this.setMatrixFormatField(WIDE);
        			else
        				this.setMatrixFormatField(TALL);
        		} //matrix format
        		
        		if (this.matrixFormatField.equals(TALL) &&
        			(this.matrixColField.equals(NONE_SELECTED) || 
        			this.matrixRowField.equals(NONE_SELECTED) ||
        			this.matrixRelField.equals(NONE_SELECTED)))
        		{
	    			UIParams params = this.getXMLforMatrix(values);
	    			boolean proceed = false;
	    			
	        	    ConfigurationUI cui = new edu.tufts.vue.ui.ConfigurationUI(new java.io.ByteArrayInputStream(params.xml.getBytes()),params.extraValuesByKey);
	    			cui.setPreferredSize(new Dimension(350, (int)cui.getPreferredSize().getHeight()));
	                
	                if (VueUtil.option(VUE.getDialogParent(),
	                        cui,
	                        VueResources.getString("optiondialog.configuration.message"),
	                        javax.swing.JOptionPane.DEFAULT_OPTION,
	                        javax.swing.JOptionPane.PLAIN_MESSAGE,
	                        new Object[] {
	                	VueResources.getString("optiondialog.configuration.continue"), VueResources.getString("optiondialog.configuration.cancel")
	                },
	                VueResources.getString("optiondialog.configuration.continue")) == 1) {
						proceed = false;
					} else {
						setConfiguration(cui.getProperties());
				        DataSourceViewer.saveDataSourceViewer();
					}
        		}
        		else if (this.matrixFormatField.equals(WIDE) && (this.matrixPivotField.equals(NONE_SELECTED)))
        		{
        			UIParams params = this.getXMLforWideMatrix(values);
	    			boolean proceed = false;
	    			
	        	    ConfigurationUI cui = new edu.tufts.vue.ui.ConfigurationUI(new java.io.ByteArrayInputStream(params.xml.getBytes()),params.extraValuesByKey);
	    			cui.setPreferredSize(new Dimension(450, (int)cui.getPreferredSize().getHeight()));
	                
	                if (VueUtil.option(VUE.getDialogParent(),
	                        cui,
	                        VueResources.getString("optiondialog.configuration.message"),
	                        javax.swing.JOptionPane.DEFAULT_OPTION,
	                        javax.swing.JOptionPane.PLAIN_MESSAGE,
	                        new Object[] {
	                	VueResources.getString("optiondialog.configuration.continue"), VueResources.getString("optiondialog.configuration.cancel")
	                },
	                VueResources.getString("optiondialog.configuration.continue")) == 1) {
						proceed = false;
					} else {
						setConfiguration(cui.getProperties());
				        DataSourceViewer.saveDataSourceViewer();
					}
        		}
        	}
	        schema.ensureFields(this,values,isMatrixDataset);
	        values = readLine(dataStream);
        } else {
            schema.ensureFields(values.length);
        }

        if (values == null)
            throw new IOException(file + ": has column names, but no data");

        schema.existingRows = new HashMap<String,Integer>();
        do {
			if (this.matrixFormatField.equals(TALL))
	            schema.addMatrixRow(this,values);
	        else
		        schema.addWideMatrixRow(this,values);
	
        } while ((values = readLine(dataStream)) != null);

        dataStream.close();

        schema.notifyAllRowsAdded();

        return schema;
    }
    
    private UIParams getXMLforWideMatrix(String[] values)
    {
        final UIParams params = new UIParams();
        final StringBuilder b = new StringBuilder();
        //final String address = dataSource.getAddress();
            
        b.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        b.append("<configuration>\n");
        final java.util.Map<String,Vector> extraValuesMap = new java.util.HashMap();
        for (ConfigField f : getWideMatrixConfigurationUIFields(values)) {
                EditLibraryPanel.addField(b, f.key, f.title, f.description, f.value, f.uiControl, f.maxLen);
                if (f.values != null)
                    extraValuesMap.put(f.key, f.values);
            }
        params.extraValuesByKey = extraValuesMap;

        

        b.append("</configuration>");

        
        params.xml=b.toString();
        return params;
    }
    
    private UIParams getXMLforMatrix(String[] values)
    {
        final UIParams params = new UIParams();
        final StringBuilder b = new StringBuilder();
        //final String address = dataSource.getAddress();
            
        b.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        b.append("<configuration>\n");
        final java.util.Map<String,Vector> extraValuesMap = new java.util.HashMap();
        for (ConfigField f : getMatrixConfigurationUIFields(values)) {
                EditLibraryPanel.addField(b, f.key, f.title, f.description, f.value, f.uiControl, f.maxLen);
                if (f.values != null)
                    extraValuesMap.put(f.key, f.values);
            }
        params.extraValuesByKey = extraValuesMap;

        

        b.append("</configuration>");

        
        params.xml=b.toString();
        return params;
    }
   
    private static class UIParams {
        String xml;
        // possible enumerated types, indexed by key field
        Map<String,Vector> extraValuesByKey = Collections.EMPTY_MAP;
    }
    
    private java.util.List<ConfigField> getWideMatrixConfigurationUIFields(String[] values) {
    	java.util.List<ConfigField> fields = new ArrayList<ConfigField>();
    	
    	ConfigField field1
        = new ConfigField(MATRIX_PIVOT_KEY
                          ,"What is the pivot attribute of the matrix?"
                          ,"Read CSV as relational matrix"
                          ,this.matrixPivotField // current value
                          ,edu.tufts.vue.ui.ConfigurationUI.COMBO_BOX_CONTROL);

    	List l = Arrays.asList(values);
    	//l.add(0, NONE_SELECTED);
    	
        field1.values = new Vector(l);
        
    	fields.add(field1);

    /*	ConfigField field2
        = new ConfigField(MATRIX_STARTROW_KEY
                          ,"What row # contains the header?"
                          ,"Read CSV as relational matrix"
                          ,this.matrixStartRowField // current value
                          ,edu.tufts.vue.ui.ConfigurationUI.SINGLE_LINE_CLEAR_TEXT_CONTROL);
    	
//        field2.values = new Vector(l);
        
    	fields.add(field2);
*/
    	ConfigField field3
        = new ConfigField(MATRIX_MATRIXSIZE_KEY
                          ,"What is the size of the matrix (ex: 8x8)?"
                          ,"Read CSV as relational matrix"
                          ,this.matrixSizeField // current value
                          ,edu.tufts.vue.ui.ConfigurationUI.SINGLE_LINE_CLEAR_TEXT_CONTROL);
    	
    	fields.add(field3);
		return fields;
	}
    
    private java.util.List<ConfigField> getMatrixConfigurationUIFields(String[] values) {
    	java.util.List<ConfigField> fields = new ArrayList<ConfigField>();
    	
    	ConfigField field1
        = new ConfigField(MATRIX_ROW_KEY
                          ,"Select row from data"
                          ,"Read CSV as relational matrix"
                          ,this.matrixRowField // current value
                          ,edu.tufts.vue.ui.ConfigurationUI.COMBO_BOX_CONTROL);

    	List l = Arrays.asList(values);
    	//l.add(0, NONE_SELECTED);
    	
        field1.values = new Vector(l);
        
    	fields.add(field1);

    	ConfigField field2
        = new ConfigField(MATRIX_COL_KEY
                          ,"Select column from data"
                          ,"Read CSV as relational matrix"
                          ,this.matrixColField // current value
                          ,edu.tufts.vue.ui.ConfigurationUI.COMBO_BOX_CONTROL);
    	
        field2.values = new Vector(l);
        
    	fields.add(field2);
    	
    	ConfigField field3
        = new ConfigField(MATRIX_RELATION_KEY
                          ,"Select relationship from data"
                          ,"Read CSV as relational matrix"
                          ,this.matrixRelField // current value
                          ,edu.tufts.vue.ui.ConfigurationUI.COMBO_BOX_CONTROL);

        field3.values = new Vector(l);
        
    	fields.add(field3);

    	
		return fields;
	}

	private Schema mSchema;
    
    public Schema getSchema()
    {
    	return mSchema;
    }
   
    private JComponent loadContentAndBuildViewer() throws java.io.IOException
    {
        Log.debug("loadContentAndBuildViewer");

        final Schema schema;
        //boolean isCSV = false;

        Log.info("INGESTING " + getAddress() + "...");

        boolean newIngest = false;

        if (getAddress().toLowerCase().endsWith(".csv")) {

            // Note: for CSV data, we pass in the existing schema, permitting it
            // to be re-loaded, and preserving existing runtime Schema references.
            if (getMatrixField().equals("true"))
            	schema = ingestMatrixCSV(mSchema, getAddress(), true);
            else	
            	schema = ingestCSV(mSchema, getAddress(), true);
            mSchema = schema;
            isCSV = true;
        } else {
            
            // TODO: would be better to pass the existing schema instance into ingestXML
            // (as we doo with ingestCSV), and have it be flushed and reloaded with new
            // data so we wouldn't have to re-update all runtime LWComponent MetaMap
            // Schema references, tho that appears to be be successfully working.  But
            // besides being cleaner and faster, if we did that, we could keep any
            // existing field style information alive that had been stored in the
            // schema.

            // TODO: What's different about XML schema loading that's not loading
            // the user styles?
            
            schema = XMLIngest.ingestXML((XmlSchema) mSchema,openInput(), getItemKey());
            newIngest = true;
            schema.setDSGUID(getGUID());
            mSchema = schema;
            //schema = XMLIngest.ingestXML(openAddress(), getItemKey());
        }

        Log.info("ingested " + getAddress());
        
        if (getKeyField() != null)
            mSchema.setKeyField(getKeyField());

        if (getEncodingField() != null)
            mSchema.setEncodingField(getEncodingField());


        schema.setImageField(getImageField());
        schema.setName(getDisplayName());

        if (newIngest)
            schema.notifyAllRowsAdded();            
        updateAllRuntimeSchemaReferences(schema);
        
        return DataTree.create(schema);

        // return buildOldStyleTree(schema);

    }

    /** find all schema handles in all nodes that match the new schema
     * and replace them with pointers to the live data schema */
    // todo: handle deleted nodes in undo queue, tho LWComponent setParent should be handling that
    private static void updateAllRuntimeSchemaReferences(final Schema newlyLoadedSchema)
    {
        Schema.reportNewAuthoritativeSchema(newlyLoadedSchema, VUE.getAllMaps());
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

