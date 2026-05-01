package cn.org.openygt.production.service;

import cn.org.openygt.production.entity.Shelf;
import cn.org.openygt.production.entity.ShelfRecord;

import java.util.List;

public interface ShelfService {
    List<Shelf> listShelves(String areaCode, Integer status);
    ShelfRecord putOn(Long prescriptionId, String packageBarcode, Long shelfId, Long putOnBy);
    ShelfRecord takeOff(Long recordId, String takeOffType, Long takeOffBy);
}
