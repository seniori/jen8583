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
package org.chiknrice.iso.codec;

import org.chiknrice.iso.CodecException;
import org.chiknrice.iso.ConfigException;
import org.chiknrice.iso.codec.BitmapCodec.Bitmap;
import org.chiknrice.iso.config.ComponentDef;
import org.chiknrice.iso.config.ComponentDef.Encoding;
import org.chiknrice.iso.util.EqualsBuilder;
import org.chiknrice.iso.util.Hash;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import static java.lang.String.format;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 */
public final class CompositeCodec implements Codec<Map<Integer, Object>> {

    private final SortedMap<Integer, ComponentDef> subComponentDefs;
    private final BitmapCodec bitmapCodec;

    public CompositeCodec(SortedMap<Integer, ComponentDef> subComponentDefs) {
        this(subComponentDefs, null);
    }

    public CompositeCodec(SortedMap<Integer, ComponentDef> subComponentDefs, BitmapCodec bitmapCodec) {
        this.subComponentDefs = subComponentDefs;
        this.bitmapCodec = bitmapCodec;
        if (bitmapCodec != null && subComponentDefs.get(1) != null) {
            throw new ConfigException("Composite components with bitmap cannot have subfield index 1");
        }
    }

    public Map<Integer, Object> decode(ByteBuffer buf) {
        Bitmap bitmap = decodeBitmap(buf);
        Map<Integer, Object> values = new TreeMap<>();
        for (Entry<Integer, ComponentDef> defEntry : getSubComponentDefs().entrySet()) {
            Integer index = defEntry.getKey();
            ComponentDef def = defEntry.getValue();

            if (bitmap != null && !bitmap.isSet(index)) {
                if (def.isMandatory()) {
                    throw new CodecException(format("Missing mandatory component %s", def));
                }
                continue;
            }

            if (def.getCodec() instanceof TagVarCodec) {
                // nothing to do with a tag yet
                Integer tag = ((TagVarCodec<?>) def.getCodec()).getTagCodec().decode(buf).intValue();
                if (!index.equals(tag)) {
                    throw new CodecException(format("Unexpected TLV tag %d", tag));
                }
            }

            ByteBuffer valueBuf;
            if (def.getCodec() instanceof VarCodec) {
                int varLength = ((VarCodec<?>) def.getCodec()).getLengthCodec().decode(buf).intValue();
                int limit = def.getCodec().getEncoding() == Encoding.BCD ? varLength / 2 + varLength % 2 : varLength;
                valueBuf = buf.slice();
                valueBuf.limit(limit);
                buf.position(buf.position() + limit);
            } else {
                valueBuf = buf;
            }

            Object value;
            try {
                value = def.getCodec().decode(valueBuf);
            } catch (Exception e) {
                if (e instanceof CodecException) {
                    throw e;
                } else {
                    throw new CodecException(format("Failed to decode %s", def), e);
                }
            }

            if (value == null && def.isMandatory()) {
                throw new CodecException(format("Null mandatory component %s", def));
            }

            values.put(index, value);
        }
        return values;
    }

    protected Bitmap decodeBitmap(ByteBuffer buf) {
        return getBitmapCodec() != null ? getBitmapCodec().decode(buf) : null;
    }

    @SuppressWarnings("unchecked")
    public void encode(ByteBuffer buf, Map<Integer, Object> values) {
        encodeBitmap(buf, values);
        Map<Integer, Object> tempMap = new HashMap<>(values);
        for (Entry<Integer, ComponentDef> defEntry : subComponentDefs.entrySet()) {
            Integer index = defEntry.getKey();
            ComponentDef def = defEntry.getValue();
            Object value = tempMap.remove(index);

            if (value == null) {
                if (def.isMandatory()) {
                    throw new CodecException(format("Missing mandatory component %s", def));
                } else {
                    continue;
                }
            }

            if (def.getCodec() instanceof TagVarCodec) {
                // nothing to do with a codec yet
                ((TagVarCodec<?>) def.getCodec()).getTagCodec().encode(buf, Long.valueOf(index));
            }

            ByteBuffer valueBuf;
            if (def.getCodec() instanceof VarCodec) {
                buf.mark();
                ((VarCodec<?>) def.getCodec()).getLengthCodec().encode(buf, 0);
                valueBuf = buf.slice();
            } else {
                valueBuf = buf;
            }

            try {
                def.getCodec().encode(valueBuf, value);
            } catch (Exception e) {
                if (e instanceof CodecException) {
                    throw e;
                } else {
                    throw new CodecException(format("Failed to encode %s", def), e);
                }
            }

            if (def.getCodec() instanceof VarCodec) {
                int endPos = buf.position() + valueBuf.position();
                buf.reset();
                int valueLength;
                if (Encoding.BINARY == def.getCodec().getEncoding()) {
                    valueLength = valueBuf.position();
                } else {
                    valueLength = value.toString().length();
                }
                ((VarCodec<?>) def.getCodec()).getLengthCodec().encode(buf, valueLength);
                buf.position(endPos);
            }
        }

        if (tempMap.size() > 0) {
            throw new CodecException(format("Unexpected components %s", tempMap));
        }
    }

    protected void encodeBitmap(ByteBuffer buf, Map<Integer, Object> values) {
        if (getBitmapCodec() != null) {
            getBitmapCodec().encode(buf, values.keySet());
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
        return Hash.build(this, subComponentDefs, bitmapCodec);
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
            CompositeCodec other = (CompositeCodec) o;
            return EqualsBuilder.newInstance(other.subComponentDefs, subComponentDefs)
                    .append(other.bitmapCodec, bitmapCodec).isEqual();
        }
    }

}
