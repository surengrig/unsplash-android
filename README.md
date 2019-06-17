Android project: Displays latest photos from Unsplash
=====================================================

The goal of the project is to make an application that does make REST requests and image loading without using any 3rd party libs.
The app displays photos in grid layout and uses pagination for continues scrolling. 

Prerequisites
--------------

- Android SDK v28
- Latest Android Build Tools
- Android Support Repository

Getting started
---------------

This project uses the Gradle build system.

1. Download the archive or clone the project
1. In Android Studio / Intellij Idea, choose the "Import non-Android Studio project" or
  "Import Project" option.
1. Browse and select project directory
1. Navigate to app directory in the project and create a keys.propeties file containing `client_id="UNSPLASH_CLIENT_ID"` replacing UNSPLASH_CLIENT_ID with an actual client id. For obtaining the clien_id please refer to [Unsplash Documentation](https://unsplash.com/documentation#creating-a-developer-account).
