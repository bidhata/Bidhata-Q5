package org.bidhata.launcher;

import java.lang.reflect.Method;

/**
 * Asks the ROM's root helper (q5_autoeq service, started by init when bidhata.autoeq.req
 * changes) to do something the launcher may not: write JamesDSP's settings, install a boot
 * animation. The target says what; the request value only has to change.
 */
final class Root {

    private Root() {}

    static void request(String target) {
        try {
            Class<?> sp = Class.forName("android.os.SystemProperties");
            Method set = sp.getMethod("set", String.class, String.class);
            set.invoke(null, "bidhata.autoeq.target", target);
            set.invoke(null, "bidhata.autoeq.req", String.valueOf(System.currentTimeMillis()));
        } catch (Exception ignored) {
        }
    }
}
