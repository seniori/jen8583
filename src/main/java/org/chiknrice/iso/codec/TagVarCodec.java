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

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 * 
 */
public class TagVarCodec<T> extends VarCodec<T> {

    private final NumericCodec tagCodec;

    public TagVarCodec(Codec<T> codec, NumericCodec lengthCodec) {
        this(codec, lengthCodec, null);
    }

    public TagVarCodec(Codec<T> codec, NumericCodec lengthCodec, NumericCodec tagCodec) {
        super(codec, lengthCodec);
        this.tagCodec = tagCodec;
    }

    private TagVarCodec(TagVarCodec<T> orig) throws CloneNotSupportedException {
        super((VarCodec<T>) orig);
        this.tagCodec = orig.tagCodec.clone();
    }

    public Integer decodeTag(ByteBuffer buf) {
        return tagCodec.decode(buf).intValue();
    }

    public void encodeTag(ByteBuffer buf, Integer tag) {
        tagCodec.encode(buf, Long.valueOf(tag));
    }

    @Override
    public TagVarCodec<T> clone() throws CloneNotSupportedException {
        return new TagVarCodec<T>(this);
    }

}
