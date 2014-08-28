# Requirements

## Supported Operating Systems

- Ubuntu 14.04

It may be possible to install Rimrock on other operating systems. The above list only includes
operating systems, which have **already** been used for Rimrock deployment in production mode. What is more,
some commands used in the installation manual are Debian-specific (e.g. `apt-get`). If your OS uses a different
package management system, you will need to modify these commands appropriately (e.g. by calling `yum` if you are using CentOS).

## Java version

Rimrock requires Oracle Java 8+.

# Hardware requirements

## CPU

**1 core** is the **recommended** minimum number of cores.

## Memory

**2GB** is the **recommended** minimum memory size.

## Storage

The following components must reside in your attached storage:

- Rimrock application packaged into single jar file (30MB)
- Rimrock database (Note: the volume of the database depends on how many jobs will be registered. 
50MB should be enough for standard deployments.)