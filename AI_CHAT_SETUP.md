# WorkHub AI Assistant — local integration (2026-11-02)

## What changed

WorkHub's sidebar now contains **AI Assistant** (`/ai.html`). Its POST `/api/ai/chat` endpoint delegates to the existing Python Agent through a local HTTP bridge. The Agent retains five original **read-only** functions: `search_knowledge`, `get_ticket`, `similar_tickets`, `suggest_resolution`, `suggest_priority`. Tickets are always checked by WorkHub using the **actual logged-in user**; the model cannot choose or elevate the user.

The original Spring Security/CSRF, Redis rate limiter (15 AI requests/user/minute), Ticket RBAC, MySQL, notifications and SLA are unchanged. **No schema migration.** Original CLI `agent.py` still works separately.

## Required Windows setup (two terminals)

Use a **single machine**, with WorkHub on `localhost:8080`, Python on `127.0.0.1:8765`. Do not expose port 8765 or forward it to the internet. Keep Spring Boot and Python in separate terminals.

Generate an unpredictable shared key in a PowerShell window **once**, then save it as a local User environment variable so both new terminals can read the same value:

```powershell
$key = [Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
[Environment]::SetEnvironmentVariable('WORKHUB_AGENT_SHARED_KEY', $key, 'User')
```

Open a **new** PowerShell window for the Python Agent. In the `ai-assistant` directory:

```powershell
$env:WORKHUB_AGENT_SHARED_KEY = [Environment]::GetEnvironmentVariable('WORKHUB_AGENT_SHARED_KEY', 'User')
$env:WORKHUB_URL = 'http://localhost:8080'
$env:GEMINI_API_KEY = [Environment]::GetEnvironmentVariable('GEMINI_API_KEY', 'User')
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe -m unittest discover -s tests -v
.\.venv\Scripts\python.exe chat_server.py
```

This secret is stored in the local Windows User environment; never commit it, paste it into chat, or print it in logs. If you rotate the key, restart both processes. If the `GEMINI_API_KEY` is only set in the current terminal, reuse that value instead of the User-level command. The existing SentenceTransformer model must also be installed / loadable (offline model download may require initial network access).

Open **another new** PowerShell window for Spring Boot, go to the `workhub` directory, and load that **same** key:

```powershell
$env:WORKHUB_AGENT_SHARED_KEY = [Environment]::GetEnvironmentVariable('WORKHUB_AGENT_SHARED_KEY', 'User')
$env:WORKHUB_AGENT_URL = 'http://127.0.0.1:8765/chat'
.\mvnw.cmd spring-boot:run
```

Continue to use the **existing database password environment variable** configured for your installation; the supplied candidate's `application.properties` references `DB_PASSWORD`. Other WorkHub env/DB/Redis requirements have not changed.

Open `http://localhost:8080/` and choose **AI Assistant**, or directly open `http://localhost:8080/ai.html`. Log in as Tom or Alice using the normal WorkHub login page. Java forwards the already-authenticated `JSESSIONID` cookie (or Basic auth) **only over loopback**. Python verifies the delegated session by calling `/api/me` and uses the same session for every Ticket tool request. No credentials are passed as Gemini tool arguments, stored in logs, or written into the browser response.

## Basic acceptance tests

1. Tom logs in, asks **"What is the status of Ticket #16?"**; expect `get_ticket`, an actual Ticket status and `[TICKET:16]` citation **if Ticket #16 exists and is visible to Tom**.
2. Alice asks about a Ticket she cannot access. Expect a **403** from the tool path. Do not hardcode Ticket #16 as forbidden unless it actually is.
3. Ask a WorkHub documentation question; expect `search_knowledge` and `[KB:...]` citation.
4. Temporarily stop Python; AI should show a controlled 503/timeout while normal Ticket CRUD still works.
5. Check browser DevTools Network: `POST /api/ai/chat`, CSRF token header, **no Gemini key** in browser code. Missing token should produce 403.
6. Test a user with no valid login: AI POST must not succeed. Run `py -m unittest discover -s tests -v` from the AI directory.

## Limits and safety

This is a **local integration candidate**, not a production authentication gateway. The local bridge rejects a missing/incorrect shared key and never follows WorkHub redirects. Java only accepts a configured loopback URL. Run it on trusted Windows localhost, do not expose the Python port, and deploy HTTPS plus a stronger cross-service identity/secret scheme before using multiple hosts. SQL write operations and automatic Ticket modifications are out of scope. Gemini live calls/real user permission paths require validation on your Windows machine with MySQL/Redis and an API key. No claim of live Gemini success is made by offline test results.
