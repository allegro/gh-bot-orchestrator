GitHub Workflow Orchestrator
============================

A service that can dispatch your GitHub Actions workflow for all repositories within the entinre GitHub organization.

Use cases:
- run automatic code migrations for every dependabot PR bumping a specific dependency (see [allwrite](https://github.com/allegro/allwrite))
- validate common config file correctness whenever it's modified
- perform a security scan on the changed files
- automatically add label when PR is approved (e.g. `_<username> : 👍`)
