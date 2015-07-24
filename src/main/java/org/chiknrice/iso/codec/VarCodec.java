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

import org.chiknrice.iso.config.ComponentDef;

import java.nio.ByteBuffer;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 */
public class VarCodec<T> implements Codec<T> {

    private final Codec<Number> lengthCodec;
    private final Codec<T> codec;

    public VarCodec(Codec<Number> lengthCodec, Codec<T> codec) {
        this.lengthCodec = lengthCodec;
        this.codec = codec;
    }

    @Override
    public T decode(ByteBuffer buf) {
        ByteBuffer valueBuf;
        if (lengthCodec != null) {
            int varLength = lengthCodec.decode(buf).intValue();
            int limit = codec.getEncoding() == ComponentDef.Encoding.BCD ? varLength / 2 + varLength % 2 : varLength;
            valueBuf = buf.slice();
            valueBuf.limit(limit);
            buf.position(buf.position() + limit);
        } else {
            valueBuf = buf;
        }

        return codec.decode(valueBuf);
    }

    @Override
    public void encode(ByteBuffer buf, T value) {
        ByteBuffer valueBuf;
        if (lengthCodec != null) {
            buf.mark();
            // this is just to consume part of the buffer which would later be filled with correct length data
            lengthCodec.encode(buf, 0);
            valueBuf = buf.slice();
        } else {
            valueBuf = buf;
        }

        codec.encode(valueBuf, value);

        if (lengthCodec != null) {
            int endPos = buf.position() + valueBuf.position();
            buf.reset();
            int valueLength;
            if (ComponentDef.Encoding.BINARY == codec.getEncoding()) {
                valueLength = valueBuf.position();
            } else {
                valueLength = value.toString().length();
            }
            lengthCodec.encode(buf, valueLength);
            buf.position(endPos);
        }
    }

    @Override
    public ComponentDef.Encoding getEncoding() {
        return codec.getEncoding();
    }
}
