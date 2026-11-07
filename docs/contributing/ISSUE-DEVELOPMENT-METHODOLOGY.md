---
title: Issue Development Methodology
description: Mandatory phased workflow (planning → implementation → testing → integration) for issue development in portunix-vim, including role responsibilities, quality gates, and the acceptance protocol template.
category: contributing-guide
ai_load: scoped
status: active
language: en
last_updated: 2026-11-07
related:
  - ISSUE-MANAGEMENT.md
---

# Issue Development Methodology

## Overview

This document defines the mandatory workflow for issue development in the Portunix VIM project.
This methodology ensures quality, traceability, and proper testing before code integration
into the main branch.

## Development Workflow

### Phase 1: Issue Creation & Planning

#### Role: Architect / Product Owner

1. **Issue Creation**
   - Create the GitHub issue first — GitHub assigns `{number}` (see
     [ISSUE-MANAGEMENT.md](ISSUE-MANAGEMENT.md))
   - Create file `docs/issues/{number}-{name}.md` with the full write-up and link it from the
     GitHub issue body
   - No overview table to update — GitHub is the list of record
   - Define acceptance criteria and requirements
   - Assign appropriate labels and priority

2. **Technical Planning**
   - Define technical requirements
   - Identify affected components
   - Estimate implementation complexity
   - Create branch following the project naming convention:
     `feature/{number}-{short-name}` for features/enhancements,
     `fix/{number}-{short-name}` for bug fixes

### Phase 2: Implementation

#### Role: Developer

1. **Branch Setup**
   - Create feature branch from main: `git checkout -b feature/{number}-{short-name}`
     (use `fix/{number}-{short-name}` for bug fixes)
   - Ensure branch is up-to-date with main before starting

2. **Development Process**
   - Follow existing code conventions and patterns
   - Implement minimal diffs with maximum impact
   - Add appropriate tests for new functionality
   - Document changes in code comments (English only)
   - Commit frequently with descriptive messages

3. **Pre-Testing Validation**
   - Run local tests and build successfully, depending on the language:
     - **Java**: `make verify` in the repository root (unit + integration tests, same as CI)
     - **Python**: `uv run pytest` in the project under `python/`
     - **Go**: `go test ./...` and `go build -o .` in the project under `go/`
   - Verify no breaking changes to existing functionality
   - Self-review code changes

4. **Merge Restriction**
   - **CRITICAL**: Developer MUST NOT merge to main branch at this stage
   - Feature branch remains separate until tester approval
   - Push feature branch to repository for tester access

### Phase 3: Testing & Validation

#### Role: Tester

1. **Test Environment Setup**
   - Checkout feature branch: `git checkout feature/{number}-{short-name}`
   - Build and deploy in test environment
   - Prepare test data and scenarios
   - **OS Validation**: Verify testing environment matches tester role
     (Linux tester -> Linux tests, Windows tester -> Windows tests)
   - For container/VM testing: Accept tests based on container/VM OS, not host OS

2. **Testing Execution**
   - Execute functional tests based on acceptance criteria
   - Perform regression testing on related components
   - Test cross-platform compatibility (Windows/Linux)
   - Validate edge cases and error handling

3. **Acceptance Protocol Creation**
   - Create acceptance protocol document: `docs/testing/acceptance-{issue-number}.md`
   - Document all test scenarios and results
   - Include screenshots/logs where applicable
   - Record any issues or recommendations
   - **MANDATORY**: Document testing OS
     (host OS for local tests, container/VM OS for containerized tests)

4. **Acceptance Decision**
   - **PASS**: Issue ready for merge - create approval document
   - **FAIL**: Document blocking issues, return to developer
   - **CONDITIONAL**: List required changes before approval

### Phase 4: Integration

#### Role: Developer (with Tester approval)

1. **Pre-Merge Validation**
   - Verify acceptance protocol exists and shows PASS status
   - Ensure all tester recommendations are addressed
   - Rebase feature branch on current main if needed

2. **Merge Process**
   - Create merge request/pull request with acceptance protocol reference
   - Merge feature branch to main: `git merge feature/{number}-{short-name}`
   - Update issue status to `✅ Implemented` in the issue file header
   - CI ([`.github/workflows/vim-parser.yml`](../../.github/workflows/vim-parser.yml)) must pass
     on the pull request
   - Close GitHub issue with reference to acceptance protocol

3. **Archive Completed Issue**
   - Move the issue file to the `done/` subdirectory (preserves git history):

     ```bash
     git mv docs/issues/{number}-{name}.md \
            docs/issues/done/{number}-{name}.md
     ```

   - No overview table to update — close the GitHub issue (`gh issue close {number}`)
   - The same rule applies to `❌ Closed` issues (closed without implementation) and
     `➡️ Moved` issues (moved to another repository)

4. **Post-Merge Cleanup**
   - Delete feature branch after successful merge
   - Update any related documentation
   - Notify team of completed feature

## Quality Gates

### Mandatory Requirements

- Issue must have defined acceptance criteria
- Code must pass all automated tests
- Feature branch must build successfully
- Tester must create acceptance protocol
- Acceptance protocol must show PASS status
- No merge to main without tester approval

### Acceptance Protocol Template

```markdown
# Acceptance Protocol - Issue #{number}

**Issue**: {issue-title}
**Branch**: feature/{number}-{short-name}
**Tester**: {tester-name}
**Date**: {test-date}
**Testing OS**: {operating-system} (host/container/VM)

## Test Summary
- Total test scenarios: {count}
- Passed: {count}
- Failed: {count}
- Skipped: {count}

## Test Results
### Functional Tests
- [x] Feature works as specified
- [x] Acceptance criteria met
- [ ] Edge cases handled

### Regression Tests
- [x] Existing functionality unaffected
- [x] Cross-platform compatibility verified

## Final Decision
**STATUS**: [PASS/FAIL/CONDITIONAL]

**Approval for merge**: [YES/NO]
**Date**: {approval-date}
**Tester signature**: {tester-name}
```

## Software Installation Testing Guidelines

### Container-Based Testing Policy

**MANDATORY**: All software installation testing MUST be performed in isolated containers,
never on the host development system.

#### Rationale

- **Host Protection**: Prevents contamination of development environment with test software
- **Isolation**: Each test runs in clean, reproducible environment
- **Safety**: Eliminates risk of conflicts with existing host software
- **Consistency**: Standardized testing environment across all developers

#### Implementation Methods

##### Method 1: Portunix Container Integration (Recommended)

Always use the `portunix container` commands — Portunix abstracts the runtime and works
with both Docker and Podman transparently. Never invoke `docker` or `podman` directly.

```bash
# Create clean Ubuntu container for testing
portunix container run ubuntu

# Inside container, install Portunix and the plugin/runtime under test
./portunix install java
./portunix install --dry-run python --variant full
```

##### Method 2: Manual Container Setup (last resort)

```bash
# Only when the portunix container commands are genuinely unavailable
docker run -it --rm ubuntu:latest bash
# Transfer binary and test in clean environment
```

#### Container Testing Workflow

1. **Environment Preparation**
   - Create fresh container instance
   - Copy Portunix binary to container
   - Verify clean starting state (no pre-installed software)

2. **Installation Testing**
   - Test primary package installation
   - Test prerequisite dependency resolution
   - Test variant selections and configurations
   - Test error handling and rollback scenarios

3. **Verification Testing**
   - Verify installed software works correctly
   - Test command availability and functionality
   - Validate PATH updates and environment setup
   - Check prerequisite satisfaction

4. **Cleanup Testing**
   - Test uninstallation if supported
   - Verify clean removal of dependencies
   - Test reinstallation after removal

#### Prohibited Practices

**NEVER DO:**

- Install software packages directly on development host for testing
- Test package installation on primary development machine
- Use `sudo` commands during testing on host system
- Install dependencies manually on host to "help" testing

**ALWAYS DO:**

- Use containers for all software installation tests
- Test in clean, isolated environments
- Document container setup procedures
- Include container commands in test scripts

#### Container Test Environment Standards

##### Base Images

- **Ubuntu**: `ubuntu:22.04` or `ubuntu:24.04` (primary testing)
- **Debian**: `debian:bookworm` (compatibility testing)
- **Fedora**: `fedora:latest` (RPM-based testing)
- **Alpine**: `alpine:latest` (minimal environment testing)

##### Container Configuration

```bash
# Standard container setup for testing
portunix container run ubuntu

# Inside container
apt update && apt install -y curl wget
# Copy and test Portunix
```

#### Integration with Testing Phase

During **Phase 3: Testing & Validation**, testers MUST:

1. **Document Container Environment**

   ```markdown
   ## Test Environment
   - Container: ubuntu:22.04
   - Portunix Version: v1.5.12
   - Test Date: 2025-09-12
   - Container Setup: portunix container run ubuntu
   ```

2. **Include Container Test Results**
   - Document container setup commands
   - Include installation output from clean environment
   - Verify no host system contamination
   - Test multiple container instances if needed

3. **Container-Specific Test Cases**
   - Fresh environment installation
   - Dependency resolution testing
   - Cross-platform compatibility
   - Clean environment verification

#### Developer Guidelines for AI Assistants

**For AI Development Assistants (Claude, etc.):**

- Always suggest container-based testing for software installation
- Never recommend installing test software on host system
- Provide container setup commands in testing recommendations
- Include dry-run testing before real installation attempts

**Example Workflow:**

```bash
# CORRECT: Container-based testing
portunix container run ubuntu
./portunix install nodejs --dry-run    # Preview changes
./portunix install nodejs             # Test actual installation

# INCORRECT: Host system testing
./portunix install nodejs             # Installs on development machine
```

## Process Enforcement

### Developer Responsibilities

- Follow all development phases in order
- Never bypass testing phase
- Never merge without tester approval
- Maintain feature branch until approval received

### Tester Responsibilities

- Test within reasonable timeframe
- Provide detailed acceptance protocol
- Clearly communicate pass/fail status
- Document all findings and recommendations

### Architect/Product Owner Responsibilities

- Provide clear acceptance criteria
- Review acceptance protocols when needed
- Make final decisions on conditional approvals

## Exception Handling

### Critical Hotfixes

- May bypass normal workflow with architect approval
- Must be tested immediately after deployment
- Retroactive acceptance protocol required within 24 hours

### Testing Delays

- If tester unavailable, architect may designate alternate tester
- Testing deadline: 48 hours for normal issues, 24 hours for high priority
- Escalation to architect if testing timeline exceeded

## Benefits of This Methodology

1. **Quality Assurance**: Every change is thoroughly tested before integration
2. **Traceability**: Clear documentation trail from issue to implementation
3. **Risk Reduction**: No untested code reaches main branch
4. **Team Coordination**: Clear roles and responsibilities
5. **Documentation**: Comprehensive testing records for future reference

---

**Last Updated**: 2026-11-07
**Version**: 1.2
**Status**: Active
