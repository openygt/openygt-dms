package cn.org.openygt.equipment.service;

import cn.org.openygt.equipment.entity.EqDevicePairing;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface EqDevicePairingService {
    EqDevicePairing create(EqDevicePairing pairing);
    EqDevicePairing update(Long id, EqDevicePairing pairing);
    void delete(Long id);
    EqDevicePairing getById(Long id);
    List<EqDevicePairing> listAll();
}
