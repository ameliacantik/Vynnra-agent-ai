const TAVILY_URL = 'https://api.tavily.com/search';
const TAVILY_EXTRACT_URL = 'https://api.tavily.com/extract';

export class TavilyError extends Error {
  constructor(message, { status = 502, retryable = false, cause = undefined } = {}) {
    super(message, { cause });
    this.name = 'TavilyError';
    this.status = status;
    this.retryable = retryable;
  }
}

export class TavilyClient {
  constructor({ apiKey = process.env.TAVILY_API_KEY, fetchImpl = fetch, timeoutMs = 20_000 } = {}) {
    this.apiKey = apiKey?.trim() || '';
    this.fetchImpl = fetchImpl;
    this.timeoutMs = Math.max(1_000, timeoutMs);
  }

  async search(request) {
    this.#requireKey();
    const payload = normalizeSearchRequest(request);
    return this.#post(TAVILY_URL, payload);
  }

  async extract(urls) {
    this.#requireKey();
    const normalized = Array.isArray(urls) ? urls.filter((url) => typeof url === 'string' && /^https?:\/\//i.test(url.trim())).slice(0, 10) : [];
    if (normalized.length === 0) {
      throw new TavilyError('At least one valid HTTP(S) URL is required.', { status: 400 });
    }
    return this.#post(TAVILY_EXTRACT_URL, { urls: normalized });
  }

  async #post(url, payload) {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), this.timeoutMs);
    try {
      const response = await this.fetchImpl(url, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${this.apiKey}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload),
        signal: controller.signal
      });
      const text = await response.text();
      let body = null;
      if (text) {
        try {
          body = JSON.parse(text);
        } catch {
          body = { raw: text };
        }
      }
      if (response.ok) return body ?? {};

      const retryable = response.status === 429 || response.status >= 500;
      const message = body?.detail?.error || body?.message || `Tavily request failed with HTTP ${response.status}`;
      throw new TavilyError(message, { status: response.status, retryable });
    } catch (error) {
      if (error instanceof TavilyError) throw error;
      if (error?.name === 'AbortError') {
        throw new TavilyError('Tavily request timed out.', { status: 504, retryable: true, cause: error });
      }
      throw new TavilyError(`Tavily request failed: ${error?.message || 'unknown error'}`, {
        status: 502,
        retryable: true,
        cause: error
      });
    } finally {
      clearTimeout(timeout);
    }
  }

  #requireKey() {
    if (!this.apiKey) {
      throw new TavilyError('Tavily API key is not configured on the server.', { status: 503 });
    }
  }
}

function normalizeSearchRequest(request = {}) {
  const query = typeof request.query === 'string' ? request.query.trim() : '';
  if (!query) throw new TavilyError('Search query is required.', { status: 400 });

  const maxResults = clampInt(request.maxResults, 1, 10, 5);
  const chunksPerSource = clampInt(request.chunksPerSource, 1, 3, 3);
  const depth = request.searchDepth === 'advanced' ? 'advanced' : 'basic';
  const topic = request.topic === 'news' ? 'news' : 'general';

  return {
    query,
    search_depth: depth,
    chunks_per_source: chunksPerSource,
    max_results: maxResults,
    topic,
    time_range: normalizeOptionalString(request.timeRange),
    start_date: normalizeOptionalString(request.startDate),
    end_date: normalizeOptionalString(request.endDate),
    include_answer: Boolean(request.includeAnswer),
    include_raw_content: Boolean(request.includeRawContent),
    include_images: false,
    include_image_descriptions: false,
    include_favicon: Boolean(request.includeFavicon),
    include_domains: normalizeDomains(request.includeDomains),
    exclude_domains: normalizeDomains(request.excludeDomains),
    country: normalizeOptionalString(request.country),
    auto_parameters: Boolean(request.autoParameters),
    exact_match: Boolean(request.exactMatch),
    include_usage: Boolean(request.includeUsage),
    safe_search: Boolean(request.safeSearch)
  };
}

function normalizeDomains(value) {
  return Array.isArray(value)
    ? value.filter((item) => typeof item === 'string' && item.trim()).slice(0, 20).map((item) => item.trim())
    : [];
}

function normalizeOptionalString(value) {
  return typeof value === 'string' && value.trim() ? value.trim() : null;
}

function clampInt(value, min, max, fallback) {
  const numeric = Number(value);
  if (!Number.isFinite(numeric)) return fallback;
  return Math.min(max, Math.max(min, Math.trunc(numeric)));
}

export { normalizeSearchRequest };
