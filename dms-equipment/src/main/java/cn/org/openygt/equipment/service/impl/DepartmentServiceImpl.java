package cn.org.openygt.equipment.service.impl;

import cn.org.openygt.equipment.entity.Department;
import cn.org.openygt.equipment.mapper.DepartmentMapper;
import cn.org.openygt.equipment.service.DepartmentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Department::getDeptName, keyword)
                    .or().like(Department::getDeptCode, keyword));
        }
        if (hospitalId != null) {
            wrapper.eq(Department::getHospitalId, hospitalId);
        }
        wrapper.orderByAsc(Department::getSortOrder);
        return baseMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public List<Department> listAll() {
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Department::getSortOrder);
        return baseMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public Department create(Department department) {
        if (department.getStatus() == null) {
            department.setStatus(1);
        }
        // 校验编码唯一性
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Department::getDeptCode, department.getDeptCode());
        if (baseMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("科室编码已存在: " + department.getDeptCode());
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
        // 校验编码唯一性（排除自身）
        if (department.getDeptCode() != null && !department.getDeptCode().equals(existing.getDeptCode())) {
            LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Department::getDeptCode, department.getDeptCode()).ne(Department::getId, id);
            if (baseMapper.selectCount(wrapper) > 0) {
                throw new IllegalArgumentException("科室编码已存在: " + department.getDeptCode());
            }
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
        Department existing = baseMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("科室不存在: " + id);
        }
        // 禁用优先，避免物理删除造成孤儿数据
        existing.setStatus(0);
        baseMapper.updateById(existing);
    }
}
