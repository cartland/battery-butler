# AI chat

The assistant is a chat panel over the person's own inventory. It can answer questions about
their devices and make changes to them. This page is about the second part: what it is allowed
to do, how the person sees it, and why.

## What the person sees

- **Their words and the assistant's reply**, as bubbles.
- **A row for each change the app made** under the reply: "Added Hall Alarm (Smoke Alarm,
  Hall)", "Recorded a battery change for Hall Alarm on 2026-11-07". The app writes these from
  what it stored, never from the model's description. Each row has **Undo** until the chat is
  cleared.
- **A card when a deletion is waiting.** Nothing is deleted until they tap the button that names
  the act. The card says what would be lost for good (a photo).
- **"Try again"** under a failed reply that changed nothing. A turn that failed after making
  changes keeps its rows and Undo, and offers no retry, because repeating it would make them twice.
- **Suggested questions** when the chat is empty. A chip sends at once, so each one is a question
  and never a change.

## The rules, and where they live

The prompt describes these rules to the model, but none of them depends on the model following
them. Each is enforced in code.

| Rule | Where |
|---|---|
| A deletion never runs on the model's say-so. The call becomes an `AiPendingAction`; the person decides on a card. One at a time; a new message withdraws it. | `AiChatSession.defer` |
| Everything else runs at once, because it can be undone. The way back is held by the session until used or the chat is cleared. | `DeviceToolHandler` returns `ToolOutcome.undo`; `AiChatSession.undo` |
| The person sees what happened, in the app's words. | `ToolOutcome.action`, written by the tool from the stored data |
| The model is told what it couldn't see: a confirmation, a refusal, an undo. The next message starts with a bracketed note, and the prompt says to trust those notes over its memory. | `AiChatSession.notes` |
| The model sees the present. The whole inventory, with ids, ages and marks, is rebuilt before every model call and sent as part of the system instruction, never into the conversation. | `BuildAiContextUseCase`; `AiEngine.generateResponse(prompt, tools, situation)`; `LabsCloudAiEngine.requestBody` |
| A type in use can't be deleted; a recorded change clears the device's mark and moves its date; an edited or deleted change recomputes that date. | `DeviceToolHandler`, mirroring the use cases the screens call |

## Tools

| Tool | Runs | Undo |
|---|---|---|
| `addDevice`, `addDeviceType` | at once | removes it (and a type it created, if nothing else uses it) |
| `recordBatteryReplacement` | at once | removes the change, restores the device's date and mark, removes a device it created |
| `updateDevice`, `updateDeviceType`, `updateBatteryEvent` | at once | restores only the fields it changed, so later edits survive |
| `markNeedsBattery` | at once | restores the prior mark |
| `getDeviceHistory` | at once | nothing to undo |
| `deleteDevice`, `deleteDeviceType`, `deleteBatteryEvent` | after the person confirms | puts it back, with its events and mark |

Names and parameters: `domain/.../ai/AiTools.kt`. Declarations and the prompt, sent with every
relay request: `data-network/.../ai/LabsAiTools.kt`.

## Where the model runs

The cloud engine relays through the Labs backend (`POST /v1/battery-butler/ai/generate`), which
holds the key and picks the model; the app owns the prompt, the tools and the conversation. The
backend accepts a 20,000-character system instruction, which is why the situation cuts its list
of recent changes first and offers `getDeviceHistory` instead. See the labs repo's
`services/labs-backend/docs/BATTERY-BUTLER.md`.

The on-device engine (Gemini Nano) has no conversation and runs at most one tool per message. It
gets the same tools and the same gate, but the situation is folded into each prompt.

## Known limits

- The transcript lives in memory. Killing the app loses it, and the undo buttons with it.
- Undo of a deletion restores the record, not a photo the server already removed.
- Batch import and icon suggestion share the cloud engine's conversation with the chat.
- Chat is Android-only in practice: desktop and both iOS apps run `NoOpAiEngine`. The SwiftUI screen
  shows messages only, so it needs action rows and the confirmation card before iOS gets a real engine.
