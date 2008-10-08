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

import java.util.*;
import java.io.*;
import java.net.URL;
import java.text.DateFormat;

import tufts.vue.LWComponent;

import com.google.common.collect.*;


/**
 * @version $Revision: 1.2 $ / $Date: 2008-10-08 01:12:28 $ / $Author: sfraize $
 * @author Scott Fraize
 */


public class Schema {

    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(Schema.class);

    static final boolean DEBUG = true;

    final Map<String,Field> mFields = new LinkedHashMap(); // "columns"

    private final List<DataRow> mRows = new ArrayList();

    private final Object mSource;
    
    protected int mLongestFieldName = 10;

    private String mName;

    private LWComponent mStyleNode;
        
    public Schema(Object source) {
        // would be very handy if source was a Resource and Resources had IO methods
        mSource = source;
    }

    public String toString() {
        return getName() + ": " + getSource();
    }
        

    public void setStyleNode(LWComponent style) {
        mStyleNode = style;
    }
        
    public LWComponent getStyleNode() {
        return mStyleNode;
    }
    
    public Object getSource() {
        return mSource;
    }

    public int getRowCount() {
        return mRows.size();
    }

    public void setName(String name) {
        mName = name;
    }
    
    public String getName() {

        if (mName != null)
            return mName;
        else
            return getSource().toString();
        
//         String s = getSource().toString();
//         int i = s.lastIndexOf('/');
//         if (i < s.length() - 2)
//             return s.substring(i+1);
//         else
//             return s;
//         Object o = getSource();
//         if (o instanceof File)
//             return ((File)o).getName();
//         else if (o instanceof URL)
//             return ((URL)o).getFile();
//         else
//             return o.toString();
    }

    public void createFields(String[] names) {
        for (String name : names)
            mFields.put(name, new Field(name, this));
    }
    
    public void createFields(int count) {
        for (int i = 0; i < count; i++) {
            String name = "Column " + (i+1);
            mFields.put(name, new Field(name, this));
        }
    }
    

    public void dumpSchema(PrintStream ps) {
        dumpSchema(new PrintWriter(new OutputStreamWriter(ps)));
    }
        
    public void dumpSchema(PrintWriter ps) {
        ps.println(getClass().getName() + ": ");
        final int pad = -mLongestFieldName;
        final String format = "%" + pad + "s: %s\n";
        for (Field f : mFields.values()) {
            ps.printf(format, f.getName(), f.valuesDebug());
        }
        //ps.println("Rows collected: " + rows.size());
    }

    public String getDump() {
        StringWriter debug = new StringWriter();
        dumpSchema(new PrintWriter(debug));
        return debug.toString();
    }
    

    protected void addRow(DataRow row) {
        mRows.add(row);
    }
    
    protected void addRow(String[] values) {

        DataRow row = new DataRow();
        int i = 0;
        for (Field field : getFields()) {
            final String value = values[i++];
            row.addValue(field, value);
        }
        addRow(row);
    }
    
    public List<DataRow> getRows() {
        return mRows;
    }

    public Collection<Field> getFields() {
        return mFields.values();
    }

    // todo: factor out XML impl reference
    public boolean isXMLKeyFold() {
        return false;
    }

    // temp hack: if we keep this, move to elsewhere and/or have it populate an existing container
//     public LWMap.Layer createSchematicLayer() {

//         final LWMap.Layer layer = new LWMap.Layer("Schema: " + getName());

//         Field keyField = null;

//         for (Field field : getFields()) {
//             if (field.isPossibleKeyField() && !field.isLenghtyValue()) {
//                 keyField = field;
//                 break;
//             }
//         }
//         // if didn't find a "short" key field, find the shortest
//         if (keyField == null) {
//             for (Field field : getFields()) {
//                 if (field.isPossibleKeyField()) {
//                     keyField = field;
//                     break;
//                 }
//             }
//         }

//         boolean labelGuessed = false;

//         LWNode keyNode = null;
//         if (keyField != null) {
//             keyNode = new LWNode("itemNode");
//             keyNode.setShape(java.awt.geom.Ellipse2D.Float.class);
//             keyNode.setProperty(tufts.vue.LWKey.FontSize, 32);
//             keyNode.setNotes(getDump());
//         }

//         int y = Short.MAX_VALUE;
//         for (Field field : Util.reverse(getFields())) { // reversed to preserve on-map stacking order
//             if (field.isSingleton())
//                 continue;
//             LWNode colNode = new LWNode(field.getName());
//             colNode.setLocation(0, y--);
//             if (field.isPossibleKeyField()) {
//                 if (keyNode != null && !labelGuessed) {
//                     keyNode.setLabel("${" + field.getName() + "}");
//                     labelGuessed = true;
//                 }
//                 colNode.setFillColor(java.awt.Color.red);
//             } else
//                 colNode.setFillColor(java.awt.Color.gray);
//             colNode.setShape(java.awt.geom.Rectangle2D.Float.class);
//             colNode.setNotes(field.valuesDebug());
//             layer.addChild(colNode);

//             if (keyNode != null)
//                 layer.addChild(new tufts.vue.LWLink(keyNode, colNode));
            
//         }

//         if (keyNode != null) {
//             layer.addChild(keyNode);
//             //tufts.vue.Actions.MakeColumn.act(layer.getChildren());
//             tufts.vue.Actions.MakeCircle.act(Collections.singletonList(keyNode));
//         } else {
//             tufts.vue.Actions.MakeColumn.act(layer.getChildren());
//         }

//         return layer;
//     }

}


    class Field
    {
        private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(Field.class);

        public static final String TYPE_UNKNOWN = "?";
        public static final String TYPE_TEXT = "text";
        public static final String TYPE_NUMBER = "number";
        public static final String TYPE_DATE = "date";
        public static final String TYPE_NUMERIC = "number";
        
        //static final int MAX_ENUM_VALUE_LENGTH = 54;
        static final int MAX_ENUM_VALUE_LENGTH = 144;
        static final int MAX_DATE_VALUE_LENGTH = 40;
        static final DateFormat DateParser = DateFormat.getDateTimeInstance();

        final String name;

        boolean allValuesUnique = true;
        int valueCount = 0;
        boolean enumDisabled = false;
        int maxValueLen = 0;
        boolean isNumeric = true;

        final Schema schema;

        LWComponent nodeStyle;

//         long minValue;
//         long maxValue;

        String type = TYPE_UNKNOWN;
        
        /** map of all possible unique values for enumeration tracking */
        Map<String,Integer> values;

        Field(String n, Schema schema) {
            this.name = n.trim();
            this.schema = schema;
            if (Schema.DEBUG) Log.debug("(created field \"" + name + "\")");
        }

        public void setStyleNode(LWComponent style) {
            if (nodeStyle != null)
                Log.debug("resetting field style " + this + " to " + style);
            nodeStyle = style;
        }
        
        public LWComponent getStyleNode() {
            return nodeStyle;
        }

        public String getName() {
            return name;
        }
        
        public Schema getSchema() {
            return schema;
        }

        public String getType() {
            return type;
        }

        public String toString() {
            //if (isNumeric) type=TYPE_NUMERIC; // HACK: NEED ANALYSIS PHASE
            //return getName();
            if (valueCount() == 1)
                //return String.format("<html><code>%s</code>:<br>\"%s\"", getName(), getValues().toArray()[0]);
                return String.format("%s=\"%s\"", getName(), getValues().toArray()[0]);
            else if (allValuesUnique)
                return String.format("%s (%d)/%s/%s", getName(), valueCount(), type, isNumeric);
            else
                return String.format("%s [%d]/%s/%s", getName(), uniqueValueCount(), type, isNumeric);
        }

        public boolean isPossibleKeyField() {
            //return allValuesUnique && valueCount == schema.getRowCount() && !(type == TYPE_DATE);
            return !enumDisabled
                && allValuesUnique
                && uniqueValueCount() == valueCount()
                && valueCount() == schema.getRowCount()
                && !(type == TYPE_DATE);
        }

        public boolean isKeyField() {
            return isPossibleKeyField();
        }

        public boolean isLenghtyValue() {
            return enumDisabled;
        }
        
        public boolean isEnumerated() {
            return !enumDisabled && uniqueValueCount() > 1;
        }

        public int getMaxValueLength() {
            return maxValueLen;
        }

        public Set<String> getValues() {
            return values == null ? Collections.EMPTY_SET : values.keySet();
        }
        
        public Map<String,Integer> getValueMap() {
            return values == null ? Collections.EMPTY_MAP : values;
        }
        
        public boolean isSingleton() {
            return allValuesUnique && (values != null && values.size() < 2);
        }

        // todo: may want to move this to a separate analysis code set
        void trackValue(String value) {

            valueCount++;

            if (value == null)
                return;

            final int valueLen = value.length();

            if (valueLen > maxValueLen)
                maxValueLen = valueLen;

            //             if (valueCount > 8 && type == TYPE_DATE) {
            //                 enumDisabled = true;
            //                 unique = false; // can't know unique if not tracking all values
            //             }
                
            if (enumDisabled)
                return;
            
            if (valueLen > 0) {
                
                if (valueCount > 1 && value.length() > MAX_ENUM_VALUE_LENGTH) {
                    values = null;
                    enumDisabled = true;
                    isNumeric = false;
                    return;
                }
                
                int count = 1;
                if (values == null) {
                    values = new HashMap();
                }
                else if (values.containsKey(value)) {
                    count = values.get(value);
                    count++;
                    allValuesUnique = false;
                }
                values.put(value, count);

                if (type == TYPE_UNKNOWN && value.length() <= MAX_DATE_VALUE_LENGTH) {

                    if (value.indexOf(':') > 0) {
                        Date date = null;

                        try {
                            date = new Date(value);
                        } catch (Throwable t) {
                            Log.debug("Failed to parse [" + value + "] as date: " + t);
                            type = TYPE_TEXT;
                        }
                    
                        //                     try {
                        //                         date = DateParser.parse(value);
                        //                     } catch (java.text.ParseException e) {
                        //                         eoutln("Failed to parse [" + value + "] as date: " + e);
                        //                         type = TYPE_TEXT;
                        //                     }
                    
                        if (date != null) {
                            type = TYPE_DATE;
                            Log.debug("PARSED DATE: " + Util.tags(date) + " from " + value);
                        }

                        if (type == TYPE_UNKNOWN && isNumeric) {
                            for (int i = 0; i < value.length(); i++) {
                                if (!Character.isDigit(value.charAt(i))) {
                                    isNumeric = false;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
                            


//                 if (type == TYPE_UNKNOWN) {
// //                     long lval;
// //                     try {
// //                         lval = Long.parseLong(value);
// //                     } catch (NumberFormatException e) {
// //                     }
//                 }
//             }
        }

        protected int valueCount() {
            //return values == null ? 0 : values.size();
            return valueCount;
        }
        
        public int uniqueValueCount() {
            return values == null ? valueCount() : values.size();
        }

        private String sampleValues(boolean unique) {

            if (values.size() <= 20)
                return unique ? values.keySet().toString() : values.toString();
                
            StringBuffer buf = new StringBuffer("[examples: ");

            int count = 0;
            for (String s : values.keySet()) {
                buf.append('"');
                buf.append(s);
                buf.append('"');
                if (++count >= 3)
                    break;
                buf.append(", ");
            }
            buf.append("]");
            return buf.toString();
        }

        public String valuesDebug() {
            if (values == null) {
                if (valueCount == 0)
                    return "(empty)";
                else
                    return String.format("%5d values (un-tracked; max-len%6d)", valueCount, maxValueLen);
            }
            else if (isSingleton()) {
                return "singleton" + values.keySet();
            }
            else if (allValuesUnique) {
                if (values.size() > 1) {
                    return String.format("%5d unique, single-instance values; %s", values.size(), sampleValues(true));
                    //                    String s = String.format("%2d unique, single-instance values", values.size());
                    //                     if (values.size() < 16)
                    //                         //return s + "; " + values.keySet();
                    //                         return s + "; " + values.toString();
                    //                     else
                    //                         return s + "; " + sampleValues();
                }
                else
                    return "<empty>?";
            }
            else 
                return String.format("%5d values, %4d unique: %s", valueCount(), values.size(), sampleValues(false));
            //return String.format("%5d unique values in %5d; %s", values.size(), valueCount(), sampleValues(false));
                
        }
    }

    
class DataRow {

    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(DataRow.class);
    
    //         final List<Field> columns;
    //         final List<String> values;

    //         VRow(int approxSize) {
    //             if (approxSize < 1)
    //                 approxSize = 10;
    //             columns = new ArrayList(approxSize);
    //             values = new ArrayList(approxSize);
    //         }

    final Map<Field,String> values;
    //final Map<String,String> asText;
    //final MetaMap asText = new MetaMap();
    final Multimap mData = Multimaps.newLinkedHashMultimap();

    DataRow() {
        values = new HashMap();
    }

    //         int size() {
    //             return values.size();
    //         }

    void addValue(Field f, String value) {
        value = value.trim();
        final String existing = values.put(f, value);
        if (existing != null && Schema.DEBUG)
            Log.debug("ROW SUB-KEY COLLISION " + f);
            
        //errout("ROW SUB-KEY COLLISION " + f + "; " + existing);
        //asText.put(f.name, value);
        mData.put(f.name, value);
        f.trackValue(value);
    }

    String getValue(Field f) {
        return values.get(f);
    }
    String getValue(String key) {
        Object o = mData.get(key);
        if (o instanceof Collection) {
            final Collection bag = (Collection) o;
            if (bag.size() == 0)
                return null;
            else if (bag instanceof List)
                return (String) ((List)bag).get(0);
            else
                return (String) Iterators.get(bag.iterator(), 0);
        }
        else
            return (String) o;
        //return (String) Iterators.get(mData.get(key).iterator(), 0);
        //return (String) asText.get(key);
    }
        
    //         Map<String,String> asMap() {
    //             return mData.asMap();
    //         }
        
    Iterable<Map.Entry<String,String>> entries() {
        return mData.entries();
    }
}

