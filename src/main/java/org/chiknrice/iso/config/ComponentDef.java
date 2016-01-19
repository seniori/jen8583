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

import java.util.Map;
import java.util.Map.Entry;

import org.chiknrice.iso.codec.Codec;
import org.chiknrice.iso.codec.CompositeCodec;
import org.chiknrice.iso.codec.VarCodec;
import org.chiknrice.iso.util.EqualsBuilder;
import org.chiknrice.iso.util.Hash;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 * 
 */
@SuppressWarnings("rawtypes")
public class ComponentDef {

    private ComponentDef parent;
    private final Codec codec;
    private final boolean mandatory;

    public ComponentDef(Codec codec, boolean mandatory) {
        this.codec = codec;
        this.mandatory = mandatory;
        CompositeCodec composite = null;
        if (codec instanceof VarCodec) {
            if (((VarCodec) codec).getCodec() instanceof CompositeCodec) {
                composite = (CompositeCodec) ((VarCodec) codec).getCodec();
            }
        } else if (codec instanceof CompositeCodec) {
            composite = (CompositeCodec) codec;
        }
        if (composite != null) {
            for (ComponentDef child : composite.getSubComponentDefs().values()) {
                child.parent = this;
            }
        }
    }

    public Codec getCodec() {
        return codec;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public static enum Encoding {
        CHAR, BCD, BINARY, BCDF
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
            if (parent.getCodec() instanceof VarCodec) {
                map = ((CompositeCodec) ((VarCodec) parent.getCodec()).getCodec()).getSubComponentDefs();
            } else {
                map = ((CompositeCodec) parent.getCodec()).getSubComponentDefs();
            }
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
        return Hash.build(this, codec, mandatory);
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
            return EqualsBuilder.newInstance(other.codec, codec).append(other.mandatory, mandatory).isEqual();
        }
    }

}
