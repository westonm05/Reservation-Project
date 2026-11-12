"""Turns the Surefire and JaCoCo reports into GitHub annotations, so test results and
coverage show up on the run's summary page (and on pull requests) without opening the logs."""
import csv
import glob
import xml.etree.ElementTree as ET
from collections import defaultdict

# Count <testcase> elements directly (deduplicated): the suite-level totals Surefire writes
# don't include tests inside @Nested classes.
cases = {}
for path in glob.glob("target/surefire-reports/TEST-*.xml"):
    for case in ET.parse(path).getroot().iter("testcase"):
        cases[(case.get("classname", ""), case.get("name"))] = case

total = len(cases)
failures = errors = skipped = 0
failed = []
for (classname, name), case in sorted(cases.items()):
    if case.find("skipped") is not None:
        skipped += 1
    problem = case.find("failure")
    if problem is not None:
        failures += 1
    else:
        problem = case.find("error")
        if problem is not None:
            errors += 1
    if problem is not None:
        reason = (problem.get("type") or "").split(".")[-1]
        failed.append(f"{classname.split('.')[-1]}.{name} ({reason})")

level = "error" if failures or errors else "notice"
print(f"::{level} title=Tests::Tests run: {total}, Failures: {failures}, Errors: {errors}, Skipped: {skipped}")
if failed:
    # One annotation listing every failing test (GitHub caps the number of annotations per step)
    print("::error title=Failing tests::" + "%0A".join(failed))

rows = list(csv.DictReader(open("target/site/jacoco/jacoco.csv"))) if glob.glob("target/site/jacoco/jacoco.csv") else []
if rows:
    by_pkg = defaultdict(lambda: [0, 0, 0, 0])  # line missed, line covered, branch missed, branch covered
    for r in rows:
        p = by_pkg[r["PACKAGE"].split(".")[-1]]
        p[0] += int(r["LINE_MISSED"]); p[1] += int(r["LINE_COVERED"])
        p[2] += int(r["BRANCH_MISSED"]); p[3] += int(r["BRANCH_COVERED"])

    def pct(missed, covered):
        return f"{100 * covered / (missed + covered):.1f}%" if missed + covered else "n/a"

    lines = [f"{pkg}: lines {pct(v[0], v[1])} ({v[1]}/{v[0] + v[1]}), branches {pct(v[2], v[3])}"
             for pkg, v in sorted(by_pkg.items())]
    tm = sum(v[0] for v in by_pkg.values()); tc = sum(v[1] for v in by_pkg.values())
    bm = sum(v[2] for v in by_pkg.values()); bc = sum(v[3] for v in by_pkg.values())
    lines.append(f"TOTAL: lines {pct(tm, tc)} ({tc}/{tm + tc}), branches {pct(bm, bc)}")
    print("::notice title=Coverage by package::" + "%0A".join(lines))

    classes = [f"{r['PACKAGE'].split('.')[-1]}.{r['CLASS']}: {pct(int(r['LINE_MISSED']), int(r['LINE_COVERED']))} "
               f"({r['LINE_MISSED']} lines missed)" for r in rows]
    print("::notice title=Coverage by class::" + "%0A".join(classes))

# Also write a short table to the run's summary page (shown on the pull request's Checks tab).
import os
summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
if summary_path:
    with open(summary_path, "a") as out:
        status = "❌ failing" if failures or errors else "✅ passing"
        out.write("### Build & Test\n\n| Tests | Failures | Errors | Skipped | Status |\n|---|---|---|---|---|\n")
        out.write(f"| {total} | {failures} | {errors} | {skipped} | {status} |\n")
        for name in failed:
            out.write(f"- `{name}`\n")
