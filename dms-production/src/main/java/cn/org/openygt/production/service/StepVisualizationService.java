package cn.org.openygt.production.service;

import cn.org.openygt.production.dto.StepDetailDTO;
import cn.org.openygt.production.dto.StepInfoDTO;

import java.util.List;

public interface StepVisualizationService {

    List<StepInfoDTO> getTaskSteps(Long taskId);

    StepDetailDTO getStepDetail(Long taskId, String stepCode);
}
