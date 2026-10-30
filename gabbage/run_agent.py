import asyncio
import os
from dotenv import load_dotenv
from google.antigravity import Agent, LocalAgentConfig

# Načtení proměnných z .env
load_dotenv()

async def main():
    api_key = os.environ.get("GEMINI_API_KEY")
    # vertex=True a api_key jsou vyžadovány pro Express Mode klíče (začínající 'AQ.')
    config = LocalAgentConfig(api_key=api_key, vertex=True)
    async with Agent(config) as agent:
        response = await agent.chat("What files are in the current directory?")
        print(await response.text())

if __name__ == "__main__":
    asyncio.run(main())
