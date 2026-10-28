# WorkHub V2 read-only/non-mutating HTTP smoke tests.
# Run with the existing Spring Boot instance running on port 8080.
# No database rows are intentionally changed.
param(
    [int]$TicketId = 16,
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"
$BaseUrl = $BaseUrl.TrimEnd('/')
$passed = 0
$failed = 0

function New-Account([string]$Username, [string]$Password) {
    $basic = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("${Username}:$Password"))
    return @{
        Username = $Username
        Headers = @{ Authorization = "Basic $basic"; Accept = "application/json" }
        Session = (New-Object Microsoft.PowerShell.Commands.WebRequestSession)
        Csrf = $null
    }
}

function Invoke-Api([string]$Verb, [string]$Path, [hashtable]$Account, [string]$Body = $null, [string]$ContentType = $null) {
    $headers = @{}
    foreach ($key in $Account.Headers.Keys) { $headers[$key] = $Account.Headers[$key] }

    # CSRF must be present, or a 403 on a PUT might come from CSRF rather than permission checks.
    if ($Verb -in @('POST', 'PUT', 'PATCH', 'DELETE')) {
        if ($null -eq $Account.Csrf) {
            $tokenResponse = Invoke-WebRequest -Method Get -Uri "$BaseUrl/csrf" `
                -Headers $Account.Headers -WebSession $Account.Session -UseBasicParsing -ErrorAction Stop
            $token = $tokenResponse.Content | ConvertFrom-Json
            if (-not $token.headerName -or -not $token.token) {
                throw "Cannot obtain CSRF token for $($Account.Username)"
            }
            $Account.Csrf = $token
        }
        $csrfHeader = [string]$Account.Csrf.headerName
        $csrfValue = [string]$Account.Csrf.token
        $headers[$csrfHeader] = $csrfValue
    }

    $request = @{
        Method = $Verb
        Uri = "$BaseUrl$Path"
        Headers = $headers
        WebSession = $Account.Session
        UseBasicParsing = $true
        ErrorAction = 'Stop'
        TimeoutSec = 15
    }
    # Windows PowerShell 5.1 may convert an omitted [string] argument to "".
    # Passing even an empty Body to a GET request causes ProtocolViolationException.
    if ($Verb -notin @('GET', 'HEAD') -and -not [string]::IsNullOrEmpty($Body)) {
        $request.Body = $Body
    }
    if ($ContentType) { $request.ContentType = $ContentType }

    try {
        $result = Invoke-WebRequest @request
        return [pscustomobject]@{ Status = [int]$result.StatusCode; Body = [string]$result.Content }
    } catch {
        if ($null -eq $_.Exception.Response) { throw }
        return [pscustomobject]@{
            Status = [int]$_.Exception.Response.StatusCode
            Body = [string]$_.ErrorDetails.Message
        }
    }
}

function Assert-Status([string]$Name, [string]$Verb, [string]$Path, [hashtable]$Account, [int]$Expected, [string]$Body = $null, [string]$ContentType = $null) {
    $r = Invoke-Api -Verb $Verb -Path $Path -Account $Account -Body $Body -ContentType $ContentType
    if ($r.Status -eq $Expected) {
        $script:passed++
        Write-Host ("[PASS] {0}: HTTP {1}" -f $Name, $r.Status) -ForegroundColor Green
    } else {
        $script:failed++
        Write-Host ("[FAIL] {0}: expected {1}, got {2}" -f $Name, $Expected, $r.Status) -ForegroundColor Red
        if ($r.Body) {
            $short = $r.Body -replace '\s+', ' '
            if ($short.Length -gt 160) { $short = $short.Substring(0, 160) + '...' }
            Write-Host "       $short"
        }
    }
    return $r
}

function Assert-Role([string]$Name, [pscustomobject]$MeResponse, [string]$ExpectedRole) {
    if ($MeResponse.Status -ne 200) { return }
    try {
        $me = $MeResponse.Body | ConvertFrom-Json
        $actual = [string]$me.role
    } catch {
        $actual = '<invalid JSON>'
    }
    if ($actual -ceq $ExpectedRole) {
        $script:passed++
        Write-Host "[PASS] $Name : $actual" -ForegroundColor Green
    } else {
        $script:failed++
        Write-Host "[FAIL] $Name : expected $ExpectedRole, got $actual" -ForegroundColor Red
    }
}

$secureTom = Read-Host 'Tom password (not echoed)' -AsSecureString
$tomPassword = [System.Net.NetworkCredential]::new('', $secureTom).Password
$secureAlice = Read-Host 'Alice password (not echoed)' -AsSecureString
$alicePassword = [System.Net.NetworkCredential]::new('', $secureAlice).Password
$tom = New-Account -Username 'Tom' -Password $tomPassword
$alice = New-Account -Username 'Alice' -Password $alicePassword
$badTom = New-Account -Username 'Tom' -Password "${tomPassword}_invalid_for_test"
$anonymous = @{ Username = 'anonymous'; Headers = @{ Accept = 'application/json' }; Session = (New-Object Microsoft.PowerShell.Commands.WebRequestSession); Csrf = $null }
$alicePassword = $null
$tomPassword = $null

Write-Host "`nWorkHub V2 HTTP smoke tests - Ticket #$TicketId" -ForegroundColor Cyan
Write-Host 'All checks are intended not to modify existing Tickets.'

[void](Assert-Status 'Unauthenticated /api/me' 'GET' '/api/me' $anonymous 401)
[void](Assert-Status 'Incorrect Tom password' 'GET' '/api/me' $badTom 401)
$tomMe = Assert-Status 'Tom login' 'GET' '/api/me' $tom 200
$aliceMe = Assert-Status 'Alice login' 'GET' '/api/me' $alice 200
Assert-Role 'Tom role' $tomMe 'MANAGER'
Assert-Role 'Alice role' $aliceMe 'USER'

[void](Assert-Status 'Manager can view Ticket' 'GET' "/tickets/$TicketId" $tom 200)
[void](Assert-Status 'USER cannot view private Ticket' 'GET' "/tickets/$TicketId" $alice 403)
[void](Assert-Status 'Manager can read comments' 'GET' "/tickets/$TicketId/comments" $tom 200)
[void](Assert-Status 'USER cannot read private comments' 'GET' "/tickets/$TicketId/comments" $alice 403)
[void](Assert-Status 'Manager can read logs' 'GET' "/tickets/$TicketId/logs" $tom 200)
[void](Assert-Status 'USER cannot read private logs' 'GET' "/tickets/$TicketId/logs" $alice 403)
[void](Assert-Status 'Manager can read audit' 'GET' "/tickets/$TicketId/audit" $tom 200)
[void](Assert-Status 'USER cannot read private audit' 'GET' "/tickets/$TicketId/audit" $alice 403)
[void](Assert-Status 'Manager can list Tickets' 'GET' '/tickets?page=1&size=10' $tom 200)
[void](Assert-Status 'USER can list accessible Tickets' 'GET' '/tickets?page=1&size=10' $alice 200)
[void](Assert-Status 'Invalid pagination rejected' 'GET' '/tickets?page=0&size=10' $tom 400)
[void](Assert-Status 'Invalid sorting rejected' 'GET' '/tickets?sortBy=invalid' $tom 400)

# A guaranteed-to-be-absent ID lets us exercise write authorization without changing data.
$missingId = 2147483647
$missing = Assert-Status 'Absent Ticket returns 404' 'GET' "/tickets/$missingId" $tom 404
if ($missing.Status -eq 404) {
    [void](Assert-Status 'USER cannot change priority (CSRF supplied)' 'PUT' "/tickets/$missingId/priority" $alice 403 'HIGH' 'text/plain')
    [void](Assert-Status 'MANAGER write requires If-Match' 'PUT' "/tickets/$missingId/priority" $tom 428 'HIGH' 'text/plain')
    [void](Assert-Status 'USER cannot assign Tickets (CSRF supplied)' 'PUT' "/tickets/$missingId/assignee" $alice 403 '2' 'application/json')
    [void](Assert-Status 'MANAGER assignment requires If-Match' 'PUT' "/tickets/$missingId/assignee" $tom 428 '2' 'application/json')
} else {
    Write-Host '[SKIP] Write-path checks: sentinel ID is not confirmed absent.' -ForegroundColor Yellow
}

Write-Host "`nResult: $passed PASS / $failed FAIL" -ForegroundColor Cyan
if ($failed -gt 0) { exit 1 }
exit 0
