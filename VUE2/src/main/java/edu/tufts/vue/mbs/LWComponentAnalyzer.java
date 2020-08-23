package edu.tufts.vue.mbs;

import java.util.List;
import java.util.Properties;

import com.google.common.collect.Multimap;

import tufts.vue.LWComponent;

public interface LWComponentAnalyzer {

	List<AnalyzerResult> analyze(LWComponent c, boolean tryFallback);
	List<AnalyzerResult> analyze(LWComponent c);
	Multimap<String,AnalyzerResult> analyzeResource(LWComponent c) throws Exception;
	String getAnalyzerName();
}
