/*
 * $Id: BarcodeElement.java,v 1.1 2003-12-13 20:23:43 jmaerki Exp $
 * ============================================================================
 * The Krysalis Patchy Software License, Version 1.1_01
 * Copyright (c) 2003 Nicola Ken Barozzi.  All rights reserved.
 *
 * This Licence is compatible with the BSD licence as described and
 * approved by http://www.opensource.org/, and is based on the
 * Apache Software Licence Version 1.1.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed for project
 *        Krysalis (http://www.krysalis.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Krysalis" and "Nicola Ken Barozzi" and
 *    "Barcode4J" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For
 *    written permission, please contact nicolaken@krysalis.org.
 *
 * 5. Products derived from this software may not be called "Krysalis"
 *    or "Barcode4J", nor may "Krysalis" appear in their name,
 *    without prior written permission of Nicola Ken Barozzi.
 *
 * 6. This software may contain voluntary contributions made by many
 *    individuals, who decided to donate the code to this project in
 *    respect of this licence, and was originally created by
 *    Jeremias Maerki <jeremias@maerki.org>.
 *
 * THIS SOFTWARE IS PROVIDED ''AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE KRYSALIS PROJECT OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 */
package org.krysalis.barcode4j.fop0205;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.Attributes;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.avalon.framework.CascadingRuntimeException;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.NullLogger;
import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.Status;
import org.apache.fop.fo.DirectPropertyListBuilder;
import org.apache.fop.layout.Area;
import org.apache.fop.layout.FontState;
import org.apache.fop.layout.inline.ForeignObjectArea;
import org.apache.fop.messaging.MessageHandler;

import org.krysalis.barcode4j.BarcodeConstants;
import org.krysalis.barcode4j.BarcodeDimension;
import org.krysalis.barcode4j.BarcodeException;
import org.krysalis.barcode4j.BarcodeGenerator;
import org.krysalis.barcode4j.BarcodeUtil;
import org.krysalis.barcode4j.tools.ConfigurationUtil;
import org.krysalis.barcode4j.tools.UnitConv;

/**
 * Class representing bc:barcode pseudo flow object.
 * 
 * @author Jeremias Maerki
 */
public class BarcodeElement extends BarcodeObj {

    private static final String XMLNS_NAMESPACE_URI =
            "http://www.w3.org/2000/xmlns/";


    /**
     * inner class for making SVG objects.
     */
    public static class Maker extends FObj.Maker {

        /**
         * make an SVG object.
         *
         * @param parent the parent formatting object
         * @param propertyList the explicit properties of this object
         *
         * @return the SVG object
         */
        public FObj make(FObj parent, PropertyList propertyList,
                         String systemId, int line, int column)
                        throws FOPException {
            return new BarcodeElement(parent, propertyList,
                                  systemId, line, column);
        }
    }

    /**
     * returns the maker for this object.
     *
     * @return the maker for SVG objects
     */
    public static FObj.Maker maker() {
        return new BarcodeElement.Maker();
    }

    FontState fs;

    /**
     * constructs an SVG object (called by Maker).
     *
     * @param parent the parent formatting object
     * @param propertyList the explicit properties of this object
     */
    public BarcodeElement(FObj parent, PropertyList propertyList,
                      String systemId, int line, int column) {
        super(parent, propertyList, "barcode", systemId, line, column);
        init();
    }

    protected void init() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            this.doc = factory.newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException pce) {
            throw new CascadingRuntimeException("Error creating DOM", pce);
        }
        this.doc.appendChild(this.doc.createElementNS(BarcodeConstants.NAMESPACE, "barcode"));

        this.element = doc.getDocumentElement();

        buildTopLevel(this.doc, this.element);
    }

    public void buildTopLevel(Document doc, Element svgRoot) {
        // build up the info for the top level element
        if(this.properties instanceof DirectPropertyListBuilder.AttrPropertyList) {
            Attributes attr = ((DirectPropertyListBuilder.AttrPropertyList)this.properties).getAttributes();
            for (int count = 0; count < attr.getLength(); count++) {
                String rf = attr.getValue(count);
                String qname = attr.getQName(count);
                if (qname.indexOf(":") == -1) {
                    element.setAttribute(qname, rf);
                } else {
                    String pref =
                       qname.substring(0, qname.indexOf(":"));
                    ns.put("xlink", "http://www.w3.org/1999/xlink");
                    if (pref.equals("xmlns")) {
                        ns.put(qname.substring(qname.indexOf(":")
                                                      + 1), rf);
                        element.setAttributeNS(XMLNS_NAMESPACE_URI,
                                               qname, rf);
                    } else {
                        element.setAttributeNS((String)ns.get(pref),
                                               qname, rf);
                    }
                }
            }
        } else {
        }
    }

    /**
     * Factory method for creating BarcodeAreas.
     * @param fontState the font state
     * @param width the width of the area
     * @param height the height of the area
     * @return the newly created BarcodeArea
     */
    protected BarcodeArea createArea(FontState fontState, float width, float height) {
        return new BarcodeArea(fontState, width, height);
    }

    /**
     * layout this formatting object.
     *
     * @param area the area to layout the object into
     *
     * @return the status of the layout
     */
    public int layout(final Area area) throws FOPException {
        if (!(area instanceof ForeignObjectArea)) {
            // this is an error
            throw new FOPException("Barcode not in fo:instream-foreign-object");
        }

        if (this.marker == START) {
            this.fs = area.getFontState();

            this.marker = 0;
        }

        //MessageHandler.logln("Creating barcode area");

        final Element barcodeRoot = element;
        /* create a barcode area */
        /* if width and height are zero, get the bounds of the content. */
        final ForeignObjectArea foa = (ForeignObjectArea)area;

        Element e = this.doc.getDocumentElement();

        //if(!e.hasAttributeNS(XMLSupport.XMLNS_NAMESPACE_URI, "xmlns")) {
            e.setAttributeNS(XMLNS_NAMESPACE_URI, "xmlns", BarcodeConstants.NAMESPACE);
        //}

        Configuration cfg = ConfigurationUtil.buildConfiguration(this.doc);
        try {
            String msg;
            try {
                msg = cfg.getAttribute("message");
            } catch (ConfigurationException ce) {
                try {
                    msg = cfg.getAttribute("msg"); //for compatibility
                } catch (ConfigurationException ce1) {
                    throw ce;
                }
            }
            //MessageHandler.logln("Barcode message: " + msg);
            final String renderMode = cfg.getAttribute("render-mode", "native");
            //MessageHandler.logln("Render mode: " + renderMode);
            
            BarcodeGenerator bargen = BarcodeUtil.getInstance().
                    createBarcodeGenerator(cfg, new NullLogger());
            BarcodeDimension bardim = bargen.calcDimensions(msg);
            final float w = (float)UnitConv.mm2pt(bardim.getWidthPlusQuiet()) * 1000;
            final float h = (float)UnitConv.mm2pt(bardim.getHeightPlusQuiet()) * 1000;
            
            
            BarcodeArea barcodeArea = createArea(fs, w, h);
            barcodeArea.setBarcode(bargen, msg, renderMode);
            barcodeArea.start();
            barcodeArea.end();
            
            /* add the SVG area to the containing area */
            foa.setObject(barcodeArea);
            foa.setIntrinsicWidth(barcodeArea.getWidth());
            foa.setIntrinsicHeight(barcodeArea.getHeight());
            
            /* return status */
            return Status.OK;
        } catch (ConfigurationException ce) {
            MessageHandler.errorln("Error in barcode XML: " + ce.getMessage());
            throw new FOPException("Error in barcode XML", ce);
        } catch (BarcodeException be) {
            MessageHandler.errorln("Error generating barcode: " + be.getMessage());
            throw new FOPException("Error generating barcode", be);
        }
    }


}