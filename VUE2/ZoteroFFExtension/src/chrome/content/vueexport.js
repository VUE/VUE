
/*
 * Copyright 2003-2008 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

var VUEExport = new function() {
	var fileLocation = "";
   	var zPane = null;
	var installLocation ="";
	var pathsep ="/";
	var win = null;
	var userag = null;
	var count = 100;
	var wm = null; 

	this.importMapDataListener=importMapDataListener;
	this.importUrlDataListener=importUrlDataListener;
	this.importFileDataListener=importFileDataListener;
	this.mainMenuListener=mainMenuListener;
	this.addNotesToMap=addNotesToMap;
	this.addRelationsToMap=addRelationsToMap;
	this.isVueRunning=isVueRunning;
	this.isLoaded=isLoaded;
    this.performLoadedCheck=performLoadedCheck;
	this.openZoteroMap=openZoteroMap;
	this.save=save;
	this.newTab=newTab;
	this.startVUE=startVUE;
	this.initializeExportFileLocation=initializeExportFileLocation;
	this.getFromVue=getFromVue;
	this.vueTabSelected=vueTabSelected;
	this.init=init;
	this.dispose;dispose;
	this.sendToVue=sendToVue;
	this.addMapFromVueToCollection=addMapFromVueToCollection;
	this.exportCSV=exportCSV;


	function importMapDataListener(evt)
	{
		//var fileName = Zotero.Utilities.prototype.htmlSpecialChars(evt.target.getAttribute("path"));
		//var title = Zotero.Utilities.prototype.htmlSpecialChars(evt.target.getAttribute("title"));

		var fileName = content.document.getElementById("VUE").wrappedJSObject.getActiveMapPath();
		var title = content.document.getElementById("VUE").wrappedJSObject.getActiveMapDisplayTitle();
	
		var file = Components.classes["@mozilla.org/file/local;1"]
	           .createInstance(Components.interfaces.nsILocalFile);
		file.initWithPath(fileName);
    		var attachmentId = Zotero.Attachments.importFromFile(file,null);
	        var attachmentItem = Zotero.Items.get(attachmentId);
		    attachmentItem.setField('accessDate', "CURRENT_TIMESTAMP");
		    attachmentItem.save();
       
	    	// Add the result item to the results collection
		if (ZoteroPane.getSelectedCollection())
		    	ZoteroPane.getSelectedCollection().addItem(attachmentItem.id);
		else
		    	alert("No Collection is currently selected.");
	
	}

	function importUrlDataListener(evt)
	{
		var fileName = content.document.getElementById("VUE").wrappedJSObject.getActiveResourceSpec();
		if (fileName)
		{
			//var attachmentId = Zotero.Attachments. importFromURL(fileName, false, false, false, ZoteroPane.getSelectedCollection(true));
			var attachmentId = Zotero.Attachments. linkFromURL(fileName, false, false);
			var attachmentItem = Zotero.Items.get(attachmentId);
			attachmentItem.setField('accessDate', "CURRENT_TIMESTAMP");
			attachmentItem.save();
		
			 if (zPane.getSelectedCollection())
			    	zPane.getSelectedCollection().addItem(attachmentItem.id);
		        else
			    	alert("No Collection is currently selected.");
		}
	}

	function importFileDataListener(evt)
	{	
		var fileName = content.document.getElementById("VUE").wrappedJSObject.getActiveResourceSpec();
		var title = content.document.getElementById("VUE").wrappedJSObject.getActiveResourceTitle();
		if (fileName)
		{
			var file = Components.classes["@mozilla.org/file/local;1"]
			   .createInstance(Components.interfaces.nsILocalFile);
			file.initWithPath(fileName);
			var attachmentId = Zotero.Attachments.importFromFile(file,null);
			var attachmentItem = Zotero.Items.get(attachmentId);
			attachmentItem.setField('accessDate', "CURRENT_TIMESTAMP");
			attachmentItem.save();
       
			// Add the result item to the results collection
			if (zPane.getSelectedCollection())
				zPane.getSelectedCollection().addItem(attachmentItem.id);
			else
				alert("No Collection is currently selected.");
		}
	}
	
	function mainMenuListener(evt)
	{

		var num = gBrowser.browsers.length;
		for (var i = 0; i < num; i++) {
		  var b = gBrowser.getBrowserAtIndex(i);
		  try {
			 //csv-export-from-zotero@tufts.edu/VueApplet.html
			  //if this is true then there is currently a tab available that have the applet loaded.
			  if (b.currentURI.spec.match(/VueApplet.html$/))
			  {
				  //VUE is loaded so you would want the option to bring VUE into focus.
				   var popups = document.getElementById("zotero-tb-actions-popup").childNodes;
				   var noOfPopups = popups.length;

			       var mi = 0;
			       for(mi=0;mi<noOfPopups;mi++)
			       {
				 var mitem = popups[mi];
				 if(mitem.label.indexOf("Start VUE") != -1)
				 {
				   mitem.label = "Bring VUE to Front";                       
				   return;
				 }
			       }
			  }
			  else
			  {
				  //VUE Is not loaded so you would assume you want the start vue action inserted here.
				  var popups = document.getElementById("zotero-tb-actions-popup").childNodes;
				  var noOfPopups = popups.length;

			       var mi = 0;
			       for(mi=0;mi<noOfPopups;mi++)
			       {
				 var mitem = popups[mi];
				 if(mitem.label.indexOf("Bring VUE") != -1)
				 {
				   mitem.label = "Start VUE";                       
				 }
			       }
			  }
		  } catch(e) {
		    Components.utils.reportError(e);
		  }
		}
	}

	function addNotesToMap()
	{	
		var c = zPane.getSelectedCollection();
		var items = c.getChildItems(false); 
		var xmlcontent ="";
		xmlcontent += '<notes>\n';
		
		for each(var arr in items) 
		{
			var notes = arr.getNotes();
			if(notes.length)
			{
				xmlcontent += '<note id="'+arr.id +'">\n';
							  
					xmlcontent +=Zotero.Utilities.prototype.cleanTags(Zotero.Items.get(notes[0]).getNote()) + "\n";
					xmlcontent += '</note>';		
				
			}
			if(arr.notes) 
			{
				var note = arr.notes[0].note
				xmlcontent += '<note id="'+arr.id +'">\n';
				  
				xmlcontent +=note + "\n";
				xmlcontent += '</note>';		
									
			}
		}
		
		xmlcontent += '</notes>\n';		
		//alert(xmlcontent);	
		content.document.getElementById("VUE").wrappedJSObject.addNotesToMap(xmlcontent);
		
	}

	function addRelationsToMap()
	{
		
		var c = zPane.getSelectedCollection();
		var items = c.getChildItems(false); 
		var xmlcontent ="";
		xmlcontent += '<links>\n';
		for each(var arr in items) 
		{
		
			var seeAlso = arr.relatedItemsBidirectional;
			for(var i in seeAlso) 
			{						
						xmlcontent += '<link from="'+arr.id +'" to="' + seeAlso[i] + '"/>\n';		
			}
		}
		xmlcontent += '</links>\n';		
			
		content.document.getElementById("VUE").wrappedJSObject.addLinksToMap(xmlcontent);
		
		
	}

	function isVueRunning()
	{
		  var browser = gBrowser.selectedTab.linkedBrowser;
		  // browser is the XUL element of the browser that's just been selected
		  if (browser.currentURI.spec.match(/VueApplet.html$/))
		  {
			  return true;
		  }
		  else
			  return false;
	}

	function isLoaded() 
	{
			var obj = null;
		  try{
			  obj=content.document.getElementById("VUE").wrappedJSObject;
			  }
			  catch(e){
				  //alert("f"); 
				  return false;};
		    
		try {
			if (!obj.isInited())
				// in IE: isActive returns an error if the applet IS loaded, 
				// false if not loaded
				// in NS: isActive returns true if loaded, an error if not loaded, 
				// so never reaches the next statement
			//	alert("a");
				return false;
	    }
		catch(e){
		//	alert("b");
			return false;
		}
		openZoteroMap();
		return true;
	}
	
	function performLoadedCheck()
	{
		  if (!isLoaded() && count > 0) 
		  {
			  count--;
			//  var _self = this;
			  setTimeout( function(){ VUEExport.performLoadedCheck();} , 1000 );
		  }
		  return;
		  
		 
	}

	function openZoteroMap()
	{
		if (!isVueRunning())
		{
			startVUE();
			count=100;
			performLoadedCheck();		

		}
		
		var items = zPane.itemsView.getSelectedItems()

		try
		{
			var file = items[0].getFile(false, true);
					var fileName = file.leafName;
					if (fileName) {
				//alert(fileName);
						content.document.getElementById("VUE").wrappedJSObject.displayLocalMap(file.path);
					
			}
		
			
		}	
		catch(e)
		{
			//Components.utils.reportError(e);
		}

		return;
	}
	
	/**
	 * 
	 * Save item data to temporary file in "csv+" format precondition:
	 * fileLocation must be initialized first postcondition: overwrites
	 * existing file
	 * 
	 * todo: correctly internationalize saved data
	 * 
	 */	
	function save(data)
	{
		   var file = Components.classes["@mozilla.org/file/local;1"].
			      createInstance(Components.interfaces.nsILocalFile);


		   try
		   {
		     file.initWithPath(fileLocation);
		   //  alert(this.fileLocation);
		   }
		   catch(e)
		   {
		     content.document.write("Error initializing file location" + e);
		   }

		   var foStream = Components.classes["@mozilla.org/network/file-output-stream;1"]
			 .createInstance(Components.interfaces.nsIFileOutputStream);

		   foStream.init(file, 0x02 | 0x08 | 0x20, 0666, 0);
		   // if you are sure there will never ever be any non-ascii text in data you can 
		   // also call foStream.writeData directly
		   var converter = Components.classes["@mozilla.org/intl/converter-output-stream;1"].
					  createInstance(Components.interfaces.nsIConverterOutputStream);
		   converter.init(foStream, "UTF-8", 0, 0);
		   converter.writeString(data);
		   converter.close(); // this closes foStream

	}

	/**
	 * Function to open a new tab in firefox
	 * @param url
	 * @return
	 */
	function newTab(url) {
		var browser = top.document.getElementById("content");
		var newtab = browser.addTab(decodeURIComponent(url));
		browser.selectedTab = newtab;
		
	//	YAHOO.util.Event.addListener(newtab, "beforeunload", this.preUnload);
	  //  YAHOO.util.Event.addListener(newtab, "unload", this.postUnload);
	}
	
	/**
	 * Starts VUE
	 */
	function startVUE() {
		
		
		
		var lastWin = wm.getMostRecentWindow("navigator:browser");
		
		 /* include any js files here */  
		/**
		 * Crude Test to see if we're running on windows, if we are, set the path separator appropriately.
		 */
		 

		 var iow = userag.indexOf("Win");
		 
	     if(iow != -1)
	     {
	       pathsep = "\\";
	     } 
	     //end test
	    
	     
	     ///////
	     /**
	      * Two possibilities exist here one VUE is already running, or it is not, 
	      * we know if VUE Is running already at this point and have set the menu title appropriately
	      * we'll take a look at that and make a guess about what to do.
	      */
	     ///////
	   
	     var popups = document.getElementById("zotero-tb-actions-popup").childNodes;
	     var noOfPopups = popups.length;

	     var mi = 0;
	     var launchVUE = false;
	     var tabIndex = -1;
	     
	     for(mi=0;mi<noOfPopups;mi++)
	     {
	       var mitem = popups[mi];
	       if(mitem.label.indexOf("Start VUE") != -1)
	       {
		 launchVUE = true;
	       }
	     }
	     
		 var num = gBrowser.browsers.length;
		 for (var i = 0; i < num; i++) 
		 {
		 var b = gBrowser.getBrowserAtIndex(i);
		 try {
			 //csv-export-from-zotero@tufts.edu/VueApplet.html
			  //if this is true then there is currently a tab available that have the applet loaded.
			  if (b.currentURI.spec.match(/VueApplet.html$/))
			  {
				  tabIndex =i;
			  }
		  } catch(e) 
		  {
			    Components.utils.reportError(e);
		  }
		 }
	     if (launchVUE)
	     {
	    	
		 initializeExportFileLocation();
	       
	       /**
		* Open a new browser TAB for VUE
		*/
	       newTab(installLocation + pathsep + "{7e8ac0b1-774a-4974-9579-eb83a447f7bf}" + pathsep + "VueApplet.html");
	       notLoaded = false;
	       win = window;
	       var frontWindow = Components.classes["@mozilla.org/embedcomp/window-watcher;1"].
	       getService(Components.interfaces.nsIWindowWatcher).activeWindow;
		
	       zPane = frontWindow.ZoteroPane;       

	       //Gears Menu
	       var popups = document.getElementById("zotero-tb-actions-popup").childNodes;
	       document.getElementById("zotero-collections-pane").addEventListener('click',mainMenuListener,false);
             
	      }
	      else
	      {
		//OK we don't need to launch VUE Because its already up and running in a tab so we just need to find the tab
		//and bring it forward
	   // 	var wm = Components.classes["@mozilla.org/appshell/window-mediator;1"]
	    //	        .getService(Components.interfaces.nsIWindowMediator);
		
		var tabbrowser = wm.getMostRecentWindow("navigator:browser");
	    
		//alert(gBrowser.mTabContainer.selectedIndex);
		// Yes--select and focus it.
		var currentTab = gBrowser.mTabContainer.selectedIndex;
	    
		tabbrowser.selectedTab = tabIndex;

		tabbrowser.focus();    	
		var move = tabIndex-currentTab;
	    
		if (move >0) {
			 for (var ii = 0; ii < move; ii++) 
			 {
				 gBrowser.mTabContainer.advanceSelectedTab(1, true);
			 }
		}
		else
		{
			for (var ii = 0; ii > move; ii--) 
			{
				gBrowser.mTabContainer.advanceSelectedTab(-1,true);
			}
		}
	      }
	}

	/**
	 * INIT EXPORT FILE LOCATION
	 */        
	function initializeExportFileLocation() {
		 var extensionManager = Components.classes["@mozilla.org/extensions/manager;1"].
			  getService(Components.interfaces.nsIExtensionManager);
		 // todo: is there any component that can dynamically
				// determine the extension id?
		 installLocation = extensionManager.getInstallLocation("{7e8ac0b1-774a-4974-9579-eb83a447f7bf}").location.path;
	     
		 var file = Components.classes["@mozilla.org/file/directory_service;1"].
		 getService(Components.interfaces.nsIProperties).
		 get("ProfD", Components.interfaces.nsIFile);
		 file.append("vue-storage");

	       //  alert(file.path);
		 if( !file.exists() || !file.isDirectory() ) {   // if it doesn't exist, create
			 file.create(Components.interfaces.nsIFile.DIRECTORY_TYPE, 0777);
		 }

	}

	/**
	 * Get from vue
	 */
	function getFromVue() {
	 
		 var items = content.document.getElementById("VUE").wrappedJSObject.getActiveMapItems();
		 var itemArray = items.split("\n");


		 for(var i=0;i<itemArray.length-1;i++)
		 {
		   var item = itemArray[i];
		   var values = item.split(",");
		   var data = {
		    title: values[0],
		    url: values[2]
	           };

		   newItem = null;
		   var key = values[1];
		   
		   if(key == "none")
		   {
		 	var ite = Zotero.Items.add('webpage', data);  // returns a
			var c = zPane.getSelectedCollection();

			if(c!=null)
			{
			    c.addItem(ite.getID());
			}
		   }

		 }
	}

	function vueTabSelected(event)
	{
		
	  var browser = gBrowser.selectedTab.linkedBrowser;
	  // browser is the XUL element of the browser that's just been selected
	  if (browser.currentURI.spec.match(/VueApplet.html$/))
	  {
		  var num = gBrowser.browsers.length;
			for (var i = 0; i < num; i++) {
			  var b = gBrowser.getBrowserAtIndex(i);
			  try {
				 //csv-export-from-zotero@tufts.edu/VueApplet.html
				  //if this is true then there is currently a tab available that have the applet loaded.
				  if (b.currentURI.spec.match(/VueApplet.html$/))
				  { 

							b.contentDocument.getElementById("VUE").wrappedJSObject.ShowPreviouslyHiddenWindows();
					
				  }
			  }
			  catch(e) {
				    Components.utils.reportError(e);
				  }
				}			
	  }
	  else
	  {
		  var num = gBrowser.browsers.length;
			for (var i = 0; i < num; i++) {
			  var b = gBrowser.getBrowserAtIndex(i);
			  try {
				 //csv-export-from-zotero@tufts.edu/VueApplet.html
				  //if this is true then there is currently a tab available that have the applet loaded.
				  if (b.currentURI.spec.match(/VueApplet.html$/))
				  { 
						if(!b.contentDocument.getElementById("VUE").wrappedJSObject.AllWindowsHidden())
							b.contentDocument.getElementById("VUE").wrappedJSObject.HideAllDockWindows();
					
				  }
			  }
			  catch(e) {
				    Components.utils.reportError(e);
				  }
				}
		
	  }
	}

	function init()
	{
		
		// During initialisation
		userag = navigator.userAgent;
		var container = gBrowser.tabContainer;
		container.addEventListener("TabSelect", vueTabSelected, false);
		
		wm = Components.classes["@mozilla.org/appshell/window-mediator;1"]
							   .getService(Components.interfaces.nsIWindowMediator);
		



	}

	function dispose()
	{
		
		// When no longer needed
		container.removeEventListener("TabSelect", vueTabSelected, false);

	}

	function sendToVue(collectionName,fileName,addToMap) {
		 /*
				 * var coll = ZoteroPane.getSelectionCollection(); var title =
				 * ""; if(coll == null) { title = "all"; } else { title =
				 * coll; }
				 */
		 
		 // try
		 // {
		   content.document.getElementById("VUE").wrappedJSObject.addZoteroDatasource(collectionName,fileName,addToMap);
		 // }
		 // catch(e)
		 // {
		 // document.content.write(e);
		 // }
	}

	function addMapFromVueToCollection() {
		return;
	}    
	
	function filterNum(str)  {
		re = /\$|,|@|#|~|`|\%|\*|\^|\&|\(|\)|\+|\=|\[|\-|\_|\]|\[|\}|\{|\;|\:|\'|\"|\<|\>|\?|\||\\|\!|\$|\./g;
		// remove special characters like "$" and "," etc...
		return str.replace(re, "");
	}    

	function exportCSV(addToMap) {	
			
		var currentCollection = zPane.getSelectedCollection();
		var currentCollectionId = zPane.getSelectedCollection(true);
		var file = Components.classes["@mozilla.org/file/directory_service;1"].
		getService(Components.interfaces.nsIProperties).
		get("ProfD", Components.interfaces.nsIFile);
		file.append("vue-storage");
		file.append(currentCollectionId +".xml");
		fileLocation = file.path;
		var its = currentCollection.getChildItems();            
		var i=0;           
		var xml ="";
		var atts;
		
		atts = {id:""+currentCollectionId};

		xml+='<?xml version="1.0" encoding="UTF-8"?>\n';
		xml+=element('zoteroCollection',null, atts,true);
		xml+="\n";
		
		for each (var it in its)
		{
		   it = its[i];
		   i++;
		   var id = it.id;
		   var item = Zotero.Items.get(id);
		   var itemTypeID = item.itemTypeID;
		   // var sees = item._getRelatedItems();
		//     atts = {id:""+id};
		     xml+='<zoteroItem>';
		     xml += "\n";        
		     xml+='<id>' +id  +'</id>\n';
			 var allFields = Zotero.ItemFields.getItemTypeFields(itemTypeID);
			 for each(var field in allFields) {
				var fieldName = Zotero.ItemFields.getName(field);
				if (fieldName == "url")
				{
					try
					{
					 //add a link element for the url so a resource gets created in VUE.
					 xml+=element("Link",Zotero.Utilities.prototype.htmlSpecialChars(item.getField("url")));
			     xml += "\n";
					}
					catch(e)
					{
						  Components.utils.reportError(e); // report the error and continue execution
					}
				}
				
						try
					{
					 xml+=element(fieldName,Zotero.Utilities.prototype.htmlSpecialChars(item.getField(fieldName)));
			     xml += "\n";
					}
					catch(e)
					{
						  Components.utils.reportError(e); // report the error and continue execution
					}

				
			 }
			 
			xml+=element('type',Zotero.ItemTypes.getLocalizedString(itemTypeID));
		    xml += "\n";
		    
		    var tags = item.getTags();
	       
		    if (tags) 
		    {
				for (var tagLength=0; tagLength<tags.length; tagLength++) 
				{
					var tag = tags[tagLength];
					xml+=element('tag',Zotero.Utilities.prototype.htmlSpecialChars(tag.name));
			    xml += "\n";
				}
			}

		    var creators = item.getCreators();
		    
		    if(creators && creators.length) {
				// split creators into subcategories
			var	 k=0;
				for each(var creator in creators) 
				{
					var firstName = creators[k].ref.firstName;
					var lastName = creators[k].ref.lastName;
					var creatorString = lastName;

					if (firstName) {
						creatorString = firstName + " " + lastName;
					}

					var creatorType =
						Zotero.CreatorTypes.getName(creators[k].creatorTypeID);

						xml+=element(creatorType,Zotero.Utilities.prototype.htmlSpecialChars(creatorString));
						xml+="\n";
						
						k++;
				}
			
			}
		     xml +="</zoteroItem>"
		     xml += "\n";
		   }
		   xml +="</zoteroCollection>"
		   
		   //save the xml to a file in the VUE extensions directory...
		   save(xml);

		   //send the reference to the data sources to vue
		   sendToVue(currentCollection.name,fileLocation,addToMap);
	}

};
