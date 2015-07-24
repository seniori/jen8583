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
import org.chiknrice.iso.config.ComponentDef;
import org.chiknrice.iso.util.EqualsBuilder;
import org.chiknrice.iso.util.Hash;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static java.lang.String.format;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 */
@SuppressWarnings("unchecked")
public class FlexiCompositeCodec implements CompositeCodec {

    private final BitmapCodec bitmapCodec;

    public FlexiCompositeCodec() {
        this(null);
    }

    public FlexiCompositeCodec(BitmapCodec bitmapCodec) {
        this.bitmapCodec = bitmapCodec;
    }


    @Override
    public Map<Integer, Object> decode(ByteBuffer buf, SortedMap<Integer, ComponentDef> subComponentDefs) {
        BitmapCodec.Bitmap bitmap;
        try {
            bitmap = bitmapCodec != null ? bitmapCodec.decode(buf) : null;
        } catch (Exception e) {
            throw new CodecException("Failed to decode bitmap", e);
        }

        Map<Integer, Object> values = new TreeMap<>();

        Integer index = 1;
        while (buf.hasRemaining()) {
            ComponentDef def = subComponentDefs.get(index);

            if (def == null) {
                if (bitmap != null && bitmap.isSet(index)) {
                    throw new CodecException(format("Missing configuration for %d", index));
                } else {
                    continue;
                }
            }

            Object value = def.getCodec().decode(buf);

            if (value == null && def.isMandatory()) {
                throw new CodecException(format("Missing mandatory component %s", def));
            }

            values.put(index, value);

            index++;
        }

        return values;
    }

    @Override
    public void encode(ByteBuffer buf, Map<Integer, Object> values, SortedMap<Integer, ComponentDef> subComponentDefs) {
        if (bitmapCodec != null) {
            try {
                bitmapCodec.encode(buf, values.keySet());
            } catch (Exception e) {
                throw new CodecException(format("Failed to encode bitmap for %s", this), e);
            }
        }
        Map<Integer, Object> toEncodeMap = new TreeMap<>(values);
        for (Map.Entry<Integer, ComponentDef> defEntry : subComponentDefs.entrySet()) {
            Integer index = defEntry.getKey();
            ComponentDef def = defEntry.getValue();
            Object value = toEncodeMap.remove(index);

            if (value == null) {
                if (def.isMandatory()) {
                    throw new CodecException(format("Missing mandatory component %s", def));
                } else {
                    continue;
                }
            }

            try {
                def.getCodec().encode(buf, value);
            } catch (Exception e) {
                throw new CodecException(format("Failed to encode %s", def), e);
            }
        }

        if (toEncodeMap.size() > 0) {
            throw new CodecException(format("Unexpected component(s) to encode %s", toEncodeMap));
        }
    }

    @Override
    public int hashCode() {
        return Hash.build(this, bitmapCodec);
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
            FlexiCompositeCodec other = (FlexiCompositeCodec) o;
            return EqualsBuilder.newInstance(other.bitmapCodec, bitmapCodec).isEqual();
        }
    }
}
