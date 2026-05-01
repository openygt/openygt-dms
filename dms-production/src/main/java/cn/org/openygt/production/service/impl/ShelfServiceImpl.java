package cn.org.openygt.production.service.impl;

import cn.org.openygt.production.entity.Shelf;
import cn.org.openygt.production.entity.ShelfRecord;
import cn.org.openygt.production.mapper.ShelfMapper;
import cn.org.openygt.production.mapper.ShelfRecordMapper;
import cn.org.openygt.production.service.ShelfService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShelfServiceImpl implements ShelfService {

    private final ShelfMapper shelfMapper;
    private final ShelfRecordMapper shelfRecordMapper;

    @Override
    public List<Shelf> listShelves(String areaCode, Integer status) {
        LambdaQueryWrapper<Shelf> wrapper = new LambdaQueryWrapper<>();
        if (areaCode != null && !areaCode.isEmpty()) {
            wrapper.eq(Shelf::getAreaCode, areaCode);
        }
        if (status != null) {
            wrapper.eq(Shelf::getStatus, status);
        }
        wrapper.orderByAsc(Shelf::getShelfCode);
        return shelfMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public ShelfRecord putOn(Long prescriptionId, String packageBarcode, Long shelfId, Long putOnBy) {
        Shelf shelf = shelfMapper.selectById(shelfId);
        if (shelf == null) {
            throw new IllegalArgumentException("货架不存在");
        }
        if (shelf.getCapacity() != null && shelf.getCurrentCount() != null && shelf.getCurrentCount() >= shelf.getCapacity()) {
            throw new IllegalStateException("货架已满");
        }

        ShelfRecord record = new ShelfRecord();
        record.setPrescriptionId(prescriptionId);
        record.setPackageBarcode(packageBarcode);
        record.setShelfId(shelfId);
        record.setShelfCode(shelf.getShelfCode());
        record.setPutOnTime(LocalDateTime.now());
        record.setPutOnBy(putOnBy);
        record.setStatus(1);
        record.setCreatedAt(LocalDateTime.now());
        shelfRecordMapper.insert(record);

        shelf.setCurrentCount(shelf.getCurrentCount() != null ? shelf.getCurrentCount() + 1 : 1);
        shelf.setUpdatedAt(LocalDateTime.now());
        shelfMapper.updateById(shelf);
        return record;
    }

    @Override
    @Transactional
    public ShelfRecord takeOff(Long recordId, String takeOffType, Long takeOffBy) {
        ShelfRecord record = shelfRecordMapper.selectById(recordId);
        if (record == null) {
            throw new IllegalArgumentException("记录不存在");
        }
        if (record.getStatus() != null && record.getStatus() == 2) {
            throw new IllegalStateException("该记录已下架");
        }

        record.setTakeOffTime(LocalDateTime.now());
        record.setTakeOffBy(takeOffBy);
        record.setTakeOffType(takeOffType);
        record.setStatus(2);
        shelfRecordMapper.updateById(record);

        Shelf shelf = shelfMapper.selectById(record.getShelfId());
        if (shelf != null && shelf.getCurrentCount() != null && shelf.getCurrentCount() > 0) {
            shelf.setCurrentCount(shelf.getCurrentCount() - 1);
            shelf.setUpdatedAt(LocalDateTime.now());
            shelfMapper.updateById(shelf);
        }
        return record;
    }
}
