# Simple Fix Applied - User Archiving When Switching to Non-PT Package

## What Was Changed

**File: `SelectMembership.java`**

When a user purchases or switches to a **non-PT package** (sessions = 0):
1. The system gets their **previous coachId** (if they had one)
2. Sets `coachId = null` (removes coach assignment)
3. If they had a coach, archives them with:
   - `isArchived: true`
   - `archivedBy: <previousCoachId>`
   - `archiveReason: "Switched to non-PT package"`
   - `archivedAt: <timestamp>`

## How It Works

### For PT Packages (sessions > 0):
- Assigns the coach
- Removes any archive fields (unarchives if needed)
- User appears in coach's active client list

### For Non-PT Packages (sessions = 0):
- Gets the previous coachId
- Removes coach assignment (`coachId = null`)
- **Archives the user** (if they had a coach)
- User will NOT appear in coach's active client list
- User WILL appear in that coach's archive

## How to Test

### Test Scenario:
1. **Setup**: Have a user with an active PT package assigned to a coach
2. **Action**: As that user, switch to a non-PT package (Daily, Standard Monthly, etc.)
3. **Complete**: Finish the purchase

### Expected Results:
✅ User's `coachId` becomes `null`
✅ User gets archived fields in Firestore:
   ```
   isArchived: true
   archivedBy: "<coach-id>"
   archivedAt: <timestamp>
   archiveReason: "Switched to non-PT package"
   ```
✅ User disappears from coach's active clients screen
✅ User appears in coach's archive screen

### Check in Firebase Console:
After the user switches packages, open Firestore and check their user document.
You should see the fields above.

### Check in Coach App:
1. Open the coach app
2. User should NOT be in the active clients list
3. Go to Menu → Archive
4. User should appear there with reason "Switched to non-PT package"

## Important Notes

- ✅ **Works immediately** - No need for coach app to be running
- ✅ **Preserves history** - Archives with the correct coach ID
- ✅ **Real-time** - If coach app is open, changes happen instantly
- ✅ **Handles edge cases** - Works even if user had no coach (just removes coachId)

## If User Switches Back to PT:
When switching to a PT package again:
- All archive fields are cleared (`isArchived = false`, etc.)
- New coach is assigned
- User reappears in the new coach's active list

## Logs to Watch

Filter Logcat for tag: **`SelectMembership`**

You should see:
```
SelectMembership: 🗄️ User had coach <coachId>, archiving them
SelectMembership: ✅ User updated with archive status
```

Or if user had no coach:
```
SelectMembership: ℹ️ User had no coach, just removing coachId
```

---

That's it! The fix is simple and complete. Test it now! 🎉

