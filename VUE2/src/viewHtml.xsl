<?xml version="1.0"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
	
	<xsl:template match="/">
		<xsl:apply-templates/>
	</xsl:template>

	<xsl:template match="LW-MAP">
		<HTML>
			<HEAD>
				<TITLE><xsl:value-of select="@label"/></TITLE>
			</HEAD>
			<BODY>
				<p><H1 align="center">
					<U>Concept Map: <xsl:value-of select="@label"/></U>
				</H1></p>
				<H3><B>
					Attributes:
				</B></H3>
					<p>location <B>x:</B><xsl:value-of select="@x"/>,
								<B>y:</B><xsl:value-of select="@y"/>,
								<B>width:</B><xsl:value-of select="@width"/>,
								<B>height:</B><xsl:value-of select="@height"/>
					</p>
					<p>
						<B>ID:</B> <xsl:value-of select="@ID"/>, 
						<B>Stroke Width:</B> <xsl:value-of select="@strokeWidth"/>
					</p>
				<H3><B>
					Properties:
				</B></H3>
					<p>
						<B>Fill Color:</B><xsl:value-of select="fillColor"/>,
						<B>Font:</B> <xsl:value-of select="font"/>, 
					</p>
					<p>	
						<B>User Zoom:</B> <xsl:value-of select="userZoom"/>, 
						<B>User Origin:</B><xsl:apply-templates select="userOrigin"/>
					</p>							
				<xsl:apply-templates select="child"/>
			</BODY>
		</HTML>		
	</xsl:template>
		
	<xsl:template match="userOrigin">
		(<B>x:</B> <xsl:value-of select="@x"/>, <B>y:</B> <xsl:value-of select="@y"/>)
	</xsl:template>
	
	<xsl:template match="child">
		<P><U><H2 align="center">
			<xsl:value-of select="@xsi:type"/>: <xsl:value-of select="@label"/>
		</H2></U></P>
		
		<P><B><H3>Attributes: </H3></B></P>
		<P>location: <B>x:</B><xsl:value-of select="@x"/>,
					 <B>y:</B><xsl:value-of select="@y"/>,
					 <B>width:</B><xsl:value-of select="@width"/>,
					 <B>height:</B><xsl:value-of select="@height"/>
		</P>
		<P>
			<B>ID:</B><xsl:value-of select="@ID"/>,
			<B>stroke width:</B><xsl:value-of select="@strokeWidth"/>,  
			<xsl:if test="starts-with(@xsi:type,'n')">
				<B>autoSized:</B><xsl:value-of select="@autoSized"/>
			</xsl:if>
		</P>
		<P><B><H3>Properties: </H3></B></P>
		<P>
			<B>fill color:</B><xsl:value-of select="fillColor"/>,  
			<xsl:if test="starts-with(@xsi:type,'l')">
				<B>text color:</B><xsl:value-of select="textColor"/>, 
			</xsl:if>
			<B>font:</B><xsl:value-of select="font"/>
		</P>
		
			  
			<xsl:if test="starts-with(@xsi:type,'p')">
				<P><B>weight:</B><xsl:value-of select="weight"/>,
				<B>ordered:</B><xsl:value-of select="ordered"/>,
				</P><p><B>comment:</B><xsl:value-of select="comment"/></p>
			</xsl:if>
		
		<P>
			  
			<xsl:if test="starts-with(@xsi:type,'l')">
				<B>ID1:</B><xsl:value-of select="ID1"/>, 
				<B>ID2:</B><xsl:value-of select="ID2"/>
			</xsl:if>
		</P>
		<xsl:if test="starts-with(@xsi:type,'p')">
			<P><B><H3>Pathway Elements: </H3></B></P>
			<P><xsl:apply-templates select="elementList"/></P>
		</xsl:if>
		<P><H3>Resource: <xsl:apply-templates select="resource"/></H3></P>		
	</xsl:template>
	
	<xsl:template match="elementList">
		<p><B>Element:</B> <xsl:value-of select="@label"/>, 
			<B>Type:</B> <xsl:value-of select="@xsi:type"/></p>
	</xsl:template>
	
	<xsl:template match="resource">
			<a>
				<xsl:attribute name="href">
					<xsl:choose>
						<xsl:when test="starts-with(spec, 'www')">
							http://<xsl:value-of select="spec"/>
						</xsl:when>
						<xsl:when test="starts-with(spec, 'C:')">
							file:///<xsl:value-of select="spec"/>	
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="spec"/>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:attribute>
				<B>
					<xsl:value-of select="spec"/>
				</B>
			</a>	
	</xsl:template>
</xsl:stylesheet>	




