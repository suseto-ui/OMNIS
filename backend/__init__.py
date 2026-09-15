try:
    from .autopoiesis import autopoiesis_router
    from .cognitive import cognitive_router
    from .dev import dev_router
    from .engine import engine_router
    from .epistemic import epistemic_router
    from .memory import memory_router
    from .phase4 import phase4_router
    from .system import system_router
except ImportError:
    pass
