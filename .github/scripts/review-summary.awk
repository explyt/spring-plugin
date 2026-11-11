# Copyright (c) 2026 Explyt Ltd
# SPDX-License-Identifier: Apache-2.0

BEGIN { blob = server "/" repo "/blob/" sha "/" }

/^[A-Z]+  / {
    split($0, part, "  ")
    severity = part[1]
    where = part[2]
    title = substr($0, length(severity) + length(where) + 5)
    file = where
    anchor = ""
    if (match(where, /:[0-9]+$/)) {
        file = substr(where, 1, RSTART - 1)
        anchor = "#L" substr(where, RSTART + 1)
    }
    print "### " severity ": " title
    print ""
    print "[`" where "`](" blob file anchor ")"
    print ""
    next
}

/^    / { sub(/^    /, ""); print; next }

/^Report: / { print "[Full report](" substr($0, 9) ")"; next }

{ print }
