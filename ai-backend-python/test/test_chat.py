import asyncio
import httpx
import time

async def test_chat():
    async with httpx.AsyncClient(timeout=30.0) as client:
        start = time.time()
        try:
            response = await client.post(
                "http://localhost:8000/api/v1/chat/query",
                json={"query": "你好", "top_k": 5}
            )
            elapsed = time.time() - start
            print(f"Response status: {response.status_code}")
            print(f"Response time: {elapsed:.2f}s")
            print(f"Response: {response.text}")
        except Exception as e:
            elapsed = time.time() - start
            print(f"Error after {elapsed:.2f}s: {e}")

if __name__ == "__main__":
    asyncio.run(test_chat())
