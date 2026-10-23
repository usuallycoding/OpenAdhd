# Spec Delta

## MODIFIED Requirements

### Requirement: Android dynamic color
On Android 12 or newer the app SHALL use the system's dynamic color scheme, in the light or dark variant resolved from the theme mode. On all other platforms and Android versions it MUST use the app's own color schemes.

#### Scenario: Android 12+
- **WHEN** the app runs on Android 12 or newer
- **THEN** colors come from the system dynamic scheme

#### Scenario: Other platforms
- **WHEN** the app runs on desktop, iOS, or Android older than 12
- **THEN** colors come from the app's light or dark scheme
