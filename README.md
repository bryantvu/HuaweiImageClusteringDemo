# Huawei Image Clustering Demo

## Implementation Summary

This image clustering demo was created without using any GMS (Google Mobile Service) dependencies. The purpose of this demo is to demonstrate image-marker clustering using the Huawei Map Kit, not to implement a proper clustering algorithm. 

Individual images are downsized and placed on the map as markers. When touch events are registered by the Huawei Map OnCameraMoveStartedListener, the psuedo-clustering effect is triggered. The geographic coordinate of each marker is rounded to a particular decimal place depending on the current zoom level.

*Notes*
1. Sample images are available in the "PictureMap gps pics" folder. Create a new folder "PictureMap" in the root directory of phone and place images inside.
2. Video Demo.m4v: video demo of the sample app running.
