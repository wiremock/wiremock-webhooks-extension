package org.wiremock.webhooks.interceptors;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.wiremock.webhooks.WebhookDefinition;

public interface WebhookInterceptor {

  WebhookDefinition intercept(ServeEvent serveEvent, WebhookDefinition webhookDefinition);
}
