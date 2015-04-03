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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;

import org.chiknrice.iso.codec.Codec;
import org.chiknrice.iso.codec.TagVarCodec;
import org.chiknrice.iso.codec.VarCodec;
import org.chiknrice.iso.config.ComponentDef.Encoding;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class TagVarCodecTest {

    @Mock
    private Codec<String> dataCodec;

    @Mock
    private Codec<Number> tagCodec;

    @Mock
    private Codec<Number> tagCodecDiff;

    @Mock
    private Codec<Number> lengthCodec;

    @Mock
    private Codec<Number> lengthCodecDiff;

    @Mock
    private ByteBuffer buf;

    @Test
    public void testCodecDelegation() {
        when(dataCodec.getEncoding()).thenReturn(Encoding.CHAR);
        TagVarCodec<String> codec = new TagVarCodec<>(dataCodec, lengthCodec, tagCodec);
        codec.decode(buf);
        verify(dataCodec).decode(buf);
        codec.encode(buf, "a");
        verify(dataCodec).encode(buf, "a");

        assertEquals(dataCodec, codec.getCodec());
        assertEquals(Encoding.CHAR, codec.getEncoding());
        assertEquals(tagCodec, codec.getTagCodec());
        assertEquals(lengthCodec, codec.getLengthCodec());
    }

    @Test
    public void testEqualsAndHashCodeVarCodec() {
        VarCodec<String> codec1 = new VarCodec<>(dataCodec, lengthCodec);
        VarCodec<String> codec2 = new VarCodec<>(dataCodec, lengthCodec);
        VarCodec<String> codec3 = new VarCodec<>(dataCodec, lengthCodecDiff);
        assertTrue(!codec1.equals(null));
        assertTrue(!codec1.equals("a"));
        assertTrue(codec1.equals(codec1));
        assertTrue(codec1.equals(codec2));
        assertEquals(codec1.hashCode(), codec2.hashCode());
        assertTrue(!codec1.equals(codec3));
        assertNotEquals(codec1.hashCode(), codec3.hashCode());
        assertTrue(!codec2.equals(codec3));
        assertNotEquals(codec2.hashCode(), codec3.hashCode());
    }

    @Test
    public void testEqualsAndHashCodeTagVarCodec() {
        when(lengthCodec.getEncoding()).thenReturn(Encoding.CHAR);
        when(lengthCodecDiff.getEncoding()).thenReturn(Encoding.BCD);
        when(tagCodec.getEncoding()).thenReturn(Encoding.CHAR);
        when(tagCodecDiff.getEncoding()).thenReturn(Encoding.BCD);

        TagVarCodec<String> codec1 = new TagVarCodec<>(dataCodec, lengthCodec, tagCodec);
        TagVarCodec<String> codec2 = new TagVarCodec<>(dataCodec, lengthCodec, tagCodec);
        TagVarCodec<String> codec3 = new TagVarCodec<>(dataCodec, lengthCodecDiff, tagCodec);
        TagVarCodec<String> codec4 = new TagVarCodec<>(dataCodec, lengthCodec, tagCodecDiff);
        assertTrue(!codec1.equals(null));
        assertTrue(!codec1.equals("a"));
        assertTrue(codec1.equals(codec1));
        assertTrue(codec1.equals(codec2));
        assertEquals(codec1.hashCode(), codec2.hashCode());
        assertTrue(!codec1.equals(codec3));
        assertNotEquals(codec1.hashCode(), codec3.hashCode());
        assertTrue(!codec2.equals(codec3));
        assertTrue(!codec2.equals(codec4));
        assertNotEquals(codec2.hashCode(), codec3.hashCode());
    }

}
