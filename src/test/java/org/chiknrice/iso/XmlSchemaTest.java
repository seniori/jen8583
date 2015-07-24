/*
 * Copyright (c) 2014 Ian Bondoc
 *
 * This file is part of Jen8583
 *
 * Jen8583 is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of the License, or(at your option) any later version.
 *
 * Jen8583 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 *
 */

package org.chiknrice.iso;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * WIP tests for schema validation
 *
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 */
public class XmlSchemaTest {

    private Document doc;
    private Element message;

    private Validator validator;

    @Before
    public void init() throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(true);
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        doc = dBuilder
                .parse(Thread.currentThread().getContextClassLoader().getResourceAsStream("schema-test-template.xml"));
        doc.getDocumentElement().normalize();

        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = factory.newSchema(
                new StreamSource(Thread.currentThread().getContextClassLoader().getResourceAsStream("jen8583.xsd")));

        message = (Element) doc.getElementsByTagName("message").item(0);

        validator = schema.newValidator();
    }

    private void validateConfig() throws SAXException {
        try {
            validator.validate(new DOMSource(doc));
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private Element createElement(String element) {
        return doc.createElementNS("http://www.chiknrice.org/jen8583", element);
    }

    @Test
    public void testEmptyMessage() {
        try {
            validateConfig();
            fail("Expecting SAXException due to empty message element");
        } catch (SAXException e) {
            assertThat(e.getMessage(), containsString("The content of element 'message' is not complete"));
        }
    }

    @Test
    public void testWithValidAlpha() {
        Element alpha = createElement("alpha");
        alpha.setAttribute("index", "1");
        alpha.setAttribute("length", "1");
        message.appendChild(alpha);

        try {
            validateConfig();
        } catch (SAXException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testWithValidAlphaAndMandatoryAttribute() {
        Element alpha = createElement("alpha");
        alpha.setAttribute("index", "1");
        alpha.setAttribute("length", "1");
        alpha.setAttribute("mandatory", "true");
        message.appendChild(alpha);

        try {
            validateConfig();
        } catch (SAXException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testWithValidAlphaAndTrimAttribute() {
        Element alpha = createElement("alpha");
        alpha.setAttribute("index", "1");
        alpha.setAttribute("length", "1");
        alpha.setAttribute("trim", "true");
        message.appendChild(alpha);

        try {
            validateConfig();
        } catch (SAXException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testWithValidAlphaAndJustifiedLeftAttribute() {
        Element alpha = createElement("alpha");
        alpha.setAttribute("index", "1");
        alpha.setAttribute("length", "1");
        alpha.setAttribute("justified", "LEFT");
        message.appendChild(alpha);

        try {
            validateConfig();
        } catch (SAXException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testWithValidAlphaAndJustifiedRightAttribute() {
        Element alpha = createElement("alpha");
        alpha.setAttribute("index", "1");
        alpha.setAttribute("length", "1");
        alpha.setAttribute("justified", "RIGHT");
        message.appendChild(alpha);

        try {
            validateConfig();
        } catch (SAXException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testWithAlphaHavingInvalidJustifiedAttribute() {
        Element alpha = createElement("alpha");
        alpha.setAttribute("index", "1");
        alpha.setAttribute("length", "1");
        alpha.setAttribute("justified", "xx");
        message.appendChild(alpha);

        try {
            validateConfig();
            fail("Expecting SAXException due to invalid justified attribute");
        } catch (SAXException e) {
            assertThat(e.getMessage(),
                    containsString("Value 'xx' is not facet-valid with respect to enumeration '[LEFT, RIGHT]'"));
        }
    }

    @Test
    public void testWithAlphaMissingIndexAttribute() {
        Element alpha = createElement("alpha");
        alpha.setAttribute("length", "1");
        message.appendChild(alpha);

        try {
            validateConfig();
            fail("Expecting SAXException due to missing index attribute");
        } catch (SAXException e) {
            assertThat(e.getMessage(), containsString("Attribute 'index' must appear on element 'alpha'"));
        }
    }

    @Test
    public void testWithAlphaMissingLengthAttribute() {
        Element alpha = createElement("alpha");
        alpha.setAttribute("index", "1");
        message.appendChild(alpha);

        try {
            validateConfig();
            fail("Expecting SAXException due to missing length attribute");
        } catch (SAXException e) {
            assertThat(e.getMessage(), containsString("Attribute 'length' must appear on element 'alpha'"));
        }
    }

}
