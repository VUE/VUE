package edu.tufts.vue.mbs;

import java.util.List;
import java.util.Properties;

import tufts.vue.LWComponent;

public interface LWComponentAnalyzer {

	public List<AnalyzerResult> analyze(LWComponent c, boolean tryFallback);
	public List<AnalyzerResult> analyze(LWComponent c);
	public String getAnalyzerName();
}
