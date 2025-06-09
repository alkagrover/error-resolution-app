package com.ai.hackathon.error_resolution_app.config;

import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.vectorstore.mongodb.atlas.MongoDBAtlasVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
@ComponentScan(basePackages = "com.ai.hackathon.error_resolution_app.springai")
@PropertySource("classpath:application-springai.properties")
public class SpringAiConfig {

    public static final String COLLECTION_NAME = "error_resolution_pdfs";

    @Bean("mongoDBAtlasVectorStoreForPdfs")
    public MongoDBAtlasVectorStore mongoDBAtlasVectorStoreForPdfs(MongoTemplate mongoTemplate,
                                     OpenAiEmbeddingModel openAiEmbeddingModel){
        return MongoDBAtlasVectorStore.
                builder(mongoTemplate, openAiEmbeddingModel)
                .collectionName(COLLECTION_NAME)
                .vectorIndexName("error_resolution_pdfs_vector_index")
                .pathName("embedding")
                .initializeSchema(true)
                .build();
    }

}
