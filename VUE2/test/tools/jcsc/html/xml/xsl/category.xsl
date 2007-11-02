<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="1.0">

<xsl:variable name="categoryName"><xsl:value-of select="jcsc/rulelist/category"/></xsl:variable>

<xsl:template match="jcsc/rulelist">
<html>
  <body>
  <xsl:apply-templates/>
  </body>
</html>
</xsl:template>

<xsl:template match="category">
  <h4>
  <xsl:if test="not(. ='All Categories')">
     <xsl:value-of select="."/>
  </xsl:if>

  <xsl:if test=". ='All Categories'">
    <xsl:value-of select="."/>
    <br/>
  </xsl:if>
  </h4>
  <xsl:apply-templates select="rules" />
</xsl:template>

<xsl:template match="rules">
   <xsl:apply-templates select="rule"/>
</xsl:template>

<xsl:template match="rule">
  <!-- <xsl:value-of select="./category"/> - --><xsl:element name="a">
    <xsl:attribute name="href"><xsl:value-of select="./name"/>.xml</xsl:attribute>
    <xsl:attribute name="target">classFrame</xsl:attribute>    
    <xsl:value-of select="./name"/>
    <br/>
  </xsl:element>
</xsl:template>

</xsl:stylesheet>
