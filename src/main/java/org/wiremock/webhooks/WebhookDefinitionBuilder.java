package org.wiremock.webhooks;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

public class WebhookDefinitionBuilder {
    private List<WebhookDefinition> webhooks = new ArrayList<>();

    public WebhookDefinitionBuilder addWebhook(WebhookDefinition webhookDefinition) {
        webhooks.add(webhookDefinition);
        return this;
    }

    public WebhookDefinitionContainer build() {
        return new WebhookDefinitionContainer(ImmutableList.copyOf(webhooks));
    }
}
