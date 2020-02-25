# Huawei Image Clustering Demo

Checkout the documentation branch for sample images to use with the sample app and a video demo of the demo app running.

## Implementation Summary

This image clustering demo was created without using any GMS (Google Mobile Service) dependencies. The purpose of this demo is to demonstrate usage of the Huawei Map Kit, not a proper clustering algorithm. 

Individual images are downsized and placed on the map as markers. When touch events are registered by the Huawei Map OnCameraMoveStartedListener, the psuedo-clustering effect is triggered. The geographic coordinate of each marker is rounded to a particular decimal place depending on the current zoom level.

* Note: Create a new folder "PictureMap" in the root directory of phone and place images inside for demo to work.
