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

import org.chiknrice.iso.CodecException;
import org.chiknrice.iso.config.ComponentDef;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static java.lang.String.format;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 */
@SuppressWarnings("unchecked")
public class FixedCompositeCodec implements CompositeCodec {

    @Override
    public Map<Integer, Object> decode(ByteBuffer buf, SortedMap<Integer, ComponentDef> subComponentDefs) {
        Map<Integer, Object> values = new TreeMap<>();
        Integer index = 1;
        while (true) {
            ComponentDef def = subComponentDefs.get(index);

            if (def == null) {
                if (subComponentDefs.lastKey() > index) {
                    throw new CodecException(format("Missing configuration for sub component %d", index));
                } else {
                    break;
                }
            }

            Object value;
            try {
                value = def.getCodec().decode(buf);
            } catch (CodecException e) {
                throw new CodecException(format("Failed to decode %s", def), e);
            }

            if (value == null) {
                throw new CodecException(format("Null component %s", def));
            }

            values.put(index, value);
            index++;
        }

        return values;
    }

    @Override
    public void encode(ByteBuffer buf, Map<Integer, Object> values, SortedMap<Integer, ComponentDef> subComponentDefs) {
        Map<Integer, Object> toEncodeMap = new HashMap<>(values);
        for (Map.Entry<Integer, ComponentDef> defEntry : subComponentDefs.entrySet()) {
            Integer index = defEntry.getKey();
            ComponentDef def = defEntry.getValue();
            Object value = toEncodeMap.remove(index);

            if (value == null) {
                throw new CodecException(format("Missing mandatory component %s", def));
            }

            try {
                def.getCodec().encode(buf, value);
            } catch (Exception e) {
                throw new CodecException(format("Failed to encode %s", def), e);
            }
        }

        if (toEncodeMap.size() > 0) {
            throw new CodecException(format("Unexpected component(s) to encode %s", toEncodeMap));
        }
    }

    @Override
    public int hashCode() {
        return this.getClass().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else return o == this || o.getClass() == getClass();
    }
}
