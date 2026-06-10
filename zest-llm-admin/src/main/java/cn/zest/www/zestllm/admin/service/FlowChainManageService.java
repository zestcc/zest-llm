package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.vo.FlowChainVO;
import cn.zest.www.zestllm.admin.repo.LlmFlowChainRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FlowChainManageService {

    private final LlmFlowChainRepo flowChainRepo;

    public List<FlowChainVO> listActive() {
        return flowChainRepo.findAllActive().stream()
                .map(row -> FlowChainVO.builder()
                        .id(row.getId())
                        .chainCode(row.getChainCode())
                        .chainName(row.getChainName())
                        .version(row.getVersion())
                        .lifecycle(row.getLifecycle())
                        .status(row.getStatus())
                        .updatedAt(row.getUpdatedAt())
                        .build())
                .toList();
    }

    public FlowChainVO getByCode(String chainCode) {
        return flowChainRepo.findByChainCode(chainCode)
                .map(row -> FlowChainVO.builder()
                        .id(row.getId())
                        .chainCode(row.getChainCode())
                        .chainName(row.getChainName())
                        .version(row.getVersion())
                        .lifecycle(row.getLifecycle())
                        .chainData(row.getChainData())
                        .status(row.getStatus())
                        .updatedAt(row.getUpdatedAt())
                        .build())
                .orElseThrow(() -> new BusinessException("FLOW_CHAIN_NOT_FOUND", "链不存在: " + chainCode));
    }
}
