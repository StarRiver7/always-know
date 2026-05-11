import asyncio
from pymilvus import connections, utility

async def test_milvus():
    print("Testing Milvus connection...")
    try:
        connections.connect(
            alias="default",
            host="localhost",
            port="19530"
        )
        print("Connected to Milvus!")

        collections = utility.listCollections()
        print(f"Collections: {collections}")

        connections.disconnect("default")
        print("Test passed!")
    except Exception as e:
        print(f"Milvus connection failed: {e}")

if __name__ == "__main__":
    asyncio.run(test_milvus())
