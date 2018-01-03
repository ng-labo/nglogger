# nglogger
This is personal location logger on Android.

The motivation to make this is to record my location information cheaply and
securely without using external service.

The purpose of periodic logging the location obtained by GPS(or Network)
service process get the device location and post them to a server using CGI.
The logging server accept and log the record by simple shell CGI.

To save battery assumption,
- To reduce a number of use Location device, try to check status for device.
- The record are stored on local device cache file and send then periodically.


## install and run
This is Android device software, but not official package.
A user need to make own's device developer mode, and translate this package into the device by adb.
- build
[Build]->[Build APK]
- install
adb.exe install /path/to/app-debug.apk

## preference
- device name
To identify the device, a user must input device nickname
(but no check to keep uniquely..)
- server url
To log user's location record, a service on user's device call CGI by https://server/path/log.cgi?devicename
CGI sample : res/script/log.cgi

- Trigger size for post
When a waiting records to send come to this indicated size, the service start try to send to a server.
The size to post the records
- Threshold for detecting step
- Threshold for sensing motion
By acceleration sensor, this service try to check device's status for moving.
 https://github.com/google/simple-pedometer
Because of simple method of checking device status,user need to adjust these parameters.

## todo
- to fix abortion on first, no setting start
- add user,password for calling logging cgi

