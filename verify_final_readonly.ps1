param(
    [Parameter(Mandatory=$true)][int]$TicketId,
    [string]$BaseUrl = 'http://localhost:8080'
)
$ErrorActionPreference = 'Stop'
$tomSecret = Read-Host 'Tom password' -AsSecureString
$aliceSecret = Read-Host 'Alice password' -AsSecureString
function BasicHeader($name, $secret) {
    $pwd = [System.Net.NetworkCredential]::new('', $secret).Password
    $raw = [System.Text.Encoding]::UTF8.GetBytes("${name}:$pwd")
    return @{ Authorization = 'Basic ' + [Convert]::ToBase64String($raw) }
}
$tom = BasicHeader 'Tom' $tomSecret
$alice = BasicHeader 'Alice' $aliceSecret
$pass = 0
$fail = 0
function Assert-Status($label, $path, $headers, $expect) {
    $url = $BaseUrl.TrimEnd('/') + $path
    try {
        $r = Invoke-WebRequest -Uri $url -Method Get -Headers $headers -UseBasicParsing -ErrorAction Stop
        $code = [int]$r.StatusCode
    } catch {
        if ($_.Exception.Response) { $code = [int]$_.Exception.Response.StatusCode }
        else { throw }
    }
    if ($code -eq $expect) {
        $script:pass++; Write-Host "[PASS] $label ($code)"
    } else {
        $script:fail++; Write-Host "[FAIL] ${label}: expected $expect got $code"
    }
}
Assert-Status 'Tom login' '/api/me' $tom 200
Assert-Status 'Alice login' '/api/me' $alice 200
Assert-Status 'Manager private Ticket' "/tickets/$TicketId" $tom 200
Assert-Status 'Alice blocked Ticket' "/tickets/$TicketId" $alice 403
Assert-Status 'Manager SLA' "/tickets/$TicketId/sla" $tom 200
Assert-Status 'Alice SLA denied' "/tickets/$TicketId/sla" $alice 403
Assert-Status 'Manager Audit' "/tickets/$TicketId/audit" $tom 200
Assert-Status 'Alice Audit denied' "/tickets/$TicketId/audit" $alice 403
Assert-Status 'Manager failed notifications' '/api/notifications/failed' $tom 200
Assert-Status 'Alice failed notifications denied' '/api/notifications/failed' $alice 403
Assert-Status 'Alice inbox' '/api/notifications' $alice 200
Assert-Status 'Tom inbox' '/api/notifications' $tom 200
Write-Host "Result: $pass PASS / $fail FAIL"
if ($fail -gt 0) { exit 1 }
