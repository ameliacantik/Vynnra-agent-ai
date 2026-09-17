import { TavilyError } from './tavilyClient.mjs';

export async function runResearch(client, request = {}) {
  const query = typeof request.query === 'string' ? request.query.trim() : '';
  if (!query) throw new TavilyError('Research query is required.', { status: 400 });

  const followUps = Array.isArray(request.followUpQueries)
    ? request.followUpQueries.filter((item) => typeof item === 'string' && item.trim()).slice(0, 3).map((item) => item.trim())
    : [];
  const queries = [query, ...followUps];
  const sources = [];
  let answer = null;
  let usage = null;

  for (const [index, currentQuery] of queries.entries()) {
    const result = await client.search({
      query: currentQuery,
      searchDepth: request.searchDepth,
      topic: request.topic,
      maxResults: request.maxResults,
      includeAnswer: index === 0,
      includeRawContent: Boolean(request.includeRawContent),
      includeFavicon: true,
      includeUsage: true,
      safeSearch: true,
      includeDomains: request.includeDomains,
      excludeDomains: request.excludeDomains
    });

    if (index === 0 && typeof result?.answer === 'string') answer = result.answer;
    if (result?.usage) usage = result.usage;

    for (const item of Array.isArray(result?.results) ? result.results : []) {
      const url = typeof item?.url === 'string' ? item.url : '';
      if (!url || sources.some((source) => source.url === url)) continue;
      sources.push({
        title: item.title || url,
        url,
        snippet: item.content || '',
        score: typeof item.score === 'number' ? item.score : null,
        favicon: item.favicon || null,
        query: currentQuery
      });
    }
  }

  return {
    query,
    answer,
    sources: sources.slice(0, 30),
    usage,
    queryCount: queries.length
  };
}
