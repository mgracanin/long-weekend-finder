# Privacy Policy — Long Weekend: Leave Optimizer

_Last updated: 30 October 2026_

Long Weekend ("the app") is a free, open-source Android app that helps you plan annual leave around public holidays. Its source code is public at <https://github.com/mgracanin/long-weekend-finder>, so you can check everything in this policy yourself.

## Summary

- The app does **not** collect, sell or share any personal data, and has **no servers of its own**.
- There are **no accounts, no analytics, no advertising and no tracking** of any kind.
- Your settings and saved plans stay **on your device**.

## Information stored on your device

The app stores the following locally on your phone, only so it can work:

- **Settings**: default country, annual leave allowance, default number of leave days to invest, first working day of the week, app language, appearance (light, dark or black theme), and whether you have seen the welcome screens.
- **Saved plans**: the breaks you bookmark and which leave dates you have marked as requested.
- **Holiday data**: the public holiday lists the app has downloaded for this year and the next few, so it works without a connection. They contain no personal information, are updated about once a week and are not included in backups.

This information never leaves your device through the app. If Android device backup is turned on, Android may include it in your own Google account backup, which is covered by Google's privacy policy. Uninstalling the app or clearing its data deletes it.

## Network requests

To show public holidays, the app downloads holiday calendars from the free public **Nager.Date API** (`https://date.nager.at`). Each request contains only a **country code and a year** (for example `2026/HR`). No personal data and no device identifiers are sent. As with any internet request, the Nager.Date server can see your IP address; see <https://date.nager.at> for how that service handles requests.

If the service cannot be reached, the app uses calendars built into the app where it has one for your country.

## Actions you start

- **Calendar** opens a pre-filled Google Calendar event in your browser or calendar app.
- **Copy dates** copies your leave dates to the clipboard.
- **About** links open GitHub or the Nager.Date website in your browser.

These happen only when you tap them. Anything you then do in those apps or sites is covered by their own privacy policies.

## Permissions

The app only requests the **Internet** permission, used for the holiday calendar requests described above.

## Children

The app is a general productivity tool, is not directed at children under 13, and does not knowingly collect information from anyone.

## Changes

Changes to this policy are published in this file in the public repository, with a new date. The full history is visible in the repository's commit log.

## Contact

Questions or concerns: open an issue at <https://github.com/mgracanin/long-weekend-finder/issues>.
