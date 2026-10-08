

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
