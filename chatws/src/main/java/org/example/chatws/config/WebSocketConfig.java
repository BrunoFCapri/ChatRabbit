package org.example.chatws.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry; // <- Importación faltante / corregida
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry; // <- Importación faltante
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer; // <- Corregido (Configurer)

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final String TOPIC = "/topic";
    private static final String QUEUE = "/queue";

    @Value("${spring.rabbitmq.host}")
    private String relayHost;

    @Value("${chat.stomp-relay.port}")
    private int relayPort;

    @Value("${spring.rabbitmq.username}")
    private String rabbitUser;

    @Value("${spring.rabbitmq.password}")
    private String rabbitPassword;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Antes: broker en memoria (solo sirve para una única instancia)
        // config.enableSimpleBroker(TOPIC);

        // Ahora: los mensajes de /topic y /queue se reenvían a RabbitMQ por STOMP
        config.enableStompBrokerRelay(TOPIC, QUEUE)
                .setRelayHost(relayHost)
                .setRelayPort(relayPort)
                .setClientLogin(rabbitUser)
                .setClientPasscode(rabbitPassword)
                .setSystemLogin(rabbitUser)
                .setSystemPasscode(rabbitPassword);
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").withSockJS();
    }
}
