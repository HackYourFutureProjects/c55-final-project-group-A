#!/usr/bin/env bash
# Validate the dbt project in CI: conflict markers, parse, and compile.
#
# Run from the repository root. Requires uv and the data/ project.
#
# Env (set by data-ci-cd.yaml):
#   DATABRICKS_HOST, DATABRICKS_HTTP_PATH, DATABRICKS_CATALOG — always
#   DBT_SCHEMA — CI scratch schema (default: ci)
#   LANDING_PATH — optional; defaults from catalog
#
# For dbt compile (needs a warehouse connection):
#   DATABRICKS_CLIENT_ID + DATABRICKS_CLIENT_SECRET — team SP (prod target)
#   or DATABRICKS_TOKEN / DATABRICKS_CI_TOKEN — personal PAT (dev target)

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
DATA="${ROOT}/data"
DBT="${DATA}/dbt"

if [ ! -d "${DBT}" ]; then
  echo "No data/dbt project; skipping dbt checks."
  exit 0
fi

# Merge conflict markers — catches unresolved git merges in SQL/Python/YAML.
# sqlfmt does not fail on these (team C PR #92).
conflict_hits="$(
  rg -n '<<<<<<<|>>>>>>>|^=======$|<<\s+<<\s+<<|>>\s+>>' \
    "${DATA}/dbt" "${DATA}/src" "${DATA}/tests" 2>/dev/null || true
)"
if [ -n "${conflict_hits}" ]; then
  echo "${conflict_hits}"
  echo "::error::Unresolved git merge conflict markers under data/. Search for <<<<<<< and fix before merging."
  exit 1
fi

export DATABRICKS_HOST="${DATABRICKS_HOST:?DATABRICKS_HOST must be set}"
export DATABRICKS_HTTP_PATH="${DATABRICKS_HTTP_PATH:?DATABRICKS_HTTP_PATH must be set}"
export DATABRICKS_CATALOG="${DATABRICKS_CATALOG:?DATABRICKS_CATALOG must be set}"
export DBT_SCHEMA="${DBT_SCHEMA:-ci}"
export LANDING_PATH="${LANDING_PATH:-/Volumes/${DATABRICKS_CATALOG}/landing/prod}"

cd "${DATA}"
uv sync --extra dbt -q

echo "Running dbt parse..."
uv run dbt parse --project-dir dbt --profiles-dir dbt

compile_target=""
if [ -n "${DATABRICKS_CLIENT_ID:-}" ] && [ -n "${DATABRICKS_CLIENT_SECRET:-}" ]; then
  export DBT_TARGET=prod
  compile_target=prod
elif [ -n "${DATABRICKS_TOKEN:-${DATABRICKS_CI_TOKEN:-}}" ]; then
  export DATABRICKS_TOKEN="${DATABRICKS_TOKEN:-${DATABRICKS_CI_TOKEN}}"
  export DBT_TARGET=dev
  compile_target=dev
else
  echo "::warning::No Databricks credentials in GitHub secrets — skipped dbt compile. dbt parse and conflict-marker checks still ran."
  echo "Add DATABRICKS_CLIENT_ID and DATABRICKS_CLIENT_SECRET (team SP from Key Vault) to enable compile on pull requests."
  exit 0
fi

echo "Running dbt compile (target=${compile_target})..."
uv run dbt compile --project-dir dbt --profiles-dir dbt --target "${compile_target}"

compiled_hits="$(
  rg -n '<<<<<<<|>>>>>>>|^=======$|<<\s+<<\s+<<|>>\s+>>' dbt/target/compiled 2>/dev/null || true
)"
if [ -n "${compiled_hits}" ]; then
  echo "${compiled_hits}"
  echo "::error::Compiled dbt SQL still contains merge conflict markers. Fix the source models."
  exit 1
fi

echo "dbt parse and compile succeeded."
