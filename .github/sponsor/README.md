# Sponsorship assets

Payment QR codes (Alipay / WeChat) referenced by the README's *Sponsoring* section.

## Why these live in their own directory

This folder is protected by `.github/CODEOWNERS` (`.github/sponsor/` → `@rui-link`).
Any pull request that adds, replaces, removes or edits a file here **requires the
maintainer's approval to merge**. That is the only control that actually stops someone from
swapping a donation QR code for their own; the file location alone does not, since both the
image and the README link are ordinary files a pull request can edit.

Keep this folder to **exactly** the donation QR codes. Product screenshots belong in
`reference/images/`, where anyone may legitimately update them — mixing the two would force
the protection rule to cover files that need to stay editable.

## Expected files

| File | Purpose |
|---|---|
| `alipay.png` | Alipay collection code |
| `wechat.png` | WeChat collection code |

Replace a QR code by committing a new image **and** updating the pinned commit SHA in the
README link (see below).

## Referencing them without letting a pull request rewrite history

The README references these images through `raw.githubusercontent.com` pinned to a full
40-character commit SHA rather than a branch name:

```markdown
![Alipay](https://raw.githubusercontent.com/rui-link/inkwash/<SHA>/.github/sponsor/alipay.png)
```

A commit SHA is immutable. If someone merges a pull request that swaps the image, the README
keeps rendering the bytes of the original commit. Replacing the QR code is therefore only
possible by deliberately updating the SHA, which is the intent.

Trade-off: after intentionally changing a QR code you **must** update the SHA by hand, or the
README will keep showing the old one forever.

## A note on search engines

These files are public for as long as the repository is public. There is no per-repository
`robots.txt` on GitHub — `robots.txt` applies to `github.com` as a whole and cannot be
extended by a repository owner, and README images are served through `raw.githubusercontent.com`,
which has its own policy. So the images cannot be hidden from crawlers by configuration.

What does help, and is what the README relies on:

- the QR codes sit inside a collapsed `<details>` block at the very bottom of the README, so
  they are not rendered in the default view and are easy to walk past;
- no crawler decodes QR payloads from images — exposure requires a human looking at the page
  or running OCR on it.

Anyone who can see the code can see the QR. Treat the merchant identity behind it as public.