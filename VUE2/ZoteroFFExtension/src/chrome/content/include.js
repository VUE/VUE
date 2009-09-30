// Only create main object once
//#if (!Zotero.VUEExport) {
//#	const loader = Components.classes["@mozilla.org/moz/jssubscript-loader;1"]		
//					.getService(Components.interfaces.mozIJSSubScriptLoader);
//	loader.loadSubScript("chrome://vueexport/content/vueexport.js");
//}
const vueLoader = Components.classes["@mozilla.org/moz/jssubscript-loader;1"]
                                    .getService(Components.interfaces.mozIJSSubScriptLoader);
vueLoader.loadSubScript("chrome://vueexport/content/yahoo-dom-event.js");
vueLoader.loadSubScript("chrome://vueexport/content/xml.js");
vueLoader.loadSubScript("chrome://vueexport/content/vueexport.js");

