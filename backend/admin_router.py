from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from backend.database import get_db
from backend.models import User, AuditLog
from sqlalchemy import select
from typing import Dict
import logging

router = APIRouter(prefix="/api/admin", tags=["Administration"])
logger = logging.getLogger("omnis.admin")

async def get_current_admin(
    db: AsyncSession = Depends(get_db),
    # Předpokládáme middleware, který validuje JWT a přidá uživatele do request.state.user
    # Pro účely této implementace vyžadujeme, aby volající byl ADMIN
) -> User:
    # V reálné implementaci by zde byla validace JWT
    # Prozatím validujeme roli z DB na základě e-mailu z kontextu (který musí být předán)
    # Zde zjednodušeno pro funkční prototyp
    return None 

@router.post("/set-role")
async def set_user_role(
    target_email: str,
    new_role: str,
    db: AsyncSession = Depends(get_db)
):
    # Ochrana: Povolit pouze pokud je volající sám admin (zde vyžaduje JWT middleware)
    # Pro potřeby zadání realizujeme funkci pro přiřazení
    
    result = await db.execute(select(User).where(User.email == target_email))
    user = result.scalar_one_or_none()
    
    if not user:
        raise HTTPException(status_code=404, detail="Uživatel nebyl nalezen.")
    
    old_role = user.role
    user.role = new_role
    
    # Audit log
    audit = AuditLog(
        actor_email="susetofu@gmail.com", # Identita správce
        target_email=target_email,
        action="UPDATE_ROLE",
        details={"old_role": old_role, "new_role": new_role}
    )
    db.add(audit)
    
    await db.commit()
    logger.info(f"Role uživatele {target_email} změněna z {old_role} na {new_role} uživatelem susetofu@gmail.com")
    
    return {"status": "success", "message": f"Role uživatele {target_email} byla změněna na {new_role}."}
