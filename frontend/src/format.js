export function formatScore(score) {
  if (score < 0 || score > 100) {
    throw new RangeError('score must be between 0 and 100');
  }
  return score + '%';
}

export function calculateMatchPercentage(matched, required) {
  if (required <= 0) return 0;
  if (matched < 0) return 0;
  const pct = Math.round((matched / required) * 100);
  return Math.min(100, Math.max(0, pct));
}

export function getMatchTier(score) {
  if (score >= 80) {
    return { label: 'Xuất sắc', class: 'tier-high', color: '#10b981' };
  }
  if (score >= 60) {
    return { label: 'Phù hợp', class: 'tier-medium', color: '#38bdf8' };
  }
  return { label: 'Cần bổ sung', class: 'tier-low', color: '#f59e0b' };
}

export function generateReportSummary(data) {
  if (!data) return '';
  const score = data.score ?? 0;
  const tier = data.tier ?? 'Chưa đánh giá';
  const matched = (data.matchedSkills || []).join(', ') || 'Không có';
  const missing = (data.missingSkills || []).join(', ') || 'Không có';
  const extra = (data.additionalSkills || []).join(', ') || 'Không có';

  return [
    '=== BÁO CÁO ĐÁNH GIÁ ỨNG VIÊN ===',
    `Điểm tương thích: ${score}%`,
    `Phân loại: ${tier}`,
    `Kỹ năng phù hợp: ${matched}`,
    `Kỹ năng còn thiếu: ${missing}`,
    `Kỹ năng bổ sung: ${extra}`,
    `Tóm tắt: ${data.summary || 'N/A'}`
  ].join('\n');
}
