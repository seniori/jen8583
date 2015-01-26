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

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.chiknrice.iso.codec.BitmapCodec.Bitmap;
import org.chiknrice.iso.config.ComponentDef;
import org.chiknrice.iso.config.ComponentDef.Encoding;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 * 
 */
public final class CompositeCodec implements Codec<Map<Integer, Object>> {

    private final Map<Integer, ComponentDef> subComponentDefs;
    private final BitmapCodec bitmapCodec;

    public CompositeCodec(Map<Integer, ComponentDef> subComponentDefs) {
        this(subComponentDefs, null);
    }

    public CompositeCodec(Map<Integer, ComponentDef> subComponentDefs, BitmapCodec bitmapCodec) {
        this.subComponentDefs = subComponentDefs;
        this.bitmapCodec = bitmapCodec;
    }

    public Map<Integer, Object> decode(ByteBuffer buf) {
        Bitmap bitmap = decodeBitmap(buf);
        Map<Integer, Object> values = new TreeMap<Integer, Object>();
        for (Entry<Integer, ComponentDef> defEntry : getSubComponentDefs().entrySet()) {
            Integer index = defEntry.getKey();
            ComponentDef def = defEntry.getValue();

            if (bitmap != null && !bitmap.isSet(index)) {
                if (def.isMandatory()) {
                    throw new CodecException(String.format("Missing mandatory component %s", def));
                }
                continue;
            }

            if (def.getCodec() instanceof TagVarCodec) {
                // nothing to do with a tag yet
                Integer tag = ((TagVarCodec<?>) def.getCodec()).decodeTag(buf);
                if (tag != index) {
                    throw new CodecException(String.format("Unexpected TLV tag %d", tag));
                }
            }

            ByteBuffer valueBuf;
            if (def.getCodec() instanceof VarCodec) {
                int varLength = ((VarCodec<?>) def.getCodec()).decodeLength(buf);
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
                    throw new CodecException(String.format("Failed to decode %s", def), e);
                }
            }

            if (value == null && def.isMandatory()) {
                throw new CodecException(String.format("Null mandatory component %s", def));
            }

            values.put(index, value);
        }
        return values;
    }

    protected Bitmap decodeBitmap(ByteBuffer buf) {
        return bitmapCodec != null ? bitmapCodec.decode(buf) : null;
    }

    @SuppressWarnings("unchecked")
    public void encode(ByteBuffer buf, Map<Integer, Object> values) {
        encodeBitmap(buf, values);
        for (Entry<Integer, Object> valueEntry : values.entrySet()) {
            Integer index = valueEntry.getKey();
            Object value = valueEntry.getValue();
            ComponentDef def = getSubComponentDefs().get(index);

            if (def == null) {
                throw new RuntimeException(String.format("No configuration for field %d", index));
            }

            if (def.getCodec() instanceof TagVarCodec) {
                // nothing to do with a codec yet
                ((TagVarCodec<?>) def.getCodec()).encodeTag(buf, index);
            }

            ByteBuffer valueBuf;
            if (def.getCodec() instanceof VarCodec) {
                buf.mark();
                ((VarCodec<?>) def.getCodec()).encodeLength(buf, 0);
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
                    throw new CodecException(String.format("Failed to encode %s", def), e);
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
                ((VarCodec<?>) def.getCodec()).encodeLength(buf, valueLength);
                buf.position(endPos);
            }
        }
    }

    protected void encodeBitmap(ByteBuffer buf, Map<Integer, Object> values) {
        if (bitmapCodec != null) {
            bitmapCodec.encode(buf, values.keySet());
        }
    }

    @Override
    public Encoding getEncoding() {
        return Encoding.BINARY;
    }

    public Map<Integer, ComponentDef> getSubComponentDefs() {
        return subComponentDefs;
    }

}
