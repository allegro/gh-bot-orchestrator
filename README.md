GH Bot Orchestrator
===================

A service that makes building custom GitHub bots easy!

## Usage

1. Install [GH Bot Orchestrator GitHub App](TODO) in your organization
2. Deploy `gh-bot-orchestrator` to your infrastructure (see [deployment](#deployment))
3. Create a workflow in a repository within your organization that looks like this:

```yaml
on:
  workflow_dispatch:
    inputs:
      comment-callback-url:
        type: text
  # other inputs omitted for brevity

jobs:
  hello-world-bot:
    runs-on: ubuntu-latest
    steps:
      - name: Post PR comment
        uses: allegro/gh-bot-orchestrator/post-comment
        with:
          comment-callback-url: ${{ inputs.comment-callback-url }}
          message: Hello, I'm the bot!
```

## Features

- a dedicated PR check is created for the bot execution
- a bot can update the status of that check via provided `check-callback-url`
- a bot can post PR comments via provided `comment-callback-url`
- a bot can check out the original repository via provided readonly `github-clone-token`
- a bot can do whatever possible via GitHub API (but a custom GitHub app is required for that)
- a bot can access the original GitHub event
- a bot can access list of files changed in a PR (it's not available in the event)
- a bot can access Dependabot metadata (which dependency was bumped to which version)

## Use cases
- run automatic code migrations for every dependabot PR bumping a specific dependency (see [allwrite](https://github.com/allegro/allwrite))
- validate common config file correctness whenever it's modified
- perform a security scan on the changed files
- automatically add label when PR is approved (e.g. `_<username> : 👍`)

## Deployment

TODO
