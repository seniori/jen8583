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
import org.chiknrice.iso.util.Bcd;
import org.chiknrice.iso.util.Binary;
import org.chiknrice.iso.util.EqualsBuilder;
import org.chiknrice.iso.util.Hash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.*;

import static java.lang.String.format;
import static org.chiknrice.iso.config.ComponentDef.Encoding;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 */
@SuppressWarnings("unchecked")
public class TlvCompositeCodec implements CompositeCodec {

    private static final Logger LOG = LoggerFactory.getLogger(TlvCompositeCodec.class);

    private final Encoding tagEncoding;
    private final Encoding lengthEncoding;
    private final boolean failFast;

    public TlvCompositeCodec(Encoding tagEncoding, Encoding lengthEncoding, boolean failFast) {
        this.tagEncoding = tagEncoding;
        this.lengthEncoding = lengthEncoding;
        this.failFast = failFast;
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
                value = codec.decode(valueBuf);
            }

            if (value == null) {
                if (def.isMandatory()) {
                    if (failFast) {
                        throw new CodecException(format("Missing mandatory component %s", def));
                    } else {
                        LOG.warn("Missing mandatory component {}", def);
                    }
                } else {
                    continue;
                }
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
            if (failFast) {
                throw new CodecException(format("Missing mandatory tags %s", missingTags));
            } else {
                LOG.warn("Missing mandatory tags {}", missingTags);
            }
        }

        return values;
    }

    // TODO: properly decode using BER
    private Integer decodeTag(ByteBuffer buf) {
        if (tagEncoding == Encoding.BCD) {
            byte[] bcdBytes = new byte[1];
            buf.get(bcdBytes);
            return Integer.valueOf(Bcd.decode(bcdBytes));
        } else {
            return 0xFF & buf.get();
        }
    }

    // TODO: properly decode using BER
    private Integer decodeLength(ByteBuffer buf) {
        if (lengthEncoding == Encoding.BCD) {
            byte[] bcdBytes = new byte[1];
            buf.get(bcdBytes);
            return Integer.valueOf(Bcd.decode(bcdBytes));
        } else {
            byte[] bytes = new byte[2];
            buf.get(bytes);
            return Binary.decodeInt(bytes);
        }
    }

    @Override
    public void encode(ByteBuffer buf, Map<Integer, Object> values, SortedMap<Integer, ComponentDef> subComponentDefs) {
        Map<Integer, Object> toEncodeMap = new TreeMap<>(values);
        for (Map.Entry<Integer, ComponentDef> defEntry : subComponentDefs.entrySet()) {
            Integer tag = defEntry.getKey();
            ComponentDef def = defEntry.getValue();
            Object value = toEncodeMap.remove(tag);

            // TODO: allow encoding of 0 length TLV?
            if (value == null) {
                if (def.isMandatory()) {
                    if (failFast) {
                        throw new CodecException(format("Missing mandatory component %s", def));
                    } else {
                        LOG.warn("Missing mandatory component {}", def);
                    }
                }
                continue;
            }

            encodeTag(buf, tag);

            // TODO: use a ByteBuffer pool
            ByteBuffer valueBuf = ByteBuffer.allocate(0x7FFF);
            def.getCodec().encode(valueBuf, value);

            encodeLength(buf, valueBuf.position());

            buf.put(valueBuf.array(), 0, valueBuf.position());
        }

        if (toEncodeMap.size() > 0) {
            if (failFast) {
                throw new CodecException(format("Unexpected component(s) to encode %s", toEncodeMap));
            } else {
                LOG.warn("Unexpected component(s) to encode {}", toEncodeMap);
            }
        }
    }

    // TODO: properly encode using BER
    private void encodeTag(ByteBuffer buf, Integer tag) {
        if (tagEncoding == Encoding.BCD) {
            buf.put(Bcd.encode(tag.toString()));
        } else {
            buf.put(tag.byteValue());
        }
    }

    // TODO: properly encode using BER
    private void encodeLength(ByteBuffer buf, Integer length) {
        if (lengthEncoding == Encoding.BCD) {
            buf.put(Bcd.encode(length.toString()));
        } else {
            // for now 2 bytes - how we make this configurable for GB?
            buf.put(Binary.encode(length, 2));
        }
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
                    .append(other.lengthEncoding, lengthEncoding).append(other.failFast, failFast).isEqual();
        }
    }

}
