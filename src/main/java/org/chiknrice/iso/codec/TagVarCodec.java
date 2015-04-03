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

import org.chiknrice.iso.util.EqualsBuilder;
import org.chiknrice.iso.util.Hash;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 * 
 */
public class TagVarCodec<T> extends VarCodec<T> {

    private final Codec<Number> tagCodec;

    public TagVarCodec(Codec<T> codec, Codec<Number> lengthCodec, Codec<Number> tagCodec) {
        super(codec, lengthCodec);
        this.tagCodec = tagCodec;
    }

    public Codec<Number> getTagCodec() {
        return tagCodec;
    }

    @Override
    public int hashCode() {
        return Hash.build(this, tagCodec) ^ super.hashCode();
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
            TagVarCodec<?> other = (TagVarCodec<?>) o;
            return EqualsBuilder.newInstance(other.tagCodec, tagCodec).isEqual() && super.equals(other);
        }
    }

}
