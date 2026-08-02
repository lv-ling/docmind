import asyncio

from docmind_ai.evaluation import run_evaluation


def test_fixed_mock_evaluation_passes() -> None:
    summary = asyncio.run(run_evaluation())

    assert summary.total_cases == 3
    assert summary.passed_cases == 3
    assert summary.data_accuracy == 1
    assert summary.review_accuracy == 1
    assert summary.schema_pass_rate == 1
    assert summary.failed_case_ids == []
