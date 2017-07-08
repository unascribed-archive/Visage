# Visage
Visage is an open source 2D/3D Minecraft avatar rendering service. It's built on
a job distribution system and hardware-accelerated OpenGL, making it extra-fast.

It's written in Java 8, and uses Jetty and LWJGL3 as backends for HTTP and
rendering, respectively. It also requires a RabbitMQ server for job
distribution, and a Redis server for caching. It is based on OpenGL 3.0, and
uses VBOs to speed up rendering.

Visage is tested on Arch Linux with the Mesa RadeonSI open-source drivers. The
offical server uses Debian with Mesa RadeonSI. Proprietary drivers are not
supported, but will probably work. *Windows is completely unsupported. Use it at
your own peril.**

## For Website Designers
There is an [official hosted Visage site][1] with full API documentation.
This source repository is for developers looking to help out and sysadmins
looking to run their own Visage servers.

## For Users
If you want an avatar to use on other people's sites, you can go over to the
[official Visage site][1] and use the Quick Render feature to generate a
maximum-resolution render of your skin and download it to your computer.

## For Sysadmins
A Visage distributor can be run rather simply, but first you need RabbitMQ and
Redis. The RabbitMQ server should be accessible by any machines you plan on
running as renderers, but the Redis server can be fully locked down.

RabbitMQ and Redis are available in most distro's repositories. On Debian/Ubuntu,
you want `rabbitmq-server` and `redis-server`. On Arch Linux, you want `redis`
and `rabbitmq`.

Please note that the default `conf/distributor.conf` file is set up for development
use, and has some rather low TTLs and includes a lot of information in the
headers. You should open the file and tweak the parameters to your particular
use-case.

You can download a ready-to-run Visage distribution at [the GitHub releases page][2].
Extract it wherever, `cd` to the directory, and run `./bin/Visage --server`.
**Windows is unsupported. Use it at your own peril.**

Renderers just need to be pointed at your RabbitMQ server, which brings us to...

## For Hardware Donors
A Visage renderer can be connected to an existing Visage distributor by simply
editing the `conf/renderer.conf` file to point to the RabbitMQ server the
sysadmin gives you.

You can download a ready-to-run Visage distribution at [the GitHub releases page][2].
Extract it wherever, `cd` to the directory, and run `./bin/Visage`.
Renderer mode is the default. **Windows is unsupported. Use it at your own
peril.**

Please note that, for technical reasons, you *must* have a real GPU and X server.
llvmpipe doesn't work, and neither does Xvfb or Xdummy.

## For Developers
Visage is open source, and as such, if you find problems, you can submit a PR
to fix said problems. The code style is basically Sun conventions with tabs
instead of spaces. To compile Visage, just run `./gradlew capsule`. A jar
containing all dependencies will be created in `build/libs/Visage-<VERSION>-capsule.jar`.

[1]: https://visage.surgeplay.com/
[2]: https://github.com/surgeplay/Visage/releases
