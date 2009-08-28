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

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import tufts.vue.LWComponent;
import tufts.vue.DEBUG;

import com.google.common.collect.*;
import org.apache.commons.lang.StringEscapeUtils;

/**
 * Represents a column in a data-set, or pseudo-column from an XML mapped data-set.
 *
 * Besides simply recording the name of the column, this class mainly provides
 * data-analysis of on all the values found in the column, discovering enumerated
 * types and doing some data-type analysis.  It also includes the ability to
 * associate a LWComponent node style with specially marked values.
 * 
 * @version $Revision: 1.17 $ / $Date: 2009-08-28 17:13:05 $ / $Author: sfraize $
 * @author Scott Fraize
 */

public class Field implements tufts.vue.XMLUnmarshalListener
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(Field.class);

    public static final String EMPTY_VALUE = "";

    public static final String TYPE_TEXT = "TEXT";
    public static final String TYPE_INTEGER = "INTEGER";
    public static final String TYPE_DECIMAL = "DECIMAL";
    public static final String TYPE_DATE = "DATE";
    public static final String TYPE_QUANTILE = "QUANTILE";

    private static final int MAX_ENUM_VALUE_LENGTH = 192;
    private static final int MAX_DATE_VALUE_LENGTH = 40;
    private static final DateFormat DateParser = DateFormat.getDateTimeInstance();

    private Schema schema; // should be final, but not due to castor persistance
    private String name;

    /** the number of actual (non-empty) values that have been inspected for analysis */
    private int mValuesSeen;
    /** the string length of the longest value seen */
    private int mMaxValueLen;
    /** if true, all values found were unique -- there were no repeated values */
    private boolean mAllValuesUnique;
    /** if true, the values were too long to meaninfully track and enumerate */
    private boolean mValueTrackDisabled;
    
    /** map of all possible unique values for enumeration tracking */
    private final Multiset<String> mValues = LinkedHashMultiset.create();

    private String mType = TYPE_INTEGER; // starts most specific as default, is cleared upon finding anything else
    private boolean mTypeDetermined = false;
    
    private final Collection<String> mDataComments = new ArrayList();
        
    /** map of values currently present in a given context (e.g., a VUE map) */
    private Multiset<String> mContextValues;

    private LWComponent mNodeStyle;
    
    //========================================================================================
    // These variables are only relevant to Fields numeric type:

    private static final int QUANTILE_BUCKETS = 4; // # of quantile ranges to create (4=quartiles, 5=quintiles, etc)

    private double mMinValue = Double.MAX_VALUE;
    private double mMaxValue = Double.MIN_VALUE;
    private double mValuesTotal;
    private double mMeanValue;
    private double mMedianValue; 
    private double mStandardDeviation;
    private double[] mQuantiles;
    private boolean mAllValuesAreIntegers = true; // defaults true: won't be valid until final analysis
    //========================================================================================
    

    private transient boolean mXMLRestoreUnderway;

    /**
     * A persistant reference to a Field for storing associations in maps via castor.
     * Note variable names in this class don't have more than one cap letter to best
     * work with castor auto-mappings.  Changing the variable names here will break
     * persistance for previously stored associations under the old names.
     */
    public static final class PersistRef {
        public String
            fieldName,
            schemaName,
            schemaId,
            schemaGuid,
            schemaDsguid;

        @Override public String toString() {
            return String.format("FieldRef[%s.%s %s/%s]", schemaName, fieldName, schemaId, schemaGuid);
        }

        public PersistRef() {} // for castor

        PersistRef(Field field) {
            final Schema s = field.getSchema();
            schemaId = s.getMapLocalID();
            schemaGuid = s.getGUID();
            schemaDsguid = s.getDSGUID();
            schemaName = s.getName();
            fieldName = field.getName();
        }
    }

    /** for castor persistance */
    public Field() {
        this.name = "<empty>";
    }

    private transient Collection<PersistRef> mRelatedFields;

    Collection<PersistRef> getRelatedFieldRefs() {
        return mRelatedFields;
    }

    Field(String n, Schema schema) {
        this.name = n;
        setSchema(schema);
        flushStats(true);
        if (DEBUG.SCHEMA) {
            Log.debug("instanced " + Util.tags(this));
            //Log.debug("instanced " + Util.tags(this), new Throwable("HERE"));
        }
    }

//     /** for castor persistance */
//     public final String getMapLocalID() {
//         return String.format("%s.%s", schema.getMapLocalID(), name);
//     }
    
    /** must be called by parent Schema after de-serialization (needed for persistance) */
    void setSchema(Schema s) {
        this.schema = s;
    }
    
    /** for persistance of associations */
    public Collection<PersistRef> getRelatedFields() {
        if (mXMLRestoreUnderway) {
            return mRelatedFields;
        } else {
            Collection<PersistRef> persists = new ArrayList();
            for (Field f : Association.getPairedFields(this)) {
                persists.add(new PersistRef(f));
            }
//             if (DEBUG.SCHEMA && persists.size() > 0) {
//                 Log.debug(this + ": GOT RELATED FIELDS: " + Util.tags(persists));
//             }
            return persists;
        }
    }

    /** interface {@link XMLUnmarshalListener} -- init */
    public void XML_initialized(Object context) {
        mXMLRestoreUnderway = true;
        mRelatedFields = new HashSet();
    }
    
    /** interface {@link XMLUnmarshalListener} -- track us */
    public void XML_completed(Object context) {
        mXMLRestoreUnderway = false;
        if (mRelatedFields.size() > 0) {
            Log.debug("GOT RELATED FIELDS for " + this);
            Util.dump(mRelatedFields);
            // todo: later, process to re-construct associations
        } else
            mRelatedFields = Collections.EMPTY_LIST;
    }
    
    /** Wrapper for display of special values: e.g., EMPTY_VALUE ("") to "(no value)" */
    public static String valueText(Object value) {
        if (value == null)
            return null;
        else if (value == EMPTY_VALUE)
            return "(no value)";
        else
            return value.toString();
    }

    public String valueDisplay(Object value) {

        final String display;
        
        if (isQuantile()) {
            display = getName() + ": " + StringEscapeUtils.escapeHtml(valueText(value));
        } else {
            display = StringEscapeUtils.escapeHtml(valueText(value));
        }
        //Log.debug(this + "; valueDisplay: " + value + " -> " + Util.tags(display));
        return display;
    }

    public int countValues(String value) {
        return mValues.count(value);
    }

    void annotateIncludedValues(final Collection<LWComponent> nodes) {
        if (mValues == null || count(mValues) < 1) {
            if (mContextValues != null)
                mContextValues.clear();
            return;
        }
        if (mContextValues == null)
            mContextValues = HashMultiset.create();
        else
            mContextValues.clear();
        if (DEBUG.META) Log.debug("MARKING INCLUDED VALUES AGAINST " + nodes.size() + " NODES for " + this);

        final Set<String> valuesToCheck = mValues.elementSet();
        for (LWComponent c : nodes) {
            for (String value : valuesToCheck) {
                //if (c.getDataSchema() == schema && c.hasDataValue(this.name, value)) {
                if (c.hasDataValue(this.name, value)) {
                    if (!c.isDataValueNode())
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
        
        mValues.clear();
        mValuesSeen = 0;
        mValueTrackDisabled = false;
        mAllValuesUnique = true;
        mAllValuesAreIntegers = true;
        mMaxValueLen = 0;
        mType = TYPE_INTEGER;
        mTypeDetermined = false;
        mDataComments.clear();
        
        mMinValue = Double.MAX_VALUE;
        mMaxValue = Double.MIN_VALUE;
        mValuesTotal = 0;
        mMeanValue = 0;
        mMedianValue = 0;
        mStandardDeviation = 0;
        mQuantiles = null;
        
        // we keep the nodeStyle, which is the whole reason we use a flush instead of
        // just creating new Schema+Field objects when reloading.  Tho at this point,
        // may be easier to re-create all & just carry over the styles.
    }

    /** for persistance */
    public void setStyleNode(LWComponent style) {
        if (DEBUG.SCHEMA) Log.debug(this + " setStyleNode " + style);
//         if (mNodeStyle != null)
//             Log.warn("resetting field style " + this + " to " + style, new Throwable("HERE"));
        mNodeStyle = style;
    }

    public boolean hasStyleNode() {
        return mNodeStyle != null;
    }
        
    public LWComponent getStyleNode() {
        return mNodeStyle;
    }

    public String getName() {
        return name;
    }
    
    /** for castor persistance only */
    public void setName(String s) {
        name = s;
    }
        
    public Schema getSchema() {
        return schema;
    }

    public String getType() {
        return mType;
    }

    public boolean isNumeric() {
        return getType() == TYPE_DECIMAL || getType() == TYPE_INTEGER;
    }

    private static final String NoCause = "(explicit-type-set)";
    
    private void takeType(String type, String cause) {
        if (DEBUG.Enabled) Log.debug(toTerm() + " type=>" + type + " on " + Util.tags(cause));
        mType = type;
    }
    
    private void setType(String type) {
        setType(type, NoCause);
    }
    
    private void setType(String type, String cause) {
        takeType(type, cause);
        mTypeDetermined = true;
    }

    public boolean isQuantile() {
        return mType == TYPE_QUANTILE;
    }

    @Override
    public String toString() {
        if (schema == null)
            return String.format("<?>.%s", getName());
        else
            return String.format("%s.%s", schema.getName(), getName());
    }

    public String toTerm() {
        return Relation.quoteKey(this);
    }
    
    
//     @Override
//     public String toString() {
//         //if (isNumeric) type=TYPE_DECIMAL; // HACK: NEED ANALYSIS PHASE
//         //return getName();

//         final String numeric = isNumeric ? "/NUMERIC" : "";

//         //final String name = schema.getName() + "." + getName();
//         final String name = getName();
        
//         if (mValuesSeen() == 1)
//             //return String.format("<html><code>%s</code>:<br>\"%s\"", getName(), getValues().toArray()[0]);
//             return String.format("%-14s=\"%s\"", name, getValues().toArray()[0]);
//         else if (mAllValuesUnique)
//             return String.format("%-14s (%d)/%s%s", name, mValuesSeen(), type, numeric);
//         else
//             return String.format("%-14s [%d]/%s%s", name, uniqueValueCount(), type, numeric);
//     }

    public boolean isPossibleKeyField() {
        //return mAllValuesUnique && mValuesSeen == schema.getRowCount() && !(type == TYPE_DATE);
        return !mValueTrackDisabled
            && mAllValuesUnique
            && uniqueValueCount() == valueCount()
            && valueCount() == schema.getRowCount()
            && !(mType == TYPE_DATE);
    }

    /** @return true if this is the schema's unique key field */
    public boolean isKeyField() {
        return schema.getKeyField() == this;

//         boolean t = (schema.getKeyField() == this);
//         Log.debug(String.format("isKeyField=%s %s", t ? "YES" : "no", Util.tags(this)));
//         return t;
    }

    public boolean isUntrackedValue() {
        return mValueTrackDisabled;
    }

        
    /** @return true if all the values for this Field have been fully tracked and recorded, and more than one
     * unique value was found */
    public boolean isEnumerated() {
        return !mValueTrackDisabled && uniqueValueCount() > 1;
    }

    /** @return true if this field appeared a single time in the entire data set.
     * This can generally only be true for fields from an XML data-set, in which a single-value
     * "column" is in effect created by an XML key that only appears once, such as keys
     * that apply to the entire feed.
     */
    public boolean isSingleton() {
        return mAllValuesUnique && (mValues != null && count(mValues) < 2);
    }
        
    /** @return true if every value found for this field has the same value.
     * Will always be true if isSingleton() is true
     */
    public boolean isSingleValue() {
        return uniqueValueCount() == 1;
    }

    /** @return the instance value count: the number of a times any value appeared for this field (includes repeats) */
    protected int valueCount() {
        return mValuesSeen;
    }

    public int getEnumValuesSeen() {
        return isEnumerated() ? uniqueValueCount() : -1;
    }
        
    protected int uniqueValueCount() {
        if (mValues == null) {
            if (mMaxValueLen == 0)
                return 0;
            else
                return valueCount();
        } else {
            return count(mValues);
        }

        //return mValues == null ? valueCount() : mValues.entrySet().size();
    }
    
    /** @return the count of all unique values in the Multiset */
    private static int count(Multiset m) {
        // to fulfill the java.util.Collection contract, Multiset.size() returns the *virtual*
        // count of items in the set, not the unqiue items as a counting HashMap impl would do --
        // we have to actually pull the entrySet/elementSet and count that to get the count of
        // unique values.  Forunately, the impl appears to cache the entrySet, so it's not creating
        // a new one each time.  (The elementSet is also cached, tho in the current google impl, the
        // entrySet has to do a tad less delegation to extract the backingMap size)

        return m == null ? 0 : m.entrySet().size();
        //return m == null ? 0 : m.elementSet().size();
    }

    public int getMaxValueLength() {
        return mMaxValueLen;
    }

    /**
     * @return the set of all unique values this Field has been seen to take amonst all rows in
     * the data-set.  Note that the returned set is modifiable, and should NOT be modified.
     */
    public Set<String> getValues() {
        return mValues.elementSet();
        // note: the set from elementSet() can modify the backing Multiset
        //return mValues == null ? Collections.EMPTY_SET : mValues.elementSet();
    }

    private static final Multiset EMPTY_MULTISET = Multisets.unmodifiableMultiset(HashMultiset.create(0));
        
    public Multiset<String> getValueSet() {
        return mValues == null ? EMPTY_MULTISET : Multisets.unmodifiableMultiset(mValues);
        //return mValues == null ? EMPTY_MULTISET : mValues;
    }
//     public Map<String,Integer> getValueMap() {
//         return mValues == null ? Collections.EMPTY_MAP : mValues;
//     }

    // todo: may want to move this to a separate analysis code set
    void trackValue(String value) {

        if (value == null)
            return;

        final int valueLen = value.length();

        if (valueLen > mMaxValueLen)
            mMaxValueLen = valueLen;

        if (mValueTrackDisabled)
            return;
            
        if (value == EMPTY_VALUE) {
            ; // don't increment value count
        } else if (valueLen == 0) {
            value = EMPTY_VALUE; // don't increment value count
        } else {
            mValuesSeen++;
        }
                
        if (mValuesSeen > 1 && value.length() > MAX_ENUM_VALUE_LENGTH) {
            mValueTrackDisabled = true;
            setType(TYPE_TEXT, value);
            return;
        }
        
        if (mValues.contains(value))
            mAllValuesUnique = false;

        mValues.add(value);
        //Log.debug(this + " added " + value + "; size=" + count(mValues));

        if (value == EMPTY_VALUE)
            return;

        if (!mTypeDetermined)
            trackForTypeInference(value);

    }

    // the inferencing depends on not passing this method null or empty values
    private void trackForTypeInference(final String text)
    {
        if (text.indexOf(':') > 0) {
            if (isDateValue(text)) {
                // THIS IS A MAJOR GUESS: we guess it's a date field if we see a single valid date
                setType(TYPE_DATE, text);
            } else {
                // having seen a ':' but not being a date, we infer that this is text (e.g., not numeric)
                setType(TYPE_TEXT, text);
            }
        } else {
            final double number = getNumericValue(text, true);
            if (Double.isNaN(number)) {
                // the first non-numeric we see, mark us as text
                setType(TYPE_TEXT, text);
            } else {
                //Log.debug(Util.tags(text) + " = " + number);
                if (number < mMinValue)
                    mMinValue = number;
                else if (number > mMaxValue)
                    mMaxValue = number;
                mValuesTotal += number;
                if (mAllValuesAreIntegers && number != (long) number) {
                    mAllValuesAreIntegers = false;
                    takeType(TYPE_DECIMAL, text); // do NOT use setType -- this is still a guess, value is not determined yet
                }
            }
        }
    }

    private double getNumericValue(final String text) {
        return getNumericValue(text, true);
    }
    
    // DecimalFormat's are not synchronized, thus these cannot be static.
    private final NumberFormat LocalNumberFormat = NumberFormat.getInstance();
    //private final NumberFormat LocalCurrencyFormat = NumberFormat.getCurrencyInstance();

    /** @return double value if one found, Double.NaN otherwise */
    private double getNumericValue(final String text, final boolean tryCurrency) {

    	try {
            // Double.parseDouble handles most stuff, including "0x2F" style
            // hex values was well as scientific notation.
            return Double.parseDouble(text);
    	} catch (Throwable t) {}

        Number value = null;

    	try {
            // This handles values of the form "1,234,567". It will also extract any
            // number that can be found at the head of a string: e.g. "7foo" will return
            // 7, or "70%" will return 70 (*not* 0.70).  The instance of LocalNumberFormat will
            // generally be a DecimalFormat
            value = LocalNumberFormat.parse(text);
    	} catch (Throwable t) {}

        // Note that if we use a NumberFormat.getCurrencyInstance() here to handle
        // currency, it will only allow the local currency symbol.

        if (value == null && tryCurrency && text.length() > 1 && isCurrencySymbol(text.codePointAt(0))) {
            value = getNumericValue(text.substring(1), false); // NOTE RECURSION
            //Log.debug("HANDLED CURRENCY " + Util.tags(text) + " = " + Util.tags(value));
        }

        // could allow for percent parsers that return value/100
        
        if (DEBUG.SCHEMA || DEBUG.DATA) Log.debug(Util.tags(text) + " = " + Util.tags(value));
        
        return value == null ? Double.NaN : value.doubleValue();
    }
    
    private static boolean isCurrencySymbol(int c) 
    {
        // checking '$' should be redundant
        return c == '$' || Character.getType(c) == Character.CURRENCY_SYMBOL;
    }
    
    
    private static boolean isDateValue(String value) {
        Date date = null;

        try {
            date = new Date(value);
            if (DEBUG.Enabled) Log.debug("PARSED DATE: " + Util.tags(date) + " from " + value);
        } catch (Throwable t) {
            if (DEBUG.DATA) Log.debug("Failed to parse [" + value + "] as date: " + t);
        }
                    
//         try {
//             date = DateParser.parse(value);
//         } catch (java.text.ParseException e) {
//             eoutln("Failed to parse [" + value + "] as date: " + e);
//             return false;
//         }

        return date != null;
    }

//     private static boolean isNumericValue(String value) {
//     	try {
//             Double.parseDouble(value);
//     	} catch (Throwable t) {
//             //if (DEBUG.SCHEMA) Log.info(t);
//             return false;
//     	}
//         return true;
//     }

    /** compute quantiles via median values and return the absolute median */
    private static double computeQuantiles(final double[] quantiles, final double[] values) {
        
        // Note: The quantile ranges will change depending on how the boundaries are handled
        // (e.g., off-by-one differences in computing which index to use). There does not
        // appear to be a commonly agreed upon method of resolving this in either direction.

        Arrays.sort(values);

        final boolean EVEN_REGIONS = (quantiles.length % 2 != 0);
        
        if (DEBUG.Enabled) Log.debug("count of all possible values: " + values.length);
        //for (int i = 0; i < values.length; i++) Log.debug("v" + i + ": " + values[i]);

        final int regions = quantiles.length + 1;
        final float range = (float) values.length / (float) regions;
        if (DEBUG.Enabled) Log.debug("each of " + regions + " quantile regions has an approx sample size of: " + range + " samples");
        
        // TODO: the below median computation for ranges with an even # of buckets should
        // be done for each range
        
        for (int i = 0; i < quantiles.length; i++) {
            final float rawIndex = (i+1) * range;
            //final int index = Math.round(rawIndex);
            final int index = (int) Math.floor(rawIndex); // using floor will exactly align middle index in odd numbered value sets
            quantiles[i] = values[index];
            if (DEBUG.Enabled) Log.debug(String.format("quantile %d index %3.2f (%d) value = " + values[index], i, rawIndex, index));
        }

        // If the number of buckets is even (and thus the # of quantile values needed is odd),
        // the middle quantile will be the median.

        final double median;
        final int halfIndex = values.length / 2;

        if (values.length % 2 == 0) {
            // even # of sample values -- absolute median must be computed separately by averaging middle two values
            final double belowMedian = values[halfIndex - 1];
            final double aboveMedian = values[halfIndex];
            median = (belowMedian + aboveMedian) / 2.0;
            if (DEBUG.Enabled) Log.debug(String.format("AVERAGED MEDIAN: %g from %g+%g halfIndex=%d",
                                                       median, belowMedian, aboveMedian, halfIndex));
        } else {
            // odd # of sample values -- median already represented by the middle value
            median = values[halfIndex];
            if (DEBUG.Enabled) Log.debug(String.format("PICKED MEDIAN: %g from exact middle index=%d",
                                                       median, halfIndex));
            //median = quantiles[(quantiles.length + 1) / 2 - 1];
        }

        if (EVEN_REGIONS) {
            if (quantiles[quantiles.length / 2] != median) {
                if (DEBUG.Enabled) Log.info(String.format("PATCHING MIDDLE QUANTILE TO ABSOLUTE MEDIAN; %g -> %g",
                                                          quantiles[quantiles.length / 2], median));
                quantiles[quantiles.length / 2] = median;
            }
        }

        return median;
    }

    private static final boolean USE_STANDARD_QUANTILES = false;
    private static final boolean USE_COMPRESSED_SAMPLE_QUANTILES = !USE_STANDARD_QUANTILES; // ignore repeated values in sample set
    
    /** compute and record the quantile values as well as the median value */
    private void computeQuantiles(final double[] allValues)
    {
        // NOTE: for data-sets with many repeated values, several of the quantiles may cover exactly
        // the same range of values.  Adding another type of analysis for that case would
        // be useful, or perhaps rolling our own "modified quantile" analysis that forces
        // quantiles to cover different values.
        
        // E.g: if QUANTILE_BUCKETS=4 (we want 4 buckets), we need to produce 3 (three) quantile
        // values to divide the range into 4 (four) regions
        
        mQuantiles = new double[QUANTILE_BUCKETS - 1];

        if (USE_STANDARD_QUANTILES) {
            // this will fill mQuantiles with appropriate values
            mMedianValue = computeQuantiles(mQuantiles, allValues);
            
        } else {

            // our own method of computing the quantiles (perhaps recapitulating an
            // existing named method I haven't seen) that removes all repeated values in
            // the sample set
            
            int validCount = mValues.elementSet().size();
            if (mValues.contains(EMPTY_VALUE))
                validCount--;
            
            final double[] uniqueValues = new double[validCount];
            
            int i = 0;
            for (String s : mValues.elementSet())
                if (s != EMPTY_VALUE)
                    uniqueValues[i++] = getNumericValue(s, true);
            
            mMedianValue = computeQuantiles(mQuantiles, uniqueValues);
        }
    }


    private static final boolean SKEW_QUANTILES_LOW = false; // anecdotally "more balanced" when skewing high
    private static final boolean SKEW_QUANTILES_HIGH = !SKEW_QUANTILES_LOW;

    /** @return the quantile the given value is determined to lie in.  Will return values from 0 - (QUANTILE_BUCKETS-1) */
    private int getQuantile(final double value) {

        // note:
        // using "value <= mQuantiles[i]" skews data to lower quantiles
        // using "value <  mQuantiles[i]" skews data to higher quantiles
        
        if (SKEW_QUANTILES_LOW) {
            for (int i = 0; i < mQuantiles.length; i++)
                if (value <= mQuantiles[i]) 
                    return i;
        } else {
            for (int i = 0; i < mQuantiles.length; i++)
                if (value < mQuantiles[i]) 
                    return i;
        }
        return mQuantiles.length;
    }
    
    private String getQuantileName(int i) {

        // A 1.0 TOP_RANGE_ADJUSTMENT value on only works for integer ranges; won't work for
        // sub-integer value ranges.  This adjustment entirely depends on which way we skew in
        // getQuantile.

        // For non-integer values we just allow the quantile names to be ambiguously overlapping
        // (e.g., allow the MAX of one range to equal the MIN of the next range).

        final double TOP_RANGE_ADJUSTMENT;

        if (mAllValuesAreIntegers && SKEW_QUANTILES_HIGH) // could still adjust low, but would need different adjustment
            TOP_RANGE_ADJUSTMENT = 1.0;
        else
            TOP_RANGE_ADJUSTMENT = 0.0;

        final double min, max;

        if (i == 0)
            min = mMinValue;
        else
            min = mQuantiles[i - 1];

        if (i == mQuantiles.length)
            max = mMaxValue;
        else
            max = mQuantiles[i] - TOP_RANGE_ADJUSTMENT;
        
        if (mAllValuesAreIntegers)
            return String.format("Q%d: %.0f-%.0f", i+1, min, max);
        else
            return String.format("Q%d: %g-%g", i+1, min, max);
        
    }

    //private static final String[] QUANTILE_NAMES = { "Lowest", "Low", "Medium", "High", "Highest" };


    void performFinalAnalysis() {

        mTypeDetermined = true;
        
        if (!isNumeric() || uniqueValueCount() <= (QUANTILE_BUCKETS*3))
            return;

        if (isKeyField())
            return;
        
        //-----------------------------------------------------------------------------
        // Compute common summary statistics & quantiles
        //-----------------------------------------------------------------------------
            
        mMeanValue = mValuesTotal / mValuesSeen;

        // TODO: we could compute the quantile values in much less memory by using a
        // sorted-by-value version of the existing mValues Multiset, and iterating through it by
        // increasing "count" to find the appropriate median values.

        // performance: if all values are integers/longs, we could optimize the following codepaths to
        // use integer types & parsing code
        
        final double[] allValues;

        if (USE_STANDARD_QUANTILES)
            allValues = new double[mValuesSeen];
        else
            allValues = null;

        double totalSquaredDeviations = 0;
        int count = 0;
        
        for (DataRow row : schema.getRows()) {
            final String text = row.getValue(this);

            if (text == null) {
                // this should only happen in XML data-sets with fields that don't have
                // values in all rows
                continue;
            }
            
            final double value = getNumericValue(text);

            if (Double.isNaN(value))
                continue;
            
            if (USE_STANDARD_QUANTILES)
                allValues[count] = value;
            count++;
            
            final double meanDeviation = value - mMeanValue;
            
            totalSquaredDeviations += (meanDeviation * meanDeviation);
        }

        if (count != mValuesSeen) {
            Log.warn(this + Util.TERM_RED + ": COUNT != mValuesSeen; " + count + " != " + mValuesSeen + Util.TERM_CLEAR);
            return;
        }

        final double variance = totalSquaredDeviations / mValuesSeen;

        mStandardDeviation = Math.sqrt(variance);

        //-----------------------------------------------------------------------------
        // Create quantiles
        //-----------------------------------------------------------------------------

        computeQuantiles(allValues);
        
        //-----------------------------------------------------------------------------
        // Explicitly create quantile value records (we do this first only so they are ordered)
        //-----------------------------------------------------------------------------
        
        final double range = mMaxValue - mMinValue;
        
        //final String[] quantileNames = QUANTILE_NAMES.clone();
        final String[] quantileNames = new String[QUANTILE_BUCKETS];


        //final Field quantileField = this;
        final Field quantileField =
            schema.addFieldBefore(this,
                                  String.format("%s [Q%d]", getName(), QUANTILE_BUCKETS));

        quantileField.setType(TYPE_QUANTILE);

        //quantileField.setStyleNode(getStyleNode()); // TODO: WON'T WORK: style-node not yet set
        // duplicate v.s. crate new via data-action so we don't use up color schemes
        quantileField.setStyleNode(DataAction.initNewStyleNode(getStyleNode().duplicate()));
        
        //Util.printStackTrace("SETTING LABEL ON " + Util.tags(quantileField.getStyleNode() + " for " + this));
        quantileField.getStyleNode().setLabelTemplate(String.format("%s Range\n${%s}", getName(), quantileField.getName()));

        for (int i = 0; i < QUANTILE_BUCKETS; i++) {
            quantileNames[i] = getQuantileName(i);
            // We add the possible values now only to enforce the order in mValues for the DataTree            
            quantileField.mValues.add(quantileNames[i]); 
            //quantileField.trackValue(quantileNames[i]); 
        }

        //-----------------------------------------------------------------------------
        // Assign quantile values to all rows:
        //-----------------------------------------------------------------------------

        for (DataRow row : schema.getRows()) {
            final String text = row.getValue(this);

            if (text == null) {
                // this should only happen in XML data-sets with fields that don't have
                // values in all rows
                continue;
            }
            
            final double value = getNumericValue(text, true);
            final String quantileValue;
            
            if (Double.isNaN(value)) {
                quantileValue = Field.EMPTY_VALUE;
            } else {
                quantileValue = quantileNames[getQuantile(value)];
                row.addValue(quantileField, quantileValue);
            }

            // Don't bother to add quartile values for empty values
            //row.addValue(quantileField, quantileValue);
        }
        
        //-----------------------------------------------------------------------------
        
        if (DEBUG.Enabled) {
            //final double deviationQ = range / QUANTILE_BUCKETS;
            //quantileField.trackValue(String.format("(DeviationQ: %.1f)", deviationQ));
//             quantileField.mValues.add(String.format("(Std Dev: %.1f)", mStandardDeviation));
//             quantileField.mValues.add(String.format("(Segments: %.1f)", range / mStandardDeviation));
        }

        final double deviationsToCoverAllValues = range / mStandardDeviation; // # of std-dev's needed to cover all values

        if (mAllValuesAreIntegers) {
            quantileField.addDataComment(String.format("Mean: %.1f", mMeanValue));
            quantileField.addDataComment(String.format("Median: %.1f", mMedianValue));
            quantileField.addDataComment(String.format("Std Dev: %d x %.1f",
                                                    (int) Math.round(mStandardDeviation),
                                                    deviationsToCoverAllValues
                                                    //(int) Math.round(deviationsToCoverAllValues)
                                                    ));
        } else {
            quantileField.addDataComment(String.format("Mean: %g", mMeanValue));
            quantileField.addDataComment(String.format("Median: %g", mMedianValue));
            quantileField.addDataComment(String.format("Std Dev: %g x %.1f",
                                                    mStandardDeviation,
                                                    deviationsToCoverAllValues
                                                    ));
        }
    }

    public Collection<String> getDataComments() {
        return mDataComments;
    }

    private void addDataComment(String s) {
        mDataComments.add(s);
    }
    

// This code appears to be calculating a quantile by calculating the linear % location the value
// has within the total range of possible values. We're now computing quantiles using a standard definition of
// quantile / quartile that involves computing by median.
    
//     private int getQuantile(final double value) {
//         return getQuantile(mMinValue, mMaxValue, value, QUANTILE_BUCKETS);
//     }
    
//     private static int getQuantile
//         (final double min,
//          final double max,
//          final double value,
//          final int N)
//     {
//         final double ratio = (value-min) / (max-min);
//         final int quantile = (int) Math.ceil(ratio*N);
        
//         if (quantile <= 0) {
//             Log.warn("quantile="+quantile + " for value " + value);
//             return 1;
//         } else
//             return quantile;
//     }
    
//     private static String getQuantileRange
//         (final double min,
//          final double max,
//          final int quantile,
//          final int N)
//     {
//         final double lowVal = min + (max-min)*(quantile-1)/N;
//         final double highVal = min + (max-min)*(quantile)/N;

//         return String.format("%.1f-%.1f", lowVal, highVal);
//     }
    
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
            if (mValuesSeen == 0)
                return "(empty)";
            else
                return String.format("%5d values (un-tracked; max-len%6d)", mValuesSeen, mMaxValueLen);
        }
        else if (isSingleton()) {
            return "singleton" + mValues.elementSet();
        }
        else if (mAllValuesUnique) {
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

    /** interface {@link XMLUnmarshalListener} -- does nothing here */
    public void XML_fieldAdded(Object context, String name, Object child) {}
    /** interface {@link XMLUnmarshalListener} -- does nothing here */
    public void XML_addNotify(Object context, String name, Object parent) {}
    
}


// abstract class AbstractValue implements CharSequence {
//     final String value;
//     AbstractValue(String s) { value = s; }
//     public int length() { return value.length(); }
//     public char charAt(int index) { return value.charAt(index); }
//     public CharSequence subSequence(int start, int end) { return value.subSequence(start, end); }
//     public int compareTo(String anotherString) { return value.compareTo(anotherString); }
// }
// final class QValue extends AbstractValue {
//     public final int quantile;
//     QValue(String s, int qv) { super(s); quantile = qv; }
// }

