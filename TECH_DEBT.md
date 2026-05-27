# Tech Debt Remediation Queue

This document is the Goal-mode execution queue for Pet Clinic technical debt. Linear is the source of truth for issue scope, priority, status, and final closure. Use this file to choose the next PR-sized unit of work, then confirm current details in Linear before starting.

## Prioritization Rubric

Work is ordered by:

1. Risk: security, credential exposure, data integrity, and supply-chain issues first.
2. User impact: issues that create 500s, broken UX, or unreliable deployments before lower-noise cleanup.
3. Implementation complexity: low-risk, low-complexity fixes are placed early when they unblock validation.
4. Dependency order: validation and platform-hardening work follows the fixes it should prove.
5. PR reviewability: group issues only when shared files and behavior make one PR easier to review.

## Linear Status Workflow

For every work item:

1. Move the Linear issue from `Backlog` to `In Progress` when active implementation starts.
2. Work in the dedicated worktree listed for the item unless the item explicitly says a worktree is optional.
3. Comment in Linear with the branch/worktree name when work starts.
4. Open the expected PR scope and title listed below; add the PR link to the Linear issue.
5. Comment in Linear before pausing if blockers, scope changes, or follow-up issues appear.
6. Move the Linear issue to `Done` only after the PR is merged and the definition of done is verified.

## Work Items

### 1. HEN-6: Remove Checked-In Database Passwords and Unsafe Credential Defaults

- Linear issue: [HEN-6](https://linear.app/henzelmann-demos/issue/HEN-6/remove-checked-in-database-passwords-and-unsafe-credential-defaults)
- Problem summary: Kubernetes manifests commit literal database credentials, and MySQL/Postgres profiles fall back to predictable passwords when explicit configuration is missing.
- Recommended fix: Replace committed secret values with a local-only sample, template, sealed secret, or external-secret reference. Make deployable MySQL/Postgres profile credentials explicit instead of silently defaulting to shared values. Update README database setup guidance.
- Sequencing/dependencies: Do this first because it is high-risk security debt and unblocks safer Kubernetes CI changes.
- Definition of done: No deployable manifest contains real or predictable database passwords; missing deployable credentials fail fast; local/test credential paths remain documented.
- Test expectations: Add or update startup/config tests for absent DB credentials and a manifest/policy check rejecting obvious literals such as `pass`, `petclinic`, or empty password values.
- Dedicated sub-agent: Yes, use a security/config sub-agent.
- Dedicated worktree: Yes.
- Expected PR scope and title: `Remove unsafe database credential defaults`.
- Linear status update: Move HEN-6 to `In Progress` at start, comment with branch/worktree and PR link, move to `Done` after merge and verification.

### 2. HEN-5: Restrict Actuator Endpoints in Deployable Profiles

- Linear issue: [HEN-5](https://linear.app/henzelmann-demos/issue/HEN-5/restrict-actuator-endpoints-in-deployable-profiles)
- Problem summary: The default application profile exposes every actuator endpoint while Kubernetes can publish the app through a service.
- Recommended fix: Default actuator web exposure to `health,info`. Keep broad actuator exposure only in explicit dev/test configuration if still needed.
- Sequencing/dependencies: Complete before cluster smoke checks so CI can assert the hardened behavior.
- Definition of done: Sensitive endpoints such as `/actuator/env` are unavailable in deployable/prod-like profiles; health, liveness, and readiness still work.
- Test expectations: Add integration coverage for `/actuator/env`, `/actuator/health`, `/livez`, and `/readyz` under deployable/prod-like configuration.
- Dedicated sub-agent: No.
- Dedicated worktree: Yes.
- Expected PR scope and title: `Restrict actuator exposure by default`.
- Linear status update: Move HEN-5 to `In Progress` at start, comment with branch/worktree and PR link, move to `Done` after merge and verification.

### 3. HEN-10: Make Postgres Seed Data Safely Idempotent

- Linear issue: [HEN-10](https://linear.app/henzelmann-demos/issue/HEN-10/make-postgres-seed-data-safely-idempotent)
- Problem summary: Postgres seed data relies on generated IDs while later rows hard-code foreign keys, and SQL initialization runs on every startup.
- Recommended fix: Seed with explicit IDs plus `ON CONFLICT`, or use stable natural-key lookups for foreign keys. Reset identity sequences after fixed-ID inserts.
- Sequencing/dependencies: Complete before schema-hardening work that adds stricter non-null constraints.
- Definition of done: Postgres initialization can run repeatedly against the same database without duplicate data, bad generated IDs, or broken relationships.
- Test expectations: Add a Postgres integration test that runs schema/data initialization twice and verifies owners, pets, visits, vets, and specialties.
- Dedicated sub-agent: Yes, use a DB/test sub-agent.
- Dedicated worktree: Yes.
- Expected PR scope and title: `Make Postgres seed data idempotent`.
- Linear status update: Move HEN-10 to `In Progress` at start, comment with branch/worktree and PR link, move to `Done` after merge and verification.

### 4. HEN-7: Deploy Kubernetes CI Against a Repo-Built Immutable App Image

- Linear issue: [HEN-7](https://linear.app/henzelmann-demos/issue/HEN-7/deploy-kubernetes-ci-against-a-repo-built-immutable-app-image)
- Problem summary: Cluster CI deploys an untagged external image instead of an image built from the current repository commit.
- Recommended fix: Build the app image in CI, load it into Kind, and patch or template `k8s/petclinic.yml` to use the commit image tag or digest.
- Sequencing/dependencies: Do after HEN-6 if credential manifests or workflow inputs overlap.
- Definition of done: The cluster workflow deploys the app image built from the current commit and the deployment manifest requires an explicit tag or digest.
- Test expectations: Run the cluster workflow or equivalent local Kind path and assert the deployed pod image matches the current build artifact.
- Dedicated sub-agent: Helpful, use a CI/Kubernetes sub-agent.
- Dedicated worktree: Yes.
- Expected PR scope and title: `Deploy repo-built image in Kubernetes CI`.
- Linear status update: Move HEN-7 to `In Progress` at start, comment with branch/worktree and PR link, move to `Done` after merge and verification.

### 5. HEN-8: Add Kubernetes Runtime Hardening and Resource Limits

- Linear issue: [HEN-8](https://linear.app/henzelmann-demos/issue/HEN-8/add-kubernetes-runtime-hardening-and-resource-limits)
- Problem summary: App and database pods do not define runtime security contexts or CPU/memory resources.
- Recommended fix: Add compatible `securityContext` settings, dropped capabilities, seccomp profile, CPU/memory requests and limits, and non-root/read-only filesystem settings where practical.
- Sequencing/dependencies: Do after HEN-7 so hardening is validated against the repo-built image.
- Definition of done: App and database pods retain readiness while running with hardened runtime settings and bounded resources.
- Test expectations: Add kube-linter, Polaris, Checkov, or equivalent policy validation; run Kind deploy and confirm pods become Ready.
- Dedicated sub-agent: Helpful, use a Kubernetes sub-agent.
- Dedicated worktree: Yes.
- Expected PR scope and title: `Harden Kubernetes runtime settings`.
- Linear status update: Move HEN-8 to `In Progress` at start, comment with branch/worktree and PR link, move to `Done` after merge and verification.

### 6. HEN-21: Add Post-Deploy Smoke and Security Checks to Cluster CI

- Linear issue: [HEN-21](https://linear.app/henzelmann-demos/issue/HEN-21/add-post-deploy-smoke-and-security-checks-to-cluster-ci)
- Problem summary: Kubernetes CI waits for pods but does not make HTTP or security assertions after deployment.
- Recommended fix: Reach the app service through port-forwarding or an equivalent Kind route and assert the main page, liveness, readiness, and sensitive actuator endpoint behavior.
- Sequencing/dependencies: Do after HEN-5 and HEN-7; prefer after HEN-8 so the smoke checks cover the hardened deployment.
- Definition of done: Cluster CI fails on broken app routing, failed probes, or exposed sensitive actuator endpoints.
- Test expectations: Assert `/` returns `200`, `/livez` and `/readyz` are healthy, and `/actuator/env` is unavailable.
- Dedicated sub-agent: Helpful, use a CI/Kubernetes sub-agent.
- Dedicated worktree: Yes.
- Expected PR scope and title: `Add Kubernetes post-deploy smoke checks`.
- Linear status update: Move HEN-21 to `In Progress` at start, comment with branch/worktree and PR link, move to `Done` after merge and verification.

### 7. HEN-12: Enforce Non-Null Parent Relationships for Pets and Visits

- Linear issue: [HEN-12](https://linear.app/henzelmann-demos/issue/HEN-12/enforce-non-null-parent-relationships-for-pets-and-visits)
- Problem summary: The database permits orphan pets and visits even though the domain model treats those parent relationships as required.
- Recommended fix: Make `pets.owner_id` and `visits.pet_id` non-null across H2, MySQL, and Postgres schemas. Align JPA mappings with `@JoinColumn(nullable = false)`.
- Sequencing/dependencies: Do after HEN-10 to avoid mixing seed-data repair with schema constraint changes.
- Definition of done: The database rejects orphan pets and visits, and normal owner/pet/visit flows still work.
- Test expectations: Add repository or integration tests proving null `owner_id` and null `pet_id` direct inserts fail; run normal owner/pet/visit tests.
- Dedicated sub-agent: No.
- Dedicated worktree: Yes.
- Expected PR scope and title: `Enforce required pet and visit parents`.
- Linear status update: Move HEN-12 to `In Progress` at start, comment with branch/worktree and PR link, move to `Done` after merge and verification.

### 8. HEN-13 + HEN-14: Pagination Correctness

- Linear issues: [HEN-13](https://linear.app/henzelmann-demos/issue/HEN-13/validate-pagination-parameters-instead-of-returning-500), [HEN-14](https://linear.app/henzelmann-demos/issue/HEN-14/preserve-owner-search-filters-across-pagination-links)
- Problem summary: Invalid pagination parameters can produce 500s, and owner search pagination drops the active `lastName` filter.
- Recommended fix: Validate or clamp invalid page values consistently for owners and vets. Add the active `lastName` to owner pagination links while keeping unfiltered URLs clean.
- Sequencing/dependencies: No dependencies.
- Definition of done: Owners and vets handle malformed page values without 500s; filtered owner pagination preserves the search context.
- Test expectations: Add MockMvc/template coverage for `page=0`, negative values, non-numeric values, filtered owner pagination links, and unfiltered pagination links.
- Dedicated sub-agent: No.
- Dedicated worktree: Yes, one worktree for both issues.
- Expected PR scope and title: `Harden owner and vet pagination`.
- Linear status update: Move HEN-13 and HEN-14 to `In Progress` at start, comment with branch/worktree and PR link on both issues, move both to `Done` after merge and verification.

### 9. HEN-15 + HEN-16: Public Error Behavior

- Linear issues: [HEN-15](https://linear.app/henzelmann-demos/issue/HEN-15/return-404-for-missing-owners-and-pets), [HEN-16](https://linear.app/henzelmann-demos/issue/HEN-16/avoid-exposing-exception-messages-on-the-public-error-page)
- Problem summary: Missing owners/pets throw internal errors, and the public HTML error page can render exception messages.
- Recommended fix: Return `404 Not Found` for missing owner/pet/visit routes. Remove exception message rendering from the public HTML error page or gate detailed messages behind explicit dev-only behavior.
- Sequencing/dependencies: No hard dependencies; complete before broader error-page visual/test cleanup.
- Definition of done: Missing owner, pet, and visit paths return 404, and public HTML errors do not expose exception details by default.
- Test expectations: Add MockMvc/integration coverage for missing owner, missing pet under valid owner, missing visit paths, and generic HTML error output without exception details.
- Dedicated sub-agent: No.
- Dedicated worktree: Yes, one worktree for both issues.
- Expected PR scope and title: `Return safe public error responses`.
- Linear status update: Move HEN-15 and HEN-16 to `In Progress` at start, comment with branch/worktree and PR link on both issues, move both to `Done` after merge and verification.

### 10. HEN-11: Avoid Loading Visits for Every Owner Search Result

- Linear issue: [HEN-11](https://linear.app/henzelmann-demos/issue/HEN-11/avoid-loading-visits-for-every-owner-search-result)
- Problem summary: Owner search eagerly loads pets and visits even though the owner list only needs owner fields and pet names.
- Recommended fix: Make owner/pet associations lazy by default and add targeted entity graphs or fetch joins for owner detail and visit form flows.
- Sequencing/dependencies: Do after HEN-12 because both change JPA mappings.
- Definition of done: Owner list pages avoid loading visits, and detail/form pages still render needed pets and visits with `spring.jpa.open-in-view=false`.
- Test expectations: Add query-count or fetch-behavior tests for `/owners?lastName=` and `/owners/{id}`.
- Dedicated sub-agent: Recommended, use an ORM/performance sub-agent.
- Dedicated worktree: Yes.
- Expected PR scope and title: `Avoid eager visit loading on owner search`.
- Linear status update: Move HEN-11 to `In Progress` at start, comment with branch/worktree and PR link, move to `Done` after merge and verification.

### 11. HEN-19: Pin Maven and Gradle Wrapper Distribution Checksums

- Linear issue: [HEN-19](https://linear.app/henzelmann-demos/issue/HEN-19/pin-maven-and-gradle-wrapper-distribution-checksums)
- Problem summary: Maven and Gradle wrappers download build tooling without repo-pinned distribution checksums.
- Recommended fix: Add checksum fields to Maven and Gradle wrapper properties and document the wrapper update process.
- Sequencing/dependencies: No dependencies.
- Definition of done: Fresh wrapper downloads validate against pinned checksums and wrapper updates are auditable.
- Test expectations: Clear wrapper caches and run `./mvnw -v` and `./gradlew -v`; add a lightweight CI check for checksum fields.
- Dedicated sub-agent: No.
- Dedicated worktree: Yes.
- Expected PR scope and title: `Pin build wrapper checksums`.
- Linear status update: Move HEN-19 to `In Progress` at start, comment with branch/worktree and PR link, move to `Done` after merge and verification.

### 12. HEN-20 + HEN-22: CI Java/Tooling Baseline

- Linear issues: [HEN-20](https://linear.app/henzelmann-demos/issue/HEN-20/standardize-java-versions-across-dev-and-ci-environments), [HEN-22](https://linear.app/henzelmann-demos/issue/HEN-22/fix-gradle-ci-dependency-cache-configuration)
- Problem summary: Development and CI environments advertise different Java versions, and the Gradle workflow configures Maven caching.
- Recommended fix: Use Java 17 as the documented baseline unless implementation discovers a stronger requirement for Java 21. Align devcontainer, Gitpod, CI, README, and build-tool enforcement with that policy. Remove Maven caching from the Gradle workflow and rely on Gradle caching.
- Sequencing/dependencies: Can follow HEN-19 but does not require it.
- Definition of done: Maven and Gradle CI run under the documented JDK, local environment docs match CI, and Gradle CI uses Gradle caching only.
- Test expectations: Run or validate both CI workflows; run `./mvnw verify` and `./gradlew build` under the documented JDK.
- Dedicated sub-agent: No.
- Dedicated worktree: Yes, one worktree for both issues.
- Expected PR scope and title: `Align Java baseline and Gradle CI cache`.
- Linear status update: Move HEN-20 and HEN-22 to `In Progress` at start, comment with branch/worktree and PR link on both issues, move both to `Done` after merge and verification.

### 13. HEN-17: Align Maven and Gradle Build Artifacts

- Linear issue: [HEN-17](https://linear.app/henzelmann-demos/issue/HEN-17/align-maven-and-gradle-build-artifacts)
- Problem summary: Maven and Gradle builds do not produce equivalent metadata, coverage, and reporting artifacts.
- Recommended fix: Add Gradle equivalents for Maven build-info, git properties, JaCoCo, and artifact checks unless implementation chooses and documents Maven as the only canonical release build.
- Sequencing/dependencies: Do after HEN-20 so build-tool changes follow the chosen Java baseline.
- Definition of done: Maven and Gradle builds produce the intended metadata/report artifacts and CI asserts their presence.
- Test expectations: Compare packaged jars and reports for `META-INF/build-info.properties`, `git.properties`, SBOM output, and coverage output.
- Dedicated sub-agent: Recommended, use a build-tooling sub-agent.
- Dedicated worktree: Yes.
- Expected PR scope and title: `Align Maven and Gradle build outputs`.
- Linear status update: Move HEN-17 to `In Progress` at start, comment with branch/worktree and PR link, move to `Done` after merge and verification.

### 14. HEN-18: Replace Maven-Only Deprecated LibSass CSS Pipeline

- Linear issue: [HEN-18](https://linear.app/henzelmann-demos/issue/HEN-18/replace-the-maven-only-deprecated-libsass-css-pipeline)
- Problem summary: CSS regeneration depends on Maven-only LibSass, while the repository supports both Maven and Gradle.
- Recommended fix: Move CSS generation to maintained Sass tooling and expose equivalent Maven/Gradle commands, or explicitly document one canonical checked-in CSS regeneration command.
- Sequencing/dependencies: Do after HEN-17 if Gradle build artifact alignment changes build conventions.
- Definition of done: CSS regenerates reproducibly with maintained tooling, and the generated `petclinic.css` diff is understood.
- Test expectations: Regenerate CSS, inspect the diff, run MVC/template smoke tests, and run a visual smoke test for the main pages.
- Dedicated sub-agent: Recommended, use a frontend/build sub-agent.
- Dedicated worktree: Yes.
- Expected PR scope and title: `Replace deprecated LibSass CSS pipeline`.
- Linear status update: Move HEN-18 to `In Progress` at start, comment with branch/worktree and PR link, move to `Done` after merge and verification.

### 15. HEN-23: Guard Owner Details Flash-Message Script Against Missing Elements

- Linear issue: [HEN-23](https://linear.app/henzelmann-demos/issue/HEN-23/guard-owner-details-flash-message-script-against-missing-elements)
- Problem summary: The owner details script unconditionally hides success and error elements that are conditionally rendered.
- Recommended fix: Null-check each flash-message element before hiding it, or render the script only when at least one flash message exists.
- Sequencing/dependencies: No dependencies; low-risk tail item.
- Definition of done: Owner details pages do not throw console errors when no flash message or only one flash message is present.
- Test expectations: Add a view or browser test for no flash, success-only flash, and error-only flash states.
- Dedicated sub-agent: No.
- Dedicated worktree: Optional but recommended for Goal-mode isolation.
- Expected PR scope and title: `Guard owner flash message script`.
- Linear status update: Move HEN-23 to `In Progress` at start, comment with branch/worktree and PR link, move to `Done` after merge and verification.

## Goal Mode Execution Notes

- Create one worktree per PR-sized work item, using the Linear branch name where practical.
- Use dedicated sub-agents only for items marked as recommended or required above.
- Keep PRs scoped to the listed issue or issue group; do not add opportunistic refactors.
- Do not group beyond HEN-13/HEN-14, HEN-15/HEN-16, and HEN-20/HEN-22 unless Linear is updated with an explicit scope decision.
- Before implementation, refresh the Linear issue and current repo state because this document is a queue, not the final source of truth.
- After opening a PR, add the PR link to every included Linear issue.
- After merge, verify the definition of done, update Linear to `Done`, and leave a final comment summarizing tests and any follow-up issues.

## Assumptions

- This queue includes all open Pet Clinic backlog issues found in Linear, HEN-5 through HEN-23.
- The document is optimized as an execution queue, not a full implementation playbook.
- Grouped PRs are limited to issues with shared files and review context.
- Java 17 is the default policy for HEN-20 unless implementation discovers a repository requirement for Java 21.
