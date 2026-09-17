# Phase 8 — Tavily Web Search

## Implementation checklist

- [x] Server-side Tavily client
- [x] Secret-safe API-key configuration through environment variables
- [x] HTTP web-search endpoint
- [x] HTTP extract endpoint
- [x] Bounded research workflow with follow-up queries
- [x] Source metadata normalization for citations
- [x] Retry handling for rate-limit/server failures
- [x] Request-size and result-count bounds
- [x] Optional backend bearer-token guard
- [x] Android web-search client
- [x] `web.tavily_search` tool contract
- [x] `web.research` tool contract
- [x] CI unit verification for the web-search server
- [ ] Production backend deployment
- [ ] Runtime end-to-end Tavily credential verification
- [ ] Chat UI citation rendering integration

## Server contract

The backend exposes:

- `GET /health`
- `POST /v1/web/search`
- `POST /v1/web/extract`
- `POST /v1/web/research`

The backend owns the Tavily API key. The Android application never receives or stores the Tavily key.

`VYNNRA_SERVER_TOKEN` can be configured as a server-side bearer guard for deployments that need an additional trusted-caller boundary. A real user-authentication layer can replace or extend this guard later.

## Search behavior

Search requests are bounded before they reach Tavily. `maxResults` is capped at 10, domain lists are capped at 20 entries, and research follow-up queries are capped at three additional queries.

Retryable failures include HTTP 429 and server-side 5xx responses. Retries are bounded and use a small backoff. Request timeouts are converted into retryable service errors.

The response preserves source title, URL, snippet/content, score and favicon metadata so the application can render citations without exposing provider credentials.

## Android contract

`WebSearchClient` calls the Vynnra search service rather than Tavily directly. `TavilySearchTool` and `WebResearchTool` require the `WEB_SEARCH` capability before the Orchestrator can execute them.

The server URL and optional caller token are injected at runtime; they are not hardcoded into the source.

## Verification boundary

CI currently verifies the server request normalization, secret-missing behavior, bearer header construction, research deduplication, server test execution, and Android APK compilation. Real Tavily requests and deployment behavior require a configured server secret in a non-source-control environment.
