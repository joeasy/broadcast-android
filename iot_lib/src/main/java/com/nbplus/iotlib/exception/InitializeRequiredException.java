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

package com.nbplus.iotlib.exception;

/**
 * Created by basagee on 2015. 10. 14..
 */
public class InitializeRequiredException extends Exception {
    private int Err_Code;

    public InitializeRequiredException(String msg, int errCode) {
        super(msg);
        this.Err_Code = errCode;
    }

    public InitializeRequiredException(String msg){
        this(msg, 100);
    }

    public int getErrCode(){
        return Err_Code;
    }
}
