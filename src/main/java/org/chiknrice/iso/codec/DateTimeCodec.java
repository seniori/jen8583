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
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.chiknrice.iso.config.ComponentDef.Encoding;
import org.chiknrice.iso.util.Bcd;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 * 
 */
public class DateTimeCodec implements Codec<Date> {

    private final String dateTimePattern;
    private final TimeZone timeZone;
    private final Encoding encoding;

    public DateTimeCodec(String dateTimePattern, TimeZone timeZone, Encoding encoding) {
        this.dateTimePattern = dateTimePattern;
        this.timeZone = timeZone;
        this.encoding = encoding;
    }

    public Date decode(ByteBuffer buf) {
        int length = dateTimePattern.length();
        byte[] bytes = new byte[Encoding.BCD == encoding ? (length / 2 + length % 2) : length];
        buf.get(bytes);
        String dateTimeString;
        switch (encoding) {
        case CHAR:
            dateTimeString = new String(bytes, StandardCharsets.ISO_8859_1);
            break;
        case BCD:
            dateTimeString = Bcd.decode(bytes);
            break;
        default:
            throw new RuntimeException(String.format("Unsupported encoding %s", encoding));
        }

        SimpleDateFormat format = new SimpleDateFormat(dateTimePattern);
        format.setLenient(false);
        format.setTimeZone(timeZone);
        try {
            return format.parse(dateTimeString);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public void encode(ByteBuffer buf, Date value) {
        SimpleDateFormat format = new SimpleDateFormat(dateTimePattern);
        format.setLenient(false);
        format.setTimeZone(timeZone);
        String stringValue = format.format(value);
        switch (encoding) {
        case CHAR:
            buf.put(stringValue.getBytes(StandardCharsets.ISO_8859_1));
            break;
        case BCD:
            buf.put(Bcd.encode(stringValue));
            break;
        default:
            throw new RuntimeException(String.format("Unsupported encoding %s", encoding));
        }
    }

    @Override
    public Encoding getEncoding() {
        return encoding;
    }

    @Override
    public DateTimeCodec clone() throws CloneNotSupportedException {
        return new DateTimeCodec(dateTimePattern, timeZone, encoding);
    }

}
