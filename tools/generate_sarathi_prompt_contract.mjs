import fs from "node:fs";
import path from "node:path";

const repoRoot = process.cwd();
const contractPath = path.join(repoRoot, "shared", "persona", "sarathi_prompt_contract.json");
const contract = JSON.parse(fs.readFileSync(contractPath, "utf8"));

const generatedHeader = "Generated from shared/persona/sarathi_prompt_contract.json. Do not edit by hand.";

function kotlinString(value) {
  return value
    .replace(/\\/g, "\\\\")
    .replace(/"/g, "\\\"")
    .replace(/\$/g, "\\$");
}

function kotlinList(values) {
  return values.map((value) => `        "${kotlinString(value)}"`).join(",\n");
}

const fallbackVerseLines = contract.fallbackGitaVerses.map(
  (verse) => `${verse.reference}: ${verse.teaching}`,
);

const kotlin = `package com.sarathi.app.llm

// ${generatedHeader}
object SarathiPromptContract {
    val personaRules: List<String> = listOf(
${kotlinList(contract.personaRules)}
    )

    val responseShapeRules: List<String> = listOf(
${kotlinList(contract.responseShapeRules)}
    )

    val fallbackGitaVerses: List<String> = listOf(
${kotlinList(fallbackVerseLines)}
    )

    const val plainTextReplyInstruction: String = "${kotlinString(contract.plainTextReplyInstruction)}"
    const val systemReplyInstruction: String = "${kotlinString(contract.systemReplyInstruction)}"

    val persona: String = buildString {
        appendLine(personaRules.joinToString("\\n"))
        appendLine()
        appendLine("Response shape:")
        responseShapeRules.forEach { appendLine("- $it") }
        appendLine()
        appendLine("Approved fallback Gita verses:")
        fallbackGitaVerses.forEach { appendLine("- $it") }
    }.trim()
}
`;

const ts = `// ${generatedHeader}

export const SARATHI_PERSONA_RULES = ${JSON.stringify(contract.personaRules, null, 2)} as const;

export const SARATHI_RESPONSE_SHAPE_RULES = ${JSON.stringify(contract.responseShapeRules, null, 2)} as const;

export const SARATHI_FALLBACK_GITA_VERSES = ${JSON.stringify(fallbackVerseLines, null, 2)} as const;

export const SARATHI_PLAIN_TEXT_REPLY_INSTRUCTION = ${JSON.stringify(contract.plainTextReplyInstruction)};

export const SARATHI_SYSTEM_REPLY_INSTRUCTION = ${JSON.stringify(contract.systemReplyInstruction)};

export const SARATHI_PERSONA = [
  SARATHI_PERSONA_RULES.join("\\n"),
  "",
  "Response shape:",
  ...SARATHI_RESPONSE_SHAPE_RULES.map((rule) => \`- \${rule}\`),
  "",
  "Approved fallback Gita verses:",
  ...SARATHI_FALLBACK_GITA_VERSES.map((verse) => \`- \${verse}\`)
].join("\\n");
`;

fs.writeFileSync(
  path.join(repoRoot, "android", "app", "src", "main", "java", "com", "sarathi", "app", "llm", "SarathiPromptContract.kt"),
  kotlin,
);
fs.writeFileSync(
  path.join(repoRoot, "web", "packages", "shared-persona", "src", "generatedPromptContract.ts"),
  ts,
);

console.log("Generated Sarathi prompt contract for Android and web.");
