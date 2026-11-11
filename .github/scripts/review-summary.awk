# Copyright (c) 2026 Explyt Ltd
# SPDX-License-Identifier: Apache-2.0

BEGIN { blob = server "/" repo "/blob/" sha "/" }

function escapeData(s) {
    gsub(/%/, "%25", s)
    gsub(/\r/, "%0D", s)
    gsub(/\n/, "%0A", s)
    return s
}

function escapeProperty(s) {
    s = escapeData(s)
    gsub(/:/, "%3A", s)
    gsub(/,/, "%2C", s)
    return s
}

function levelOf(severity) {
    if (severity == "LOW") return "notice"
    if (severity == "MEDIUM") return "warning"
    return "error"
}

function flush() {
    if (severity == "" || annotations == "") { severity = ""; return }
    location = file == "" ? "" : "file=" escapeProperty(file) (line == "" ? "" : ",line=" line) ","
    printf "::%s %stitle=%s::%s\n", levelOf(severity), location, escapeProperty(severity ": " title), escapeData(scenario) > annotations
    severity = ""
}

/^[A-Z]+  / {
    flush()
    split($0, part, "  ")
    severity = part[1]
    where = part[2]
    title = substr($0, length(severity) + length(where) + 5)
    file = where
    line = ""
    scenario = ""
    if (match(where, /:[0-9]+$/)) {
        file = substr(where, 1, RSTART - 1)
        line = substr(where, RSTART + 1)
    }
    print "### " severity ": " title
    print ""
    print "[`" where "`](" blob file (line == "" ? "" : "#L" line) ")"
    print ""
    next
}

/^    / {
    sub(/^    /, "")
    scenario = scenario == "" ? $0 : scenario "\n" $0
    print
    next
}

{ flush() }

/^Report: / { print "[Full report](" substr($0, 9) ")"; next }

{ print }

END { flush() }
