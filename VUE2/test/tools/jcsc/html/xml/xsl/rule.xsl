<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="1.0">

<xsl:template match="/">
<html>
<body>
<h3><xsl:value-of select="jcsc/category"/></h3>
<h1><xsl:value-of select="jcsc/rule"/><xsl:value-of select="rule/name"/> (<xsl:element name="a">
  <xsl:attribute name="href">examplehtml/<xsl:value-of select="translate(jcsc/category,
                                                      'ABCDEFGHIJKLMNOPQRSTUVWXYZ',
                                                      'abcdefghijklmnopqrstuvwxyz')"/>/<xsl:value-of select="jcsc/rule"/>.html
  </xsl:attribute>
  <xsl:attribute name="target">classFrame</xsl:attribute>*</xsl:element>)
</h1>

<table CELLPADDING="1" CELLSPACING="1" BORDER="0" width="100%">
  <tr>
    <td valign="top" width="3%" bgcolor="#ccccff" align="center"><b>L#</b></td>
    <td valign="top" width="3%" bgcolor="#ccccff" align="center"><b>C#</b></td>
    <td valign="top" width="20%" bgcolor="#ccccff" align="center"><b>Class</b></td>
    <td valign="top" bgcolor="#ccccff" align="center"><B>Violation Message</B></td>
    <td valign="top" width="2%" bgcolor="#ccccff" align="center"><B>Severity</B></td>
  </tr>
  <xsl:for-each select="jcsc/violations/violation">
  <tr>
    <td valign="top" width="3%" align="center" bgcolor="#ffffcc"><xsl:value-of select="line"/></td>
    <td valign="top" width="3%" align="center" bgcolor="#ffffcc"><xsl:value-of select="column"/></td>
    <td valign="top" width="20%" bgcolor="#ffcccc">
      <xsl:element name="a">
        <xsl:attribute name="href"><xsl:value-of select="package"/>.<xsl:value-of select="classname"/>.xml</xsl:attribute>
        <xsl:attribute name="target">classFrame</xsl:attribute>
        <xsl:value-of select="package"/>.<xsl:value-of select="classname"/>
      </xsl:element>
    </td>
    <td valign="top" bgcolor="#ffcccc"><xsl:value-of select="message"/></td>
    <td valign="top" width="2%" align="center" bgcolor="#ffcccc"><xsl:value-of select="rule/severity"/></td>
  </tr>
  </xsl:for-each>
</table>

</body>
</html>
</xsl:template>
</xsl:stylesheet>
