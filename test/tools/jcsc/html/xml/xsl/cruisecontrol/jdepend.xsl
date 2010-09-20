<!-- ===================================================== -->
<!-- JDepend XSL                                           -->
<!-- ===================================================== -->

<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
    xmlns:lxslt="http://xml.apache.org/xslt">

    <xsl:output method="html"/>

    <xsl:template match="/">
        <table align="center" cellpadding="2" cellspacing="0" border="0" width="98%">
  
          <tr>
            <td bgcolor="#000066" align="left" colspan="10">
              <font color="#ffffff" face="arial" size="2">JDepend statistics </font>
              <a href="/cruisecontrol/logs/jdepend/index.html"><font color="#ffffff" face="arial" size="2"><b>(see latest JDepend Details)</b></font></a>
            </td>
          </tr> 
     
          <tr>
            <td bgcolor="#333388" align="left" colspan="2">
              <font color="#ffffff" face="arial" size="2">Package</font>
            </td>
            <td bgcolor="#333388" align="left" colspan="1">
              <font color="#ffffff" face="arial" size="2">#Cl</font>
            </td>
            <td bgcolor="#333388" align="left" colspan="1">
              <font color="#ffffff" face="arial" size="2">#Co</font>
            </td>
            <td bgcolor="#333388" align="left" colspan="1">
              <font color="#ffffff" face="arial" size="2">#Ab</font>
            </td>
            <td bgcolor="#333388" align="left" colspan="1">
              <font color="#ffffff" face="arial" size="2">Ca</font>
            </td>
            <td bgcolor="#333388" align="left" colspan="1">
              <font color="#ffffff" face="arial" size="2">Ce</font>
            </td>
            <td bgcolor="#333388" align="left" colspan="1">
              <font color="#ffffff" face="arial" size="2">A</font>
            </td>
            <td bgcolor="#333388" align="left" colspan="1">
              <font color="#ffffff" face="arial" size="2">I</font>
            </td>
            <td bgcolor="#333388" align="left" colspan="1">
              <font color="#ffffff" face="arial" size="2"><b>D</b></font>
            </td>
          </tr>

         <xsl:for-each select="/cruisecontrol/JDepend/Packages/Package">
            <xsl:if test="Stats/D &gt; 0.5">
            <tr>
              <td bgcolor="#ffffcc" colspan="2">
                <font face="arial" size="1"><xsl:value-of select="@name"/></font>
              </td>
              <td bgcolor="#ffcccc" colspan="1"><font face="arial" size="1">
                <xsl:value-of select="Stats/TotalClasses"/></font>
              </td>
              <td bgcolor="#ccccff" colspan="1"><font face="arial" size="1">
                <xsl:value-of select="Stats/ConcreteClasses"/></font>
              </td>
              <td bgcolor="#ffcccc" colspan="1"><font face="arial" size="1">
                <xsl:value-of select="Stats/AbstractClasses"/></font>
              </td>
              <td bgcolor="#ccccff" colspan="1"><font face="arial" size="1">
                <xsl:value-of select="Stats/Ca"/></font>
              </td>
              <td bgcolor="#ffcccc" colspan="1"><font face="arial" size="1">
                <xsl:value-of select="Stats/Ce"/></font>
              </td>
              <td bgcolor="#ccccff" colspan="1"><font face="arial" size="1">
                <xsl:value-of select="Stats/A"/></font>
              </td>
              <td bgcolor="#ffcccc" colspan="1"><font face="arial" size="1">
                <xsl:value-of select="Stats/I"/></font>
              </td>
              <td bgcolor="#ccccff" colspan="1"><font face="arial" size="1">
                <b><xsl:value-of select="Stats/D"/></b></font>
              </td>
            </tr>
            </xsl:if>
         </xsl:for-each>

         <xsl:variable name="jdepend.packages" select="/cruisecontrol/JDepend/Packages/Package"/>
         <xsl:variable name="jdepend.numdist" select="count($jdepend.packages/Stats)"/>
         <xsl:variable name="jdepend.totdist" select="sum($jdepend.packages/Stats/D)"/>

         <p/>
         <tr>
           <td bgcolor="#333388" colspan="2">
             <font color="#ffffff" face="arial" size="2">SDS Average Distance</font>
           </td>
           <td bgcolor="#333388" colspan="8">
             <font color="#ffffff" face="arial" size="2"><b>
             <xsl:value-of select="$jdepend.totdist div $jdepend.numdist"/>
             </b></font>
           </td>
        </tr>
   </table>
  </xsl:template>
</xsl:stylesheet>
