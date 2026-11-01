from __future__ import annotations
from datetime import datetime
from typing import List, Optional, Dict, Any
from pydantic import BaseModel, Field, EmailStr

# ==========================================================
# User & Auth Schémata
# ==========================================================

class UserBase(BaseModel):
    username: str
    email: EmailStr

class UserCreate(UserBase):
    password: str

class UserLogin(BaseModel):
    username: str
    password: str

class GoogleAuthRequest(BaseModel):
    id_token: str

class Token(BaseModel):
    access_token: str
    token_type: str = "bearer"
    username: str
    role: str

class UserProfileUpdate(BaseModel):
    avatar_url: Optional[str] = None
    birth_date: Optional[datetime] = None
    bio: Optional[str] = None
    preferences_json: Optional[Dict[str, Any]] = None

class UserProfileResponse(BaseModel):
    id: str
    user_id: str
    username: str
    email: str
    avatar_url: Optional[str]
    birth_date: Optional[datetime]
    bio: Optional[str]
    preferences_json: Dict[str, Any]
    role: str
    created_at: datetime

# ==========================================================
# Mobilní Synchronizační Schémata (Mobile API Gateway Sync)
# ==========================================================

class SyncMessageItem(BaseModel):
    id: int
    role: str
    content: str
    cognitive_process: Optional[str] = ""
    follow_up_questions: Optional[str] = ""
    val_sys: float = 0.5
    val_econ: float = 0.5
    val_psych: float = 0.5
    val_eco: float = 0.5
    val_law: float = 0.5
    val_sec: float = 0.5
    val_phys: float = 0.5
    val_soc: float = 0.5
    composite_score: float = 0.5
    domain: str = "SYSTEMS_INTELLIGENCE"
    attached_image_path: Optional[str] = None
    timestamp: int
    defense_tier: str = "APPROVED"
    defense_notes: Optional[str] = ""
    thread_id: str = "thread_main"
    thread_title: str = "Hlavní vlákno"
    user_name: str = "operator"

class SyncMessagesRequest(BaseModel):
    messages: List[SyncMessageItem]
    client_version: Optional[str] = "4.5"
    device_id: Optional[str] = None

class SyncMessagesResponse(BaseModel):
    synced_count: int
    status: str = "SUCCESS"
    message: str
    timestamp: int = Field(default_factory=lambda: int(datetime.now().timestamp() * 1000))

class SyncMemoryItem(BaseModel):
    id: int
    content: str
    domain: str = "SYSTEMS_INTELLIGENCE"
    val_sys: float = 0.5
    val_econ: float = 0.5
    val_psych: float = 0.5
    val_eco: float = 0.5
    val_law: float = 0.5
    val_sec: float = 0.5
    val_phys: float = 0.5
    val_soc: float = 0.5
    importance_score: float = 1.0
    timestamp: int
    user_name: str = "operator"

class SyncMemoryRequest(BaseModel):
    fragments: List[SyncMemoryItem]

class SyncMemoryResponse(BaseModel):
    synced_count: int
    status: str = "SUCCESS"
    message: str

class SyncTelemetryItem(BaseModel):
    level: str
    tag: str
    message: str
    metadata_json: Optional[str] = ""
    timestamp: int

class SyncTelemetryRequest(BaseModel):
    logs: List[SyncTelemetryItem]

class SyncTelemetryResponse(BaseModel):
    synced_count: int
    status: str = "SUCCESS"

class HealthCheckResponse(BaseModel):
    status: str
    database: Dict[str, Any]
    service: str = "O.M.N.I.S. Core Gateway"
    version: str = "4.5"
    timestamp: int = Field(default_factory=lambda: int(datetime.now().timestamp() * 1000))

# ==========================================================
# Self-Healing & Prompt Logic Korekční Schémata
# ==========================================================

class PromptCorrectionRecord(BaseModel):
    original_prompt: str
    detected_inconsistencies: List[str] = Field(default_factory=list)
    corrected_prompt: str
    confidence_score: float = Field(default=0.95, ge=0.0, le=1.0)
    prompt_health_index: float = Field(default=98.5, ge=0.0, le=100.0)
    healing_actions_applied: List[str] = Field(default_factory=list)
    requires_user_authorization: bool = False

class SelfHealingRequest(BaseModel):
    prompt: str
    domain: Optional[str] = "SYSTEMS_INTELLIGENCE"
    user_role: Optional[str] = "ADMIN_OPERATOR"

class SelfHealingResponse(BaseModel):
    status: str = "SUCCESS"
    correction: PromptCorrectionRecord
    timestamp: int = Field(default_factory=lambda: int(datetime.now().timestamp() * 1000))

# ==========================================================
# Co-Synthesis & Pre-Execution Analysis Schémata
# ==========================================================

class CoSynthesisRequest(BaseModel):
    query: str
    ontology_domain: Optional[str] = "SYSTEMS_INTELLIGENCE"
    user_role: Optional[str] = "ADMIN_OPERATOR"

class CoSynthesisResponse(BaseModel):
    status: str = "SUCCESS"
    original_query: str
    detected_gaps: List[str]
    cascade_preview: Dict[str, Any]
    completion_proposals: List[Dict[str, str]]
    timestamp: int = Field(default_factory=lambda: int(datetime.now().timestamp() * 1000))

