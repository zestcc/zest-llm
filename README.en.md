# ZestLLM — XXL-Job for AI

Java AI control plane: pick your stack (A), configure scenarios (B), declare jobs with `@ZestLLM(code)`.

## Quick Start

```powershell
powershell -File deploy/scripts/zest-stack-up.ps1 -Tier small
powershell -File deploy/scripts/full-acceptance.ps1
```

| Tier | Use case |
|------|----------|
| small | POC, 1–2 AI jobs |
| medium | Langfuse + Eval gates |
| large | Dify + RAGFlow + MCP |

## Modules

Same as [README.md](README.md).

## Docs

- [Full plan (CN)](docs/Zest-Stack完整实现方案.md)
- [XXL-Job comparison](docs/XXL-Job对比.md)
- [Spring AI relationship](docs/Spring-AI与ZestLLM.md)
- [15-min demo](docs/15分钟Demo.md)
