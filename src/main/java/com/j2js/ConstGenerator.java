/*
 * Created on 26.12.2005
 * Copyright Wolfgang Kuehn 2005
 */
package com.j2js;

import java.io.File;

import javax.xml.transform.Result;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * @author kuehn
 */
public class ConstGenerator {

    public static void main(String[] args) throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        StreamSource xslSource = new StreamSource(new File("src/com/j2js/instructions.xsl"));
        Transformer xltTransformer = factory.newTransformer(xslSource);
        
        StreamSource xmlSource = new StreamSource(new File("src/com/j2js/instructions.xml"));
        Result result = new StreamResult(new File("src/com/j2js/Const.java"));
        xltTransformer.transform(xmlSource, result);
    }
}
