package cn.org.openygt.equipment.service.impl;

import cn.org.openygt.equipment.entity.EqDevicePairing;
import cn.org.openygt.equipment.mapper.EqDevicePairingMapper;
import cn.org.openygt.equipment.service.EqDevicePairingService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EqDevicePairingServiceImpl implements EqDevicePairingService {

    private final EqDevicePairingMapper pairingMapper;

    public EqDevicePairingServiceImpl(EqDevicePairingMapper pairingMapper) {
        this.pairingMapper = pairingMapper;
    }

    @Override
    @Transactional
    public EqDevicePairing create(EqDevicePairing pairing) {
        if (pairing.getStatus() == null) {
            pairing.setStatus("ACTIVE");
        }
        pairingMapper.insert(pairing);
        return pairing;
    }

    @Override
    @Transactional
    public EqDevicePairing update(Long id, EqDevicePairing pairing) {
        EqDevicePairing existing = pairingMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("配对记录不存在: " + id);
        }
        pairing.setId(id);
        pairingMapper.updateById(pairing);
        return pairingMapper.selectById(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        pairingMapper.deleteById(id);
    }

    @Override
    public EqDevicePairing getById(Long id) {
        return pairingMapper.selectById(id);
    }

    @Override
    public List<EqDevicePairing> listAll() {
        LambdaQueryWrapper<EqDevicePairing> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EqDevicePairing::getStatus, "ACTIVE").orderByDesc(EqDevicePairing::getCreatedAt);
        return pairingMapper.selectList(wrapper);
    }
}
