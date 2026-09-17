import test from 'node:test';
import assert from 'node:assert/strict';
import { TavilyClient, TavilyError, normalizeSearchRequest } from '../src/tavilyClient.mjs';
import { runResearch } from '../src/research.mjs';

test('normalizes search requests with bounded values', () => {
  const payload = normalizeSearchRequest({
    query: '  Vynnra  ',
    maxResults: 999,
    chunksPerSource: 0,
    searchDepth: 'advanced',
    topic: 'news',
    includeDomains: [' example.com ', '']
  });

  assert.equal(payload.query, 'Vynnra');
  assert.equal(payload.max_results, 10);
  assert.equal(payload.chunks_per_source, 1);
  assert.equal(payload.search_depth, 'advanced');
  assert.equal(payload.topic, 'news');
  assert.deepEqual(payload.include_domains, ['example.com']);
});

test('fails safely when Tavily key is missing', async () => {
  const client = new TavilyClient({ apiKey: '' });
  await assert.rejects(
    () => client.search({ query: 'test' }),
    (error) => error instanceof TavilyError && error.status === 503
  );
});

test('Tavily client sends bearer token and parses JSON', async () => {
  let captured;
  const client = new TavilyClient({
    apiKey: 'tvly-test',
    fetchImpl: async (_url, options) => {
      captured = options;
      return new Response(JSON.stringify({ results: [{ title: 'A', url: 'https://example.com' }] }), {
        status: 200,
        headers: { 'content-type': 'application/json' }
      });
    }
  });

  const result = await client.search({ query: 'hello', maxResults: 2 });
  assert.equal(captured.headers.Authorization, 'Bearer tvly-test');
  assert.equal(JSON.parse(captured.body).max_results, 2);
  assert.equal(result.results[0].title, 'A');
});

test('research workflow deduplicates sources across bounded follow-up queries', async () => {
  let callCount = 0;
  const client = {
    async search({ query }) {
      callCount += 1;
      return {
        answer: callCount === 1 ? 'summary' : null,
        results: [
          { title: 'Shared', url: 'https://example.com/shared', content: query, score: 0.9 },
          { title: `Unique ${callCount}`, url: `https://example.com/${callCount}`, content: query, score: 0.8 }
        ],
        usage: { credits: callCount }
      };
    }
  };

  const result = await runResearch(client, { query: 'first', followUpQueries: ['second', 'third', 'fourth'] });
  assert.equal(callCount, 4);
  assert.equal(result.queryCount, 4);
  assert.equal(result.answer, 'summary');
  assert.equal(result.sources.length, 5);
});
