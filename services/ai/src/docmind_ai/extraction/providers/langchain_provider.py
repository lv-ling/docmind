from __future__ import annotations

import json

from langchain_core.language_models.chat_models import BaseChatModel
from langchain_core.messages import HumanMessage, SystemMessage

from docmind_ai.contracts.extraction import AiExtractionRequest, ModelExtractionOutput
from docmind_ai.extraction.providers.base import (
    ProviderPermanentError,
    ProviderResult,
    ProviderTransientError,
)


class LangChainChatProvider:
    def __init__(
        self,
        *,
        chat_model: BaseChatModel,
        provider_name: str,
        model_name: str,
        prompt_version: str,
    ) -> None:
        self._chat_model = chat_model
        self._provider_name = provider_name
        self._model_name = model_name
        self._prompt_version = prompt_version

    async def extract(self, request: AiExtractionRequest) -> ProviderResult:
        structured_model = self._chat_model.with_structured_output(ModelExtractionOutput)
        safe_payload = {
            "fields": [field.model_dump(mode="json") for field in request.fields],
            "json_schema": request.json_schema,
            "document": request.document.model_dump(mode="json"),
        }
        messages = [
            SystemMessage(
                content=(
                    "Extract only from the tokenized document. Return the requested JSON schema, "
                    "cite exact tokenized evidence, preserve every sensitive placeholder exactly, "
                    "and never invent or transform placeholders."
                )
            ),
            HumanMessage(
                content=json.dumps(safe_payload, ensure_ascii=False, separators=(",", ":"))
            ),
        ]
        try:
            raw_result = await structured_model.ainvoke(messages)
        except TimeoutError as exception:
            raise ProviderTransientError("模型调用超时") from exception
        except Exception as exception:
            raise ProviderTransientError("模型调用失败") from exception
        try:
            output = (
                raw_result
                if isinstance(raw_result, ModelExtractionOutput)
                else ModelExtractionOutput.model_validate(raw_result)
            )
        except Exception as exception:
            raise ProviderPermanentError("模型未返回有效结构") from exception
        return ProviderResult(
            output=output,
            provider=self._provider_name,
            model=self._model_name,
            prompt_version=self._prompt_version,
        )
