<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="1.0">

<xsl:template match="/">
<html>
  <body>
    <h4><a href="overview.xml" target="classFrame">Overview</a></h4>
    <h3>Packages</h3>
    <h4><a href="allclasses.xml" target="packageFrame">All Classes</a></h4>
    <xsl:for-each select="jcsc/packagelist/package">
      <xsl:sort select="."/>
      <xsl:element name="a">
        <xsl:attribute name="href"><xsl:value-of select="."/>.xml</xsl:attribute>
        <xsl:attribute name="target">packageFrame</xsl:attribute>
        <xsl:value-of select="."/>
      </xsl:element>
      <br></br>
    </xsl:for-each>
    <h3>Categories</h3>
    <h4><a href="allcategories.xml" target="packageFrame">All Categories</a></h4>
    <xsl:for-each select="jcsc/categories/category">
      <xsl:sort select="."/>
      <xsl:element name="a">
        <xsl:attribute name="href"><xsl:value-of select="."/>.xml</xsl:attribute>
        <xsl:attribute name="target">packageFrame</xsl:attribute>
        <xsl:value-of select="."/>
      </xsl:element>
      <br></br>
    </xsl:for-each>
    <h3>Severities</h3>
    <h4><a href="allseverities.xml" target="packageFrame">All Severities</a></h4>
    <xsl:for-each select="jcsc/severities/severity">
      <xsl:sort select="."/>
      <xsl:element name="a">
        <xsl:attribute name="href"><xsl:value-of select="."/>.xml</xsl:attribute>
        <xsl:attribute name="target">packageFrame</xsl:attribute>
        <xsl:value-of select="."/>
      </xsl:element>
      <br></br>
    </xsl:for-each>
  </body>
</html>
</xsl:template>
</xsl:stylesheet>
