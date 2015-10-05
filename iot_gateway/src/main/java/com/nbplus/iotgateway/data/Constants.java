package com.nbplus.iotgateway.data;

/**
 * Created by basagee on 2015. 8. 6..
 */
public class Constants {
    public static final boolean USE_IOT_GATEWAY = false;

    // from App to Service
    public static final String ACTION_START_IOT_SERVICE = "com.nbplus.iotgateway.action.START_IOT_SERVICE";
    public static final String ACTION_GET_IOT_DEVICE_LIST = "com.nbplus.iotgateway.action.IOT_DEVICE_LIST";
    public static final String ACTION_SEND_IOT_COMMAND = "com.nbplus.iotgateway.action.SEND_IOT_COMMAND";
    // TODO : iot test
    public static final String ACTION_TEST_UPDATE_IOT_DATA = "com.nbplus.iotgateway.action.TEST_UPDATE_IOT_DATA";

    // from Service to App
    public static final String ACTION_IOT_DEVICE_LIST = "com.nbplus.iotgateway.action.IOT_DEVICE_LIST";
    public static final String ACTION_IOT_GATEWAY_DATA = "com.nbplus.iotgateway.action.IOT_GATEWAY_DATA";
    // do not use. 서비스에서 알아서 서버에 보낸다.
    public static final String ACTION_UPDATE_IOT_DEVICE_DATA = "com.nbplus.iotgateway.action.UPDATE_IOT_DEVICE_DATA";

//    public static final String EXTRA_IOT_GATEWAY_DATA = "EXTRA_IOT_GATEWAY_DATA";
    public static final String EXTRA_IOT_DEVICE_LIST = "EXTRA_IOT_DEVICE_LIST";
    public static final String EXTRA_IOT_SEND_COMM_DEVICE_ID = "EXTRA_IOT_SEND_COMM_DEVICE_ID";
    public static final String EXTRA_IOT_SEND_COMM_COMMAND_ID = "EXTRA_IOT_SEND_COMM_COMMAND_ID";
    // from App to service
    public static final String EXTRA_IOT_GATEWAY_CONNECTION_INFO = "EXTRA_IOT_DEVICE_LIST";

    enum IoTGatewayConnType {
        TYPE_TCP,           // tcp : 2015년개발용
        TYPE_HTTP,          // http
        TYPE_COAP,          // coap
        TYPE_MQTT
    }


    /**
     *
     * BLUETOOTH
     *
     */
    // flag value
    public static final int FLAG_VALUE_TRUE = 0x01;

    // value byte length
    public static final int DATE_TIME_LEN = 7;
    public static final int UINT16_LEN = 2;
    public static final int UINT8_LEN = 1;
    public static final int SFLOAT_LEN = 2;
    public static final int FLOAT_LEN = 4;

    /**
     Name: Record Access Control Point
     Type: org.bluetooth.characteristic.record_access_control_pointDownload / View
     Assigned Number: 0x2A52
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
    // request
    public static final int RACP_REQ_OP_CODE_REPORT_STORED_RECORDS = 0x01;  // with operator table value and operand
    public static final int RACP_REQ_OP_CODE_DELETE_STORED_RECORDS = 0x02;  // with operator table value and operand
    public static final int RACP_REQ_OP_CODE_ABORT_OPERATION = 0x03;        // with operator 0x00
    public static final int RACP_REQ_OP_CODE_REPORT_NUMBER_OF_STORED_RECORDS = 0x04;        // with operator 0x00
    // response
    public static final int RACP_RES_OP_CODE_NUMBER_OF_STORED_RECORDS = 0x05;   // response for op code 0x04. result number see operand 2bytes(num records)
    public static final int RACP_RES_OP_CODE_RESPONSE_CODE = 0x06;   // response for op code 0x01 ~ 0x03, with operator 0x00. req op code and result see operand

    // operator
    public static final int RACP_OPERATOR_NULL = 0x00;
    public static final int RACP_OPERATOR_ALL_RECORDS = 0x01;
    public static final int RACP_OPERATOR_LESS_THAN_OR_EQUAL_TO = 0x02;
    public static final int RACP_OPERATOR_GREATER_THAN_OR_EQUAL_TO = 0x03;
    public static final int RACP_OPERATOR_WITHIN_RANGE_OF = 0x04;       // inclusive
    public static final int RACP_OPERATOR_FIRST_RECORD = 0x05;          // oldest record
    public static final int RACP_OPERATOR_LAST_RECORD = 0x06;          // most recent record

    // oprand - op code / operand value correspondence
    public static final int RACP_OPERAND_OP_CORRESPOND_NA = 0x00;
    public static final int RACP_OPERAND_OP_CORRESPOND_FILTER_PARAM_1 = 0x01;
    public static final int RACP_OPERAND_OP_CORRESPOND_FILTER_PARAM_2 = 0x02;
    public static final int RACP_OPERAND_OP_CORRESPOND_NOT_INCLUDED = 0x03;
    public static final int RACP_OPERAND_OP_CORRESPOND_FILTER_PARAM_4 = 0x04;
    public static final int RACP_OPERAND_OP_CORRESPOND_NUMBER_OF_RECORDS = 0x05;
    public static final int RACP_OPERAND_OP_CORRESPOND_RESPONSE_FOR_OP = 0x06;

    // operand - response code values
    public static final int RACP_OPERAND_RES_VALUE_SUCCESS = 0x01;
    public static final int RACP_OPERAND_RES_VALUE_OP_CODE_NOT_SUPPORTED = 0x02;
    public static final int RACP_OPERAND_RES_VALUE_INVALID_OPERATOR = 0x03;
    public static final int RACP_OPERAND_RES_VALUE_OPERATOR_NOT_SUPPORTED = 0x04;
    public static final int RACP_OPERAND_RES_VALUE_INVALID_OPERAND = 0x05;
    public static final int RACP_OPERAND_RES_VALUE_NO_RECORDS_FOUND = 0x06;
    public static final int RACP_OPERAND_RES_VALUE_ABORT_UNSUCCESSFUL = 0x07;
    public static final int RACP_OPERAND_RES_VALUE_PROCEDURE_NOT_COMPLETED = 0x08;
    public static final int RACP_OPERAND_RES_VALUE_OPERAND_NOT_SUPPORTED = 0x09;

    /**
     * mi scale control point
     */
    public static final byte NOTIFY_MISCALE_SAVED_RECORD_NUMBER[] = new byte[] { (byte)0x01, (byte)0x95, (byte)0xb1, (byte)0xa2, (byte)0x5e };
    public static final byte NOTIFY_MISCALE_SAVED_RECORD_DATA[] = new byte[] { (byte)0x02 };
    public static final byte NOTIFY_MISCALE_SAVED_RECORD_DONE[] = new byte[] { (byte)0x03 };
    public static final byte DELETE_MISCALE_SAVED_RECORD[] = new byte[] { (byte)0x04, (byte)0x95, (byte)0xb1, (byte)0xa2, (byte)0x5e };

    /**
     * mi band control point - "ff05"
     */
    public static final byte WRITE_CONTROL_POINT_FACTORY_RESET[] = new byte[] { (byte)0x09 };
    public static final byte WRITE_CONTROL_POINT_REBOOT[] = new byte[] { (byte)0x0c };
    public static final byte WRITE_CONTROL_POINT_START_VIBRATION[] = new byte[] { (byte)0x08, (byte)0x02 };
    public static final byte WRITE_CONTROL_POINT_STOP_VIBRATION[] = new byte[] { (byte)0x13 };

    // uuid = "ff0d" (TEST)
    public static final byte WRITE_CHARACTERISTIC_PAIR[] = new byte[] { (byte)0x02 };
    public static final byte WRITE_CHARACTERISTIC_TEST_REMOVE_BONDING[] = new byte[] { (byte)0x01 };
}
