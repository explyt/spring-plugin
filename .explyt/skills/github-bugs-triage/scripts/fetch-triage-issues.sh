#!/usr/bin/env bash
#
# Copyright (c) 2026 Explyt Ltd
# SPDX-License-Identifier: Apache-2.0
#

# Read-only helper for the github-bugs-triage skill.
# Fetches the N oldest open bug/compatibility issues of explyt/spring-plugin as JSONL.
set -euo pipefail

repo="explyt/spring-plugin"

# Number of oldest issues to triage (optional argument $1, default 5)
issue_limit="${1:-5}"
if ! [[ "$issue_limit" =~ ^[1-9][0-9]*$ ]]; then
  echo "error: invalid count: '$issue_limit'. Use a positive integer" >&2
  exit 1
fi

# 1. Collect open issues carrying either triage-relevant form label,
#    oldest first, de-duplicated across the two label queries.
all_issue_rows=$(
  for label in "plugin-bug" "compatibility"; do
    gh issue list --repo "$repo" \
      --state open \
      --label "$label" \
      --limit 200 \
      --json number,createdAt \
      --jq '.[] | "\(.createdAt)\t\(.number)"'
  done | sort -u
)

if [[ -z "$all_issue_rows" ]]; then
  echo "info: no open plugin-bug/compatibility issues found in $repo" >&2
  exit 0
fi

# 2. Sort by creation date and pick the N oldest issues
selected_rows=$(printf '%s\n' "$all_issue_rows" | sort | awk -v n="$issue_limit" 'NR <= n')

# 3. Emit issue details for analysis (JSONL, one object per line)
while IFS=$'\t' read -r _created_at issue_number; do
  gh issue view "$issue_number" --repo "$repo" \
    --json number,title,url,body,labels,comments \
  | jq -c '{
      number,
      title,
      url,
      body,
      labels: [.labels[] | {name, description}],
      comments: [.comments[].body]
    }'
done <<<"$selected_rows"
