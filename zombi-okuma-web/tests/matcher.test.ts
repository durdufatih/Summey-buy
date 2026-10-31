import { test } from "node:test";
import assert from "node:assert/strict";
import { matchesOne } from "../lib/matcher.ts";

const cases: [string, string, boolean][] = [
  ["Ali", "ali", true],
  ["Ela", "Ela", true],
  ["nane", "na ne", true],
  ["nane", "nene", true],
  ["at", "ad", false],
  ["at", "at at", true],
  ["ne", "ne ne", true],
  ["il", "İL", true],
  ["Lale", "lale", true],
  ["Ela nane al.", "Ela nane al", true],
  ["Ela nane al.", "ela nane", false],
  ["Ali Ela ile el al.", "Ali Ela ile el al", true],
  ["Lale ile Nil nane al.", "lale ile nil nane al", true],
  ["Nil el al.", "Nil el aldı", false],
];

for (const [target, heard, want] of cases) {
  test(`${target} ← "${heard}" = ${want}`, () => {
    assert.equal(matchesOne(target, heard), want);
  });
}
