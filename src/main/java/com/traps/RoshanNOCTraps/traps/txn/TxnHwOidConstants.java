package com.traps.RoshanNOCTraps.traps.txn;

import org.snmp4j.smi.OID;

import java.util.Arrays;
import java.util.List;

public class TxnHwOidConstants {


    public static List<Long> alarmIdList = Arrays.asList(
            13444L,
            235L,
            13538L,
            12863L,
            166L,
            1L,
            13417L,
            13930L,
            265L,
            12587L,
            412L,
            631L,
            638L,
            12793L,
            12794L,
            8L,
            639L,
            640L,
            12305L,
            383L,
            12306L
    );



    public static final String FILE_PATH = "txn-hw-traps.txt";

    public static final OID TXN_HW_TRAP_IDENTIFIER = new OID("1.3.6.1.4.1.2011.2.15.1.7.1");
    public static final OID TXN_HW_ALARM_CODE = new OID("1.3.6.1.4.1.2011.2.15.1.7.1.25.0");
    public static final OID TXN_HW_ALARM_FAULT_ID = new OID("1.3.6.1.4.1.2011.2.15.1.7.1.26.0");
    public static final OID TXN_HW_ALARM_DEVICE_TYPE_ID = new OID("1.3.6.1.4.1.2011.2.15.1.7.1.27.0");
    public static final OID TXN_HW_ALARM_AFFECTED_TRAIL_NAME = new OID("1.3.6.1.4.1.2011.2.15.1.7.1.28.0");
    public static final OID TXN_HW_ALARM_IS_ROOT_ALARM = new OID("1.3.6.1.4.1.2011.2.15.1.7.1.29.0");
    public static final OID TXN_HW_ALARM_GROUP_ID = new OID("1.3.6.1.4.1.2011.2.15.1.7.1.30.0");
    public static final OID TXN_HW_ALARM_MAINTAINING_STATUS = new OID("1.3.6.1.4.1.2011.2.15.1.7.1.31.0");


    public static final OID TXN_HW_ALARM_NETWORK_ELEMENT_NAME = new OID("1.3.6.1.4.1.2011.2.15.1.7.1.1.0");
    public static final OID TXN_HW_ALARM_DEVICE_TYPE = new OID("1.3.6.1.4.1.2011.2.15.1.7.1.2.0");
    public static final OID TXN_HW_ALARM_INSTANCE_INFO = new OID("1.3.6.1.4.1.2011.2.15.1.7.1.3.0");
    public static final OID TXN_HW_ALARM_EVENT_TYPE = new OID("1.3.6.1.4.1.2011.2.15.1.7.1.4.0");
    public static final OID TXN_HW_ALARM_OCCUR_TIME = new OID("1.3.6.1.4.1.2011.2.15.1.7.1.5.0");
    public static final OID TXN_HW_ALARM_PROBABLE_CAUSE = new OID("1.3.6.1.4.1.2011.2.15.1.7.1.6.0");
    public static final OID TXN_HW_ALARM_LEVEL = new OID("1.3.6.1.4.1.2011.2.15.1.7.1.7.0");
    public static final OID TXN_HW_ALARM_DETAILS = new OID("1.3.6.1.4.1.2011.2.15.1.7.1.8.0");
    public static final OID TXN_HW_ALARM_ADDITIONAL_INFO = new OID("1.3.6.1.4.1.2011.2.15.1.7.1.9.0");
    public static final OID TXN_HW_ALARM_FAULT_FLAG = new OID("1.3.6.1.4.1.2011.2.15.1.7.1.10.0");
    public static final OID TXN_HW_ALARM_FUNCTION_TYPE = new OID("1.3.6.1.4.1.2011.2.15.1.7.1.11.0");
    public static final OID TXN_HW_ALARM_IP_ADDRESS = new OID("1.3.6.1.4.1.2011.2.15.1.7.1.12.0");
    public static final OID TXN_HW_ALARM_SERIAL_NUMBER = new OID("1.3.6.1.4.1.2011.2.15.1.7.1.13.0");
    public static final OID TXN_HW_ALARM_REPAIR_RECOMMENDATIONS = new OID("1.3.6.1.4.1.2011.2.15.1.7.1.14.0");
    public static final OID TXN_HW_ALARM_RESOURCE_ID = new OID("1.3.6.1.4.1.2011.2.15.1.7.1.15.0");
    public static final OID TXN_HW_ALARM_ADDITIONAL_INFORMATION = new OID("1.3.6.1.4.1.2011.2.15.1.7.1.16.0");
    public static final OID TXN_HW_ALARM_ADDITIONAL_INFORMATION2 = new OID("1.3.6.1.4.1.2011.2.15.1.7.1.17.0");
    public static final OID TXN_HW_ALARM_ADDITIONAL_INFORMATION3 = new OID("1.3.6.1.4.1.2011.2.15.1.7.1.18.0");
    public static final OID TXN_HW_ALARM_ADDITIONAL_INFORMATION4 = new OID("1.3.6.1.4.1.2011.2.15.1.7.1.19.0");
    public static final OID TXN_HW_ALARM_ADDITIONAL_INFORMATION5 = new OID("1.3.6.1.4.1.2011.2.15.1.7.1.20.0");
    public static final OID TXN_HW_ALARM_ADDITIONAL_INFORMATION6 = new OID("1.3.6.1.4.1.2011.2.15.1.7.1.21.0");
    public static final OID TXN_HW_ALARM_ADDITIONAL_INFORMATION7 = new OID("1.3.6.1.4.1.2011.2.15.1.7.1.22.0");
    public static final OID TXN_HW_ALARM_ADDITIONAL_INFORMATION8 = new OID("1.3.6.1.4.1.2011.2.15.1.7.1.23.0");
    public static final OID TXN_HW_ALARM_EVENT_NAME = new OID("1.3.6.1.4.1.2011.2.15.1.7.1.24.0");



}
