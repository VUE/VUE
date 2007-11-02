<!-- ===================================================== -->
<!-- JCSC XSL                                              -->
<!-- ===================================================== -->

<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
    xmlns:lxslt="http://xml.apache.org/xslt">

    <xsl:output method="html"/>

    <xsl:template match="/">
        <table align="center" cellpadding="2" cellspacing="0" border="0" width="98%">

          <tr>
            <td valign="top" bgcolor="#000066" colspan="6">
              <font color="#ffffff" face="arial" size="3">JCSC (<xsl:value-of select="/cruisecontrol/jcsc/overview/violationscount"/>) </font>
              <a href="/cruisecontrol/logs/jcsc/index.html"><font color="#ffffff" face="arial" size="2"><b>(see last JCSC Details)</b></font></a>
            </td>
            <td bgcolor="#0000066" align="right" colspan="4">
              <font color="#ffffff" face="arial" size="1"><i>Check and feel better about your code!</i></font>
            </td>
          </tr>

          <tr>
            <td bgcolor="#333388" align="left" colspan="10">
              <font color="#ffffff" face="arial" size="2">Overview</font>
            </td>
          </tr>

          <tr>
            <td valign="top" bgcolor="#ccccff" colspan="4">
              <font face="arial" size="1">Package Count</font>
            </td>
            <td valign="top" bgcolor="#ffffcc" colspan="6">
              <font face="arial" size="1"><xsl:value-of select="/cruisecontrol/jcsc/overview/packagecount"/></font>
            </td>
          </tr>
          <tr>
            <td valign="top" bgcolor="#ccccff" colspan="4">
              <font face="arial" size="1">Class Count</font>
            </td>
            <td valign="top" bgcolor="#ffffcc" colspan="6">
              <font face="arial" size="1"><xsl:value-of select="/cruisecontrol/jcsc/overview/classcount"/></font>
            </td>
          </tr>
          <tr>
            <td valign="top" bgcolor="#ccccff" colspan="4">
              <font face="arial" size="1">Violations Count</font>
            </td>
            <td valign="top" bgcolor="#ffffcc" colspan="6">
              <font face="arial" size="1"><xsl:value-of select="/cruisecontrol/jcsc/overview/violationscount"/></font>
            </td>
          </tr>
          <tr>
            <td valign="top" bgcolor="#ccccff" colspan="4">
              <font face="arial" size="1">Average per Class</font>
            </td>
            <td valign="top" bgcolor="#ffffcc" colspan="6">
              <font face="arial" size="1"><xsl:value-of select="/cruisecontrol/jcsc/overview/violationscount div cruisecontrol/jcsc/overview/classcount"/></font>
            </td>
          </tr>
          <tr>
            <td valign="top" bgcolor="#ccccff" colspan="4">
              <font face="arial" size="1">Total NCSS Count</font>
            </td>
            <td valign="top" bgcolor="#ffffcc" colspan="6">
              <font face="arial" size="1"><xsl:value-of select="/cruisecontrol/jcsc/overview/ncsscount"/></font>
            </td>
         </tr>
         <tr>
           <td valign="top" bgcolor="#ccccff" colspan="4">
             <font face="arial" size="1">Avergage per NCSS</font>
         </td>
         <td valign="top" bgcolor="#ffffcc" colspan="6">
           <font face="arial" size="1"><xsl:value-of select="/cruisecontrol/jcsc/overview/violationscount div cruisecontrol/jcsc/overview/ncsscount"/></font>
         </td>
         </tr>
         <tr>
           <td valign="top" bgcolor="#ccccff" colspan="4">
             <font face="arial" size="1">Unit Test Class Count</font>
           </td>
           <td valign="top" bgcolor="#ffffcc" colspan="6">
             <font face="arial" size="1"><xsl:value-of select="/cruisecontrol/jcsc/overview/unittestclasscount"/></font>
           </td>
         </tr>

         <tr>
           <td valign="top" bgcolor="#ccccff" colspan="4">
             <font face="arial" size="1">Unit Tests Count</font>
           </td>
           <td valign="top" bgcolor="#ffffcc" colspan="6">
             <font face="arial" size="1"><xsl:value-of select="/cruisecontrol/jcsc/overview/unittestscount"/></font>
           </td>
         </tr>

         <tr>
           <td bgcolor="#333388" align="left" colspan="10">
             <font color="#ffffff" face="arial" size="2">Worst Classes</font>
           </td>
         </tr>
  
         <xsl:for-each select="/cruisecontrol/jcsc/overview/worstclasses/worst">
         <xsl:if test="count &gt; 0.0">
         <tr>
           <td bgcolor="#ffffcc" colspan="5">
             <font face="arial" size="1"><xsl:value-of select="class"/></font>
           </td>
           <td bgcolor="#ffcccc" colspan="1"><font face="arial" size="1">
             <xsl:value-of select="count"/></font>
           </td>
           <td bgcolor="#ccccff" colspan="4"><font face="arial" size="1">
             <xsl:value-of select="author"/></font>
           </td>
         </tr>
       </xsl:if>
     </xsl:for-each>
   </table>
  </xsl:template>
</xsl:stylesheet>


