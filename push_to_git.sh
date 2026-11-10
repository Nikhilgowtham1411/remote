#!/usr/bin/env bash

set -e

echo "======================================================="
echo "    Smart TV Remote - Git Setup & GitHub Push"
echo "======================================================="
echo ""

if ! command -v git &> /dev/null; then
    echo "[ERROR] Git is not installed. Please install git and try again."
    exit 1
fi

if [ ! -d ".git" ]; then
    echo "[*] Initializing Git repository..."
    git init
    git branch -M main
else
    echo "[*] Git repository already exists."
    git branch -M main
fi

echo "[*] Staging files..."
git add .

echo "[*] Committing files..."
git commit -m "Initial commit: Smart TV Remote Android application (Google TV v2)" || true

if ! git remote get-url origin &> /dev/null; then
    echo ""
    echo "-------------------------------------------------------"
    echo "Enter your GitHub / GitLab repository URL:"
    echo "Example: https://github.com/your-username/smart-tv-remote.git"
    echo "-------------------------------------------------------"
    read -rp "Repository URL: " REPO_URL

    if [ -z "$REPO_URL" ]; then
        echo "[ERROR] Repository URL cannot be empty."
        exit 1
    fi

    git remote add origin "$REPO_URL"
    echo "[*] Remote origin set to $REPO_URL"
else
    CURRENT_ORIGIN=$(git remote get-url origin)
    echo "[*] Remote origin already set: $CURRENT_ORIGIN"
fi

echo ""
echo "[*] Pushing to remote repository on branch 'main'..."
git push -u origin main

echo ""
echo "======================================================="
echo "[SUCCESS] Project pushed! GitHub Actions will now build your APK automatically."
echo "Check the 'Actions' tab on your GitHub repository."
echo "======================================================="
