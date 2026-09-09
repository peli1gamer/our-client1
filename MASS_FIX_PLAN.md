# Mass bug-fix pass

Stability-first pass covering compile failures, render lifecycle safety, module registration/state synchronization, GUI input/scroll behavior, and runtime exception isolation.

## First blockers
- Remove unsupported WorldRenderContext.tickCounter() usage.
- Remove unsupported RenderSetupBuilder.translucent() usage.
- Rebuild and use CI as the gate before producing a test artifact.
