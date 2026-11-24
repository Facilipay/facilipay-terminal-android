# Security

Please report a vulnerability in the Facilipay Terminal SDK, its sample or its guide privately,
through this repository's **Security** tab: **Report a vulnerability**. Don't open a public issue.

We answer within five working days and keep you informed until it is fixed.

## Checking what you download

Every published version of `io.facilipay:terminal-sdk` comes with a build provenance attestation.
To check that a file was built by this repository's release workflow from a release tag:

```bash
gh attestation verify terminal-sdk-1.0.0.aar \
  --repo Facilipay/facilipay-terminal-android \
  --signer-workflow Facilipay/facilipay-terminal-android/.github/workflows/publish.yml \
  --source-ref refs/tags/v1.0.0 \
  --deny-self-hosted-runners
```
