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
import java.util.*;

import static java.lang.String.format;
import static org.chiknrice.iso.config.ComponentDef.Encoding;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 */
@SuppressWarnings("unchecked")
public class TlvCompositeCodec implements CompositeCodec {

    private final Encoding tagEncoding;
    private final Encoding lengthEncoding;

    public TlvCompositeCodec(Encoding tagEncoding, Encoding lengthEncoding) {
        this.tagEncoding = tagEncoding;
        this.lengthEncoding = lengthEncoding;
    }

    @Override
    public Map<Integer, Object> decode(ByteBuffer buf, SortedMap<Integer, ComponentDef> subComponentDefs) {
        Map<Integer, ComponentDef> possibleSubComponents = new TreeMap<>(subComponentDefs);
        Map<Integer, Object> values = new TreeMap<>();

        while (buf.hasRemaining()) {
            Integer tag = decodeTag(buf);
            ComponentDef def = possibleSubComponents.remove(tag);
            if (def == null) {
                throw new CodecException(format("Missing configuration for %d", tag));
            }

            Codec codec = def.getCodec();

            Integer length = decodeLength(buf);
            int limit = codec.getEncoding() == ComponentDef.Encoding.BCD ? length / 2 + length % 2 : length;
            ByteBuffer valueBuf = buf.slice();
            valueBuf.limit(limit);
            buf.position(buf.position() + limit);

            Object value = null;
            if (length > 0) {
                value = codec.decode(buf);
            }

            if (value == null && def.isMandatory()) {
                throw new CodecException(format("Got null value for %d tag", tag));
            }

            values.put(tag, value);
        }

        Set<Integer> missingTags = new HashSet<>();
        for (Map.Entry<Integer, ComponentDef> defEntry : possibleSubComponents.entrySet()) {
            if (defEntry.getValue().isMandatory()) {
                missingTags.add(defEntry.getKey());
            }
        }
        if (missingTags.size() > 0) {
            throw new CodecException(format("Missing mandatory tags %s", missingTags));
        }

        return values;
    }

    private Integer decodeLength(ByteBuffer buf) {
        // TODO: implement this
        throw new UnsupportedOperationException();
    }

    private Integer decodeTag(ByteBuffer buf) {
        // TODO: implement this
        throw new UnsupportedOperationException();
    }

    @Override
    public void encode(ByteBuffer buf, Map<Integer, Object> values, SortedMap<Integer, ComponentDef> subComponentDefs) {
        Map<Integer, Object> toEncodeMap = new TreeMap<>(values);
        for (Map.Entry<Integer, ComponentDef> defEntry : subComponentDefs.entrySet()) {
            Integer tag = defEntry.getKey();
            ComponentDef def = defEntry.getValue();
            Object value = toEncodeMap.remove(tag);

            if (value == null && def.isMandatory()) {
                throw new CodecException(format("Missing mandatory component %s", def));
            }

            encodeTag(buf, tag);

            // TODO: use a ByteBuffer pool
            ByteBuffer valueBuf = ByteBuffer.allocate(0x7FFF);
            def.getCodec().encode(valueBuf, value);

            // TODO: compute length based on value encoding
            encodeLength(buf, valueBuf.position());

            buf.put(valueBuf.array(), 0, valueBuf.position());
        }

        if (toEncodeMap.size() > 0) {
            throw new CodecException(format("Unexpected component(s) to encode %s", toEncodeMap));
        }
    }

    private void encodeTag(ByteBuffer buf, Integer tag) {
        // TODO: implement this
        throw new UnsupportedOperationException();
    }

    private void encodeLength(ByteBuffer buf, Integer length) {
        // TODO: implement this
        throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
        return Hash.build(this, tagEncoding, lengthEncoding);
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
            TlvCompositeCodec other = (TlvCompositeCodec) o;
            return EqualsBuilder.newInstance(other.tagEncoding, tagEncoding)
                    .append(other.lengthEncoding, lengthEncoding).isEqual();
        }
    }

}
