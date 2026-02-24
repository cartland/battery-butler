#!/usr/bin/env bash

# run_worker_swarm.sh
# Spawns independent AI agents (using the claude CLI) to review the codebase
# and create specialized PRs.

set -e

# Define unique personas/improvements. 
PERSONAS=(
    "CodeStyleCop" "DepUpgradeBot" "KDocScribe" "TestCoverageCzar" "ComposeLayoutPro"
    "SwiftUIArchitect" "GrpcGuru" "RoomOptimiser" "UseCasePurist" "SecuritySentinel"
    "BazelBuilder" "KoinInjector" "AccessibiltyAlly" "IosA11yAuditor" "LogMonitor"
    "StringLocalizer" "CoroutineConductor" "MemoryLeakHunter" "StartupSpeeder" "PiiProtector"
    "ErrorHandler" "AnalyticsAgent" "ThemeDesigner" "IconArtist" "AnimationArtist"
    "BatteryMiser" "NetworkConditionsTester" "ScreenshotPa" "E2EOrchestrator" "MockDataMaker"
    "ReadmeReviewer" "FunctionNamer" "DeadCodeReaper" "MagicNumberMagician" "ModuleSplitter"
    "BuildTimeBooster" "SecretsManager" "FeatureGater" "DeepLinkDiver" "IosLinkHandler"
    "FormValidator" "ErrorUiPolisher" "DarkThemeDetective" "TouchTargetTester" "SqlSafeGuy"
    "GraphVisualizer" "CiCdPipelinePro" "ReleaseManager" "AppStorePrep" "PlayStorePrep"
    "KotlinUpgrader" "SwiftUpgrader" "TimestampTracker" "PriceFormatter" "UnitConverter"
    "RegexRanger" "JsonJuggler" "CacheMeIfYouCan" "DebounceDuke" "BackStackBoss"
    "CoordinatorCop" "KeyboardKing" "HapticHero" "SoundSmith" "ImageOptimiser"
    "FontFoundry" "LicenseLawyer" "TodoTracker" "CommentCurator" "GitGuardian"
    "BranchProtector" "KspKeeper" "TypealiasTycoon" "SealedClassSage" "EnumEmperor"
    "ExtensionExpert" "ModifierMaster" "PreviewPro" "SnapshotSavant" "LifeCycleLifeguard"
    "DisposableDirector" "SideEffectSamurai" "PlatformSpecific" "DesktopDocker" "MenuMaestro"
    "DialogDirector" "ToastMaster" "PullToRefresher" "PaginationPal" "SearchSleuth"
    "SortSultan" "FilterFox" "UndoUser" "ClipboardClerk" "ShareSherpa"
    "NotificationNinja" "BackgroundButler" "AlarmArchitect" "WidgetWizard" "FinalPolisher"
)

# Optional argument to test just one persona
TEST_MODE=false
LIMIT=${#PERSONAS[@]}
START_INDEX=0

while [[ "$#" -gt 0 ]]; do
    case $1 in
        --test|-t) TEST_MODE=true; LIMIT=1; shift ;;
        --limit|-l) LIMIT="$2"; shift; shift ;;
        --start|-s) START_INDEX="$2"; shift; shift ;;
        *) echo "Unknown parameter passed: $1"; exit 1 ;;
    esac
done

echo "Starting Worker Swarm with $LIMIT agents starting at index $START_INDEX..."
if [ "$TEST_MODE" = true ]; then
    echo "Running in TEST MODE (only first persona)."
fi

# Ensure working directory is clean before starting
if [[ -n $(git status -s) ]]; then
    echo "Warning: Working directory is not clean. Agents should ideally start from a clean state."
    echo "Do you want to proceed anyway? (y/n)"
    read -r response
    if [[ ! "$response" =~ ^([yY][eE][sS]|[yY])$ ]]; then
        echo "Exiting."
        exit 1
    fi
fi

# File to hold all PR bodies to reduce authorization prompts.
COMMON_PR_BODY_FILE="/Users/cartland/github/cartland/battery-butler/WORKER_SWARM_PR_BODY.md"

# Loop through personas
END_INDEX=$((START_INDEX + LIMIT))
for (( i=$START_INDEX; i<$END_INDEX; i++ )); do
    persona="${PERSONAS[$i]}"
    echo "---------------------------------------------------"
    echo "Spawning Agent $((i+1)) (Index $i) / 100 : $persona"
    echo "---------------------------------------------------"
    
    # Prompt formulation
    PROMPT="You are $persona, an autonomous AI agent participating in a Worker Swarm. \
Your objective is to independently review this codebase and make ONE highly specialized type of improvement \
related to your persona. \
\
Follow these steps EXACTLY: \
1. Sync and checkout a new branch from origin/main named 'agent/$persona-audit'. (Use git fetch origin main && git checkout -b agent/$persona-audit origin/main) \
2. Review the codebase to find ONE area where you can apply your specific expertise. \
3. Make the necessary code changes. Ensure the code compiles. \
4. Run formatting using './scripts/spotless-apply.sh' and fix any errors. \
5. Verify your changes locally (e.g., './gradlew spotlessCheck' or basic compilation). \
6. Commit the changes with a clear conventional commit message. \
7. Push the branch to origin. \
8. Write the Pull Request description body to the file '$COMMON_PR_BODY_FILE'. \
   - You MUST write to this exact path so the user only has to grant one file permission. Overwrite it. \
   - Description MUST explain WHAT you are trying to do, WHY you are trying to do it, and HOW you validated that the change is correct. \
   - Since 100 PRs will be created by the swarm, this PR serves primarily to communicate your specific goals and ideas. \
9. Create a Pull Request against main using the gh cli. Use the file you just created. (e.g., gh pr create --head agent/$persona-audit --base main --title \"\$TITLE\" --body-file $COMMON_PR_BODY_FILE) \
   - Title MUST clearly state the specific improvement. \
   - Do NOT merge the PR. Just create it. \
\
Do this autonomously now and exit when the PR is created."

    if command -v claude &> /dev/null; then
        echo "Using claude CLI..."
        claude -p "$PROMPT"
    elif command -v gemini &> /dev/null; then
        echo "Using gemini CLI..."
        gemini "$PROMPT"
    else
        echo "Error: Neither 'gemini' nor 'claude' CLI found."
        exit 1
    fi

    if [ "$i" -lt $((END_INDEX-1)) ]; then
        echo "Agent $persona completed. Sleeping for 30s before next agent..."
        sleep 30
    fi
done

echo "Worker Swarm execution completed."
