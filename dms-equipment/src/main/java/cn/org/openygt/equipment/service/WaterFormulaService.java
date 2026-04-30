package cn.org.openygt.equipment.service;

import cn.org.openygt.equipment.entity.WaterFormula;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface WaterFormulaService {

    List<WaterFormula> getAllFormulas();

    WaterFormula createFormula(WaterFormula formula);

    WaterFormula updateFormula(Long id, WaterFormula formula);

    void deleteFormula(Long id);

    Map<String, Object> calculate(Long formulaId, Map<String, Object> variables);

    WaterFormula getDefaultFormula();
}
