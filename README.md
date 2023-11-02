# Android_Engineer_Coding_Test_2023
This is a submission for the Upfeat's Android Engineer Coding test, for the Android Engineer position.

- Steps to build and run the app:
1) Open Android Studio
2) Open the project on Android Studio
3) Choose emulator/device and click on run
, PLEASE NOTE : 
- The snapshots are saved in Internal Storage/Pictures/UpfeatTest folder, otherwise in Internal Storage/UpfeatTest/, depending on the device's OS version.
- Custom unique colors are assigned to each unique category upon each app session, since the categories cannot be known beforehand, an algorithm ensures each category's object's bounding box color is unique for every new session.
4) The functionnalities are implemented as per their description from the test, so use them as described.
- Any assumptions made:
  1) There is suitable conditions for image classification, like optimal lighting, camera quality.
  2) Minimum SDK is 21.
- Challenges faced and how they were addressed:
  There were no notable challenges faced, other than : 
  Ensuring that there is no significant lag, which was surpassed by  :
   1) Using data types like LiveData.
   2) Using Coroutines.
   3) Ensuring the use of minimal approach to achieve the wanted functionality.
