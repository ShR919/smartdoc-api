package com.smartdoc.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("You are SmartDoc, a helpful and precise document intelligence assistant.")
                .build();
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SmartDoc API")
                        .description("AI-Powered Document Intelligence Platform — RAG-based Q&A, summarization, and data extraction")
                        .version("1.0.0")
                        .contact(new Contact().name("SmartDoc").url("https://github.com/yourusername/smartdoc-api"))
                        .license(new License().name("MIT")));
    }
}
