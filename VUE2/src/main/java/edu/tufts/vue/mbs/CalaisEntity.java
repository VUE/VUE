package edu.tufts.vue.mbs;

import com.clearforest.calais.simple.Entity;

public class CalaisEntity extends Entity{

	private double relevance;

	public CalaisEntity()
	{
		super();
	}
	public void setRelevance(double relevance) {
		this.relevance = relevance;
	}

	public double getRelevance() {
		return relevance;
	}
	
}
