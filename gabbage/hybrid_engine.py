from enum import Enum
from typing import Any, Dict, List, Optional
from pydantic import BaseModel, Field, ConfigDict

class IntentType(str, Enum):
    LEGAL_ANALYSIS = "legal_analysis"
    CODE_AUDIT = "code_audit"
    DOCUMENT_PROCESSING = "document_processing"
    GENERAL_ASSISTANT = "general_assistant"
    DATA_EXTRACTION = "data_extraction"

class ToolCallSpec(BaseModel):
    tool_id: str = Field(..., description="Unikátní identifikátor nástroje (např. 'ocr_engine', 'vector_search')")
    parameters: Dict[str, Any] = Field(default_factory=dict, description="Vstupní argumenty pro nástroj")

class PlanStep(BaseModel):
    step_number: int = Field(..., description="Sekvenční pořadí kroku")
    action_description: str = Field(..., description="Co tento krok vykonává")
    tool_call: Optional[ToolCallSpec] = Field(None, description="Nástroj k vyvolání, pokud je krok závislý na lokální funkci")

class UnifiedOmnisResponse(BaseModel):
    """
    Hlavní kontrakt pro O.M.N.I.S. Hybrid Engine. 
    Sloučený výstup Intent + Plan + Response.
    """
    model_config = ConfigDict(extra="forbid")
    
    intent: IntentType = Field(..., description="Klasifikovaný záměr uživatele")
    confidence_score: float = Field(..., ge=0.0, le=1.0, description="Míra jistoty klasifikace")
    execution_plan: List[PlanStep] = Field(..., description="Kompletní posloupnost kroků řešení")
    immediate_response: Optional[str] = Field(
        None, 
        description="Přímá textová odpověď, pokud není nutná další externí exekuce"
    )
    required_output_format: str = Field(
        default="markdown", 
        description="Požadovaný formát (markdown, json, code)"
    )