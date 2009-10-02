
function setMaxAppletSize()
{
	document.getElementById("VUE").setSize(getAppletWidth(),getAppletHeight());
	document.getElementById("content").style.width=getAppletWidth();
	document.getElementById("content").style.height=getAppletHeight();
	document.getElementById("content").width=getAppletWidth();
	document.getElementById("content").height=getAppletHeight();	
	setTimeout('document.getElementById("VUE").width=getAppletWidth();document.getElementById("VUE").height=getAppletHeight();document.getElementById("VUE").style.width=getAppletWidth();document.getElementById("VUE").style.height=getAppletHeight();',500);
}
function getAppletHeight()
{
	var appletHeight;
	var topToolbarHeight = 20;


	if (navigator.userAgent.toLowerCase().indexOf("mac_") > 0)
        {
                appletHeight = document.body.clientHeight;
        }

        else if (navigator.userAgent.toLowerCase().indexOf("msie") > 0)
        {
                //IE5.5 percents do not work on applets inside tables defined by percents so we need to work out the size.
                appletHeight = document.body.clientHeight;
        }

        else if ((navigator.userAgent.toLowerCase().indexOf("safari")> 0) && (navigator.userAgent.toLowerCase().indexOf("ozilla")> 0))
        {
                //This is the only way we know how to set WIDTH and HEIGHT for an applet on a Mac
                appletHeight = document.body.clientHeight;
        }

        else if ((navigator.userAgent.toLowerCase().indexOf("macintosh")> 0) && (navigator.userAgent.toLowerCase().indexOf("ozilla")> 0))
        {
                //This is the only way we know how to set WIDTH and HEIGHT for an applet on a Mac
		appletHeight = document.body.clientHeight;
        }

        else if (navigator.userAgent.toLowerCase().indexOf("netscape6")> 0)
        {
                //This is the only way we know how to set WIDTH and HEIGHT for an applet on a Mac
                appletHeight = document.body.clientHeight;
        }

        else
        {
                //Netscape percents do not work on applets inside tables so we need to work out the size.
                appletHeight = document.body.clientHeight;
        }

	return appletHeight-topToolbarHeight;

}
function getAppletWidth()
{
	var appletWidth;
	var leftToolbarWidth = 20;
	
	if (navigator.userAgent.toLowerCase().indexOf("mac_") > 0)
        {
                appletWidth = document.body.clientWidth;
        }

        else if (navigator.userAgent.toLowerCase().indexOf("msie") > 0)
        {
                //IE5.5 percents do not work on applets inside tables defined by percents so we need to work out the size.
                appletWidth = document.body.clientWidth;
        }

        else if ((navigator.userAgent.toLowerCase().indexOf("safari")> 0) && (navigator.userAgent.toLowerCase().indexOf("ozilla")> 0))
        {
                //This is the only way we know how to set WIDTH and HEIGHT for an applet on a Mac
                appletWidth = document.body.clientWidth;
        }       

        else if ((navigator.userAgent.toLowerCase().indexOf("macintosh")> 0) && (navigator.userAgent.toLowerCase().indexOf("ozilla")> 0))
        {
                //This is the only way we know how to set WIDTH and HEIGHT for an applet on a Mac
                appletWidth = document.body.clientWidth;
        }       

        else if (navigator.userAgent.toLowerCase().indexOf("netscape6")> 0)
        {
                //This is the only way we know how to set WIDTH and HEIGHT for an applet on a Mac
                appletWidth = document.body.clientWidth;
        }

        else
        {
                //Netscape percents do not work on applets inside tables so we need to work out the size.
                appletWidth = document.body.clientWidth;
        }

	return appletWidth-leftToolbarWidth;
}

function outputSizeTag()
{
	var sizeTag;

	sizeTag = 'WIDTH="' + getAppletWidth() + '" HEIGHT="' + getAppletHeight() + '"';

	return sizeTag;
}
