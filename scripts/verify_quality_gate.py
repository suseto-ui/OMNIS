#!/usr/bin/env python3
"""
O.M.N.I.S. Quality Gate Automated Barrier Evaluator.
Implements SonarQube strict formula for combined Condition & Line Coverage on New Code:
    Coverage = (CT + CF + LC) / (2 * B + EL) >= 80.0%
where:
    CT = Conditions evaluated to True (covered true branches)
    CF = Conditions evaluated to False (covered false branches)
    LC = Lines Covered
    B  = Total Branches (decision points)
    EL = Total Executable Lines
"""

import sys
from dataclasses import dataclass
from typing import List, Tuple, Dict, Any


@dataclass
class CodeCoverageMetrics:
    covered_conditions_true: int
    covered_conditions_false: int
    covered_lines: int
    total_branches: int
    executable_lines: int

    @property
    def combined_coverage_pct(self) -> float:
        denominator = (2 * self.total_branches) + self.executable_lines
        if denominator == 0:
            return 100.0
        numerator = self.covered_conditions_true + self.covered_conditions_false + self.covered_lines
        return (numerator / denominator) * 100.0

    @property
    def branch_coverage_pct(self) -> float:
        total_conditions = 2 * self.total_branches
        if total_conditions == 0:
            return 100.0
        covered = self.covered_conditions_true + self.covered_conditions_false
        return (covered / total_conditions) * 100.0

    @property
    def line_coverage_pct(self) -> float:
        if self.executable_lines == 0:
            return 100.0
        return (self.covered_lines / self.executable_lines) * 100.0


@dataclass
class QualityGateResult:
    passed: bool
    violations: List[str]
    metrics_summary: Dict[str, Any]


class QualityGateEngine:
    MAX_CYCLOMATIC_COMPLEXITY = 15
    MIN_COVERAGE_THRESHOLD = 80.0
    MAX_VULNERABILITIES = 0
    MAX_BUGS = 0
    MAX_DUPLICATION_PCT = 3.0

    @classmethod
    def evaluate(
        cls,
        coverage: CodeCoverageMetrics,
        max_method_cc: int,
        vulnerabilities: int = 0,
        bugs: int = 0,
        duplicated_lines_pct: float = 0.0,
    ) -> QualityGateResult:
        violations = []

        # 1. Check Combined Condition/Line Coverage
        comb_cov = coverage.combined_coverage_pct
        if comb_cov < cls.MIN_COVERAGE_THRESHOLD:
            violations.append(
                f"REJECTED: Combined New Code Coverage ({comb_cov:.2f}%) is below strict barrier threshold ({cls.MIN_COVERAGE_THRESHOLD}%). "
                f"Formula (CT={coverage.covered_conditions_true} + CF={coverage.covered_conditions_false} + LC={coverage.covered_lines}) / "
                f"(2*B={2*coverage.total_branches} + EL={coverage.executable_lines}) = {comb_cov:.2f}%"
            )

        # 2. Check Cyclomatic Complexity
        if max_method_cc > cls.MAX_CYCLOMATIC_COMPLEXITY:
            violations.append(
                f"REJECTED: Maximum Cyclomatic Complexity ({max_method_cc}) exceeds limit of {cls.MAX_CYCLOMATIC_COMPLEXITY}. "
                "Refactor method using Strategy Pattern or Extract Method."
            )

        # 3. Security & Quality Invariants
        if vulnerabilities > cls.MAX_VULNERABILITIES:
            violations.append(f"REJECTED: Found {vulnerabilities} vulnerabilities. Zero tolerance required.")

        if bugs > cls.MAX_BUGS:
            violations.append(f"REJECTED: Found {bugs} bugs. Zero tolerance required.")

        if duplicated_lines_pct > cls.MAX_DUPLICATION_PCT:
            violations.append(
                f"REJECTED: Duplicated lines density ({duplicated_lines_pct:.1f}%) exceeds limit of {cls.MAX_DUPLICATION_PCT}%."
            )

        passed = len(violations) == 0
        summary = {
            "combined_coverage_pct": round(comb_cov, 2),
            "branch_coverage_pct": round(coverage.branch_coverage_pct, 2),
            "line_coverage_pct": round(coverage.line_coverage_pct, 2),
            "max_cyclomatic_complexity": max_method_cc,
            "vulnerabilities": vulnerabilities,
            "bugs": bugs,
            "duplicated_lines_pct": duplicated_lines_pct,
            "status": "PASSED" if passed else "FAILED_GATE",
        }

        return QualityGateResult(passed=passed, violations=violations, metrics_summary=summary)


if __name__ == "__main__":
    # Example dry run in CI
    sample = CodeCoverageMetrics(
        covered_conditions_true=45,
        covered_conditions_false=42,
        covered_lines=190,
        total_branches=50,
        executable_lines=200,
    )
    res = QualityGateEngine.evaluate(
        coverage=sample,
        max_method_cc=3,
        vulnerabilities=0,
        bugs=0,
        duplicated_lines_pct=0.8,
    )
    print("Quality Gate Check:", res.metrics_summary)
    if not res.passed:
        for v in res.violations:
            print("❌", v)
        sys.exit(1)
    else:
        print("✅ QUALITY GATE PASSED: All metrics strictly satisfied.")
        sys.exit(0)
