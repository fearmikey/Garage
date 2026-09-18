# F-Droid Submission Guide for Garage

This directory contains the ready-to-use F-Droid build recipe for **Garage** (`com.fearmikey.garage`).

## Quick Steps to Submit to F-Droid

### 1. Ensure `v1.1.8` Tag is Pushed to GitHub
Run the following git commands in your repository root:
```bash
git tag -a v1.1.8 -m "Release 1.1.8"
git push origin v1.1.8
```

### 2. Fork `fdroiddata` on GitLab
1. Go to [https://gitlab.com/fdroid/fdroiddata](https://gitlab.com/fdroid/fdroiddata)
2. Click **Fork** to create a copy under your GitLab account.

### 3. Add `com.fearmikey.garage.yml` to `fdroiddata`
Clone your fork locally:
```bash
git clone https://gitlab.com/<YOUR_GITLAB_USERNAME>/fdroiddata.git
cd fdroiddata
git checkout -b add-garage
```

Copy `fdroid/com.fearmikey.garage.yml` from this project into `metadata/com.fearmikey.garage.yml` inside the `fdroiddata` repository:
```bash
cp /path/to/Garage/fdroid/com.fearmikey.garage.yml metadata/com.fearmikey.garage.yml
```

### 4. Commit and Open Merge Request
```bash
git add metadata/com.fearmikey.garage.yml
git commit -m "com.fearmikey.garage: Add new package"
git push origin add-garage
```
Then visit GitLab and open a **Merge Request** from your `add-garage` branch to `fdroid/fdroiddata:master`.
F-Droid's automated CI will lint and test building your app!
