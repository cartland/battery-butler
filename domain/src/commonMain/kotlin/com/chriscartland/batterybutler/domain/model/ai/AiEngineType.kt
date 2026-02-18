package com.chriscartland.batterybutler.domain.model.ai

enum class AiEngineType {
    Cloud, // Gemini API
    OnDevice, // MediaPipe / LocalAgents
    NoOp, // Disabled / Mock
}
