// Browser adaptation of Android's built-in lexical path in TextClassifier.kt.
// MiniLM embeddings, Android persistence, and held-out personal-head adoption
// are outside this presentation demo. Nothing is transmitted or persisted.
export const normalize = text => text.trim().replace(/\s+/gu, ' ').toLowerCase();
const idea = /\b(what if|we could|idea for|idea:|maybe (we|i|try|build)|how about|could (build|make|add|create))\b/i;
const question = /^(who|what|when|where|why|how|which)\b|^(is|are|can|does|do|should|would|will) (this|that|it|the|a|i|we|you)\b/i;
const task = /^(todo\b|remember to|remind me|(?:i|we) (?:need|have|want) to|buy|send|email|call|book|schedule|finish|fix|pay|submit|write|read|review|update|clean|check|make|prepare|apply|order|download|install|pick up|follow up|learn|watch|take|highlight)\b/i;
const code = new RegExp("```|^\\s*(def \\w+\\(|class \\w+[:(]|(?:const|let|var|val) \\w+(?:\\s*:\\s*[^=\\n]+)?\\s*=|(?:suspend )?fun (?:<[^>\\n]+>\\s*)?[\\w.]+\\s*\\(|function |Traceback|<[/]?\\w+)|=>|^\\s*SELECT\\s+(?:DISTINCT\\s+)?(?:\\*|[\\w.]+(?:\\s*,\\s*[\\w.]+)*)\\s+FROM\\s+[\\w.]+\\b|^\\s*(?:INSERT\\s+INTO\\s+\\w+\\s*(?:\\(|VALUES\\b|SELECT\\b)|DELETE\\s+FROM\\s+\\w+\\s+(?:WHERE\\b|;)|CREATE\\s+TABLE\\s+\\w+\\s*\\(|UPDATE\\s+\\w+\\s+SET\\s+\\w+\\s*=)", 'im');
const junk = /^(asdf|qwerty|test test|[.]{3,})$|file attached|image omitted|IMG-\d+/i;
const link = /(?<![\p{L}\p{N}_@])(?:https?:\/\/[^\s<>]+|(?:www\.)?(?:[\p{L}\p{N}](?:[\p{L}\p{N}-]*[\p{L}\p{N}])?\.)+[\p{L}]{2,63}(?::\d{1,5})?(?:[/#?][^\s<>]*)?)/iu;
const encoder = new TextEncoder();
const f = Math.fround;

export async function createClassifier(categories) {
  const featureCache = new Map();
  const wordHashes = new Map();
  const memory = new Map();
  async function feature(source) {
    const text = source.slice(0, 12000);
    if (featureCache.has(text)) return featureCache.get(text);
    const x = new Float32Array(524);
    const cues = [code.test(text), junk.test(text), link.test(text), text.trim().startsWith('- ['),
      idea.test(text), question.test(text) || text.trim().endsWith('?'), task.test(text),
      /tomorrow|today|deadline|due/i.test(text), Math.min(text.length / 500, 1),
      Math.min(text.split(/\s+/u).length / 60, 1), text.toUpperCase() === text && /\p{L}/u.test(text),
      /\b(think|feel|noticed|realized|wonder)\b/i.test(text)];
    x.set(cues.map(Number), 384);
    const tokens = [...normalize(text).slice(0, 6000).matchAll(/[\p{L}\p{N}_]+/gu)].map(m => m[0]);
    const hashes = await Promise.all(tokens.map(word => {
      if (!wordHashes.has(word)) wordHashes.set(word, crypto.subtle.digest('SHA-256', encoder.encode(word)).then(b => new Uint8Array(b)[0] & 127));
      return wordHashes.get(word);
    }));
    for (const hash of hashes) x[396 + hash] += 1;
    let norm = 0;
    for (let d = 396; d < 524; d++) norm += f(x[d] * x[d]);
    norm = Math.max(f(Math.sqrt(norm)), 1e-12);
    for (let d = 396; d < 524; d++) x[d] /= norm;
    if (featureCache.size >= 256) featureCache.delete(featureCache.keys().next().value);
    featureCache.set(text, x);
    return x;
  }
  function probabilities(x, weights, bias) {
    const logits = categories.map((_, c) => {
      let sum = 0;
      for (let d = 384; d < 524; d++) sum += f(x[d] * weights[c][d]);
      return f(bias[c] + f(sum));
    });
    const max = Math.max(...logits);
    const exp = logits.map(v => f(Math.exp(f(v - max))));
    const sum = exp.reduce((a, b) => f(a + b), 0);
    return exp.map(v => f(v / Math.max(sum, 1e-12)));
  }
  const seeds = categories.flatMap((c, index) => (c.examples.length ? c.examples : [c.description]).map(text => ({ text, actual: index })));
  const rows = await Promise.all(seeds.map(async row => ({ ...row, x: await feature(row.text) })));
  const counts = categories.map((_, c) => rows.filter(r => r.actual === c).length);
  const weights = categories.map(() => new Float32Array(524));
  const bias = new Float32Array(categories.length);
  // Only dimensions 384..523 are active without the optional encoder.
  for (let step = 0; step < 180; step++) {
    const gradients = categories.map(() => new Float32Array(524));
    const biases = new Float32Array(categories.length);
    for (const { x, actual } of rows) {
      const p = probabilities(x, weights, bias);
      const balance = f(rows.length / (categories.length * Math.max(counts[actual], 1)));
      for (let c = 0; c < categories.length; c++) {
        const error = f(f(p[c] - (actual === c ? 1 : 0)) * balance);
        biases[c] += error;
        for (let d = 384; d < 524; d++) gradients[c][d] += f(error * x[d]);
      }
    }
    const rate = f(.3 / f(1 + f(step / 200)));
    for (let c = 0; c < categories.length; c++) {
      bias[c] -= f(f(rate * biases[c]) / rows.length);
      for (let d = 384; d < 524; d++) weights[c][d] -= f(rate * f(f(gradients[c][d] / rows.length) + f(.003 * weights[c][d])));
    }
  }
  return {
    async predict(source) {
      if (!source.trim()) throw new Error('Write a note first.');
      const confirmed = memory.get(normalize(source));
      if (confirmed) return { label: confirmed, source: 'approved', reason: 'You previously confirmed this exact note.' };
      const text = source.slice(0, 12000);
      const rules = [
        ['junk', code.test(text) || junk.test(text), 'Code or placeholder text.'],
        ['todos', text.trim().startsWith('- ['), 'A task checkbox marks an action.'],
        ['ideas', idea.test(text), 'This wording proposes a possibility or change.'],
        ['questions', question.test(text) || text.trim().endsWith('?'), 'The wording asks a question.'],
        ['todos', task.test(text), 'The wording describes an action to complete.'],
        ['links', link.test(text), 'This note contains a saved web reference.'],
      ];
      for (const [label, matches, reason] of rules) if (matches && categories.some(c => c.name === label)) return { label, source: 'wording', reason };
      const p = probabilities(await feature(text), weights, bias);
      const winner = p.indexOf(Math.max(...p));
      return { label: categories[winner].name, source: 'classifier', reason: `The small classifier favors ${categories[winner].title.toLowerCase()} from its wording features.` };
    },
    approve(text, label) {
      if (!categories.some(c => c.name === label) || !text.trim()) throw new Error('Choose a valid category.');
      if (memory.size >= 100 && !memory.has(normalize(text))) throw new Error('This demo remembers up to 100 notes. Reset it to start again.');
      memory.set(normalize(text), label);
      return memory.size;
    },
    reset() { memory.clear(); },
    get approvedCount() { return memory.size; },
  };
}
