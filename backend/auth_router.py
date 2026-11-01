from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from google.oauth2 import id_token
from google.auth.transport import requests
import os

from .database import get_db
from .models import User, UserProfile
from .schemas import UserCreate, UserLogin, Token, GoogleAuthRequest, UserProfileResponse, UserProfileUpdate
from .security import get_password_hash, verify_password, create_access_token, decode_access_token
from fastapi.security import OAuth2PasswordBearer

from backend.gcp_secrets import get_secret

router = APIRouter(prefix="/api/auth", tags=["Authentication"])
user_router = APIRouter(prefix="/api/user", tags=["User Profile"])

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/api/auth/login")

GOOGLE_CLIENT_ID = get_secret("GOOGLE_CLIENT_ID", "")

async def get_current_user(token: str = Depends(oauth2_scheme), db: AsyncSession = Depends(get_db)):
    payload = decode_access_token(token)
    if not payload:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid token")
    
    username: str = payload.get("sub")
    if not username:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid token payload")
    
    result = await db.execute(select(User).where(User.username == username))
    user = result.scalar_one_or_none()
    if not user:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="User not found")
    return user

@router.post("/register", response_model=Token)
async def register(user_in: UserCreate, db: AsyncSession = Depends(get_db)):
    # Check existing
    result = await db.execute(select(User).where((User.username == user_in.username) | (User.email == user_in.email)))
    if result.scalar_one_or_none():
        raise HTTPException(status_code=400, detail="Username or email already registered")
    
    hashed_pw = get_password_hash(user_in.password)
    user = User(
        username=user_in.username,
        email=user_in.email,
        hashed_password=hashed_pw
    )
    db.add(user)
    await db.flush()
    
    profile = UserProfile(user_id=user.id)
    db.add(profile)
    
    await db.commit()
    await db.refresh(user)
    
    access_token = create_access_token(data={"sub": user.username, "role": user.role})
    return {"access_token": access_token, "token_type": "bearer", "username": user.username, "role": user.role}

@router.post("/login", response_model=Token)
async def login(user_in: UserLogin, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(User).where(User.username == user_in.username))
    user = result.scalar_one_or_none()
    
    if not user or not user.hashed_password or not verify_password(user_in.password, user.hashed_password):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Incorrect username or password")
    
    access_token = create_access_token(data={"sub": user.username, "role": user.role})
    return {"access_token": access_token, "token_type": "bearer", "username": user.username, "role": user.role}

@router.post("/google", response_model=Token)
async def google_auth(req: GoogleAuthRequest, db: AsyncSession = Depends(get_db)):
    try:
        idinfo = id_token.verify_oauth2_token(req.id_token, requests.Request(), GOOGLE_CLIENT_ID)
        
        email = idinfo['email']
        google_id = idinfo['sub']
        name = idinfo.get('name', email.split('@')[0])
        picture = idinfo.get('picture')
        
        result = await db.execute(select(User).where(User.google_id == google_id))
        user = result.scalar_one_or_none()
        
        if not user:
            # Fallback to email search
            result = await db.execute(select(User).where(User.email == email))
            user = result.scalar_one_or_none()
            
            if user:
                user.google_id = google_id
            else:
                role = "ADMIN_OPERATOR" if email == "susetofu@gmail.com" else "STANDARD_USER"
                user = User(
                    username=name,
                    email=email,
                    google_id=google_id,
                    role=role
                )
                db.add(user)
                await db.flush()
                profile = UserProfile(user_id=user.id, avatar_url=picture)
                db.add(profile)
        
        # Vynucení role pro hlavního vlastníka
        user_role = "ADMIN_OPERATOR" if user.email == "susetofu@gmail.com" else user.role
        if user.role != user_role:
            user.role = user_role
            await db.commit()
            await db.refresh(user)
        
        access_token = create_access_token(data={"sub": user.username, "role": user.role, "email": user.email})
        return {"access_token": access_token, "token_type": "bearer", "username": user.username, "role": user.role}
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid Google ID token")

@user_router.get("/profile", response_model=UserProfileResponse)
async def get_profile(current_user: User = Depends(get_current_user), db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(UserProfile).where(UserProfile.user_id == current_user.id))
    profile = result.scalar_one_or_none()
    
    return {
        "id": profile.id,
        "user_id": current_user.id,
        "username": current_user.username,
        "email": current_user.email,
        "avatar_url": profile.avatar_url,
        "birth_date": profile.birth_date,
        "bio": profile.bio,
        "preferences_json": profile.preferences_json,
        "role": current_user.role,
        "created_at": current_user.created_at
    }

@user_router.put("/profile", response_model=UserProfileResponse)
async def update_profile(
    profile_in: UserProfileUpdate, 
    current_user: User = Depends(get_current_user), 
    db: AsyncSession = Depends(get_db)
):
    result = await db.execute(select(UserProfile).where(UserProfile.user_id == current_user.id))
    profile = result.scalar_one_or_none()
    
    if profile_in.avatar_url is not None:
        profile.avatar_url = profile_in.avatar_url
    if profile_in.birth_date is not None:
        profile.birth_date = profile_in.birth_date
    if profile_in.bio is not None:
        profile.bio = profile_in.bio
    if profile_in.preferences_json is not None:
        profile.preferences_json = profile_in.preferences_json
        
    await db.commit()
    
    return await get_profile(current_user, db)
