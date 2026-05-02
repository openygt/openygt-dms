package cn.org.openygt.system.service.impl;

import cn.org.openygt.system.entity.Signature;
import cn.org.openygt.system.mapper.SignatureMapper;
import cn.org.openygt.system.service.SignatureService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SignatureServiceImpl extends ServiceImpl<SignatureMapper, Signature>
        implements SignatureService {

    @Override
    public Signature submitSignature(Signature signature) {
        signature.setSignTime(LocalDateTime.now());
        save(signature);
        return signature;
    }

    @Override
    public List<Signature> querySignatures(String bizType, Long bizId) {
        return baseMapper.selectList(
                new LambdaQueryWrapper<Signature>()
                        .eq(Signature::getBizType, bizType)
                        .eq(Signature::getBizId, bizId)
                        .orderByDesc(Signature::getSignTime)
        );
    }
}
