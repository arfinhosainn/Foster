# Adaptive UI Phase B

## Requirements

### Overview & Goals
- Add expanded-width supporting-pane layouts on top of the completed Phase A adaptive shell.
- Preserve the existing compact and medium single-screen navigation experience and route/back-stack semantics.
- Revisit the navigation-animation behavior only where a destination is rendered as a pane instead of a full-screen route.
- Keep the custom `Navigator`; do not migrate to Navigation 3 or `NavigationSuiteScaffold`.

### Scope
#### In Scope
- Add an expanded-width Home/contact list-detail presentation: Home remains visible as the primary pane while the selected contact profile appears in a supporting pane.
- Add an expanded-width group-management presentation where group selection and member management can be viewed together when the existing flow provides both contexts.
- Keep compact and medium widths on the existing full-screen `ContactProfile` and `GroupDetail` routes.
- Preserve existing sheets, dialogs, loading states, lifecycle refresh behavior, and callbacks.
- Add state and layout tests for expanded-pane selection, compact/medium fallback, empty selection, and back behavior.
- Update the animation policy or host integration only as needed to avoid applying a full-screen route transition to content that is now an in-place supporting pane.

#### Out of Scope
- Shared-element transitions, predictive back, drag-to-resize panes, persistent multi-window state, or desktop-specific input behavior.
- Changing the `Screen` hierarchy, saved navigation-stack serialization, authentication flow, or route order.
- Converting Settings into a multi-pane destination unless the existing screen structure proves it is a natural, low-risk supporting-pane layout.
- Removing or redesigning the Phase A custom bottom bar, navigation rail, width constraints, or responsive calendar.

### Presentation Policy
| Width | Contact flow | Group flow |
|---|---|---|
| Compact / Medium | Home navigates to full-screen `ContactProfile`; back returns through `Navigator` | Existing full-screen `GroupDetail` route and sheets remain unchanged |
| Expanded | Home stays visible and hosts a selected-contact supporting pane; selecting a contact updates the pane without pushing another full-screen route | Use a two-context group layout only where the current group-management entry point exposes a stable group list plus detail context; otherwise retain the full-screen route |

### Acceptance Criteria
- Expanded Home presents a usable primary list/timeline and a readable contact-detail pane without overlap, clipping, or accidental navigation-bar coverage.
- Compact and medium behavior remains unchanged, including route transitions and back-stack behavior.
- Closing or changing the expanded supporting pane does not leave stale contact data or mutate the navigator stack unexpectedly.
- Existing full-screen animations remain active for compact/medium `ContactProfile` and `GroupDetail` routes; expanded pane changes do not replay those transitions as route changes.
- Group management remains usable at expanded widths, and no pane is introduced where the current flow lacks enough context to justify it.
- Existing sheet/dialog ownership and lifecycle-driven refresh behavior remain intact.
- Common and Android tests cover the presentation decisions and all changed production code compiles.

## Technical Design

### Key Decisions
- Use `WindowWidthSizeClass.Expanded` as the only Phase B breakpoint; do not invent a second width threshold.
- Keep pane-selection state local to the expanded screen host, or derive it from the current route without persisting it through `RememberNavigator`.
- Reuse `ContactProfileScreen` content through a pane-capable composable boundary rather than duplicating its view-model and sheet logic.
- Treat an expanded supporting pane as layout state, not a new `Screen` route, so the navigator remains authoritative for full-screen flows.
- Reuse existing Material 3 sheet/dialog transitions inside panes and do not wrap nested overlays in another full-screen animation.
- Re-open the transition matrix only for destinations whose presentation mode changes; do not alter unrelated route styles.

### Proposed Changes
1. Introduce a small adaptive presentation model/policy in the shared or home common source set that maps width class and available route context to `SinglePane` or `SupportingPane`.
2. Refactor the contact-profile UI so its full-screen and supporting-pane surfaces share state/actions while allowing pane-specific top-bar and close behavior.
3. Update Home's expanded layout to retain its existing timeline/list as the primary content and render the selected contact in a constrained supporting pane; compact and medium continue invoking `navigator.navigate(Screen.ContactProfile(...))`.
4. Audit the group entry points and implement a supporting pane only if both group navigation contexts are available without changing the existing route contract; otherwise add only width constraints and retain `GroupDetail` as a full-screen route.
5. Ensure expanded pane selection, dismissal, and route changes do not trigger stale or duplicate lifecycle work, and keep route callbacks in `OnboardingApp.kt` compatible.
6. Add previews or deterministic layout-policy tests for compact, medium, and expanded widths, including no selected contact and selected-contact replacement.

### Files To Review
- `shared/src/commonMain/kotlin/app/usefoster/adaptive/WindowWidthSizeClass.kt`
- `shared/src/commonMain/kotlin/app/usefoster/navigation/FosterNavHost.kt`
- `onboarding/src/commonMain/kotlin/app/usefoster/onboarding/OnboardingApp.kt`
- `home/src/commonMain/kotlin/app/usefoster/home/HomeScreen.kt`
- `home/src/commonMain/kotlin/app/usefoster/home/presentation/contactprofile/ContactProfileScreen.kt`
- `home/src/commonMain/kotlin/app/usefoster/home/presentation/settings/GroupBottomSheet.kt`
- `home/src/commonMain/kotlin/app/usefoster/home/presentation/settings/GroupDetailScreen.kt`
- Existing adaptive and Android unit tests.

### Risks & Mitigations
- **Duplicated contact-profile logic:** extract shared content/actions before adding pane layout; keep one view-model ownership path per visible profile.
- **Unexpected navigator mutations:** expanded contact selection must be handled locally, while compact/medium selection must continue using the existing route callback.
- **Animation mismatch:** only full-screen route changes should reach `FosterNavHost`; pane content changes need a local, restrained layout transition or no transition.
- **Lifecycle churn:** use stable contact keys and existing refresh guards so changing selection does not cancel unrelated outgoing work unexpectedly.
- **Insufficient group context:** do not force a group pane if the current UI cannot provide a stable master list; preserve the existing full-screen `GroupDetail` route.

## Testing

### Validation Approach
- Add common unit tests for the adaptive presentation policy and selection state transitions.
- Run relevant shared/common tests and Android unit tests.
- Compile `home`, `onboarding`, `shared`, and `androidApp` Android targets.
- Perform visual checks on compact phone, medium foldable/tablet, and expanded tablet/desktop widths in light and dark themes.

### Key Scenarios
- Expanded Home with no selected contact shows the primary content and an intentional empty supporting-pane state.
- Expanded Home selecting one contact opens its profile beside Home without pushing `Screen.ContactProfile`.
- Expanded Home switching directly between contacts replaces the supporting pane without stale details.
- Expanded pane back/close clears the pane without unexpectedly popping Home.
- Compact and medium Home selection still navigates to `ContactProfile` and uses the existing horizontal route animation.
- Existing contact-profile sheets and note/reminder actions work in both full-screen and pane presentations.
- Group management remains functional at expanded widths; if no stable two-context entry exists, `GroupDetail` remains full-screen and is not regressed.
- Rotation or width-class changes do not leave an invalid selected contact or overlapping content.

### Edge Cases
- Contact disappears while selected; the pane closes or returns to the intentional empty state without a navigation loop.
- Loading and refresh indicators remain scoped to the selected profile.
- Very large font scale does not make the supporting pane unusably narrow.
- Rapid contact selection leaves the final displayed contact and pane actions consistent.

## Delivery Steps

### ✓ Step 1: Define Phase B presentation policy
- Add tests first for compact/medium single-pane and expanded supporting-pane decisions.
- Inspect the actual group navigation entry points and decide whether a group supporting pane is justified without route-contract changes.
- Keep policy logic pure and separate from Compose layout.

### ✓ Step 2: Extract pane-capable contact profile content
- Share contact-profile state/actions between full-screen and expanded-pane presentations.
- Preserve all existing sheets, dialogs, refresh behavior, and route callbacks.
- Verify full-screen `ContactProfileScreen` behavior before wiring Home.

### ✓ Step 3: Add expanded Home supporting pane
- Keep Home's current primary content and adaptive navigation shell.
- Use local selected-contact state only at expanded widths.
- Keep compact/medium navigation unchanged and avoid navigator stack mutations for expanded pane selection.

### ✓ Step 4: Resolve group expanded presentation
- Implement a group supporting pane only if the current UI has a stable master/detail context.
- Otherwise document the decision in code/tests and retain full-screen `GroupDetail` with its existing behavior.

### ✓ Step 5: Validate integration and visual behavior
- Run all relevant tests and Android compilation.
- Exercise width changes, route transitions, overlays, lifecycle refresh, and large-font scenarios.
- Leave this branch unmerged for review.