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

    private static class ConfigBuilder {

        private static final String TAG_DEFAULTS = "defaults";

        private static final String TAG_VAR = "var";
        private static final String TAG_COMPOSITE = "composite";
        private static final String TAG_COMPOSITE_VAR = "composite-var";
        private static final String TAG_FIELD = "field";
        private static final String TAG_ALPHA = "alpha";
        private static final String TAG_ALPHA_VAR = "alpha-var";
        private static final String TAG_NUMERIC = "numeric";
        private static final String TAG_NUMERIC_VAR = "numeric-var";
        private static final String TAG_DATE = "date";
        private static final String TAG_BINARY = "binary";
        private static final String TAG_BINARY_VAR = "binary-var";
        private static final String TAG_CUSTOM = "custom";
        private static final String TAG_CUSTOM_VAR = "custom-var";
        private static final String TAG_ORDINALITY = "ordinality";
        private static final String TAG_MTI_ENCODING = "mti-encoding";
        private static final String TAG_MSG_BITMAP = "msg-bitmap";
        private static final String TAG_HEADER = "header";
        private static final String TAG_MESSAGE = "message";
        private static final String TAG_MESSAGE_EXT = "message-ext";
        private static final String TAG_SET = "set";
        private static final String TAG_REMOVE = "remove";

        private static final String ATTR_MTI = "mti";
        private static final String ATTR_EXTENDS = "extends";
        private static final String ATTR_TAG_ENCODING = "tag-encoding";
        private static final String ATTR_TAG_LENGTH = "tag-length";
        private static final String ATTR_LENGTH_ENCODING = "length-encoding";
        private static final String ATTR_LENGTH = "length";
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

        private Encoding defaultTagEncoding;
        private Encoding defaultLengthEncoding;
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
                Schema schema = factory.newSchema(new StreamSource(Thread.currentThread().getContextClassLoader()
                        .getResourceAsStream("jen8583.xsd")));

                Validator validator = schema.newValidator();
                validator.validate(new DOMSource(doc));
            } catch (Exception e) {
                throw new ConfigException(e.getMessage(), e);
            }

            Element defaults = (Element) doc.getElementsByTagName(TAG_DEFAULTS).item(0);

            for (Element e : getSubElements(defaults)) {
                switch (e.getTagName()) {
                    case TAG_VAR:
                        defaultTagEncoding = Encoding.valueOf(e.getAttribute(ATTR_TAG_ENCODING));
                        LOG.info("Default tag encoding: {}", defaultTagEncoding);
                        defaultLengthEncoding = Encoding.valueOf(e.getAttribute(ATTR_LENGTH_ENCODING));
                        LOG.info("Default length encoding: {}", defaultLengthEncoding);
                        break;
                    case TAG_ALPHA:
                        defaultTrim = Boolean.valueOf(e.getAttribute(ATTR_TRIM));
                        LOG.info("Default trim: {}", defaultTrim);
                        String stringJustify = e.getAttribute(ATTR_JUSTIFIED);
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
                    case TAG_NUMERIC:
                        defaultNumericEncoding = Encoding.valueOf(e.getAttribute(ATTR_ENCODING));
                        LOG.info("Default numeric encoding: {}", defaultNumericEncoding);
                        break;
                    case TAG_DATE:
                        defaultDateEncoding = Encoding.valueOf(e.getAttribute(ATTR_ENCODING));
                        LOG.info("Default date encoding: {}", defaultDateEncoding);
                        String tzDefault = e.getAttribute(ATTR_TIMEZONE);
                        defaultTimeZone = ATTR_CONST_SYSTEM.equals(tzDefault) ? TimeZone.getDefault() : ZoneInfo
                                .getTimeZone(tzDefault);
                        LOG.info("Default timezone: {}", defaultTimeZone.getID());
                        break;
                    case TAG_ORDINALITY:
                        defaultMandatory = Boolean.valueOf(e.getAttribute(ATTR_MANDATORY));
                        break;
                }
            }

        }

        public IsoMessageDef build() {
            Encoding mtiEncoding = Encoding
                    .valueOf(((Element) doc.getElementsByTagName(TAG_MTI_ENCODING).item(0)).getAttribute(ATTR_TYPE));
            NumericCodec mtiCodec = new NumericCodec(mtiEncoding, 4);
            LOG.info("MTI encoding: {}", mtiEncoding);

            CompositeDef headerDef = null;
            SortedMap<Integer, ComponentDef> headerComponents = buildHeader();
            if (headerComponents != null) {
                headerDef = new CompositeDef(headerComponents);
            }

            Bitmap.Type msgBitmapType = Bitmap.Type
                    .valueOf(((Element) doc.getElementsByTagName(TAG_MSG_BITMAP).item(0)).getAttribute(ATTR_TYPE));
            LOG.info("Bitmap type: {}", msgBitmapType);
            BitmapCodec msgBitmapCodec = new BitmapCodec(msgBitmapType);
            Map<Integer, CompositeDef> fieldsDef = buildFieldsDefs(msgBitmapCodec);

            buildFieldsDefsExtension(fieldsDef);

            return new IsoMessageDef(headerDef, mtiCodec, fieldsDef);
        }

        /**
         * @return
         */
        private SortedMap<Integer, ComponentDef> buildHeader() {
            Element headerElement = (Element) doc.getElementsByTagName(TAG_HEADER).item(0);
            return headerElement != null ? buildFixedComponents(headerElement) : null;
        }

        /**
         * @param bitmapCodec
         * @return
         */
        private Map<Integer, CompositeDef> buildFieldsDefs(BitmapCodec bitmapCodec) {
            NodeList messageList = doc.getElementsByTagName(TAG_MESSAGE);
            Map<Integer, CompositeDef> defs = new TreeMap<>();

            for (int i = 0; i < messageList.getLength(); i++) {
                Element messageDef = (Element) messageList.item(i);
                Integer mti = getInteger(messageDef, ATTR_MTI);
                if (defs.containsKey(mti)) {
                    throw new ConfigException(format("Duplicate message config for mti %d", mti));
                }
                defs.put(mti, new CompositeDef(buildVarComponents(messageDef), bitmapCodec));
            }
            return defs;
        }

        /**
         * @param ce
         * @return
         */
        private SortedMap<Integer, ComponentDef> buildVarComponents(Element ce) {
            List<Element> fields = getSubElements(ce);
            SortedMap<Integer, ComponentDef> fieldDefs = null;
            if (fields.size() > 0) {
                fieldDefs = new TreeMap<>();
                for (Element e : fields) {
                    Integer index = Integer.valueOf(e.getAttribute(ATTR_INDEX));
                    ComponentDef def = buildComponent(e, getMandatory(e));
                    if (fieldDefs.containsKey(index)) {
                        throw new ConfigException(format("Duplicate field index: %d", index));
                    }
                    fieldDefs.put(index, def);
                }
            }

            return fieldDefs;
        }

        /**
         * @param ce
         * @return
         */
        private SortedMap<Integer, ComponentDef> buildFixedComponents(Element ce) {
            List<Element> fields = getSubElements(ce);
            SortedMap<Integer, ComponentDef> fieldDefs = null;
            if (fields.size() > 0) {
                fieldDefs = new TreeMap<>();
                int index = 1;
                for (Element e : fields) {
                    ComponentDef def = buildComponent(e, true);
                    fieldDefs.put(index++, def);
                }
            }
            return fieldDefs;
        }

        /**
         * @param e
         * @return
         */
        private ComponentDef buildComponent(Element e, boolean mandatory) {
            SortedMap<Integer, ComponentDef> subComponentDefs = null;
            BitmapCodec bitmapCodec = null;
            Codec<Number> tagCodec = null;
            Codec<Number> lengthCodec = null;
            Codec<?> valueCodec = null;
            switch (e.getTagName()) {
                case TAG_COMPOSITE_VAR:
                    Bitmap.Type bitmapType = getBitmapType(e);
                    bitmapCodec = bitmapType != null ? new BitmapCodec(bitmapType) : null;
                    subComponentDefs = buildVarComponents(e);
                    Integer tagDigits = getInteger(e, ATTR_TAG_LENGTH);

                    lengthCodec = buildVarLengthCodec(e);
                    if (tagDigits != null) {
                        tagCodec = new NumericCodec(getEncoding(e, ATTR_TAG_ENCODING, defaultTagEncoding), tagDigits);
                    }
                    break;
                case TAG_COMPOSITE:
                    subComponentDefs = buildFixedComponents(e);
                    break;
                case TAG_ALPHA:
                    valueCodec = new AlphaCodec(getTrim(e), getLeftJustified(e), Integer
                            .valueOf(e.getAttribute(ATTR_LENGTH)));
                    break;
                case TAG_ALPHA_VAR:
                    lengthCodec = buildVarLengthCodec(e);
                    valueCodec = new AlphaCodec(getTrim(e));
                    break;
                case TAG_NUMERIC:
                    valueCodec = new NumericCodec(getEncoding(e, ATTR_ENCODING, defaultNumericEncoding), Integer
                            .valueOf(e.getAttribute(ATTR_LENGTH)));
                    break;
                case TAG_NUMERIC_VAR:
                    lengthCodec = buildVarLengthCodec(e);
                    valueCodec = new NumericCodec(getEncoding(e, ATTR_ENCODING, defaultNumericEncoding));
                    break;
                case TAG_DATE:
                    valueCodec = new DateTimeCodec(e
                            .getAttribute(ATTR_FORMAT), getTimeZone(e), getEncoding(e, ATTR_ENCODING, defaultDateEncoding));
                    break;
                case TAG_BINARY:
                    valueCodec = new BinaryCodec(Integer.valueOf(e.getAttribute(ATTR_LENGTH)));
                    break;
                case TAG_BINARY_VAR:
                    lengthCodec = buildVarLengthCodec(e);
                    valueCodec = new BinaryCodec();
                    break;
                case TAG_CUSTOM_VAR:
                    lengthCodec = buildVarLengthCodec(e);
                case TAG_CUSTOM:
                    valueCodec = buildCustomCodec(e);
                    break;
                default:
                    throw new ConfigException("Unexepcted tag: " + e.getTagName());
            }
            if (subComponentDefs != null) {
                return new CompositeDef(subComponentDefs, bitmapCodec, tagCodec, lengthCodec, mandatory);
            } else {
                return new ComponentDef(tagCodec, lengthCodec, valueCodec, mandatory);
            }
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

                    return new CustomCodecAdapter(customCodec, getInteger(e, ATTR_LENGTH));
                } else {
                    throw new ConfigException(format("Invalid custom class %s", classAttr));
                }
            } catch (ClassNotFoundException | SecurityException | InstantiationException | IllegalAccessException
                    | IllegalArgumentException ex) {
                throw new ConfigException(ex.getMessage(), ex);
            }
        }

        private NumericCodec buildVarLengthCodec(Element e) {
            return new NumericCodec(getEncoding(e, ATTR_LENGTH_ENCODING, defaultLengthEncoding), getInteger(e, ATTR_LENGTH));
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

        private Integer getInteger(Element e, String attributeName) {
            String value = getOptionalAttribute(e, attributeName);
            return value != null ? Integer.valueOf(value) : null;
        }

        public Bitmap.Type getBitmapType(Element e) {
            String value = getOptionalAttribute(e, ATTR_BITMAP_TYPE);
            return value != null ? Bitmap.Type.valueOf(value) : null;
        }

        private boolean getMandatory(Element e) {
            String value = getOptionalAttribute(e, ATTR_MANDATORY);
            return value != null ? Boolean.parseBoolean(value) : defaultMandatory;
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
            NodeList messageExtList = doc.getElementsByTagName(TAG_MESSAGE_EXT);
            Map<Integer, CompositeDef> extensions = new TreeMap<>();
            for (int i = 0; i < messageExtList.getLength(); i++) {
                Element messageDef = (Element) messageExtList.item(i);
                Integer mtiExisting = getInteger(messageDef, ATTR_EXTENDS);
                Integer mti = getInteger(messageDef, ATTR_MTI);
                if (existingCodecs.containsKey(mti) || extensions.containsKey(mti)) {
                    throw new ConfigException(format("Duplicate message config for mti %d", mti));
                }

                CompositeDef existing = existingCodecs.get(mtiExisting);
                if (existing == null) {
                    throw new ConfigException(format("Error extending mti %d, no config available", mti));
                }
                SortedMap<Integer, ComponentDef> existingSubComponentDefs = existing.getSubComponentDefs();
                SortedMap<Integer, ComponentDef> clonedFieldsDef = clone(existingSubComponentDefs);

                Element setElement = null;
                Element removeElement = null;
                for (Element e : getSubElements(messageDef)) {
                    switch (e.getTagName()) {
                        case TAG_SET:
                            setElement = e;
                            break;
                        case TAG_REMOVE:
                            removeElement = e;
                            break;
                        default:
                            throw new ConfigException(format("Unknown message extension instruction %s", e
                                    .getTagName()));
                    }
                }

                if (setElement != null) {
                    setVarFields(clonedFieldsDef, getSubElements(setElement));
                }

                if (removeElement != null) {
                    removeFields(clonedFieldsDef, getSubElements(removeElement));
                }

                BitmapCodec existingBitmapCodec = existing.getBitmapCodec();
                extensions.put(mti, new CompositeDef(clonedFieldsDef, existingBitmapCodec));

            }
            existingCodecs.putAll(extensions);
        }

        private void removeFields(Map<Integer, ComponentDef> componentDefs, List<Element> subElements) {
            for (Element e : subElements) {
                Integer index = Integer.valueOf(e.getAttribute(ATTR_INDEX));
                switch (e.getTagName()) {
                    case TAG_FIELD:
                        if (componentDefs.remove(index) == null) {
                            throw new ConfigException(format("Expected field %d not found", index));
                        }
                        break;
                    case TAG_COMPOSITE:
                        ComponentDef def = componentDefs.get(index);
                        if (def instanceof CompositeDef) {
                            SortedMap<Integer, ComponentDef> subComponentDefs = ((CompositeDef) def)
                                    .getSubComponentDefs();
                            removeFields(subComponentDefs, getSubElements(e));
                            break;
                        }
                        throw new ConfigException(format("Expected composite field %d not found", index));
                    default:
                        throw new ConfigException(format("Unknown message remove component instruction %s", e
                                .getTagName()));
                }
            }
        }

        private void setVarFields(Map<Integer, ComponentDef> components, List<Element> elements) {
            for (Element e : elements) {
                Integer index = Integer.valueOf(e.getAttribute(ATTR_INDEX));

                ComponentDef newDef = null;

                if (TAG_COMPOSITE_VAR.equals(e.getTagName())) {
                    Codec<Number> existingTagCodec = null;
                    Codec<Number> existingLengthCodec = null;
                    BitmapCodec existingBitmapCodec = null;
                    Boolean existingMandatory = null;
                    SortedMap<Integer, ComponentDef> existingSubComponentDefs = null;

                    ComponentDef existingDef = components.get(index);
                    if (existingDef != null) {
                        existingTagCodec = existingDef.getTagCodec();
                        existingLengthCodec = existingDef.getLengthCodec();
                        existingMandatory = existingDef.isMandatory();

                        if (existingDef instanceof CompositeDef) {
                            existingBitmapCodec = ((CompositeDef) existingDef).getBitmapCodec();
                            existingSubComponentDefs = ((CompositeDef) existingDef).getSubComponentDefs();
                        }
                    }

                    NumericCodec newTagCodec = null;
                    Integer tagDigits = getInteger(e, ATTR_TAG_LENGTH);
                    if (tagDigits != null) {
                        newTagCodec = new NumericCodec(getEncoding(e, ATTR_TAG_ENCODING, defaultTagEncoding), tagDigits);
                    }
                    NumericCodec newLengthCodec = buildVarLengthCodec(e);

                    Bitmap.Type bitmapType = getBitmapType(e);
                    BitmapCodec newBitmapCodec = bitmapType != null ? new BitmapCodec(bitmapType) : null;

                    Boolean newMandatory = getMandatory(e);

                    if (existingSubComponentDefs != null && EqualsBuilder.newInstance(existingTagCodec, newTagCodec)
                            .append(existingLengthCodec, newLengthCodec).append(existingBitmapCodec, newBitmapCodec)
                            .append(existingMandatory, newMandatory).isEqual()) {
                        setVarFields(existingSubComponentDefs, getSubElements(e));

                        newDef = new CompositeDef(existingSubComponentDefs, newBitmapCodec, newTagCodec, newLengthCodec, newMandatory);
                    }
                }

                if (newDef == null) {
                    newDef = buildComponent(e, getMandatory(e));
                }
                components.put(index, newDef);
            }
        }

        private SortedMap<Integer, ComponentDef> clone(SortedMap<Integer, ComponentDef> existingDefs) {
            SortedMap<Integer, ComponentDef> clone = new TreeMap<>();
            for (Entry<Integer, ComponentDef> defEntry : existingDefs.entrySet()) {
                Integer index = defEntry.getKey();
                ComponentDef def = defEntry.getValue();
                if (def instanceof CompositeDef) {
                    clone.put(index, new CompositeDef(clone(((CompositeDef) def)
                            .getSubComponentDefs()), ((CompositeDef) def).getBitmapCodec(), def.getTagCodec(), def
                            .getLengthCodec(), def.isMandatory()));
                } else {
                    clone.put(index, new ComponentDef(def.getTagCodec(), def.getLengthCodec(), def.getValueCodec(), def
                            .isMandatory()));
                }
            }
            return clone;
        }

    }

}
