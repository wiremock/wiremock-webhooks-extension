# WireMock Webhooks Extension

This library extends WireMock to add support for asynchronously making arbitrary HTTP call-outs (webhooks).
   
## Installation

### Maven

Add the following to your POM:


```xml
<dependency>
    <groupId>com.github.tomakehurst</groupId>
    <artifactId>wiremock</artifactId>
    <version>2.21.0</version>
    <scope>test</test>
</dependency>
<dependency>
    <groupId>org.wiremock</groupId>
    <artifactId>wiremock-webhooks-extension</artifactId>
    <version>1.0.1</version>
    <scope>test</test>
</dependency>
```

### Gradle

Add the following to your dependencies:


```groovy
testCompile 'com.github.tomakehurst:wiremock:2.21.0'
testCompile 'org.wiremock:wiremock-webhooks-extension:1.0.2'
```

## Using in your project

### Java

When constructing the WireMock JUnit rule or server, add a `Webhooks` instance as an extension:
 
```java
@Rule
public WireMockRule rule = new WireMockRule(
    options()
        .dynamicPort()
        .extensions(webhooks));
```

Then use the DSL provided by the extension to configure webhooks to respond when specific stubs are hit:


```java
import static org.wiremock.webhooks.Webhooks.webhook;

...

rule.stubFor(post(urlPathEqualTo("/something-async"))
    .willReturn(aResponse().withStatus(200))
    .withPostServeAction("webhook", webhook()
        .withMethod(POST)
        .withUrl("http://localhost:" + targetServer.port() + "/callback")
        .withHeader("Content-Type", "application/json")
        .withBody("{ \"result\": \"SUCCESS\" }"))
);
```

### JSON

You can also use JSON to configure webhooks:

```json
{
  "request" : {
    "urlPath" : "/something-async",
    "method" : "POST"
  },
  "response" : {
    "status" : 200
  },
  "postServeActions" : {
    "webhook" : {
      "headers" : {
        "Content-Type" : "application/json"
      },
      "method" : "POST",
      "body" : "{ \"result\": \"SUCCESS\" }",
      "url" : "http://localhost:56299/callback"
    }
  }
}
```

You can also get the JSON representation of something produced by the DSL:

```java
System.out.println(Json.write(post(urlPathEqualTo("/something"))
    .willReturn(aResponse().withStatus(200))
    .withPostServeAction("webhook", webhook()
        .withMethod(POST)
        .withUrl("http://localhost:" + targetServer.port() + "/callback")
        .withHeader("Content-Type", "application/json")
        .withBody("{ \"result\": \"SUCCESS\" }")).build()
    )
);
```

## Customising the webhook with a transformer

If you need to dynamically modify the webhook HTTP request before it is sent e.g. to add an authentication token or copy values from the original request,
you can register one or more instances of `WebhookTransformer` when you construct the extension.

```java
public class AddAuthHeaderWebhookTransformer implements WebhookTransformer {

  @Override
  public WebhookDefinition transform(ServeEvent serveEvent, WebhookDefinition webhookDefinition) {
    return webhookDefinition.withHeader("Authorization", "Token abc123");
  }
}

@Rule
public WireMockRule rule = new WireMockRule(
    options()
        .extensions(new Webhooks(new AddAuthHeaderWebhookTransformer())));
```

    