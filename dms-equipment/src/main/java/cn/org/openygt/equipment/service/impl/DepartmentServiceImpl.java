package cn.org.openygt.equipment.service.impl;

import cn.org.openygt.equipment.entity.Department;
import cn.org.openygt.equipment.mapper.DepartmentMapper;
import cn.org.openygt.equipment.service.DepartmentService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, Department> implements DepartmentService {

    @Override
    public IPage<Department> list(String keyword, Long hospitalId, int page, int size) {
        QueryWrapper<Department> wrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like("dept_name", keyword)
                    .or().like("dept_code", keyword));
        }
        if (hospitalId != null) {
            wrapper.eq("hospital_id", hospitalId);
        }
        wrapper.orderByAsc("sort_order");
        return baseMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public List<Department> listAll() {
        QueryWrapper<Department> wrapper = new QueryWrapper<>();
        wrapper.orderByAsc("sort_order");
        return baseMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public Department create(Department department) {
        if (department.getStatus() == null) {
            department.setStatus(1);
        }
        baseMapper.insert(department);
        return department;
    }

    @Override
    @Transactional
    public Department update(Long id, Department department) {
        Department existing = baseMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("科室不存在: " + id);
        }
        department.setId(id);
        baseMapper.updateById(department);
        return baseMapper.selectById(id);
    }

    @Override
    public Department getById(Long id) {
        Department department = baseMapper.selectById(id);
        if (department == null) {
            throw new IllegalArgumentException("科室不存在: " + id);
        }
        return department;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        baseMapper.deleteById(id);
    }
}
