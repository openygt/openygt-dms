package cn.org.openygt.system.service;

import cn.org.openygt.system.entity.Signature;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface SignatureService extends IService<Signature> {

    Signature submitSignature(Signature signature);

    List<Signature> querySignatures(String bizType, Long bizId);
}
