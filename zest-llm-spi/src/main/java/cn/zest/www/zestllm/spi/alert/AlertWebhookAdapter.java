package cn.zest.www.zestllm.spi.alert;

/**
 * FinOps 成本告警 Webhook SPI。
 */
public interface AlertWebhookAdapter {

    String adapterId();

    void send(String webhookUrl, CostAlertEvent event);
}
