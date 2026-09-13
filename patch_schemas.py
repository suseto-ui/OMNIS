with open("backend/schemas.py", "r") as f:
    content = f.read()

import re

# 1. Replace ImpactMatrixScores
old_ims = """class ImpactMatrixScores(BaseModel):
    \"\"\"Structured 4-dimensional Impact Matrix schema normalized (0.0 - 1.0).\"\"\"
    model_config = ConfigDict(from_attributes=True)

    economic_viability: float = Field(
        ...,
        ge=0.0,
        le=1.0,
        description="Ekonomická životaschopnost (0.0 - 1.0)",
    )
    eco_social_regeneration: float = Field(
        ...,
        ge=0.0,
        le=1.0,
        description="Ekologicko-sociální regenerace (0.0 - 1.0)",
    )
    technological_elegance: float = Field(
        ...,
        ge=0.0,
        le=1.0,
        description="Technologická elegance a modularita (0.0 - 1.0)",
    )
    psychological_acceptability: float = Field(
        ...,
        ge=0.0,
        le=1.0,
        description="Psychologická a etická přijatelnost (0.0 - 1.0)",
    )"""

new_ims = """class ImpactMatrixScores(BaseModel):
    \"\"\"Structured 8-dimensional Impact Matrix schema normalized (0.0 - 1.0).\"\"\"
    model_config = ConfigDict(from_attributes=True)

    sys: float = Field(..., ge=0.0, le=1.0, description="Systems Engineering (0.0 - 1.0)")
    econ: float = Field(..., ge=0.0, le=1.0, description="Economics (0.0 - 1.0)")
    psych: float = Field(..., ge=0.0, le=1.0, description="Psychology (0.0 - 1.0)")
    eco: float = Field(..., ge=0.0, le=1.0, description="Ecology (0.0 - 1.0)")
    law: float = Field(..., ge=0.0, le=1.0, description="Law & Regulation (0.0 - 1.0)")
    sec: float = Field(..., ge=0.0, le=1.0, description="Security (0.0 - 1.0)")
    phys: float = Field(..., ge=0.0, le=1.0, description="Physics (0.0 - 1.0)")
    soc: float = Field(..., ge=0.0, le=1.0, description="Sociology (0.0 - 1.0)")"""

content = content.replace(old_ims, new_ims)

with open("backend/schemas.py", "w") as f:
    f.write(content)
