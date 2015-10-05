package com.nbplus.iotgateway.data;

import android.text.format.Time;
import android.util.Log;

/**
 * Created by basagee on 2015. 9. 17..
 */
public class DataGenerator {
    private static final String TAG = DataGenerator.class.getSimpleName();

    public static byte[] getCurrentTimeData() {
        /*
         * org.bluetooth.characteristic.current_time
         * - Exact Time 256: org.bluetooth.characteristic.exact_time_256
         *   - Day Date Time: org.bluetooth.characteristic.day_date_time
         *     - Date Time: org.bluetooth.characteristic.date_time
         *       - Year: uint16
         *       - Month: uint8
         *       - Day: uint8
         *       - Hours: uint8
         *       - Minutes: uint8
         *       - Seconds: uint8
         *     - Day of Week: org.bluetooth.characteristic.day_of_week
         *       - uint8
         *   - Fractions256: uint8
         * - Adjust Reason: 8bit
         */
        Time currentTime = new Time();
        currentTime.setToNow();

        byte[] timeData = new byte[10];
        timeData[0] = (byte) (currentTime.year % 256);
        timeData[1] = (byte) (currentTime.year / 256);
        timeData[2] = (byte) (currentTime.month + 1);
        timeData[3] = (byte) (currentTime.monthDay);
        timeData[4] = (byte) (currentTime.hour);
        timeData[5] = (byte) (currentTime.minute);
        timeData[6] = (byte) (currentTime.second);
        timeData[7] = (byte) (currentTime.weekDay + 1);
        timeData[8] = 0;
        timeData[9] = 0;

        return timeData;
    }

    /**
     * Glucose Record Access Control Point
     *
     Names	Field Requirement	Format	Minimum Value	Maximum Value	Additional Information
     Op Code
         Mandatory
         uint8	N/A	N/A
         Enumerations
         Key	Value
         0	Reserved for future use (Operator:N/A)
         1	Report stored records (Operator: Value from Operator Table)
         2	Delete stored records (Operator: Value from Operator Table)
         3	Abort operation (Operator: Null 'value of 0x00 from Operator Table')
         4	Report number of stored records (Operator: Value from Operator Table)
         5	Number of stored records response (Operator: Null 'value of 0x00 from Operator Table')
         6	Response Code (Operator: Null 'value of 0x00 from Operator Table')
         7 - 255	Reserved for future use
     Operator
         Mandatory
         uint8	N/A	N/A
         Enumerations
         Key	Value
         0	Null
         1	All records
         2	Less than or equal to
         3	Greater than or equal to
         4	Within range of (inclusive)
         5	First record(i.e. oldest record)
         6	Last record (i.e. most recent record)
         7 - 255	Reserved for future use
     Operand
         Information:
         The operands correspond to the Op Code values (Keys 0 to 255) defined in the Op Code Field above
         Mandatory
         variable	N/A	N/A
         Op Code / Operand Value Correspondence
         Key	Value
         0	N/A
         1	Filter parameters (as appropriate to Operator and Service)
         2	Filter parameters (as appropriate to Operator and Service)
         3	Not included
         4	Filter parameters (as appropriate to Operator and Service)
         5	Number of Records (Field size defined per service)
         6	Request Op Code, Response Code Value
         7 - 255	Reserved for future use

     Response Code Values
         Key	Value	Description
         0	Reserved For Future Use	N/A
         1	Success	Normal response for successful operation
         2	Op Code not supported	Normal response if unsupported Op Code is received
         3	Invalid Operator	Normal response if Operator received does not meet the requirements of the service (e.g. Null was expected)
         4	Operator not supported	Normal response if unsupported Operator is received
         5	Invalid Operand	Normal response if Operand received does not meet the requirements of the service
         6	No records found	Normal response if request to report stored records or request to delete stored records resulted in no records meeting criteria.
         7	Abort unsuccessful	Normal response if request for Abort cannot be completed
         8	Procedure not completed	Normal response if unable to complete a procedure for any reason
         9	Operand not supported	Normal response if unsupported Operand is received
         10 - 255	Reserved for future use
     */
    public static byte[] getRacpOperation(int opCode, int operator) {
        return getRacpOperation(opCode, operator, -1, -1);
    }

    // greater than or less than
    public static byte[] getRacpOperation(int opCode, int operator, int sequenceOperand) {
        return getRacpOperation(opCode, operator, -1, -1);
    }

    // range of
    public static byte[] getRacpOperation(int opCode, int operator, int startOperand, int endOperand) {
        int valueLen = 0;
        byte[] value = null;

        switch (opCode) {
            case Constants.RACP_REQ_OP_CODE_REPORT_STORED_RECORDS :
            case Constants.RACP_REQ_OP_CODE_DELETE_STORED_RECORDS :
            case Constants.RACP_REQ_OP_CODE_REPORT_NUMBER_OF_STORED_RECORDS :
                valueLen++;
                if (operator == Constants.RACP_OPERATOR_ALL_RECORDS || operator == Constants.RACP_OPERATOR_FIRST_RECORD
                        || operator == Constants.RACP_OPERATOR_LAST_RECORD) {
                    valueLen++;         // no operand
                    value = new byte[valueLen];
                    value[0] = (byte) (opCode);
                    value[1] = (byte) (operator);
                } else if (operator == Constants.RACP_OPERATOR_GREATER_THAN_OR_EQUAL_TO
                        || operator == Constants.RACP_OPERATOR_LESS_THAN_OR_EQUAL_TO) {
                    if (startOperand == -1) {
                        Log.e(TAG, "range of must have operands");
                        return null;
                    }
                    valueLen += (Constants.UINT8_LEN * 2);
                    value = new byte[valueLen];
                    value[0] = (byte) (opCode);
                    value[1] = (byte) (operator);
                    // set uint8 operand - op code
                    value[2] = (byte)(opCode);
                    // set uint8 operand - sequence number
                    value[3] = (byte)(startOperand & 0x00FF);
                } else if (operator == Constants.RACP_OPERATOR_WITHIN_RANGE_OF) {
                    if (startOperand == -1 || endOperand == -1) {
                        Log.e(TAG, "range of must have two operands");
                        return null;
                    }
                    valueLen += (Constants.UINT8_LEN * 2);
                    value = new byte[valueLen];
                    value[0] = (byte) (opCode);
                    value[1] = (byte) (operator);
                    // set uint8 operand - start sequence number
                    value[2] = (byte)(startOperand & 0x00FF);
                    // set uint8 operand - end sequence number
                    value[3] = (byte)(startOperand & 0x00FF);
                } else {
                    Log.d(TAG, "invalid operator : opcode = " + opCode + ", operator = " + operator);
                    return null;
                }

                break;
            // TODO : do not implement
            case Constants.RACP_REQ_OP_CODE_ABORT_OPERATION :
                break;
            default:
                Log.d(TAG, "invalid opcode = " + opCode);
                break;
        }

        return value;
    }
}
