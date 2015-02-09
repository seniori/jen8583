/**
 * 
 */
package org.chiknrice.iso;

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.chiknrice.iso.codec.NumericCodec;
import org.chiknrice.iso.config.ComponentDef.Encoding;
import org.junit.Test;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 *
 */
public class TestNumericCodec extends BaseTest {
    
    @Test
    public void testEncodeChar() {
        NumericCodec codec = new NumericCodec(Encoding.CHAR);
        ByteBuffer buf = ByteBuffer.allocate(20);
        codec.encode(buf, 1234);
        String encoded = new String(buf.array(), 0, 4, StandardCharsets.ISO_8859_1);
        assertEquals("1234", encoded);
    }

}
