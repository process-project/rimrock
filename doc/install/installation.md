# Overview

Rimrock installation consists of setting up the following components:

1. Packages / Dependencies
1. Java
1. System user account
1. Database configuration
1. Grid certificates configuration
1. Rimrock application
1. Logrotate
1. Nginx
1. New Relic

## 1. Packages / Dependencies

Install the required packages:

```
sudo apt-get update

sudo apt-get install -y wget
```

## 2. Java

```
su -
export OS_VERSION = change me
echo "deb http://ppa.launchpad.net/webupd8team/java/ubuntu $OS_VERSION main" | tee /etc/apt/sources.list.d/webupd8team-java.list
echo "deb-src http://ppa.launchpad.net/webupd8team/java/ubuntu $OS_VERSION main" | tee -a /etc/apt/sources.list.d/webupd8team-java.list
apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys EEA14886
apt-get update
apt-get install oracle-java8-installer
exit
```
See [Web upd8](http://www.webupd8.org/2014/03/how-to-install-oracle-java-8-in-debian.html) webpage for details.

## 3. System user account

Create a 'rimrock' user for Rimrock:

```
sudo adduser --gecos 'Rimrock' rimrock
```

## 4. Database configuration

Install PostgreSQL database:

```
# Install the database packages
sudo apt-get install -y postgresql-9.3 postgresql-client libpq-dev

# Log in to PostgreSQL
sudo -u postgres psql -d template1

# Create a user for Rimrock
template1=# CREATE USER rimrock CREATEDB;

# Create the Rimrock production database & grant all privileges to user rimrock
template1=# CREATE DATABASE rimrock_production OWNER rimrock;

# Quit the database session
template1=# \q

# Try connecting to the new database with the new user
sudo -u rimrock -H psql -d rimrock_production
```

## 5. Grid CA certificates installation

Rimrock communicates with infrastructure using secured connections. In order to verify a remote server it is necessary to have CA Certificate of CA responsible for server's certificate. This can be done by installing "EGI IGTF" CA package, which can be found here: https://wiki.egi.eu/wiki/EGI_IGTF_Release.
Installation method depends on the operating system, in case of Ubuntu it is necessary to follow these steps: https://wiki.egi.eu/wiki/EGI_IGTF_Release#Using_the_distribution_on_a_Debian_or_Debian-derived_platform

## 6. Rimrock application

Get Rimrock code and its configuration:

```
# We'll install Rimrock into the home directory of the user "rimrock"
cd /home/rimrock

# Create the Rimrock home directory
sudo -u rimrock -H mkdir rimrock

# Download Rimrock app packages into single jar
export RIMROCK_VERSION=change_me
wget http://dev.cyfronet.pl/mvnrepo/pl/cyfronet/rimrock/${RIMROCK_VERSION}/rimrock-${RIMROCK_VERSION}.jar -O /home/rimrock/rimrock/rimrock.jar

# Download Rimrock configuration template and customize it
# FIXME
```

Create upstart scripts:

```
sudo -u rimrock -H mkdir -p /home/rimrock/.config/upstart
sudo -u rimrock -H wget --no-check-certificate https://gitlab.dev.cyfronet.pl/plgrid-core-4-1/rimrock/raw/master/install/doc/support/upstart/rimrock.conf -O /home/rimrock/.config/upstart/rimrock.conf
```

As a result Rimrock can be started/stopped/restarted using following commands:

```
sudo -u rimrock -H initctl start rimrock
sudo -u rimrock -H initctl stop rimrock
sudo -u rimrock -H initctl restart rimrock
```

## 7. Logrotate

```
sudo wget wget --no-check-certificate https://gitlab.dev.cyfronet.pl/plgrid-core-4-1/rimrock/raw/master/install/doc/support/logrotate/rimrock -O /etc/logrotate.d/rimrock
```

## 8. Nginx

```
# Install nginx
sudo apt-get install -y nginx-light

# Download Rimrock nginx configuration file
sudo wget --no-check-certificate https://gitlab.dev.cyfronet.pl/plgrid-core-4-1/rimrock/raw/master/install/doc/support/nginx/rimrock -O /etc/nginx/sites-available/rimrock

# customize nginx configuration file
sudo editor /etc/nginx/sites-available/rimrock

# ...enable it...
sudo ln -s /etc/nginx/sites-available/rimrock /etc/nginx/sites-enabled/rimrock

# ...and restart nginx
sudo service nginx restart
```

## 9. New Relic

Go to [New Relic](http://newrelic.com) and create configuration for new Java application. Download prepared zip file into `/home/rimrock/rimrock' and unzip it:

```
cd /home/rimrock/rimrock
sudo -u rimrock -H newrelic-java-3.11.0.zip
sudo -u rimrock -H rm newrelic-java-3.11.0.zip
```

Next customize application New Relic configuration:

```
sudo -u rimrock -H edit /home/rimrock/rimrock/newrelic/newrelic.yml
```

The most important think to customize is the location of the new relic agent logs. Please change it into:

```
log_file_path: /home/rimrock/rimrock/log
```

Last think is to modify upstart script and enable New Relict by commenting/uncommenting lines according to the instruction
available inside upstart configuration file:

```
sudo -u rimrock -H edit /home/rimrock/.init/rimrock.conf
```
