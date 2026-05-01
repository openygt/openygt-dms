package cn.org.openygt.production.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.production.dto.HerbGroupConfirmRequest;
import cn.org.openygt.production.dto.HerbGroupDTO;
import cn.org.openygt.production.service.HerbGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class HerbGroupController {

    private final HerbGroupService herbGroupService;

    @GetMapping("/prescription/{prescriptionId}/herb-groups")
    public ApiResponse<List<HerbGroupDTO>> getHerbGroups(@PathVariable Long prescriptionId) {
        return ApiResponse.success(herbGroupService.getHerbGroupsByPrescription(prescriptionId));
    }

    @PostMapping("/herb-group/{groupId}/confirm")
    public ApiResponse<HerbGroupDTO> confirmHerbGroup(@PathVariable Long groupId,
                                                       @Validated @RequestBody HerbGroupConfirmRequest request) {
        return ApiResponse.success(herbGroupService.confirmHerbGroup(groupId, request.getOperatorId(), request.getDeviceId()));
    }
}
