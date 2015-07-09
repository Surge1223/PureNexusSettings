# PureNexusSettings
This is an attempt at creating a "standalone" settings app for PureNexus

This was done using android studio as the source bits for some of the support packages weren't quite where they needed to be given some of the things that were already added in from a previous tinkering project. 
As such - I used a modified android.jar to make everything happy. It basically includes the "hidden" api bits as well as some things included in PureNexus framework.jar.
Here is a link to a zip w/ an augmented android.jar file for sdk 22 that allows android studio to build w/ such things in place. Just back up the existing ones you got when DLing the sdk and drop this in its place (renamed to android.jar of course):

<a href=https://www.dropbox.com/s/xc1womlca5xhkur/Cheekyandroidjar.zip?dl=0>Link</a>

NOTE: In order to function properly - the end result of this needs to be dropped into /system - likely best in priv-app as well
