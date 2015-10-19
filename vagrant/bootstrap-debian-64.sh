#!/usr/bin/env bash

# set motd
cp /vagrant/vagrant/build.motd.tail /etc/motd.tail

# add repo for R 
apt-key adv --keyserver keyserver.ubuntu.com --recv-keys E084DAB9
echo "deb https://cran.rstudio.com/bin/linux/ubuntu trusty/" >> /etc/apt/sources.list

# bring apt database up to date with R packages
apt-get update

# install R
apt-get install -y r-base r-base-dev

# install minimal packages needed to run bootstrap scripts
apt-get install -y unzip
apt-get install -y git
apt-get install -y g++

# add users 
apt-get install -y whois
for userdetails in `cat /vagrant/vagrant/rstudiousers.txt`
do
    user=`echo $userdetails | cut -f 1 -d ,`
    passwd=`echo $userdetails | cut -f 2 -d ,`
    useradd -p `mkpasswd $passwd` $user
done

# install dependencies
cd /vagrant/dependencies/linux
./install-dependencies-debian

# create build folder and run cmake
sudo -u vagrant mkdir -p /home/vagrant/rstudio-build
pushd .
cd /home/vagrant/rstudio-build
sudo -u vagrant cmake /vagrant/src/cpp
popd 

