const fs = require("fs");
const path = require("path");

const gitaRoot = path.resolve(__dirname, "..");
const processedDir = path.join(gitaRoot, "processed");
const refinedPath = path.join(processedDir, "gita_verses.jsonl");
const reportPath = path.join(processedDir, "gita_verses.report.json");

const allowedIntents = new Set([
  "comfort",
  "motivation",
  "detachment",
  "courage",
  "grief_support",
  "discipline",
  "self_control",
  "surrender",
  "clarity",
  "duty",
  "anxiety_relief",
  "anger_management",
  "devotion",
  "meditation",
  "wisdom",
  "moral_guidance",
  "faith",
  "liberation",
  "humility",
  "action_guidance",
]);

const maxArraySizes = {
  primary_topics: 3,
  secondary_topics: 5,
  intent_matches: 3,
  emotional_states: 4,
  life_situations: 5,
  user_prompt_keywords: 10,
  spiritual_concepts: 6,
  avoid_for_prompts: 3,
};

const requiredTopLevelKeys = [
  "id",
  "source",
  "content",
  "retrieval",
  "teaching",
  "rag",
  "safety",
  "metadata",
];

const errors = [];
const warnings = [];

function readJsonl(filePath, label) {
  if (!fs.existsSync(filePath)) {
    errors.push(`Missing ${label}: ${filePath}`);
    return [];
  }
  return fs.readFileSync(filePath, "utf8")
    .split(/\r?\n/)
    .filter((line) => line.trim().length > 0)
    .map((line, index) => {
      try {
        return JSON.parse(line);
      } catch (error) {
        errors.push(`${label} line ${index + 1}: invalid JSON: ${error.message}`);
        return null;
      }
    })
    .filter(Boolean);
}

function pairKey(row) {
  return `${row.source ? row.source.chapter : row.chapter}.${row.source ? row.source.verse : row.verse}`;
}

function countBy(items) {
  return items.reduce((acc, item) => {
    acc[item] = (acc[item] || 0) + 1;
    return acc;
  }, {});
}

function repeatedValues(records, selector) {
  const counts = new Map();
  records.forEach((record) => {
    const value = selector(record);
    if (!value) return;
    counts.set(value, (counts.get(value) || 0) + 1);
  });
  return [...counts.entries()]
    .filter(([, count]) => count > 1)
    .sort((a, b) => b[1] - a[1])
    .map(([text, count]) => ({ text, count }));
}

const refinedRows = readJsonl(refinedPath, "refined");
const seenIds = new Set();
const seenPairs = new Set();
const versesWithTooManyIntents = [];
const versesWithTooManyKeywords = [];

if (refinedRows.length !== 700) errors.push(`Expected 700 refined verses, found ${refinedRows.length}`);

refinedRows.forEach((record, index) => {
  const line = index + 1;
  requiredTopLevelKeys.forEach((key) => {
    if (!Object.prototype.hasOwnProperty.call(record, key)) {
      errors.push(`Line ${line}: missing top-level key ${key}`);
    }
  });

  const source = record.source || {};
  const content = record.content || {};
  const retrieval = record.retrieval || {};
  const rag = record.rag || {};
  const safety = record.safety || {};
  const metadata = record.metadata || {};
  const key = `${source.chapter}.${source.verse}`;

  if (seenIds.has(record.id)) errors.push(`Line ${line}: duplicate id ${record.id}`);
  seenIds.add(record.id);
  if (seenPairs.has(key)) errors.push(`Line ${line}: duplicate chapter/verse ${key}`);
  seenPairs.add(key);
  if (record.id !== `BG-${source.chapter}-${source.verse}`) {
    errors.push(`Line ${line}: id must be BG-{chapter}-{verse}`);
  }
  if (source.citation !== `Bhagavad Gita ${source.chapter}.${source.verse}`) {
    errors.push(`Line ${line}: citation mismatch`);
  }
  if (typeof source.chapter !== "number" || typeof source.verse !== "number") {
    errors.push(`Line ${line}: chapter and verse must be numbers`);
  }
  if (typeof content.translation !== "string" || content.translation.trim() === "") {
    errors.push(`Line ${line}: translation is empty`);
  }

  Object.entries(maxArraySizes).forEach(([field, max]) => {
    if (!Array.isArray(retrieval[field])) {
      errors.push(`Line ${line}: retrieval.${field} must be an array`);
      return;
    }
    if (retrieval[field].length > max) {
      errors.push(`Line ${line}: retrieval.${field} exceeds max ${max}`);
    }
  });

  (retrieval.intent_matches || []).forEach((intent) => {
    if (!allowedIntents.has(intent)) errors.push(`Line ${line}: invalid intent ${intent}`);
  });
  if ((retrieval.intent_matches || []).length > 3) versesWithTooManyIntents.push(record.id);
  if ((retrieval.user_prompt_keywords || []).length > 10) versesWithTooManyKeywords.push(record.id);

  if (typeof rag.search_text !== "string" || rag.search_text.trim() === "") {
    errors.push(`Line ${line}: rag.search_text is empty`);
  }
  if (typeof rag.embedding_text !== "string" || rag.embedding_text.trim() === "") {
    errors.push(`Line ${line}: rag.embedding_text is empty`);
  }
  if (typeof rag.priority_score !== "number" || rag.priority_score < 0 || rag.priority_score > 1) {
    errors.push(`Line ${line}: priority_score out of range`);
  }
  if (typeof rag.confidence !== "number" || rag.confidence < 0 || rag.confidence > 1) {
    errors.push(`Line ${line}: confidence out of range`);
  }
  if (!["low", "medium", "high"].includes(safety.interpretation_risk)) {
    errors.push(`Line ${line}: invalid interpretation_risk`);
  }
  if (metadata.enrichment_status !== "refined") {
    errors.push(`Line ${line}: metadata.enrichment_status must be refined`);
  }
});

const priorities = refinedRows.map((row) => row.rag.priority_score);
const avgPriority = priorities.reduce((sum, value) => sum + value, 0) / Math.max(1, priorities.length);
const chapter1Rows = refinedRows.filter((row) => row.source.chapter === 1);
const chapter1Avg = chapter1Rows.reduce((sum, row) => sum + row.rag.priority_score, 0) / Math.max(1, chapter1Rows.length);
const repeatedSummaries = repeatedValues(refinedRows, (row) => row.teaching && row.teaching.one_line_summary);
const repeatedEmbeddings = repeatedValues(refinedRows, (row) => row.rag && row.rag.embedding_text);
const intentDistribution = countBy(refinedRows.flatMap((row) => row.retrieval.intent_matches || []));
const riskDistribution = countBy(refinedRows.map((row) => row.safety.interpretation_risk));

if (repeatedSummaries.length > 0) {
  warnings.push(`${repeatedSummaries.length} repeated one_line_summary value(s) found`);
}
if (repeatedEmbeddings.length > 0) {
  warnings.push(`${repeatedEmbeddings.length} repeated embedding_text value(s) found`);
}
if (avgPriority < 0.5 || avgPriority > 0.62) {
  warnings.push(`Average priority_score ${avgPriority.toFixed(4)} is outside target 0.50-0.62`);
}
if (chapter1Avg > 0.45) {
  warnings.push(`Chapter 1 average priority_score ${chapter1Avg.toFixed(4)} is above 0.45`);
}

const report = {
  total_verses: refinedRows.length,
  raw_verses: refinedRows.length,
  average_priority_score: Number(avgPriority.toFixed(4)),
  chapter_1_average_priority_score: Number(chapter1Avg.toFixed(4)),
  count_by_interpretation_risk: riskDistribution,
  count_by_intent_matches: intentDistribution,
  top_20_highest_priority_verses: refinedRows
    .slice()
    .sort((a, b) => b.rag.priority_score - a.rag.priority_score)
    .slice(0, 20)
    .map((row) => ({
      id: row.id,
      citation: row.source.citation,
      priority_score: row.rag.priority_score,
      primary_topics: row.retrieval.primary_topics,
      intent_matches: row.retrieval.intent_matches,
    })),
  repeated_one_line_summaries: repeatedSummaries.slice(0, 50),
  repeated_embedding_texts: repeatedEmbeddings.slice(0, 50),
  verses_with_too_many_intents: versesWithTooManyIntents,
  verses_with_too_many_keywords: versesWithTooManyKeywords,
  warnings,
};

if (refinedRows.length > 0) {
  fs.writeFileSync(reportPath, JSON.stringify(report, null, 2), "utf8");
}

if (errors.length > 0) {
  console.error(`Validation failed with ${errors.length} error(s):`);
  errors.slice(0, 80).forEach((error) => console.error(`- ${error}`));
  if (errors.length > 80) console.error(`- ... ${errors.length - 80} more`);
  process.exit(1);
}

console.log("Validation passed.");
console.log(`Canonical enriched verse count: ${refinedRows.length}`);
console.log(`Average priority_score: ${avgPriority.toFixed(4)}`);
console.log(`Chapter 1 average priority_score: ${chapter1Avg.toFixed(4)}`);
console.log(`Verses with more than 3 intents: ${versesWithTooManyIntents.length}`);
console.log(`Repeated one_line_summary values: ${repeatedSummaries.length}`);
console.log(`Repeated embedding_text values: ${repeatedEmbeddings.length}`);
if (warnings.length > 0) {
  console.log("Warnings:");
  warnings.forEach((warning) => console.log(`- ${warning}`));
} else {
  console.log("Warnings: none");
}
