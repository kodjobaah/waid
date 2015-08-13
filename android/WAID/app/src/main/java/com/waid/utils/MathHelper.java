package com.waid.utils;
/**
 * Copyright (C) 2009, 2010 SC 4ViewSoft SRL
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
 */
//package org.achartengine.util;

import java.text.NumberFormat;
import java.util.List;

/**
 * Utility class for math operations.
 */
public class MathHelper {
    /** A value that is used a null value. */
    public static final double NULL_VALUE = Double.MAX_VALUE;

    /**
     * A number formatter to be used to make sure we have a maximum number of
     * fraction digits in the labels.
     */
    private static final NumberFormat FORMAT = NumberFormat.getNumberInstance();

    private MathHelper() {
        // empty constructor
    }

    public static double roundUp(double value, double factor)
    {
        final double up =
                value < 0 ? Math.ceil(value / factor) * factor : Math.floor(value / factor) * factor;
        // need to normalize -0 with +0 or we get a silly "-0" on our scale! :(
        return up == 0.0d ? 0.0d : up;
    }

}

