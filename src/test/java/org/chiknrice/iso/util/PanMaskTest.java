package org.chiknrice.iso.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 */
public class PanMaskTest {

    @Test
    public void testMask() {
        PanMask mask = new PanMask();
        String pan = "1234568888881234";

        pan = mask.apply(pan);
        assertEquals("123456******1234", pan);
    }

}
