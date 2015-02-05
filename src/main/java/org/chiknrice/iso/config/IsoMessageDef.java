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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import org.chiknrice.iso.util.EqualsBuilder;
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
        return new ConfigBuilder(configXml).build();
    }

    private static class ConfigBuilder {

        private Encoding defaultTagEncoding;
        private Encoding defaultLengthEncoding;
        private boolean defaultTrim;
        private boolean defaultLeftJustified;
        private Encoding defaultNumericEncoding;
        private Encoding defaultDateEncoding;
        private TimeZone defaultTimeZone;
        private boolean defaultMandatory;

        private final Document doc;

        public ConfigBuilder(String configXml) {
            Schema schema = null;
            try {
                SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                schema = factory.newSchema(new StreamSource(Thread.currentThread().getContextClassLoader()
                        .getResourceAsStream("jen8583.xsd")));
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

            for (Element e : getSubElements(defaults)) {
                switch (e.getTagName()) {
                case "var":
                    defaultTagEncoding = Encoding.valueOf(e.getAttribute("tag-encoding"));
                    LOG.info("Default tag encoding: {}", defaultTagEncoding);
                    defaultLengthEncoding = Encoding.valueOf(e.getAttribute("length-encoding"));
                    LOG.info("Default length encoding: {}", defaultLengthEncoding);
                    break;
                case "alpha":
                    defaultTrim = Boolean.valueOf(e.getAttribute("trim"));
                    LOG.info("Default trim: {}", defaultTrim);
                    defaultLeftJustified = "LEFT".equals(e.getAttribute("justified"));
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

        public IsoMessageDef build() {
            Encoding mtiEncoding = Encoding.valueOf(((Element) doc.getElementsByTagName("mti-encoding").item(0))
                    .getAttribute("type"));
            NumericCodec mtiCodec = new NumericCodec(mtiEncoding, 4);
            LOG.info("MTI encoding: {}", mtiEncoding);

            ComponentDef headerDef = null;
            Map<Integer, ComponentDef> headerComponents = buildHeader();
            if (headerComponents != null) {
                headerDef = new ComponentDef(new CompositeCodec(headerComponents), true);
            }

            Bitmap.Type msgBitmapType = Bitmap.Type.valueOf(((Element) doc.getElementsByTagName("msg-bitmap").item(0))
                    .getAttribute("type"));
            LOG.info("Bitmap type: {}", msgBitmapType);
            BitmapCodec bitmapCodec = new BitmapCodec(msgBitmapType);
            Map<Integer, ComponentDef> fieldsDef = buildFieldsDefs(bitmapCodec);

            buildFieldsDefsExtension(fieldsDef, bitmapCodec);

            return new IsoMessageDef(headerDef, mtiCodec, fieldsDef);
        }

        /**
         * @param doc
         * @return
         */
        private Map<Integer, ComponentDef> buildHeader() {
            Element headerElement = (Element) doc.getElementsByTagName("header").item(0);
            return headerElement != null ? buildFixedComponents(headerElement) : null;
        }

        /**
         * @param bitmapCodec
         * @return
         */
        private Map<Integer, ComponentDef> buildFieldsDefs(BitmapCodec bitmapCodec) {
            NodeList messageList = doc.getElementsByTagName("message");
            Map<Integer, ComponentDef> defs = new TreeMap<>();

            for (int i = 0; i < messageList.getLength(); i++) {
                Element messageDef = (Element) messageList.item(i);
                Integer mti = getInteger(messageDef, "mti");
                if (defs.containsKey(mti)) {
                    throw new RuntimeException(String.format("Duplicate message config for mti %d", mti));
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
                    Integer index = Integer.valueOf(e.getAttribute("index"));
                    ComponentDef def = buildComponent(e, getMandatory(e));
                    if (fieldDefs.containsKey(index)) {
                        throw new RuntimeException(String.format("Duplicate field index: %d", index));
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
            case "composite-var":
                Bitmap.Type bitmapType = getBitmapType(e);
                BitmapCodec bitmapCodec = bitmapType != null ? new BitmapCodec(bitmapType) : null;
                CompositeCodec compositeCodec = new CompositeCodec(buildVarComponents(e), bitmapCodec);
                Integer tagDigits = getInteger(e, "tag-length");

                NumericCodec lengthCodec = buildVarLengthCodec(e);
                if (tagDigits != null) {
                    codec = new TagVarCodec<>(compositeCodec, lengthCodec, new NumericCodec(getEncoding(e,
                            "tag-encoding", defaultTagEncoding), tagDigits));
                } else {
                    codec = new VarCodec<>(compositeCodec, lengthCodec);
                }
                break;
            case "composite":
                codec = new CompositeCodec(buildFixedComponents(e));
                break;
            case "alpha":
                codec = new AlphaCodec(getTrim(e), getLeftJustified(e), Integer.valueOf(e.getAttribute("length")));
                break;
            case "alpha-var":
                codec = new VarCodec<>(new AlphaCodec(getTrim(e)), buildVarLengthCodec(e));
                break;
            case "numeric":
                codec = new NumericCodec(getEncoding(e, "encoding", defaultNumericEncoding), Integer.valueOf(e
                        .getAttribute("length")));
                break;
            case "numeric-var":
                codec = new VarCodec<>(new NumericCodec(getEncoding(e, "encoding", defaultNumericEncoding)),
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
                codec = new VarCodec<>(new BinaryCodec(), buildVarLengthCodec(e));
                break;
            case "custom":
            case "custom-var":
                codec = buildCustomCodec(e);
                break;
            default:
                throw new RuntimeException("Unexepcted tag: " + e.getTagName());
            }
            return new ComponentDef(codec, mandatory);
        }

        private Codec<?> buildCustomCodec(Element e) {
            String classAttr = e.getAttribute("class");
            Class<?> customClass;
            try {
                customClass = Class.forName(classAttr);
                if (CustomCodec.class.isAssignableFrom(customClass)) {
                    CustomCodec customCodec = (CustomCodec) customClass.newInstance();

                    Map<String, String> params = new HashMap<>();
                    List<Element> paramElements = getSubElements(e);
                    for (Element paramElement : paramElements) {
                        params.put(paramElement.getAttribute("key"), paramElement.getAttribute("value"));
                    }
                    if (customCodec instanceof Configurable) {
                        ((Configurable) customCodec).configure(params);
                    }

                    Codec<Object> codec = new CustomCodecAdapter(customCodec);
                    if ("custom-var".equals(e.getTagName())) {
                        return new VarCodec<>(codec, buildVarLengthCodec(e));
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

        private Boolean getTrim(Element e) {
            String value = getOptionalAttribute(e, "trim");
            return value != null ? "LEFT".equals(value) : defaultTrim;
        }

        private Boolean getLeftJustified(Element e) {
            String value = getOptionalAttribute(e, "justified");
            return value != null ? "LEFT".equals(value) : defaultLeftJustified;
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

        private boolean getMandatory(Element e) {
            String value = getOptionalAttribute(e, "mandatory");
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

        private void buildFieldsDefsExtension(Map<Integer, ComponentDef> existingCodecs, BitmapCodec bitmapCodec) {
            NodeList messageExtList = doc.getElementsByTagName("message-ext");
            Map<Integer, ComponentDef> extensions = new TreeMap<>();
            for (int i = 0; i < messageExtList.getLength(); i++) {
                Element messageDef = (Element) messageExtList.item(i);
                Integer mtiExisting = getInteger(messageDef, "extends");
                Integer mti = getInteger(messageDef, "mti");
                if (existingCodecs.containsKey(mti) || extensions.containsKey(mti)) {
                    throw new RuntimeException(String.format("Duplicate message config for mti %d", mti));
                }

                ComponentDef existing = existingCodecs.get(mtiExisting);
                if (existing == null) {
                    throw new RuntimeException(String.format("Error extending mti %d, no config available", mti));
                }
                CompositeCodec existingCompositeCodec = (CompositeCodec) existing.getCodec();
                Map<Integer, ComponentDef> clonedFieldsDef = clone(existingCompositeCodec.getSubComponentDefs());

                Element setElement = null;
                Element removeElement = null;
                for (Element e : getSubElements(messageDef)) {
                    switch (e.getTagName()) {
                    case "set":
                        setElement = e;
                        break;
                    case "remove":
                        removeElement = e;
                        break;
                    default:
                        throw new RuntimeException(String.format("Unknown message extension instruction %s",
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
                Integer index = Integer.valueOf(e.getAttribute("index"));
                switch (e.getTagName()) {
                case "field":
                    if (componentDefs.remove(index) == null) {
                        throw new RuntimeException(String.format("Expected field %d not found", index));
                    }
                    break;
                case "composite":
                    Codec<?> codec = componentDefs.get(index).getCodec();
                    if (codec instanceof VarCodec) {
                        codec = ((VarCodec<?>) codec).getCodec();
                    }
                    if (codec instanceof CompositeCodec) {
                        removeFields(((CompositeCodec) codec).getSubComponentDefs(), getSubElements(e));
                        break;
                    }
                    throw new RuntimeException(String.format("Expected composite field %d not found", index));
                default:
                    throw new RuntimeException(String.format("Unknown message remove component instruction %s",
                            e.getTagName()));
                }
            }
        }

        private void setVarFields(Map<Integer, ComponentDef> components, List<Element> elements) {
            for (Element e : elements) {
                Integer index = Integer.valueOf(e.getAttribute("index"));

                ComponentDef newDef = null;

                if ("composite-var".equals(e.getTagName())) {
                    NumericCodec existingTagCodec = null;
                    NumericCodec existingLengthCodec = null;
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
                    Integer tagDigits = getInteger(e, "tag-length");
                    if (tagDigits != null) {
                        newTagCodec = new NumericCodec(getEncoding(e, "tag-encoding", defaultTagEncoding), tagDigits);
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
