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

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 * 
 */
@SuppressWarnings("rawtypes")
public class ComponentDef {

    private ComponentDef parent;
    private final Integer index;
    private final Codec codec;
    private final boolean mandatory;

    public ComponentDef(Integer index, Codec codec, boolean mandatory) {
        this.index = index;
        this.codec = codec;
        this.mandatory = mandatory;
    }

    public Codec getCodec() {
        return codec;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public static enum Encoding {
        CHAR, BCD, BINARY
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (parent != null) {
            sb.append(parent.toString()).append('.');
        }
        sb.append(index);
        return sb.toString();
    }

    public void setParent(ComponentDef parent) {
        this.parent = parent;
    }

}
