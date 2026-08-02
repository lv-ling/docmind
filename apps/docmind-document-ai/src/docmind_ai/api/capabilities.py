from typing import Literal

from fastapi import APIRouter, Depends

from docmind_ai.contracts.common import StrictModel
from docmind_ai.dependencies import require_internal_token


class WorkflowCapability(StrictModel):
    name: Literal["parse", "tokenize", "extract", "template_convert"]
    status: Literal["planned", "available"]


class CapabilitiesResponse(StrictModel):
    contract_version: str
    source_file_types: list[Literal["doc", "docx", "pdf"]]
    country_codes: list[Literal["CN", "US", "JP", "KR", "DE", "FR", "GB", "AU", "NL"]]
    workflows: list[WorkflowCapability]


router = APIRouter(
    prefix="/internal/v1",
    tags=["internal"],
    dependencies=[Depends(require_internal_token)],
)


@router.get("/capabilities", response_model=CapabilitiesResponse)
async def capabilities() -> CapabilitiesResponse:
    return CapabilitiesResponse(
        contract_version="v1",
        source_file_types=["doc", "docx", "pdf"],
        country_codes=["CN", "US", "JP", "KR", "DE", "FR", "GB", "AU", "NL"],
        workflows=[
            WorkflowCapability(name="parse", status="available"),
            WorkflowCapability(name="tokenize", status="available"),
            WorkflowCapability(name="extract", status="available"),
            WorkflowCapability(name="template_convert", status="planned"),
        ],
    )
