# Visage
An avatar service based on a master/slave system. **Not finished.**

## Compiling
`./gradlew`. Only Linux is confirmed to work. You can try your luck on Windows.

## Benchmarking
Visage includes an extremely simple benchmark that renders as many skins as possible in 5 seconds and reports
how many skins it was able to render. You can invoke it by running `java -jar Visage2.jar --benchmark`.

## Setup

**Everything in this section is just a basic spec for Aesen's reference.** It is subject to drastic change.

### Prerequisites

 - RabbitMQ

### Master
All Visage systems need a master instance. This handles all frontend requests and manages the delegation of jobs, and
also manages the cache.

A master can also offer a fallback slave for when no other slaves are available. It defaults to software rendering. As
you can guess, this fallback is slow and undesirable, but helps ensure high uptime.

#### Configuration
Edit the `conf/master.conf` file. It is self-documenting.

#### Running
Run `java -jar Visage.jar --master`. It will pick up the `master.conf` in the current directory and use it.

### Slave
Visage systems can optionally have a (theoretically) infinite number of slaves. A good example of a slave is a computer
that has a GPU, but is on an unreliable Internet connection. Slaves can be run with or without hardware acceleration.

#### Configuration
Edit the `conf/slave.conf` file. It is self-documenting.

#### Running
Run `java -jar Visage.jar`. Slave mode is the default. It will pick up the `slave.conf` in the current directory and use it.