package cn.zest.www.zestllm.admin.model.request;

import lombok.Data;

import java.util.List;

@Data
public class IntegrationImportGatewayModelsRequest {
    private List<CreateGatewayModelRequest> items;
}
