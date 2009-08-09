function doImportMap(file,title)
{
	var element = document.createElement("ImportMapDataElementPath");
	document.documentElement.appendChild(element);
	var evt = document.createEvent("Events");
	evt.initEvent("ImportMapDataEvent", true, false);
	//alert("dispatch");
	element.dispatchEvent(evt);
	
}
/* check to see if form field values have changed */

/* method to run as the page is about to unload */
function preUnload()
{
	var l_sMessage = "Clicking reload or navigating away from this page";
    l_sMessage	  += " will result in the loss of any unsaved work.";

   
    return l_sMessage;
  
}

function postUnload()
{

	/*var element = document.createElement("MyExtensionDataElement");
	element.setAttribute("attribute1", "foobar");
	element.setAttribute("attribute2", "hello world");
	document.documentElement.appendChild(element);

	var evt = document.createEvent("Events");
	evt.initEvent("MyExtensionEvent", true, false);
	element.dispatchEvent(evt);*/

}

/* initialise the page, attaching the DOM events */
function init()
{
    window.onbeforeunload = preUnload;
 
}

window.onload = init;