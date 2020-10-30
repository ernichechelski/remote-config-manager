<img src="https://badgen.net/badge/Based on/Github Actions/gray?icon=github"><img src="https://badgen.net/badge/Based on/Spring Boot, JUnit/yellow?icon=github"><img src="https://badgen.net/badge/Based on/Firebase Remote Config/red?icon=github">

# Remote Config Manager

This project allows to easly control any changes in Firebase Remote Config by using its API and moving any editions to the Github flow. It's just idea, that I hope can be easy to use and extend by new features.

## How to setup entire flow? 🤔

You must have:

- Knowledge about basic GitHub flow (pull requests, pushing contents to the main branch etc). 📒 🌊
- Firebase project with Remote Config enabled (https://console.firebase.google.com) 🔥
- `serviceAccountKey.json` file which can be generated using console of the firebase (can be found at `https://console.firebase.google.com/u/0/project/<projectid>/settings/serviceaccounts/adminsdk`). 📁

> **☣️ This script will override any current Remote Config properties!**


Just fork/copy contents of this repository and set two secret environment variables in Github web client.

- `PROJECT_ID` with firebase project id in raw string format.

- `KEYS` with base64 encoded contents of `serviceAccountKey.json` (format of this file you can find in `secrets.json.example` file).

To trigger uploading any changes locally, create file `secrets.properties` and paste all contents of `serviceAccountKey.json` there and create `projectId.properties` file and paste project id here, and just run command:

`./gradlew test --tests PushMasterTests`

Script will try to read environment variables `PROJECT_ID` and `KEYS` as well, but also will try to read necessary values from above files.

## Usage

**TLDR; Every push to the main branch triggers unit tests which uploads changes to the remote config**

Modify file `Config.java` and create pull request (to trigger configuration initial validation) or just push to the main branch (to publish configuration to the Firebase). 

> Entire security is based on Github flows. Just prevent direct pushes to the main branch by requiring code reviews! 👮‍♂️

You can nest classes as deep as you want, however on the first level make sure that every property is wrapped in `ParameterContainer` (to constrain valid encoding to the JSON format), as in below example:

```java
public class Config {

    public ParameterContainer<String> text = new ParameterContainer("Lorem ipsum");

    public ParameterContainer<Theme> theme = new ParameterContainer(new Theme());

    public static class Theme {
        public String colorHex = "#92003B";
        public Integer size = 54;
    }
}
```

> Remember to initialize all propertes here. Otherwise you have to check how the whole solution works 🤪

## Under a hood 🚙

On every pull request gradle runs tests from the `PullRequestTests` class. On every push to the main branch gradle runs tests from the `PushMasterTests`. Of cource you can write any additional logic as you want. 

## Example client
Of course Firebase needs client application to check fully if it works. Simple project can be found here: https://github.com/ernichechelski/remote-config-manager-ios-client-example/

## Why it is Spring Boot app?
I consider a way to expand this project to setup Spring Boot web application which publishes changes on the Remote Config based on some Cron operations :) 
