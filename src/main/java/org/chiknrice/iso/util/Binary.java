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
package org.chiknrice.iso.util;

import java.nio.ByteBuffer;

import org.chiknrice.iso.CodecException;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 *
 */
public class Binary {

    public static Long decodeLong(byte[] bytes) {
        return wrap(bytes, 8).getLong();
    }

    public static Integer decodeInt(byte[] bytes) {
        return wrap(bytes, 4).getInt();
    }

    private static ByteBuffer wrap(byte[] bytes, int maxBytes) {
        ByteBuffer buf = ByteBuffer.allocate(maxBytes);
        if (bytes.length < maxBytes) {
            buf.position(maxBytes - bytes.length);
        }
        buf.put(bytes);
        buf.clear();
        return buf;
    }

    public static byte[] encode(Long value) {
        return encode(value, 8);
    }

    public static byte[] encode(Long value, int maxBytes) {
        return encode((Number) value, maxBytes);
    }

    public static byte[] encode(Integer value) {
        return encode(value, 4);
    }

    public static byte[] encode(Integer value, int maxBytes) {
        return encode((Number) value, maxBytes);
    }

    private static byte[] encode(Number value, int maxBytes) {
        int size;
        if (value instanceof Long) {
            size = 8;
        } else {
            size = 4;
        }
        ByteBuffer buf = ByteBuffer.allocate(size);
        if (value instanceof Long) {
            buf.putLong((Long) value);
        } else {
            buf.putInt((Integer) value);
        }

        buf.flip();
        byte[] bytes = buf.array();
        if (maxBytes < bytes.length) {
            byte[] trimmed = new byte[maxBytes];
            System.arraycopy(bytes, bytes.length - maxBytes, trimmed, 0, trimmed.length);

            // verify no data trimmed
            buf = ByteBuffer.allocate(size);
            buf.put(bytes, 0, bytes.length - maxBytes);
            buf.clear();
            if (((size == 8) ? buf.getLong() : buf.getInt()) > 0) {
                throw new CodecException(String.format("%d trimmed on encoding to %d bytes", value, maxBytes));
            }

            // use trimmed
            bytes = trimmed;
        }
        return bytes;
    }

}
