# 🔒 GitHub Branch Protection & CI/CD Setup Guide

## 📋 Overview

This document explains how to configure GitHub branch protection rules and the CI/CD pipeline for the uHabits project.

---

## 🔄 CI/CD Pipeline Stages

The pipeline consists of the following stages:

```
┌─────────────────────────────────────────────────────────────────────┐
│                          CI/CD Pipeline                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐          │
│  │ Code Quality │───▶│  Unit Tests  │───▶│ Build Debug  │          │
│  │   (ktlint)   │    │   (JUnit)    │    │    APK       │          │
│  └──────────────┘    └──────────────┘    └──────────────┘          │
│                                                 │                    │
│                                                 ▼                    │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐          │
│  │   Security   │◀───│  Instrument  │    │Build Release │          │
│  │    Scan      │    │    Tests     │    │    APK       │          │
│  └──────────────┘    └──────────────┘    └──────────────┘          │
│                                                 │                    │
│                                                 ▼                    │
│                                          ┌──────────────┐           │
│                                          │   Notify &   │           │
│                                          │   Upload     │           │
│                                          └──────────────┘           │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 📁 Workflow Files

| File | Trigger | Purpose |
|------|---------|---------|
| `ci-cd-pipeline.yml` | Push/PR to main, dev | Full pipeline with tests & builds |
| `create-release.yml` | Tag `v*` or manual | Creates GitHub release with APKs |
| `coverage.yml` | Push/PR to main, dev | Generates code coverage reports |
| `build-apk.yml` | Push/PR | Quick APK build (legacy) |

---

## 🔧 Setup Instructions

### Step 1: Enable GitHub Actions

1. Go to your repository on GitHub
2. Click **Settings** → **Actions** → **General**
3. Select **Allow all actions and reusable workflows**
4. Click **Save**

### Step 2: Configure Branch Protection Rules

1. Go to **Settings** → **Branches**
2. Click **Add branch protection rule**
3. Configure for `main` branch:

```yaml
Branch name pattern: main

✅ Require a pull request before merging
   ✅ Require approvals (1)
   ✅ Dismiss stale pull request approvals when new commits are pushed

✅ Require status checks to pass before merging
   ✅ Require branches to be up to date before merging
   Status checks that are required:
     - 🔍 Code Quality
     - 🧪 Unit Tests
     - 🔨 Build Debug APK

✅ Require conversation resolution before merging

❌ Do not allow bypassing the above settings
```

4. Repeat for `dev` branch with relaxed rules if needed

### Step 3: Add Repository Secrets (Optional)

For signed release APKs, add these secrets:

1. Go to **Settings** → **Secrets and variables** → **Actions**
2. Add the following secrets:

| Secret Name | Description |
|-------------|-------------|
| `LOOP_KEY_ALIAS` | Keystore alias |
| `LOOP_KEY_PASSWORD` | Key password |
| `LOOP_KEY_STORE` | Base64 encoded keystore file |
| `LOOP_STORE_PASSWORD` | Keystore password |

**To encode keystore:**
```bash
base64 -i your-keystore.jks > keystore-base64.txt
```

---

## 🚀 Triggering Builds

### Automatic Triggers

| Event | Branches | Pipeline |
|-------|----------|----------|
| Push | main, dev, master | Full CI/CD |
| Pull Request | main, dev, master | Full CI/CD |
| Tag `v*` | Any | Create Release |

### Manual Triggers

1. Go to **Actions** tab
2. Select workflow (e.g., "CI/CD Pipeline")
3. Click **Run workflow**
4. Select options:
   - Branch
   - Build type (debug/release)
   - Run tests (yes/no)

---

## 📦 Downloading APK Artifacts

### From Actions

1. Go to **Actions** tab
2. Click on the completed workflow run
3. Scroll to **Artifacts** section
4. Download the APK zip file

### From Releases

1. Go to **Releases** section
2. Find the desired version
3. Download APK from Assets

---

## 🧪 Quality Gates

The pipeline enforces these quality gates:

| Check | Tool | Blocking |
|-------|------|----------|
| Kotlin Style | ktlint | ⚠️ Warning |
| Android Lint | Android Lint | ⚠️ Warning |
| Unit Tests | JUnit | ✅ Required |
| Build | Gradle | ✅ Required |
| Security | OWASP Dependency Check | ⚠️ Warning |

---

## 📊 Viewing Reports

After a pipeline run, check the **Artifacts** section for:

- `ktlint-report` - Code style issues
- `lint-report` - Android lint warnings
- `unit-test-reports` - Test results & failures
- `coverage-reports` - Code coverage data
- `security-report` - Dependency vulnerabilities

---

## 🔄 Updating Dependencies

Dependabot is configured to:

- Check for Gradle dependency updates weekly
- Check for GitHub Actions updates weekly
- Create PRs automatically with `dependencies` label

Review and merge Dependabot PRs to keep dependencies current.

---

## 🐛 Troubleshooting

### Build Fails with "SDK not found"

The pipeline uses `android-actions/setup-android@v3` to setup the SDK.
If issues persist, check the workflow logs.

### Tests Fail on Emulator

Instrumented tests require KVM support. These only run on:
- Pull requests
- When explicitly enabled

### APK Not Signed

Release APKs are unsigned unless secrets are configured.
Configure signing secrets for production releases.

---

## 📞 Support

For issues with the CI/CD pipeline:

1. Check workflow run logs
2. Review error messages in the Summary
3. Check the Artifacts for detailed reports

---

## 🎉 Quick Commands

```bash
# Trigger a build locally
./gradlew assembleDebug

# Run tests locally
./gradlew test

# Run lint locally
./gradlew ktlintCheck lintDebug

# Build release APK
./gradlew assembleRelease
```

---

**Happy Building! 🚀**
