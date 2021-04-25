#!/bin/bash

# Run this script only after sourcing build/envsetup.sh

# Device
export DEVICE=sm8150-common

# Vendor
export VENDOR=oneplus

# Run the main extractor script
./../../../vendor/krypton/scripts/extract-main.sh $*
