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

list_open_bug_rows() {
  gh issue list --repo "$repo" \
    --state open \
    "$@" \
    --limit 200 \
    --json number,createdAt,issueType,labels \
    --jq '.[]
      | select(.issueType == null or .issueType.name == "Bug")
      | select([.labels[].name] | index("question") | not)
      | "\(.createdAt)\t\(.number)"'
}

bug_rows=$(list_open_bug_rows --type=Bug)
compatibility_rows=$(list_open_bug_rows --label=compatibility)
all_issue_rows=$(printf '%s\n%s\n' "$bug_rows" "$compatibility_rows" | sed '/^$/d' | sort -u)

if [[ -z "$all_issue_rows" ]]; then
  echo "info: no open Bug-type or compatibility issues found in $repo" >&2
  exit 0
fi

# 2. Sort by creation date and pick the N oldest issues
selected_rows=$(printf '%s\n' "$all_issue_rows" | sort | awk -v n="$issue_limit" 'NR <= n')

# 3. Emit issue details for analysis (JSONL, one object per line)
while IFS=$'\t' read -r _created_at issue_number; do
  gh issue view "$issue_number" --repo "$repo" \
    --json number,title,url,issueType,body,labels,comments \
  | jq -c '{
      number,
      title,
      url,
      type: .issueType.name,
      body,
      labels: [.labels[] | {name, description}],
      comments: [.comments[].body]
    }'
done <<<"$selected_rows"
