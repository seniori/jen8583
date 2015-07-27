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
import org.chiknrice.iso.codec.CompositeCodec;
import org.chiknrice.iso.codec.VarCodec;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Encode:
 *
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 */
public class CompositeDefTest {

    @Test
    public void testInstanceConstruction() {
        SortedMap<Integer, ComponentDef> subComponentDefs = new TreeMap<>();
        subComponentDefs.put(1, mock(ComponentDef.class));
        SortedMap<Integer, ComponentDef> subComponentDefs2 = new TreeMap<>();
        subComponentDefs2.put(1, mock(ComponentDef.class));

        CompositeCodec compositeCodec = mock(CompositeCodec.class);
        CompositeCodec compositeCodec2 = mock(CompositeCodec.class);

        CompositeDef compositeDef = new CompositeDef(subComponentDefs, compositeCodec, true);

        assertThat(compositeDef.getSubComponentDefs(), is(subComponentDefs));
        assertThat(compositeDef.getSubComponentDefs(), is(not(subComponentDefs2)));

        assertThat(compositeDef.getCompositeCodec(), is(compositeCodec));
        assertThat(compositeDef.getCompositeCodec(), is(not(compositeCodec2)));
        assertThat(compositeDef.isMandatory(), is(true));

        assertThat(compositeDef.getCodec(), is(notNullValue()));
        assertThat(compositeDef.getCodec(), is(not(instanceOf(VarCodec.class))));

        Codec<Number> lengthCodec = mock(Codec.class);

        CompositeDef compositeDef2 = new CompositeDef(subComponentDefs2, compositeCodec2, false, lengthCodec);

        assertThat(compositeDef2.getSubComponentDefs(), is(subComponentDefs2));
        assertThat(compositeDef2.getSubComponentDefs(), is(not(subComponentDefs)));

        assertThat(compositeDef2.getCompositeCodec(), is(compositeCodec2));
        assertThat(compositeDef2.getCompositeCodec(), is(not(compositeCodec)));
        assertThat(compositeDef2.isMandatory(), is(false));
        assertThat(compositeDef2.getLengthCodec(), is(lengthCodec));

        assertThat(compositeDef2.getCodec(), is(notNullValue()));
        assertThat(compositeDef2.getCodec(), is(instanceOf(VarCodec.class)));
    }

    @Test
    public void testVarCompositeDef() {
        SortedMap<Integer, ComponentDef> subComponentDefs = new TreeMap<>();
        subComponentDefs.put(1, mock(ComponentDef.class));

        CompositeCodec compositeCodec = mock(CompositeCodec.class);

        ByteBuffer buf = ByteBuffer.wrap(new byte[]{0x31, 0x32});

        Codec<Number> lengthCodec = mock(Codec.class);
        when(lengthCodec.decode(buf)).thenAnswer(new Answer<Number>() {
            @Override
            public Number answer(InvocationOnMock invocation) throws Throwable {
                invocation.getArgumentAt(0, ByteBuffer.class).get();
                return 1;
            }
        });

        when(lengthCodec.decode(buf)).thenReturn(1);

        CompositeDef compositeDef = new CompositeDef(subComponentDefs, compositeCodec, true, lengthCodec);

        compositeDef.getCodec().decode(buf);

        ArgumentCaptor<SortedMap> arg = ArgumentCaptor.forClass(SortedMap.class);
        verify(compositeCodec).decode(any(ByteBuffer.class), arg.capture());
        assertThat(arg.getValue(), CoreMatchers.<SortedMap>is(subComponentDefs));

        Map<Integer, Object> toEncode = new HashMap<>();
        toEncode.put(1, "1");

        buf.clear();

        compositeDef.getCodec().encode(buf, toEncode);

        verify(compositeCodec).encode(buf, toEncode, subComponentDefs);
    }


    @Test
    public void testDecode() {
        SortedMap<Integer, ComponentDef> subComponentDefs = new TreeMap<>();

        ComponentDef componentDef1 = mock(ComponentDef.class);
        ComponentDef componentDef2 = mock(ComponentDef.class);
        ComponentDef componentDef3 = mock(ComponentDef.class);

        subComponentDefs.put(1, componentDef1);
        subComponentDefs.put(2, componentDef2);
        subComponentDefs.put(3, componentDef3);

        CompositeCodec compositeCodec = mock(CompositeCodec.class);

        Codec<Number> lengthCodec = mock(Codec.class);

        CompositeDef compositeDef = new CompositeDef(subComponentDefs, compositeCodec, true, lengthCodec);

        assertThat(compositeDef.getSubComponentDefs(), is(subComponentDefs));
        verify(componentDef1).setParent(compositeDef);
        verify(componentDef2).setParent(compositeDef);
        verify(componentDef3).setParent(compositeDef);
    }

    @Test
    public void testClone() {

    }

    @Test
    @SuppressWarnings({"EqualsBetweenInconvertibleTypes", "EqualsWithItself", "ObjectEqualsNull"})
    public void testEqualsAndHashCode() {
        SortedMap<Integer, ComponentDef> subComponentDefs = new TreeMap<>();
        subComponentDefs.put(1, mock(ComponentDef.class));
        CompositeCodec compositeCodec = mock(CompositeCodec.class);
        Codec<Number> lengthCodec = mock(Codec.class);
        CompositeDef compositeDef1 = new CompositeDef(subComponentDefs, compositeCodec, true, null);
        CompositeDef compositeDef2 = new CompositeDef(subComponentDefs, compositeCodec, true, null);
        CompositeDef compositeDef3 = new CompositeDef(subComponentDefs, compositeCodec, true, lengthCodec);
        assertTrue(!compositeDef1.equals(null));
        assertTrue(!compositeDef1.equals("a"));
        assertTrue(compositeDef1.equals(compositeDef1));
        assertTrue(compositeDef1.equals(compositeDef2));
        assertEquals(compositeDef1.hashCode(), compositeDef2.hashCode());
        assertTrue(!compositeDef1.equals(compositeDef3));
        assertNotEquals(compositeDef1.hashCode(), compositeDef3.hashCode());
        assertTrue(!compositeDef2.equals(compositeDef3));
        assertNotEquals(compositeDef2.hashCode(), compositeDef3.hashCode());
    }

}
