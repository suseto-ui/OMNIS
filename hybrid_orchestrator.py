import os
from fastapi import APIRouter, HTTPException, status
from google import genai
from google.genai import types
from backend.schemas.hybrid_engine import UnifiedOmnisResponse

router = APIRouter(prefix="/api/v1/omnis", tags=["OMNIS Hybrid Core"])

# Inicializace klienta (předpokládá nastavené prostředí GEMINI_API_KEY)
client = genai.Client(api_key=os.getenv("GEMINI_API_KEY"))

OMNIS_SYSTEM_INSTRUCTION = """
Jsi OMNIS Core Orchestrator. Tvým úkolem je analyzovat vstup a v JEDNOM cyklu:
1. Určit IntentType (kategorii záměru).
2. Sestavit exekuční plán (PlanStep).
3. Specifikovat parametry nástrojů v ToolCallSpec.

Pracuj v režimu 'Zero-Defect'. Pokud je dotaz nejednoznačný, vyžádej si upřesnění v immediate_response.
"""

@router.post("/process", response_model=UnifiedOmnisResponse)
async def process_hybrid_intent(user_input: str) -> UnifiedOmnisResponse:
    """
    Single-turn hybridní endpoint pro zpracování kognitivního toku.
    """
    if not user_input.strip():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Vstupní parametr nesmí být prázdný."
        )

    try:
        response = client.models.generate_content(
            model="gemini-2.0-flash", # Doporučeno pro rychlost hybridní orchestrace
            contents=user_input,
            config=types.GenerateContentConfig(
                system_instruction=OMNIS_SYSTEM_INSTRUCTION,
                response_mime_type="application/json",
                response_schema=UnifiedOmnisResponse,
                temperature=0.1, # Nízká teplota pro deterministický plán
            ),
        )
        
        # Validace a parsování
        result = UnifiedOmnisResponse.model_validate_json(response.text)
        return result

    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Selhání kognitivní syntézy: {str(e)}"
        )