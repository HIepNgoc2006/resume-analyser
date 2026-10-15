import { test } from 'node:test';
import assert from 'node:assert/strict';
import { formatScore } from '../src/format.js';

test('formats a valid score', () => {
  assert.equal(formatScore(85), '85%');
});

test('rejects an out-of-range score', () => {
  assert.throws(() => formatScore(120), RangeError);
});
