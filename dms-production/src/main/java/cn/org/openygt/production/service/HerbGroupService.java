package cn.org.openygt.production.service;

import cn.org.openygt.production.dto.HerbGroupDTO;

import java.util.List;

public interface HerbGroupService {

    List<HerbGroupDTO> getHerbGroupsByPrescription(Long prescriptionId);

    HerbGroupDTO confirmHerbGroup(Long groupId, Long operatorId, Long deviceId);
}
