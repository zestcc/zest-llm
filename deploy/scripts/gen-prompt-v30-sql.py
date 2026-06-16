#!/usr/bin/env python3
"""Generate V30 Flyway SQL from .hbs prompt sources."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2] / "zest-llm-admin" / "src" / "main" / "resources"
INVOKE = (ROOT / "prompts" / "zeststory-invoke-v2.hbs").read_text(encoding="utf-8")
RAG = (ROOT / "prompts" / "zeststory-rag-v2.hbs").read_text(encoding="utf-8")
SCHEMA = '{"type":"object","properties":{"answer":{"type":"string"}},"required":["answer"]}'


def esc(text: str) -> str:
    return text.replace("'", "''")


sql = f"""-- ZestStory Prompt v2: global constitution + taskType branches + Zestory flavor slot
UPDATE llm_prompt_version SET status = 'DRAFT', published_at = NULL WHERE task_id IN (3, 4) AND status = 'PUBLISHED';

INSERT INTO llm_prompt_version (id, task_id, version, template_body, output_schema, status, published_at, created_by)
SELECT 3030, 3, 'v2', '{esc(INVOKE)}', '{SCHEMA}', 'PUBLISHED', CURRENT_TIMESTAMP, 'flyway-v30'
WHERE NOT EXISTS (SELECT 1 FROM llm_prompt_version WHERE task_id = 3 AND version = 'v2');

INSERT INTO llm_prompt_version (id, task_id, version, template_body, output_schema, status, published_at, created_by)
SELECT 3040, 4, 'v2', '{esc(RAG)}', '{SCHEMA}', 'PUBLISHED', CURRENT_TIMESTAMP, 'flyway-v30'
WHERE NOT EXISTS (SELECT 1 FROM llm_prompt_version WHERE task_id = 4 AND version = 'v2');

UPDATE llm_prompt_version SET status = 'PUBLISHED', published_at = CURRENT_TIMESTAMP, created_by = 'flyway-v30'
WHERE task_id = 3 AND version = 'v2';
UPDATE llm_prompt_version SET status = 'PUBLISHED', published_at = CURRENT_TIMESTAMP, created_by = 'flyway-v30'
WHERE task_id = 4 AND version = 'v2';
"""

out = ROOT / "db" / "migration" / "zestllm" / "V30__zeststory_prompt_v2.sql"
out.write_text("\ufeff" + sql, encoding="utf-8")
print(f"Wrote {out} ({out.stat().st_size} bytes)")
