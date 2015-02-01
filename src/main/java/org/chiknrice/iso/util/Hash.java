/**
 * 
 */
package org.chiknrice.iso.util;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 *
 */
public class Hash {

    public static int build(Object object, Object... fields) {
        int hash = object.getClass().hashCode();
        for (Object field : fields) {
            if (field != null) {
                hash ^= field.hashCode();
            }
        }
        return hash;
    }

}
