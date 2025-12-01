# Serve APK on port 43123
$port = 43123
$apkPath = "app\build\outputs\apk\debug\app-debug.apk"

if (-not (Test-Path $apkPath)) {
    Write-Host "APK not found. Building..."
    .\gradlew.bat assembleDebug
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Build failed!"
        exit 1
    }
}

if (-not (Test-Path $apkPath)) {
    Write-Host "APK still not found at: $apkPath"
    exit 1
}

Write-Host "========================================"
Write-Host "Serving APK on port $port"
Write-Host "========================================"
Write-Host "APK: $apkPath"
Write-Host ""
Write-Host "Access at: http://localhost:$port/app-debug.apk"
Write-Host "Press Ctrl+C to stop"
Write-Host "========================================"
Write-Host ""

# Start HTTP server
$listener = New-Object System.Net.HttpListener
$listener.Prefixes.Add("http://+:$port/")
$listener.Start()

try {
    while ($listener.IsListening) {
        $context = $listener.GetContext()
        $request = $context.Request
        $response = $context.Response
        
        $localPath = $request.Url.LocalPath
        
        if ($localPath -eq "/" -or $localPath -eq "/app-debug.apk") {
            $filePath = Resolve-Path $apkPath
            $content = [System.IO.File]::ReadAllBytes($filePath)
            
            $response.ContentType = "application/vnd.android.package-archive"
            $response.ContentLength64 = $content.Length
            $response.AddHeader("Content-Disposition", "attachment; filename=app-debug.apk")
            
            $response.OutputStream.Write($content, 0, $content.Length)
            $response.StatusCode = 200
        } else {
            $response.StatusCode = 404
            $response.StatusDescription = "Not Found"
        }
        
        $response.Close()
    }
} finally {
    $listener.Stop()
}
