import { test } from 'node:test';
import assert from 'node:assert/strict';
import { formatScore, calculateMatchPercentage, getMatchTier } from '../src/format.js';

test('formatScore formats a valid score', () => {
  assert.equal(formatScore(85), '85%');
  assert.equal(formatScore(0), '0%');
  assert.equal(formatScore(100), '100%');
});

test('formatScore rejects out-of-range score', () => {
  assert.throws(() => formatScore(120), RangeError);
  assert.throws(() => formatScore(-5), RangeError);
});

test('calculateMatchPercentage computes correct percentage', () => {
  assert.equal(calculateMatchPercentage(4, 5), 80);
  assert.equal(calculateMatchPercentage(0, 5), 0);
  assert.equal(calculateMatchPercentage(5, 5), 100);
  assert.equal(calculateMatchPercentage(10, 5), 100);
  assert.equal(calculateMatchPercentage(2, 0), 0);
  assert.equal(calculateMatchPercentage(-1, 5), 0);
});

test('getMatchTier categorizes score accurately', () => {
  assert.equal(getMatchTier(95).label, 'Xuất sắc');
  assert.equal(getMatchTier(80).label, 'Xuất sắc');
  assert.equal(getMatchTier(70).label, 'Phù hợp');
  assert.equal(getMatchTier(60).label, 'Phù hợp');
  assert.equal(getMatchTier(50).label, 'Cần bổ sung');
  assert.equal(getMatchTier(10).label, 'Cần bổ sung');
});
