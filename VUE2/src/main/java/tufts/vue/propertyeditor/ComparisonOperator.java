package tufts.vue.propertyeditor;
/**
 * ComparisonOperator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.3 Oct 05, 2005 (05:23:37 EDT) WSDL2Java emitter.
 */

public class ComparisonOperator implements java.io.Serializable {
    private java.lang.String _value_;
    private static java.util.HashMap _table_ = new java.util.HashMap();

    // Constructor
    protected ComparisonOperator(java.lang.String value) {
        _value_ = value;
        _table_.put(_value_,this);
    }

    public static final java.lang.String _has = "has";
    public static final java.lang.String _eq = "eq";
    public static final java.lang.String _lt = "lt";
    public static final java.lang.String _le = "le";
    public static final java.lang.String _gt = "gt";
    public static final java.lang.String _ge = "ge";
    public static final ComparisonOperator has = new ComparisonOperator(_has);
    public static final ComparisonOperator eq = new ComparisonOperator(_eq);
    public static final ComparisonOperator lt = new ComparisonOperator(_lt);
    public static final ComparisonOperator le = new ComparisonOperator(_le);
    public static final ComparisonOperator gt = new ComparisonOperator(_gt);
    public static final ComparisonOperator ge = new ComparisonOperator(_ge);
    public java.lang.String getValue() { return _value_;}
    public static ComparisonOperator fromValue(java.lang.String value)
          throws java.lang.IllegalArgumentException {
        ComparisonOperator enumeration = (ComparisonOperator)
            _table_.get(value);
        if (enumeration==null) throw new java.lang.IllegalArgumentException();
        return enumeration;
    }
    public static ComparisonOperator fromString(java.lang.String value)
          throws java.lang.IllegalArgumentException {
        return fromValue(value);
    }
    public boolean equals(java.lang.Object obj) {return (obj == this);}
    public int hashCode() { return toString().hashCode();}
    public java.lang.String toString() { return _value_;}
 
    /*public java.lang.Object readResolve() throws java.io.ObjectStreamException { return fromValue(_value_);}
    public static org.apache.axis.encoding.Serializer getSerializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new org.apache.axis.encoding.ser.EnumSerializer(
            _javaType, _xmlType);
    }
    public static org.apache.axis.encoding.Deserializer getDeserializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new org.apache.axis.encoding.ser.EnumDeserializer(
            _javaType, _xmlType);
    }
    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(ComparisonOperator.class);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.fedora.info/definitions/1/0/types/", "ComparisonOperator"));
    }
    
    public static org.apache.axis.description.TypeDesc getTypeDesc() {
        return typeDesc;
    }
*/
}
