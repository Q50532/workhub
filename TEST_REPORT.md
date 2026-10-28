# WorkHub + AI Assistant integrated candidates — verification record (2026-10-27)

## Traceability

WorkHub input: `workhub_v2_plus_20261027.zip` (already contained phase1 + concurrency + async + Redis Lua + idempotency + SLA + bench tools). Added creation/comment/resolution audit, failed-notification manager read/retry, regression tests and safer concurrency smoke script.

AI input: user-uploaded `ai_assistant_v2_phase1(1).zip` merged with `ai_assistant_security_tools_20261027.zip`. Replaced original `agent.py` in-place to use all five read-only tools and a KB-only vector adapter; retained original KB embeddings after stripping private-ticket rows (25 -> 19), removed raw Ticket exports from packaged artifacts; added manual knowledge review workflow.

## Checks actually performed

| Check | Result |
| --- | --- |
| Python `compileall` for AI project | PASS |
| AI standard-library `unittest discover` | **13 PASS, 0 FAIL** |
| Auth-negative test: Alice forbidden Ticket | PASS (mock backend) |
| Gemini tool wrapper argument/exfiltration tests | PASS (mock model layer) |
| KB retrieval excludes historical private Ticket chunks | PASS |
| Human approval denies non-Manager | PASS (mock backend) |
| Local WorkHub URL anti-exfiltration and redirects hardening | PASS (offline checks) |
| Index / embedding array consistency after sanitization | PASS, 19 x 384 |
| Compile isolated Java 21 business rule/model classes | PASS |
| Execute isolated Java SLA/rate-rule checks | PASS |
| Both frontend JS modules `node --check` | PASS |
| Maven `pom.xml` XML validation | PASS |
| Python benchmark and concurrency scripts `py_compile` | PASS |
| ZIP CRC / file presence / private-data exclusion check | PASS |

## NOT VERIFIED — blocking final-production acceptance

| Check | Reason |
| --- | --- |
| WorkHub full Maven compile + JUnit | Maven wrapper download blocked (`wget: Failed to fetch ...apache-maven-3.9.16-bin.zip`); available Java is 21, `pom.xml` targets Java 25. This is an environment blocker, not a passing build. |
| MySQL migrations, real HTTP authorization, transactions | No user's Windows MySQL/Redis/backend access |
| Concurrent live 200+409 and idempotency behavior | Requires locally running migrated WorkHub |
| Redis Lua limit / cache races / async recovery / SLA overdue | Requires Redis + DB integration |
| Real throughput/P95/P99 | Requires measured load on user's system |
| Actual Gemini automatic function call and SentenceTransformer embedding runtime | API key/model/runtime dependencies not available in this container; offline tests use fakes |

## Outcome

**Deliverable status: release candidates.** They are source-complete to the defined scope but **not verified final builds** until Java 25/DB/Redis/Gemini checks run in the target Windows environment. Neither the original phase1 21 HTTP checks nor its 17 JUnit passes should be presented as passes for this revision. Do not overwrite the verified phase1 installation. Both ZIPs contain source and test instructions; secrets are not bundled.
