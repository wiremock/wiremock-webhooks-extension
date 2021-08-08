package org.wiremock.webhooks;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

import java.util.List;

public class WebhookDefinitionContainer {
    private List<WebhookDefinition> webhooks;

    @JsonCreator
    public WebhookDefinitionContainer(@JsonProperty("webhooks") List<WebhookDefinition> webhooks) {
        this.webhooks = webhooks;
    }

    public List<WebhookDefinition> getWebhooks() {
        return ImmutableList.copyOf(webhooks);
    }
}
