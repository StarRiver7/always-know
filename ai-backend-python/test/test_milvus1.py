import sys
sys.path.insert(0, '.')

from pymilvus import connections, utility
import time

print("Testing Milvus connection...")
try:
    start = time.time()
    connections.connect(
        alias="default",
        host="localhost",
        port="19530",
        timeout=30
    )
    elapsed = time.time() - start
    print(f"Connected to Milvus in {elapsed:.2f}s")
    
    collections = utility.list_collections()
    print(f"Collections: {collections}")
    
    connections.disconnect("default")
    print("Test passed!")
except Exception as e:
    elapsed = time.time() - start
    print(f"Failed after {elapsed:.2f}s: {type(e).__name__}: {e}")
    import traceback
    traceback.print_exc()