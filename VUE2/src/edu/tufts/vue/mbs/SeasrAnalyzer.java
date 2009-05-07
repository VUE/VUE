package edu.tufts.vue.mbs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.beans.XMLDecoder;
import tufts.vue.LWComponent;
import java.net.URL;

public class SeasrAnalyzer implements LWComponentAnalyzer {
	private static final String ANALYZER_NAME = "Seasr Web Page Analyzer";
	@SuppressWarnings("unchecked")
	public List<AnalyzerResult> analyze(LWComponent c, boolean tryFallback) {
		// TODO Auto-generated method stub
		List<AnalyzerResult> results = new ArrayList<AnalyzerResult>();
		try {
			URL  url = new URL("http://localhost:1715/service/ping?url="+c.getLabel());

			XMLDecoder decoder = new XMLDecoder(url.openStream());
			Map<String,Integer> map = (Map<String,Integer>) decoder.readObject();
			for(String key: map.keySet()) {
			results.add(new AnalyzerResult("NA", key));
			}
 		}catch(Exception ex) {
			ex.printStackTrace();
		}
		return results;
	}

	public List analyze(LWComponent c)
	{
		return analyze(c,true);
	}

	public String getAnalyzerName() {
		// TODO Auto-generated method stub
		return null;
	}
}