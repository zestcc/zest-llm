package cn.zest.www.zestllm.infra.alert;

import cn.zest.www.zestllm.spi.alert.AlertWebhookAdapter;
import cn.zest.www.zestllm.spi.alert.CostAlertEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NoopAlertWebhookAdapter implements AlertWebhookAdapter {

    @Override
    public String adapterId() {
        return "noop";
    }

    @Override
    public void send(String webhookUrl, CostAlertEvent event) {
        log.trace("Noop cost alert appKey={} cost={}", event.getAppKey(), event.getDailyCost());
    }
}
