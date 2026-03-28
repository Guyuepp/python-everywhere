# Workspace Copilot Instructions

## Command Output Safety (Must Follow)

- Never run unbounded high-output commands directly.
- For potentially large output, always use this sequence:
  1. Count first: use `wc -l`.
  2. Preview only: use `head -n 100`.
  3. If full output is required, redirect to a temporary file and provide a concise summary.
- Default terminal output budget is 200 lines or less per command.
- Prefer narrow searches and scoped paths over repository-wide scans.
- Always exclude heavy directories from recursive search when possible: `.git`, `.venv`, `.gradle`, `build`, `node_modules`.
- Prefer command patterns that are safe for large repos:
  - `find ... | wc -l`
  - `find ... | head -n 100`
  - `grep ... | head -n 100`
  - `command > /tmp/some_report.txt`

## Reporting Behavior

- Summarize results rather than dumping full logs.
- Include only key lines needed for diagnosis.
- If output is expected to be huge, state this first and switch to count plus preview mode.
