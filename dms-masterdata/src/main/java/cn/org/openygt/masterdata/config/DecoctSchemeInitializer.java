package cn.org.openygt.masterdata.config;

import cn.org.openygt.masterdata.entity.DecoctScheme;
import cn.org.openygt.masterdata.mapper.DecoctSchemeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 煎药方案内置数据初始化。
 * MVP 阶段预置常用煎药方案，避免首次使用时空表。
 */
@Component
public class DecoctSchemeInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DecoctSchemeInitializer.class);

    private final DecoctSchemeMapper schemeMapper;

    public DecoctSchemeInitializer(DecoctSchemeMapper schemeMapper) {
        this.schemeMapper = schemeMapper;
    }

    @Override
    public void run(String... args) {
        long count = schemeMapper.selectCount(new LambdaQueryWrapper<DecoctScheme>()
                .eq(DecoctScheme::getTenantId, "default"));
        if (count > 0) {
            log.info("煎药方案已存在 {} 条，跳过内置初始化", count);
            return;
        }

        insertIfNotExists("常规煎药方案", 0, 1, 1, new BigDecimal("300.0"), 30, 0, 0);
        insertIfNotExists("浓缩煎药方案", 1, 2, 2, new BigDecimal("200.0"), 45, 5, 5);
        insertIfNotExists("儿童轻量方案", 0, 1, 1, new BigDecimal("150.0"), 20, 0, 0);

        log.info("内置煎药方案初始化完成");
    }

    private void insertIfNotExists(String name, Integer schemeType, Integer decoctTimes,
                                    Integer pressure, BigDecimal upperWater, Integer heatingTime,
                                    Integer preHeatingTime, Integer postHeatingTime) {
        DecoctScheme scheme = new DecoctScheme();
        scheme.setName(name);
        scheme.setSchemeType(schemeType);
        scheme.setDecoctTimes(decoctTimes);
        scheme.setPressure(pressure);
        scheme.setUpperWater(upperWater);
        scheme.setHeatingTime(heatingTime);
        scheme.setPreHeatingTime(preHeatingTime);
        scheme.setPostHeatingTime(postHeatingTime);
        scheme.setDescription("系统内置方案");
        schemeMapper.insert(scheme);
    }
}
