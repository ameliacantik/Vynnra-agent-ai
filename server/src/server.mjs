import http from 'node:http';
import { TavilyClient, TavilyError } from './tavilyClient.mjs';
import { runResearch } from './research.mjs';

const port = Number(process.env.PORT || 8787);
const serverToken = process.env.VYNNRA_SERVER_TOKEN?.trim() || '';
const client = new TavilyClient();

const server = http.createServer(async (req, res) => {
  setJsonHeaders(res);

  if (req.method === 'GET' && req.url === '/health') {
    return send(res, 200, {
      ok: true,
      service: 'vynnra-web-search',
      tavilyConfigured: Boolean(process.env.TAVILY_API_KEY?.trim())
    });
  }

  if (req.method !== 'POST') return send(res, 405, { error: 'Method not allowed' });

  if (serverToken && !isAuthorized(req, serverToken)) {
    return send(res, 401, { error: 'Unauthorized' });
  }

  if (!['/v1/web/search', '/v1/web/extract', '/v1/web/research'].includes(req.url)) {
    return send(res, 404, { error: 'Not found' });
  }

  try {
    const body = await readJson(req);
    let result;
    if (req.url === '/v1/web/search') {
      result = await withRetry(() => client.search(body), 2);
    } else if (req.url === '/v1/web/extract') {
      result = await withRetry(() => client.extract(body.urls), 2);
    } else {
      result = await withRetry(() => runResearch(client, body), 1);
    }
    return send(res, 200, { ok: true, data: sanitizePayload(result) });
  } catch (error) {
    const status = error instanceof TavilyError ? error.status : 500;
    const publicMessage = error instanceof TavilyError ? error.message : 'Web search service failed.';
    return send(res, status, {
      ok: false,
      error: publicMessage,
      retryable: Boolean(error?.retryable)
    });
  }
});

server.listen(port, '0.0.0.0', () => {
  console.log(`Vynnra web-search server listening on :${port}`);
});

function setJsonHeaders(res) {
  res.setHeader('Content-Type', 'application/json; charset=utf-8');
  res.setHeader('Cache-Control', 'no-store');
}

function send(res, status, body) {
  res.writeHead(status);
  res.end(JSON.stringify(body));
}

async function readJson(req) {
  const chunks = [];
  let size = 0;
  for await (const chunk of req) {
    size += chunk.length;
    if (size > 256 * 1024) throw new TavilyError('Request body is too large.', { status: 413 });
    chunks.push(chunk);
  }
  const text = Buffer.concat(chunks).toString('utf8');
  if (!text) return {};
  try {
    return JSON.parse(text);
  } catch {
    throw new TavilyError('Invalid JSON request body.', { status: 400 });
  }
}

function isAuthorized(req, token) {
  const header = req.headers.authorization || '';
  return header === `Bearer ${token}`;
}

async function withRetry(operation, attempts) {
  let lastError;
  for (let attempt = 0; attempt <= attempts; attempt += 1) {
    try {
      return await operation();
    } catch (error) {
      lastError = error;
      if (!error?.retryable || attempt === attempts) throw error;
      await new Promise((resolve) => setTimeout(resolve, 250 * (attempt + 1)));
    }
  }
  throw lastError;
}

function sanitizePayload(payload) {
  if (!payload || typeof payload !== 'object') return payload;
  return JSON.parse(JSON.stringify(payload, (_key, value) => {
    if (typeof value === 'string' && value.length > 100_000) return value.slice(0, 100_000);
    return value;
  }));
}
