package cn.org.openygt.equipment.service;

import cn.org.openygt.equipment.entity.LabelTemplate;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface LabelTemplateService extends IService<LabelTemplate> {
    IPage<LabelTemplate> list(String keyword, String templateType, int page, int size);
}
