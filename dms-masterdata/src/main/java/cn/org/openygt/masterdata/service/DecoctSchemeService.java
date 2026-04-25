package cn.org.openygt.masterdata.service;

import cn.org.openygt.masterdata.entity.DecoctScheme;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface DecoctSchemeService {

    DecoctScheme create(DecoctScheme scheme);

    DecoctScheme update(Long id, DecoctScheme scheme);

    DecoctScheme getById(Long id);

    IPage<DecoctScheme> list(String keyword, int page, int size);

    void delete(Long id);
}
