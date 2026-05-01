package cn.org.openygt.production.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.production.dto.PutOnRequest;
import cn.org.openygt.production.dto.TakeOffRequest;
import cn.org.openygt.production.entity.Shelf;
import cn.org.openygt.production.entity.ShelfRecord;
import cn.org.openygt.production.service.ShelfService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shelf")
@RequiredArgsConstructor
public class ShelfController {

    private final ShelfService shelfService;

    @GetMapping("/list")
    public ApiResponse<List<Shelf>> list(@RequestParam(required = false) String areaCode,
                                         @RequestParam(required = false) Integer status) {
        return ApiResponse.success(shelfService.listShelves(areaCode, status));
    }

    @PostMapping("/put-on")
    public ApiResponse<ShelfRecord> putOn(@Validated @RequestBody PutOnRequest req) {
        return ApiResponse.success(shelfService.putOn(req.getPrescriptionId(), req.getPackageBarcode(), req.getShelfId(), req.getPutOnBy()));
    }

    @PostMapping("/take-off")
    public ApiResponse<ShelfRecord> takeOff(@Validated @RequestBody TakeOffRequest req) {
        return ApiResponse.success(shelfService.takeOff(req.getRecordId(), req.getTakeOffType(), req.getTakeOffBy()));
    }
}
