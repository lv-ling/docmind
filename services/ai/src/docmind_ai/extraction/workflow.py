from __future__ import annotations

import asyncio
from typing import TypedDict, cast

from langgraph.graph import END, START, StateGraph

from docmind_ai.contracts.extraction import (
    AiExtractionRequest,
    AiExtractionResponse,
    ExtractionModelMetadata,
)
from docmind_ai.extraction.providers import (
    ExtractionProvider,
    ProviderPermanentError,
    ProviderResult,
    ProviderTransientError,
)
from docmind_ai.extraction.validation import ExtractionOutputValidator
from docmind_ai.extraction.workflow_errors import ExtractionWorkflowError


class ExtractionState(TypedDict, total=False):
    request: AiExtractionRequest
    provider_result: ProviderResult
    response: AiExtractionResponse


class ExtractionWorkflow:
    def __init__(
        self,
        *,
        provider: ExtractionProvider,
        provider_timeout_seconds: float = 30,
        total_timeout_seconds: float = 45,
        retry_attempts: int = 2,
        retry_backoff_seconds: float = 0.05,
        input_token_budget: int = 50_000,
        output_token_budget: int = 10_000,
        validator: ExtractionOutputValidator | None = None,
    ) -> None:
        self._provider = provider
        self._provider_timeout_seconds = provider_timeout_seconds
        self._total_timeout_seconds = total_timeout_seconds
        self._retry_attempts = retry_attempts
        self._retry_backoff_seconds = retry_backoff_seconds
        self._input_token_budget = input_token_budget
        self._output_token_budget = output_token_budget
        self._validator = validator or ExtractionOutputValidator()

        builder = StateGraph(ExtractionState)
        builder.add_node("provider", self._provider_node)
        builder.add_node("validate", self._validation_node)
        builder.add_edge(START, "provider")
        builder.add_edge("provider", "validate")
        builder.add_edge("validate", END)
        self._graph = builder.compile()

    async def run(self, request: AiExtractionRequest) -> AiExtractionResponse:
        estimated_input_tokens = _estimate_tokens(request.model_dump_json())
        if estimated_input_tokens > self._input_token_budget:
            raise ExtractionWorkflowError(
                "INPUT_TOKEN_BUDGET_EXCEEDED",
                "抽取请求超过输入 Token 预算",
                retryable=False,
            )
        try:
            async with asyncio.timeout(self._total_timeout_seconds):
                state = await self._graph.ainvoke(
                    ExtractionState(request=request),
                    config={"recursion_limit": 6},
                )
        except TimeoutError as exception:
            raise ExtractionWorkflowError(
                "EXTRACTION_TOTAL_TIMEOUT", "抽取工作流超时", retryable=True
            ) from exception
        response = state.get("response")
        if response is None:
            raise ExtractionWorkflowError(
                "EXTRACTION_RESPONSE_MISSING", "抽取工作流未产生结果", retryable=False
            )
        return cast(AiExtractionResponse, response)

    async def _provider_node(self, state: ExtractionState) -> ExtractionState:
        request = state["request"]
        result = await self._invoke_provider_with_retry(request)
        output_tokens = result.output_tokens or _estimate_tokens(result.output.model_dump_json())
        if output_tokens > self._output_token_budget:
            raise ExtractionWorkflowError(
                "OUTPUT_TOKEN_BUDGET_EXCEEDED",
                "模型输出超过 Token 预算",
                retryable=False,
            )
        return ExtractionState(provider_result=result)

    async def _validation_node(self, state: ExtractionState) -> ExtractionState:
        request = state["request"]
        result = state["provider_result"]
        validation_errors = self._validator.validate(request, result.output)
        return ExtractionState(
            response=AiExtractionResponse(
                request_id=request.request_id,
                job_id=request.job_id,
                extraction_run_id=request.extraction_run_id,
                result=result.output,
                model=ExtractionModelMetadata(
                    provider=result.provider,
                    model=result.model,
                    prompt_version=result.prompt_version,
                    input_tokens=result.input_tokens,
                    output_tokens=result.output_tokens,
                ),
                validation_errors=validation_errors,
            )
        )

    async def _invoke_provider_with_retry(self, request: AiExtractionRequest) -> ProviderResult:
        last_error: BaseException | None = None
        for attempt in range(self._retry_attempts + 1):
            try:
                async with asyncio.timeout(self._provider_timeout_seconds):
                    return await self._provider.extract(request)
            except ProviderPermanentError as exception:
                raise ExtractionWorkflowError(
                    "MODEL_OUTPUT_INVALID", "模型未返回有效结构", retryable=False
                ) from exception
            except (ProviderTransientError, TimeoutError) as exception:
                last_error = exception
                if attempt < self._retry_attempts:
                    await asyncio.sleep(self._retry_backoff_seconds * (2**attempt))
        raise ExtractionWorkflowError(
            "MODEL_PROVIDER_UNAVAILABLE", "模型服务暂时不可用", retryable=True
        ) from last_error


def _estimate_tokens(text: str) -> int:
    return max(1, (len(text) + 3) // 4)
