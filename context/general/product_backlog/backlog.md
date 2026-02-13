## Sprint 1 Feb 11 - 2026 
### TODO
- Agent loose context many times this is because there is not defined how the data is being saved and what data is used for the prompt
making sometimes the agent start with a completily new conversation loosing all the context. 
- Being able to create recurrent jobs using WorkManager


### DONE
- Currently when user closes the app the telegram flag is lost
- When the response is too long in telegram the agent doesn't reply so user never recieves a message
- Listen for notifications so the agent can react to new notifications


## Backlog? 

- Currently we don't send chunks of data to the chat UI, we show the full response
- All conversations from chat UI are stored and displayed and given to the agent in the context
- The telegram conversations are stored and displayed and given to the agent for the context
- Give the model the capability to search on internet using Brave Search
- Improve chat UI so it looks similar to Claude/Chat GPT

