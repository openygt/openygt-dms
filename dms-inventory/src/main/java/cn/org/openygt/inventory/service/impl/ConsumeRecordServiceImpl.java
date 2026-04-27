package cn.org.openygt.inventory.service.impl;

import cn.org.openygt.inventory.dto.ConsumeRecordDTO;
import cn.org.openygt.inventory.dto.ConsumeItemRequest;
import cn.org.openygt.inventory.dto.ConsumeRecordRequest;
import cn.org.openygt.inventory.entity.InvStockLog;
import cn.org.openygt.inventory.mapper.InvStockLogMapper;
import cn.org.openygt.inventory.service.ConsumeRecordService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsumeRecordServiceImpl implements ConsumeRecordService {

    private final InvStockLogMapper invStockLogMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Long> recordConsume(ConsumeRecordRequest request) {
        if (request == null || request.getTaskId() == null) {
            log.warn("消耗记录请求为空或任务ID缺失，跳过");
            return Collections.emptyList();
        }
        List<ConsumeItemRequest> items = request.getItems();
        if (CollectionUtils.isEmpty(items)) {
            log.info("任务[{}]无消耗明细，跳过流水写入", request.getTaskId());
            return Collections.emptyList();
        }
        List<Long> ids = new ArrayList<>();
        for (ConsumeItemRequest item : items) {
            try {
                InvStockLog stockLog = new InvStockLog();
                stockLog.setTaskId(request.getTaskId());
                stockLog.setMedicineId(item.getMedicineId());
                stockLog.setMedicineCode(item.getMedicineCode());
                stockLog.setMedicineName(item.getMedicineName());
                stockLog.setChangeType("CONSUME");
                stockLog.setChangeQuantity(item.getQuantity());
                stockLog.setOperatorId(request.getOperatorId());
                stockLog.setRemark(item.getRemark());
                stockLog.setTenantId("default");
                stockLog.setCreatedAt(LocalDateTime.now());
                invStockLogMapper.insert(stockLog);
                ids.add(stockLog.getId());
                log.debug("任务[{}]药材[{}]消耗记录已写入，id={}", request.getTaskId(), item.getMedicineName(), stockLog.getId());
            } catch (Exception e) {
                log.error("任务[{}]药材[{}]消耗记录写入失败，继续处理其他明细", request.getTaskId(), item.getMedicineName(), e);
            }
        }
        log.info("任务[{}]消耗记录完成：应写入{}条，实际写入{}条", request.getTaskId(), items.size(), ids.size());
        return ids;
    }

    @Override
    public List<ConsumeRecordDTO> listByTaskId(Long taskId) {
        LambdaQueryWrapper<InvStockLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InvStockLog::getTaskId, taskId).orderByDesc(InvStockLog::getCreatedAt);
        return invStockLogMapper.selectList(wrapper).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public IPage<ConsumeRecordDTO> pageQuery(Long taskId, Long medicineId, int page, int size) {
        LambdaQueryWrapper<InvStockLog> wrapper = new LambdaQueryWrapper<>();
        if (taskId != null) wrapper.eq(InvStockLog::getTaskId, taskId);
        if (medicineId != null) wrapper.eq(InvStockLog::getMedicineId, medicineId);
        wrapper.orderByDesc(InvStockLog::getCreatedAt);
        IPage<InvStockLog> entityPage = invStockLogMapper.selectPage(new Page<>(page, size), wrapper);
        return entityPage.convert(this::toDTO);
    }

    private ConsumeRecordDTO toDTO(InvStockLog log) {
        if (log == null) return null;
        ConsumeRecordDTO dto = new ConsumeRecordDTO();
        dto.setId(log.getId());
        dto.setTaskId(log.getTaskId());
        dto.setMedicineId(log.getMedicineId());
        dto.setMedicineCode(log.getMedicineCode());
        dto.setMedicineName(log.getMedicineName());
        dto.setChangeType(log.getChangeType());
        dto.setChangeQuantity(log.getChangeQuantity());
        dto.setBeforeQuantity(log.getBeforeQuantity());
        dto.setAfterQuantity(log.getAfterQuantity());
        dto.setRefNo(log.getRefNo());
        dto.setOperatorId(log.getOperatorId());
        dto.setRemark(log.getRemark());
        dto.setCreatedAt(log.getCreatedAt());
        return dto;
    }
}
