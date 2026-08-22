# Release Checklist

This file documents the release process for ja-netfilter-kotlin.

## First Release Process

1. ✅ Code is pushed to `main` branch on GitHub
2. ✅ CI workflows (`.github/workflows/ci.yml`) pass on Linux, macOS, Windows
3. ✅ release-please (`.github/workflows/release-please.yml`) detects Conventional Commits
4. 📝 release-please creates a "Release PR" with:
   - New version number (based on commit types)
   - Auto-generated CHANGELOG.md
5. ✋ **Maintainer reviews and merges the Release PR**
6. 🚀 GitHub Release is automatically created with all build artifacts

## Triggering a Release

Any commit to `main` matching Conventional Commits spec will trigger release-please:

- `feat: <description>` → minor version bump (2.2.0 → 2.3.0)
- `fix: <description>` → patch version bump (2.2.0 → 2.2.1)
- `feat!: <description>` or `BREAKING CHANGE:` → major version bump (2.2.0 → 3.0.0)
- `chore:`, `docs:`, etc. → no version bump

## One-Click Install

After release, users can install via:

```bash
curl -fsSL https://raw.githubusercontent.com/MapleREHub/ja-netfilter-kotlin/main/scripts/install-from-release.sh | bash
```
