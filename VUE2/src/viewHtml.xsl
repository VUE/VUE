<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/TR/WD-xsl">

	<xsl:template match="/">
		<HTML>
			<HEAD>
				<TITLE>XML to HTML Test</TITLE>
			</HEAD>
			<BODY>
				<xsl:apply-templates/>
			</BODY>
		</HTML>
	</xsl:template>

	<xsl:template match="concept-map">
		<H1>
			<xsl:value-of select="label"/>
		</H1>		
			<xsl:apply-templates/>
	</xsl:template>
	
	<xsl:template match="node-list">
		<P><U><H2>Node: </H2><H3>
			Label : <xsl:value-of select="label"/>
		</H3></U></P>
		<P><H3>Attributes: </H3></P>
		<P>X : <xsl:value-of select="@x"/></P> 
		<P>Y : <xsl:value-of select="@y"/></P>
		<P>xsi:type : <xsl:value-of select="@xsi:type"/></P>
		<P>xmlns:xsi : <xsl:value-of select="@xmlns:xsi"/></P>
	
		<H3>Properties: </H3>
			<P>ID: <xsl:value-of select="ID"/></P>
			<P>Category: <xsl:value-of select="category"/></P>
			<P>Meta-Data: <xsl:value-of select="meta-data"/></P>
			<P>Notes: <xsl:value-of select="notes"/></P>
			<P>Child-Iterator: <xsl:value-of select="child-iterator"/></P>
			<P>Resource: <xsl:apply-templates/></P>

	</xsl:template>
	
	<xsl:template match="link-list">
		<P><U><H2> Link: </H2><H3>
			Between Nodes "<xsl:apply-templates select="item1"/>" and "<xsl:apply-templates select="item2"/>"
		</H3></U></P>
		
		<P><H3>Attributes: </H3></P>
		<P>Fixed? : <xsl:value-of select="@fixed"/></P> 
		<P>Weight : <xsl:value-of select="@weight"/></P>
		<P>Ordered : <xsl:value-of select="@ordered"/></P>
		<P>xsi:type : <xsl:value-of select="@xsi:type"/></P>
		<P>xmlns:xsi : <xsl:value-of select="@xmlns:xsi"/></P>
		
	</xsl:template>
	
	<xsl:template match="item1 | item2">
		<xsl:value-of select="label"/>
	</xsl:template>
	
	<xsl:template match="resource">
			<xsl:value-of select="spec"/>
	</xsl:template>
</xsl:stylesheet>	