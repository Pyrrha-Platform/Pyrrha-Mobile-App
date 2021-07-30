# Pyrrha mobile app

[![License](https://img.shields.io/badge/License-Apache2-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0) [![Slack](https://img.shields.io/static/v1?label=Slack&message=%23prometeo-pyrrha&color=blue)](https://callforcode.org/slack)

This repository contains the [Pyrrha](https://github.com/Pyrrha-Platform/Pyrrha) solution mobile application that targets Samsung smartphones paired with the [sensor device](https://github.com/Pyrrha-Platform/Pyrrha-Firmware) and Samsung [watch](https://github.com/Pyrrha-Platform/Pyrrha-Watch-App) carried by the firefighters.

## Features

The smartphone includes features that allow it to:

1. Discover and pair with a sensor device using Bluetooth Low Energy.
1. Provide real-time information about device readings.
1. Relay information back to the IBM Cloud.
1. Relay information from the IBM Cloud back to the device and companion smartwatch.

The application is built as a [Java Android Application](https://developer.samsung.com/galaxy-watch-develop/creating-your-first-app/web-companion/setup-sdk.html).

## Setting up the development environment

- [Install Android Studio](https://developer.android.com/studio) and open this clone repo. The initial Gradle build will fail until we create a properties file, which we'll do later on.
- [Review the Samsung-specific setup instructions](https://developer.samsung.com/mobile/galaxy-sdk-getting-started.html). You may need to sign up for a Samsung Developer Account to see documentation.
- [Add the Samsung Accessory SDKs to use the watch as a consumer and smartphone (smart device) as a consumer](https://developer.samsung.com/galaxy-accessory)
- [Add the Samsung device skin for your target device](https://developer.samsung.com/galaxy-emulator-skin/guide.html) and configure it with the AVD (Android Virtual Device) Manager. The application has been tested with the Samsung Galaxy A51 and [Galaxy XCover Pro](https://www.samsung.com/es/business/smartphones/galaxy-xcover-pro-g715/). If you can't find configure the skins, take note of their display and screen resolution specs and configure a generic simulator accordingly.
- Copy `pyrrha.properties.template` to `pyrrha.properties` and provide your App ID and backend service configuration information.

## Run on phone or simulator

1. TBD

## Running on a physical phone

1. TBD

## Continue work and integrate with the Android watch app

- Install Tizen Studio according to the [Pyrrha-Watch-App](https://github.com/Pyrrha-Platform/Pyrrha-Watch-App) documentation.

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct, and the process for submitting Pyrrha pull requests.

## License

This project is licensed under the Apache 2 License - see the [LICENSE](LICENSE) file for details. Refer to the respective licenses in the code for the IBM, Samsung, and Google libraries and APIs.
