# Android I love Free Software Day library

Shows a dialog with informations about the [#ilovefs](https://fsfe.org/campaigns/ilovefs/) day. You can add buttons to rate your app, write you a message and a donation button.

# Contribute

Fork this Lib and do a pull request. I will merge your changes back into the main project.

# Screenshot

![Screenshot](https://raw.github.com/azapps/ilovefs-android/master/screenshot.png)

# How to use
Add the following code to the activity you want to show the dialog:
```java
		ILoveFS ilfs = new ILoveFS(this, "mirakel@azapps.de", Mirakel.APK_NAME);
		if (ilfs.isILFSDay()) {
            /* optional
			ilfs.donateListener = new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					Intent intent = new Intent(MainActivity.this,
							DonationsActivity.class);
					startActivity(intent);
				}
			};
            */
			ilfs.show();
		}
```

Insert your data and it's ready.

# Build Example App with Gradle

Clone this repo in the root directory of your project.

Add the following lines to the end of your `build.gradle` (or add it directly to the dependencies section:
```
dependencies {
    compile project(':ilovefs-android')
}
```

Add the following line to the `settings.gradle`:
```
include 'ilovefs-android'
```


# Add the lib to your eclipse project

Just download the repo, create a new project from the source and add it to the build path (Project settings -> Java Build Path) and as an library (Project settings -> Android -> Library -> Add…)

# License

    Copyright 2014 Anatolij Zelenin <az@azapps.de>

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
