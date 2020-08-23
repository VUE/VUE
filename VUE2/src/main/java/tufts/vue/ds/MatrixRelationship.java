package tufts.vue.ds;

public class MatrixRelationship {

	private String relationLabel;
	private String fromLabel;
	private String toLabel;
	
	public MatrixRelationship()
	{
		relationLabel=fromLabel=toLabel=null;
	}
	public MatrixRelationship(String fromLabel,String toLabel, String relationLabel)
	{
		this.relationLabel=relationLabel;
		this.fromLabel=fromLabel;
		this.toLabel = toLabel;		
	}
	public void setRelationLabel(String relationLabel) {
		this.relationLabel = relationLabel;
	}
	public String getRelationLabel() {
		return relationLabel;
	}
	public void setFromLabel(String fromLabel) {
		this.fromLabel = fromLabel;
	}
	public String getFromLabel() {
		return fromLabel;
	}
	public void setToLabel(String toLabel) {
		this.toLabel = toLabel;
	}
	public String getToLabel() {
		return toLabel;
	}
}
