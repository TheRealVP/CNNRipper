<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:template match="/">
<html>
<head>
<meta charset="utf-8"></meta>
<title> DEI News CNN Ripoff</title>
</head>
<body>
<h1> DEI News CNN Ripoff </h1>
<p></p>
<div id="tabs">
  <ul>
    <li><a href="#tabs-1"><b> News from United States </b></a></li>
    <li><a href="#tabs-2"><b> News from Africa </b></a></li>
    <li><a href="#tabs-3"><b> News from Asia </b></a></li>
	<li><a href="#tabs-4"><b> News from Europe </b></a></li>
	<li><a href="#tabs-5"><b> News from Latin America </b></a></li>
	<li><a href="#tabs-6"><b> News from Middle-East </b></a></li>
  </ul>
  <div id="tabs-1">
  <div id="accordion">
	<xsl:apply-templates select="//news_us/news_item"/>
	</div>
  </div>
  <div id="tabs-2">
  <div id="accordion2">
	<xsl:apply-templates select="//news_africa/news_item"/>
	</div>
  </div>
  <div id="tabs-3">
  <div id="accordion3">
	<xsl:apply-templates select="//news_asia/news_item"/>
  </div>
  </div>
  <div id="tabs-4">
  <div id="accordion4">
	<xsl:apply-templates select="//news_europe/news_item"/>
  </div>
  </div>
  <div id="tabs-5">
  <div id="accordion5">
	<xsl:apply-templates select="//news_latinamerica/news_item"/>
  </div>
  </div>
  <div id="tabs-6">
  <div id="accordion6">
	<xsl:apply-templates select="//news_middleeast/news_item"/>
  </div>
  </div>
</div>
<!--para incluir scripts (devem ficar no fim da pagina, imediatamente antes do </body>
neste caso ainda faltava carregar a barra lateral...
-->
<link type="text/css" href="css/smoothness/jquery-ui-1.8.20.custom.css" rel="stylesheet" />
<script type="text/javascript" src="js/jquery-1.7.2.min.js"></script> 
<script type="text/javascript" src="js/jquery-ui-1.8.20.custom.min.js"></script> 
<script type="text/javascript">
			$(function() {
    $( "#tabs" ).tabs();
	$( "#accordion" ).accordion({collapsible: true, autoHeight: false});
	$( "#accordion2" ).accordion({collapsible: true, autoHeight: false});
	$( "#accordion3" ).accordion({collapsible: true, autoHeight: false});
	$( "#accordion4" ).accordion({collapsible: true, autoHeight: false});
	$( "#accordion5" ).accordion({collapsible: true, autoHeight: false});
	$( "#accordion6" ).accordion({collapsible: true, autoHeight: false});
  });
</script>
</body>
</html>
</xsl:template>

<xsl:template match="//news_item">
  <h3>Title:<xsl:value-of select="title"/></h3>
  <div>
  <table border="1">
<tr>
<td>Date:</td>
<td> <xsl:value-of select="date"/> </td>
</tr>
<tr>
<td>Author:</td>
<td> <xsl:value-of select="author"/> </td>
</tr>
<tr>
<td>Highlights:</td>
<td> <xsl:value-of select="highlights"/> </td>
</tr>
<tr>
<td>Text:</td>
<td> <xsl:value-of select="text"/> </td>
</tr>
<tr>
<td>Url:</td>
<td> <xsl:variable name="URL"><xsl:value-of select="url"/></xsl:variable><a href="{$URL}"><xsl:value-of select="url"/></a> </td>
</tr>
<tr>
<td>Photos:</td>
<td>
<xsl:for-each select="photos">
<xsl:for-each select="photo">
<xsl:variable name="Photo"><xsl:value-of select="."/></xsl:variable><img src="{$Photo}" alt="{$Photo}" style="width:80px;height:80px"></img>
</xsl:for-each>
</xsl:for-each>
</td>
</tr>
<tr>
<td>Videos:</td>
<td>
<xsl:for-each select="videos">
<xsl:for-each select="video">
<p>
<xsl:variable name="Video"><xsl:value-of select="."/></xsl:variable><a href="{$Video}"><xsl:value-of select="."/></a>
</p>
</xsl:for-each>
</xsl:for-each>
</td>
</tr>
	</table>
  </div>
</xsl:template>
</xsl:stylesheet>