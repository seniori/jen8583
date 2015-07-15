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

package org.chiknrice.iso;

import org.chiknrice.iso.codec.AlphaCodec;
import org.chiknrice.iso.codec.BitmapCodec;
import org.chiknrice.iso.codec.CompositeCodec;
import org.chiknrice.iso.codec.NumericCodec;
import org.chiknrice.iso.config.ComponentDef;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.chiknrice.iso.codec.BitmapCodec.Bitmap.Type;
import static org.chiknrice.iso.config.ComponentDef.Encoding;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 */
public class CompositeCodecTest {

    @Test(expected = ConfigException.class)
    public void testBitmapNotAllowedWithSubfield1() {
        SortedMap<Integer, ComponentDef> subFieldsDef = new TreeMap<>();
        subFieldsDef.put(1, new ComponentDef(new NumericCodec(Encoding.BCD), false));
        new CompositeCodec(subFieldsDef, new BitmapCodec(Type.BINARY));
    }

    @Test
    public void testEncodeNonCompositeSubFields() {
        SortedMap<Integer, ComponentDef> subFieldsDef = new TreeMap<>();
        subFieldsDef.put(1, new ComponentDef(new AlphaCodec(false), false));
        subFieldsDef.put(2, new ComponentDef(new AlphaCodec(false), false));
        CompositeCodec codec = new CompositeCodec(subFieldsDef);

        ByteBuffer buf = ByteBuffer.allocate(64);
        Map<Integer, Object> values = new HashMap<>();
        values.put(1, "1");
        values.put(2, "2");

        codec.encode(buf, values);

        assertThat(buf.position(), is(2));

        byte[] result = new byte[2];
        buf.clear();
        buf.get(result);

        byte[] expected = "12".getBytes(StandardCharsets.ISO_8859_1);

        assertThat(result, is(expected));
    }

}
