# Lapitar2
A rewrite of Lapitar based on a master/slave system.

## Compiling
`./gradlew`. Only Linux is confirmed to work. You can try your luck on Windows

## Usage

### Prerequisites

 - RabbitMQ

### Master
All Lapitar systems need a Master instance. This handles all frontend requests and manages the delegation of jobs, and
also manages the cache.

A Master can also offer a fallback unaccelerated slave for when no other slaves are available. As you can guess, this
fallback is slow and undesirable, but helps ensure high uptime.

#### Configuration
Edit the `master.conf` file. It is self-documenting.

#### Running
Run `java -jar Lapitar2.jar --master`. It will pick up the `master.conf` in the current directory and use it.

### Slave
Lapitar systems can optionally have a (theoretically) infinite number of Slaves. A good example of a Slave is a computer
that has a GPU, but is on an unreliable internet connection. Slaves can be run with or without hardware acceleration.

#### Configuration
Edit the `slave.conf` file. It is self-documenting.

#### Running
Run `java -jar Lapitar2.jar`. Slave mode is the default. It will pick up the `slave.conf` in the current directory and use it.