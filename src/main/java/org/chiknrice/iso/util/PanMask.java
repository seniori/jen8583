package org.chiknrice.iso.util;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 */
public class PanMask implements Mask {

    private static final int FIRST = 6;
    private static final int LAST = 4;

    @Override
    public String apply(Object o) {
        String pan = o.toString();
        StringBuilder s = new StringBuilder();
        int startMasked = FIRST;
        int endMasked = pan.length() - LAST;
        if (startMasked > endMasked) {
            startMasked = 0;
            endMasked = pan.length();
        }
        s.append(pan.substring(0, startMasked));
        for (int i = startMasked; i < endMasked; i++) {
            s.append('*');
        }
        s.append(pan.substring(endMasked));
        return s.toString();
    }

}
