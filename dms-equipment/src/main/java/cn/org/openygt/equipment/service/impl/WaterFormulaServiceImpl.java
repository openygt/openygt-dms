package cn.org.openygt.equipment.service.impl;

import cn.org.openygt.equipment.entity.WaterFormula;
import cn.org.openygt.equipment.mapper.WaterFormulaMapper;
import cn.org.openygt.equipment.service.WaterFormulaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WaterFormulaServiceImpl implements WaterFormulaService {

    private final WaterFormulaMapper formulaMapper;
    private final ExpressionParser parser = new SpelExpressionParser();

    @Override
    public List<WaterFormula> getAllFormulas() {
        return formulaMapper.selectList(null);
    }

    @Override
    public WaterFormula createFormula(WaterFormula formula) {
        validateExpression(formula.getExpression());
        formulaMapper.insert(formula);
        return formula;
    }

    @Override
    public WaterFormula updateFormula(Long id, WaterFormula formula) {
        validateExpression(formula.getExpression());
        formula.setId(id);
        formulaMapper.updateById(formula);
        return formula;
    }

    @Override
    public void deleteFormula(Long id) {
        formulaMapper.deleteById(id);
    }

    @Override
    public Map<String, Object> calculate(Long formulaId, Map<String, Object> variables) {
        WaterFormula formula = formulaMapper.selectById(formulaId);
        if (formula == null) {
            throw new IllegalArgumentException("公式不存在: " + formulaId);
        }

        StandardEvaluationContext context = new StandardEvaluationContext();
        if (variables != null) {
            variables.forEach((key, value) -> {
                String varName = key.replace(".", "_");
                if (value instanceof Number) {
                    context.setVariable(varName, ((Number) value).doubleValue());
                } else {
                    context.setVariable(varName, value);
                }
            });
        }

        try {
            Expression exp = parser.parseExpression(formula.getExpression().replace(".", "_"));
            Double result = exp.getValue(context, Double.class);
            BigDecimal bd = BigDecimal.valueOf(result);

            Map<String, Object> r = new HashMap<>();
            r.put("result", bd);
            r.put("formula", formula.getExpression());
            r.put("variables", variables != null ? variables : new HashMap<>());
            return r;
        } catch (Exception e) {
            log.error("公式计算失败: id={}, expression={}, variables={}", formulaId, formula.getExpression(), variables, e);
            throw new IllegalArgumentException("公式计算失败: " + e.getMessage());
        }
    }

    @Override
    public WaterFormula getDefaultFormula() {
        return formulaMapper.findDefault();
    }

    private void validateExpression(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            throw new IllegalArgumentException("公式表达式不能为空");
        }
        String[] blacklist = {"new ", "System.", "Runtime.", "Process", "exec", "java.lang"};
        for (String bad : blacklist) {
            if (expression.contains(bad)) {
                throw new IllegalArgumentException("表达式包含非法关键字: " + bad);
            }
        }
    }
}
