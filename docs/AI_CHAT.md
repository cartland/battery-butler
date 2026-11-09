# AI chat

The assistant is a chat panel over the person's own inventory. It is built on one assumption: **the
model may be wrong, confused or misbehaving, and the app must still be safe and make sense.** So the
model can read and it can ask. It can't change anything, and it doesn't get to describe what changed.

## What the person sees

- **Their words and the assistant's reply**, as bubbles, with a quiet line saying what it looked at.
- **A card for every change it wants to make**, listing each one in the app's own words from the
  real data: "Update Hall Alarm: location Hall → Landing". Nothing has happened yet, and the card says
  so. Approve, or Decline; a card of several changes can be trimmed with checkboxes. When everything
  on it is a deletion the button says Delete.
- **The same card then carries the result**: Done, or Some of this worked, with the reason beside
  anything that failed. **Undo** is on the card and takes back every change on it.
- **Try again** under a reply that failed without proposing anything.
- **Suggested questions** when the chat is empty. A chip sends at once, so each is a question.

## The strategy

| Principle | How it's enforced |
|---|---|
| Reads run, writes propose | `AiToolNames.READ_ONLY` goes to `DeviceReadTools`, a class with no write path. Anything else is previewed and carded. A tool nobody classified can't run. |
| The card is the truth | `DeviceToolHandler.preview` writes the summary from stored data. A reply that proposes shows only the card, so "Done!" from the model can't contradict it. |
| One decision, applied once | Approve, Decline and Undo are gated on the card's status under a mutex, so a double tap is a no-op. A new message withdraws a waiting card. |
| No silent overwrite | A card remembers the row it was built from. If that row changed before approval, the item is left alone and says so. |
| Every outcome is visible | Each item has its own outcome and, when it didn't work, a reason. One failure doesn't stop the rest. |
| The model hears what it can't see | Approvals, declines and undos open the next message as bracketed notes, and survive a failed turn. |
| A failed turn changed nothing | No tool can write during a turn, so the failure message is always true. |

## What a bad model can and can't do

| It does this | What happens |
|---|---|
| Says "done" without proposing | The reply gets a line: no changes were made. |
| Says "done" while proposing | Only the card is shown. |
| Invents an id, a bad date, a future date | The preview refuses it and tells the model. No card. |
| Misspells a device | The card warns that it would add a new device. |
| Proposes the same thing repeatedly | It appears once. |
| Reads or proposes forever | Budgets: 12 reads and 50 changes a message. |
| Leaks tool syntax into its reply | Stripped. If nothing's left, an honest error with Try again. |
| Ignores the prompt entirely | Nothing changes. None of this depends on the prompt. |

## Undo

Undo takes a card back newest change first, so a card that adds a device and records its battery
comes apart cleanly. The hard cases, and what happens:

| Case | Behaviour |
|---|---|
| A field was edited again since | That item refuses and says so, rather than overwrite the newer edit. Other fields are never touched. |
| The record was deleted since | Refused, and it isn't resurrected. |
| A device added here has battery changes from a later card | Refused with the reason. Undo the later card first; the Undo stays available. |
| A deleted device had a photo | The card warned before approval. Undo restores everything but the photo. |
| Some items fail | The card says which and why, and Undo can be tried again for those. |

Undos are held in memory, so they last until the chat is cleared or the app is killed.

## Tools

Read-only, run immediately: `listDevices`, `getDevice`, `getDeviceHistory`, `listBatteryChanges`,
`getBatteriesToBuy`. Proposing: `addDevice`, `addDeviceType`, `recordBatteryReplacement`,
`updateDevice`, `updateDeviceType`, `updateBatteryEvent`, `markNeedsBattery`, `deleteDevice`,
`deleteDeviceType`, `deleteBatteryEvent`.

To add a tool: a read goes in `DeviceReadTools` and `AiToolNames.READ_ONLY`. A write needs a
`preview` case and a `run` case that returns an undo. Names are in `domain/.../ai/AiTools.kt`, and
declarations and the prompt in `data-network/.../ai/LabsAiTools.kt`.

The model sees the present: `BuildAiContextUseCase` rebuilds the inventory before every model call
and sends it in the system instruction, never into the conversation.

## Where the model runs

The cloud engine relays through the Labs backend (`POST /v1/battery-butler/ai/generate`), which holds
the key and picks the model; the app owns the prompt, the tools and the conversation. See the labs
repo's `services/labs-backend/docs/BATTERY-BUTLER.md`. The on-device engine has no conversation and
runs at most one tool per message, through the same gate.

## Known limits

- The transcript and undos are in memory. Killing the app loses them.
- Batch import and icon suggestion share the cloud engine's conversation with the chat.
- Chat is Android-only in practice: desktop and both iOS apps run `NoOpAiEngine`. The SwiftUI screen
  shows messages only, so it needs cards before iOS gets a real engine.
