import pytest
from scripts.verify_quality_gate import CodeCoverageMetrics, QualityGateEngine


def test_quality_gate_passes_when_all_criteria_met():
    # 50 branches -> 100 conditions. CT=45, CF=45 (90/100 branch covered = 90%)
    # 200 executable lines -> 180 covered (90% line covered)
    # Total formula: (45 + 45 + 180) / (100 + 200) = 270 / 300 = 90.0% >= 80.0%
    metrics = CodeCoverageMetrics(
        covered_conditions_true=45,
        covered_conditions_false=45,
        covered_lines=180,
        total_branches=50,
        executable_lines=200,
    )
    result = QualityGateEngine.evaluate(
        coverage=metrics,
        max_method_cc=4,
        vulnerabilities=0,
        bugs=0,
        duplicated_lines_pct=1.2,
    )

    assert result.passed is True
    assert len(result.violations) == 0
    assert result.metrics_summary["combined_coverage_pct"] == 90.0
    assert result.metrics_summary["status"] == "PASSED"


def test_quality_gate_rejects_when_false_branches_uncovered():
    # If developer only tested 'True' branches (CT=50, CF=0 out of 50 branches)
    # 200 executable lines, 160 lines covered
    # Total formula: (50 + 0 + 160) / (100 + 200) = 210 / 300 = 70.0% < 80.0% -> REJECTED
    metrics = CodeCoverageMetrics(
        covered_conditions_true=50,
        covered_conditions_false=0,
        covered_lines=160,
        total_branches=50,
        executable_lines=200,
    )
    result = QualityGateEngine.evaluate(
        coverage=metrics,
        max_method_cc=3,
        vulnerabilities=0,
        bugs=0,
        duplicated_lines_pct=0.0,
    )

    assert result.passed is False
    assert any("below strict barrier threshold" in v for v in result.violations)
    assert result.metrics_summary["combined_coverage_pct"] == 70.0


def test_quality_gate_rejects_high_cyclomatic_complexity():
    metrics = CodeCoverageMetrics(
        covered_conditions_true=50,
        covered_conditions_false=50,
        covered_lines=200,
        total_branches=50,
        executable_lines=200,
    )
    # CC exceeds 15
    result = QualityGateEngine.evaluate(
        coverage=metrics,
        max_method_cc=18,
        vulnerabilities=0,
        bugs=0,
    )

    assert result.passed is False
    assert any("Cyclomatic Complexity (18) exceeds limit of 15" in v for v in result.violations)


def test_quality_gate_rejects_vulnerabilities():
    metrics = CodeCoverageMetrics(
        covered_conditions_true=50,
        covered_conditions_false=50,
        covered_lines=200,
        total_branches=50,
        executable_lines=200,
    )
    result = QualityGateEngine.evaluate(
        coverage=metrics,
        max_method_cc=2,
        vulnerabilities=1,
    )

    assert result.passed is False
    assert any("vulnerabilities" in v for v in result.violations)
