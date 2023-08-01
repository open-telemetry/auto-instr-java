# Releasing

## Publish Release via Github Workflow

### Prepare for Release

Create/switch to a new branch off of `main`.

```sh
git checkout -b Update_for_new_release
```

From the project root, run the following command to update the repo with the new version (ex. 1.0.0)
```sh
> ./scripts/release.sh "<VERSION>" 
```
Also update the repo's CHANGELOG with details about the release. Then commit/push the changes and open a PR. 
Merge the PR once approved.

```sh
git tag -a v<VERSION> -m "Release v<VERSION>"
git push origin v<VERSION>
```
