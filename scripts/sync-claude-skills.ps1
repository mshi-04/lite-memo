[CmdletBinding()]
param(
    [switch]$Check
)

$ErrorActionPreference = 'Stop'

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$sourceRoot = Join-Path $repositoryRoot '.agents/skills'
$destinationRoot = Join-Path $repositoryRoot '.claude/skills'
$excludedClaudeSkillNames = @()

# Claude Code へ同期しない相対パス（skill directory 起点）。
# `agents/` 配下は Codex 専用メタ情報（agents/openai.yaml など）なので Claude Code には配らない。
# スキル名単位ではなく相対パス単位で判定し、Copy 側と -Check 側の両方で同じ規則を使う。
$excludedRelativePathPrefixes = @('agents/')

function Test-IsSyncedRelativePath {
    param([Parameter(Mandatory)][string]$RelativePath)

    $normalized = ($RelativePath -replace '\\', '/')
    foreach ($prefix in $excludedRelativePathPrefixes) {
        $trimmed = $prefix.TrimEnd('/')
        if ($normalized -eq $trimmed -or $normalized.StartsWith($prefix)) {
            return $false
        }
    }

    return $true
}

if (-not (Test-Path -LiteralPath $sourceRoot -PathType Container)) {
    throw "Source skill directory does not exist: $sourceRoot"
}

$sourceSkills = Get-ChildItem -LiteralPath $sourceRoot -Directory | Sort-Object Name
if ($sourceSkills.Count -eq 0) {
    throw "No source skills found in: $sourceRoot"
}

$differences = [System.Collections.Generic.List[string]]::new()
$sourceSkillNames = @($sourceSkills.Name)

if (-not (Test-Path -LiteralPath $destinationRoot -PathType Container)) {
    if ($Check) {
        $differences.Add('Missing Claude Code skill directory: .claude/skills')
    } else {
        New-Item -ItemType Directory -Path $destinationRoot -Force | Out-Null
    }
}

if (Test-Path -LiteralPath $destinationRoot -PathType Container) {
    $generatedSkills = Get-ChildItem -LiteralPath $destinationRoot -Directory | Where-Object {
        $_.Name -notin $excludedClaudeSkillNames
    }

    foreach ($generatedSkill in $generatedSkills) {
        if ($generatedSkill.Name -notin $sourceSkillNames) {
            $message = "Stale generated Claude Code skill: .claude/skills/$($generatedSkill.Name)"
            if ($Check) {
                $differences.Add($message)
            } else {
                Remove-Item -LiteralPath $generatedSkill.FullName -Recurse -Force
            }
        }
    }
}

foreach ($sourceSkill in $sourceSkills) {
    $destinationSkill = Join-Path $destinationRoot $sourceSkill.Name

    if (-not (Test-Path -LiteralPath $destinationSkill -PathType Container)) {
        $differences.Add("Missing Claude Code skill: .claude/skills/$($sourceSkill.Name)")
        if ($Check) {
            continue
        }
    }

    if (-not $Check) {
        if (Test-Path -LiteralPath $destinationSkill) {
            Remove-Item -LiteralPath $destinationSkill -Recurse -Force
        }
        New-Item -ItemType Directory -Path $destinationSkill -Force | Out-Null

        $sourceFiles = Get-ChildItem -LiteralPath $sourceSkill.FullName -File -Recurse
        foreach ($sourceFile in $sourceFiles) {
            $relativePath = $sourceFile.FullName.Substring($sourceSkill.FullName.Length + 1)
            if (-not (Test-IsSyncedRelativePath $relativePath)) {
                continue
            }

            $destinationFile = Join-Path $destinationSkill $relativePath
            $destinationDirectory = Split-Path -Parent $destinationFile
            if (-not (Test-Path -LiteralPath $destinationDirectory -PathType Container)) {
                New-Item -ItemType Directory -Path $destinationDirectory -Force | Out-Null
            }
            Copy-Item -LiteralPath $sourceFile.FullName -Destination $destinationFile -Force
        }
        continue
    }

    $sourceFiles = Get-ChildItem -LiteralPath $sourceSkill.FullName -File -Recurse
    $destinationFiles = Get-ChildItem -LiteralPath $destinationSkill -File -Recurse
    $sourceRelativePaths = @(
        $sourceFiles |
            ForEach-Object { $_.FullName.Substring($sourceSkill.FullName.Length + 1) } |
            Where-Object { Test-IsSyncedRelativePath $_ }
    )
    $destinationRelativePaths = @($destinationFiles | ForEach-Object { $_.FullName.Substring($destinationSkill.Length + 1) })

    foreach ($relativePath in Compare-Object -ReferenceObject $sourceRelativePaths -DifferenceObject $destinationRelativePaths) {
        $differences.Add("File list differs for $($sourceSkill.Name): $($relativePath.InputObject)")
    }

    foreach ($relativePath in $sourceRelativePaths) {
        $sourceFile = Join-Path $sourceSkill.FullName $relativePath
        $destinationFile = Join-Path $destinationSkill $relativePath
        if ((Test-Path -LiteralPath $destinationFile -PathType Leaf) -and
            (Get-FileHash -LiteralPath $sourceFile -Algorithm SHA256).Hash -ne (Get-FileHash -LiteralPath $destinationFile -Algorithm SHA256).Hash) {
            $differences.Add("Content differs for $($sourceSkill.Name): $relativePath")
        }
    }
}

# AGENTS.md は skill 一覧を地図として手動で持つため、`.agents/skills/` との増減ずれを検出する。
# AGENTS.md の説明文は人手で書くので自動生成せず、-Check で不一致を報告するだけに留める。
$agentsMdPath = Join-Path $repositoryRoot 'AGENTS.md'
if (Test-Path -LiteralPath $agentsMdPath -PathType Leaf) {
    $agentsMdContent = Get-Content -LiteralPath $agentsMdPath -Raw
    $listedSkillNames = @(
        [regex]::Matches($agentsMdContent, '\.agents/skills/([^/]+)/SKILL\.md') |
            ForEach-Object { $_.Groups[1].Value } |
            Sort-Object -Unique
    )

    foreach ($listedName in $listedSkillNames) {
        if ($listedName -notin $sourceSkillNames) {
            $differences.Add("AGENTS.md references a missing skill: $listedName")
        }
    }

    foreach ($sourceName in $sourceSkillNames) {
        if ($sourceName -notin $listedSkillNames) {
            $differences.Add("AGENTS.md does not list skill: $sourceName")
        }
    }
}

if ($Check) {
    if ($differences.Count -gt 0) {
        Write-Output 'Claude Code skills are out of sync with .agents/skills:'
        $differences | ForEach-Object { Write-Output "  - $_" }
        exit 1
    }

    Write-Output 'Claude Code skills are synchronized with .agents/skills.'
    exit 0
}

Write-Output 'Synchronized .agents/skills to .claude/skills.'
