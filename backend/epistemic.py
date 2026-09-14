from fastapi import APIRouter, BackgroundTasks
from backend.schemas import OmnisEntityIngestion
from backend.memory import background_record_vector_memory

epistemic_router = APIRouter(prefix="/api/epistemic", tags=["OMNIS Epistemic Layer"])

@epistemic_router.post("/ingest")
async def ingest_epistemic_data(
    payload: OmnisEntityIngestion,
    background_tasks: BackgroundTasks,
) -> dict:
    summary = (
        f"Entity: {payload.omnis_entity_id} | Purpose: {payload.fundamental_purpose} | "
        f"Hard: {list(payload.epistemic_data_layer.hard_data.parameters.keys())} | "
        f"Soft: {list(payload.epistemic_data_layer.soft_data.parameters.keys())}"
    )

    background_tasks.add_task(
        background_record_vector_memory, conversation_id=None, content=f"[Epistemic Ingest]: {summary}", memory_type="epistemic_entity", metadata_json=payload.model_dump(),
    )
    return {
        "status": "assimilated", "omnis_entity_id": payload.omnis_entity_id,
        "message": "Entita byla úspěšně asimilována do epistemické roviny bez sémantického šumu.",
    }