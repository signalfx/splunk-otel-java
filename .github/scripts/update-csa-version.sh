#!/usr/bin/env bash
set -euo pipefail

source_repo="signalfx/csa-releases"
target_repo="${GITHUB_REPOSITORY:?GITHUB_REPOSITORY must be set}"
base_branch="main"
target_file="agent-csa-bundle/build.gradle.kts"
expected_asset="oss-agent-mtagent-extension-deployment.jar"

read_csa_version() {
  sed -nE 's/^val csaVersion = "([^"]+)"$/\1/p'
}

release_json="$(gh api "repos/${source_repo}/releases/latest")"
release_id="$(jq -r '.id' <<< "$release_json")"
release_tag="$(jq -r '.tag_name' <<< "$release_json")"
release_url="$(jq -r '.html_url' <<< "$release_json")"

if [[ ! "$release_id" =~ ^[0-9]+$ ]]; then
  echo "Invalid latest release ID: $release_id" >&2
  exit 1
fi

if [[ ! "$release_tag" =~ ^[0-9]+\.[0-9]+\.[0-9]+-[0-9]+$ ]]; then
  echo "Unsupported CSA release tag: $release_tag" >&2
  exit 1
fi

if ! jq -e --arg asset "$expected_asset" \
  'any(.assets[]; .name == $asset)' <<< "$release_json" >/dev/null; then
  echo "Latest release $release_tag does not contain $expected_asset" >&2
  exit 1
fi

if [[ ! -f "$target_file" ]]; then
  echo "Target file does not exist: $target_file" >&2
  exit 1
fi

mapfile -t current_versions < <(read_csa_version < "$target_file")
if [[ "${#current_versions[@]}" -ne 1 ]]; then
  echo "Expected exactly one csaVersion declaration in $target_file" >&2
  exit 1
fi

current_version="${current_versions[0]}"
if [[ "$current_version" == "$release_tag" ]]; then
  echo "splunk-otel-java already bundles the latest CSA release, $release_tag."
  exit 0
fi

# Avoid duplicating a manually-created or differently-named update PR.
while IFS=$'\t' read -r pr_number pr_url; do
  if gh pr diff "$pr_number" --repo "$target_repo" |
    grep -Fx "+val csaVersion = \"${release_tag}\"" >/dev/null; then
    echo "An update pull request already exists: $pr_url"
    exit 0
  fi
done < <(
  gh pr list \
    --repo "$target_repo" \
    --state open \
    --limit 100 \
    --json number,url \
  --jq '.[] | [.number, .url] | @tsv'
)

if [[ "${DRY_RUN:-false}" == true ]]; then
  echo "Would update CSA from $current_version to $release_tag."
  exit 0
fi

branch_name="update-csa-version-${release_tag}"
existing_pr="$(
  gh pr list \
    --repo "$target_repo" \
    --base "$base_branch" \
    --head "$branch_name" \
    --state open \
    --json url \
    --jq '.[0].url // empty'
)"
if [[ -n "$existing_pr" ]]; then
  echo "An update pull request already exists: $existing_pr"
  exit 0
fi

base_sha="$(
  gh api "repos/${target_repo}/git/ref/heads/${base_branch}" --jq '.object.sha'
)"
branch_sha="$(
  gh api "repos/${target_repo}/git/ref/heads/${branch_name}" \
    --jq '.object.sha' 2>/dev/null || true
)"
needs_commit=true

if [[ -n "$branch_sha" ]]; then
  remote_version="$(
    gh api "repos/${target_repo}/contents/${target_file}?ref=${branch_name}" \
      -H 'Accept: application/vnd.github.raw+json' |
      read_csa_version
  )"

  if [[ "$remote_version" == "$release_tag" ]]; then
    echo "The update commit already exists on $branch_name."
    needs_commit=false
  elif [[ "$branch_sha" != "$base_sha" ]]; then
    echo "Remote branch $branch_name contains unexpected changes; refusing to overwrite it." >&2
    exit 1
  fi
else
  gh api \
    --method POST \
    "repos/${target_repo}/git/refs" \
    -f ref="refs/heads/${branch_name}" \
    -f sha="$base_sha" \
    --silent
fi

if [[ "$needs_commit" == true ]]; then
  sed -i.bak -E \
    "s/^val csaVersion = \"[^\"]+\"$/val csaVersion = \"${release_tag}\"/" \
    "$target_file"
  rm -f "${target_file}.bak"

  updated_version="$(read_csa_version < "$target_file")"
  if [[ "$updated_version" != "$release_tag" ]]; then
    echo "Failed to update csaVersion in $target_file" >&2
    exit 1
  fi

  target_file_sha="$(
    gh api "repos/${target_repo}/contents/${target_file}?ref=${branch_name}" \
      --jq '.sha'
  )"
  encoded_content="$(base64 < "$target_file" | tr -d '\n')"
  commit_message="[automated] Update CSA version to $release_tag"

  jq -n \
    --arg message "$commit_message" \
    --arg content "$encoded_content" \
    --arg branch "$branch_name" \
    --arg sha "$target_file_sha" \
    '{message: $message, content: $content, branch: $branch, sha: $sha}' |
    gh api \
      --method PUT \
      "repos/${target_repo}/contents/${target_file}" \
      --input - \
      --jq '"Created commit " + .commit.sha'
fi

pr_body="$(cat <<EOF
Updates the Cisco Secure Application version bundled with splunk-otel-java from \`$current_version\` to \`$release_tag\`.

Source release: $release_url

This pull request was created automatically by the daily CSA version workflow.
EOF
)"

gh pr create \
  --repo "$target_repo" \
  --base "$base_branch" \
  --head "$branch_name" \
  --title "Update CSA version to $release_tag" \
  --body "$pr_body" \
  --label automated
