package cn.org.openygt.equipment.service;

import cn.org.openygt.equipment.entity.TimeCheckRule;

import java.util.List;
import java.util.Map;

public interface TimeCheckService {

    List<TimeCheckRule> getAllRules();

    TimeCheckRule createRule(TimeCheckRule rule);

    TimeCheckRule updateRule(Long id, TimeCheckRule rule);

    void deleteRule(Long id);

    Map<String, Object> validate(String prescriptionNo, String toStep);

    List<Map<String, Object>> getPrescriptionCheckStatus(String prescriptionNo);
}
