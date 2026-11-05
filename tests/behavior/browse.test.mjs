import { test } from 'node:test';
import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { createInterface } from 'node:readline';
import { once } from 'node:events';

const hosts = [
  ['iOS', process.env.DAMOVES_IOS_HOST],
  ['Android', process.env.DAMOVES_ANDROID_HOST],
];
const moves = [
  { id: 'basic', name: 'Basic step' },
  { id: 'cross', name: 'Cross body lead' },
  { id: 'turn', name: 'Inside turn' },
];

class Session {
  constructor(executable) {
    assert.ok(executable, 'Host executable is required; use tests/behavior/run.sh');
    this.child = spawn(executable, [], { stdio: ['pipe', 'pipe', 'pipe'] });
    this.lines = createInterface({ input: this.child.stdout })[Symbol.asyncIterator]();
    this.stderr = '';
    this.child.stderr.setEncoding('utf8');
    this.child.stderr.on('data', data => { this.stderr += data; });
  }
  async request(request) {
    this.child.stdin.write(JSON.stringify(request) + '\n');
    let timer;
    try {
      const result = await Promise.race([
        this.lines.next(),
        new Promise((_, reject) => { timer = setTimeout(() => reject(new Error(`Host timed out: ${this.stderr}`)), 5000); }),
      ]);
      assert.equal(result.done, false, `Host exited: ${this.stderr}`);
      return JSON.parse(result.value);
    } finally { clearTimeout(timer); }
  }
  async close() {
    this.child.stdin.end();
    if (this.child.exitCode === null) await once(this.child, 'exit');
    assert.equal(this.child.exitCode, 0, this.stderr);
  }
}

async function journey(path, run) {
  const session = new Session(path);
  try { await run(session); } finally { await session.close(); }
}

for (const [platform, path] of hosts) {
  test(`${platform}: browse, search, tile selection and full-library dice`, async () => journey(path, async s => {
    let r = await s.request({ op: 'init', moves, randomIndex: 1, soundEnabled: true });
    assert.equal(r.name, 'Browse');
    assert.deepEqual(r.actions, ['search', 'setSound', 'open', 'rollDice', 'clearChoice']);
    assert.deepEqual(r.state, { query: '', soundEnabled: true, visibleMoves: moves, choice: null });
    r = await s.request({ op: 'do', action: 'search', value: '  TURN  ' });
    assert.deepEqual(r.state.visibleMoves, [moves[2]]);
    r = await s.request({ op: 'do', action: 'open', id: 'turn' });
    assert.deepEqual(r.state.choice, { id: 'turn', loop: false });
    r = await s.request({ op: 'do', action: 'clearChoice' });
    assert.equal(r.state.choice, null);
    r = await s.request({ op: 'do', action: 'open', id: 'cross' });
    assert.equal(r.state.choice, null, 'a filtered-out tile cannot be opened');
    r = await s.request({ op: 'do', action: 'rollDice' });
    assert.deepEqual(r.state.choice, { id: 'cross', loop: true }, 'dice ignores the filter');
    r = await s.request({ op: 'do', action: 'search', value: 'no matches' });
    assert.deepEqual(r.state.visibleMoves, []);
    r = await s.request({ op: 'do', action: 'rollDice' });
    assert.deepEqual(r.state.choice, { id: 'cross', loop: true });
    r = await s.request({ op: 'do', action: 'search', value: '  ' });
    assert.deepEqual(r.state.visibleMoves, moves);
    assert.equal(r.state.choice, null);
  }));

  test(`${platform}: empty library and sound state`, async () => journey(path, async s => {
    let r = await s.request({ op: 'init', moves: [], randomIndex: 0, soundEnabled: false });
    assert.equal(r.state.soundEnabled, false);
    r = await s.request({ op: 'do', action: 'rollDice' });
    assert.equal(r.state.choice, null);
    r = await s.request({ op: 'do', action: 'setSound', enabled: true });
    assert.equal(r.state.soundEnabled, true);
    r = await s.request({ op: 'do', action: 'open', id: 'missing' });
    assert.equal(r.state.choice, null);
  }));

  test(`${platform}: random selection handles a singleton and an invalid picker`, async () => journey(path, async s => {
    let r = await s.request({ op: 'init', moves: [moves[0]], randomIndex: 0, soundEnabled: true });
    r = await s.request({ op: 'do', action: 'rollDice' });
    assert.deepEqual(r.state.choice, { id: 'basic', loop: true });
    r = await s.request({ op: 'init', moves, randomIndex: 99, soundEnabled: true });
    r = await s.request({ op: 'do', action: 'rollDice' });
    assert.equal(r.state.choice, null);
  }));

  test(`${platform}: malformed actions are rejected without changing state`, async () => journey(path, async s => {
    const initial = await s.request({ op: 'init', moves, randomIndex: 0, soundEnabled: true });
    for (const req of [
      { op: 'do', action: 'search' }, { op: 'do', action: 'setSound', enabled: 'false' },
      { op: 'do', action: 'unknown' }, { op: 'init', moves, randomIndex: 0 },
      { op: 'init', moves: [moves[0], moves[0]], randomIndex: 0, soundEnabled: true },
      { op: 'init', moves, randomIndex: 2147483648, soundEnabled: true },
    ]) assert.equal((await s.request(req)).ok, false);
    assert.deepEqual((await s.request({ op: 'ls' })).state, initial.state);
  }));
}
