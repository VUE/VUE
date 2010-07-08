package edu.tufts.vue.mbs;

import java.util.ArrayList;

public class AnalyzerResult {

    private String type;
    private ArrayList subtypes;
    private String value;
    private double relevance;
    private int count;
    
    public AnalyzerResult(String type, String value)
    {
        this.type = type;
        this.value = value;
        this.relevance=0.0;
    }
    
    public AnalyzerResult(String type, String value, double relevance, int count)
    {
        this.type = type;
        this.value = value;
        this.relevance=relevance;
        this.count = count;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    public String getType() {
        return type;
    }
    public void setValue(String value) {
        this.value = value;
    }
    public String getValue() {
        return value;
    }
    public void setRelevance(double relevance) {
        this.relevance = relevance;
    }
    public double getRelevance() {
        return relevance;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }
    
    public ArrayList getSubtypes() {
        return subtypes;
    }
    
    public void initSubtypes() {
        subtypes = new ArrayList();
    }
    
    public void addSubtypes(ArrayList toAdd) {
        subtypes.addAll(toAdd);
    }
    
}
