from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/nc/geyserext/netease"

class SafeListenerContract(unittest.TestCase):
    def test_no_primary_listener_restart(self):
        s = (JAVA / "util/ServerRestartUtil.java").read_text()
        self.assertNotIn("getGeyserServer().shutdown()", s)
        self.assertIn("new RaknetServer(geyser", s)
        self.assertIn("bootstrap.childHandler(initializer)", s)

    def test_separate_v8_port(self):
        s = (JAVA / "util/ServerRestartUtil.java").read_text()
        self.assertIn("port == geyser.config().bedrock().port()", s)
        self.assertIn("port == 0", s)
        self.assertIn("new int[] {8}", s)
        self.assertIn("port == webrtcPort", s)

    def test_fail_closed_on_start_failure(self):
        s = (JAVA / "util/ServerRestartUtil.java").read_text()
        self.assertIn("close(candidate, initializer, bindAttempted)", s)
        self.assertIn("playerGroupField.set(candidate, initializer.getEventLoopGroup())", s)
        self.assertIn("bootstrapFutures", s)

    def test_shutdown_cleans_side_listener(self):
        ext = (JAVA / "NeteaseExtension.java").read_text()
        self.assertIn("GeyserShutdownEvent", ext)
        self.assertIn("ServerRestartUtil.stop()", ext)

    def test_default_off(self):
        c = (JAVA / "config/NeteaseConfig.java").read_text()
        y = (ROOT / "src/main/resources/config.yml").read_text()
        self.assertIn("private int neteaseListenPort = 0", c)
        self.assertIn("netease-listen-port: 0", y)

    def test_isolated_mappings(self):
        h = (JAVA / "handler/NetEaseUpstreamHandler.java").read_text()
        self.assertNotIn("using shared:", h)
        self.assertIn("NetEase block mapping initialization failed", h)

    def test_verified_uid_required(self):
        h = (JAVA / "handler/NetEaseUpstreamHandler.java").read_text()
        self.assertNotIn("neteaseUid = Math.abs(name.hashCode())", h)
        self.assertIn("NetEase identity is missing a verified UID", h)

    def test_all_mapping_arrays_must_patch(self):
        h = (JAVA / "session/SpoofedUpstream.java").read_text()
        self.assertIn("if (!javaBlocks || !runtimeMap || !vanillaBlocks) return false;", h)

if __name__ == "__main__":
    unittest.main()
