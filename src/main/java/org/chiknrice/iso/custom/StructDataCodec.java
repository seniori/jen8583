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
package org.chiknrice.iso.custom;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.chiknrice.iso.codec.Configurable;
import org.chiknrice.iso.codec.CustomCodec;

/**
 * An example custom codec. For a custom codec to participate in a message extension, it has to implement Clonable and
 * properly implement clone() method.
 * 
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 * 
 */
public class StructDataCodec implements CustomCodec, Configurable {

    private enum Segment {
        KEY_LENGTH_IND, KEY_LENGTH, KEY, VALUE_LENGTH_IND, VALUE_LENGTH, VALUE
    }

    @Override
    public Object decode(byte[] bytes) {
        String text = new String(bytes, StandardCharsets.ISO_8859_1);
        StringReader reader = new StringReader(text);

        Segment segment = Segment.KEY_LENGTH_IND;
        int lengthToRead = 1;
        char[] buff = new char[512];
        int lengthRead = -1;

        String key = null;
        String val = null;

        Map<String, String> map = new LinkedHashMap<>();

        try {
            while ((lengthRead = reader.read(buff, 0, lengthToRead)) != -1) {
                switch (segment) {
                case KEY_LENGTH_IND:
                    String lengthInd = new String(buff, 0, lengthRead);
                    lengthToRead = Integer.valueOf(lengthInd);
                    segment = Segment.KEY_LENGTH;
                    break;
                case KEY_LENGTH:
                    String keyLength = new String(buff, 0, lengthRead);
                    lengthToRead = Integer.valueOf(keyLength);
                    segment = Segment.KEY;
                    break;
                case KEY:
                    key = new String(buff, 0, lengthRead);
                    lengthToRead = 1;
                    segment = Segment.VALUE_LENGTH_IND;
                    break;
                case VALUE_LENGTH_IND:
                    lengthInd = new String(buff, 0, lengthRead);
                    lengthToRead = Integer.valueOf(lengthInd);
                    segment = Segment.VALUE_LENGTH;
                    break;
                case VALUE_LENGTH:
                    String valueLength = new String(buff, 0, lengthRead);
                    lengthToRead = Integer.valueOf(valueLength);
                    segment = Segment.VALUE;
                    break;
                case VALUE:
                    val = new String(buff, 0, lengthRead);
                    lengthToRead = 1;
                    segment = Segment.KEY_LENGTH_IND;
                    map.put(key, val);
                    break;
                default:
                    throw new RuntimeException("Unknown segment type: " + segment);
                }
            }
        } catch (NumberFormatException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return map;
    }

    @Override
    public byte[] encode(Object value) {
        @SuppressWarnings("unchecked")
        Map<String, String> map = (Map<String, String>) value;
        StringBuilder sb = new StringBuilder();
        for (Entry<String, String> kvPair : map.entrySet()) {
            String length = String.valueOf(kvPair.getKey().length());
            sb.append(length.length());
            sb.append(length);
            sb.append(kvPair.getKey());
            length = String.valueOf(kvPair.getValue().length());
            sb.append(length.length());
            sb.append(length);
            sb.append(kvPair.getValue());
        }
        return sb.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    private Map<String, String> params;

    @Override
    public void configure(Map<String, String> params) {
        this.params = params;
        // Configure codec here
    }

    public CustomCodec clone() throws CloneNotSupportedException {
        StructDataCodec codec = new StructDataCodec();
        codec.configure(params);
        return codec;
    }

}
