<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="1.0">

  <xsl:output method="text"/>

  <xsl:template match="instructions">
    package com.j2js;
    public class Const {
    
    public static InstructionType[] instructionTypes = new InstructionType[<xsl:value-of select="count(instruction)"/>];
    <xsl:apply-templates select="instruction" mode="b">
    </xsl:apply-templates>
    
    static {
    InstructionType i;
    Form f;
    <xsl:apply-templates select="instruction" mode="a">
      <xsl:sort select="code" data-type="number"/>
    </xsl:apply-templates>
    }
    }
  </xsl:template>
  

  <xsl:template match="instruction" mode="a">
    i = new InstructionType((short)<xsl:value-of select="code"/>, "<xsl:value-of select="mnemonic"/>", <xsl:value-of select="count(form)"/>);
    <xsl:apply-templates select="form"/>
    instructionTypes[<xsl:value-of select="code"/>] = i;
  </xsl:template>

  <xsl:template match="form">
    f = new Form();
    f.setIns(new Form.Value[]{<xsl:apply-templates select="in"/>});
    f.setOuts(new Form.Value[]{<xsl:apply-templates select="out"/>});
    f.setOperands(new Form.Value[]{<xsl:apply-templates select="operand"/>});
    i.setForm(f, <xsl:value-of select="position()-1"/>);
  </xsl:template>
  
  <xsl:template match="instruction" mode="b">
    public static final int <xsl:value-of select="translate(mnemonic,'abcdefghijklmnopqrstuvwxyz1234567890','ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890')"/> = <xsl:value-of select="code"/>;
  </xsl:template>

  <xsl:template match="out">new Form.Value("<xsl:value-of select="@type"/>","<xsl:value-of select="."/>"), </xsl:template>

  <xsl:template match="in">new Form.Value("<xsl:value-of select="@type"/>","<xsl:value-of select="."/>"), </xsl:template>

  <xsl:template match="operand">new Form.Value("<xsl:value-of select="@type"/>","<xsl:value-of select="."/>"), </xsl:template>

</xsl:stylesheet>
