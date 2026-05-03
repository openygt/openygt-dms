package cn.org.openygt.iot.adapter;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdapterRegistryTest {

    @Test
    void startAll_returnsStartupReportWithFailures() {
        FakeAdapter ok = new FakeAdapter("ok", false);
        FakeAdapter fail = new FakeAdapter("fail", true);
        AdapterRegistry registry = new AdapterRegistry(Arrays.asList(ok, fail));
        registry.init();

        AdapterRegistry.StartupReport report = registry.startAll();

        assertEquals(1, report.getStarted().size());
        assertTrue(report.getFailed().containsKey("fail"));
        assertTrue(report.hasFailures());
    }

    @Test
    void restart_stopsAndStartsAdapter() {
        FakeAdapter adapter = new FakeAdapter("ok", false);
        AdapterRegistry registry = new AdapterRegistry(Arrays.asList(adapter));
        registry.init();

        boolean restarted = registry.restart("ok");

        assertTrue(restarted);
        assertTrue(adapter.stopCalled);
        assertEquals(1, adapter.startCount);
    }

    @Test
    void restart_returnsFalseWhenAdapterMissing() {
        AdapterRegistry registry = new AdapterRegistry(Arrays.asList());
        registry.init();

        assertFalse(registry.restart("missing"));
    }

    private static class FakeAdapter implements DeviceAdapter {
        private final String protocolType;
        private final boolean failOnStart;
        private boolean stopCalled;
        private int startCount;

        private FakeAdapter(String protocolType, boolean failOnStart) {
            this.protocolType = protocolType;
            this.failOnStart = failOnStart;
        }

        @Override
        public String getProtocolType() {
            return protocolType;
        }

        @Override
        public void start() {
            startCount++;
            if (failOnStart) {
                throw new IllegalStateException("boom");
            }
        }

        @Override
        public void stop() {
            stopCalled = true;
        }

        @Override
        public void sendCommand(String deviceCode, DeviceCommandDTO command) {
        }

        @Override
        public AdapterStatus getStatus() {
            return AdapterStatus.RUNNING;
        }
    }
}
