# Repo Notes

- For local stdio MCPs in this demo platform, follow the repo-local plugin examples under `plugins/*/.mcp.json`: anchor paths off the launchers' repo-local `CODEX_HOME` (for example `"$CODEX_HOME/../scripts/..."`) instead of relying on relative `command`, `args`, or `cwd` paths.
- Demo plugin aliases:
  - If a user asks for the `demo-slack` MCP, the Slack demo MCP, or similar old Slack wording, use the `Slack (Demo)` plugin.
  - If a user asks for the `demo-pager` MCP, the PagerDuty demo MCP, or similar old pager wording, use the `PagerDuty (Demo)` plugin.
  - If a user asks for the `demo-observability` MCP, the Datadog demo MCP, or similar old observability wording, use the `Datadog (Demo)` plugin.
  - If a user asks for the Figma demo MCP or old demo Figma wording, use the `Figma (Demo)` plugin.

## Git Pushes

- When pushing to GitHub from this repo, use `PUSHPATROL_BYPASS=1 git push ...` so the repo's PushPatrol hook does not block intentional Codex git actions.

## In-App Browser (IAB)

Browser Use does not expose a separate `browser.*` tool.

To use the in-app browser, run Browser Use through `mcp__node_repl__.js`:

If no `browser.*` tool appears, that is expected.
