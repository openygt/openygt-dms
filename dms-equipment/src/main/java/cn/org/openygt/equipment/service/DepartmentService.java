package cn.org.openygt.equipment.service;

import cn.org.openygt.equipment.entity.Department;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface DepartmentService extends IService<Department> {

    IPage<Department> list(String keyword, Long hospitalId, int page, int size);

    List<Department> listAll();

    Department create(Department department);

    Department update(Long id, Department department);

    Department getById(Long id);

    void delete(Long id);
}
