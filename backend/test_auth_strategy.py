import pytest
from backend.auth_strategy import (
    UserRole,
    SystemPermission,
    StrategyRegistry,
    AdminOperatorStrategy,
    StandardUserStrategy,
    AuditorStrategy,
)

def test_admin_strategy_permissions():
    strategy = StrategyRegistry.get_strategy(UserRole.ADMIN_OPERATOR)
    assert strategy.role == UserRole.ADMIN_OPERATOR
    assert strategy.can_access_system_actions() is True
    assert strategy.can_access_dev_diagnostic() is True
    assert strategy.can_modify_system_state() is True
    assert strategy.has_permission(SystemPermission.ACCESS_SYSTEM_ACTIONS) is True
    assert strategy.has_permission(SystemPermission.EXECUTE_DATA_SYNC) is True
    assert strategy.validate_credentials("omnis2026") is True
    assert strategy.validate_credentials("invalid_secret") is False
    assert strategy.validate_credentials("") is False

def test_standard_user_strategy_permissions():
    strategy = StrategyRegistry.get_strategy(UserRole.STANDARD_USER)
    assert strategy.role == UserRole.STANDARD_USER
    assert strategy.can_access_system_actions() is False
    assert strategy.can_access_dev_diagnostic() is False
    assert strategy.can_modify_system_state() is False
    assert strategy.has_permission(SystemPermission.ACCESS_KNOWLEDGE_BASE) is True
    assert strategy.has_permission(SystemPermission.MODIFY_SYSTEM_STATE) is False
    assert strategy.can_execute_transaction("chat_inference") is True
    assert strategy.can_execute_transaction("postgres_auto_sync") is False
    assert strategy.validate_credentials("") is True

def test_auditor_strategy_permissions():
    strategy = StrategyRegistry.get_strategy(UserRole.AUDITOR)
    assert strategy.role == UserRole.AUDITOR
    assert strategy.can_access_system_actions() is False
    assert strategy.can_access_dev_diagnostic() is True
    assert strategy.has_permission(SystemPermission.AUDIT_GOVERNANCE) is True
    assert strategy.can_execute_transaction("ai_governance_export") is True
    assert strategy.can_execute_transaction("resilience_circuit_breaker_audit") is False

def test_registry_fallback_to_standard():
    # Unknown role string fallback
    strategy = StrategyRegistry.get_strategy("NON_EXISTENT")
    assert strategy.role == UserRole.STANDARD_USER
