from __future__ import annotations

import json
from pathlib import Path
from typing import Any, Protocol


class Tool(Protocol):
    name: str

    def call(self, **kwargs: Any) -> Any: ...


class SandboxSearchTool:
    """Read-only deterministic search over mock JSON/text files."""

    name = "sandbox.search"

    def __init__(self, root: str | Path):
        self.root = Path(root)

    def call(self, query: str, scope: str | None = None) -> list[dict[str, Any]]:
        query_l = query.lower()
        base = self.root / scope if scope else self.root
        results: list[dict[str, Any]] = []

        for path in sorted(base.rglob("*")):
            if not path.is_file():
                continue
            try:
                text = path.read_text(encoding="utf-8")
            except UnicodeDecodeError:
                continue

            if query_l not in text.lower() and query_l not in path.name.lower():
                continue

            payload: Any
            if path.suffix == ".json":
                try:
                    payload = json.loads(text)
                except json.JSONDecodeError:
                    payload = text
            else:
                payload = text

            results.append({
                "path": str(path.relative_to(self.root)),
                "content": payload,
            })

        return results
