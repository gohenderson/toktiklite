# TokTikLite

A minimal Android app that opens TikTok links from other apps and shows them in TikTok's
embed player, with no feed, no algorithm, and no scrolling.

## How it works

When you tap a TikTok link anywhere on your phone, TokTikLite intercepts it and loads the
video using TikTok's official embed URL: a clean single-video player. No For You page, no
recommended videos, no account required.

Supported link formats:
- `https://www.tiktok.com/@user/video/ID`
- `https://vm.tiktok.com/...` (short links)
- `https://vt.tiktok.com/...` (short links)
- `https://m.tiktok.com/v/ID`

Opening **TokTikLite** from the launcher shows whether it's set to open TikTok links (on
Android 12+), a **Link settings** shortcut, and a box to **paste** a link or a whole share
text ("Check this out https://vm.tiktok.com/…").

**Stays on TikTok.** Only TikTok's own pages (https, TikTok and its CDN domains) load inside
the app. A link that leads elsewhere (a description link, an ad) opens in your browser
instead, and a link to any other site sent to the app is refused. File and content access
are off in the WebView.

Requires Android 8.0+ (API 26).

## Setting as default link handler

- Open TokTikLite and tap **Link settings**, turn on **Open supported links** and tick the
  TikTok addresses, or
- Tap a TikTok link, choose TokTikLite, and select **Always**.

If the TikTok app is installed, you may also need to turn off its link handling under its
App info settings.

## Install

Download the signed APK from [Releases](../../releases) on your phone and open it (allow
installing from your browser or Files app when asked). Each release has a `.sha256` file to
verify the download, and screenshots.

## Development

- **No dependencies in the app:** Android's own WebView and views only (no AndroidX).
- Gradle 9.9, Android Gradle Plugin 9.5 (built-in Kotlin 2.4), compile SDK 36, target 35.
- Open the project in Android Studio and press **Run**, or `gradle assembleDebug` with a
  local Gradle 9.9.

### Tests

- **Unit tests** (`app/src/test`, run on the JVM, seconds): every link format becomes the
  right embed URL; short links and profiles load as-is; only https TikTok domains are
  allowed (look-alike domains, `user@host` tricks, http, `javascript:` and `file:` are
  refused); a link is found inside pasted share text.
- **UI tests** (`app/src/androidTest`, Espresso on an emulator): a video link opens the
  embed player; a short link loads so it can redirect; a second link replaces the video in
  the same screen; a non-TikTok link is refused; the home screen shows its link status and
  settings button; pasted share text opens the player. Tests check what the app asks to
  load, so they don't depend on TikTok's servers.

### CI (GitHub Actions)

Same setup as Gist:

- **Build** (every push to `main` and every pull request): debug APK, unit tests, lint
  (report only), and the UI tests on an Android 15 emulator. Compile errors and failing
  tests are annotated on the code and summarized on the run page; failed UI tests leave a
  screenshot of the screen in the **ui-test-logs** artifact, with the emulator's logcat.
- **CodeQL** (pushes, pull requests, weekly): GitHub's security scanner for the Kotlin code
  and the workflow files.
- **Security report** (weekly): outdated libraries, build tools and actions, known
  vulnerabilities (OSV) in every resolved dependency, and open CodeQL alerts. If anything
  is found it opens one issue assigned to you (so GitHub emails you) and closes it when
  everything's clear.
- **Dependabot** (weekly): pull requests for newer libraries and actions.
- Actions are pinned to commit SHAs, and Gradle caching uses the MIT-licensed basic cache.

### Releases

**Actions → Release → Run workflow**, enter a version like `v1.0.0`. The full test suite
runs first; then the release job waits for your approval (it needs the signing secrets),
builds the signed APK, publishes a GitHub release with a `.sha256`, and attaches emulator
screenshots (also kept on the `screenshots` branch).

One-time setup (Settings → Environments → **New environment** `release`):
1. Under **Deployment protection rules**, tick **Required reviewers** and add yourself.
2. Add these **environment secrets**:
   - `ANDROID_KEYSTORE_BASE64`: your keystore file, base64-encoded
     (`base64 -w0 release.jks` on Linux, `base64 -i release.jks` on macOS)
   - `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`

The same keystore you use for Gist works. Keep using the same one for every TokTikLite
release: Android only installs an update signed with the same key.
