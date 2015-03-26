/**
 * 
 */
package org.chiknrice.iso;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.chiknrice.iso.codec.AlphaCodec;
import org.chiknrice.iso.codec.NumericCodec;
import org.chiknrice.iso.codec.TagVarCodec;
import org.chiknrice.iso.config.ComponentDef.Encoding;
import org.junit.Test;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 *
 */
public class TestEqualsHash {

    @Test
    public void testHash() {
        TagVarCodec<String> left = new TagVarCodec<>(new AlphaCodec(true), new NumericCodec(Encoding.BCD),
                new NumericCodec(Encoding.BCD));
        TagVarCodec<String> right = new TagVarCodec<>(new AlphaCodec(true), new NumericCodec(Encoding.BCD),
                new NumericCodec(Encoding.BCD));
        TagVarCodec<String> wrong = new TagVarCodec<>(new AlphaCodec(false), new NumericCodec(Encoding.BCD),
                new NumericCodec(Encoding.BCD));
        assertEquals(left.hashCode(), right.hashCode());
        assertNotEquals(left.hashCode(), wrong.hashCode());
    }

    @Test
    public void testEquals() {
        TagVarCodec<String> left = new TagVarCodec<>(new AlphaCodec(true), new NumericCodec(Encoding.BCD),
                new NumericCodec(Encoding.BCD));
        TagVarCodec<String> right = new TagVarCodec<>(new AlphaCodec(true), new NumericCodec(Encoding.BCD),
                new NumericCodec(Encoding.BCD));
        TagVarCodec<String> wrong = new TagVarCodec<>(new AlphaCodec(false), new NumericCodec(Encoding.BCD),
                new NumericCodec(Encoding.BCD));
        assertEquals(left, right);
        assertNotEquals(left, wrong);
    }

}
