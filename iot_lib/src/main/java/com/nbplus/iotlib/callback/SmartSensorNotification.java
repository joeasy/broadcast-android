/*
 * Copyright (c) 2015. NB Plus (www.nbplus.co.kr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.nbplus.iotlib.callback;

import com.nbplus.iotlib.data.IoTDevice;

/**
 * Created by basagee on 2015. 10. 14..
 */
public interface SmartSensorNotification {
    /**
     * 스마트센서 움직임에 관련된 콜백.
     * @param device
     * @param isActive
     */
    public void notifyMotionSensor(IoTDevice device, boolean isMotionActive, boolean isDoorOpened);
}
