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

import java.text.DateFormat;
import tufts.vue.LWComponent;

import com.google.common.collect.*;

import java.util.*;

/**
 * Represents a column in a data-set, or pseudo-column from an XML mapped data-set.
 *
 * Besides simply recording the name of the column, this class mainly provides
 * data-analysis of on all the values found in the column, discovering enumerated
 * types and doing some data-type analysis.  It also includes the ability to
 * associate a LWComponent node style with specially marked values.
 * 
 * @version $Revision: 1.1 $ / $Date: 2008-11-20 17:48:30 $ / $Author: sfraize $
 * @author Scott Fraize
 */

public class Field
    
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(Field.class);

    public static final String TYPE_UNKNOWN = "?";
    public static final String TYPE_TEXT = "TEXT";
    public static final String TYPE_NUMBER = "NUMBER";
    public static final String TYPE_DATE = "DATE";
        
    //static final int MAX_ENUM_VALUE_LENGTH = 54;
    static final int MAX_ENUM_VALUE_LENGTH = 144;
    static final int MAX_DATE_VALUE_LENGTH = 40;
    static final DateFormat DateParser = DateFormat.getDateTimeInstance();

    final String name;

    boolean allValuesUnique;
    int valueCount;
    boolean enumDisabled;
    int maxValueLen;
    boolean isNumeric;

    final Schema schema;

    LWComponent nodeStyle;

    //         long minValue;
    //         long maxValue;

    String type = TYPE_UNKNOWN;
        
    /** map of all possible unique values for enumeration tracking */
    Multiset<String> mValues;

    /** map of values currently present in a given context (e.g., a VUE map) */
    Multiset<String> mContextValues;

    Field(String n, Schema schema) {
        this.name = n.trim();
        this.schema = schema;
        flushStats(true);
        if (Schema.DEBUG) Log.debug("(created field \"" + name + "\")");
    }

    void markIncludedValues(final Collection<LWComponent> nodes) {
        if (mValues == null || count(mValues) < 1) {
            if (mContextValues != null)
                mContextValues.clear();
            return;
        }
        if (mContextValues == null)
            mContextValues = Multisets.newHashMultiset();
        else
            mContextValues.clear();
        Log.debug("MARKING INCLUDED VALUES AGAINST " + nodes.size() + " NODES for " + this);
        //final Set<String> valuesToCheck = new HashSet(mValues.keySet());
        //final Set<String> valuesToCheck = mValues.keySet();
        final Set<String> valuesToCheck = mValues.elementSet();
        for (LWComponent c : nodes) {
            for (String value : valuesToCheck) {
                //if (c.isSchematicFieldNode() && c.hasDataValue(this.name, value)) {
                if (c.hasDataValue(this.name, value)) {
                    mContextValues.add(value);
                    //Log.debug(String.format("found in context: %s=[%s], count=%d", this.name, value, mContextValues.count(value)));
                }
            }
            
//             final Iterator<String> i = valuesToCheck.iterator();
//             while (i.hasNext()) {
//                 final String value = i.next();
//                 if (c.isSchematicFieldNode() && c.hasDataValue(this.name, value)) {
//                     //Log.debug(String.format("found in context: %s=[%s]", this.name, value));
//                     mContextValues.add(value);
//                     i.remove();
//                 }
//             }
//             if (valuesToCheck.size() < 1) {
//                 //Log.debug(this + "; no more values to check, found: " + mContextValues);
//                 Log.debug(String.format("all %d data-set values found on the map, done marking early for [%s]",
//                                         mValues.size(),
//                                         this.name));
//                 if (mContextValues.size() != mValues.size())
//                     Log.error(new Throwable(String.format("context values %d != data-set values size %d in [%s]", 
//                                                           mContextValues.size(),
//                                                           mValues.size(),
//                                                           this.name)));
//                 //                     Log.debug(String.format("all values discovered, found %3d on-map out of %3d in data-set [%s]",
//                 //                                             mContextValues.size(),
//                 //                                             mValues.size(),
//                 //                                             this.name));
//                 break;
//             }

            
        }
    }

    public boolean hasContextValue(String value) {
        return mContextValues != null && mContextValues.contains(value);
    }
    
    public int countContextValue(String value) {
        return mContextValues == null ? 0 : mContextValues.count(value);
    }
        
    public int getContextValueCount() {
        return mContextValues == null ? 0 : mContextValues.size();
    }

    protected void flushStats() {
        flushStats(false);
    }
    private void flushStats(boolean init) {
        if (!init) Log.debug("flushing " + this);
        // reset to initial defaults
        allValuesUnique = true;
        valueCount = 0;
        enumDisabled = false;
        maxValueLen = 0;
        isNumeric = true;
        if (mValues != null)
            mValues.clear();
        // keep nodeStyle -- the whole reason we use a flush instead of
        // just creating new Schema+Field objects when reloading
    }

    public void setStyleNode(LWComponent style) {
        if (nodeStyle != null)
            Log.warn("resetting field style " + this + " to " + style);
        nodeStyle = style;
    }

    public boolean hasStyleNode() {
        return nodeStyle != null;
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
        
    /** @return true if all the values for this Field have been fully tracked and recorded, and more than one
     * unique value was found */
    public boolean isEnumerated() {
        return !enumDisabled && uniqueValueCount() > 1;
    }

    /** @return true if this field appeared a single time in the entire data set, with a single value
     * This can generally only be true for fields from an XML data-set, in which a single-value
     * "column" is in effect created by an XML key that only appears once.
     */
            
    public boolean isSingleton() {
        return allValuesUnique && (mValues != null && count(mValues) < 2);
    }
        
    /** @return true if every value found for this field has the same value.
     * Will always be true if isSingleton() is true
     */
    public boolean isSingleValue() {
        return uniqueValueCount() == 1;
    }

    protected int valueCount() {
        //return values == null ? 0 : values.size();
        return valueCount;
    }

    public int getEnumValueCount() {
        return isEnumerated() ? uniqueValueCount() : -1;
    }
        
    protected int uniqueValueCount() {
        return mValues == null ? valueCount() : mValues.entrySet().size();
    }
    
    private static int count(Multiset m) {
        // very annoying: apparently to fulfill the java.util.Collection contract,
        // Multiset.size() returns the *virtual* count of items in the set, not
        // the unqiue items as a counting HashMap impl would do -- we have to actually
        // pull the entrySet and count that to get the count of unique values.
        // Forunately, the impl appears to cache the entrySet, so it's not
        // creating a new one each time.  (The elementSet is also cached, tho
        // in the currently impl, the entrySet has to do a tad less delegation
        // to extract the backingMap size)
        return m == null ? 0 : m.entrySet().size();
    }

    public int getMaxValueLength() {
        return maxValueLen;
    }

    public Set<String> getValues() {
        // todo: should return unmodiable set: the set from elementSet() can modify the backing Multiset
        return mValues == null ? Collections.EMPTY_SET : mValues.elementSet();
    }

    private static final Multiset EMPTY_MULTISET = Multisets.unmodifiableMultiset(Multisets.newHashMultiset());
        
    // todo: make a multi-set
    public Multiset<String> getValueSet() {
        return mValues == null ? EMPTY_MULTISET : Multisets.unmodifiableMultiset(mValues);
        //return mValues == null ? EMPTY_MULTISET : mValues;
    }
//     public Map<String,Integer> getValueMap() {
//         return mValues == null ? Collections.EMPTY_MAP : mValues;
//     }

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
                mValues = null;
                enumDisabled = true;
                isNumeric = false;
                return;
            }
                
            if (mValues == null) {
                mValues = Multisets.newHashMultiset();
            } else if (mValues.contains(value)) {
                allValuesUnique = false;
            }
            mValues.add(value);
            //Log.debug(this + " added " + value + "; size=" + count(mValues));

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


    private String sampleValues(boolean unique) {

        if (count(mValues) <= 20)
            return unique ? mValues.elementSet().toString() : mValues.toString();
                
        final StringBuilder buf = new StringBuilder("[examples: ");

        int count = 0;
        for (String s : mValues.elementSet()) {
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
        if (mValues == null) {
            if (valueCount == 0)
                return "(empty)";
            else
                return String.format("%5d values (un-tracked; max-len%6d)", valueCount, maxValueLen);
        }
        else if (isSingleton()) {
            return "singleton" + mValues.elementSet();
        }
        else if (allValuesUnique) {
            if (count(mValues) > 1) {
                return String.format("%5d unique, single-instance values; %s", count(mValues), sampleValues(true));
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
            return String.format("%5d values, %4d unique: %s", valueCount(), count(mValues), sampleValues(false));
        //return String.format("%5d unique values in %5d; %s", values.size(), valueCount(), sampleValues(false));
                
    }
}
