<?xml version="1.0"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format">

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
					<xsl:apply-templates select="concept-map"/>
				</fo:flow>
			</fo:page-sequence>
		</fo:root>	
	</xsl:template>	
	
	<xsl:template match="concept-map">
		<fo:block font-weight="bold" font-size="32pt"
			line-height="36pt" font-family="sans-serif" text-align="center">
			Concept Map: 
			"<xsl:value-of select="label"/>"
		</fo:block>
		<fo:block font-size="16pt"
			line-height="20pt" font-family="sans-serif" text-align="left">
			location x: <xsl:value-of select="@x"/>, y: <xsl:value-of select="@y"/>
			<xsl:apply-templates select="ID"/>
			<xsl:apply-templates select="meta-data"/>
			<xsl:apply-templates select="category"/>
			<xsl:apply-templates select="resource"/>
			<xsl:apply-templates select="notes"/>
		</fo:block>
		<fo:block>
			<xsl:apply-templates select="node-list | link-list"/>
		</fo:block>
	</xsl:template>
	
	<xsl:template match="node-list">
		<fo:block font-size="24pt" line-height="30pt" text-align="center">
			Node "<xsl:value-of select="label"/>"
		</fo:block>
		<fo:block font-size="14pt" line-height="16pt" text-align="left">
			location x:<xsl:value-of select="@x"/>, y:<xsl:value-of select="@y"/>
			<xsl:apply-templates select="ID"/>
			<xsl:apply-templates select="meta-data"/>
			<xsl:apply-templates select="category"/>
			<xsl:apply-templates select="resource"/>
			<xsl:apply-templates select="notes"/>
		</fo:block>
	</xsl:template>
	
	<xsl:template match="link-list">
		<fo:block font-size="24pt"
			line-height="30pt" font-family="sans-serif" text-align="center">
			Link	
		</fo:block>
		<fo:block font-size="14pt" line-height="16pt" text-align="left">
			<xsl:apply-templates select="item1"/>
			<xsl:apply-templates select="item2"/>
			<xsl:apply-templates select="meta-data"/>
			<xsl:apply-templates select="category"/>
			<xsl:apply-templates select="ID"/>
			<xsl:apply-templates select="item1_-iD"/>
			<xsl:apply-templates select="item2_-iD"/>
			<xsl:apply-templates select="notes"/>	
		</fo:block>
	</xsl:template>
	
	<xsl:template match="meta-data">
		<fo:block>
			Meta Data: <xsl:value-of select="."/>
		</fo:block>		
	</xsl:template>
	
	<xsl:template match="category">
		<fo:block>
			Category:  <xsl:value-of select="."/>
		</fo:block>		
	</xsl:template>
	
	<xsl:template match="resource">
		<fo:block>
			Resource:	
			<xsl:apply-templates select="spec"/>
		</fo:block>		
	</xsl:template>
	
	<xsl:template match="spec">
		<fo:block>	
			<xsl:value-of select="."/>
		</fo:block>		
	</xsl:template>
	
	<xsl:template match="notes">
		<fo:block>
			Notes:	
			<xsl:value-of select="."/>
		</fo:block>		
	</xsl:template>
			
	<xsl:template match="ID">
		<fo:block >
			ID:	
			<xsl:value-of select="."/>
		</fo:block>		
	</xsl:template>
	
	<xsl:template match="label">
		<fo:block>
			Label:	
			<xsl:value-of select="."/>
		</fo:block>		
	</xsl:template>
	
	<xsl:template match="item1">
		<fo:block>
			Link Node 1: "<xsl:value-of select="label"/>"
		</fo:block>		
	</xsl:template>
	
	<xsl:template match="item2">
		<fo:block>
			Link Node 2: "<xsl:value-of select="label"/>"
		</fo:block>		
	</xsl:template>
	
	<xsl:template match="item2_-iD">
		<fo:block>
			Item 2 -iD:	<xsl:value-of select="."/>		
		</fo:block>		
	</xsl:template>
	
	<xsl:template match="item1_-iD">
		<fo:block>
			Item 1 -iD:	<xsl:value-of select="."/>	
		</fo:block>		
	</xsl:template>
	
</xsl:stylesheet>