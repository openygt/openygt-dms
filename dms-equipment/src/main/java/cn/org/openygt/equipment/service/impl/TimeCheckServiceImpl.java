package cn.org.openygt.equipment.service.impl;

import cn.org.openygt.equipment.entity.DecoctionTrace;
import cn.org.openygt.equipment.entity.TimeCheckRule;
import cn.org.openygt.equipment.mapper.DecoctionTraceMapper;
import cn.org.openygt.equipment.mapper.TimeCheckRuleMapper;
import cn.org.openygt.equipment.service.TimeCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimeCheckServiceImpl implements TimeCheckService {

    private final TimeCheckRuleMapper ruleMapper;
    private final DecoctionTraceMapper traceMapper;

    @Override
    public List<TimeCheckRule> getAllRules() {
        return ruleMapper.selectList(null);
    }

    @Override
    public TimeCheckRule createRule(TimeCheckRule rule) {
        ruleMapper.insert(rule);
        return rule;
    }

    @Override
    public TimeCheckRule updateRule(Long id, TimeCheckRule rule) {
        rule.setId(id);
        ruleMapper.updateById(rule);
        return rule;
    }

    @Override
    public void deleteRule(Long id) {
        ruleMapper.deleteById(id);
    }

    @Override
    public Map<String, Object> validate(String prescriptionNo, String toStep) {
        DecoctionTrace trace = traceMapper.findByPrescriptionNo(prescriptionNo);
        if (trace == null) {
            Map<String, Object> r = new HashMap<>();
            r.put("valid", true);
            r.put("message", "追溯记录不存在，跳过校验");
            return r;
        }

        List<TimeCheckRule> rules = ruleMapper.findByToStep(toStep);
        if (rules == null || rules.isEmpty()) {
            Map<String, Object> r = new HashMap<>();
            r.put("valid", true);
            r.put("message", "无校验规则");
            return r;
        }

        for (TimeCheckRule rule : rules) {
            LocalDateTime fromTime = getStepTime(trace, rule.getFromStep());
            if (fromTime == null) {
                continue;
            }

            long actualSeconds = Duration.between(fromTime, LocalDateTime.now()).getSeconds();

            if (rule.getMinDuration() != null && rule.getMinDuration() > 0) {
                if (actualSeconds < rule.getMinDuration()) {
                    long remaining = rule.getMinDuration() - actualSeconds;
                    String message = rule.getBlockMessage() != null
                        ? rule.getBlockMessage().replace("%s", String.valueOf(remaining / 60))
                        : "时间不足，还需等待 " + (remaining / 60) + " 分钟";
                    Map<String, Object> r = new HashMap<>();
                    r.put("valid", false);
                    r.put("checkType", "BLOCK");
                    r.put("message", message);
                    r.put("actualDuration", actualSeconds);
                    r.put("requiredDuration", rule.getMinDuration());
                    r.put("remainingSeconds", remaining);
                    return r;
                }
            }

            if (rule.getMaxDuration() != null && rule.getMaxDuration() > 0) {
                if (actualSeconds > rule.getMaxDuration()) {
                    String message = rule.getWarningMessage() != null
                        ? rule.getWarningMessage().replace("%s", String.valueOf(actualSeconds / 60))
                        : "已超时 " + (actualSeconds / 60) + " 分钟";
                    Map<String, Object> r = new HashMap<>();
                    r.put("valid", true);
                    r.put("checkType", "WARN");
                    r.put("message", message);
                    r.put("actualDuration", actualSeconds);
                    r.put("maxDuration", rule.getMaxDuration());
                    return r;
                }
            }
        }

        Map<String, Object> r = new HashMap<>();
        r.put("valid", true);
        r.put("checkType", "PASS");
        r.put("message", "校验通过");
        return r;
    }

    @Override
    public List<Map<String, Object>> getPrescriptionCheckStatus(String prescriptionNo) {
        DecoctionTrace trace = traceMapper.findByPrescriptionNo(prescriptionNo);
        if (trace == null) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> results = new ArrayList<>();
        List<TimeCheckRule> rules = ruleMapper.selectList(null);

        for (TimeCheckRule rule : rules) {
            LocalDateTime fromTime = getStepTime(trace, rule.getFromStep());
            if (fromTime == null) {
                continue;
            }

            long actualSeconds = Duration.between(fromTime, LocalDateTime.now()).getSeconds();
            String checkStatus = "NORMAL";
            String message = "";

            if (rule.getMaxDuration() != null && rule.getMaxDuration() > 0) {
                if (actualSeconds > rule.getMaxDuration()) {
                    checkStatus = "TIMEOUT";
                    message = "已超时";
                } else if (actualSeconds > rule.getMaxDuration() * 0.9) {
                    checkStatus = "WARNING";
                    message = "即将超时";
                }
            }

            Map<String, Object> item = new HashMap<>();
            item.put("ruleCode", rule.getRuleCode());
            item.put("ruleName", rule.getRuleName());
            item.put("checkStatus", checkStatus);
            item.put("message", message);
            item.put("actualDuration", actualSeconds);
            item.put("requiredDuration", rule.getMinDuration() != null ? rule.getMinDuration() : 0);
            results.add(item);
        }

        return results;
    }

    private LocalDateTime getStepTime(DecoctionTrace trace, String step) {
        if (trace == null || step == null) return null;
        if ("SOAK_START".equals(step)) return trace.getSoakStartTime();
        if ("FIRST_DECOCT_START".equals(step)) return trace.getFirstDecoctStart();
        if ("FIRST_DECOCT_END".equals(step)) return trace.getFirstDecoctEnd();
        if ("SECOND_DECOCT_START".equals(step)) return trace.getSecondDecoctStart();
        if ("SECOND_DECOCT_END".equals(step)) return trace.getSecondDecoctEnd();
        if ("PACKAGE_COMPLETE".equals(step)) return trace.getPackageEndTime();
        if ("ADD_LATE_REMIND".equals(step)) return trace.getAddLateTimeActual();
        return null;
    }
}
