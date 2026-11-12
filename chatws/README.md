# Chat WebSocket - Guía para Alumnos

Este proyecto implementa un sistema de chat en tiempo real utilizando **WebSocket** y el protocolo **STOMP** con Spring Boot. A continuación se explica el flujo completo de funcionamiento y los conceptos clave.

## 📝 Descripción del Proyecto

ChatWS es una aplicación de chat en tiempo real que permite a múltiples usuarios conectarse, enviar mensajes y recibir actualizaciones instantáneas sin necesidad de recargar la página. Utiliza una arquitectura basada en eventos donde los mensajes se propagan automáticamente a todos los clientes conectados.

**Tecnologías principales:**
- Spring Boot 4.1.1
- WebSocket para comunicación bidireccional
- STOMP como protocolo de mensajería
- SockJS para compatibilidad con navegadores antiguos
- Jackson para serialización JSON

---

## 🏗️ Arquitectura y Flujo

### Diagrama de Arquitectura

```
┌─────────────────┐         WebSocket          ┌──────────────────┐
│   Cliente Web   │ ◄────────────────────────► │   Servidor       │
│  (Navegador)    │     (ws://localhost:8080)  │   Spring Boot    │
└─────────────────┘                            └──────────────────┘
       │                                                 │
       │ 1. Conexión WebSocket                          │
       │ 2. Suscripción a /topic/public                 │
       │ 3. Envío a /app/chat.sendMessage               │
       │                                                 │
       │                                                 │
       │ 4. Recepción de mensajes                       │
       │    (broadcast desde /topic/public)             │
```

### Flujo Completo de un Mensaje

1. **Conexión:** El cliente web establece una conexión WebSocket con el servidor
2. **Suscripción:** El cliente se suscribe al canal `/topic/public` para recibir mensajes
3. **Envío:** El cliente envía un mensaje a `/app/chat.sendMessage`
4. **Procesamiento:** El `ChatController` recibe el mensaje
5. **Broadcast:** El mensaje se reenvía a todos los suscriptores de `/topic/public`
6. **Recepción:** Todos los clientes conectados reciben el mensaje simultáneamente


---

## 🐇 Migración a RabbitMQ

El broker en memoria (`enableSimpleBroker`) solo funciona con **una** instancia de la app: cada instancia tiene sus propios suscriptores y no ve los mensajes de las demás. Ahora los mensajes de `/topic` y `/queue` se reenvían a **RabbitMQ** con un *STOMP broker relay*, así varias instancias comparten los mensajes.

```
Cliente A ──WebSocket──► App :8080 ──STOMP :61613──┐
                                                   ├──► RabbitMQ (exchange amq.topic)
Cliente B ──WebSocket──► App :8081 ──STOMP :61613──┘
```

### Cambios realizados

| Archivo | Cambio |
|---|---|
| `pom.xml` | `spring-boot-starter-amqp` y `reactor-netty` (el relay lo usa como cliente TCP; sin él la app no arranca) |
| `WebSocketConfig.java` | `enableSimpleBroker(TOPIC)` → `enableStompBrokerRelay(TOPIC, QUEUE)` con host, puerto y credenciales |
| `application.yaml` | Conexión a RabbitMQ (`spring.rabbitmq.*`) y puerto STOMP (`chat.stomp-relay.port: 61613`) |
| `docker-compose.yml` + `rabbitmq/enabled_plugins` | RabbitMQ con el plugin **`rabbitmq_stomp`** habilitado |
| `script.js` | El campo de IP acepta `ip:puerto` para conectarse a otra instancia |

### Cómo correrlo todo con Docker

```bash
docker compose up -d --build
```

Levanta RabbitMQ y **dos instancias** de la app (imagen construida con el `Dockerfile`):

- App instancia 1: http://localhost:8080 (en el campo de IP: `localhost`)
- App instancia 2: http://localhost:8081 (en el campo de IP: `localhost:8081`)
- Panel de RabbitMQ: http://localhost:15672 (`guest` / `guest`)

Dentro de Docker la app se conecta a RabbitMQ por el nombre del servicio (`SPRING_RABBITMQ_HOST=rabbitmq`), no por `localhost`. Para bajar todo: `docker compose down`.

### Cómo correrlo sin dockerizar la app

1. Levantar solo RabbitMQ:
   ```bash
   docker compose up -d rabbitmq
   ```
   Panel de administración: http://localhost:15672 (usuario `guest`, contraseña `guest`).

2. Levantar la app:
   ```bash
   ./mvnw spring-boot:run
   ```
   Abrir http://localhost:8080.

3. **Probar la escalabilidad horizontal:** levantar una segunda instancia en otro puerto
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
   ```
   Abrir http://localhost:8081 y escribir `localhost:8081` en el campo de IP. Los mensajes enviados desde una instancia llegan a los clientes de la otra.

### Sin Docker (RabbitMQ instalado con brew/apt)

El relay habla STOMP, así que además de instalar RabbitMQ hay que habilitar el plugin:

```bash
rabbitmq-plugins enable rabbitmq_stomp
```
