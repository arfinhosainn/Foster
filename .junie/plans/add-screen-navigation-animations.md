---
sessionId: session-260815-134851-p5kq
---

# Requirements

### Overview & Goals
- Add consistent full-screen navigation animations across the existing shared Compose Multiplatform navigation flow.
- Make forward navigation, back navigation, modal-style destinations, and app-flow resets visually distinct without migrating from the project’s custom `Navigator`.
- Keep existing screen behavior, back-stack semantics, bottom sheets, dialogs, and platform entry points unchanged apart from transition behavior.

### Scope
#### In Scope
- Animate all route changes rendered by `shared/src/commonMain/kotlin/app/usenekko/navigation/NekkoNavHost.kt`.
- Define and document a transition recommendation for every `Screen` subtype in `shared/src/commonMain/kotlin/app/usenekko/navigation/Screen.kt`.
- Preserve the existing vertical Paywall presentation and extend the same directional behavior to other appropriate screens.
- Distinguish `navigate`, `goBack`, `replace`, and `replaceAll` so reverse navigation and auth/onboarding resets do not use the wrong animation.
- Keep existing `ModalBottomSheet` and `AlertDialog` behavior in `home/.../SettingScreen.kt` and `home/.../GroupBottomSheet.kt`; these are not full-screen navigator destinations.

#### Out of Scope
- Migrating to Jetpack Navigation 3 or replacing `Navigator` with `NavDisplay`.
- Changing route order, authentication logic, screen content, or back-stack persistence.
- Adding shared-element transitions, gesture-driven predictive back, reduced-motion support, or platform-specific animation implementations in this iteration.

### Recommended Transition Policy
 Destination / flow | Forward or open | Back or dismiss |
---|---|---|
 `Welcome`, `Name`, `Contact`, `Group`, `Reminder`, `TimeReminder`, `CustomReminder`, `AddNote`, `Notification` | Subtle horizontal slide in from the right with a light fade; this supports the linear onboarding progression and existing step indicator | Reverse horizontal slide from the left with a light fade |
 `Home` reached by onboarding completion or auth routing | Short fade with a very small scale-up; this communicates a flow reset/entry into the main app rather than another step in the stack | Not normally popped because onboarding uses `replaceAll` |
 `ContactProfile` from `Home` | Horizontal slide in from the right; it is a hierarchical detail screen | Reverse horizontal slide from the left |
 `Brainstorm` from `ContactProfile` | Vertical slide up with a subtle fade; it is a focused action/workspace launched from a contact | Vertical slide down |
 `Settings` from `Home` | Horizontal slide in from the right; it is a standard top-level secondary screen | Reverse horizontal slide from the left |
 `Paywall` from `Home` | Vertical slide up, preserving the current implementation’s modal/purchase emphasis | Vertical slide down, preserving the current implementation |
 `Account` | Vertical slide up with a fade because it is a focused account/reward destination | Vertical slide down |
 `GroupSettings` | Vertical slide up because the existing product model presents group management through a modal-style flow | Vertical slide down |
 `GroupDetail` | Horizontal detail slide if used as a full-screen route | Reverse horizontal slide |
 Settings account/group sheets and move/delete dialogs | Continue using Material 3 sheet/dialog transitions owned by `SettingScreen.kt` and `GroupBottomSheet.kt` | Existing Material 3 dismissal behavior |

### Acceptance Criteria
- Every full-screen route change has a deliberate enter/exit animation; no route falls back to the current `EnterTransition.None`/`ExitTransition.None` behavior unintentionally.
- `AnimatedContent` receives one atomic target value containing the screen, operation, and stack depth, so rapid mutations cannot pair a screen with stale direction metadata.
- Forward and backward animations are visually reciprocal, with back transitions styled from the outgoing/deeper screen rather than the destination screen.
- `Paywall` retains its vertical presentation, `replace` is treated as a target-directed screen swap, and `replaceAll` flows use a non-stack-oriented reset transition.
- The animation policy is centralized, exhaustive over `Screen`, and readable rather than duplicated in each screen composable.
- Rapid repeated navigation does not leave the host with a stale direction, incorrect z-order, or an inconsistent displayed screen.

### Branching
- Requested implementation branch: `feature/screen-navigation-animations`.
- Branch creation is deferred until implementation begins because this planning-only session does not perform repository or source changes.

# Technical Design

### Current Implementation
- `shared/src/commonMain/kotlin/app/usenekko/navigation/Navigator.kt` owns a `SnapshotStateList<Screen>` and exposes `navigate`, `replace`, `replaceAll`, and `goBack`; it currently exposes only `currentScreen` and has no atomic transition metadata.
- `shared/src/commonMain/kotlin/app/usenekko/navigation/NekkoNavHost.kt` renders `navigator.currentScreen` through `AnimatedContent`; only `Screen.Paywall` has a vertical transition, while all other transitions are disabled.
- `shared/src/commonMain/kotlin/app/usenekko/navigation/Screen.kt` is a sealed hierarchy containing the current route variants, allowing the transition classifier to use an exhaustive `when` without a fallback.
- `onboarding/src/commonMain/kotlin/app/usenekko/onboarding/OnboardingApp.kt` has concrete `replaceAll` flow-reset call sites at onboarding completion and authentication/account recovery; production code currently has no `replace` call site, so its swap semantics remain a forward-compatible contract.
- `shared/src/commonMain/kotlin/app/usenekko/App.kt` wraps `NekkoNavHost` in the shared theme and is the common entry point used by `onboarding/src/commonMain/kotlin/app/usenekko/onboarding/OnboardingApp.kt`.
- `OnboardingApp.kt` is the route composition root: onboarding steps use `navigate`, completion/auth recovery uses `replaceAll`, detail/settings screens use `navigate`, and the reward overlay can navigate to `Screen.Account`.
- `shared/src/commonMain/kotlin/app/usenekko/navigation/RememberNavigator.kt` serializes only the screen stack, so transition metadata must remain transient and must not become part of the persisted navigation contract.
- Existing animation conventions include `PlantRewardOverlay.kt` using `AnimatedContent` with `fadeIn`/`fadeOut` and spring-based scale transitions, and Material 3 `ModalBottomSheet`/`AlertDialog` transitions in the settings UI.

### Key Decisions
- **Keep the custom navigation architecture:** implement transitions around the existing `AnimatedContent` host rather than introducing Navigation 3; the current app is shared across Android and iOS and already has the required `compose-animation` dependency in `shared/build.gradle.kts`.
- **Bundle navigation metadata atomically:** expose an observable `NavState(screen, operation, depth, zIndex)` from `Navigator` and update the stack plus this state in the same navigation mutation. `NekkoNavHost` must target the complete value and use `contentKey = { it.screen }`, preventing metadata-only changes from replaying content transitions.
- **Separate stack operations:** classify `navigate` as `Forward`, successful `goBack` as `Backward`, `replace` as `Replace`, and `replaceAll` as `ResetStack`. `Replace` uses the target screen’s forward-oriented style, while `ResetStack` uses fade/scale flow-reset motion.
- **Let the deeper screen own back styling:** the policy selects the target screen for `Forward`/`Replace`, but the outgoing initial screen for `Backward`; this makes `Paywall`, `Account`, and `Brainstorm` slide down when dismissed even when the target is `Home`.
- **Derive z-order from the operation:** store a transient `zIndex` in `NavState`, increment it for `Forward`, `Replace`, and `ResetStack`, and decrement it for `Backward`; do not use raw stack depth because reset targets can have a shallower depth while still needing to render above the outgoing screen.
- **Return styles, not Compose transitions:** a sealed `ScreenTransitionStyle` is pure and unit-testable; only `NekkoNavHost.kt` maps it to `EnterTransition`/`ExitTransition` primitives.
- **Treat overlays separately:** do not wrap `SettingScreen`’s appearance/account/group/delete sheets or `GroupBottomSheet`’s nested group member view in the full-screen route policy; their existing Material 3 transition ownership is the correct boundary.

### Proposed Changes
1. Refactor `Navigator` around an observable `NavState` containing the current `Screen`, `NavigationOperation`, stack `depth`, and transient `zIndex`; update the stack and state together for successful `navigate`, `goBack`, `replace`, and `replaceAll` mutations. Initialize constructors and restored stacks with `operation = ResetStack`, `depth = backStack.size`, and a neutral z-index. Keep `NavigatorSaver` unchanged so restored stacks do not restore stale transition metadata.
2. Add a pure transition-style decision layer in `shared/src/commonMain/kotlin/app/usenekko/navigation` that maps `(operation, initialState, targetState)` to sealed styles. Select style ownership from the target on forward/swap and from the outgoing initial screen on back.
3. Make the `Screen` classification exhaustive with no fallback branch, covering every subtype in `Screen.kt`; compile-time exhaustiveness must prevent a new route from silently receiving no animation.
4. Update `NekkoNavHost.kt` to target the complete `NavState`, use `contentKey = { it.screen }`, and return a `ContentTransform` from `transitionSpec`. Set `targetContentZIndex` from the operation-derived `targetState.zIndex` inside that `ContentTransform`, and set `sizeTransform = SizeTransform(clip = false)` so vertical/modal-style motion is layered correctly.
5. Map pure styles to common Compose transitions using horizontal slides with light fade, vertical slides with fade, and reset fade/scale. Keep `Paywall` direction-aware for both `navigate` and `goBack`; use reset motion for `replaceAll` into `Home`, `Welcome`, or recovered onboarding steps.
6. Centralize objective animation specs in one `NavAnimationSpecs` object: 300 ms horizontal motion with `FastOutSlowInEasing`, 400 ms vertical motion, and 200 ms reset fade with scale from `0.92f` to `1f`. Offset the outgoing horizontal content by only about 30% on forward to create a restrained parallax effect.
7. Leave `OnboardingApp.kt` route callbacks and screen content unchanged; its existing `navigate`, `goBack`, and `replaceAll` operations should supply the metadata automatically.

### Data Model / Contracts
```kotlin
enum class NavigationOperation {
    Forward,
    Backward,
    Replace,
    ResetStack,
}

data class NavState(
    val screen: Screen,
    val operation: NavigationOperation,
    val depth: Int,
    val zIndex: Int,
)
```

`Navigator.navState` is the single observable value consumed by `NekkoNavHost`; `currentScreen` may remain as a compatibility accessor derived from it. The stack mutation and `NavState` update must occur as one navigation write. Constructors and restored stacks initialize `operation = ResetStack`, `depth = backStack.size`, and a neutral `zIndex`. `navigate`, `replace`, and `replaceAll` assign a z-index above the outgoing state; successful `goBack` assigns one below it. `targetContentZIndex` is then set from `targetState.zIndex`, not from raw depth.

The transition policy should conceptually expose a pure mapping such as:

```kotlin
fun transitionStyle(
    initial: NavState,
    target: NavState,
): ScreenTransitionStyle
```

`NekkoNavHost` maps the returned pure style to Compose transitions inside `transitionSpec` by constructing `ContentTransform(targetContentEnter = ..., initialContentExit = ..., targetContentZIndex = targetState.zIndex.toFloat(), sizeTransform = SizeTransform(clip = false))`.

The policy uses `target.screen` for `Forward`/`Replace` and `initial.screen` for `Backward`. `replaceAll` maps to `ResetStack`; a failed `goBack()` must not change `navState`. `ScreenTransitionStyle` is sealed and includes horizontal, vertical, and reset styles; it contains no `EnterTransition`/`ExitTransition` values. The existing serialized `List<Screen>` contract in `RememberNavigator.kt` remains unchanged.

### File Structure
- **Modify:** `shared/src/commonMain/kotlin/app/usenekko/navigation/Navigator.kt` to expose atomic `NavState` metadata, initialize constructor/restored state as `ResetStack` with `depth = backStack.size`, and preserve `currentScreen`, stack APIs, and saveable stack behavior.
- **Modify:** `shared/src/commonMain/kotlin/app/usenekko/navigation/NekkoNavHost.kt` to consume the complete state and construct z-ordered, unclipped directional `ContentTransform` values inside `transitionSpec`.
- **Add:** a transition-policy/spec file under `shared/src/commonMain/kotlin/app/usenekko/navigation/` containing `NavigationOperation`, `NavState`/style contracts if not colocated, exhaustive route classification, pure style selection, and `NavAnimationSpecs`.
- **Modify or extend tests:** `shared/src/commonTest/kotlin/app/usenekko/navigation/NavigatorTest.kt` and add a common policy test file for operation/depth/z-index updates and style selection.
- **Review only:** `shared/src/commonMain/kotlin/app/usenekko/navigation/Screen.kt`, `shared/src/commonMain/kotlin/app/usenekko/navigation/RememberNavigator.kt`, and `onboarding/src/commonMain/kotlin/app/usenekko/onboarding/OnboardingApp.kt` to ensure all route cases and persistence behavior remain aligned.
- **Do not modify for full-screen transitions:** `home/.../SettingScreen.kt`, `home/.../GroupBottomSheet.kt`, and `home/.../presentation/badges/PlantRewardOverlay.kt`, except if a narrowly scoped integration adjustment is needed.

### Risks and Mitigations
- **Direction races from rapid taps:** make `NavState` the sole `AnimatedContent` target, update it with the stack mutation, and use `contentKey = { it.screen }` so metadata changes cannot trigger unrelated content replays.
- **Incorrect back styling:** derive a backward transition from `initialState.screen`, the dismissing/deeper route, rather than from the destination `targetState.screen`.
- **Incorrect layering or clipping:** compute a transient operation-aware z-index in `NavState`, pass it as `ContentTransform.targetContentZIndex` inside `transitionSpec`, and use `SizeTransform(clip = false)` for slides that must remain visible outside their measured bounds.
- **Incorrect restoration animation:** leave operation/depth/z-index metadata out of `NavigatorSaver` and initialize restored navigation with neutral reset-style metadata.
- **Nested overlays being animated twice:** keep bottom sheets/dialogs outside `NekkoNavHost`’s route policy.
- **Cross-platform API differences:** use common Compose animation APIs already used by `shared` and avoid Android-only transition APIs.

# Testing

### Validation Approach
- Add/extend common navigation tests for atomic `NavState` operation/depth/z-index updates and transition-style classification, alongside `shared/src/commonTest/kotlin/app/usenekko/navigation/NavigatorTest.kt`.
- Run the shared/common test suite and compile the Android target; verify the same common navigation code remains usable by the iOS entry point in `onboarding/src/iosMain/kotlin/app/usenekko/MainViewController.kt`.
- Perform an Android visual smoke pass through onboarding, Home, detail, settings, Paywall, and reward flows to confirm direction, z-order, unclipped sizing, and back behavior.
- During the smoke pass, verify the incoming screen’s `LaunchedEffect`-driven analytics or data loading does not begin noticeably before it is visible, and that outgoing work is not cancelled unexpectedly during the transition.

### Key Scenarios
- Navigate through `Name` → `Contact` → `Group` and confirm right-to-left progression with the reverse animation on back.
- Navigate `Home` → `ContactProfile` → `Brainstorm` and confirm detail/action-specific styles, then return through both back actions.
- Navigate `Home` → `Settings` and `Home` → `Paywall`; confirm Settings uses horizontal hierarchy motion while Paywall uses vertical presentation motion.
- Complete onboarding through `Notification` → `Home` via `replaceAll` and confirm the main app uses a short fade/scale rather than a directional stack slide.
- Trigger account deletion back to `Welcome` via `replaceAll` and confirm the reset does not animate as a normal back navigation.
- Exercise a direct `replace` when available and confirm it uses the target route’s forward-oriented style rather than reset motion.
- Open appearance, account, group, delete-account, and move-member overlays and confirm their Material 3 sheet/dialog transitions are not doubled by the full-screen host.
- Observe a screen with `LaunchedEffect`-driven work during a transition and confirm incoming work does not start noticeably before visibility and outgoing work is not cancelled unexpectedly.

### Edge Cases
- Attempt `goBack()` at the root and verify no operation, depth, z-index, or animation state changes.
- Restore a saved stack through `rememberNavigator` and verify it initializes with `ResetStack`, `depth = backStack.size`, and neutral transient z-index metadata rather than replaying the previous session’s transition direction.
- Rapidly tap navigation actions and verify the final `navState.screen`, operation, z-order, and displayed content remain consistent.
- Navigate to parameterized `ContactProfile`, `Brainstorm`, and `GroupDetail` routes and verify route arguments remain intact while the transition runs.

# Delivery Steps

### ✓ Step 1: Add atomic navigation state tracking
`Navigator` exposes a single transient `NavState` value that keeps the displayed screen, operation, stack depth, and operation-aware z-index synchronized without changing saved-stack serialization.

- Update `shared/src/commonMain/kotlin/app/usenekko/navigation/Navigator.kt` with `NavigationOperation` values `Forward`, `Backward`, `Replace`, and `ResetStack`, plus `NavState(screen, operation, depth, zIndex)`.
- Initialize constructor and restored-stack state with `operation = ResetStack`, `depth = backStack.size`, and neutral z-index metadata.
- Update the stack and `NavState` together in `navigate`, successful `goBack`, `replace`, and `replaceAll`; assign z-order above the outgoing state for forward/swap/reset operations and below it for back; leave the state unchanged for failed root back actions.
- Preserve `currentScreen`, `canGoBack`, constructor behavior, and the `List<Screen>` persistence contract in `shared/src/commonMain/kotlin/app/usenekko/navigation/RememberNavigator.kt`.
- Extend `shared/src/commonTest/kotlin/app/usenekko/navigation/NavigatorTest.kt` for initialization, operation/depth/z-index updates, `replace` versus `replaceAll`, and root/back-stack behavior.

### ✓ Step 2: Implement an exhaustive, pure transition policy
`shared` defines testable style decisions and fixed specs for every `Screen` route without exposing Compose transition objects to policy tests.

- Add or colocate `ScreenTransitionStyle`, the exhaustive `Screen` classifier, and `NavAnimationSpecs` under `shared/src/commonMain/kotlin/app/usenekko/navigation/`.
- Classify onboarding routes, Home reset entry, hierarchical detail/settings routes, vertical action routes, Paywall, Account, and group routes according to the approved transition matrix.
- Select the target screen’s style for `Forward`/`Replace` and the initial/outgoing screen’s style for `Backward`; map `ResetStack` to fade/scale.
- Pin 300 ms horizontal, 400 ms vertical, and 200 ms reset timing, including the 30% outgoing parallax offset.
- Add common policy tests for every route family, forward/backward symmetry, and `replaceAll` reset behavior; rely on the sealed `Screen` compiler exhaustiveness check rather than duplicating it as a runtime test.

### ✓ Step 3: Wire layered animations into the shared host
`NekkoNavHost` animates full-screen route changes with atomic direction, reciprocal back motion, correct z-order, and preserved overlay boundaries.

- Replace the current Paywall-only conditional in `shared/src/commonMain/kotlin/app/usenekko/navigation/NekkoNavHost.kt` with `targetState = navigator.navState` and `contentKey = { it.screen }`.
- Map `ScreenTransitionStyle` to common `slideInHorizontally`, `slideOutHorizontally`, `slideInVertically`, `slideOutVertically`, fade, and scale transitions.
- Build a `ContentTransform` inside `transitionSpec`, setting `targetContentZIndex = targetState.zIndex.toFloat()` and `sizeTransform = SizeTransform(clip = false)` so forward/reset content layers above and backward content dismisses above the destination without clipping.
- Preserve Paywall’s vertical presentation, apply vertical behavior to Account/Brainstorm/GroupSettings as specified, and use fade/scale for `replaceAll` resets.
- Verify `onboarding/src/commonMain/kotlin/app/usenekko/onboarding/OnboardingApp.kt` callbacks continue to drive metadata without changing screen content.
- Keep `SettingScreen.kt`, `GroupBottomSheet.kt`, and `PlantRewardOverlay.kt` on their existing Material 3/local animation boundaries, then run common tests, Android compilation, and the visual/lifecycle smoke scenarios.