# Copyright (c) 2026 Explyt Ltd
# SPDX-License-Identifier: Apache-2.0

def data: gsub("%"; "%25") | gsub("\r"; "%0D") | gsub("\n"; "%0A");
def property: data | gsub(":"; "%3A") | gsub(","; "%2C");

def level:
  if .known or .kind == "note" or .severity == "low" then "notice"
  elif .severity == "medium" then "warning"
  else "error" end;

def location:
  if .file then "file=\(.file | property)" + (if .line then ",line=\(.line)" else "" end) + "," else "" end;

.findings[]?
| "::\(level) \(location)title=\("\(.severity | ascii_upcase): \(.title // "")" | property)::\(.body // "" | data)"
