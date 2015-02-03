/**
 * 
 */
package org.chiknrice.iso;

import org.chiknrice.iso.config.IsoMessageDef;
import org.junit.Test;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 *
 */
public class TestXmlConfigErrors extends BaseTest {

    @Test(expected = RuntimeException.class)
    public void testInvalidExtension() {
        IsoMessageDef.build("test-invalid-extension.xml");
    }

    @Test(expected = RuntimeException.class)
    public void testInvalidBitmapComposite() {
        IsoMessageDef.build("test-invalid-bitmap-composite.xml");
    }

    @Test(expected = RuntimeException.class)
    public void testDuplicateMti() {
        IsoMessageDef.build("test-duplicate-mti.xml");
    }

    @Test(expected = RuntimeException.class)
    public void testDuplicateMtiExtension() {
        IsoMessageDef.build("test-duplicate-mti-extension.xml");
    }

    @Test(expected = RuntimeException.class)
    public void testDuplicateField() {
        IsoMessageDef.build("test-duplicate-field.xml");
    }

    @Test(expected = RuntimeException.class)
    public void testInvalidSchema() {
        IsoMessageDef.build("test-invalid-schema.xml");
    }

}
