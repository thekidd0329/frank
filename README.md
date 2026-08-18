# FrAnK bones

This is the platform-neutral Kotlin core for FrAnK.

Current skeleton:
- domain states and data models
- runtime/reasoning/speech/health adapter contracts
- repository contract
- central AgentController
- confirmation hook
- task pause/resume/cancel hooks
- observe -> reason -> act loop seam

The Android app, local models, AccessibilityService, speech engines, Room/DataStore,
and privileged execution layer can plug into these contracts without changing UI-facing controller logic.
