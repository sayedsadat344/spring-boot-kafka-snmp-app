package com.traps.RoshanNOCTraps.traps.trapsConfig;

import org.snmp4j.Snmp;
import org.snmp4j.smi.Address;
import org.snmp4j.util.MultiThreadedMessageDispatcher;
import org.snmp4j.util.ThreadPool;

public class TrapsConfig {

    protected MultiThreadedMessageDispatcher dispatcher;
    protected Snmp snmp = null;
    protected Address listenAddress;
    protected ThreadPool threadPool;
    protected int n = 0;
    protected long start = -1;

}
