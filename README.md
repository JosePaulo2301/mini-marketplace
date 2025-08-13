# Visão Geral

Um projeto completo para portfólio que demonstra domínio de **Java 17 + Spring Boot**, **arquitetura de microservices**, **API REST**, **mensageria** (usando **Azure Service Bus** para simular SQS e **Service Bus Topics**/**Event Grid** para simular SNS), **containerização** com **Docker**, **orquestração** com **Kubernetes (AKS)** e **CI/CD** (GitHub Actions + Azure). Inclui **IaC (Terraform)**, **observabilidade (OpenTelemetry/Prometheus/Grafana/Azure Monitor)**, **segurança (Azure AD OIDC + Key Vault)** e **boas práticas** (idempotência, retries, DLQ, contratos de eventos, testes).

---

## Domínio do Projeto: "OrderHub"

**Cenário:** um mini marketplace com fluxo de **Pedidos → Pagamentos → Estoque → Notificações**.

* **order-service** (REST + Saga/Outbox): cria e gerencia pedidos.
* **payment-service**: processa pagamentos (mock) e publica eventos.
* **inventory-service**: reserva/baixa estoque.
* **notification-service**: envia e-mails/push (mock) para o cliente.
* **api-gateway**: roteia tráfego externo para os serviços.
* **auth-service** (opcional): integração OIDC com Azure AD (ou Keycloak local).

**Comunicação:**

* **Síncrona**: REST via **Spring Cloud Gateway**.
* **Assíncrona**: Azure **Service Bus Queues** (≃ SQS) e **Service Bus Topics**/**Event Grid** (≃ SNS) com **DLQ**.

**Banco de Dados:**

* **PostgreSQL** por serviço (próprio schema). **Flyway** para migrações.

**Cache:**

* **Redis** para cache de consultas e locks breves de idempotência.

**Armazenamento:**

* **Azure Blob Storage** para anexos de pedido (ex.: comprovantes).

---

## Arquitetura (alto nível)

```
[Client] → [API Gateway] → (REST) → [order-service]
                                    ↓ (event: order.created)
                              [Service Bus Topic: orders]
                                    ↙               ↘
                         [inventory-service]     [payment-service]
                               ↓ (event)              ↓ (event)
                        [order.inventory-*]     [order.payment-*]
                                    ↘               ↙
                               [notification-service]
```

**Equivalências AWS → Azure:**

* **SQS → Azure Service Bus Queue**
* **SNS → Azure Service Bus Topic + Subscriptions** (ou **Event Grid** conforme o caso)
* **EKS/ECR → AKS/ACR**
* **SSM/Secrets → Key Vault**

---

## Stack Técnica

* **Linguagem/Framework**: Java 17, Spring Boot 3.x, Spring Web, Spring Data JPA, Spring Cloud Stream (Azure Binder), Spring Cloud Gateway, Spring Security (OIDC), Spring Retry, Resilience4j.
* **Mensageria**: `spring-cloud-azure-starter-servicebus` (Queues/Topics + DLQ).
* **Banco**: PostgreSQL + Flyway.
* **Build**: Maven.
* **Testes**: JUnit 5, Testcontainers (Postgres + Service Bus emulator via container), WireMock.
* **Container**: Dockerfile multi-stage.
* **Orquestração**: Kubernetes (AKS), manifests/Helm + KEDA (autoescalonamento por mensagens Service Bus).
* **CI/CD**: GitHub Actions (CI) + Deploy em AKS (CD) com OIDC federated credentials.
* **IaC**: Terraform para ACR, AKS, Service Bus, PostgreSQL Flexible Server, Redis, Key Vault, Storage, Log Analytics.
* **Observabilidade**: OpenTelemetry + Prometheus + Grafana, logs para Azure Monitor/Log Analytics, Zipkin/Tempo (opcional).

---

## Estrutura de Repositório (monorepo)

```
orderhub/
  apps/
    api-gateway/
    order-service/
    payment-service/
    inventory-service/
    notification-service/
    auth-service/ (opcional)
  libs/
    common-models/
    common-messaging/
  deploy/
    helm/
      api-gateway/
      order-service/
      payment-service/
      inventory-service/
      notification-service/
    k8s/ (manifests simples para dev)
  infra/
    terraform/
      environments/
        dev/
        prod/
  .github/workflows/
  docker/
  docs/
  README.md
```

---

## Modelos e Contratos

**Entidade Pedido (order-service):**

```java
public record OrderDTO(UUID id, String customerEmail, BigDecimal total,
                       List<OrderItemDTO> items, String status, String idempotencyKey) {}
```

**Evento (orders.topic – `order.created.v1`):**

```json
{
  "eventType": "order.created.v1",
  "eventId": "<uuid>",
  "timestamp": "2025-08-13T15:00:00Z",
  "data": {
    "orderId": "<uuid>",
    "total": 199.90,
    "items": [{"sku":"ABC-123","qty":2}],
    "customerEmail":"user@example.com"
  },
  "traceId": "<w3c-trace-context>"
}
```

**Padrões:** idempotência por `Idempotency-Key` (header), **Outbox Pattern** (tabela `outbox_events`), **DLQ** com reprocessamento manual/automático, **dead-letter reasons**.

---

## Endpoints Principais

* `POST /orders` – cria pedido (idempotente).
* `GET /orders/{id}` – consulta pedido.
* `POST /orders/{id}/confirm` – confirma pagamento (simulado) → publica `order.paid.v1`.
* `GET /inventory/{sku}` – consulta estoque.
* `POST /inventory/reserve` – reserva estoque.
* `POST /notifications/test` – dispara notificação mock.

**Gateway (Spring Cloud Gateway)**

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: order-service
          uri: http://order-service:8080
          predicates: [ Path=/orders/** ]
        - id: inventory-service
          uri: http://inventory-service:8080
          predicates: [ Path=/inventory/** ]
```

---

## Dependências (exemplo `pom.xml` – order-service)

```xml
<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
  </dependency>
  <dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
  </dependency>
  <dependency>
    <groupId>com.azure.spring</groupId>
    <artifactId>spring-cloud-azure-starter-servicebus</artifactId>
  </dependency>
  <dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
  </dependency>
  <dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
  </dependency>
</dependencies>
```

---

## Configuração Azure (simulando SQS/SNS)

**Service Bus**

* `orders.topic`

  * Subscriptions: `inventory-sub`, `payment-sub`, `notification-sub`
* Filas (Queues) por serviço: `inventory-queue`, `payment-queue`, `notification-queue`
* **DLQ** habilitado por padrão; política de reentrega com `maxDeliveryCount` (ex.: 5).

**application.yaml** (order-service)

```yaml
spring:
  cloud:
    azure:
      servicebus:
        namespace: ${SB_NAMESPACE}
        entity-type: topic
        entity-name: orders
  messaging:
    consumer:
      concurrency: 3
```

**Listener (exemplo)**

```java
@Component
public class PaymentListener {
  @ServiceBusListener(destination = "orders", subscription = "payment-sub")
  public void onMessage(String eventJson) {
    // parse, processa pagamento, publica order.paid.v1 ou envia para DLQ se falhar
  }
}
```

---

## Docker e Kubernetes

**Dockerfile (multi-stage)**

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
```

**Helm values.yaml (trecho)**

```yaml
image:
  repository: <ACR_LOGIN>/order-service
  tag: "1.0.0"
  pullPolicy: IfNotPresent

env:
  - name: SPRING_PROFILES_ACTIVE
    value: prod
  - name: SB_NAMESPACE
    valueFrom:
      secretKeyRef:
        name: sb-secrets
        key: namespace
```

**KEDA (Auto Scale por mensagens Service Bus)**

```yaml
apiVersion: keda.sh/v1alpha1
kind: ScaledObject
metadata:
  name: payment-service-scaler
spec:
  scaleTargetRef:
    name: payment-service
  triggers:
    - type: azure-servicebus
      metadata:
        queueName: payment-queue
        namespace: ${SB_NAMESPACE}
        messageCount: "50"
```

---

## CI/CD (GitHub Actions)

**CI – build, testes, imagem e push para ACR**

```yaml
name: ci
on: [push]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17
      - run: mvn -B -DskipTests=false test package
      - name: Login ACR
        uses: azure/docker-login@v1
        with:
          login-server: ${{ secrets.ACR_LOGIN_SERVER }}
          username: ${{ secrets.ACR_USERNAME }}
          password: ${{ secrets.ACR_PASSWORD }}
      - name: Build & Push
        run: |
          docker build -t ${{ secrets.ACR_LOGIN_SERVER }}/order-service:${{ github.sha }} apps/order-service
          docker push ${{ secrets.ACR_LOGIN_SERVER }}/order-service:${{ github.sha }}
```

**CD – deploy em AKS com OIDC (sem secrets long-lived)**

```yaml
name: cd-order-service
on:
  workflow_run:
    workflows: ["ci"]
    types: ["completed"]
jobs:
  deploy:
    permissions:
      id-token: write
      contents: read
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: azure/login@v2
        with:
          client-id: ${{ secrets.AZURE_CLIENT_ID }}
          tenant-id: ${{ secrets.AZURE_TENANT_ID }}
          subscription-id: ${{ secrets.AZURE_SUBSCRIPTION_ID }}
      - uses: azure/aks-set-context@v3
        with:
          resource-group: ${{ secrets.AKS_RG }}
          cluster-name: ${{ secrets.AKS_NAME }}
      - name: Helm upgrade
        run: |
          helm upgrade --install order-service deploy/helm/order-service \
            --set image.repository=${{ secrets.ACR_LOGIN_SERVER }}/order-service \
            --set image.tag=${{ github.sha }}
```

---

## Infraestrutura como Código (Terraform)

**Recursos:** ACR, AKS, Service Bus (namespace, queues, topics, subscriptions), PostgreSQL Flexible, Redis, Key Vault, Storage, Log Analytics, identidade gerenciada.

**Exemplo – Service Bus Topic**

```hcl
resource "azurerm_servicebus_topic" "orders" {
  name                = "orders"
  namespace_id        = azurerm_servicebus_namespace.sb.id
  enable_partitioning = true
}

resource "azurerm_servicebus_subscription" "payment_sub" {
  name               = "payment-sub"
  topic_id           = azurerm_servicebus_topic.orders.id
  max_delivery_count = 5
}
```

---

## Segurança

* **OIDC Azure AD** pelo Gateway; roles via **JWT** (scopes/claims).
* **Key Vault** para segredos (connection strings), acessados por **Managed Identity**.
* **Políticas**: TLS, `NetworkPolicy` no AKS, NSG restrito, portas 443/22/3306 conforme necessidade.

---

## Observabilidade

* **Tracing**: OpenTelemetry (OTLP) → Azure Monitor/Jaeger/Tempo.
* **Métricas**: Micrometer/Prometheus → Grafana.
* **Logs**: Serilog/Logback JSON → Log Analytics + correlação por `traceId`.

---

## Testes

* **Unitários** (JUnit 5), **contrato** (Spring Cloud Contract), **integração** (Testcontainers Postgres + Service Bus emulator), **carga** (k6/Locust – opcional).
* **Chaos**: fault injection básico com Resilience4j para timeouts e retries.

---

## Roteiro de Entrega (Roadmap)

1. **MVP**: order-service (REST), Postgres, Docker, CI → ACR.
2. Mensageria: publicar `order.created.v1` no Topic, criar `payment-service` consumidor.
3. KEDA + DLQ + retries + idempotência (Outbox + `Idempotency-Key`).
4. API Gateway + OIDC + Key Vault.
5. Observabilidade (OTEL + Prometheus + Grafana) + dashboards.
6. Infra Terraform + ambientes dev/prod.
7. Inventory/Notification services + Event-driven end-to-end.
8. CD automático para AKS.

---

## Scripts Úteis

**docker-compose (dev local)**

```yaml
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_PASSWORD: postgres
    ports: ["5432:5432"]
  redis:
    image: redis:7
    ports: ["6379:6379"]
```

**Makefile (atalhos)**

```makefile
build:
	mvn -q -DskipTests package

docker:
	docker build -t order-service:dev apps/order-service

run:
	docker run -p 8080:8080 --env-file .env order-service:dev
```

---

## Diferenciais de Portfólio para destacar no README

* **Padrões avançados**: Saga/Outbox, KEDA auto-scaling por fila, políticas de retry/backoff, DLQ com reprocessamento.
* **Security by default**: OIDC, Secrets via Key Vault, supply-chain (SBOM e assinaturas de imagem – Cosign).
* **Observabilidade de ponta a ponta** com rastreamento distribuído.
* **IaC completo** com ambientes dev/prod e permissões mínimas.

---

## Próximos Passos

* Inicializar monorepo e publicar README com diagrama.
* Criar `order-service` com `POST /orders` idempotente + Flyway.
* Provisionar ACR/AKS/Service Bus via Terraform (ambiente dev).
* Habilitar CI (build, test, push imagem) e CD (deploy helm) do \`order-service
