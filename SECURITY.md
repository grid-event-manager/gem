# Security Policy

## Supported Versions

GEM is alpha software. Security reports should target the latest public release only unless the issue clearly affects retained source on `main`.

## Reporting a Vulnerability

Do not put secrets, account names, passwords, session identifiers, inventory identifiers, local machine paths, or live-grid evidence into a public issue.

When the GitHub repository is created, enable GitHub private vulnerability reporting. Until that route is available, open a minimal public issue that says a private security contact is needed and include no sensitive details.

## Local Credentials

GEM stores account credentials locally through the platform vault route. Reports about credential storage should include:

- operating system and version;
- GEM version;
- whether this is a clean install, upgrade, or reinstall;
- exact visible behaviour;
- no real passwords or raw vault files.

## Disclosure

ANVLL will triage reports before publishing details. Security fixes may be released before public discussion.
