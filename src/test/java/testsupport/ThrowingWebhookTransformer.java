package testsupport;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.wiremock.webhooks.WebhookDefinition;
import org.wiremock.webhooks.interceptors.WebhookTransformer;

public class ThrowingWebhookTransformer implements WebhookTransformer {

  @Override
  public WebhookDefinition intercept(ServeEvent serveEvent, WebhookDefinition webhookDefinition) {
    throw new RuntimeException("oh no");
  }
}
