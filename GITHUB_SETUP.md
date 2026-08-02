# GitHub Actions APK build — setup

## 1. Push this project to a GitHub repo

```bash
cd TermuxAI
git init
git add .
git commit -m "Add MainActivity, voice I/O, CI workflow"
git remote add origin https://github.com/<your-username>/<your-repo>.git
git branch -M main
git push -u origin main
```

## 2. Download the Vosk model and commit it (no signup needed)

The wake word engine (Vosk) needs no account and no API key — but it does
need its model files present, or the app builds fine but the wake word
engine fails at runtime.

1. Download (direct download, no signup): https://alphacephei.com/vosk/models
   → "vosk-model-small-en-us-0.15" (~40MB zip)
2. Unzip it, then copy its CONTENTS (the `am/`, `conf/`, `graph/`,
   `ivector/` folders) into `app/src/main/assets/model-en-us/` in this
   project (full instructions in that folder's README.txt).
3. Commit and push those files along with everything else.

This does make the repo/APK ~40MB bigger — that's the real tradeoff for a
fully offline, no-account wake word engine.

## 3. Push (or use "Run workflow" button)

The workflow at `.github/workflows/build.yml` runs automatically on every
push to `main`, or manually from the **Actions** tab → **Build APK** →
**Run workflow**.

## 4. Download the APK

Once the run finishes (green check): open that run → **Artifacts** section
at the bottom → download **termuxai-debug-apk** → unzip → install
`app-debug.apk` on your phone (you'll need to allow installs from unknown
sources, since this isn't a Play Store build).

## Important — what I could NOT verify here

I don't have a GitHub Actions runner or Android SDK/network access in this
environment, so **this workflow has not actually been executed**. The
Kotlin/Compose code was checked for syntax and structure, but the very
first real GitHub Actions run is the true test — if it fails, paste me the
Actions log and I'll fix it.

## About the missing Gradle wrapper

This project didn't have a committed `gradlew` / `gradlew.bat` /
`gradle-wrapper.jar`. I can't generate the real `gradle-wrapper.jar` myself
in this sandbox — it's a compiled binary that requires an actual Gradle
install with network access to produce, and I don't have that here.

Instead there's a one-time workflow that does it for real, using GitHub's
own runner (real network, real Gradle):

1. Push this project to GitHub (see step 1 above) first.
2. Go to the **Actions** tab → **Generate Gradle Wrapper** → **Run workflow**.
3. It'll commit real `gradlew`, `gradlew.bat`, and `gradle-wrapper.jar` files
   back to your `main` branch automatically.
4. After that, `git pull` locally and you'll have the standard wrapper for
   Android Studio / local builds.
5. You can delete `.github/workflows/generate-wrapper.yml` afterward if you
   want — it's only needed once. The main `build.yml` workflow keeps working
   either way (it uses `gradle` directly, not `./gradlew`).
