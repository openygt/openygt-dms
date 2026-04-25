package cn.org.openygt.masterdata.service.impl;

import cn.org.openygt.masterdata.entity.DecoctScheme;
import cn.org.openygt.masterdata.mapper.DecoctSchemeMapper;
import cn.org.openygt.masterdata.service.DecoctSchemeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DecoctSchemeServiceImpl implements DecoctSchemeService {

    private final DecoctSchemeMapper schemeMapper;

    public DecoctSchemeServiceImpl(DecoctSchemeMapper schemeMapper) {
        this.schemeMapper = schemeMapper;
    }

    @Override
    @Transactional
    public DecoctScheme create(DecoctScheme scheme) {
        schemeMapper.insert(scheme);
        return scheme;
    }

    @Override
    @Transactional
    public DecoctScheme update(Long id, DecoctScheme scheme) {
        DecoctScheme existing = schemeMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("煎药方案不存在: " + id);
        }
        scheme.setId(id);
        schemeMapper.updateById(scheme);
        return schemeMapper.selectById(id);
    }

    @Override
    public DecoctScheme getById(Long id) {
        DecoctScheme scheme = schemeMapper.selectById(id);
        if (scheme == null) {
            throw new IllegalArgumentException("煎药方案不存在: " + id);
        }
        return scheme;
    }

    @Override
    public IPage<DecoctScheme> list(String keyword, int page, int size) {
        LambdaQueryWrapper<DecoctScheme> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(DecoctScheme::getName, keyword);
        }
        wrapper.orderByDesc(DecoctScheme::getCreatedAt);
        return schemeMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        DecoctScheme existing = schemeMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("煎药方案不存在: " + id);
        }
        schemeMapper.deleteById(id);
    }
}
