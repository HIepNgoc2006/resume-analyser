export function formatScore(score) {
  if (score < 0 || score > 100) {
    throw new RangeError('score must be between 0 and 100');
  }
  return score + '%';
}
