/*
 * Copyright (C) Photon Vision.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.photonvision.vision.camera.USBCameras;

import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.cscore.UsbCamera;
import edu.wpi.first.cscore.VideoException;
import edu.wpi.first.cscore.VideoProperty;
import java.util.Objects;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.frame.provider.USBFrameProvider;
import org.photonvision.vision.processes.VisionSource;
import org.photonvision.vision.processes.VisionSourceSettables;

/**
 * A camera source that binds to a specific device path without using v4l2 by-path symlinks, vendor
 * IDs, or product IDs. The path provided by the user is used directly to open the camera device.
 */
public class PathCameraSource extends VisionSource {
    private final Logger logger;
    private final UsbCamera camera;
    private GenericUSBCameraSettables settables;
    private FrameProvider frameProvider;

    private void onCameraConnected() {
        printCameraProperties();
        settables.onCameraConnected();
    }

    public PathCameraSource(CameraConfiguration config) {
        super(config);

        logger = new Logger(PathCameraSource.class, config.nickname, LogGroup.Camera);

        camera = new UsbCamera(config.nickname, config.getDevicePath());

        settables = new GenericUSBCameraSettables(config, camera);
        logger.info("Created path-bound camera at " + config.getDevicePath());

        frameProvider = new USBFrameProvider(camera, settables, this::onCameraConnected);
    }

    private void printCameraProperties() {
        VideoProperty[] cameraProperties = null;
        try {
            cameraProperties = camera.enumerateProperties();
        } catch (VideoException e) {
            logger.error("Failed to list camera properties!", e);
        }

        if (cameraProperties != null) {
            StringBuilder cameraPropertiesStr = new StringBuilder("Cam Properties Dump:\n");
            for (VideoProperty prop : cameraProperties) {
                cameraPropertiesStr
                        .append("Name: ")
                        .append(prop.getName())
                        .append(", Kind: ")
                        .append(prop.getKind())
                        .append(", Value: ")
                        .append(prop.getKind().getValue())
                        .append(", Min: ")
                        .append(prop.getMin())
                        .append(", Max: ")
                        .append(prop.getMax())
                        .append(", Dflt: ")
                        .append(prop.getDefault())
                        .append(", Step: ")
                        .append(prop.getStep())
                        .append("\n");
            }
            logger.debug(cameraPropertiesStr.toString());
        }
    }

    @Override
    public FrameProvider getFrameProvider() {
        return frameProvider;
    }

    @Override
    public VisionSourceSettables getSettables() {
        return settables;
    }

    @Override
    public boolean isVendorCamera() {
        return false;
    }

    @Override
    public boolean hasLEDs() {
        return false;
    }

    @Override
    public void release() {
        CameraServer.removeCamera(camera.getName());
        camera.close();
        frameProvider.release();
        frameProvider = null;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        PathCameraSource other = (PathCameraSource) obj;
        return Objects.equals(camera, other.camera) && Objects.equals(settables, other.settables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(camera, settables, cameraConfiguration);
    }
}
