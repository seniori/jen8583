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

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 */
public final class IsoMessageDef {

    private static final Logger LOG = LoggerFactory.getLogger(IsoMessageDef.class);

    private final ComponentDef headerCodec;
    private final NumericCodec mtiCodec;
    private final Map<Integer, ComponentDef> fieldsCodec;

    public IsoMessageDef(ComponentDef headerDef, NumericCodec mtiCodec, Map<Integer, ComponentDef> fieldsDef) {
        this.headerCodec = headerDef;
        this.mtiCodec = mtiCodec;
        this.fieldsCodec = fieldsDef;
    }

    public ComponentDef getHeaderDef() {
        return headerCodec;
    }

    public NumericCodec getMtiCodec() {
        return mtiCodec;
    }

    public Map<Integer, ComponentDef> getFieldsDef() {
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
        private static final String ATTR_LENGTH_ENCODING = "tag-encoding";
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
        private static final String ATTR_CLASS = "class";
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
                                throw new ConfigException(String.format("Invalid value for justified: %s", stringJustify));
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
                        defaultTimeZone = ATTR_CONST_SYSTEM.equals(tzDefault) ? TimeZone.getDefault() : ZoneInfo.getTimeZone(tzDefault);
                        LOG.info("Default timezone: {}", defaultTimeZone.getID());
                        break;
                    case TAG_ORDINALITY:
                        defaultMandatory = Boolean.valueOf(e.getAttribute(ATTR_MANDATORY));
                        break;
                }
            }

        }

        public IsoMessageDef build() {
            Encoding mtiEncoding = Encoding.valueOf(((Element) doc.getElementsByTagName(TAG_MTI_ENCODING).item(0))
                    .getAttribute(ATTR_TYPE));
            NumericCodec mtiCodec = new NumericCodec(mtiEncoding, 4);
            LOG.info("MTI encoding: {}", mtiEncoding);

            ComponentDef headerDef = null;
            Map<Integer, ComponentDef> headerComponents = buildHeader();
            if (headerComponents != null) {
                headerDef = new ComponentDef(new CompositeCodec(headerComponents), true);
            }

            Bitmap.Type msgBitmapType = Bitmap.Type.valueOf(((Element) doc.getElementsByTagName(TAG_MSG_BITMAP).item(0))
                    .getAttribute(ATTR_TYPE));
            LOG.info("Bitmap type: {}", msgBitmapType);
            BitmapCodec msgBitmapCodec = new BitmapCodec(msgBitmapType);
            Map<Integer, ComponentDef> fieldsDef = buildFieldsDefs(msgBitmapCodec);

            buildFieldsDefsExtension(fieldsDef);

            return new IsoMessageDef(headerDef, mtiCodec, fieldsDef);
        }

        /**
         * @return
         */
        private Map<Integer, ComponentDef> buildHeader() {
            Element headerElement = (Element) doc.getElementsByTagName(TAG_HEADER).item(0);
            return headerElement != null ? buildFixedComponents(headerElement) : null;
        }

        /**
         * @param bitmapCodec
         * @return
         */
        private Map<Integer, ComponentDef> buildFieldsDefs(BitmapCodec bitmapCodec) {
            NodeList messageList = doc.getElementsByTagName(TAG_MESSAGE);
            Map<Integer, ComponentDef> defs = new TreeMap<>();

            for (int i = 0; i < messageList.getLength(); i++) {
                Element messageDef = (Element) messageList.item(i);
                Integer mti = getInteger(messageDef, ATTR_MTI);
                if (defs.containsKey(mti)) {
                    throw new ConfigException(String.format("Duplicate message config for mti %d", mti));
                }
                defs.put(mti, new ComponentDef(new CompositeCodec(buildVarComponents(messageDef), bitmapCodec), true));
            }
            return defs;
        }

        /**
         * @param ce
         * @return
         */
        private Map<Integer, ComponentDef> buildVarComponents(Element ce) {
            List<Element> fields = getSubElements(ce);
            Map<Integer, ComponentDef> fieldDefs = null;
            if (fields.size() > 0) {
                fieldDefs = new TreeMap<>();
                for (Element e : fields) {
                    Integer index = Integer.valueOf(e.getAttribute(ATTR_INDEX));
                    ComponentDef def = buildComponent(e, getMandatory(e));
                    if (fieldDefs.containsKey(index)) {
                        throw new ConfigException(String.format("Duplicate field index: %d", index));
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
        private Map<Integer, ComponentDef> buildFixedComponents(Element ce) {
            List<Element> fields = getSubElements(ce);
            Map<Integer, ComponentDef> fieldDefs = null;
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
            Codec<?> codec;
            switch (e.getTagName()) {
                case TAG_COMPOSITE_VAR:
                    Bitmap.Type bitmapType = getBitmapType(e);
                    BitmapCodec bitmapCodec = bitmapType != null ? new BitmapCodec(bitmapType) : null;
                    CompositeCodec compositeCodec = new CompositeCodec(buildVarComponents(e), bitmapCodec);
                    Integer tagDigits = getInteger(e, ATTR_TAG_LENGTH);

                    NumericCodec lengthCodec = buildVarLengthCodec(e);
                    if (tagDigits != null) {
                        codec = new TagVarCodec<>(compositeCodec, lengthCodec, new NumericCodec(getEncoding(e,
                                ATTR_TAG_ENCODING, defaultTagEncoding), tagDigits));
                    } else {
                        codec = new VarCodec<>(compositeCodec, lengthCodec);
                    }
                    break;
                case TAG_COMPOSITE:
                    codec = new CompositeCodec(buildFixedComponents(e));
                    break;
                case TAG_ALPHA:
                    codec = new AlphaCodec(getTrim(e), getLeftJustified(e), Integer.valueOf(e.getAttribute(ATTR_LENGTH)));
                    break;
                case TAG_ALPHA_VAR:
                    codec = new VarCodec<>(new AlphaCodec(getTrim(e)), buildVarLengthCodec(e));
                    break;
                case TAG_NUMERIC:
                    codec = new NumericCodec(getEncoding(e, ATTR_ENCODING, defaultNumericEncoding), Integer.valueOf(e
                            .getAttribute(ATTR_LENGTH)));
                    break;
                case TAG_NUMERIC_VAR:
                    codec = new VarCodec<>(new NumericCodec(getEncoding(e, ATTR_ENCODING, defaultNumericEncoding)),
                            buildVarLengthCodec(e));
                    break;
                case TAG_DATE:
                    codec = new DateTimeCodec(e.getAttribute(ATTR_FORMAT), getTimeZone(e), getEncoding(e, ATTR_ENCODING,
                            defaultDateEncoding));
                    break;
                case TAG_BINARY:
                    codec = new BinaryCodec(Integer.valueOf(e.getAttribute(ATTR_LENGTH)));
                    break;
                case TAG_BINARY_VAR:
                    codec = new VarCodec<>(new BinaryCodec(), buildVarLengthCodec(e));
                    break;
                case TAG_CUSTOM:
                case TAG_CUSTOM_VAR:
                    codec = buildCustomCodec(e);
                    break;
                default:
                    throw new ConfigException("Unexepcted tag: " + e.getTagName());
            }
            return new ComponentDef(codec, mandatory);
        }

        private Codec<?> buildCustomCodec(Element e) {
            String classAttr = e.getAttribute(ATTR_CLASS);
            Class<?> customClass;
            try {
                customClass = Class.forName(classAttr);
                if (CustomCodec.class.isAssignableFrom(customClass)) {
                    CustomCodec customCodec = (CustomCodec) customClass.newInstance();

                    Map<String, String> params = new HashMap<>();
                    List<Element> paramElements = getSubElements(e);
                    for (Element paramElement : paramElements) {
                        params.put(paramElement.getAttribute(ATTR_KEY), paramElement.getAttribute(ATTR_VALUE));
                    }
                    if (customCodec instanceof Configurable) {
                        ((Configurable) customCodec).configure(params);
                    }

                    Codec<Object> codec = new CustomCodecAdapter(customCodec);
                    if (TAG_CUSTOM_VAR.equals(e.getTagName())) {
                        return new VarCodec<>(codec, buildVarLengthCodec(e));
                    }
                    return codec;
                } else {
                    throw new ConfigException(String.format("Invalid custom class %s", classAttr));
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

        private void buildFieldsDefsExtension(Map<Integer, ComponentDef> existingCodecs) {
            NodeList messageExtList = doc.getElementsByTagName(TAG_MESSAGE_EXT);
            Map<Integer, ComponentDef> extensions = new TreeMap<>();
            for (int i = 0; i < messageExtList.getLength(); i++) {
                Element messageDef = (Element) messageExtList.item(i);
                Integer mtiExisting = getInteger(messageDef, ATTR_EXTENDS);
                Integer mti = getInteger(messageDef, ATTR_MTI);
                if (existingCodecs.containsKey(mti) || extensions.containsKey(mti)) {
                    throw new ConfigException(String.format("Duplicate message config for mti %d", mti));
                }

                ComponentDef existing = existingCodecs.get(mtiExisting);
                if (existing == null) {
                    throw new ConfigException(String.format("Error extending mti %d, no config available", mti));
                }
                CompositeCodec existingCompositeCodec = (CompositeCodec) existing.getCodec();
                Map<Integer, ComponentDef> clonedFieldsDef = clone(existingCompositeCodec.getSubComponentDefs());

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
                            throw new ConfigException(String.format("Unknown message extension instruction %s",
                                    e.getTagName()));
                    }
                }

                if (setElement != null) {
                    setVarFields(clonedFieldsDef, getSubElements(setElement));
                }

                if (removeElement != null) {
                    removeFields(clonedFieldsDef, getSubElements(removeElement));
                }

                extensions.put(mti,
                        new ComponentDef(new CompositeCodec(clonedFieldsDef, existingCompositeCodec.getBitmapCodec()),
                                true));

            }
            existingCodecs.putAll(extensions);
        }

        private void removeFields(Map<Integer, ComponentDef> componentDefs, List<Element> subElements) {
            for (Element e : subElements) {
                Integer index = Integer.valueOf(e.getAttribute(ATTR_INDEX));
                switch (e.getTagName()) {
                    case TAG_FIELD:
                        if (componentDefs.remove(index) == null) {
                            throw new ConfigException(String.format("Expected field %d not found", index));
                        }
                        break;
                    case TAG_COMPOSITE:
                        Codec<?> codec = componentDefs.get(index).getCodec();
                        if (codec instanceof VarCodec) {
                            codec = ((VarCodec<?>) codec).getCodec();
                        }
                        if (codec instanceof CompositeCodec) {
                            removeFields(((CompositeCodec) codec).getSubComponentDefs(), getSubElements(e));
                            break;
                        }
                        throw new ConfigException(String.format("Expected composite field %d not found", index));
                    default:
                        throw new ConfigException(String.format("Unknown message remove component instruction %s",
                                e.getTagName()));
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
                    Map<Integer, ComponentDef> existingSubComponentDefs = null;

                    ComponentDef existingDef = components.get(index);
                    if (existingDef != null) {
                        existingMandatory = existingDef.isMandatory();
                        Codec<?> codec = existingDef.getCodec();
                        if (codec instanceof TagVarCodec) {
                            existingTagCodec = ((TagVarCodec<?>) codec).getTagCodec();
                        }
                        if (codec instanceof VarCodec) {
                            existingLengthCodec = ((VarCodec<?>) codec).getLengthCodec();
                            codec = ((VarCodec<?>) codec).getCodec();
                        }
                        if (codec instanceof CompositeCodec) {
                            existingBitmapCodec = ((CompositeCodec) codec).getBitmapCodec();
                            existingSubComponentDefs = ((CompositeCodec) codec).getSubComponentDefs();
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

                    if (existingSubComponentDefs != null
                            && EqualsBuilder.newInstance(existingTagCodec, newTagCodec)
                            .append(existingLengthCodec, newLengthCodec)
                            .append(existingBitmapCodec, newBitmapCodec)
                            .append(existingMandatory, newMandatory).isEqual()) {
                        setVarFields(existingSubComponentDefs, getSubElements(e));
                        CompositeCodec compositeCodec = new CompositeCodec(existingSubComponentDefs, newBitmapCodec);
                        Codec<?> codec = null;
                        if (newTagCodec != null) {
                            codec = new TagVarCodec<>(compositeCodec, newLengthCodec, newTagCodec);
                        } else {
                            codec = new VarCodec<>(compositeCodec, newLengthCodec);
                        }
                        newDef = new ComponentDef(codec, newMandatory);
                    }
                }

                if (newDef == null) {
                    newDef = buildComponent(e, getMandatory(e));
                }
                components.put(index, newDef);
            }
        }

        private Map<Integer, ComponentDef> clone(Map<Integer, ComponentDef> existingDefs) {
            Map<Integer, ComponentDef> clone = new TreeMap<>();
            for (Entry<Integer, ComponentDef> defEntry : existingDefs.entrySet()) {
                Integer index = defEntry.getKey();
                ComponentDef def = defEntry.getValue();
                Codec<?> codec = def.getCodec();
                if (codec instanceof TagVarCodec) {
                    codec = cloneTagVarCodec((TagVarCodec<?>) codec);
                } else if (codec instanceof VarCodec) {
                    codec = cloneVarCodec((VarCodec<?>) codec);
                } else if (codec instanceof CompositeCodec) {
                    codec = cloneComposite((CompositeCodec) codec);
                }
                clone.put(index, new ComponentDef(codec, def.isMandatory()));
            }
            return clone;

        }

        private TagVarCodec<?> cloneTagVarCodec(TagVarCodec<?> tagVarCodec) {
            if (tagVarCodec.getCodec() instanceof CompositeCodec) {
                CompositeCodec compositeCodec = cloneComposite((CompositeCodec) tagVarCodec.getCodec());
                return new TagVarCodec<>(compositeCodec, tagVarCodec.getLengthCodec(), tagVarCodec.getTagCodec());
            } else {
                return tagVarCodec;
            }
        }

        private VarCodec<?> cloneVarCodec(VarCodec<?> varCodec) {
            if (varCodec.getCodec() instanceof CompositeCodec) {
                CompositeCodec compositeCodec = cloneComposite((CompositeCodec) varCodec.getCodec());
                return new VarCodec<>(compositeCodec, varCodec.getLengthCodec());
            } else {
                return varCodec;
            }
        }

        private CompositeCodec cloneComposite(CompositeCodec composite) {
            Map<Integer, ComponentDef> clone = clone(composite.getSubComponentDefs());
            return new CompositeCodec(clone, composite.getBitmapCodec());
        }

    }

}
