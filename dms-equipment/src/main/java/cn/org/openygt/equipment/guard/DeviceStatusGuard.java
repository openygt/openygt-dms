package cn.org.openygt.equipment.guard;

import cn.org.openygt.common.enums.DeviceType;
import cn.org.openygt.equipment.enums.DeviceDetailStatus;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 设备状态类型守卫
 */
@Component
public class DeviceStatusGuard {

    private static final Map<DeviceType, Set<DeviceDetailStatus>> ALLOWED;
    static {
        Map<DeviceType, Set<DeviceDetailStatus>> map = new HashMap<>();
        map.put(DeviceType.COOKING_MACHINE, Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            DeviceDetailStatus.IDLE, DeviceDetailStatus.STANDBY, DeviceDetailStatus.READY,
            DeviceDetailStatus.SOAKING, DeviceDetailStatus.PRE_DECOCTING,
            DeviceDetailStatus.FIRST_DECOCTING, DeviceDetailStatus.ADD_LATE,
            DeviceDetailStatus.ADD_LATE_REMIND, DeviceDetailStatus.SECOND_DECOCTING,
            DeviceDetailStatus.DRAINING, DeviceDetailStatus.COOLING,
            DeviceDetailStatus.CLEANING, DeviceDetailStatus.CALIBRATING,
            DeviceDetailStatus.PAUSED, DeviceDetailStatus.FAULT,
            DeviceDetailStatus.OFFLINE, DeviceDetailStatus.RESERVED,
            DeviceDetailStatus.MAINTENANCE
        ))));
        map.put(DeviceType.PACKING_MACHINE, Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            DeviceDetailStatus.IDLE, DeviceDetailStatus.STANDBY, DeviceDetailStatus.READY,
            DeviceDetailStatus.PACKAGING, DeviceDetailStatus.PACKAGE_COMPLETE,
            DeviceDetailStatus.LABELING, DeviceDetailStatus.LABEL_COMPLETE,
            DeviceDetailStatus.PAUSED, DeviceDetailStatus.FAULT,
            DeviceDetailStatus.OFFLINE, DeviceDetailStatus.RESERVED,
            DeviceDetailStatus.MAINTENANCE
        ))));
        map.put(DeviceType.PRINTER, Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            DeviceDetailStatus.IDLE, DeviceDetailStatus.STANDBY, DeviceDetailStatus.READY,
            DeviceDetailStatus.PAUSED, DeviceDetailStatus.FAULT,
            DeviceDetailStatus.OFFLINE, DeviceDetailStatus.MAINTENANCE
        ))));
        ALLOWED = Collections.unmodifiableMap(map);
    }

    public void validate(DeviceType type, DeviceDetailStatus target) {
        if (type == null || target == null) {
            throw new IllegalArgumentException("设备类型和目标状态不能为空");
        }
        if (!ALLOWED.getOrDefault(type, Collections.emptySet()).contains(target)) {
            throw new IllegalStateException(
                type.name() + " 不允许设置状态 " + target.name()
            );
        }
    }
}
