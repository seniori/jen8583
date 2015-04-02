/**
 * 
 */
package org.chiknrice.iso;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.chiknrice.iso.codec.AlphaCodec;
import org.chiknrice.iso.codec.NumericCodec;
import org.chiknrice.iso.codec.TagVarCodec;
import org.chiknrice.iso.config.ComponentDef.Encoding;
import org.junit.Test;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 *
 */
public class TagVarCodecTest {

    @Test
    public void testGetCodec() {
        NumericCodec tagCodec = new NumericCodec(Encoding.BCD, 3);
        TagVarCodec<String> codec = new TagVarCodec<>(new AlphaCodec(true), new NumericCodec(Encoding.BCD, 3),
                tagCodec);
        assertEquals(tagCodec, codec.getTagCodec());
    }

    @Test
    public void testEqualsAndHashCode() {
        TagVarCodec<String> codec1 = new TagVarCodec<>(new AlphaCodec(true), new NumericCodec(Encoding.BCD, 3),
                new NumericCodec(Encoding.BCD, 3));
        TagVarCodec<String> codec2 = new TagVarCodec<>(new AlphaCodec(true), new NumericCodec(Encoding.BCD, 3),
                new NumericCodec(Encoding.BCD, 3));
        TagVarCodec<String> codec3 = new TagVarCodec<>(new AlphaCodec(false), new NumericCodec(Encoding.BCD, 4),
                new NumericCodec(Encoding.BCD, 3));
        TagVarCodec<String> codec4 = new TagVarCodec<>(new AlphaCodec(true), new NumericCodec(Encoding.BCD, 3),
                new NumericCodec(Encoding.BCD, 4));
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
