# Adaptive UI Phase 3

## Branch and gate

- Current implementation branch: `feature/adaptive-ui-phase-3-implementation`.
- This branch was created from the existing checkout and must not be pushed by this task.
- Phase B visual approval is still a human gate: validate phone, foldable, tablet, and desktop in light and dark themes before merging Phase B.
- Do not merge Phase B or cut a branch from unmerged work as part of this task; the implementation branch intentionally remains reviewable and unmerged.

## 3a — certain value, no layout risk

### Completed in this branch

- Constrain large-screen sheets and form surfaces with `AdaptiveSurface`.
- Keep modal content centered and readable on medium/expanded windows while preserving full-width phone behavior.
- Use a smaller horizontal inset for `1.5x` font scale and widen large-screen surfaces so size-class thresholds and text scaling are tested together.
- Retain expanded-pane selection across resize/recomposition and clear it only when the selected contact is no longer available.
- Add pure policy tests for compact/medium/expanded widths, orientation, large-font behavior, and selection retention.
- Add an explicit `InPlacePane` navigation presentation mode so pane updates do not replay full-screen route animations.
- Expand form-factor previews for Home and Settings to phone, foldable, tablet, and desktop.

### Required validation before merging 3a

1. Visually validate Phase B on phone, foldable, tablet, and desktop in both themes.
2. Exercise sheets, dialogs, forms, empty states, and loading states at large widths.
3. Exercise `1.5x` font scale at compact and expanded widths.
4. Exercise keyboard/mouse focus, orientation changes, and resize drags.
5. Confirm scroll positions, side effects, and text-field focus survive resize/recomposition.
6. Add or update visual regression baselines only after these layout changes are approved; otherwise baselines will be recorded twice.

## 3b — conditional, decision-gated

These items are deliberately not implemented in this branch:

- **Group-management supporting pane:** written decision required first. Build it only if group management has a stable list/detail context today; otherwise keep the existing full-screen route.
- **Settings rail-plus-content:** written information-architecture decision required first. This changes Settings into a two-level in-place hierarchy and is not merely responsive layout.

The animation policy change is the prerequisite for either pane: pass `NavigationPresentation.InPlacePane` when a pane is updated so the custom Navigator remains responsible only for full-screen routes.

## Delivery sequence

1. Review and visually approve Phase B, then merge Phase B separately.
2. Review and merge this 3a slice after the validation checklist passes.
3. Land the animation-policy change (included here as the prerequisite seam).
4. Obtain written decisions for group-management and Settings IA.
5. Implement only the approved 3b pane(s), preserving the custom Navigator and existing navigation boundaries.

## Testing

- Common policy tests: `:shared:jvmTest` (or the platform-equivalent common test task).
- Android compilation: `./gradlew :shared:compileAndroidMain :home:compileAndroidMain`.
- Visual review: use the Home and Settings form-factor previews for the four device buckets, then record baselines only after approval.
- Resize/state-retention pass: select a contact, drag the window or change fold posture, and verify the selected profile, scroll position, side effects, and focused text field remain valid.