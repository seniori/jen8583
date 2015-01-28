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
public class VarCodec<T> implements Codec<T> {

    private final Codec<T> codec;
    private final NumericCodec lengthCodec;

    public VarCodec(Codec<T> codec, NumericCodec lengthCodec) {
        this.codec = codec;
        this.lengthCodec = lengthCodec;
    }

    @Override
    public T decode(ByteBuffer buf) {
        return codec.decode(buf);
    }

    @Override
    public void encode(ByteBuffer buf, T value) {
        codec.encode(buf, value);
    }

    @Override
    public Encoding getEncoding() {
        return getCodec().getEncoding();
    }

    public Codec<T> getCodec() {
        return codec;
    }

    public NumericCodec getLengthCodec() {
        return lengthCodec;
    }

}
