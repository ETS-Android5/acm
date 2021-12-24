#!/bin/bash

# Clean directory
rm -rf ACM
mkdir ACM

# ACM & TB-Loader
cp -av ../acm/dist/acm.jar ./ACM/
cp -av ../acm/dist/splash-acm.jpg ./ACM/
cp -av ../acm/dist/lib ./ACM/
cp -av ./bats/*.bat ./ACM/

# S3Sync
cp -av ../acm/dist/S3Sync.jar ./ACM/
cp -av ../acm/dist/ctrl-all.jar ./ACM/

# Combined version properties
 cat ../acm/dist/build.properties ../../S3Sync/dist/s3sync.properties >./ACM/build.properties

# Audio converters
cp -av ./converters ./ACM/

# Desktop icons and splash screen
mkdir ./ACM/images
cp -av ./images/* ./ACM/images/

# JRE
# cp -av ./jre ./ACM/