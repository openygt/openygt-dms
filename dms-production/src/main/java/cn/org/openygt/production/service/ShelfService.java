package cn.org.openygt.production.service;

import cn.org.openygt.production.entity.Shelf;
import cn.org.openygt.production.entity.ShelfRecord;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

public interface ShelfService {
    List<Shelf> listShelves(String areaCode, Integer status);

    IPage<Shelf> pageShelves(String areaCode, Integer status, String keyword, int page, int size);

    Shelf createShelf(Shelf shelf);

    Shelf updateShelf(Long id, Shelf shelf);

    void deleteShelf(Long id);

    ShelfRecord putOn(Long prescriptionId, String packageBarcode, Long shelfId, Long putOnBy);
    ShelfRecord takeOff(Long recordId, String takeOffType, Long takeOffBy);
}
