# FMD Android

Find your device via SMS, instant messengers, or a web interface
([FMD Server](https://gitlab.com/fmd-foss/fmd-server)).

FMD allows you to locate, ring, wipe and issue other commands to your device when it's lost.
It aims to be a secure open source alternative to Google's Find My Device.

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/de.nulide.findmydevice/)
[<img src="https://raw.githubusercontent.com/ImranR98/Obtainium/refs/heads/main/assets/graphics/badge_obtainium.png"
     alt="Get it on Obtainium"
     height="80">](https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22de.nulide.findmydevice%22%2C%22url%22%3A%22https%3A%2F%2Fpackages.fmd-foss.org%2Ffdroid%2Frepo%22%2C%22author%22%3A%22FMD%20Team%22%2C%22name%22%3A%22FMD%22%2C%22additionalSettings%22%3A%22%7B%5C%22appIdOrName%5C%22%3A%5C%22de.nulide.findmydevice%5C%22%2C%5C%22pickHighestVersionCode%5C%22%3Afalse%2C%5C%22trySelectingSuggestedVersionCode%5C%22%3Atrue%2C%5C%22appName%5C%22%3A%5C%22FMD%5C%22%2C%5C%22allowInsecure%5C%22%3Afalse%2C%5C%22allowedSigningCertHashes%5C%22%3A%5C%2294f404bc4b9dcd2c7df4ce42000e8ecdd4cb6490e04ef7d76ca9967e6182ee71%5C%22%7D%22%2C%22overrideSource%22%3A%22FDroidRepo%22%7D)

## Why?

I lost my brand new phone last week.
If I would have used Google Services I would have had a better chance of finding it.
Why has nobody created an open source application to find a lost device without giving up your privacy?

If you like FMD and want to support it, spread the word or donate.

## Documentation

The documentation can be found [on the website](https://fmd-foss.org/docs/overview).

## Contribute

For translations instructions and other ways to contribute,
see the [contribution guide](https://fmd-foss.org/docs/contribute/overview).

## Building

Building FMD Android requires Java and the Android SDK.
The easiest way is to open the repository in [Android Studio](https://developer.android.com/studio) and click "Run".

Alternatively, build it from the command line:

```sh
./gradlew assembleDevDebug
```

The resulting APK should be at `app/build/outputs/apk/dev/debug/app-dev-debug.apk`.

This will build the "dev" flavour of FMD Android, which can be installed in parallel with the prod app.
The command keyword is "fmdev" (for example, "fmdev locate").

## Community

There is a matrix space for discussions: https://matrix.to/#/#fmd:matrix.org.

You can join the "Community" room for general discussions and the "Announcement" room to get project news.

## Donate

<script src="https://liberapay.com/FMD/widgets/button.js"></script>

<noscript><a href="https://liberapay.com/FMD/donate"><img alt="Donate using Liberapay" src="https://liberapay.com/assets/widgets/donate.svg"></a></noscript>

<a href='https://ko-fi.com/H2H35JLOY' target='_blank'><img height='36' style='border:0px;height:36px;' src='https://cdn.ko-fi.com/cdn/kofi4.png?v=2' border='0' alt='Buy Me a Coffee at ko-fi.com' /></a>

## Funding

<div style="display: inline-flex; align-items: center;">
    <a href="https://nlnet.nl/" target="_blank">
        <img src="https://nlnet.nl/logo/banner.svg" alt="nlnet" height="50">
    </a>
    <a href="https://nlnet.nl/taler" target="_blank">
        <img src="https://nlnet.nl/image/logos/NGI_Mobifree_tag.svg" alt="NextGenerationInternet" height="50">
    </a>
</div>

This project was funded through the NGI Mobifree Fund.
For more details, visit our [project page](https://nlnet.nl/project/FMD/).

## Why is it taking that long to fix x?

I am working full-time.
This project is my free-time and I have spent a lot of time in this and still will.
If there are bugs I will try to fix them as soon as possible, but my full-time job has priority.
If it takes too long to fix something you can of course try to fix it by yourself and contribute to this project.

Thanks,

Nulide

## License

FMD Android is published under [GPLv3-or-later](LICENSE).
