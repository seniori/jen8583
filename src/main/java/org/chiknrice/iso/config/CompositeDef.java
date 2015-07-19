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

import org.chiknrice.iso.CodecException;
import org.chiknrice.iso.ConfigException;
import org.chiknrice.iso.codec.BitmapCodec;
import org.chiknrice.iso.codec.Codec;
import org.chiknrice.iso.util.EqualsBuilder;
import org.chiknrice.iso.util.Hash;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static java.lang.String.format;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 */
public class CompositeDef extends ComponentDef implements Codec<Map<Integer, Object>> {

    private final SortedMap<Integer, ComponentDef> subComponentDefs;
    private final BitmapCodec bitmapCodec;

    public CompositeDef(SortedMap<Integer, ComponentDef> subComponentDefs) {
        this(subComponentDefs, null);
    }

    public CompositeDef(SortedMap<Integer, ComponentDef> subComponentDefs, BitmapCodec bitmapCodec) {
        this(subComponentDefs, bitmapCodec, null, null, true);
    }

    public CompositeDef(SortedMap<Integer, ComponentDef> subComponentDefs, BitmapCodec bitmapCodec, Codec<Number> tagCodec, Codec<Number> lengthCodec, boolean mandatory) {
        super(tagCodec, lengthCodec, null, mandatory);
        this.subComponentDefs = subComponentDefs;
        this.bitmapCodec = bitmapCodec;

        if (bitmapCodec != null && subComponentDefs.containsKey(1)) {
            throw new ConfigException("Composite components with bitmap cannot have sub field index 1");
        }

        if (subComponentDefs == null || subComponentDefs.size() == 0) {
            throw new ConfigException("Composite components should have at least 1 sub field");
        }

        for (ComponentDef subComponentDef : subComponentDefs.values()) {
            subComponentDef.parent = this;
        }
    }

    public Map<Integer, Object> decode(ByteBuffer buf) {
        BitmapCodec.Bitmap bitmap = null;
        try {
            bitmap = bitmapCodec != null ? bitmapCodec.decode(buf) : null;
        } catch (Exception e) {
            throw new CodecException(format("Failed to decode bitmap for %s", this), e);
        }

        Map<Integer, Object> values = new TreeMap<>();
        for (Map.Entry<Integer, ComponentDef> defEntry : subComponentDefs.entrySet()) {
            Integer index = defEntry.getKey();
            ComponentDef def = defEntry.getValue();
            Codec<Number> tagCodec = def.getTagCodec();
            Codec<Number> lengthCodec = def.getLengthCodec();
            Codec valueCodec = def.getValueCodec();

            if (bitmap != null && !bitmap.isSet(index)) {
                if (def.isMandatory()) {
                    throw new CodecException(format("Missing mandatory component %s", def));
                }
                continue;
            }

            Object value;
            try {
                if (tagCodec != null) {
                    // nothing to do with a tag yet
                    Integer tag = tagCodec.decode(buf).intValue();
                    if (!index.equals(tag)) {
                        throw new CodecException(format("Unexpected TLV tag %d when decoding %s", tag, def));
                    }
                }

                ByteBuffer valueBuf;
                if (lengthCodec != null) {
                    int varLength = lengthCodec.decode(buf).intValue();
                    int limit = valueCodec.getEncoding() == Encoding.BCD ? varLength / 2 + varLength % 2 : varLength;
                    valueBuf = buf.slice();
                    valueBuf.limit(limit);
                    buf.position(buf.position() + limit);
                } else {
                    valueBuf = buf;
                }

                if (def instanceof CompositeDef) {
                    value = ((CompositeDef) def).decode(valueBuf);
                } else {
                    value = valueCodec.decode(valueBuf);
                }
            } catch (CodecException e) {
                throw new CodecException(format("Failed to decode %s", def), e);
            }

            if (value == null && def.isMandatory()) {
                throw new CodecException(format("Missing mandatory component %s", def));
            }

            values.put(index, value);
        }
        return values;
    }

    @SuppressWarnings("unchecked")
    public void encode(ByteBuffer buf, Map<Integer, Object> values) {
        if (bitmapCodec != null) {
            try {
                bitmapCodec.encode(buf, values.keySet());
            } catch (Exception e) {
                throw new CodecException(format("Failed to encode bitmap for %s", this), e);
            }
        }
        Map<Integer, Object> tempMap = new HashMap<>(values);
        for (Map.Entry<Integer, ComponentDef> defEntry : subComponentDefs.entrySet()) {
            Integer index = defEntry.getKey();
            ComponentDef def = defEntry.getValue();
            Object value = tempMap.remove(index);
            Codec<Number> tagCodec = def.getTagCodec();
            Codec<Number> lengthCodec = def.getLengthCodec();
            Codec valueCodec = def.getValueCodec();

            if (value == null) {
                if (def.isMandatory()) {
                    throw new CodecException(format("Missing mandatory component %s", def));
                } else {
                    continue;
                }
            }

            try {
                if (tagCodec != null) {
                    // nothing to do with a codec yet
                    tagCodec.encode(buf, index);
                }

                ByteBuffer valueBuf;
                if (lengthCodec != null) {
                    buf.mark();
                    // this is just to consume part of the buffer which would later be filled with correct length data
                    lengthCodec.encode(buf, 0);
                    valueBuf = buf.slice();
                } else {
                    valueBuf = buf;
                }

                if (def instanceof CompositeDef) {
                    ((CompositeDef) def).encode(valueBuf, (Map<Integer, Object>) value);
                } else {
                    valueCodec.encode(valueBuf, value);
                }

                if (lengthCodec != null) {
                    int endPos = buf.position() + valueBuf.position();
                    buf.reset();
                    int valueLength;
                    if (Encoding.BINARY == valueCodec.getEncoding()) {
                        valueLength = valueBuf.position();
                    } else {
                        valueLength = value.toString().length();
                    }
                    lengthCodec.encode(buf, valueLength);
                    buf.position(endPos);
                }
            } catch (Exception e) {
                throw new CodecException(format("Failed to encode %s", def), e);
            }
        }

        if (tempMap.size() > 0) {
            throw new CodecException(format("Unexpected components %s while encoding %s", tempMap, this));
        }
    }

    @Override
    public Encoding getEncoding() {
        return Encoding.BINARY;
    }

    public SortedMap<Integer, ComponentDef> getSubComponentDefs() {
        return subComponentDefs;
    }

    public BitmapCodec getBitmapCodec() {
        return bitmapCodec;
    }

    @Override
    public int hashCode() {
        return Hash.build(this, subComponentDefs, bitmapCodec, super.hashCode());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (o == this) {
            return true;
        } else if (o.getClass() != getClass()) {
            return false;
        } else {
            CompositeDef other = (CompositeDef) o;
            return EqualsBuilder.newInstance(other.subComponentDefs, subComponentDefs)
                    .append(other.bitmapCodec, bitmapCodec).isEqual() && super.equals(o);
        }
    }
}
