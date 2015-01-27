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

import org.chiknrice.iso.config.ComponentDef.Encoding;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 * 
 */
public final class CustomCodecAdapter implements Codec<Object> {

    private final CustomCodec customCodec;
    private final Integer fixedLength;

    public CustomCodecAdapter(CustomCodec customCodec) {
        this(customCodec, null);
    }

    public CustomCodecAdapter(CustomCodec customCodec, Integer fixedLength) {
        this.customCodec = customCodec;
        this.fixedLength = fixedLength;
    }

    public final Object decode(ByteBuffer buf) {
        byte[] bytes = new byte[fixedLength != null ? fixedLength : buf.limit() - buf.position()];
        buf.get(bytes);
        return customCodec.decode(bytes);
    }

    public final void encode(ByteBuffer buf, Object value) {
        byte[] bytes = customCodec.encode(value);
        buf.put(bytes);
    }

    @Override
    public Encoding getEncoding() {
        return Encoding.BINARY;
    }

    @Override
    public CustomCodecAdapter clone() throws CloneNotSupportedException {
        return new CustomCodecAdapter(customCodec.clone(), fixedLength);
    }

}
