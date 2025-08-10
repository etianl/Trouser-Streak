This PR refactors NewerNewChunks auto-follow to meet the latest requirements.

Changes
- Disable-on-input: pressing movement/interaction disables the auto-follow setting (as if toggled off in Meteor UI) and cancels all Baritone navigation.
- Relative-direction trail walking: the bot maintains a dynamic heading; it prefers straight, then left, then right relative to heading and never chooses the reverse direction.
- End-of-trail behavior: when no forward/lateral adjacent target exists, navigation is cancelled and (optionally) logout is performed. Removed radius/nearest fallbacks that could cause backtracking or churn.
- Simplified state: removed look-direction lock and periodic goal refresh. Goal is only set when advancing along the contiguous trail.

Manual Test Steps
1) Enable NewerNewChunks and select target type. Enable chat logging for follow.
2) Start auto-follow and observe: it moves along the marked trail, taking turns but never reversing.
3) Reach end of trail: it cancels navigation and logs out if configured.
4) Press movement/interaction: auto-follow toggles off and pathing stops immediately.

Build
- Verified with `./gradlew build -x test`.
