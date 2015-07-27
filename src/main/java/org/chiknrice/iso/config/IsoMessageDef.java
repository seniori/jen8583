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

import org.chiknrice.iso.ConfigException;
import org.chiknrice.iso.codec.*;
import org.chiknrice.iso.codec.BitmapCodec.Bitmap;
import org.chiknrice.iso.config.ComponentDef.Encoding;
import org.chiknrice.iso.util.EqualsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import sun.util.calendar.ZoneInfo;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.InputStream;
import java.util.*;
import java.util.Map.Entry;

import static java.lang.String.format;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 */
public final class IsoMessageDef {

    private static final Logger LOG = LoggerFactory.getLogger(IsoMessageDef.class);

    private final CompositeDef headerCodec;
    private final NumericCodec mtiCodec;
    private final Map<Integer, CompositeDef> fieldsCodec;

    private IsoMessageDef(CompositeDef headerDef, NumericCodec mtiCodec, Map<Integer, CompositeDef> fieldsDef) {
        this.headerCodec = headerDef;
        this.mtiCodec = mtiCodec;
        this.fieldsCodec = Collections.unmodifiableMap(fieldsDef);
    }

    public CompositeDef getHeaderDef() {
        return headerCodec;
    }

    public NumericCodec getMtiCodec() {
        return mtiCodec;
    }

    public Map<Integer, CompositeDef> getFieldsDef() {
        return fieldsCodec;
    }

    public static IsoMessageDef build(String configXml) {
        return build(Thread.currentThread().getContextClassLoader().getResourceAsStream(configXml));
    }

    public static IsoMessageDef build(InputStream configXml) {
        return new ConfigBuilder(configXml).build();
    }

    @SuppressWarnings("unchecked")
    private static class ConfigBuilder {

        private static final String ELEMENT_DEFAULTS = "defaults";

        private static final String ELEMENT_VAR = "var";
        private static final String ELEMENT_TLV = "tlv";
        private static final String ELEMENT_COMPOSITE = "composite";
        private static final String ELEMENT_COMPOSITE_VAR = "composite-var";
        private static final String ELEMENT_COMPOSITE_TLV = "composite-tlv";
        private static final String ELEMENT_FIELD = "field";
        private static final String ELEMENT_ALPHA = "alpha";
        private static final String ELEMENT_ALPHA_VAR = "alpha-var";
        private static final String ELEMENT_NUMERIC = "numeric";
        private static final String ELEMENT_NUMERIC_VAR = "numeric-var";
        private static final String ELEMENT_DATE = "date";
        private static final String ELEMENT_BINARY = "binary";
        private static final String ELEMENT_BINARY_VAR = "binary-var";
        private static final String ELEMENT_CUSTOM = "custom";
        private static final String ELEMENT_CUSTOM_VAR = "custom-var";
        private static final String ELEMENT_ORDINALITY = "ordinality";
        private static final String ELEMENT_MTI_ENCODING = "mti-encoding";
        private static final String ELEMENT_MSG_BITMAP = "msg-bitmap";
        private static final String ELEMENT_HEADER = "header";
        private static final String ELEMENT_MESSAGE = "message";
        private static final String ELEMENT_MESSAGE_EXT = "message-ext";
        private static final String ELEMENT_SET = "set";
        private static final String ELEMENT_REMOVE = "remove";

        private static final String ATTR_MTI = "mti";
        private static final String ATTR_EXTENDS = "extends";
        private static final String ATTR_TAG = "tag";
        private static final String ATTR_TAG_ENCODING = "tag-encoding";
        private static final String ATTR_LENGTH_ENCODING = "length-encoding";
        private static final String ATTR_LENGTH = "length";
        private static final String ATTR_LENGTH_DIGITS = "length-digits";
        private static final String ATTR_MAX_LENGTH = "max-length";
        private static final String ATTR_TRIM = "trim";
        private static final String ATTR_JUSTIFIED = "justified";
        private static final String ATTR_ENCODING = "encoding";
        private static final String ATTR_TIMEZONE = "timezone";
        private static final String ATTR_FORMAT = "format";
        private static final String ATTR_MANDATORY = "mandatory";
        private static final String ATTR_TYPE = "type";
        private static final String ATTR_BITMAP_TYPE = "bitmap-type";
        private static final String ATTR_INDEX = "index";
        private static final String ATTR_CODEC = "codec";
        private static final String ATTR_KEY = "key";
        private static final String ATTR_VALUE = "value";

        private static final String ATTR_CONST_LEFT = "LEFT";
        private static final String ATTR_CONST_RIGHT = "RIGHT";
        private static final String ATTR_CONST_SYSTEM = "SYSTEM";

        private Encoding defaultLengthEncoding;
        private Encoding defaultTlvTagEncoding;
        private Encoding defaultTlvLengthEncoding;
        private boolean defaultTrim;
        private boolean defaultLeftJustified;
        private Encoding defaultNumericEncoding;
        private Encoding defaultDateEncoding;
        private TimeZone defaultTimeZone;
        private boolean defaultMandatory;

        private final Document doc;

        public ConfigBuilder(InputStream configXml) {
            try {
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                dbFactory.setNamespaceAware(true);
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                doc = dBuilder.parse(configXml);
                doc.getDocumentElement().normalize();

                SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                Schema schema = factory.newSchema(new StreamSource(
                        Thread.currentThread().getContextClassLoader().getResourceAsStream("jen8583.xsd")));

                Validator validator = schema.newValidator();
                validator.validate(new DOMSource(doc));
            } catch (Exception e) {
                throw new ConfigException(e.getMessage(), e);
            }

            Element defaults = (Element) doc.getElementsByTagName(ELEMENT_DEFAULTS).item(0);

            for (Element e : getSubElements(defaults)) {
                switch (e.getTagName()) {
                    case ELEMENT_VAR:
                        defaultLengthEncoding = Encoding.valueOf(getMandatoryAttribute(e, ATTR_LENGTH_ENCODING));
                        LOG.info("Default length encoding: {}", defaultLengthEncoding);
                        break;
                    case ELEMENT_TLV:
                        defaultTlvTagEncoding = Encoding.valueOf(getMandatoryAttribute(e, ATTR_TAG_ENCODING));
                        LOG.info("Default tlv tag encoding: {}", defaultTlvTagEncoding);
                        defaultTlvLengthEncoding = Encoding.valueOf(getMandatoryAttribute(e, ATTR_LENGTH_ENCODING));
                        LOG.info("Default tlv length encoding: {}", defaultTlvLengthEncoding);
                        break;
                    case ELEMENT_ALPHA:
                        defaultTrim = Boolean.valueOf(getMandatoryAttribute(e, ATTR_TRIM));
                        LOG.info("Default trim: {}", defaultTrim);
                        String stringJustify = getMandatoryAttribute(e, ATTR_JUSTIFIED);
                        switch (stringJustify) {
                            case ATTR_CONST_LEFT:
                                defaultLeftJustified = true;
                                break;
                            case ATTR_CONST_RIGHT:
                                defaultLeftJustified = false;
                                break;
                            default:
                                throw new ConfigException(format("Invalid value for justified: %s", stringJustify));
                        }
                        LOG.info("Default {} justified", defaultLeftJustified ? ATTR_CONST_LEFT : ATTR_CONST_RIGHT);
                        break;
                    case ELEMENT_NUMERIC:
                        defaultNumericEncoding = Encoding.valueOf(getMandatoryAttribute(e, ATTR_ENCODING));
                        LOG.info("Default numeric encoding: {}", defaultNumericEncoding);
                        break;
                    case ELEMENT_DATE:
                        defaultDateEncoding = Encoding.valueOf(getMandatoryAttribute(e, ATTR_ENCODING));
                        LOG.info("Default date encoding: {}", defaultDateEncoding);
                        String tzDefault = getMandatoryAttribute(e, ATTR_TIMEZONE);
                        defaultTimeZone = ATTR_CONST_SYSTEM.equals(tzDefault) ? TimeZone.getDefault() : ZoneInfo
                                .getTimeZone(tzDefault);
                        LOG.info("Default timezone: {}", defaultTimeZone.getID());
                        break;
                    case ELEMENT_ORDINALITY:
                        defaultMandatory = Boolean.valueOf(getMandatoryAttribute(e, ATTR_MANDATORY));
                        break;
                }
            }

        }

        public IsoMessageDef build() {
            Encoding mtiEncoding = Encoding.valueOf(
                    getMandatoryAttribute((Element) doc.getElementsByTagName(ELEMENT_MTI_ENCODING).item(0), ATTR_TYPE));
            NumericCodec mtiCodec = new NumericCodec(mtiEncoding, 4);
            LOG.info("MTI encoding: {}", mtiEncoding);

            CompositeDef headerDef = null;
            SortedMap<Integer, ComponentDef> headerComponents = buildHeaderComponents();
            if (headerComponents != null) {
                headerDef = new CompositeDef(headerComponents, new FixedCompositeCodec(), true);
            }

            Bitmap.Type msgBitmapType = Bitmap.Type.valueOf(
                    getMandatoryAttribute((Element) doc.getElementsByTagName(ELEMENT_MSG_BITMAP).item(0), ATTR_TYPE));
            LOG.info("Bitmap type: {}", msgBitmapType);
            BitmapCodec msgBitmapCodec = new BitmapCodec(msgBitmapType);
            Map<Integer, CompositeDef> fieldsDef = buildFieldsDefs(msgBitmapCodec);

            buildFieldsDefsExtension(fieldsDef);

            return new IsoMessageDef(headerDef, mtiCodec, fieldsDef);
        }

        /**
         * @return
         */
        private SortedMap<Integer, ComponentDef> buildHeaderComponents() {
            Element headerElement = (Element) doc.getElementsByTagName(ELEMENT_HEADER).item(0);
            return headerElement != null ? buildFixedComponents(headerElement) : null;
        }

        /**
         * @param bitmapCodec
         * @return
         */
        private Map<Integer, CompositeDef> buildFieldsDefs(BitmapCodec bitmapCodec) {
            NodeList messageList = doc.getElementsByTagName(ELEMENT_MESSAGE);
            Map<Integer, CompositeDef> defs = new TreeMap<>();

            for (int i = 0; i < messageList.getLength(); i++) {
                Element messageDef = (Element) messageList.item(i);
                Integer mti = getOptionalInteger(messageDef, ATTR_MTI);

                if (defs.containsKey(mti)) {
                    throw new ConfigException(format("Duplicate message config for mti %d", mti));
                }

                SortedMap<Integer, ComponentDef> messageFieldDefs = buildVarComponents(messageDef);

                if (messageFieldDefs.containsKey(1)) {
                    throw new ConfigException("Message field with index 1 not allowed");
                }

                defs.put(mti, new CompositeDef(messageFieldDefs, new FlexiCompositeCodec(bitmapCodec), true));
            }

            return defs;
        }

        /**
         * @param ce
         * @return
         */
        private SortedMap<Integer, ComponentDef> buildVarComponents(Element ce) {
            List<Element> fields = getSubElements(ce);
            SortedMap<Integer, ComponentDef> fieldDefs;
            if (fields.size() > 0) {
                fieldDefs = new TreeMap<>();
                for (Element e : fields) {
                    Integer index = Integer.valueOf(getMandatoryAttribute(e, ATTR_INDEX));
                    ComponentDef def = buildComponent(e, getOrdinality(e));
                    if (fieldDefs.containsKey(index)) {
                        throw new ConfigException(format("Duplicate field index: %d", index));
                    }
                    fieldDefs.put(index, def);
                }
            } else {
                throw new ConfigException("Composite components should have at least 1 sub field");
            }

            return fieldDefs;
        }

        /**
         * @param ce
         * @return
         */
        private SortedMap<Integer, ComponentDef> buildTlvComponents(Element ce) {
            List<Element> fields = getSubElements(ce);
            SortedMap<Integer, ComponentDef> fieldDefs;
            if (fields.size() > 0) {
                fieldDefs = new TreeMap<>();
                for (Element e : fields) {
                    Integer tag = Integer.valueOf(getMandatoryAttribute(e, ATTR_TAG));
                    ComponentDef def = buildTlvComponent(e, getOrdinality(e));
                    if (fieldDefs.containsKey(tag)) {
                        throw new ConfigException(format("Duplicate component tag: %d", tag));
                    }
                    fieldDefs.put(tag, def);
                }
            } else {
                throw new ConfigException("Composite components should have at least 1 sub field");
            }

            return fieldDefs;
        }

        /**
         * @param ce
         * @return
         */
        private SortedMap<Integer, ComponentDef> buildFixedComponents(Element ce) {
            List<Element> fields = getSubElements(ce);
            SortedMap<Integer, ComponentDef> fieldDefs;
            if (fields.size() > 0) {
                fieldDefs = new TreeMap<>();
                int index = 1;
                for (Element e : fields) {
                    ComponentDef def = buildComponent(e, true);
                    fieldDefs.put(index++, def);
                }
            } else {
                throw new ConfigException("Composite components should have at least 1 sub field");
            }
            return fieldDefs;
        }

        private ComponentDef buildTlvComponent(Element e, boolean mandatory) {
            ComponentDef def;
            switch (e.getTagName()) {
                case ELEMENT_COMPOSITE_TLV:
                    def = new CompositeDef(buildTlvComponents(e),
                            new TlvCompositeCodec(getEncoding(e, ATTR_TAG_ENCODING, defaultTlvTagEncoding),
                                    getEncoding(e, ATTR_LENGTH_ENCODING, defaultTlvLengthEncoding)), mandatory);
                    break;
                case ELEMENT_COMPOSITE:
                    Bitmap.Type bitmapType = getBitmapType(e);
                    BitmapCodec bitmapCodec = bitmapType != null ? new BitmapCodec(bitmapType) : null;
                    def = new CompositeDef(buildVarComponents(e), new FlexiCompositeCodec(bitmapCodec), mandatory);
                    break;
                case ELEMENT_ALPHA:
                    def = new ComponentDef(new AlphaCodec(getTrim(e)), mandatory);
                    break;
                case ELEMENT_NUMERIC:
                    def = new ComponentDef(new NumericCodec(getEncoding(e, ATTR_ENCODING, defaultNumericEncoding)),
                            mandatory);
                    break;
                case ELEMENT_BINARY:
                    def = new ComponentDef(new BinaryCodec(), mandatory);
                    break;
                case ELEMENT_CUSTOM:
                    def = new ComponentDef(buildCustomCodec(e), mandatory);
                    break;
                default:
                    throw new ConfigException("Unexepcted element: " + e.getTagName());
            }
            return def;
        }

        /**
         * @param e
         * @return
         */
        private ComponentDef buildComponent(Element e, boolean mandatory) {
            ComponentDef def;
            switch (e.getTagName()) {
                case ELEMENT_COMPOSITE_VAR:
                    Bitmap.Type bitmapType = getBitmapType(e);
                    BitmapCodec bitmapCodec = bitmapType != null ? new BitmapCodec(bitmapType) : null;

                    def = new CompositeDef(buildVarComponents(e), new FlexiCompositeCodec(bitmapCodec), mandatory,
                            buildVarLengthCodec(e));
                    break;
                case ELEMENT_COMPOSITE:
                    def = new CompositeDef(buildFixedComponents(e), new FixedCompositeCodec(), mandatory);
                    break;
                case ELEMENT_COMPOSITE_TLV:
                    def = new CompositeDef(buildTlvComponents(e),
                            new TlvCompositeCodec(getEncoding(e, ATTR_TAG_ENCODING, defaultTlvTagEncoding),
                                    getEncoding(e, ATTR_LENGTH_ENCODING, defaultTlvLengthEncoding)), mandatory,
                            buildVarLengthCodec(e));
                    break;
                case ELEMENT_ALPHA:
                    def = new ComponentDef(new AlphaCodec(getTrim(e), getLeftJustified(e),
                            Integer.valueOf(getMandatoryAttribute(e, ATTR_LENGTH))), mandatory);
                    break;
                case ELEMENT_ALPHA_VAR:
                    def = new ComponentDef(new VarCodec(buildVarLengthCodec(e), new AlphaCodec(getTrim(e))), mandatory);
                    break;
                case ELEMENT_NUMERIC:
                    def = new ComponentDef(new NumericCodec(getEncoding(e, ATTR_ENCODING, defaultNumericEncoding),
                            Integer.valueOf(e.getAttribute(ATTR_LENGTH))), mandatory);
                    break;
                case ELEMENT_NUMERIC_VAR:
                    def = new ComponentDef(new VarCodec(buildVarLengthCodec(e),
                            new NumericCodec(getEncoding(e, ATTR_ENCODING, defaultNumericEncoding))), mandatory);
                    break;
                case ELEMENT_DATE:
                    def = new ComponentDef(new DateTimeCodec(e.getAttribute(ATTR_FORMAT), getTimeZone(e),
                            getEncoding(e, ATTR_ENCODING, defaultDateEncoding)), mandatory);
                    break;
                case ELEMENT_BINARY:
                    def = new ComponentDef(new BinaryCodec(Integer.valueOf(e.getAttribute(ATTR_LENGTH))), mandatory);
                    break;
                case ELEMENT_BINARY_VAR:
                    def = new ComponentDef(new VarCodec(buildVarLengthCodec(e), new BinaryCodec()), mandatory);
                    break;
                case ELEMENT_CUSTOM_VAR:
                    def = new ComponentDef(new VarCodec(buildVarLengthCodec(e), buildCustomCodec(e)), mandatory);
                    break;
                case ELEMENT_CUSTOM:
                    def = new ComponentDef(buildCustomCodec(e), mandatory);
                    break;
                default:
                    throw new ConfigException("Unexepcted element: " + e.getTagName());
            }
            return def;
        }

        private Codec<?> buildCustomCodec(Element e) {
            String classAttr = e.getAttribute(ATTR_CODEC);
            Class<?> customCodecClass;
            try {
                customCodecClass = Class.forName(classAttr);
                if (CustomCodec.class.isAssignableFrom(customCodecClass)) {
                    CustomCodec customCodec = (CustomCodec) customCodecClass.newInstance();

                    Map<String, String> params = new HashMap<>();
                    List<Element> paramElements = getSubElements(e);
                    for (Element paramElement : paramElements) {
                        params.put(paramElement.getAttribute(ATTR_KEY), paramElement.getAttribute(ATTR_VALUE));
                    }
                    if (customCodec instanceof Configurable) {
                        ((Configurable) customCodec).configure(params);
                    }

                    return new CustomCodecAdapter(customCodec, getOptionalInteger(e, ATTR_LENGTH));
                } else {
                    throw new ConfigException(format("Invalid custom class %s", classAttr));
                }
            } catch (ClassNotFoundException | SecurityException | InstantiationException | IllegalAccessException
                    | IllegalArgumentException ex) {
                throw new ConfigException(ex.getMessage(), ex);
            }
        }

        private NumericCodec buildVarLengthCodec(Element e) {
            return new NumericCodec(getEncoding(e, ATTR_LENGTH_ENCODING, defaultLengthEncoding),
                    Integer.valueOf(getMandatoryAttribute(e, ATTR_LENGTH_DIGITS)));
        }

        private Boolean getTrim(Element e) {
            String value = getOptionalAttribute(e, ATTR_TRIM);
            return value != null ? ATTR_CONST_LEFT.equals(value) : defaultTrim;
        }

        private Boolean getLeftJustified(Element e) {
            String value = getOptionalAttribute(e, ATTR_JUSTIFIED);
            return value != null ? ATTR_CONST_LEFT.equals(value) : defaultLeftJustified;
        }

        private TimeZone getTimeZone(Element e) {
            String value = getOptionalAttribute(e, ATTR_TIMEZONE);
            return value != null ? TimeZone.getTimeZone(value) : defaultTimeZone;
        }

        private Encoding getEncoding(Element e, String attributeName, Encoding defaultEncoding) {
            String value = getOptionalAttribute(e, attributeName);
            return value != null ? Encoding.valueOf(value) : defaultEncoding;
        }

        private Integer getOptionalInteger(Element e, String attributeName) {
            String value = getOptionalAttribute(e, attributeName);
            return value != null ? Integer.valueOf(value) : null;
        }

        public Bitmap.Type getBitmapType(Element e) {
            String value = getOptionalAttribute(e, ATTR_BITMAP_TYPE);
            return value != null ? Bitmap.Type.valueOf(value) : null;
        }

        private boolean getOrdinality(Element e) {
            String value = getOptionalAttribute(e, ATTR_MANDATORY);
            return value != null ? Boolean.parseBoolean(value) : defaultMandatory;
        }

        private String getMandatoryAttribute(Element e, String attribute) {
            String value = getOptionalAttribute(e, attribute);
            if (value == null) {
                throw new ConfigException(
                        format("Missing mandatory attribute %s for element %s", attribute, e.getTagName()));
            } else {
                return value;
            }
        }

        private String getOptionalAttribute(Element e, String attribute) {
            return e.getAttribute(attribute).length() > 0 ? e.getAttribute(attribute) : null;
        }

        private List<Element> getSubElements(Element parent) {
            List<Element> subElements = new ArrayList<>();
            NodeList childNodes = parent.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node childNode = childNodes.item(i);
                if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                    subElements.add((Element) childNode);
                }
            }
            return subElements;
        }

        private void buildFieldsDefsExtension(Map<Integer, CompositeDef> existingCodecs) {
            NodeList messageExtList = doc.getElementsByTagName(ELEMENT_MESSAGE_EXT);
            Map<Integer, CompositeDef> extensions = new TreeMap<>();
            for (int i = 0; i < messageExtList.getLength(); i++) {
                Element messageDef = (Element) messageExtList.item(i);
                Integer mtiExisting = getOptionalInteger(messageDef, ATTR_EXTENDS);
                Integer mti = getOptionalInteger(messageDef, ATTR_MTI);
                if (existingCodecs.containsKey(mti) || extensions.containsKey(mti)) {
                    throw new ConfigException(format("Duplicate message config for mti %d", mti));
                }

                CompositeDef existing = existingCodecs.get(mtiExisting);
                if (existing == null) {
                    throw new ConfigException(format("Error extending mti %d, no config available", mti));
                }
                SortedMap<Integer, ComponentDef> clonedFieldsDef = cloneSubComponentDefs(
                        existing.getSubComponentDefs());

                Element setElement = null;
                Element removeElement = null;
                for (Element e : getSubElements(messageDef)) {
                    switch (e.getTagName()) {
                        case ELEMENT_SET:
                            setElement = e;
                            break;
                        case ELEMENT_REMOVE:
                            removeElement = e;
                            break;
                        default:
                            throw new ConfigException(
                                    format("Unknown message extension instruction %s", e.getTagName()));
                    }
                }

                if (setElement != null) {
                    setVarFields(clonedFieldsDef, getSubElements(setElement));
                }

                if (removeElement != null) {
                    removeFields(clonedFieldsDef, getSubElements(removeElement));
                }

                extensions.put(mti,
                        new CompositeDef(clonedFieldsDef, existing.getCompositeCodec(), existing.isMandatory(),
                                existing.getLengthCodec()));

            }
            existingCodecs.putAll(extensions);
        }

        private void removeFields(Map<Integer, ComponentDef> componentDefs, List<Element> subElements) {
            for (Element e : subElements) {
                Integer index = Integer.valueOf(e.getAttribute(ATTR_INDEX));
                switch (e.getTagName()) {
                    case ELEMENT_FIELD:
                        if (componentDefs.remove(index) == null) {
                            throw new ConfigException(format("Expected field %d not found", index));
                        }
                        break;
                    case ELEMENT_COMPOSITE:
                        ComponentDef def = componentDefs.get(index);
                        if (def instanceof CompositeDef) {
                            SortedMap<Integer, ComponentDef> subComponentDefs = ((CompositeDef) def)
                                    .getSubComponentDefs();
                            removeFields(subComponentDefs, getSubElements(e));
                            break;
                        }
                        throw new ConfigException(format("Expected composite field %d not found", index));
                    default:
                        throw new ConfigException(
                                format("Unknown message remove component instruction %s", e.getTagName()));
                }
            }
        }

        private boolean isCompositeVar(Element e) {
            return ELEMENT_COMPOSITE_VAR.equals(e.getTagName()) || (ELEMENT_COMPOSITE_TLV
                    .equals(((Element) e.getParentNode()).getTagName()) && ELEMENT_COMPOSITE.equals(e.getTagName()));
        }

        private CompositeDef buildCompositeDef(ComponentDef existingDef, Element e) {
            SortedMap<Integer, ComponentDef> existingSubComponentDefs;
            CompositeCodec existingCompositeCodec;
            Boolean existingMandatory = existingDef.isMandatory();
            Codec<Number> existingLengthCodec;

            CompositeDef newDef = null;

            if (existingDef instanceof CompositeDef) {
                existingSubComponentDefs = ((CompositeDef) existingDef).getSubComponentDefs();
                existingCompositeCodec = ((CompositeDef) existingDef).getCompositeCodec();
                existingLengthCodec = ((CompositeDef) existingDef).getLengthCodec();

                NumericCodec newLengthCodec = ELEMENT_COMPOSITE_TLV
                        .equals(((Element) e.getParentNode()).getTagName()) ? null : buildVarLengthCodec(e);

                CompositeCodec newCompositeCodec;
                Boolean newMandatory = getOrdinality(e);
                if (ELEMENT_COMPOSITE_TLV.equals(e.getTagName())) {
                    newCompositeCodec = new TlvCompositeCodec(getEncoding(e, ATTR_TAG_ENCODING, defaultTlvTagEncoding),
                            getEncoding(e, ATTR_LENGTH_ENCODING, defaultTlvLengthEncoding));
                } else {
                    Bitmap.Type bitmapType = getBitmapType(e);
                    BitmapCodec newBitmapCodec = bitmapType != null ? new BitmapCodec(bitmapType) : null;
                    newCompositeCodec = new FlexiCompositeCodec(newBitmapCodec);
                }

                if (existingSubComponentDefs != null && EqualsBuilder
                        .newInstance(existingCompositeCodec, newCompositeCodec).append(existingMandatory, newMandatory)
                        .append(existingLengthCodec, newLengthCodec).isEqual()) {
                    setVarFields(existingSubComponentDefs, getSubElements(e));

                    newDef = new CompositeDef(existingSubComponentDefs, newCompositeCodec, newMandatory,
                            newLengthCodec);
                }
            }

            return newDef;
        }

        private void setVarFields(Map<Integer, ComponentDef> components, List<Element> elements) {
            for (Element e : elements) {

                Integer index = getOptionalInteger(e, ATTR_INDEX);
                if (index == null) {
                    index = Integer.valueOf(getMandatoryAttribute(e, ATTR_TAG));
                }
                ComponentDef existingDef = components.get(index);
                ComponentDef newDef = null;
                if (isCompositeVar(e) || ELEMENT_COMPOSITE_TLV.equals(e.getTagName())) {
                    if (existingDef != null) {
                        newDef = buildCompositeDef(existingDef, e);
                    }
                }

                if (newDef == null) {
                    if (ELEMENT_COMPOSITE_TLV.equals(((Element) e.getParentNode()).getTagName())) {
                        newDef = buildTlvComponent(e, getOrdinality(e));
                    } else {
                        newDef = buildComponent(e, getOrdinality(e));
                    }

                }

                components.put(index, newDef);
            }
        }

        private SortedMap<Integer, ComponentDef> cloneSubComponentDefs(SortedMap<Integer, ComponentDef> existingDefs) {
            SortedMap<Integer, ComponentDef> clone = new TreeMap<>();
            for (Entry<Integer, ComponentDef> defEntry : existingDefs.entrySet()) {
                Integer index = defEntry.getKey();
                ComponentDef def = defEntry.getValue();
                if (def instanceof CompositeDef) {
                    CompositeDef compositeDef = (CompositeDef) def;

                    clone.put(index, new CompositeDef(cloneSubComponentDefs(compositeDef.getSubComponentDefs()),
                            compositeDef.getCompositeCodec(), compositeDef.isMandatory(),
                            compositeDef.getLengthCodec()));
                } else {
                    clone.put(index, new ComponentDef(def.getCodec(), def.isMandatory()));
                }
            }
            return clone;
        }

    }

}
