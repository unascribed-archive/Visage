# Visage
Visage is a new open source Minecraft avatar service, inspired by, and made by the same person as,
[Lapitar](https://github.com/LapisBlue/Lapitar). It provides both 2D and 3D avatars with an
extremely simple API.

## Overview
Visage is written in Java 7, and uses Jetty and LWJGL2 as backends for HTTP and rendering, respectively. It also
requires a RabbitMQ server for job distribution, and a Redis server for caching. It is based on OpenGL1.2, and
uses VBOs to speed up rendering.

Visage is tested on Ubuntu 14.10 with the Mesa open-source drivers. Proprietary drivers are not supported, but
will probably work.
**Windows is completely unsupported. Use it at your own peril. Issues reported that are specific to Windows over a certain complexity are likely to be ignored.**

## For Website Designers
There is an [official hosted Visage site][1] with full API documentation.
This source repository is for developers looking to help out and sysadmins looking to run their own Visage servers.

## For Users
If you want an avatar to use on other people's sites, you can go over to the [official Visage site][1] and
use the Quick Render feature to generate a maximum-resolution render of your skin and download it to your
computer.

## For Sysadmins
A Visage master can be run rather simply, but first you need RabbitMQ and Redis. For Debian/Ubuntu, this is a
simple `sudo apt install rabbitmq-server redis-server` away. *The redis-server package in Wheezy is too old. Install it from backports.*
The RabbitMQ server should be accessible by any machines you plan on running as slaves, but the Redis server 
can be fully locked down.

Please note that the default `conf/master.conf` file is set up for development use, and has some rather low TTLs and
includes a lot of information in the headers. You should open the file and tweak the parameters to your particular
use-case.

You can download a ready-to-run Visage distribution at [the GitHub releases page](https://github.com/surgeplay/Visage/releases).
Extract it wherever, `cd` to the directory, and run `./bin/Visage --master`. **Windows is unsupported. Use it at your own peril.**

Slaves just need to be pointed at your RabbitMQ server, which brings us to...

## For Hardware Donors
A Visage slave can be connected to an existing Visage master by simply editing the `conf/slave.conf` file to point to the
RabbitMQ server the sysadmin gives you.

You can download a ready-to-run Visage distribution at [the GitHub releases page](https://github.com/surgeplay/Visage/releases).
Extract it wherever, `cd` to the directory, and run `./bin/Visage`. Slave mode is the default.
**Windows is unsupported. Use it at your own peril.**

If you want to get an idea of how well Visage will run on your hardware, you can run `./bin/Visage --benchmark`.
It will run a series of 24 benchmarks with varying render parameters; all of the renders are players with lighting,
shadows, and all secondary layers enabled. It uses a [test skin](https://github.com/surgeplay/Visage/blob/master/src/main/resources/test_skin.png)
that wasn't chosen for any particular reason.
After it finishes, it will give you a number. Here's the results of some benchmarks:

 * Kubuntu 14.10 w/ Radeon R9 290X on the open-source Mesa drivers, running a KDE Plasma 5 desktop under a light load: **3307.7617**
 * Xubuntu 14.10 w/ GeForce GTX 550 Ti on the proprietary drivers, running an Xfce 4.11 desktop under virtually no load: **5688.4517**
 * Xubuntu 14.04 w/ R9 290X on the open-source Mesa drivers, running an Xfce 4.12 under pretty low load: **6806.3027**

## For Developers
Visage is open source, and as such, if you find problems, you can submit Pull Requests or Merge Requests to
fix said problems. The code style is basically Sun conventions with tabs instead of spaces. To compile
Visage, just run `./gradlew distZip`. A distribution zip with all necessary files will be created in
`build/distributions/Visage-X.Y.Z-SNAPSHOT.zip`.

## Wishlist
 * Don't explode into flaming death when running under Xdummy
 * Update to LWJGL3
 * Stop using Pbuffers and use FBOs

[1]: https://visage.surgeplay.com/
