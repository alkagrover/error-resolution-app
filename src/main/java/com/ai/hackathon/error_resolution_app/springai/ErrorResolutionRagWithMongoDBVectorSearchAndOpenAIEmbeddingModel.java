package com.ai.hackathon.error_resolution_app.springai;

import com.ai.hackathon.error_resolution_app.common.CommonHelper;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.Media;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.mongodb.atlas.MongoDBAtlasVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;


@RestController
public class ErrorResolutionRagWithMongoDBVectorSearchAndOpenAIEmbeddingModel {

    private final ChatClient chatClient;

    private final ChatClient chatClientLLM;

    private final MongoDBAtlasVectorStore mongoDBAtlasVectorStore;

    @Value("${hackathon.rag.allow-load-data-to-vector-db}")
    private String allowLoadDataToVectorDb;

    private static final String CUSTOM_USER_TEXT_ADVISE = """

			Context information is below, surrounded by ---------------------

			---------------------
			{question_answer_context}
			---------------------

			Given the context and provided history information and not prior knowledge,
			reply to the user comment. If the answer is not in the context, inform
			the user that context doesn't have answer but I can answer based on general knowledge.
			Then reply to user with general knowledge using prior knowledge
			""";


    public ErrorResolutionRagWithMongoDBVectorSearchAndOpenAIEmbeddingModel(
            @Qualifier("openAiChatModel") ChatModel openAiChatModel,
            OpenAiEmbeddingModel openAiEmbeddingModel,
            MongoDBAtlasVectorStore mongoDBAtlasVectorStore) {

        /**
         * By default, spring uses OpenAI embedding model for MongoDB Atlas vector store.
         */
        this.mongoDBAtlasVectorStore = mongoDBAtlasVectorStore;

        ChatClient.Builder chatClientBuilder = ChatClient.builder(openAiChatModel);
        ChatClient.Builder chatClientBuilderLLM = ChatClient.builder(openAiChatModel);

        /**
         * This advisor does following,
         *
         * Retrieval: Converts user's input into embeddings using "SAME" model,
         * then searches MongoDB Atlas for similar embeddings using vector search,
         * then gets actual string text for matched results.
         *
         * Augmentation: It appends the retrieved data to user's prompt as context.
         *
         * Generation: Then it continues the chain so that request goes to LLM for generation.
         *
         * Custom advise - Inform LLM how to use the data from context.
         * TopK - Maximum number of nearest neighbor documents to return
         */
        QuestionAnswerAdvisor questionAnswerAdvisor = new QuestionAnswerAdvisor(mongoDBAtlasVectorStore,
                SearchRequest.builder().topK(3).build(), CUSTOM_USER_TEXT_ADVISE);

        this.chatClient = chatClientBuilder
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(new InMemoryChatMemory())
                        , questionAnswerAdvisor
                        , new SimpleLoggerAdvisor()
                )
                .build();

        this.chatClientLLM = chatClientBuilderLLM
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(new InMemoryChatMemory())
                        , new SimpleLoggerAdvisor()
                )
                .build();
    }

    /**
     * This needs to run only once. In case of hackathon it is already done.
     * Running this multiple times will create duplicate data in MongoDB.
     *
     * Loading of data in vector database is ideally an offline process.
     * For hackathon purposes we have put in the same class & put it behind the flag.
     * Data has already been loaded in MongoDB so this code is for reference only.
     * @return
     */
    @GetMapping(CommonHelper.URL_PREFIX_FOR_SPRING + "errorResolutionRag/1.0/loadData")
    String firstTimeLoadData(@Value("${spring.ai.vectorstore.mongodb.collection-name}")
                             String collectionName) {

        try {

            /**
             * Loading of data in vector database is ideally an offline process.
             * For hackathon purposes we have put in the same class & put it behind the flag.
             * Data has already been loaded in MongoDB so this code is for reference only.
             */
            if (BooleanUtils.toBoolean(allowLoadDataToVectorDb)) {

                /**
                 * Clear all previous data to avoid duplicate data.
                 */
                ((MongoTemplate)mongoDBAtlasVectorStore.getNativeClient().get()).remove(new Query(),collectionName);

                /**
                 * In Mongo DB Atlas Vector database with OpenAI embedding model
                 */
                List<Document> documents =
                        CommonHelper.getDocuments("classpath:/incident_reports_bulk_wrapped.pdf");
                // Store data in the vector store
                mongoDBAtlasVectorStore.add(documents);
            }
        } catch (Exception e){
            e.printStackTrace();
            return "FAILED";
        }
        return "SUCCESS";
    }

    @GetMapping(CommonHelper.URL_PREFIX_FOR_SPRING + "errorResolutionRag/1.0")
    String generation(String userInput) {

        String aIResponse = this.chatClient.prompt()
                .user(userInput)
                .call()
                .content();

        return CommonHelper.surroundMessage(
                getClass(),
                userInput,
                aIResponse
        );
    }

    /**
     * Method to handle user input, perform RAG, and generate a response.
     */
    @GetMapping(CommonHelper.URL_PREFIX_FOR_SPRING + "errorResolutionRag/1.0/image/search")
    public String generation_image_search(String userInput) throws IOException {

        //read a file from classpath
        String filePath = userInput; // Example image path
        // Convert the image file to a byte array
        Media mediaFile = CommonHelper.getImageDocument(filePath);

        UserMessage userMessage = new UserMessage("Image for Vector Search", List.of(mediaFile));

        String aIResponse = this.chatClientLLM.prompt()
                .user("return the text from the image").messages(userMessage)
                .call()
                .content();
        String aIResponseUpdated = StringUtils.replace(aIResponse, "\\n", "\n");

        String searchString = StringUtils.substringBetween(aIResponseUpdated, "```", "```");

        String aIResponse2 = this.chatClient.prompt()
                .user(searchString)
                .call()
                .content();

        return CommonHelper.surroundMessage(
                getClass(),
                searchString,
                aIResponse2
        );
    }



}
