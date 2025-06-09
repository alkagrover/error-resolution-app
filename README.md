# error-resolution-app
Error resolution RAG based app

Let's you save known issues with their resolutions in the MongoDB's Altas vector store and retrieve the resolution for the saved issue. If the issue is not found in the vector DB, it fetches possible resolutions from the LLM.

NOTE: In application-dev.properties, replace <openai-api-key> with your openai api key and <mongodb-atlas-connection-string> with mongodb connection string.
