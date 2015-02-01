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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.chiknrice.iso.config.ComponentDef.Encoding;
import org.chiknrice.iso.util.EqualsBuilder;
import org.chiknrice.iso.util.Hash;

/**
 * A codec implementation for alphanumeric fields. It accepts a character which would be used to encode/decode the
 * string to and from byte[]. The character set used is either the one used for the specific field or the one set as the
 * default (both in the xml configuration).
 * 
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 * 
 */
public final class AlphaCodec implements Codec<String> {

    private final Charset charset;
    private final Boolean trim;
    private final Boolean leftJustified;
    private final Integer fixedLength;

    public AlphaCodec(Charset charset, Boolean trim) {
        this(charset, trim, null, null);
    }

    public AlphaCodec(Charset charset, Boolean trim, Boolean leftJustified, Integer fixedLength) {
        this.charset = charset;
        this.trim = trim;
        this.leftJustified = leftJustified;
        this.fixedLength = fixedLength;
    }

    public String decode(ByteBuffer buf) {
        byte[] bytes = new byte[fixedLength != null ? fixedLength : buf.limit() - buf.position()];
        buf.get(bytes);
        String value = new String(bytes, charset != null ? charset : StandardCharsets.ISO_8859_1);
        return trim ? value.trim() : value;
    }

    public void encode(ByteBuffer buf, String value) {
        String stringValue = value.toString();
        if (fixedLength != null) {
            stringValue = String.format("%" + (leftJustified ? "-" : "") + fixedLength + "s", stringValue);
        }
        buf.put(stringValue.getBytes(charset != null ? charset : StandardCharsets.ISO_8859_1));
    }

    @Override
    public Encoding getEncoding() {
        return Encoding.CHAR;
    }

    @Override
    public int hashCode() {
        return Hash.build(this, charset, trim, leftJustified, fixedLength);
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
            AlphaCodec other = (AlphaCodec) o;
            return EqualsBuilder.newInstance(other.charset, charset).append(other.trim, trim)
                    .append(other.leftJustified, leftJustified).append(other.fixedLength, fixedLength).isEqual();
        }
    }

}
