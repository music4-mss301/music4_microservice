# Wake up the 7 Music4 services on Render (free plan sleeps after 15 min idle, cold start 30-60 s).
# Usage: ./deploy/render/warmup.ps1                  (default prefix music4-mss301)
#        ./deploy/render/warmup.ps1 -Prefix other-prefix
# (File is ASCII-only on purpose: Windows PowerShell 5.1 misreads UTF-8 without BOM.)
# Calls /actuator/health/liveness of each service, up to 12 tries, 10 s apart.
param(
    [string]$Prefix = "music4-mss301",
    [int]$MaxTries = 12,
    [int]$SleepSeconds = 10
)

$services = "gateway", "user", "content", "engagement", "analytics", "media", "search"

# Run in parallel so all 7 services boot at the same time.
$jobs = foreach ($s in $services) {
    Start-Job -ArgumentList $s, $Prefix, $MaxTries, $SleepSeconds -ScriptBlock {
        param($s, $Prefix, $MaxTries, $SleepSeconds)
        $url = "https://$Prefix-$s.onrender.com/actuator/health/liveness"
        for ($i = 1; $i -le $MaxTries; $i++) {
            try {
                $r = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 60
                if ($r.StatusCode -eq 200) { return "[ OK ] $s (try $i)" }
            } catch {
                # service is asleep or still starting: retry
            }
            Start-Sleep -Seconds $SleepSeconds
        }
        return "[FAIL] $s not up after $($MaxTries * $SleepSeconds)s - check Logs on Render"
    }
}

$results = $jobs | Wait-Job | Receive-Job
$jobs | Remove-Job
$results | ForEach-Object { Write-Host $_ }

if ($results -match "^\[FAIL\]") {
    Write-Host "Some services are not up."
    exit 1
}
Write-Host "All 7 services are ready."
