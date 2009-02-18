package edu.tufts.vue.mbs;

public class AnalyzerResult {

	private String type;
	private String value;
	
	public AnalyzerResult(String type, String value)
	{
		this.type = type;
		this.value = value;
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
	
}
