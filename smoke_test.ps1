
$ErrorActionPreference = "Stop"

function Test-Step {
    param($Name, $ScriptBlock)
    Write-Host "=== Step: $Name ===" -ForegroundColor Cyan
    try {
        & $ScriptBlock
        Write-Host "SUCCESS" -ForegroundColor Green
    } catch {
        Write-Host "FAILED: $_" -ForegroundColor Red
        exit 1
    }
}

$base = "http://localhost:8081/api"
$u = "u$([int](Get-Random -Minimum 10000 -Maximum 99999))"
$pwd = "Passw0rd!"
$nick = "Smoke"

Test-Step "Register" {
    $body = @{username=$u; password=$pwd; nickname=$nick} | ConvertTo-Json -Compress
    $r = Invoke-WebRequest -UseBasicParsing -Method Post -Uri "$base/user/register" -ContentType "application/json" -Body $body
    Write-Host "Response: $($r.Content)"
}

$token = ""
Test-Step "Login" {
    $body = @{username=$u; password=$pwd} | ConvertTo-Json -Compress
    $r = Invoke-WebRequest -UseBasicParsing -Method Post -Uri "$base/user/login" -ContentType "application/json" -Body $body
    $json = $r.Content | ConvertFrom-Json
    $token = $json.data
    if (-not $token) { throw "No token received" }
    $global:token = $token
    Write-Host "Token received (len=$($token.Length)): $token"
}

$headers = @{Authorization="Bearer $token"}
Write-Host "Headers: $($headers | ConvertTo-Json)"

Test-Step "Get Profile" {
    $r = Invoke-WebRequest -UseBasicParsing -Method Get -Uri "$base/user/profile" -Headers $headers
    Write-Host "Profile: $($r.Content)"
}

Test-Step "Update Profile" {
    $update = @{
        city="Chengdu"
        job="Chef"
        cookAge=5
        favoriteCuisine="Sichuan"
        tastePreference="Spicy"
        dietaryRestrictions="None"
    } | ConvertTo-Json -Compress
    $r = Invoke-WebRequest -UseBasicParsing -Method Put -Uri "$base/user/profile" -Headers $headers -ContentType "application/json" -Body $update
    Write-Host "Update result: $($r.Content)"
}

$rid = 0
Test-Step "Publish Recipe" {
    $pub = @{
        title="Mapo Tofu $u"
        coverUrl="https://example.com/mapo.jpg"
        description="Spicy tofu"
        categoryId=1
        difficulty=2
        timeCost=30
        tags=@("Sichuan", "Spicy")
        steps=@(
            @{stepNo=1; desc="Cut tofu"; isKeyStep=$true},
            @{stepNo=2; desc="Stir fry"; isKeyStep=$true}
        )
    } | ConvertTo-Json -Depth 6 -Compress
    
    $r = Invoke-WebRequest -UseBasicParsing -Method Post -Uri "$base/recipe/publish" -Headers $headers -ContentType "application/json" -Body $pub
    $json = $r.Content | ConvertFrom-Json
    $rid = $json.data
    if (-not $rid) { throw "No recipe ID received" }
    $global:rid = $rid
    Write-Host "Published Recipe ID: $rid"
}

Test-Step "Check Interact Status (Initial)" {
    $r = Invoke-WebRequest -UseBasicParsing -Method Get -Uri "$base/social/interact/status?targetType=1&targetId=$rid" -Headers $headers
    Write-Host "Status: $($r.Content)"
    $json = $r.Content | ConvertFrom-Json
    if ($json.data.liked) { throw "Should not be liked yet" }
}

Test-Step "Like Recipe" {
    $r = Invoke-WebRequest -UseBasicParsing -Method Post -Uri "$base/social/interact?targetType=1&targetId=$rid&actionType=1" -Headers $headers
    Write-Host "Like result: $($r.Content)"
}

Test-Step "Check Interact Status (After Like)" {
    $r = Invoke-WebRequest -UseBasicParsing -Method Get -Uri "$base/social/interact/status?targetType=1&targetId=$rid" -Headers $headers
    Write-Host "Status: $($r.Content)"
    $json = $r.Content | ConvertFrom-Json
    if (-not $json.data.liked) { throw "Should be liked now" }
}

Test-Step "Unlike Recipe" {
    $r = Invoke-WebRequest -UseBasicParsing -Method Post -Uri "$base/social/interact?targetType=1&targetId=$rid&actionType=1" -Headers $headers
    Write-Host "Unlike result: $($r.Content)"
}

Test-Step "Check Interact Status (After Unlike)" {
    $r = Invoke-WebRequest -UseBasicParsing -Method Get -Uri "$base/social/interact/status?targetType=1&targetId=$rid" -Headers $headers
    Write-Host "Status: $($r.Content)"
    $json = $r.Content | ConvertFrom-Json
    if ($json.data.liked) { throw "Should not be liked anymore" }
}

Write-Host "ALL TESTS PASSED" -ForegroundColor Green
