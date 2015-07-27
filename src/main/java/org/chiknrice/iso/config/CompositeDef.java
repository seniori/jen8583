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

package org.chiknrice.iso.config;

import org.chiknrice.iso.codec.Codec;
import org.chiknrice.iso.codec.CompositeCodec;
import org.chiknrice.iso.codec.VarCodec;
import org.chiknrice.iso.util.EqualsBuilder;
import org.chiknrice.iso.util.Hash;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.SortedMap;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 */
public class CompositeDef extends ComponentDef {

    private final SortedMap<Integer, ComponentDef> subComponentDefs;
    private final CompositeCodec compositeCodec;
    private final Codec<Number> lengthCodec;

    private final Codec<Map<Integer, Object>> codec;

    public CompositeDef(final SortedMap<Integer, ComponentDef> subComponentDefs, final CompositeCodec compositeCodec, final boolean mandatory) {
        this(subComponentDefs, compositeCodec, mandatory, null);
    }

    public CompositeDef(final SortedMap<Integer, ComponentDef> subComponentDefs, final CompositeCodec compositeCodec, final boolean mandatory, final Codec<Number> lengthCodec) {
        super(null, mandatory);

        this.subComponentDefs = subComponentDefs;
        this.compositeCodec = compositeCodec;
        this.lengthCodec = lengthCodec;

        Codec<Map<Integer, Object>> codec = new Codec<Map<Integer, Object>>() {
            @Override
            public Map<Integer, Object> decode(ByteBuffer buf) {
                return getCompositeCodec().decode(buf, getSubComponentDefs());
            }

            @Override
            public void encode(ByteBuffer buf, Map<Integer, Object> value) {
                getCompositeCodec().encode(buf, value, getSubComponentDefs());
            }

            @Override
            public Encoding getEncoding() {
                return Encoding.BINARY;
            }
        };

        if (lengthCodec != null) {
            codec = new VarCodec<>(lengthCodec, codec);
        }

        this.codec = codec;

        for (ComponentDef subComponentDef : subComponentDefs.values()) {
            subComponentDef.setParent(this);
        }
    }

    CompositeCodec getCompositeCodec() {
        return compositeCodec;
    }

    Codec<Number> getLengthCodec() {
        return lengthCodec;
    }

    public SortedMap<Integer, ComponentDef> getSubComponentDefs() {
        return subComponentDefs;
    }

    @Override
    public Codec<Map<Integer, Object>> getCodec() {
        return codec;
    }

    @Override
    public int hashCode() {
        return Hash.build(this, subComponentDefs, compositeCodec, lengthCodec, isMandatory(), super.hashCode());
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
            CompositeDef other = (CompositeDef) o;
            return EqualsBuilder.newInstance(other.subComponentDefs, subComponentDefs)
                    .append(other.compositeCodec, compositeCodec).append(other.lengthCodec, lengthCodec)
                    .append(other.isMandatory(), isMandatory()).isEqual();
        }
    }

}
