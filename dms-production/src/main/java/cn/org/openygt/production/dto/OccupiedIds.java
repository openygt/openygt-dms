package cn.org.openygt.production.dto;

import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class OccupiedIds {
    private List<Long> taskIds = Collections.emptyList();
    private List<Long> employeeIds = Collections.emptyList();
    private List<Long> deviceIds = Collections.emptyList();
}
