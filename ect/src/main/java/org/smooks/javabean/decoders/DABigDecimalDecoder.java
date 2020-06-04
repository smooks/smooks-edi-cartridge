/*-
 * ========================LICENSE_START=================================
 * smooks-ect
 * %%
 * Copyright (C) 2020 Smooks
 * %%
 * Licensed under the terms of the Apache License Version 2.0, or
 * the GNU Lesser General Public License version 3.0 or later.
 * 
 * SPDX-License-Identifier: Apache-2.0 OR LGPL-3.0-or-later
 * 
 * ======================================================================
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * ======================================================================
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 * =========================LICENSE_END==================================
 */
package org.smooks.javabean.decoders;

import org.smooks.container.ExecutionContext;
import org.smooks.delivery.Filter;
import org.smooks.edi.edisax.model.internal.Delimiters;
import org.smooks.javabean.DataDecodeException;
import org.smooks.javabean.DecodeType;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;

/**
 * {@link BigDecimal} Decoder, which is EDI delimiters aware for parsing decimal.
 * 
 * @author <a href="mailto:sinfomicien@gmail.com">sinfomicien@gmail.com</a>
 * @author <a href="mailto:michael@krueske.net">michael@krueske.net</a> (patched to ensure that always a {@link BigDecimal} value is decoded)
 */
@DecodeType(BigDecimal.class)
public class DABigDecimalDecoder extends BigDecimalDecoder {

    public Object decode(String data) throws DataDecodeException {
        DecimalFormat decimalFormat = getDecimalFormat();
        setDecimalPointFormat(decimalFormat, getContextDelimiters());
        
        final Number number;
        try {
            number = decimalFormat.parse(data.trim());
        } catch (final ParseException e) {
            throw new DataDecodeException("Failed to decode BigDecimal value '" + data
                    + "' using NumberFormat instance " + decimalFormat + ".", e);
        }
        
        return (BigDecimal) number;
    }

    public String encode(Object object) throws DataDecodeException {
        DecimalFormat decimalFormat = getDecimalFormat();
        return decimalFormat.format(object);
    }

    //Thread safe function to encode with delimiters awareness
    public String encode(Object object, Delimiters interchangeDelimiters) throws DataDecodeException {
        DecimalFormat decimalFormat = getDecimalFormat();
        setDecimalPointFormat(decimalFormat, interchangeDelimiters);
        return decimalFormat.format(object);
    }

    private synchronized DecimalFormat getDecimalFormat() {
        //Check to see if we can use the parent default format
        NumberFormat parentNumberFormat = getNumberFormat();
        
        if (parentNumberFormat != null && parentNumberFormat instanceof DecimalFormat) {
            // Clone because we potentially need to modify the decimal point...
            return (DecimalFormat) parentNumberFormat.clone();
        } else {
            return new DecimalFormat();
        }
    }

    private synchronized void setDecimalPointFormat(DecimalFormat decimalFormat, Delimiters interchangeDelimiters) {
        DecimalFormatSymbols dfs = decimalFormat.getDecimalFormatSymbols();

        decimalFormat.applyPattern("#0.0#");
        if (interchangeDelimiters != null) {
            dfs.setDecimalSeparator(interchangeDelimiters.getDecimalSeparator().charAt(0));
        }
        decimalFormat.setDecimalFormatSymbols(dfs);
        decimalFormat.setParseBigDecimal(true);
    }

    protected Delimiters getContextDelimiters() {
        ExecutionContext ec = Filter.getCurrentExecutionContext();
        Delimiters delimiters = null;

        if (ec != null) {
            delimiters = ec.getBeanContext().getBean(Delimiters.class);
        }

        return delimiters;
    }
}
