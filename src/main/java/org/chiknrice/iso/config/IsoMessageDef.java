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
package org.chiknrice.iso.config;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.chiknrice.iso.codec.AlphaCodec;
import org.chiknrice.iso.codec.BinaryCodec;
import org.chiknrice.iso.codec.BitmapCodec;
import org.chiknrice.iso.codec.BitmapCodec.Bitmap;
import org.chiknrice.iso.codec.BitmapCodec.Bitmap.Type;
import org.chiknrice.iso.codec.Codec;
import org.chiknrice.iso.codec.CompositeCodec;
import org.chiknrice.iso.codec.Configurable;
import org.chiknrice.iso.codec.CustomCodec;
import org.chiknrice.iso.codec.CustomCodecAdapter;
import org.chiknrice.iso.codec.DateTimeCodec;
import org.chiknrice.iso.codec.NumericCodec;
import org.chiknrice.iso.codec.TagVarCodec;
import org.chiknrice.iso.codec.VarCodec;
import org.chiknrice.iso.config.ComponentDef.Encoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 * 
 */
public final class IsoMessageDef {

    private static final Logger LOG = LoggerFactory.getLogger(IsoMessageDef.class);

    private final CompositeCodec headerCodec;
    private final NumericCodec mtiCodec;
    private final Map<Integer, CompositeCodec> fieldsCodec;

    public IsoMessageDef(CompositeCodec headerCodec, NumericCodec mtiCodec, Map<Integer, CompositeCodec> fieldsCodec) {
        this.headerCodec = headerCodec;
        this.mtiCodec = mtiCodec;
        this.fieldsCodec = fieldsCodec;
    }

    public CompositeCodec getHeaderCodec() {
        return headerCodec;
    }

    public NumericCodec getMtiCodec() {
        return mtiCodec;
    }

    public Map<Integer, CompositeCodec> getFieldsCodec() {
        return fieldsCodec;
    }

    public static IsoMessageDef build(String configXml) {
        return new ConfigBuilder(configXml).build();
    }

    private static class ConfigBuilder {

        private Encoding defaultTagEncoding;
        private Encoding defaultLengthEncoding;
        private Charset defaultCharset;
        private Encoding defaultNumericEncoding;
        private Encoding defaultDateEncoding;
        private TimeZone defaultTimeZone;
        private boolean defaultLeftJustified;
        private boolean defaultMandatory;

        private final Document doc;

        public ConfigBuilder(String configXml) {
            Schema schema = null;
            try {
                SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                schema = factory.newSchema(new StreamSource(Thread.currentThread().getContextClassLoader()
                        .getResourceAsStream("iso8583.xsd")));
                Validator validator = schema.newValidator();
                validator.validate(new StreamSource(Thread.currentThread().getContextClassLoader()
                        .getResourceAsStream(configXml)));

                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                doc = dBuilder.parse(Thread.currentThread().getContextClassLoader().getResourceAsStream(configXml));
                doc.getDocumentElement().normalize();
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }

            Element defaults = (Element) doc.getElementsByTagName("defaults").item(0);

            NodeList nodes = defaults.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node n = nodes.item(i);
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    Element e = (Element) n;
                    switch (e.getTagName()) {
                    case "var":
                        defaultTagEncoding = Encoding.valueOf(e.getAttribute("tag-encoding"));
                        LOG.info("Default tag encoding: {}", defaultTagEncoding);
                        defaultLengthEncoding = Encoding.valueOf(e.getAttribute("length-encoding"));
                        LOG.info("Default length encoding: {}", defaultLengthEncoding);
                        break;
                    case "alpha":
                        String charsetName = e.getAttribute("charset");
                        defaultCharset = "SYSTEM".equals(charsetName) ? Charset.defaultCharset() : Charset
                                .forName(charsetName);
                        LOG.info("Default charset: {}", defaultCharset);
                        defaultLeftJustified = isLeftJustified(e);
                        LOG.info("Default {} justified", defaultLeftJustified ? "LEFT" : "RIGHT");
                        break;
                    case "numeric":
                        defaultNumericEncoding = Encoding.valueOf(e.getAttribute("encoding"));
                        LOG.info("Default numeric encoding: {}", defaultNumericEncoding);
                        break;
                    case "date":
                        defaultDateEncoding = Encoding.valueOf(e.getAttribute("encoding"));
                        LOG.info("Default date encoding: {}", defaultDateEncoding);
                        String tzDefault = e.getAttribute("timezone");
                        defaultTimeZone = "SYSTEM".equals(tzDefault) ? TimeZone.getDefault() : TimeZone
                                .getTimeZone(tzDefault);
                        LOG.info("Default timezone: {}", defaultTimeZone.getID());
                        break;
                    case "ordinality":
                        defaultMandatory = Boolean.valueOf(e.getAttribute("mandatory"));
                        break;
                    }
                }
            }

        }

        public IsoMessageDef build() {
            Encoding mtiEncoding = Encoding.valueOf(((Element) doc.getElementsByTagName("mti-encoding").item(0))
                    .getAttribute("type"));
            NumericCodec mtiCodec = new NumericCodec(mtiEncoding, 4);
            LOG.info("MTI encoding: {}", mtiEncoding);

            CompositeCodec headerCodec = null;
            Map<Integer, ComponentDef> headerDef = buildHeader(doc);
            if (headerDef != null) {
                headerCodec = new CompositeCodec(headerDef);
            }

            Bitmap.Type msgBitmapType = Bitmap.Type.valueOf(((Element) doc.getElementsByTagName("msg-bitmap").item(0))
                    .getAttribute("type"));
            LOG.info("Bitmap type: {}", msgBitmapType);

            Map<Integer, CompositeCodec> fieldsCodecs = buildFieldsCodecs(doc, msgBitmapType);

            return new IsoMessageDef(headerCodec, mtiCodec, fieldsCodecs);
        }

        /**
         * @param doc
         * @return
         */
        private Map<Integer, ComponentDef> buildHeader(Document doc) {
            Element headerElement = (Element) doc.getElementsByTagName("header").item(0);
            return headerElement != null ? buildFixedComponents(headerElement) : null;
        }

        /**
         * @param doc
         * @param bitmapType
         * @return
         */
        private Map<Integer, CompositeCodec> buildFieldsCodecs(Document doc, Type bitmapType) {
            NodeList messageList = doc.getElementsByTagName("message");
            Map<Integer, CompositeCodec> defs = new TreeMap<Integer, CompositeCodec>();
            final BitmapCodec bitmapCodec = new BitmapCodec(bitmapType);
            for (int i = 0; i < messageList.getLength(); i++) {
                Element messageDef = (Element) messageList.item(i);
                Integer mti = getInteger(messageDef, "mti");
                if (defs.containsKey(mti)) {
                    throw new RuntimeException(String.format("Duplicate message config for mti %d", mti));
                }
                
                Integer superMti = getInteger(messageDef, "extends");
                
                Map<Integer, ComponentDef> fieldDefs;
                if(superMti != null) {
                    fieldDefs = extendMessageDef(messageDef, defs.get(superMti));
                } else {
                    fieldDefs = buildVarComponents(messageDef);    
                }
                
                
                defs.put(mti, new CompositeCodec(fieldDefs, bitmapCodec));
            }
            return defs;
        }

        /**
         * @param ce
         * @return
         */
        private Map<Integer, ComponentDef> buildVarComponents(Element ce) {
            NodeList nodes = ce.getChildNodes();
            Map<Integer, ComponentDef> fieldDefs = null;
            if (nodes.getLength() > 0) {
                fieldDefs = new TreeMap<Integer, ComponentDef>();
                for (int i = 0; i < nodes.getLength(); i++) {
                    Node n = nodes.item(i);
                    if (n.getNodeType() == Node.ELEMENT_NODE) {
                        Element e = (Element) n;
                        Integer index = Integer.valueOf(e.getAttribute("index"));
                        ComponentDef def = buildComponent(index, e, isMandatory(e));
                        if (fieldDefs.containsKey(index)) {
                            throw new RuntimeException(String.format("Duplicate field index: %d", index));
                        }
                        fieldDefs.put(index, def);
                    }
                }
            }

            return fieldDefs;
        }

        /**
         * @param ce
         * @return
         */
        private Map<Integer, ComponentDef> buildFixedComponents(Element ce) {
            NodeList nodes = ce.getChildNodes();
            Map<Integer, ComponentDef> fieldDefs = null;
            if (nodes.getLength() > 0) {
                fieldDefs = new TreeMap<Integer, ComponentDef>();
                int index = 1;
                for (int i = 0; i < nodes.getLength(); i++) {
                    Node n = nodes.item(i);
                    if (n.getNodeType() == Node.ELEMENT_NODE) {
                        Element e = (Element) n;
                        ComponentDef def = buildComponent(index, e, true);
                        fieldDefs.put(index++, def);
                    }
                }
            }
            return fieldDefs;
        }

        /**
         * @param e
         * @return
         */
        private ComponentDef buildComponent(Integer index, Element e, boolean mandatory) {
            Map<Integer, ComponentDef> childDefs = null;
            Codec<?> codec;
            switch (e.getTagName()) {
            case "composite-var":
                Bitmap.Type bitmapType = getBitmapType(e);
                BitmapCodec bitmapCodec = bitmapType != null ? new BitmapCodec(bitmapType) : null;
                childDefs = buildVarComponents(e);
                CompositeCodec compositeCodec = new CompositeCodec(childDefs, bitmapCodec);
                Integer tagDigits = getInteger(e, "tag-length");

                NumericCodec lengthCodec = buildVarLengthCodec(e);
                if (tagDigits != null) {
                    codec = new TagVarCodec<Map<Integer, Object>>(compositeCodec, lengthCodec, new NumericCodec(
                            getEncoding(e, "tag-encoding", defaultTagEncoding), tagDigits));
                } else {
                    codec = new VarCodec<Map<Integer, Object>>(compositeCodec, lengthCodec);
                }
                break;
            case "composite":
                childDefs = buildFixedComponents(e);
                codec = new CompositeCodec(childDefs);
                break;
            case "alpha":
                codec = new AlphaCodec(getCharset(e), isLeftJustified(e), Integer.valueOf(e.getAttribute("length")));
                break;
            case "alpha-var":
                codec = new VarCodec<String>(new AlphaCodec(getCharset(e)), buildVarLengthCodec(e));
                break;
            case "numeric":
                codec = new NumericCodec(getEncoding(e, "encoding", defaultNumericEncoding), Integer.valueOf(e
                        .getAttribute("length")));
                break;
            case "numeric-var":
                codec = new VarCodec<Number>(new NumericCodec(getEncoding(e, "encoding", defaultNumericEncoding)),
                        buildVarLengthCodec(e));
                break;
            case "date":
                codec = new DateTimeCodec(e.getAttribute("format"), getTimeZone(e), getEncoding(e, "encoding",
                        defaultDateEncoding));
                break;
            case "binary":
                codec = new BinaryCodec(Integer.valueOf(e.getAttribute("length")));
                break;
            case "binary-var":
                codec = new VarCodec<byte[]>(new BinaryCodec(), buildVarLengthCodec(e));
                break;
            case "custom":
            case "custom-var":
                codec = buildCustomCodec(e);
                break;
            default:
                throw new RuntimeException("Unexepcted tag: " + e.getTagName());
            }
            ComponentDef def = new ComponentDef(index, codec, mandatory);
            if (childDefs != null) {
                for (ComponentDef childDef : childDefs.values()) {
                    childDef.setParent(def);
                }
            }
            return def;
        }

        private Codec<?> buildCustomCodec(Element e) {
            String classAttr = e.getAttribute("class");
            Class<?> customClass;
            try {
                customClass = Class.forName(classAttr);
                if (CustomCodec.class.isAssignableFrom(customClass)) {
                    CustomCodec customCodec = (CustomCodec) customClass.newInstance();

                    NodeList paramNodes = e.getChildNodes();
                    Map<String, String> params = new HashMap<String, String>();
                    for (int i = 0; i < paramNodes.getLength(); i++) {
                        Node n = paramNodes.item(i);
                        if (n.getNodeType() == Node.ELEMENT_NODE) {
                            Element paramNode = (Element) n;
                            params.put(paramNode.getAttribute("key"), paramNode.getAttribute("value"));
                        }
                    }
                    if (customCodec instanceof Configurable) {
                        ((Configurable) customCodec).configure(params);
                    }

                    Codec<Object> codec = new CustomCodecAdapter(customCodec);
                    if ("custom-var".equals(e.getTagName())) {
                        return new VarCodec<Object>(codec, buildVarLengthCodec(e));
                    }
                    return codec;
                } else {
                    throw new RuntimeException(String.format("Invalid custom class %s", classAttr));
                }
            } catch (ClassNotFoundException | SecurityException | InstantiationException | IllegalAccessException
                    | IllegalArgumentException ex) {
                throw new RuntimeException(ex.getMessage(), ex);
            }
        }

        private NumericCodec buildVarLengthCodec(Element e) {
            return new NumericCodec(getEncoding(e, "length-encoding", defaultLengthEncoding), getInteger(e, "length"));
        }

        private Charset getCharset(Element e) {
            String value = getOptionalAttribute(e, "charset");
            return value != null ? Charset.forName(value) : defaultCharset;
        }

        private TimeZone getTimeZone(Element e) {
            String value = getOptionalAttribute(e, "timezone");
            return value != null ? TimeZone.getTimeZone(value) : defaultTimeZone;
        }

        private Encoding getEncoding(Element e, String attributeName, Encoding defaultEncoding) {
            String value = getOptionalAttribute(e, attributeName);
            return value != null ? Encoding.valueOf(value) : defaultEncoding;
        }

        private Integer getInteger(Element e, String attributeName) {
            String value = getOptionalAttribute(e, attributeName);
            return value != null ? Integer.valueOf(value) : null;
        }

        public Bitmap.Type getBitmapType(Element e) {
            String value = getOptionalAttribute(e, "bitmap-type");
            return value != null ? Bitmap.Type.valueOf(value) : null;
        }

        private Boolean isLeftJustified(Element e) {
            String value = getOptionalAttribute(e, "justified");
            return value != null ? "LEFT".equals(value) : defaultLeftJustified;
        }

        private boolean isMandatory(Element e) {
            String value = getOptionalAttribute(e, "mandatory");
            return value != null ? Boolean.parseBoolean(value) : defaultMandatory;
        }

        private String getOptionalAttribute(Element e, String attribute) {
            return e.getAttribute(attribute).length() > 0 ? e.getAttribute(attribute) : null;
        }
        
        


        /**
         * @param messageDef
         * @param compositeCodec
         * @return
         */
        private Map<Integer, ComponentDef> extendMessageDef(Element messageDef, CompositeCodec compositeCodec) {
            // TODO Auto-generated method stub
            return null;
        }

    }

}
