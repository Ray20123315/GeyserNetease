/*
 * Copyright (c) 2026 EiluDick/ZDarkZ
 * Released under the MIT License.
 */

package nc.geyserext.netease;

import nc.geyserext.netease.config.*;
import nc.geyserext.netease.util.*;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserShutdownEvent;
import org.geysermc.geyser.api.extension.*;

public class NeteaseExtension implements Extension {

    public static ExtensionLogger LOG;
    public static NeteaseConfig CONFIG;

    @Subscribe
    public void onPostInit(GeyserPostInitializeEvent event) {
        LOG = logger();
        LOG.info("NetEase Extension starting...");

        CONFIG = NeteaseConfigLoader.load(this, NeteaseExtension.class);

        if (CONFIG == null) {
            LOG.error("Invalid configuration; NetEase listener NOT started");
            return;
        }
        if (CONFIG.neteaseListenPort() == 0) {
            LOG.info("NetEase listener disabled (netease-listen-port: 0)");
            return;
        }
        try {
            ServerRestartUtil.start(CONFIG.neteaseListenPort());
        } catch (Throwable error) {
            LOG.error("NetEase listener startup failed; international Bedrock left unchanged", error);
        }
    }
    @Subscribe
    public void onShutdown(GeyserShutdownEvent event) {
        ServerRestartUtil.stop();
    }
}
