# Long Weekend — Leave Optimizer

Free, open-source Android app that shows where a few days of annual leave turn into long breaks around public holidays — for **204 countries**, in **English and Croatian**.

## Features

- **Discover** — pick a country, year (±10 years) and how many leave days to invest; the Opportunity Radar and Top picks show the best bridges at a glance.
- **Suggested breaks** — every useful bridge ("take 3, get 9"), sortable by longest break, fewest leave days or date, with a day-by-day strip.
- **Plan details** — month calendars, the exact dates to request from HR (with a checklist), copy-to-clipboard and Google Calendar export.
- **Holidays** — a year-at-a-glance calendar and list of all public holidays, national and regional.
- **Saved plans** — bookmarked breaks with request progress, kept on the device.
- **Profile** — default country, yearly allowance, default leave to invest, app language, and About.
- **Welcome screens** on first start, with a quick setup.
- **Works offline** — built-in calendars for 10 countries (HR, GB, DE, FR, IT, ES, US, CA, AU, IE) when the holiday API can't be reached; other countries need a connection.
- **Private** — no accounts, ads, analytics or tracking. See [PRIVACY.md](PRIVACY.md).

## How it works

Holiday data comes from the free [Nager.Date](https://date.nager.at) public holidays API. For each holiday the optimizer tries every surrounding window, keeps breaks that start and end next to working days, need at least one leave day within your budget and include a holiday on a weekday, then drops any break that an overlapping one beats on both leave cost and length.

## Tech

Kotlin · Jetpack Compose (Material 3) · MVVM with StateFlow · Retrofit + Gson · `minSdk 26`, `targetSdk 36` · R8-minified release builds.

```
app/src/main/java/com/longweekend/optimizer/
  model/        data classes, supported-country list
  optimizer/    HolidayBridgeOptimizer — the bridge-finding algorithm
  repository/   holiday API + cache + offline calendars, settings, saved plans
  viewmodel/    MainViewModel
  ui/           Compose screens, components and text formatting
```

## Building

Requires the Android SDK (platform 36) and JDK 17+. The easiest way is to open the project in Android Studio, which creates `local.properties` with your SDK path. From the command line, create it yourself:

```properties
sdk.dir=/path/to/Android/Sdk
```

Then:

```bash
./gradlew testDebugUnitTest   # unit tests
./gradlew assembleDebug       # debug APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug        # build and install on a connected device or emulator
```

### Release builds

Release builds are minified with R8 and signed with a key read from `keystore.properties` in the project root (git-ignored, never commit it):

```properties
storeFile=upload-keystore.jks
storePassword=...
keyAlias=upload
keyPassword=...
```

```bash
./gradlew bundleRelease       # app/build/outputs/bundle/release/app-release.aab
```

To test a release build locally without your own key, add `-PsignReleaseWithDebug` (debug-signed; not accepted by Google Play). If you publish a fork, change `applicationId` in `app/build.gradle.kts` and the links in `AppLinks.kt`.

## Translations

UI text lives in `app/src/main/res/values/strings.xml` (English) and `values-hr/strings.xml` (Croatian). To add a language, copy `values/strings.xml` to `values-<code>/strings.xml`, translate it, and add the option to `LANGUAGES` in `ProfileScreen.kt`. Pull requests welcome.

## Contributing

Issues and pull requests are welcome. Please run the unit tests before submitting.

## Licence

[MIT](LICENSE). Holiday data © [Nager.Date](https://date.nager.at) (MIT).
