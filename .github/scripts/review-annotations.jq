# Copyright (c) 2026 Explyt Ltd
# SPDX-License-Identifier: Apache-2.0

def text: if . == null then "" elif type == "string" then . else tojson end;
def data: text | gsub("%"; "%25") | gsub("\r"; "%0D") | gsub("\n"; "%0A");
def property: data | gsub(":"; "%3A") | gsub(","; "%2C");
def severity: .severity | text | ascii_downcase | if . == "" then "unrated" else . end;

def level:
  if .known == true or .kind == "note" or severity == "low" then "notice"
  elif severity == "medium" then "warning"
  else "error" end;

def location:
  (.file | text) as $file
  | if $file == "" then ""
    else "file=\($file | property)" + (if (.line | type) == "number" then ",line=\(.line | floor)" else "" end) + ","
    end;

.findings[]?
| "::\(level) \(location)title=\("\(severity | ascii_upcase): \(.title | text)" | property)::\(.body | data)"
