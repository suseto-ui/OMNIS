"""
O.M.N.I.S. Backend Authorization & Strategy Pattern Module.
Implements the Strategy Pattern and Extract Method techniques to maintain
cyclomatic complexity strictly below 3 across all auth and policy checks.
"""

from __future__ import annotations
from abc import ABC, abstractmethod
from enum import Enum
from typing import Dict, Set, Optional
import hashlib
import hmac


class UserRole(str, Enum):
    ADMIN_OPERATOR = "ADMIN_OPERATOR"
    STANDARD_USER = "STANDARD_USER"
    AUDITOR = "AUDITOR"


class SystemPermission(str, Enum):
    ACCESS_SYSTEM_ACTIONS = "ACCESS_SYSTEM_ACTIONS"
    ACCESS_DEV_DIAGNOSTIC = "ACCESS_DEV_DIAGNOSTIC"
    MODIFY_SYSTEM_STATE = "MODIFY_SYSTEM_STATE"
    EXECUTE_DATA_SYNC = "EXECUTE_DATA_SYNC"
    AUDIT_GOVERNANCE = "AUDIT_GOVERNANCE"
    ACCESS_KNOWLEDGE_BASE = "ACCESS_KNOWLEDGE_BASE"
    ACCESS_TELEMETRY = "ACCESS_TELEMETRY"


class UserAuthorizationStrategy(ABC):
    """
    Polymorphic Strategy Interface for authorization.
    Eliminates cascaded if-else conditionals.
    """

    @property
    @abstractmethod
    def role(self) -> UserRole:
        pass

    @property
    @abstractmethod
    def max_concurrent_transactions(self) -> int:
        pass

    @abstractmethod
    def has_permission(self, permission: SystemPermission) -> bool:
        pass

    @abstractmethod
    def can_access_system_actions(self) -> bool:
        pass

    @abstractmethod
    def can_access_dev_diagnostic(self) -> bool:
        pass

    @abstractmethod
    def can_modify_system_state(self) -> bool:
        pass

    @abstractmethod
    def can_execute_transaction(self, transaction_type: str) -> bool:
        pass

    @abstractmethod
    def validate_credentials(self, secret: str) -> bool:
        pass

    @abstractmethod
    def get_allowed_endpoints(self) -> Set[str]:
        pass


class AdminOperatorStrategy(UserAuthorizationStrategy):
    """Admin / Operator Strategy - Full privileges."""

    @property
    def role(self) -> UserRole:
        return UserRole.ADMIN_OPERATOR

    @property
    def max_concurrent_transactions(self) -> int:
        return 10

    _allowed_endpoints: Set[str] = {
        "/api/admin/diagnostics",
        "/api/admin/sync",
        "/api/admin/resilience",
        "/api/admin/governance",
        "/api/dev/token-telemetry",
        "/api/dev/reset-tokens",
        "/api/query",
    }

    _valid_secrets: Set[str] = {
        "omnis2026",
        "root_operator",
        "admin",
    }

    def has_permission(self, permission: SystemPermission) -> bool:
        return True

    def can_access_system_actions(self) -> bool:
        return True

    def can_access_dev_diagnostic(self) -> bool:
        return True

    def can_modify_system_state(self) -> bool:
        return True

    def can_execute_transaction(self, transaction_type: str) -> bool:
        return True

    def validate_credentials(self, secret: str) -> bool:
        if not secret or not secret.strip():
            return False
        return secret.strip() in self._valid_secrets

    def get_allowed_endpoints(self) -> Set[str]:
        return self._allowed_endpoints


class StandardUserStrategy(UserAuthorizationStrategy):
    """Standard User Strategy - Limited to query and knowledge base."""

    @property
    def role(self) -> UserRole:
        return UserRole.STANDARD_USER

    @property
    def max_concurrent_transactions(self) -> int:
        return 3

    _allowed_endpoints: Set[str] = {
        "/api/query",
        "/api/feedback",
        "/api/memory",
        "/api/conversations",
    }

    _permitted_transactions: Set[str] = {
        "db_connectivity_test",
        "chat_inference",
        "query_memory",
    }

    def has_permission(self, permission: SystemPermission) -> bool:
        return permission in {
            SystemPermission.ACCESS_KNOWLEDGE_BASE,
            SystemPermission.ACCESS_TELEMETRY,
        }

    def can_access_system_actions(self) -> bool:
        return False

    def can_access_dev_diagnostic(self) -> bool:
        return False

    def can_modify_system_state(self) -> bool:
        return False

    def can_execute_transaction(self, transaction_type: str) -> bool:
        return transaction_type in self._permitted_transactions

    def validate_credentials(self, secret: str) -> bool:
        return True

    def get_allowed_endpoints(self) -> Set[str]:
        return self._allowed_endpoints


class AuditorStrategy(UserAuthorizationStrategy):
    """Auditor Strategy - Focus on compliance and read-only telemetry."""

    @property
    def role(self) -> UserRole:
        return UserRole.AUDITOR

    @property
    def max_concurrent_transactions(self) -> int:
        return 5

    _allowed_endpoints: Set[str] = {
        "/api/admin/governance",
        "/api/dev/token-telemetry",
        "/api/query",
    }

    def has_permission(self, permission: SystemPermission) -> bool:
        return permission in {
            SystemPermission.AUDIT_GOVERNANCE,
            SystemPermission.ACCESS_TELEMETRY,
            SystemPermission.ACCESS_KNOWLEDGE_BASE,
        }

    def can_access_system_actions(self) -> bool:
        return False

    def can_access_dev_diagnostic(self) -> bool:
        return True

    def can_modify_system_state(self) -> bool:
        return False

    def can_execute_transaction(self, transaction_type: str) -> bool:
        return transaction_type in {"ai_governance_export", "db_connectivity_test"}

    def validate_credentials(self, secret: str) -> bool:
        return True

    def get_allowed_endpoints(self) -> Set[str]:
        return self._allowed_endpoints


class StrategyRegistry:
    """Strategy registry providing O(1) lookup with CC = 1."""

    _strategies: Dict[UserRole, UserAuthorizationStrategy] = {
        UserRole.ADMIN_OPERATOR: AdminOperatorStrategy(),
        UserRole.STANDARD_USER: StandardUserStrategy(),
        UserRole.AUDITOR: AuditorStrategy(),
    }

    @classmethod
    def get_strategy(cls, role: UserRole) -> UserAuthorizationStrategy:
        return cls._strategies.get(role, cls._strategies[UserRole.STANDARD_USER])
