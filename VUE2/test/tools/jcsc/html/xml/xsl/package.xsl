<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="1.0">

<xsl:variable name="packageName"><xsl:value-of select="jcsc/classlist/package"/></xsl:variable>

<xsl:template match="jcsc/classlist">
<html>
  <body>
  <xsl:apply-templates/>
  </body>
</html>
</xsl:template>

<xsl:template match="package">   
  <h4>
  <xsl:if test="not(. ='All Classes')">
  <xsl:element name="a">
    <xsl:attribute name="href"><xsl:value-of select="."/>.overview.xml</xsl:attribute>
    <xsl:attribute name="target">classFrame</xsl:attribute>      
    <xsl:value-of select="."/>
  </xsl:element>
  (Overview)<br/>
  </xsl:if>
  <xsl:if test=". ='All Classes'">
    <xsl:value-of select="."/>
    <br/>
  </xsl:if>
  </h4>
  <xsl:apply-templates select="classes" />
</xsl:template>

<xsl:template match="classes">
   <xsl:apply-templates select="class"/>
</xsl:template>

<xsl:template match="class">
  <xsl:element name="a">
    <xsl:attribute name="href"><xsl:value-of select="."/>.xml</xsl:attribute>
    <xsl:attribute name="target">classFrame</xsl:attribute>    
    <xsl:if test="$packageName!='All Classes'">
      <xsl:value-of select="substring-after(., concat($packageName, '.'))"/>
    </xsl:if>
    <xsl:if test="$packageName='All Classes'">
      <xsl:value-of select="."/>
    </xsl:if>
    <br/>
  </xsl:element>
</xsl:template>

</xsl:stylesheet>
