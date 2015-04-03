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
import org.chiknrice.iso.util.EqualsBuilder;
import org.chiknrice.iso.util.Hash;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 * 
 */
public class VarCodec<T> implements Codec<T> {

    private final Codec<T> codec;
    private final Codec<Number> lengthCodec;

    public VarCodec(Codec<T> codec, Codec<Number> lengthCodec) {
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

    public Codec<Number> getLengthCodec() {
        return lengthCodec;
    }

    @Override
    public int hashCode() {
        return Hash.build(this, codec, lengthCodec);
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
            VarCodec<?> other = (VarCodec<?>) o;
            return EqualsBuilder.newInstance(other.codec, codec).append(other.lengthCodec, lengthCodec).isEqual();
        }
    }

}
