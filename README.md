# opentok-android-audio-recorder

### Compiling the application
1. Edit `OpentokConfig.kt` according to your API Key, Session ID and Token of the Opentok session.
2. Compile and run the app on your Android phone.


### How it works
1. This application uses a custom audio driver to power the capture (microphone input) and render (speaker output).
2. On receiving data from the microphone, before sending to Opentok as a publisher stream, a copy of it is written into a capturer pipe stream.
3. Similarly, on receiving data from Opentok subscriber stream, before writing to the speaker, a copy of it is written into a renderer pipe stream.
4. A separate thread runs in the background reading from both pipe streams (byte array), mixing the both into a single byte array, and then write into a file on the Android file system.
5. The file written is in raw PCM format (44.1kHz sampling rate, 1 channel, 16 bits, little endian byte order).
6. The default directory which the file will be written is `sdcard/Android/data/com.nexmo.audiorecorder/files`.
