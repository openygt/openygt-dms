package cn.org.openygt.iot.gateway;

import cn.org.openygt.iot.adapter.AdapterRegistry;
import cn.org.openygt.iot.adapter.AdapterStatus;
import cn.org.openygt.iot.adapter.DeviceAdapter;
import cn.org.openygt.iot.config.GatewayProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdapterLifecycleManagerTest {

    @Mock
    private AdapterRegistry registry;

    @Mock
    private DeviceAdapter deviceAdapter;

    @Test
    void healthCheck_restartsAdaptersInErrorState() {
        Map<String, DeviceAdapter> adapters = new HashMap<>();
        adapters.put("penglin-mqtt", deviceAdapter);
        when(registry.getAllAdapters()).thenReturn(adapters);
        when(deviceAdapter.getStatus()).thenReturn(AdapterStatus.ERROR);
        when(registry.restart("penglin-mqtt")).thenReturn(true);

        AdapterLifecycleManager manager = new AdapterLifecycleManager(registry, new GatewayProperties());
        manager.healthCheck();

        verify(registry).restart("penglin-mqtt");
    }

    @Test
    void healthCheck_doesNotRestartRunningAdapters() {
        when(registry.getAllAdapters()).thenReturn(Collections.singletonMap("penglin-mqtt", deviceAdapter));
        when(deviceAdapter.getStatus()).thenReturn(AdapterStatus.RUNNING);

        AdapterLifecycleManager manager = new AdapterLifecycleManager(registry, new GatewayProperties());
        manager.healthCheck();

        verify(registry, never()).restart("penglin-mqtt");
    }
}
