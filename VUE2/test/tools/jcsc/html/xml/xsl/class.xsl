<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="1.0">

<xsl:template match="/">
<html>
<body>
<h3><xsl:value-of select="jcsc/package"/></h3>
<h1><xsl:value-of select="jcsc/class"/></h1>

<table CELLPADDING="1" CELLSPACING="1" BORDER="0" width="100%">
  <tr>
    <td valign="top" width="40%" align="left" bgcolor="#ffffcc">Author</td>
    <td valign="top" width="60%" bgcolor="#ccccff" align="left">
      <b><xsl:value-of select="jcsc/author"/></b>
    </td>
  </tr>
  <tr>
    <td valign="top" width="40%" align="left" bgcolor="#ffffcc">Violations Count</td>
    <td valign="top" width="60%" bgcolor="#ccccff" align="left">
      <b><xsl:value-of select="jcsc/count"/></b>
    </td>
  </tr>
  <tr>
    <td valign="top" width="40%" align="left" bgcolor="#ffffcc">Methods Count</td>
    <td valign="top" width="60%" bgcolor="#ccccff" align="left">
      <b><xsl:value-of select="jcsc/methods"/></b>
    </td>
  </tr>
  <tr>
    <td valign="top" width="40%" align="left" bgcolor="#ffffcc">Lines Count</td>
    <td valign="top" width="60%" bgcolor="#ccccff" align="left">
      <b><xsl:value-of select="jcsc/lines"/></b>
    </td>
  </tr>
  <tr>
    <td valign="top" width="40%" align="left" bgcolor="#ffffcc">NCSS Count</td>
    <td valign="top" width="60%" bgcolor="#ccccff" align="left">
      <b><xsl:value-of select="jcsc/ncss"/></b>
    </td>
  </tr>
  <tr>
    <td valign="top" width="40%" align="left" bgcolor="#ffffcc">Avg per NCSS</td>
    <td valign="top" width="60%" bgcolor="#ccccff" align="left">
      <b><xsl:value-of select="jcsc/count div jcsc/ncss"/></b>
    </td>
  </tr>
  <xsl:if test="jcsc/unittestclasses&gt;0">
    <tr>
      <td valign="top" width="40%" align="left" bgcolor="#ffffcc">Unit Tests Count</td>
      <td valign="top" width="60%" bgcolor="#ccccff" align="left">
        <b><xsl:value-of select="jcsc/unittests"/></b>
      </td>
    </tr>
  </xsl:if>
</table>

<p></p>

<table CELLPADDING="1" CELLSPACING="1" BORDER="0" width="100%">
  <tr>
    <td valign="top" width="3%" bgcolor="#ccccff" align="center"><b>L#</b></td>
    <td valign="top" width="3%" bgcolor="#ccccff" align="center"><b>C#</b></td>
    <td valign="top" bgcolor="#ccccff" align="center"><B>Violation Message</B></td>
    <td valign="top" width="20%" bgcolor="#ccccff" align="center"><B>Rule</B></td>
    <td valign="top" width="2%" bgcolor="#ccccff" align="center"><B>Severity</B></td>
  </tr>
  <xsl:for-each select="jcsc/violation">
  <tr>
    <td valign="top" width="3%" align="center" bgcolor="#ffffcc"><xsl:value-of select="line"/></td>
    <td valign="top" width="3%" align="center" bgcolor="#ffffcc"><xsl:value-of select="column"/></td>
    <td valign="top" bgcolor="#ffcccc"><xsl:value-of select="message"/></td>
    <td valign="top" width="20%" bgcolor="#ffcccc">
      <xsl:element name="a">
      <xsl:attribute name="href"><xsl:value-of select="rule/name"/>.xml</xsl:attribute>
      <xsl:attribute name="target">classFrame</xsl:attribute>
      <xsl:value-of select="rule/name"/></xsl:element> (<xsl:element name="a">
        <xsl:attribute name="href">
          examplehtml/<xsl:value-of select="translate(rule/category,
                                                      'ABCDEFGHIJKLMNOPQRSTUVWXYZ',
                                                      'abcdefghijklmnopqrstuvwxyz')"/>/<xsl:value-of select="rule/name"/>.html
        </xsl:attribute>
        <xsl:attribute name="target">classFrame</xsl:attribute>*</xsl:element>)
    </td>
    <!--
    <td valign="top" width="2%" align="center" bgcolor="#ffcccc">
      <xsl:element name="a">
        <xsl:attribute name="href"><xsl:value-of select="rule/severity"/>.xml</xsl:attribute>
        <xsl:attribute name="target">classFrame</xsl:attribute>
        <xsl:value-of select="rule/severity"/>
      </xsl:element> -->
    <td valign="top" width="2%" align="center" bgcolor="#ffcccc">
      <xsl:value-of select="rule/severity"/>
    </td>

  </tr>
  </xsl:for-each>
</table>

<p></p>

<table CELLPADDING="1" CELLSPACING="1" BORDER="0" width="100%">
  <tr>
    <td valign="top" width="3%" bgcolor="#ccccff" align="center"><b>L#</b></td>
    <td valign="top" width="3%" bgcolor="#ccccff" align="center"><b>C#</b></td>
    <td valign="top" width="24%" bgcolor="#ccccff" align="center"><b>Type</b></td>
    <td valign="top" width="30%" bgcolor="#ccccff" align="center"><b>Name</b></td>
    <td valign="top" width="20%" bgcolor="#ccccff" align="center"><b>NCSS Count</b></td>
    <td valign="top" width="20%" bgcolor="#ccccff" align="center"><b>CCN Count</b></td>
  </tr>
  <xsl:for-each select="jcsc/metric/method">
  <tr>
    <td valign="top" width="3%" align="center" bgcolor="#ffffcc"><xsl:value-of select="line"/></td>
    <td valign="top" width="3%" align="center" bgcolor="#ffffcc"><xsl:value-of select="column"/></td>
    <td valign="top" width="24%" align="center" bgcolor="#ffcccc">Method</td>
    <td valign="top" width="30%" align="left" bgcolor="#ffffcc"><xsl:value-of select="name"/></td>
    <td valign="top" width="20%" align="center" bgcolor="#ffcccc"><xsl:value-of select="ncss"/></td>
    <td valign="top" width="20%" align="center" bgcolor="#ffcccc"><xsl:value-of select="ccn"/></td>
  </tr>
  </xsl:for-each>
</table>

</body>
</html>
</xsl:template>
</xsl:stylesheet>
