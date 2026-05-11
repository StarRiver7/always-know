import sys
sys.path.insert(0, '.')
from app.services.milvus_service import milvus_service
from app.core.config import settings
import asyncio

async def check():
    await milvus_service.init_connection()
    
    from pymilvus import utility, Collection
    
    # 检查集合是否存在
    has_collection = utility.has_collection(settings.MILVUS_COLLECTION_NAME)
    print(f'Collection exists: {has_collection}')
    
    if has_collection:
        collection = Collection(settings.MILVUS_COLLECTION_NAME)
        collection.load()
        print(f'Collection name: {collection.name}')
        print(f'Num entities: {collection.num_entities}')
        
        # 查看前几条数据
        if collection.num_entities > 0:
            results = collection.query(expr='id >= 0', output_fields=['id', 'document_id', 'content'], limit=5)
            print(f'Sample data:')
            for r in results:
                doc_id = r.get('document_id', 'N/A')
                content = r.get('content', '')[:50]
                print(f'  ID: {r["id"]}, DocID: {doc_id}, Content: {content}...')

asyncio.run(check())