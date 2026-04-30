package cn.org.openygt.equipment.service;

import cn.org.openygt.equipment.entity.PackageSpec;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface PackageSpecService extends IService<PackageSpec> {

    IPage<PackageSpec> list(String keyword, int page, int size);

    PackageSpec create(PackageSpec packageSpec);

    PackageSpec update(Long id, PackageSpec packageSpec);

    PackageSpec getById(Long id);

    void delete(Long id);
}
