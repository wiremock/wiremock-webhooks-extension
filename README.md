# WireMock Webhooks Extension

This library extends WireMock to add support for asynchronously making arbitrary HTTP call-outs (webhooks).
   
## Installation

### Maven

Add the following to your POM:


```xml
<dependency>
    <groupId>com.github.tomakehurst</groupId>
    <artifactId>wiremock</artifactId>
    <version>2.2.1</version>
    <scope>test</test>
</dependency>
<dependency>
    <groupId>org.wiremock</groupId>
    <artifactId>wiremock-webhooks-extension</artifactId>
    <version>0.0.1</version>
    <scope>test</test>
</dependency>
```

### Gradle

Add the following to your dependencies:


```groovy

testCompile 'com.github.tomakehurst:wiremock:2.2.1'
testCompile 'org.wiremock:wiremock-webhooks-extension:0.0.1'
```

## Using in your project

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