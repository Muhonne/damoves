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
    assert.deepEqual(r.actions, ['search', 'setSound', 'open', 'rollDice', 'clearChoice', 'requestDelete', 'cancelDelete', 'confirmDelete']);
    assert.deepEqual(r.state, { query: '', soundEnabled: true, visibleMoves: moves, choice: null, pendingDelete: null, deleting: false, deleteError: null });
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
    r = await s.request({ op: 'restart' });
    assert.equal(r.state.soundEnabled, true, 'preference survives reconstructing the core');
    await s.request({ op: 'do', action: 'setSound', enabled: false });
    r = await s.request({ op: 'restart' });
    assert.equal(r.state.soundEnabled, false);
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

  test(`${platform}: delete imported moves with confirmation`, async () => journey(path, async s => {
    const library = [moves[0], { ...moves[1], imported: true }, moves[2]];
    let r = await s.request({ op: 'init', moves: library, randomIndex: 1, soundEnabled: true });
    r = await s.request({ op: 'do', action: 'requestDelete', id: 'basic' });
    assert.equal(r.state.pendingDelete, null, 'bundled moves cannot be deleted');
    r = await s.request({ op: 'do', action: 'requestDelete', id: 'cross' });
    assert.equal(r.state.pendingDelete, 'cross');
    r = await s.request({ op: 'do', action: 'cancelDelete' });
    assert.equal(r.state.pendingDelete, null);
    await s.request({ op: 'do', action: 'search', value: 'cross' });
    await s.request({ op: 'do', action: 'requestDelete', id: 'cross' });
    r = await s.request({ op: 'do', action: 'confirmDelete' });
    assert.equal(r.state.pendingDelete, null);
    assert.deepEqual(r.state.visibleMoves, []);
    r = await s.request({ op: 'do', action: 'rollDice' });
    assert.deepEqual(r.state.choice, { id: 'turn', loop: true }, 'deleted move is absent from the full dice pool');
    await s.request({ op: 'init', moves: library, randomIndex: 0, soundEnabled: true, deleteSucceeds: false });
    await s.request({ op: 'do', action: 'requestDelete', id: 'cross' });
    r = await s.request({ op: 'do', action: 'confirmDelete' });
    assert.equal(r.state.deleteError, 'Could not delete this clip.');
    assert.deepEqual(r.state.visibleMoves, moves, 'failed delete retains the move');
  }));

  test(`${platform}: navigate between browse, player and editor`, async () => journey(path, async s => {
    let r = await s.request({ op: 'initNavigation' });
    assert.equal(r.state.screen, 'browse');
    r = await s.request({ op: 'do', action: 'openPlayer' });
    assert.equal(r.state.screen, 'player');
    r = await s.request({ op: 'do', action: 'openEditor' });
    assert.equal(r.state.screen, 'player', 'a second screen cannot open over the player');
    r = await s.request({ op: 'do', action: 'close' });
    assert.equal(r.state.screen, 'browse');
    r = await s.request({ op: 'do', action: 'openEditor' });
    assert.equal(r.state.screen, 'editor');
    r = await s.request({ op: 'do', action: 'close' });
    assert.equal(r.state.screen, 'browse');
  }));

  test(`${platform}: playback completion, repeat, close and errors`, async () => journey(path, async s => {
    let r = await s.request({ op: 'initPlayer', loop: false });
    assert.equal(r.name, 'Player');
    r = await s.request({ op: 'do', action: 'completed' });
    assert.deepEqual(r.state, { loop: false, closed: true, error: null });
    r = await s.request({ op: 'do', action: 'failed' });
    assert.equal(r.state.error, null, 'events after closing are ignored');
    await s.request({ op: 'initPlayer', loop: true });
    for (let i = 0; i < 3; i++) {
      r = await s.request({ op: 'do', action: 'completed' });
      assert.equal(r.state.closed, false, 'dice repeats the same move');
    }
    r = await s.request({ op: 'do', action: 'close' });
    assert.equal(r.state.closed, true);
    await s.request({ op: 'initPlayer', loop: true });
    r = await s.request({ op: 'do', action: 'leave' });
    assert.equal(r.state.closed, true);
    await s.request({ op: 'initPlayer', loop: false });
    r = await s.request({ op: 'do', action: 'failed' });
    assert.equal(r.state.closed, true);
    assert.equal(r.state.error, 'Could not play this video.');
  }));

  test(`${platform}: trim, preview, validate and save a selected clip`, async () => journey(path, async s => {
    let r = await s.request({ op: 'initEditor', duration: 4000, name: '', saveSucceeds: true });
    assert.equal(r.name, 'Editor');
    assert.deepEqual(r.state, { duration: 4000, start: 0, end: 4000, name: '', previewing: false, saving: false, saved: false, error: null, sourceFailed: false, prepared: true });
    r = await s.request({ op: 'do', action: 'setStart', value: 500 });
    r = await s.request({ op: 'do', action: 'setEnd', value: 3000 });
    assert.equal(r.state.start, 500);
    assert.equal(r.state.end, 3000);
    r = await s.request({ op: 'do', action: 'togglePreview' });
    assert.equal(r.state.previewing, true);
    r = await s.request({ op: 'do', action: 'previewEnded' });
    assert.equal(r.state.previewing, false);
    await s.request({ op: 'do', action: 'togglePreview' });
    r = await s.request({ op: 'do', action: 'setEnd', value: 3000 });
    assert.equal(r.state.previewing, false, 'adjusting the range stops preview');
    await s.request({ op: 'do', action: 'setName', value: '   ' });
    r = await s.request({ op: 'do', action: 'save' });
    assert.equal(r.state.error, 'Enter a move name');
    assert.equal(r.state.saved, false);
    assert.equal(r.lastExport, null, 'invalid names never reach storage');
    await s.request({ op: 'do', action: 'setName', value: '  New move  ' });
    r = await s.request({ op: 'do', action: 'save' });
    assert.equal(r.state.error, null);
    assert.equal(r.state.saved, true);
    assert.equal(r.state.saving, false);
    assert.deepEqual(r.lastExport, { name: 'New move', start: 500, end: 3000 });
  }));

  test(`${platform}: editor clamps endpoints and handles a failed export`, async () => journey(path, async s => {
    await s.request({ op: 'initEditor', duration: 4000, name: 'Imported move', saveSucceeds: false });
    let r = await s.request({ op: 'do', action: 'setStart', value: 9999 });
    assert.equal(r.state.start, 3999);
    r = await s.request({ op: 'do', action: 'setEnd', value: -100 });
    assert.equal(r.state.end, 4000);
    r = await s.request({ op: 'do', action: 'setStart', value: -1 });
    assert.equal(r.state.start, 0);
    r = await s.request({ op: 'do', action: 'save' });
    assert.equal(r.state.saved, false);
    assert.equal(r.state.saving, false);
    assert.equal(r.state.error, 'Could not save this video. Try an MP4 video.');
    assert.deepEqual(r.lastExport, { name: 'Imported move', start: 0, end: 4000 });
    r = await s.request({ op: 'do', action: 'clearError' });
    assert.equal(r.state.error, null);
    assert.equal(r.state.name, 'Imported move');
  }));

  test(`${platform}: asynchronous save uses a stable request and ignores duplicate taps`, async () => journey(path, async s => {
    await s.request({ op: 'initEditor', duration: 4000, name: 'Original', saveSucceeds: true, deferSave: true });
    await s.request({ op: 'do', action: 'setStart', value: 500 });
    let r = await s.request({ op: 'do', action: 'save' });
    assert.equal(r.state.saving, true);
    assert.deepEqual(r.lastExport, { name: 'Original', start: 500, end: 4000 });
    await s.request({ op: 'do', action: 'setName', value: 'Changed' });
    await s.request({ op: 'do', action: 'setStart', value: 1000 });
    r = await s.request({ op: 'do', action: 'togglePreview' });
    assert.equal(r.state.previewing, false, 'preview cannot start during export');
    r = await s.request({ op: 'do', action: 'save' });
    assert.deepEqual(r.lastExport, { name: 'Original', start: 500, end: 4000 });
    assert.equal(r.exportCount, 1);
    r = await s.request({ op: 'do', action: 'completeSave' });
    assert.equal(r.state.saving, false);
    assert.equal(r.state.saved, true);
  }));

  test(`${platform}: unavailable source cannot be saved`, async () => journey(path, async s => {
    let r = await s.request({ op: 'initEditor', duration: 4000, name: 'Bad source', sourceAvailable: false, saveSucceeds: true });
    assert.equal(r.state.sourceFailed, true);
    assert.equal(r.state.error, 'Could not open this video.');
    r = await s.request({ op: 'do', action: 'togglePreview' });
    assert.equal(r.state.previewing, false);
    r = await s.request({ op: 'do', action: 'save' });
    assert.equal(r.state.saved, false);
    assert.equal(r.state.saving, false);
    assert.equal(r.lastExport, null);
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
