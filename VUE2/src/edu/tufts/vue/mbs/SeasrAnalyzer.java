package edu.tufts.vue.mbs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.beans.XMLDecoder;
import tufts.vue.LWComponent;
import tufts.vue.Resource;
import tufts.vue.VueUtil;

import java.io.IOException;
import java.net.URL;
import tufts.vue.VueResources;
import edu.tufts.seasr.Flow;

import com.google.common.collect.Multimap;


public class SeasrAnalyzer implements LWComponentAnalyzer {
	public static final String DEFAULT_FLOW_URL = "http://vue-dl.tccs.tufts.edu:1719/service/ping";
	public static final String DEFAULT_INPUT="location";
	private static final String ANALYZER_NAME = "Seasr Web Page Analyzer";
	private Flow flow;
	public SeasrAnalyzer(Flow flow)  {
		this.flow = flow;
	}
			
	
	public Flow getFlow() {
		return flow;
	}
	public List<AnalyzerResult> analyze(LWComponent c, boolean tryFallback) {
		List<AnalyzerResult> results = new ArrayList<AnalyzerResult>();
		try {
			URL  url = new URL(DEFAULT_FLOW_URL+"?"+DEFAULT_INPUT+"="+c.getLabel());

			
			if(flow != null) {
				url = new URL(flow.getUrl()+"?"+flow.getInputList().get(0)+"="+ getSpecFromComponent(c));
			}
			XMLDecoder decoder = new XMLDecoder(url.openStream());
			Map map =  (Map)decoder.readObject();
			for(Object key: map.keySet()) {
			results.add(new AnalyzerResult(key.toString(), map.get(key).toString()));
			}
 		}catch(Exception ex) {
			ex.printStackTrace();
			VueUtil.alert("Can't Execute Flow on the node "+c.getLabel(), "Can't Execute Seasr flow");
		}
		return results;
	}

	public List<AnalyzerResult> analyze(String urlString, boolean tryFallback) {
		List<AnalyzerResult> results = new ArrayList<AnalyzerResult>();

		try {
			URL  url;

			if (flow == null) {
				url = new URL(DEFAULT_FLOW_URL + "?" + DEFAULT_INPUT + "=" + urlString);
			} else {
				url = new URL(flow.getUrl() + "?" + flow.getInputList().get(0) + "=" + urlString);
				
			}
			System.out.println("Executing: "+url.toString());
			XMLDecoder decoder = new XMLDecoder(url.openStream());
			Map map =  (Map)decoder.readObject();
			for(Object key: map.keySet()) {
			results.add(new AnalyzerResult(key.toString(), map.get(key).toString()));
			} 		} catch(Exception ex) {
			ex.printStackTrace();
			VueUtil.alert("Can't Execute Flow on the url " + urlString, "Can't Execute Seasr flow");
		}

		return results;
	}

	private String getSpecFromComponent(LWComponent c) throws Exception   {
		if (c !=null)
		{
			Resource r = c.getResource();
			if(r != null) {
				String spec = r.getSpec();
				if (spec.startsWith("http") || spec.startsWith("https")){
					return spec;
	 			}	
			}else if(c.getLabel().startsWith("http") || c.getLabel().startsWith("https")) {
				return c.getLabel();
			}
		}
		throw new Exception("No URL");
	}
	
	public List analyze(LWComponent c)
	{
		return analyze(c,true);
	}

	public String getAnalyzerName() {
		// TODO Auto-generated method stub
		return null;
	}

	public Multimap<String, AnalyzerResult> analyzeResource(LWComponent c) {
		// TODO Auto-generated method stub
		return null;
	}
}