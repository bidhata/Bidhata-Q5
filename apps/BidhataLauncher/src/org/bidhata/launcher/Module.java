package org.bidhata.launcher;

import android.content.Context;

/**
 * One background feature of the launcher (it lives in the launcher process, which is persistent).
 * Modules are started by {@link Modules} at boot and can be switched off by the user.
 * A module must leave nothing running after stop().
 */
interface Module {
    /** Short stable name, used as the settings key and in logs. */
    String id();

    /** String resources for the Background features page. */
    int title();
    int summary();

    void start(Context app);

    void stop(Context app);
}
