<?xml version="1.0"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

	<xsl:output method="xml"/>

	<xsl:template match="/">
		<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
			<fo:layout-master-set>
				<fo:simple-page-master margin-right="75pt" margin-left="75pt"
					page-height="11in" page-width="8.5in"
					margin-bottom="25pt" margin-top="25pt" master-name="main">			
					<fo:region-before extent="25pt"/>
					<fo:region-body margin-top="50pt" margin-bottom="50pt"/>
					<fo:region-after extent="25pt"/>	
				</fo:simple-page-master>
				
				<fo:page-sequence-master master-name="standard">
					<fo:repeatable-page-master-alternatives>
						<fo:conditional-page-master-reference
						  master-name="main" odd-or-even="any"/>
					</fo:repeatable-page-master-alternatives>
				</fo:page-sequence-master>
			</fo:layout-master-set>
			
			<fo:page-sequence master-reference="main">
				<fo:flow flow-name="xsl-region-body">
					<xsl:apply-templates select="vue2d-map"/>
				</fo:flow>
			</fo:page-sequence>
		</fo:root>	
	</xsl:template>	
	
	<xsl:template match="vue2d-map">
		<fo:block font-weight="bold" font-size="32pt"
			line-height="60pt" font-family="sans-serif" text-align="center">
			Concept Map 
			"<xsl:value-of select="@label"/>"
		</fo:block>
		<fo:block font-weight="bold" font-size="24pt"
			line-height="36pt" font-family="sans-serif" text-align="left">
			Attributes:
		</fo:block>
		<fo:block font-size="16pt"
			line-height="36pt" font-family="sans-serif" text-align="left">
			location x: <xsl:value-of select="@x"/>, y: <xsl:value-of select="@y"/>,
			width: <xsl:value-of select="@width"/>, height: <xsl:value-of select="@height"/>
		</fo:block>
		<fo:block font-size="16pt"
			line-height="36pt" font-family="sans-serif" text-align="left">
			ID: <xsl:value-of select="@ID"/>, Stoke Width: <xsl:value-of select="@strokeWidth"/>
		</fo:block>
		<fo:block font-weight="bold" font-size="24pt"
			line-height="36pt" font-family="sans-serif" text-align="left">
			Properties:
		</fo:block>
		<fo:block font-size="16pt"
			line-height="36pt" font-family="sans-serif" text-align="left">
			Fill Color: <xsl:value-of select="fillColor"/>, Font: <xsl:value-of select="font"/>
		</fo:block>		
		<fo:block>
			<xsl:apply-templates select="child"/>
		</fo:block>
	</xsl:template>
	
	<xsl:template match="child">
		<fo:block font-size="28pt" line-height="50pt" text-align="center">
			<!--<xsi:value-of select="type"/>:-->
				"<xsl:value-of select="@label"/>"
		</fo:block>
		<fo:block font-weight="bold" font-size="24pt"
			line-height="36pt" font-family="sans-serif" text-align="left">
			Attributes:
		</fo:block>
		<fo:block font-size="16pt" line-height="36pt" text-align="left">
			location x:<xsl:value-of select="@x"/>, y:<xsl:value-of select="@y"/>
			width: <xsl:value-of select="@width"/>, height: <xsl:value-of select="@height"/>
		</fo:block>
		<fo:block font-size="16pt"
			line-height="36pt" font-family="sans-serif" text-align="left">
			ID: <xsl:value-of select="@ID"/>, Stoke Width: <xsl:value-of select="@strokeWidth"/>
		</fo:block>
		<fo:block font-weight="bold" font-size="24pt"
			line-height="36pt" font-family="sans-serif" text-align="left">
			Properties:
		</fo:block>
		<fo:block font-size="16pt"
			line-height="36pt" font-family="sans-serif" text-align="left">
			Fill Color: <xsl:value-of select="fillColor"/>, Font: <xsl:value-of select="font"/>
		</fo:block>		
		<fo:block>
			<xsl:apply-templates select="resource"/>
		</fo:block>
	</xsl:template>
	
	<xsl:template match="resource">
		<fo:block font-size="16pt"
			line-height="36pt" font-family="sans-serif" text-align="left">
			Resource:	
			<xsl:value-of select="spec"/>
		</fo:block>
		<fo:block font-size="16pt"
			line-height="36pt" font-family="sans-serif" text-align="left">
			Resource Attributes:	
			size: <xsl:value-of select="@size"/>, access-attempted: <xsl:value-of select="@access-attempted"/>
			access-successful: <xsl:value-of select="@access-successful"/>, 
			reference-created: <xsl:value-of select="@reference-created"/>
		</fo:block>		
	</xsl:template>	
	
</xsl:stylesheet>