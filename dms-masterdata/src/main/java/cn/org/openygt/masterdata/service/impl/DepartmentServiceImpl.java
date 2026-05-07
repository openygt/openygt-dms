package cn.org.openygt.masterdata.service.impl;

import cn.org.openygt.masterdata.entity.Department;
import cn.org.openygt.masterdata.mapper.DepartmentMapper;
import cn.org.openygt.masterdata.service.DepartmentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentMapper departmentMapper;

    public DepartmentServiceImpl(DepartmentMapper departmentMapper) {
        this.departmentMapper = departmentMapper;
    }

    @Override
    @Transactional
    public Department create(Department department) {
        departmentMapper.insert(department);
        return department;
    }

    @Override
    @Transactional
    public Department update(Long id, Department department) {
        department.setId(id);
        departmentMapper.updateById(department);
        return departmentMapper.selectById(id);
    }

    @Override
    public Department getById(Long id) {
        return departmentMapper.selectById(id);
    }

    @Override
    public IPage<Department> list(String keyword, int page, int size) {
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Department::getName, keyword)
                    .or().like(Department::getSourceId, keyword));
        }
        wrapper.orderByAsc(Department::getSort);
        return departmentMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        departmentMapper.deleteById(id);
    }
}
