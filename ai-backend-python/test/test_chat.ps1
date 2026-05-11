$body = @{
    query = "你好"
    top_k = 5
} | ConvertTo-Json

$response = Invoke-WebRequest -Uri "http://localhost:8002/api/v1/chat/query" -Method POST -ContentType "application/json" -Body $body -TimeoutSec 120
$response.Content