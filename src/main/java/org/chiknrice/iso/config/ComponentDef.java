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

import org.chiknrice.iso.ConfigException;
import org.chiknrice.iso.codec.Codec;
import org.chiknrice.iso.util.EqualsBuilder;
import org.chiknrice.iso.util.Hash;

import java.util.Map;
import java.util.Map.Entry;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 */
@SuppressWarnings("rawtypes")
public class ComponentDef {

    private final Codec valueCodec;
    private final Codec<Number> lengthCodec;
    private final Codec<Number> tagCodec;
    private final boolean mandatory;

    protected CompositeDef parent;

    public ComponentDef(Codec valueCodec) {
        this(valueCodec, true);
    }

    public ComponentDef(Codec valueCodec, boolean mandatory) {
        this(null, null, valueCodec, mandatory);
    }

    public ComponentDef(Codec<Number> lengthCodec, Codec valueCodec) {
        this(lengthCodec, valueCodec, true);
    }

    public ComponentDef(Codec<Number> lengthCodec, Codec valueCodec, boolean mandatory) {
        this(null, lengthCodec, valueCodec, mandatory);
    }

    public ComponentDef(Codec<Number> tagCodec, Codec<Number> lengthCodec, Codec valueCodec) {
        this(tagCodec, lengthCodec, valueCodec, true);
    }

    public ComponentDef(Codec<Number> tagCodec, Codec<Number> lengthCodec, Codec valueCodec, boolean mandatory) {
        this.tagCodec = tagCodec;
        this.lengthCodec = lengthCodec;
        this.valueCodec = valueCodec;
        this.mandatory = mandatory;

        if (valueCodec instanceof CompositeDef) {
            throw new ConfigException(String
                    .format("%s shouldn't be used as valueCodec", CompositeDef.class.getSimpleName()));
        }
    }

    public Codec<Number> getTagCodec() {
        return tagCodec;
    }

    public Codec<Number> getLengthCodec() {
        return lengthCodec;
    }

    public Codec getValueCodec() {
        return valueCodec;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public enum Encoding {
        CHAR, BCD, BINARY
    }

    @Override
    public String toString() {
        if (parent != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(parent.toString());
            if (sb.length() > 0) {
                sb.append('.');
            }
            Map<Integer, ComponentDef> map;
            map = parent.getSubComponentDefs();
            for (Entry<Integer, ComponentDef> defEntry : map.entrySet()) {
                ComponentDef def = defEntry.getValue();
                if (def == this) {
                    sb.append(defEntry.getKey());
                    break;
                }
            }
            return sb.toString();
        } else {
            return "";
        }
    }

    @Override
    public int hashCode() {
        return Hash.build(this, valueCodec, mandatory);
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
            ComponentDef other = (ComponentDef) o;
            return EqualsBuilder.newInstance(other.tagCodec, tagCodec).append(other.lengthCodec, lengthCodec)
                    .append(other.valueCodec, valueCodec).append(other.mandatory, mandatory).isEqual();
        }
    }

}
