<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   version="1.0">

   <xsl:template match="jcsc">
      <html>
         <body>
            <h2>Applied Rules</h2>
            <xsl:apply-templates select="rules"/>
         </body>
      </html>
   </xsl:template>

   <xsl:template match="rules">
      <table CELLPADDING="1" CELLSPACING="1" BORDER="0" width="100%">
         <tr>
            <td valign="top" width="30%" bgcolor="#ccccff" align="center">
               <b>Rule</b>
            </td>
            <td valign="top" width="20%" bgcolor="#ccccff" align="center">
               <b>Category</b>
            </td>
            <td valign="top" width="5%" bgcolor="#ccccff" align="center">
               <b>Enabled</b>
            </td>
            <td valign="top" width="10%" bgcolor="#ccccff" align="center">
               <b>Type</b>
            </td>
            <td valign="top" width="30%" bgcolor="#ccccff" align="center">
               <b>Value</b>
            </td>
            <td valign="top" width="5%" bgcolor="#ccccff" align="center">
               <b>Severity</b>
            </td>
         </tr>
         <xsl:for-each select="rule">
            <xsl:sort select="@category"/>
            <tr>
               <td valign="top" width="30%" align="left" bgcolor="#ffcccc">
                  <xsl:value-of select="@name"/>
               </td>
               <td valign="top" width="20%" align="left" bgcolor="#ffffcc">
                  <xsl:value-of select="@category"/>
               </td>
               <td valign="top" width="5%" align="center" bgcolor="#ffffcc">
                  <xsl:value-of select="@enabled"/>
               </td>
               <td valign="top" width="10%" align="left" bgcolor="#ffffcc">
                  <xsl:value-of select="type"/>
               </td>
               <td valign="top" width="30%" align="left" bgcolor="#ffffcc">
                  <xsl:value-of select="value"/>
               </td>
               <td valign="top" width="5%" align="center" bgcolor="#ffcccc">
                  <xsl:value-of select="severity"/>
               </td>
            </tr>
         </xsl:for-each>
   </table>
</xsl:template>

</xsl:stylesheet>

