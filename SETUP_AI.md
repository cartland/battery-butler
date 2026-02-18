# On-Device AI Setup

To use the On-Device AI features (powered by MediaPipe and Google AI Edge), you must manually deploy a compatible Large Language Model (LLM) to your Android device or emulator.

## Prerequisites

- **Android Device**:
    - Android 12+ (API level 31+) highly recommended for GPU acceleration.
    - At least 8GB of RAM.
    - Developer Mode and USB Debugging enabled.
- **Emulator**:
    - Recent Android System Image (API 34+ recommended).
    - "Pixel 8" or similar high-spec profile.
    - **Crucial**: Internal storage partition must be large enough to hold the ~2.5GB model.

## 1. Download the Model

We use the **Gemma 2b IT** (Instruction Tuned) model, optimized for GPU inference on mobile devices.

### Option A: Kaggle (Recommended)
1.  Go to [Gemma 2b models on Kaggle](https://www.kaggle.com/models/google/gemma/tensorFlowLite/gemma-2b-it-gpu-int4).
2.  Select the **TensorFlow Lite** variant.
3.  Download `gemma-2b-it-gpu-int4.bin`.

### Option B: Hugging Face
1.  Search for `gemma-2b-it-gpu-int4` TFLite / MediaPipe compatible models.

## 2. Deploy to Device

Once downloaded, you need to push the file to the app's accessible storage on the device. We use `/data/local/tmp/` as it is a standard writable directory for testing and development.

**Run the following command in your terminal:**

```bash
# Replace path/to/downloaded/model.bin with your actual file path
adb push path/to/gemma-2b-it-gpu-int4.bin /data/local/tmp/llm.bin
```

> **Note**: The app looks specifically for the filename `llm.bin` in `/data/local/tmp/`.

## 3. Verify Deployment

Check if the file exists on the device:

```bash
adb shell ls -lh /data/local/tmp/llm.bin
```

You should see output similar to:
`-rw-rw-rw- 1 shell shell 2.5G ... /data/local/tmp/llm.bin`

## 4. Run the App

1.  Open **Battery Butler**.
2.  Go to **Settings**.
3.  Change **AI Engine** to **On Device**.
4.  Try an AI feature (e.g., "Add a device").
