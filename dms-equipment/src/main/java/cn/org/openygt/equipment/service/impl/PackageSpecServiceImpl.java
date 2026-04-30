package cn.org.openygt.equipment.service.impl;

import cn.org.openygt.equipment.entity.PackageSpec;
import cn.org.openygt.equipment.mapper.PackageSpecMapper;
import cn.org.openygt.equipment.service.PackageSpecService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PackageSpecServiceImpl extends ServiceImpl<PackageSpecMapper, PackageSpec> implements PackageSpecService {

    @Override
    public IPage<PackageSpec> list(String keyword, int page, int size) {
        QueryWrapper<PackageSpec> wrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like("spec_name", keyword)
                    .or().like("spec_code", keyword));
        }
        wrapper.orderByAsc("sort_order");
        return baseMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional
    public PackageSpec create(PackageSpec packageSpec) {
        if (packageSpec.getStatus() == null) {
            packageSpec.setStatus(1);
        }
        baseMapper.insert(packageSpec);
        return packageSpec;
    }

    @Override
    @Transactional
    public PackageSpec update(Long id, PackageSpec packageSpec) {
        PackageSpec existing = baseMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("包装规格不存在: " + id);
        }
        packageSpec.setId(id);
        baseMapper.updateById(packageSpec);
        return baseMapper.selectById(id);
    }

    @Override
    public PackageSpec getById(Long id) {
        PackageSpec packageSpec = baseMapper.selectById(id);
        if (packageSpec == null) {
            throw new IllegalArgumentException("包装规格不存在: " + id);
        }
        return packageSpec;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        baseMapper.deleteById(id);
    }
}
