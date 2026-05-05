package cn.org.openygt.production.service.impl;

import cn.org.openygt.production.entity.Shelf;
import cn.org.openygt.production.entity.ShelfRecord;
import cn.org.openygt.production.mapper.ShelfMapper;
import cn.org.openygt.production.mapper.ShelfRecordMapper;
import cn.org.openygt.production.service.ShelfService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    public IPage<Shelf> pageShelves(String areaCode, Integer status, String keyword, int page, int size) {
        LambdaQueryWrapper<Shelf> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(areaCode)) {
            wrapper.eq(Shelf::getAreaCode, areaCode);
        }
        if (status != null) {
            wrapper.eq(Shelf::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Shelf::getShelfCode, kw)
                    .or().like(Shelf::getShelfName, kw)
                    .or().like(Shelf::getAreaName, kw));
        }
        wrapper.orderByAsc(Shelf::getAreaCode).orderByAsc(Shelf::getShelfCode);
        return shelfMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional
    public Shelf createShelf(Shelf shelf) {
        if (!StringUtils.hasText(shelf.getShelfCode())) {
            throw new IllegalArgumentException("货架编码不能为空");
        }
        Long exists = shelfMapper.selectCount(
                new LambdaQueryWrapper<Shelf>().eq(Shelf::getShelfCode, shelf.getShelfCode().trim()));
        if (exists != null && exists > 0) {
            throw new IllegalArgumentException("货架编码已存在: " + shelf.getShelfCode());
        }
        shelf.setShelfCode(shelf.getShelfCode().trim());
        if (shelf.getStatus() == null) {
            shelf.setStatus(1);
        }
        if (shelf.getCurrentCount() == null) {
            shelf.setCurrentCount(0);
        }
        if (!StringUtils.hasText(shelf.getShelfType())) {
            shelf.setShelfType("NORMAL");
        }
        shelf.setCreatedAt(LocalDateTime.now());
        shelf.setUpdatedAt(LocalDateTime.now());
        shelfMapper.insert(shelf);
        return shelf;
    }

    @Override
    @Transactional
    public Shelf updateShelf(Long id, Shelf incoming) {
        Shelf existing = shelfMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("货架不存在: " + id);
        }
        if (incoming.getCapacity() != null && existing.getCurrentCount() != null
                && incoming.getCapacity() < existing.getCurrentCount()) {
            throw new IllegalArgumentException("容量不能小于当前存放数量");
        }
        if (StringUtils.hasText(incoming.getShelfName())) {
            existing.setShelfName(incoming.getShelfName());
        }
        if (incoming.getAreaCode() != null) {
            existing.setAreaCode(incoming.getAreaCode());
        }
        if (incoming.getAreaName() != null) {
            existing.setAreaName(incoming.getAreaName());
        }
        if (incoming.getRowNum() != null) {
            existing.setRowNum(incoming.getRowNum());
        }
        if (incoming.getLayerNum() != null) {
            existing.setLayerNum(incoming.getLayerNum());
        }
        if (incoming.getCapacity() != null) {
            existing.setCapacity(incoming.getCapacity());
        }
        if (StringUtils.hasText(incoming.getShelfType())) {
            existing.setShelfType(incoming.getShelfType());
        }
        if (incoming.getStatus() != null) {
            existing.setStatus(incoming.getStatus());
        }
        existing.setUpdatedAt(LocalDateTime.now());
        shelfMapper.updateById(existing);
        return existing;
    }

    @Override
    @Transactional
    public void deleteShelf(Long id) {
        Shelf existing = shelfMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("货架不存在: " + id);
        }
        int cnt = existing.getCurrentCount() != null ? existing.getCurrentCount() : 0;
        if (cnt > 0) {
            throw new IllegalStateException("货架上仍有成品，无法删除");
        }
        shelfMapper.deleteById(id);
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

        // 幂等校验：同一药袋不能重复上架
        Long existingOnShelf = shelfRecordMapper.selectCount(
                new LambdaQueryWrapper<ShelfRecord>()
                        .eq(ShelfRecord::getPackageBarcode, packageBarcode)
                        .eq(ShelfRecord::getStatus, 1));
        if (existingOnShelf != null && existingOnShelf > 0) {
            throw new IllegalStateException("该药袋已在架上，请勿重复上架: " + packageBarcode);
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
