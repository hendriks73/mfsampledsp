README.txt
==========

MFSampledSP is a better-than-nothing implementation of the javax.sound.sampled
service provider interfaces based on Microsoft's Media Foundation API.

Its main purpose is to decode audio files or streams to signed linear pcm.
It's part of the SampledSP collection of libraries.

This library comes with absolutely no support, warranty etc. you name it.

Build
=====

You can only build this library on Windows 7 or later.

 To do so, you also need:

    * git
    * Maven 3.0.5, http://maven.apache.org/
    * Windows SDK 7.1, http://www.microsoft.com/en-us/download/details.aspx?id=8279
    * a JDK (to run Maven and get the JNI headers)

Once you have all this set up, clone the repository like this:

git clone git@github.com:hendriks73/mfsampledsp.git mfsampledsp

Then you still need to adjust some properties in the parent pom.xml.
Or.. simply override them using -Dname=value notation. E.g. to point to your
JDK's JNI headers, add

-Dwin32.headers.jni=C:\jdk1.8.0_31\include\

to your mvn call.

You might also need to change win32.sdk, if you don't have your Windows SDK
installed at C:\Program Files\Microsoft SDKs\Windows\v7.1

So all in all, something like the following might work for you:

mvn -Dwin32.headers.jni=C:\jdk1.8.0_31\include\ \
    "-Dwin32.sdk=C:\Program Files\Microsoft SDKs\Windows\v7.1" \
    clean install

Note that if you have a space character in your SDK path, you need to quote the <entire>
"-Dname=value" parameter, not just the value part.


Have fun,

-hendrik
hs@tagtraum.com