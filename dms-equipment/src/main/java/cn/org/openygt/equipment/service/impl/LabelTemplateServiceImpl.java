package cn.org.openygt.equipment.service.impl;

import cn.org.openygt.equipment.entity.LabelTemplate;
import cn.org.openygt.equipment.mapper.LabelTemplateMapper;
import cn.org.openygt.equipment.service.LabelTemplateService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LabelTemplateServiceImpl extends ServiceImpl<LabelTemplateMapper, LabelTemplate> implements LabelTemplateService {

    @Override
    public IPage<LabelTemplate> list(String keyword, String templateType, int page, int size) {
        QueryWrapper<LabelTemplate> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like("template_code", keyword).or().like("template_name", keyword));
        }
        if (StringUtils.hasText(templateType)) {
            wrapper.eq("template_type", templateType);
        }
        wrapper.orderByDesc("created_at");
        return this.page(new Page<>(page, size), wrapper);
    }
}
