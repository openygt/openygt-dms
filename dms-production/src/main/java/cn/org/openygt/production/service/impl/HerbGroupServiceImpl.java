package cn.org.openygt.production.service.impl;

import cn.org.openygt.production.dto.HerbGroupDTO;
import cn.org.openygt.production.entity.HerbGroupRule;
import cn.org.openygt.production.entity.PrescriptionHerbGroup;
import cn.org.openygt.production.mapper.HerbGroupRuleMapper;
import cn.org.openygt.production.mapper.PrescriptionHerbGroupMapper;
import cn.org.openygt.production.service.HerbGroupService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HerbGroupServiceImpl implements HerbGroupService {

    private final PrescriptionHerbGroupMapper prescriptionHerbGroupMapper;
    private final HerbGroupRuleMapper herbGroupRuleMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<HerbGroupDTO> getHerbGroupsByPrescription(Long prescriptionId) {
        LambdaQueryWrapper<PrescriptionHerbGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PrescriptionHerbGroup::getPrescriptionId, prescriptionId)
                .orderByAsc(PrescriptionHerbGroup::getGroupSeq);
        List<PrescriptionHerbGroup> groups = prescriptionHerbGroupMapper.selectList(wrapper);

        if (groups.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        List<String> groupCodes = groups.stream()
                .map(PrescriptionHerbGroup::getGroupCode)
                .distinct()
                .collect(Collectors.toList());

        LambdaQueryWrapper<HerbGroupRule> ruleWrapper = new LambdaQueryWrapper<>();
        ruleWrapper.in(HerbGroupRule::getGroupCode, groupCodes);
        List<HerbGroupRule> rules = herbGroupRuleMapper.selectList(ruleWrapper);
        Map<String, HerbGroupRule> ruleMap = rules.stream()
                .collect(Collectors.toMap(HerbGroupRule::getGroupCode, r -> r, (a, b) -> a));

        List<HerbGroupDTO> result = new ArrayList<>();
        for (PrescriptionHerbGroup group : groups) {
            HerbGroupRule rule = ruleMap.get(group.getGroupCode());
            HerbGroupDTO dto = convertToDTO(group, rule);
            result.add(dto);
        }
        return result;
    }

    @Override
    @Transactional
    public HerbGroupDTO confirmHerbGroup(Long groupId, Long operatorId, Long deviceId) {
        PrescriptionHerbGroup group = prescriptionHerbGroupMapper.selectById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("药材分组不存在");
        }
        if (group.getProcessStatus() != null && group.getProcessStatus() == 1) {
            throw new IllegalStateException("该分组已确认投料");
        }

        group.setProcessStatus(1);
        group.setProcessTime(LocalDateTime.now());
        group.setOperatorId(operatorId);
        group.setDeviceId(deviceId);
        prescriptionHerbGroupMapper.updateById(group);

        HerbGroupRule rule = herbGroupRuleMapper.selectOne(
                new LambdaQueryWrapper<HerbGroupRule>()
                        .eq(HerbGroupRule::getGroupCode, group.getGroupCode())
                        .last("LIMIT 1"));
        return convertToDTO(group, rule);
    }

    private HerbGroupDTO convertToDTO(PrescriptionHerbGroup group, HerbGroupRule rule) {
        HerbGroupDTO dto = new HerbGroupDTO();
        dto.setId(group.getId());
        dto.setPrescriptionId(group.getPrescriptionId());
        dto.setGroupCode(group.getGroupCode());
        dto.setGroupSeq(group.getGroupSeq());
        dto.setProcessStatus(group.getProcessStatus());
        dto.setProcessTime(group.getProcessTime());
        dto.setOperatorId(group.getOperatorId());
        dto.setDeviceId(group.getDeviceId());

        if (rule != null) {
            dto.setGroupName(rule.getGroupName());
            dto.setGroupColor(rule.getGroupColor());
            dto.setGroupIcon(rule.getGroupIcon());
            dto.setProcessType(rule.getProcessType());
            dto.setSpecialInstruction(rule.getSpecialInstruction());
        }

        if (group.getHerbsJson() != null && !group.getHerbsJson().isEmpty()) {
            try {
                List<Map<String, Object>> herbs = objectMapper.readValue(group.getHerbsJson(), new com.fasterxml.jackson.core.type.TypeReference<java.util.List<java.util.Map<String, Object>>>() {});
                dto.setHerbs(herbs.stream().map(h -> {
                    HerbGroupDTO.HerbItemDTO item = new HerbGroupDTO.HerbItemDTO();
                    item.setHerbName((String) h.getOrDefault("herbName", h.get("name")));
                    item.setDosage(String.valueOf(h.getOrDefault("dosage", h.get("quantity"))));
                    item.setUnit((String) h.getOrDefault("unit", ""));
                    return item;
                }).collect(Collectors.toList()));
            } catch (Exception e) {
                log.warn("解析药材JSON失败: {}", group.getHerbsJson(), e);
                dto.setHerbs(java.util.Collections.emptyList());
            }
        } else {
            dto.setHerbs(java.util.Collections.emptyList());
        }

        return dto;
    }
}
