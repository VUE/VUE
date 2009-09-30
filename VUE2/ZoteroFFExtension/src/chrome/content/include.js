// Only create main object once
//#if (!Zotero.VUEExport) {
//#	const loader = Components.classes["@mozilla.org/moz/jssubscript-loader;1"]		
//					.getService(Components.interfaces.mozIJSSubScriptLoader);
//	loader.loadSubScript("chrome://vueexport/content/vueexport.js");
//}
const jsLoader = Components.classes["@mozilla.org/moz/jssubscript-loader;1"]
                                    .getService(Components.interfaces.mozIJSSubScriptLoader);
jsLoader.loadSubScript("chrome://vueexport/content/yahoo-dom-event.js");
jsLoader.loadSubScript("chrome://vueexport/content/xml.js");
jsLoader.loadSubScript("chrome://vueexport/content/vueexport.js");

