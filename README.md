# Visage
An avatar service based on a master/slave system.

# A hosted Visage server is now available at https://visage.gameminers.com/ 

## Why did you split this off from Lapis?
I'm not directly involved with Sponge or Lapis anymore, and the 2.0.0 rewrite was turning out to be all my
code. Nobody in Lapis really knows GL except me (Aesen), and as such all the core code was going to be mine.

Since this project is ultimately mine anyway, and I'm going to put it on my hardware, I figured a rename and
move was in order. The project is still open source, still MIT licensed, and contributions are still very
welcome. I'm just not using the Lapis name anymore.

## Compiling
`./gradlew`. Only Linux is confirmed to work. You can try your luck on Windows.

## Benchmarking
Visage includes a simple benchmark to help see how well it will work on a given system. You can invoke it by
running `java -jar Visage.jar --benchmark`.

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
Run `java -jar Visage.jar --master`. It will pick up the `master.conf` in the `conf` directory and use it.  
If you need to use a different config file, use the `--config` option.

### Slave
Visage systems can optionally have a (theoretically) infinite number of slaves. A good example of a slave is a computer
that has a GPU, but is on an unreliable Internet connection. Slaves can be run with or without hardware acceleration.

#### Configuration
Edit the `conf/slave.conf` file. It is self-documenting.

#### Running
Run `java -jar Visage.jar`. Slave mode is the default. It will pick up the `slave.conf` in the `conf` directory and use it.  
If you need to use a different config file, use the `--config` option.